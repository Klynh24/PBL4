// AssignmentsTab.tsx (Đã sửa để dùng biến boolean rõ ràng)

import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import * as api from '../../api/apiService';
import { Assignment } from '../../types';
import styles from './ClassDetailsPage.module.css';
import AssignmentModal from '../../components/common/AssignmentModal/AssignmentModal';
import SubmissionModal from '../../components/common/SubmissionModal/SubmissionModal';
import { useClassDetailsContext } from './ClassDetailsPage';

const AssignmentsTab: React.FC = () => {
  const { classId } = useParams<{ classId: string }>();
  const { user } = useClassDetailsContext();

  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [isCreateModalOpen, setCreateModalOpen] = useState(false);
  const [isSubmitModalOpen, setSubmitModalOpen] = useState(false);
  const [selectedAssignment, setSelectedAssignment] = useState<Assignment | null>(null);

  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [error, setError] = useState('');

  // ⭐ ĐỊNH NGHĨA ROLE CHECK RÕ RÀNG
  const isTeacher = user?.role === 'teacher' || user?.role === 'admin' || user?.role === 'Giáo viên';
  const isStudent = user?.role === 'student';


  const formatDate = (dateString: string) => {
    if (!dateString) return "Chưa cập nhật";
    try {
      let formattedString = dateString;
      if (dateString.length === 10) {
          formattedString = `${dateString}T00:00:00`;
      }
      const date = new Date(formattedString);

      return new Intl.DateTimeFormat('vi-VN', {
        hour: '2-digit',
        minute: '2-digit',
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      }).format(date);
    } catch (e) {
      return dateString;
    }
  };

  const fetchAssignments = () => {
    // ... (Giữ nguyên fetchAssignments) ...
    if(classId) {
        setIsLoading(true);
        setError('');
        api.getAssignments(classId)
          .then(res => {
              const data: any = res.data.data ? res.data.data : res.data;
              setAssignments(Array.isArray(data) ? data : []);
          })
          .catch(err => {
            console.error(api.getErrorMessage(err));
            setError('Không thể tải danh sách bài tập.');
          })
          .finally(() => setIsLoading(false));
      }
  };

  useEffect(() => {
    fetchAssignments();
  }, [classId]);

  const handleCreateAssignment = async (data: any) => {
    if(classId) {
      setIsCreating(true);
      try {
        await api.createAssignment({ ...data, classId: parseInt(classId) });
        alert("Tạo bài tập thành công!");
        setCreateModalOpen(false);
        fetchAssignments();
      } catch (err) {
        alert(`Tạo bài tập thất bại: ${api.getErrorMessage(err)}`);
      } finally {
          setIsCreating(false);
      }
    }
  };

  const handleOpenSubmitModal = (assignment: Assignment) => {
    setSelectedAssignment(assignment);
    setSubmitModalOpen(true);
  };

  const handleConfirmSubmission = async (file: File) => {
    if (!selectedAssignment) return;

    try {
      const uploadRes = await api.uploadFile(file);
      const data: any = uploadRes.data.data ? uploadRes.data.data : uploadRes.data;
      const fileUrl = typeof data === 'string' ? data : data.url;

      if (!fileUrl) {
        throw new Error("Không nhận được đường dẫn file từ máy chủ.");
      }

      await api.submitAssignment(selectedAssignment.id, { fileUrl });

      alert(`Đã nộp bài tập "${selectedAssignment.title}" thành công!`);
      setSubmitModalOpen(false);
    } catch (err) {
      console.error(err);
      alert(`Nộp bài thất bại: ${api.getErrorMessage(err)}`);
    }
  };

  if (isLoading) return <p className={styles.message}>Đang tải bài tập...</p>;
  if (error) return <p className={`${styles.message} ${styles.error}`}>{error}</p>;

  return (
    <>
      {/* Nút tạo bài tập (Sử dụng isTeacher) */}
      {isTeacher && (
        <button
            onClick={() => setCreateModalOpen(true)}
            className={styles.actionButton}
            disabled={isCreating}
        >
            {isCreating ? "Đang xử lý..." : "Giao bài tập mới"}
        </button>
      )}

      <div className={styles.assignmentList}>
        {assignments.length > 0 ? (
          assignments.map((assignment) => (
            <div key={assignment.id} className={styles.assignmentItem}>
              <div>
                <h4 className={styles.assignmentTitle}>{assignment.title}</h4>
                <p className={styles.assignmentDue}>
                    Hạn nộp: <span style={{fontWeight: 'bold', color: '#d32f2f'}}>{formatDate(assignment.dueDate)}</span>
                </p>
              </div>

              {/* Nút nộp bài (Sử dụng isStudent) */}
              {isStudent ? (
                <button onClick={() => handleOpenSubmitModal(assignment)} className={styles.submitButton}>
                    Nộp bài
                </button>
            ) : (
                <span style={{color: '#666', fontSize: '14px', fontStyle: 'italic'}}>
                    (Xem chi tiết nộp bài)
                </span>
            )}
            </div>
          ))
        ) : <p className={styles.message}>Chưa có bài tập nào trong lớp này.</p>}
      </div>

      {/* Modal Tạo bài tập */}
      {classId && (
        <AssignmentModal
            isOpen={isCreateModalOpen}
            onClose={() => setCreateModalOpen(false)}
            onConfirm={handleCreateAssignment}
            classId={parseInt(classId)}
        />
      )}

      {/* Modal Nộp bài */}
      {selectedAssignment && (
        <SubmissionModal
            isOpen={isSubmitModalOpen}
            onClose={() => setSubmitModalOpen(false)}
            onConfirm={handleConfirmSubmission}
            assignmentTitle={selectedAssignment.title}
        />
      )}
    </>
  );
};

export default AssignmentsTab;