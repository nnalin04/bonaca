package com.bonaca.backend.auth.service;

import com.bonaca.backend.auth.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Issues and validates short-lived access JWTs. Refresh tokens are opaque
 * random strings stored hashed in the database (see RefreshTokenService),
 * not JWTs — they're never sent to a resource server, so there's nothing to
 * gain from making them self-describing, and an opaque token is trivially
 * revocable by deleting/marking the DB row.
 */
@Service
public class JwtService {

    private final Key signingKey;
    private final Duration accessTokenTtl;

    public JwtService(JwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.accessTokenTtl = Duration.ofMinutes(properties.accessTokenTtlMinutes());
    }

    public String issueAccessToken(UUID userId, String phoneNumber) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("phoneNumber", phoneNumber)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(signingKey)
                .compact();
    }

    /** Returns empty if the token is missing, malformed, expired, or has an invalid signature. */
    public Optional<AccessTokenClaims> validateAccessToken(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            String phoneNumber = claims.get("phoneNumber", String.class);
            return Optional.of(new AccessTokenClaims(userId, phoneNumber));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record AccessTokenClaims(UUID userId, String phoneNumber) {
    }
}
