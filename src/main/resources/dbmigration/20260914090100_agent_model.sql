-- liquibase formatted sql
-- changeset kstarosta:20260914090100

ALTER TABLE agent ADD COLUMN model VARCHAR(64) DEFAULT NULL;
