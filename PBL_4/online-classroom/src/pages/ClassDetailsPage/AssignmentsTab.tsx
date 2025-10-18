import React, { useState, useEffect } from 'react';
import { useParams, useOutletContext } from 'react-router-dom';
import * as api from '../../api/apiService';
import { User } from '../../contexts/AuthContext';
import styles from '../ClassDetailsPage.module.css';
import AssignmentModal from '../../components/common/AssignmentModal/AssignmentModal';
import SubmissionModal from '../../components/common/SubmissionModal/SubmissionModal';

interface Assignment { id: number; title: string; dueDate: string; }

const AssignmentsTab: React.FC = () => {
  const { classId } = useParams<{ classId: string }>();
  const { user } = useOutletContext<{ user: User | null }>();
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [isCreateModalOpen, setCreateModalOpen] = useState(false);
  const [isSubmitModalOpen, setSubmitModalOpen] = useState(false);
  const [selectedAssignment, setSelectedAssignment] = useState<Assignment | null>(null);

  const fetchAssignments = () => { if(classId) api.getAssignments(classId).then(res => setAssignments(res.data)); };
  useEffect(fetchAssignments, [classId]);

  const handleCreateAssignment = async (data: any) => { if(classId) { await api.createAssignment({ ...data, classId: parseInt(classId) }); fetchAssignments(); }};
  const handleOpenSubmitModal = (assignment: Assignment) => { setSelectedAssignment(assignment); setSubmitModalOpen(true); };
  const handleConfirmSubmission = async (file: File) => { if (!selectedAssignment) return; await api.submitAssignment(selectedAssignment.id, { fileUrl: file.name }); alert(`Đã nộp bài tập "${selectedAssignment.title}" thành công!`); };

  return (
    <>
      {user?.role === 'teacher' && <button onClick={() => setCreateModalOpen(true)} className={styles.actionButton}>Giao bài tập mới</button>}
      <div className={styles.assignmentList}>
        {assignments.length > 0 ? (
          assignments.map((assignment) => (
            <div key={assignment.id} className={styles.assignmentItem}>
              <div>
                <h4 className={styles.assignmentTitle}>{assignment.title}</h4>
                <p className={styles.assignmentDue}>Hạn nộp: {assignment.dueDate}</p>
              </div>
              {user?.role === 'student' ? <button onClick={() => handleOpenSubmitModal(assignment)} className={styles.submitButton}>Nộp bài</button> : <span>Đã nộp: 0</span>}
            </div>
          ))
        ) : <p className={styles.message}>Chưa có bài tập nào trong lớp này.</p>}
      </div>
      {classId && <AssignmentModal isOpen={isCreateModalOpen} onClose={() => setCreateModalOpen(false)} onConfirm={handleCreateAssignment} classId={parseInt(classId)} />}
      {selectedAssignment && <SubmissionModal isOpen={isSubmitModalOpen} onClose={() => setSubmitModalOpen(false)} onConfirm={handleConfirmSubmission} assignmentTitle={selectedAssignment.title} />}
    </>
  );
};

export default AssignmentsTab;