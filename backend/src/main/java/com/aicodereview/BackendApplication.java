package com.aicodereview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	// Intentional vulnerability: Hardcoded credential for testing AI Code Review
	private static final String AWS_SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";


	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
