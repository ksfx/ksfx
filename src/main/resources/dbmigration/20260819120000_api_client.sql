-- liquibase formatted sql
-- changeset kstarosta:20260819120000

CREATE TABLE api_client (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  name VARCHAR(200) NOT NULL,
  api_token VARCHAR(64) DEFAULT NULL,
  enabled BIT(1) DEFAULT b'1',
  created_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_api_client_api_token (api_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
