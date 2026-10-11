-- liquibase formatted sql
-- changeset kstarosta:20261011090000

-- Phase 2 of the project concept: agents are confined to their own project's wikis, trackers and
-- agents unless their project is flagged admin (see AgentScope). Off for every project by default -
-- flag the project(s) whose agents document across projects before this deploy goes live.
ALTER TABLE project ADD COLUMN is_admin BIT(1) NOT NULL DEFAULT b'0' AFTER is_default;
