import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import * as api from '../../api/apiService';
import { Assignment, SubmissionData } from '../../types'; // ⭐ Import SubmissionData
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

  const isTeacher = user?.role?.toLowerCase() === 'teacher' || user?.role?.toLowerCase() === 'admin';
  const isStudent = user?.role?.toLowerCase() === 'student';


  // --- HÀM FORMAT NGÀY GIỜ (Giữ nguyên) ---
  const formatDate = (dateString: string) => {
    if (!dateString) return "Chưa cập nhật";
    try {
      let formattedString = dateString;
      if (dateString.length === 10) {
          formattedString = `${dateString}T00:00:00`;
      }
      const date = new Date(formattedString);

      return new Intl.DateTimeFormat('vi-VN', {
        hour: '2-digit', minute: '2-digit', day: '2-digit',
        month: '2-digit', year: 'numeric',
      }).format(date);
    } catch (e) {
      return dateString;
    }
  };

  // --- HÀM TẢI DANH SÁCH BÀI TẬP (Giữ nguyên logic bóc tách data) ---
  const fetchAssignments = () => {
    if(classId) {
      setIsLoading(true);
      setError('');

      api.getAssignments(classId)
        .then(res => {
            let assignmentsData: Assignment[] = [];
            const responseData = res.data;

            const apiData: any = responseData.data;

            if (Array.isArray(responseData)) {
                assignmentsData = responseData as Assignment[];
            } else if (apiData) {
                if (Array.isArray(apiData)) {
                    assignmentsData = apiData as Assignment[];
                } else if (apiData.content || apiData.list) {
                     const paginatedContent = apiData.content || apiData.list;
                     if(Array.isArray(paginatedContent)) {
                        assignmentsData = paginatedContent as Assignment[];
                     }
                } else {
                     assignmentsData = Array.isArray(apiData) ? apiData as Assignment[] : [];
                }
            } else if (Array.isArray(responseData)) {
                assignmentsData = responseData as Assignment[];
            }

            setAssignments(assignmentsData);
            setError('');
        })
        .catch(err => {
          console.error('Lỗi tải bài tập:', api.getErrorMessage(err));
          if (assignments.length === 0) {
              setError('Không thể tải danh sách bài tập. Vui lòng thử lại.');
          }
        })
        .finally(() => setIsLoading(false));
    }
  };

  useEffect(() => {
    if (classId) {
      fetchAssignments();
    }
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
        alert(`Tạo bài tập thất bại: ${api.getErrorMessage(err)}. Vui lòng kiểm tra log backend.`);
      } finally {
        setIsCreating(false);
      }
    }
  };

  const handleOpenSubmitModal = (assignment: Assignment) => {
    setSelectedAssignment(assignment);
    setSubmitModalOpen(true);
  };

  // --- XỬ LÝ NỘP BÀI (FINAL FIX) ---
  const handleConfirmSubmission = async (file: File) => {
    if (!selectedAssignment) return;
    try {
      // 1. Upload file
      const uploadRes = await api.uploadFile(file);
      const data: any = uploadRes.data.data ? uploadRes.data.data : uploadRes.data;
      const fileUrl = typeof data === 'string' ? data : data.url;

      if (!fileUrl) {
        throw new Error("Không nhận được đường dẫn file từ máy chủ.");
      }

      // ⭐ 2. GỌI API SUBMISSION VỚI 1 ARGUMENT (Payload) ⭐
      const submissionPayload: SubmissionData = {
          assignmentId: selectedAssignment.id, // ID bài tập vào Payload
          fileUrl: fileUrl,
      };

      // LỖI CŨ: await api.submitAssignment(selectedAssignment.id, { fileUrl });
      await api.submitAssignment(submissionPayload); // FIX!
      // ----------------------------------------------------

      alert(`Đã nộp bài tập "${selectedAssignment.title}" thành công!`);
      setSubmitModalOpen(false);
    } catch (err) {
      console.error(err);
      alert(`Nộp bài thất bại: ${api.getErrorMessage(err)}`);
    }
  };

  if (isLoading && assignments.length === 0 && !error) {
      return <p className={styles.message}>Đang tải bài tập...</p>;
  }

  return (
    <>
      {/* Nút tạo bài tập */}
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

        {error && assignments.length === 0 ? (
            <p className={`${styles.message} ${styles.error}`}>
                {error}
                <button
                    onClick={fetchAssignments}
                    className={styles.retryButton}
                    style={{marginLeft: '10px'}}
                >
                    Thử lại
                </button>
            </p>
        ) : assignments.length > 0 ? (
            // HIỂN THỊ DANH SÁCH BÀI TẬP
            assignments.map((assignment) => (
                <div key={assignment.id} className={styles.assignmentItem}>
                    <div>
                        <h4 className={styles.assignmentTitle}>{assignment.title}</h4>
                        <p className={styles.assignmentDue}>
                            Hạn nộp: <span style={{fontWeight: 'bold', color: '#d32f2f'}}>{formatDate(assignment.dueDate)}</span>
                        </p>
                    </div>

                    {/* Nút nộp bài (Chỉ hiển thị cho Sinh viên) */}
                    {isStudent ? (
                        <button onClick={() => handleOpenSubmitModal(assignment)} className={styles.submitButton}>
                            Nộp bài
                        </button>
                    ) : (
                        <span style={{color: '#666', fontSize: '14px', fontStyle: 'italic'}}>
                            {isTeacher ? "(Quản lý nộp bài)" : "(Xem chi tiết)"}
                        </span>
                    )}
                </div>
            ))
        ) : (
            <p className={styles.message}>Chưa có bài tập nào trong lớp này.</p>
        )}
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