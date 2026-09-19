-- liquibase formatted sql
-- changeset kstarosta:20260919090000

CREATE TABLE issue_tracker (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  created_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE issue_label (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  issue_tracker_id BIGINT(20) NOT NULL,
  name VARCHAR(255) NOT NULL,
  color VARCHAR(16) DEFAULT NULL,
  created_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_issue_label_tracker_name (issue_tracker_id, name),
  CONSTRAINT fk_issue_label_tracker FOREIGN KEY (issue_tracker_id) REFERENCES issue_tracker (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE issue (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  issue_tracker_id BIGINT(20) NOT NULL,
  title VARCHAR(500) NOT NULL,
  description LONGTEXT,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
  assignee_user_id BIGINT(20) DEFAULT NULL,
  assignee_agent_id BIGINT(20) DEFAULT NULL,
  created_by_user_id BIGINT(20) DEFAULT NULL,
  created_by_agent_id BIGINT(20) DEFAULT NULL,
  created_at DATETIME DEFAULT NULL,
  updated_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_issue_tracker_id (issue_tracker_id),
  KEY idx_issue_status (issue_tracker_id, status),
  CONSTRAINT fk_issue_tracker FOREIGN KEY (issue_tracker_id) REFERENCES issue_tracker (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_issue_assignee_user FOREIGN KEY (assignee_user_id) REFERENCES ksfxuser (id) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_issue_assignee_agent FOREIGN KEY (assignee_agent_id) REFERENCES agent (id) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_issue_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES ksfxuser (id) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_issue_created_by_agent FOREIGN KEY (created_by_agent_id) REFERENCES agent (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE issue_issue_label (
  issue_id BIGINT(20) NOT NULL,
  issue_label_id BIGINT(20) NOT NULL,
  PRIMARY KEY (issue_id, issue_label_id),
  CONSTRAINT fk_issue_issue_label_issue FOREIGN KEY (issue_id) REFERENCES issue (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_issue_issue_label_label FOREIGN KEY (issue_label_id) REFERENCES issue_label (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE issue_comment (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  issue_id BIGINT(20) NOT NULL,
  content LONGTEXT,
  created_by_user_id BIGINT(20) DEFAULT NULL,
  created_by_agent_id BIGINT(20) DEFAULT NULL,
  created_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_issue_comment_issue_id (issue_id),
  CONSTRAINT fk_issue_comment_issue FOREIGN KEY (issue_id) REFERENCES issue (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_issue_comment_user FOREIGN KEY (created_by_user_id) REFERENCES ksfxuser (id) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_issue_comment_agent FOREIGN KEY (created_by_agent_id) REFERENCES agent (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE issue_asset (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  issue_id BIGINT(20) DEFAULT NULL,
  file_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(100) DEFAULT NULL,
  content LONGBLOB,
  file_size BIGINT(20) DEFAULT NULL,
  uploaded_by_user_id BIGINT(20) DEFAULT NULL,
  uploaded_by_agent_id BIGINT(20) DEFAULT NULL,
  created_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_issue_asset_issue_id (issue_id),
  CONSTRAINT fk_issue_asset_issue FOREIGN KEY (issue_id) REFERENCES issue (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_issue_asset_user FOREIGN KEY (uploaded_by_user_id) REFERENCES ksfxuser (id) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_issue_asset_agent FOREIGN KEY (uploaded_by_agent_id) REFERENCES agent (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
