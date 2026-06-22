package com.bonaca.backend.metrics.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bonaca.backend.auth.service.JwtService;
import com.bonaca.backend.metrics.dto.InsightResponse;
import com.bonaca.backend.metrics.dto.MetricDetailResponse;
import com.bonaca.backend.metrics.dto.MetricRange;
import com.bonaca.backend.metrics.dto.MetricSummaryResponse;
import com.bonaca.backend.metrics.model.MetricType;
import com.bonaca.backend.metrics.service.MetricsQueryService;
import java.time.LocalDate;
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

/** Controller-layer slice covering every endpoint MetricsController exposes. */
@WebMvcTest(MetricsController.class)
@AutoConfigureMockMvc(addFilters = false)
class MetricsControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MetricsQueryService metricsQueryService;

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
    void getMemberMetricsSummaryReturnsTheServiceResult() throws Exception {
        UUID memberId = UUID.randomUUID();
        MetricSummaryResponse summary = new MetricSummaryResponse("heart_rate", 75.0, "bpm", 70.0, 80.0, "same_as_usual", 0.2);
        when(metricsQueryService.getMemberMetricsSummary(USER_ID, memberId, MetricRange.SEVEN_DAYS)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/members/" + memberId + "/metrics").param("range", "7d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].metricType").value("heart_rate"));
    }

    @Test
    void getMemberMetricsSummaryReturns400ForAnInvalidRange() throws Exception {
        UUID memberId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/members/" + memberId + "/metrics").param("range", "bogus"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMetricDetailReturnsTheServiceResult() throws Exception {
        UUID memberId = UUID.randomUUID();
        MetricDetailResponse detail = new MetricDetailResponse(
                "heart_rate", true, 75.0, "bpm", 70.0, 80.0, List.of(70.0, 75.0, 80.0), "same_as_usual", null);
        when(metricsQueryService.getMetricDetail(USER_ID, memberId, MetricType.HEART_RATE, MetricRange.TWENTY_FOUR_HOURS))
                .thenReturn(detail);

        mockMvc.perform(get("/api/v1/members/" + memberId + "/metrics/heart_rate").param("range", "24h"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasData").value(true))
                .andExpect(jsonPath("$.average").value(75.0));
    }

    @Test
    void getMetricDetailReturns400ForAnUnknownMetricType() throws Exception {
        UUID memberId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/members/" + memberId + "/metrics/not_a_real_metric").param("range", "24h"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listInsightsReturnsTheServiceResult() throws Exception {
        UUID memberId = UUID.randomUUID();
        InsightResponse insight = new InsightResponse(UUID.randomUUID(), null, "Routine Consistency: Stable", "trend", LocalDate.now());
        when(metricsQueryService.listInsights(USER_ID, memberId)).thenReturn(List.of(insight));

        mockMvc.perform(get("/api/v1/members/" + memberId + "/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].generatedText").value("Routine Consistency: Stable"));
    }
}
