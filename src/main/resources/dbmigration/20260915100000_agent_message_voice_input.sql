-- liquibase formatted sql
-- changeset kstarosta:20260915100000

ALTER TABLE agent_message ADD COLUMN voice_input BIT(1) NOT NULL DEFAULT b'0';
