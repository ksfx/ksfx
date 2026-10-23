--liquibase formatted sql

-- NOT YET IN liquibase-changeLog.xml ON PURPOSE. Include this in the release AFTER
-- 20261023090000_project_agentic_settings shipped: as long as these columns still exist, a
-- rollback to the pre-split jar simply works (it reads the untouched instance row). Their values
-- are frozen the moment the new code stops reading them, so keeping them one release costs
-- nothing. See the concept "Agentic settings per project" (KSFX wiki, Concepts).

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
