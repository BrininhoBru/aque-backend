# Codebase Structure

**Analysis Date:** 2026-08-01

## Directory Layout

```
aque-backend/
├── src/
│   ├── main/
│   │   ├── java/com/aque/
│   │   │   ├── AqueBackendApplication.java   # Spring Boot entry point (@EnableScheduling)
│   │   │   ├── auth/                         # Login/JWT issuance endpoints
│   │   │   │   └── dto/{request,response}/
│   │   │   ├── category/                     # Income/expense categories
│   │   │   │   └── dto/{request,response}/
│   │   │   ├── config/                       # SecurityConfig, OpenApiConfig (cross-cutting)
│   │   │   ├── dashboard/                    # Aggregated summary endpoints (read-only)
│   │   │   │   └── dto/response/
│   │   │   ├── exception/                    # BusinessException, GlobalExceptionHandler
│   │   │   ├── health/                       # Health check endpoint
│   │   │   ├── person/                       # People sharing transactions/splits
│   │   │   │   └── dto/{request,response}/
│   │   │   ├── recurring/                    # Recurring transaction templates + monthly job
│   │   │   │   └── dto/{request,response}/
│   │   │   ├── security/                     # JwtFilter, JwtService, CustomUserDetailsService
│   │   │   ├── split/                        # Split rules (transaction cost-sharing)
│   │   │   │   └── dto/{request,response}/
│   │   │   ├── transaction/                  # Core financial transaction domain
│   │   │   │   └── dto/{request,response}/
│   │   │   └── user/                         # User entity + repository (auth backing)
│   │   └── resources/
│   │       ├── application.properties        # Shared config
│   │       ├── application-dev.properties    # Dev profile overrides
│   │       ├── application-prod.properties   # Prod profile overrides
│   │       ├── logback-spring.xml            # Logging config
│   │       └── db/migration/                 # Flyway SQL migrations (V*.sql)
│   └── test/
│       ├── java/com/aque/                    # Mirrors main package structure per domain
│       │   ├── auth/, category/, dashboard/, recurring/, security/, split/, transaction/
│       └── resources/                        # Test-only config (Testcontainers etc.)
├── .github/workflows/                        # CI pipelines
├── .mvn/wrapper/                             # Maven wrapper
├── pom.xml                                   # Maven build config
└── mvnw / mvnw.cmd                           # Maven wrapper scripts
```

## Directory Purposes

**`src/main/java/com/aque/<domain>/`:**
- Purpose: one vertical slice per business domain — feature-complete Controller/Service/Repository/Entity/DTOs.
- Contains: `<Domain>Controller.java`, `<Domain>Service.java`, `<Domain>Repository.java`, `<Domain>.java` (JPA entity), enum types where relevant (e.g. `category/CategoryType.java`, `transaction/TransactionStatus.java`), `dto/request/*.java`, `dto/response/*.java`.
- Key files: `transaction/TransactionController.java`, `transaction/TransactionService.java`.

**`src/main/java/com/aque/config/`:**
- Purpose: application-wide Spring `@Configuration` classes not tied to one domain.
- Contains: `SecurityConfig.java` (JWT filter chain, auth provider, password encoder), `OpenApiConfig.java` (Swagger/OpenAPI setup).

**`src/main/java/com/aque/security/`:**
- Purpose: JWT authentication mechanics, shared across all domains via the security filter chain.
- Contains: `JwtFilter.java` (per-request token validation), `JwtService.java` (token issue/parse), `CustomUserDetailsService.java` (loads `User` for Spring Security).

**`src/main/java/com/aque/exception/`:**
- Purpose: centralized error handling.
- Contains: `BusinessException.java` (status-carrying runtime exception), `GlobalExceptionHandler.java` (`@RestControllerAdvice` mapping exceptions to JSON error responses).

**`src/main/resources/db/migration/`:**
- Purpose: Flyway-managed schema evolution — the only sanctioned way to change the database schema (`ddl-auto=validate`, no Hibernate auto-DDL).
- Contains: `V{n}__description.sql` files, applied in order at startup.

**`src/test/java/com/aque/<domain>/`:**
- Purpose: tests mirroring the main package layout, one subpackage per domain that has tests (`auth`, `category`, `dashboard`, `recurring`, `security`, `split`, `transaction`).
- Note: not every domain has a dedicated test subpackage (e.g. `person`, `user`, `health`, `config` currently untested at this granularity — verify before assuming coverage).

