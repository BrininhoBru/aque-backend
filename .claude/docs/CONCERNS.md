# Codebase Concerns

## Tech Debt

**No ownership/multi-user model:**
- Issue: Entities (`Transaction`, `Category`, `Person`, `SplitRule`, `RecurringTransaction`) have no `user_id` / owner column. A single admin user is seeded (`src/main/resources/db/migration/V3__seed_user.sql`) and any authenticated JWT holder can read/write all data.
- Files: `src/main/java/com/aque/transaction/Transaction.java`, `src/main/java/com/aque/config/SecurityConfig.java`
- Impact: Fine for a single-user personal app (intentional design), but blocks adding a second household/user without a schema migration and authorization rework.
- Fix approach: If multi-tenancy is ever needed, add `owner_id` to all domain tables and filter every repository query/spec by the authenticated principal.

**No role-based authorization:**
- Issue: `SecurityConfig` only distinguishes `permitAll()` vs `authenticated()` — there is exactly one role/user, so no `@PreAuthorize` or role checks exist anywhere.
- Impact: Any valid JWT can call `/recurring/generate/{year}/{month}`, delete recurring templates, or modify split rules — acceptable for single-admin use, becomes a gap if a second, lower-privilege user is ever introduced.
- Fix approach: Add `@PreAuthorize` / role claims to JWT if multi-role access is introduced later.

**Duplicated `RecurringTransactionRepository.findByActive(boolean)` / `findByActiveTrue()`:**
- Issue: Two Spring Data query methods do the same thing (`RecurringTransactionService.findAll` uses `findByActive`, `RecurringTransactionJob.generate` uses `findByActiveTrue`).
- Files: `src/main/java/com/aque/recurring/RecurringTransactionRepository.java`
- Impact: Minor redundancy, no functional bug — cosmetic debt only.
- Fix approach: Collapse to a single `findByActive(true)` call site.

**Split percentage remainder logic is manual and comment-documented as a workaround:**
- Files: `src/main/java/com/aque/dashboard/DashboardService.java` (`getSplit`)
- Issue: To keep the sum of rounded per-person amounts equal to `totalExpense`, the last item (sorted by `person.id`, an arbitrary but stable order) absorbs the rounding remainder instead of each item rounding independently.
- Impact: Correct today, but fragile — if the sort key or item list changes, remainder assignment silently shifts to a different person. No test currently locks in *which* person absorbs the remainder.
- Fix approach: Keep as is; add a unit test asserting remainder goes to the last-sorted item if this logic is touched again.

**`loghub-logger` (internal/private SNAPSHOT dependency):**
- Risk: `pom.xml` depends on `loghub-logger:0.1.0-SNAPSHOT` — inherently unstable/mutable, and unused (no call sites in `src/main/java`).
- Impact: Builds aren't fully reproducible while depending on a SNAPSHOT; CI/CD depends on the private repo's availability for no functional benefit.
- Migration plan: Pin to a released version once available, or remove the dependency if it stays unused.

## Known Bugs

**`DashboardServiceTest.getSplit_ultimoItemAbsorveORestoDoArredondamento` fails with a `NullPointerException`** on a full `./mvnw test` run — deterministic (fixed 33.34/33.33/33.33 split of 100.01 inputs), not flaky, and unrelated to any in-flight changes. Matches the rounding-remainder tech debt above (`DashboardService.getSplit`). File: `src/test/java/com/aque/dashboard/DashboardServiceTest.java`.

## Security Considerations

**Bleeding-edge Spring Boot version:**
- Risk: `pom.xml` pins `spring-boot-starter-parent` to `4.0.0` on Java 25 — both are very recent major versions, fewer years of production hardening than Spring Boot 3.x LTS lines.
- Recommendations: Track Spring Boot security advisories closely; keep the parent POM updated as 4.x patch releases land.

**Dev-only defaults leak convention, not secrets:**
- Risk: `application-dev.properties` hardcodes a dev DB password default and a dev-only JWT secret fallback. Both are clearly dev-only; `application.properties` (shared/prod) requires `JWT_SECRET` with no default, failing boot if unset.
- Current mitigation: Prod profile has no default secret — deliberate fail-fast design, correctly implemented.
- Recommendations: Verify `application-dev.properties` is never active in a deployed environment.

