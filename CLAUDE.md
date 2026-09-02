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

Lint/format: no formatter is configured yet (no Spotless/Checkstyle, no `.editorconfig`) —
match the indentation and style of neighboring files.

## Architecture

**aque-backend** is a Spring Boot 4 (Java 25) REST API for a personal finance app, serving the `aque-web` Angular SPA. All routes are under `/api` (`server.servlet.context-path`).

### Package-by-feature layout

`src/main/java/com/aque/<domain>/` — each domain (`auth`, `user`, `transaction`, `category`, `person`, `recurring`, `split`, `dashboard`) follows the same shape:

```
<domain>/
  <Domain>Controller.java
  <Domain>Service.java
  <Domain>Repository.java
  <Domain>.java          # JPA entity
  dto/request/, dto/response/   # request/response DTOs (records)
```

Cross-cutting concerns live outside the domains: `config/` (`SecurityConfig`, `OpenApiConfig`), `security/` (`JwtFilter`, `JwtService`, `CustomUserDetailsService`), `exception/` (global exception handling), `health/` (Docker healthcheck endpoint).

### Auth

Stateless JWT auth. `JwtFilter` validates the `Authorization: Bearer` header on every request; `JwtService` issues/parses tokens (secret + expiry via `app.jwt.secret` / `app.jwt.expiration-ms`, both env-overridable — see `.env.example`). No sessions.

**401 vs 403 contract with `aque-web`:** `401` is returned only by `GlobalExceptionHandler.handleBadCredentials` for wrong credentials at `POST /auth/login`. A missing, invalid, or expired token on any protected route falls through to Spring Security's default (`403`), since `JwtFilter` never populates the `SecurityContext` for those requests — there's no custom `AuthenticationEntryPoint` overriding it. `aque-web`'s `authInterceptor` depends on this exact split to decide when to force a logout. If this ever changes (e.g. adding an `AuthenticationEntryPoint` that returns 401 for missing tokens), update the matching note in `aque-web/CLAUDE.md` and its `authInterceptor`.

### Database

PostgreSQL, schema managed by **Flyway** (`src/main/resources/db/migration/V*.sql`, `ddl-auto=validate` — never let Hibernate auto-generate schema, add a new `V{n}__description.sql` migration instead). Tests use Testcontainers to spin up a real Postgres.

### Config profiles

`application.properties` holds shared config; `application-dev.properties` / `application-prod.properties` hold profile overrides. Active profile defaults to `dev` (`spring.profiles.active`).

### API docs
Swagger UI at `/api/swagger-ui.html`, OpenAPI JSON at `/api/v3/api-docs` (springdoc).

### CORS
Explicit `CorsConfigurationSource` in `SecurityConfig`, allowed origins from `app.cors.allowed-origins` (env `CORS_ALLOWED_ORIGINS`, empty by default). **Required in local dev**: `aque-web`'s dev proxy uses `changeOrigin: true`, so the backend sees a `Host` header that doesn't match the browser's `Origin` header — Spring treats this as a real cross-origin request even though it's same-origin from the browser's point of view. Without `CORS_ALLOWED_ORIGINS=http://localhost:4200` (or whatever port the dev server uses) in `.env`, every request from `aque-web` — including `/auth/login` — is rejected with `403` before it reaches any controller. In production this stays empty, since Nginx proxies same-origin with no `changeOrigin` rewrite.

## Reference docs

Deeper, verified-against-code analysis lives in `.claude/docs/`: `ARCHITECTURE.md` (layers, data flow, anti-patterns), `CONVENTIONS.md` (naming, error handling, style), `STACK.md` (dependencies, versions), `STRUCTURE.md` (where to add new code), `TESTING.md` (test patterns/fixtures), `INTEGRATIONS.md` (external services, env vars), `CONCERNS.md` (tech debt, known bugs, fragile areas). Read the relevant one before a structural change or when this file doesn't have the answer.

Narrower, path-triggered conventions live in `.claude/rules/` (style, testing, security, API design, migrations, team-wide standards) and load automatically based on the files you touch.

## Key decisions

- **Single-admin, no ownership model** — personal/household finance app, not multi-tenant SaaS. Don't add `owner_id`/multi-tenancy without a real second-user requirement.
- **Flyway over Hibernate auto-DDL** — predictable, reviewable schema changes in production.
- **Stateless JWT, no sessions** — simple REST API consumed by one SPA (`aque-web`).

## Workflow

- Never commit directly to `main` or `dev` — always via PR (see `.claude/rules/standards.md`). PRs target `dev`; `main` only moves via a `dev` → `main` release PR, which is what triggers `docker-publish.yml` (a push to `dev` builds/deploys nothing).
- Commit messages follow Conventional Commits (`feat:`, `fix:`, `chore:`...) — the `commit-msg` skill generates and commits directly.
- Known gaps against `.claude/rules/standards.md`'s mandatory tooling: no automated lint/format, no pre-commit hook, no semantic-release/automatic versioning, and CI (`docker-publish.yml`) doesn't gate on lint/tests — none of that tooling is set up in this repo yet.

## Common flows

- Security review of a diff: `/security-review`
- Generate a PR description from the git diff: `/gerar-pr`
- Generate a commit message (Conventional Commits, English, no ticket prefix — this team doesn't use a ticket/board on branch names): skill `commit-msg` — triggers automatically on "commit this" / "write a commit message", and commits directly
- Standalone code review (no edits): delegate to the `code-reviewer` subagent
- Investigate a bug/failing test: delegate to the `debugger` subagent
