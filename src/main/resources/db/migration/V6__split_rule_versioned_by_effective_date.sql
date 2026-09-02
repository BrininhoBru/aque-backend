ALTER TABLE split_rules
    ADD COLUMN effective_from DATE,
    ADD COLUMN created_at TIMESTAMP;

UPDATE split_rules
SET effective_from = make_date(reference_year, reference_month, 1),
    created_at = NOW();

ALTER TABLE split_rules
    ALTER COLUMN effective_from SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    DROP COLUMN reference_month,
    DROP COLUMN reference_year;
