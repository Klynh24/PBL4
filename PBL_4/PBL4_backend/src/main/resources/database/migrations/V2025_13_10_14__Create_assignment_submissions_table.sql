CREATE TABLE assignment_submissions (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    assignment_id INT UNSIGNED ,
    user_id INT UNSIGNED,
    file_url VARCHAR(100) NOT NULL,
    score VARCHAR(10),
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_assignment_id FOREIGN KEY (assignment_id)
        REFERENCES assignments(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_assignment_user_id FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE CASCADE
);