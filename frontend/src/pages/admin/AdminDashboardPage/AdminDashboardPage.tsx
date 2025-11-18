import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../contexts/AuthContext';
import * as api from '../../../api/apiService';
import styles from './AdminDashboardPage.module.css';
import Modal from '../../../components/common/Modal/Modal';
import CreateUserModal from '../../../components/common/CreateUserModal/CreateUserModal';
import { User, Class, RegisterData } from '../../../types';

// Helpers: chuẩn hoá và xác định vai trò chính
const removeDiacritics = (s?: string) =>
  (s || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toUpperCase()
    .trim();

const getPrimaryRole = (roles?: string[]): 'admin' | 'teacher' | 'student' => {
  if (!roles || roles.length === 0) return 'student';
  const normalized = roles.map(removeDiacritics);
  if (normalized.some(r => r === 'ROLE_ADMIN' || r === 'ADMIN')) return 'admin';
  if (normalized.some(r => r === 'ROLE_TEACHER' || r === 'TEACHER' || r === 'GIAO VIEN')) return 'teacher';
  return 'student';
};

// Từ một user bất kỳ, suy ra role chính (ưu tiên field role; fallback roles; cuối cùng fallback userCatalogues từ API cũ nếu có)
const primaryRoleFromUser = (u: any): 'admin' | 'teacher' | 'student' => {
  if (u?.role) return u.role;
  const rolesFromArray: string[] | undefined = Array.isArray(u?.roles)
    ? u.roles
    : Array.isArray(u?.userCatalogues) // fallback nếu API cũ trả { id, name }
      ? u.userCatalogues.map((c: any) => c?.name).filter(Boolean)
      : [];
  return getPrimaryRole(rolesFromArray);
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

      const [usersResponse, classesResponse] = await Promise.all([
        api.getUsers(),
        api.getClasses(),
      ]);

      // Chuẩn hoá danh sách người dùng: tính role chính, đảm bảo isBanned có giá trị
      const fetchedUsers: User[] = (usersResponse.data.data as any[]).map((u: any) => ({
        ...u,
        isBanned: u.isBanned ?? false,
        role: primaryRoleFromUser(u),
      }));

      setUsers(fetchedUsers);
      setClasses(classesResponse.data.data);
    } catch (err) {
      const errorMsg = api.getErrorMessage(err);
      setError(`Không thể tải dữ liệu quản trị. Lỗi: ${errorMsg}`);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleToggleBan = async (userToToggle: User) => {
    try {
      const action = userToToggle.isBanned ? api.unbanUser : api.banUser;
      await action(userToToggle.id);

      setUsers(prev =>
        prev.map(u => (u.id === userToToggle.id ? { ...u, isBanned: !u.isBanned } : u)),
      );
      alert(`Người dùng ${userToToggle.email} đã được ${userToToggle.isBanned ? 'bỏ cấm' : 'cấm'}.`);
    } catch (err) {
      alert(`Lỗi: Không thể thực hiện thao tác. ${api.getErrorMessage(err)}`);
    }
  };

  const handleOpenDeleteModal = (item: User | Class) => {
    setItemToDelete(item);
    setDeleteModalOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (!itemToDelete) return;
    try {
      if ('email' in itemToDelete) {
        await api.deleteUser(itemToDelete.id);
        setUsers(prev => prev.filter(u => u.id !== itemToDelete.id));
      } else {
        // TODO: thêm api.deleteClass nếu backend hỗ trợ
        alert('API Xóa lớp học chưa được cài đặt (TODO).');
        setClasses(prev => prev.filter(cls => cls.id !== itemToDelete.id));
      }
    } catch (err) {
      alert(`Lỗi: Không thể thực hiện thao tác xóa. ${api.getErrorMessage(err)}`);
    } finally {
      setDeleteModalOpen(false);
      setItemToDelete(null);
    }
  };

  const handleCreateUser = async (userData: RegisterData) => {
    try {
      const response = await api.createUser(userData);
      const created = response.data.data as any;
      const normalized: User = {
        ...created,
        isBanned: created.isBanned ?? false,
        role: primaryRoleFromUser(created),
      };
      setUsers(prev => [...prev, normalized]);
      setCreateUserModalOpen(false);
      alert('Tạo người dùng thành công!');
    } catch (error) {
      alert(`Tạo người dùng thất bại: ${api.getErrorMessage(error)}`);
    }
  };

  const handleSendNotification = async () => {
    if (!notificationMessage.trim()) {
      alert('Vui lòng nhập nội dung thông báo.');
      return;
    }
    try {
      await api.createNotification({ message: notificationMessage });
      alert('Gửi thông báo thành công!');
      setNotificationMessage('');
    } catch (error) {
      alert(`Gửi thông báo thất bại. ${api.getErrorMessage(error)}`);
    }
  };

  if (loading) return <div className={styles.message}>Đang tải dữ liệu...</div>;
  if (error) return <div className={`${styles.message} ${styles.error}`}>{error}</div>;

  return (
    <>
      <div className={styles.container}>
        <div className={styles.header}>
          <h1 className={styles.title}>Bảng điều khiển Quản trị</h1>
          {view === 'users' && (
            <button onClick={() => setCreateUserModalOpen(true)} className={styles.actionButton}>
              Tạo người dùng mới
            </button>
          )}
        </div>

        <div className={styles.tabs}>
          <button
            onClick={() => setView('users')}
            className={`${styles.tabButton} ${view === 'users' ? styles.active : ''}`}
          >
            Quản lý Người dùng
          </button>
          <button
            onClick={() => setView('classes')}
            className={`${styles.tabButton} ${view === 'classes' ? styles.active : ''}`}
          >
            Quản lý Lớp học
          </button>
          <button
            onClick={() => setView('notifications')}
            className={`${styles.tabButton} ${view === 'notifications' ? styles.active : ''}`}
          >
            Gửi thông báo
          </button>
        </div>

        <div className={styles.content}>
          {view === 'users' && (
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Email</th>
                  <th>Vai trò</th>
                  <th>Trạng thái</th>
                  <th>Hành động</th>
                </tr>
              </thead>
              <tbody>
                {users.map(u => (
                  <tr key={u.id}>
                    <td>{u.id}</td>
                    <td>{u.email}</td>
                    <td>{u.role ?? 'student'}</td>
                    <td>{u.isBanned ? 'Cấm' : 'Hoạt động'}</td>
                    <td>
                      <button
                        onClick={() => handleToggleBan(u)}
                        className={u.isBanned ? styles.unbanButton : styles.banButton}
                      >
                        {u.isBanned ? 'Bỏ cấm' : 'Cấm'}
                      </button>
                      <button className={styles.editButton}>Sửa</button>
                      <button onClick={() => handleOpenDeleteModal(u)} className={styles.deleteButton}>
                        Xóa
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {view === 'classes' && (
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Tên lớp</th>
                  <th>Giáo viên</th>
                  <th>Hành động</th>
                </tr>
              </thead>
              <tbody>
                {classes.map(cls => (
                  <tr key={cls.id}>
                    <td>{cls.id}</td>
                    <td>{cls.name}</td>
                    <td>{cls.teacher}</td>
                    <td>
                      <button className={styles.editButton}>Sửa</button>
                      <button onClick={() => handleOpenDeleteModal(cls)} className={styles.deleteButton}>
                        Xóa
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {view === 'notifications' && (
            <div className={styles.notificationForm}>
              <h2>Tạo thông báo mới</h2>
              <textarea
                value={notificationMessage}
                onChange={e => setNotificationMessage(e.target.value)}
                placeholder="Nhập nội dung thông báo cho toàn bộ người dùng..."
                rows={5}
              />
              <button onClick={handleSendNotification} className={styles.actionButton}>
                Gửi đi
              </button>
            </div>
          )}
        </div>
      </div>

      <Modal
        isOpen={isDeleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa"
      >
        <p>Bạn có chắc chắn muốn xóa mục này? Hành động này không thể hoàn tác.</p>
      </Modal>

      <CreateUserModal
        isOpen={isCreateUserModalOpen}
        onClose={() => setCreateUserModalOpen(false)}
        onConfirm={handleCreateUser}
      />
    </>
  );
};

export default AdminDashboardPage;