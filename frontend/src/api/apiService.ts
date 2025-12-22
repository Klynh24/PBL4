import axios, { AxiosError } from 'axios';
import * as T from '../types';

const axiosInstance = axios.create({
    // Use same-origin so Docker nginx proxy can forward /api/v1/* to backend
    // (avoids hardcoding localhost:8080 which breaks inside containers)
    baseURL: '',
    headers: { 'Content-Type': 'application/json' },
});

// --- Interceptor: Tự động đính kèm Token ---
axiosInstance.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token && !config.url?.includes('/auth/')) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
}, (error) => Promise.reject(error));

export const getErrorMessage = (error: unknown): string => {
    if (error instanceof AxiosError) {
        return error.response?.data?.error?.message || error.response?.data?.message || error.message || 'Lỗi hệ thống';
    }
    return String(error);
};

// --- [PHÂN HỆ AUTH & PASSWORD] ---
export const login = (c: T.LoginCredentials) => axiosInstance.post<T.ApiResponse<T.LoginResponseData>>('/api/v1/auth/login', c);
export const register = (d: T.RegisterData) => axiosInstance.post<T.ApiResponse<T.User>>('/api/v1/auth/register', d);
export const logout = () => axiosInstance.post('/api/v1/auth/logout');
export const requestPasswordReset = (d: T.PasswordResetRequestData) => axiosInstance.post<T.ApiResponse<any>>('/api/v1/auth/forgot-password', d);
export const resetPassword = (d: T.ResetPasswordData) => axiosInstance.post<T.ApiResponse<any>>('/api/v1/auth/reset-password', d);

// --- [PHÂN HỆ NGƯỜI DÙNG & QUẢN TRỊ] ---
export const getMe = () => axiosInstance.get<T.ApiResponse<T.User>>('/api/v1/users/me');
export const getUsers = () => axiosInstance.get<T.ApiResponse<any>>('/api/v1/users');
export const createUser = (d: T.RegisterData) => axiosInstance.post<T.ApiResponse<T.User>>('/api/v1/users', d);
export const updateUser = (id: number, d: T.UpdateUserData) => axiosInstance.put<T.ApiResponse<T.User>>(`/api/v1/users/${id}`, d);
export const deleteUser = (id: number) => axiosInstance.delete(`/api/v1/users/${id}`);
export const banUser = (id: number) => axiosInstance.put(`/api/v1/users/${id}/ban`);
export const unbanUser = (id: number) => axiosInstance.put(`/api/v1/users/${id}/unban`);

// --- [PHÂN HỆ ROOMS] ---
export const createRoom = (d: T.StoreRoomRequest) => axiosInstance.post<T.ApiResponse<T.RoomResource>>('/api/v1/rooms', d);
export const getRoomInfo = (id: number) => axiosInstance.get<T.ApiResponse<T.RoomResource>>(`/api/v1/rooms/${id}`);
export const joinRoom = (id: number) => axiosInstance.post(`/api/v1/rooms/${id}/join`);
export const leaveRoom = (id: number) => axiosInstance.post(`/api/v1/rooms/${id}/leave`);
export const getParticipants = (id: number) => axiosInstance.get<T.ApiResponse<T.User[]>>(`/api/v1/rooms/${id}/participants`);

// --- [PHÂN HỆ LỚP HỌC] ---
export const getClasses = () => axiosInstance.get<T.ApiResponse<any>>('/api/v1/classes');
export const createClass = (d: T.CreateClassData) => axiosInstance.post<T.ApiResponse<T.Class>>('/api/v1/classes', d);
export const getClassDetails = (id: string) => axiosInstance.get<T.ApiResponse<T.ClassDetails>>(`/api/v1/classes/${id}`);
export const joinClass = (code: string) => axiosInstance.post('/api/v1/classes/join', { code });
export const getClassMembers = (id: string) => axiosInstance.get<T.ApiResponse<T.User[]>>(`/api/v1/classes/${id}/members`);
export const addStudentToClass = (id: string, email: string) => axiosInstance.post(`/api/v1/classes/${id}/members`, { userEmails: [email] });

// --- [PHÂN HỆ BÀI TẬP & BÀI ĐĂNG] ---
export const getAssignments = (classId: string) => axiosInstance.get<T.ApiResponse<T.Assignment[]>>('/api/v1/assignments', { params: { classId } });
export const createAssignment = (d: T.AssignmentData) => axiosInstance.post('/api/v1/assignments', d);
export const submitAssignment = (d: T.SubmissionData) => axiosInstance.post('/api/v1/submission', d);
export const gradeAssignment = (id: number, d: T.GradeData) => axiosInstance.post(`/api/v1/assignments/${id}/grade`, d);
export const getPosts = (classId: string) => axiosInstance.get<T.ApiResponse<T.Post[]>>(`/api/v1/classes/${classId}/posts`);
export const createPost = (classId: string, d: T.CreatePostData) => axiosInstance.post(`/api/v1/classes/${classId}/posts`, d);

// --- [PHÂN HỆ THÔNG BÁO & CHAT] ---
export const getNotifications = () => axiosInstance.get<T.ApiResponse<T.Notification[]>>('/api/v1/notifications');
export const markNotificationAsRead = (id: number) => axiosInstance.put(`/api/v1/notifications/${id}`, { read: true });
export const getChatHistory = (uId: number) => axiosInstance.get(`/api/v1/chat/${uId}`);
export const sendChatMessage = (uId: number, data: T.ChatMessageData) => axiosInstance.post(`/api/v1/chat/${uId}`, data);

// --- [HỆ THỐNG] ---
export const uploadFile = (file: File) => {
    const fd = new FormData();
    fd.append('file', file);
    return axiosInstance.post<T.ApiResponse<string>>('/api/v1/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
};

export const getActiveRoom = (classId: string | number) => {
    // Sửa api.get thành axiosInstance.get và thêm /api/v1
    return axiosInstance.get<T.ApiResponse<{id: number}>>(`/api/v1/rooms/active/${classId}`);
};

export default axiosInstance;