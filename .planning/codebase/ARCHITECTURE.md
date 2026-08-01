<!-- refreshed: 2026-08-01 -->
# Architecture

**Analysis Date:** 2026-08-01

## System Overview

```text
┌─────────────────────────────────────────────────────────────┐
│                    HTTP Layer (Controllers)                  │
│  `src/main/java/com/aque/<domain>/<Domain>Controller.java`   │
│  @RestController, @RequestMapping, springdoc annotations     │
├──────────────────────────┬────────────────────────────────────
│  JwtFilter (per request) │  GlobalExceptionHandler (errors)   │
│  `security/JwtFilter.java`│ `exception/GlobalExceptionHandler` │
└──────────────────────────┴────────────────────────────────────
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Service Layer (business logic)             │
│  `src/main/java/com/aque/<domain>/<Domain>Service.java`      │
│  @Service, request DTO -> entity mapping, validation,        │
│  cross-domain lookups (e.g. TransactionService -> Category)  │
└─────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│              Persistence Layer (Spring Data JPA)              │
│  `src/main/java/com/aque/<domain>/<Domain>Repository.java`   │
│  extends JpaRepository / JpaSpecificationExecutor             │
└─────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│              PostgreSQL (schema via Flyway migrations)        │
│  `src/main/resources/db/migration/V*.sql`                    │
└─────────────────────────────────────────────────────────────┘
```

Scheduled job (`RecurringTransactionJob`) runs outside the request cycle, writing directly through repositories (bypasses controllers/services of the `transaction` domain but reuses its repository).

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| `<Domain>Controller` | HTTP binding, request validation (`@Valid`), status codes, OpenAPI docs | `src/main/java/com/aque/<domain>/<Domain>Controller.java` |
| `<Domain>Service` | Business rules, DTO↔entity mapping, orchestration across repositories | `src/main/java/com/aque/<domain>/<Domain>Service.java` |
| `<Domain>Repository` | Data access, Spring Data JPA query derivation + `Specification` filters | `src/main/java/com/aque/<domain>/<Domain>Repository.java` |
| `<Domain>` (entity) | JPA-mapped table, Lombok `@Getter/@Setter` | `src/main/java/com/aque/<domain>/<Domain>.java` |
| DTOs (records) | Request/response shape, `from(entity)` static factory for responses | `src/main/java/com/aque/<domain>/dto/{request,response}/*.java` |
| `SecurityConfig` | Stateless JWT filter chain, public route allowlist, password encoder | `src/main/java/com/aque/config/SecurityConfig.java` |
| `JwtFilter` | Extracts/validates Bearer token per request, populates `SecurityContextHolder` | `src/main/java/com/aque/security/JwtFilter.java` |
| `JwtService` | Issues/parses JWTs (secret + expiry from `app.jwt.*`) | `src/main/java/com/aque/security/JwtService.java` |
| `CustomUserDetailsService` | Loads `User` for Spring Security auth | `src/main/java/com/aque/security/CustomUserDetailsService.java` |
| `GlobalExceptionHandler` | Maps `BusinessException`, validation errors, generic errors to JSON responses | `src/main/java/com/aque/exception/GlobalExceptionHandler.java` |
| `BusinessException` | Domain-level error carrying an `HttpStatus` | `src/main/java/com/aque/exception/BusinessException.java` |
| `RecurringTransactionJob` | Monthly `@Scheduled` cron job that materializes `Transaction` rows from active `RecurringTransaction`s | `src/main/java/com/aque/recurring/RecurringTransactionJob.java` |

## Pattern Overview

**Overall:** Layered / package-by-feature monolith (Spring Boot 3, Java 25). Each business domain (`auth`, `user`, `category`, `person`, `transaction`, `recurring`, `split`, `dashboard`) is a self-contained vertical slice: Controller → Service → Repository → Entity, plus a local `dto/` folder.

