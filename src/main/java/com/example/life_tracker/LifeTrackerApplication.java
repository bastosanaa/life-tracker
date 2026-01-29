package com.example.life_tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LifeTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(LifeTrackerApplication.class, args);
	}

}
