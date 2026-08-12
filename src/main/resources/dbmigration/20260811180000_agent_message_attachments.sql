-- liquibase formatted sql
-- changeset kstarosta:20260811180000

ALTER TABLE agent_message ADD COLUMN attachments LONGTEXT;
