--liquibase formatted sql

--changeset claude:20261116090000-1
-- Accesses per project (concept "Agentic accesses"): a project can hold several named accounts -
-- "company API key", "Kejo's Max subscription" - and each agent picks one; agents without a choice
-- use the project's default access. Auth mode, key and the rate-limit snapshot move here from
-- project_agentic_settings, because they describe an ACCOUNT, and a project may now have several.
-- Default model, permission mode and the enabled switch stay project policy.
CREATE TABLE agentic_access (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    auth_mode VARCHAR(32) NOT NULL DEFAULT 'API_KEY',
    api_key LONGTEXT,
    is_default BIT(1) NOT NULL DEFAULT b'0',
    claude_rate_limit_status VARCHAR(32) DEFAULT NULL,
    claude_rate_limit_type VARCHAR(32) DEFAULT NULL,
    claude_rate_limit_resets_at DATETIME DEFAULT NULL,
    claude_rate_limit_overage_status VARCHAR(32) DEFAULT NULL,
    claude_rate_limit_overage_resets_at DATETIME DEFAULT NULL,
    claude_rate_limit_using_overage BIT(1) DEFAULT NULL,
    claude_rate_limit_updated_at DATETIME DEFAULT NULL,
    created_at DATETIME DEFAULT NULL,
    CONSTRAINT fk_agentic_access_project FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE
);

--changeset claude:20261116090000-2
-- Backfill: every project's current account becomes its default access "Standard", so nothing
-- changes for anybody on deploy day.
INSERT INTO agentic_access (project_id, name, auth_mode, api_key, is_default,
    claude_rate_limit_status, claude_rate_limit_type, claude_rate_limit_resets_at, claude_rate_limit_overage_status,
    claude_rate_limit_overage_resets_at, claude_rate_limit_using_overage, claude_rate_limit_updated_at, created_at)
SELECT s.project_id, 'Standard', COALESCE(s.auth_mode, 'API_KEY'), s.api_key, b'1',
    s.claude_rate_limit_status, s.claude_rate_limit_type, s.claude_rate_limit_resets_at, s.claude_rate_limit_overage_status,
    s.claude_rate_limit_overage_resets_at, s.claude_rate_limit_using_overage, s.claude_rate_limit_updated_at, NOW()
FROM project_agentic_settings s;

--changeset claude:20261116090000-3
-- The access an agent runs on (null = the project's default access), and the access a turn
-- actually ran on - recorded per answer, so usage stays with the account it was spent on even
-- after an agent switches. Deleting an access sets both back to null.
ALTER TABLE agent ADD COLUMN agentic_access_id BIGINT DEFAULT NULL;
ALTER TABLE agent ADD CONSTRAINT fk_agent_agentic_access FOREIGN KEY (agentic_access_id) REFERENCES agentic_access (id) ON DELETE SET NULL;
ALTER TABLE agent_message ADD COLUMN agentic_access_id BIGINT DEFAULT NULL;
ALTER TABLE agent_message ADD CONSTRAINT fk_agent_message_agentic_access FOREIGN KEY (agentic_access_id) REFERENCES agentic_access (id) ON DELETE SET NULL;

--changeset claude:20261116090000-4
-- Until today a project had exactly one account, so every past answer ran on what is now its
-- project's "Standard" access.
UPDATE agent_message m
    JOIN agent a ON a.id = m.agent_id
    JOIN agentic_access x ON x.project_id = a.project_id AND x.is_default = b'1'
SET m.agentic_access_id = x.id
WHERE m.role = 'ASSISTANT';
