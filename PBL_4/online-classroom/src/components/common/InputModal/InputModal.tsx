import React, { useState } from 'react';
import styles from './InputModal.module.css';

interface InputModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (inputValue: string) => void;
  title: string;
  placeholder?: string;
}

const InputModal: React.FC<InputModalProps> = ({ isOpen, onClose, onConfirm, title, placeholder }) => {
  const [inputValue, setInputValue] = useState('');

  if (!isOpen) {
    return null;
  }

  const handleConfirm = () => {
    onConfirm(inputValue);
    setInputValue(''); // Reset input sau khi xác nhận
    onClose(); // Tự động đóng modal sau khi xác nhận
  };
  
  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter') {
      handleConfirm();
    }
  };

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <h2 className={styles.title}>{title}</h2>
        <input
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          className={styles.input}
          placeholder={placeholder || 'Nhập tại đây...'}
          autoFocus
        />
        <div className={styles.actions}>
          <button onClick={onClose} className={`${styles.btn} ${styles.btnSecondary}`}>
            Hủy
          </button>
          <button onClick={handleConfirm} className={`${styles.btn} ${styles.btnPrimary}`}>
            Xác nhận
          </button>
        </div>
      </div>
    </div>
  );
};

export default InputModal;