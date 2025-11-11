CREATE TABLE notifications (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL ,
    message VARCHAR(500) NOT NULL,
    type VARCHAR(50),
    read_status BOOLEAN DEFAULT FALSE,
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_notifications_id FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);