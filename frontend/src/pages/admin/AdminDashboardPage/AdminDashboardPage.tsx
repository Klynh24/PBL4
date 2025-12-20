import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../contexts/AuthContext';
import * as api from '../../../api/apiService';
import styles from './AdminDashboardPage.module.css';
import Modal from '../../../components/common/Modal/Modal';
import CreateUserModal from '../../../components/common/CreateUserModal/CreateUserModal';
import { User, Class, RegisterData } from '../../../types';

// --- HELPERS: XỬ LÝ DỮ LIỆU NGOÀI COMPONENT ---
const removeDiacritics = (s?: string) =>
  (s || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase().trim();

/**
 * Xác định vai trò dựa trên ID hoặc Tên từ bảng trung gian
 */
const getPrimaryRole = (rolesData: any[]): 'admin' | 'teacher' | 'student' => {
  if (!rolesData || rolesData.length === 0) return 'student';

  // 1. Kiểm tra ưu tiên theo ID từ bảng trung gian (ID 1 là Admin)
  const hasAdmin = rolesData.some(r => r.id === 1 || r.user_catalogue_id === 1);
  if (hasAdmin) return 'admin';

  // 2. Kiểm tra theo ID 2 (Giáo viên)
  const hasTeacher = rolesData.some(r => r.id === 2 || r.user_catalogue_id === 2);
  if (hasTeacher) return 'teacher';

  // 3. Dự phòng kiểm tra theo tên nếu ID bị sai lệch
  const names = rolesData.map(r => removeDiacritics(r.name || r.catalogue_name || ''));
  if (names.some(n => n.includes('ADMIN') || n.includes('QUAN TRI VIEN'))) return 'admin';
  if (names.some(n => n.includes('TEACHER') || n.includes('GIAO VIEN'))) return 'teacher';

  return 'student';
};

const primaryRoleFromUser = (u: any): 'admin' | 'teacher' | 'student' => {
  // Trích xuất mảng từ các trường có thể có trong quan hệ Nhiều-Nhiều
  const rawRoles = u.userCatalogues || u.user_catalogues || u.roles || [];
  return getPrimaryRole(Array.isArray(rawRoles) ? rawRoles : []);
};

const AdminDashboardPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [users, setUsers] = useState<User[]>([]);
  const [classes, setClasses] = useState<Class[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [view, setView] = useState<'users' | 'classes' | 'notifications'>('users');

  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false);
  const [itemToDelete, setItemToDelete] = useState<User | Class | null>(null);
  const [isCreateUserModalOpen, setCreateUserModalOpen] = useState(false);
  const [notificationMessage, setNotificationMessage] = useState('');

  const fetchData = async () => {
    try {
      setLoading(true);
      setError('');
      const [uRes, cRes] = await Promise.all([api.getUsers(), api.getClasses()]);

      /**
       * SỬA LỖI .MAP(): Trích xuất trường content từ PageImpl
       * Dùng ép kiểu 'as any' để TypeScript không báo lỗi thuộc tính không tồn tại
       */
      const uRaw: any = uRes.data.data;
      const usersArray = uRaw?.content || (Array.isArray(uRaw) ? uRaw : []);
      
      const cRaw: any = cRes.data.data;
      const classesArray = cRaw?.content || (Array.isArray(cRaw) ? cRaw : []);

      // Ánh xạ dữ liệu và tính toán vai trò thực tế
      const fetchedUsers: User[] = usersArray.map((u: any) => ({
        ...u,
        isBanned: u.isBanned ?? false,
        role: primaryRoleFromUser(u),
      }));

      setUsers(fetchedUsers);
      setClasses(classesArray);
    } catch (err) {
      setError(`Không thể tải dữ liệu: ${api.getErrorMessage(err)}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleToggleBan = async (userToToggle: User) => {
    try {
      const action = userToToggle.isBanned ? api.unbanUser : api.banUser;
      await action(userToToggle.id);
      setUsers(prev => prev.map(u => (u.id === userToToggle.id ? { ...u, isBanned: !u.isBanned } : u)));
    } catch (err) {
      alert(`Lỗi: ${api.getErrorMessage(err)}`);
    }
  };

  const handleConfirmDelete = async () => {
    if (!itemToDelete) return;
    try {
      if ('email' in itemToDelete) {
        await api.deleteUser(itemToDelete.id);
        setUsers(prev => prev.filter(u => u.id !== itemToDelete.id));
      } else {
        alert('API xóa lớp học đang được cập nhật.');
      }
    } catch (err) {
      alert(`Lỗi: ${api.getErrorMessage(err)}`);
    } finally {
      setDeleteModalOpen(false);
      setItemToDelete(null);
    }
  };

  const handleSendNotification = async () => {
    if (!notificationMessage.trim()) return alert('Vui lòng nhập nội dung!');
    try {
      await api.createNotification({ message: notificationMessage });
      alert('Gửi thông báo thành công!');
      setNotificationMessage('');
    } catch (err) {
      alert(`Lỗi: ${api.getErrorMessage(err)}`);
    }
  };

  if (loading) return <div className={styles.message}>Đang tải dữ liệu quản trị...</div>;
  if (error) return <div className={`${styles.message} ${styles.error}`}>{error}</div>;

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1 className={styles.title}>Quản trị hệ thống</h1>
        {view === 'users' && (
          <button onClick={() => setCreateUserModalOpen(true)} className={styles.actionButton}>
            + Tạo người dùng mới
          </button>
        )}
      </div>

      <div className={styles.tabs}>
        <button onClick={() => setView('users')} className={`${styles.tabButton} ${view === 'users' ? styles.active : ''}`}>Người dùng</button>
        <button onClick={() => setView('classes')} className={`${styles.tabButton} ${view === 'classes' ? styles.active : ''}`}>Lớp học</button>
        <button onClick={() => setView('notifications')} className={`${styles.tabButton} ${view === 'notifications' ? styles.active : ''}`}>Thông báo</button>
      </div>

      <div className={styles.content}>
        {view === 'users' && (
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Email</th>
                <th>Vai trò</th>
                <th>Trạng thái</th>
                <th>Hành động</th>
              </tr>
            </thead>
            <tbody>
              {users.map(u => (
                <tr key={u.id}>
                  <td>{u.email}</td>
                  <td style={{ fontWeight: u.role === 'admin' ? 'bold' : 'normal', color: u.role === 'admin' ? '#4f46e5' : 'inherit' }}>
                    {u.role === 'admin' ? 'Quản trị viên' : (u.role === 'teacher' ? 'Giáo viên' : 'Học sinh')}
                  </td>
                  <td>{u.isBanned ? 'Bị khóa' : 'Hoạt động'}</td>
                  <td>
                    <button onClick={() => handleToggleBan(u)} className={styles.editButton}>
                      {u.isBanned ? 'Mở khóa' : 'Khóa'}
                    </button>
                    <button onClick={() => { setItemToDelete(u); setDeleteModalOpen(true); }} className={styles.deleteButton}>Xóa</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {view === 'classes' && (
          <table className={styles.table}>
            <thead>
              <tr><th>Tên lớp</th><th>Giáo viên</th><th>Hành động</th></tr>
            </thead>
            <tbody>
              {classes.map(cls => (
                <tr key={cls.id}>
                  <td>{cls.name}</td>
                  <td>{cls.teacher}</td>
                  <td>
                    <button className={styles.editButton}>Sửa</button>
                    <button onClick={() => { setItemToDelete(cls); setDeleteModalOpen(true); }} className={styles.deleteButton}>Xóa</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {view === 'notifications' && (
          <div className={styles.notificationForm}>
            <h2>Gửi thông báo toàn hệ thống</h2>
            <textarea value={notificationMessage} onChange={e => setNotificationMessage(e.target.value)} placeholder="Nhập nội dung thông báo..." rows={5} />
            <button onClick={handleSendNotification} className={styles.actionButton}>Gửi đi</button>
          </div>
        )}
      </div>

      <CreateUserModal 
        isOpen={isCreateUserModalOpen} 
        onClose={() => setCreateUserModalOpen(false)} 
        onConfirm={(data: any) => { api.createUser(data).then(fetchData); setCreateUserModalOpen(false); }} 
      />
      
      <Modal 
        isOpen={isDeleteModalOpen} 
        onClose={() => setDeleteModalOpen(false)} 
        onConfirm={handleConfirmDelete} 
        title="Xác nhận xóa"
      >
        <p>Hành động này không thể hoàn tác. Bạn có chắc chắn muốn xóa mục này?</p>
      </Modal>
    </div>
  );
};

export default AdminDashboardPage;