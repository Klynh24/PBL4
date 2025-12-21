import React, { useState, useEffect } from 'react';
import styles from './CreateUserModal.module.css';
import { User } from '../../../types';

interface CreateUserModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (userData: any) => void;
  initialData?: User | null;
  title?: string;
}

const CreateUserModal: React.FC<CreateUserModalProps> = ({ 
  isOpen, 
  onClose, 
  onConfirm, 
  initialData, 
  title = "Tạo thành viên mới" 
}) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState(''); // THÊM STATE PHONE
  const [role, setRole] = useState<'student' | 'teacher'>('student');

  useEffect(() => {
    if (isOpen) {
      if (initialData) {
        setEmail(initialData.email || '');
        setName(initialData.name || '');
        setPhone(initialData.phone || ''); // Nạp phone nếu sửa
        setPassword('');
        setRole(initialData.role === 'teacher' ? 'teacher' : 'student');
      } else {
        setEmail('');
        setPassword('');
        setName('');
        setPhone(''); // Reset phone khi tạo mới
        setRole('student');
      }
    }
  }, [isOpen, initialData]);

  if (!isOpen) return null;

  const handleConfirm = () => {
    // Kiểm tra thêm trường phone
    if (!email || !phone || (!initialData && password.length < 6)) {
      alert("Vui lòng nhập đầy đủ thông tin, bao gồm số điện thoại.");
      return;
    }

    // MAP DỮ LIỆU ĐỂ KHỚP VỚI BACKEND
    const payload: any = { 
      email, 
      name, 
      phone, // Gửi phone lên Backend
      // Map role sang ID (Giả sử: 2 là Giáo viên, 3 là Học sinh)
      userCatalogues: role === 'teacher' ? [2] : [3] 
    };
    
    if (password) payload.password = password;
    
    onConfirm(payload);
  };

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <h2 className={styles.title}>{title}</h2>
        
        <div className={styles.formGroup}>
          <label>Email</label>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} disabled={!!initialData} className={styles.input} />
        </div>

        <div className={styles.formGroup}>
          <label>Họ và tên</label>
          <input type="text" value={name} onChange={(e) => setName(e.target.value)} className={styles.input} />
        </div>

        {/* THÊM Ô NHẬP SỐ ĐIỆN THOẠI */}
        <div className={styles.formGroup}>
          <label>Số điện thoại</label>
          <input 
            type="text" 
            value={phone} 
            onChange={(e) => setPhone(e.target.value)} 
            className={styles.input} 
            placeholder="Nhập số điện thoại"
          />
        </div>

        <div className={styles.formGroup}>
          <label>{initialData ? "Mật khẩu mới (để trống nếu giữ nguyên)" : "Mật khẩu"}</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} className={styles.input} />
        </div>

        <div className={styles.formGroup}>
          <label>Vai trò</label>
          <select value={role} onChange={(e) => setRole(e.target.value as any)} className={styles.input}>
            <option value="student">Học viên</option>
            <option value="teacher">Giáo viên</option>
          </select>
        </div>

        <div className={styles.actions}>
          <button onClick={onClose} className={`${styles.btn} ${styles.btnSecondary}`}>Hủy</button>
          <button onClick={handleConfirm} className={`${styles.btn} ${styles.btnPrimary}`}>
            {initialData ? "Cập nhật" : "Tạo"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default CreateUserModal;