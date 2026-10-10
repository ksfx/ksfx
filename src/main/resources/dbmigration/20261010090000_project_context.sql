-- liquibase formatted sql
-- changeset kstarosta:20261010090000

-- Project as the top-level context (phase 1 of the concept "Project as the top-level context",
-- Lexaris wiki). The former agentic_project becomes project; wiki, issue_tracker, activity,
-- publishing_configuration, both category tables, code_lib and git_sync_config get a mandatory
-- project_id. One default project is created and everything that has no project today is
-- attached to it - existing projects keep their agents, only "Ungrouped" agents join the default
-- project. Nothing filters yet, so the application behaves exactly as before this migration.
-- git_sync_config becomes one row per project (the existing row becomes the default project's).

-- 1. agentic_project -> project (+ default flag)
ALTER TABLE agentic_project RENAME TO project;
ALTER TABLE project ADD COLUMN is_default BIT(1) NOT NULL DEFAULT b'0' AFTER description;

-- 2. agent.agentic_project_id -> project_id (constraint renamed too; still SET NULL at DB level as a
--    backstop, the application never leaves an agent without a project)
ALTER TABLE agent DROP FOREIGN KEY fk_agent_agentic_project;
ALTER TABLE agent DROP INDEX idx_agent_agentic_project_id;
ALTER TABLE agent CHANGE COLUMN agentic_project_id project_id BIGINT DEFAULT NULL;
ALTER TABLE agent ADD KEY idx_agent_project_id (project_id);
ALTER TABLE agent ADD CONSTRAINT fk_agent_project FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE SET NULL ON UPDATE CASCADE;

-- 3. the default project
-- docker_container_status is stored by Ebean as the enum ORDINAL ('0' = NOT_CREATED); the column's
-- own DEFAULT 'NOT_CREATED' is a leftover from an older migration and would make the row unloadable.
INSERT INTO project (name, description, is_default, created_at, docker_isolation_enabled, docker_container_status) VALUES ('Default', 'Created by the project migration - holds everything that existed before projects. Rename freely.', b'1', NOW(), b'0', '0');
SET @default_project_id = LAST_INSERT_ID();

UPDATE agent SET project_id = @default_project_id WHERE project_id IS NULL;

-- 4. project_id on every content table, backfilled, then mandatory
ALTER TABLE wiki ADD COLUMN project_id BIGINT(20) DEFAULT NULL AFTER id;
UPDATE wiki SET project_id = @default_project_id WHERE project_id IS NULL;
ALTER TABLE wiki MODIFY COLUMN project_id BIGINT(20) NOT NULL;
ALTER TABLE wiki ADD KEY idx_wiki_project (project_id);
ALTER TABLE wiki ADD CONSTRAINT fk_wiki_project FOREIGN KEY (project_id) REFERENCES project (id) ON UPDATE CASCADE;

ALTER TABLE issue_tracker ADD COLUMN project_id BIGINT(20) DEFAULT NULL AFTER id;
UPDATE issue_tracker SET project_id = @default_project_id WHERE project_id IS NULL;
ALTER TABLE issue_tracker MODIFY COLUMN project_id BIGINT(20) NOT NULL;
ALTER TABLE issue_tracker ADD KEY idx_issue_tracker_project (project_id);
ALTER TABLE issue_tracker ADD CONSTRAINT fk_issue_tracker_project FOREIGN KEY (project_id) REFERENCES project (id) ON UPDATE CASCADE;

ALTER TABLE activity_category ADD COLUMN project_id BIGINT(20) DEFAULT NULL AFTER id;
UPDATE activity_category SET project_id = @default_project_id WHERE project_id IS NULL;
ALTER TABLE activity_category MODIFY COLUMN project_id BIGINT(20) NOT NULL;
ALTER TABLE activity_category ADD KEY idx_activity_category_project (project_id);
ALTER TABLE activity_category ADD CONSTRAINT fk_activity_category_project FOREIGN KEY (project_id) REFERENCES project (id) ON UPDATE CASCADE;

ALTER TABLE activity ADD COLUMN project_id BIGINT(20) DEFAULT NULL AFTER id;
UPDATE activity SET project_id = @default_project_id WHERE project_id IS NULL;
ALTER TABLE activity MODIFY COLUMN project_id BIGINT(20) NOT NULL;
ALTER TABLE activity ADD KEY idx_activity_project (project_id);
ALTER TABLE activity ADD CONSTRAINT fk_activity_project FOREIGN KEY (project_id) REFERENCES project (id) ON UPDATE CASCADE;

ALTER TABLE publishing_category ADD COLUMN project_id BIGINT(20) DEFAULT NULL AFTER id;
UPDATE publishing_category SET project_id = @default_project_id WHERE project_id IS NULL;
ALTER TABLE publishing_category MODIFY COLUMN project_id BIGINT(20) NOT NULL;
ALTER TABLE publishing_category ADD KEY idx_publishing_category_project (project_id);
ALTER TABLE publishing_category ADD CONSTRAINT fk_publishing_category_project FOREIGN KEY (project_id) REFERENCES project (id) ON UPDATE CASCADE;

ALTER TABLE publishing_configuration ADD COLUMN project_id BIGINT(20) DEFAULT NULL AFTER id;
UPDATE publishing_configuration SET project_id = @default_project_id WHERE project_id IS NULL;
ALTER TABLE publishing_configuration MODIFY COLUMN project_id BIGINT(20) NOT NULL;
ALTER TABLE publishing_configuration ADD KEY idx_publishing_configuration_project (project_id);
ALTER TABLE publishing_configuration ADD CONSTRAINT fk_publishing_configuration_project FOREIGN KEY (project_id) REFERENCES project (id) ON UPDATE CASCADE;

ALTER TABLE code_lib ADD COLUMN project_id BIGINT(20) DEFAULT NULL AFTER id;
UPDATE code_lib SET project_id = @default_project_id WHERE project_id IS NULL;
ALTER TABLE code_lib MODIFY COLUMN project_id BIGINT(20) NOT NULL;
ALTER TABLE code_lib ADD KEY idx_code_lib_project (project_id);
ALTER TABLE code_lib ADD CONSTRAINT fk_code_lib_project FOREIGN KEY (project_id) REFERENCES project (id) ON UPDATE CASCADE;

-- 5. one Git configuration per project; the existing (single) row becomes the default project's.
--    ON DELETE CASCADE: a project's Git settings go with the project.
ALTER TABLE git_sync_config ADD COLUMN project_id BIGINT(20) DEFAULT NULL AFTER id;
UPDATE git_sync_config SET project_id = @default_project_id WHERE project_id IS NULL;
ALTER TABLE git_sync_config MODIFY COLUMN project_id BIGINT(20) NOT NULL;
ALTER TABLE git_sync_config ADD UNIQUE KEY uq_git_sync_config_project (project_id);
ALTER TABLE git_sync_config ADD CONSTRAINT fk_git_sync_config_project FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE ON UPDATE CASCADE;
