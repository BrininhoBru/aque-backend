# Codebase Concerns

**Analysis Date:** 2026-08-01

## Tech Debt

**No ownership/multi-user model:**
- Issue: Entities (`Transaction`, `Category`, `Person`, `SplitRule`, `RecurringTransaction`) have no `user_id` / owner column. A single admin user is seeded (`src/main/resources/db/migration/V3__seed_user.sql`) and any authenticated JWT holder can read/write all data.
- Files: `src/main/java/com/aque/transaction/Transaction.java`, `src/main/java/com/aque/config/SecurityConfig.java`
- Impact: Fine for a single-user personal app (per `CLAUDE.md`, this is the intended design), but blocks adding a second household/user without a schema migration and authorization rework.
- Fix approach: If multi-tenancy is ever needed, add `owner_id` to all domain tables and filter every repository query/spec by the authenticated principal.

**No role-based authorization:**
- Issue: `SecurityConfig` only distinguishes `permitAll()` vs `authenticated()` — there is exactly one role/user, so no `@PreAuthorize` or role checks exist anywhere.
- Files: `src/main/java/com/aque/config/SecurityConfig.java`
- Impact: Any valid JWT can call `/recurring/generate/{year}/{month}`, delete recurring templates, or modify split rules — acceptable for single-admin use, becomes a gap if a second, lower-privilege user is ever introduced.
- Fix approach: Add `@PreAuthorize` / role claims to JWT if multi-role access is introduced later.

**Duplicated `RecurringTransactionRepository.findByActive(boolean)` / `findByActiveTrue()`:**
- Issue: Two Spring Data query methods do the same thing (`RecurringTransactionService.findAll` uses `findByActive`, `RecurringTransactionJob.generate` uses `findByActiveTrue`).
- Files: `src/main/java/com/aque/recurring/RecurringTransactionRepository.java`
- Impact: Minor redundancy, no functional bug — cosmetic debt only.
- Fix approach: Collapse to a single `findByActive(true)` call site.

**Split percentage remainder logic is manual and comment-documented as a workaround:**
- Files: `src/main/java/com/aque/dashboard/DashboardService.java` (`getSplit`, lines ~101-125)
- Issue: To keep the sum of rounded per-person amounts equal to `totalExpense`, the last item (sorted by `person.id`, an arbitrary but stable order) absorbs the rounding remainder instead of each item rounding independently.
- Impact: Correct today, but fragile — if the sort key or item list changes, remainder assignment silently shifts to a different person. No test currently locks in *which* person absorbs the remainder.
- Fix approach: Keep as is; add a unit test asserting remainder goes to the last-sorted item if this logic is touched again.

## Known Bugs

None identified. No `TODO`/`FIXME`/`HACK` markers exist in `src/main/java` and core money/date logic is covered by unit tests (`SplitRuleServiceTest`, `TransactionServiceTest`, `RecurringTransactionJobUnitTest`).

## Security Considerations

**Bleeding-edge Spring Boot version:**
- Risk: `pom.xml` pins `spring-boot-starter-parent` to `4.0.0` on Java 25 — both are very recent major versions. Fewer years of production hardening, security patches, and community troubleshooting exist compared to Spring Boot 3.x LTS lines.
- Files: `pom.xml`
- Current mitigation: None specific — relies on Spring's standard patch cadence.
- Recommendations: Track Spring Boot security advisories closely; keep the parent POM updated as 4.x patch releases land, since a young major version is more likely to receive rapid follow-up fixes.

**Dev-only defaults leak convention, not secrets:**
- Risk: `src/main/resources/application-dev.properties` hardcodes a dev DB password default (`DB_PASS:123@Mudar`) and a dev-only JWT secret fallback (`app.jwt.secret=${JWT_SECRET:dev-only-secret-key-...}`). Both are clearly dev-only (`# nunca herdado em prod` comment) and `application.properties` (shared/prod) requires `JWT_SECRET` with no default, failing boot if unset.
- Files: `src/main/resources/application-dev.properties`, `src/main/resources/application.properties`
- Current mitigation: Prod profile has no default secret — deliberate fail-fast design, correctly implemented.
- Recommendations: None needed; verify `application-dev.properties` is never active in a deployed environment (`spring.profiles.active` must not resolve to `dev` in prod containers).

