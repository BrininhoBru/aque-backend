# Net worth module: manual assets and investments tracking

- **Issue:** #33 — https://github.com/BrininhoBru/aque-backend/issues/33
- **Status:** Implemented
- **Repo:** BrininhoBru/aque-backend

## Problema

O backend hoje só modela receitas/despesas via `Transaction` — não existe conceito de patrimônio (contas, investimentos, ativos com valor atual). Objetivo: permitir cadastro manual de ativos/investimentos (individual ou via import do extrato da B3) e consultar o patrimônio total. Sem integração automática com B3 por ora — não existe API gratuita da B3 para pessoa física; sincronização automática exigiria um agregador Open Finance pago (Pluggy/Belvo).

## Escopo

**Dentro:**
- Entidade `Asset` com CRUD completo: nome, tipo (enum fechado), valor atual, `personId` opcional (reaproveitando `Person`, já usado por `SplitRule`).
- Endpoint de patrimônio total: soma de `currentValue` de todos os ativos.
- Endpoint de import em lote a partir do arquivo **"Posição" exportado pela Área do Investidor da B3** (`.xlsx`), que o usuário já consegue baixar hoje sem custo — formato real confirmado a partir de um export de exemplo.
- Reimportar o mesmo arquivo **atualiza automaticamente** os ativos já cadastrados (casados por nome + tipo) em vez de criar duplicata — cobre o uso real de reimportar periodicamente pra atualizar valores.
- Cada erro de import carrega um indicador de severidade (`informational`) pra diferenciar rodapé/subtotal esperado do export (não é um problema de verdade) de um erro genuíno.

**Fora:**
- Sincronização automática com B3/corretoras (agregador Open Finance pago).
- Histórico de cotação/rentabilidade automática.
- Import de qualquer formato além do export nativo da B3 (CSV genérico, PDF de extrato de corretora, planilhas de outros bancos).
- Tela — tratada na issue companheira `aque-web`.

## Abordagem

Novo módulo `com.aque.asset`, espelhando a estrutura de `com.aque.category` (entidade + controller + service + repository + `dto/request`, `dto/response`):

