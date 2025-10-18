import axios from 'axios';
// Import kiểu User từ AuthContext để đảm bảo code nhất quán
import { User } from '../contexts/AuthContext'; 

// --- Định nghĩa các kiểu dữ liệu khác ---
interface LoginCredentials {
  email: string;
  password: string;
}

interface RegisterData extends LoginCredentials {
  role: 'student' | 'teacher';
}

// Interface riêng cho mock data, bao gồm cả password để giả lập đăng nhập
interface UserWithPassword extends User {
    password: string;
}

interface AssignmentData {
    classId: number;
    title: string;
    description: string;
    dueDate: string;
}
interface SubmissionData {
    fileUrl: string;
}
interface GradeData {
    studentId: number;
    score: number;
}
interface EventData {
    title: string;
    date: string;
    description: string;
}

// THÊM MỚI: Interface cho việc tạo thông báo
interface NotificationData {
    message: string;
    targetRole?: 'student' | 'teacher';
}


// ========================================================================
// --- MOCK DATA SECTION (KHO DỮ LIỆU GIẢ) ---
// ========================================================================

let mockUsers: UserWithPassword[] = [
    { id: 1, email: 'admin@gmail.com', password: '123', role: 'admin', name: 'Quản Trị Viên', dob: '1990-01-01', gender: 'Khác' },
    { id: 2, email: 'teacher@gmail.com', password: '123', role: 'teacher', name: 'Trần Văn An', dob: '1985-05-15', gender: 'Nam' },
    { id: 3, email: 'student@gmail.com', password: '123', role: 'student', name: 'Nguyễn Thị Bích', dob: '2005-08-20', gender: 'Nữ' },
    { id: 4, email: 'student2@gmail.com', password: '123', role: 'student', name: 'Lê Văn Cường', dob: '2005-09-10', gender: 'Nam' },
    { id: 5, email: 'teacher2@gmail.com', password: '123', role: 'teacher', name: 'Phạm Thị Diệu', dob: '1992-11-30', gender: 'Nữ'},
];
let mockClasses = [
  { id: 1, name: 'Toán cao cấp A1', teacher: 'Trần Văn An', teacherId: 2 },
  { id: 2, name: 'Vật lý đại cương', teacher: 'Phạm Thị Diệu', teacherId: 5 },
  { id: 3, name: 'Lập trình Web Frontend', teacher: 'Trần Văn An', teacherId: 2 },
  { id: 4, name: 'Cấu trúc dữ liệu & Giải thuật', teacher: 'Phạm Thị Diệu', teacherId: 5 },
];
const mockEnrollments = [ { userId: 3, classId: 1 }, { userId: 3, classId: 3 }, { userId: 4, classId: 2 }];
let mockAssignments = [ { id: 101, classId: 1, title: 'Bài tập chương 1: Ma trận', description: 'Làm các bài tập 1.1 đến 1.5 trong sách giáo khoa.', dueDate: '2025-10-20' }, { id: 102, classId: 1, title: 'Bài tập chương 2: Định thức', description: 'Làm các bài tập 2.1, 2.2', dueDate: '2025-10-28' }, { id: 301, classId: 3, title: 'Bài tập HTML/CSS', description: 'Code lại giao diện trang chủ a.com', dueDate: '2025-10-25' }];
let mockNotifications = [ { id: 1, message: "Lớp 'Lập trình Web Frontend' có bài tập mới.", read: false }, { id: 2, message: "Bài tập 'Toán cao cấp A1' sẽ hết hạn vào ngày mai.", read: false }, { id: 3, message: "Chào mừng bạn đến với hệ thống học tập Pastel Learn!", read: true }];
let mockEvents = [ { id: 1, title: 'Thi giữa kỳ Toán A1', date: '2025-11-05', description: 'Thi tập trung tại phòng A101' }, { id: 2, title: 'Nghỉ lễ Quốc Khánh', date: '2025-09-02', description: 'Toàn trường được nghỉ lễ' }];


