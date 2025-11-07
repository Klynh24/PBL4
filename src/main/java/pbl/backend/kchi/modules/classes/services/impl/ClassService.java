package pbl.backend.kchi.modules.classes.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pbl.backend.kchi.modules.classes.entities.Classes;
import pbl.backend.kchi.modules.classes.repositories.ClassRepository;
import pbl.backend.kchi.modules.classes.requests.StoreClassRequest;
import pbl.backend.kchi.modules.classes.requests.UpdateClassRequest;
import pbl.backend.kchi.modules.classes.services.interfaces.ClassServiceInterface;
import pbl.backend.kchi.modules.users.entities.User;
import pbl.backend.kchi.modules.users.repositories.UserRepository;

import pbl.backend.kchi.services.BaseService;

import javax.persistence.EntityNotFoundException;
import java.util.Map;

@Service
public class ClassService extends BaseService implements ClassServiceInterface {
    private static final Logger logger = LoggerFactory.getLogger(ClassService.class);

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public Classes create(Long userId, StoreClassRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Bạn phải đăng nhập để tạo lớp"));
        Long userid = user.getId();
            try {

                Classes payload = Classes.builder()
                        .name(request.getName())
                        .description(request.getDescription())
                        .userid(userid)
                        .build();
                return classRepository.save(payload);
            } catch (Exception e) {
                throw new RuntimeException("Transaction failed" + e.getMessage());
            }
        }

    @Override
    @Transactional
    public Classes update(Long id, UpdateClassRequest request) {
        Classes classes = classRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lớp học không tồn tại"));
        Classes payload = classes.toBuilder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return classRepository.save(payload);
    }
    @Override
    public Page<Classes> paginate(Map<String, String[]> parameters) {
        int page = parameters.containsKey("page") ? Integer.parseInt(parameters.get("page")[0]) : 1;
        int perpage = parameters.containsKey("perpage") ? Integer.parseInt(parameters.get("perpage")[0]) : 20;
        String sortParam = parameters.containsKey("sort") ? parameters.get("sort")[0] : null;
        Sort sort = createSort(sortParam);
        Pageable pageable = PageRequest.of(page - 1, perpage, sort);
        return classRepository.findAll(pageable);

    }

}
