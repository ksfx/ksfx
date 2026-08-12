-- liquibase formatted sql
-- changeset kstarosta:20260810121000

CREATE TABLE agent (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(1024) DEFAULT NULL,
  system_prompt LONGTEXT,
  claude_session_id VARCHAR(128) DEFAULT NULL,
  workspace_path VARCHAR(1024) DEFAULT NULL,
  enabled BIT(1) DEFAULT b'1',
  created_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE agent_message (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  agent_id BIGINT(20) NOT NULL,
  role VARCHAR(32) NOT NULL,
  content LONGTEXT,
  tool_activity LONGTEXT,
  created_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_agent_message_agent_id (agent_id),
  CONSTRAINT fk_agent_message_agent FOREIGN KEY (agent_id) REFERENCES agent (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
