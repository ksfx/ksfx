--liquibase formatted sql

-- NOT yet in liquibase-changeLog.xml - activate one release after 20261116090000_agentic_access
-- shipped and was confirmed working on HQ (same two-step as 20261101000000 for agentic_config):
-- until then the old jar can still roll back onto the untouched columns.

--changeset claude:20261201000000-1
ALTER TABLE project_agentic_settings DROP COLUMN auth_mode;
ALTER TABLE project_agentic_settings DROP COLUMN api_key;
ALTER TABLE project_agentic_settings DROP COLUMN claude_rate_limit_status;
ALTER TABLE project_agentic_settings DROP COLUMN claude_rate_limit_type;
ALTER TABLE project_agentic_settings DROP COLUMN claude_rate_limit_resets_at;
ALTER TABLE project_agentic_settings DROP COLUMN claude_rate_limit_overage_status;
ALTER TABLE project_agentic_settings DROP COLUMN claude_rate_limit_overage_resets_at;
ALTER TABLE project_agentic_settings DROP COLUMN claude_rate_limit_using_overage;
ALTER TABLE project_agentic_settings DROP COLUMN claude_rate_limit_updated_at;
