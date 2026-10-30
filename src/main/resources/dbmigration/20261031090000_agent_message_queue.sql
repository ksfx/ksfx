--liquibase formatted sql

--changeset claude:20261031090000-1
-- Messages sent while the agent's turn is still running (chat "queue"): held here, NOT in
-- agent_message, until their turn starts - agent_message is ordered by id, and a queued message
-- inserted there would sort before the answer that is still being produced. When the running turn
-- ends, all queued messages of the agent are moved into agent_message (in order, after that
-- answer) and run together as the next turn. cli_text is the exact text handed to the CLI
-- (message + note about uploaded files), content what the chat shows. utf8mb4 like agent_message
-- (emoji in chat text, see 20260813090000).
-- Agent-to-agent messages queue here too when the target is busy: from_agent_id is the sender,
-- deliver_reply = the answer of the turn that processes this entry goes back to the sender
-- automatically, is_reply = this entry IS such a delivered answer (never forwarded back - no loops).
CREATE TABLE agent_message_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    content LONGTEXT,
    cli_text LONGTEXT,
    attachments LONGTEXT,
    voice_input BIT(1) NOT NULL DEFAULT b'0',
    from_agent_id BIGINT DEFAULT NULL,
    deliver_reply BIT(1) NOT NULL DEFAULT b'0',
    is_reply BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME DEFAULT NULL,
    CONSTRAINT fk_agent_message_queue_agent FOREIGN KEY (agent_id) REFERENCES agent (id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_message_queue_from_agent FOREIGN KEY (from_agent_id) REFERENCES agent (id) ON DELETE SET NULL
) DEFAULT CHARSET=utf8mb4;
