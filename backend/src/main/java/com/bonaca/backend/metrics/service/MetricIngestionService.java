package com.bonaca.backend.metrics.service;

import com.bonaca.backend.members.exception.MemberNotFoundException;
import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.repository.MemberRepository;
import com.bonaca.backend.metrics.exception.SubscriptionInactiveException;
import com.bonaca.backend.metrics.model.MetricReading;
import com.bonaca.backend.metrics.model.MetricType;
import com.bonaca.backend.metrics.repository.MetricReadingRepository;
import com.bonaca.backend.subscriptions.service.SubscriptionService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * The single entry point for writing a MetricReading — see
 * docs/TECHNICAL/METRICS_IMPLEMENTATION_PLAN.md §3.1: deliberately not exposed over REST/GraphQL.
 * A future Spike API integration calls this directly; until then, only tests do.
 *
 * <p>PRD §12: "Subscription lapses: wearable sync pauses." This is the one place that's
 * enforced — old readings stay visible read-only (the read path, MetricsQueryService, is
 * deliberately untouched), only *new* ingestion is blocked once the owning account's
 * subscription isn't active.
 */
@Service
public class MetricIngestionService {

    private final MetricReadingRepository metricReadingRepository;
    private final MemberRepository memberRepository;
    private final SubscriptionService subscriptionService;

    public MetricIngestionService(
            MetricReadingRepository metricReadingRepository,
            MemberRepository memberRepository,
            SubscriptionService subscriptionService) {
        this.metricReadingRepository = metricReadingRepository;
        this.memberRepository = memberRepository;
        this.subscriptionService = subscriptionService;
    }

    public MetricReading recordReading(
            UUID memberId, MetricType metricType, double value, String unit, Instant recordedAt, String sourceDeviceId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new MemberNotFoundException("Member not found"));
        if (!subscriptionService.isActive(member.getAccountId())) {
            throw new SubscriptionInactiveException("This account's subscription isn't active — wearable sync is paused");
        }
        return metricReadingRepository.save(new MetricReading(memberId, metricType, value, unit, recordedAt, sourceDeviceId));
    }
}
