package com.sapereapi.lightserver;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.ServerConfig;
import com.sapereapi.util.SapereUtil;

import eu.sapere.middleware.node.NodeLocation;
import eu.sapere.middleware.node.NodeManager;

public class LightServer {
	static SapereLogger logger = null;
	static LightHTTPServer server = null;
	static ServerConfig serverConfig = null;


	public static void main(String[] args) throws IOException {
		File logDirectory = new File("log");
	    if (! logDirectory.exists()){
			logDirectory.mkdir();
	    }
	    File dbDirectory = new File("db");
	    if (! dbDirectory.exists()){
			dbDirectory.mkdir();
	    }
		//Properties prop = SapereUtil.loadProperties(propFileName);
		//prop = SapereUtil.addParamProperties(prop, args);
		serverConfig = SapereUtil.initServerConfig(args);
		try {
			initSapere();
		} catch (Exception e) {
			logger.error(e);
		}
		logger.info("LightServer.main : args=" + Arrays.asList(args));

		// dependency to add in pom.xml : com.sun.net.httpserver
		//server = HttpServer.create(new InetSocketAddress(nodeLocation.getRestPort()), 0);
		server = new LightHTTPServer(serverConfig, logger);
	}

	private static void initSapere() throws HandlingException {
		// TODO : set a default NodeContext value to Sapere instance
		//logger = SapereLogger.getInstance();
		NodeManager.setConfiguration(serverConfig.getNodeLocation());
		logger = SapereLogger.getInstance();
		Sapere.getInstance();
		Sapere.init(serverConfig);
	}

	public static NodeLocation getNodeLocation() {
		return serverConfig.getNodeLocation();
	}
}
