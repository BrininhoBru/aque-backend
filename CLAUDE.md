# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the app (dev profile, needs Postgres — see .env.example)
./mvnw spring-boot:run

# Run all tests (JUnit 5 + Testcontainers, needs Docker)
./mvnw test

# Run a single test class
./mvnw test -Dtest=TransactionServiceTest

# Build jar
./mvnw clean package
```

## Architecture

**aque-backend** is a Spring Boot 3 (Java 25) REST API for a personal finance app, serving the `aque-web` Angular SPA. All routes are under `/api` (`server.servlet.context-path`).

### Package-by-feature layout

`src/main/java/com/aque/<domain>/` — each domain (`auth`, `user`, `transaction`, `category`, `person`, `recurring`, `split`, `dashboard`) follows the same shape:

```
<domain>/
  <Domain>Controller.java
  <Domain>Service.java
  <Domain>Repository.java
  <Domain>.java          # JPA entity
  dto/                   # request/response DTOs
```

Cross-cutting concerns live outside the domains: `config/` (`SecurityConfig`, `OpenApiConfig`), `security/` (`JwtFilter`, `JwtService`, `CustomUserDetailsService`), `exception/` (global exception handling).

### Auth

Stateless JWT auth. `JwtFilter` validates the `Authorization: Bearer` header on every request; `JwtService` issues/parses tokens (secret + expiry via `app.jwt.secret` / `app.jwt.expiration-ms`, both env-overridable — see `.env.example`). No sessions.

### Database

PostgreSQL, schema managed by **Flyway** (`src/main/resources/db/migration/V*.sql`, `ddl-auto=validate` — never let Hibernate auto-generate schema, add a new `V{n}__description.sql` migration instead). Tests use Testcontainers to spin up a real Postgres.

### Config profiles

`application.properties` holds shared config; `application-dev.properties` / `application-prod.properties` hold profile overrides. Active profile defaults to `dev` (`spring.profiles.active`).

### API docs
Swagger UI at `/api/swagger-ui.html`, OpenAPI JSON at `/api/v3/api-docs` (springdoc).
