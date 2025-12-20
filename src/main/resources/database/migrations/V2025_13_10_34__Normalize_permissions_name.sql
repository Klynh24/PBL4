USE team1;

-- 1. Xóa tất cả khoảng trắng dư thừa trong tên quyền (Ví dụ: 'user : me' thành 'user:me')
UPDATE permissions SET name = REPLACE(name, ' ', '');

-- 2. Đổi tên Module từ số ít sang số nhiều cho khớp với Aspect (Ví dụ: 'class' thành 'classes')
-- Dựa trên log: ASPECT ĐANG CẦN QUYỀN: [classes:store]
UPDATE permissions SET name = REPLACE(name, 'class:', 'classes:') WHERE name LIKE 'class:%';
UPDATE permissions SET name = REPLACE(name, 'user:', 'users:') WHERE name LIKE 'user:%';
UPDATE permissions SET name = REPLACE(name, 'assignment:', 'assignments:') WHERE name LIKE 'assignment:%';
UPDATE permissions SET name = REPLACE(name, 'submission:', 'submissions:') WHERE name LIKE 'submission:%';
UPDATE permissions SET name = REPLACE(name, 'message:', 'messages:') WHERE name LIKE 'message:%';
UPDATE permissions SET name = REPLACE(name, 'conversation:', 'conversations:') WHERE name LIKE 'conversation:%';
UPDATE permissions SET name = REPLACE(name, 'notification:', 'notifications:') WHERE name LIKE 'notification:%';
UPDATE permissions SET name = REPLACE(name, 'permission:', 'permissions:') WHERE name LIKE 'permission:%';

-- 3. Kiểm tra lại kết quả
SELECT name FROM permissions;