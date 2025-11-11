import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import * as api from '../../api/apiService';
import styles from './ProfilePage.module.css';
import { FiEdit3, FiSave } from 'react-icons/fi';
import { UpdateUserData } from '../../types'; 

const ProfilePage: React.FC = () => {
    const { user, updateUserContext } = useAuth();
    const [isEditing, setIsEditing] = useState(false);
    
    const [formData, setFormData] = useState<UpdateUserData>({
        name: '',
        dob: '',
        gender: 'Khác',
    });

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
        setFormData(prev => ({ ...prev, [name]: value as any })); 
    };

    const handleSave = async () => {
        if (!user) return;
        try {
            const response = await api.updateUser(user.id, formData);
            const updatedUserData = response.data.data;
            
            updateUserContext(updatedUserData); 
            setIsEditing(false);
            alert("Cập nhật thông tin thành công!");
        } catch (error) {
            console.error("Failed to update profile", api.getErrorMessage(error));
            alert(`Cập nhật thất bại: ${api.getErrorMessage(error)}`);
        }
    };

    if (!user) {
        return <div className={styles.loading}>Đang tải thông tin người dùng...</div>;
    }

    return (
        <div className={styles.container}>
            <div className={styles.profileCard}>
                <div className={styles.header}>
                    <div className={styles.avatar}>
                        {}
                        {user.name?.charAt(0).toUpperCase() || user.email?.charAt(0).toUpperCase() || '?'}
                    </div>
                    <div className={styles.headerInfo}>
                        <h1 className={styles.name}>{user.name || 'Người dùng mới'}</h1>
                        <p className={styles.email}>{user.email}</p>
                    </div>
                    <button onClick={() => {
                        if (isEditing) {
                            setFormData({ name: user.name || '', dob: user.dob || '', gender: user.gender || 'Khác' });
                        }
                        setIsEditing(!isEditing);
                    }} className={styles.editButton}>
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