**Key Characteristics:**
- No separate "domain model" vs "persistence model" split — the JPA `@Entity` doubles as the domain object; DTOs (Java records) are the only translation boundary.
- Services are constructor-injected (`@RequiredArgsConstructor`, Lombok) and largely stateless.
- No repository/service interfaces — concrete classes only (`TransactionService`, not `TransactionServiceImpl` behind an interface). Avoid adding interfaces for single implementations.
- Cross-domain reads happen directly via another domain's repository (e.g. `TransactionService` injects `CategoryRepository` to validate `categoryId`), not through a service-to-service call.
- Stateless auth: no HTTP sessions, JWT validated per-request in a servlet filter (`OncePerRequestFilter`).
- All error handling centralized in one `@RestControllerAdvice` (`GlobalExceptionHandler`); domain code throws `BusinessException(message, HttpStatus)` rather than defining per-domain exception types.

## Layers

**Controller layer:**
- Purpose: HTTP request/response mapping, input binding, OpenAPI/Swagger annotations, no business logic.
- Location: `src/main/java/com/aque/<domain>/<Domain>Controller.java`
- Contains: `@RestController` classes, one per domain, mapped under a domain-root path (e.g. `/transactions`, note: `server.servlet.context-path=/api` prefixes all routes in production).
- Depends on: the domain's `Service`.
- Used by: HTTP clients (aque-web SPA), Swagger UI.

**Service layer:**
- Purpose: business rules, request→entity mapping, cross-domain lookups, throwing `BusinessException` on not-found/invalid state.
- Location: `src/main/java/com/aque/<domain>/<Domain>Service.java`
- Contains: `@Service` classes; private helper methods for mapping (e.g. `mapperTrasaction`, `applyPayment` in `TransactionService.java`); `Specification<T>` lambdas built inline for dynamic filtering (see `TransactionService.findAll`).
- Depends on: own domain `Repository`, occasionally another domain's `Repository` directly (not its Service).
- Used by: the domain's `Controller`; `RecurringTransactionJob` (uses `TransactionRepository` directly, bypassing `TransactionService`).

**Repository layer:**
- Purpose: data access via Spring Data JPA.
- Location: `src/main/java/com/aque/<domain>/<Domain>Repository.java`
- Contains: interfaces extending `JpaRepository<Entity, UUID>`, some also `JpaSpecificationExecutor<Entity>` for dynamic query support; derived query methods (e.g. `existsByRecurringIdAndReferenceMonthAndReferenceYear`, `findByActiveTrue`).
- Depends on: the domain `Entity`.
- Used by: the domain `Service`, and occasionally other domains' `Service`/`Job` classes.

**Entity layer:**
- Purpose: JPA mapping to Postgres tables.
- Location: `src/main/java/com/aque/<domain>/<Domain>.java`
- Contains: `@Entity` classes with Lombok `@Getter @Setter @NoArgsConstructor`, `UUID` primary keys (`GenerationType.UUID`), `@Enumerated(EnumType.STRING)` for enums, explicit `@Column(name = "...")` snake_case mapping.
- Depends on: other domain entities via `@ManyToOne`/`@JoinColumn` (e.g. `Transaction.category -> Category`).

**DTO layer:**
- Purpose: request validation and response shaping; decouples API contract from entity structure.
- Location: `src/main/java/com/aque/<domain>/dto/request/*.java`, `src/main/java/com/aque/<domain>/dto/response/*.java`
- Contains: Java `record` types. Request DTOs use `jakarta.validation` annotations. Response DTOs expose a static `from(Entity entity)` factory that performs entity→record mapping (nested DTOs composed by calling nested `from()`, e.g. `TransactionResponse.from` calls `CategoryResponse.from(transaction.getCategory())`).

**Cross-cutting (`config/`, `security/`, `exception/`):**
- Purpose: shared infrastructure not owned by any single domain.
- Location: `src/main/java/com/aque/config/` (`SecurityConfig`, `OpenApiConfig`), `src/main/java/com/aque/security/` (`JwtFilter`, `JwtService`, `CustomUserDetailsService`), `src/main/java/com/aque/exception/` (`BusinessException`, `GlobalExceptionHandler`).
- Depends on: `user` domain (`CustomUserDetailsService` loads `User`/`UserRepository`).
- Used by: every domain implicitly, via the Spring Security filter chain and the global `@RestControllerAdvice`.

