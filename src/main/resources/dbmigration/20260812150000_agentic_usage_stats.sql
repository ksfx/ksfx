-- liquibase formatted sql
-- changeset kstarosta:20260812150000

ALTER TABLE agentic_config ADD COLUMN claude_rate_limit_status VARCHAR(32) DEFAULT NULL;
ALTER TABLE agentic_config ADD COLUMN claude_rate_limit_type VARCHAR(32) DEFAULT NULL;
ALTER TABLE agentic_config ADD COLUMN claude_rate_limit_resets_at DATETIME DEFAULT NULL;
ALTER TABLE agentic_config ADD COLUMN claude_rate_limit_overage_status VARCHAR(32) DEFAULT NULL;
ALTER TABLE agentic_config ADD COLUMN claude_rate_limit_overage_resets_at DATETIME DEFAULT NULL;
ALTER TABLE agentic_config ADD COLUMN claude_rate_limit_using_overage BIT(1) DEFAULT NULL;
ALTER TABLE agentic_config ADD COLUMN claude_rate_limit_updated_at DATETIME DEFAULT NULL;

ALTER TABLE agent_message ADD COLUMN input_tokens INT DEFAULT NULL;
ALTER TABLE agent_message ADD COLUMN output_tokens INT DEFAULT NULL;
ALTER TABLE agent_message ADD COLUMN cache_creation_input_tokens INT DEFAULT NULL;
ALTER TABLE agent_message ADD COLUMN cache_read_input_tokens INT DEFAULT NULL;
ALTER TABLE agent_message ADD COLUMN duration_ms INT DEFAULT NULL;
