# Deferred Items — Phase 01

Out-of-scope discoveries found during plan execution. Not fixed here per the executor's scope boundary (only issues directly caused by the current task's changes are auto-fixed).

## From 01-01 (CORS configuration)

- **`DashboardServiceTest.getSplit_ultimoItemAbsorveORestoDoArredondamento` fails with a `NullPointerException`** on a clean `./mvnw test` run (unrelated to `SecurityConfig`/CORS changes — confirmed via `git log` that no dashboard files were touched by 01-01's commits). This matches the known tech debt already logged in `.planning/codebase/CONCERNS.md`: "manual rounding-remainder logic in `DashboardService.getSplit`". Deterministic failure with fixed test inputs (33.34/33.33/33.33 split of 100.01), not flaky.
  - File: `src/test/java/com/aque/dashboard/DashboardServiceTest.java:93-117`
  - Related: `src/main/java/com/aque/dashboard/DashboardService.java` (`getSplit`)