**Seeded admin credential committed as a bcrypt hash:**
- Risk: `src/main/resources/db/migration/V3__seed_user.sql` inserts a fixed username (`admin`) and bcrypt hash into every fresh database. Anyone with repo access who can also crack/guess the plaintext gains admin access to any environment that ran this migration without changing the password.
- Files: `src/main/resources/db/migration/V3__seed_user.sql`
- Current mitigation: Bcrypt (cost 12) hash rather than plaintext.
- Recommendations: Confirm the seeded password is rotated immediately after first deploy to any non-local environment; consider making the seed conditional on an env var or documenting the rotation step in deploy docs.

**No CORS configuration found:**
- Risk: `SecurityConfig` does not configure a `CorsConfigurationSource`; Spring Security's default (deny cross-origin unless explicitly allowed) applies. If `aque-web` is ever served from a different origin than the backend in production, requests will fail until CORS is added — not a vulnerability today, but a gap if deployment topology changes.
- Files: `src/main/java/com/aque/config/SecurityConfig.java`
- Current mitigation: Dev setup proxies `/api` through the Angular dev server, avoiding CORS entirely (per root `CLAUDE.md`).
- Recommendations: Add explicit `CorsConfigurationSource` if frontend and backend are ever deployed to different origins/ports in production.

**JWT has no refresh/revocation mechanism:**
- Risk: `JwtService` issues stateless tokens with only an expiration (`app.jwt.expiration-ms`, default 24h). There is no token blacklist, refresh flow, or logout endpoint — a leaked token remains valid until natural expiry.
- Files: `src/main/java/com/aque/security/JwtService.java`, `src/main/java/com/aque/auth/AuthController.java`
- Current mitigation: 24h default expiration limits exposure window.
- Recommendations: Acceptable for a personal single-user app; add a revocation list or shorter-lived tokens with refresh if the threat model changes.

## Performance Bottlenecks

None significant for current scale. `TransactionService.findAll` builds a dynamic JPA `Specification` and loads all matching rows into memory (no pagination) — files: `src/main/java/com/aque/transaction/TransactionService.java`. Fine at personal-finance-app volumes (hundreds to low thousands of transactions); would need pagination if transaction volume grows by orders of magnitude.

**Raspberry Pi 3B deployment target constrains headroom:**
- Files: `src/main/resources/application-prod.properties`
- Notes: `spring.main.lazy-initialization=true` (comment: reduces boot from a 75s baseline) and HikariCP tuned to `maximum-pool-size=5` are deliberate constraints for the Pi 3B host (see root `fluxo-deploy-aque.md`). Any new feature adding heavy startup-time bean initialization or concurrent DB access should be tested against this constrained profile, not just dev.

## Fragile Areas

**`RecurringTransactionService.generate(int, int)` delegates directly to `RecurringTransactionJob.generate`:**
- Files: `src/main/java/com/aque/recurring/RecurringTransactionService.java`, `src/main/java/com/aque/recurring/RecurringTransactionJob.java`
- Why fragile: A `@Component` job class is injected into and called by a `@Service`, blending "scheduled batch job" and "service-layer business logic" responsibilities. Not currently broken, but if the job process needs synchronous vs. async execution semantics to differ (e.g., in the manual `/recurring/generate` endpoint vs. the cron trigger), they currently cannot.
- Safe modification: Keep `generate(year, month)` as the single source of truth; if manual-trigger behavior needs to diverge from cron behavior, split into two methods rather than adding conditionals inside the shared one.
- Test coverage: `RecurringTransactionJobUnitTest` (79 lines) covers the job directly; no test exercises `RecurringTransactionService.generate` as a thin delegate — low risk given its simplicity.

