package pbl.backend.kchi.databases.seeder;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pbl.backend.kchi.modules.classes.entities.Classes;
import pbl.backend.kchi.modules.classes.repositories.ClassRepository;
import pbl.backend.kchi.modules.users.entities.User;
import pbl.backend.kchi.modules.users.entities.UserCatalogue;
import pbl.backend.kchi.modules.users.repositories.UserCataloguesRespository;
import pbl.backend.kchi.modules.users.repositories.UserRepository;

@Component
@DependsOn("flyway") // Đảm bảo chỉ chạy sau khi Flyway tạo bảng
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClassRepository classRepository;

    // ### SỬA LỖI: Sử dụng đúng tên class/biến mà bạn đã tạo ###
    @Autowired
    private UserCataloguesRespository userCataloguesRespository; 

    @Transactional
    @Override
    public void run(String... args) throws Exception {
        if (isDataSeeded()) {
            logger.info("Database already seeded. Skipping...");
            return;
        }

        logger.info("Starting database seeding...");

        // 1. Tạo 3 "Vai trò" (UserCatalogues)
        UserCatalogue adminCatalogue = new UserCatalogue();
        adminCatalogue.setName("ROLE_ADMIN"); 
        adminCatalogue.setPublish(1); 
        // ### SỬA LỖI: Dùng đúng tên biến ###
        userCataloguesRespository.save(adminCatalogue);

        UserCatalogue teacherCatalogue = new UserCatalogue();
        teacherCatalogue.setName("ROLE_TEACHER");
        teacherCatalogue.setPublish(1);
        // ### SỬA LỖI: Dùng đúng tên biến ###
        userCataloguesRespository.save(teacherCatalogue);

        UserCatalogue studentCatalogue = new UserCatalogue();
        studentCatalogue.setName("ROLE_STUDENT");
        studentCatalogue.setPublish(1);
        // ### SỬA LỖI: Dùng đúng tên biến ###
        userCataloguesRespository.save(studentCatalogue);

        logger.info("Seeded 3 UserCatalogues (Roles)");

        // 2. Tạo User Admin
        String adminPassword = passwordEncoder.encode("password"); // Mật khẩu mặc định là "password"
        
        User adminUser = new User();
        adminUser.setName("Administrator"); 
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(adminPassword);
        adminUser.setPhone("0123456789");
        
        // Gán vai trò "ROLE_ADMIN" cho user này
        adminUser.setUserCatalogues(Set.of(adminCatalogue)); 

        userRepository.save(adminUser);
        logger.info("Seeded admin user with password 'password'");

        // 3. Tạo một Lớp học (Class) mẫu do Admin tạo
        Classes demoClass = new Classes();
        demoClass.setName("Lớp học Demo"); 
        demoClass.setDescription("Đây là lớp học demo đầu tiên.");
        demoClass.setUser(adminUser); // Gán adminUser làm người tạo/giáo viên của lớp

        classRepository.save(demoClass);
        logger.info("Seeded demo class");

        logger.info("Database seeding finished.");
    }

    /**
     * Kiểm tra xem data đã được seed hay chưa (bằng cách kiểm tra bảng User)
     */
    private boolean isDataSeeded() {
        // "User" là tên của @Entity class, không phải tên bảng SQL
        Long userCount = (Long) entityManager.createQuery("SELECT COUNT(id) FROM User").getSingleResult();
        return userCount > 0;
    }
}