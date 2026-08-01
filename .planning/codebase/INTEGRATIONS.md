# External Integrations

**Analysis Date:** 2026-08-01

## APIs & External Services

**None detected.** No outbound HTTP clients (`RestTemplate`, `WebClient`, `@FeignClient`) found in `src/main/java`. This is a self-contained REST API with no third-party API calls (payment, email, SMS, etc.).

**Package registry (build-time only):**
- GitHub Packages — `io.loghub:loghub-logger` dependency resolved from `https://maven.pkg.github.com/LogHub-Open/loghub-sdk` (`pom.xml`)
  - Auth: GitHub token via Maven `settings.xml`, injected as Docker BuildKit secret `maven_settings` in `Dockerfile` (not committed, not in `.env.example`)
  - Note: dependency is declared but has no call sites in `src/main/java` — not actively used at runtime as of this analysis

## Data Storage

**Databases:**
- PostgreSQL (single database)
  - Connection: `spring.datasource.url` built from `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS` env vars (`application-prod.properties`); hardcoded `jdbc:postgresql://localhost:5432/aque` in dev (`application-dev.properties`)
  - Client/ORM: Spring Data JPA + Hibernate (`spring-boot-starter-data-jpa`), driver `org.postgresql:postgresql`
  - Schema managed by Flyway migrations: `src/main/resources/db/migration/V1__create_schema.sql` … `V4__add_due_date_to_transactions.sql`; `ddl-auto=validate` (Hibernate never auto-generates schema)
  - Prod pool: HikariCP, `maximum-pool-size=5`, `minimum-idle=2`, `connection-timeout=20000` (tuned for Raspberry Pi 3B)
  - Test databases: real Postgres via Testcontainers (`spring-boot-testcontainers`, `org.testcontainers:postgresql`), injected via `@ServiceConnection` (see test `TestcontainersConfiguration`)

**File Storage:**
- None — no file/object storage integration found (no S3, GCS, local upload endpoints detected)

**Caching:**
- None — no Redis/Caffeine/Spring Cache usage detected

## Authentication & Identity

**Auth Provider:**
- Custom, self-hosted JWT auth (no external identity provider / OAuth)
  - Implementation: `src/main/java/com/aque/security/JwtFilter.java` (validates `Authorization: Bearer` header per request), `src/main/java/com/aque/security/JwtService.java` (issues/parses tokens via `io.jsonwebtoken` / jjwt 0.12.6), `src/main/java/com/aque/security/CustomUserDetailsService.java`
  - Config: `src/main/java/com/aque/config/SecurityConfig.java`
  - Stateless — no server-side sessions
  - Secret/expiry: `app.jwt.secret` (env `JWT_SECRET`, no default outside dev/test — boot fails if unset in prod), `app.jwt.expiration-ms` (env `JWT_EXPIRATION_MS`, default `86400000` ms / 24h)
  - Login/registration flow: `src/main/java/com/aque/auth/` (`AuthController`, `AuthService`, DTOs under `auth/dto/`)

## Monitoring & Observability

**Error Tracking:**
- None (no Sentry, Bugsnag, or similar detected)

**Logs:**
- Logback (Spring Boot default), configured in `src/main/resources/logback-spring.xml`
- Log levels per profile: dev sets `com.aque=DEBUG`, `org.springframework.security=DEBUG`; prod sets `com.aque=INFO`, `org.springframework.security=WARN`
- `io.loghub:loghub-logger` dependency present in `pom.xml` but unused in code — potential future log-shipping integration, not wired up yet

## CI/CD & Deployment

**Hosting:**
- Self-hosted on a Raspberry Pi 3B (see repo-root `fluxo-deploy-aque.md`), production runtime is a Docker container built from `Dockerfile`

**CI Pipeline:**
- GitHub Actions builds the Docker image; image is pulled and run on the Pi (details in repo-root `fluxo-deploy-aque.md`, not in this repo)

## Environment Configuration

**Required env vars** (`.env.example`):
- `DB_USER`, `DB_PASS`, `DB_HOST` (default `localhost`), `DB_PORT` (default `5432`), `DB_NAME` (default `aque_db`)
- `JWT_SECRET` (required, no default in prod), `JWT_EXPIRATION_MS` (default `86400000`)

**Secrets location:**
- Local dev: `.env` file (gitignored, templated by `.env.example`) — not read by Spring directly; must be exported to the process environment or supplied via `./mvnw` / Docker Compose env injection
- Docker build: Maven GitHub Packages credentials supplied via BuildKit secret `maven_settings`, not stored in this repo
- Production secrets (DB creds, JWT secret) are injected as container environment variables at deploy time (see repo-root `fluxo-deploy-aque.md`)

## Webhooks & Callbacks

**Incoming:**
- None — no webhook receiver endpoints found

**Outgoing:**
- None — no outbound webhook calls found

---

*Integration audit: 2026-08-01*
