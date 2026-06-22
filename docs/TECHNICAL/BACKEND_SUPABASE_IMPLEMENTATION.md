# Backend Implementation Plan — Supabase Route

**Status: path not taken.** The actual backend (`backend/`) is custom Java/Spring Boot — see [`BACKEND_COMPARISON_AND_MIGRATION.md`](BACKEND_COMPARISON_AND_MIGRATION.md) for why. This document is kept for reference as a possible future migration target (self-hosted Supabase as a "halfway house," per that doc's §6), not as an active build plan.

Builds on the platform choice originally made in [`docs/TECHNICAL_REQUIREMENTS.md`](../TECHNICAL_REQUIREMENTS.md) (Supabase, region `ap-south-1`/Mumbai). That doc says **what** was decided; this doc explains **how to actually build it**, in plain language, with the implementation-level details that doc didn't cover.

**What Supabase is, in one line:** a managed Postgres database plus ready-made building blocks (login/auth, file storage, instant APIs, scheduled background functions) on top of it — so you get a real SQL database without running your own servers.

---

## 1. Architecture at a glance

| Bonaca needs | Supabase piece | Notes |
|---|---|---|
| Database | Postgres (managed) | Real SQL, same as any other Postgres — nothing proprietary here |
| Login / sessions | Auth (GoTrue) | Issues the login session + token after OTP is verified |
| "Who can see what" | Row Level Security (RLS) policies | Rules written directly on the database tables |
| Custom server logic (OTP send/verify, webhooks) | Edge Functions | Small serverless functions, written in TypeScript |
| Scheduled jobs (daily/weekly rollups) | `pg_cron` | A built-in scheduler inside Postgres itself |
| Avatar/file uploads | Storage | File buckets with their own access rules |
| Mobile app talks to all of this via | `supabase-js` client SDK | The app never connects to the database directly |

The mobile app **never** talks to Postgres directly — it always goes through Supabase's API layer (which enforces RLS) or an Edge Function. This is true for both this plan and the custom-backend plan; it's just a sound API design, not Supabase-specific.

---

## 2. Database schema & permissions (the most important part)

Bonaca's permission model is unusual: it's not "a user owns their own rows" (the easy case). It's **family sharing with scopes** — a Secondary Member is visible to a Primary Member only for the specific data scopes a `SharingGrant` actually permits. *(Per `docs/PRD.pdf` §11: 3 scopes — `vitals`/`activity`/`behaviour`, no separate `location` — all-on by default, and Primary/Secondary here mean the PDF's roles, parent/data-owner and adult-child/viewer respectively, not the inverted labels this doc's siblings were originally written against.)*

- Build the schema to mirror `src/types/index.ts` exactly: `accounts`, `members`, `wearable_connections`, `metric_readings`, `insights`, `subscriptions`, `sharing_grants`, `notifications`, `invites`.
- **Do not write raw RLS policies that re-check `sharing_grants` directly inside each table's policy.** A policy that joins back into a related RLS-protected table can hit Postgres's recursion-detection and fail outright.
  - **Fix**: write one helper function, `can_view(member_id, scope)`, marked `SECURITY DEFINER` (meaning it runs with elevated rights, bypassing RLS *inside the function only*). Every scoped table's policy just calls `can_view(member_id, 'vitals')` etc. One function to get right, reused everywhere.
- **A subtle performance trap**: writing `auth.uid()` directly inside a policy makes Postgres re-run that check on *every single row*. Always wrap it as `(select auth.uid())` instead — this is a documented 100x+ speedup on larger tables, and Supabase's own dashboard will warn you (the "RLS performance advisor") if you get this wrong.
- Index every column used inside a policy (especially the foreign keys joined in `can_view`) — RLS doesn't get free indexing just because it's a security feature.
- **Heads up, in plain terms**: Postgres RLS is fundamentally a *row*-level tool — it's good at "can this person see this row at all," less natural for "can this person see this *field* of this row but not that one" (which is closer to what `SharingGrant`'s scopes actually are). In practice this usually still works fine by splitting data into separate tables per scope (e.g. `vitals_readings` vs `location_readings` instead of one giant table), so each table's RLS check is a clean yes/no. Keep this in mind when designing the schema — don't try to force one mega-table with column-level secrecy.

---

## 3. Login & OTP (simpler than it first looks)

The original tech-requirements doc assumed you'd need to hand-build the whole OTP flow (custom Edge Functions that mint a session yourself via the Admin API). **There's a simpler, more "supported" way: Supabase's "Send SMS" Auth Hook.**

