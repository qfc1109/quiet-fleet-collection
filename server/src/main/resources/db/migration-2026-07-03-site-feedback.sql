SET NAMES utf8mb4;

USE qfc_site;

CREATE TABLE IF NOT EXISTS site_feedback (
  id BIGINT NOT NULL AUTO_INCREMENT,
  author_user_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  content TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_site_feedback_author_user_id (author_user_id),
  KEY idx_site_feedback_status (status),
  KEY idx_site_feedback_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
