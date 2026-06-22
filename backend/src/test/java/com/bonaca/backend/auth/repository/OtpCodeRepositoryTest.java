package com.bonaca.backend.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bonaca.backend.auth.model.OtpCode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

/**
 * Contract OtpService relies on: findTopBy...OrderByCreatedAtDesc must return only the latest
 * *unconsumed* OTP for a phone number (a consumed one must never be re-verifiable), and
 * countByPhoneNumberAndCreatedAtAfter must scope strictly to the rate-limit window, not count
 * older requests outside it.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class OtpCodeRepositoryTest {

    private static final String PHONE = "+919876543210";

    @Autowired
    private OtpCodeRepository otpCodeRepository;

    @Test
    void findTopByPhoneIgnoresConsumedCodesAndReturnsTheNewestUnconsumedOne() {
        OtpCode older = new OtpCode(PHONE, "hash-older", Instant.now().plus(5, ChronoUnit.MINUTES));
        otpCodeRepository.saveAndFlush(older);
        OtpCode consumed = new OtpCode(PHONE, "hash-consumed", Instant.now().plus(5, ChronoUnit.MINUTES));
        consumed.markConsumed(java.util.UUID.randomUUID());
        otpCodeRepository.saveAndFlush(consumed);
        OtpCode newest = new OtpCode(PHONE, "hash-newest", Instant.now().plus(5, ChronoUnit.MINUTES));
        otpCodeRepository.saveAndFlush(newest);

        Optional<OtpCode> found =
                otpCodeRepository.findTopByPhoneNumberAndConsumedAtIsNullOrderByCreatedAtDesc(PHONE);

        assertThat(found).isPresent();
        assertThat(found.get().getCodeHash()).isEqualTo("hash-newest");
    }

    @Test
    void findTopByPhoneReturnsEmptyWhenEveryCodeForThatNumberIsConsumed() {
        OtpCode consumed = new OtpCode(PHONE, "hash", Instant.now().plus(5, ChronoUnit.MINUTES));
        consumed.markConsumed(java.util.UUID.randomUUID());
        otpCodeRepository.saveAndFlush(consumed);

        assertThat(otpCodeRepository.findTopByPhoneNumberAndConsumedAtIsNullOrderByCreatedAtDesc(PHONE))
                .isEmpty();
    }

    @Test
    void countByPhoneNumberAndCreatedAtAfterOnlyCountsRequestsInsideTheWindow() {
        OtpCode insideWindow = new OtpCode(PHONE, "hash-1", Instant.now().plus(5, ChronoUnit.MINUTES));
        otpCodeRepository.saveAndFlush(insideWindow);

        long countWithGenerousWindow = otpCodeRepository.countByPhoneNumberAndCreatedAtAfter(
                PHONE, Instant.now().minus(1, ChronoUnit.HOURS));
        long countWithFutureWindow = otpCodeRepository.countByPhoneNumberAndCreatedAtAfter(
                PHONE, Instant.now().plus(1, ChronoUnit.HOURS));

        assertThat(countWithGenerousWindow).isEqualTo(1);
        assertThat(countWithFutureWindow).isEqualTo(0);
    }
}
