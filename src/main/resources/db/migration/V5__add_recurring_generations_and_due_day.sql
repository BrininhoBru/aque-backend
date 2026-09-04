CREATE TABLE recurring_generations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recurring_id UUID NOT NULL REFERENCES recurring_transactions(id),
    reference_month INTEGER NOT NULL CHECK (reference_month BETWEEN 1 AND 12),
    reference_year INTEGER NOT NULL,
    UNIQUE (recurring_id, reference_month, reference_year)
);

ALTER TABLE recurring_transactions
    ADD COLUMN due_day INTEGER CHECK (due_day BETWEEN 1 AND 31);
