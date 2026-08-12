-- liquibase formatted sql
-- changeset kstarosta:20260810120000

CREATE TABLE agentic_config (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  api_key LONGTEXT,
  claude_cli_path VARCHAR(1024) DEFAULT 'claude',
  default_permission_mode VARCHAR(64) DEFAULT 'default',
  workspace_root VARCHAR(1024) DEFAULT NULL,
  enabled BIT(1) DEFAULT b'0',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
