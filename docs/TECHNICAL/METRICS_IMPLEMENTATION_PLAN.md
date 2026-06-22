# Metrics — Implementation Plan

Written before any `metrics` code, per the process in
[`BACKEND_TESTING_AND_PACKAGING_PLAYBOOK.md`](BACKEND_TESTING_AND_PACKAGING_PLAYBOOK.md) §7 — this
is what test cases get written against, not a retrospective. Update this file if the design
changes during implementation; don't let it drift silently like `CLAUDE.md`'s realignment note did
(see the playbook §6).

## 1. What "metrics" covers in this pass

Per the original architecture sketch (`BACKEND_CUSTOM_IMPLEMENTATION.md` §3): *"metrics —
wearable ingestion, MetricReading, rollups."* This pass builds the **data model, the deterministic
baseline/insight engine, and the read API** a client can call today. It does **not** build the
Spike API ingestion pipeline (OAuth, webhook receiver, polling) — `CLAUDE.md`'s Wearable
Integration Phasing section explicitly gates that behind "not yet started, don't scaffold without
an explicit task." Instead, an internal `MetricIngestionService.recordReading(...)` method is the
seam a real Spike integration plugs into later; nothing public calls it yet, and tests call it
directly to seed data.

## 2. PRD requirements this maps to

Source: `docs/PRD.md` (mirrors `docs/PRD.pdf`).

- **§3 Data Sources**: Vitals/Activity from wearables (Spike API, out of scope here); Behaviour
  (screen time, outside time, last active location, steps-as-fallback) from the phone, supportive
  only. Both ultimately land as `MetricReading` rows — the source doesn't matter to this layer.
- **§4 Metric Taxonomy**: 18 metric types across 3 categories (Vitals, Activity, Behaviour) — see
  §4 below for the exact list, matching `src/types/index.ts`'s `MetricType` union so the backend
  contract and the existing frontend domain model don't diverge.
- **§5 Derived Logic & Baselines** — the core algorithm this package implements:
  - Rolling baseline over **14-21 valid days**; a day is excluded if the wearable wasn't worn,
    sync gaps exceed a threshold, or phone data is missing. Recalculated daily.
  - Comparison language is **relative only**: Higher / Same / Lower than usual — no raw thresholds,
    no medical framing. Matches `MetricTrendLabel` already in the frontend types.
  - **Outside Time**: home-cluster detection + relative comparison to baseline. *Deferred*: this
    needs geolocation/clustering logic this pass doesn't build (no location data source wired up
    yet either) — `outdoor_time` readings can be stored and trended like any other metric once a
    source exists, but the home-cluster derivation itself isn't part of this pass.
  - **Routine Consistency Score**: normalizes smartphone usage, outside time, steps, and sleep
    against baseline; behaviour weighted higher than vitals; outputs Stable / Slightly different /
    Noticeably different. Deterministic, no ML — implemented as one of `InsightGenerationService`'s
    generated `Insight` rows (kind `TREND`, `metricType = null` since it's a composite, not a
    single metric).
- **§10 Core App UX**: Member Detail needs metrics "grouped & ordered by deviation" + a daily NLP
  summary; Metric Detail needs **24h/7d/30d** ranges (not the 1D/7D/4W/1Y the built frontend
  currently uses — a known, already-tracked frontend/PRD mismatch per `CLAUDE.md`'s realignment
  note; this backend follows the PRD's ranges, the frontend range-picker mismatch is out of scope
  here).
- **§12 UX Edge Cases**: *"Device disconnected: show 'No recent data,' never show inferred
  values."* A metric type with zero readings in the requested range is omitted/returned as an
  explicit no-data shape — the baseline/trend engine never fabricates a reading.

## 3. Decisions

### 3.1 No public ingestion endpoint
`MetricIngestionService.recordReading(memberId, metricType, value, unit, recordedAt,
sourceDeviceId)` is the only entry point for writing a `MetricReading`. No `POST` endpoint is
exposed. When a real Spike integration is scoped, its webhook/poll handler calls this method —
the read side and authorization model don't change.

### 3.2 Hybrid baseline computation
- **Nightly** (`MetricsRollupScheduler`, `@Scheduled`, no new infra): `BaselineService.recomputeAllBaselines()`
  recomputes the rolling 14-21-day mean/stddev per `(member, metricType)` pair with recent data,
  upserting `MetricBaseline` rows. `InsightGenerationService.generateDailyInsights()` then
  generates/stores that day's `Insight` rows from the fresh baselines.
- **Live, on read**: `MetricsQueryService` computes the requested range's (24h/7d/30d) average,
  min/max, and chart series **directly from `MetricReading` rows at request time** — always
  current. The trend label (higher/lower/same-as-usual) compares that live data against the
  **most recently cached** `MetricBaseline` row, not a live recomputation of the 14-21-day window
  — the expensive rolling computation only runs once a night; the comparison itself is cheap and
  always reflects the latest reading.
