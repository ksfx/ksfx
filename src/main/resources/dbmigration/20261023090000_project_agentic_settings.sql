--liquibase formatted sql

--changeset claude:20261023090000-1
-- Agentic settings per project (concept "Agentic settings per project", KSFX wiki): account
-- (auth mode + key) and policy (default model / permission mode) move from the instance to the
-- project - no inheritance, a project without a row (or disabled) has no working agents. The
-- rate-limit snapshot columns move along: they describe the account, and the account now belongs
-- to the project. agentic_config keeps only instance infrastructure (workspace root, CLI path,
-- kill switch); its moved columns are dropped by a LATER release (see
-- 20261101000000_agentic_config_drop_moved_columns.sql, deliberately not yet in the changelog,
-- so a rollback to the previous jar keeps working against the untouched columns).
CREATE TABLE project_agentic_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    auth_mode VARCHAR(32) DEFAULT 'API_KEY',
    api_key LONGTEXT,
    default_model VARCHAR(64) DEFAULT NULL,
    default_permission_mode VARCHAR(64) DEFAULT 'default',
    enabled BIT(1) NOT NULL DEFAULT b'0',
    claude_rate_limit_status VARCHAR(32) DEFAULT NULL,
    claude_rate_limit_type VARCHAR(32) DEFAULT NULL,
    claude_rate_limit_resets_at DATETIME DEFAULT NULL,
    claude_rate_limit_overage_status VARCHAR(32) DEFAULT NULL,
    claude_rate_limit_overage_resets_at DATETIME DEFAULT NULL,
    claude_rate_limit_using_overage BIT(1) DEFAULT NULL,
    claude_rate_limit_updated_at DATETIME DEFAULT NULL,
    CONSTRAINT uq_project_agentic_settings UNIQUE (project_id),
    CONSTRAINT fk_project_agentic_settings_project FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE
);

--changeset claude:20261023090000-2
-- Backfill: every existing project gets the instance's current values, so nothing changes for
-- anybody on deploy day. Splitting projects onto their own keys is an admin's task afterwards.
-- No agentic_config row (never configured instance) -> no settings rows, same as today.
INSERT INTO project_agentic_settings (project_id, auth_mode, api_key, default_model, default_permission_mode, enabled,
    claude_rate_limit_status, claude_rate_limit_type, claude_rate_limit_resets_at, claude_rate_limit_overage_status,
    claude_rate_limit_overage_resets_at, claude_rate_limit_using_overage, claude_rate_limit_updated_at)
SELECT p.id, c.auth_mode, c.api_key, c.default_model, c.default_permission_mode, c.enabled,
    c.claude_rate_limit_status, c.claude_rate_limit_type, c.claude_rate_limit_resets_at, c.claude_rate_limit_overage_status,
    c.claude_rate_limit_overage_resets_at, c.claude_rate_limit_using_overage, c.claude_rate_limit_updated_at
FROM project p CROSS JOIN agentic_config c;
