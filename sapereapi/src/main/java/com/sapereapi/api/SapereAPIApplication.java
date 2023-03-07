package com.sapereapi.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication(scanBasePackages={ "com.sapereapi.api" ,
		"com.sapereapi.entity", "com.sapereapi.model"})
public class SapereAPIApplication {

	 @Autowired
	 private static Environment environment;

	public static void main(String[] args) {
		//System.setProperty("spring.config.name", "my-project");
		if(args.length>0) {
			String env = args[0];
			System.setProperty("spring.profiles.active", env);
		}
		SpringApplication.run(SapereAPIApplication.class, args);
		System.out.print("environment = " + environment);
	}
}
