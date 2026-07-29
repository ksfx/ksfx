-- liquibase formatted sql
-- changeset kstarosta:20260729120000

ALTER TABLE activity ADD COLUMN git_path VARCHAR(1024) NULL DEFAULT NULL;

CREATE TABLE activity_repository_config (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  repo_url VARCHAR(1024) NOT NULL,
  branch VARCHAR(255) DEFAULT 'master',
  access_token LONGTEXT,
  local_clone_path VARCHAR(1024) NOT NULL,
  last_synced_commit VARCHAR(64) DEFAULT NULL,
  last_synced_at DATETIME DEFAULT NULL,
  enabled BIT(1) DEFAULT b'0',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
