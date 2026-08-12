-- liquibase formatted sql
-- changeset kstarosta:20260811170000

ALTER TABLE agent ADD COLUMN api_token VARCHAR(64) DEFAULT NULL;
ALTER TABLE agent ADD UNIQUE KEY uq_agent_api_token (api_token);