**Seeded admin credential committed as a bcrypt hash:**
- Risk: `src/main/resources/db/migration/V3__seed_user.sql` inserts a fixed username (`admin`) and bcrypt hash into every fresh database. Anyone with repo access who can also crack/guess the plaintext gains admin access to any environment that ran this migration without changing the password.
- Current mitigation: Bcrypt (cost 12) hash rather than plaintext.
- Recommendations: Rotate the seeded password immediately after first deploy to any non-local environment (see `../fluxo-deploy-aque.md` for the rotation runbook).

**JWT has no refresh/revocation mechanism:**
- Risk: `JwtService` issues stateless tokens with only an expiration (`app.jwt.expiration-ms`, default 24h). No token blacklist, refresh flow, or logout endpoint — a leaked token remains valid until natural expiry.
- Current mitigation: 24h default expiration limits exposure window.
- Recommendations: Acceptable for a personal single-user app; add a revocation list or shorter-lived tokens with refresh if the threat model changes.

## Performance Bottlenecks

None significant for current scale. `TransactionService.findAll` builds a dynamic JPA `Specification` and loads all matching rows into memory (no pagination). Fine at personal-finance-app volumes (hundreds to low thousands of transactions); would need pagination if transaction volume grows by orders of magnitude.

**Raspberry Pi 3B deployment target constrains headroom:**
- Files: `src/main/resources/application-prod.properties`
- Notes: `spring.main.lazy-initialization=true` and HikariCP tuned to `maximum-pool-size=5` are deliberate constraints for the Pi 3B host. Any new feature adding heavy startup-time bean initialization or concurrent DB access should be tested against this constrained profile, not just dev.

## Fragile Areas

**`RecurringTransactionService.generate(int, int)` delegates directly to `RecurringTransactionJob.generate`:**
- Files: `src/main/java/com/aque/recurring/RecurringTransactionService.java`, `RecurringTransactionJob.java`
- Why fragile: A `@Component` job class is injected into and called by a `@Service`, blending "scheduled batch job" and "service-layer business logic" responsibilities. Not currently broken, but if the job process needs synchronous vs. async execution semantics to differ (manual `/recurring/generate` endpoint vs. cron trigger), they currently cannot.
- Safe modification: Keep `generate(year, month)` as the single source of truth; if manual-trigger behavior needs to diverge from cron behavior, split into two methods rather than adding conditionals inside the shared one.

**Split rule save does `clear()` + `saveAndFlush()` before re-adding items, with an explicit ordering workaround:**
- Files: `src/main/java/com/aque/split/SplitRuleService.java` (`save`)
- Why fragile: Without the intermediate flush, Hibernate orders INSERTs before the orphan-removal DELETEs and collides with the `UNIQUE(split_rule_id, person_id)` constraint. This is correct but non-obvious ORM behavior; a future refactor that removes the `saveAndFlush()` call (looking like dead code) would silently reintroduce a constraint-violation bug under certain item-reordering scenarios.
- Safe modification: Don't remove the `saveAndFlush(rule)` call before repopulating `items`; if refactoring, add a regression test that updates an existing split rule's person list (add + remove in the same request).

## Scaling Limits

Not applicable at current scope — personal single-user finance app deployed to a Raspberry Pi 3B. HikariCP pool size (5 connections) and lazy initialization are already tuned for this constrained target, not for growth.

## Missing Critical Features

Not applicable — small, feature-complete domain (transactions, recurring transactions, categories, people, split rules, dashboard).

## Test Coverage Gaps

**No dedicated tests for several main-package classes:**
- What's not tested: `PersonController` / `PersonService`, `CategoryService` (only `CategoryController` has a test), `AuthService` (only `AuthController` has a test), `RecurringTransactionController` / `RecurringTransactionService` (only the underlying `RecurringTransactionJob` is tested), `CustomUserDetailsService`.
- Risk: `PersonService` and `CategoryService` are simple CRUD and low-risk, but `AuthService` and `CustomUserDetailsService` sit directly on the authentication path — an untested regression there could silently break or weaken login.
- Priority: Medium for `AuthService`/`CustomUserDetailsService` (security-adjacent); Low for `PersonService`/`CategoryService`/`RecurringTransactionController` (simple CRUD, covered indirectly elsewhere).
