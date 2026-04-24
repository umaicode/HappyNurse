package com.ssafy.happynurse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HappynurseApplication {

	public static void main(String[] args) {
		SpringApplication.run(HappynurseApplication.class, args);
	}

}
