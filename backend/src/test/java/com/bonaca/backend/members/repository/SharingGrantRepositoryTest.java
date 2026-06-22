package com.bonaca.backend.members.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bonaca.backend.members.model.SharingGrant;
import com.bonaca.backend.members.model.SharingScope;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SharingGrantRepositoryTest {

    @Autowired
    private SharingGrantRepository sharingGrantRepository;

    @Test
    void findByGranteeIgnoresInvisibleGrants() {
        UUID grantee = UUID.randomUUID();
        sharingGrantRepository.saveAndFlush(new SharingGrant(UUID.randomUUID(), grantee, SharingScope.VITALS, true));
        sharingGrantRepository.saveAndFlush(new SharingGrant(UUID.randomUUID(), grantee, SharingScope.ACTIVITY, false));

        assertThat(sharingGrantRepository.findByGranteeMemberIdAndVisibleTrue(grantee)).hasSize(1);
    }

    @Test
    void findByGranterOrGranteeInReturnsGrantsOnEitherSide() {
        UUID memberA = UUID.randomUUID();
        UUID memberB = UUID.randomUUID();
        UUID unrelated = UUID.randomUUID();
        sharingGrantRepository.saveAndFlush(new SharingGrant(memberA, memberB, SharingScope.VITALS, true));
        sharingGrantRepository.saveAndFlush(new SharingGrant(unrelated, UUID.randomUUID(), SharingScope.ACTIVITY, true));

        List<SharingGrant> result = sharingGrantRepository.findByGranterMemberIdInOrGranteeMemberIdIn(
                List.of(memberA), List.of(memberA));

        assertThat(result).hasSize(1);
    }

    @Test
    void existsByGranterAndGranteeAndVisibleTrueIsFalseWhenGrantIsHidden() {
        UUID granter = UUID.randomUUID();
        UUID grantee = UUID.randomUUID();
        sharingGrantRepository.saveAndFlush(new SharingGrant(granter, grantee, SharingScope.BEHAVIOUR, false));

        assertThat(sharingGrantRepository.existsByGranterMemberIdAndGranteeMemberIdAndVisibleTrue(granter, grantee))
                .isFalse();
    }

    @Test
    void existsByGranterAndGranteeAndVisibleTrueIsTrueWhenVisible() {
        UUID granter = UUID.randomUUID();
        UUID grantee = UUID.randomUUID();
        sharingGrantRepository.saveAndFlush(new SharingGrant(granter, grantee, SharingScope.BEHAVIOUR, true));

        assertThat(sharingGrantRepository.existsByGranterMemberIdAndGranteeMemberIdAndVisibleTrue(granter, grantee))
                .isTrue();
    }
}
