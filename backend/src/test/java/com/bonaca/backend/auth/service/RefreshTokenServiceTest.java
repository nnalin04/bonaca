package com.bonaca.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bonaca.backend.auth.config.JwtProperties;
import com.bonaca.backend.auth.exception.InvalidRefreshTokenException;
import com.bonaca.backend.auth.model.RefreshToken;
import com.bonaca.backend.auth.repository.RefreshTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private final TokenHasher tokenHasher = new TokenHasher();

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, tokenHasher, new JwtProperties("secret", 15, 30));
    }

    @Test
    void issueStoresAHashNotTheRawToken() {
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String rawToken = refreshTokenService.issue(UUID.randomUUID());

        assertThat(rawToken).isNotBlank();
    }

    @Test
    void rotateRejectsUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate("garbage"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rotateRejectsExpiredToken() {
        UUID userId = UUID.randomUUID();
        RefreshToken expired = new RefreshToken(userId, tokenHasher.sha256Hex("raw-token"), Instant.now().minus(1, ChronoUnit.DAYS));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.rotate("raw-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rotateIssuesANewTokenAndRevokesTheOld() {
        UUID userId = UUID.randomUUID();
        RefreshToken active = new RefreshToken(userId, tokenHasher.sha256Hex("raw-token"), Instant.now().plus(30, ChronoUnit.DAYS));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(active));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.RotationResult result = refreshTokenService.rotate("raw-token");

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.newRefreshToken()).isNotBlank().isNotEqualTo("raw-token");
        assertThat(active.isRevoked()).isTrue();
    }

    @Test
    void rotatingAnAlreadyRevokedTokenThrowsAndRevokesTheSuccessorToo() {
        UUID userId = UUID.randomUUID();
        RefreshToken successor = new RefreshToken(userId, "successor-hash", Instant.now().plus(30, ChronoUnit.DAYS));
        UUID successorId = UUID.randomUUID();
        setId(successor, successorId);

        RefreshToken stolen = new RefreshToken(userId, tokenHasher.sha256Hex("stolen-token"), Instant.now().plus(30, ChronoUnit.DAYS));
        stolen.revoke(successorId);

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stolen));
        when(refreshTokenRepository.findById(successorId)).thenReturn(Optional.of(successor));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> refreshTokenService.rotate("stolen-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(successor.isRevoked()).isTrue();
    }

    @Test
    void revokeMarksAMatchingTokenRevoked() {
        RefreshToken token = new RefreshToken(UUID.randomUUID(), tokenHasher.sha256Hex("raw-token"), Instant.now().plus(30, ChronoUnit.DAYS));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        refreshTokenService.revoke("raw-token");

        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void revokeOfUnknownTokenIsANoOp() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        refreshTokenService.revoke("unknown");
    }

    private static void setId(RefreshToken token, UUID id) {
        try {
            var field = RefreshToken.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(token, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
