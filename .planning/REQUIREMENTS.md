# Requirements: Aque Backend

**Defined:** 2026-08-01 (updated 2026-08-01 for milestone v1.0)
**Core Value:** Track and split personal/household expenses reliably, with correct balances and splits above all else.

## Milestone v1.0 Requirements

Scoped 1:1 from the 7 GitHub issues filed after the `/gsd-onboard` codebase audit ([BrininhoBru/aque-backend#7-#13](https://github.com/BrininhoBru/aque-backend/issues)). No new scope beyond these issues.

### Security

- [ ] **SEC-01**: Seeded admin credential is rotated/verified after first deploy to any non-local environment, with the rotation step documented in deploy docs — [GH #7](https://github.com/BrininhoBru/aque-backend/issues/7)

### Testing

- [ ] **TEST-01**: SplitRuleService has a regression test that updates an existing split rule's person list (add + remove in the same request), locking in the flush-before-repopulate ordering behavior — [GH #8](https://github.com/BrininhoBru/aque-backend/issues/8)
- [ ] **TEST-02**: AuthService and CustomUserDetailsService have dedicated unit tests covering the authentication path — [GH #9](https://github.com/BrininhoBru/aque-backend/issues/9)
- [ ] **TEST-03**: PersonService, CategoryService, and RecurringTransactionService/Controller have dedicated unit tests — [GH #13](https://github.com/BrininhoBru/aque-backend/issues/13)

### Tech Debt

- [ ] **DEBT-01**: `loghub-logger` is pinned to a stable (non-SNAPSHOT) release, or removed from `pom.xml` if confirmed unused — [GH #10](https://github.com/BrininhoBru/aque-backend/issues/10)
- [ ] **DEBT-02**: Explicit `CorsConfigurationSource` exists in `SecurityConfig` for future split-origin deploys — [GH #11](https://github.com/BrininhoBru/aque-backend/issues/11)
- [ ] **DEBT-03**: Duplicated `findByActive`/`findByActiveTrue` query methods are collapsed into a single call site — [GH #12](https://github.com/BrininhoBru/aque-backend/issues/12)

## Future Requirements

None deferred from this milestone — all 7 audit issues are in scope.

## Out of Scope

| Feature | Reason |
|---------|--------|
| Multi-user / multi-tenant ownership model | Single-admin personal finance app by design; see PROJECT.md |
| Role-based authorization | No second, lower-privilege user exists |
| httpOnly-cookie JWT storage / token revocation list | Acceptable risk at current single-user scale per CONCERNS.md; revisit only if threat model changes |

## Traceability

| Requirement | GH Issue | Phase | Status |
|-------------|----------|-------|--------|
| SEC-01 | #7 | Phase 1 - Security & Deploy Hardening | Pending |
| DEBT-02 | #11 | Phase 1 - Security & Deploy Hardening | Pending |
| DEBT-01 | #10 | Phase 2 - Dependency & Repository Cleanup | Pending |
| DEBT-03 | #12 | Phase 2 - Dependency & Repository Cleanup | Pending |
| TEST-01 | #8 | Phase 3 - Test Coverage Lock-In | Pending |
| TEST-02 | #9 | Phase 3 - Test Coverage Lock-In | Pending |
| TEST-03 | #13 | Phase 3 - Test Coverage Lock-In | Pending |

**Coverage:**
- v1.0 requirements: 7 total
- Mapped to phases: 7 ✓
- Unmapped: 0

---
*Requirements defined: 2026-08-01*
*Last updated: 2026-08-01 after roadmap creation for milestone v1.0*
