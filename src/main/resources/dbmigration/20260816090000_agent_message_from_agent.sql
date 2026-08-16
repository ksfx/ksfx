-- liquibase formatted sql
-- changeset kstarosta:20260816090000

ALTER TABLE agent_message ADD COLUMN from_agent_id BIGINT(20) DEFAULT NULL AFTER agent_id;
ALTER TABLE agent_message ADD KEY idx_agent_message_from_agent_id (from_agent_id);
ALTER TABLE agent_message ADD CONSTRAINT fk_agent_message_from_agent FOREIGN KEY (from_agent_id) REFERENCES agent (id) ON DELETE SET NULL ON UPDATE CASCADE;
