-- liquibase formatted sql
-- changeset kstarosta:20210717220822

ALTER TABLE publishing_configuration ADD COLUMN cron_schedule VARCHAR(255) NULL DEFAULT NULL;
ALTER TABLE publishing_configuration ADD COLUMN cron_schedule_enabled BIT(1);
UPDATE publishing_configuration SET cron_schedule_enabled = b'0';