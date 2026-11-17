--liquibase formatted sql

--changeset claude:20261117100000-1
-- Mail service (concept "Mail service", KSFX wiki): one platform-wide SMTP configuration, set by
-- the platform admin on Admin -> Mail. Single row. daily_limit is the emergency brake across the
-- whole platform; limit_notice_sent_on makes sure the admins are told only once per day.
CREATE TABLE mail_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enabled BIT(1) NOT NULL DEFAULT b'0',
    smtp_host VARCHAR(255) DEFAULT NULL,
    smtp_port INT NOT NULL DEFAULT 587,
    smtp_security VARCHAR(16) NOT NULL DEFAULT 'STARTTLS',
    smtp_username VARCHAR(255) DEFAULT NULL,
    smtp_password LONGTEXT,
    from_address VARCHAR(255) DEFAULT NULL,
    from_name VARCHAR(255) DEFAULT NULL,
    reply_to VARCHAR(255) DEFAULT NULL,
    daily_limit INT NOT NULL DEFAULT 200,
    limit_notice_sent_on DATE DEFAULT NULL
);

--changeset claude:20261117100000-2
-- The outbox is also the log: every mail is written here first and sent by a background job (with
-- retries), so callers never wait for SMTP. BLOCKED = rejected by the daily limit, kept for the log.
CREATE TABLE mail_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    status VARCHAR(16) NOT NULL,
    source_type VARCHAR(16) NOT NULL,
    source_label VARCHAR(255) DEFAULT NULL,
    agent_id BIGINT DEFAULT NULL,
    project_id BIGINT DEFAULT NULL,
    to_addresses TEXT NOT NULL,
    cc_addresses TEXT,
    subject VARCHAR(998) NOT NULL,
    body LONGTEXT,
    attempts INT NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at DATETIME NOT NULL,
    next_attempt_at DATETIME DEFAULT NULL,
    sent_at DATETIME DEFAULT NULL,
    INDEX idx_mail_outbox_status (status, next_attempt_at),
    INDEX idx_mail_outbox_created (created_at),
    CONSTRAINT fk_mail_outbox_agent FOREIGN KEY (agent_id) REFERENCES agent (id) ON DELETE SET NULL,
    CONSTRAINT fk_mail_outbox_project FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE SET NULL
);

-- Attachments are copied in when the mail is accepted - the file in the agent's workspace may
-- change or vanish before the job sends it.
CREATE TABLE mail_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    outbox_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) DEFAULT NULL,
    data LONGBLOB NOT NULL,
    CONSTRAINT fk_mail_attachment_outbox FOREIGN KEY (outbox_id) REFERENCES mail_outbox (id) ON DELETE CASCADE
);
