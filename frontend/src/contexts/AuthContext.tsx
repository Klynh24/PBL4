import React, { createContext, useState, useEffect, useContext, ReactNode } from 'react';
import * as api from '../api/apiService'; 
import { User, UserCatalogue, LoginCredentials } from '../types'; 
import axios from 'axios'; // ⭐ CẦN IMPORT AXIOS

// --- Kiểu dữ liệu cho Context ---
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

// ⭐ HÀM MỚI: Thiết lập Header Authorization mặc định cho Axios
const setAuthHeaders = (token: string | null) => {
    if (token) {
        axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    } else {
        delete axios.defaults.headers.common['Authorization'];
    }
};

// --- (Giữ nguyên các hàm helper) ---

const getRoleFromCatalogues = (catalogues: UserCatalogue[]): 'admin' | 'teacher' | 'student' => {
  if (!catalogues || catalogues.length === 0) {
    return 'student';
  }
  const roles = catalogues.map(c => c.name.toUpperCase());
  if (roles.includes("ROLE_ADMIN")) return 'admin';
  if (roles.includes("ROLE_TEACHER")) return 'teacher';
  return 'student';
};

const normalizeUserFromMeResponse = (meData: any): User | null => {
  if (!meData) return null;

  const role = getRoleFromCatalogues(meData.userCatalogues);
  return {
    id: meData.id,
    email: meData.email,
    name: meData.name,
    phone: meData.phone,
    image: meData.image,
    address: meData.address,

    userCatalogues: meData.userCatalogues,
    role: role,
  };
};


export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const verifyTokenOnLoad = async () => {
    // ⭐ FIX: Thiết lập header khi tải trang để request /me thành công
    const token = localStorage.getItem('token');

    if (token) {
      setAuthHeaders(token);
      try {
        const response = await api.getMe();
        const normalizedUser = normalizeUserFromMeResponse(response.data.data);
        if (normalizedUser) {
          setUser(normalizedUser);
        }
      } catch (error) {
        console.error('Failed to verify token on load:', api.getErrorMessage(error));
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        setAuthHeaders(null);
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

      // ⭐ FIX 1: Lưu token vào Local Storage
      localStorage.setItem('token', token);
      localStorage.setItem('refreshToken', refreshToken);

      // ⭐ FIX 2: Thiết lập Header Authorization ngay lập tức cho request /me
      setAuthHeaders(token);

      // Request /me sẽ hoạt động vì đã có header
      const meResponse = await api.getMe();
      const normalizedUser = normalizeUserFromMeResponse(meResponse.data.data);

      if (normalizedUser) {
        setUser(normalizedUser);
        return true;
      } else {
        // Nếu /me thất bại, xóa token
        handleLogout();
        return false;
      }

    } catch (error) {
      console.error('Login failed:', api.getErrorMessage(error));
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      setAuthHeaders(null);
      return false;
    }
  };

  const handleLogout = async () => {
    try {
      await api.logout();
    } catch (error) {
      console.error('Logout API failed:', api.getErrorMessage(error));
    } finally {
      // ⭐ FIX 3: Xóa header khi đăng xuất
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      setAuthHeaders(null);
      setUser(null);
    }
  };

  const updateUserContext = (updatedData: Partial<User>) => {
    if (user) {
      setUser({ ...user, ...updatedData });
    }
  };

  const value: AuthContextType = {
    user,
    login: handleLogin,
    logout: handleLogout,
    isAuthenticated: !!user,
    loading,
    updateUserContext
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