-- liquibase formatted sql
-- changeset kstarosta:20260729170000

ALTER TABLE publishing_configuration ADD COLUMN git_path VARCHAR(1024) NULL DEFAULT NULL;
ALTER TABLE publishing_resource ADD COLUMN git_path VARCHAR(1024) NULL DEFAULT NULL;