- Supabase's *native* phone-login flow (`signInWithOtp` / `verifyOtp`) already does the hard parts — generates the code, checks it, issues the session/token.
- The **Send SMS Hook** just lets you swap out *who actually sends the text message* — so MSG91 sends the SMS, but Supabase still owns the OTP generation/verification/session logic. This means far less custom code than originally planned.
- How it's wired: Supabase Dashboard → Auth Hooks → "Send SMS Hook" → point it at a small Edge Function → that function calls MSG91's API with your DLT-approved template.
- One limit worth knowing: this hook is available on the **Free and Pro plans**, not the higher Team/Enterprise tiers (not a concern for Bonaca's stage).
- DLT template registration with MSG91 (per `docs/TECHNICAL_REQUIREMENTS.md` §4) still has to happen first — that's an MSG91/TRAI compliance step, unrelated to which integration pattern you use.

---

## 4. Edge Functions — the custom-logic layer

Used for: the MSG91 SMS hook above, RevenueCat/Razorpay payment webhooks, wearable-data ingestion endpoint, and anything else needing custom server logic.

- Written in TypeScript, run on Deno (not Node — small syntax differences, e.g. imports work a bit differently).
- **Keep each function short-lived** — there's no persistent memory between calls, and Supabase recommends keeping execution under ~2 seconds, with a 256MB memory ceiling. Don't try to run a long batch job inside one; trigger a scheduled job instead (see below).
- Cold-start time (how long the *first* call takes after idle) is reported anywhere from near-instant to ~200-500ms depending on the source — plan for the slower end, it won't be noticeable to users but don't assume "always instant."
- Shared code (e.g. CORS helpers, shared types) goes in a `_shared/` folder so the CLI doesn't try to deploy it as its own function.
- Test locally with `supabase functions serve` against a local Supabase instance (`supabase start`) before deploying — same pattern used in CI (§7 below).

---

## 5. Scheduled rollups (`pg_cron`)

Per `docs/TECHNICAL_REQUIREMENTS.md` §3, Metric Details' 1Y view needs pre-aggregated data, not raw rows summed on every request.

- `pg_cron` is a Postgres extension you enable in the Supabase dashboard — it's a scheduler that lives *inside* the database itself.
- Pattern: write a Postgres function that does the aggregation (`INSERT ... ON CONFLICT DO UPDATE` from `metric_readings` into `metric_daily_rollups`/`metric_weekly_rollups`), then schedule a one-line call to that function — don't write the aggregation SQL directly into the schedule string.
- **Always check `cron.job_run_details`** after setting this up — pg_cron does not loudly tell you when a job silently fails, you have to go look.

---

## 6. Realtime — skip it for MVP

Supabase has a live-update feature (Realtime) that could push instant UI updates (e.g. "last synced 2 min ago" ticking live). **Recommendation: don't use it for MVP.**
- Nothing in the PRD actually requires live in-app updates — actual alerts go through Expo Push notifications (already decided), which is a completely separate, simpler mechanism.
- Adding Realtime now would be solving a problem nobody asked for yet — consistent with the project's existing "don't over-architect" principle (same reasoning that ruled out TimescaleDB).
- If a real need shows up later, it's a small addition, not a redesign.

---

## 7. File storage (avatars, etc.)

- Buckets default to **deny-all** — you must write explicit access rules, same idea as RLS but for files.
- A "public" bucket only means public *reads* — uploads/deletes are still rule-gated.
- Standard pattern for avatars: each member can only upload into a folder named after their own ID (`storage.foldername()` helper makes this easy to check in a policy).

---

## 8. Connections — don't let the mobile app touch Postgres directly

- Supabase's connection pooler (Supavisor) has two modes: **session mode** (port 5432) and **transaction mode** (port 6543, built for serverless/bursty traffic — use this one for Edge Functions).
- The mobile app should only ever talk to the Supabase API (which goes through RLS) or to Edge Functions — never opens a raw database connection. This is already the natural way `supabase-js` works, so it's hard to get wrong by accident.

---

## 9. Migrations, types, and CI

- Workflow: `supabase migration new <name>` → write the SQL → `supabase db push` (run this from CI, not by hand, once it matters).
- Generate TypeScript types straight from the schema: `supabase gen types typescript` → save into `src/types/database.ts`. Keep this **separate** from the hand-written domain types in `src/types/index.ts` — one describes the database, the other describes the product's concepts; both are useful.
- GitHub Actions: Supabase's own `supabase/setup-cli` action + three repo secrets (`SUPABASE_ACCESS_TOKEN`, `SUPABASE_DB_PASSWORD`, `SUPABASE_PROJECT_ID`) is enough to run `supabase db push` automatically on merge.

---

## 10. What this actually costs

| Plan | Good for | Roughly includes |
|---|---|---|
| Free | Building/testing only | 500MB database, 1GB file storage, 50K monthly logins — **but the project auto-pauses after a week of inactivity**, so it's not usable for anything real users touch |
| **Pro — $25/mo** | First real users onward | 8GB database, 100GB storage, 100K monthly logins, 2M Edge Function calls/month |

At Bonaca's likely early scale (a few hundred families × a handful of members each), **Pro covers everything comfortably** — no surprise overage risk in the first stretch of real usage.

---

## 11. What's portable later vs. what's Supabase-specific

This matters because of the planned eventual move to a custom backend — see [`BACKEND_COMPARISON_AND_MIGRATION.md`](BACKEND_COMPARISON_AND_MIGRATION.md) for the full playbook. Quick summary:

| Piece | Portable? |
|---|---|
| The actual database + schema | ✅ Fully — it's just Postgres, `pg_dump` always works |
| RLS policies | ✅ Fully — plain Postgres SQL, no Supabase magic |
| Login/auth (GoTrue) | ⚠️ Portable to *self-hosted* Supabase, not to a hand-built auth system without rework |
| Edge Functions | ⚠️ Same — portable to self-hosted Supabase as-is, need rewriting for a non-Supabase backend |
| File storage | ⚠️ Same pattern |
| Realtime (if ever used) | ❌ Not portable — Supabase-specific, would need replacing |

**Bottom line**: building on Supabase now does not lock the data in — it locks in the *auth/functions/storage glue code*, which is a real but bounded amount of rework later, not a rewrite from zero.

---

## Sources
- [Supabase RLS recursion debugging](https://lindanthillanayagam.substack.com/p/row-level-security-recursion-a-debugging)
- [Supabase RLS in production patterns](https://dev.to/whoffagents/supabase-row-level-security-in-production-patterns-that-actually-work-2l78)
- [RLS auth.uid() performance trap](https://vibeappscanner.com/supabase-row-level-security)
- [76 RLS policies rewritten — init plan trap](https://dev.to/arvavit/76-rls-policies-rewritten-in-one-migration-the-authuid-init-plan-trap-in-supabase-4hg)
- [Supabase RLS best practices](https://makerkit.dev/blog/tutorials/supabase-rls-best-practices)
- [Supabase Row Level Security docs](https://supabase.com/docs/guides/database/postgres/row-level-security)
- [Send SMS Hook docs](https://supabase.com/docs/guides/auth/auth-hooks/send-sms-hook)
- [Implementing custom SMS auth with MSG91 — Medium](https://medium.com/@shreebhagwat94/implementing-custom-sms-authentication-in-supabase-using-sms-hook-and-msg91-366d13acc81c)
- [Supabase + MSG91 in India — DEV Community](https://dev.to/acetrondi/using-supabase-sms-hook-to-send-custom-authentication-messages-in-india-4nj7)
- [Edge Functions architecture](https://supabase.com/docs/guides/functions/architecture)
- [Edge Functions production guide](https://dev.to/kanta13jp1/supabase-edge-functions-in-deno-a-production-guide-5d95)
- [Managing Edge Function dependencies](https://supabase.com/docs/guides/functions/dependencies)
- [pg_cron docs](https://supabase.com/docs/guides/database/extensions/pg_cron)
- [pg_cron debugging guide](https://supabase.com/docs/guides/troubleshooting/pgcron-debugging-guide-n1KTaz)
- [Realtime: Postgres Changes vs Broadcast](https://dev.to/kanta13jp1/supabase-realtime-postgres-changes-presence-and-broadcast-4fkd)
- [Storage access control](https://supabase.com/docs/guides/storage/security/access-control)
- [Storage helper functions](https://supabase.com/docs/guides/storage/schema/helper-functions)
- [Supavisor connection terminology](https://supabase.com/docs/guides/troubleshooting/supavisor-and-connection-terminology-explained-9pr_ZO)
- [Managing environments / CI](https://supabase.com/docs/guides/deployment/managing-environments)
- [Generating types via GitHub Actions](https://supabase.com/docs/guides/deployment/ci/generating-types)
- [Supabase pricing 2026 — UI Bakery](https://uibakery.io/blog/supabase-pricing)
- [Supabase self-hosting docs](https://supabase.com/docs/guides/self-hosting)
- [Self-hosted deployment overview](https://deepwiki.com/supabase/supabase/3-self-hosted-deployment)
- [Supabase vendor lock-in analysis](https://www.hrekov.com/blog/supabase-vendor-lock-in-myth)
