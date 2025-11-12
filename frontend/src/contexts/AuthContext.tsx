import React, { createContext, useState, useEffect, useContext, ReactNode } from 'react';
import * as api from '../api/apiService'; 
import { User, UserCatalogue, LoginCredentials } from '../types'; 

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
    const token = localStorage.getItem('token');
    if (token) {
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

      localStorage.setItem('token', token);
      localStorage.setItem('refreshToken', refreshToken);

      const meResponse = await api.getMe();
      const normalizedUser = normalizeUserFromMeResponse(meResponse.data.data);

      if (normalizedUser) {
        setUser(normalizedUser);
        return true;
      } else {
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