# Technology Stack

## Languages

**Primary:** Java 25 — all application code, `pom.xml` (`<java.version>25</java.version>`)
**Secondary:** SQL (PostgreSQL dialect) — Flyway migrations, `src/main/resources/db/migration/V*.sql`

## Runtime

**Environment:** JDK 25 (Eclipse Temurin in Docker: `eclipse-temurin:25-jre` runtime image)

**Package Manager:**
- Maven (wrapper committed: `./mvnw`)
- Lockfile: N/A for Maven (dependency versions pinned in `pom.xml`, most inherited from `spring-boot-starter-parent`)
- Custom repository: GitHub Packages (`https://maven.pkg.github.com/LogHub-Open/loghub-sdk`) — requires Maven settings with GitHub auth (`maven_settings` Docker secret in `Dockerfile`)

## Frameworks

**Core:**
- Spring Boot 3 (parent version `4.0.0` in `pom.xml`) — `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`
- Spring Data JPA / Hibernate — ORM layer, entities under `src/main/java/com/aque/<domain>/`
- Spring Security — stateless JWT auth, `src/main/java/com/aque/config/SecurityConfig.java`, `src/main/java/com/aque/security/`

**Testing:**
- JUnit 5 (`junit-jupiter`) — `src/test/`
- Testcontainers 1.21.4 (`spring-boot-testcontainers`, `testcontainers:postgresql`, `testcontainers:junit-jupiter`) — real Postgres in tests
- Spring Security Test (`spring-security-test`)
- `spring-boot-starter-webmvc-test` — MVC slice tests

**Build/Dev:**
- Maven (`maven-compiler-plugin`, `spring-boot-maven-plugin`, `maven-surefire-plugin`)
- Lombok (optional, annotation processor) — boilerplate reduction on entities/DTOs
- Maven profiles: `dev` (default), `prod`, `test` — set `spring.profiles.active` via `-P<profile>`

## Key Dependencies

**Critical:**
- `io.jsonwebtoken:jjwt-api/impl/jackson` 0.12.6 — JWT issuance/parsing (`src/main/java/com/aque/security/JwtService.java`)
- `org.postgresql:postgresql` (runtime) — JDBC driver
- `org.flywaydb:flyway-database-postgresql` + `spring-boot-starter-flyway` — schema migrations
- `io.loghub:loghub-logger` 0.1.0-SNAPSHOT — pulled from private GitHub Packages repo; declared but not referenced anywhere in `src/main/java` (no call sites)

**Infrastructure:**
- `org.springdoc:springdoc-openapi-starter-webmvc-ui` 2.8.6 — OpenAPI/Swagger UI generation
- Logback (`src/main/resources/logback-spring.xml`) — default Spring Boot logging backend

## Configuration

**Environment:**
- Profile-based: `application.properties` (shared) + `application-dev.properties` / `application-prod.properties` / `application-test.properties` (overrides)
- Active profile via `spring.profiles.active` (defaults to `dev`, set per Maven profile in `pom.xml`)
- Secrets/env vars documented in `.env.example`: `DB_USER`, `DB_PASS`, `DB_HOST`, `DB_PORT`, `DB_NAME`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `CORS_ALLOWED_ORIGINS`
- `app.jwt.secret` has **no default in base `application.properties`** — boot fails fast if `JWT_SECRET` unset in prod; dev profile supplies an insecure fallback secret for local convenience only
- `app.cors.allowed-origins` (env `CORS_ALLOWED_ORIGINS`) defaults to empty — same-origin only until a split-origin deploy sets it

## Build

- `pom.xml` — single-module Maven project, no submodules
- `Dockerfile` — multi-stage ARM64 build (`maven:3.9-eclipse-temurin-25` build stage → `eclipse-temurin:25-jre` runtime stage), Maven settings injected via Docker BuildKit secret (`maven_settings`) for the private GitHub Packages repo auth

## Platform Requirements

**Development:**
- JDK 25, Docker (for Testcontainers-based tests), local or containerized PostgreSQL
- `./mvnw spring-boot:run` for local dev server (context path `/api`, port 8080 default)

**Production:**
- Target platform: Raspberry Pi 3B (ARM64) — see repo-root `fluxo-deploy-aque.md`
- Runtime tuned for constrained hardware: `-Xmx256m -Xms128m -XX:+UseSerialGC`, `spring.main.lazy-initialization=true`, HikariCP capped at `maximum-pool-size=5` (`application-prod.properties`)
- Deployed via Docker image built in GitHub Actions, pulled on the Pi
