# Backend Testing & Packaging Playbook

How `auth` and `members`/`sharing` were actually restructured and tested (2026-06-21). This is the
reference pattern for every backend feature module that follows — `metrics`, `subscriptions`,
`notifications` — not a proposal, a record of what was done and verified to work, including the
real gotchas hit along the way.

---

## 1. Package layout — one feature, layered underneath

Each business-area package (`auth`, `members`, and so on for future ones) splits into:

```
<feature>/
  controller/   @RestController classes — thin, delegate to service/, no business logic
  service/      @Service classes — orchestration + business rules; authorization helpers
                (e.g. MemberPermissions) also live here, not in their own top-level package
  repository/   Spring Data JPA interfaces
  model/        @Entity classes AND the enums tied to their columns (e.g. MemberRole,
                SubscriptionStatus) — enums live with the entities that use them, not separately
  dto/          Request/response records — already its own package per feature, unchanged
  exception/    Feature-specific RuntimeExceptions, handled centrally by
                com.bonaca.backend.common.ApiExceptionHandler
  integration/  (test-only) full-stack Postgres/Testcontainers flow tests — see §3
```

App-wide cross-cutting concerns (`com.bonaca.backend.config` — SecurityConfig, JwtAuthFilter,
CryptoConfig; `com.bonaca.backend.common` — ApiExceptionHandler) stay outside any feature
package and aren't restructured.

**Real gotcha hit doing this**: an entity's `protected` no-arg constructor (the JPA convention)
stops being callable from a service class once they're in different packages — `protected` only
grants same-package or subclass access, not cross-package. If the entity has no real "business"
constructor (e.g. `Account`, built bare then mutated via setters), make the no-arg constructor
`public` rather than inventing a fake business constructor just to satisfy the access modifier.

## 2. Test tiers — H2 fast tier + Testcontainers Postgres tier, not one or the other

Two deliberately different tiers, kept side by side:

- **Fast tier (H2, milliseconds)**: `@DataJpaTest` repository slices, `@WebMvcTest` controller
  slices, plain Mockito unit tests for services. Activated via `@ActiveProfiles("test")`, which
  picks up `src/test/resources/application-test.yml`:
  - H2 in-memory, `MODE=PostgreSQL`, `DB_CLOSE_DELAY=-1`
  - `spring.jpa.hibernate.ddl-auto: create-drop` — schema generated **from the JPA entities**,
    not replayed from the Postgres-flavoured Flyway scripts under `src/main/resources/db/migration`
  - `spring.flyway.enabled: false`
  - Fixed, explicit `bonaca.jwt.*` / `bonaca.otp.*` values (not relying on the dev secret's
    env-var fallback) so tests are deterministic
  - For `@DataJpaTest`, always add `@AutoConfigureTestDatabase(replace = Replace.NONE)` —
    otherwise Spring Boot silently swaps in its own auto-configured embedded DB and ignores the
    explicit datasource config above.
  - **Known, deliberate gap**: because the schema comes from the entities, not the Flyway SQL,
    this tier won't catch a broken migration, and any DB-level constraint that exists only in
    the raw SQL (not mirrored on the `@Entity` via `@Column`/`@Table(uniqueConstraints=...)`)
    isn't enforced here either. Example found in `members`: `sharing_grants` has a composite
    `UNIQUE (granter_member_id, grantee_member_id, scope)` in
    `V2__create_members_schema.sql` that `SharingGrant.java` never declared — a real,
    pre-existing gap, left as-is rather than silently "fixed" as a packaging side effect.
  - **Reserved-word column names fail silently per-table, not per-build**: `metrics`' `value`
    column (on `MetricReading`) is a reserved word in H2's SQL grammar (not Postgres's) — schema
    generation for that one table threw a `CommandAcceptanceException` that Hibernate logs as a
    `WARN`, not an error, so `./mvnw test` kept running and every *other* table's tests passed
    fine; only the affected table's tests failed, with a confusing "Table X not found" error
    instead of a syntax error pointing at the real cause. If a `@DataJpaTest` fails with "table
    not found" for a table that's clearly declared, check the H2 schema-creation `WARN` logs for
    a `CommandAcceptanceException` before assuming the test itself is wrong. Renamed to
    `metric_value` in both the entity and the Flyway migration, to keep both schemas in sync —
    cheaper than relying on H2-specific identifier quoting.
  - **`@WebMvcTest` controller slices**: also pull in `JwtAuthFilter` from `com.bonaca.backend.config`
    as a `Filter` bean during component scan even with `addFilters = false` — mock `JwtService`
    too (`@MockitoBean private JwtService jwtService;`) just to satisfy its constructor, since the
    filter never actually runs.
  - **Testing `@AuthenticationPrincipal` under `addFilters = false`**: the standard
    `SecurityMockMvcRequestPostProcessors.authentication(...)` post-processor relies on a security
    filter that's stripped out by `addFilters = false`, so it silently doesn't work. Push the
    principal directly instead: `SecurityContextHolder.getContext().setAuthentication(...)` in a
    `@BeforeEach`/inline, with `SecurityContextHolder.clearContext()` in `@AfterEach`.

