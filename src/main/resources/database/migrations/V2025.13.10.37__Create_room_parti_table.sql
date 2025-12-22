USE team1;

CREATE TABLE IF NOT EXISTS room_participants (
    room_id INT NOT NULL,
    user_id INT NOT NULL,
    PRIMARY KEY (room_id, user_id),
    CONSTRAINT fk_room_id FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);