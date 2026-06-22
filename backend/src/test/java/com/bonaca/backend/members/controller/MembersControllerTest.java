package com.bonaca.backend.members.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bonaca.backend.auth.service.JwtService;
import com.bonaca.backend.members.dto.MemberResponse;
import com.bonaca.backend.members.service.MembersService;
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

/** Controller-layer slice covering every endpoint MembersController exposes. */
@WebMvcTest(MembersController.class)
@AutoConfigureMockMvc(addFilters = false)
class MembersControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MembersService membersService;

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

    private static MemberResponse sampleResponse(UUID id) {
        return new MemberResponse(id, UUID.randomUUID(), "primary", "Asha", null, false, false, null, null, null, null, null, true);
    }

    @Test
    void completeProfileReturnsTheCreatedMember() throws Exception {
        MemberResponse response = sampleResponse(UUID.randomUUID());
        when(membersService.completeProfile(eq(USER_ID), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/members/complete-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Asha Kumar"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Asha"));
    }

    @Test
    void completeProfileReturns400WhenNameIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/members/complete-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": ""}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listMembersReturnsTheServiceResult() throws Exception {
        MemberResponse response = sampleResponse(UUID.randomUUID());
        when(membersService.listVisibleMembers(USER_ID)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(response.id().toString()));
    }

    @Test
    void getMemberReturnsTheRequestedMember() throws Exception {
        UUID memberId = UUID.randomUUID();
        MemberResponse response = sampleResponse(memberId);
        when(membersService.getMember(USER_ID, memberId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/members/" + memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberId.toString()));
    }

    @Test
    void updateMemberReturnsTheUpdatedMember() throws Exception {
        UUID memberId = UUID.randomUUID();
        MemberResponse response = sampleResponse(memberId);
        when(membersService.updateMember(eq(USER_ID), eq(memberId), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/members/" + memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname": "Bro"}"""))
                .andExpect(status().isOk());

        verify(membersService).updateMember(eq(USER_ID), eq(memberId), any());
    }
}
