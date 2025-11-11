package pbl.backend.kchi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KchiApplication {

	public static void main(String[] args) {

        SpringApplication.run(KchiApplication.class, args);
	}

}