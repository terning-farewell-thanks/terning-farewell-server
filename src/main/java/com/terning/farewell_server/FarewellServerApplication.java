package com.terning.farewell_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class FarewellServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(FarewellServerApplication.class, args);
	}
}
