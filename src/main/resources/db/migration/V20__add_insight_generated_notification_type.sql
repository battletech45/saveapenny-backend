ALTER TABLE notifications DROP CONSTRAINT chk_notifications_type;

ALTER TABLE notifications
    ADD CONSTRAINT chk_notifications_type CHECK (
        type IN (
            'BUDGET_WARNING',
            'BUDGET_EXCEEDED',
            'RECURRING_TRANSACTION_CREATED',
            'GOAL_OFF_TRACK',
            'INSIGHT_GENERATED',
            'SYSTEM'
        )
    );
