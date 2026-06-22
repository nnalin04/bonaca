package com.bonaca.backend.subscriptions.integration;

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
import com.bonaca.backend.subscriptions.dto.SubscriptionResponse;
import com.bonaca.backend.subscriptions.model.Subscription;
import com.bonaca.backend.subscriptions.model.SubscriptionStatus;
import com.bonaca.backend.subscriptions.repository.SubscriptionRepository;
import com.bonaca.backend.subscriptions.service.SubscriptionLifecycleScheduler;
import com.bonaca.backend.subscriptions.service.SubscriptionService;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * End-to-end pass per docs/TECHNICAL/SUBSCRIPTIONS_IMPLEMENTATION_PLAN.md: a real Primary signed
 * up and onboarded through the actual HTTP auth/members flow, real Postgres via Testcontainers,
 * trial creation triggered by the real MembersService -> SubscriptionService call (not seeded
 * directly), lifecycle transitions driven through SubscriptionService's internal entry points
 * (no write endpoint exists yet — see the plan doc §1), and the nightly lapsed-trial job exercised
 * through the real SubscriptionLifecycleScheduler bean.
 *
 * <p>Each step is checked two ways: directly against the repository (does Postgres actually hold
 * the row, in the state the service claims to have written) and via the real GET REST endpoint
 * (does the API correctly expose what's really stored) — asserting only through API responses
 * would miss a bug where the persisted data is wrong but the response mapping still looks right.
 */
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, OtpTestConfig.class})
class SubscriptionsFlowIntegrationTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FakeOtpSender fakeOtpSender;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionService subscriptionService;

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

    /** Invites and completes onboarding for a new Secondary Member attached to primaryToken's account. */
    private MemberWithToken addSecondary(String primaryToken, String name) {
        String phone = uniquePhoneNumber();
        ResponseEntity<InviteResponse> inviteResponse =
                authedPost(primaryToken, "/api/v1/invites", new CreateInviteRequest(phone), InviteResponse.class);
        assertThat(inviteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String secondaryToken = signUp(phone);
        return new MemberWithToken(completeProfile(secondaryToken, name), secondaryToken);
    }

    private record MemberWithToken(MemberResponse member, String accessToken) {
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

    @Test
    void completingProfileStartsATrialSubscriptionVisibleViaTheStatusEndpoint() {
        String accessToken = signUp(uniquePhoneNumber());
        MemberResponse primary = completeProfile(accessToken, "Asha Kumar");
        assertThat(primary).isNotNull();

        // Confirms the trial row was actually persisted to Postgres by the real
        // MembersService -> SubscriptionService.startTrial call, not inferred from a response.
        Subscription persisted = subscriptionRepository.findByAccountId(primary.accountId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(SubscriptionStatus.TRIAL);
        assertThat(persisted.getTrialEndsAt()).isAfter(Instant.now());

        // GET /accounts/{id}/subscription -> controller -> service -> repository -> Postgres ->
        // back. Cross-checked against the same row just fetched above, not re-derived.
        ResponseEntity<SubscriptionResponse> response = authedGet(
                accessToken, "/api/v1/accounts/" + primary.accountId() + "/subscription", SubscriptionResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(persisted.getId());
        assertThat(response.getBody().status()).isEqualTo("trial");
        assertThat(response.getBody().trialEndsAt()).isEqualTo(persisted.getTrialEndsAt());
    }

    @Test
    void secondaryMemberOfTheSameAccountCanViewTheSharedSubscriptionButAnOutsiderCannot() {
        String primaryToken = signUp(uniquePhoneNumber());
        MemberResponse primary = completeProfile(primaryToken, "Asha Kumar");
        MemberWithToken secondary = addSecondary(primaryToken, "Rakesh Kumar");

        // A subscription is account-level (docs/TECHNICAL/SUBSCRIPTIONS_IMPLEMENTATION_PLAN.md
        // §3): both members of the same account share the one persisted row.
        ResponseEntity<SubscriptionResponse> secondaryView = authedGet(
                secondary.accessToken(),
                "/api/v1/accounts/" + primary.accountId() + "/subscription",
                SubscriptionResponse.class);
        assertThat(secondaryView.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondaryView.getBody()).isNotNull();
        assertThat(secondaryView.getBody().accountId()).isEqualTo(primary.accountId());
        assertThat(secondaryView.getBody().status()).isEqualTo("trial");

        // Confirms only one subscription row exists for the account — the Secondary's view is
        // the same persisted row as the Primary's, not a separately-created one.
        Subscription persisted = subscriptionRepository.findByAccountId(primary.accountId()).orElseThrow();
        assertThat(secondaryView.getBody().id()).isEqualTo(persisted.getId());
    }

    @Test
    void aMemberOfADifferentAccountIsForbiddenFromViewingAnotherAccountsSubscription() {
        String primaryToken = signUp(uniquePhoneNumber());
        MemberResponse primary = completeProfile(primaryToken, "Asha Kumar");
        assertThat(primary).isNotNull();

        String otherToken = signUp(uniquePhoneNumber());
        MemberResponse other = completeProfile(otherToken, "Stranger");
        assertThat(other).isNotNull();

        ResponseEntity<ApiExceptionHandler.ErrorResponse> response = authedGet(
                otherToken, "/api/v1/accounts/" + primary.accountId() + "/subscription", ApiExceptionHandler.ErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void lifecycleTransitionsThroughSubscriptionServiceArePersistedAndReflectedInTheStatusEndpoint() {
        String accessToken = signUp(uniquePhoneNumber());
        MemberResponse primary = completeProfile(accessToken, "Asha Kumar");

        // No payment-processor integration exists yet (CLAUDE.md blocker) — these calls simulate
        // what a future real integration would trigger by calling the same internal entry points.
        subscriptionService.activate(primary.accountId(), Instant.now());
        Subscription afterActivate = subscriptionRepository.findByAccountId(primary.accountId()).orElseThrow();
        assertThat(afterActivate.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        ResponseEntity<SubscriptionResponse> activeResponse = authedGet(
                accessToken, "/api/v1/accounts/" + primary.accountId() + "/subscription", SubscriptionResponse.class);
        assertThat(activeResponse.getBody().status()).isEqualTo("active");
        assertThat(activeResponse.getBody().renewedAt()).isEqualTo(afterActivate.getRenewedAt());

        subscriptionService.markExpiring(primary.accountId());
        Subscription afterExpiring = subscriptionRepository.findByAccountId(primary.accountId()).orElseThrow();
        assertThat(afterExpiring.getStatus()).isEqualTo(SubscriptionStatus.EXPIRING);
        ResponseEntity<SubscriptionResponse> expiringResponse = authedGet(
                accessToken, "/api/v1/accounts/" + primary.accountId() + "/subscription", SubscriptionResponse.class);
        assertThat(expiringResponse.getBody().status()).isEqualTo("expiring");

        subscriptionService.cancel(primary.accountId());
        Subscription afterCancel = subscriptionRepository.findByAccountId(primary.accountId()).orElseThrow();
        assertThat(afterCancel.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        ResponseEntity<SubscriptionResponse> cancelledResponse = authedGet(
                accessToken, "/api/v1/accounts/" + primary.accountId() + "/subscription", SubscriptionResponse.class);
        assertThat(cancelledResponse.getBody().status()).isEqualTo("cancelled");
    }

    @Test
    void theNightlySchedulerExpiresALapsedTrialAndLeavesAnUnlapsedTrialUntouched() {
        String lapsedToken = signUp(uniquePhoneNumber());
        MemberResponse lapsedPrimary = completeProfile(lapsedToken, "Lapsed Account");
        String freshToken = signUp(uniquePhoneNumber());
        MemberResponse freshPrimary = completeProfile(freshToken, "Fresh Account");

        // Backdates the lapsed account's trial directly in Postgres (replacing the freshly
        // started TRIAL row) to simulate seven days having passed, without a setter that would
        // let production code do this — only the test reaches around the entity API this way.
        Subscription lapsed = subscriptionRepository.findByAccountId(lapsedPrimary.accountId()).orElseThrow();
        subscriptionRepository.delete(lapsed);
        subscriptionRepository.save(new Subscription(
                lapsedPrimary.accountId(), SubscriptionStatus.TRIAL, Instant.now().minus(1, ChronoUnit.DAYS)));

        subscriptionLifecycleScheduler.expireLapsedTrials();

        // Direct DB assertion: the lapsed account is EXPIRED, the fresh one is untouched.
        Subscription lapsedAfter = subscriptionRepository.findByAccountId(lapsedPrimary.accountId()).orElseThrow();
        assertThat(lapsedAfter.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        Subscription freshAfter = subscriptionRepository.findByAccountId(freshPrimary.accountId()).orElseThrow();
        assertThat(freshAfter.getStatus()).isEqualTo(SubscriptionStatus.TRIAL);

        // Closes the loop on the read side through the real GET endpoint.
        ResponseEntity<SubscriptionResponse> lapsedResponse = authedGet(
                lapsedToken, "/api/v1/accounts/" + lapsedPrimary.accountId() + "/subscription", SubscriptionResponse.class);
        assertThat(lapsedResponse.getBody().status()).isEqualTo("expired");
        ResponseEntity<SubscriptionResponse> freshResponse = authedGet(
                freshToken, "/api/v1/accounts/" + freshPrimary.accountId() + "/subscription", SubscriptionResponse.class);
        assertThat(freshResponse.getBody().status()).isEqualTo("trial");
    }
}
