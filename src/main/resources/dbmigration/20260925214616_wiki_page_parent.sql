-- liquibase formatted sql
-- changeset kstarosta:20260925214616

-- Subpages: WikiPage.parentPage, self-referencing within the SAME folder (see
-- WikiService#validateParentPage - a page's parent must live in the same folder as the page
-- itself, so folders stay the only real containment hierarchy and the page tree is just a
-- finer-grained ordering within one, never a second independent hierarchy that could diverge from
-- it). ON DELETE CASCADE mirrors wiki_folder's own parent_folder_id: deleting a page removes its
-- whole subtree, same as deleting a folder already removes everything underneath it.

ALTER TABLE wiki_page ADD COLUMN parent_page_id BIGINT(20) DEFAULT NULL AFTER folder_id;
ALTER TABLE wiki_page ADD KEY idx_wiki_page_parent (parent_page_id);
ALTER TABLE wiki_page ADD CONSTRAINT fk_wiki_page_parent FOREIGN KEY (parent_page_id) REFERENCES wiki_page (id) ON DELETE CASCADE ON UPDATE CASCADE;
