-- liquibase formatted sql
-- changeset kstarosta:20260811160000

CREATE TABLE agent_schedule (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  agent_id BIGINT(20) NOT NULL,
  name VARCHAR(255) NOT NULL,
  task_prompt LONGTEXT,
  cron_schedule VARCHAR(255) DEFAULT NULL,
  cron_schedule_enabled BIT(1) DEFAULT b'0',
  last_run_at DATETIME DEFAULT NULL,
  last_run_status VARCHAR(32) DEFAULT NULL,
  last_run_error LONGTEXT,
  created_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_agent_schedule_agent_id (agent_id),
  CONSTRAINT fk_agent_schedule_agent FOREIGN KEY (agent_id) REFERENCES agent (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
