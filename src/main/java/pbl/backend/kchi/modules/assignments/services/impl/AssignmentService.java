package pbl.backend.kchi.modules.assignments.services.impl;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pbl.backend.kchi.modules.assignments.entities.Assignments;
import pbl.backend.kchi.modules.assignments.mapper.AssignmentMapper;
import pbl.backend.kchi.modules.assignments.requests.StoreAssignmentRequest;
import pbl.backend.kchi.modules.assignments.requests.UpdateAssignmentRequest;
import pbl.backend.kchi.modules.assignments.services.interfaces.AssignmentServiceInterface;
import pbl.backend.kchi.modules.assignments.repositories.AssignmentRepository;
import pbl.backend.kchi.modules.classes.entities.Classes;
import pbl.backend.kchi.modules.classes.mapper.ClassMapper;
import pbl.backend.kchi.modules.classes.repositories.ClassRepository;
import pbl.backend.kchi.modules.classes.requests.StoreClassRequest;
import pbl.backend.kchi.modules.classes.requests.UpdateClassRequest;
import pbl.backend.kchi.modules.classes.services.interfaces.ClassServiceInterface;
import pbl.backend.kchi.services.BaseService;

import java.util.Map;

@Service
public class AssignmentService extends BaseService<
        Assignments,
        AssignmentMapper,
        StoreAssignmentRequest,
        UpdateAssignmentRequest,
        AssignmentRepository
        > implements AssignmentServiceInterface {

    private final AssignmentMapper assignmentMapper;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private ClassRepository classRepository;

    public AssignmentService(
            AssignmentMapper assignmentMapper
    ){
        this.assignmentMapper =assignmentMapper;
    }

    @Override
    protected String[] getSearchFields(){
        return new String[]{"name"};
    }

    @Override
    protected String[] getRelations(){
        return new String[0];
    }

    @Override
    protected AssignmentRepository getRepository(){
        return assignmentRepository;
    }

    @Override
    protected AssignmentMapper getMapper(){
        return assignmentMapper;
    }

    @Override
    @Transactional // Đảm bảo giao dịch
    public Assignments create(StoreAssignmentRequest request){

        // 1. TÌM KIẾM ĐỐI TƯỢNG CLASSES
        Classes classesEntity = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lớp học với ID: " + request.getClassId()));

        Assignments payload = assignmentMapper.toEntity(request);

        // 3. GÁN ĐỐI TƯỢNG CLASSES VÀO ENTITY
        payload.setClasses(classesEntity);

        // 4. LƯU ENTITY VÀO DB
        Assignments entity = assignmentRepository.save(payload);


        return entity;
    }

}