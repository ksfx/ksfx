-- liquibase formatted sql
-- changeset kstarosta:20261012090000

-- Phase 3 of the project concept: the dashboard is per project (the single global
-- DASHBOARD_CONTENT Generic Data Store entry becomes the default project's dashboard), and an
-- API client may be bound to a project, which is then the default scope of its calls.
ALTER TABLE project ADD COLUMN dashboard_content LONGTEXT NULL AFTER system_prompt;
UPDATE project SET dashboard_content = (SELECT data_value FROM generic_data_store WHERE data_key = 'DASHBOARD_CONTENT' LIMIT 1) WHERE is_default = b'1';
DELETE FROM generic_data_store WHERE data_key = 'DASHBOARD_CONTENT';

ALTER TABLE api_client ADD COLUMN project_id BIGINT(20) DEFAULT NULL AFTER id;
ALTER TABLE api_client ADD KEY idx_api_client_project (project_id);
ALTER TABLE api_client ADD CONSTRAINT fk_api_client_project FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE SET NULL ON UPDATE CASCADE;
