# Aque Backend

## What This Is

Aque Backend is the Spring Boot 3 (Java 25) REST API powering **aque**, a personal finance app for a single household/user. It serves the `aque-web` Angular SPA over `/api`, backed by PostgreSQL with Flyway-managed schema, and runs in production on a Raspberry Pi 3B.

## Core Value

Track and split personal/household expenses (transactions, categories, people, recurring transactions, split rules) reliably, with data that's always correct — this is a money-tracking tool, so correctness of balances and splits matters more than anything else.

## Current Milestone: v1.0 Tech Debt & Security Cleanup

**Goal:** Close out the 7 issues filed from the `/gsd-onboard` codebase audit — the seeded-credential security gap, two fragile-area regression tests, and coverage/dependency/cleanup debt.

**Target features:**
- Rotate/verify the seeded admin credential (security)
- Lock in the two fragile areas (split remainder logic, split-rule save ordering) with regression tests
- Fill the identified test-coverage gaps (auth path, low-risk CRUD services)
- Clear the small cleanup/dependency items (duplicated query method, SNAPSHOT dependency, missing CORS config)

## Requirements

### Validated

- ✓ JWT-based stateless authentication (single seeded admin user) — existing
- ✓ Transaction CRUD with category and person association — existing
- ✓ Category management — existing
- ✓ Person management (for expense splitting) — existing
- ✓ Recurring transactions with monthly scheduled generation (`RecurringTransactionJob`) — existing
- ✓ Split rules for dividing expenses across people — existing
- ✓ Dashboard/aggregation endpoints (including split calculation) — existing
- ✓ OpenAPI/Swagger docs at `/api/swagger-ui.html` — existing
- ✓ Flyway-managed PostgreSQL schema, Testcontainers-backed test suite — existing

### Active

<!-- Milestone v1.0: tech-debt/security cleanup, scoped from GitHub issues #7-#13 -->

- [ ] Seeded admin credential is rotated/verified after first deploy — GH #7
- [ ] SplitRuleService update (membership change) has a regression test locking the flush-ordering behavior — GH #8
- [ ] AuthService and CustomUserDetailsService have dedicated unit tests — GH #9
- [ ] `loghub-logger` is pinned to a stable release or removed if unused — GH #10
- [ ] Explicit CORS config exists for future split-origin deploys — GH #11
- [ ] Duplicated `findByActive`/`findByActiveTrue` query methods are collapsed — GH #12
- [ ] PersonService/CategoryService/RecurringTransaction* have dedicated unit tests — GH #13

### Out of Scope

- Multi-user / multi-tenant ownership model — single-admin design is intentional per existing `CLAUDE.md`; would require `owner_id` on all domain tables plus authorization rework if ever revisited
- Role-based authorization — no second, lower-privilege user exists; not needed while single-admin holds

## Context

- Brownfield onboarding via `/gsd-onboard` on 2026-08-01. Full codebase map at `.planning/codebase/` (STACK, ARCHITECTURE, STRUCTURE, CONVENTIONS, TESTING, INTEGRATIONS, CONCERNS).
- Sibling repo `aque-web` (Angular 21 SPA) is the only consumer of this API; the two are independent git repos under the same parent directory, no monorepo tooling, coordinated only via `aque-web/proxy.conf.json` in dev.
- No external integrations detected (no outbound HTTP calls, file storage, caching, or error tracking) — a `io.loghub:loghub-logger` dependency is declared but has no call sites.
- Deploys via GitHub Actions building images pulled by a Raspberry Pi 3B (see `../fluxo-deploy-aque.md`).
- Known tech debt (see `.planning/codebase/CONCERNS.md`): no ownership model (by design for single-user use), duplicated `RecurringTransactionRepository` query methods, manual rounding-remainder logic in `DashboardService.getSplit`, bleeding-edge Spring Boot 4.0.0 / Java 25 pairing.

## Constraints

- **Tech stack**: Spring Boot 3, Java 25, Maven, PostgreSQL + Flyway (`ddl-auto=validate` — schema changes go through migrations, never Hibernate auto-DDL) — established, not up for debate without a migration plan
- **Hardware**: Production runs on a Raspberry Pi 3B — JVM/Hikari tuning must respect constrained CPU/RAM
- **Security**: `JWT_SECRET` has no default in shared/prod config — boot fails fast if unset; keep it that way

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Single-admin, no ownership model | Personal/household finance app, not multi-tenant SaaS | ✓ Good — matches actual usage |
| Flyway over Hibernate auto-DDL | Predictable, reviewable schema changes in production | ✓ Good |
| Stateless JWT, no sessions | Simple REST API consumed by one SPA | ✓ Good |

---
*Last updated: 2026-08-01 after starting milestone v1.0*

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state