## Data Flow

### Primary Request Path (e.g. `POST /transactions`)

1. Request hits `JwtFilter.doFilterInternal` — validates Bearer token, populates `SecurityContextHolder` (`src/main/java/com/aque/security/JwtFilter.java:28`).
2. `SecurityConfig.filterChain` authorization rule allows/denies based on route (`src/main/java/com/aque/config/SecurityConfig.java:34`).
3. `TransactionController.create` binds and `@Valid`-ates `TransactionRequest` (`src/main/java/com/aque/transaction/TransactionController.java:64`).
4. `TransactionService.create` resolves `Category` via `CategoryRepository.findById` (throwing `BusinessException` on 404), maps the request onto a new `Transaction` entity, persists via `TransactionRepository.save` (`src/main/java/com/aque/transaction/TransactionService.java:44`).
5. `TransactionResponse.from(entity)` converts the saved entity to a response record; controller wraps it in `ResponseEntity.status(201)` (`src/main/java/com/aque/transaction/dto/response/TransactionResponse.java:26`).
6. Any thrown `BusinessException`/validation error is intercepted by `GlobalExceptionHandler` and converted to a JSON error body (`src/main/java/com/aque/exception/GlobalExceptionHandler.java`).

### Scheduled Recurring-Transaction Generation

1. `RecurringTransactionJob.generateMonthlyTransactions` fires on cron `0 0 0 1 * *` (midnight, 1st of month) via Spring's `@Scheduled` (requires `@EnableScheduling` on `AqueBackendApplication`).
2. Loads all active `RecurringTransaction`s (`RecurringTransactionRepository.findByActiveTrue`).
3. For each, checks idempotency via `TransactionRepository.existsByRecurringIdAndReferenceMonthAndReferenceYear` to avoid duplicate generation.
4. Builds a new `Transaction` from the recurring template and saves it; per-item failures are caught and logged so one bad recurring rule doesn't abort the batch (`src/main/java/com/aque/recurring/RecurringTransactionJob.java:55`).

**State Management:**
- No in-memory application state; all state is in Postgres. Auth state is entirely stateless (JWT), no server-side sessions.

## Key Abstractions

**`BusinessException` (unchecked, status-carrying):**
- Purpose: single exception type for all domain-level failures (not-found, invalid state), carries an `HttpStatus` to drive the HTTP response.
- Examples: `src/main/java/com/aque/exception/BusinessException.java`, thrown from `TransactionService.findCategory`, `PersonService`, etc.
- Pattern: `throw new BusinessException("<message>", HttpStatus.NOT_FOUND)`.

**`Specification<T>` inline lambdas for dynamic filtering:**
- Purpose: build optional-predicate queries (e.g. filter transactions by month/year/category/type/status only when parameters are non-null) without generating N repository methods.
- Examples: `src/main/java/com/aque/transaction/TransactionService.java:26`
- Pattern: repository extends `JpaSpecificationExecutor<Entity>`; service builds the `Specification` lambda inline and calls `repository.findAll(spec)`.

**Response DTO `from(entity)` static factory:**
- Purpose: uniform entity→DTO conversion co-located with the DTO definition (no separate mapper class/library).
- Examples: `TransactionResponse.from`, `CategoryResponse.from` (nested composition).
- Pattern: `public static XResponse from(XEntity e) { return new XResponse(...); }`.

## Entry Points

**`AqueBackendApplication.main`:**
- Location: `src/main/java/com/aque/AqueBackendApplication.java`
- Triggers: JVM startup (`./mvnw spring-boot:run` or packaged jar).
- Responsibilities: Spring Boot bootstrap, `@EnableScheduling` activates cron-based jobs.

**REST Controllers:**
- Location: `src/main/java/com/aque/<domain>/<Domain>Controller.java`
- Triggers: incoming HTTP requests under `/api` context path (`server.servlet.context-path`).
- Responsibilities: per-domain CRUD/query endpoints (`auth`, `category`, `dashboard`, `person`, `recurring`, `split`, `transaction`, `health`).

