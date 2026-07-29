-- liquibase formatted sql
-- changeset kstarosta:20260729130000

CREATE TABLE activity_library (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  name VARCHAR(512) DEFAULT NULL,
  description VARCHAR(2048) DEFAULT NULL,
  groovy_code LONGTEXT,
  git_path VARCHAR(1024) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
