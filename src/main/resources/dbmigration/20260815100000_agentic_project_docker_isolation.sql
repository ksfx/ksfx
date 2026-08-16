-- liquibase formatted sql
-- changeset kstarosta:20260815100000

ALTER TABLE agentic_project ADD COLUMN docker_isolation_enabled BIT(1) NOT NULL DEFAULT b'0';
ALTER TABLE agentic_project ADD COLUMN docker_container_name VARCHAR(128) DEFAULT NULL;
ALTER TABLE agentic_project ADD COLUMN docker_container_status VARCHAR(32) DEFAULT 'NOT_CREATED';
ALTER TABLE agentic_project ADD COLUMN docker_container_last_checked_at DATETIME DEFAULT NULL;
