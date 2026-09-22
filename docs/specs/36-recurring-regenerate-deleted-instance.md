# Regerar recorrente cuja instância foi excluída

- **Issue:** #36 — https://github.com/BrininhoBru/aque-backend/issues/36
- **Status:** Implemented
- **Repo:** BrininhoBru/aque-backend

## Problema

Excluir um lançamento gerado a partir de um recorrente remove a `Transaction`
(`TransactionService.delete`), mas deixa viva a linha correspondente em
`recurring_generations`. Esse ledger foi introduzido pela #19 justamente pra tornar a
geração idempotente — mas ele passou a ser a única fonte de verdade sobre "esse mês já
foi gerado?". Resultado: `RecurringTransactionJob.generate` encontra
`alreadyGenerated = true`, pula o recorrente, e o lançamento nunca mais volta. Clicar
"Gerar Recorrentes" na tela de Lançamentos do `aque-web` retorna
`0 instâncias geradas` sem explicar por quê.

Na prática, hoje excluir a instância de um mês é irreversível: a única saída é recriar o
lançamento na mão, sem vínculo com o recorrente (`recurringId` fica nulo).

## Escopo

**Dentro:**
- `TransactionService.delete` apaga a linha de `recurring_generations` correspondente
  quando a transação excluída veio de um recorrente
- `RecurringGenerationRepository` ganha o `deleteBy...` derivado equivalente ao `existsBy...`
  que já existe
- Migration V8 limpando as linhas de ledger que já ficaram órfãs antes deste fix — sem
  ela, quem já bateu no bug continua sem conseguir regerar
- Testes de regressão cobrindo gerar → excluir → gerar de novo, e o predicado da V8

**Fora:**
- Qualquer mudança em `RecurringTransactionJob`, `RecurringTransactionService` ou
  `RecurringTransactionController` — nenhuma assinatura muda, nenhum parâmetro novo na API
- Qualquer mudança no `aque-web`: o botão explícito ("Gerar Recorrentes",
  `transactions.component.html:9`) já existe e já é a ação manual
- Soft delete de `Transaction`, ou registrar "instância excluída de propósito" como
  estado — é um modelo de dados maior do que esse bug pede
- Vincular a `Transaction` à `RecurringGeneration` que a originou (`generation_id`) — ver
  "Limitações conhecidas"
- Mudar o comportamento de `isOverride` ou preservar valores editados de uma instância
  excluída: a regeração recria a partir do template, com `defaultAmount` e
  `status = PENDENTE`

## Abordagem

O bug é um vazamento de estado num lugar só: a linha do ledger sobrevive à transação que
ela representa. A correção fica onde todos os deletes passam, em vez de espalhar guarda
nova pelo job e pelo service.

**`RecurringGenerationRepository`**: método derivado
`deleteByRecurringIdAndReferenceMonthAndReferenceYear`, ao lado do `existsBy...`.

**`TransactionService.delete`**: injeta `RecurringGenerationRepository` (o service já é
`@RequiredArgsConstructor`), vira `@Transactional` — necessário pro `deleteBy...` derivado
e, de quebra, torna as duas exclusões atômicas — e limpa o ledger quando
`transaction.getRecurringId() != null`. Lançamento avulso não toca no ledger.

### Alternativa descartada

A outra rota era `generate(year, month, manual)`: modo manual checando se a `Transaction`
existe (`TransactionRepository.existsByRecurringIdAndReferenceMonthAndReferenceYear`, que
já existe e não é usado nesse fluxo), cron continuando no ledger. Descartada por dois
motivos:

1. O risco que a motivava — "o cron ressuscita o que foi excluído de propósito" — é quase
   inexistente: `generateMonthlyTransactions` usa `LocalDate.now()` e roda uma vez, à
   meia-noite do dia 1, só pro mês corrente. Excluir algo no dia 5 não é alcançado por
   nada.
2. Ela quebra `generate_moverInstanciaParaOutroMes_naoDuplicaAoGerarDeNovo`: com a guarda
   por transação viva, mover o lançamento de março pra abril deixa março vazio e
   "Gerar Recorrentes" cria uma segunda cópia em março. Trocaria um bug por outro.

## Critério de aceite

- [x] Gerar recorrentes para um mês, excluir a `Transaction` gerada, gerar de novo → a
      transação é recriada e a contagem retorna 1
      (`generate_apagarInstanciaGerada_recriaAoGerarDeNovo`)
