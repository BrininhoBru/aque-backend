# Roadmap: Aque Backend

## Overview

Milestone **v1.0 — Tech Debt & Security Cleanup** closes the 7 issues filed after the `/gsd-onboard` codebase audit ([BrininhoBru/aque-backend#7-#13](https://github.com/BrininhoBru/aque-backend/issues)). No new product features — this is a hardening pass on an already-shipped API: rotate the seeded credential, unblock split-origin deploys, make the build reproducible, and lock the two fragile areas plus the identified coverage gaps behind tests.

Each requirement traces 1:1 to a GitHub issue. Every phase plan closes its issues on completion.

## Milestones

- ✅ **Pre-GSD baseline** — existing shipped app, mapped via `/gsd-onboard` on 2026-08-01 (no phase-by-phase history; built before GSD tracking)
- 🚧 **v1.0 Tech Debt & Security Cleanup** — 3 phases, 7 requirements, GH #7-#13

## Phases

- [ ] **Phase 1: Security & Deploy Hardening** - Rotate the seeded admin credential and add explicit CORS config
- [ ] **Phase 2: Dependency & Repository Cleanup** - Remove the SNAPSHOT dependency and collapse the duplicated active-lookup query
- [ ] **Phase 3: Test Coverage Lock-In** - Regression-test the fragile split-rule ordering and cover the untested auth + CRUD services

## Phase Details

### Phase 1: Security & Deploy Hardening
**Goal**: No deployed environment runs on the committed seed credential, and a future split-origin deploy of `aque-web` won't be blocked by missing CORS config
**Depends on**: Nothing (first phase)
**Requirements**: SEC-01 ([GH #7](https://github.com/BrininhoBru/aque-backend/issues/7)), DEBT-02 ([GH #11](https://github.com/BrininhoBru/aque-backend/issues/11))
**Success Criteria** (what must be TRUE):
  1. The `admin` password in the deployed (Raspberry Pi) environment is rotated off the `V3__seed_user.sql` hash — logging in with the new password succeeds and with the old one returns 401
  2. `../fluxo-deploy-aque.md` documents credential rotation as an explicit first-deploy step, so a fresh environment can't silently stay on the seeded hash
  3. `SecurityConfig` exposes a `CorsConfigurationSource` bean whose allowed origins come from env-overridable configuration (no hardcoded production origin)
  4. A test asserts a cross-origin request from an allowed origin passes and one from a disallowed origin is rejected
  5. The existing dev flow (`aque-web` proxying `/api` to `localhost:8080`) still works unchanged after the CORS bean is added
**Plans**: 2 plans

Plans:
- [ ] 01-01-PLAN.md — CORS: `corsConfigurationSource` bean driven by `app.cors.allowed-origins` (empty default), with allow/reject/dev-flow tests (DEBT-02, GH #11)
- [ ] 01-02-PLAN.md — Admin credential rotation: prove the external-hash path, write the deploy runbook, rotate on the Pi (SEC-01, GH #7)

### Phase 2: Dependency & Repository Cleanup
**Goal**: The build resolves only stable artifacts and the recurring-transaction repository has exactly one active-lookup path
**Depends on**: Nothing (independent of Phase 1)
**Requirements**: DEBT-01 ([GH #10](https://github.com/BrininhoBru/aque-backend/issues/10)), DEBT-03 ([GH #12](https://github.com/BrininhoBru/aque-backend/issues/12))
**Success Criteria** (what must be TRUE):
  1. `pom.xml` declares no `-SNAPSHOT` dependency version — `io.loghub:loghub-logger` is either pinned to a stable release or removed (with the LogHub `<repositories>` entry removed too if it becomes unused)
  2. `./mvnw clean package` succeeds on a clean local repo, and the GitHub Actions build no longer depends on the private LogHub repo being reachable (if the dependency was removed)
  3. `RecurringTransactionRepository` declares a single active-lookup method, called by both `RecurringTransactionService.findAll` and `RecurringTransactionJob.generate`
  4. `GET /api/recurring?active=true` and the scheduled generation job return the same results as before the change — `./mvnw test` passes green
**Plans**: TBD

### Phase 3: Test Coverage Lock-In
**Goal**: The two fragile behaviors and the untested auth/CRUD services fail loudly on regression instead of silently
**Depends on**: Phase 2 (recurring repository is collapsed before its service gets tests, so the tests target final code)
**Requirements**: TEST-01 ([GH #8](https://github.com/BrininhoBru/aque-backend/issues/8)), TEST-02 ([GH #9](https://github.com/BrininhoBru/aque-backend/issues/9)), TEST-03 ([GH #13](https://github.com/BrininhoBru/aque-backend/issues/13))
**Success Criteria** (what must be TRUE):
  1. `SplitRuleServiceTest` includes an update case that adds AND removes people in the same request, and that test fails if the `saveAndFlush(rule)` call before repopulating `items` is removed from `SplitRuleService.save`
  2. `AuthServiceTest` and `CustomUserDetailsServiceTest` exist and cover both the success path and the failure paths (wrong password, unknown username → expected exception type)
  3. Dedicated tests exist for `PersonService`, `CategoryService`, and `RecurringTransactionService`/`RecurringTransactionController`, covering CRUD happy path plus not-found behavior
  4. `./mvnw test` passes green with all new tests included
**Plans**: TBD

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Security & Deploy Hardening | 0/2 | Planned | - |
| 2. Dependency & Repository Cleanup | 0/? | Not started | - |
| 3. Test Coverage Lock-In | 0/? | Not started | - |

## Requirement Coverage

| Requirement | GH Issue | Phase |
|-------------|----------|-------|
| SEC-01 | #7 | Phase 1 |
| DEBT-02 | #11 | Phase 1 |
| DEBT-01 | #10 | Phase 2 |
| DEBT-03 | #12 | Phase 2 |
| TEST-01 | #8 | Phase 3 |
| TEST-02 | #9 | Phase 3 |
| TEST-03 | #13 | Phase 3 |

**Coverage:** 7/7 v1.0 requirements mapped, no orphans, no duplicates.

---
*Roadmap created: 2026-08-01 for milestone v1.0*
