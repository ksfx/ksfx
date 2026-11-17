--liquibase formatted sql

-- Activated 2026-11-17, after 20261118090000_agentic_access_explicit shipped and was confirmed working
-- on HQ: a rollback of THIS release lands on the explicit-access release, which no longer reads the
-- column - only a two-release rollback would need it back.

--changeset claude:20261202000000-1
ALTER TABLE agentic_access DROP COLUMN is_default;
