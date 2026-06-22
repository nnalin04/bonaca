package com.bonaca.backend.auth.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void newUserStartsWithProfileNotCompletedAndACreationTimestamp() {
        User user = new User("+919876543210");

        assertThat(user.getPhoneNumber()).isEqualTo("+919876543210");
        assertThat(user.isProfileCompleted()).isFalse();
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    void markProfileCompletedFlipsTheFlag() {
        User user = new User("+919876543210");

        user.markProfileCompleted();

        assertThat(user.isProfileCompleted()).isTrue();
    }
}
