# Boas práticas gerais

Aplica-se a todo o repositório, independente de linguagem ou stack.

## Nomenclatura
- Nomes descritivos e sem abreviação forçada: `calculateOrderTotal`, não `calcOrdTot`
- Booleanos com prefixo de pergunta: `isActive`, `hasPermission`, `canEdit`
- Evite nomes genéricos (`data`, `info`, `temp`, `handler`) quando um nome específico é possível
- Idioma do código é inglês — ver regra obrigatória em `standards.md`

## Git e commits
- Conventional Commits: `feat:`, `fix:`, `refactor:`, `chore:`, `docs:`, `test:`
- Um commit = uma mudança lógica; evite commits "ajustes diversos"
- Branch a partir de `main`/`develop` atualizada; nomeie como `feat/nome-da-feature` ou `fix/descricao-do-bug`
- Nunca commitar arquivos gerados (build, node_modules, target) — confira `.gitignore` antes de criar algo novo

## Tratamento de erros
- Nunca engula exceções silenciosamente (`catch {}` vazio); ao menos logue
- Erros de negócio são explícitos e tipados — evite lançar `Exception`/`Error` genérico
- Mensagens de erro voltadas ao usuário são claras e acionáveis; mensagens técnicas detalhadas vão para o log, não para a resposta

## Funções e complexidade
- Funções fazem uma coisa; se o nome precisa de "e" (`saveAndSendEmail`), provavelmente deveria ser duas funções
- Prefira retornar cedo (early return) a aninhar `if`s profundamente
- Evite "números mágicos" e strings soltas no meio do código — extraia para constantes nomeadas
- Não duplique lógica de negócio em mais de um lugar; extraia para uma função/serviço compartilhado

## Dependências
- Antes de adicionar uma lib nova, verifique se já existe algo equivalente no projeto
- Fixe versões (sem `^`/`~` soltos em libs críticas) para builds reproduzíveis
- Remova imports e dependências não utilizados antes de abrir o PR

## Configuração e segredos
- Nada de valores de ambiente (URLs, chaves, flags) hardcoded — usar variáveis de ambiente/config
- Um `.env.example` sempre atualizado, sem valores reais, documentando o que é necessário

## Documentação
- Comentários explicam o "porquê", não o "o quê" (o código já diz o quê)
- Funções públicas/exportadas com propósito não óbvio merecem uma linha de doc
- Atualize o README quando um comando de setup ou execução mudar

## Revisão de código
- PRs pequenos e focados; se o diff passar de ~400 linhas, considere quebrar
- Todo PR descreve o que mudou e por quê (use a skill `/gerar-pr`)
- Não aprove o próprio PR; peça revisão de outra pessoa do time
