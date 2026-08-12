-- liquibase formatted sql
-- changeset kstarosta:20260812090000

CREATE TABLE agentic_project (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(1024) DEFAULT NULL,
  created_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE agent ADD COLUMN agentic_project_id BIGINT(20) DEFAULT NULL AFTER id;
ALTER TABLE agent ADD KEY idx_agent_agentic_project_id (agentic_project_id);
ALTER TABLE agent ADD CONSTRAINT fk_agent_agentic_project FOREIGN KEY (agentic_project_id) REFERENCES agentic_project (id) ON DELETE SET NULL ON UPDATE CASCADE;
