--liquibase formatted sql

-- NOT yet in liquibase-changeLog.xml - activate one release after 20261116090000_agentic_access and
-- 20261117090000_agentic_access_defaults shipped and were confirmed working on HQ (same two-step as
-- 20261101000000 for agentic_config): until then the old jar can still roll back onto the table.
-- Everything it held now lives on agentic_access (account, rate-limit snapshot, defaults, enabled).

--changeset claude:20261201000000-1
DROP TABLE project_agentic_settings;
