--liquibase formatted sql

--changeset claude:20261117090000-1
-- Defaults per access instead of per project: the model often follows the account (sonnet on the
-- pay-per-token key, opus on the subscription), and "enabled" becomes a switch per account - turn
-- off a blocked or over-budget account and only its agents stop. An agent's own model/permission
-- mode still wins; the access default applies when the agent leaves the field empty.
ALTER TABLE agentic_access ADD COLUMN default_model VARCHAR(64) DEFAULT NULL;
ALTER TABLE agentic_access ADD COLUMN default_permission_mode VARCHAR(64) DEFAULT 'default';
ALTER TABLE agentic_access ADD COLUMN enabled BIT(1) NOT NULL DEFAULT b'1';

--changeset claude:20261117090000-2
-- Backfill: every access takes over its project's current defaults and enabled flag, so nothing
-- changes on deploy day. project_agentic_settings is no longer read afterwards; it is dropped
-- one release later (20261201000000).
UPDATE agentic_access x
    JOIN project_agentic_settings s ON s.project_id = x.project_id
SET x.default_model = s.default_model,
    x.default_permission_mode = COALESCE(s.default_permission_mode, 'default'),
    x.enabled = s.enabled;
