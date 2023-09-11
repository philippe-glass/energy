package com.sapereapi.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.log.AbstractLogger;

public class DBConnection {
	public final static String CR = System.getProperty("line.separator");	// Cariage return
	private static AbstractLogger logger = null;
	private static int debugLevel = 0;
	private static int slowQueryThresholdMS = 500;
	private static String sessionId = null;
	private static String reqSeparator = "§";
	private static String reqSeparator2 = CR + "§";

	// Database credentials
	static DBConfig dbConfig = null;
	static Connection connection = null;

	static {
		sessionId = UtilDates.generateSessionId();
	}

	/**
	 * Constructor
	 * 
	 * @param _user
	 * @param _password
	 */
	public DBConnection(DBConfig aDBConfig, AbstractLogger _aLogger) {
		super();
		dbConfig = aDBConfig;
		logger = _aLogger;
		try {
			Class.forName(dbConfig.getDriverClassName());
			logger.info("Connecting to " + dbConfig.getUrl() + " database...");
			connection = DriverManager.getConnection(dbConfig.getUrl(), dbConfig.getUser(), dbConfig.getPassword());
			logger.info("Connected database " + dbConfig.getUrl() + " successfully...");
		} catch (SQLException e) {
			logger.error(e);
		} catch (ClassNotFoundException e) {
			logger.error(e);
		}
		if(useSQLLite()) {
			reqSeparator = ";";
			reqSeparator2 = CR + reqSeparator;
		}
	}

	public  void init() throws SQLException, ClassNotFoundException {
		if (connection == null) {
			// STEP 1: Register JDBC driver
			Class.forName(dbConfig.getDriverClassName());

			// STEP 2: Open a connection
			logger.info("Connecting to " + dbConfig.getUrl() + " database [2]...");
			connection = DriverManager.getConnection(dbConfig.getUrl(), dbConfig.getUser(), dbConfig.getPassword());
			logger.info("Connected database successfully [2]...");
		}
	}

	public static String getReqSeparator() {
		return reqSeparator;
	}

	public static String getReqSeparator2() {
		return reqSeparator2;
	}

	public int getDebugLevel() {
		return debugLevel;
	}

	public void setDebugLevel(int _debugLevel) {
		debugLevel = _debugLevel;
	}

	public static String getSessionId() {
		return sessionId;
	}

	public static void setSessionId(String sessionId) {
		DBConnection.sessionId = sessionId;
	}

	public static void changeSessionId() {
		sessionId = UtilDates.generateSessionId();
	}

	public  long execUpdate(String queries) {
		long result = 0;
		long timeBefore = new Date().getTime();
		String currentQuery = null;
		try {
			init();
		} catch (Throwable e1) {
			logger.error(e1);
		}
		// STEP 4: Execute a query
		if(debugLevel>0) {
			logger.info("execUpdate ...");
		}
		Statement stmt = null;
		try {
			stmt = connection.createStatement();
			String[] array_query = queries.split(reqSeparator);
			for (String nextQuery : array_query) {
				currentQuery = nextQuery;
				result = stmt.executeUpdate(nextQuery, PreparedStatement.RETURN_GENERATED_KEYS);
			}
			if (result > 0) {
				ResultSet generatedKeys = stmt.getGeneratedKeys();
				if (generatedKeys.next()) {
					result = generatedKeys.getLong(1);
				}
			}
			if(debugLevel>0) {
				logger.info("--- Execute SQL query : \r\n" + currentQuery);
			}
		} catch (SQLException se) {
			// Handle errors for JDBC
			logger.error("SQL Error executing query : \r\n" + currentQuery + "\r\n" + se);
			result = -1;
		} catch (Exception e) {
			// Handle errors for Class.forName
			logger.error(e);
			result = -1;
		} finally {
			try {
				stmt.close();
			} catch (SQLException e) {
				logger.error(e);
			}
		}
		long timeAfter = new Date().getTime();
		long duration = timeAfter -timeBefore ;
		if(duration >= slowQueryThresholdMS) {
			logger.warning("execUpdate : slow SQL request (" + duration + " MS): " + queries);
		}
		return result;
	}

	public  Map<String, Object> executeSingleRowSelect(String queries) {
		List<Map<String, Object>> rows = executeSelect(queries);
		if(rows.size()>0) {
			return rows.get(0);
		}
		return null;
	}

