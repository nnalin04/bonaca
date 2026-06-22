package com.bonaca.backend.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.bonaca.backend.TestcontainersConfiguration;
import com.bonaca.backend.auth.dto.RequestOtpRequest;
import com.bonaca.backend.auth.dto.VerifyOtpRequest;
import com.bonaca.backend.auth.model.OtpCode;
import com.bonaca.backend.auth.repository.OtpCodeRepository;
import com.bonaca.backend.common.ApiExceptionHandler;
import java.security.SecureRandom;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

/** A zero-minute OTP TTL means any code is expired by the time verify is called — no sleep needed. */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, OtpTestConfig.class})
@TestPropertySource(properties = "bonaca.otp.ttl-minutes=0")
class OtpExpiryIntegrationTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FakeOtpSender fakeOtpSender;

    @Autowired
    private OtpCodeRepository otpCodeRepository;

    @Test
    void verifyingAnExpiredOtpIsRejected() {
        String phone = "+91" + (6000000000L + RANDOM.nextInt(900000000));
        restTemplate.postForEntity("/api/v1/auth/otp/request", new RequestOtpRequest(phone), Void.class);
        String code = fakeOtpSender.lastCodeFor(phone);

        // Confirms the zero-minute TTL property actually took effect in the persisted row, not
        // just that the test expects it to.
        OtpCode persistedOtpCode = otpCodeRepository
                .findTopByPhoneNumberAndConsumedAtIsNullOrderByCreatedAtDesc(phone)
                .orElseThrow();
        assertThat(persistedOtpCode.isExpired(Instant.now())).isTrue();

        ResponseEntity<ApiExceptionHandler.ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/otp/verify", new VerifyOtpRequest(phone, code), ApiExceptionHandler.ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // And confirms the rejected verify attempt did NOT mark the row consumed in the database.
        OtpCode afterRejectedVerify = otpCodeRepository
                .findTopByPhoneNumberAndConsumedAtIsNullOrderByCreatedAtDesc(phone)
                .orElseThrow();
        assertThat(afterRejectedVerify.isConsumed()).isFalse();
    }
}
