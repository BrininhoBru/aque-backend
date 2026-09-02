# Checklist de revisão de segurança

## Validação de entrada
- [ ] Todo input de usuário é sanitizado antes de queries no banco
- [ ] Tipos e tamanhos de upload de arquivo são validados
- [ ] Path traversal é prevenido em operações de arquivo

## Autenticação e autorização
- [ ] Tokens JWT expiram em prazo razoável
- [ ] Chaves de API ficam em variáveis de ambiente, nunca no código
- [ ] Senhas são hasheadas com bcrypt/argon2
- [ ] Rotas protegidas verificam papel/permissão do usuário, não só autenticação

## Dados sensíveis
- [ ] Nenhum dado sensível aparece em logs
- [ ] Respostas de erro não vazam detalhes internos (stack trace, versão de lib)
