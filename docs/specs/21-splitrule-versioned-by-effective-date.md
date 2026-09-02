# SplitRule onipresente, versionada por data de vigência

- **Issue:** #21 — https://github.com/BrininhoBru/aque-backend/issues/21
- **Status:** Draft
- **Repo:** BrininhoBru/aque-backend

## Problema

Hoje `SplitRule` é uma entidade por `referenceMonth`/`referenceYear` (`SplitRuleRepository.findByReferenceMonthAndReferenceYear`): cada mês precisa da sua própria regra configurada, e `SplitRuleService.save()` sobrescreve a linha existente do mês (`clear()` + reinserção dos itens). Na prática, quem usa o app configura a mesma divisão todo mês — repetir a configuração é trabalho manual desnecessário, e não existe forma de "mudar a regra a partir de agora" sem também ter que recriar manualmente os meses futuros. Decisão tomada durante a auditoria de #17 (`docs/specs/17-audit-business-rules-and-code.md`): a regra deve ser onipresente (uma parametrização única, válida por padrão em todos os meses), com histórico de vigência — editar hoje não pode alterar o split de meses já fechados.

## Escopo

**Dentro:**
- Trocar o modelo de `SplitRule` de "uma linha por mês/ano" para um histórico de versões, cada uma com um `effectiveFrom` (mês/ano a partir do qual passa a valer).
- `SplitRuleService.save()` passa a **inserir uma nova versão** em vez de sobrescrever a existente — a nova versão é sempre vigente a partir do mês/ano atual (sem campo para escolher vigência futura ou passada nesta primeira versão).
- Busca da regra vigente num mês (`SplitRuleService.findByMonth` e `DashboardService.getSplit`) passa a ser "a versão mais recente com `effectiveFrom <= mês consultado`", não mais um match exato de mês/ano.
- Migração Flyway: nova tabela/coluna para suportar múltiplas versões preservando os dados já existentes (cada `SplitRule` de mês vira uma versão vigente a partir daquele mês).
- Manter a regra de validação existente (soma de `percentage` = 100%, via `BigDecimal`).
- Manter a correção de #20 (rejeitar `personId` duplicado no mesmo conjunto de itens) já que essa issue toca a mesma validação — implementar junto ou logo em seguida, mas sem depender uma da outra para o merge.

**Fora:**
- Permitir escolher uma vigência futura ou passada ao salvar (ex.: campo "vigente a partir de mm/aaaa" no request) — editar sempre vale do mês atual em diante.
- Expor endpoint/listagem de histórico de versões — o histórico existe no banco e é usado internamente pelo dashboard, mas não há API para consultá-lo nesta entrega.
- Mudanças na tela `split` do `aque-web` — tratadas em aque-web#17.
- Mudar a lógica de arredondamento do último item em `DashboardService.getSplit` (já coberta como item de tech-debt separado em `.claude/docs/CONCERNS.md`).

## Abordagem

Adicionar um campo de vigência à entidade `SplitRule` (ex.: `effectiveFrom` como `LocalDate`, normalizado para o primeiro dia do mês — mais simples de comparar do que dois inteiros `month`/`year`) e permitir múltiplas linhas na tabela `split_rules`, uma por versão, em vez de uma constraint implícita de "uma por mês/ano".

- **Migração**: nova coluna `effective_from DATE NOT NULL` substituindo `reference_month`/`reference_year` (ou coexistindo temporariamente, a decidir na migração). Cada linha existente vira uma versão com `effective_from = primeiro dia de reference_month/reference_year`.
- **`SplitRuleRepository`**: troca `findByReferenceMonthAndReferenceYear` por algo como `findTopByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate monthStart)` — pega a versão vigente mais recente até aquele mês.
- **`SplitRuleService.save(int year, int month, SplitRuleRequest)`**: em vez de buscar-ou-criar e sobrescrever, sempre cria uma `SplitRule` nova com `effectiveFrom` = primeiro dia do mês/ano atual (do relógio do servidor, não do mês navegado na tela — a UI de `aque-web` não terá mais controle sobre isso, ver aque-web#17). Mantém `validatePercentages`.
- **`SplitRuleService.findByMonth(year, month)`** e **`DashboardService.getSplit(year, month)`**: trocam a busca exata por `findTopByEffectiveFromLessThanEqualOrderByEffectiveFromDesc`, usando o mês/ano consultado (que pode ser passado, presente ou futuro) para achar a versão vigente naquele momento.
- **`SplitRuleController`**: mantém as rotas `GET/PUT /split/{year}/{month}` por compatibilidade com o consumo por mês (dashboard/histórico continuam navegáveis por mês), mas o `PUT` passa a sempre gravar a vigência a partir do mês atual, independente do `{year}/{month}` da URL usado só para o cálculo de exibição prévia. Avaliar durante a implementação se faz mais sentido um novo endpoint `PUT /split` (sem mês na URL) para deixar essa semântica explícita — decisão de API fica para o PR, já que não muda o comportamento de negócio acordado aqui.

## Critério de aceite

- [ ] Migração Flyway aplicada sem perda de dado: toda `SplitRule` existente vira uma versão vigente a partir do seu mês/ano original.
- [ ] Salvar uma nova regra de split não sobrescreve nem apaga a versão anterior — cria uma nova versão vigente a partir do mês atual.
- [ ] Consultar a regra/split de um mês passado (anterior à edição mais recente) continua retornando a versão que estava vigente naquele mês, mesmo depois de uma edição posterior.
- [ ] Consultar a regra/split do mês atual ou futuro retorna a versão mais recente vigente.
- [ ] A soma dos percentuais de uma nova versão continua validada como exatamente 100% (`BigDecimal.compareTo`).
- [ ] Nenhum endpoint novo de listagem de histórico é criado nesta entrega.
- [ ] `DashboardService.getSplit` usa a versão vigente no mês consultado para calcular os valores por pessoa — testado com pelo menos dois meses cobertos por versões diferentes.

## Questões em aberto

- Forma exata da API (manter `PUT /split/{year}/{month}` com semântica de "salvar vigente a partir de agora" vs. introduzir `PUT /split` sem mês na URL) — decisão de implementação, não de produto; fica para quando a spec virar PR.
