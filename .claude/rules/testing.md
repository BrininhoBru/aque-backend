# Convenções de testes

- Nomeie testes descrevendo comportamento: `<cenário>_<condição>_<resultadoEsperado>`, em
  português (ex.: `create_semAmountPaid_ficaPendente`,
  `criarLancamento_comValorPago_deveSerPago`)
- Um teste testa uma coisa só — evite múltiplos asserts não relacionados no mesmo teste
- Mocks apenas para dependências externas (repositórios em testes de unidade de service); nunca
  mocke o próprio código sob teste — testes de controller (`*ControllerTest`) não mockam nada,
  sobem o contexto Spring completo
- Toda função pública nova precisa de teste cobrindo o caminho feliz e pelo menos um caso de erro

## Backend (JUnit 5 + Mockito + AssertJ)
- Testes de unidade em `src/test/java`, espelhando o pacote da classe testada
- Assertions com AssertJ (`assertThat`, `assertThatThrownBy`), não JUnit puro nem Hamcrest —
  para `BigDecimal`, use `isEqualByComparingTo` em vez de `isEqualTo`
- Testes de integração usam `@SpringBootTest` + Testcontainers (Postgres real via
  `TestcontainersConfiguration`), nunca banco em memória — controllers estendem
  `BaseIntegrationTest` para herdar autenticação (`token` JWT) de graça
- Testes de integração limpam as tabelas explicitamente em `@BeforeEach` (`deleteAll()`), não
  dependem só de rollback transacional
