CREATE TABLE goals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(120) NOT NULL,
    target_amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    target_date DATE NOT NULL,
    linked_account_id UUID NULL,
    status VARCHAR(16) NOT NULL,
    inputs_json TEXT NOT NULL,
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_goals_linked_account FOREIGN KEY (linked_account_id) REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT chk_goals_type CHECK (type IN ('SAVINGS', 'DEBT_PAYOFF', 'PURCHASE', 'RETIREMENT', 'INCOME_TARGET')),
    CONSTRAINT chk_goals_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ACHIEVED', 'ABANDONED'))
);

CREATE TABLE goal_scenarios (
    id UUID PRIMARY KEY,
    goal_id UUID NOT NULL,
    name VARCHAR(80) NOT NULL,
    inputs_json TEXT NOT NULL,
    is_baseline BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_goal_scenarios_goal FOREIGN KEY (goal_id) REFERENCES goals (id) ON DELETE CASCADE
);

CREATE TABLE goal_runs (
    id UUID PRIMARY KEY,
    goal_id UUID NOT NULL,
    scenario_id UUID NOT NULL,
    inputs_snapshot_json TEXT NOT NULL,
    output_summary_json TEXT NOT NULL,
    output_series_json TEXT NULL,
    feasibility VARCHAR(16) NOT NULL,
    triggered_by VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_goal_runs_goal FOREIGN KEY (goal_id) REFERENCES goals (id) ON DELETE CASCADE,
    CONSTRAINT fk_goal_runs_scenario FOREIGN KEY (scenario_id) REFERENCES goal_scenarios (id) ON DELETE CASCADE,
    CONSTRAINT chk_goal_runs_feasibility CHECK (feasibility IN ('ON_TRACK', 'TIGHT', 'AT_RISK', 'INFEASIBLE')),
    CONSTRAINT chk_goal_runs_triggered_by CHECK (triggered_by IN ('USER', 'AGENT', 'PROGRESS_JOB', 'WHAT_IF'))
);

CREATE INDEX idx_goals_user_id ON goals (user_id);
CREATE INDEX idx_goals_user_status ON goals (user_id, status);
CREATE INDEX idx_goals_user_type ON goals (user_id, type);
CREATE INDEX idx_goals_user_deleted ON goals (user_id, deleted_at);
CREATE INDEX idx_goal_scenarios_goal_id ON goal_scenarios (goal_id);
CREATE UNIQUE INDEX uq_goal_scenarios_one_baseline_per_goal ON goal_scenarios (goal_id) WHERE is_baseline = TRUE;
CREATE INDEX idx_goal_runs_goal_id_created_at ON goal_runs (goal_id, created_at DESC);
