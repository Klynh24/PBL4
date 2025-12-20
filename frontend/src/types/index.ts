// src/types/index.ts

export interface ApiResponse<T> {
 success: boolean;
 message: string;
 data: T;
 status?: string;
 timestamp?: string;
}

export interface UserCatalogue {
 id: number;
 name: string;
 publish: number;
}

export type PrimaryRole = 'student' | 'teacher' | 'admin';

// Sửa: Không khai báo trùng 'role'. Giữ roles (mảng) và role (vai trò chính) dạng optional.
export interface User {
 id: number;
 email: string;
 name: string;
 phone?: string | null;
 image?: string | null;
 address?: string | null;

 // BE trả về mảng tên vai trò
 roles?: string[];

 // Vai trò chính đã chuẩn hoá (tính ở FE)
 role?: PrimaryRole;

 isBanned?: boolean;
}

export interface Class {
 id: number;
 name: string;
 teacher: string;
 description?: string;
 user?: User;
}

export interface ClassDetails {
 id: number;
 name: string;
 code: string; // <-- ĐÃ THÊM: Mã lớp học cho tính năng Join Class
 teacher: string;
 teacherId: number;
 students: User[];
 user: User;
}

// --- Các kiểu dữ liệu cho API ---

export interface LoginCredentials {
 email: string;
 password: string;
}

export interface LoginResponseData {
 token: string;
 refreshToken: string;
 user: any; // hoặc User nếu BE trả về
}

export interface RegisterData {
 email: string;
 password: string;
 userCatalogues: number[];
 name: string;
 address: string;
 phone: string;
}

export type UpdateUserData = Partial<Pick<User, 'name' | 'address' | 'phone' | 'image'>>;

export interface CreateClassData {
 name: string;
}

export interface JoinClassData {
 code?: string;
}

export interface AssignmentData {
 classId: number;
 title: string;
 description: string;
 dueDate: string;
}

export interface Assignment {
 id: number;
 title: string;
 dueDate: string;
}

export interface SubmissionData {
    // Trường này đã được thêm vào payload nhưng thiếu trong interface
    assignmentId: number;

    // Trường fileUrl đã có
    fileUrl: string;

    // Bạn có thể thêm các trường khác nếu cần (ví dụ: description)
}

export interface GradeData {
 studentId: number;
 score: number;
}

export interface ChatMessageData {
 message: string;
}

export interface NotificationData {
 message: string;
 targetRole?: 'student' | 'teacher' | 'admin';
}

export interface MarkReadData {
 read: boolean;
}

export interface CreateUserCatalogueData {
 name: string;
 publish: string | number;
}

export type UpdateUserCatalogueData = Partial<CreateUserCatalogueData>;

export interface Notification {
 id: number;
 message: string;
 read: boolean;
 createAt?: string;
}

export interface Post {
 id: number;
 authorName: string;
 content: string;
 timestamp: string;
 authorRole: 'teacher' | 'student' | 'admin';
}

export interface CreatePostData {
 content: string;
}

export interface PasswordResetRequestData {
 email: string;
}

export interface ResetPasswordData {
 token: string;
 newPassword: string;
}