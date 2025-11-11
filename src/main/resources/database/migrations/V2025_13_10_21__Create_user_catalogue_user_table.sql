CREATE TABLE user_catalogue_user(
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_catalogue_id INT UNSIGNED NOT NULL,
    user_id INT UNSIGNED NOT NULL,
    CONSTRAINT fk_user_catalogue_user_id FOREIGN KEY (user_catalogue_id) REFERENCES users_catalogues(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_users_catalogue_users_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE
);