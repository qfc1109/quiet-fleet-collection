SET NAMES utf8mb4;

USE qfc_site;

CREATE TABLE IF NOT EXISTS project_issue (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  author_user_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  content TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_project_issue_project_id (project_id),
  KEY idx_project_issue_author_user_id (author_user_id),
  KEY idx_project_issue_status (status),
  KEY idx_project_issue_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
