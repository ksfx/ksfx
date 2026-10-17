-- liquibase formatted sql
-- changeset kstarosta:20260921120000

-- Per-tracker issue numbers (GitHub semantics) replacing the globally-counting DB id as the
-- user-visible "#N" - see KSFX issue "IDs in the KSFX-Trackers are per Instance not per Project".
-- Backfill numbers existing issues per tracker in creation (id) order, so already-communicated
-- relative ordering within a tracker stays intact.

ALTER TABLE issue ADD COLUMN number BIGINT(20) DEFAULT NULL AFTER issue_tracker_id;

UPDATE issue i
JOIN (
  SELECT id, ROW_NUMBER() OVER (PARTITION BY issue_tracker_id ORDER BY id) AS tracker_number
  FROM issue
) numbered ON numbered.id = i.id
SET i.number = numbered.tracker_number;

ALTER TABLE issue MODIFY COLUMN number BIGINT(20) NOT NULL;
ALTER TABLE issue ADD UNIQUE KEY uq_issue_tracker_number (issue_tracker_id, number);
