USE team1;

-- 1. GÁN QUYỀN CHO NHÓM GIÁO VIÊN (ID = 2)
-- Giáo viên có quyền Store, Update, List, View trên hầu hết các module
INSERT INTO user_catalogue_permission (user_catalogue_id, permission_id)
SELECT 2, id FROM permissions 
WHERE name LIKE 'assignment : %' 
   OR name LIKE 'class : %' 
   OR name LIKE 'submission : %'
   OR name LIKE 'message : %'
   OR name LIKE 'conversation : %'
   OR name LIKE 'notification : %'
   OR name IN ('user : me', 'user : list', 'upload : store');

-- 2. GÁN QUYỀN CHO NHÓM HỌC SINH (ID = 3)
-- Học sinh chỉ có quyền List, View và một số Store/Update hạn chế (như nộp bài)
INSERT INTO user_catalogue_permission (user_catalogue_id, permission_id)
SELECT 3, id FROM permissions 
WHERE name IN (
    'user : me', 
    'class : list', 'class : join', 'class : view',
    'assignment : list', 'assignment : view',
    'submission : store', 'submission : update', 'submission : list',
    'message : store', 'message : list',
    'notification : list',
    'upload : store'
);

-- 3. GÁN TOÀN BỘ QUYỀN CHO QUẢN TRỊ VIÊN (ID = 1)
INSERT INTO user_catalogue_permission (user_catalogue_id, permission_id)
SELECT 1, id FROM permissions;