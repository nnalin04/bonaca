package com.bonaca.backend.auth.service;

import com.bonaca.backend.auth.config.OtpProperties;
import com.bonaca.backend.auth.exception.InvalidOtpException;
import com.bonaca.backend.auth.exception.OtpExpiredException;
import com.bonaca.backend.auth.exception.OtpLockedException;
import com.bonaca.backend.auth.exception.RateLimitExceededException;
import com.bonaca.backend.auth.model.OtpCode;
import com.bonaca.backend.auth.model.User;
import com.bonaca.backend.auth.repository.OtpCodeRepository;
import com.bonaca.backend.auth.repository.UserRepository;
import com.bonaca.backend.auth.service.otp.OtpSender;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpService {

    private final OtpCodeRepository otpCodeRepository;
    private final UserRepository userRepository;
    private final OtpSender otpSender;
    private final PasswordEncoder passwordEncoder;
    private final OtpProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(
            OtpCodeRepository otpCodeRepository,
            UserRepository userRepository,
            OtpSender otpSender,
            PasswordEncoder passwordEncoder,
            OtpProperties properties) {
        this.otpCodeRepository = otpCodeRepository;
        this.userRepository = userRepository;
        this.otpSender = otpSender;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Transactional
    public void requestOtp(String phoneNumber) {
        Instant windowStart = Instant.now().minus(Duration.ofHours(1));
        long recentRequests = otpCodeRepository.countByPhoneNumberAndCreatedAtAfter(phoneNumber, windowStart);
        if (recentRequests >= properties.maxRequestsPerHour()) {
            throw new RateLimitExceededException("Too many OTP requests for this number — try again later");
        }

        String code = generateCode();
        String codeHash = passwordEncoder.encode(code);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(properties.ttlMinutes()));

        otpCodeRepository.save(new OtpCode(phoneNumber, codeHash, expiresAt));
        otpSender.send(phoneNumber, code);
    }

    /**
     * noRollbackFor is required here: the wrong-code branch deliberately persists an
     * incremented attempt count and then throws — without this, Spring's default
     * rollback-on-RuntimeException would silently undo the increment, making the
     * attempt-lockout unenforceable (every wrong guess would look like the first).
     */
    @Transactional(noRollbackFor = InvalidOtpException.class)
    public User verifyOtp(String phoneNumber, String code) {
        OtpCode otpCode = otpCodeRepository
                .findTopByPhoneNumberAndConsumedAtIsNullOrderByCreatedAtDesc(phoneNumber)
                .orElseThrow(() -> new InvalidOtpException("No OTP was requested for this number"));

        if (otpCode.isExpired(Instant.now())) {
            throw new OtpExpiredException("This OTP has expired — request a new one");
        }
        if (otpCode.hasExceededAttempts(properties.maxVerifyAttempts())) {
            throw new OtpLockedException("Too many incorrect attempts — request a new OTP");
        }

        if (!passwordEncoder.matches(code, otpCode.getCodeHash())) {
            otpCode.incrementAttempts();
            otpCodeRepository.save(otpCode);
            throw new InvalidOtpException("Incorrect OTP");
        }

        User user = userRepository
                .findByPhoneNumber(phoneNumber)
                .orElseGet(() -> userRepository.save(new User(phoneNumber)));

        otpCode.markConsumed(user.getId());
        otpCodeRepository.save(otpCode);

        return user;
    }

    private String generateCode() {
        int length = properties.codeLength();
        int max = (int) Math.pow(10, length);
        int value = secureRandom.nextInt(max);
        return String.format("%0" + length + "d", value);
    }
}
