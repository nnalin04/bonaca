package com.bonaca.backend.members.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bonaca.backend.auth.service.JwtService;
import com.bonaca.backend.members.dto.InviteResponse;
import com.bonaca.backend.members.service.InviteService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InvitesController.class)
@AutoConfigureMockMvc(addFilters = false)
class InvitesControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String VALID_PHONE = "+919876543210";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InviteService inviteService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void authenticate() {
        var claims = new JwtService.AccessTokenClaims(USER_ID, VALID_PHONE);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(claims, null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createInviteReturnsTheCreatedInvite() throws Exception {
        InviteResponse response = new InviteResponse(UUID.randomUUID(), VALID_PHONE, "secondary", "pending");
        when(inviteService.create(USER_ID, VALID_PHONE)).thenReturn(response);

        mockMvc.perform(post("/api/v1/invites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber": "%s"}""".formatted(VALID_PHONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value(VALID_PHONE))
                .andExpect(jsonPath("$.status").value("pending"));
    }

    @Test
    void createInviteReturns400ForANonE164PhoneNumber() throws Exception {
        mockMvc.perform(post("/api/v1/invites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phoneNumber": "9876543210"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listInvitesReturnsTheServiceResult() throws Exception {
        InviteResponse response = new InviteResponse(UUID.randomUUID(), VALID_PHONE, "secondary", "pending");
        when(inviteService.listForCurrentAccount(USER_ID)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/invites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].phoneNumber").value(VALID_PHONE));
    }
}
