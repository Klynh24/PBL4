import React, { useState, useEffect } from 'react';
import styles from './CreateUserModal.module.css';

interface CreateUserModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (userData: any) => void;
}

const CreateUserModal: React.FC<CreateUserModalProps> = ({ isOpen, onClose, onConfirm }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<'student' | 'teacher'>('student');

  useEffect(() => {
    // Reset form khi modal mở
    if (isOpen) {
      setEmail('');
      setPassword('');
      setRole('student');
    }
  }, [isOpen]);

  if (!isOpen) {
    return null;
  }

  const handleConfirm = () => {
    if (!email || password.length < 6) {
      alert("Vui lòng nhập email và mật khẩu (ít nhất 6 ký tự).");
      return;
    }
    onConfirm({ email, password, role });
  };

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <h2 className={styles.title}>Tạo người dùng mới</h2>
        <div className={styles.formGroup}>
          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className={styles.input}
            placeholder="example@gmail.com"
            autoFocus
          />
        </div>
        <div className={styles.formGroup}>
          <label htmlFor="password">Mật khẩu</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={styles.input}
            placeholder="Ít nhất 6 ký tự"
          />
        </div>
        <div className={styles.formGroup}>
          <label htmlFor="role">Vai trò</label>
          <select id="role" value={role} onChange={(e) => setRole(e.target.value as any)} className={styles.input}>
            <option value="student">Học viên</option>
            <option value="teacher">Giáo viên</option>
          </select>
        </div>
        <div className={styles.actions}>
          <button onClick={onClose} className={`${styles.btn} ${styles.btnSecondary}`}>
            Hủy
          </button>
          <button onClick={handleConfirm} className={`${styles.btn} ${styles.btnPrimary}`}>
            Tạo
          </button>
        </div>
      </div>
    </div>
  );
};

export default CreateUserModal;