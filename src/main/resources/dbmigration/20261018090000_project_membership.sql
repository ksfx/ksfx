-- liquibase formatted sql
-- changeset kstarosta:20261018090000

-- Project membership for users, step 1 (KSFX wiki, "Concept: Project membership for users"):
-- which user may use which project at which level (READER / WRITER / OWNER). Instance
-- administration stays the existing ADMIN role. Backfill: every existing user becomes OWNER of
-- every existing project and holds ADMIN - nothing changes for anybody on deploy day; narrowing
-- down is an administrator's operational task afterwards.
CREATE TABLE project_membership (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    user_id BIGINT(20) NOT NULL,
    project_id BIGINT(20) NOT NULL,
    level VARCHAR(16) NOT NULL,
    created_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_project_membership (user_id, project_id),
    KEY idx_project_membership_project (project_id),
    CONSTRAINT fk_project_membership_user FOREIGN KEY (user_id) REFERENCES ksfxuser (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_project_membership_project FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO project_membership (user_id, project_id, level, created_at)
SELECT u.id, p.id, 'OWNER', NOW() FROM ksfxuser u CROSS JOIN project p;

INSERT INTO ksfxrole (name) SELECT 'ADMIN' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ksfxrole WHERE name = 'ADMIN');
INSERT INTO user_ksfxroles (user, ksfxroles)
SELECT u.id, r.id FROM ksfxuser u JOIN ksfxrole r ON r.name = 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM user_ksfxroles ur WHERE ur.user = u.id AND ur.ksfxroles = r.id);
