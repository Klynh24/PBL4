package pbl.backend.kchi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = "pbl.backend.kchi")
public class KchiApplication {

    public static void main(String[] args) {
        SpringApplication.run(KchiApplication.class, args);
    }
}