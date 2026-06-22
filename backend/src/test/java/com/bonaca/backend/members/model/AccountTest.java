package com.bonaca.backend.members.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccountTest {

    @Test
    void newAccountHasACreationTimestampAndNoOwnerYet() {
        Account account = new Account();

        assertThat(account.getCreatedAt()).isNotNull();
        assertThat(account.getOwnerMemberId()).isNull();
    }
}
