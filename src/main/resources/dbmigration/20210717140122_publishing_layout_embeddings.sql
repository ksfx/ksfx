-- liquibase formatted sql
-- changeset kstarosta:20210717140122

ALTER TABLE publishing_configuration ADD COLUMN layout_integration VARCHAR(255) NULL DEFAULT NULL;
UPDATE publishing_configuration SET layout_integration = 'publishing_viewer_layout_integration_full.html' WHERE embed_in_layout = b'1';

ALTER TABLE publishing_resource ADD COLUMN layout_integration VARCHAR(255) NULL DEFAULT NULL;
UPDATE publishing_resource SET layout_integration = 'publishing_viewer_layout_integration_full.html' WHERE embed_in_layout = b'1';