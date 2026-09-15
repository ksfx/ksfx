-- liquibase formatted sql
-- changeset kstarosta:20260915110000

ALTER TABLE agent_message ADD COLUMN completion_source VARCHAR(16) DEFAULT NULL;
