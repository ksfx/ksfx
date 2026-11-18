--liquibase formatted sql

-- Activated 2026-10-24, one release after 20261023090000_project_agentic_settings shipped and
-- was confirmed working on HQ: a rollback of THIS release lands on the split release, which no
-- longer reads these columns - only a two-release rollback to the pre-split jar would need a
-- restore. See the concept "Agentic settings per project" (KSFX wiki, Concepts).

--changeset claude:20261101000000-1
ALTER TABLE agentic_config DROP COLUMN auth_mode;
ALTER TABLE agentic_config DROP COLUMN api_key;
ALTER TABLE agentic_config DROP COLUMN default_model;
ALTER TABLE agentic_config DROP COLUMN default_permission_mode;
ALTER TABLE agentic_config DROP COLUMN claude_rate_limit_status;
ALTER TABLE agentic_config DROP COLUMN claude_rate_limit_type;
ALTER TABLE agentic_config DROP COLUMN claude_rate_limit_resets_at;
ALTER TABLE agentic_config DROP COLUMN claude_rate_limit_overage_status;
ALTER TABLE agentic_config DROP COLUMN claude_rate_limit_overage_resets_at;
ALTER TABLE agentic_config DROP COLUMN claude_rate_limit_using_overage;
ALTER TABLE agentic_config DROP COLUMN claude_rate_limit_updated_at;
