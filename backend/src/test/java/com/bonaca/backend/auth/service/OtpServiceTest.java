package com.bonaca.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final String PHONE = "+919876543210";

    @Mock
    private OtpCodeRepository otpCodeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpSender otpSender;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        OtpProperties properties = new OtpProperties(4, 5, 5, 5);
        otpService = new OtpService(otpCodeRepository, userRepository, otpSender, passwordEncoder, properties);
    }

    @Test
    void requestOtpGeneratesAndSendsACodeOfTheConfiguredLength() {
        when(otpCodeRepository.countByPhoneNumberAndCreatedAtAfter(eq(PHONE), any())).thenReturn(0L);
        when(otpCodeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        otpService.requestOtp(PHONE);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(otpSender).send(eq(PHONE), codeCaptor.capture());
        assertThat(codeCaptor.getValue()).hasSize(4).containsOnlyDigits();

        ArgumentCaptor<OtpCode> savedCaptor = ArgumentCaptor.forClass(OtpCode.class);
        verify(otpCodeRepository).save(savedCaptor.capture());
        assertThat(passwordEncoder.matches(codeCaptor.getValue(), savedCaptor.getValue().getCodeHash())).isTrue();
    }

    @Test
    void requestOtpIsRateLimitedAfterTooManyRequestsInTheWindow() {
        when(otpCodeRepository.countByPhoneNumberAndCreatedAtAfter(eq(PHONE), any())).thenReturn(5L);

        assertThatThrownBy(() -> otpService.requestOtp(PHONE)).isInstanceOf(RateLimitExceededException.class);

        verify(otpSender, never()).send(anyString(), anyString());
    }

    @Test
    void verifyOtpThrowsWhenNoOtpWasRequested() {
        when(otpCodeRepository.findTopByPhoneNumberAndConsumedAtIsNullOrderByCreatedAtDesc(PHONE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.verifyOtp(PHONE, "1234")).isInstanceOf(InvalidOtpException.class);
    }

    @Test
    void verifyOtpThrowsWhenExpired() {
        OtpCode expired = new OtpCode(PHONE, passwordEncoder.encode("1234"), Instant.now().minus(1, ChronoUnit.MINUTES));
        when(otpCodeRepository.findTopByPhoneNumberAndConsumedAtIsNullOrderByCreatedAtDesc(PHONE))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> otpService.verifyOtp(PHONE, "1234")).isInstanceOf(OtpExpiredException.class);
    }

    @Test
    void verifyOtpThrowsWhenAttemptsExceeded() {
        OtpCode code = new OtpCode(PHONE, passwordEncoder.encode("1234"), Instant.now().plus(5, ChronoUnit.MINUTES));
        for (int i = 0; i < 5; i++) {
            code.incrementAttempts();
        }
        when(otpCodeRepository.findTopByPhoneNumberAndConsumedAtIsNullOrderByCreatedAtDesc(PHONE))
                .thenReturn(Optional.of(code));

        assertThatThrownBy(() -> otpService.verifyOtp(PHONE, "1234")).isInstanceOf(OtpLockedException.class);
    }

    @Test
    void verifyOtpIncrementsAttemptCountOnWrongCode() {
        OtpCode code = new OtpCode(PHONE, passwordEncoder.encode("1234"), Instant.now().plus(5, ChronoUnit.MINUTES));
        when(otpCodeRepository.findTopByPhoneNumberAndConsumedAtIsNullOrderByCreatedAtDesc(PHONE))
                .thenReturn(Optional.of(code));

        assertThatThrownBy(() -> otpService.verifyOtp(PHONE, "0000")).isInstanceOf(InvalidOtpException.class);

        assertThat(code.getAttemptCount()).isEqualTo(1);
        verify(otpCodeRepository).save(code);
    }

    @Test
    void verifyOtpCreatesNewUserOnFirstSuccessfulVerification() {
        OtpCode code = new OtpCode(PHONE, passwordEncoder.encode("1234"), Instant.now().plus(5, ChronoUnit.MINUTES));
        when(otpCodeRepository.findTopByPhoneNumberAndConsumedAtIsNullOrderByCreatedAtDesc(PHONE))
                .thenReturn(Optional.of(code));
        when(userRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User result = otpService.verifyOtp(PHONE, "1234");

        assertThat(result.getPhoneNumber()).isEqualTo(PHONE);
        verify(userRepository).save(any(User.class));
        assertThat(code.isConsumed()).isTrue();
    }

    @Test
    void verifyOtpReusesExistingUserOnSubsequentVerification() {
        OtpCode code = new OtpCode(PHONE, passwordEncoder.encode("1234"), Instant.now().plus(5, ChronoUnit.MINUTES));
        when(otpCodeRepository.findTopByPhoneNumberAndConsumedAtIsNullOrderByCreatedAtDesc(PHONE))
                .thenReturn(Optional.of(code));
        User existing = new User(PHONE);
        when(userRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.of(existing));

        User result = otpService.verifyOtp(PHONE, "1234");

        assertThat(result).isSameAs(existing);
        verify(userRepository, times(0)).save(any(User.class));
    }
}
