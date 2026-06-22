package com.bonaca.backend.notifications.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bonaca.backend.auth.service.JwtService;
import com.bonaca.backend.members.exception.ForbiddenMemberAccessException;
import com.bonaca.backend.notifications.exception.NotificationNotFoundException;
import com.bonaca.backend.notifications.exception.SelfPaymentRequestException;
import com.bonaca.backend.notifications.model.Notification;
import com.bonaca.backend.notifications.model.NotificationType;
import com.bonaca.backend.notifications.service.NotificationService;
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

@WebMvcTest(NotificationsController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationsControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

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

    private static Notification notification(UUID memberId, NotificationType type) {
        return new Notification(memberId, memberId, type, "Title", "Body", "/deep-link", null);
    }

    @Test
    void listNotificationsReturnsTheServiceResultForTheRequestedMember() throws Exception {
        UUID memberId = UUID.randomUUID();
        when(notificationService.listForMember(USER_ID, memberId))
                .thenReturn(List.of(notification(memberId, NotificationType.SUBSCRIPTION)));

        mockMvc.perform(get("/api/v1/members/" + memberId + "/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("subscription"))
                .andExpect(jsonPath("$[0].memberId").value(memberId.toString()));
    }

    @Test
    void listNotificationsReturns403WhenTheServiceRejectsTheRequester() throws Exception {
        UUID memberId = UUID.randomUUID();
        when(notificationService.listForMember(USER_ID, memberId))
                .thenThrow(new ForbiddenMemberAccessException("You can only view your own notifications"));

        mockMvc.perform(get("/api/v1/members/" + memberId + "/notifications")).andExpect(status().isForbidden());
    }

    @Test
    void markReadReturnsTheUpdatedNotification() throws Exception {
        UUID notificationId = UUID.randomUUID();
        Notification updated = notification(UUID.randomUUID(), NotificationType.METRIC_ANOMALY);
        updated.markRead();
        when(notificationService.markRead(USER_ID, notificationId)).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void markReadReturns403WhenTheRequesterIsNotTheRecipient() throws Exception {
        UUID notificationId = UUID.randomUUID();
        when(notificationService.markRead(USER_ID, notificationId))
                .thenThrow(new ForbiddenMemberAccessException("You don't have access to this notification"));

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read")).andExpect(status().isForbidden());
    }

    @Test
    void markReadReturns404WhenTheNotificationDoesNotExist() throws Exception {
        UUID notificationId = UUID.randomUUID();
        when(notificationService.markRead(USER_ID, notificationId))
                .thenThrow(new NotificationNotFoundException("Notification not found"));

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read")).andExpect(status().isNotFound());
    }

    @Test
    void requestPaymentReturns201WithTheCreatedNotification() throws Exception {
        UUID memberId = UUID.randomUUID();
        when(notificationService.requestPayment(USER_ID, memberId))
                .thenReturn(notification(memberId, NotificationType.PAYMENT_REQUEST));

        mockMvc.perform(post("/api/v1/members/" + memberId + "/notifications/payment-request"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("payment-request"));
    }

    @Test
    void requestPaymentReturns409WhenTargetingYourself() throws Exception {
        UUID memberId = UUID.randomUUID();
        when(notificationService.requestPayment(USER_ID, memberId))
                .thenThrow(new SelfPaymentRequestException("You can't request payment from yourself"));

        mockMvc.perform(post("/api/v1/members/" + memberId + "/notifications/payment-request"))
                .andExpect(status().isConflict());
    }

    @Test
    void requestPaymentReturns403WhenTheRecipientIsNotInTheSameAccount() throws Exception {
        UUID memberId = UUID.randomUUID();
        when(notificationService.requestPayment(USER_ID, memberId))
                .thenThrow(new ForbiddenMemberAccessException("You don't have access to this member"));

        mockMvc.perform(post("/api/v1/members/" + memberId + "/notifications/payment-request"))
                .andExpect(status().isForbidden());
    }
}
