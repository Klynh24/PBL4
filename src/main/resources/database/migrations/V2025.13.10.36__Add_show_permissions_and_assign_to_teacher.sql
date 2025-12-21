USE team1;

-- 1. THÊM CÁC QUYỀN "SHOW" VÀO BẢNG PERMISSIONS VỚI PUBLISH = 2
-- Bảng của bạn có id tự tăng, name, publish, created_at, updated_at
INSERT IGNORE INTO permissions (name, publish) VALUES 
('user:show', 2),
('classes:show', 2),
('assignments:show', 2),
('notifications:show', 2),
('messages:show', 2),
('submissions:show', 2),
('conversations:show', 2);

-- 2. GÁN QUYỀN CHO ADMIN (ID: 1)
INSERT IGNORE INTO user_catalogue_permission (user_catalogue_id, permission_id)
SELECT 1, id FROM permissions 
WHERE name IN ('user:show', 'classes:show', 'assignments:show', 'notifications:show', 'messages:show', 'submissions:show', 'conversations:show');

-- 3. GÁN QUYỀN CHO GIÁO VIÊN (ID: 2)
INSERT IGNORE INTO user_catalogue_permission (user_catalogue_id, permission_id)
SELECT 2, id FROM permissions 
WHERE name IN ('user:show', 'classes:show', 'assignments:show', 'notifications:show', 'messages:show', 'submissions:show', 'conversations:show');

-- 4. GÁN QUYỀN CHO HỌC SINH (ID: 3)
INSERT IGNORE INTO user_catalogue_permission (user_catalogue_id, permission_id)
SELECT 3, id FROM permissions 
WHERE name IN ('user:show', 'classes:show', 'assignments:show', 'notifications:show', 'messages:show', 'submissions:show', 'conversations:show');