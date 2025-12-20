USE team1;

-- Module Thành viên & Nhóm (Users & Catalogues)
INSERT INTO permissions (name, publish) VALUES 
('user : store', 2), ('user : update', 2), ('user : delete', 2), ('user : list', 2), ('user : me', 2),
('user_catalogue : store', 2), ('user_catalogue : update', 2), ('user_catalogue : list', 2), ('user_catalogue : view', 2);

-- Module Lớp học & Phòng (Classes & Rooms)
INSERT INTO permissions (name, publish) VALUES 
('class : store', 2), ('class : update', 2), ('class : delete', 2), ('class : list', 2), ('class : join', 2), ('class : members', 2),
('room : store', 2), ('room : view', 2), ('room : join', 2), ('room : leave', 2);

-- Module Bài tập & Nộp bài (Assignments & Submissions)
INSERT INTO permissions (name, publish) VALUES 
('assignment : store', 2), ('assignment : update', 2), ('assignment : delete', 2), ('assignment : list', 2),
('submission : store', 2), ('submission : update', 2), ('submission : delete', 2), ('submission : list', 2),
('upload : store', 2);

-- Module Trao đổi (Messages, Conversations, Notifications)
INSERT INTO permissions (name, publish) VALUES 
('message : store', 2), ('message : update', 2), ('message : delete', 2), ('message : list', 2),
('conversation : store', 2), ('conversation : update', 2), ('conversation : delete', 2), ('conversation : list', 2),
('notification : store', 2), ('notification : update', 2), ('notification : delete', 2), ('notification : list', 2);

-- Module Quyền hạn (Permissions API)
INSERT INTO permissions (name, publish) VALUES 
('permission : store', 2), ('permission : update', 2), ('permission : delete', 2), ('permission : list', 2);