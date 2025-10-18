import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext'; 

import Header from './components/layout/Header/Header';
import Footer from './components/layout/Footer/Footer';
import HomePage from './pages/HomePage/HomePage';
import LoginPage from './pages/LoginPage/LoginPage';
import RegisterPage from './pages/RegisterPage/RegisterPage';
import ClassesPage from './pages/ClassesPage/ClassesPage';
import ClassDetailsPage from './pages/ClassDetailsPage/ClassDetailsPage';
import ProfilePage from './pages/ProfilePage/ProfilePage';
import NotificationsPage from './pages/NotificationsPage/NotificationsPage';
import AdminDashboardPage from './pages/admin/AdminDashboardPage/AdminDashboardPage';
import ChatPage from './pages/ChatPage/ChatPage';
import MeetingPage from './pages/MeetingPage/MeetingPage';

// Import các component con của ClassDetailsPage
import PostsTab from './pages/ClassDetailsPage/PostsTab';
import FilesTab from './pages/ClassDetailsPage/FilesTab';
import AssignmentsTab from './pages/ClassDetailsPage/AssignmentsTab';
import MembersTab from './pages/ClassDetailsPage/MembersTab';

const MainLayout: React.FC = () => ( <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}><Header /><main style={{ padding: '32px', flexGrow: 1, backgroundColor: 'var(--background-main)' }}><Outlet /></main></div> );
const PublicLayout: React.FC = () => ( <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}><Outlet /></div> );
const MeetingLayout: React.FC = () => (<Outlet />);
interface ProtectedRouteProps { allowedRoles?: string[]; }
const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ allowedRoles }) => {
  const { isAuthenticated, user, loading } = useAuth();
  if (loading) return <div>Đang xác thực...</div>;
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (allowedRoles && user && !allowedRoles.includes(user.role)) { return <Navigate to="/classes" replace />; }
  return <Outlet />;
};


const AppRoutes: React.FC = () => {
  const { isAuthenticated } = useAuth();

  return (
    <Routes>
      <Route element={<PublicLayout />}><Route path="/" element={<HomePage />} /><Route path="/login" element={<LoginPage />} /><Route path="/register" element={<RegisterPage />} /></Route>

      <Route element={<ProtectedRoute />}>
          <Route element={<MeetingLayout />}><Route path="/classes/:classId/meet" element={<MeetingPage />} /></Route>
          
          <Route element={<MainLayout />}>
            <Route path="/classes" element={<ClassesPage />} />
            
            <Route path="/classes/:classId" element={<ClassDetailsPage />}>
                {/* Route mặc định khi vào /classes/:classId sẽ là tab Bài đăng */}
                <Route index element={<Navigate to="posts" replace />} /> 
                <Route path="posts" element={<PostsTab />} />
                <Route path="files" element={<FilesTab />} />
                <Route path="assignments" element={<AssignmentsTab />} />
                <Route path="members" element={<MembersTab />} />
            </Route>

            <Route path="/profile" element={<ProfilePage />} />
            <Route path="/notifications" element={<NotificationsPage />} />
            <Route path="/chat" element={<ChatPage />} />
            <Route path="/admin/dashboard" element={<AdminRoute><AdminDashboardPage /></AdminRoute>} />
          </Route>
      </Route>

      <Route path="*" element={<Navigate to={isAuthenticated ? "/classes" : "/"} replace />} />
    </Routes>
  );
};

const AdminRoute: React.FC<{ children: React.ReactElement }> = ({ children }) => {
  const { user } = useAuth();
  if (user?.role !== 'admin') { return <Navigate to="/classes" replace />; }
  return children;
};
const App: React.FC = () => {
  return ( <Router><AuthProvider><AppRoutes /></AuthProvider></Router> );
}
export default App;