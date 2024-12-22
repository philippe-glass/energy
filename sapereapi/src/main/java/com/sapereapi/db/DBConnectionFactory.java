package com.sapereapi.db;

import java.util.HashMap;
import java.util.Map;

import eu.sapere.middleware.log.AbstractLogger;

public class DBConnectionFactory {
	private static Map<String, DBConnection> mapInstances = new HashMap<String, DBConnection>();
	private static Map<String, DBConfig> mapDbConfigs = new HashMap<String, DBConfig>();
	private static Map<String, AbstractLogger> mapLoggers = new HashMap<String, AbstractLogger>();

	public static void init(String dbname, DBConfig aDBConfig, AbstractLogger logger) {
		mapDbConfigs.put(dbname, aDBConfig);
		mapLoggers.put(dbname, logger);
		DBConnection newConnection = new DBConnection(aDBConfig, logger);
		mapInstances.put(dbname, newConnection);
	}

	public static DBConnection getInstance(String dbName) {
		if (!mapInstances.containsKey(dbName)) {
			DBConfig dbConfig = mapDbConfigs.get(dbName);
			AbstractLogger logger = mapLoggers.get(dbName);
			DBConnection instance = new DBConnection(dbConfig, logger);
			mapInstances.put(dbName, instance);
		}
		return mapInstances.get(dbName);
	}
}
