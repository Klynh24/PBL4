import React, { createContext, useState, useEffect, useContext, ReactNode } from 'react';
import * as api from '../api/apiService';

// Định nghĩa kiểu dữ liệu cho user, được dùng chung trong toàn bộ ứng dụng
export interface User {
  id: number;
  email: string;
  role: 'student' | 'teacher' | 'admin';
  name: string;
  dob: string;
  gender: 'Nam' | 'Nữ' | 'Khác';
}

// Định nghĩa kiểu dữ liệu cho giá trị mà Context sẽ cung cấp
interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (credentials: any) => Promise<void>;
  logout: () => void;
  updateUserContext: (updatedData: Partial<User>) => void;
}

// Tạo Context
const AuthContext = createContext<AuthContextType | undefined>(undefined);

// Định nghĩa kiểu cho props của Provider
interface AuthProviderProps {
  children: ReactNode;
}

// Hàm giải mã token an toàn với ký tự Unicode (Tiếng Việt)
const decodeToken = (token: string) => {
    try {
        const payloadBase64 = token.split('.')[1];
        // Giải mã an toàn
        const decodedPayload = decodeURIComponent(atob(payloadBase64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        return JSON.parse(decodedPayload);
    } catch (e) {
        console.error("Failed to decode token:", e);
        return null;
    }
}

// Component Provider: Chứa logic và cung cấp dữ liệu cho các component con
export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  // Tự động kiểm tra token khi tải lại trang
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
        const payload = decodeToken(token);
        if (payload) {
            setUser({ 
                id: payload.id, 
                email: payload.sub, 
                role: payload.role,
                name: payload.name,
                dob: payload.dob,
                gender: payload.gender
            });
        } else {
            // Nếu token không hợp lệ, xóa nó đi
            localStorage.removeItem('token');
        }
    }
    setLoading(false);
  }, []);

  // Hàm xử lý đăng nhập
  const handleLogin = async (credentials: any) => {
    const { data } = await api.login(credentials);
    localStorage.setItem('token', data.token);
    const payload = decodeToken(data.token);
    if(payload) {
        setUser({ 
            id: payload.id, 
            email: payload.sub, 
            role: payload.role,
            name: payload.name,
            dob: payload.dob,
            gender: payload.gender
        });
    }
  };

  // Hàm xử lý đăng xuất
  const handleLogout = () => {
    localStorage.removeItem('token');
    setUser(null);
  };
  
  // Hàm cập nhật thông tin user trong context (ví dụ: sau khi sửa profile)
  const updateUserContext = (updatedData: Partial<User>) => {
      if(user) {
          setUser({ ...user, ...updatedData });
      }
  };

  const value = { user, login: handleLogin, logout: handleLogout, isAuthenticated: !!user, loading, updateUserContext };

  return (
    <AuthContext.Provider value={value}>
      {!loading && children}
    </AuthContext.Provider>
  );
};

// Custom hook để sử dụng AuthContext một cách tiện lợi
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};