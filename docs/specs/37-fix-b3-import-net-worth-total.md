# Corrigir divergência entre o total da B3 e o patrimônio do app

- **Issue:** #37 — https://github.com/BrininhoBru/aque-backend/issues/37
- **Status:** Implemented
- **Repo:** BrininhoBru/aque-backend

## Problema

Depois de importar o .xlsx de "Posição" da B3, `GET /assets/net-worth` não bate com o
total do extrato.

Diagnóstico rodado contra um arquivo real de "Posição" isolou a causa: **o upsert do
import colapsa linhas distintas que compartilham o mesmo valor na coluna `Produto`**. Em
`AssetService.importSheet`, cada linha fazia `findByNameIgnoreCaseAndType(produto, tipo)`
e, achando qualquer ativo com aquele nome e tipo, **sobrescrevia** o `current_value` em vez
de criar um segundo ativo.

Isso não é caso de borda: na aba `Renda Fixa` é rotineiro ter várias aplicações no mesmo
papel do mesmo emissor, distintas apenas por data de emissão, vencimento e pela coluna
`Código`. No arquivo diagnosticado, uma única colisão dessas derrubou mais da metade do
patrimônio total.

Os totais que o importador **lê** batem exatamente com os rodapés `Total` de cada aba da
B3 — o parse está correto, a perda acontece na persistência. E como `AssetImportResponse`
só reportava `created`/`updated`/`errors`, nunca um total, a divergência era invisível: o
import "dava certo", a tela mostrava um número, e nada na resposta denunciava que faltou
dinheiro ali dentro.

### Candidatos descartados no diagnóstico

Ficam registrados porque são plausíveis à leitura do código e não vale alguém reabrir a
investigação por eles:

- **Abas não mapeadas** (`resolveAssetType` cobre só `Ações`, `Fundo de Investimento`,
  `Renda Fixa`, `Tesouro Direto`): o arquivo real tem exatamente essas quatro abas.
  Continua sendo um risco se a B3 passar a exportar BDR/ETF/cripto — mas não é a causa.
- **Escolha da coluna de valor na Renda Fixa** (`CURVA` → `FECHAMENTO` → `MTM`): no
  arquivo real só `Valor Atualizado CURVA` é numérica; `MTM` e `FECHAMENTO` vêm como `-`
  (texto). `cellAsNumeric` já devolve `null` pra célula de texto, então a ordem atual
  acerta. Comportamento correto por construção, não por sorte.
- **Ativos órfãos** (ativo que saiu do extrato novo nunca era removido): comparando dois
  extratos consecutivos, nenhum produto sumiu — o bug é real mas ainda não se manifestou.
  Entra no escopo porque distorce o total da mesma forma assim que algo for vendido ou um
  título vencer.

## Escopo

**Dentro:**
- Upsert passa a usar a chave natural da linha (o código da posição), não o nome do produto
- Reconciliação no `AssetImportResponse`: total lido por aba vs. total persistido
- Ativos de import anterior ausentes do arquivo são **reportados**, nunca removidos
- Arredondamento explícito na leitura da célula

**Fora:**
- Redesenhar a tela de patrimônio no `aque-web` — a `missing` e a reconciliação chegam na
  resposta mas não têm onde aparecer ainda; issue própria naquele repo
- Histórico de posição (valor do ativo ao longo do tempo): `Asset` continua guardando só
  `current_value`
- Cotação automática / integração com API de mercado
- Outros formatos de extrato (corretora, banco) — o escopo é o .xlsx de "Posição" da Área
  do Investidor da B3
- Mapear abas que a B3 hoje não exporta pra essa conta (BDR, ETF, cripto) e os valores de
  `AssetType` que elas exigiriam
