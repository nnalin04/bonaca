package com.bonaca.backend.members.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.bonaca.backend.TestcontainersConfiguration;
import com.bonaca.backend.auth.integration.FakeOtpSender;
import com.bonaca.backend.auth.integration.OtpTestConfig;
import com.bonaca.backend.auth.dto.AuthTokensResponse;
import com.bonaca.backend.auth.dto.RequestOtpRequest;
import com.bonaca.backend.auth.dto.VerifyOtpRequest;
import com.bonaca.backend.common.ApiExceptionHandler;
import com.bonaca.backend.members.dto.CompleteProfileRequest;
import com.bonaca.backend.members.dto.CreateInviteRequest;
import com.bonaca.backend.members.dto.InviteResponse;
import com.bonaca.backend.members.dto.MemberResponse;
import com.bonaca.backend.members.dto.SharingGrantResponse;
import com.bonaca.backend.members.dto.UpdateMemberRequest;
import com.bonaca.backend.members.model.Account;
import com.bonaca.backend.members.model.Invite;
import com.bonaca.backend.members.model.InviteStatus;
import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.model.MemberRole;
import com.bonaca.backend.members.model.SharingGrant;
import com.bonaca.backend.members.repository.AccountRepository;
import com.bonaca.backend.members.repository.InviteRepository;
import com.bonaca.backend.members.repository.MemberRepository;
import com.bonaca.backend.members.repository.SharingGrantRepository;
import com.bonaca.backend.subscriptions.model.Subscription;
import com.bonaca.backend.subscriptions.model.SubscriptionStatus;
import com.bonaca.backend.subscriptions.repository.SubscriptionRepository;
import java.security.SecureRandom;
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

