CREATE TABLE user_catalogue_permission(
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_catalogue_id INT UNSIGNED NOT NULL,
    permission_id INT UNSIGNED NOT NULL,
    CONSTRAINT fk_user_catalogue_permission FOREIGN KEY (user_catalogue_id) REFERENCES users_catalogues(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE ON UPDATE CASCADE
);