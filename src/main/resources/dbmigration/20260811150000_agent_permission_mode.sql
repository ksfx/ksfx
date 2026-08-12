-- liquibase formatted sql
-- changeset kstarosta:20260811150000

ALTER TABLE agent ADD COLUMN permission_mode VARCHAR(64) DEFAULT NULL;
