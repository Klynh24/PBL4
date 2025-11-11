import React, { useState, useEffect } from 'react';
import styles from './AssignmentModal.module.css';

interface AssignmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (assignmentData: { title: string; description: string; dueDate: string }) => void;
  classId: number; 
}

const AssignmentModal: React.FC<AssignmentModalProps> = ({ isOpen, onClose, onConfirm, classId }) => {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [dueDate, setDueDate] = useState('');

  useEffect(() => {
    if (isOpen) {
      setTitle('');
      setDescription('');
      setDueDate('');
    }
  }, [isOpen]);

  if (!isOpen) {
    return null;
  }

  const handleConfirm = () => {
    if (!title || !dueDate) {
        alert("Vui lòng nhập tiêu đề và ngày hết hạn.");
        return;
    }
    onConfirm({ title, description, dueDate });
    onClose(); 
  };
  
  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter') {
      handleConfirm();
    }
  };

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <h2 className={styles.title}>Tạo bài tập mới</h2>
        <div className={styles.formGroup}>
            <label htmlFor="title">Tiêu đề bài tập</label>
            <input
                id="title"
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                onKeyDown={handleKeyDown}
                className={styles.input}
                placeholder="Ví dụ: Bài tập chương 1"
                autoFocus
            />
        </div>
        <div className={styles.formGroup}>
            <label htmlFor="description">Mô tả (Không bắt buộc)</label>
            <textarea
                id="description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className={styles.textarea}
                rows={4}
                placeholder="Hướng dẫn chi tiết cho bài tập..."
            />
        </div>
        <div className={styles.formGroup}>
            <label htmlFor="dueDate">Ngày hết hạn</label>
            <input
                id="dueDate"
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                onKeyDown={handleKeyDown}
                className={styles.input}
            />
        </div>
        <div className={styles.actions}>
          <button onClick={onClose} className={`${styles.btn} ${styles.btnSecondary}`}>
            Hủy
          </button>
          <button onClick={handleConfirm} className={`${styles.btn} ${styles.btnPrimary}`}>
            Giao bài
          </button>
        </div>
      </div>
    </div>
  );
};

export default AssignmentModal;