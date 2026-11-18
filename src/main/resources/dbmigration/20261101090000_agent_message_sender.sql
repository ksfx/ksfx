--liquibase formatted sql

--changeset claude:20261101090000-1
-- The human who sent a chat message (shown by first name instead of a fixed "You" - with several
-- members in a project, "You" was misleading for everyone else reading the chat). Null for older
-- messages, schedule prompts and every non-USER row; the chat then shows a neutral "User".
ALTER TABLE agent_message ADD COLUMN from_user_id BIGINT DEFAULT NULL;
ALTER TABLE agent_message ADD CONSTRAINT fk_agent_message_from_user FOREIGN KEY (from_user_id) REFERENCES ksfxuser (id) ON DELETE SET NULL;
ALTER TABLE agent_message_queue ADD COLUMN from_user_id BIGINT DEFAULT NULL;
ALTER TABLE agent_message_queue ADD CONSTRAINT fk_agent_message_queue_from_user FOREIGN KEY (from_user_id) REFERENCES ksfxuser (id) ON DELETE SET NULL;
