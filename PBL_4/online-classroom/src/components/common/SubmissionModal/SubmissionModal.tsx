import React, { useState, useRef } from 'react';
import styles from './SubmissionModal.module.css';
import { FiUploadCloud, FiFile, FiX } from 'react-icons/fi';

interface SubmissionModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (file: File) => void;
  assignmentTitle: string;
}

const SubmissionModal: React.FC<SubmissionModalProps> = ({ isOpen, onClose, onConfirm, assignmentTitle }) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen) {
    return null;
  }

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.files && event.target.files[0]) {
      setSelectedFile(event.target.files[0]);
    }
  };

  const handleConfirm = () => {
    if (selectedFile) {
      onConfirm(selectedFile);
      onClose();
    } else {
      alert("Vui lòng chọn một file để nộp.");
    }
  };

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <h2 className={styles.title}>Nộp bài cho: {assignmentTitle}</h2>
        
        <div 
          className={styles.dropzone} 
          onClick={() => fileInputRef.current?.click()}
        >
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileChange}
            style={{ display: 'none' }}
          />
          <FiUploadCloud className={styles.uploadIcon} />
          {selectedFile ? (
            <p>Đã chọn file: <strong>{selectedFile.name}</strong></p>
          ) : (
            <p>Kéo và thả file vào đây, hoặc bấm để chọn file</p>
          )}
        </div>

        {selectedFile && (
            <div className={styles.filePreview}>
                <FiFile />
                <span>{selectedFile.name}</span>
                <button onClick={() => setSelectedFile(null)} className={styles.removeFileButton}><FiX/></button>
            </div>
        )}

        <div className={styles.actions}>
          <button onClick={onClose} className={`${styles.btn} ${styles.btnSecondary}`}>
            Hủy
          </button>
          <button onClick={handleConfirm} className={`${styles.btn} ${styles.btnPrimary}`} disabled={!selectedFile}>
            Xác nhận nộp bài
          </button>
        </div>
      </div>
    </div>
  );
};

export default SubmissionModal;