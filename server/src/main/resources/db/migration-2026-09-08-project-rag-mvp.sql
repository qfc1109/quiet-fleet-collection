USE qfc_site;
CREATE TABLE IF NOT EXISTS project_member (
  id BIGINT NOT NULL AUTO_INCREMENT, project_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', created_at DATETIME NOT NULL,
  PRIMARY KEY(id), UNIQUE KEY uk_project_member(project_id,user_id), KEY idx_project_member_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS knowledge_file (
  id BIGINT NOT NULL AUTO_INCREMENT, project_id BIGINT NOT NULL, file_id BIGINT NOT NULL,
  included TINYINT NOT NULL DEFAULT 1, sensitive TINYINT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING', error_message VARCHAR(500) NOT NULL DEFAULT '',
  source_updated_at DATETIME NULL, indexed_at DATETIME NULL, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL,
  PRIMARY KEY(id), UNIQUE KEY uk_knowledge_file(project_id,file_id), KEY idx_knowledge_file_status(project_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS knowledge_chunk (
  id BIGINT NOT NULL AUTO_INCREMENT, knowledge_file_id BIGINT NOT NULL, chunk_no INT NOT NULL,
  content TEXT NOT NULL, locator VARCHAR(255) NOT NULL DEFAULT '', created_at DATETIME NOT NULL,
  PRIMARY KEY(id), UNIQUE KEY uk_knowledge_chunk(knowledge_file_id,chunk_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS chat_feedback (
  id BIGINT NOT NULL AUTO_INCREMENT, answer_id VARCHAR(64) NOT NULL, user_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL, vote VARCHAR(20) NOT NULL, created_at DATETIME NOT NULL,
  PRIMARY KEY(id), UNIQUE KEY uk_chat_feedback(answer_id,user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
