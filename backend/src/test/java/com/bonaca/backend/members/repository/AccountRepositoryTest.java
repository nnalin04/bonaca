package com.bonaca.backend.members.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bonaca.backend.members.model.Account;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void savedAccountCanBeFoundById() {
        Account account = accountRepository.saveAndFlush(new Account());

        assertThat(accountRepository.findById(account.getId())).isPresent();
    }

    @Test
    void ownerMemberIdIsNullUntilSet() {
        Account account = accountRepository.saveAndFlush(new Account());

        assertThat(account.getOwnerMemberId()).isNull();

        account.setOwnerMemberId(UUID.randomUUID());
        accountRepository.saveAndFlush(account);

        assertThat(accountRepository.findById(account.getId()).orElseThrow().getOwnerMemberId()).isNotNull();
    }
}
