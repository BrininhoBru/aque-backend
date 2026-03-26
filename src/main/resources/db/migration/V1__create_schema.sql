CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('RECEITA', 'DESPESA')),
    predefined BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE persons (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL
);

CREATE TABLE recurring_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    description VARCHAR(255) NOT NULL,
    category_id UUID NOT NULL REFERENCES categories(id),
    type VARCHAR(10) NOT NULL CHECK (type IN ('RECEITA', 'DESPESA')),
    default_amount NUMERIC(15, 2) NOT NULL CHECK (default_amount >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    description VARCHAR(255) NOT NULL,
    category_id UUID NOT NULL REFERENCES categories(id),
    type VARCHAR(10) NOT NULL CHECK (type IN ('RECEITA', 'DESPESA')),
    reference_month INTEGER NOT NULL CHECK (reference_month BETWEEN 1 AND 12),
    reference_year INTEGER NOT NULL,
    amount_expected NUMERIC(15, 2) NOT NULL CHECK (amount_expected >= 0),
    amount_paid NUMERIC(15, 2) CHECK (amount_paid >= 0),
    status VARCHAR(10) NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE', 'PAGO')),
    recurring_id UUID REFERENCES recurring_transactions(id),
    is_override BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE split_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reference_month INTEGER NOT NULL CHECK (reference_month BETWEEN 1 AND 12),
    reference_year INTEGER NOT NULL,
    UNIQUE (reference_month, reference_year)
);

CREATE TABLE split_rule_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    split_rule_id UUID NOT NULL REFERENCES split_rules(id) ON DELETE CASCADE,
    person_id UUID NOT NULL REFERENCES persons(id),
    percentage NUMERIC(5, 2) NOT NULL CHECK (percentage > 0),
    UNIQUE (split_rule_id, person_id)
);