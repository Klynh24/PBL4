import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import * as api from '../../api/apiService';
import styles from './ClassesPage.module.css'; 
import InputModal from '../../components/common/InputModal/InputModal';
import { FiMoreVertical, FiFolder, FiCalendar, FiCheckSquare } from 'react-icons/fi';

interface UserInfo {
    id: number;
    name: string;
    email: string;
}

interface Class {
  id: number;
  name: string;
  description?: string;
  user?: UserInfo;
}

const colorClasses = [
  styles.color1,
  styles.color2,
  styles.color3,
  styles.color4,
  styles.color5,
];

const getInitials = (name: string) => {
    if (!name) return "C";
    return name.split(' ').map(word => word[0]).join('').substring(0, 2).toUpperCase();
}

const ClassesPage: React.FC = () => {
  const { user } = useAuth();
  const [classes, setClasses] = useState<Class[]>([]);
  const [loading, setLoading] = useState(true);
  const [isJoinModalOpen, setJoinModalOpen] = useState(false);
  const [isCreateModalOpen, setCreateModalOpen] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchClasses();
  }, []);

  const fetchClasses = () => {
    setLoading(true);
    api.getClasses()
      .then(res => {
        // ⭐ FIX LỖI TS2339: Ép kiểu về any để tránh lỗi kiểm tra type chặt chẽ
        const responseData: any = res.data.data ? res.data.data : res.data;

        if (responseData && responseData.content && Array.isArray(responseData.content)) {
            setClasses(responseData.content);
        }
        else if (Array.isArray(responseData)) {
            setClasses(responseData);
        }
        else {
            setClasses([]);
        }
      })
      .catch(err => {
        console.error("Lỗi tải lớp:", err);
        setError("Không thể tải danh sách lớp học.");
      })
      .finally(() => setLoading(false));
  };

  const handleJoinClass = async (classCode: string) => {
      if (!classCode) return;
      try {
        await api.joinClass(classCode);

        alert("Tham gia thành công!");
        fetchClasses();
        setJoinModalOpen(false);
      } catch (error: any) {
        const msg = error.response?.data?.message || "Tham gia lớp học thất bại.";
        alert(msg);
      }
    };

  const handleCreateClass = async (className: string) => {
    if (!className) return;
    try {
      await api.createClass({ name: className });
      fetchClasses();
      setCreateModalOpen(false);
      setError('');
    } catch (error: any) {
      console.error("Lỗi tạo lớp:", error);
      if (error.response && error.response.status === 403) {
          alert("Bạn không có quyền tạo lớp (403). Hãy đăng xuất và đăng nhập lại!");
      } else {
          alert("Tạo lớp học thất bại.");
      }
    }
  };

  const renderHeader = () => (
    <div className={styles.header}>
      <h1 className={styles.title}>Lớp học</h1>
      <div className={styles.headerActions}>
        {user?.role === 'teacher' || user?.role === 'admin' ? (
            <button onClick={() => setCreateModalOpen(true)} className={styles.actionButton}>Tạo lớp mới</button>
        ) : (
            <button onClick={() => setJoinModalOpen(true)} className={styles.actionButton}>Tham gia bằng mã</button>
        )}
      </div>
    </div>
  );

  if (loading) return <div className={styles.message}>Đang tải dữ liệu...</div>;

  if (classes.length === 0) {
    return (
      <>
        <div className={styles.container}>
          {renderHeader()}
          <div className={styles.message}>
             {error ? error : "Bạn chưa có lớp học nào. Hãy tạo hoặc tham gia lớp mới!"}
          </div>
        </div>
        <InputModal isOpen={isJoinModalOpen} onClose={() => setJoinModalOpen(false)} onConfirm={handleJoinClass} title="Tham gia lớp học" placeholder="Nhập mã lớp..."/>
        <InputModal isOpen={isCreateModalOpen} onClose={() => setCreateModalOpen(false)} onConfirm={handleCreateClass} title="Tạo lớp học mới" placeholder="Nhập tên lớp học..."/>
      </>
    );
  }

  return (
    <>
      <div className={styles.container}>
        {renderHeader()}
        {error && <p className={styles.error}>{error}</p>}

        <div className={styles.grid}>
          {classes.map((cls, index) => (
            <Link to={`/classes/${cls.id}`} key={cls.id} className={styles.cardLink}>
              <div className={styles.card}>
                <div className={`${styles.cardAvatar} ${colorClasses[index % colorClasses.length]}`}>
                    <span>{getInitials(cls.name)}</span>
                </div>
                <div className={styles.cardContent}>
                    <h3 className={styles.cardTitle}>{cls.name}</h3>
                    <p className={styles.cardSubtext}>
                        {cls.user ? cls.user.name : "Chưa có giáo viên"}
                    </p>
                </div>
                <div className={styles.cardFooter}>
                    <FiFolder />
                    <FiCalendar />
                    <FiCheckSquare />
                </div>
                <button
                    className={styles.moreOptionsButton}
                    onClick={(e) => {
                        e.preventDefault();
                        e.stopPropagation();
                        alert('Chức năng đang phát triển');
                    }}
                >
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