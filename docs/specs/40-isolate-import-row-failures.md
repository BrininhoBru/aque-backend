# Isolar falha de linha no import e parar de culpar o arquivo

- **Issue:** #40 — https://github.com/BrininhoBru/aque-backend/issues/40
- **Status:** Draft
- **Repo:** BrininhoBru/aque-backend

## Problema

`AssetService.importFromXlsx` envolvia o laço inteiro em
`catch (IOException | RuntimeException e)` e lançava
`BusinessException("Arquivo não é um .xlsx de Posição da B3 válido", BAD_REQUEST)` **sem
nunca tocar no `e`**. O `GlobalExceptionHandler` só loga no handler genérico de `Exception`;
`handleBusiness` não loga. A causa sumia por completo.

Sem transação — cada `save` commita sozinho — uma falha na linha 40 de 60 deixava as 39
primeiras gravadas, devolvia "o arquivo é inválido" para um arquivo correto, e não
registrava nada em log nenhum. Reimportar gravava por cima de um estado parcial.

Levantado na revisão da #37 e deliberadamente deixado fora dela, porque a alternativa
avaliada na época (`@Transactional`) mexeria no tempo de vida do persistence context que a
reconciliação daquela PR acabara de estabilizar.

## Escopo

**Dentro:**
- Falha inesperada numa linha vira `AssetImportError` e não derruba as demais
- A causa é logada com a exceção
- Erro de banco deixa de se apresentar como arquivo corrompido

**Fora:**
- `@Transactional` no import — ver "Decisão"
- Mudar o tratamento do que já é linha inválida prevista (produto vazio, valor ilegível,
  valor negativo) — isso já vira `AssetImportError` e continua igual
- Retry, importação retomável, fila

**Nunca:** dado real do extrato em código, teste, log ou commit. `aque-backend` é público.

## Decisão: isolar por linha, não transacionar

O issue propunha `@Transactional`. Optamos pelo contrário, por dois motivos:

**É o padrão da casa.** `RecurringTransactionJob.generate` captura por item justamente pra
que uma recorrência quebrada não trave as demais. E este importador já trata linha inválida
exatamente assim.

**As duas rotas são excludentes.** Um `DataIntegrityViolationException` marca a transação
como rollback-only; capturar por linha e seguir terminaria em `UnexpectedRollbackException`
no commit. Não dá pra ter atomicidade e isolamento por linha ao mesmo tempo.

O custo aceito: um arquivo com uma linha problemática importa as demais em vez de não
importar nada. A perda não é silenciosa — a linha fica fora de `totalRead` e de
`totalPersisted`, e o erro aparece em `errors`, que a tela do `aque-web` já renderiza.

## Abordagem

**`importSheet`** — o bloco que resolve e grava o ativo saiu para um `upsert` próprio e
passou a rodar dentro de `try`/`catch (RuntimeException)`. No catch: `log.error` com a
exceção e um `AssetImportError` não-informacional com a linha. `totalRead` e `rows` só
incrementam **depois** do sucesso, senão a reconciliação acusaria divergência inexistente.

**`openWorkbook`** — a abertura do arquivo virou método próprio, com o catch largo (o POI
lança `POIXMLException`, `NotOfficeXmlFileException`, `IllegalArgumentException` para
arquivo corrompido) → `BusinessException` 400, agora com `log.warn(..., e)`. O laço das abas
ficou fora desse catch: o que escapar do isolamento por linha propaga e o handler genérico
loga e devolve 500, em vez de culpar o arquivo.

O `catch (IOException)` que restou no `try-with-resources` cobre só o `close()`, e apenas
loga — o import já terminou nesse ponto.

## Critério de aceite

- [x] Linha recusada pelo banco vira `AssetImportError` não-informacional e as demais
      importam (`importFromXlsx_falhaInesperadaNumaLinha_importaAsOutrasEReportaAQuebrada`)
- [x] A linha que falhou fica fora de `totalRead`, de `totalPersisted` e da contagem de
      `rows`, então a reconciliação da aba continua fechando
      (`importFromXlsx_falhaInesperadaNumaLinha_naoEntraNaReconciliacao`)
- [x] Ponta a ponta com Postgres real e gatilho de verdade — produto acima do `VARCHAR(255)`
      da V7: a linha boa persiste no banco e a resposta é 200 com o erro da linha longa
      (`importarPosicaoB3_linhaRecusadaPeloBanco_importaAsOutrasEReportaAQuebrada`)
- [x] Erro de banco não é mais reportado como arquivo inválido
- [x] Arquivo vazio e arquivo que não é .xlsx continuam 400
      (`importFromXlsx_arquivoVazio_...`, `importFromXlsx_arquivoNaoEhXlsx_...`, sem edição)
- [x] A causa é logada em todo caminho de falha — `general-best-practices.md` proíbe engolir
      exceção sem ao menos logar
- [x] Vermelho comprovado: revertendo só o `AssetService`, os dois unitários novos falham com
      `BusinessException` de arquivo inválido e o de integração recebe 400 em vez de 200
- [x] Testes escritos antes do fix (TDD — `standards.md`)

## Limitações conhecidas

- **Linha que falha some do patrimônio até alguém agir.** Ela aparece em `errors`, mas o
  total fica menor que o do extrato até a causa ser corrigida e o arquivo reimportado.
- **A mensagem do erro expõe o nome da classe da exceção** (`DataIntegrityViolationException`),
  não o detalhe. Detalhe técnico fica no log, como manda `general-best-practices.md`.
