# Testing Patterns

## Test Framework

**Runner:**
- JUnit 5 (Jupiter) via `spring-boot-starter-test` + explicit `junit-jupiter` dependency.
- Testcontainers `1.21.4` for Postgres-backed integration tests (`spring-boot-testcontainers`, `testcontainers/postgresql`, `testcontainers/junit-jupiter`, all `scope=test`).

**Assertion Library:** AssertJ (`org.assertj.core.api.Assertions` — `assertThat`, `assertThatThrownBy`), not JUnit's built-in assertions or Hamcrest.

**Mocking:** Mockito via `org.mockito.junit.jupiter.MockitoExtension` for pure unit tests.

**Run Commands:**
```bash
./mvnw test                              # Run all tests (needs Docker for Testcontainers)
./mvnw test -Dtest=TransactionServiceTest # Run a single test class
```

## Test File Organization

**Location:** Mirrors `src/main/java/com/aque/<domain>/` under `src/test/java/com/aque/<domain>/` — one test class per production class being tested, same package.

**Naming:**
- `<ClassUnderTest>Test.java` for both unit and MockMvc/integration tests (e.g. `TransactionServiceTest.java`, `TransactionControllerTest.java`).
- Exception: `RecurringTransactionJobUnitTest.java` (pure Mockito unit test) vs `RecurringTransactionJobTest.java` (Spring/Testcontainers integration test) — when a class needs both a fast unit test and a full-context integration test, disambiguate with a `Unit` infix rather than two identically-named classes in different packages.

**Shared base classes (`src/test/java/com/aque/`):**
- `BaseIntegrationTest.java` — abstract base for all `@SpringBootTest` MockMvc tests. Provides `userRepository`, `passwordEncoder`, `authService`, a `token` field (JWT bearer token string, refreshed in `@BeforeEach`), and a locally-instantiated `objectMapper` (not autowired, to avoid depending on the app context's Jackson config). Any new controller-level integration test should `extends BaseIntegrationTest` to get auth wired for free.
- `TestcontainersConfiguration.java` — `@TestConfiguration` supplying a `PostgreSQLContainer<?>` (`postgres:16-alpine`) via `@ServiceConnection`, imported into `BaseIntegrationTest` with `@Import(TestcontainersConfiguration.class)`.
- `AqueBackendApplicationTests.java` — trivial context-loads smoke test.
- `application-test.properties` (`src/test/resources/`) — test-profile config, activated via `@ActiveProfiles("test")`.

## Test Structure

**Unit test suite (Mockito, no Spring context):**
```java
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @InjectMocks
    private TransactionService service;

    @Test
    void create_semAmountPaid_ficaPendente() {
        var request = new TransactionRequest(...);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response = service.create(request);

        assertThat(response.status()).isEqualTo(TransactionStatus.PENDENTE);
    }
}
```

**Integration test suite (Spring context + Testcontainers Postgres, via MockMvc):**
```java
@AutoConfigureMockMvc
class TransactionControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        transactionRepository.deleteAll();  // clean slate per test, real DB
        categoryRepository.deleteAll();
        // ...
    }

    @Test
    void criarLancamento_semValorPago_deveSerPendente() throws Exception {
        mockMvc.perform(post("/transactions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }
}
```

**Suite organization:**
- Test method naming: `<scenario>_<condition>_<expectedOutcome>` in Portuguese, e.g. `create_semAmountPaid_ficaPendente`, `criarLancamento_comValorPago_deveSerPago`, `update_lancamentoOriginadoDeRecorrente_marcaOverride`, `isTokenValid_tokenExpirado_retornaFalse`. Follow this three-part underscore naming for all new test methods.
- `@BeforeEach setup()` is used consistently to build fresh fixture state per test (never shared mutable static fixtures).
- Integration tests explicitly `deleteAll()` on every repository touched in `setup()` before creating fresh fixtures — don't rely on `@Transactional` rollback alone; this codebase clears tables each test.

## Mocking

**Framework:** Mockito (`@Mock`, `@InjectMocks`, `@ExtendWith(MockitoExtension.class)`).

**Patterns:**
```java
when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
```
- `thenAnswer(inv -> inv.getArgument(0))` is the standard way to mock a JPA `save(...)` call that should just return whatever was passed in (echo pattern) — reuse this instead of hand-building a return entity.
- `ReflectionTestUtils.setField(...)` is used to inject `@Value`-style config fields (e.g. JWT secret/expiration) directly into a manually-`new`'d object under test in pure unit tests that don't use Spring context.

**What to mock:** Repository dependencies (`*Repository`) in service-layer unit tests. Services under test are real (`@InjectMocks`), never mocked.

**What NOT to mock:** Controller-layer tests (`*ControllerTest`) don't mock anything — they run the full Spring context via `@SpringBootTest` with a real Testcontainers Postgres instance and real MockMvc-driven HTTP requests. No `@WebMvcTest` + mocked service layer pattern is used here.

## Fixtures and Factories

No dedicated fixture/factory classes or libraries (no Instancio/EasyRandom/Builder pattern). Test data is constructed inline in each `@BeforeEach` using entity setters or record constructors. No shared fixtures directory — the only shared setup is auth bootstrapping in `BaseIntegrationTest`.

## Coverage

No coverage tool (JaCoCo, etc.) configured, no enforced threshold.

## Test Types

**Unit Tests:** Pure Mockito tests for services and stateless utility classes (JWT signing/parsing) with no Spring context: `TransactionServiceTest`, `SplitRuleServiceTest`, `DashboardServiceTest`, `JwtServiceTest`, `JwtFilterTest`, `RecurringTransactionJobUnitTest`.

**Integration Tests:** `@SpringBootTest` + `@AutoConfigureMockMvc` + Testcontainers Postgres, extending `BaseIntegrationTest`, exercising the full HTTP → Controller → Service → Repository → real DB stack: `TransactionControllerTest`, `CategoryControllerTest`, `SplitRuleControllerTest`, `DashboardControllerTest`, `AuthControllerTest`, `RecurringTransactionJobTest`. Docker must be running locally to execute `./mvnw test`.

**E2E Tests:** Not used at the backend level.

## Common Patterns

**Auth in integration tests:**
```java
// BaseIntegrationTest.setupAuth() runs before every integration test:
userRepository.deleteAll();
User user = new User();
user.setUsername("test");
user.setPassword(passwordEncoder.encode("test123"));
userRepository.save(user);
token = "Bearer " + authService.login(new LoginRequest("test", "test123")).token();
```

**Exception assertion:**
```java
assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(BusinessException.class)
        .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
```
Use `.extracting("status")` (reflection-based) rather than casting, since `BusinessException` only exposes `status` via `@Getter`.

**BigDecimal comparison:**
```java
assertThat(response.amountPaid()).isEqualByComparingTo("480");
```
Use `isEqualByComparingTo` (not `isEqualTo`) for `BigDecimal` fields to avoid scale-mismatch false negatives.
