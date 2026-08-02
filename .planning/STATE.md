---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: Tech Debt & Security Cleanup
current_phase: 1
status: roadmapped
stopped_at: Roadmap created for milestone v1.0 (3 phases, 7/7 requirements mapped)
last_updated: "2026-08-02T11:58:40.902Z"
last_activity: 2026-08-01
last_activity_desc: ROADMAP.md created for milestone v1.0
progress:
  total_phases: 1
  completed_phases: 0
  total_plans: 2
  completed_plans: 0
  percent: 0
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-08-01)

**Core value:** Track and split personal/household expenses reliably, with correct balances and splits above all else.
**Current focus:** Milestone v1.0 roadmapped into 3 phases — next up is Phase 1 (Security & Deploy Hardening).

## Current Position

Phase: 1 - Security & Deploy Hardening (not started)
Plan: —
Status: Roadmap complete, awaiting phase planning
Last activity: 2026-08-01 — ROADMAP.md created for milestone v1.0

Progress: [ ] [ ] [ ] — 0/3 phases complete

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

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.

- Onboarding: single-admin, no ownership model — matches actual usage
- Onboarding: Flyway over Hibernate auto-DDL — predictable prod schema changes
- Roadmap: 3 phases grouped by kind of work (security, cleanup, tests) rather than one phase per issue — coarse granularity, all 7 items are small and independent
- Roadmap: Phase 3 (tests) runs after Phase 2 (cleanup) so recurring-transaction tests target the collapsed query method, not the one about to be deleted

### Pending Todos

- Each phase plan must close its GitHub issues on completion (Phase 1: #7, #11 — Phase 2: #10, #12 — Phase 3: #8, #9, #13)

### Blockers/Concerns

- SEC-01 (#7) requires access to the deployed Raspberry Pi environment to actually rotate the credential — code/doc changes alone don't satisfy it
- DEBT-01 (#10) resolution depends on whether `loghub-logger` has a stable release; if not, removal is the fallback (it currently has no call sites)
- No ownership/multi-user model (intentional, see CONCERNS.md) — out of scope for v1.0
- Bleeding-edge Spring Boot 4.0.0 / Java 25 pairing — track security advisories

## Deferred Items

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| Security | httpOnly-cookie JWT storage / token revocation list | Out of scope for v1.0 | 2026-08-01 |
| Architecture | Multi-user ownership model, role-based authz | Out of scope (by design) | 2026-08-01 |

## Session Continuity

Last session: 2026-08-01
Stopped at: Roadmap created for milestone v1.0 (3 phases, 7/7 requirements mapped)
Resume file: None
Next action: `/gsd-plan-phase 1`