- [x] Na regeração acima, a linha de `recurring_generations` é **substituída** (id novo) e
      continua sendo exatamente uma — nenhuma duplicata, nenhuma violação da constraint
      `UNIQUE` da V5 (`generate_apagarInstanciaGerada_trocaRegistroDeGeracaoSemDuplicar`).
      Comparar o id é o que discrimina: asserir só "tem uma linha" passava também no código
      sem o fix
- [x] Linhas de ledger já órfãs antes deste fix voltam a permitir geração — migration V8,
      coberta por `ClearOrphanRecurringGenerationsMigrationTest` executando o próprio
      arquivo .sql contra órfã, não-órfã e instância movida de mês
- [x] Gerar duas vezes seguidas sem excluir nada gera 0 na segunda chamada — idempotência
      da #19 preservada (`generate_deveSerIdempotente`, sem edição)
- [x] Mover a instância pra outro mês e gerar de novo não duplica
      (`generate_moverInstanciaParaOutroMes_naoDuplicaAoGerarDeNovo`, sem edição)
- [x] Excluir um lançamento avulso não toca em `recurring_generations`
      (`delete_lancamentoAvulso_naoMexeEmRecurringGenerations`)
- [x] Excluir um lançamento de recorrente limpa a linha do mês daquele recorrente
      (`delete_lancamentoDeRecorrente_limpaRegistroDeGeracao`)
- [x] Um recorrente inativo (`active = false`) não é gerado
      (`generate_recorrenteInativo_naoDeveGerar`, sem edição)
- [x] Todo teste de regressão do bug comprovadamente falha sem o fix: revertendo só
      `TransactionService.java`, `RecurringTransactionJobTest` dá exatamente 2 falhas
      (`..._recriaAoGerarDeNovo` e `..._trocaRegistroDeGeracaoSemDuplicar`), e os unitários
      de `TransactionServiceTest` nem compilam. Os dois primeiros foram escritos antes do
      fix (TDD — `standards.md`); o `..._trocaRegistroDeGeracaoSemDuplicar` e o
      `ClearOrphanRecurringGenerationsMigrationTest` vieram depois, da revisão de código —
      o primeiro teve o vermelho comprovado, o segundo fixa o comportamento da V8 e não tem
      "antes" pra falhar
- [x] Excluir um lançamento de recorrente realmente apaga a `Transaction`, não só a linha
      do ledger (`verify(transactionRepository).delete(existing)` em
      `delete_lancamentoDeRecorrente_limpaRegistroDeGeracao`)
- [x] Docs atualizadas: `.claude/docs/ARCHITECTURE.md` (que descrevia o repositório errado
      na checagem de idempotência) e `README.md`, incluindo o caso do mês futuro

## Limitações conhecidas

- **Excluir instância de mês futuro**: o ledger daquele mês é limpo, então o cron do dia 1
  a recria quando aquele mês chegar. Único caso de "ressurreição" que sobra.
- **Mover e depois excluir**: se a instância de março foi movida pra abril e só então
  excluída, o ledger limpo é o de abril; a linha de março fica órfã e março não regenera.
  O vínculo entre geração e transação é só `recurringId` + o mês *atual* da transação.
  Resolver exige `generation_id` na `Transaction` — issue à parte.
  A variante pior: se março **e** abril já tinham sido gerados, mover a instância de março
  pra abril e excluí-la limpa o ledger de abril — cuja instância legítima continua viva.
  Um `POST /recurring/generate/{ano}/4` depois disso cria uma segunda instância de abril, e
  nada no banco impede (`transactions` não tem `UNIQUE` em
  `recurring_id + reference_month + reference_year`). Exige edição manual seguida de
  geração manual: o job automático só toca o mês corrente.
- **A V8 não distingue "excluído" de "movido de mês"**: ambos deixam a linha do mês
  original sem `Transaction` correspondente, então a migration apaga as duas. Para uma
  instância movida isso significa o mês original gerar uma segunda cópia. O cabeçalho da
  V8 traz o `SELECT` de auditoria pra conferir o que será apagado antes de aplicar; o
  comportamento está fixado em `migracaoV8_instanciaMovidaParaOutroMes_apagaALinhaDoMesOriginal`.
- **Concorrência**: duas chamadas simultâneas de `POST /recurring/generate/{y}/{m}` ainda
  podem correr na inserção do ledger e ter a violação de `UNIQUE` engolida pelo
  `catch (RuntimeException)` do job. Pré-existente da #19, não ampliado aqui.
- Se algum dia o usuário quiser excluir uma instância *definitivamente* (sem que
  "Gerar Recorrentes" a traga de volta), aí sim entra um estado explícito no modelo.
