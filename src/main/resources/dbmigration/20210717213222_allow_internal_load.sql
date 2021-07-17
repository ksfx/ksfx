-- liquibase formatted sql
-- changeset kstarosta:20210717213222

ALTER TABLE publishing_configuration ADD COLUMN allow_internal_load BIT(1);
UPDATE publishing_configuration SET allow_internal_load = b'1';