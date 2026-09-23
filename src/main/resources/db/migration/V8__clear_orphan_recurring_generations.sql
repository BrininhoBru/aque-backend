-- #36: até esta versão, excluir um lançamento gerado por um recorrente removia só a
-- `transactions`, deixando viva a linha de `recurring_generations` que marca "esse mês já
-- foi gerado". O mês ficava marcado como gerado sem instância nenhuma, e o recorrente era
-- ignorado pra sempre. O fix em TransactionService.delete impede órfãs novas, mas não
-- alcança as que já existem — não há mais Transaction pra excluir e disparar a limpeza.
--
-- ATENÇÃO antes de aplicar em um banco com dados: uma instância que foi *movida* de mês
-- (editada pra outro reference_month) também deixa a linha do mês original sem
-- correspondência, e é indistinguível de uma exclusão. Apagar essa linha faz o mês
-- original gerar uma segunda cópia. Auditar antes com:
--
--   SELECT g.recurring_id, g.reference_month, g.reference_year, r.description
--   FROM recurring_generations g
--   JOIN recurring_transactions r ON r.id = g.recurring_id
--   WHERE NOT EXISTS (
--       SELECT 1 FROM transactions t
--       WHERE t.recurring_id = g.recurring_id
--         AND t.reference_month = g.reference_month
--         AND t.reference_year = g.reference_year
--   );
DELETE FROM recurring_generations g
WHERE NOT EXISTS (
    SELECT 1 FROM transactions t
    WHERE t.recurring_id = g.recurring_id
      AND t.reference_month = g.reference_month
      AND t.reference_year = g.reference_year
);
