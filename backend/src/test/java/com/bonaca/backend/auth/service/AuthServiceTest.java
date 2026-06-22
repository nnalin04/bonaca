package com.bonaca.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bonaca.backend.auth.dto.AuthTokensResponse;
import com.bonaca.backend.auth.dto.MeResponse;
import com.bonaca.backend.auth.exception.InvalidRefreshTokenException;
import com.bonaca.backend.auth.model.User;
import com.bonaca.backend.auth.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AuthService is the orchestration layer fronting AuthController (see CLAUDE.md's domain model
 * + the endpoint contracts in AuthController): it delegates OTP issuance/verification, JWT
 * minting, and refresh-token rotation to their respective single-purpose services, and is the
 * one place that assembles the AuthTokensResponse/MeResponse shapes returned over HTTP.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String PHONE = "+919876543210";

    @Mock
    private OtpService otpService;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserRepository userRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(otpService, jwtService, refreshTokenService, userRepository);
    }

    @Test
    void requestOtpDelegatesToOtpService() {
        authService.requestOtp(PHONE);

        verify(otpService).requestOtp(PHONE);
    }

    @Test
    void verifyOtpIssuesAccessAndRefreshTokensForTheVerifiedUser() {
        User user = userWithId(UUID.randomUUID(), PHONE, false);
        when(otpService.verifyOtp(PHONE, "1234")).thenReturn(user);
        when(jwtService.issueAccessToken(user.getId(), PHONE)).thenReturn("access-token");
        when(refreshTokenService.issue(user.getId())).thenReturn("refresh-token");

        AuthTokensResponse tokens = authService.verifyOtp(PHONE, "1234");

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.refreshToken()).isEqualTo("refresh-token");
        assertThat(tokens.profileCompleted()).isFalse();
    }

    @Test
    void refreshRotatesTheTokenAndReturnsAFreshAccessTokenForTheSameUser() {
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId, PHONE, true);
        when(refreshTokenService.rotate("old-refresh")).thenReturn(new RefreshTokenService.RotationResult(userId, "new-refresh"));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtService.issueAccessToken(userId, PHONE)).thenReturn("new-access");

        AuthTokensResponse tokens = authService.refresh("old-refresh");

        assertThat(tokens.accessToken()).isEqualTo("new-access");
        assertThat(tokens.refreshToken()).isEqualTo("new-refresh");
        assertThat(tokens.profileCompleted()).isTrue();
    }

    @Test
    void refreshThrowsWhenTheRotatedTokenBelongsToAUserThatNoLongerExists() {
        UUID userId = UUID.randomUUID();
        when(refreshTokenService.rotate("old-refresh")).thenReturn(new RefreshTokenService.RotationResult(userId, "new-refresh"));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("old-refresh")).isInstanceOf(InvalidRefreshTokenException.class);

        verify(jwtService, never()).issueAccessToken(any(), any());
    }

    @Test
    void logoutDelegatesToRefreshTokenServiceRevoke() {
        authService.logout("a-refresh-token");

        verify(refreshTokenService).revoke("a-refresh-token");
    }

    @Test
    void getMeReturnsTheCurrentUsersProfile() {
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId, PHONE, true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        MeResponse me = authService.getMe(userId);

        assertThat(me).isEqualTo(new MeResponse(userId, PHONE, true));
    }

    @Test
    void getMeThrowsWhenTheUserNoLongerExists() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getMe(userId)).isInstanceOf(InvalidRefreshTokenException.class);
    }

    private static User userWithId(UUID id, String phoneNumber, boolean profileCompleted) {
        User user = new User(phoneNumber);
        if (profileCompleted) {
            user.markProfileCompleted();
        }
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return user;
    }
}