**`RecurringTransactionJob`:**
- Location: `src/main/java/com/aque/recurring/RecurringTransactionJob.java`
- Triggers: Spring `@Scheduled` cron (monthly), or direct call to `generate(year, month)` (used by tests/manual invocation).
- Responsibilities: materialize `Transaction` rows from `RecurringTransaction` templates.

## Architectural Constraints

- **Threading:** Standard servlet thread-per-request model (Spring MVC on embedded Tomcat); the scheduled job runs on Spring's task scheduler thread, not a request thread — no explicit worker-thread pool configured.
- **Global state:** None observed — no static mutable fields or singleton caches outside Spring-managed singleton beans (which are themselves stateless service/repository/filter beans).
- **Circular imports:** None observed between domain packages; dependencies are one-directional at the repository level (e.g. `transaction` → `category`, `recurring` → `transaction`, never the reverse).
- **Schema changes:** `ddl-auto=validate` — Hibernate never auto-generates schema. New columns/tables require a new Flyway migration in `src/main/resources/db/migration/V{n}__description.sql`.
- **Auth boundary:** every route requires a valid JWT except `/auth/login`, `/health`, and Swagger UI/OpenAPI paths (`src/main/java/com/aque/config/SecurityConfig.java:35`).

## Anti-Patterns

### Cross-domain repository injection instead of service delegation

**What happens:** A domain's `Service` injects another domain's `Repository` directly (e.g. `TransactionService` holds `CategoryRepository`) rather than calling `CategoryService`.
**Why it's wrong:** Business rules living in `CategoryService` (if any exist or are added later) can be silently bypassed by callers that go straight to the repository.
**Do this instead:** This is the established pattern throughout the codebase (low domain coupling is intentional at repository level) — keep following it for read-only lookups, but if a target domain grows validation/business logic, route through its `Service` instead of adding another direct repository dependency.

### Mixed responsibility in `RecurringTransactionJob`

**What happens:** `RecurringTransactionJob` bypasses `TransactionService` entirely and writes `Transaction` entities via `TransactionRepository` directly, duplicating some of the field-mapping logic that also exists in `TransactionService.mapperTrasaction`.
**Why it's wrong:** Any future business rule added to `TransactionService.create`/`update` (e.g. new default field, validation) will not automatically apply to job-generated transactions, causing drift between manually created and auto-generated transactions.
**Do this instead:** When adding transaction creation logic, check both `TransactionService` and `RecurringTransactionJob.getTransaction` to keep them in sync, or extract shared field-population logic into a method both can call.

## Error Handling

**Strategy:** Centralized via a single `@RestControllerAdvice` (`GlobalExceptionHandler`); domain code throws `BusinessException` with an explicit `HttpStatus` rather than relying on generic Spring exceptions.

**Patterns:**
- Not-found / invalid-state: `throw new BusinessException("<Portuguese message>", HttpStatus.NOT_FOUND)` — caught and rendered as `{status, message, timestamp}`.
- Bean validation (`@Valid` on request DTOs): `MethodArgumentNotValidException` → field-name/message map (`400`).
- Query-param constraint violations: `ConstraintViolationException` → concatenated `field: message` string (`400`).
- Auth failures: `BadCredentialsException` → hardcoded `401` message ("Credenciais inválidas").
- Everything else: caught by a generic `Exception` handler, logged at `error` level, returns a generic `500` body (no internal detail leaked).

## Cross-Cutting Concerns

**Logging:** Lombok `@Slf4j` on services/filters/jobs that log; config in `src/main/resources/logback-spring.xml`. Error/warn-level logs used for unhandled exceptions and auth edge cases.
**Validation:** `jakarta.validation` annotations on request DTO records, enforced via `@Valid` in controller method signatures.
**Authentication:** Stateless JWT — `JwtFilter` (per-request), `JwtService` (issue/parse), `CustomUserDetailsService` (loads `User` for Spring Security), configured in `SecurityConfig`.

---

*Architecture analysis: 2026-08-01*
