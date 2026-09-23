# Auditoria de regras de negócio e código (backend)

- **Issue:** #17 — https://github.com/BrininhoBru/aque-backend/issues/17
- **Status:** Auditoria concluída — achados confirmados e decisões do autor registradas; fixes ficam para issues separadas
- **Repo:** BrininhoBru/aque-backend

## Problema

O backend cresceu com módulos de domínio (`transaction`, `split`, `recurring`, `category`,
`person`, `auth`/`security`, `dashboard`) implementados em momentos diferentes, sem uma
revisão cruzada que confirme que as regras de negócio continuam consistentes entre eles —
por exemplo, se o cálculo do dashboard usa a mesma definição de "transação paga" que o
`TransactionService`, ou se `SplitRule`/`SplitRuleItem` garante que os itens somam 100%
tanto na criação quanto na edição. Sem essa revisão, inconsistências e bugs ficam
invisíveis até aparecerem como dado errado em produção.

## Escopo

**Dentro:**
- Revisão de código e regras de negócio dos módulos: `transaction`, `split`, `recurring`,
  `category`, `person`, `auth`, `security`, `dashboard`.
- Checagem de transições de estado (`TransactionStatus.PENDENTE`/`PAGO`), geração de
  transações a partir de `RecurringTransaction` (incluindo `isOverride`/`recurringId`) e
  agregações do `DashboardService`.
- Checagem de invariantes do `SplitRule`/`SplitRuleItem` (soma de valores/percentuais,
  cascade/orphanRemoval).
- Checagem de autorização (JWT, roles) nos controllers e do fluxo de login/`AuthService`.
- Registro de cada achado (bug de código, inconsistência de regra, ou dúvida) num
  checklist nesta spec, com pergunta ao autor antes de virar tarefa de fix.

**Fora:**
- Implementar os fixes dos achados — isso é decidido item a item com o autor e vira
  issue/PR separada.
