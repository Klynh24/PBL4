CREATE TABLE classes_members (
  class_id INT UNSIGNED AUTO_INCREMENT,
  user_id  INT UNSIGNED ,
  role     ENUM('TEACHER','STUDENT') NOT NULL DEFAULT 'STUDENT',
  joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (class_id, user_id),
  INDEX idx_cm_user (user_id),
  CONSTRAINT fk_classes_members_class FOREIGN KEY (class_id)
           REFERENCES classes(id)
           ON DELETE CASCADE,
  CONSTRAINT fk_classes_members_user FOREIGN KEY (user_id)
           REFERENCES users(id)
           ON DELETE CASCADE
)