// src/pages/admin/AdminDashboardPage/AdminDashboardPage.tsx

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../contexts/AuthContext'; 
import * as api from '../../../api/apiService';
import styles from './AdminDashboardPage.module.css';
import Modal from '../../../components/common/Modal/Modal';
import CreateUserModal from '../../../components/common/CreateUserModal/CreateUserModal'; 
// 🛑 SỬA LỖI 1: Import types từ file chung
import { User, Class, RegisterData } from '../../../types'; // Thêm RegisterData

/* 🛑 SỬA LỖI 2: Xóa interface cục bộ
interface User { ... }
interface Class { ... }
*/

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
            setError(''); // Xóa lỗi cũ
            const [usersResponse, classesResponse] = await Promise.all([
                api.getUsers(),
                api.getClasses(),
            ]);
            
            // 🛑 SỬA LỖI 3: Đọc từ response.data.data (đây là mảng User[])
            setUsers(usersResponse.data.data.map((u: User) => ({ // Dùng kiểu User
                ...u,
                isBanned: u.isBanned || false, 
                // Chuẩn hóa role
                role: u.userCatalogues?.some((c:any) => c.name === 'ROLE_ADMIN') ? 'admin' 
                    : u.userCatalogues?.some((c:any) => c.name === 'ROLE_TEACHER') ? 'teacher' 
                    : 'student' 
            }))); 
            
            // 🛑 SỬA LỖI 4: Đọc từ response.data.data (đây là mảng Class[])
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
    }, []);

    const handleToggleBan = async (userToToggle: User) => {
        try {
            // 🛑 SỬA LỖI 5: Hàm banUser/unbanUser đã tồn tại trong apiService
            const action = userToToggle.isBanned ? api.unbanUser : api.banUser;
            await action(userToToggle.id);
            
            setUsers(prevUsers => prevUsers.map(u => 
                u.id === userToToggle.id ? { ...u, isBanned: !u.isBanned } : u
            ));
            alert(`Người dùng ${userToToggle.email} đã được ${userToToggle.isBanned ? 'bỏ cấm' : 'cấm'}.`);
        
        // 🛑 SỬA LỖI 7 (Cú pháp): Sửa lại cú pháp catch
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
            if ('email' in itemToDelete) { // Đây là User
                await api.deleteUser(itemToDelete.id);
                setUsers(users.filter(user => user.id !== itemToDelete.id));
            } else { // Đây là Class
                // TODO: Cần thêm api.deleteClass(id) vào apiService.ts
                // await api.deleteClass(itemToDelete.id); 
                alert("API Xóa lớp học chưa được cài đặt (TODO).");
                setClasses(classes.filter(cls => cls.id !== itemToDelete.id));
            }
        // 🛑 SỬA LỖI 7 (Cú pháp): Sửa lại cú pháp catch
        } catch (err) {
            alert(`Lỗi: Không thể thực hiện thao tác xóa. ${api.getErrorMessage(err)}`); 
        } finally {
            setDeleteModalOpen(false);
            setItemToDelete(null);
        }
    };

    // 🛑 SỬA LỖI 8: Dùng đúng kiểu 'RegisterData'
    const handleCreateUser = async (userData: RegisterData) => { 
        try {
            const response = await api.createUser(userData);
            // 🛑 SỬA LỖI 6: Đọc từ response.data.data (đây là object User)
            setUsers(prevUsers => [...prevUsers, { ...response.data.data, isBanned: false }]); 
            setCreateUserModalOpen(false);
            alert("Tạo người dùng thành công!");
        // 🛑 SỬA LỖI 7 (Cú pháp): Sửa lại cú pháp catch
        } catch (error) { 
            alert(`Tạo người dùng thất bại: ${api.getErrorMessage(error)}`);
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
        // 🛑 SỬA LỖI 7 (Cú pháp): Sửa lại cú pháp catch
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
                        <thead><tr><th>ID</th><th>Email</th><th>Vai trò</th><th>Trạng thái</th><th>Hành động</th></tr></thead>
                        <tbody>{users.map(user => (
                            <tr key={user.id}>
                                <td>{user.id}</td>
                                <td>{user.email}</td>
                                <td>{user.role}</td>
                                <td>{user.isBanned ? 'Cấm' : 'Hoạt động'}</td>
                                <td>
                                    <button 
                                        onClick={() => handleToggleBan(user)}
                                        className={user.isBanned ? styles.unbanButton : styles.banButton}
                                    >
                                        {user.isBanned ? 'Bỏ cấm' : 'Cấm'}
                                    </button>
                                    <button className={styles.editButton}>Sửa</button>
                                    <button onClick={() => handleOpenDeleteModal(user)} className={styles.deleteButton}>Xóa</button>
                                </td>
                            </tr>
                        ))}</tbody>
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

            {/* Mở modal tạo người dùng */}
            <CreateUserModal isOpen={isCreateUserModalOpen} onClose={() => setCreateUserModalOpen(false)} onConfirm={handleCreateUser} />
        </>
    );
};

export default AdminDashboardPage;