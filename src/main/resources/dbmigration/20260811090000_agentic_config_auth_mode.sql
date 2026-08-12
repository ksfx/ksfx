-- liquibase formatted sql
-- changeset kstarosta:20260811090000

ALTER TABLE agentic_config ADD COLUMN auth_mode VARCHAR(32) DEFAULT 'API_KEY';
