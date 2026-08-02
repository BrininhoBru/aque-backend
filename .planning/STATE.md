---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: Tech Debt & Security Cleanup
current_phase: 01
current_phase_name: Security & Deploy Hardening
status: executing
stopped_at: Completed 01-01-PLAN.md
last_updated: "2026-08-02T12:37:49.477Z"
last_activity: 2026-08-02
last_activity_desc: Phase 01 execution started
progress:
  total_phases: 1
  completed_phases: 0
  total_plans: 2
  completed_plans: 1
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-08-01)

**Core value:** Track and split personal/household expenses reliably, with correct balances and splits above all else.
**Current focus:** Phase 01 — Security & Deploy Hardening

## Current Position

Phase: 01 (Security & Deploy Hardening) — EXECUTING
Plan: 2 of 2
Status: Ready to execute
Last activity: 2026-08-02 — Phase 01 execution started

Progress: [█████░░░░░] 50%

## Performance Metrics

**Velocity:**

- Total plans completed: 0
- Average duration: —
- Total execution time: —

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 1. Security & Deploy Hardening | 0 | — | — |
| 2. Dependency & Repository Cleanup | 0 | — | — |
| 3. Test Coverage Lock-In | 0 | — | — |

**Recent Trend:**

- Last 5 plans: —
- Trend: —

*Updated after each plan completion*
**Per-Plan Metrics:**

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 01 P01 | 6min | 2 tasks | 5 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.

- Onboarding: single-admin, no ownership model — matches actual usage
- Onboarding: Flyway over Hibernate auto-DDL — predictable prod schema changes
- Roadmap: 3 phases grouped by kind of work (security, cleanup, tests) rather than one phase per issue — coarse granularity, all 7 items are small and independent
- Roadmap: Phase 3 (tests) runs after Phase 2 (cleanup) so recurring-transaction tests target the collapsed query method, not the one about to be deleted
- [Phase ?]: 01-01: Empty default app.cors.allowed-origins preserves today's same-origin behavior; config-only change needed for future split-origin deploy
- [Phase ?]: 01-01: chmod +x mvnw — wrapper script was not executable, blocking all test runs (Rule 3 blocking-issue fix)

### Pending Todos

- Each phase plan must close its GitHub issues on completion (Phase 1: #7, #11 — Phase 2: #10, #12 — Phase 3: #8, #9, #13)

### Blockers/Concerns

- SEC-01 (#7) requires access to the deployed Raspberry Pi environment to actually rotate the credential — code/doc changes alone don't satisfy it
- DEBT-01 (#10) resolution depends on whether `loghub-logger` has a stable release; if not, removal is the fallback (it currently has no call sites)
- No ownership/multi-user model (intentional, see CONCERNS.md) — out of scope for v1.0
- Bleeding-edge Spring Boot 4.0.0 / Java 25 pairing — track security advisories
- Pre-existing unrelated failure: DashboardServiceTest.getSplit_ultimoItemAbsorveORestoDoArredondamento NPE on full ./mvnw test run — tracked in deferred-items.md and WINDOWS.md, out of scope for phase 01

## Deferred Items

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| Security | httpOnly-cookie JWT storage / token revocation list | Out of scope for v1.0 | 2026-08-01 |
| Architecture | Multi-user ownership model, role-based authz | Out of scope (by design) | 2026-08-01 |

## Session Continuity

Last session: 2026-08-02T12:37:49.467Z
Stopped at: Completed 01-01-PLAN.md
Resume file: None
Next action: `/gsd-plan-phase 1`
