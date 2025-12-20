import React, { createContext, useState, useEffect, useContext, ReactNode } from 'react';
import * as api from '../api/apiService';
import { User, LoginCredentials } from '../types';

// ----------------- Helpers -----------------
const removeDiacritics = (s?: string) =>
  (s || '')
    .normalize('NFD')
    // dải dấu tiếng Việt
    .replace(/[\u0300-\u036f]/g, '')
    .toUpperCase()
    .trim();

const getPrimaryRole = (roles?: string[]): 'admin' | 'teacher' | 'student' => {
  if (!roles || roles.length === 0) return 'student';
  const normalized = roles.map(removeDiacritics);

if (normalized.some(r => r === 'ROLE_ADMIN' || r === 'ADMIN' || r === 'QUAN TRI VIEN')) return 'admin';  if (normalized.some(r => r === 'ROLE_TEACHER' || r === 'TEACHER' || r === 'GIAO VIEN')) return 'teacher';
  return 'student';
};

// Ưu tiên BE trả "roles: string[]"; fallback nếu một API khác vẫn trả "userCatalogues: {name}[]"
const normalizeUserFromMeResponse = (meData: any): User | null => {
  if (!meData) return null;

  const roles: string[] = Array.isArray(meData.roles)
    ? meData.roles
    : Array.isArray(meData.userCatalogues)
      ? meData.userCatalogues.map((c: any) => c?.name).filter(Boolean)
      : [];

  const user: User = {
    id: meData.id,
    email: meData.email,
    name: meData.name,
    phone: meData.phone ?? null,
    image: meData.image ?? null,
    address: meData.address ?? null,
    roles,
    role: getPrimaryRole(roles),
  };

  return user;
};

// ----------------- Context types -----------------
interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (credentials: LoginCredentials) => Promise<boolean>;
  logout: () => void;
  updateUserContext: (updatedData: Partial<User>) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

// ----------------- Provider -----------------
export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  // Kiểm tra token khi load trang, nếu có thì gọi /me
  const verifyTokenOnLoad = async () => {
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const response = await api.getMe(); // apiService interceptor sẽ tự gắn token từ localStorage
        const normalizedUser = normalizeUserFromMeResponse(response.data.data);
        if (normalizedUser) setUser(normalizedUser);
      } catch (error) {
        console.error('Failed to verify token on load:', api.getErrorMessage(error));
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
      }
    }
    setLoading(false);
  };

  useEffect(() => {
    verifyTokenOnLoad();
  }, []);

  const handleLogin = async (credentials: LoginCredentials): Promise<boolean> => {
    try {
      const loginResponse = await api.login(credentials);
      if (!loginResponse.data || !loginResponse.data.data) {
        console.error('Login response format invalid:', loginResponse);
        return false;
      }

      const { token, refreshToken } = loginResponse.data.data;
      // Lưu token trước, interceptor sẽ đọc từ localStorage cho request kế tiếp
      localStorage.setItem('token', token);
      localStorage.setItem('refreshToken', refreshToken);

      // Gọi /me để lấy thông tin người dùng + roles
      const meResponse = await api.getMe();
      const normalizedUser = normalizeUserFromMeResponse(meResponse.data.data);

      if (normalizedUser) {
        setUser(normalizedUser);
        return true;
      } else {
        await handleLogout();
        return false;
      }
    } catch (error) {
      console.error('Login failed:', api.getErrorMessage(error));
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      return false;
    }
  };

  const handleLogout = async () => {
    try {
      await api.logout();
    } catch (error) {
      console.error('Logout API failed:', api.getErrorMessage(error));
    } finally {
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      setUser(null);
    }
  };

  const updateUserContext = (updatedData: Partial<User>) => {
    setUser(prev => (prev ? { ...prev, ...updatedData } : prev));
  };

  const value: AuthContextType = {
    user,
    isAuthenticated: !!user,
    loading,
    login: handleLogin,
    logout: handleLogout,
    updateUserContext,
  };

  return (
    <AuthContext.Provider value={value}>
      {!loading && children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};