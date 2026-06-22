# Backend: Supabase vs. Custom — Comparison, Delegation, and Migration Plan

Ties together [`BACKEND_SUPABASE_IMPLEMENTATION.md`](BACKEND_SUPABASE_IMPLEMENTATION.md) and [`BACKEND_CUSTOM_IMPLEMENTATION.md`](BACKEND_CUSTOM_IMPLEMENTATION.md). Answers three questions in plain language: which is easier for what, which is better overall right now, and exactly how to move from one to the other when the time comes.

---

## 1. The short answer

- **Start on Supabase.** It gets a real product in front of real users fastest, with the least setup work, for a solo engineer.
- **Build it in a way that doesn't lock you in** — this doc explains exactly what that means (§3).
- **Migrate later, not because Supabase breaks, but on your own schedule** — once the team is bigger and "full control" starts mattering more than "fastest to ship."

This matches what [`docs/TECHNICAL_REQUIREMENTS.md`](../TECHNICAL_REQUIREMENTS.md) already recommended — this doc adds the *how* and *when* for the migration half of that decision.

---

## 2. Side-by-side comparison

| | Supabase | Custom (Java + Spring Boot) |
|---|---|---|
| Time to first working backend | Fast — auth, database, file storage all ready-made | Slower — you build auth, permission-checking, and job scheduling yourself |
| Who maintains the server | Supabase | You |
| Cost at Bonaca's early scale | ~$25/month (Pro plan) | ~$25-80/month — similar, not cheaper, at this size |
| Database | Real Postgres | Real Postgres (identical either way) |
| "Who can see what" logic | Row Level Security (database-enforced rules) | Spring Security checks in code (app-enforced), optionally backed by RLS too |
| Best fit for the team today | Solo engineer, need to move fast | Once there's a team to own ongoing infrastructure |
| Lock-in | Auth, file storage, and serverless-function code are Supabase-specific; the database and its rules are not | None — but you own everything, including the parts Supabase would've handled |

---

## 3. What to delegate to Supabase vs. build custom — a hybrid view

You don't have to pick one path for *everything* on day one. Some pieces are easy to delegate; others are worth building carefully even while on Supabase, because they're the pieces you'll keep either way.

**Lean on Supabase for (low value in building yourself):**
- Login session management, token issuing, password/OTP plumbing
- File storage and its access rules
- Hosting/patching/scaling the database server itself
- Day-to-day backups

**Build carefully regardless of platform (these survive any future migration almost unchanged):**
- The database schema and table design — this is just Postgres either way
- Row Level Security policies — plain SQL, fully portable
- The actual business logic (OTP request/verify flow shape, rollup calculation logic, anomaly-scoring rules) — write this as plain, framework-light functions where possible, so porting them later is closer to copy-paste than rewrite

**Avoid building custom unless/until you have a specific reason:**
- A second backend service in a different language ("microservices") — not worth the operational cost yet (see the custom-implementation doc, §3)
- Live/realtime updates beyond push notifications — nothing in the product needs this yet on either platform

---

## 4. Which is "better"? — it depends on what you're optimizing for

- **Optimizing for speed to first users**: Supabase, clearly.
- **Optimizing for long-term cost at large scale**: directionally custom, but the crossover point is well beyond Bonaca's current scale — not a near-term concern.
- **Optimizing for "my team already knows this tech"**: custom (Java), if and when there's a team. Today, with one engineer, Supabase's ready-made auth/storage/hosting outweighs that.
- **Optimizing for data control / no vendor dependency**: custom — but as §6 shows, this is achievable as a *later* migration, not something you need to sacrifice speed for today.

**Recommendation stands: Supabase now, custom later, when the "later" actually arrives — not before.**

---

## 5. A real example of this exact migration

A team called **Val Town** built on Supabase and later migrated off it to a custom Postgres + Node setup. Their experience is the closest real-world data point available, and it's worth knowing both the good and bad parts honestly:

