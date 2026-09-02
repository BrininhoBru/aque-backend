# Padrões obrigatórios do time

Aplicam-se a todo repositório, independente de linguagem. Diferente das outras rules,
esta não deve ser removida ou relaxada ao adaptar o template — apenas os comandos e
ferramentas de exemplo devem ser ajustados para a stack de cada repositório.

## Idioma do código
- Todo o código é escrito em **inglês**: nomes de variáveis, funções,
  métodos, classes, interfaces, tabelas/colunas de banco, branches e comentários
- Isso vale também para testes: nomes de métodos de teste e mensagens de assert em inglês
- Termos de domínio de negócio específicos do time (nomes próprios, siglas internas sem
  tradução natural) podem permanecer como estão — não force uma tradução artificial
- Commits seguem o mesmo padrão — ver skill `commit-msg`
- **Código legado em português**: essa regra vale para código novo. Não renomeie em massa
  identificadores existentes só para adequá-los ao padrão — isso é um refactor arriscado
  (quebra referências, integrações, histórico de `git blame`). Renomear código legado é
  uma decisão à parte, feita de forma isolada e deliberada, nunca como efeito colateral
  de outra tarefa

### Exemplos
```java
// Sim
public class OrderService {
    public Order calculateOrderTotal(Order order) { ... }
    public boolean isOrderCancellable(Order order) { ... }
}

// Não
public class PedidoService {
    public Pedido calcularTotalPedido(Pedido pedido) { ... }
}
```

## Commits e versionamento
- Conventional Commits obrigatório: `feat:`, `fix:`, `refactor:`, `chore:`, `docs:`, `test:`, `perf:`, `ci:`
- Versionamento semântico automático a partir dos commits (ex: semantic-release) —
  nunca bump de versão manual no `package.json`/`pom.xml`
- Mudança que quebra compatibilidade usa `!` no tipo (`feat!:`) ou rodapé `BREAKING CHANGE:`
- Use a skill `commit-msg` para gerar a mensagem (formato completo com escopo, prefixo
  de ticket extraído da branch) e executar o commit diretamente — não escreva
  a mensagem manualmente sem seguir esse padrão

## Desenvolvimento orientado a testes (TDD)
- Ciclo red → green → refactor: escreva o teste que falha antes de implementar
- Nenhuma função ou endpoint novo é implementado sem um teste que existia primeiro e falhava
- Ao alterar código legado sem teste, adicione um teste de regressão antes de mexer

## Pre-commit hooks
- Lint e format rodam automaticamente antes de cada commit local
  (ex: Husky + lint-staged para Node, framework `pre-commit` para outras stacks)
- O commit é bloqueado localmente se lint/format falhar — não depender só do CI para isso

## Definition of Done
Uma task só é considerada concluída quando:
- [ ] Código implementado e testado (unitário + integração quando aplicável)
- [ ] Lint e testes passando localmente e no CI
- [ ] PR revisado e aprovado por outra pessoa
- [ ] Documentação relevante atualizada (README, CLAUDE.md, comentários de API)
- [ ] Sem TODOs ou débitos técnicos não documentados/registrados

## Template de Pull Request
Todo PR segue esta estrutura (a skill `/gerar-pr` já gera isso automaticamente):
- **Contexto**: o que motivou a mudança
- **Mudanças**: lista do que foi alterado
- **Como testar**: passos para validar manualmente
- **Checklist**: testes adicionados, docs atualizadas, breaking changes (sim/não)

## Logging estruturado
- Logs em formato estruturado (JSON), nunca `print`/`console.log` solto em produção
- Campos padrão em todo log: `timestamp`, `level`, `service`, `traceId` (ou correlation ID equivalente)
- Nunca logar dados sensíveis — ver `security.md`
- Nível correto: `error` para falhas reais, `warn` para degradação, `info` para eventos
  de negócio, `debug` só em ambiente de desenvolvimento
