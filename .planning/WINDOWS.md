---
schema_version: 1
open_count: 1
waived_count: 0
fixed_count: 0
total_count: 1
last_updated: 2026-08-02T12:36:33.514Z
---

# Broken Windows Ledger

> Cross-phase defect register. `/gsd-ship` blocks while `open_count > 0`.
> Waive with `gsd-tools windows waive <id> "<reason>"` (reason required).
> Mark fixed with `gsd-tools windows fixed <id>`.

| id | phase | kind | file | line | description | status | reason | recorded_at | resolved_at |
|----|-------|------|------|------|-------------|--------|--------|-------------|-------------|
| 1 | 01 | deviation | src/test/java/com/aque/dashboard/DashboardServiceTest.java | 93 | getSplit_ultimoItemAbsorveORestoDoArredondamento fails with NPE on full ./mvnw test run — pre-existing, unrelated to 01-01 CORS changes (see deferred-items.md, CONCERNS.md rounding-remainder debt) | open |  | 2026-08-02T12:36:33.514Z |  |

````json
[
  {
    "id": 1,
    "kind": "deviation",
    "phase": "01",
    "file": "src/test/java/com/aque/dashboard/DashboardServiceTest.java",
    "line": 93,
    "description": "getSplit_ultimoItemAbsorveORestoDoArredondamento fails with NPE on full ./mvnw test run — pre-existing, unrelated to 01-01 CORS changes (see deferred-items.md, CONCERNS.md rounding-remainder debt)",
    "status": "open",
    "reason": "",
    "recorded_at": "2026-08-02T12:36:33.514Z",
    "resolved_at": null
  }
]
````