- **Migration-accurate tier (Testcontainers, real Postgres, real Flyway)**: full
  `@SpringBootTest(webEnvironment = RANDOM_PORT)` HTTP flow tests in each feature's
  `integration/` test package (`AuthFlowIntegrationTest`, `MembersFlowIntegrationTest`, etc.),
  importing `TestcontainersConfiguration`. This is the tier that actually replays the Postgres
  migrations and catches anything the fast tier structurally can't. **Requires a running Docker
  daemon** — if none is available in the current environment, say so explicitly rather than
  silently skipping; don't claim these as verified without actually running them.
  - **Assert against the database directly, not just through API responses.** A flow test that
    only checks API responses (`POST` → 200, then a later `GET` reflects the change) can pass
    even if the persisted row itself is wrong, as long as whatever the read path happens to
    expose still looks plausible — it doesn't prove the write actually landed correctly. Inject
    the relevant repositories (`@Autowired private XRepository xRepository;`) into the flow test
    and assert on the real row after every meaningful write: does it exist, are the
    actually-computed/actually-stored field values correct (not just "the response had *a*
    value"), and — just as importantly — did an operation that's supposed to be a no-op (a
    rejected request, a duplicate) correctly *not* create or mutate a row. Do both: the direct
    repository assertion (is it really in the database) and the API-response assertion (does the
    API correctly expose what's really there) — they catch different classes of bug. Retrofitted
    into `AuthFlowIntegrationTest`, `OtpExpiryIntegrationTest`, `MembersFlowIntegrationTest`, and
    `MetricsFlowIntegrationTest` after this gap was pointed out by the user — apply it from the
    start for every new feature's flow test, not as an afterthought.
  - **The general principle, beyond just "check the database": a flow test must exercise every
    real component actually in the request's path, not just its input/output boundary.** Today
    that path is exactly 3 hops for every endpoint in this codebase — controller → service →
    repository → Postgres — and no more, because there is no cache layer anywhere yet (no Redis,
    no Spring Cache, nothing in `pom.xml`). If a cache is ever introduced (a plausible future
    candidate: `metrics` baseline/insight lookups, explicitly called out as an "expensive
    computation" the nightly-precompute design already exists to avoid recomputing per-request —
    see `METRICS_IMPLEMENTATION_PLAN.md` §3.2), the flow test for that feature must verify
    *both* paths the cache introduces, not just the end-to-end happy path: a cache-miss request
    populates the cache *and* still returns the correct DB-backed value, and a subsequent
    cache-hit request returns the correct value *without* hitting the database again (assert via
    a Mockito spy/verify on the repository, or an explicit cache-inspection call, that the second
    request didn't re-query Postgres). The database is the example that came up, not the whole
    rule — whatever layers genuinely sit in the real request pipeline need their own pass/fail
    behavior verified, not just the two ends of it.

## 3. Mocking external HTTP calls — WireMock, only when there's a real call to mock

Dependency: `org.wiremock:wiremock-standalone` (test scope) — **not** the bare `org.wiremock:wiremock`
core + `wiremock-jetty12` combo. The split-artifact version pulls in a Jetty version that
collides with Spring Boot's own Jetty dependency management (`NoClassDefFoundError` on
`org.eclipse.jetty.io.WriteFlusher$Listener`), even though `mvn dependency:tree` shows nothing
obviously wrong. `wiremock-standalone` is shaded/relocated and sidesteps the conflict entirely.

Only add a WireMock-based test where the production code actually makes an outbound HTTP call.
For not-yet-implemented integrations (e.g. `Msg91OtpSender`, gated behind DLT registration — see
`docs/TECHNICAL_REQUIREMENTS.md` §4), a documented scaffold test that proves the dependency
itself works is reasonable, but don't invent fake assertions about a call that doesn't exist yet.

## 4. Coverage gate — JaCoCo, 90% instruction ratio, scoped per pass

`backend/pom.xml` → `jacoco-maven-plugin` (version `0.8.15`, pinned explicitly — neither
`spring-boot-dependencies` nor `spring-boot-starter-parent` manage a JaCoCo version): `prepare-agent`
bound to `test`, `report` + `check` bound to `verify`. The `check` rule is a single `BUNDLE`-element
rule with `<includes>` listing every feature package finished so far
(`com.bonaca.backend.auth.**`, `com.bonaca.backend.members.**`, ...) — **append to this list as
each feature is restructured**, don't gate on the whole codebase before every feature has been
through this process, or the build breaks on not-yet-touched code.

`./mvnw verify` runs the gate. To inspect actual per-package numbers (not just pass/fail):

```bash
python3 - <<'EOF'
import xml.etree.ElementTree as ET
root = ET.parse('target/site/jacoco/jacoco.xml').getroot()
for pkg in root.findall('package'):
    name = pkg.get('name').replace('/', '.')
    for c in pkg.findall('counter'):
        if c.get('type') == 'INSTRUCTION':
            m, cov = int(c.get('missed')), int(c.get('covered'))
            print(f"{name}: {cov}/{m+cov} = {100*cov/(m+cov):.1f}%")
EOF
```

Result so far: `auth.**` 95.34% standalone (98.84% after a model-package gap-fill pass),
`members.**` 97.36% (99.38% combined with `auth` after gap-fill). Gap-fill targets are almost
always unused entity getters/setters never hit by service or repository tests — close them with
small, direct entity tests rather than padding service tests artificially.

## 5. TDD discipline — write the test against the documented contract, verify it's actually red

For every net-new test, not just "does it pass":
1. Write the assertion against the **documented** behavior — `application.yml` for numeric
   constants with no higher spec (OTP TTL, JWT expiry), `docs/PRD.md`/`docs/PRD.pdf` for product
   rules, the current implementation's own contract when no higher doc exists and the task scope
   doesn't include changing that behavior (see §6).
2. Temporarily break the corresponding production code (revert the specific line/condition under
   test, not the whole method) and rerun just that test class to confirm it actually fails red,
   not "passes trivially." This caught real gaps twice in this pass — e.g. `MemberPermissionsTest`
   initially had no case for "a Primary from a *different* account," so a deliberately broken
   same-account check still passed every existing test until that case was added.
3. Revert the break, rerun, confirm green.

This is worth the extra round trip specifically because it catches tests that compile and pass
but don't actually exercise the behavior they claim to.

## 6. When the documented spec and the current code disagree

`members` surfaced a real case: `CLAUDE.md`'s PRD-realignment note claims the 2-Secondary-Member
cap isn't enforced, defaults are narrower than all-on, and `SharingScope` has a 4th `location`
value — but reading the actual code showed all three already match `docs/PRD.pdf`. **Docs and
memory can go stale relative to the code; verify against the current code before trusting a
written claim about what it does.**

Where a real, current mismatch exists (not just a stale doc) — write tests against current code
behavior, not the aspirational target, unless realigning the behavior is explicitly the task.
Flag the mismatch in comments/summary rather than quietly "fixing" it as a side effect of a
restructuring/coverage task — behavior changes need their own explicit task and review.

## 7. Sequencing

Restructure + test one feature completely (package move → fix all cross-imports → compile →
relocate/fix existing tests → new unit tests → new controller slice tests → new repository slice
tests → entity gap-fill tests → expand the JaCoCo include list → full `verify`) before starting
the next. Don't restructure all packages first and backfill tests after — each feature should
leave the build green and the coverage gate passing before moving on.
