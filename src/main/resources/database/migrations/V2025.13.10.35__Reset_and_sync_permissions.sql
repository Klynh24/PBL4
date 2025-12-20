USE team1; -- Đảm bảo dùng đúng database team1

-- 1. Tắt kiểm tra khóa ngoại để dọn dẹp
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE user_catalogue_permission;
TRUNCATE TABLE permissions;
TRUNCATE TABLE users_catalogues;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO users_catalogues (id, name, publish) VALUES 
(1, 'Quản trị viên', 1), 
(2, 'Giáo viên', 1), 
(3, 'Học sinh', 1);

-- 3. Nạp danh sách Permissions chuẩn Enum Java (Không dấu cách)
INSERT INTO permissions (id, name, publish) VALUES 
(1, 'user:me', 2), (2, 'user:list', 2), (3, 'user:store', 2), (4, 'user:update', 2), (5, 'user:delete', 2),
(6, 'user_catalogue:store', 2), (7, 'user_catalogue:list', 2), (8, 'user_catalogue:update', 2),
(9, 'permission:list', 2), (10, 'permission:store', 2),
(11, 'classes:store', 2), (12, 'classes:update', 2), (13, 'classes:list', 2), (14, 'classes:join', 2), (15, 'classes:members', 2), (16, 'classes:delete', 2),
(17, 'assignments:store', 2), (18, 'assignments:update', 2), (19, 'assignments:list', 2), (20, 'assignments:delete', 2),
(21, 'submissions:store', 2), (22, 'submissions:update', 2), (23, 'submissions:list', 2), (24, 'submissions:delete', 2),
(25, 'messages:store', 2), (26, 'messages:list', 2), (27, 'messages:delete', 2),
(28, 'notifications:store', 2), (29, 'notifications:list', 2),
(30, 'conversations:list', 2), (31, 'conversations:store', 2),
(32, 'upload:store', 2);

-- 4. GÁN QUYỀN CHO ADMIN (ID 1): Có tất cả các quyền
INSERT INTO user_catalogue_permission (user_catalogue_id, permission_id)
SELECT 1, id FROM permissions;

-- 5. GÁN QUYỀN CHO GIÁO VIÊN (ID 2): Quyền quản lý lớp, bài tập, chấm bài
INSERT INTO user_catalogue_permission (user_catalogue_id, permission_id)
SELECT 2, id FROM permissions 
WHERE name NOT LIKE 'permission:%' -- Không cho giáo viên sửa bảng quyền hệ thống
AND name NOT LIKE 'user_catalogue:%'; -- Không cho giáo viên sửa nhóm thành viên

-- 6. GÁN QUYỀN CHO HỌC SINH (ID 3): Chỉ xem và thực hiện các chức năng cá nhân
INSERT INTO user_catalogue_permission (user_catalogue_id, permission_id)
SELECT 3, id FROM permissions 
WHERE name IN (
    'user:me', 
    'classes:list', 'classes:join', 'classes:members',
    'assignments:list', 
    'submissions:store', 'submissions:update', 'submissions:list',
    'messages:store', 'messages:list',
    'notifications:list',
    'conversations:list',
    'upload:store'
);