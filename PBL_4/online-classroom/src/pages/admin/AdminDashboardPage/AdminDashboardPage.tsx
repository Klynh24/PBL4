import React, { useState, useEffect } from 'react';
import * as api from '../../../api/apiService';
import styles from './AdminDashboardPage.module.css';
import Modal from '../../../components/common/Modal/Modal';
import CreateUserModal from '../../../components/common/CreateUserModal/CreateUserModal'; // Import modal mới

// Định nghĩa kiểu dữ liệu
interface User { id: number; email: string; role: string; }
interface Class { id: number; name: string; teacher: string; }

const AdminDashboardPage: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [classes, setClasses] = useState<Class[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [view, setView] = useState('users');

  // State cho Modal xác nhận xóa
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false);
  const [itemToDelete, setItemToDelete] = useState<User | Class | null>(null);

  // State cho Modal tạo người dùng
  const [isCreateUserModalOpen, setCreateUserModalOpen] = useState(false);

  // State cho form tạo thông báo
  const [notificationMessage, setNotificationMessage] = useState('');

  const fetchData = async () => {
    try {
      setLoading(true);
      const [usersResponse, classesResponse] = await Promise.all([
        api.getUsers(),
        api.getClasses(),
      ]);
      setUsers(usersResponse.data);
      setClasses(classesResponse.data);
    } catch (err) {
      setError('Không thể tải dữ liệu quản trị.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // --- Logic Xóa ---
  const handleOpenDeleteModal = (item: User | Class) => {
    setItemToDelete(item);
    setDeleteModalOpen(true);
  };
  const handleConfirmDelete = async () => {
    if (!itemToDelete) return;
    try {
      if ('email' in itemToDelete) {
        await api.deleteUser(itemToDelete.id);
        setUsers(users.filter(user => user.id !== itemToDelete.id));
      } else {
        // await api.deleteClass(itemToDelete.id);
        setClasses(classes.filter(cls => cls.id !== itemToDelete.id));
      }
    } catch (err) {
      alert('Lỗi: Không thể thực hiện thao tác xóa.'); 
    } finally {
      setDeleteModalOpen(false);
      setItemToDelete(null);
    }
  };

  // --- Logic Tạo người dùng ---
  const handleCreateUser = async (userData: any) => {
    try {
        const response = await api.createUser(userData);
        setUsers(prevUsers => [...prevUsers, response.data]); // Thêm user mới vào danh sách
        setCreateUserModalOpen(false);
    } catch (error: any) {
        alert(`Tạo người dùng thất bại: ${error.response?.data?.message || 'Lỗi không xác định'}`);
    }
  };

  // --- Logic Gửi thông báo ---
  const handleSendNotification = async () => {
    if (!notificationMessage.trim()) {
        alert("Vui lòng nhập nội dung thông báo.");
        return;
    }
    try {
        await api.createNotification({ message: notificationMessage });
        alert("Gửi thông báo thành công!");
        setNotificationMessage(''); // Xóa nội dung sau khi gửi
    } catch (error) {
        alert("Gửi thông báo thất bại.");
    }
  };

  if (loading) return <div className={styles.message}>Đang tải dữ liệu...</div>;
  if (error) return <div className={`${styles.message} ${styles.error}`}>{error}</div>;

  return (
    <>
      <div className={styles.container}>
        <div className={styles.header}>
            <h1 className={styles.title}>Bảng điều khiển Quản trị</h1>
            {view === 'users' && 
                <button onClick={() => setCreateUserModalOpen(true)} className={styles.actionButton}>
                    Tạo người dùng mới
                </button>
            }
        </div>
        <div className={styles.tabs}>
          <button onClick={() => setView('users')} className={`${styles.tabButton} ${view === 'users' ? styles.active : ''}`}>Quản lý Người dùng</button>
          <button onClick={() => setView('classes')} className={`${styles.tabButton} ${view === 'classes' ? styles.active : ''}`}>Quản lý Lớp học</button>
          <button onClick={() => setView('notifications')} className={`${styles.tabButton} ${view === 'notifications' ? styles.active : ''}`}>Gửi thông báo</button>
        </div>

        <div className={styles.content}>
          {view === 'users' && (
            <table className={styles.table}>
                <thead><tr><th>ID</th><th>Email</th><th>Vai trò</th><th>Hành động</th></tr></thead>
                <tbody>{users.map(user => (<tr key={user.id}><td>{user.id}</td><td>{user.email}</td><td>{user.role}</td><td><button className={styles.editButton}>Sửa</button><button onClick={() => handleOpenDeleteModal(user)} className={styles.deleteButton}>Xóa</button></td></tr>))}</tbody>
            </table>
          )}
          {view === 'classes' && (
            <table className={styles.table}>
                <thead><tr><th>ID</th><th>Tên lớp</th><th>Giáo viên</th><th>Hành động</th></tr></thead>
                <tbody>{classes.map(cls => (<tr key={cls.id}><td>{cls.id}</td><td>{cls.name}</td><td>{cls.teacher}</td><td><button className={styles.editButton}>Sửa</button><button onClick={() => handleOpenDeleteModal(cls)} className={styles.deleteButton}>Xóa</button></td></tr>))}</tbody>
            </table>
          )}
          {view === 'notifications' && (
            <div className={styles.notificationForm}>
                <h2>Tạo thông báo mới</h2>
                <textarea 
                    value={notificationMessage}
                    onChange={(e) => setNotificationMessage(e.target.value)}
                    placeholder="Nhập nội dung thông báo cho toàn bộ người dùng..."
                    rows={5}
                />
                <button onClick={handleSendNotification} className={styles.actionButton}>Gửi đi</button>
            </div>
          )}
        </div>
      </div>

      <Modal isOpen={isDeleteModalOpen} onClose={() => setDeleteModalOpen(false)} onConfirm={handleConfirmDelete} title="Xác nhận xóa">
        <p>
          Bạn có chắc chắn muốn xóa mục này? Hành động này không thể hoàn tác.
        </p>
      </Modal>

      <CreateUserModal isOpen={isCreateUserModalOpen} onClose={() => setCreateUserModalOpen(false)} onConfirm={handleCreateUser} />
    </>
  );
};

export default AdminDashboardPage;