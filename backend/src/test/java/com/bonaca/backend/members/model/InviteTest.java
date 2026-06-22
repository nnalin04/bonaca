package com.bonaca.backend.members.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class InviteTest {

    @Test
    void newInviteStartsPendingWithACreationTimestamp() {
        Invite invite = new Invite(UUID.randomUUID(), "+919876543210", MemberRole.SECONDARY);

        assertThat(invite.getStatus()).isEqualTo(InviteStatus.PENDING);
        assertThat(invite.getCreatedAt()).isNotNull();
        assertThat(invite.getAcceptedMemberId()).isNull();
    }
}
