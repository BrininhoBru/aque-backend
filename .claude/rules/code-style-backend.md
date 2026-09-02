---
paths:
  - "src/main/java/**/*.java"
  - "src/test/java/**/*.java"
---

# Estilo de código — Backend (Java/Spring)

- Indentação de 4 espaços, sem tabs
- Uma classe/record pública por arquivo, nome do arquivo = nome do tipo
- Injeção de dependência via construtor (`@RequiredArgsConstructor` + campos `private final`),
  nunca `@Autowired` em campo
- DTOs de request/response são `record` (sem Lombok), nunca expor entidades JPA diretamente nos
  controllers — response records expõem uma factory estática `from(Entity)` para o mapeamento
- Pacotes organizados por feature (`com.aque.transaction`, `com.aque.category`), não por camada
- Exceptions de negócio devem estender `BusinessException` e ser tratadas no
  `@RestControllerAdvice` global (`GlobalExceptionHandler`)
- Logs com SLF4J via Lombok `@Slf4j` (`log.info`, `log.error`), nunca `System.out.println`
- Identificadores (classes, métodos, variáveis) em inglês — mas mensagens de validação/erro de
  negócio, textos das anotações OpenAPI e nomes de métodos de teste são em **português**
  deliberadamente, por ser a língua voltada ao usuário final do app. Isso não é código legado a
  corrigir, é a convenção atual do projeto.
