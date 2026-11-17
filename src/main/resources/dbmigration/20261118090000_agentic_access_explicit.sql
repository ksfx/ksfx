--liquibase formatted sql

--changeset claude:20261118090000-1
-- No default access anymore (Kejo, 2026-11-17): every agent names its access explicitly - a new
-- agent cannot be saved without one. Agents that ran on their project's default until now get
-- exactly that access written down, so nothing changes for them. is_default is no longer read;
-- it is dropped one release later (20261202000000).
UPDATE agent a
    JOIN agentic_access x ON x.project_id = a.project_id AND x.is_default = b'1'
SET a.agentic_access_id = x.id
WHERE a.agentic_access_id IS NULL;
