--liquibase formatted sql

-- Activated 2026-11-16, after 20261116090000_agentic_access and 20261117090000_agentic_access_defaults
-- shipped and were confirmed working on HQ: a rollback of THIS release lands on the access release,
-- which no longer reads this table - only a two-release rollback would need a restore. Everything it
-- held now lives on agentic_access (account, rate-limit snapshot, defaults, enabled).

--changeset claude:20261201000000-1
DROP TABLE project_agentic_settings;
