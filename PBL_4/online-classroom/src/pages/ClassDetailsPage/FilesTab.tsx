import React, { useState, useEffect } from 'react';
import { useOutletContext, useParams } from 'react-router-dom';
import * as api from '../../api/apiService'; 
import { User } from '../../types'; 
import styles from './ClassDetailsPage.module.css'; 
import UploadModal from '../../components/common/UploadModal/UploadModal';
import { FiFolder, FiFileText, FiUpload, FiDownload } from 'react-icons/fi';

interface ClassFile {
    id: number; type: 'folder' | 'file'; name: string;
    date: string; size?: string;
}

const mockApiGetFiles = async (classId: string): Promise<ClassFile[]> => {
    console.warn(`[TODO] Gọi API tải files cho Class ID: ${classId}`);
    return new Promise(resolve => setTimeout(() => resolve([]), 500)); 
};

const FilesTab: React.FC = () => {
    const { user } = useOutletContext<{ user: User | null }>();
    const { classId } = useParams<{ classId: string }>();
    
    const [isUploadModalOpen, setUploadModalOpen] = useState(false);
    const [files, setFiles] = useState<ClassFile[]>([]);
    const [loading, setLoading] = useState(true);

    const fetchFiles = async () => {
        if (!classId) return;
        setLoading(true);
        try {
            const data = await mockApiGetFiles(classId); 
            setFiles(data);
        } catch (error) {
            console.error("Không thể tải tài liệu:", api.getErrorMessage(error));
        } finally {
            setLoading(false);
        }
    };
    
    useEffect(() => {
        fetchFiles();
    }, [classId]);

    const handleConfirmUpload = (file: File) => {
        const newFile: ClassFile = {
            id: Date.now(), type: 'file', name: file.name,
            size: `${(file.size / 1024 / 1024).toFixed(2)} MB`,
            date: new Date().toLocaleDateString('vi-VN')
        };
        setFiles(prevFiles => [newFile, ...prevFiles]);
        alert(`Tải lên file "${file.name}" thành công! (Chức năng API cần được triển khai)`);
    };

    if (loading) return <p className={styles.message}>Đang tải tài liệu...</p>;

    return (
        <>
            {user?.role === 'teacher' && <button onClick={() => setUploadModalOpen(true)} className={styles.actionButton}><FiUpload/> Tải lên</button>}
            
            <ul className={styles.fileList}>
                {files.length === 0 ? (
                    <p className={styles.message}>Chưa có tài liệu nào trong thư mục này.</p>
                ) : (
                    files.map(file => (
                        <li key={file.id} className={styles.fileItem}>
                            <div className={styles.fileInfo}>
                                <div className={styles.fileIcon}>{file.type === 'folder' ? <FiFolder/> : <FiFileText/>}</div>
                                <span className={styles.fileName}>{file.name}</span>
                            </div>
                            <div className={styles.fileMeta}>
                                <span>{file.date}</span>
                                <span>{file.size || '--'}</span>
                                <button className={styles.fileActionButton}><FiDownload/></button>
                            </div>
                        </li>
                    ))
                )}
            </ul>
            <UploadModal isOpen={isUploadModalOpen} onClose={() => setUploadModalOpen(false)} onConfirm={handleConfirmUpload}/>
        </>
    );
};

export default FilesTab;