package pbl.backend.kchi.databases.seeder;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowire;

import pbl.backend.kchi.modules.users.entities.User;
import pbl.backend.kchi.modules.users.repositories.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DatabaseSeeder implements CommandLineRunner{

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    @Override
    public void run(String... args) throws Exception {
//        if(isTableEmpty()) {
//
//            String passwordEncode = passwordEncoder.encode("password");
//
//            User user = new User("toan", "tuitentoan3004@gmai.com", passwordEncode,1L,"012345678");
//            userRepository.save(user);
//            logger.info("Seeding user data");
//
//
//            System.out.println("password: " + passwordEncode);
//        }

    }

    private boolean isTableEmpty() {
        Long count = (Long) entityManager.createQuery("SELECT COUNT(id) FROM User").getSingleResult();
        return count == 0;
    }
}