**Split rule save does `clear()` + `saveAndFlush()` before re-adding items, with an explicit ordering workaround:**
- Files: `src/main/java/com/aque/split/SplitRuleService.java` (`save`, lines ~34-59)
- Why fragile: The code comment explains that without the intermediate flush, Hibernate orders INSERTs before the orphan-removal DELETEs and collides with the `UNIQUE(split_rule_id, person_id)` constraint. This is a correct but non-obvious ORM behavior; a future refactor that removes the `saveAndFlush()` call (looking like dead code) would silently reintroduce a constraint-violation bug under certain item-reordering scenarios.
- Safe modification: Do not remove the `saveAndFlush(rule)` call before repopulating `items`; if refactoring, add a regression test that updates an existing split rule's person list (add + remove in the same request) to catch the ordering issue.
- Test coverage: `SplitRuleServiceTest` (150 lines) exists; verify it includes an update scenario with changed membership, not just initial creation.

## Scaling Limits

Not applicable at current scope — this is a personal single-user finance app deployed to a Raspberry Pi 3B (see root `fluxo-deploy-aque.md`). HikariCP pool size (5 connections) and lazy initialization are already tuned for this constrained target, not for growth.

## Dependencies at Risk

**`loghub-logger` (internal/private SNAPSHOT dependency):**
- Risk: `pom.xml` depends on `loghub-logger:0.1.0-SNAPSHOT` — a SNAPSHOT version is inherently unstable/mutable (the artifact content can change without a version bump) and typically indicates an internal, unpublished-to-Maven-Central library.
- Impact: Builds are not fully reproducible while depending on a SNAPSHOT; if the artifact is pulled from a private/local repository, CI/CD builds (GitHub Actions, per root `CLAUDE.md`) depend on that repository's availability.
- Migration plan: Pin to a released (non-SNAPSHOT) version once `loghub-logger` reaches a stable release, or vendor/inline the needed logging behavior if the dependency is small.

**Spring Boot 4.0.0 / Java 25 (very recent major versions):**
- Risk: Both are new-generation releases with a shorter track record than Spring Boot 3.x LTS + Java 21 LTS. Third-party library compatibility (springdoc-openapi 2.8.6, jjwt 0.12.6) should be re-verified on each Spring Boot 4.x patch bump.
- Impact: Higher chance of encountering edge-case incompatibilities or needing rapid dependency bumps to track Spring Boot 4.x patch releases.
- Migration plan: No action needed now; monitor Spring Boot 4.x release notes for breaking changes before upgrading further.

## Missing Critical Features

Not applicable — this is a small, feature-complete domain (transactions, recurring transactions, categories, people, split rules, dashboard). No obvious functional gaps relative to the app's stated purpose.

## Test Coverage Gaps

**No dedicated tests for several main-package classes:**
- What's not tested: `PersonController` / `PersonService` (`src/main/java/com/aque/person/`), `CategoryService` (`src/main/java/com/aque/category/CategoryService.java` — only `CategoryController` has a test), `AuthService` (`src/main/java/com/aque/auth/AuthService.java` — only `AuthController` has a test), `RecurringTransactionController` / `RecurringTransactionService` (only the underlying `RecurringTransactionJob` is tested), `CustomUserDetailsService` (`src/main/java/com/aque/security/CustomUserDetailsService.java`).
- Files: no corresponding `*Test.java` under `src/test/java/com/aque/person/`, and missing `CategoryServiceTest`, `AuthServiceTest`, `RecurringTransactionServiceTest`, `RecurringTransactionControllerTest`, `CustomUserDetailsServiceTest`.
- Risk: `PersonService` and `CategoryService` are simple CRUD and low-risk, but `AuthService` and `CustomUserDetailsService` sit directly on the authentication path — an untested regression there (e.g., wrong password comparison, wrong exception type) could silently break or weaken login.
- Priority: Medium for `AuthService`/`CustomUserDetailsService` (security-adjacent, currently only indirectly covered via `JwtFilterTest` and `AuthControllerTest`); Low for `PersonService`/`CategoryService`/`RecurringTransactionController` (simple CRUD, low complexity, covered indirectly by controller-level and integration-style tests where they exist).

---

*Concerns audit: 2026-08-01*
