import React, { useState, useEffect } from 'react';
import styles from './AssignmentModal.module.css';

interface AssignmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  // Sửa lại type của onConfirm: giờ đã được kết hợp vào chuỗi dueDate
  onConfirm: (assignmentData: { title: string; description: string; dueDate: string }) => void;
  classId: number;
}

const AssignmentModal: React.FC<AssignmentModalProps> = ({ isOpen, onClose, onConfirm, classId }) => {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [dueDateOnly, setDueDateOnly] = useState(''); // State riêng cho Ngày (YYYY-MM-DD)
  const [dueTimeOnly, setDueTimeOnly] = useState('23:59'); // State riêng cho Giờ (HH:mm), mặc định 23:59
  const [error, setError] = useState('');

  // Reset state khi modal mở
  useEffect(() => {
    if (isOpen) {
      setTitle('');
      setDescription('');
      setDueDateOnly('');
      setDueTimeOnly('23:59');
      setError('');
    }
  }, [isOpen]);

  if (!isOpen) {
    return null;
  }

  const handleConfirm = () => {
    if (!title || !dueDateOnly) {
        setError("Vui lòng nhập tiêu đề và Ngày hết hạn.");
        return;
    }

    // ⭐ KẾT HỢP NGÀY (YYYY-MM-DD) VÀ GIỜ (HH:mm) ⭐
    // Định dạng kết quả: YYYY-MM-DDTHH:mm
    const combinedDueDate = `${dueDateOnly}T${dueTimeOnly}`;

    // Gọi hàm onConfirm với chuỗi ngày giờ đầy đủ
    onConfirm({
        title,
        description,
        dueDate: combinedDueDate
    });

    onClose();
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    // Chỉ kích hoạt khi nhấn Enter trên input text/date/time
    if (event.key === 'Enter' && event.currentTarget.tagName !== 'TEXTAREA') {
      handleConfirm();
    }
  };

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <h2 className={styles.title}>Tạo bài tập mới</h2>

        {/* Tiêu đề */}
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

        {/* Mô tả */}
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

        {/* ⭐ NGÀY & GIỜ HẾT HẠN (Sửa đổi) ⭐ */}
        <div className={styles.dateTimeGroup}>
            {/* Ngày hết hạn */}
            <div className={`${styles.formGroup} ${styles.dateInputWrapper}`}>
                <label htmlFor="dueDateOnly">Ngày hết hạn</label>
                <input
                    id="dueDateOnly"
                    type="date"
                    value={dueDateOnly}
                    onChange={(e) => setDueDateOnly(e.target.value)}
                    onKeyDown={handleKeyDown}
                    className={styles.input}
                />
            </div>

            {/* Giờ hết hạn */}
            <div className={`${styles.formGroup} ${styles.timeInputWrapper}`}>
                <label htmlFor="dueTimeOnly">Giờ hết hạn</label>
                <input
                    id="dueTimeOnly"
                    type="time"
                    value={dueTimeOnly}
                    onChange={(e) => setDueTimeOnly(e.target.value)}
                    onKeyDown={handleKeyDown}
                    className={styles.input}
                />
            </div>
        </div>

        {/* Thông báo lỗi */}
        {error && <p className={styles.errorMessage}>{error}</p>}

        {/* Actions */}
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