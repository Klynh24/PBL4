// --- 1. CẤU TRÚC CHUNG ---
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  status?: string;
  timestamp?: string;
}

export type PrimaryRole = 'student' | 'teacher' | 'admin';

// --- 2. NGƯỜI DÙNG & DANH MỤC ---
export interface UserCatalogue {
  id: number;
  name: string;
  publish: number;
}

export interface User {
  id: number;
  email: string;
  name: string;
  phone?: string | null;
  image?: string | null;
  address?: string | null;
  roles?: string[]; // Trả về từ Backend
  role?: PrimaryRole; // Chuẩn hoá ở Frontend
  isBanned?: boolean;
}

// FIX LỖI TS2305: Export UpdateUserData cho ProfilePage
export type UpdateUserData = Partial<Pick<User, 'name' | 'address' | 'phone' | 'image'>>;

// --- 3. LỚP HỌC & PHÒNG HỌP ---
export interface Class {
  id: number;
  name: string;
  code: string;
  description?: string;
  user?: User; // Đối tượng giáo viên
}

export interface ClassDetails extends Class {
  teacher?: string;
  teacherId?: number;
  students?: User[];
}

export interface RoomResource {
  id: number;
  code: string;
  name: string;
  user?: User; // Người tạo phòng
}

export interface StoreRoomRequest {
  classId: number;
  name?: string;
  userId: number; // Bắt buộc theo RoomController
}

// --- 4. AUTH & REQUEST DATA ---
export interface LoginCredentials { email: string; password: string; }
export interface LoginResponseData { token: string; refreshToken: string; user: any; }

export interface PasswordResetRequestData { email: string; }
export interface ResetPasswordData { token: string; newPassword: string; }

export interface RegisterData {
  email: string;
  password: string;
  name: string;
  address: string;
  phone: string; // Fix lỗi 400 validation
  userCatalogues: number[]; // Mảng ID
}

// --- 5. BÀI TẬP, BÀI ĐĂNG, CHAT & NOTIFY ---
export interface CreateClassData { name: string; }
export interface AssignmentData { classId: number; title: string; description: string; dueDate: string; }
export interface Assignment { id: number; title: string; dueDate: string; description?: string; }
export interface SubmissionData { assignmentId: number; fileUrl: string; }
export interface GradeData { studentId: number; score: number; }
export interface CreatePostData { content: string; }
export interface ChatMessageData { message: string; }
export interface NotificationData { message: string; targetRole?: PrimaryRole; }
export interface Post { id: number; authorName: string; content: string; timestamp: string; authorRole: PrimaryRole; }
export interface Notification { id: number; message: string; read: boolean; createAt?: string; }