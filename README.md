# Aque — Backend

> API REST para gerenciamento de finanças pessoais domésticas. Controle de receitas e despesas mensais, lançamentos recorrentes, divisão de custos entre pessoas e dashboard de indicadores financeiros.

## Sumário

- [Descrição](#-descrição)
- [Tecnologias](#️-tecnologias)
- [Estrutura de Pastas](#-estrutura-de-pastas)
- [Como rodar](#-como-rodar)
- [Variáveis de Ambiente](#-variáveis-de-ambiente)
- [Endpoints](#-endpoints)

---

## 📋 Descrição

O `aque-backend` é uma API REST desenvolvida com Java 25 e Spring Boot 4 para uso em rede doméstica (incluindo Raspberry Pi). A aplicação oferece:

- **Autenticação** via usuário e senha com retorno de JWT
- **Lançamentos mensais** com valores previstos e pagos separados, e status automático (`PENDENTE` / `PAGO`)
- **Lançamentos recorrentes** com job mensal que gera instâncias automaticamente para cada template ativo, com idempotência garantida
- **Divisão de custos** configurável por mês, com percentual por pessoa aplicado sobre o total de despesas previstas
- **Dashboard** com saldo previsto vs pago, totais por categoria, evolução anual e divisão calculada por pessoa

A arquitetura segue o padrão **Package by Feature**: cada domínio de negócio (`auth`, `category`, `transaction`, etc.) contém suas próprias camadas (entidade, repositório, serviço, controller, DTOs).

O schema do banco é gerenciado via **Flyway** com migrations versionadas. Os testes de integração utilizam **H2 em memória** com perfil isolado.

---

## 🛠️ Tecnologias

| Tecnologia        | Versão  |
|-------------------|---------|
| Java              | 25      |
| Spring Boot       | 4.0.0   |
| Spring Security   | 7.0.0   |
| Spring Data JPA   | 4.0.0   |
| PostgreSQL        | 18+     |
| Flyway            | 11.14.1 |
| jjwt              | 0.12.6  |
| Hibernate         | 7.1.8   |
| Lombok            | 1.18.x  |
| springdoc-openapi | 2.8.6   |
| H2 (testes)       | 2.4.x   |

---

## 📁 Estrutura de Pastas

```
aque-backend/
├── src/
│   ├── main/
│   │   ├── java/com/aque/
│   │   │   ├── auth/              # Login, JWT, LoginRequest/Response
│   │   │   ├── category/          # CRUD de categorias
│   │   │   ├── transaction/       # CRUD de lançamentos mensais
│   │   │   ├── recurring/         # Templates recorrentes + job mensal
│   │   │   ├── person/            # CRUD de pessoas
│   │   │   ├── split/             # Regras de divisão de custos
│   │   │   ├── dashboard/         # Endpoints de agregação
│   │   │   ├── user/              # Entidade User e repositório
│   │   │   ├── security/          # SecurityConfig, JwtFilter, JwtService
│   │   │   ├── exception/         # GlobalExceptionHandler, BusinessException
│   │   │   ├── config/            # OpenApiConfig (Swagger)
│   │   │   └── AqueBackendApplication.java
│   │   └── resources/
│   │       ├── db/migration/
│   │       │   ├── V1__create_schema.sql
│   │       │   ├── V2__seed_categories.sql
│   │       │   └── V3__seed_user.sql
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       └── application-prod.properties
│   └── test/
│       ├── java/com/aque/
│       │   ├── auth/AuthControllerTest.java
│       │   ├── category/CategoryControllerTest.java
│       │   ├── transaction/TransactionControllerTest.java
│       │   ├── recurring/RecurringTransactionJobTest.java
│       │   ├── split/SplitRuleControllerTest.java
│       │   ├── BaseIntegrationTest.java
│       │   └── AqueApplicationTests.java
│       └── resources/
│           ├── application.properties
│           └── application-test.properties
├── .env
├── .env.example
└── pom.xml
```

---

## 🚀 Como rodar

### Pré-requisitos

- Java 25+
- Maven 3.9+
- PostgreSQL 14+

### 1. Clone o repositório

```bash
git clone <url-do-repositorio>
cd aque-backend
```

### 2. Configure o banco de dados

```sql
CREATE USER aque WITH PASSWORD 'aque';
CREATE DATABASE aque_db OWNER aque;
```

### 3. Configure as variáveis de ambiente

Copie o arquivo de exemplo e preencha os valores:

```bash
cp .env.example .env
```

### 4. Execute a aplicação

```bash
./mvnw spring-boot:run
```

As migrations Flyway rodam automaticamente na inicialização. Um usuário `admin` com senha `admin123` é criado via seed.

### 5. Acesse o Swagger

```
http://localhost:8080/api/swagger-ui.html
```

### Rodar os testes

```bash
./mvnw test -P test
```

Os testes utilizam H2 em memória — nenhuma configuração adicional é necessária.

### Perfil de produção (Raspberry Pi)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## 🔐 Variáveis de Ambiente

Definidas no arquivo `.env` na raiz do projeto:

| Variável                 | Tipo     | Descrição                                        | Exemplo                     |
|--------------------------|----------|--------------------------------------------------|-----------------------------|
| `SPRING_PROFILES_ACTIVE` | `string` | Perfil ativo da aplicação                        | `dev`                       |
| `DB_HOST`                | `string` | Host do PostgreSQL                               | `localhost`                 |
| `DB_PORT`                | `number` | Porta do PostgreSQL                              | `5432`                      |
| `DB_NAME`                | `string` | Nome do banco de dados                           | `aque_db`                   |
| `DB_USER`                | `string` | Usuário do banco                                 | `aque`                      |
| `DB_PASS`                | `string` | Senha do banco                                   | `aque`                      |
| `JWT_SECRET`             | `string` | Chave secreta para assinar o JWT (mín. 256 bits) | `minha-chave-secreta-longa` |
| `JWT_EXPIRATION_MS`      | `number` | Tempo de expiração do token em ms                | `86400000`                  |

---

## 📡 Endpoints

Todos os endpoints REST usam o prefixo `/api`.

Todos os endpoints exceto `/api/auth/login` exigem o header:
```
Authorization: Bearer {token}
```

---

### Autenticação

#### `POST /auth/login`

Autentica o usuário e retorna um JWT.

**Request Body**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 86400000
}
```

---

### Categorias

#### `GET /categories`

Lista todas as categorias. Filtro opcional: `?type=DESPESA` ou `?type=RECEITA`.

**Response** `200 OK`
```json
[
  { "id": "uuid", "name": "Moradia", "type": "DESPESA", "predefined": true },
  { "id": "uuid", "name": "Pet", "type": "DESPESA", "predefined": false }
]
```

#### `POST /categories`

Cria uma categoria customizada.

**Request Body**
```json
{ "name": "Pet", "type": "DESPESA" }
```

**Response** `201 Created`

#### `PUT /categories/{id}`

Edita o nome de uma categoria customizada. Retorna `400` se a categoria for pré-definida.

#### `DELETE /categories/{id}`

Exclui uma categoria customizada. Retorna `400` se a categoria for pré-definida.

---

### Lançamentos Mensais

#### `GET /transactions`

Lista lançamentos com filtros opcionais: `?month=3&year=2026&categoryId=uuid&type=DESPESA&status=PENDENTE`.

**Response** `200 OK`
```json
[
  {
    "id": "uuid",
    "description": "Aluguel",
    "category": { "id": "uuid", "name": "Moradia", "type": "DESPESA", "predefined": true },
    "type": "DESPESA",
    "referenceMonth": 3,
    "referenceYear": 2026,
    "amountExpected": 1500.00,
    "amountPaid": null,
    "status": "PENDENTE",
    "recurringId": "uuid",
    "isOverride": false
  }
]
```

#### `POST /transactions`

Cria um lançamento. Se `amountPaid` for informado, o status é automaticamente `PAGO`.

**Request Body**
```json
{
  "description": "Aluguel",
  "categoryId": "uuid",
  "type": "DESPESA",
  "referenceMonth": 3,
  "referenceYear": 2026,
  "amountExpected": 1500.00,
  "amountPaid": null
}
```

**Response** `201 Created`

#### `PUT /transactions/{id}`

Edita qualquer campo do lançamento. Status é recalculado automaticamente com base em `amountPaid`.

#### `DELETE /transactions/{id}`

Exclui o lançamento.

---

### Lançamentos Recorrentes

#### `GET /recurring`

Lista templates recorrentes. Filtro opcional: `?active=true`.

**Response** `200 OK`
```json
[
  {
    "id": "uuid",
    "description": "Aluguel mensal",
    "category": { "id": "uuid", "name": "Moradia", "type": "DESPESA", "predefined": true },
    "type": "DESPESA",
    "defaultAmount": 1500.00,
    "active": true
  }
]
```

#### `POST /recurring`

Cria um template recorrente. Inicia com `active: true`.

**Request Body**
```json
{
  "description": "Aluguel mensal",
  "categoryId": "uuid",
  "type": "DESPESA",
  "defaultAmount": 1500.00
}
```

#### `PUT /recurring/{id}`

Edita o template. Afeta apenas meses futuros.

#### `DELETE /recurring/{id}`

Desativa o template (`active: false`). Instâncias já geradas são mantidas.

#### `POST /recurring/generate/{year}/{month}`

Força a geração manual de instâncias para um mês/ano específico. Útil para backfill e testes. A operação é idempotente.

**Response** `200 OK`
```json
{ "generated": 3 }
```

---

### Pessoas

#### `GET /persons`

Lista todas as pessoas.

#### `POST /persons`

**Request Body**
```json
{ "name": "Eu" }
```

#### `PUT /persons/{id}` / `DELETE /persons/{id}`

Edita ou exclui. Retorna `400` se a pessoa estiver vinculada a uma regra de divisão ativa.

---

### Regra de Divisão

#### `GET /split/{year}/{month}`

Retorna a regra de divisão do mês. Retorna `404` se não configurada.

**Response** `200 OK`
```json
{
  "year": 2026,
  "month": 3,
  "items": [
    { "person": { "id": "uuid", "name": "Eu" }, "percentage": 70.00 },
    { "person": { "id": "uuid", "name": "Esposa" }, "percentage": 30.00 }
  ]
}
```

#### `PUT /split/{year}/{month}`

Cria ou substitui a regra do mês. A soma dos percentuais deve ser exatamente 100%.

**Request Body**
```json
{
  "items": [
    { "personId": "uuid", "percentage": 70 },
    { "personId": "uuid", "percentage": 30 }
  ]
}
```

---

### Dashboard

#### `GET /dashboard/summary/{year}/{month}`

Saldo previsto vs pago do mês.

**Response** `200 OK`
```json
{
  "totalIncomeExpected": 5000.00,
  "totalIncomePaid": 5000.00,
  "totalExpenseExpected": 3200.00,
  "totalExpensePaid": 2800.00,
  "balanceExpected": 1800.00,
  "balancePaid": 2200.00
}
```

#### `GET /dashboard/by-category/{year}/{month}`

Totais por categoria. Filtro opcional: `?type=DESPESA`. Ordenado por `totalExpected` decrescente.

**Response** `200 OK`
```json
[
  {
    "category": { "id": "uuid", "name": "Moradia" },
    "totalExpected": 1500.00,
    "totalPaid": 1500.00
  }
]
```

#### `GET /dashboard/evolution/{year}`

Totais mensais do ano inteiro (sempre 12 registros, zeros para meses sem lançamentos).

**Response** `200 OK`
```json
[
  {
    "month": 1,
    "totalIncomeExpected": 5000.00,
    "totalIncomePaid": 5000.00,
    "totalExpenseExpected": 3200.00,
    "totalExpensePaid": 3200.00
  }
]
```

#### `GET /dashboard/split/{year}/{month}`

Divisão calculada por pessoa com base na regra do mês e no total de despesas previstas. Retorna `404` se não houver regra configurada.

**Response** `200 OK`
```json
{
  "totalExpenseExpected": 3200.00,
  "items": [
    { "person": { "id": "uuid", "name": "Eu" }, "percentage": 70, "amount": 2240.00 },
    { "person": { "id": "uuid", "name": "Esposa" }, "percentage": 30, "amount": 960.00 }
  ]
}
```
