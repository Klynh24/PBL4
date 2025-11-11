CREATE TABLE rooms (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    class_id INT UNSIGNED ,
    user_id INT UNSIGNED ,
    name VARCHAR(50) NOT NULL,
    status VARCHAR(100) NOT NULL,
    started_at DATETIME NULL,
    ended_at DATETIME NULL,
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_room_id FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

     CONSTRAINT fk_class_room_id FOREIGN KEY (class_id)
            REFERENCES classes(id)
            ON DELETE CASCADE
);