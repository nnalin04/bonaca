package com.bonaca.backend.auth.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OtpCodeTest {

    @Test
    void isConsumedIsFalseUntilMarkConsumedIsCalled() {
        OtpCode code = new OtpCode("+919876543210", "hash", Instant.now().plus(5, ChronoUnit.MINUTES));

        assertThat(code.isConsumed()).isFalse();

        code.markConsumed(UUID.randomUUID());

        assertThat(code.isConsumed()).isTrue();
    }

    @Test
    void accessorsReflectConstructorArguments() {
        Instant expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES);
        OtpCode code = new OtpCode("+919876543210", "a-hash", expiresAt);

        assertThat(code.getPhoneNumber()).isEqualTo("+919876543210");
        assertThat(code.getCodeHash()).isEqualTo("a-hash");
        assertThat(code.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(code.getCreatedAt()).isNotNull();
        assertThat(code.getAttemptCount()).isZero();
    }
}
