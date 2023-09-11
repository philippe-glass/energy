package com.sapereapi.lightserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sapereapi.db.DBConfig;
import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.db.PredictionDbHelper;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.ServerConfig;

import eu.sapere.middleware.node.NodeConfig;

public class LightServer {
	//static NodeConfig nodeConfig = null;//new NodeConfig("N1", "localhost", 10001, 9191);
	static DBConfig dbConfig = null;//("jdbc:mariadb://localhost/energy1", "learning_agent", "sql2537");
	static SapereLogger logger = null;
	static LightHTTPServer server = null;
	static ServerConfig serverConfig = null;
	private static Pattern patternOption = Pattern.compile("^-(?<param>[0-9a-zA-Z_.]+):(?<value>[0-9a-zA-Z_.:;\\-/,]+)$");

	private static NodeConfig loadNodeConfig(Properties prop) {
		String nodeName = prop.getProperty("lsa_server.name");
		String host = prop.getProperty("lsa_server.host");
		String sMainPort = prop.getProperty("lsa_server.port");
		Integer mainPort = Integer.valueOf(sMainPort);
		String sRestport = prop.getProperty("server.port");
		Integer restPort = Integer.valueOf(sRestport);
		NodeConfig result = new NodeConfig(nodeName, host, mainPort, restPort);
		return result;
	}

	private static List<NodeConfig> loadDefaultNeighbours(Properties prop) {
		List<NodeConfig> defaultNeighbours = new ArrayList<>();
		if (prop.containsKey("lsa.server.neighbours")) {
			String sDefaultNeighbours = prop.getProperty("lsa.server.neighbours");
			String[] listNodeConfig = sDefaultNeighbours.split(",");
			for(String sNextNodeConfig : listNodeConfig) {
				String[] nextNodeConfig2 = sNextNodeConfig.split(":");
				if(nextNodeConfig2.length == 4) {
					String node = nextNodeConfig2[0];
					String host = nextNodeConfig2[1];
					Integer post = Integer.valueOf(nextNodeConfig2[2]);
					Integer restPort = Integer.valueOf(nextNodeConfig2[3]);
					NodeConfig nextnodeConfig = new NodeConfig(node, host, post, restPort);
					defaultNeighbours.add(nextnodeConfig);
				}
			}
		}
		return defaultNeighbours;
	}

	private static DBConfig loadDBConfig(Properties prop) {
		String driverClassName = prop.getProperty("spring.datasource.driver-class-name");
		String url = prop.getProperty("spring.datasource.url");
		String username = prop.getProperty("spring.datasource.username");
		String password = prop.getProperty("spring.datasource.password");
		//DBConfig dbConfig = new DBConfig("jdbc:mariadb://localhost/energy1", "learning_agent", "sql2537");
		DBConfig dbConfig = new DBConfig(driverClassName, url, username, password);
		return dbConfig;
	}

	private static String loadRessourceContent(String propFileName) throws IOException {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		InputStream stream = loader.getResourceAsStream(propFileName);
		if(stream == null) {
			stream = loader.getResourceAsStream("resources/"+propFileName);
			System.out.println("loadProperties : step2 : stream = " + stream);
		}
		if(stream == null) {
			System.out.println("### resource " +  propFileName + " not loaded");
		}
		StringBuilder textBuilder = new StringBuilder();
		try(Reader reader = new BufferedReader(new InputStreamReader(stream))) {
	        int c = 0;
	        while ((c = reader.read()) != -1) {
	            textBuilder.append((char) c);
	        }
		} catch (IOException e) {
			throw e;
		}
	    return textBuilder.toString();
	}


	private static Properties addParamProperties(Properties initialProperties, String args[]) {
		Properties prop = initialProperties;
		for (String arg : args) {
			Matcher matcher = patternOption.matcher(arg);
			if (matcher.find()) {
				//int groupCouht = matcher.groupCount();
				String param = matcher.group("param");
				String value = matcher.group("value");
				prop.put(param, value);
			}
		}
		return prop;
	}

