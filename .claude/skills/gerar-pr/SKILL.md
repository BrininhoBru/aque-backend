---
description: Gera título (Conventional Commits) e descrição de Pull Request em português a partir das mudanças da branch atual, com link de issue e checklist de risco (migration, breaking change de API)
argument-hint: [branch-base]
---

## Mudanças desde a base

!`git diff $ARGUMENTS...HEAD --stat`
!`git log $ARGUMENTS...HEAD --oneline`
!`git branch --show-current`

## Issue relacionada

Procure o número da issue, nesta ordem: (1) no nome da branch, se seguir um padrão com número;
(2) em `docs/specs/<N>-*.md` cujo slug bata com o tema da branch/commits; (3) nos commits acima,
se algum citar `#N`. Se achar, adicione `Closes #N` na descrição (ou `Relates to #N` se a PR não
fecha a issue sozinha — ex.: várias PRs pra uma mesma issue grande). Se não achar nenhuma
referência clara, não invente número — pergunte ao usuário ou omita a linha.

Com base nas mudanças acima, gere:

## Título do PR

Formato Conventional Commits: `tipo(escopo): resumo imperativo`, mesmas regras de tipo/escopo da
skill `commit-msg` deste repo. Use `!` depois do tipo pra breaking change (ex.: `feat(auth)!:
...`). Isso importa porque, em squash-merge, o título do PR vira a mensagem do commit final em
`dev`/`main` — sem o formato certo, quebra o Conventional Commits ali.

## Descrição

1. **Resumo** — o que foi feito, em 1-2 frases
2. **Motivação** — por que essa mudança foi necessária
3. **Mudanças** — lista dos principais pontos alterados
4. **Como testar** — passos concretos e verificáveis (ex.: `./mvnw test -Dtest=...`, não "teste a
   feature")
5. **Risco/Impacto** — adiciona migration Flyway nova (`src/main/resources/db/migration/V*.sql`)?
   Muda contrato de API que o `aque-web` consome (rota, payload, a convenção 401/403 documentada
   no `CLAUDE.md`)? Adiciona variável de ambiente nova? Se nenhum, escreva "nenhum"
   explicitamente — não omita a seção
6. **Checklist** — itens que dá pra confirmar em poucos segundos (ex.: "`./mvnw test` passando
   localmente", não "testado"), docs atualizadas (sim/não), breaking changes (sim/não), issue
   linkada (sim/não/n-a)
