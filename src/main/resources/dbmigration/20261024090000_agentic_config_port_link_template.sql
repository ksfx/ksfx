--liquibase formatted sql

--changeset claude:20261024090000-1
-- Optional URL template for the breadcrumb port links (concept: breadcrumb ports dropdown +
-- HTTPS). With HSTS on the KSFX domain the browser force-upgrades http://domain:18160 to https,
-- which a bare dev server cannot answer. The template routes the links through the reverse proxy
-- instead, e.g. https://p{port}.apps.example.com - {port} is the published host port. Empty =
-- plain http://<request host>:<port> as before. Instance infrastructure, lives on agentic_config.
ALTER TABLE agentic_config ADD COLUMN port_link_template VARCHAR(512) DEFAULT NULL;
