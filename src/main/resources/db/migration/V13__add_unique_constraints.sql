DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_accounts_user_name') THEN
        ALTER TABLE accounts ADD CONSTRAINT uq_accounts_user_name UNIQUE (user_id, name);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_categories_user_name_type') THEN
        ALTER TABLE categories ADD CONSTRAINT uq_categories_user_name_type UNIQUE (user_id, name, type);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_budgets_user_category_period_dates') THEN
        ALTER TABLE budgets ADD CONSTRAINT uq_budgets_user_category_period_dates UNIQUE (user_id, category_id, period, start_date, end_date);
    END IF;
END $$;
