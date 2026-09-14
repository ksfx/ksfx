-- liquibase formatted sql
-- changeset kstarosta:20260914090000

ALTER TABLE agentic_config ADD COLUMN default_model VARCHAR(64) DEFAULT NULL;
