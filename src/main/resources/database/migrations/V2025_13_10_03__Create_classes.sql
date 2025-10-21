CREATE TABLE classes (
  id           INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id      INT UNSIGNED NOT NULL,
  name          VARCHAR(150) NOT NULL,
  description   VARCHAR(500),
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_classes_owner_id (user_id),
   CONSTRAINT fk_user_id FOREIGN KEY (user_id)
         REFERENCES users(id)
         ON DELETE CASCADE
)