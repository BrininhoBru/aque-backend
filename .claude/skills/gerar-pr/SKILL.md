---
description: Gera uma descrição de Pull Request em português a partir das mudanças da branch atual
argument-hint: [branch-base]
---

## Mudanças desde a base

!`git diff $ARGUMENTS...HEAD --stat`
!`git log $ARGUMENTS...HEAD --oneline`

Com base nas mudanças acima, gere uma descrição de PR em português com:

1. **Resumo** — o que foi feito, em 1-2 frases
2. **Motivação** — por que essa mudança foi necessária
3. **Mudanças** — lista dos principais pontos alterados
4. **Como testar** — passos para validar manualmente
5. **Checklist** — testes adicionados, docs atualizadas, breaking changes (sim/não)
