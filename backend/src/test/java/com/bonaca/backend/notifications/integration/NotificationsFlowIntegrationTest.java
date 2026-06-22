package com.bonaca.backend.notifications.integration;

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
import com.bonaca.backend.metrics.model.MetricType;
import com.bonaca.backend.metrics.service.BaselineService;
import com.bonaca.backend.metrics.service.InsightGenerationService;
import com.bonaca.backend.metrics.service.MetricIngestionService;
import com.bonaca.backend.notifications.dto.NotificationResponse;
import com.bonaca.backend.notifications.model.Notification;
import com.bonaca.backend.notifications.repository.NotificationRepository;
import com.bonaca.backend.notifications.service.NotificationGenerationService;
import com.bonaca.backend.subscriptions.model.Subscription;
import com.bonaca.backend.subscriptions.model.SubscriptionStatus;
import com.bonaca.backend.subscriptions.repository.SubscriptionRepository;
import com.bonaca.backend.subscriptions.service.SubscriptionLifecycleScheduler;
import java.security.SecureRandom;
import java.time.Instant;
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
 * End-to-end pass per docs/TECHNICAL/NOTIFICATIONS_IMPLEMENTATION_PLAN.md §8: a real Primary +
 * Secondary onboarded through the actual HTTP auth/members flow, real Postgres via Testcontainers,
 * the metric-anomaly trigger exercised by seeding readings through MetricIngestionService and
 * running the real metrics rollup + notification scan, the lapsed-subscription trigger exercised
 * through SubscriptionLifecycleScheduler + the real notification scan, and payment-request through
 * the real POST endpoint. Each step is checked both directly against the repository and via the
 * real GET endpoint, not just one or the other.
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, OtpTestConfig.class})
class NotificationsFlowIntegrationTest {

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
    private NotificationGenerationService notificationGenerationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionLifecycleScheduler subscriptionLifecycleScheduler;

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
    void metricAnomalyNotifiesAnInScopeSecondaryButNotTheSubjectThemself() {
        String primaryToken = signUp(uniquePhoneNumber());
        MemberResponse primary = completeProfile(primaryToken, "Asha Kumar");

        String secondaryPhone = uniquePhoneNumber();
        authedPost(primaryToken, "/api/v1/invites", new CreateInviteRequest(secondaryPhone), InviteResponse.class);
        String secondaryToken = signUp(secondaryPhone);
        MemberResponse secondary = completeProfile(secondaryToken, "Rakesh Kumar");
        revokeNonVitalsGrants(primaryToken, primary.accountId().toString(), secondary.id().toString());

        // 14 valid days at mean 70/stddev 2, then a sharply deviated reading today -> z way past
        // ANOMALY_THRESHOLD (2.0), so the nightly rollup classifies it ANOMALY, not just TREND.
        Instant now = Instant.now();
        for (int i = 1; i <= 14; i++) {
            double value = i % 2 == 0 ? 68.0 : 72.0;
            metricIngestionService.recordReading(primary.id(), MetricType.HEART_RATE, value, "bpm", now.minus(i, ChronoUnit.DAYS), "device-1");
        }
        metricIngestionService.recordReading(primary.id(), MetricType.HEART_RATE, 95.0, "bpm", now, "device-1");

        baselineService.recomputeAllBaselines();
        insightGenerationService.generateDailyInsightsForAllMembers();
        notificationGenerationService.generateMetricAnomalyNotifications();

        // Direct DB assertion: exactly one notification exists, addressed to the Secondary, about
        // the Primary, not a notification the Primary received about themself.
        List<Notification> secondaryNotifications = notificationRepository.findByRecipientMemberIdOrderByCreatedAtDesc(secondary.id());
        assertThat(secondaryNotifications).hasSize(1);
        assertThat(secondaryNotifications.get(0).getSubjectMemberId()).isEqualTo(primary.id());
        List<Notification> primaryNotifications = notificationRepository.findByRecipientMemberIdOrderByCreatedAtDesc(primary.id());
        assertThat(primaryNotifications).isEmpty();

        // Read-side cross-check through the real GET endpoint.
        ResponseEntity<NotificationResponse[]> secondaryGet =
                authedGet(secondaryToken, "/api/v1/members/" + secondary.id() + "/notifications", NotificationResponse[].class);
        assertThat(secondaryGet.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondaryGet.getBody()).hasSize(1);
        assertThat(secondaryGet.getBody()[0].type()).isEqualTo("metric-anomaly");
        assertThat(secondaryGet.getBody()[0].memberId()).isEqualTo(primary.id());
        assertThat(secondaryGet.getBody()[0].id()).isEqualTo(secondaryNotifications.get(0).getId());

        // A member can't read another member's notification list.
        ResponseEntity<ApiExceptionHandler.ErrorResponse> forbidden = authedGet(
                secondaryToken, "/api/v1/members/" + primary.id() + "/notifications", ApiExceptionHandler.ErrorResponse.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void paymentRequestIsCreatedThroughTheRealEndpointAndCanBeMarkedRead() {
        String primaryToken = signUp(uniquePhoneNumber());
        MemberResponse primary = completeProfile(primaryToken, "Asha Kumar");
        String secondaryPhone = uniquePhoneNumber();
        authedPost(primaryToken, "/api/v1/invites", new CreateInviteRequest(secondaryPhone), InviteResponse.class);
        String secondaryToken = signUp(secondaryPhone);
        MemberResponse secondary = completeProfile(secondaryToken, "Rakesh Kumar");

        ResponseEntity<NotificationResponse> created = authedPost(
                secondaryToken, "/api/v1/members/" + primary.id() + "/notifications/payment-request", null, NotificationResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().type()).isEqualTo("payment-request");
        assertThat(created.getBody().body()).contains("Rakesh Kumar");
        assertThat(created.getBody().read()).isFalse();

        // Direct DB assertion: persisted, addressed to the Primary, unread.
        Notification persisted = notificationRepository.findById(created.getBody().id()).orElseThrow();
        assertThat(persisted.getRecipientMemberId()).isEqualTo(primary.id());
        assertThat(persisted.isRead()).isFalse();

        ResponseEntity<NotificationResponse> markedRead = authedPatch(
                primaryToken, "/api/v1/notifications/" + created.getBody().id() + "/read", null, NotificationResponse.class);
        assertThat(markedRead.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(markedRead.getBody().read()).isTrue();

        // Closes the loop: re-fetching the persisted row directly shows the flag really flipped.
        assertThat(notificationRepository.findById(created.getBody().id()).orElseThrow().isRead()).isTrue();

        // A non-recipient can't request payment from themself.
        ResponseEntity<ApiExceptionHandler.ErrorResponse> selfRequest = authedPost(
                primaryToken,
                "/api/v1/members/" + primary.id() + "/notifications/payment-request",
                null,
                ApiExceptionHandler.ErrorResponse.class);
        assertThat(selfRequest.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void lapsedSubscriptionNotifiesEveryAccountMemberOnceTheNightlyScansRun() {
        String primaryToken = signUp(uniquePhoneNumber());
        MemberResponse primary = completeProfile(primaryToken, "Asha Kumar");
        String secondaryPhone = uniquePhoneNumber();
        authedPost(primaryToken, "/api/v1/invites", new CreateInviteRequest(secondaryPhone), InviteResponse.class);
        String secondaryToken = signUp(secondaryPhone);
        MemberResponse secondary = completeProfile(secondaryToken, "Rakesh Kumar");

        // Backdates the trial directly in Postgres to simulate seven days having passed — same
        // technique SubscriptionsFlowIntegrationTest uses, since there's no setter for trialEndsAt.
        Subscription trial = subscriptionRepository.findByAccountId(primary.accountId()).orElseThrow();
        subscriptionRepository.delete(trial);
        subscriptionRepository.save(
                new Subscription(primary.accountId(), SubscriptionStatus.TRIAL, Instant.now().minus(1, ChronoUnit.DAYS)));

        subscriptionLifecycleScheduler.expireLapsedTrials();
        notificationGenerationService.generateLapsedSubscriptionNotifications();

        // Direct DB assertion: both account members got a self-concerning SUBSCRIPTION notification.
        List<Notification> primaryNotifications = notificationRepository.findByRecipientMemberIdOrderByCreatedAtDesc(primary.id());
        assertThat(primaryNotifications).hasSize(1);
        assertThat(primaryNotifications.get(0).getSubjectMemberId()).isEqualTo(primary.id());
        List<Notification> secondaryNotifications = notificationRepository.findByRecipientMemberIdOrderByCreatedAtDesc(secondary.id());
        assertThat(secondaryNotifications).hasSize(1);
        assertThat(secondaryNotifications.get(0).getSubjectMemberId()).isEqualTo(secondary.id());

        // Read-side cross-check through the real GET endpoint for both members.
        ResponseEntity<NotificationResponse[]> primaryGet =
                authedGet(primaryToken, "/api/v1/members/" + primary.id() + "/notifications", NotificationResponse[].class);
        assertThat(primaryGet.getBody()).hasSize(1);
        assertThat(primaryGet.getBody()[0].type()).isEqualTo("subscription");
        assertThat(primaryGet.getBody()[0].id()).isEqualTo(primaryNotifications.get(0).getId());

        ResponseEntity<NotificationResponse[]> secondaryGet =
                authedGet(secondaryToken, "/api/v1/members/" + secondary.id() + "/notifications", NotificationResponse[].class);
        assertThat(secondaryGet.getBody()).hasSize(1);
        assertThat(secondaryGet.getBody()[0].type()).isEqualTo("subscription");

        // Re-running the scan doesn't duplicate (idempotent per account, see plan doc §3.2).
        notificationGenerationService.generateLapsedSubscriptionNotifications();
        assertThat(notificationRepository.findByRecipientMemberIdOrderByCreatedAtDesc(primary.id())).hasSize(1);
    }
}
