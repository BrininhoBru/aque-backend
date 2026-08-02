---
phase: 01-security-deploy-hardening
plan: 01
subsystem: auth
tags: [spring-security, cors, jwt, spring-boot]

# Dependency graph
requires: []
provides:
  - "SecurityConfig.corsConfigurationSource() bean, env-overridable via CORS_ALLOWED_ORIGINS"
  - "SecurityConfigTest integration test proving allow/reject/no-origin CORS behavior"
affects: [01-02-security-deploy-hardening, future split-origin aque-web deploy]

# Actuals (#2632)
actuals:
  tokens: 1680
  tasks: 2
  commits: 3

tech-stack:
  added: []
  patterns:
    - "@Value non-final field for env-overridable config, mirrors JwtService"
    - "UrlBasedCorsConfigurationSource bean registered at /** and wired via .cors(Customizer.withDefaults())"

key-files:
  created:
    - src/test/java/com/aque/config/SecurityConfigTest.java
    - .planning/phases/01-security-deploy-hardening/deferred-items.md
  modified:
    - src/main/java/com/aque/config/SecurityConfig.java
    - src/main/resources/application.properties
    - src/test/resources/application-test.properties
    - mvnw

key-decisions:
  - "Default app.cors.allowed-origins is empty — matches today's same-origin dev proxy and prod nginx topology, cross-origin stays rejected until an operator explicitly sets CORS_ALLOWED_ORIGINS"
  - "Parsed the origins property manually (split/trim/filter) instead of binding directly to a List<String>, since an empty property string otherwise binds to a single blank-string origin"
  - "chmod +x mvnw — the wrapper script was not executable, blocking all test runs; fixed as a Rule 3 blocking-issue since verification could not run at all otherwise"

patterns-established:
  - "CORS config pattern: property -> @Value field -> CorsConfigurationSource bean -> .cors(Customizer.withDefaults()) in filterChain"

requirements-completed: [DEBT-02]

coverage:
  - id: D1
    description: "Preflight OPTIONS from an allowed Origin returns 200 with matching Access-Control-Allow-Origin header"
    requirement: "DEBT-02"
    verification:
      - kind: integration
        ref: "src/test/java/com/aque/config/SecurityConfigTest.java#preflight_origemPermitida_retornaCabecalhoCors"
        status: pass
    human_judgment: false
  - id: D2
    description: "Preflight OPTIONS from a disallowed Origin returns 403 with no Access-Control-Allow-Origin header"
    requirement: "DEBT-02"
    verification:
      - kind: integration
        ref: "src/test/java/com/aque/config/SecurityConfigTest.java#preflight_origemNaoPermitida_semCabecalhoCors"
        status: pass
    human_judgment: false
  - id: D3
    description: "Authenticated request with no Origin header (dev proxy / prod nginx shape) still returns 200"
    requirement: "DEBT-02"
    verification:
      - kind: integration
        ref: "src/test/java/com/aque/config/SecurityConfigTest.java#requisicaoSemOrigin_fluxoProxy_continuaFuncionando"
        status: pass
    human_judgment: false

duration: 6min
completed: 2026-08-02
status: complete
---

# Phase 01 Plan 01: CORS Configuration Hardening Summary

**Explicit `CorsConfigurationSource` bean in `SecurityConfig`, driven by `app.cors.allowed-origins`/`CORS_ALLOWED_ORIGINS` with an empty (deny cross-origin) default, proven end-to-end by a 3-test `SecurityConfigTest`.**

## Performance

- **Duration:** 6 min
- **Started:** 2026-08-02T12:31:18Z
- **Completed:** 2026-08-02T12:36:50Z
- **Tasks:** 2
- **Files modified:** 4 (+ 1 created plan artifact)

## Accomplishments
- `SecurityConfig` now exposes a `corsConfigurationSource()` bean with zero hardcoded URL literals — every allowed origin flows in via `app.cors.allowed-origins` (`CORS_ALLOWED_ORIGINS` env var)
- `.cors(Customizer.withDefaults())` wired into the filter chain so the bean is actually consulted (previously would have been dead code without this)
- `SecurityConfigTest` proves all three success criteria end-to-end against a real Postgres/Testcontainers-backed context: allowed origin passes preflight, disallowed origin is rejected with no CORS header, no-Origin authenticated request (the actual dev-proxy/prod-nginx shape) is unaffected
- Fixed `mvnw` not being executable, which was silently blocking every test run in this environment

## Task Commits

Each task was committed atomically (Task 1 used the TDD RED/GREEN split since it carries `tdd="true"`):

