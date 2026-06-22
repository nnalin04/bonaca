package com.bonaca.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Contract from TokenHasher's class Javadoc: refresh tokens are looked up by hash equality, so
 * the digest must be deterministic (same input always yields the same output) — unlike
 * OtpService's BCrypt usage, which is deliberately non-deterministic and unsuitable as a lookup
 * key. SHA-256 always produces a 32-byte digest, i.e. a 64-character lowercase hex string.
 */
class TokenHasherTest {

    private final TokenHasher tokenHasher = new TokenHasher();

    @Test
    void sameInputAlwaysProducesTheSameHash() {
        String input = "a-raw-refresh-token-value";

        assertThat(tokenHasher.sha256Hex(input)).isEqualTo(tokenHasher.sha256Hex(input));
    }

    @Test
    void hashIsA64CharacterLowercaseHexDigest() {
        String hash = tokenHasher.sha256Hex("any-value");

        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void differentInputsProduceDifferentHashes() {
        String first = tokenHasher.sha256Hex("token-one");
        String second = tokenHasher.sha256Hex("token-two");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void hashDoesNotContainTheRawInput() {
        String input = "super-secret-raw-token";

        assertThat(tokenHasher.sha256Hex(input)).doesNotContain(input);
    }
}
