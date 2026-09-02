# Segurança

- Nunca commitar segredos, tokens ou senhas — usar variáveis de ambiente (`${VAR}`) e `.env` (gitignorado)
- Toda entrada de usuário no backend passa por validação (`@Valid` + Bean Validation)
- Queries sempre parametrizadas — nunca concatenar SQL
- Endpoints autenticados por padrão; endpoints públicos precisam de justificativa no PR
- Dependências novas passam por `npm audit` / `./mvnw dependency-check:check` antes do merge
- Dados sensíveis de usuário (CPF, e-mail, telefone) nunca aparecem em logs
