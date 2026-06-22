package com.bonaca.backend.auth.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bonaca.backend.auth.dto.AuthTokensResponse;
import com.bonaca.backend.auth.dto.MeResponse;
import com.bonaca.backend.auth.service.AuthService;
import com.bonaca.backend.auth.service.JwtService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Controller-layer slice covering every endpoint AuthController exposes (see
 * AuthController.java): the 5 routes under /api/v1/auth, the validation contract each request
 * DTO declares (RequestOtpRequest/VerifyOtpRequest/RefreshRequest — E.164 phone, numeric code,
 * non-blank refresh token), and that /me resolves its principal via @AuthenticationPrincipal.
 * Security filters are disabled here deliberately — filter-chain behavior (e.g. rejecting
 * missing/garbage tokens) is already covered by AuthFlowIntegrationTest's
 * meRejectsMissingOrGarbageTokens, this slice is only about the controller's own routing,
 * delegation, and validation.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    private static final String VALID_PHONE = "+919876543210";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    // JwtAuthFilter (com.bonaca.backend.config) is picked up by @WebMvcTest's component scan as
    // a Filter bean even though addFilters=false stops it running — its constructor still needs
    // a JwtService bean to satisfy context startup.
    @MockitoBean
    private JwtService jwtService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestOtpReturns202WhenPhoneNumberIsValid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber": "%s"}""".formatted(VALID_PHONE)))
                .andExpect(status().isAccepted());

        verify(authService).requestOtp(VALID_PHONE);
    }

    @Test
    void requestOtpReturns400ForANonE164PhoneNumber() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber": "9876543210"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyOtpReturnsTokensOnSuccess() throws Exception {
        AuthTokensResponse tokens = new AuthTokensResponse("access-token", "refresh-token", false);
        when(authService.verifyOtp(VALID_PHONE, "1234")).thenReturn(tokens);

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber": "%s", "code": "1234"}""".formatted(VALID_PHONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.profileCompleted").value(false));
    }

    @Test
    void verifyOtpReturns400ForANonNumericCode() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber": "%s", "code": "abcd"}""".formatted(VALID_PHONE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshReturnsRotatedTokens() throws Exception {
        AuthTokensResponse tokens = new AuthTokensResponse("new-access", "new-refresh", true);
        when(authService.refresh("old-refresh")).thenReturn(tokens);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "old-refresh"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void refreshReturns400ForABlankRefreshToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": ""}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logoutReturns204AndDelegatesToAuthService() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "a-refresh-token"}"""))
                .andExpect(status().isNoContent());

        verify(authService).logout("a-refresh-token");
    }

    @Test
    void meReturnsTheAuthenticatedUsersProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        var claims = new JwtService.AccessTokenClaims(userId, VALID_PHONE);
        when(authService.getMe(userId)).thenReturn(new MeResponse(userId, VALID_PHONE, true));

        // addFilters=false strips the whole filter chain, including the security-test filter
        // that would normally apply a with(authentication(...)) post-processor — so the
        // principal is pushed onto SecurityContextHolder directly instead. @AuthenticationPrincipal
        // reads from there regardless of which filters ran.
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(claims, null, List.of()));

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value(VALID_PHONE))
                .andExpect(jsonPath("$.profileCompleted").value(true));

        verify(authService).getMe(eq(userId));
    }
}
