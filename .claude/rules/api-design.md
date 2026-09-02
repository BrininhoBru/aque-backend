---
paths:
  - "src/main/java/**/*Controller.java"
---

# Design de API

- Context path já é `/api` (configurado globalmente) — rotas dos controllers não repetem
  `/api` nem versionam com `/v1` (ex.: `@RequestMapping("/categories")`, não
  `/api/v1/categories`). Não introduza versionamento na URL sem alinhar com o time antes.
- Rotas no plural (`/categories`, `/persons`, `/recurring`); use kebab-case só se o recurso for
  multi-palavra
- Resposta de erro no formato usado por `GlobalExceptionHandler`: um `record` local
  `{ status: int, message: string, timestamp: LocalDateTime }` — reuse esse formato para novos
  handlers, não invente um DTO de erro diferente
- Sem paginação implementada hoje (`findAll` carrega tudo via `Specification`) — aceitável no
  volume atual (app pessoal); se for adicionar paginação, use query params `page`/`size` e
  inclua `totalElements`/`totalPages` na resposta
- Status codes: 201 para criação, 204 para deleção sem corpo, 400 para erro de validação
  (`MethodArgumentNotValidException`/`ConstraintViolationException`, não 422)
- Toda rota nova precisa de anotações OpenAPI no mesmo nível de detalhe das existentes:
  `@Operation(summary, description, responses)` + `@Parameter` em cada `@RequestParam`/
  `@PathVariable`, com textos em português; controller carrega `@Tag` e
  `@SecurityRequirement(name = "Bearer")` a nível de classe
