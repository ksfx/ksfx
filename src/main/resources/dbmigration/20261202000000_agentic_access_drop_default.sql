--liquibase formatted sql

-- NOT yet in liquibase-changeLog.xml - activate one release after 20261118090000_agentic_access_explicit
-- shipped and was confirmed working on HQ: until then the previous jar (which still reads is_default)
-- can roll back onto the untouched column.

--changeset claude:20261202000000-1
ALTER TABLE agentic_access DROP COLUMN is_default;
