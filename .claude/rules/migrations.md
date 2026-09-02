---
paths:
  - "src/main/resources/db/migration/**/*.sql"
---

# Migrations (Flyway)

- Nome do arquivo: `V{n}__descricao_em_snake_case.sql`, `{n}` sequencial a partir da última
  migration existente — nunca reutilize ou reordene um número já aplicado
- Migrations são a única fonte de verdade do schema — `ddl-auto=validate` no Hibernate, nunca
  `update`/`create`; toda coluna nova precisa de migration **e** do `@Column(name = "...")`
  correspondente na entidade JPA (explícito quando o nome não é o mapeamento
  camelCase→snake_case óbvio)
- Migrations já aplicadas em qualquer ambiente (dev, prod, CI) nunca são editadas
  retroativamente — uma correção vira uma nova migration
- Mudança de schema que afeta dado existente (rename, drop, not-null novo) inclui o
  `UPDATE`/backfill necessário no mesmo arquivo ou em uma migration anterior na mesma PR
