CREATE TABLE sessions (
  id            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  class_id      INT UNSIGNED,
  title         VARCHAR(200) NOT NULL,
  scheduled_at  DATETIME NULL,
  started_at    DATETIME NULL,
  ended_at      DATETIME NULL,
  status        ENUM('SCHEDULED','LIVE','ENDED','CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_sessions_class (class_id),
  INDEX idx_sessions_status (status),
  CONSTRAINT fk_sessions_class
    FOREIGN KEY (class_id) REFERENCES classes(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
)