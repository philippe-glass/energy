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

import com.sapereapi.model.HandlingException;

import eu.sapere.middleware.log.AbstractLogger;

public class DBConnection {
	public final static String CR = System.getProperty("line.separator");	// Cariage return
	private static int debugLevel = 0;

	private AbstractLogger logger = null;
	private static int slowQueryThresholdMS = 2000;
	private String reqSeparator = "§";
	private String reqSeparator2 = CR + "§";

	private String OP_DATETIME = "NOW";
	private String OP_CURRENT_DATETIME = "NOW()";
	private String OP_TEMPORARY = "TEMPORARY";
	private String OP_GREATEST = "GREATEST";
	private String OP_LEAST = "LEAST";
	private String OP_IF = "IF";

	// Database credentials
	private DBConfig dbConfig = null;
	private Connection connection = null;

	/**
	 * Constructor
	 * 
	 * @param _user
	 * @param _password
	 */
	DBConnection(DBConfig aDBConfig, AbstractLogger _aLogger) {
		super();
		this.dbConfig = aDBConfig;
		this.logger = _aLogger;
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
		boolean sqlite = useSQLLite();
		if(sqlite) {
			reqSeparator = ";";
			reqSeparator2 = CR + reqSeparator;
		}
		OP_DATETIME = (sqlite ? "datetime" : "NOW");
		OP_CURRENT_DATETIME = (sqlite ? "DATETIME('now', 'localtime')" : "NOW()");
		OP_TEMPORARY = (sqlite ? "" : "TEMPORARY");
		OP_GREATEST = (sqlite ? "MAX" : "GREATEST");
		OP_LEAST = (sqlite ? "MIN" : "LEAST");
		OP_IF = (sqlite ? "IIF" : "IF");
	}

	public DBConnection init(DBConfig aDBConfig, AbstractLogger _aLogger) {
		dbConfig = aDBConfig;
		logger = _aLogger;
		DBConnection newConnection = new DBConnection(dbConfig, logger);
		return newConnection;
		//instances.put(newConnection.get, newConnection)
	}



	public void initConnection() throws SQLException, ClassNotFoundException {
		if (connection == null) {
			// STEP 1: Register JDBC driver
			Class.forName(dbConfig.getDriverClassName());

			// STEP 2: Open a connection
			logger.info("Connecting to " + dbConfig.getUrl() + " database [2]...");
			connection = DriverManager.getConnection(dbConfig.getUrl(), dbConfig.getUser(), dbConfig.getPassword());
			logger.info("Connected database successfully [2]...");
		}
	}

	public String getReqSeparator() {
		return reqSeparator;
	}

	public String getReqSeparator2() {
		return reqSeparator2;
	}

	public int getDebugLevel() {
		return debugLevel;
	}

	public void setDebugLevel(int _debugLevel) {
		debugLevel = _debugLevel;
	}

	public  long execUpdate(String queries) throws HandlingException {
		HandlingException exceptionToThrow = null;
		long result = 0;
		long timeBefore = new Date().getTime();
		String currentQuery = null;
		try {
			initConnection();
		} catch (Throwable e1) {
			logger.error(e1);
		}
		// STEP 4: Execute a query
		if(debugLevel>0) {
			logger.info("execUpdate ...");
		}
		synchronized (connection) {
			Statement stmt = null;
			try {
				stmt = connection.createStatement();
				String[] array_query = queries.split(reqSeparator);
				int resultExec = 0;
				for (String nextQuery : array_query) {
					currentQuery = nextQuery;
					resultExec = stmt.executeUpdate(nextQuery, PreparedStatement.RETURN_GENERATED_KEYS);
				}
				if (resultExec > 0) {
					int step = 0;
					try {
						ResultSet generatedKeys = stmt.getGeneratedKeys();
						step++;
						if (generatedKeys.next()) {
							result = generatedKeys.getLong(1);
						}
						step++;
						generatedKeys.close();
						step++;
					} catch (Exception e1) {
						logger.error("execUpdate : error using ResultSet generatedKeys at step " + step + " :" + e1 + " : \r\n" + currentQuery);
					}
				}
				if(debugLevel>0) {
					logger.info("--- Execute SQL query : \r\n" + currentQuery);
				}
			} catch (SQLException se) {
				// Handle errors for JDBC
				logger.error("execUpdate : error executing query : \r\n" + currentQuery + "\r\n" + se);
				String exceptionMsg = ""+se.getMessage() + ((currentQuery == null)? "" : " : on query " + currentQuery);
				exceptionToThrow = new HandlingException(exceptionMsg);
				result = -1;
			} catch (Exception e) {
				// Handle errors for Class.forName
				logger.error(e);
				String exceptionMsg = ""+e.getMessage() + ((currentQuery == null)? "" : " : on query " + currentQuery);
				exceptionToThrow = new HandlingException(exceptionMsg);
				result = -1;
			} finally {
				try {
					stmt.close();
				} catch (SQLException e) {
					logger.error(e);
				}
			}
		}
		if(exceptionToThrow != null && false) {
			throw exceptionToThrow;
		}
		long timeAfter = new Date().getTime();
		long duration = timeAfter -timeBefore ;
		if(duration >= slowQueryThresholdMS) {
			logger.warning("execUpdate : slow SQL request (" + duration + " MS): " + queries);
		}
		return result;
	}

