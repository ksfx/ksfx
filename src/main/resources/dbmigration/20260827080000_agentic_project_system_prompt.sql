-- liquibase formatted sql
-- changeset kejo:20260827080000

ALTER TABLE agentic_project ADD COLUMN system_prompt LONGTEXT DEFAULT NULL;
