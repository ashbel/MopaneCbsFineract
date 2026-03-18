-- Capitalised fee flag on loan charge (Mambu-style)
ALTER TABLE m_loan_charge ADD COLUMN is_capitalized TINYINT(1) NOT NULL DEFAULT 0;