	public  Map<String, Object> executeSingleRowSelect(String queries) throws HandlingException {
		List<Map<String, Object>> rows = executeSelect(queries);
		if(rows!=null && rows.size()>0) {
			return rows.get(0);
		}
		return null;
	}

	public  List<Map<String, Object>> executeSelect(String queries) throws HandlingException {
		try {
			initConnection();
		} catch (Throwable e1) {
			logger.error(e1);
		}
		HandlingException exceptionToThrow = null;
		List<Map<String, Object>> result = null;
		long timeBefore = new Date().getTime();
		Statement stmt = null;
		String partialQueryToLog = null;
		synchronized (connection) {
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
					if(array_query.length > 1) {
						partialQueryToLog = nextQuery;
					}
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
				int columnCount = rs.getMetaData().getColumnCount();
				result = new ArrayList<Map<String, Object>>();
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
			} catch (SQLException e) {
				logger.error(e + CR + " request = " + queries + CR + " partialQueryToLog = " + partialQueryToLog);
				String exceptionMsg = ""+e.getMessage() + ((partialQueryToLog == null)? "" : " : on query " + partialQueryToLog);
				exceptionToThrow = new HandlingException(exceptionMsg);
			} finally {
				try {
					stmt.close();
				} catch (SQLException e) {
					logger.error(e);
				}
			}
		}
		if(exceptionToThrow != null) {
			throw exceptionToThrow;
		}
		return result;
	}

	public void closeConnection() {
		// finally block used to close resources
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException se) {
			logger.error(se);
		} // end
	}

	public boolean useSQLLite() {
		return dbConfig.getDriverClassName().contains("sqlite");
	}


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

	public String generateQueryCreateVariableTable() {
		StringBuffer query = new StringBuffer();
		query.append("DROP TABLE IF EXISTS _variables").append(reqSeparator2);
		query.append(CR).append("CREATE TEMPORARY TABLE _variables("
				+ "	name TEXT PRIMARY KEY"
				+ ", dec_value DECIMAL"
				+ ", int_value INTEGER"
				+ ", date_value DATETIME)");
		return query.toString();
	}

	public String generateQueryDropTable(String tableName) {
		StringBuffer query = new StringBuffer();
		query.append("DROP TABLE IF EXISTS ").append(tableName);
		return query.toString();
	}

	public String generateQueryDropTmpTable(String tableName) {
		StringBuffer query = new StringBuffer();
		query.append("DROP ").append(OP_TEMPORARY).append(" TABLE IF EXISTS ").append(tableName);
		return query.toString();
	}

	public String getOP_DATETIME() {
		return OP_DATETIME;
	}

	public String getOP_CURRENT_DATETIME() {
		return OP_CURRENT_DATETIME;
	}

	public String getOP_TEMPORARY() {
		return OP_TEMPORARY;
	}

	public String getOP_GREATEST() {
		return OP_GREATEST;
	}

	public String getOP_LEAST() {
		return OP_LEAST;
	}

	public String getOP_IF() {
		return OP_IF;
	}

	public void logVariables() throws HandlingException {
		if (useSQLLite()) {
			List<Map<String, Object>> rows = executeSelect("SELECT * FROM _variables");
			Map<String, Object> mapVariables = new HashMap<>();
			for (Map<String, Object> nextRow : rows) {
				String variable = "" + nextRow.get("name");
				for (String field : nextRow.keySet()) {
					if (!field.equalsIgnoreCase("name")) {
						Object value = nextRow.get(field);
						if (value != null) {
							mapVariables.put(variable, value);
						}
					}
				}
			}
			logger.info("logVariables : " + mapVariables);
		}
	}
}
