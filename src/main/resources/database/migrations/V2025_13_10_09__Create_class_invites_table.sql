CREATE TABLE class_invites (
  id            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY ,
  class_id      INT UNSIGNED ,
  token         CHAR(32) NOT NULL UNIQUE,
  role          ENUM('TEACHER','STUDENT') NOT NULL DEFAULT 'STUDENT',
  expires_at    DATETIME NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  used_id       INT UNSIGNED,
  used_at       DATETIME NULL,
  INDEX idx_ci_class (class_id),
  CONSTRAINT fk_ci_class
    FOREIGN KEY (class_id) REFERENCES classes(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT fk_ci_used_by
    FOREIGN KEY (used_id) REFERENCES users(id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
)