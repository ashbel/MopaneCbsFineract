-- Optional: capitalized-fee-accounting = direct-income (default) | unearned-income (future: credit liability, amortise to fee income)
INSERT INTO c_configuration (`name`, `value`, `enabled`, `is_trap_door`, `description`)
VALUES ('capitalized-fee-accounting', 'direct-income', 1, 0,
        'Capitalised fee GL posting: direct-income = Dr Loan portfolio Cr Fee income; unearned-income reserved for future use');
