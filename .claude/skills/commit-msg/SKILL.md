---
name: commit-msg
description: Use this skill when the user asks to "write a commit message", "generate a commit", "create a commit", "fazer commit", "mensagem de commit", "gerar commit", or when staging changes and preparing a git commit. Provides rules for Conventional Commits format in English with scope and ticket prefix from branch name, and commits directly.
version: 1.0.0
---

# Commit Message Generation Instructions

## Language
- Always write commit messages in **English**.

## Format
- Use the **Conventional Commits** standard.
- Always include the **scope** in parentheses after the type.
- Choose the correct type based on the table below:

| Type | When to use |
|------|------------|
| `feat` | New feature for the user or system |
| `fix` | Bug fix |
| `refactor` | Restructuring of **source code** without changing behavior (renaming variables, extracting methods, reorganizing classes) |
| `chore` | Maintenance tasks that **don't change source code or tests** (configs, .env, .gitignore, dependencies, build scripts) |
| `docs` | Changes only to documentation (README, comments, Javadoc) |
| `style` | Code formatting without changing logic (whitespace, indentation, semicolons) |
| `test` | Adding or fixing tests |
| `perf` | Performance improvement |
| `ci` | Changes to CI/CD pipelines (GitHub Actions, Jenkins, etc.) |
| `build` | Changes to the build system (pom.xml, Gradle, Dockerfile) |
| `revert` | Reverting a previous commit |

### Important rules for choosing the type
- Config files (.env, .gitignore, application.properties) → `chore`
- Changes to pom.xml, Dockerfile, docker-compose → `build`
- **Never** use `refactor` for files that aren't source code (e.g. configs, scripts, docs)

## First line (subject)

### Structure
```
[TASK-XXX] type(scope): imperative summary of what was done
```

### Rules
1. Check the current branch name.
2. If the branch starts with a ticket pattern (e.g. `TASK-123`, `ORD-456`, `BUG-789` —
   any prefix followed by a hyphen and numbers), extract that reference and place it
   at the **start** of the first line in brackets.
3. If the branch has **no** ticket reference, omit the brackets and start directly
   with the type.
4. The summary must be written in the **imperative mood** (e.g. "add", "fix",
   "remove", "update").
5. First letter of the summary in **lowercase**.
6. **Do not** end with a period.
7. **Do not** use emojis.

### Examples
- Branch `TASK-123-add-order-filter`:
  ```
  [TASK-123] feat(orders): add order filter by status
  ```
- Branch `refactor-orders` (no ticket):
  ```
  refactor(orders): reorganize response DTO structure
  ```

## Commit body

Write the body with the reviewer in mind, not as a change log — the goal is that
someone doing code review understands the *reasoning* without needing to open the
full diff first.

- Include a body **only when necessary** (complex changes, non-obvious decisions,
  anything a reviewer would otherwise have to ask about)
- Separate the body from the first line with a **blank line**
- Break the body into **bullet points** using `-`
- Each bullet point explains the **why**, not just the what: the reasoning, trade-off,
  or context behind the change — not a restatement of the diff
- Keep it short and objective; if it needs more than ~5 bullet points, the change is
  probably too large for one commit — consider splitting it

### Example with body
```
[ORD-456] feat(orders): add order cancellation support

- Cancellation only allowed before shipment, per business rule from ORD-456
- Chose a dedicated endpoint over a generic PATCH to keep the status transition
  explicit and easy to validate
```

### Example with minimal body
```
chore(config): update gitignore rules
```

## Breaking Changes

- For changes that break compatibility, add `BREAKING CHANGE:` in the **body**
  of the message.
- Describe what changed and the impact.

### Example
```
[TASK-789] refactor(auth): change authentication token structure

- Migrate from plain JWT to JWT with custom claims, needed to carry role info
  without an extra lookup on every request

BREAKING CHANGE: token format changed, clients need to update their parser
```

## Commit authorship

- **Never** add `Co-Authored-By` or any reference to the assistant in the commit
  message.
- The commit must contain only the user's authorship.

## Execution

- Build the commit message following the rules above and run `git commit` with it
  **directly** — do not ask for approval first.
- After committing, show the user the exact message that was committed, so they can
  amend it if something's off.
- **Never** run `git push` — local commit only.
