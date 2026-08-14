-- liquibase formatted sql
-- changeset kstarosta:20260812180000

ALTER TABLE agent_message ADD COLUMN generated_files LONGTEXT DEFAULT NULL;
