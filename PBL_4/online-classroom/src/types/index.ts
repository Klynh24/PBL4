
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

export interface User {
  id: number;
  email: string;
  role: 'student' | 'teacher' | 'admin';
  name: string;
  phone?: string;
  image?: string;
  address?: string;
  dob?: string; 
  gender?: 'Nam' | 'Nữ' | 'Khác'; 
  userCatalogues: UserCatalogue[];
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
  teacher: string; 
  teacherId: number; 
  students: User[];
  user: User; 
}


export interface LoginCredentials {
    email: string;
    password: string;
}

export interface LoginResponseData {
    token: string;
    refreshToken: string;
    user: any; 
}

export interface RegisterData {
    email: string;
    password: string;
    userCatalogueId: number; 
    name: string;
    dob: string;
    gender: string;
    address?: string; 
    phone?: string; 
}

export type UpdateUserData = Partial<Pick<User, 'name' | 'dob' | 'gender' | 'address' | 'phone' | 'image'>>;

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
    fileUrl: string;
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
    targetRole?: 'student' | 'teacher';
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