# Backend Implementation Plan — Custom (Java + Spring Boot)

A from-scratch backend, not built on Supabase — either as the eventual migration target, or as the primary path if chosen instead. **Recommended stack: Java + Spring Boot**, given the team's existing Java background (see reasoning in §2). Node.js was also researched as an alternative and is noted in §9 for completeness.

**What "custom backend" means here, in one line:** you run your own server and database instead of using a managed platform — full control, but you build and maintain everything Supabase would otherwise hand you ready-made.

---

## 1. Recommended stack at a glance

| Layer | Choice | Why |
|---|---|---|
| Language/framework | **Java 21 + Spring Boot 3** | Matches existing team expertise; virtual threads close the old I/O-performance gap with Node |
| Database | PostgreSQL | Same database either way — RLS and SQL skills carry over from the Supabase path |
| Authorization | Spring Security method-level checks (`@PreAuthorize`), Postgres RLS as optional backup | See §4 — this is the one place Java has a genuine edge over Node for Bonaca's exact permission shape |
| Auth tokens | JWT (access + refresh, rotation) | Standard, well-understood pattern |
| Scheduled jobs | Spring `@Scheduled` / Spring Batch | Java's built-in equivalent of `pg_cron` |
| File storage | Cloudflare R2 | Cheapest option with zero egress fees, S3-compatible |
| Hosting | Fly.io, Mumbai region (`bom1`) | Only PaaS-grade host with a real India region — matches AWS-level latency at lower setup complexity |
| Architecture shape | **Modular monolith** (one deployable app, clean internal module boundaries) | Not microservices — see §3 |

---

## 2. Why Java + Spring Boot over Node.js (brief)

- **Switching cost is the dominant factor at this team size.** A team that already knows Java has effectively zero ramp-up cost; learning Node well enough to be equally productive is a real, ongoing tax — not a one-time cost.
- **The old "Node wins at I/O concurrency" argument is weaker now.** Java 21's virtual threads let one service handle huge numbers of concurrent requests (DB calls, calling MSG91/RevenueCat/Expo Push) without the complexity of reactive programming that used to be Java's answer to this. Bonaca's workload is exactly this kind of I/O-bound work, not CPU-heavy computation.
- **India hiring**: Node has the broadest pool, but Java/Spring Boot has a deep, well-established one — especially strong in enterprise/fintech and Mumbai specifically. The same "a future hire already knows this" logic the Supabase decision used for Postgres applies equally to Java.
- **Postgres RLS works the same way regardless of language** (see §4) — no technical disadvantage to Java there.

---

## 3. Architecture: modular monolith, not microservices

The idea of splitting into multiple services by language was considered and **rejected for now**:

- For teams under ~5 engineers, the 2026 consensus is unambiguous: a modular monolith (one deployable app with clean internal boundaries) beats microservices. Microservices add real, ongoing cost — service-to-service contracts, multiple deploy pipelines, duplicated monitoring/DevOps per service — for benefits (independent scaling, fault isolation) that only pay off at a scale Bonaca isn't at yet.
- Instead, organize the single Spring Boot app into clearly separated packages by business area:
  - `auth` — OTP request/verify, JWT issuance
  - `members` / `sharing` — Account, Member, SharingGrant
  - `metrics` — wearable ingestion, MetricReading, rollups
  - `subscriptions` — billing state, RevenueCat webhook handling
  - `notifications` — Insight generation, Expo Push dispatch