// ========================================================================
// --- CÁC HÀM TIỆN ÍCH ---
// ========================================================================
const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

const createMockToken = (user: User) => {
    const payload = { sub: user.email, role: user.role, id: user.id, name: user.name, dob: user.dob, gender: user.gender };
    const header = { alg: 'HS256', typ: 'JWT' };
    const encodedHeader = btoa(JSON.stringify(header));
    const encodedPayload = btoa(unescape(encodeURIComponent(JSON.stringify(payload))));
    return `${encodedHeader}.${encodedPayload}.mock_signature`;
};

// ========================================================================
// --- CÁC HÀM API GIẢ LẬP ---
// ========================================================================

// 🔐 Auth
export const login = async (credentials: LoginCredentials) => {
    await sleep(500);
    const inputEmail = credentials.email.trim().toLowerCase();
    const inputPassword = credentials.password.trim();
    const user = mockUsers.find(u => u.email.toLowerCase() === inputEmail && u.password === inputPassword);
    if (user) {
        const token = createMockToken(user);
        return Promise.resolve({ data: { token } });
    }
    return Promise.reject({ response: { data: { message: 'Email hoặc mật khẩu không chính xác' } } });
};

export const register = async (userData: RegisterData) => {
    await sleep(500);
    if (mockUsers.some(u => u.email.trim().toLowerCase() === userData.email.trim().toLowerCase())) {
        return Promise.reject({ response: { data: { message: 'Email đã tồn tại' } } });
    }
    const newUser: UserWithPassword = { id: Date.now(), password: userData.password.trim(), email: userData.email.trim().toLowerCase(), role: userData.role, name: 'Người dùng mới', dob: '', gender: 'Khác' };
    mockUsers.push(newUser);
    return Promise.resolve({ data: { id: newUser.id, email: newUser.email, role: newUser.role } });
};
export const logout = async () => {
    await sleep(200);
    return Promise.resolve({ data: { message: 'Logged out' } });
};

// 👥 Users
export const getUsers = async () => {
    await sleep(500);
    return Promise.resolve({ data: mockUsers });
};

// THÊM MỚI: Hàm tạo user giả lập
export const createUser = async (userData: RegisterData) => {
    await sleep(500);
    if (mockUsers.some(u => u.email.trim().toLowerCase() === userData.email.trim().toLowerCase())) {
        return Promise.reject({ response: { data: { message: 'Email đã tồn tại' } } });
    }
    const newUser: UserWithPassword = { id: Date.now(), password: userData.password.trim(), email: userData.email.trim().toLowerCase(), role: userData.role, name: `User ${Date.now()}`, dob: '', gender: 'Khác' };
    mockUsers.push(newUser);
    return Promise.resolve({ data: newUser });
};

