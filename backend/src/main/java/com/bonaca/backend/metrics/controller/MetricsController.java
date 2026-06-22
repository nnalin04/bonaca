package com.bonaca.backend.metrics.controller;

import com.bonaca.backend.auth.service.JwtService;
import com.bonaca.backend.metrics.dto.InsightResponse;
import com.bonaca.backend.metrics.dto.MetricDetailResponse;
import com.bonaca.backend.metrics.dto.MetricRange;
import com.bonaca.backend.metrics.dto.MetricSummaryResponse;
import com.bonaca.backend.metrics.exception.InvalidMetricTypeException;
import com.bonaca.backend.metrics.model.MetricType;
import com.bonaca.backend.metrics.service.MetricsQueryService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST, not GraphQL — see docs/TECHNICAL/METRICS_IMPLEMENTATION_PLAN.md §3.4. The
 * member-metrics-summary endpoint returns every metric in one response rather than one round
 * trip per card, which is the actual mobile-efficiency win a GraphQL query would otherwise give.
 */
@RestController
@RequestMapping("/api/v1/members/{memberId}")
public class MetricsController {

    private final MetricsQueryService metricsQueryService;

    public MetricsController(MetricsQueryService metricsQueryService) {
        this.metricsQueryService = metricsQueryService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<List<MetricSummaryResponse>> getMemberMetricsSummary(
            @AuthenticationPrincipal JwtService.AccessTokenClaims claims,
            @PathVariable UUID memberId,
            @RequestParam String range) {
        return ResponseEntity.ok(
                metricsQueryService.getMemberMetricsSummary(claims.userId(), memberId, MetricRange.fromQueryValue(range)));
    }

    @GetMapping("/metrics/{metricType}")
    public ResponseEntity<MetricDetailResponse> getMetricDetail(
            @AuthenticationPrincipal JwtService.AccessTokenClaims claims,
            @PathVariable UUID memberId,
            @PathVariable String metricType,
            @RequestParam String range) {
        return ResponseEntity.ok(metricsQueryService.getMetricDetail(
                claims.userId(), memberId, parseMetricType(metricType), MetricRange.fromQueryValue(range)));
    }

    @GetMapping("/insights")
    public ResponseEntity<List<InsightResponse>> listInsights(
            @AuthenticationPrincipal JwtService.AccessTokenClaims claims, @PathVariable UUID memberId) {
        return ResponseEntity.ok(metricsQueryService.listInsights(claims.userId(), memberId));
    }

    private static MetricType parseMetricType(String value) {
        try {
            return MetricType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidMetricTypeException("Unknown metric type '" + value + "'");
        }
    }
}
