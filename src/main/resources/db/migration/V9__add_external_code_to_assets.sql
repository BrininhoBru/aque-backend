-- #37: o import da B3 fazia upsert por (nome, tipo), então duas posições no mesmo papel
-- distintas só pelo código (várias aplicações no mesmo CDB, por exemplo) colapsavam em um
-- ativo só, com o valor da última linha lida — o resto sumia do patrimônio.
--
-- Toda aba do extrato traz um identificador estável da posição: "Código de Negociação"
-- (Ações, Fundo de Investimento), "Código" (Renda Fixa) e "Código ISIN" (Tesouro Direto).
--
-- Nullable porque ativo cadastrado à mão (imóvel, cripto fora da B3) não tem código. Sem
-- UNIQUE: os manuais são todos NULL e a unicidade só valeria entre os importados — regra
-- fraca demais pra um índice parcial carregar.
--
-- Sem backfill: não há de onde derivar o código das linhas existentes. O primeiro import
-- depois do deploy adota os códigos pelo nome (ver AssetService.importSheet).
ALTER TABLE assets
    ADD COLUMN external_code VARCHAR(50);
