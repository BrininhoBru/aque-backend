# Coding Conventions

## Naming Patterns

**Files:**
- One public class/record/interface per file, filename matches type name exactly (`TransactionService.java`, `TransactionRequest.java`).
- Package-by-feature: each domain package (`transaction`, `category`, `person`, `recurring`, `split`, `dashboard`, `auth`, `user`) contains `<Domain>Controller.java`, `<Domain>Service.java`, `<Domain>Repository.java`, `<Domain>.java` (JPA entity), and a `dto/request/` + `dto/response/` subpackage.
- DTOs are named `<Domain>Request.java` / `<Domain>Response.java`, living in `dto/request/` and `dto/response/` respectively.
- Cross-cutting concerns live outside domain packages: `config/`, `security/`, `exception/`, `health/`.

**Classes:**
- PascalCase. Suffix indicates role: `Controller`, `Service`, `Repository`, `Request`, `Response`, `Job`, `Filter`, `Exception`, `Config`.

**Functions/Methods:**
- camelCase, verb-first: `findAll`, `create`, `update`, `delete`, `findById`, `findCategory`.
- Private helper methods used for internal mapping/lookup logic (e.g. `mapperTrasaction`, `applyPayment`, `findById`, `findCategory` in `TransactionService.java`). Note: existing typo `mapperTrasaction` is present in the codebase — don't "fix" it silently in unrelated diffs; only rename if the phase's actual concern is that class.

**Variables:**
- camelCase, descriptive Portuguese-influenced domain nouns are acceptable within business logic (`amountPaid`, `referenceMonth`) — field/method names are English, but validation messages and test method names are Portuguese.

**Types/DTOs:**
- Request/Response DTOs are Java `record`s, not classes — no Lombok on DTOs.
- Response records expose a static factory `from(Entity)` for entity→DTO mapping, colocated in the response record itself. Apply this pattern for any new response DTO instead of a separate mapper class.

## Code Style

**Formatting:**
- No `.editorconfig`, no Checkstyle/Spotless config — no enforced formatter. Follow existing file indentation (4 spaces) and brace style (opening brace same line) by eyeballing neighboring files.

**Linting:**
- No ESLint/Checkstyle equivalent configured for Java. Rely on IDE defaults + code review.

**Entities (Lombok):**
- JPA entities use `@Entity`, `@Table(name = "...")`, `@Getter`, `@Setter`, `@NoArgsConstructor` from Lombok — never hand-write getters/setters.
- Services and Controllers use `@RequiredArgsConstructor` for constructor injection over `@Autowired` fields — declare dependencies as `private final` fields.
- `BusinessException` uses `@Getter` for its single `status` field.
- `GlobalExceptionHandler` uses `@Slf4j` for logging.

**Column mapping:**
- Entity fields use explicit `@Column(name = "snake_case_name")` for anything not directly camelCase-to-snake_case obvious, plus `nullable = false` where DB enforces it. Schema is Flyway-managed — never rely on Hibernate `ddl-auto` to create columns; add a migration and match it with `@Column`.

## Import Organization

**Order:**
1. Same-domain / project packages (`com.aque.*`)
2. Third-party framework packages (`jakarta.*`, `org.springframework.*`, `lombok.*`, `io.swagger.*`)
3. `java.*` stdlib, typically last

No enforced import-sorting tool; follow the pattern visible in existing files.

**Wildcard imports:**
- Controllers use `org.springframework.web.bind.annotation.*` and static wildcard imports for MockMvc DSL in tests (`import static ...MockMvcRequestBuilders.*;`) — acceptable for annotation/DSL-heavy files.

## Error Handling

**Pattern: `BusinessException` + centralized handler.**
- Domain/business errors throw `new BusinessException(message, HttpStatus)` at the point of failure — always via `.orElseThrow(() -> new BusinessException("...", HttpStatus.NOT_FOUND))` on repository lookups.
- Never catch and swallow exceptions in service/controller code; let them propagate to `GlobalExceptionHandler`.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) is the single place mapping exception types to HTTP responses:
  - `BusinessException` → its own carried `HttpStatus`
  - `BadCredentialsException` → 401
  - `MethodArgumentNotValidException` → 400 with field-level error map
  - `ConstraintViolationException` → 400 with combined message
  - `Exception` (catch-all) → 500, logs via `log.error("Erro não tratado", ex)`
- Error responses use a local `record ErrorResponse(int status, String message, LocalDateTime timestamp)` defined inside the handler — reuse this record/shape for any new handler, don't invent a new error DTO.
- Validation error messages (both Bean Validation annotation messages and `BusinessException` messages) are written in Portuguese, matching the user-facing app language (e.g. `"Categoria não encontrada"`, `"Descrição é obrigatória"`).

## Logging

**Framework:** Lombok `@Slf4j` (SLF4J).

**Patterns:**
- Only used where actually needed (currently just the global exception handler's catch-all). Don't add ad-hoc `System.out.println`; use `@Slf4j` + `log.error/warn/info` when logging is needed in a new class.

## Comments

**When to Comment:**
- Minimal in-code comments, used sparingly to explain non-obvious framework decisions (e.g. `// ObjectMapper instanciado localmente — sem depender de bean do contexto`). Comments are written in Portuguese, matching test method naming.
- No JSDoc/Javadoc blocks on service/controller methods. API documentation lives in Swagger/OpenAPI annotations (`@Operation`, `@ApiResponse`, `@Parameter`) directly on controller methods.

**API Documentation:**
- Every controller endpoint is annotated with springdoc annotations: `@Operation(summary=..., description=..., responses={@ApiResponse(...)})` plus `@Parameter` on each `@RequestParam`/`@PathVariable`. Summaries/descriptions are Portuguese, matching the domain's user-facing terms. New endpoints should follow this same annotation density.
- Controllers also carry class-level `@Tag(name=..., description=...)` and `@SecurityRequirement(name = "Bearer")`.

## Function Design

**Size:** Small, single-purpose methods. Controllers only translate HTTP concerns and delegate to a service method of the same/similar name (`findAll`, `create`, `update`, `delete`) — no business logic in controllers.

**Parameters:** Services accept primitive/wrapper filter parameters directly (not a filter object) for simple queries, e.g. `findAll(Integer month, Integer year, UUID categoryId, CategoryType type, TransactionStatus status)`. Filtering uses JPA `Specification` with null-checks for each optional predicate — follow this pattern for other filterable list endpoints instead of building custom repository query methods per filter combination.

**Return Values:** Services return response DTOs (never entities) from `create`/`update`/`findAll`; `delete` returns `void`. Controllers wrap results in `ResponseEntity` with explicit status codes (`ResponseEntity.status(HttpStatus.CREATED)`, `ResponseEntity.noContent().build()`).

## Module Design

**Exports:** No explicit public API surface control — package-by-feature packages expose Controller/Service/Repository/Entity/DTOs as public; no `package-private` restriction pattern. Repositories extend Spring Data JPA interfaces (e.g. `JpaRepository<Transaction, UUID>` + `JpaSpecificationExecutor<Transaction>`).

**Barrel Files:** Not applicable (Java has no barrel-file equivalent); package structure itself serves this purpose.

**Validation:** Bean Validation (`jakarta.validation.constraints.*`) annotations directly on request record components, each with a Portuguese `message`. Controllers trigger validation with `@Valid @RequestBody`.
