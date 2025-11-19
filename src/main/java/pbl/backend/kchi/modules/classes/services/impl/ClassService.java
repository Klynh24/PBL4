package pbl.backend.kchi.modules.classes.services.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pbl.backend.kchi.modules.classes.entities.Classes;
import pbl.backend.kchi.modules.classes.mapper.ClassMapper;
import pbl.backend.kchi.modules.classes.repositories.ClassRepository;
import pbl.backend.kchi.modules.classes.requests.StoreClassRequest;
import pbl.backend.kchi.modules.classes.requests.UpdateClassRequest;
import pbl.backend.kchi.modules.classes.services.interfaces.ClassServiceInterface;
import pbl.backend.kchi.modules.users.entities.User;
import pbl.backend.kchi.modules.users.mappers.UserMapper;
import pbl.backend.kchi.modules.users.repositories.UserRepository;
import pbl.backend.kchi.modules.users.resources.CustomUserDetail;
import pbl.backend.kchi.modules.users.resources.UserResource;
import pbl.backend.kchi.services.BaseService;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClassService extends BaseService<
        Classes,
        ClassMapper,
        StoreClassRequest,
        UpdateClassRequest,
        ClassRepository
        > implements ClassServiceInterface {

    private final ClassMapper classMapper;

    @Autowired
    private UserMapper userMapper;

    @Transactional(readOnly = true)
    @Override
    public List<UserResource> getClassMembers(Long classId) {
        Classes classes = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Lớp học không tồn tại"));


        return classes.getMembers().stream()
                .map(userMapper::tResource)
                .collect(Collectors.toList());
    }


    private static final Logger logger = LoggerFactory.getLogger(ClassService.class);

    @Autowired
    private HttpServletRequest httpRequest; // for debugging header (can remove later)


    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private UserRepository userRepository;

    public ClassService(ClassMapper classMapper) {
        this.classMapper = classMapper;
    }


    @Transactional
    @Override
    public void joinClass(String code) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetail userDetails = (CustomUserDetail) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();

        User student = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        Classes classToJoin = classRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Mã lớp không hợp lệ hoặc lớp không tồn tại"));

        boolean isAlreadyMember = classToJoin.getMembers().stream()
                .anyMatch(m -> m.getId().equals(currentUserId));

        if (isAlreadyMember) {
            throw new RuntimeException("Bạn đã là thành viên của lớp này rồi");
        }

        classToJoin.getMembers().add(student);

        classRepository.save(classToJoin);
    }

    @Override
    @Transactional
    public Classes create(StoreClassRequest request) {
        Classes newClass = classMapper.toEntity(request);

        logger.info("Creating class - request payload: {}", request);
        // log incoming Authorization header
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            logger.info("Incoming Authorization header: {}", authHeader);
        } catch (Exception e) {
            logger.warn("Cannot read HttpServletRequest header: {}", e.getMessage());
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetail) {
            CustomUserDetail userDetails = (CustomUserDetail) authentication.getPrincipal();
            Long currentUserId = userDetails.getId();

            User teacher = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên (User ID: " + currentUserId + ")"));

            newClass.setUser(teacher);
        }

        return classRepository.save(newClass);
    }

    @Override
    public Page<Classes> paginate(Map<String, String[]> params, HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = null;

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetail) {
            CustomUserDetail userDetails = (CustomUserDetail) authentication.getPrincipal();
            currentUserId = userDetails.getId();
        }

        if (currentUserId == null) {
            throw new RuntimeException("Không xác định được người dùng hiện tại");
        }


        int page = 0;
        int size = 100;

        if(params.containsKey("page")) page = Integer.parseInt(params.get("page")[0]) - 1;
        if(params.containsKey("limit")) size = Integer.parseInt(params.get("limit")[0]);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());


        return classRepository.findByUserId(currentUserId, pageable);
    }

    @Override
    protected String[] getSearchFields(){
        return new String[]{"name"};
    }

    @Override
    protected String[] getRelations(){
        return new String[]{};
    }

    @Override
    protected ClassRepository getRepository(){
        return classRepository;
    }

    @Override
    protected ClassMapper getMapper(){
        return classMapper;
    }
}