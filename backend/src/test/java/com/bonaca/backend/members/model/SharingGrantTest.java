package com.bonaca.backend.members.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SharingGrantTest {

    @Test
    void newGrantHasACreationTimestampAndTheConstructedVisibility() {
        SharingGrant grant = new SharingGrant(UUID.randomUUID(), UUID.randomUUID(), SharingScope.VITALS, true);

        assertThat(grant.getCreatedAt()).isNotNull();
        assertThat(grant.isVisible()).isTrue();
    }
}
