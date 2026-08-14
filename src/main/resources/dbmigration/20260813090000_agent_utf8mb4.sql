-- liquibase formatted sql
-- changeset kstarosta:20260813090000

ALTER TABLE agent CONVERT TO CHARACTER SET utf8mb4;
ALTER TABLE agent_message CONVERT TO CHARACTER SET utf8mb4;