1. **Task 1 RED: failing preflight CORS test** - `253070d` (test)
2. **Task 1 GREEN: CORS bean + filter chain wiring** - `94ca54f` (feat)
3. **Task 2: rejection + no-Origin regression tests** - `87df75f` (test)

**Plan metadata:** _pending — committed after this SUMMARY via final_commit step_

## Files Created/Modified
- `src/main/java/com/aque/config/SecurityConfig.java` - new `corsConfigurationSource()` bean, `@Value`-injected `allowedOrigins` field, `.cors(...)` added to filter chain
- `src/main/resources/application.properties` - new `app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:}` property (empty default)
- `src/test/resources/application-test.properties` - test profile sets `app.cors.allowed-origins=http://localhost:4200`
- `src/test/java/com/aque/config/SecurityConfigTest.java` - new integration test class, 3 tests
- `mvnw` - executable bit fixed (was `644`, needed `755` to run at all)
- `.planning/phases/01-security-deploy-hardening/deferred-items.md` - logs an unrelated pre-existing test failure found while running the full suite

## Decisions Made
- Empty default allowlist to preserve today's same-origin behavior exactly (see plan `<constraints>`) — a config change, not a code change, is what will be needed for a future split-origin deploy
- Manual origin-list parsing (split/trim/filter blanks) rather than direct `List<String>` property binding, to avoid an empty string binding to a single blank origin
- `allowCredentials` left at its Spring default (`false`) — JWT auth uses `Authorization: Bearer`, not cookies, so no credentialed CORS is needed

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed non-executable `mvnw` wrapper**
- **Found during:** Task 1 (running `./mvnw test -Dtest=SecurityConfigTest` for the RED check)
- **Issue:** `mvnw` had mode `644` (not executable), so `./mvnw` failed with "permissão negada" before any test could run
- **Fix:** `chmod +x mvnw`
- **Files modified:** `mvnw` (mode-only change, no content diff)
- **Committed in:** `94ca54f` (Task 1 GREEN commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Necessary to run any verification at all in this environment. No scope creep.

## Issues Encountered

- **Plan acceptance-criteria mismatch (not a code defect):** Task 1's acceptance criteria state `grep -c 'cors(' src/main/java/com/aque/config/SecurityConfig.java` should return "at least 2 (the `.cors(...)` chain call and the `corsConfigurationSource()` bean method)". The bean method name `corsConfigurationSource()` does not actually contain the literal substring `cors(` (it's `corsC...`, not `cors(`), so the true count is 1. The underlying `<verify>` command still exits 0 (count > 0), and the real requirement — a CORS bean that's actually wired into the filter chain — is correctly implemented and proven by the passing end-to-end test. Did not modify code to artificially satisfy the count.
- **Pre-existing unrelated test failure surfaced by `./mvnw test`:** `DashboardServiceTest.getSplit_ultimoItemAbsorveORestoDoArredondamento` fails with a `NullPointerException` on a clean full-suite run. Confirmed via `git log` that no dashboard files were touched by this plan's commits — this is pre-existing tech debt already tracked in `.planning/codebase/CONCERNS.md` ("manual rounding-remainder logic in `DashboardService.getSplit`"). Logged to `deferred-items.md` and the broken-windows ledger (`.planning/WINDOWS.md`, entry #1, kind `deviation`) rather than fixed, per the scope-boundary rule (only issues directly caused by this task's changes are auto-fixed). `SecurityConfigTest`'s own 3 tests and the rest of the 55-test suite (54 pass + 3 CORS tests included in that count) are green — only this one unrelated test fails.

## User Setup Required

None - no external service configuration required. (The `CORS_ALLOWED_ORIGINS` env var only needs to be set by an operator once a future split-origin deploy actually needs cross-origin access; today's empty default requires no action.)

## Next Phase Readiness

- DEBT-02 / GH #11 closed by this plan's `Closes #11` commit trailer
- CORS groundwork is in place for whatever the sibling plan `01-02` needs from `SecurityConfig` (no file overlap confirmed by plan-checker)
- Known pre-existing `DashboardServiceTest` rounding-remainder failure remains open — out of scope for phase 01, tracked in `.planning/WINDOWS.md` and `.planning/codebase/CONCERNS.md`

---
*Phase: 01-security-deploy-hardening*
*Completed: 2026-08-02*

## Self-Check: PASSED

All created/modified files found on disk; all 3 task commits (`253070d`, `94ca54f`, `87df75f`) found in git history.
