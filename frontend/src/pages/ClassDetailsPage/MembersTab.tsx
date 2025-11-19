import React, { useEffect, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import * as api from '../../api/apiService'; // Import API call
import { User, ClassDetails } from '../../types';
import styles from './ClassDetailsPage.module.css';

interface ContextType {
    user: User | null;
    classDetails: ClassDetails | null;
}

const MembersTab: React.FC = () => {
    const { classDetails } = useOutletContext<ContextType>();

    // State để lưu danh sách thành viên lấy từ API về
    const [members, setMembers] = useState<User[]>([]);
    const [loading, setLoading] = useState(true);

    // Gọi API lấy thành viên khi component được load
    useEffect(() => {
        if (classDetails?.id) {
            const fetchMembers = async () => {
                try {
                    // Gọi API riêng để lấy danh sách thành viên
                    const res = await api.getClassMembers(classDetails.id.toString());

                    // Xử lý dữ liệu trả về (tùy cấu trúc response của bạn)
                    const data: any = res.data.data ? res.data.data : res.data;
                    setMembers(Array.isArray(data) ? data : []);
                } catch (error) {
                    console.error("Lỗi tải thành viên:", error);
                } finally {
                    setLoading(false);
                }
            };
            fetchMembers();
        }
    }, [classDetails]);

    if (!classDetails) return <p className={styles.message}>Đang tải thông tin lớp...</p>;

    return (
        <div className={styles.membersContainer}>
            {/* --- PHẦN 1: GIÁO VIÊN (Lấy từ classDetails) --- */}
            <h3 className={styles.sectionTitle}>Giáo viên</h3>
            <ul className={styles.memberList}>
                <li className={styles.memberItem}>
                    <div className={styles.memberAvatar} style={{ backgroundColor: '#f8bbd0', color: '#e91e63' }}>
                        GV
                    </div>
                    <span className={styles.memberName}>
                        {/* Sửa lỗi teacher -> user.name */}
                        {classDetails.user ? classDetails.user.name : "Chưa cập nhật"} (Chủ phòng)
                    </span>
                </li>
            </ul>

            {/* --- PHẦN 2: HỌC VIÊN (Lấy từ API riêng) --- */}
            <h3 className={styles.sectionTitle} style={{ marginTop: '20px' }}>
                Thành viên lớp học ({members.length})
            </h3>

            {loading ? (
                <p className={styles.message}>Đang tải danh sách...</p>
            ) : members.length === 0 ? (
                <p className={styles.emptyMessage}>Chưa có thành viên nào tham gia.</p>
            ) : (
                <ul className={styles.memberList}>
                    {members.map(s => (
                        <li key={s.id} className={styles.memberItem} style={{ display: 'flex', alignItems: 'center', marginBottom: '15px' }}>
                            {/* Avatar */}
                            <div className={styles.memberAvatar} style={{
                                marginRight: '12px', // Tạo khoảng cách giữa Avatar và chữ
                                width: '40px', height: '40px', borderRadius: '50%',
                                backgroundColor: '#eee', display: 'flex', alignItems: 'center', justifyContent: 'center',
                                fontWeight: 'bold', color: '#555'
                            }}>
                                {s.name ? s.name.charAt(0).toUpperCase() : "U"}
                            </div>

                            {/* Thông tin (Dùng flex column để tên và email nằm dọc) */}
                            <div style={{ display: 'flex', flexDirection: 'column' }}>
                                <span className={styles.memberName} style={{ fontWeight: '600', fontSize: '1rem' }}>
                                    {s.name}
                                </span>
                                <span className={styles.memberEmail} style={{ fontSize: '0.85rem', color: '#888', marginTop: '2px' }}>
                                    {s.email}
                                </span>
                            </div>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
};

export default MembersTab;