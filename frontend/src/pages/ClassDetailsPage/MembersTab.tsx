import React, { useEffect, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import * as api from '../../api/apiService'; // Import API
import { User, ClassDetails } from '../../types';
import styles from './ClassDetailsPage.module.css'; // Import CSS
import { FiPlusCircle, FiUserPlus, FiX, FiMail } from 'react-icons/fi'; // Icons

interface ContextType {
    user: User | null;
    classDetails: ClassDetails | null;
}

const MembersTab: React.FC = () => {
    const { user, classDetails } = useOutletContext<ContextType>();

    const [members, setMembers] = useState<User[]>([]);
    const [loading, setLoading] = useState(true);
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);

    // Hàm tải danh sách thành viên
    const fetchMembers = async () => {
        if (classDetails?.id) {
            try {
                const res = await api.getClassMembers(classDetails.id.toString());
                // Xử lý dữ liệu trả về tùy theo format API (data.data hoặc data trực tiếp)
                const data: any = res.data.data ? res.data.data : res.data;
                setMembers(Array.isArray(data) ? data : []);
            } catch (error) {
                console.error("Lỗi tải thành viên:", error);
            } finally {
                setLoading(false);
            }
        }
    };

    // Gọi API khi component mount hoặc classDetails thay đổi
    useEffect(() => {
        fetchMembers();
    }, [classDetails]);

    const isTeacher = user?.role === 'teacher';

    if (!classDetails) return <p className={styles.message}>Đang tải thông tin lớp...</p>;

    const handleAddClick = () => {
        setIsAddModalOpen(true);
    };

    // Xử lý logic thêm sinh viên
    const handleAddSubmit = async (email: string) => {
        try {
            // Gọi API thêm sinh viên (đã thêm vào apiService.ts)
            await api.addStudentToClass(classDetails.id.toString(), email);

            alert(`Đã thêm sinh viên ${email} vào lớp học thành công!`);
            setIsAddModalOpen(false);

            // Tải lại danh sách thành viên để cập nhật giao diện ngay lập tức
            setLoading(true);
            fetchMembers();
        } catch (error) {
            console.error("Lỗi thêm sinh viên:", error);
            alert(`Thêm thất bại: ${api.getErrorMessage(error)}`);
        }
    };

    return (
        <div className={styles.membersContainer}>
            {/* Modal Thêm Sinh Viên */}
            {isAddModalOpen && (
                <AddStudentModal
                    onClose={() => setIsAddModalOpen(false)}
                    onSubmit={handleAddSubmit}
                />
            )}

            {/* --- PHẦN 1: GIÁO VIÊN --- */}
            <h3 className={styles.sectionTitle}>Giáo viên</h3>
            <ul className={styles.memberList}>
                <li className={styles.memberItem}>
                    <div className={styles.memberAvatar} style={{ backgroundColor: '#f8bbd0', color: '#e91e63' }}>
                        GV
                    </div>
                    <span className={styles.memberName}>
                        {classDetails.user ? classDetails.user.name : "Chưa cập nhật"} (Chủ phòng)
                    </span>
                </li>
            </ul>

            {/* --- PHẦN 2: HỌC VIÊN --- */}
            <div className={styles.studentsHeader}>
                <h3 className={styles.sectionTitle}>
                    Danh sách sinh viên ({members.length})
                </h3>
                {isTeacher && (
                    <button onClick={handleAddClick} className={styles.inviteButton}>
                        <FiUserPlus style={{ marginRight: '8px' }} /> Thêm sinh viên
                    </button>
                )}
            </div>

            {loading ? (
                <p className={styles.message}>Đang tải danh sách...</p>
            ) : members.length === 0 ? (
                <p className={styles.emptyMessage}>Lớp học chưa có sinh viên nào.</p>
            ) : (
                <ul className={styles.memberList}>
                    {members.map(s => (
                        <li key={s.id} className={styles.memberItem}>
                            {/* Avatar */}
                            <div className={styles.memberAvatar} style={{
                                marginRight: '15px',
                                width: '42px', height: '42px', borderRadius: '50%',
                                backgroundColor: '#e3f2fd', display: 'flex', alignItems: 'center', justifyContent: 'center',
                                fontWeight: 'bold', color: '#1976d2', fontSize: '1.1rem'
                            }}>
                                {s.name ? s.name.charAt(0).toUpperCase() : "U"}
                            </div>

                            {/* Thông tin sinh viên */}
                            <div className={styles.memberInfo}>
                                <span className={styles.memberName}>{s.name}</span>
                                <span className={styles.memberEmail}>{s.email}</span>
                            </div>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
};

// --- Modal Component (Nằm cùng file để tiện quản lý) ---
interface AddStudentModalProps {
    onClose: () => void;
    onSubmit: (email: string) => void;
}

const AddStudentModal: React.FC<AddStudentModalProps> = ({ onClose, onSubmit }) => {
    const [email, setEmail] = useState('');

    return (
        <div className={styles.modalOverlay}>
            <div className={styles.modalContent}>
                {/* Nút đóng X */}
                <button className={styles.closeButtonIcon} onClick={onClose}>
                    <FiX />
                </button>

                <div className={styles.modalHeaderCenter}>
                    <div className={styles.iconCircle}>
                        <FiUserPlus />
                    </div>
                    <h2 className={styles.modalTitleCenter}>Thêm Sinh Viên</h2>
                    <p className={styles.modalSubtitle}>
                        Nhập email của sinh viên để thêm vào lớp học này.
                    </p>
                </div>

                <form onSubmit={(e) => { e.preventDefault(); if(email) onSubmit(email); }} className={styles.addForm}>
                    <div className={styles.inputGroup}>
                        <FiMail className={styles.inputIcon} />
                        <input
                            type="email"
                            placeholder="name@example.com"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className={styles.modernInput}
                            autoFocus
                            required
                        />
                    </div>

                    <div className={styles.modalActionsFull}>
                        <button type="button" onClick={onClose} className={styles.cancelButtonGhost}>
                            Hủy bỏ
                        </button>
                        <button type="submit" className={styles.submitButtonFull}>
                            Thêm vào lớp
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default MembersTab;