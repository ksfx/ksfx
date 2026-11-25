--liquibase formatted sql

--changeset claude:20261125090000-1
-- Units (Kejo, 2026-11-25): agents can optionally be grouped into units of their project, shown as
-- collapsible groups in the agent sidebar. One level, no nesting; UI only for now - agents do not
-- see units. Units are project-bound like accesses; deleting a unit (or its project) leaves its
-- agents without a unit, never deletes an agent.
CREATE TABLE agent_unit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT NULL,
    CONSTRAINT fk_agent_unit_project FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE
);

--changeset claude:20261125090000-2
ALTER TABLE agent ADD COLUMN agent_unit_id BIGINT DEFAULT NULL;
ALTER TABLE agent ADD CONSTRAINT fk_agent_agent_unit FOREIGN KEY (agent_unit_id) REFERENCES agent_unit (id) ON DELETE SET NULL;
