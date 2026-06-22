package com.bonaca.backend.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bonaca.backend.auth.model.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * Schema contract from V1__create_auth_schema.sql: phone_number is UNIQUE on the users table —
 * AuthService/OtpService rely on findByPhoneNumber to be the single source of truth for "does
 * this phone number already have an account." Runs on the fast H2 tier (see CLAUDE.md /
 * application-test.yml) — schema is Hibernate-generated from the entity, not replayed from the
 * Postgres-flavoured Flyway script, so this only verifies the entity-level contract.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByPhoneNumberReturnsTheMatchingUser() {
        userRepository.saveAndFlush(new User("+919876543210"));

        Optional<User> found = userRepository.findByPhoneNumber("+919876543210");

        assertThat(found).isPresent();
        assertThat(found.get().getPhoneNumber()).isEqualTo("+919876543210");
        assertThat(found.get().isProfileCompleted()).isFalse();
    }

    @Test
    void findByPhoneNumberReturnsEmptyWhenNoUserExists() {
        assertThat(userRepository.findByPhoneNumber("+910000000000")).isEmpty();
    }

    @Test
    void phoneNumberMustBeUnique() {
        userRepository.saveAndFlush(new User("+919876543210"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("+919876543210")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
