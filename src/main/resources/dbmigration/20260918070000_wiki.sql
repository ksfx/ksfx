-- liquibase formatted sql
-- changeset kstarosta:20260918070000

CREATE TABLE wiki (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  created_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wiki_folder (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  wiki_id BIGINT(20) NOT NULL,
  parent_folder_id BIGINT(20) DEFAULT NULL,
  name VARCHAR(255) NOT NULL,
  created_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_wiki_folder_wiki_id (wiki_id),
  KEY idx_wiki_folder_parent (parent_folder_id),
  CONSTRAINT fk_wiki_folder_wiki FOREIGN KEY (wiki_id) REFERENCES wiki (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_wiki_folder_parent FOREIGN KEY (parent_folder_id) REFERENCES wiki_folder (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wiki_page (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  wiki_id BIGINT(20) NOT NULL,
  folder_id BIGINT(20) DEFAULT NULL,
  slug VARCHAR(255) NOT NULL,
  title VARCHAR(255) NOT NULL,
  created_at DATETIME DEFAULT NULL,
  updated_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_wiki_page_wiki_folder_slug (wiki_id, folder_id, slug),
  CONSTRAINT fk_wiki_page_wiki FOREIGN KEY (wiki_id) REFERENCES wiki (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_wiki_page_folder FOREIGN KEY (folder_id) REFERENCES wiki_folder (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wiki_page_version (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  wiki_page_id BIGINT(20) NOT NULL,
  content LONGTEXT,
  edited_by_user_id BIGINT(20) DEFAULT NULL,
  edited_by_agent_id BIGINT(20) DEFAULT NULL,
  source_agent_message_id BIGINT(20) DEFAULT NULL,
  created_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_wiki_page_version_page_id (wiki_page_id),
  CONSTRAINT fk_wiki_page_version_page FOREIGN KEY (wiki_page_id) REFERENCES wiki_page (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_wiki_page_version_user FOREIGN KEY (edited_by_user_id) REFERENCES ksfxuser (id) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_wiki_page_version_agent FOREIGN KEY (edited_by_agent_id) REFERENCES agent (id) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_wiki_page_version_message FOREIGN KEY (source_agent_message_id) REFERENCES agent_message (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wiki_asset (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  wiki_page_id BIGINT(20) DEFAULT NULL,
  file_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(100) DEFAULT NULL,
  content LONGBLOB,
  file_size BIGINT(20) DEFAULT NULL,
  uploaded_by_user_id BIGINT(20) DEFAULT NULL,
  uploaded_by_agent_id BIGINT(20) DEFAULT NULL,
  created_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_wiki_asset_page_id (wiki_page_id),
  CONSTRAINT fk_wiki_asset_page FOREIGN KEY (wiki_page_id) REFERENCES wiki_page (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_wiki_asset_user FOREIGN KEY (uploaded_by_user_id) REFERENCES ksfxuser (id) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_wiki_asset_agent FOREIGN KEY (uploaded_by_agent_id) REFERENCES agent (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