export const updateUser = async (id: number, data: Partial<User>): Promise<{ data: User }> => {
    await sleep(300);
    let userFound = false;
    let updatedUser: UserWithPassword | undefined;
    mockUsers = mockUsers.map(u => {
        if (u.id === id) {
            userFound = true;
            updatedUser = { ...u, ...data };
            return updatedUser;
        }
        return u;
    });
    if (userFound && updatedUser) {
        const token = localStorage.getItem('token');
        if (token) {
            const payload = JSON.parse(atob(token.split('.')[1]));
            if (payload.id === id) {
                const newToken = createMockToken(updatedUser);
                localStorage.setItem('token', newToken);
            }
        }
        return Promise.resolve({ data: updatedUser });
    }
    return Promise.reject({ response: { data: { message: `User with id ${id} not found` } } });
};
export const deleteUser = async (id: number) => {
    await sleep(300);
    mockUsers = mockUsers.filter(u => u.id !== id);
    return Promise.resolve({ data: { message: 'User deleted' } });
};
export const getClasses = async () => {
    await sleep(700);
    const token = localStorage.getItem('token');
    if (!token) return Promise.resolve({ data: [] });
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const { id: userId, role } = payload;
        if (role === 'admin') return Promise.resolve({ data: mockClasses });
        if (role === 'teacher') return Promise.resolve({ data: mockClasses.filter(c => c.teacherId === userId) });
        if (role === 'student') {
            const enrolledClassIds = mockEnrollments.filter(e => e.userId === userId).map(e => e.classId);
            return Promise.resolve({ data: mockClasses.filter(c => enrolledClassIds.includes(c.id)) });
        }
        return Promise.resolve({ data: [] });
    } catch (e) {
        return Promise.resolve({ data: [] });
    }
};
export const createClass = async (classData: { name: string }) => {
    await sleep(500);
    const token = localStorage.getItem('token');
    const payload = JSON.parse(atob(token!.split('.')[1]));
    const newClass = { id: Date.now(), name: classData.name, teacher: payload.name, teacherId: payload.id };
    mockClasses.push(newClass);
    return Promise.resolve({ data: newClass });
};
export const getClassDetails = async (id: string) => {
    await sleep(600);
    const classId = parseInt(id, 10);
    const details = mockClasses.find(c => c.id === classId);
    if(details) {
        const studentsInClass = mockEnrollments.filter(e => e.classId === classId).map(e => mockUsers.find(u => u.id === e.userId)!);
        return Promise.resolve({ data: { ...details, students: studentsInClass } });
    }
    return Promise.reject({ response: { data: { message: 'Class not found' } } });
};
export const joinClass = async (classIdStr: string) => {
    await sleep(400);
    const classId = parseInt(classIdStr, 10);
    const token = localStorage.getItem('token');
    const payload = JSON.parse(atob(token!.split('.')[1]));
    if (mockClasses.some(c => c.id === classId) && !mockEnrollments.some(e => e.userId === payload.id && e.classId === classId)) {
        mockEnrollments.push({ userId: payload.id, classId: classId });
        return Promise.resolve({ data: { message: "Joined successfully" } });
    }
    return Promise.reject({ response: { data: { message: 'Class not found or already joined' } } });
};
export const getAssignments = async (classId: string) => {
    await sleep(400);
    return Promise.resolve({ data: mockAssignments.filter(a => a.classId === parseInt(classId, 10)) });
};
export const createAssignment = async (data: AssignmentData) => {
    await sleep(500);
    const newAssignment = { id: Date.now(), ...data };
    mockAssignments.push(newAssignment);
    return Promise.resolve({ data: newAssignment });
};
export const submitAssignment = async (id: number, data: SubmissionData) => {
    await sleep(600);
    console.log(`Submitting assignment ${id} with file ${data.fileUrl}`);
    return Promise.resolve({ data: { message: "Submitted" } });
};
export const gradeAssignment = async (id: number, data: GradeData) => {
    await sleep(500);
    console.log(`Grading assignment ${id} for student ${data.studentId} with score ${data.score}`);
    return Promise.resolve({ data: { message: "Graded" } });
};

// 🔔 Notifications
export const getNotifications = async () => {
    await sleep(300);
    return Promise.resolve({ data: mockNotifications });
};

// THÊM MỚI: Hàm tạo thông báo giả lập
export const createNotification = async (data: NotificationData) => {
    await sleep(400);
    const newNotification = { id: Date.now(), message: data.message, read: false };
    mockNotifications.unshift(newNotification); // Thêm vào đầu danh sách
    return Promise.resolve({ data: newNotification });
};

export const markNotificationAsRead = async (id: number) => {
    await sleep(200);
    mockNotifications = mockNotifications.map(n => n.id === id ? { ...n, read: true } : n);
    return Promise.resolve({ data: { id, read: true } });
};

// 📅 Calendar / Events
export const getEvents = async () => {
    await sleep(400);
    return Promise.resolve({ data: mockEvents });
};
export const getStats = async () => {
    await sleep(800);
    const updatedStats = { users: mockUsers.length, classes: mockClasses.length, assignments: mockAssignments.length, feedback: 30 };
    return Promise.resolve({ data: updatedStats });
};

