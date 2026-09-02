---
description: Revisa mudanças de código em busca de vulnerabilidades de segurança, falhas de autenticação e riscos de injeção
disable-model-invocation: true
argument-hint: <branch-ou-path>
---

## Diff a revisar

!`git diff $ARGUMENTS`

Audite as mudanças acima quanto a:

1. Vulnerabilidades de injeção (SQL, XSS, command injection)
2. Falhas de autenticação e autorização
3. Segredos ou credenciais hardcoded
4. Validação de entrada ausente

Use o checklist.md nesta pasta como referência completa.

Reporte cada achado com nível de severidade e sugestão de correção.
