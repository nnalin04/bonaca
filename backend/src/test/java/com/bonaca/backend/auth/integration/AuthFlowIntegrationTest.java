package com.bonaca.backend.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.bonaca.backend.TestcontainersConfiguration;
import com.bonaca.backend.auth.dto.AuthTokensResponse;
import com.bonaca.backend.auth.dto.MeResponse;
import com.bonaca.backend.auth.dto.RefreshRequest;
import com.bonaca.backend.auth.dto.RequestOtpRequest;
import com.bonaca.backend.auth.dto.VerifyOtpRequest;
import com.bonaca.backend.auth.model.OtpCode;
import com.bonaca.backend.auth.model.RefreshToken;
import com.bonaca.backend.auth.repository.OtpCodeRepository;
import com.bonaca.backend.auth.repository.RefreshTokenRepository;
import com.bonaca.backend.auth.repository.UserRepository;
import com.bonaca.backend.auth.service.TokenHasher;
import com.bonaca.backend.common.ApiExceptionHandler;
import java.security.SecureRandom;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, OtpTestConfig.class})
class AuthFlowIntegrationTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FakeOtpSender fakeOtpSender;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpCodeRepository otpCodeRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TokenHasher tokenHasher;

    private String uniquePhoneNumber() {
        return "+91" + (6000000000L + RANDOM.nextInt(900000000));
    }

    private void requestOtp(String phone) {
        ResponseEntity<Void> response =
                restTemplate.postForEntity("/api/v1/auth/otp/request", new RequestOtpRequest(phone), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void fullHappyPathFromOtpRequestToProtectedEndpoint() {
        String phone = uniquePhoneNumber();
        requestOtp(phone);
        String code = fakeOtpSender.lastCodeFor(phone);

        ResponseEntity<AuthTokensResponse> verifyResponse = restTemplate.postForEntity(
                "/api/v1/auth/otp/verify", new VerifyOtpRequest(phone, code), AuthTokensResponse.class);

        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuthTokensResponse tokens = verifyResponse.getBody();
        assertThat(tokens).isNotNull();
        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.profileCompleted()).isFalse();

        // The full pipeline for this app has exactly 3 real hops — controller -> service ->
        // repository -> Postgres, no cache layer anywhere — so fetching the row directly here
        // and comparing it field-by-field against the GET response below proves the read path
        // is actually serving what's in the database, not two independently-plausible values.
        var persistedUser = userRepository.findByPhoneNumber(phone);
        assertThat(persistedUser).isPresent();
        assertThat(persistedUser.get().isProfileCompleted()).isFalse();

        var persistedRefreshToken = refreshTokenRepository.findByTokenHash(tokenHasher.sha256Hex(tokens.refreshToken()));
        assertThat(persistedRefreshToken).isPresent();
        assertThat(persistedRefreshToken.get().getUserId()).isEqualTo(persistedUser.get().getId());
        assertThat(persistedRefreshToken.get().isRevoked()).isFalse();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken());
        ResponseEntity<MeResponse> meResponse =
                restTemplate.exchange("/api/v1/auth/me", org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers), MeResponse.class);

        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody()).isNotNull();
        // Direct DB-row-to-API-response comparison, not two values independently checked
        // against the test's local `phone` variable.
        assertThat(meResponse.getBody().userId()).isEqualTo(persistedUser.get().getId());
        assertThat(meResponse.getBody().phoneNumber()).isEqualTo(persistedUser.get().getPhoneNumber());
        assertThat(meResponse.getBody().profileCompleted()).isEqualTo(persistedUser.get().isProfileCompleted());
    }

    @Test
    void wrongCodeIsRejectedAndLocksOutAfterTooManyAttempts() {
        String phone = uniquePhoneNumber();
        requestOtp(phone);

        for (int i = 0; i < 5; i++) {
            ResponseEntity<ApiExceptionHandler.ErrorResponse> response = restTemplate.postForEntity(
                    "/api/v1/auth/otp/verify", new VerifyOtpRequest(phone, "0000"), ApiExceptionHandler.ErrorResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        // Confirms the attempt count was actually persisted to Postgres after each wrong guess —
        // not just that the API kept returning 401.
        OtpCode persistedOtpCode = otpCodeRepository
                .findTopByPhoneNumberAndConsumedAtIsNullOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> new AssertionError("Expected an OTP code row to exist for " + phone));
        assertThat(persistedOtpCode.getAttemptCount()).isEqualTo(5);

        // even the correct code is now rejected — the OTP is locked out, not just the wrong guess
        String code = fakeOtpSender.lastCodeFor(phone);
        ResponseEntity<ApiExceptionHandler.ErrorResponse> lockedResponse = restTemplate.postForEntity(
                "/api/v1/auth/otp/verify", new VerifyOtpRequest(phone, code), ApiExceptionHandler.ErrorResponse.class);
        assertThat(lockedResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void sixthOtpRequestWithinTheHourIsRateLimited() {
        String phone = uniquePhoneNumber();
        for (int i = 0; i < 5; i++) {
            requestOtp(phone);
        }

        // Confirms 5 distinct OTP code rows were actually written to Postgres, not just that 5
        // requests each individually returned 202.
        long persistedCount = otpCodeRepository.countByPhoneNumberAndCreatedAtAfter(phone, Instant.now().minusSeconds(3600));
        assertThat(persistedCount).isEqualTo(5);

        ResponseEntity<ApiExceptionHandler.ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/otp/request", new RequestOtpRequest(phone), ApiExceptionHandler.ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // And confirms the 6th, rejected attempt did NOT add a 6th row.
        assertThat(otpCodeRepository.countByPhoneNumberAndCreatedAtAfter(phone, Instant.now().minusSeconds(3600))).isEqualTo(5);
    }

    @Test
    void refreshRotatesTheTokenAndTheOldOneStopsWorking() {
        String phone = uniquePhoneNumber();
        requestOtp(phone);
        String code = fakeOtpSender.lastCodeFor(phone);
        AuthTokensResponse initialTokens = restTemplate
                .postForEntity("/api/v1/auth/otp/verify", new VerifyOtpRequest(phone, code), AuthTokensResponse.class)
                .getBody();
        assertThat(initialTokens).isNotNull();

        ResponseEntity<AuthTokensResponse> refreshResponse = restTemplate.postForEntity(
                "/api/v1/auth/refresh", new RefreshRequest(initialTokens.refreshToken()), AuthTokensResponse.class);
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuthTokensResponse rotatedTokens = refreshResponse.getBody();
        assertThat(rotatedTokens).isNotNull();
        assertThat(rotatedTokens.refreshToken()).isNotEqualTo(initialTokens.refreshToken());

        // Confirms the rotation was actually persisted: the old token's row is marked revoked
        // in Postgres, and the new token's row exists and isn't.
        RefreshToken oldTokenRow = refreshTokenRepository
                .findByTokenHash(tokenHasher.sha256Hex(initialTokens.refreshToken()))
                .orElseThrow(() -> new AssertionError("Expected the original refresh token row to still exist"));
        assertThat(oldTokenRow.isRevoked()).isTrue();
        RefreshToken newTokenRow = refreshTokenRepository
                .findByTokenHash(tokenHasher.sha256Hex(rotatedTokens.refreshToken()))
                .orElseThrow(() -> new AssertionError("Expected the rotated refresh token row to exist"));
        assertThat(newTokenRow.isRevoked()).isFalse();
        assertThat(oldTokenRow.getReplacedById()).isEqualTo(newTokenRow.getId());

        // reusing the old (now-rotated) refresh token must fail, and per the replay-attack
        // protection, must also revoke the token that replaced it
        ResponseEntity<ApiExceptionHandler.ErrorResponse> reuseResponse = restTemplate.postForEntity(
                "/api/v1/auth/refresh", new RefreshRequest(initialTokens.refreshToken()), ApiExceptionHandler.ErrorResponse.class);
        assertThat(reuseResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<ApiExceptionHandler.ErrorResponse> rotatedNowDeadResponse = restTemplate.postForEntity(
                "/api/v1/auth/refresh", new RefreshRequest(rotatedTokens.refreshToken()), ApiExceptionHandler.ErrorResponse.class);
        assertThat(rotatedNowDeadResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Confirms the replay-attack revocation chain actually reached the database — the
        // rotated token's row, which was valid moments ago, must now be revoked too.
        RefreshToken newTokenRowAfterReplay = refreshTokenRepository
                .findByTokenHash(tokenHasher.sha256Hex(rotatedTokens.refreshToken()))
                .orElseThrow();
        assertThat(newTokenRowAfterReplay.isRevoked()).isTrue();
    }

    @Test
    void logoutRevokesTheRefreshToken() {
        String phone = uniquePhoneNumber();
        requestOtp(phone);
        String code = fakeOtpSender.lastCodeFor(phone);
        AuthTokensResponse tokens = restTemplate
                .postForEntity("/api/v1/auth/otp/verify", new VerifyOtpRequest(phone, code), AuthTokensResponse.class)
                .getBody();
        assertThat(tokens).isNotNull();

        restTemplate.postForEntity("/api/v1/auth/logout", new RefreshRequest(tokens.refreshToken()), Void.class);

        // Confirms the logout actually persisted a revocation to Postgres, not just that a
        // later refresh attempt happened to fail.
        RefreshToken tokenRow =
                refreshTokenRepository.findByTokenHash(tokenHasher.sha256Hex(tokens.refreshToken())).orElseThrow();
        assertThat(tokenRow.isRevoked()).isTrue();

        ResponseEntity<ApiExceptionHandler.ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/refresh", new RefreshRequest(tokens.refreshToken()), ApiExceptionHandler.ErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void meRejectsMissingOrGarbageTokens() {
        ResponseEntity<String> noTokenResponse = restTemplate.getForEntity("/api/v1/auth/me", String.class);
        assertThat(noTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("this-is-not-a-jwt");
        ResponseEntity<String> garbageTokenResponse = restTemplate.exchange(
                "/api/v1/auth/me", org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(garbageTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
