package com.bonaca.backend.auth.service;

import com.bonaca.backend.auth.config.JwtProperties;
import com.bonaca.backend.auth.exception.InvalidRefreshTokenException;
import com.bonaca.backend.auth.model.RefreshToken;
import com.bonaca.backend.auth.repository.RefreshTokenRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher tokenHasher;
    private final Duration refreshTokenTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository, TokenHasher tokenHasher, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHasher = tokenHasher;
        this.refreshTokenTtl = Duration.ofDays(jwtProperties.refreshTokenTtlDays());
    }

    @Transactional
    public String issue(UUID userId) {
        String rawToken = generateRawToken();
        String tokenHash = tokenHasher.sha256Hex(rawToken);
        Instant expiresAt = Instant.now().plus(refreshTokenTtl);
        refreshTokenRepository.save(new RefreshToken(userId, tokenHash, expiresAt));
        return rawToken;
    }

    /**
     * Validates and rotates a refresh token: the presented token is revoked and a new one is
     * issued in its place. If the presented token was already revoked (i.e. it's a replay of a
     * token that was already used to rotate once before — a sign of token theft), the entire
     * chain descending from it is revoked too, forcing the user to log in again.
     */
    /**
     * noRollbackFor is required here: the replay-detection branch deliberately revokes the
     * rest of the token chain and then throws — without this, Spring's default
     * rollback-on-RuntimeException would silently undo that revocation, leaving the stolen
     * token's successor still valid (defeating the whole point of replay detection).
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RotationResult rotate(String rawToken) {
        String tokenHash = tokenHasher.sha256Hex(rawToken);
        RefreshToken token = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Unknown refresh token"));

        if (token.isRevoked()) {
            revokeChainFrom(token);
            throw new InvalidRefreshTokenException("Refresh token was already used — session revoked");
        }
        if (token.isExpired(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        String newRawToken = generateRawToken();
        String newTokenHash = tokenHasher.sha256Hex(newRawToken);
        RefreshToken next = new RefreshToken(token.getUserId(), newTokenHash, Instant.now().plus(refreshTokenTtl));
        refreshTokenRepository.save(next);

        token.revoke(next.getId());
        refreshTokenRepository.save(token);

        return new RotationResult(token.getUserId(), newRawToken);
    }

    @Transactional
    public void revoke(String rawToken) {
        String tokenHash = tokenHasher.sha256Hex(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.revoke(null);
            refreshTokenRepository.save(token);
        });
    }

    private void revokeChainFrom(RefreshToken token) {
        RefreshToken current = token;
        while (current.getReplacedById() != null) {
            current = refreshTokenRepository.findById(current.getReplacedById()).orElse(null);
            if (current == null || current.isRevoked()) {
                break;
            }
            current.revoke(null);
            refreshTokenRepository.save(current);
        }
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record RotationResult(UUID userId, String newRefreshToken) {
    }
}
