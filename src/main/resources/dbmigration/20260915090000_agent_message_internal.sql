-- liquibase formatted sql
-- changeset kstarosta:20260915090000

ALTER TABLE agent_message ADD COLUMN internal BIT(1) NOT NULL DEFAULT b'0';
