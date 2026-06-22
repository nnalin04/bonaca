package com.bonaca.backend.members.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    void accessorsReflectConstructorArguments() {
        UUID userId = UUID.randomUUID();
        Member member = new Member(UUID.randomUUID(), userId, MemberRole.SECONDARY, "Name", "female", null, 160, 55);

        assertThat(member.getUserId()).isEqualTo(userId);
    }

    @Test
    void setHiddenTogglesTheHiddenFlag() {
        Member member = new Member(UUID.randomUUID(), UUID.randomUUID(), MemberRole.PRIMARY, "Name", null, null, null, null);

        assertThat(member.isHidden()).isFalse();

        member.setHidden(true);

        assertThat(member.isHidden()).isTrue();
    }
}