## Key File Locations

**Entry Points:**
- `src/main/java/com/aque/AqueBackendApplication.java`: Spring Boot main class, `@EnableScheduling`.

**Configuration:**
- `src/main/resources/application.properties`: shared settings (context path `/api`, JPA `ddl-auto=validate`, active profile default).
- `src/main/resources/application-dev.properties` / `application-prod.properties`: profile-specific overrides.
- `src/main/java/com/aque/config/SecurityConfig.java`: security filter chain, public route allowlist.

**Core Logic:**
- `src/main/java/com/aque/transaction/TransactionService.java`: primary domain business logic, representative of the Service-layer pattern used everywhere.
- `src/main/java/com/aque/recurring/RecurringTransactionJob.java`: scheduled cron logic outside the request cycle.

**Testing:**
- `src/test/java/com/aque/<domain>/`: JUnit 5 test classes, named `<Domain>ServiceTest`/`<Domain>ControllerTest` by convention (verify per-domain).
- `src/test/resources/`: Testcontainers/test-profile config.

## Naming Conventions

**Files:**
- Entity: `<Domain>.java` (e.g. `Transaction.java`, `Category.java`) — singular, matches table concept.
- Controller: `<Domain>Controller.java`.
- Service: `<Domain>Service.java`.
- Repository: `<Domain>Repository.java`.
- Scheduled job: `<Domain>Job.java` (e.g. `RecurringTransactionJob.java`).
- Request DTO: `<Domain>Request.java` in `dto/request/`.
- Response DTO: `<Domain>Response.java` in `dto/response/`.
- Enum: `<Concept>.java` co-located in the domain package (e.g. `CategoryType.java`, `TransactionStatus.java`), not nested inside another class.
- Exceptions: `<Name>Exception.java` in `exception/` (currently a single shared `BusinessException`, not per-domain).

**Directories:**
- Lowercase, singular domain noun (`transaction`, not `transactions`).
- Every domain package with DTOs has a nested `dto/request/` and `dto/response/` split — never a flat `dto/` folder mixing both.

## Where to Add New Code

**New Feature (new domain, e.g. `budget`):**
- Create `src/main/java/com/aque/budget/` with `Budget.java` (entity), `BudgetRepository.java`, `BudgetService.java`, `BudgetController.java`, and `dto/request/BudgetRequest.java` + `dto/response/BudgetResponse.java`.
- Add a Flyway migration `src/main/resources/db/migration/V{next}__create_budget_table.sql` for the new table (never rely on `ddl-auto`).
- Tests: `src/test/java/com/aque/budget/BudgetServiceTest.java` (mirror the domain package).

**New endpoint on an existing domain:**
- Add method to `<Domain>Controller.java`, delegate to a new/existing method on `<Domain>Service.java`. Add OpenAPI `@Operation`/`@ApiResponse` annotations matching existing style (see `TransactionController.java`).

**Cross-domain business rule:**
- Prefer adding logic to the owning domain's `Service`. If another domain's `Service` needs read access, inject that domain's `Repository` directly (established pattern) rather than creating a new cross-service call chain.

**Shared/reusable logic (not domain-specific):**
- `config/` for `@Configuration` beans, `security/` for auth mechanics, `exception/` for new shared exception types or handler additions to `GlobalExceptionHandler`.

## Special Directories

**`src/main/resources/db/migration/`:**
- Purpose: Flyway SQL migration scripts, the single source of truth for schema.
- Generated: No — hand-written.
- Committed: Yes.

**`target/`:**
- Purpose: Maven build output (compiled classes, packaged jar).
- Generated: Yes.
- Committed: No.

**`.mvn/wrapper/`:**
- Purpose: Maven wrapper JAR/config so `./mvnw` works without a system Maven install.
- Generated: No (checked-in wrapper).
- Committed: Yes.

**`.github/workflows/`:**
- Purpose: CI pipeline definitions (build images for Raspberry Pi deploy per root `fluxo-deploy-aque.md`).
- Generated: No.
- Committed: Yes.

---

*Structure analysis: 2026-08-01*
