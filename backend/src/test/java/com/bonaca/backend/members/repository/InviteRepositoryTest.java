package com.bonaca.backend.members.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bonaca.backend.members.model.Invite;
import com.bonaca.backend.members.model.InviteStatus;
import com.bonaca.backend.members.model.MemberRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

/**
 * Contract InviteService relies on: findFirstBy...OrderByCreatedAtAsc must return only a
 * PENDING invite (an accepted/expired one for the same phone number must not be reused or
 * double-counted), and it must be the oldest pending one when more than one exists.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class InviteRepositoryTest {

    private static final String PHONE = "+919876543210";

    @Autowired
    private InviteRepository inviteRepository;

    @Test
    void findFirstPendingIgnoresAcceptedInvitesForTheSamePhoneNumber() {
        Invite accepted = new Invite(UUID.randomUUID(), PHONE, MemberRole.SECONDARY);
        accepted.markAccepted(UUID.randomUUID());
        inviteRepository.saveAndFlush(accepted);

        assertThat(inviteRepository.findFirstByPhoneNumberAndStatusOrderByCreatedAtAsc(PHONE, InviteStatus.PENDING))
                .isEmpty();
    }

    @Test
    void findFirstPendingReturnsTheOldestPendingInvite() throws InterruptedException {
        Invite older = inviteRepository.saveAndFlush(new Invite(UUID.randomUUID(), PHONE, MemberRole.SECONDARY));
        Thread.sleep(5);
        inviteRepository.saveAndFlush(new Invite(UUID.randomUUID(), PHONE, MemberRole.SECONDARY));

        var found = inviteRepository.findFirstByPhoneNumberAndStatusOrderByCreatedAtAsc(PHONE, InviteStatus.PENDING);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(older.getId());
    }

    @Test
    void findByInviterAccountIdReturnsOnlyThatAccountsInvites() {
        UUID accountId = UUID.randomUUID();
        inviteRepository.saveAndFlush(new Invite(accountId, PHONE, MemberRole.SECONDARY));
        inviteRepository.saveAndFlush(new Invite(UUID.randomUUID(), "+919999999999", MemberRole.SECONDARY));

        assertThat(inviteRepository.findByInviterAccountId(accountId)).hasSize(1);
    }
}
