package com.bonaca.backend.auth.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** isActive is the combined "still usable" predicate RefreshTokenService's intent depends on. */
class RefreshTokenTest {

    @Test
    void isActiveIsTrueWhenNeitherExpiredNorRevoked() {
        RefreshToken token = new RefreshToken(UUID.randomUUID(), "hash", Instant.now().plus(30, ChronoUnit.DAYS));

        assertThat(token.isActive(Instant.now())).isTrue();
    }

    @Test
    void isActiveIsFalseWhenExpired() {
        RefreshToken token = new RefreshToken(UUID.randomUUID(), "hash", Instant.now().minus(1, ChronoUnit.DAYS));

        assertThat(token.isActive(Instant.now())).isFalse();
    }

    @Test
    void isActiveIsFalseWhenRevokedEvenIfNotExpired() {
        RefreshToken token = new RefreshToken(UUID.randomUUID(), "hash", Instant.now().plus(30, ChronoUnit.DAYS));
        token.revoke(null);

        assertThat(token.isActive(Instant.now())).isFalse();
    }

    @Test
    void accessorsReflectConstructorAndRevokeArguments() {
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(30, ChronoUnit.DAYS);
        RefreshToken token = new RefreshToken(userId, "a-hash", expiresAt);
        UUID replacementId = UUID.randomUUID();

        token.revoke(replacementId);

        assertThat(token.getUserId()).isEqualTo(userId);
        assertThat(token.getTokenHash()).isEqualTo("a-hash");
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(token.getReplacedById()).isEqualTo(replacementId);
        assertThat(token.getRevokedAt()).isNotNull();
    }
}
