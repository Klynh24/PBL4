package pbl.backend.kchi.modules.assignments.services.impl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pbl.backend.kchi.modules.assignments.entities.AssignmentSubmitteds;
import pbl.backend.kchi.modules.assignments.repositories.SubmittedRepository;
import pbl.backend.kchi.modules.assignments.requests.assignmentSubmissions.StoreSubmittedRequest;
import pbl.backend.kchi.modules.assignments.services.interfaces.SubmittedServiceInterface;
import pbl.backend.kchi.modules.users.entities.User;
import pbl.backend.kchi.modules.users.repositories.UserRepository;
import pbl.backend.kchi.services.BaseService;

import javax.persistence.EntityNotFoundException;

@Service
public class SubmittedService extends BaseService implements SubmittedServiceInterface {
    private static final Logger logger = LoggerFactory.getLogger(SubmittedService.class);

    @Autowired
    private SubmittedRepository submittedRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public AssignmentSubmitteds create(Long userId, StoreSubmittedRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Bạn phải đăng nhập để tạo lớp"));
        Long userid = user.getId();
        try {

            AssignmentSubmitteds payload = AssignmentSubmitteds.builder()
                    .fileUrl(request.getFileUrl())
                    .score(request.getScore())
                    .userId(userid)
                    .assignmentId(request.getAssignmentId())
                    .build();
            return submittedRepository.save(payload);
        } catch (Exception e) {
            throw new RuntimeException("Transaction failed" + e.getMessage());
        }
    }

}
