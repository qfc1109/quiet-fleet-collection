SET NAMES utf8mb4;

ALTER TABLE qfc_site.project
  ADD COLUMN deleted_at DATETIME NULL AFTER updated_at,
  ADD COLUMN deleted_by_user_id BIGINT NULL AFTER deleted_at,
  ADD KEY idx_project_deleted_at (deleted_at);
