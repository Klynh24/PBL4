import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import * as api from '../../api/apiService';
import styles from './ProfilePage.module.css';
import { FiEdit3, FiSave } from 'react-icons/fi';

const ProfilePage: React.FC = () => {
  const { user, updateUserContext } = useAuth();
  const [isEditing, setIsEditing] = useState(false);
  
  // State được khởi tạo với giá trị mặc định để tránh lỗi
  const [formData, setFormData] = useState({
    name: '',
    dob: '',
    gender: 'Khác' as 'Nam' | 'Nữ' | 'Khác',
  });

  // useEffect sẽ cập nhật form data khi user đã có sẵn
  useEffect(() => {
    if (user) {
      setFormData({
        name: user.name || '',
        dob: user.dob || '',
        gender: user.gender || 'Khác',
      });
    }
  }, [user]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSave = async () => {
    if (!user) return;
    try {
        const updatedUser = await api.updateUser(user.id, formData);
        updateUserContext(updatedUser.data); // Cập nhật context
        setIsEditing(false);
    } catch (error) {
        console.error("Failed to update profile", error);
        alert("Cập nhật thất bại!");
    }
  };

  // SỬA LỖI: Hiển thị thông báo tải rõ ràng hơn
  if (!user) {
    return <div className={styles.loading}>Đang tải thông tin người dùng...</div>;
  }

  return (
    <div className={styles.container}>
      <div className={styles.profileCard}>
        <div className={styles.header}>
            <div className={styles.avatar}>
                {/* SỬA LỖI: Thêm phương án dự phòng để không bị crash */}
                {user.name?.charAt(0).toUpperCase() || user.email?.charAt(0).toUpperCase() || '?'}
            </div>
            <div className={styles.headerInfo}>
                <h1 className={styles.name}>{user.name || 'Người dùng mới'}</h1>
                <p className={styles.email}>{user.email}</p>
            </div>
            <button onClick={() => setIsEditing(!isEditing)} className={styles.editButton}>
                {isEditing ? <FiSave /> : <FiEdit3 />}
                {isEditing ? 'Hủy' : 'Chỉnh sửa'}
            </button>
        </div>

        <div className={styles.details}>
            <h2 className={styles.sectionTitle}>Thông tin cá nhân</h2>
            {isEditing ? (
                <div className={styles.editForm}>
                    <div className={styles.formGroup}>
                        <label>Họ và tên</label>
                        <input type="text" name="name" value={formData.name} onChange={handleInputChange} className={styles.input} />
                    </div>
                    <div className={styles.formGroup}>
                        <label>Ngày sinh</label>
                        <input type="date" name="dob" value={formData.dob} onChange={handleInputChange} className={styles.input} />
                    </div>
                    <div className={styles.formGroup}>
                        <label>Giới tính</label>
                        <select name="gender" value={formData.gender} onChange={handleInputChange} className={styles.input}>
                            <option value="Nam">Nam</option>
                            <option value="Nữ">Nữ</option>
                            <option value="Khác">Khác</option>
                        </select>
                    </div>
                    <button onClick={handleSave} className={styles.saveButton}>Lưu thay đổi</button>
                </div>
            ) : (
                <div className={styles.infoGrid}>
                    <div className={styles.infoItem}>
                        <span className={styles.infoLabel}>Họ và tên</span>
                        <span className={styles.infoValue}>{user.name || 'Chưa cập nhật'}</span>
                    </div>
                    <div className={styles.infoItem}>
                        <span className={styles.infoLabel}>Ngày sinh</span>
                        <span className={styles.infoValue}>{user.dob || 'Chưa cập nhật'}</span>
                    </div>
                    <div className={styles.infoItem}>
                        <span className={styles.infoLabel}>Giới tính</span>
                        <span className={styles.infoValue}>{user.gender || 'Chưa cập nhật'}</span>
                    </div>
                     <div className={styles.infoItem}>
                        <span className={styles.infoLabel}>Vai trò</span>
                        <span className={styles.infoValue}>{user.role}</span>
                    </div>
                </div>
            )}
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;

