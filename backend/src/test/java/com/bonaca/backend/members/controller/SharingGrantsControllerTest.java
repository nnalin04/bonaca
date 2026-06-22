package com.bonaca.backend.members.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bonaca.backend.auth.service.JwtService;
import com.bonaca.backend.members.dto.SharingGrantResponse;
import com.bonaca.backend.members.service.SharingGrantService;
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

@WebMvcTest(SharingGrantsController.class)
@AutoConfigureMockMvc(addFilters = false)
class SharingGrantsControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SharingGrantService sharingGrantService;

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

    @Test
    void listGrantsReturnsTheServiceResultForTheGivenAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        SharingGrantResponse response =
                new SharingGrantResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "vitals", true);
        when(sharingGrantService.listForAccount(USER_ID, accountId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/sharing-grants").param("accountId", accountId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scope").value("vitals"));
    }

    @Test
    void updateGrantReturnsTheUpdatedGrant() throws Exception {
        UUID grantId = UUID.randomUUID();
        SharingGrantResponse response =
                new SharingGrantResponse(grantId, UUID.randomUUID(), UUID.randomUUID(), "activity", false);
        when(sharingGrantService.updateVisibility(USER_ID, grantId, false)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/sharing-grants/" + grantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"visible": false}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visible").value(false));
    }

    @Test
    void updateGrantReturns400WhenVisibleIsMissing() throws Exception {
        mockMvc.perform(patch("/api/v1/sharing-grants/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
