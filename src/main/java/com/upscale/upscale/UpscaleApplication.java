package com.upscale.upscale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UpscaleApplication {

	public static void main(String[] args) {
		SpringApplication.run(UpscaleApplication.class, args);
	}

}
