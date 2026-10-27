--liquibase formatted sql

--changeset claude:20261027090000-1
-- The model that ACTUALLY answered a turn, captured from the CLI's init stream event. Ground
-- truth next to the configured resolution chain (agent override > project default > provider
-- default) - the chain only says what KSFX asked for; this says what ran. Shown on the agent form
-- and the chat header, and it ends "which model is this agent really on" debates for good
-- (self-reports inside a resumed conversation parrot the transcript and are worthless evidence).
ALTER TABLE agent_message ADD COLUMN model VARCHAR(64) DEFAULT NULL;
