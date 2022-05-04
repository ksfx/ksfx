-- liquibase formatted sql
-- changeset kstarosta:20220504220824

ALTER TABLE publishing_configuration_cache_data ADD COLUMN file_name_or_page_title VARCHAR(2048) NULL DEFAULT NULL;
ALTER TABLE publishing_resource_cache_data ADD COLUMN file_name_or_page_title VARCHAR(2048) NULL DEFAULT NULL;