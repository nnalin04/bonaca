package com.bonaca.backend.members.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.model.MemberRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * Schema contract from V2__create_members_schema.sql: user_id is UNIQUE on members — a user can
 * only ever have one Member row (their own profile), which is exactly what
 * MembersService.completeProfile relies on to detect "profile already complete."
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    private static Member member(UUID accountId, UUID userId, MemberRole role) {
        return new Member(accountId, userId, role, "Name", null, null, null, null);
    }

    @Test
    void findByUserIdReturnsTheMatchingMember() {
        UUID userId = UUID.randomUUID();
        memberRepository.saveAndFlush(member(UUID.randomUUID(), userId, MemberRole.PRIMARY));

        assertThat(memberRepository.findByUserId(userId)).isPresent();
    }

    @Test
    void findByAccountIdReturnsAllMembersOfThatAccount() {
        UUID accountId = UUID.randomUUID();
        memberRepository.saveAndFlush(member(accountId, UUID.randomUUID(), MemberRole.PRIMARY));
        memberRepository.saveAndFlush(member(accountId, UUID.randomUUID(), MemberRole.SECONDARY));
        memberRepository.saveAndFlush(member(UUID.randomUUID(), UUID.randomUUID(), MemberRole.PRIMARY));

        assertThat(memberRepository.findByAccountId(accountId)).hasSize(2);
    }

    @Test
    void findByIdInReturnsOnlyTheRequestedMembers() {
        Member wanted = memberRepository.saveAndFlush(member(UUID.randomUUID(), UUID.randomUUID(), MemberRole.PRIMARY));
        memberRepository.saveAndFlush(member(UUID.randomUUID(), UUID.randomUUID(), MemberRole.PRIMARY));

        assertThat(memberRepository.findByIdIn(java.util.List.of(wanted.getId())))
                .extracting(Member::getId)
                .containsExactly(wanted.getId());
    }

    @Test
    void userIdMustBeUnique() {
        UUID userId = UUID.randomUUID();
        memberRepository.saveAndFlush(member(UUID.randomUUID(), userId, MemberRole.PRIMARY));

        assertThatThrownBy(() -> memberRepository.saveAndFlush(member(UUID.randomUUID(), userId, MemberRole.SECONDARY)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