- Revisão de infraestrutura/deploy (`fluxo-deploy-aque.md`) e de performance/carga.
- Revisão de UI/fluxo do usuário — coberta pela spec irmã em `aque-web`
  (issue aque-web#12).
- Adicionar testes novos como parte desta auditoria (pode virar recomendação, não execução).

## Abordagem

Revisão manual guiada, módulo por módulo, lendo o código de `main` (services, entities,
controllers) e comparando com o comportamento esperado descrito no domínio (finanças
pessoais: transações com valor esperado vs. pago, rateio entre pessoas, recorrência
mensal). Cada módulo gera uma lista de achados classificados como:
- **Bug de código** — comportamento diverge do que o próprio código pretende fazer.
- **Inconsistência de regra de negócio** — dois pontos do sistema tratam a mesma regra
  de forma diferente (ex.: dashboard soma transações PENDENTE de um jeito e o service de
  outro).
- **Dúvida** — comportamento ambíguo que só o autor do produto pode resolver (ex.: o que
  deveria acontecer ao editar um `RecurringTransaction` que já gerou `override`s?).

Toda dúvida é levantada ao autor via pergunta direta antes de ser registrada como bug ou
fechada como intencional — não presumir a resposta.

## Critério de aceite

- [x] Todos os módulos listados em Escopo foram lidos e revisados (services + entities +
      controllers, não só as assinaturas).
- [x] Cada achado (bug ou inconsistência) está documentado nesta spec com: módulo, arquivo,
      descrição do problema, e se foi confirmado ou é hipótese pendente de resposta do
      autor.
- [x] Toda dúvida ambígua foi perguntada ao autor e a resposta está registrada aqui antes
      da spec ser marcada como concluída.
- [x] Nenhum fix é aplicado nesta spec — achados confirmados como bug real viram
      referência para uma issue/PR separada (linkada aqui quando existir).

## Achados

Módulos lidos por completo: `transaction`, `category`, `split`, `recurring`, `person`, `auth`,
`security`, `dashboard` (services, entities, controllers, repositories, DTOs). Itens já
documentados em `.claude/docs/CONCERNS.md` (modelo single-user sem roles, fragilidade do
arredondamento em `getSplit`, teste `getSplit_ultimoItemAbsorveORestoDoArredondamento`
falhando com NPE) não são repetidos abaixo — permanecem válidos e cobrem parte do módulo
`split`/`dashboard`.

- **BUG** transaction — `TransactionService.java:66-82` (`mapperTrasaction`/`applyPayment`) —
  `update()` faz replace completo do lançamento a partir do `TransactionRequest`: todo campo
  não reenviado (ex.: `amountPaid`, `dueDate`) é sobrescrito com `null`, e `applyPayment`
  força o status de volta a `PENDENTE` sempre que `amountPaid` vier nulo. Não existe
  suporte a atualização parcial (PATCH) nem endpoint dedicado para "marcar como pago" —
  qualquer client que edite só um campo (descrição, categoria) sem reenviar `amountPaid`
  desfaz o pagamento e a data de vencimento já registrados. — status: confirmado. Fix
  rastreado em #18.
- **BUG** recurring — `RecurringTransactionJob.java:41-44` (`existsByRecurringIdAndReferenceMonthAndReferenceYear`)
  + `TransactionController.java` (`PUT /transactions/{id}`) — a checagem de idempotência da
  geração mensal usa `recurringId + referenceMonth + referenceYear` da própria linha. Como
  `TransactionService.update()` permite editar `referenceMonth`/`referenceYear` de um
  lançamento vinculado a um recorrente sem nenhuma restrição, mover manualmente a instância
  gerada para outro mês faz o mês original "esvaziar" a checagem de idempotência; uma nova
  chamada a `generate()` (cron do dia 1 ou `POST /recurring/generate/{year}/{month}`) recria
  uma segunda instância para o mesmo recorrente/mês. — status: confirmado. Fix rastreado
  em #19.
- **BUG/GAP** recurring + transaction — `RecurringTransactionJob.java:66-78` (`getTransaction`)
  — a instância gerada nunca recebe `dueDate` (não existe esse campo em
  `RecurringTransactionRequest`/`RecurringTransaction`, e o job não seta
  `transaction.setDueDate(...)`). Como `TransactionRepository.sumOverdue`/`countOverdue`
  (linhas 48 e 57) exigem `dueDate < CURRENT_DATE`, nenhum lançamento recorrente conta como
  atrasado no dashboard a menos que o usuário edite manualmente cada instância para incluir
  uma data — o que, pelo bug acima, também marca `isOverride = true`. Na prática, o
  indicador de "atrasados" do dashboard só funciona para lançamentos avulsos. — status:
  confirmado. Fix rastreado em #19.
- **BUG** transaction + recurring + category — `TransactionService.java:69`,
  `RecurringTransactionService.java:39,52`, `Category.java` — `Transaction.type` e
  `RecurringTransaction.type` são campos independentes, preenchidos direto do request, e
  nunca validados contra `category.getType()`. Um client pode enviar `type=RECEITA` com uma
  `categoryId` cuja categoria é `DESPESA` (ou vice-versa); nada barra isso na criação nem na
  edição. Como o dashboard agrega por `t.type` (não por `category.type`), esse dado
  divergente entra nas somas de receita/despesa do jeito que foi enviado, não do jeito que a
  categoria diz que deveria ser. — status: **confirmado pelo autor** — `type` deve ser
  sempre igual ao `type` da categoria vinculada; falta validação na criação/edição de
  `Transaction` e `RecurringTransaction`. Fix rastreado em #18.
- **BUG** recurring — `RecurringTransactionJob.java:33-49` — a geração é documentada como
  "idempotente" (não duplica), mas se o usuário deletar a instância gerada de um mês
  (`DELETE /transactions/{id}`) e depois `generate()` rodar de novo para o mesmo mês/ano
  (cron ou reprocessamento manual), a instância é recriada do zero. — status: **confirmado
  pelo autor** — a geração deve ser idempotente "por instância": uma vez deletada, não deve
  reaparecer. A checagem hoje (`existsByRecurringIdAndReferenceMonthAndReferenceYear`) não
  distingue "nunca gerada" de "gerada e depois deletada" — precisa de um jeito de marcar
  "não gerar de novo" (ex.: soft-delete/tombstone) em vez de depender só da ausência da
  linha. Fix rastreado em #19.
- **DECISÃO DE PRODUTO (redesenho, fora do escopo desta auditoria)** split + person —
  `SplitRule.java` (campos `referenceMonth`/`referenceYear`), `PersonService.java:40`
  (`isLinkedToSplitRule`) — a pergunta original era sobre o alcance do bloqueio de exclusão
  de `Person` (só meses atuais/futuros vs. para sempre). A resposta do autor muda a premissa:
  `SplitRule` não deveria ser uma configuração por mês — deveria ser **onipresente**,
  configurada uma vez e válida para todos os meses, como uma parametrização à parte. Ao
  editar essa configuração, **meses passados devem manter o split que estava vigente na
  época** (ou seja, precisa de histórico/vigência por data, não uma única regra mutável in
  place). Isso é uma mudança de modelo (schema `split_rules` deixa de ser
  1-linha-por-mês/ano) que afeta `SplitRuleService`, `DashboardService`
  (`getSplit`/`SplitResultResponse`) e o cálculo de qualquer mês já fechado — não é um bug
  pontual, é uma feature nova. Recomendação: abrir uma issue/spec própria para esse
  redesenho em vez de encaixar como fix desta auditoria. — status: decisão registrada,
  aguardando o autor confirmar se quer abrir a spec de redesenho agora ou depois.
- **BUG** split — `SplitRuleItemRequest.java:11-15` — nada impede o mesmo `personId` de
  aparecer mais de uma vez na lista de itens de um `SplitRuleRequest`; só a soma total de
  `percentage` é validada (deve dar 100%). — status: **confirmado pelo autor** — deve ser
  rejeitado como erro de validação. Fix rastreado em #20. Nota:
  esse mesmo request deixa de fazer sentido do jeito que está caso o redesenho acima
  (`SplitRule` onipresente) seja implementado — vale revisitar junto.
- **NÃO É BUG** security — `SecurityConfig.java:50-53` — `/swagger-ui/**` e
  `/v3/api-docs/**` ficam liberados sem autenticação (`permitAll()`), expondo o contrato
  completo da API a qualquer um que alcance o host. — status: **confirmado pelo autor** —
  risco aceito para o deploy atual (Raspberry Pi doméstico). Nenhuma ação necessária; manter
  documentado aqui caso o cenário de deploy mude (ex.: exposição pública na internet).

## Achado adicional (auditoria ao vivo do aque-web, 2026-09-02)

- **BUG/GAP DE DOCUMENTAÇÃO** security/CORS — `SecurityConfig.corsConfigurationSource()` +
  `.env.example` — o fluxo de dev documentado (`aque-web` via proxy → `aque-backend` em
  `:8080`) **não funciona do zero**: login retorna 403 (não 401) porque
  `app.cors.allowed-origins` fica vazio por padrão e o proxy do Angular
  (`changeOrigin: true`) faz o backend ver `Host` e `Origin` diferentes, o que o Spring
  trata como requisição cross-origin de verdade — rejeitada pela lista de origens vazia.
  `.env.example` não cita `CORS_ALLOWED_ORIGINS`, então um setup local novo esbarra nisso
  sem nenhuma pista. Reproduzido ao vivo durante a auditoria de UI do `aque-web` (issue
  aque-web#12) montando infra Docker local. — status: confirmado; falta documentar (no
  mínimo) a necessidade de `CORS_ALLOWED_ORIGINS=http://localhost:4200` (ou a porta usada)
  em dev, ou considerar um default de dev mais amigável. Corrigido diretamente
  (`.env.example` + `CLAUDE.md`) durante a própria auditoria, sem necessidade de issue.
- **BUG (regra de negócio)** person — nada impede cadastrar duas `Person` com o mesmo
  nome (`PersonService`/`PersonRequest`) — `POST /api/persons` aceita duplicatas sem
  rejeição. Isso deixa ambíguo qual pessoa está sendo referenciada em `SplitRuleItem` e em
  transações, já que a UI só exibe o nome. — status: confirmado; falta validação de
  unicidade (provavelmente em `PersonService.create`). Fix rastreado em #23.

## Questões em aberto

Nenhuma — todas as dúvidas levantadas nesta auditoria foram respondidas pelo autor (ver
Achados acima). Fica em aberto apenas a decisão de *quando* abrir a spec de redesenho da
`SplitRule` onipresente.