	public  List<Map<String, Object>> executeSelect(String queries) {
		try {
			init();
		} catch (Throwable e1) {
			logger.error(e1);
		}
		long timeBefore = new Date().getTime();
		Statement stmt = null;
		try {
			stmt = connection.createStatement();
			if(debugLevel>0) {
				// logger.info("--- Execute SQL query : \r\n" + queries);
			}
			String[] array_query = queries.split(reqSeparator);
			ResultSet rs = null;
			int queryIndex = 1;
			for (String nextQuery : array_query) {
				boolean isLast = (array_query.length ==  queryIndex);
				Date before = new Date();
				if(isLast) {
					rs = stmt.executeQuery(nextQuery);
				} else {
					stmt.executeUpdate(nextQuery);
				}
				if(debugLevel>0) {
					Date after = new Date();
					long requestTime = after.getTime() - before.getTime();
					logger.info("--- Execute SQL query : (" + requestTime + " MS) " + CR + nextQuery );
				}
				queryIndex++;
			}
			// ResultSet rs = stmt.executeQuery(queries);
			int columnCount = rs.getMetaData().getColumnCount();
			List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
			Map<Integer, String> mapColumns = new HashMap<Integer, String>();
			// Extract data from result set
			while (rs.next()) {
				// Retrieve by column name
				Map<String, Object> nextRow = new HashMap<String, Object>();
				for (int colIdx = 1; colIdx <= columnCount; colIdx++) {
					if (!mapColumns.containsKey(colIdx)) {
						String colName = rs.getMetaData().getColumnName(colIdx);
						mapColumns.put(colIdx, colName);
						String colLabel = rs.getMetaData().getColumnLabel(colIdx);
						if (!colLabel.equals(colName)) {
							mapColumns.put(colIdx, colLabel);
						}
					}
					Object obj = rs.getObject(colIdx);
					String colName = mapColumns.get(colIdx);
					nextRow.put(colName, obj);
				}
				result.add(nextRow);
			}
			long timeAfter = new Date().getTime();
			long duration = timeAfter -timeBefore ;
			if(duration >= slowQueryThresholdMS) {
				logger.warning("executeSelect : slow SQL request (" + duration + " MS): " + queries);
			}
			return result;
		} catch (SQLException e) {
			logger.error(e);
		} finally {
			try {
				stmt.close();
			} catch (SQLException e) {
				logger.error(e);
			}
		}
		return null;
	}

	public static void closeConnection() {
		// finally block used to close resources
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException se) {
			//se.printStackTrace();
			logger.error(se);
		} // end
	}

	public boolean useSQLLite() {
		return dbConfig.getDriverClassName().contains("sqlite");
	}
/*
	public static void main(String[] args) {
		execUpdate("INSERT INTO energy.history(date, learning_agent, location) "
				+ "	SELECT NOW(), 'FOO_1', '192.168.179.1:10001'");
		closeConnection();
	}// end main
	*/

	public static String aux_affectationsToSql( Map<String, String> affectation) {
		String sep = "";
		StringBuffer query = new StringBuffer();
		for(String field : affectation.keySet()) {
			String value = affectation.get(field);
			query.append(CR).append(sep).append(" ").append(field).append(" = ").append(value);
			sep = ",";
		}
		return query.toString();
	}

	public String generateInsertQuery(String tableName, Map<String, String> defaultAffectation) {
		Map<String, String> conflictAffectation = new HashMap<String,String>();
		return generateInsertQuery(tableName, defaultAffectation, conflictAffectation);
	}

	public String generateInsertQuery(String tableName, Map<String, String> defaultAffectation, Map<String, String> confictAffectation) {
		StringBuffer query = new StringBuffer();
		if(useSQLLite()) {
			query.append("INSERT INTO ").append(tableName)
				.append(" (").append(String.join(",", defaultAffectation.keySet())).append(") VALUES ");
			query.append(CR);
			List<String> values = new ArrayList<>();
			for(String field : defaultAffectation.keySet()) {
				values.add(defaultAffectation.get(field));
			}
			query.append("(").append(String.join(",", values)).append(")");
			if(confictAffectation.size() > 0) {
				query.append(CR).append(" ON CONFLICT DO UPDATE SET ");
				query.append(aux_affectationsToSql(confictAffectation));
			}
		} else {
			query.append("INSERT INTO ").append(tableName).append(" SET");
			query.append(aux_affectationsToSql(defaultAffectation));
			if(confictAffectation.size() > 0) {
				query.append(CR).append(" ON DUPLICATE KEY UPDATE ");
				query.append(aux_affectationsToSql(confictAffectation));
			}
		}
		return query.toString();
	}

	public static String generateQueryCreateVariableTable() {
		StringBuffer query = new StringBuffer();
		query.append("DROP TABLE IF EXISTS _variables").append(reqSeparator2);
		query.append(CR).append("CREATE TEMPORARY TABLE _variables("
				+ "	name TEXT PRIMARY KEY"
				+ ", dec_value DECIMAL"
				+ ", int_value INTEGER"
				+ ", date_value DATETIME)");
		return query.toString();
	}
}
