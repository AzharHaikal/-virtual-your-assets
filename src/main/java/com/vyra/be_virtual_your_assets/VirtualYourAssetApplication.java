package com.vyra.be_virtual_your_assets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@EnableAsync
@SpringBootApplication
public class VirtualYourAssetApplication {

	public static void main(String[] args) {
		// Every LocalDateTime in this application is UTC (see TimeService), whatever the host's zone.
		// Set before Spring starts so JDBC sessions and Hibernate use the same zone.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(VirtualYourAssetApplication.class, args);
	}

}