@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, OtpTestConfig.class})
class MembersFlowIntegrationTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FakeOtpSender fakeOtpSender;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private InviteRepository inviteRepository;

    @Autowired
    private SharingGrantRepository sharingGrantRepository;

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

    private ResponseEntity<InviteResponse> invite(String accessToken, String phoneNumber) {
        return authedPost(accessToken, "/api/v1/invites", new CreateInviteRequest(phoneNumber), InviteResponse.class);
    }

    /** Invites and completes onboarding for a new Secondary Member attached to primaryToken's account. */
    private MemberResponse addSecondary(String primaryToken, String name) {
        String phone = uniquePhoneNumber();
        ResponseEntity<InviteResponse> inviteResponse = invite(primaryToken, phone);
        assertThat(inviteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String secondaryToken = signUp(phone);
        return completeProfile(secondaryToken, name);
    }

    private <T> ResponseEntity<T> authedPost(String accessToken, String path, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), responseType);
    }

    private <T> ResponseEntity<T> authedGet(String accessToken, String path, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), responseType);
    }

    private <T> ResponseEntity<T> authedPatch(String accessToken, String path, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return restTemplate.exchange(path, HttpMethod.PATCH, new HttpEntity<>(body, headers), responseType);
    }

    @Test
    void completingProfileWithNoPendingInviteCreatesANewAccountAndPrimaryMember() {
        String accessToken = signUp(uniquePhoneNumber());

        MemberResponse member = completeProfile(accessToken, "Asha Kumar");

        assertThat(member).isNotNull();
        assertThat(member.role()).isEqualTo("primary");
        assertThat(member.name()).isEqualTo("Asha Kumar");
        assertThat(member.accountId()).isNotNull();

        // Confirms the account, member, and trial subscription rows were actually persisted to
        // Postgres — not inferred from the response, queried directly via the repositories.
        Member persistedMember = memberRepository.findById(member.id()).orElseThrow();
        assertThat(persistedMember.getRole()).isEqualTo(MemberRole.PRIMARY);
        assertThat(persistedMember.getAccountId()).isEqualTo(member.accountId());

        Account persistedAccount = accountRepository.findById(member.accountId()).orElseThrow();
        assertThat(persistedAccount.getOwnerMemberId()).isEqualTo(member.id());

        List<Subscription> persistedSubscriptions = subscriptionRepository.findAll().stream()
                .filter(s -> s.getAccountId().equals(member.accountId()))
                .toList();
        assertThat(persistedSubscriptions).hasSize(1);
        assertThat(persistedSubscriptions.get(0).getStatus()).isEqualTo(SubscriptionStatus.TRIAL);
        assertThat(persistedSubscriptions.get(0).getTrialEndsAt()).isAfter(java.time.Instant.now());

        // Closes the loop on the read side: GET /members/{id} -> controller -> service ->
        // repository -> Postgres -> back. Compared directly against the same row just fetched
        // above, not independently re-checked against the test's literal "Asha Kumar" string.
        ResponseEntity<MemberResponse> getResponse =
                authedGet(accessToken, "/api/v1/members/" + member.id(), MemberResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().name()).isEqualTo(persistedMember.getName());
        assertThat(getResponse.getBody().role()).isEqualTo(persistedMember.getRole().name().toLowerCase());
    }

    @Test
    void invitedSecondaryMemberIsAttachedToInviterAccountWithDefaultGrantsFromThePrimary() {
        String primaryToken = signUp(uniquePhoneNumber());
        MemberResponse primary = completeProfile(primaryToken, "Asha Kumar");
        assertThat(primary).isNotNull();

        MemberResponse secondary = addSecondary(primaryToken, "Rakesh Kumar");

        assertThat(secondary).isNotNull();
        assertThat(secondary.role()).isEqualTo("secondary");
        assertThat(secondary.accountId()).isEqualTo(primary.accountId());

        // GET /members -> controller -> service -> repository -> Postgres -> back. Cross-checked
        // directly against the rows fetched straight from the repository, not just against the
        // local primary/secondary response objects from the earlier write calls.
        List<Member> persistedMembers = memberRepository.findByAccountId(primary.accountId());
        assertThat(persistedMembers).hasSize(2);

        ResponseEntity<MemberResponse[]> listResponse = authedGet(primaryToken, "/api/v1/members", MemberResponse[].class);
        assertThat(listResponse.getBody())
                .extracting(MemberResponse::id)
                .containsExactlyInAnyOrderElementsOf(persistedMembers.stream().map(Member::getId).toList());

        // all access is enabled by default (docs/PRD.pdf §11.1): all 3 scopes, granter = Primary (data owner), grantee = Secondary
        ResponseEntity<SharingGrantResponse[]> grantsResponse = authedGet(
                primaryToken, "/api/v1/sharing-grants?accountId=" + primary.accountId(), SharingGrantResponse[].class);
        List<SharingGrantResponse> grants = List.of(grantsResponse.getBody());
        assertThat(grants).hasSize(3);
        assertThat(grants)
                .allMatch(g -> g.granterMemberId().equals(primary.id()) && g.granteeMemberId().equals(secondary.id()) && g.visible());

        // Confirms the invite was actually marked ACCEPTED in Postgres (not just that a Member
        // row happens to exist), and that the 3 SharingGrant rows are really there with the
        // correct granter/grantee/visible values — not inferred from the read endpoint's response.
        List<Invite> persistedInvites = inviteRepository.findByInviterAccountId(primary.accountId());
        assertThat(persistedInvites).hasSize(1);
        assertThat(persistedInvites.get(0).getStatus()).isEqualTo(InviteStatus.ACCEPTED);
        assertThat(persistedInvites.get(0).getAcceptedMemberId()).isEqualTo(secondary.id());

        List<SharingGrant> persistedGrants = sharingGrantRepository.findByGranteeMemberIdAndVisibleTrue(secondary.id());
        assertThat(persistedGrants).hasSize(3);
        assertThat(persistedGrants).allMatch(g -> g.getGranterMemberId().equals(primary.id()) && g.isVisible());
    }

    @Test
    void secondaryMemberWithoutAccountOwnershipCannotManageMembersOrPermissions() {
        String primaryToken = signUp(uniquePhoneNumber());
        MemberResponse primary = completeProfile(primaryToken, "Asha Kumar");
        assertThat(primary).isNotNull();

        String secondaryPhone = uniquePhoneNumber();
        invite(primaryToken, secondaryPhone);
        String secondaryToken = signUp(secondaryPhone);
        MemberResponse secondary = completeProfile(secondaryToken, "Sibling");
        assertThat(secondary.role()).isEqualTo("secondary");

        ResponseEntity<ApiExceptionHandler.ErrorResponse> patchResponse = authedPatch(
                secondaryToken,
                "/api/v1/members/" + primary.id(),
                new UpdateMemberRequest("Bro", null, null),
                ApiExceptionHandler.ErrorResponse.class);
        assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<ApiExceptionHandler.ErrorResponse> grantsResponse = authedGet(
                secondaryToken, "/api/v1/sharing-grants?accountId=" + primary.accountId(), ApiExceptionHandler.ErrorResponse.class);
        assertThat(grantsResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void thirdInviteIsRejectedOnceAccountHasTwoSecondaryMembers() {
        String primaryToken = signUp(uniquePhoneNumber());
        MemberResponse primary = completeProfile(primaryToken, "Asha Kumar");
        assertThat(primary).isNotNull();

        addSecondary(primaryToken, "Secondary One");
        addSecondary(primaryToken, "Secondary Two");

        ResponseEntity<ApiExceptionHandler.ErrorResponse> thirdInviteResponse = authedPost(
                primaryToken,
                "/api/v1/invites",
                new CreateInviteRequest(uniquePhoneNumber()),
                ApiExceptionHandler.ErrorResponse.class);
        assertThat(thirdInviteResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Confirms the rejected 3rd invite did NOT create a row in Postgres — exactly 2 accepted
        // invites exist, not 3, and exactly 2 Secondary members are persisted for this account.
        List<Invite> persistedInvites = inviteRepository.findByInviterAccountId(primary.accountId());
        assertThat(persistedInvites).hasSize(2);
        assertThat(persistedInvites).allMatch(i -> i.getStatus() == InviteStatus.ACCEPTED);

        long persistedSecondaryCount = memberRepository.findByAccountId(primary.accountId()).stream()
                .filter(m -> m.getRole() == MemberRole.SECONDARY)
                .count();
        assertThat(persistedSecondaryCount).isEqualTo(2);
    }

    @Test
    void primaryCanPinAndNicknameAMemberAndSecondCompleteProfileCallIsRejected() {
        String primaryToken = signUp(uniquePhoneNumber());
        MemberResponse primary = completeProfile(primaryToken, "Asha Kumar");
        assertThat(primary).isNotNull();

        ResponseEntity<MemberResponse> patchResponse = authedPatch(
                primaryToken, "/api/v1/members/" + primary.id(), new UpdateMemberRequest("Me", true, null), MemberResponse.class);
        assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patchResponse.getBody()).isNotNull();
        assertThat(patchResponse.getBody().nickname()).isEqualTo("Me");
        assertThat(patchResponse.getBody().pinned()).isTrue();

        // Confirms the nickname and pinned flag were actually written to Postgres — not just
        // that the response happened to echo back what was sent.
        Member persistedMember = memberRepository.findById(primary.id()).orElseThrow();
        assertThat(persistedMember.getNickname()).isEqualTo("Me");
        assertThat(persistedMember.isPinned()).isTrue();

        ResponseEntity<ApiExceptionHandler.ErrorResponse> secondCompleteResponse = authedPost(
                primaryToken,
                "/api/v1/members/complete-profile",
                new CompleteProfileRequest("Asha Again", "female", null, null, null),
                ApiExceptionHandler.ErrorResponse.class);
        assertThat(secondCompleteResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
