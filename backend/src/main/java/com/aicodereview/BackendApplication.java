package com.aicodereview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BackendApplication {

	public static void main(String[] args) {
        // TEST REVIEW COMMENT: Starting the backend application
        String debugPassword = "admin123"; // intentional code smell

		SpringApplication.run(BackendApplication.class, args);
	}

}