- **Why not one or the other**: precompute-only would mean "immediate data" lags a full day
  behind a fresh reading; live-only would mean recomputing a 14-21-day rolling window on every
  request, which doesn't scale and isn't necessary since the baseline itself only needs to move
  day-to-day, not request-to-request.

### 3.3 No Kafka/MQ
No event-driven requirement exists in this pass — there's no real ingestion producer yet, and the
rollup job is a simple nightly scheduled task. Matches `BACKEND_CUSTOM_IMPLEMENTATION.md` §6's
"skip realtime infra for MVP" call. Revisit only when a real Spike webhook pipeline is built —
that's the point an async boundary (queue or otherwise) might actually earn its complexity.

### 3.4 REST, not GraphQL
The query shapes needed are small and fixed (member's metrics summary, one metric's detail, a
member's insights) — not an open-ended, client-driven query surface, which is where GraphQL
actually pays off. Adding GraphQL here would mean a new dependency, a second API paradigm
alongside the fully-REST `auth`/`members` API, and a separate authorization layer to design, for
marginal benefit at this scale. Instead, the summary endpoint returns *all* of a member's metrics
in one response (not one round trip per metric card) — the actual mobile-efficiency win GraphQL
would have given, without the paradigm switch.

## 4. Domain model

`MetricType` (18 values, exact match to `src/types/index.ts`):

| Category (`SharingScope`) | Metric types |
|---|---|
| `VITALS` | `HEART_RATE`, `HEART_RATE_VARIABILITY`, `BLOOD_OXYGEN`, `RESPIRATION_RATE`, `SLEEP`, `STRESS_LEVEL`, `BODY_TEMPERATURE`, `ECG`, `BLOOD_GLUCOSE`, `VO2_MAX` |
| `ACTIVITY` | `STEPS`, `CALORIES`, `WORKOUTS`, `TRAINING_LOAD` |
| `BEHAVIOUR` | `SCREEN_TIME`, `OUTDOOR_TIME`, `ROUTINE_ADHERENCE`, `LAST_ACTIVE_LOCATION` |

This category mapping reuses `members.model.SharingScope` (not a duplicate enum) — it's a static
lookup (`MetricType -> SharingScope`) inside `MetricsQueryService`, since that's exactly the
PRD §11.2 permission category each metric type belongs to.

`MetricTrendLabel`: `HIGHER_THAN_USUAL`, `LOWER_THAN_USUAL`, `SAME_AS_USUAL` (matches the frontend
type of the same name).

`InsightKind`: `TREND`, `ANOMALY`.

Entities (package `com.bonaca.backend.metrics.model`): `MetricReading`, `MetricBaseline`,
`Insight` — schema in §6.

## 5. Authorization — `canViewScope`, an additive extension to `members`

`members.service.MemberPermissions.canView(requester, target)` only checks *member-level*
visibility (does any visible grant exist between these two members) — correct for
`getMember`/`listVisibleMembers`, but not enough for metrics: PRD §11.2's permission categories
are scope-specific (a Secondary might have Vitals but not Behaviour). New method, same class:

```java
public boolean canViewScope(Member requester, Member target, SharingScope scope) {
    if (requester.getId().equals(target.getId())) return true;
    if (requester.getRole() == MemberRole.PRIMARY && requester.getAccountId().equals(target.getAccountId())) return true;
    return sharingGrantRepository.existsByGranterMemberIdAndGranteeMemberIdAndScopeAndVisibleTrue(
            target.getId(), requester.getId(), scope);
}
```

New repository method on `members.repository.SharingGrantRepository`:
`existsByGranterMemberIdAndGranteeMemberIdAndScopeAndVisibleTrue(UUID, UUID, SharingScope)`.
Both additions are purely additive — `canView` and the existing
`existsByGranterMemberIdAndGranteeMemberIdAndVisibleTrue` are untouched, so no existing `auth`/
`members` test should change behavior.

## 6. Schema — `V3__create_metrics_schema.sql`

```sql
CREATE TABLE metric_readings (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id        UUID NOT NULL REFERENCES members (id),
    metric_type      VARCHAR(40) NOT NULL,
    metric_value     NUMERIC(10,3) NOT NULL,
    unit             VARCHAR(20) NOT NULL,
    recorded_at      TIMESTAMPTZ NOT NULL,
    source_device_id VARCHAR(120),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_metric_readings_member_type_recorded ON metric_readings (member_id, metric_type, recorded_at DESC);

CREATE TABLE metric_baselines (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id       UUID NOT NULL REFERENCES members (id),
    metric_type     VARCHAR(40) NOT NULL,
    baseline_mean   NUMERIC(10,3) NOT NULL,
    baseline_stddev NUMERIC(10,3) NOT NULL,
    valid_day_count INT NOT NULL,
    computed_at     TIMESTAMPTZ NOT NULL,
    UNIQUE (member_id, metric_type)
);

CREATE TABLE insights (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id      UUID NOT NULL REFERENCES members (id),
    metric_type    VARCHAR(40),              -- nullable: Routine Consistency Score isn't tied to one metric
    generated_text VARCHAR(280) NOT NULL,
    kind           VARCHAR(20) NOT NULL,
    insight_date   DATE NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (member_id, metric_type, insight_date)
);
```

Per the playbook §2's known gap (entity constraints not always mirroring migration SQL exactly):
the `UNIQUE (member_id, metric_type, insight_date)` and `UNIQUE (member_id, metric_type)`
constraints **will** be declared on the `@Entity` classes too (via `@Table(uniqueConstraints=...)`)
this time, specifically because the upsert logic in `BaselineService`/`InsightGenerationService`
depends on them being enforced consistently across both the H2 fast tier and the real Postgres
tier — unlike `SharingGrant`'s pre-existing gap, this one is load-bearing for this feature's own
logic, not incidental.

## 7. API

All under JWT auth, same `@AuthenticationPrincipal JwtService.AccessTokenClaims` pattern as
`auth`/`members`.

- `GET /api/v1/members/{memberId}/metrics?range=24h|7d|30d` → `List<MetricSummaryResponse>`.
  One entry per metric type with at least one reading in range, sorted by deviation from the
  cached baseline (`|currentAverage - baselineMean| / baselineStddev`, descending) — PRD §10's
  "grouped & ordered by deviation."
- `GET /api/v1/members/{memberId}/metrics/{metricType}?range=24h|7d|30d` → `MetricDetailResponse`
  (average, rangeMin/rangeMax, chart series, live trend label, today's insight text for that
  metric if one was generated). 404 via `MemberNotFoundException` if the member doesn't exist;
  empty/no-data shape (not 404) if the member exists but has no readings for that metric/range.
- `GET /api/v1/members/{memberId}/insights` → `List<InsightResponse>`.
- 403 via the existing `members.exception.ForbiddenMemberAccessException` (already wired into
  `common.ApiExceptionHandler`) when `canViewScope` denies access — no new exception type needed.

DTOs are shaped to closely mirror `src/features/metrics/mockData.ts`'s
`MetricDetailSummary`/`MetricReading`/`Insight` so a future frontend integration swapping the mock
for a real fetch is close to a drop-in replacement — not done in this pass, backend only.

## 8. Package layout

```
metrics/
  controller/  MetricsController
  service/     MetricIngestionService, BaselineService, InsightGenerationService,
               MetricsQueryService, MetricsRollupScheduler
  repository/  MetricReadingRepository, MetricBaselineRepository, InsightRepository
  model/       MetricReading, MetricBaseline, Insight, MetricType, MetricTrendLabel, InsightKind
  dto/         MetricSummaryResponse, MetricDetailResponse, InsightResponse
  integration/ (test) MetricsFlowIntegrationTest
```

## 9. Test strategy

Per the playbook: TDD red/green verified for every net-new unit test, H2 fast tier + Postgres
Testcontainers tier, JaCoCo gate expanded to `com.bonaca.backend.metrics.**` at the same 90%
instruction-ratio threshold.

- `BaselineServiceTest`: deterministic mean/stddev over a 14-21-valid-day window; a metric with
  fewer than 14 valid days doesn't get a baseline (or keeps its last one — pick one, test it).
- `InsightGenerationServiceTest`: trend text generation from a baseline comparison; Routine
  Consistency Score composite insight.
- `MetricsQueryServiceTest`: deviation ordering, no-data omission, live-range-vs-cached-baseline
  trend computation, scope-gated denial.
- `MemberPermissionsTest`: new cases for `canViewScope` (self / Primary-same-account / scoped
  grant present / wrong-scope-denied).
- `MetricsControllerTest` (`@WebMvcTest`, `addFilters = false`, mocked `JwtService`,
  `SecurityContextHolder`-pushed principal — same pattern as `MembersControllerTest`).
- `MetricReadingRepositoryTest`, `MetricBaselineRepositoryTest`, `InsightRepositoryTest` (H2,
  `@DataJpaTest`, `@AutoConfigureTestDatabase(replace = NONE)`, `@ActiveProfiles("test")`).
- `MetricsFlowIntegrationTest` (Testcontainers/Postgres, `metrics/integration/`): real
  sign-up/complete-profile/invite flow for a Primary + Secondary (reusing
  `MembersFlowIntegrationTest`'s helper pattern), grant the Secondary Vitals-only access, seed
  15-20 days of heart-rate readings via `MetricIngestionService` directly, call
  `BaselineService`/`InsightGenerationService` directly to simulate the nightly job, then hit the
  real REST endpoints as both members and assert trend label correctness, deviation ordering,
  scope-403 for an ungranted category, and the no-data shape for a metric with zero readings.
  Requires a running Docker daemon — if unavailable, this gets flagged explicitly as unverified
  rather than claimed.

## 10. Non-goals (explicit)

- Spike API client, OAuth, webhook receiver, polling — gated behind a future explicit task.
- Outside Time's home-cluster geolocation derivation — needs a location data source this pass
  doesn't have.
- Kafka/any message broker.
- GraphQL.
- A public ingestion/write endpoint.
- Changing the frontend's mock data wiring (`src/features/metrics/mockData.ts`) — backend only.