	private static Properties loadProperties(String propFileName) throws IOException {
		Properties prop = new Properties();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		//InputStream stream = loader.getResourceAsStream("src\\main\\resources\\" + propFileName);		
		InputStream stream = loader.getResourceAsStream(propFileName);
		if(stream == null) {
			stream = loader.getResourceAsStream("resources/"+propFileName);
			System.out.println("loadProperties : step2 : stream = " + stream);
		}
		if(stream == null) {
			System.out.println("### resource " +  propFileName + " not loaded");
		}
		prop.load(stream);
		System.out.println("loadProperties : loaded properties = " + prop);
		return prop;
	}

	public static void main(String[] args) throws IOException {
		System.out.println("LightServer.main : args=" + Arrays.asList(args));
		serverConfig = new ServerConfig();
		String propFileName = "application.properties"; 
		if(args.length>0) {
			String env = args[0];
			propFileName = "application-" + env + ".properties";
			System.setProperty("spring.profiles.active", env);
		}
		File logDirectory = new File("log");
	    if (! logDirectory.exists()){
			logDirectory.mkdir();
	    }
	    File dbDirectory = new File("db");
	    if (! dbDirectory.exists()){
			dbDirectory.mkdir();
	    }
		Properties prop = loadProperties(propFileName);
		prop = addParamProperties(prop, args);
		NodeConfig nodeConfig = loadNodeConfig(prop);
		serverConfig.setNodeConfig(nodeConfig);
		List<NodeConfig> defaultNeighbours = loadDefaultNeighbours(prop);
		serverConfig.setDefaultNeighbours(defaultNeighbours);
		dbConfig = loadDBConfig(prop);
		if(prop.containsKey("init_script")) {
			String initSqlScript = prop.getProperty("init_script");
			serverConfig.setInitSqlScript(initSqlScript);
		}
		boolean modeAuto = false;
		if(prop.containsKey("mode_auto")) {
			String sModeAuto = prop.getProperty("mode_auto");
			modeAuto = ("1".equalsIgnoreCase(sModeAuto) || "true".equalsIgnoreCase(sModeAuto));
		}
		serverConfig.setModeAuto(modeAuto);
		if(prop.containsKey("csv_file")) {
			String csvFile = prop.getProperty("csv_file");
			serverConfig.setCsvFile(csvFile);

		}
		if(prop.containsKey("url_forcasting")) {
			String urlForcasting = prop.getProperty("url_forcasting");
			serverConfig.setUrlForcasting(urlForcasting);
		}
		initSapereService();

		logger.info("LightServer.main : args=" + Arrays.asList(args));
		// Just for debug :
		if (prop.containsKey("lsa.server.neighbours")) {
			logger.info("loadDefaultNeighbours : lsa.server.neighbours = " + prop.getProperty("lsa.server.neighbours"));
		}

		// dependency to add in pom.xml : com.sun.net.httpserver
		//server = HttpServer.create(new InetSocketAddress(nodeConfig.getRestPort()), 0);
		server = new LightHTTPServer(serverConfig, logger);
	}

	static void initSapereService() {
		Sapere.setLocation(serverConfig.getNodeConfig());
		logger = SapereLogger.getInstance();
		EnergyDbHelper.init(dbConfig);
		PredictionDbHelper.init(dbConfig);
		if(serverConfig.getInitSqlScript() != null) {
			try {
				String scriptContent = loadRessourceContent(serverConfig.getInitSqlScript());
				logger.info("exec sql script " + serverConfig.getInitSqlScript());
				EnergyDbHelper.getDbConnection().execUpdate(scriptContent);
			} catch (IOException e) {
				logger.error(e);
			}
		}
	}

	public static NodeConfig getNodeConfig() {
		return serverConfig.getNodeConfig();
	}

	public static DBConfig getDbConfig() {
		return dbConfig;
	}

}
