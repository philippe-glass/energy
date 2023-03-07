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

import com.sapereapi.log.SapereLogger;
import com.sapereapi.util.UtilDates;

public class DBConnection {
	static final String JDBC_DRIVER = "org.mariadb.jdbc.Driver";
	public final static String CR = System.getProperty("line.separator");	// Cariage return
	private static SapereLogger logger = SapereLogger.getInstance();
	private static int debugLevel = 0;
	private static int slowQueryThresholdMS = 500;
	private String url = "";//"jdbc:mariadb://localhost/energy";
	private static String sessionId = null;

	// Database credentials
	// static String user = "root";
	// static String password = "maria123";

	// Database credentials
	static String user = "";//"learning_agent";
	static String password = "";//"sql2537";

	static Connection connection = null;
	static Statement stmt = null;

	static {
		sessionId = UtilDates.generateSessionId();
	}

	/**
	 * Constructor
	 * 
	 * @param _user
	 * @param _password
	 */
	public DBConnection(String _url, String _user, String _password) {
		super();
		url = _url;
		user = _user;
		password = _password;
		try {
			connection = DriverManager.getConnection(url, user, password);
		} catch (SQLException e) {
			logger.error(e);
		}
	}

	public  void init() throws SQLException, ClassNotFoundException {
		if (connection == null) {
			// STEP 2: Register JDBC driver
			Class.forName("org.mariadb.jdbc.Driver");

			// STEP 3: Open a connection
			logger.info("Connecting to a selected database...");
			connection = DriverManager.getConnection(url, user, password);
			logger.info("Connected database successfully...");
		}
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
		try {
			init();

			// STEP 4: Execute a query
			if(debugLevel>0) {
				logger.info("execUpdate ...");
			}
			Statement stmt2 = connection.createStatement();
			String[] array_query = queries.split("§");
			for (String nextQuery : array_query) {
				result = stmt2.executeUpdate(nextQuery, PreparedStatement.RETURN_GENERATED_KEYS);
			}
			if (result > 0) {
				ResultSet generatedKeys = stmt2.getGeneratedKeys();
				if (generatedKeys.next()) {
					result = generatedKeys.getLong(1);
				}
			}
			if(debugLevel>0) {
				logger.info("--- Execute SQL query : \r\n" + queries);
			}
		} catch (SQLException se) {
			// Handle errors for JDBC
			//se.printStackTrace();
			logger.error(se);
			logger.error("SQL Error executing query : \r\n" + queries);
			result = -1;
		} catch (Exception e) {
			// Handle errors for Class.forName
			//e.printStackTrace();
			logger.error(e);
			result = -1;
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
		} catch (ClassNotFoundException e1) {
			logger.error(e1);
		} catch (SQLException e1) {
			logger.error(e1);
		}
		long timeBefore = new Date().getTime();
		try {
			stmt = connection.createStatement();
			if(debugLevel>0) {
				// logger.info("--- Execute SQL query : \r\n" + queries);
			}
			String[] array_query = queries.split("§");
			ResultSet rs = null;
			for (String nextQuery : array_query) {
				Date before = new Date();
				rs = stmt.executeQuery(nextQuery);
				if(debugLevel>0) {
					Date after = new Date();
					long requestTime = after.getTime() - before.getTime();
					logger.info("--- Execute SQL query : (" + requestTime + " MS) " + CR + nextQuery );
				}
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
		}
		return null;
	}

	public static void closeConnection() {
		// finally block used to close resources
		try {
			if (stmt != null) {
				connection.close();
			}
		} catch (SQLException se) {
		} // do nothing
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException se) {
			//se.printStackTrace();
			logger.error(se);
		} // end
	}
/*
	public static void main(String[] args) {
		execUpdate("INSERT INTO energy.history(date, learning_agent, location) "
				+ "	SELECT NOW(), 'FOO_1', '192.168.179.1:10001'");
		closeConnection();
	}// end main
	*/
}