- Documentar o módulo de patrimônio no `README.md`/`ARCHITECTURE.md`: ele nunca foi
  documentado lá (lacuna que veio da #33), e resolver isso aqui seria escopo alheio. A
  mudança de contrato do import está na anotação OpenAPI do endpoint
- Rateio de ativo por pessoa: o import continua deixando `person_id` nulo

**Nunca:** o arquivo real de posição não entra no repositório, nem valores, nomes de
produto, CNPJ, instituição ou número de conta dele — em spec, teste, fixture, log, commit
ou issue. `aque-backend` é um repositório **público**. Todo dado de teste é inventado.

## Abordagem

### 1. Chave natural: `external_code`

Toda aba traz um identificador estável da posição, e o importador ignorava os quatro:

| Aba | Coluna |
|---|---|
| `Ações`, `Fundo de Investimento` | `Código de Negociação` |
| `Renda Fixa` | `Código` |
| `Tesouro Direto` | `Código ISIN` |

`V9__add_external_code_to_assets.sql` adiciona `external_code VARCHAR(50)` (nullable).
Nullable porque ativo cadastrado à mão (imóvel, cripto fora da B3) não tem código. Sem
`UNIQUE`: os manuais são todos `NULL` e a unicidade só valeria entre os importados — regra
fraca demais pra um índice parcial carregar.

### 2. Upsert em duas etapas, e a adoção do dado legado

`AssetService.findExisting`:

1. `findByExternalCode(codigo)` → achou, atualiza (inclusive o nome, se o produto foi
   renomeado mantendo o código).
2. Senão, `findByNameIgnoreCaseAndTypeAndExternalCodeIsNull(nome, tipo)` → achou, **adota**:
   atualiza o valor e grava o código.
3. Senão, cria.

O `ExternalCodeIsNull` do passo 2 é o que impede o bug de voltar pela porta dos fundos:
assim que a primeira linha adota o ativo legado, ele deixa de ser candidato, e a segunda
linha com outro código cai no passo 3 e vira ativo novo. É também o que faz o primeiro
import pós-deploy migrar o dado sozinho, sem backfill na migration — não há de onde
derivar o código das linhas existentes.

### 3. Reconciliação — a rede que protege qualquer escolha de chave

`AssetImportSheetSummary` traz, por aba, `totalRead` (somado durante o parse) e
`totalPersisted` (somado sobre os ativos distintos efetivamente tocados, via mapa chaveado
pelo **id** da linha no banco). `AssetImportResponse` acumula os dois no total geral.

Chavear pela identidade do objeto `Asset` funcionaria só enquanto um mesmo
`EntityManager` atendesse o import inteiro (`open-in-view` ligado, o default do Boot). Sem
isso, `save` pode devolver instâncias diferentes pra mesma linha, duas linhas colapsando
viram duas chaves, e o detector reportaria "tudo certo" justamente durante um colapso.

E divergir não basta aparecer num campo da resposta: sai em `log.warn` e entra em
`errors`, que é o que a tela já renderiza. Só devolver os dois números num JSON repetiria o
erro que deixou esse bug passar despercebido.

Campos aditivos — `AssetImportResult` no `aque-web` ignora o que não conhece.

### 4. Órfãos: reportar, nunca apagar

Ativo com `external_code` que não apareceu no arquivo entra em `missing`. O import não
apaga: subir um extrato parcial ou filtrado por engano não pode custar registros, e um
ativo importado com pessoa atribuída à mão perderia a atribuição junto. Ativo manual
(`external_code IS NULL`) nunca entra — ele nunca veio de extrato.

Consequência aceita: o total só fecha depois que o usuário age na tela.

### 5. Precisão

`cellAsNumeric` passa a fazer `setScale(2, HALF_UP)` na leitura. O `double` do POI carrega
ruído binário, e sem isso o valor comparado na reconciliação não seria o mesmo que o
Postgres guarda em `NUMERIC(15,2)`.

## Critério de aceite

- [x] Duas linhas com o mesmo `Produto` e `Código` diferente viram **dois ativos**, e a soma
      bate com a soma das linhas
      (`importFromXlsx_duasLinhasMesmoProdutoCodigosDiferentes_criaDoisAtivos`)
- [x] O teste acima usa um repositório com estado (`stubStatefulRepository`): com o mock
      devolvendo vazio em toda busca, nenhum caminho de upsert seria exercitado e o teste
      passaria sem provar nada
- [x] Ponta a ponta com Postgres real: importar as duas aplicações e conferir que
      `GET /assets/net-worth` bate com o arquivo
      (`importarPosicaoB3_duasAplicacoesNoMesmoPapel_patrimonioTotalBateComOArquivo`)
- [x] `AssetImportResponse` expõe, por aba, total lido e total persistido, mais o total geral
      (`importFromXlsx_reconciliacao_totalLidoIgualAoPersistidoPorAba`)
- [x] Divergência entre lido e persistido vira `log.warn` **e** entrada em `errors`, não só
      um campo JSON (`importFromXlsx_duasLinhasComMesmoCodigo_reportaDivergenciaNaReconciliacao`)
- [x] Ativo legado sem código é adotado pela linha de mesmo nome, não duplicado
      (`importFromXlsx_ativoLegadoSemCodigo_adotaOCodigoEmVezDeDuplicar`)
- [x] Ativo legado já adotado não é roubado por uma segunda linha com outro código
      (`importFromXlsx_ativoLegadoJaAdotado_naoEhRoubadoPorOutraLinha`)
- [x] Produto renomeado mantendo o código é atualizado, não duplicado
      (`importFromXlsx_produtoRenomeadoMesmoCodigo_atualizaEmVezDeDuplicar`)
- [x] Importar o mesmo arquivo **com coluna de código** duas vezes produz o mesmo total e a
      mesma quantidade de ativos, no unitário e ponta a ponta
      (`importFromXlsx_mesmoArquivoDuasVezes_atualizaSemDuplicarEMantemOTotal`,
      `importarPosicaoB3_mesmoArquivoComCodigoDuasVezes_naoDuplicaEMantemOPatrimonio`).
      O teste antigo (`importarPosicaoB3_duasVezes_atualizaEmVezDeDuplicar`, sem edição)
      cobre só o fallback por nome — a planilha dele não tem coluna de código
- [x] Ativo ausente do arquivo novo entra em `missing` e **continua no banco**
      (`importFromXlsx_ativoComCodigoAusenteDoArquivo_entraEmMissingSemSerApagado`,
      `importarPosicaoB3_ativoDeImportAnteriorAusente_entraEmMissingSemSerApagado`)
- [x] Linha cujo valor não pôde ser lido não faz o ativo dela entrar em `missing`: o código
      é registrado antes das validações de valor, porque `missing` significa "não apareceu
      no extrato", não "não deu pra ler"
      (`importFromXlsx_linhaComValorIlegivel_naoReportaOAtivoDelaComoMissing`)
- [x] Aba sem a coluna de código esperada suprime `missing` inteiro em vez de reportar
      falsos ausentes, e o erro só é emitido quando havia de fato algo a verificar
      (`importFromXlsx_abaSemColunaDeCodigo_naoReportaNadaComoMissing`)
- [x] Ativo manual (sem código) nunca entra em `missing`
      (`importFromXlsx_ativoManualSemCodigo_naoEntraEmMissing`)
- [x] Linha de rodapé da B3 (`Produto` vazio) continua `isInformational = true` e fica fora
      dos dois totais (`importFromXlsx_linhaDeRodape_naoEntraNosTotais`)
- [x] Coluna de valor não-numérica (`-`) continua caindo pra próxima candidata — é o que faz
      a Renda Fixa acertar hoje
      (`importFromXlsx_abaRendaFixa_semMtmNemFechamento_usaValorCurva`, sem edição)
- [x] Aba não reconhecida continua virando erro não-informacional
      (`importFromXlsx_abaDesconhecida_viraErroSemAbortarImport`, sem edição)
- [x] Vermelho comprovado: neutralizando `resolveCodeColumn`, `AssetServiceTest` e
      `AssetControllerTest` dão 8 falhas somadas — entre elas a da colisão, as duas de
      idempotência com código e a do `missing`
- [x] Nenhum dado real (valor, produto, CNPJ, instituição, conta) em teste, log ou mensagem
- [x] Todo teste de regressão do bug comprovadamente falha sem o fix (as 8 acima). Os
      unitários da chave natural foram escritos antes da implementação (TDD —
      `standards.md`); o `stubStatefulRepository`, os de integração e os dois de
      idempotência vieram depois — de uma revisão que mostrou que os primeiros não
      discriminavam — e tiveram o vermelho comprovado retroativamente

## Correções em relação ao rascunho desta spec

- A causa é **uma**, confirmada; os outros três candidatos viraram "descartados"
- Fixture `.xlsx` sintético commitado → reuso do helper `workbook(Map<String, Object[][]>)`
  que já existia no `AssetServiceTest`, montando a planilha em memória
- Saiu o critério "aba não reconhecida reporta o total que ficou de fora": não dá pra somar
  uma aba cujas colunas não se sabe ler
- Órfãos: reportados, não removidos (o rascunho previa remoção)
- Coluna `source` / replace-by-source: descartada, `external_code != null` já responde a
  mesma pergunta sem coluna extra pra manter em sincronia

## Limitações conhecidas

- **Dois papéis com o mesmo código** (mesmo papel em duas instituições, por exemplo)
  colapsariam do mesmo jeito. Não acontece no extrato real diagnosticado, e a reconciliação
  denuncia em `errors` se acontecer — é justamente pra isso que ela existe.
- **`missing` e a reconciliação não aparecem na UI ainda.** O bloco de erros do
  `assets.component.html` renderiza `{{err.sheet}} (linha {{err.row}}): {{err.message}}` sob
  o título "N item(ns) não importado(s)", errado pros dois casos novos. Até a issue no
  `aque-web` sair, os campos ficam visíveis só via API/Swagger.
- **O total só fecha depois que o usuário apaga os `missing`.** É o preço de não deixar o
  import apagar nada sozinho.
- **Aba sem a coluna de código esperada** (a B3 renomear o cabeçalho, ou um export de
  formato antigo): `missing` é suprimido por inteiro, com erro avisando, em vez de reportar
  falsos ausentes. Nessa situação o colapso original da #37 volta naquela aba — mas a
  reconciliação pega e reporta em `errors`, então a perda de valor não é silenciosa.
- **Contagem do cabeçalho de erros na tela.** A divergência da reconciliação entra em
  `realErrors()`, então o `assets.component.html` a conta como "item não importado" e
  mostra "(linha 0)". Impreciso, mas visível — que é o ponto. Vira critério de aceite da
  issue de UI no `aque-web`.
