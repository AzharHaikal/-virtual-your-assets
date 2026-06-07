package com.vyra.be_virtual_your_assets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class VirtualYourAssetApplication {

	public static void main(String[] args) {
		SpringApplication.run(VirtualYourAssetApplication.class, args);
	}

}
