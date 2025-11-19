import axios, { AxiosError } from 'axios';
import { 
  ApiResponse, 
  User,
  Class, 
  ClassDetails, 
  LoginCredentials,
  LoginResponseData, 
  RegisterData,
  UpdateUserData,
  CreateClassData,
  Assignment,
  AssignmentData,
  SubmissionData,
  GradeData,
  ChatMessageData,
  NotificationData,
  CreateUserCatalogueData,
  UpdateUserCatalogueData,
  UserCatalogue,
  Notification,
  Post,
  CreatePostData,
  PasswordResetRequestData,
  ResetPasswordData
} from '../types';

// 1. Khởi tạo instance với tên là axiosInstance (thay vì api)
const axiosInstance = axios.create({
    baseURL: 'http://localhost:8080',
    headers: {
        'Content-Type': 'application/json',
    },
});

//

// 2. Cấu hình Interceptor
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    const url = config.url || '';

    const publicEndpoints = [
      '/api/v1/auth/login',
      '/api/v1/auth/register',
      '/api/v1/auth/forgot-password',
      '/api/v1/auth/reset-password',
      '/api-docs',
      '/v3/api-docs',
      '/swagger-ui'
    ];

    const isPublic = publicEndpoints.some(endpoint => url.startsWith(endpoint));

    if (token && !isPublic) {
        if (!config.headers) {
            config.headers = {} as any;
        }
        (config.headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

export const getErrorMessage = (error: unknown): string => {
    if (error instanceof AxiosError) {
        if (error.response?.status === 403) {
             return "Truy cập bị từ chối (403). Kiểm tra Token hoặc Quyền.";
        }
        return error.response?.data?.error?.message || error.response?.data?.message || error.message || 'Lỗi không xác định';
    }
    if (error instanceof Error) {
        return error.message;
    }
    return String(error);
};

// --- AUTH ---
export const login = async (credentials: LoginCredentials) => {
    return await axiosInstance.post<ApiResponse<LoginResponseData>>('/api/v1/auth/login', credentials);
};
export const register = async (userData: RegisterData) => {
    return await axiosInstance.post<ApiResponse<User>>('/api/v1/auth/register', userData);
};
export const requestPasswordReset = async (data: PasswordResetRequestData) => {
    return await axiosInstance.post<ApiResponse<any>>('/api/v1/auth/forgot-password', data);
};
export const resetPassword = async (data: ResetPasswordData) => {
    return await axiosInstance.post<ApiResponse<any>>('/api/v1/auth/reset-password', data);
};
export const logout = async () => {
    return await axiosInstance.post<ApiResponse<any>>('/api/v1/auth/logout');
};

// --- USERS ---
export const getMe = async () => {
    return await axiosInstance.get<ApiResponse<User>>('/api/v1/users/me');
};
export const getUsers = async () => {
    return await axiosInstance.get<ApiResponse<User[]>>('/api/v1/users');
};
export const createUser = async (userData: RegisterData) => {
    return await axiosInstance.post<ApiResponse<User>>('/api/v1/users', userData);
};
export const updateUser = async (id: number, data: UpdateUserData) => {
    return await axiosInstance.put<ApiResponse<User>>(`/api/v1/users/${id}`, data);
};
export const deleteUser = async (id: number) => {
    return await axiosInstance.delete<ApiResponse<any>>(`/api/v1/users/${id}`);
};
export const banUser = async (id: number) => {
    return await axiosInstance.put<ApiResponse<User>>(`/api/v1/users/${id}/ban`);
};
export const unbanUser = async (id: number) => {
    return await axiosInstance.put<ApiResponse<User>>(`/api/v1/users/${id}/unban`);
};

// --- USER CATALOGUES ---
export const getUserCatalogues = async () => {
    return await axiosInstance.get<ApiResponse<any[]>>('/api/v1/user_catalogues');
};
export const getUserCatalogueDetails = async (id: number | string) => {
    return await axiosInstance.get<ApiResponse<UserCatalogue>>(`/api/v1/user_catalogues/${id}`);
};
export const createUserCatalogue = async (data: CreateUserCatalogueData) => {
    return await axiosInstance.post<ApiResponse<any>>('/api/v1/user_catalogues', data);
};
export const updateUserCatalogue = async (id: number | string, data: UpdateUserCatalogueData) => {
    return await axiosInstance.put<ApiResponse<any>>(`/api/v1/user_catalogues/${id}`, data);
};
export const deleteUserCatalogue = async (id: number | string) => {
    return await axiosInstance.delete<ApiResponse<any>>(`/api/v1/user_catalogues/${id}`);
};
export const deleteManyUserCatalogues = async (ids: number[]) => {
    return await axiosInstance.delete<ApiResponse<any>>('/api/v1/user_catalogues', { data: { ids } });
};

// --- CLASSES ---
export const getClasses = async () => {
    return await axiosInstance.get<ApiResponse<Class[]>>('/api/v1/classes');
};
export const createClass = async (classData: CreateClassData) => {
    return await axiosInstance.post<ApiResponse<Class>>('/api/v1/classes', classData);
};
export const getClassDetails = async (id: string) => {
    return await axiosInstance.get<ApiResponse<ClassDetails>>(`/api/v1/classes/${id}`);
};
// ⭐ HÀM JOIN CLASS ĐÃ ĐƯỢC SỬA LỖI
export const joinClass = async (code: string) => {
    return await axiosInstance.post<ApiResponse<any>>('/api/v1/classes/join', { code });
};

// --- ASSIGNMENTS ---
export const getAssignments = async (classId: string) => {
    return await axiosInstance.get<ApiResponse<Assignment[]>>('/api/v1/assignments', { params: { classId } });
};
export const createAssignment = async (data: AssignmentData) => {
    return await axiosInstance.post<ApiResponse<any>>('/api/v1/assignments', data);
};
export const submitAssignment = async (id: number, data: SubmissionData) => {
    return await axiosInstance.post<ApiResponse<any>>(`/api/v1/assignments/${id}/submit`, data);
};
export const gradeAssignment = async (id: number, data: GradeData) => {
    return await axiosInstance.post<ApiResponse<any>>(`/api/v1/assignments/${id}/grade`, data);
};

// --- POSTS ---
export const getPosts = async (classId: string) => {
    return await axiosInstance.get<ApiResponse<Post[]>>(`/api/v1/classes/${classId}/posts`);
};
export const createPost = async (classId: string, data: CreatePostData) => {
    return await axiosInstance.post<ApiResponse<Post>>(`/api/v1/classes/${classId}/posts`, data);
};

// --- CHAT ---
export const getChatHistory = async (otherUserId: number) => {
    return await axiosInstance.get<ApiResponse<any[]>>(`/api/v1/chat/${otherUserId}`);
};
export const sendChatMessage = async (otherUserId: number, data: ChatMessageData) => {
    return await axiosInstance.post<ApiResponse<any>>(`/api/v1/chat/${otherUserId}`, data);
};

// --- NOTIFICATIONS ---
export const getNotifications = async () => {
    return await axiosInstance.get<ApiResponse<Notification[]>>('/api/v1/notifications');
};
export const createNotification = async (data: NotificationData) => {
    return await axiosInstance.post<ApiResponse<any>>('/api/v1/notifications', data);
};
export const markNotificationAsRead = async (id: number) => {
    return await axiosInstance.put<ApiResponse<any>>(`/api/v1/notifications/${id}`, { read: true });
};
export const getClassMembers = (classId: string) => {
    return axiosInstance.get<ApiResponse<User[]>>(`/api/v1/classes/${classId}/members`);
};
// Export mặc định để dùng ở nơi khác nếu cần
export default axiosInstance;