package com.bonaca.backend.metrics.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.bonaca.backend.TestcontainersConfiguration;
import com.bonaca.backend.auth.dto.AuthTokensResponse;
import com.bonaca.backend.auth.dto.RequestOtpRequest;
import com.bonaca.backend.auth.dto.VerifyOtpRequest;
import com.bonaca.backend.auth.integration.FakeOtpSender;
import com.bonaca.backend.auth.integration.OtpTestConfig;
import com.bonaca.backend.common.ApiExceptionHandler;
import com.bonaca.backend.members.dto.CompleteProfileRequest;
import com.bonaca.backend.members.dto.CreateInviteRequest;
import com.bonaca.backend.members.dto.InviteResponse;
import com.bonaca.backend.members.dto.MemberResponse;
import com.bonaca.backend.members.dto.SharingGrantResponse;
import com.bonaca.backend.members.dto.UpdateSharingGrantRequest;
import com.bonaca.backend.metrics.dto.MetricDetailResponse;
import com.bonaca.backend.metrics.dto.MetricSummaryResponse;
import com.bonaca.backend.metrics.model.MetricBaseline;
import com.bonaca.backend.metrics.model.MetricReading;
import com.bonaca.backend.metrics.model.MetricType;
import com.bonaca.backend.metrics.repository.InsightRepository;
import com.bonaca.backend.metrics.repository.MetricBaselineRepository;
import com.bonaca.backend.metrics.repository.MetricReadingRepository;
import com.bonaca.backend.metrics.service.BaselineService;
import com.bonaca.backend.metrics.service.InsightGenerationService;
import com.bonaca.backend.metrics.service.MetricIngestionService;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * End-to-end pass per docs/TECHNICAL/METRICS_IMPLEMENTATION_PLAN.md §9: a real Primary +
 * Secondary signed up and onboarded through the actual HTTP auth/members flow, real Postgres via
 * Testcontainers, readings seeded through MetricIngestionService (no write endpoint exists —
 * §3.1), the nightly rollup simulated by calling BaselineService/InsightGenerationService
 * directly, then the real REST read endpoints hit as both members.
 *
 * <p>Each write step is checked two ways: directly against the repository (does the database
 * actually contain the row, with the actually-computed values — not "an endpoint reported
 * something plausible"), and then via the real REST read endpoints (does the API correctly
 * expose what's really stored). Asserting only through API responses would miss a bug where the
 * persisted data is wrong but the response-mapping code happens to still look right.
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, OtpTestConfig.class})
class MetricsFlowIntegrationTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FakeOtpSender fakeOtpSender;

    @Autowired
    private MetricIngestionService metricIngestionService;

    @Autowired
    private BaselineService baselineService;

    @Autowired
    private InsightGenerationService insightGenerationService;

    @Autowired
    private MetricReadingRepository metricReadingRepository;

    @Autowired
    private MetricBaselineRepository metricBaselineRepository;

    @Autowired
    private InsightRepository insightRepository;

    private String uniquePhoneNumber() {
        return "+91" + (6000000000L + RANDOM.nextInt(900000000));
    }

    private String signUp(String phone) {
        restTemplate.postForEntity("/api/v1/auth/otp/request", new RequestOtpRequest(phone), Void.class);
        String code = fakeOtpSender.lastCodeFor(phone);
        AuthTokensResponse tokens = restTemplate
                .postForEntity("/api/v1/auth/otp/verify", new VerifyOtpRequest(phone, code), AuthTokensResponse.class)
                .getBody();
        assertThat(tokens).isNotNull();
        return tokens.accessToken();
    }

    private MemberResponse completeProfile(String accessToken, String name) {
        return authedPost(
                        accessToken,
                        "/api/v1/members/complete-profile",
                        new CompleteProfileRequest(name, "female", null, null, null),
                        MemberResponse.class)
                .getBody();
    }

    private <T> ResponseEntity<T> authedGet(String accessToken, String path, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), responseType);
    }

    private <T> ResponseEntity<T> authedPost(String accessToken, String path, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), responseType);
    }

    private <T> ResponseEntity<T> authedPatch(String accessToken, String path, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return restTemplate.exchange(path, HttpMethod.PATCH, new HttpEntity<>(body, headers), responseType);
    }

    /** Revokes every non-Vitals grant for the given grantee, leaving them with Vitals-only access. */
    private void revokeNonVitalsGrants(String primaryToken, String accountId, String secondaryMemberId) {
        ResponseEntity<SharingGrantResponse[]> grants =
                authedGet(primaryToken, "/api/v1/sharing-grants?accountId=" + accountId, SharingGrantResponse[].class);
        for (SharingGrantResponse grant : grants.getBody()) {
            if (!"vitals".equals(grant.scope()) && grant.granteeMemberId().toString().equals(secondaryMemberId)) {
                authedPatch(
                        primaryToken,
                        "/api/v1/sharing-grants/" + grant.id(),
                        new UpdateSharingGrantRequest(false),
                        SharingGrantResponse.class);
            }
        }
    }

    @Test
    void metricsFlowFromIngestionThroughRollupToScopeGatedRead() {
        String primaryToken = signUp(uniquePhoneNumber());
        MemberResponse primary = completeProfile(primaryToken, "Asha Kumar");

        String secondaryPhone = uniquePhoneNumber();
        authedPost(primaryToken, "/api/v1/invites", new CreateInviteRequest(secondaryPhone), InviteResponse.class);
        String secondaryToken = signUp(secondaryPhone);
        MemberResponse secondary = completeProfile(secondaryToken, "Rakesh Kumar");

        revokeNonVitalsGrants(primaryToken, primary.accountId().toString(), secondary.id().toString());

        // 14 valid days of heart rate alternating 68/72 bpm (mean 70, stddev 2) establishes the
        // baseline — alternating values, not a flat one, so the baseline has a real (non-zero)
        // spread for the z-score comparison below to be meaningful. Today's reading deviates
        // sharply upward.
        Instant now = Instant.now();
        for (int i = 1; i <= 14; i++) {
            double value = i % 2 == 0 ? 68.0 : 72.0;
            metricIngestionService.recordReading(primary.id(), MetricType.HEART_RATE, value, "bpm", now.minus(i, ChronoUnit.DAYS), "device-1");
        }
        metricIngestionService.recordReading(primary.id(), MetricType.HEART_RATE, 95.0, "bpm", now, "device-1");

        // Confirms the writes actually landed in Postgres — not inferred from a later API
        // response, queried directly via the repository against the real database.
        List<MetricReading> persistedHeartRate = metricReadingRepository
                .findByMemberIdAndMetricTypeAndRecordedAtAfterOrderByRecordedAtAsc(
                        primary.id(), MetricType.HEART_RATE, now.minus(30, ChronoUnit.DAYS));
        assertThat(persistedHeartRate).hasSize(15);
        assertThat(persistedHeartRate.get(persistedHeartRate.size() - 1).getValue()).isEqualTo(95.0);

        // Steps (Activity scope, revoked for the Secondary) also has data — only used to verify
        // the Secondary is denied it, not for a cross-metric ordering comparison (that's already
        // covered with controlled, uncontaminated inputs in MetricsQueryServiceTest).
        for (int i = 1; i <= 14; i++) {
            double value = i % 2 == 0 ? 7900.0 : 8100.0;
            metricIngestionService.recordReading(primary.id(), MetricType.STEPS, value, "steps", now.minus(i, ChronoUnit.DAYS), "device-1");
        }
        metricIngestionService.recordReading(primary.id(), MetricType.STEPS, 20000.0, "steps", now, "device-1");

        // Simulates the nightly job firing.
        baselineService.recomputeAllBaselines();
        insightGenerationService.generateDailyInsightsForAllMembers();

        // Confirms the rollup actually persisted a correctly computed baseline row — not
        // "the API reported something plausible," the real stored mean/stddev.
        MetricBaseline heartRateBaseline = metricBaselineRepository
                .findByMemberIdAndMetricType(primary.id(), MetricType.HEART_RATE)
                .orElseThrow(() -> new AssertionError("Expected a heart_rate baseline row to have been persisted"));
        assertThat(heartRateBaseline.getValidDayCount()).isGreaterThanOrEqualTo(14);
        assertThat(heartRateBaseline.getBaselineMean()).isBetween(65.0, 75.0);
        assertThat(heartRateBaseline.getBaselineStddev()).isGreaterThan(0.0);

        // Confirms the insight job actually wrote a row to the insights table for today, not
        // just that some downstream endpoint happens to render trend text.
        boolean heartRateInsightPersisted = insightRepository
                .findByMemberIdAndMetricTypeAndInsightDate(primary.id(), MetricType.HEART_RATE, LocalDate.now())
                .map(insight -> insight.getGeneratedText().contains("higher"))
                .orElse(false);
        assertThat(heartRateInsightPersisted).isTrue();

        // The Primary (self) sees both metrics — self-view always bypasses scope gating.
        ResponseEntity<MetricSummaryResponse[]> primarySummary = authedGet(
                primaryToken, "/api/v1/members/" + primary.id() + "/metrics?range=7d", MetricSummaryResponse[].class);
        assertThat(primarySummary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(primarySummary.getBody())
                .extracting(MetricSummaryResponse::metricType)
                .containsExactlyInAnyOrder("steps", "heart_rate");

        // The Secondary (Vitals-only) sees only heart_rate in the summary, not steps.
        ResponseEntity<MetricSummaryResponse[]> secondarySummary = authedGet(
                secondaryToken, "/api/v1/members/" + primary.id() + "/metrics?range=7d", MetricSummaryResponse[].class);
        assertThat(secondarySummary.getBody()).extracting(MetricSummaryResponse::metricType).containsExactly("heart_rate");

        // The Secondary can view the Vitals metric detail, with a correctly computed trend label.
        ResponseEntity<MetricDetailResponse> heartRateDetail = authedGet(
                secondaryToken, "/api/v1/members/" + primary.id() + "/metrics/heart_rate?range=24h", MetricDetailResponse.class);
        assertThat(heartRateDetail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(heartRateDetail.getBody().hasData()).isTrue();
        assertThat(heartRateDetail.getBody().trendLabel()).isEqualTo("higher_than_usual");

        // The Secondary is forbidden from the Activity-scoped metric they don't have a grant for.
        ResponseEntity<ApiExceptionHandler.ErrorResponse> stepsDenied = authedGet(
                secondaryToken, "/api/v1/members/" + primary.id() + "/metrics/steps?range=7d", ApiExceptionHandler.ErrorResponse.class);
        assertThat(stepsDenied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // A Vitals metric with zero readings returns the explicit "no data" shape, not a fabricated value.
        ResponseEntity<MetricDetailResponse> sleepDetail = authedGet(
                secondaryToken, "/api/v1/members/" + primary.id() + "/metrics/sleep?range=7d", MetricDetailResponse.class);
        assertThat(sleepDetail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sleepDetail.getBody().hasData()).isFalse();
        assertThat(sleepDetail.getBody().average()).isNull();
    }
}
