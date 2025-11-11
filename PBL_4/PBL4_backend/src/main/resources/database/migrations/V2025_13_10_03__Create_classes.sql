CREATE TABLE classes (
  id           INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id      INT UNSIGNED NOT NULL,
  name         VARCHAR(150) NOT NULL,
  description  VARCHAR(500),
  
  -- Sửa tên cột này cho nhất quán (không 'd'):
  create_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  -- CỘT BẠN BỊ THIẾU LÀ ĐÂY:
  update_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  INDEX idx_classes_owner_id (user_id),
    CONSTRAINT fk_user_id FOREIGN KEY (user_id)
      REFERENCES users(id)
      ON DELETE CASCADE
);