- `Asset` (`@Entity`, tabela `assets`): `id` (UUID), `name`, `type` (`AssetType` enum: `RENDA_FIXA`, `ACAO`, `FUNDO`, `CRIPTO`, `IMOVEL`, `OUTRO`), `currentValue` (`BigDecimal`), `person` (`@ManyToOne` opcional pra `Person`, mesmo padrão de FK nullable usado em `SplitRuleItem`/`Transaction`).
- Migração `V7__create_assets.sql`: `CREATE TABLE assets` com `CHECK` no `type` (mesmo padrão de `categories.type`/`transactions.status`) e `CHECK (current_value >= 0)` (mesmo padrão de `transactions.amount_expected`).
- `AssetController` em `/assets`: `GET` (lista, filtro opcional `personId`), `POST`, `PUT /{id}`, `DELETE /{id}` — seguindo exatamente o formato de `CategoryController` (Swagger annotations, `@Valid`, `ResponseEntity`).
- `GET /assets/net-worth` retorna `NetWorthResponse { totalValue: BigDecimal }` — soma feita em `AssetService` (query de agregação no repository, mesmo estilo de `DashboardService`).
- `POST /assets/import` (multipart, `MultipartFile`) recebe o `.xlsx` de "Posição" da B3 sem nenhuma edição manual. Esse export tem **4 abas fixas** — `Acoes`, `Fundo de Investimento`, `Renda Fixa`, `Tesouro Direto` — cada uma com colunas diferentes, mas todas com uma coluna `Produto` (nome do ativo) e uma coluna de valor atual:
  - `Acoes`, `Fundo de Investimento`, `Tesouro Direto`: coluna `Valor Atualizado`
  - `Renda Fixa`: três variantes (`Valor Atualizado MTM`, `Valor Atualizado CURVA`, `Valor Atualizado FECHAMENTO`), qualquer uma pode vir como `-` (não populada) — usa a primeira numérica, na ordem CURVA → FECHAMENTO → MTM
  - Mapeamento aba → `AssetType`: `Acoes`→`ACAO`, `Fundo de Investimento`→`FUNDO`, `Renda Fixa`→`RENDA_FIXA`, `Tesouro Direto`→`RENDA_FIXA` (Tesouro Direto é renda fixa na prática; sem valor de enum dedicado por ora)
  - Ativos importados entram com `person = null` (o extrato da B3 não tem noção do `Person` do app)
  - Uma linha sem valor utilizável em nenhuma coluna, uma linha com `Produto` vazio (o export da B3 sempre inclui linhas de rodapé/subtotal em branco no final de cada aba — sem essa checagem viravam "ativos fantasma" sem nome), um valor negativo, ou uma aba com nome fora das 4 esperadas, entra num relatório de erro em vez de abortar a importação inteira — as linhas válidas continuam sendo salvas
  - `Produto` é sempre `trim()`ado antes de salvar (sanitização contra espaços nas bordas do export)
  - Colunas do export não usadas aqui (instituição, quantidade, código ISIN, etc.) são lidas só para localizar nome/valor e não são persistidas — sem histórico de cotação/quantidade por ora
  - **Atualização automática**: para cada linha válida, `AssetRepository.findByNameIgnoreCaseAndType(name, type)` (retorna `List<Asset>`, não `Optional` — não pode quebrar se já existir mais de um registro com mesmo nome+tipo, cenário real já visto em produção local). Se encontrar ao menos um, atualiza o `currentValue` do primeiro (não mexe em `person` — import nunca teve esse conceito); se não encontrar, cria um novo `Asset`.
  - `AssetImportResponse` reflete isso: `{ created: List<AssetResponse>, updated: List<AssetResponse>, errors: List<AssetImportError> }` (`imported` único vira dois campos separados).
  - `AssetImportError` ganha o campo `informational: boolean` — `true` só para a linha de rodapé/subtotal (`Produto` vazio), `false` para os demais casos (valor indisponível, valor negativo, aba desconhecida, coluna `Produto` ausente) — permite a tela do `aque-web` diferenciar "isso é esperado" de "isso é um problema".

## Critério de aceite

- [x] `POST /assets` cria um ativo válido e retorna 201 com `AssetResponse`
- [x] `POST /assets` com `currentValue` negativo retorna 400
- [x] `POST /assets` com `type` fora do enum retorna 400
- [x] `GET /assets` retorna todos os ativos; `GET /assets?personId=X` filtra só os daquela pessoa
- [x] `PUT /assets/{id}` atualiza nome/tipo/valor de um ativo existente; retorna 404 se o id não existe
- [x] `DELETE /assets/{id}` remove o ativo; retorna 404 se o id não existe
- [x] `GET /assets/net-worth` retorna a soma de `currentValue` de todos os ativos cadastrados (0 se não houver nenhum)
- [x] `POST /assets/import` com o `.xlsx` de Posição da B3 cria um ativo por linha válida em cada uma das 4 abas, com o `type` correto por aba
- [x] `POST /assets/import` com uma linha da aba `Renda Fixa` sem MTM/FECHAMENTO usa o valor de CURVA
- [x] `POST /assets/import` com uma linha sem nenhum valor utilizável, ou uma aba com nome inesperado, reporta o problema em `errors` e ainda assim importa as linhas/abas válidas restantes
- [x] `POST /assets/import` com uma linha de `Produto` vazio reporta o erro com `informational: true`; os demais tipos de erro vêm com `informational: false`
- [x] Reimportar o mesmo arquivo depois de já ter ativos cadastrados **atualiza** `currentValue` dos existentes (casados por nome+tipo, case-insensitive) em vez de criar duplicata — resposta reflete isso em `updated`, não em `created`
- [x] Um nome novo (sem correspondência existente) entra em `created`; um nome já cadastrado entra em `updated`

## Questões em aberto

Nenhuma — formato de import confirmado a partir de um export real da B3.
