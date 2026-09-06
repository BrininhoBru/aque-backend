CREATE TABLE assets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(150) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('RENDA_FIXA', 'ACAO', 'FUNDO', 'CRIPTO', 'IMOVEL', 'OUTRO')),
    current_value NUMERIC(15, 2) NOT NULL CHECK (current_value >= 0),
    person_id UUID REFERENCES persons(id)
);