- This costs nothing extra now and makes a *real* future split (if one module's load genuinely outgrows the rest) a clean extraction instead of a rewrite.
- **One legitimate future exception**: if the PRD's rule-based anomaly-confidence scoring ever becomes an actual ML model, that specific piece is a reasonable candidate for a small separate Python service the Java app calls — a narrow, justified exception, not a default pattern.

---

## 4. Authentication & "who can see what"

### Login (OTP)
- `POST /auth/request-otp` — generate a code, store its **hash** (never plaintext) with an expiry, call MSG91 directly.
- `POST /auth/verify-otp` — check the hash, on success issue a short-lived access token (~15 min) and a refresh token.
- **Refresh token rotation**: every time a refresh token is used, issue a new one and invalidate the old — so a stolen old token becomes useless after the next legitimate refresh. Store only a hash of the refresh token server-side, same principle as the OTP code.
- This is well-trodden, low-risk territory — the work here is writing and testing it yourself (rate-limiting, expiry, revocation-on-logout), not solving anything novel.

### Authorization (the `SharingGrant` permission model)
Bonaca's actual need — "can Member X see Member Y's vitals data" — is closer to *field-level* visibility than simple row ownership. Two complementary tools:

1. **Primary: Spring Security method-level checks** (`@PreAuthorize("@permissions.canView(#memberId, 'vitals')")` style, on each service method). This maps naturally onto "does this request have permission to do this specific thing" — arguably a *better* fit than database-level RLS for Bonaca's scope-based model, and it's where a real team (see migration doc) ended up putting their authorization logic anyway after trying RLS-first.
2. **Optional: Postgres RLS as defense-in-depth**, using the same approach as the Supabase path — a session variable set per request (`SET LOCAL app.member_id = '...'`, inside an explicit transaction) that policies check via `current_setting()`. Wire this through a Spring interceptor that runs before each request.
   - **Important gotcha if using a connection pool (PgBouncer) in transaction mode**: a plain `SET` (not `SET LOCAL`) can leak across pooled connections and silently leak one user's data to another under concurrent load. Always use `SET LOCAL` wrapped in a transaction — never bare `SET`.

**Recommendation**: start with Spring Security checks as the real enforcement layer; add RLS only if you want belt-and-braces protection, not as the primary mechanism.

---

## 5. Scheduled rollups

Java's equivalent of Supabase's `pg_cron`:
- **Spring `@Scheduled`** for simple, low-stakes periodic jobs.
- **Spring Batch** if the rollup job needs proper retry/recovery semantics (recommended for the actual `metric_daily_rollups`/`metric_weekly_rollups` job, since silent failures there directly affect what a user sees on Metric Details).
- No extra infrastructure needed — both run inside the same Java application, same principle as `pg_cron` running inside Postgres.

---

## 6. Realtime — skip for MVP (same call as the Supabase plan)

Nothing in the PRD needs live in-app updates; Expo Push already covers real notifications. If a genuine need shows up later, **Postgres's built-in `LISTEN`/`NOTIFY`** plus a small WebSocket layer is the lowest-dependency option — no new infrastructure required, consistent with "go custom" meaning *less* incidental complexity, not more.

---

## 7. File storage

**Cloudflare R2** is the recommended choice — about a third cheaper per GB than AWS S3, and critically, **zero egress fees** (no surprise bandwidth bill as usage grows). It speaks the same API as S3, so switching providers later is low-friction if ever needed.

---

## 8. Hosting & everything Supabase would otherwise hand you

| Concern | Recommended approach | Effort |
|---|---|---|
| Hosting | **Fly.io, `bom1` Mumbai region** — the only PaaS-grade host with a true India region (5-15ms latency, matching AWS `ap-south-1`) | Low-medium; Docker-based, CLI-first |
| Database | Fly Postgres (managed) or AWS RDS `ap-south-1` as fallback | Low if using a managed option |
| Connection pooling | Self-hosted PgBouncer (one Docker container) | Medium — this is the trickiest piece to get right alongside RLS (§4) |
| Migrations | A schema migration tool (e.g. Flyway, which is Spring Boot's standard pairing) | Low — standard part of the workflow once set up |
| Backups | Handled by the managed-Postgres host if used; manual `pg_dump` + off-site storage if self-hosting Postgres | Low if managed, real effort if not |
| Monitoring | Start simple — a hosted option (e.g. Better Stack) rather than self-running Prometheus/Grafana, until team/scale justifies it | Low-medium |
| Secrets | **Doppler** (free for small teams) | Low |
| Zero-downtime deploys | Native on Fly.io via rolling deploys | Low if on a PaaS host |

This table is the honest answer to "what does Supabase actually save you" — every row here is something Supabase already does for you out of the box.

---

## 9. What it costs, and the Node.js alternative (for reference)

**Rough monthly cost** at a few hundred to ~2,000 users, using the stack above: **$25-80/month** — directionally similar to Supabase Pro's flat $25/month, not meaningfully cheaper at this scale. The custom path's cost advantage (if any) only shows up at much higher scale, where Supabase's usage-based overages start exceeding a more controllable custom setup.

**On Node.js** (researched as the alternative, not recommended given the team's Java background): if chosen instead, **Hono** or **Fastify** would be the right framework picks for a solo engineer (lighter-weight than NestJS, which is better suited once a team grows past ~3 engineers); the auth/RLS/hosting/storage recommendations above are identical regardless of language — only the framework-level code changes. Worth knowing in case a future hire's background ever tips the language choice the other way.

---

## 10. Realistic time-to-build vs. Supabase

No hard benchmark exists for this exact comparison, but as a planning assumption: expect **roughly 2-4x the initial backend setup time** versus Supabase for equivalent functionality (auth, authorization wiring, connection pooling, migrations, monitoring) — concentrated entirely in *setup*, not in ongoing feature work afterward. Once the foundation is built, day-to-day development speed should be comparable.

---

## Sources
- [Spring Boot vs Node.js 2026 — Brilworks](https://www.brilworks.com/blog/node-js-vs-spring-boot/)
- [Spring Boot vs Node.js for startups — Webyot](https://webyot.in/learning/spring-boot-vs-nodejs.html)
- [Spring Boot vs Node.js 2026 — TheLinuxCode](https://thelinuxcode.com/spring-boot-vs-nodejs-which-one-should-you-choose-in-2026/)
- [Multi-tenant Postgres RLS with Spring Boot 3 — Medium](https://medium.com/@priyaranjanpatraa/multi-tenant-the-safe-way-postgresql-row-level-security-rls-with-spring-boot-3-4132a4d142fa)
- [Multi-tenancy Spring Boot + RLS reference repo](https://github.com/wenqiglantz/multi-tenancy-spring-boot)
- [Spring Boot multitenancy with Postgres RLS — ByteFish](https://www.bytefish.de/blog/spring_boot_multitenancy_using_rls.html)
- [Node.js vs Java vs Go enterprise backend 2026 — WorkforceNext](https://workforcenext.in/blog/nodejs-vs-java-vs-go-enterprise-backend-2026/)
- [Hire Node.js developers from India 2026 — WorkforceNext](https://workforcenext.in/blog/hire-nodejs-developers-from-india-2026/)
- [Monolith vs microservices 2026 decision framework — DistantJob](https://distantjob.com/blog/monolith-vs-microservices/)
- [Rethinking microservices in 2026 — Enqcode](https://enqcode.com/blog/rethinking-microservices-in-2026-when-modular-monolith-architecture-actually-win)
- [Postgres RLS implementation guide — Permit.io](https://www.permit.io/blog/postgres-rls-implementation-guide)
- [PgBouncer / SET LOCAL gotcha](https://dev.to/jacksonkasi/comment/36bi0)
- [Auth.js refresh token rotation](https://authjs.dev/guides/refresh-token-rotation)
- [Cloudflare R2 vs AWS S3 cost comparison](https://r2drop.com/blog/cloudflare-r2-vs-aws-s3-cost-comparison)
- [Fly.io Mumbai region review](https://productgrowth.in/tools/developer/fly-io/)
- [AWS RDS India pricing](https://www.itforsme.in/pricing/aws-rds-india/)
- [PgBouncer self-hosted setup](https://dev.to/whoffagents/pgbouncer-database-connection-pooling-that-actually-scales-4ek4)
- [Secrets management tools compared 2026](https://guptadeepak.com/top-5-secrets-management-tools-hashicorp-vault-aws-doppler-infisical-and-azure-key-vault-compared/)
- [Encore.dev — NestJS vs Fastify vs Hono](https://encore.dev/articles/nestjs-vs-fastify-vs-hono)
