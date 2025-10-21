CREATE TABLE session_policies (
  session_id         INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  allow_screen_share TINYINT(1) NOT NULL DEFAULT 1,
  allow_chat         TINYINT(1) NOT NULL DEFAULT 1,
  mute_on_join       TINYINT(1) NOT NULL DEFAULT 0,
  lock_room          TINYINT(1) NOT NULL DEFAULT 0,
  CONSTRAINT fk_policies_session
    FOREIGN KEY (session_id) REFERENCES sessions(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
)