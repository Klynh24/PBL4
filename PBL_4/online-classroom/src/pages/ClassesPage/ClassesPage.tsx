import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import * as api from '../../api/apiService';
import styles from './ClassesPage.module.css';
import InputModal from '../../components/common/InputModal/InputModal';
import { FiMoreVertical, FiFolder, FiCalendar, FiCheckSquare } from 'react-icons/fi';

// Định nghĩa kiểu dữ liệu
interface Class {
  id: number;
  name: string;
  teacher: string;
}

// Danh sách các lớp màu để áp dụng cho avatar
const colorClasses = [
  styles.color1,
  styles.color2,
  styles.color3,
  styles.color4,
  styles.color5,
];

// Hàm lấy chữ cái đầu của tên lớp
const getInitials = (name: string) => {
    return name.split(' ').map(word => word[0]).join('').substring(0, 2).toUpperCase();
}

const ClassesPage: React.FC = () => {
  const { user } = useAuth();
  const [classes, setClasses] = useState<Class[]>([]);
  const [loading, setLoading] = useState(true);
  const [isJoinModalOpen, setJoinModalOpen] = useState(false);
  const [isCreateModalOpen, setCreateModalOpen] = useState(false);

  useEffect(() => {
    api.getClasses()
      .then(res => setClasses(res.data))
      .finally(() => setLoading(false));
  }, []);

  const handleJoinClass = async (classCode: string) => {
    if (!classCode) return;
    try {
      await api.joinClass(classCode);
      const res = await api.getClasses();
      setClasses(res.data);
    } catch (error) {
      alert("Tham gia lớp học thất bại.");
    }
  };

  const handleCreateClass = async (className: string) => {
    if (!className) return;
    try {
      await api.createClass({ name: className });
      const res = await api.getClasses();
      setClasses(res.data);
    } catch (error) {
      alert("Tạo lớp học thất bại.");
    }
  };

  if (loading) return <div className={styles.message}>Đang tải danh sách lớp học...</div>;

  return (
    <>
      <div className={styles.container}>
        <div className={styles.header}>
          <h1 className={styles.title}>Lớp học</h1>
          <div className={styles.headerActions}>
            {user?.role === 'teacher' ? (
                <button onClick={() => setCreateModalOpen(true)} className={styles.actionButton}>Tạo lớp mới</button>
            ) : user?.role === 'student' ? (
                <button onClick={() => setJoinModalOpen(true)} className={styles.actionButton}>Tham gia bằng mã</button>
            ) : null}
          </div>
        </div>
        
        <div className={styles.grid}>
          {classes.map((cls, index) => (
            <Link to={`/classes/${cls.id}`} key={cls.id} className={styles.cardLink}>
              <div className={styles.card}>
                <div className={`${styles.cardAvatar} ${colorClasses[index % colorClasses.length]}`}>
                    <span>{getInitials(cls.name)}</span>
                </div>
                <div className={styles.cardContent}>
                    <h3 className={styles.cardTitle}>{cls.name}</h3>
                    <p className={styles.cardSubtext}>{cls.teacher}</p>
                </div>
                <div className={styles.cardFooter}>
                    <FiFolder />
                    <FiCalendar />
                    <FiCheckSquare />
                </div>
                <button className={styles.moreOptionsButton} onClick={(e) => {e.preventDefault(); alert('More options');}}>
                    <FiMoreVertical />
                </button>
              </div>
            </Link>
          ))}
        </div>
      </div>

      <InputModal isOpen={isJoinModalOpen} onClose={() => setJoinModalOpen(false)} onConfirm={handleJoinClass} title="Tham gia lớp học" placeholder="Nhập mã lớp..."/>
      <InputModal isOpen={isCreateModalOpen} onClose={() => setCreateModalOpen(false)} onConfirm={handleCreateClass} title="Tạo lớp học mới" placeholder="Nhập tên lớp học..."/>
    </>
  );
};

export default ClassesPage;

