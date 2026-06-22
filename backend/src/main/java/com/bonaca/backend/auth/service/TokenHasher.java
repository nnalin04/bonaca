package com.bonaca.backend.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * SHA-256 hashing for refresh tokens specifically — NOT for OTP codes (those use
 * BCryptPasswordEncoder via Spring Security, see OtpService). Refresh tokens must be looked up
 * by hash equality in the database, which requires a deterministic digest; BCrypt salts its
 * output randomly on every call, so the same input never hashes the same way twice and can't be
 * used as a lookup key. SHA-256 is deterministic and the input (a 256-bit random token) already
 * has enough entropy that a fast hash isn't a brute-force risk the way it would be for a
 * low-entropy password.
 */
@Component
public class TokenHasher {

    public String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
