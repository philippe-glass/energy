package com.sapereapi.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.ServerConfig;
import com.sapereapi.util.SapereUtil;

import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.node.NodeManager;

@SpringBootApplication(scanBasePackages = { "com.sapereapi.api", "com.sapereapi.entity", "com.sapereapi.model" })
public class SapereAPIApplication {

	@Autowired
	private static Environment environment;
	private static ServerConfig serverConfig;
	private static AbstractLogger logger = SapereLogger.getInstance();

	public static void main(String[] args) {
		// System.setProperty("spring.config.name", "my-project");
		if (args.length > 0) {
			String env = args[0];
			System.setProperty("spring.profiles.active", env);
		}
		serverConfig = SapereUtil.initServerConfig(args);
		NodeManager.setConfiguration(serverConfig.getNodeLocation());
		Sapere.getInstance();
		try {
			Sapere.init(serverConfig);
		} catch (Exception e) {
			logger.error(e);
		}
		SpringApplication.run(SapereAPIApplication.class, args);
		logger.info("environment = " + environment);
	}

	public static ServerConfig getServerConfig() {
		return serverConfig;
	}

	public static void setServerConfig(ServerConfig serverConfig) {
		SapereAPIApplication.serverConfig = serverConfig;
	}

}
