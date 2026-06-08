ALTER TABLE accounts ADD CONSTRAINT uq_accounts_user_name UNIQUE (user_id, name);

ALTER TABLE categories ADD CONSTRAINT uq_categories_user_name_type UNIQUE (user_id, name, type);

ALTER TABLE budgets ADD CONSTRAINT uq_budgets_user_category_period_dates UNIQUE (user_id, category_id, period, start_date, end_date);