- **What went wrong for them on Supabase**: their local development setup was unreliable, scheduled backups briefly took their database offline, and — the scariest part — a storage resize failure once left their ~40GB database stuck in **read-only mode** at 95% capacity, with no easy way out.
- **A finding directly relevant to Bonaca**: Postgres Row Level Security can't restrict access at the *column* level, only the *row* level — they hit this limitation and worked around it by splitting one table into three. This is the exact same RLS limitation called out in both implementation docs for Bonaca's scope-based `SharingGrant` model — so it's a known, real constraint, not a theoretical one.
- **How the actual migration went**: their most critical, live data moved over in about **25 minutes**; the rest (80% of their data, non-urgent) migrated gradually in the background over several nights. Total data-transfer cost: **$6**.
- **What they changed after migrating**: they removed RLS and database-level triggers entirely, moving that logic into the application instead — and said this *improved* both their development speed and ability to ship features. This directly supports the custom-implementation doc's recommendation to make Spring Security checks the primary authorization layer, not RLS.

**Takeaway**: a real migration, done deliberately, is a days-to-weeks project, not a months-long rewrite — *if* the groundwork in §3 was followed.

---

## 6. The actual migration playbook

When the time comes, this is the order of operations, from least to most work:

### Step 1 — Move the data (easy)
Postgres is Postgres. `pg_dump` the Supabase database, restore it into the new server. This is the part Val Town did in ~25 minutes for their critical tables.

### Step 2 — Decide: self-hosted Supabase, or fully custom?
This is the most important fork in the migration, and it's worth pausing on:
- **Self-hosted Supabase** (running Supabase's own open-source pieces — Postgres, Auth, file storage, serverless functions — yourself, e.g. via Docker) keeps almost everything as-is: your RLS policies, your serverless-function code, even your app's API calls barely change. This is a **much smaller step** than it sounds — you're not rewriting anything, just hosting the same software yourself instead of paying Supabase to host it.
- **Fully custom (Java + Spring Boot)** is the bigger step — described below — but gives full control and lets you fold the backend into one Java codebase instead of running Supabase's stack alongside it.

Either is a legitimate stopping point; self-hosted Supabase is the natural "halfway house" if full control isn't actually the goal, just independence from the managed-cloud cost/limits.

### Step 3 — If going fully custom, rebuild these pieces (in order of effort)
1. **Auth** — re-implement OTP request/verify + JWT issuing in Spring Boot (§4 of the custom-implementation doc). Since Bonaca's auth is phone+OTP only (no passwords to migrate), this is simpler than a typical auth migration — there's no password-hash compatibility problem to solve.
2. **Authorization** — port the `can_view`-style permission checks into Spring Security method-level checks. If RLS was used as a backing layer, it can stay (RLS is just Postgres) or be retired in favor of app-only checks, per the Val Town finding above.
3. **Serverless functions → API endpoints** — Supabase Edge Functions are written in TypeScript; the actual logic inside them (OTP sending, webhook handling, ingestion) ports conceptually easily even though the language changes to Java — it's the same steps, different syntax.
4. **File storage** — move files from Supabase Storage to the new choice (Cloudflare R2 per the custom-implementation doc); both are S3-compatible, so this is a bulk-copy operation, not a redesign.
5. **Scheduled jobs** — replace `pg_cron` jobs with Spring `@Scheduled`/Spring Batch equivalents doing the identical SQL aggregation.

### Step 4 — Cut over
Run both systems in parallel briefly if possible (point a test build of the app at the new backend first), then switch the production app over. Keep the old Supabase project intact and paused (not deleted) for a safety window after cutover.

---

## 7. When should this migration actually happen?

Not on a fixed date — on these signals:
- The team has grown enough that someone can own backend infrastructure full-time (the main thing Supabase was covering for a solo engineer).
- A specific Supabase limit becomes a real, recurring problem (cost at scale, a feature gap, the column-level RLS limitation above).
- There's a concrete reason "more control" now matters more than "less to maintain" — not a preference in the abstract.

Until one of those is true, the right move is to keep building on Supabase and let this document wait.

---

## Sources
- [Val Town's Supabase migration writeup](https://blog.val.town/blog/migrating-from-supabase/)
- [Supabase vendor lock-in analysis](https://www.hrekov.com/blog/supabase-vendor-lock-in-myth)
- [Supabase self-hosting docs](https://supabase.com/docs/guides/self-hosting)
- [Self-hosted deployment overview](https://deepwiki.com/supabase/supabase/3-self-hosted-deployment)
- See also the source lists in [`BACKEND_SUPABASE_IMPLEMENTATION.md`](BACKEND_SUPABASE_IMPLEMENTATION.md) and [`BACKEND_CUSTOM_IMPLEMENTATION.md`](BACKEND_CUSTOM_IMPLEMENTATION.md)
