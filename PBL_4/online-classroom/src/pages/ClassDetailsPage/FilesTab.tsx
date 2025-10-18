import React, { useState, useEffect } from 'react';
import { useOutletContext } from 'react-router-dom';
import { User } from '../../contexts/AuthContext';
import styles from '../ClassDetailsPage.module.css';
import UploadModal from '../../components/common/UploadModal/UploadModal';
import { FiFolder, FiFileText, FiUpload, FiDownload } from 'react-icons/fi';

interface ClassFile {
    id: number; type: 'folder' | 'file'; name: string;
    date: string; size?: string;
}

const FilesTab: React.FC = () => {
    const { user } = useOutletContext<{ user: User | null }>();
    const [isUploadModalOpen, setUploadModalOpen] = useState(false);
    const [files, setFiles] = useState<ClassFile[]>([]);
    
    useEffect(() => {
        // Sau này sẽ thay bằng api.getFiles(classId)
        const mockFiles = [
            { id: 1, type: 'folder' as const, name: 'Bài giảng', date: '16/10/2025' },
            { id: 2, type: 'file' as const, name: 'de-cuong-mon-hoc.pdf', size: '1.2 MB', date: '15/10/2025' },
        ];
        setFiles(mockFiles);
    }, []);

    const handleConfirmUpload = (file: File) => {
        const newFile: ClassFile = {
            id: Date.now(), type: 'file', name: file.name,
            size: `${(file.size / 1024 / 1024).toFixed(2)} MB`,
            date: new Date().toLocaleDateString('vi-VN')
        };
        setFiles(prevFiles => [newFile, ...prevFiles]);
    };

    return (
        <>
            {user?.role === 'teacher' && <button onClick={() => setUploadModalOpen(true)} className={styles.actionButton}><FiUpload/> Tải lên</button>}
            <ul className={styles.fileList}>
                {files.map(file => (
                    <li key={file.id} className={styles.fileItem}>
                        <div className={styles.fileInfo}><div className={styles.fileIcon}>{file.type === 'folder' ? <FiFolder/> : <FiFileText/>}</div><span className={styles.fileName}>{file.name}</span></div>
                        <div className={styles.fileMeta}><span>{file.date}</span><span>{file.size || '--'}</span><button className={styles.fileActionButton}><FiDownload/></button></div>
                    </li>
                ))}
            </ul>
            <UploadModal isOpen={isUploadModalOpen} onClose={() => setUploadModalOpen(false)} onConfirm={handleConfirmUpload}/>
        </>
    );
};

export default FilesTab;