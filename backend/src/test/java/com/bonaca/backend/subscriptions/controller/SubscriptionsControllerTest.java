package com.bonaca.backend.subscriptions.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bonaca.backend.auth.service.JwtService;
import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.model.MemberRole;
import com.bonaca.backend.members.service.MemberPermissions;
import com.bonaca.backend.subscriptions.exception.SubscriptionNotFoundException;
import com.bonaca.backend.subscriptions.model.Subscription;
import com.bonaca.backend.subscriptions.model.SubscriptionStatus;
import com.bonaca.backend.subscriptions.service.SubscriptionService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SubscriptionsController.class)
@AutoConfigureMockMvc(addFilters = false)
class SubscriptionsControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private MemberPermissions permissions;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void authenticate() {
        var claims = new JwtService.AccessTokenClaims(USER_ID, "+919876543210");
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(claims, null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static Member member(UUID accountId) {
        Member m = new Member(accountId, UUID.randomUUID(), MemberRole.PRIMARY, "Name", null, null, null, null);
        try {
            var field = Member.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(m, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return m;
    }

    @Test
    void getSubscriptionReturnsTheServiceResultForAMemberOfTheAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        Member requester = member(accountId);
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(requester);
        Subscription subscription = new Subscription(accountId, SubscriptionStatus.TRIAL, Instant.now());
        when(subscriptionService.getForAccount(accountId)).thenReturn(subscription);

        mockMvc.perform(get("/api/v1/accounts/" + accountId + "/subscription"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("trial"));
    }

    @Test
    void getSubscriptionReturns403WhenRequesterIsNotAMemberOfTheAccount() throws Exception {
        Member requester = member(UUID.randomUUID());
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(requester);

        mockMvc.perform(get("/api/v1/accounts/" + UUID.randomUUID() + "/subscription"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSubscriptionReturns404WhenNoSubscriptionExistsForTheAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        Member requester = member(accountId);
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(requester);
        when(subscriptionService.getForAccount(accountId)).thenThrow(new SubscriptionNotFoundException("No subscription exists"));

        mockMvc.perform(get("/api/v1/accounts/" + accountId + "/subscription"))
                .andExpect(status().isNotFound());
    }
}
