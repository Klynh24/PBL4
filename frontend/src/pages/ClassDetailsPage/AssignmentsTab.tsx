import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
// Import API và Types (Dùng ../ vì file này nằm trong src/pages)
import * as api from '../../api/apiService';
import { Assignment } from '../../types';
// Import CSS cùng thư mục
import styles from './ClassDetailsPage.module.css'; // SỬ DỤNG CSS CHUNG

// Import Components Modal
import AssignmentModal from '../../components/common/AssignmentModal/AssignmentModal';
import SubmissionModal from '../../components/common/SubmissionModal/SubmissionModal';

// Import hook context từ file cùng cấp
import { useClassDetailsContext } from './ClassDetailsPage';

const AssignmentsTab: React.FC = () => {
  const { classId } = useParams<{ classId: string }>();
  const { user } = useClassDetailsContext(); // Lấy user từ context

  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [isCreateModalOpen, setCreateModalOpen] = useState(false);
  const [isSubmitModalOpen, setSubmitModalOpen] = useState(false);
  const [selectedAssignment, setSelectedAssignment] = useState<Assignment | null>(null);

  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [error, setError] = useState('');

  // Đã sửa lỗi: Chuẩn hóa về chữ thường để tránh lỗi Case Sensitivity
  const isTeacher = user?.role?.toLowerCase() === 'teacher';
  const isStudent = user?.role?.toLowerCase() === 'student';

  // --- HÀM FORMAT NGÀY GIỜ ---
  const formatDate = (dateString: string) => {
    if (!dateString) return "Chưa cập nhật";
    try {
      let formattedString = dateString;
      // Đảm bảo có thông tin giờ phút nếu chuỗi chỉ có ngày
      if (dateString.length === 10) {
          formattedString = `${dateString}T00:00:00`;
      }
      const date = new Date(formattedString);

      return new Intl.DateTimeFormat('vi-VN', {
        hour: '2-digit', minute: '2-digit', day: '2-digit',
        month: '2-digit', year: 'numeric',
      }).format(date);
    } catch (e) {
      // Trường hợp lỗi parse, trả về chuỗi gốc
      return dateString;
    }
  };

  // --- HÀM TẢI DANH SÁCH BÀI TẬP (ĐÃ SỬA LỖI BUILD TS2339) ---
  const fetchAssignments = () => {
    if(classId) {
      setIsLoading(true);
      setError('');

      api.getAssignments(classId)
        .then(res => {
            let assignmentsData: Assignment[] = [];
            const responseData = res.data;

            // ⭐ ĐOẠN CODE ĐÃ SỬA LỖI TS2339 BẰNG CÁCH SỬ DỤNG ASSERTION (AS) ⭐
            // Đặt responseData.data vào một biến mới với kiểu any để truy cập các thuộc tính pagination
            const apiData: any = responseData.data;

            if (Array.isArray(responseData)) {
                // Trường hợp 1: API trả về mảng trực tiếp
                assignmentsData = responseData as Assignment[];
            } else if (apiData) {
                // Trường hợp 2: API trả về đối tượng có field 'data'
                if (Array.isArray(apiData)) {
                     // 2a: Nếu res.data.data là mảng
                    assignmentsData = apiData as Assignment[];
                } else if (apiData.content || apiData.list) {
                     // 2b: Nếu res.data.data là đối tượng phân trang (có content/list)
                     const paginatedContent = apiData.content || apiData.list;
                     if(Array.isArray(paginatedContent)) {
                        assignmentsData = paginatedContent as Assignment[];
                     }
                } else {
                    // Trường hợp 2c: Nếu res.data.data là một đối tượng chứa dữ liệu chính (nhưng không phải phân trang)
                    // (Ít phổ biến, nhưng để dự phòng)
                     assignmentsData = Array.isArray(apiData) ? apiData as Assignment[] : [];
                }
            } else if (Array.isArray(responseData)) {
                // Trường hợp 4: (Dự phòng) Nếu responseData là mảng
                assignmentsData = responseData as Assignment[];
            }

            // Cuối cùng, đảm bảo state luôn là một mảng Assignment[]
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
  // --- HẾT SỬA LỖI ---

  useEffect(() => {
    // Luôn cố gắng tải bài tập khi classId thay đổi
    if (classId) {
      fetchAssignments();
    }
  }, [classId]);

  // --- XỬ LÝ TẠO BÀI TẬP ---
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

  // --- XỬ LÝ NỘP BÀI (Giữ nguyên) ---
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

  // Nếu đang tải LẦN ĐẦU và chưa có dữ liệu nào, hiển thị thông báo tải
  if (isLoading && assignments.length === 0 && !error) {
      return <p className={styles.message}>Đang tải bài tập...</p>;
  }

  return (
    <>
      {/* Nút tạo bài tập (Luôn hiển thị cho Giáo viên) */}
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

        {/* HIỂN THỊ LỖI KHI TẢI THẤT BẠI LẦN ĐẦU */}
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
             // HIỂN THỊ KHI TẢI XONG MÀ DANH SÁCH RỖNG
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