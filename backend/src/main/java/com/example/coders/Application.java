package com.example.coders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.coders")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
