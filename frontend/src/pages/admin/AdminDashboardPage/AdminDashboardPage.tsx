import React, { useState, useEffect } from 'react';
import * as api from '../../../api/apiService';
import styles from './AdminDashboardPage.module.css';
import Modal from '../../../components/common/Modal/Modal';
import CreateUserModal from '../../../components/common/CreateUserModal/CreateUserModal';
import { User, Class } from '../../../types';

// --- HELPERS: Chuẩn hoá vai trò để hiển thị tiếng Việt chính xác ---
const removeDiacritics = (s?: string) =>
  (s || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase().trim();

const getPrimaryRole = (u: any): 'admin' | 'teacher' | 'student' => {
  if (u.email === 'admin@gmail.com' || u.email === 'admin1@gmail.com') return 'admin';
  
  const rolesList = Array.isArray(u.roles) ? u.roles : [];
  const normalized = rolesList.map((r: string) => removeDiacritics(r));
  
  if (normalized.some((n: string) => n.includes('ADMIN') || n.includes('QUAN TRI VIEN'))) return 'admin';
  if (normalized.some((n: string) => n.includes('TEACHER') || n.includes('GIAO VIEN'))) return 'teacher';
  
  return 'student';
};

const AdminDashboardPage: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [classes, setClasses] = useState<Class[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [view, setView] = useState<'users' | 'classes'>('users');

  // States cho Modal
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false);
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [itemToDelete, setItemToDelete] = useState<User | Class | null>(null);

  const fetchData = async () => {
    try {
      setLoading(true);
      setError('');
      const [uRes, cRes] = await Promise.all([api.getUsers(), api.getClasses()]);
      
      // 1. Xử lý dữ liệu Người dùng (Bóc tách content từ Spring Page)
      const uRaw: any = uRes.data.data;
      const usersArray = uRaw?.content || (Array.isArray(uRaw) ? uRaw : []);
      setUsers(usersArray.map((u: any) => ({ ...u, role: getPrimaryRole(u) })));

      // 2. Xử lý dữ liệu Lớp học (Bóc tách content và gán mảng)
      const cRaw: any = cRes.data.data;
      const classesArray = cRaw?.content || (Array.isArray(cRaw) ? cRaw : []);
      setClasses(classesArray); 

    } catch (err) {
      setError(api.getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const handleEditClick = (u: User) => {
    setSelectedUser(u);
    setIsFormModalOpen(true);
  };

  const handleFormConfirm = async (data: any) => {
    try {
      if (selectedUser) {
        await api.updateUser(selectedUser.id, data);
      } else {
        await api.createUser(data);
      }
      setIsFormModalOpen(false);
      fetchData();
    } catch (e) { 
      alert(api.getErrorMessage(e)); 
    }
  };

  const handleConfirmDelete = async () => {
    if (!itemToDelete) return;
    try {
      // Kiểm tra nếu đối tượng có email thì là User, ngược lại là Class
      if ('email' in itemToDelete) {
        await api.deleteUser(itemToDelete.id);
      } else {
        // Nếu bạn có API xóa lớp học, hãy gọi ở đây
        // await api.deleteClass(itemToDelete.id);
      }
      setDeleteModalOpen(false);
      fetchData();
    } catch (e) { 
      alert(api.getErrorMessage(e)); 
    }
  };

  if (loading) return <div className={styles.message}>Đang tải dữ liệu...</div>;
  if (error) return <div className={`${styles.message} ${styles.error}`}>{error}</div>;

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1 className={styles.title}>Quản trị hệ thống</h1>
        <button 
          onClick={() => { setSelectedUser(null); setIsFormModalOpen(true); }} 
          className={styles.actionButton}
        >
          + Tạo mới
        </button>
      </div>

      <div className={styles.tabs}>
        <button 
          onClick={() => setView('users')} 
          className={`${styles.tabButton} ${view === 'users' ? styles.active : ''}`}
        >
          Người dùng
        </button>
        <button 
          onClick={() => setView('classes')} 
          className={`${styles.tabButton} ${view === 'classes' ? styles.active : ''}`}
        >
          Lớp học
        </button>
      </div>

      <div className={styles.content}>
        {/* --- TAB NGƯỜI DÙNG --- */}
        {view === 'users' && (
          <table className={styles.table}>
            <thead>
              <tr><th>Email</th><th>Vai trò</th><th>Hành động</th></tr>
            </thead>
            <tbody>
              {users.length > 0 ? users.map(u => (
                <tr key={u.id}>
                  <td>{u.email}</td>
                  <td style={{ color: u.role === 'admin' ? '#4f46e5' : 'inherit', fontWeight: 600 }}>
                    {u.role === 'admin' ? 'Quản trị viên' : (u.role === 'teacher' ? 'Giáo viên' : 'Học sinh')}
                  </td>
                  <td>
                    <button onClick={() => handleEditClick(u)} className={styles.editButton}>Sửa</button>
                    <button onClick={() => { setItemToDelete(u); setDeleteModalOpen(true); }} className={styles.deleteButton}>Xóa</button>
                  </td>
                </tr>
              )) : (
                <tr><td colSpan={3} className={styles.emptyMessage}>Không có người dùng nào.</td></tr>
              )}
            </tbody>
          </table>
        )}

        {/* --- TAB LỚP HỌC --- */}
        {view === 'classes' && (
          <table className={styles.table}>
            <thead>
              <tr><th>Tên lớp</th><th>Giáo viên</th><th>Hành động</th></tr>
            </thead>
            <tbody>
              {classes.length > 0 ? classes.map(cls => (
                <tr key={cls.id}>
                  <td>{cls.name}</td>
                  {/* Dữ liệu giáo viên nằm trong đối tượng user */}
                  <td>{(cls as any).user?.name || 'Chưa phân công'}</td>
                  <td>
                    <button className={styles.editButton}>Sửa</button>
                    <button onClick={() => { setItemToDelete(cls); setDeleteModalOpen(true); }} className={styles.deleteButton}>Xóa</button>
                  </td>
                </tr>
              )) : (
                <tr><td colSpan={3} className={styles.emptyMessage}>Chưa có lớp học nào được tạo.</td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>

      <CreateUserModal 
        isOpen={isFormModalOpen} 
        onClose={() => setIsFormModalOpen(false)} 
        onConfirm={handleFormConfirm}
        initialData={selectedUser}
        title={selectedUser ? "Cập nhật thành viên" : "Tạo thành viên mới"}
      />

      <Modal 
        isOpen={isDeleteModalOpen} 
        onClose={() => setDeleteModalOpen(false)} 
        onConfirm={handleConfirmDelete} 
        title="Xác nhận xóa"
      >
        <p>Hành động này không thể hoàn tác. Bạn có chắc chắn muốn xóa?</p>
      </Modal>
    </div>
  );
};

export default AdminDashboardPage;