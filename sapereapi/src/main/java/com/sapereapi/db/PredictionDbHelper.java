package com.sapereapi.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.OptionItem;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.energy.SingleNodeStateItem;
import com.sapereapi.model.energy.SingleStateItem;
import com.sapereapi.model.learning.FeaturesKey;
import com.sapereapi.model.learning.LearningModelType;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.learning.NodeStatesTransitions;
import com.sapereapi.model.learning.PredictionScope;
import com.sapereapi.model.learning.TimeWindow;
import com.sapereapi.model.learning.VariableFeaturesKey;
import com.sapereapi.model.learning.VariableState;
import com.sapereapi.model.learning.VariableStateHistory;
import com.sapereapi.model.learning.markov.CompleteMarkovModel;
import com.sapereapi.model.learning.markov.TransitionMatrix;
import com.sapereapi.model.learning.markov.VariableMarkovModel;
import com.sapereapi.model.learning.prediction.LearningAggregationOperator;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.model.learning.prediction.PredictionData;
import com.sapereapi.model.learning.prediction.PredictionDeviation;
import com.sapereapi.model.learning.prediction.PredictionResult;
import com.sapereapi.model.learning.prediction.PredictionStatistic;
import com.sapereapi.model.learning.prediction.PredictionStep;
import com.sapereapi.model.learning.prediction.StatesStatistic;
import com.sapereapi.model.learning.prediction.input.StatisticsRequest;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;
import com.sapereapi.util.matrix.DoubleMatrix;
import com.sapereapi.util.matrix.IterationMatrix;

public class PredictionDbHelper {
	private static DBConnection dbConnection = null;
	private static PredictionDbHelper instance = null;
	public final static String CR = System.getProperty("line.separator");	// Cariage return
	private static int debugLevel = 0;
	private static SapereLogger logger = SapereLogger.getInstance();
	public final static String CLEMAPDATA_DBNAME = "clemap_data_light";
	private static boolean sqlite = false;
	//private static String OP_DATETIME = null;
	private static String OP_CURRENT_DATETIME = null;
	private static String OP_TEMPORARY = null;
	private static String OP_GREATEST = null;
	private static String OP_LEAST = null;
	private static String OP_IF = null;
	private static String unixTimeOp = null;
	private static boolean isModeDebug = false;


	public static void init() {
		// initialise db connection
		instance = new PredictionDbHelper();
	}
	public static PredictionDbHelper getInstance() {
		if (instance == null) {
			instance = new PredictionDbHelper();
		}
		return instance;
	}

	public PredictionDbHelper() {
		// initialise db connection
		dbConnection = DBConnectionFactory.getInstance(Sapere.DB_SERVER);
		sqlite = dbConnection.useSQLLite();
		OP_CURRENT_DATETIME = dbConnection.getOP_CURRENT_DATETIME();
		OP_TEMPORARY = dbConnection.getOP_TEMPORARY();
		OP_GREATEST = dbConnection.getOP_GREATEST();
		OP_LEAST = dbConnection.getOP_LEAST();
		OP_IF = dbConnection.getOP_IF();
		unixTimeOp = sqlite ? "UNIXEPOCH" : "UNIX_TIMESTAMP";

	}

	public static Long getSessionId() {
		return SessionManager.getSessionId();
	}

	public static DBConnection getDbConnection() {
		return dbConnection;
	}

	public static PredictionContext registerPredictionContext(PredictionContext predictionContext) throws HandlingException {
		// TODO : register node config into node_location
		Map<String, String> affectation = new HashMap<>();
		// Dummy model_context, just for test (to have different id as for node_context
		/*
		dbConnection.execUpdate("INSERT INTO model_context(scope) VALUES"
				+ " ('NODE')"
				+ ",('NODE')"
				+ ",('NODE')"
				+ ",('CLUSTER')"
				+ ",('CLUSTER')"
				+ ",('CLUSTER')");
		*/
		String sScope = ""+predictionContext.getScope();
		affectation.put("scope", addSingleQuotes(sScope));
		if(predictionContext.getAggregationOperator() != null) {
			LearningAggregationOperator aggregationOp = predictionContext.getAggregationOperator();
			affectation.put("aggregation_operator", addSingleQuotes(aggregationOp.getName()));
		}
		affectation.put("model_type", addSingleQuotes("" + predictionContext.getModelType()));
		affectation.put("id_node_context", "" + predictionContext.getNodeContext().getId());
		affectation.put("learning_window", "" + predictionContext.getLearningWindow());
		String query1 = dbConnection.generateInsertQuery("model_context", affectation);
		Long predictionContextId = dbConnection.execUpdate(query1);
		predictionContext.setId(predictionContextId);
		return predictionContext;
	}

	public static List<OptionItem> getStateDates(PredictionContext predictionContext) throws HandlingException {
		List<OptionItem> result = new ArrayList<>();
		Long predictionCtxId = predictionContext.getId();
		List<Map<String, Object>> rows = dbConnection.executeSelect("SELECT DATE(date) AS day"
				+ CR + " FROM state_history sh"
				+ CR + " WHERE id_model_context = " + predictionCtxId
				+ CR + " GROUP BY day"
				+ CR + " ORDER BY day DESC");
		for(Map<String, Object> nextRow : rows) {
			Date day = SapereUtil.getDateValue(nextRow, "day", logger);
			String sday = UtilDates.format_sql_day.format(day);
			result.add(new OptionItem(sday, sday));
		}
		return result;
	}

	private static Date cleanHistory(PredictionContext predictionContext, Date date1, Date date2) throws HandlingException {
		StringBuffer query1 = new StringBuffer();
		Long sessionId = getSessionId();
		String reqSeparator2 = dbConnection.getReqSeparator2() + CR;
		String sqlDate1 = addSingleQuotes(UtilDates.format_sql.format(date1));
		String sqlDate2 = addSingleQuotes(UtilDates.format_sql.format(date2));
		Long predictionCtxId = predictionContext.getId();
		query1.append(CR).append(dbConnection.generateQueryDropTmpTable("TmpCleanSH"));
		query1.append(reqSeparator2);
		query1.append(CR).append("CREATE TEMPORARY TABLE TmpCleanSH AS")
			.append(CR).append("SELECT id FROM state_history WHERE date BETWEEN ").append(sqlDate1).append(" AND ").append(sqlDate2)
			.append(CR).append(" AND id_model_context = ").append(predictionCtxId)
			.append(CR).append(" AND NOT id_session = ").append(sessionId);
		query1.append(reqSeparator2);
		//query1.append("INSERT INTO TmpCleanSH(id) SELECT id FROM state_history WHERE id_session = ").append(sessionId2);
		//.append(reqSeparator2);
		query1.append(CR).append(dbConnection.generateQueryDropTmpTable("TmpSH2"));
		query1.append(reqSeparator2);
		query1.append(CR).append("CREATE TEMPORARY TABLE TmpSH2 AS "
				+ " SELECT sh.id FROM TmpCleanSH"
				+ " JOIN state_history sh  on sh.id_last  = TmpCleanSH.id"
				+ " WHERE sh.id_model_context = " + predictionCtxId
				);
		query1.append(reqSeparator2);
		query1.append(CR).append("UPDATE state_history SET id_last = NULL WHERE id IN (SELECT id FROM TmpSH2)");
		query1.append(reqSeparator2);
		query1.append(CR).append(dbConnection.generateQueryDropTmpTable("TmpPrediction2"));
		query1.append(reqSeparator2);
		query1.append("CREATE TEMPORARY TABLE TmpPrediction2 AS "
				+ CR + " SELECT p.id FROM TmpCleanSH"
				+ CR + " JOIN prediction p ON p.id_target_state_histo  = TmpCleanSH.id");
		query1.append(reqSeparator2);
		query1.append(CR).append("UPDATE prediction SET id_target_state_histo = NULL WHERE id IN (SELECT id FROM TmpPrediction2)");
		query1.append(reqSeparator2);
		query1.append(CR).append("DELETE ").append(sqlite? "" : "state_history").append(" FROM state_history WHERE id IN (SELECT id FROM TmpCleanSH)");
		query1.append(reqSeparator2);
		query1.append("SELECT MAX(date) AS maxDate FROM state_history WHERE id_model_context =" + predictionCtxId + " AND date < " + sqlDate1
					+ CR +  "					AND id_session = " + sessionId);
		Map<String, Object> row = dbConnection.executeSingleRowSelect(query1.toString());
		Date lastDate = SapereUtil.getDateValue(row, "maxDate", logger);
		return lastDate;
	}

	public static Map<String, Long> saveAllHistoryStates(PredictionContext predictionContext
			, List<SingleNodeStateItem> listStateHistory
			//, Map<Date, SingleNodeStateItem> mapStateHistory
			) throws HandlingException {
		Map<String, Long> result = new HashMap<String, Long>();
		boolean sqlite = dbConnection.useSQLLite();
		String reqSeparator2 = dbConnection.getReqSeparator2() + CR;
		Long sessionId = getSessionId();
		Long predictionCtxId = predictionContext.getId();
		NodeContext nodeContext = predictionContext.getNodeContext();
		Long nodeCtxId = nodeContext.getId();
		SingleNodeStateItem firstNodeState = listStateHistory.get(0);
		Date date1 =  firstNodeState.getDate(); // mapStateHistory.keySet().iterator().next();
		Date date2 = UtilDates.shiftDateDays(date1, 366);
		// Clean existing data in state_history and retrieve the last date
		Date maxPreviousDate = cleanHistory(predictionContext, date1, date2);
		Date lastDate = maxPreviousDate == null ? null : new Date(maxPreviousDate.getTime());

		// Insert data into state_history table
		StringBuffer query1 = new StringBuffer();
		// Clean existting data linked to the same model_context
		long result1 = dbConnection.execUpdate("DELETE FROM state_history WHERE id_model_context = " + predictionCtxId + " AND id_session = " + sessionId);
		query1.append(reqSeparator2);
		String query1Header = "INSERT INTO state_history(id_model_context,id_node_context,date,date_last,id_session,variable_name,state_idx,state_name,value) VALUES ";
		query1.append(query1Header);
		int stateAdded = 0;
		String lastDateSql = "null";
		if(lastDate != null) {
			lastDateSql = addSingleQuotes(UtilDates.format_sql.format(lastDate));
		}
		String separator = "";
		for(SingleNodeStateItem nodeStateItem : listStateHistory ) {
			Date nextDate = nodeStateItem.getDate();
			Map<String, SingleStateItem> mapStateHistory2 = nodeStateItem.generateMapSingleStateItem();
			String nextDateSql = addSingleQuotes(UtilDates.format_sql.format(nextDate));
			for(String variable : mapStateHistory2.keySet()) {
				SingleStateItem stateItem = mapStateHistory2.get(variable);
				int stateId = stateItem.getStateId();
				double value = stateItem.getValue();
				int stateIdx = stateId -1;
				String stateName = "S"+ stateId;
				query1.append(CR).append(separator)
					.append("(")
					.append(predictionCtxId)
					.append(",").append(nodeCtxId)
					.append(",").append(nextDateSql)
					.append(",").append(lastDateSql)
					.append(",").append(sessionId)
					.append(",").append(addSingleQuotes(variable))
					.append(",").append(stateIdx)
					.append(",").append(addSingleQuotes(stateName))
					.append(",").append(addQuotes(value))
					.append(")");
				separator = ", ";
				stateAdded++;
				if(stateAdded >= 1000) {
					if(sqlite) {
						query1.append(CR).append(" ON CONFLICT DO UPDATE SET value = value");
					} else {
						query1.append(CR).append(" ON DUPLICATE KEY UPDATE value = value");
					}
					result1 = dbConnection.execUpdate(query1.toString());
					if(result1 < 0) {
						logger.error("saveAllHistoryStates : error in the insertion");
					}
					separator = "";
					stateAdded = 0;
					query1 = new StringBuffer();
					query1.append(query1Header);
				}
			}
			lastDate = nextDate;
			lastDateSql =  addSingleQuotes(UtilDates.format_sql.format(lastDate));
		}
		if(sqlite) {
			query1.append(CR).append(" ON CONFLICT DO UPDATE SET value = value");
		} else {
			query1.append(CR).append(" ON DUPLICATE KEY UPDATE value = value");
		}
		result1 = dbConnection.execUpdate(query1.toString());
		if(result1 < 0) {
			logger.error("saveAllHistoryStates : error in query1 : " + query1);
		}
		// Set idLast
		String sqlDate1 = addSingleQuotes(UtilDates.format_sql.format(date1));
		StringBuffer query2 = new StringBuffer();
		query2.append(CR).append("UPDATE state_history SET observation_update = ").append(OP_CURRENT_DATETIME)
			.append(CR).append(", id_last = ")
			.append(CR).append("    (SELECT MAX(last.id) FROM state_history AS last WHERE ")
			.append(CR).append(" 			last.id_model_context = ").append( predictionCtxId)
			.append(CR).append(" 		AND	last.id_session = ").append( sessionId)
			.append(CR).append("      	AND last.date =  state_history.date_last ")
			.append(CR).append(" 		AND last.variable_name = state_history.variable_name)")
			.append(CR).append(" WHERE id_model_context = ").append(predictionCtxId).append(" AND id_session = " + sessionId + " AND  date >= " + sqlDate1);
		query2.append(reqSeparator2);
		query2.append(CR).append("UPDATE state_history SET ")
			.append(CR).append("    state_idx_last =  (SELECT MAX(last.state_idx) FROM state_history AS last WHERE last.id = state_history.id_last)" )
			.append(CR).append("   ,state_name_last =  (SELECT MAX(last.state_name) FROM state_history AS last WHERE last.id = state_history.id_last)" )
			.append(CR).append(" WHERE id_model_context = ").append(predictionCtxId).append(" AND id_session = " + sessionId + " AND  date >= " + sqlDate1 + " AND NOT id_last IS NULL");
		query2.append(reqSeparator2);
		query2.append(CR).append("UPDATE state_history SET date_next = (SELECT MAX(next.date) FROM state_history AS next ")
			.append(CR).append("	 WHERE next.id_last = state_history.id)")
			.append(CR).append(" WHERE id_model_context = ").append(predictionCtxId).append(" AND id_session = " + sessionId + " AND  date >= " + sqlDate1);
		if(stateAdded > 0) {
			long result2 = dbConnection.execUpdate(query2.toString());
			if(result2 < 0) {
				logger.error("saveAllHistoryStates : error in query2 : " + query2);
			}
			List<Map<String, Object>> rows = dbConnection.executeSelect("SELECT * FROM state_history WHERE id_model_context = " + predictionCtxId + " AND  date >= " + sqlDate1);
			for(Map<String, Object> row: rows) {
				Long id = SapereUtil.getLongValue(row, "id");
				String variableName = "" + row.get("variable_name");
				result.put(variableName, id);
			}
		}
		return result;
	}

	public static List<SingleNodeStateItem> loadStateHistory(PredictionContext predictionContext, Date dateMin, Date dateMax)
			throws HandlingException {
		List<SingleNodeStateItem> result = new ArrayList<SingleNodeStateItem>();
		SingleNodeStateItem nextStateItem = null;
		String sqlDateMin = addSingleQuotes(UtilDates.format_sql.format(dateMin));
		String sqlDateMax = addSingleQuotes(UtilDates.format_sql.format(dateMax));
		String query1 = "SELECT sh.* "
				+ CR + "FROM state_history sh "
				+ CR + "WHERE sh.id_model_context = " + predictionContext.getId()
				+ CR + " AND sh.date >= " + sqlDateMin + " AND sh.date <= " + sqlDateMax
				+ CR + " ORDER BY sh.date";
		List<Map<String, Object>> rows = dbConnection.executeSelect(query1);
		for(Map<String, Object> row : rows) {
			Date date = SapereUtil.getDateValue(row, "date", logger);
			if(nextStateItem == null || nextStateItem.getDate().before(date)) {
				if(nextStateItem != null) {
					result.add(nextStateItem);
				}
				nextStateItem = new SingleNodeStateItem();
			}
			nextStateItem.setDate(date);
			String variable = "" + row.get("variable_name");
			Integer stateId = SapereUtil.getIntValue(row, "state_idx");
			Double value = SapereUtil.getDoubleValue(row, "value", logger);
			nextStateItem.setStateId(variable, stateId);
			nextStateItem.setValue(variable, value);
		}
		if(nextStateItem != null) {
			result.add(nextStateItem);
		}
		return result;
	}

	public static long saveHistoryStates(
			 NodeStatesTransitions nodeStateTransitions
			,PredictionContext predictionContext) throws HandlingException {
		//if(!nodeTotal.hasActivity()) {
			//return result;
		//}
		boolean sqlite = dbConnection.useSQLLite();
		String reqSeparator2 = dbConnection.getReqSeparator2();
		Long sessionId = getSessionId();
		Long modelContextId = predictionContext.getId();
		Long nodeContextId = predictionContext.getNodeContext().getId();
		Map<String, VariableState> mapStates = nodeStateTransitions.getMapStates();
		Map<String, Double> mapValues = nodeStateTransitions.getMapValues();
		StringBuffer query1 = new StringBuffer();
		Date stateDate = nodeStateTransitions.getStateDate(); // nodeTotal.getDate()
		String sqlDate1 = UtilDates.format_sql.format(stateDate);
		String sqlDate1Quotes = addSingleQuotes(sqlDate1);
		Date date2 = UtilDates.shiftDateMinutes(stateDate, 60);
		String sqlDate2 = UtilDates.format_sql.format(date2);
		String sqlDate2Quotes = addSingleQuotes(sqlDate2);
		// Clean existing data in state_history for the same dates
		query1.append(CR).append(dbConnection.generateQueryDropTmpTable("TmpCleanSH"));
		query1.append(reqSeparator2);
		query1.append(CR).append("CREATE TEMPORARY TABLE TmpCleanSH AS")
			.append(CR).append("SELECT id FROM state_history WHERE date BETWEEN ").append(sqlDate1Quotes).append(" AND ").append(sqlDate2Quotes)
			.append(CR).append(" AND id_model_context = ").append(modelContextId)
			.append(CR).append(" AND NOT id_session = ").append(sessionId);
		query1.append(reqSeparator2);
		query1.append(CR).append(dbConnection.generateQueryDropTmpTable("TmpSH2"));
		query1.append(reqSeparator2);
		query1.append(CR).append("CREATE TEMPORARY TABLE TmpSH2 AS ")
			.append(CR).append("	SELECT sh.id FROM TmpCleanSH")
			.append(CR).append("	JOIN state_history sh  on sh.id_last  = TmpCleanSH.id")
			.append(CR).append("	WHERE id_model_context = ").append(modelContextId);
		query1.append(reqSeparator2);
		query1.append(CR).append("UPDATE state_history SET id_last = NULL WHERE id IN (SELECT id FROM TmpSH2)");
		query1.append(reqSeparator2);
		query1.append(CR).append(dbConnection.generateQueryDropTmpTable("TmpPrediction2"));
		query1.append(reqSeparator2);
		query1.append(CR).append("CREATE TEMPORARY TABLE TmpPrediction2 AS "
				+ CR + " SELECT p.id FROM TmpCleanSH"
				+ CR + " JOIN prediction p ON p.id_target_state_histo  = TmpCleanSH.id");
		query1.append(reqSeparator2);
		query1.append(CR).append("UPDATE prediction SET id_target_state_histo = NULL WHERE id IN (SELECT id FROM TmpPrediction2)");
		query1.append(reqSeparator2);
		query1.append(CR).append("DELETE ").append(sqlite? "" : "state_history").append(" FROM state_history WHERE id IN (SELECT id FROM TmpCleanSH)");
		long result1 = dbConnection.execUpdate(query1.toString());
		logger.info("saveHistoryStates : result1 = " + result1);

		// Insert data into state_history table
		StringBuffer query2 = new StringBuffer();
		if(sqlite) {
			query2.append(dbConnection.generateQueryCreateVariableTable());
			query2.append(reqSeparator2);
			query2.append(CR).append("INSERT INTO _variables(name, date_value)")
			.append(CR).append("SELECT 'date_last', MAX(date) "
					+ CR + " FROM state_history WHERE id_model_context =" + modelContextId
					+ " AND date < " + sqlDate1Quotes
					+ " AND id_session = " + sessionId
					+ "");
		} else {
			query2.append(CR).append("SET @date_last = (SELECT MAX(date) FROM state_history WHERE id_model_context = " + modelContextId
				+ " AND date < " + sqlDate1Quotes
				+ " AND id_session = " + sessionId
				+ ")");
		}
		String varDateLast = sqlite? "(SELECT date_value FROM _variables WHERE name = 'date_last')" : "@date_last";
		query2.append(reqSeparator2);
		query2.append(CR).append("INSERT INTO state_history(id_model_context,id_node_context,date,date_last,id_session,variable_name,state_idx,state_name,value,id_last) VALUES ").append(CR);
		boolean stateAdded = false;
		for(String variable : predictionContext.getNodeContext().getVariables()) {
			if(mapStates.containsKey(variable)) {
				VariableState state = mapStates.get(variable);
				double value = -1;
				if(mapValues.containsKey(variable)) {
					value = mapValues.get(variable);
				}
				int stateIdx = state.getId() -1;
				String stateName = "S"+ state.getId();
				if(stateAdded) {
					query2.append(CR).append(", ");
				}
				query2.append("(")
					.append(modelContextId)
					.append(",").append(nodeContextId)
					.append(",").append(sqlDate1Quotes)
					.append(",").append(varDateLast)
					.append(",").append(sessionId)
					.append(",").append(addSingleQuotes(variable))
					.append(",").append(stateIdx).append(",").append(addSingleQuotes(stateName))
					.append(",").append(addQuotes(value))
					.append(",").append("(SELECT lastSH.id FROM state_history AS lastSH WHERE lastSH.id_session = " + sessionId
											+ " AND lastSH.date = " + varDateLast + " AND lastSH.variable_name = " + addSingleQuotes(variable) + " LIMIT 0,1)")
					.append(")");
				stateAdded = true;
			}
		}
		if(sqlite) {
			query2.append(CR).append(" ON CONFLICT DO UPDATE SET value = value");
		} else {
			query2.append(CR).append(" ON DUPLICATE KEY UPDATE value = value");
		}
		query2.append(CR).append(reqSeparator2);
		query2.append(CR).append("UPDATE state_history SET ")
			.append(CR).append("    state_idx_last =  (SELECT MAX(last.state_idx) FROM state_history AS last WHERE last.id = state_history.id_last)" )
			.append(CR).append("   ,state_name_last =  (SELECT MAX(last.state_name) FROM state_history AS last WHERE last.id = state_history.id_last)" )
			.append(CR).append(" WHERE id_session = " + sessionId + " AND  date = " + sqlDate1Quotes + " AND NOT id_last IS NULL");
		query2.append(CR).append(reqSeparator2);
		query2.append(CR).append("UPDATE state_history SET ")
		.append(CR).append("date_next =  " + sqlDate1Quotes)
		.append(CR).append(" WHERE id_session = " + sessionId + " AND  date = " + varDateLast);
		if(stateAdded) {
			dbConnection.execUpdate(query2.toString());
			List<Map<String, Object>> rows = dbConnection.executeSelect("SELECT * FROM state_history WHERE id_model_context = " + modelContextId + " AND  date = " + sqlDate1Quotes);
			nodeStateTransitions.clearMapStateHistoryId();
			for(Map<String, Object> row: rows) {
				Long id = SapereUtil.getLongValue(row, "id");
				String variableName = "" + row.get("variable_name");
				nodeStateTransitions.setStateHistoryId(variableName, id);
			}
		}
		return 0;
	}


	public static List<TimeWindow> retrieveTimeWindows() {
		List<TimeWindow> result = new ArrayList<TimeWindow>();
		try {
			List<Map<String, Object>> rows = dbConnection.executeSelect("SELECT * FROM time_window");
			for(Map<String, Object> row : rows) {
				String sDaysOfWeek = "" + row.get("days_of_week");
				String[] tabDaysOfWeek = sDaysOfWeek.split(",");
				Set<Integer> daysOfWeek = new HashSet<Integer>();
				for(String nextDayOfWeek : tabDaysOfWeek) {
					daysOfWeek.add(Integer.valueOf(nextDayOfWeek));
				}
				int id = SapereUtil.getIntValue(row, "id");
				int startHour = SapereUtil.getIntValue(row, "start_hour");
				int startMinute = SapereUtil.getIntValue(row, "start_minute");
				int endHour = SapereUtil.getIntValue(row, "end_hour");
				int endMinute = SapereUtil.getIntValue(row, "end_minute");
				TimeWindow timeWindow = new TimeWindow(id, daysOfWeek, startHour, startMinute, endHour, endMinute);
				result.add(timeWindow);
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return result;
	}

	public static String addSingleQuotes(String str) {
		return SapereUtil.addSingleQuotes(str);
	}

	public static String addQuotes(Integer number) {
		return SapereUtil.addSingleQuotes(number);
	}

	public static String addQuotes(Long number) {
		return SapereUtil.addSingleQuotes(number);
	}

	public static String addQuotes(Float number) {
		return SapereUtil.addSingleQuotes(number);
	}

	public static String addQuotes(Double number) {
		return SapereUtil.addSingleQuotes(number);
	}
/*
	public static Map<Integer, Map<String, VariableFeaturesKey>> loadMapNodeTransitionMatrixKeys(
			PredictionContext context) throws HandlingException {
		Map<Integer, Map<String, VariableFeaturesKey>> result = new HashMap<Integer, Map<String, VariableFeaturesKey>>();
		List<VariableFeaturesKey> listTrKeys = loadListNodeTransitionMatrixKeys(context);
		for(VariableFeaturesKey nextTrKey : listTrKeys) {
			Integer timeWindowId = nextTrKey.getTimeWindowId();
			if(!result.containsKey(timeWindowId)) {
				result.put(timeWindowId, new HashMap<String, VariableFeaturesKey>());
			}
			Map<String, VariableFeaturesKey> map1 = result.get(timeWindowId);
			String variable = nextTrKey.getVariable();
			map1.put(variable, nextTrKey);
		}
		return result;
	}
*/
	public static Map<FeaturesKey, Map<String, VariableFeaturesKey>> loadMapNodeTransitionMatrixKeys2(
			PredictionContext context) throws HandlingException {
		Map<FeaturesKey, Map<String, VariableFeaturesKey>> result = new HashMap<FeaturesKey, Map<String, VariableFeaturesKey>>();
		List<VariableFeaturesKey> listTrKeys = loadListNodeTransitionMatrixKeys(context);
		for (VariableFeaturesKey nextTrKey : listTrKeys) {
			FeaturesKey featuresKey = nextTrKey.getFeaturesKey();
			if (!result.containsKey(featuresKey)) {
				result.put(featuresKey, new HashMap<String, VariableFeaturesKey>());
			}
			Map<String, VariableFeaturesKey> map1 = result.get(featuresKey);
			String variable = nextTrKey.getVariable();
			map1.put(variable, nextTrKey);
		}
		return result;
	}

	public static List<VariableFeaturesKey> loadListNodeTransitionMatrixKeys(
			PredictionContext predictionContext) throws HandlingException {
		List<VariableFeaturesKey> result = new ArrayList<VariableFeaturesKey>();
		List<Map<String, Object>> rows1 = dbConnection.executeSelect(
				"SELECT * FROM mc_transition_matrix WHERE id_model_context = " + predictionContext.getId());
		for(Map<String, Object> row : rows1) {
			String variable = "" + row.get("variable_name");
			Long idTM = SapereUtil.getLongValue(row, "id");
			int timeWindowId = SapereUtil.getIntValue(row, "id_time_window");
			FeaturesKey featuresKey = predictionContext.getFeaturesKey(timeWindowId);
			VariableFeaturesKey trKey = new VariableFeaturesKey(idTM, predictionContext.getId(), variable, featuresKey);
			result.add(trKey);
		}
		return result;
	}


	public static CompleteMarkovModel loadPartialMarkovModel(
			 CompleteMarkovModel markovModel
			,String[] _variables
			,List<FeaturesKey> listFeaturesKey
			,Date currentDate
			) throws HandlingException {
		logger.info("loadListNodeTransitionMatrice : begin");
		PredictionContext predictionContext = markovModel.getPredictionContext();
		List<String> idsTimeWindows = new ArrayList<String>();
		for(FeaturesKey nextFeaturesKey : listFeaturesKey) {
			markovModel.initTransitionMatrices(nextFeaturesKey);
			idsTimeWindows.add(""+nextFeaturesKey.getTimeWindowId());
		}
		String reqSeparator2 = dbConnection.getReqSeparator2();
		//String sCurrentDate = UtilDates.format_sql.format(currentDate);
		//String sCurrentDay = UtilDates.format_sql_day.format(currentDate);
		String sIdsTimeWindows = SapereUtil.implode(idsTimeWindows, ",");
		String sVariableNames = "'"+ SapereUtil.implode(Arrays.asList(_variables), "','") + "'";
		StringBuffer loadQuery1 = new StringBuffer();
		loadQuery1.append(CR).append("DROP ").append(OP_TEMPORARY).append(" TABLE IF EXISTS TmpTrMatrix")
			.append(reqSeparator2);
		loadQuery1.append(CR).append( "CREATE TEMPORARY TABLE TmpTrMatrix AS")
			.append(CR).append("SELECT tm.id")
			.append(CR).append(",tm.variable_name")
			.append(CR).append(",tm.last_update")
			.append(CR).append(",tm.id_time_window")
			.append(CR).append(",tm.current_iteration_number")
			.append(CR).append("FROM mc_transition_matrix AS tm ")
			.append(CR).append("WHERE tm.id_model_context = ").append(addQuotes(predictionContext.getId()))
			.append(CR).append(" 	AND tm.id_time_window IN (").append(sIdsTimeWindows).append(")")
			.append(CR).append(" 	AND tm.variable_name IN (").append(sVariableNames).append(")")
		;
		loadQuery1.append(reqSeparator2);
		loadQuery1.append(CR).append("SELECT * FROM TmpTrMatrix");
		Date before = new Date();
		// At first, load the transition matrix headers
		List<Map<String, Object>> rows1 = dbConnection.executeSelect(loadQuery1.toString());
		for(Map<String, Object> row : rows1) {
			String variable = "" + row.get("variable_name");
			Long timeWindowId1 = SapereUtil.getLongValue(row, "id_time_window");
			Integer timeWindowId = Integer.valueOf(timeWindowId1.intValue());
			FeaturesKey featuresKey = predictionContext.getFeaturesKey(timeWindowId);
			Long idTM = SapereUtil.getLongValue(row, "id");
			markovModel.setTransitionMatrixId(featuresKey, variable, idTM);
		}
		Date after = new Date();
		// Secondly, load all transition  mc_transition_matrix_cell_iteration contents
		StringBuffer loadQuery2 = new StringBuffer();
		loadQuery2.append(CR).append("SELECT TmpTrMatrix.variable_name, TmpTrMatrix.id_time_window")
				.append(CR).append("	,cellIt.*")
				.append(CR).append(" FROM TmpTrMatrix ")
				.append(CR).append(" JOIN mc_transition_matrix_cell_iteration  AS cellIt ON cellIt.id_transition_matrix = TmpTrMatrix.id")
				.append(CR).append(" WHERE 1")
				.append(CR).append(" ORDER BY TmpTrMatrix.ID");
		//dbConnection.setDebugLevel(1);
		List<Map<String, Object>> rows2 = dbConnection.executeSelect(loadQuery2.toString());
		after = new Date();
		long loadTime = after.getTime() - before.getTime();
		logger.info("loadListNodeTransitionMatrice : request time = " + loadTime);
		for(Map<String, Object> row : rows2) {
			String variable = "" + row.get("variable_name");
			Long timeWindowId1 = SapereUtil.getLongValue(row, "id_time_window");
			Integer timeWindowId = Integer.valueOf(timeWindowId1.intValue());
			Integer rowIdx = SapereUtil.getIntValue(row,"row_idx");
			Integer columnIdx = SapereUtil.getIntValue(row,"column_idx");
			Integer iterationNumber = SapereUtil.getIntValue(row, "iteration_number");
			Date iterationDate = SapereUtil.getDateValue(row, "iteration_date", logger);
			Object oNumberOfObservation = row.get("obs_number");
			if(oNumberOfObservation != null) {
				double nbOfObs = SapereUtil.getDoubleValue(row, "obs_number", logger);
				double nbOfCorrections = SapereUtil.getDoubleValue(row, "corrections_number", logger);
				if(nbOfCorrections>0 && debugLevel> 0) {
					logger.info("loadListNodeTransitionMatrice : " + variable + " " + timeWindowId1 + " nb of corrections : " + nbOfCorrections + " " + nbOfCorrections);
				}
				FeaturesKey featuresKey = predictionContext.getFeaturesKey(timeWindowId);
				markovModel.setValueAtIteration(featuresKey, variable, iterationNumber, iterationDate, rowIdx, columnIdx, nbOfObs, nbOfCorrections);
			}
		}
		markovModel.refreshAllMatrices();
		Date after2 = new Date();
		long loadTime2 = after2.getTime() - after.getTime();
		logger.info("loadListNodeTransitionMatrice : end : loadTime2 = " + loadTime2);
		return markovModel;
	}



	public static Map<Date,NodeStatesTransitions> loadMapNodeStateTransitions(
			PredictionContext predictionContext,
			TimeSlot timeSlot, String[] variableNames, double maxTotalPower) throws HandlingException {
		if(variableNames.length == 0) {
			return null;
		}
		String firstVariable = variableNames[0];
		Date stateDate = null;
		String sqlDateMin = UtilDates.format_sql.format(timeSlot.getBeginDate());
		String sqlDateMax = UtilDates.format_sql.format(timeSlot.getEndDate());
		String query1 = "SELECT sh.date "
				+ CR + "FROM state_history sh "
				+ CR + "JOIN state_history AS last ON last.id = sh.id_last"
				+ CR + "WHERE sh.id_model_context = " + predictionContext.getId()
				+ " AND sh.variable_name = " + addSingleQuotes(firstVariable)
				+ " AND sh.date >= " + addSingleQuotes(sqlDateMin)
				+ " AND sh.date <= " + addSingleQuotes(sqlDateMax)
		;
		List<Date> listStateDate = new ArrayList<>();
		List<Map<String, Object>> rows = dbConnection.executeSelect(query1);
		for(Map<String, Object> row : rows) {
			stateDate = SapereUtil.getDateValue(row, "date", logger);
			listStateDate.add(stateDate);
		}
		Map<Date,NodeStatesTransitions> mapNodeTransition = aux_loadNodeStateTransitions(predictionContext, listStateDate, variableNames, maxTotalPower);
		return mapNodeTransition;
	}

	public static NodeStatesTransitions loadClosestNodeStateTransition(
			PredictionContext predictionContext,
			Date aDate, String[] variableNames, double maxTotalPower) throws HandlingException {
		if(variableNames.length == 0) {
			return null;
		}
		String firstVariable = variableNames[0];
		Date stateDate = null;
		String quotedDate =  addSingleQuotes(UtilDates.format_sql.format(aDate));
		String unixDate = unixTimeOp + "(" + quotedDate + ")";
		String query1 = "SELECT sh.date "
				+ CR + "FROM state_history sh "
				+ CR + "JOIN state_history AS last ON last.id = sh.id_last"
				+ CR + "WHERE sh.id_model_context = " + predictionContext.getId()
				+ CR + " 	AND sh.variable_name = " + addSingleQuotes(firstVariable)
				+ CR + " 	AND " + unixTimeOp + "(sh.date) >= " + unixDate + " - 300"
				+ CR + " 	AND " + unixTimeOp +  "(sh.date) <= " + unixDate + " + 300"
				+ CR + "ORDER BY ABS(" + unixDate + " - " + unixTimeOp + "(sh.date)), sh.date LIMIT 0,1";
		List<Map<String, Object>> rows = dbConnection.executeSelect(query1);
		if(rows.size()>0) {
			Map<String, Object> row = rows.get(0);
			stateDate = SapereUtil.getDateValue(row, "date", logger);
		} else {
			return null;
		}
		List<Date> listStateDate = new ArrayList<>();
		listStateDate.add(stateDate);
		Map<Date,NodeStatesTransitions> mapNodeTransition = aux_loadNodeStateTransitions(predictionContext, listStateDate, variableNames, maxTotalPower);
		if(mapNodeTransition.containsKey(stateDate)) {
			return mapNodeTransition.get(stateDate);
		}
		return null;
	}

	public static Map<Date,NodeStatesTransitions> aux_loadNodeStateTransitions(
			 PredictionContext predictionContext
			,List<Date> listStateDate
			,String[] variableNames
			,double maxTotalPower) throws HandlingException {
		 Map<Date,NodeStatesTransitions> result = new HashMap<Date,NodeStatesTransitions>();
		String variableFilter = "'"+ String.join("','", variableNames) + "'";
		List<String> listSqlDate = new ArrayList<>();
		for(Date nextDate : listStateDate) {
			listSqlDate.add(UtilDates.format_sql.format(nextDate));
		}
		String dateFilter = "'" + String.join("','", listSqlDate) + "'";
		String query = "SELECT sh.date, sh.variable_name, sh.value, last.value AS value_last "
				+ CR + "FROM state_history sh "
				+ CR + "JOIN state_history AS last ON last.id = sh.id_last"
				+ CR + "WHERE sh.id_model_context = " + predictionContext.getId()
				+ CR + " AND sh.variable_name IN (" + variableFilter + ")"
				+ CR + " AND sh.date IN (" + dateFilter + ")"
				+ CR + " ORDER BY sh.date, sh.variable_name"
				;
		List<Map<String, Object>> rows = dbConnection.executeSelect(query);
		Date lastDate = null;
		Date currentDate = null;
		for(Map<String, Object> row : rows) {
			if(rows.size()>0) {
				currentDate = SapereUtil.getDateValue(row, "date", logger);
				if(lastDate == null || lastDate.getTime() != currentDate.getTime()) {
					result.put(currentDate, new NodeStatesTransitions(currentDate));
				}
				NodeStatesTransitions nodeTransition = result.get(currentDate);
				String variableName = "" + row.get("variable_name") ;
				double value = SapereUtil.getDoubleValue(row, "value", logger);
				double valueLast = SapereUtil.getDoubleValue(row, "value_last", logger);
				try {
					VariableState stateCurrent = NodeStates.getVariablState(value);
					VariableState stateLast = NodeStates.getVariablState(valueLast);
					nodeTransition.setValue(variableName, value, valueLast);
					nodeTransition.setState(variableName, stateCurrent, stateLast);
					lastDate = new Date(currentDate.getTime());
				} catch (Exception e) {
					logger.error(e);
				}
			}
		}
		return result;
	}

	public static List<VariableStateHistory> retrieveLastHistoryStates(
			PredictionContext predictionContext
			, NodeStatesTransitions nodeStateTransitions
			, Date minCreationDate
			, String variableName
			, boolean observationUpdated)  throws HandlingException {
		Long sessionId = SessionManager.getSessionId();
		List<VariableStateHistory> result = new ArrayList<VariableStateHistory>();
		String query = "SELECT state_history.*, 1+state_idx AS state_id FROM state_history WHERE "
			+ " id_model_context = " + predictionContext.getId()
			+ " AND variable_name = " + addSingleQuotes(variableName)
			+ " AND id_session = " + sessionId
			+ " AND creation_date >= " + addSingleQuotes(UtilDates.format_sql.format(minCreationDate));
		if(observationUpdated) {
			query = query +  " AND NOT observation_update IS NULL";
		}
		logger.info("retrieveLastHistoryStates : query = " + query);
		List<Map<String, Object>> rows = dbConnection.executeSelect(query);
		for(Map<String, Object> row: rows) {
			Long id = SapereUtil.getLongValue(row, "id");
			double value = SapereUtil.getDoubleValue(row, "value", logger);
			try {
				VariableStateHistory nextState = new VariableStateHistory();
				nextState.setId(id);
				nextState.setStateId(SapereUtil.getIntValue(row, "state_id"));
				nextState.setValue(value);
				nextState.setDate(SapereUtil.getDateValue(row, "date", logger));
				nextState.setStateLabel("" + row.get("state_name"));
				if(row.get("state_idx_last") != null) {
					nextState.setStateIdLast(SapereUtil.getIntValue(row, "state_idx_last"));
				}
				if(row.get("state_name_last") != null) {
					nextState.setStateLabelLast("" + row.get("state_name_last"));
				}
				result.add(nextState);
			} catch (Exception e) {
				logger.error(e);
			}
		}
		return result;
	}

	private static long auxSaveMarkovModelIterations(CompleteMarkovModel markovModel) throws HandlingException {
		String reqSeparator2 = dbConnection.getReqSeparator2();
		List<Integer> iterations = markovModel.getIterations();
		Map<Integer, Date> mapIterationDates = markovModel.getMapIterationDates();
		PredictionContext predictionContext = markovModel.getPredictionContext();
		// Insert model iterations if necessary
		StringBuffer query0 = new StringBuffer("");
		for(Integer nextIterationNumber : iterations) {
			Map<String, String> affectationModelIteration = new HashMap<>();
			affectationModelIteration.put("id_model_context", addQuotes(predictionContext.getId()));
			affectationModelIteration.put("iteration_number", addQuotes(nextIterationNumber));
			Date nextDate = mapIterationDates.get(nextIterationNumber);
			String sqlNextDate = UtilDates.format_sql_day.format(nextDate);
			affectationModelIteration.put("iteration_date", addSingleQuotes(sqlNextDate));
			affectationModelIteration.put("last_update", OP_CURRENT_DATETIME);
			Map<String, String> confictAffectationModelIteration = new HashMap<>();
			confictAffectationModelIteration.put("last_update", OP_CURRENT_DATETIME);
			String insertQueryModelIteration = dbConnection.generateInsertQuery("mc_model_iteration", affectationModelIteration, confictAffectationModelIteration);
			query0.append(insertQueryModelIteration);
			query0.append(reqSeparator2);
		}
		query0.append(CR).append("SELECT * FROM mc_model_iteration WHERE 1");
		List<Map<String, Object>> rowsModelIterations = dbConnection.executeSelect(query0.toString());
		if(rowsModelIterations == null)  {
			return -1;
		}
		return 0;
	}

	private static long auxSaveMarkovModelTrMatrices(CompleteMarkovModel markovModel)  throws HandlingException {
		PredictionContext predictionContext = markovModel.getPredictionContext();
		Integer lastIerationNumber=  markovModel.getLastIterationNumber();
		long nodeContextId = predictionContext.getNodeContext().getId();
		long predictionContextId = predictionContext.getId();
		String reqSeparator2 = dbConnection.getReqSeparator2();
		// Insert transition matrices if not done
		StringBuffer query1 = new StringBuffer("");
		for(String variable : markovModel.getUsedVariables()) {
			VariableMarkovModel varMarkovModel = markovModel.getVariableMarkovModel(variable);
			for(FeaturesKey featuresKey : varMarkovModel.getKeys(logger)) {
				// Save transition matrix
				TransitionMatrix trMatrix = varMarkovModel.getTransitionMatrix(featuresKey);// nodeTransitionMatrices.getTransitionMatrix(variable);
				if(trMatrix.getKey().getId() == null) {
					Map<String, String> affectationTrMatrix = new HashMap<>();
					affectationTrMatrix.put("id_time_window", addQuotes(featuresKey.getTimeWindowId()));
					affectationTrMatrix.put("id_node_context", addQuotes(nodeContextId));
					affectationTrMatrix.put("id_model_context", addQuotes(predictionContextId));
					affectationTrMatrix.put("variable_name", addSingleQuotes(variable));
					Map<String, String> confictAffectationTrMatrix = new HashMap<>();
					confictAffectationTrMatrix.put("last_update", OP_CURRENT_DATETIME);
					String insertQueryTrMatrix = dbConnection.generateInsertQuery("mc_transition_matrix", affectationTrMatrix, confictAffectationTrMatrix);
					query1.append(insertQueryTrMatrix);
					query1.append(reqSeparator2);
				}
			}
			//}
		}
		query1.append("UPDATE mc_transition_matrix SET last_update = " + OP_CURRENT_DATETIME + ", current_iteration_number = ")
			.append(addQuotes(lastIerationNumber))
			.append(" WHERE id_model_context = ").append(predictionContextId);
		query1.append(reqSeparator2);
		query1.append(CR).append("SELECT tm.id, tm.id_time_window, tm.variable_name FROM mc_transition_matrix AS tm WHERE tm.id_model_context =").append(predictionContextId);
		List<Map<String, Object>> rowsTrMatrix = dbConnection.executeSelect(query1.toString());
		if(rowsTrMatrix == null) {
			return -1;
		}
		for(Map<String, Object>  rowTrMatrix : rowsTrMatrix) {
			Long idTrMatrix = SapereUtil.getLongValue(rowTrMatrix, "id");
			String variable = "" + rowTrMatrix.get("variable_name");
			Integer timeWindowId = SapereUtil.getIntValue(rowTrMatrix, "id_time_window");
			FeaturesKey featuresKey = predictionContext.getFeaturesKey(timeWindowId);
			markovModel.setTransitionMatrixId(featuresKey, variable, idTrMatrix);
		}
		return 0;
	}


	/**
	 * 
	 * @param markovModel
	 * @param onlyCurrentMatrix
	 * @param saveAllIterations
	 * @return
	 * @throws HandlingException
	 */
	public static long saveMarkovModel(
			 CompleteMarkovModel markovModel
			, boolean onlyCurrentMatrix
			, boolean saveAllIterations)  throws HandlingException {
		long result = 0;
		long timeBegin = new Date().getTime();
		logger.info("saveMarkovModel begin :");
		String reqSeparator2 = dbConnection.getReqSeparator2();
		List<Integer> iterations = markovModel.getIterations();
		Integer lastIerationNumber = -1;
		if(iterations.size() > 0) {
			int idx = iterations.size()-1;
			lastIerationNumber = iterations.get(idx);
		}
		// Save model iterations
		long resultSaveIterations = auxSaveMarkovModelIterations(markovModel);
		if(resultSaveIterations < 0) {
			return resultSaveIterations;
		}

		// Save model transition matrix headers
		long resultSaveTrMatrices = auxSaveMarkovModelTrMatrices(markovModel);
		if(resultSaveTrMatrices < 0) {
			return resultSaveTrMatrices;
		}
		// Save matrix cell iteration
		StringBuffer query3 = new StringBuffer("");
		String idsTrMatrx = "";
		String sep="";
		// Clean existing matric cell iterations
		List<Long> listIdTrMatrix = markovModel.getListMatrixIds(onlyCurrentMatrix, logger);
		for(Long nextIdTrMatrix : listIdTrMatrix) {
			idsTrMatrx+= sep+SapereUtil.addSingleQuotes(nextIdTrMatrix);
			sep=",";
		}
		query3.append("DELETE ").append(sqlite ? "" : "mc_transition_matrix_cell_iteration")
			.append(" FROM mc_transition_matrix_cell_iteration WHERE id_transition_matrix IN (").append(idsTrMatrx).append(")");
		if(!saveAllIterations) {
			query3.append(" AND iteration_number = ").append(lastIerationNumber);
		}
		FeaturesKey currentFeaturesKey = markovModel.getCurrentFeaturesKey();
		List<String> iterationCells = new ArrayList<String>();
		long modelContextId = markovModel.getPredictionContext().getId();
		for(String variable : markovModel.getUsedVariables()) {
			VariableMarkovModel varMarkovModel = markovModel.getVariableMarkovModel(variable);
			// For debug
			/*
			for(FeaturesKey featuresKey : varMarkovModel.getKeys()) { 
				TransitionMatrixKey trMKey = varMarkovModel.getTransitionMatrix(featuresKey).getKey();
				logger.info("trMKey = " + trMKey);
			}*/
			for(FeaturesKey featuresKey : varMarkovModel.getKeys(logger)) { // markovModel.getContent().keySet()) {
				boolean isCurrentFeatureKey = currentFeaturesKey != null && currentFeaturesKey.equals(featuresKey);
				if(!onlyCurrentMatrix || isCurrentFeatureKey) {
					TransitionMatrix trMatrix = varMarkovModel.getTransitionMatrix(featuresKey); // nodeTransitionMatrices.getTransitionMatrix(variable);
					Long idTrMatrix = trMatrix.getKey().getId();
					// Retrieve ID of transition matrix
					IterationMatrix completeObsMatrix = trMatrix.getCompleteObsMatrix();
					if(completeObsMatrix!=null) {
						for(Integer nextIterationNumber : iterations) {
							if(saveAllIterations || lastIerationNumber == nextIterationNumber) {
								DoubleMatrix iterObsMatrix = completeObsMatrix.generateMatrixAtIteration(nextIterationNumber);
								DoubleMatrix iterCorrectionMatrix = new DoubleMatrix(iterObsMatrix.getRowDimension(), iterObsMatrix.getColumnDimension());
								for(int rowIdx = 0; rowIdx < iterObsMatrix.getRowDimension(); rowIdx++) {
									for(int columnIdx = 0; columnIdx < iterObsMatrix.getColumnDimension(); columnIdx++) {
										double iterObsNb = iterObsMatrix.get(rowIdx, columnIdx);
										double iterCorrectionNb = iterCorrectionMatrix.get(rowIdx, columnIdx);
										double iterObAndCorrectionNb = iterObsNb + iterCorrectionNb;
										if(iterObAndCorrectionNb != 0) {
											StringBuffer buffCellIteration = new StringBuffer();
											buffCellIteration.append("(").append(idTrMatrix).append(",").append(modelContextId).append(",").append(nextIterationNumber).append(",").append(rowIdx).append(",").append(columnIdx).append(",").append(iterObsNb).append(",").append(iterCorrectionNb).append(")");
											iterationCells.add(buffCellIteration.toString());
										}
									}
								}
							}
						}
					}
				} // end for
			}
		}
		// Check if there are some interation cells to save
		if(iterationCells.size()>0) {
			query3.append(reqSeparator2)
				 .append(CR).append("INSERT INTO mc_transition_matrix_cell_iteration(id_transition_matrix,id_model_context, iteration_number,row_idx,column_idx,obs_number,corrections_number)")
				 .append(" VALUES ").append(CR).append(" ");
			;
			query3.append(SapereUtil.implode(iterationCells, CR+","));
			String[] variables = markovModel.getPredictionContext().getNodeContext().getVariables();
			for(String variable : variables) {
				Long lastStateHistoryId = markovModel.getNodeStatesTransitions().getStateHistoryId(variable);
				if(lastStateHistoryId != null) {
					query3.append(reqSeparator2)
						.append(CR).append("UPDATE state_history SET observation_update = ").append(OP_CURRENT_DATETIME).append("WHERE id =" + addQuotes(lastStateHistoryId));
				}
			}
			//refreshTransitionMatrixCells = true;
		}
		//logger.info("saveMarkovModel : query3 = " + query3.toString());
		long result3 = dbConnection.execUpdate(query3.toString());
		if(result3 < 0) {
			logger.error("saveMarkovModel error in query 3 " + query3);
			return result3;
		} else {
			result = result3;
		}
		logger.info("saveMarkovModel : result = " + result);
		long timeEnd = new Date().getTime();
		long timeSpentMS = timeEnd - timeBegin;
		logger.info("saveMarkovModel end : timeSpentMS = " + timeSpentMS + ", result = " + result);
		return result;
	}



	public static Integer addPredictionCorrection(
			 PredictionContext predictionContext
			,PredictionDeviation deviation
			,String tag
			)  throws HandlingException {
		if(!deviation.isMeaningful()) {
			// nothing to do.
			return null;
		}
		logger.info("addPredictionCorrection : deviation = " + deviation);
		Long sessionId = SessionManager.getSessionId();
		String reqSeparator2 = dbConnection.getReqSeparator2();
		int initialStateIdx = deviation.getInitialState().getIndex();
		int overStateIdx = deviation.getStateOver().getIndex();
		int underStateIdx = deviation.getStateUnder().getIndex();
		long idTransitionMatrix = deviation.getTransitionMatrixKey().getId();
		long modelContextid = predictionContext.getId();
		int rowIdx = overStateIdx; 	// first version : does not work very well
		double excess = deviation.getExcess();
		rowIdx = initialStateIdx; // works better ???
		StringBuffer query = new StringBuffer();
		query.append("SET @it_number = (SELECT current_iteration_number FROM mc_transition_matrix tm where id = ")
			.append(idTransitionMatrix).append(")")
		;
		query.append(reqSeparator2);
		query.append(CR).append("SET @row_sum = (SELECT IFNULL(SUM(obs_number + corrections_number),0) FROM v_mc_transition_matrix_cell AS tmc ")
			.append(" WHERE tmc.id_transition_matrix = ").append(idTransitionMatrix)
			.append(" AND row_idx = ").append(rowIdx).append(")")
		;
		query.append(reqSeparator2);
		//query.append(CR).append("SET @added_corrections_number = GREATEST(1, ROUND(0.1*(0.5*@row_sum - @cell_sum)))");
		//query.append(reqSeparator2);
		query.append(CR).append("SET @added_corrections_number = GREATEST(1, ROUND(0.1*").append(excess).append(" * @row_sum))");
		query.append(reqSeparator2);
		query.append(CR).append("INSERT INTO mc_transition_matrix_cell_iteration SET")
				.append(CR).append(",id_transition_matrix = ").append(idTransitionMatrix)
				.append(CR).append(",id_model_context = ").append(modelContextid)
				.append(CR).append(" iteration_number = @it_number")
				.append(CR).append(",row_idx = ").append(rowIdx)
				.append(CR).append(",column_idx = ").append(overStateIdx)
				.append(CR).append(",corrections_number = -1*@added_corrections_number")
				.append(CR).append(" ON DUPLICATE KEY UPDATE corrections_number = -1*@added_corrections_number + corrections_number")
		;
		query.append(reqSeparator2);
		query.append(CR).append("INSERT INTO mc_transition_matrix_cell_iteration SET")
				.append(CR).append(",id_transition_matrix = ").append(idTransitionMatrix)
				.append(CR).append(",id_model_context = ").append(modelContextid)
				.append(CR).append(",iteration_number = @it_number")
				.append(CR).append(",row_idx = ").append(rowIdx)
				.append(CR).append(",column_idx = ").append(underStateIdx)
				.append(CR).append(",corrections_number = 1*@added_corrections_number")
				.append(CR).append(" ON DUPLICATE KEY UPDATE corrections_number = 1*@added_corrections_number + corrections_number")
		;
		query.append(reqSeparator2);
		query.append(CR).append("UPDATE mc_transition_matrix SET last_update=NOW() WHERE id = ").append(idTransitionMatrix);
		query.append(reqSeparator2);
		query.append(CR).append("INSERT INTO log_mc_self_correction SET")
			.append(CR).append(" id_session = ").append(sessionId)
			.append(CR).append(",tag = ").append(addSingleQuotes(tag))
			.append(CR).append(",id_transition_matrix = ").append(idTransitionMatrix)
			.append(CR).append(",iteration_number = @it_number")
			.append(CR).append(",it_number = @it_number")
			.append(CR).append(",initial_state_idx = ").append(rowIdx)
			.append(CR).append(",from_state_idx = ").append(overStateIdx)
			.append(CR).append(",dest_state_idx = ").append(underStateIdx)
			.append(CR).append(",cardinality = ").append(deviation.getCardinality())
			.append(CR).append(",excess = ").append(addQuotes(excess))
			.append(CR).append(",cell_sum = IFNULL(@cell_sum,0)")
			.append(CR).append(",row_sum = @row_sum")
			.append(CR).append(",corrections_number = @added_corrections_number")
		 ;
		/*
		if(predictionId != null) {
			query.append(CR).append(",id_prediction = ").append(predictionId);
		}*/
		query.append(reqSeparator2);
		query.append(CR).append("SET @id_correction = LAST_INSERT_ID()");
		String sPredictionIds = deviation.getStrListIdPredictions(",");
		if(sPredictionIds.length() > 0) {
			query.append(reqSeparator2);
			query.append("UPDATE prediction SET id_correction = @id_correction WHERE id IN (").append(sPredictionIds).append(")");
		}
		query.append(reqSeparator2);
		query.append(CR).append("SELECT * FROM log_mc_self_correction WHERE id = @id_correction");
		/*
		query.append(CR).append("UPDATE mc_transition_matrix_cell_iteration tmci SET tmci.errors_number = tmci.errors_number + 1 ")
			.append(" WHERE tmci.id_transition_matrix = ").append(idTransitionMatrix).append(" AND tmci.iteration_number = @it_number AND tmci.row_idx = " + fromStateIdx + " AND tmci.column_idx = " + destStateIdx )
		;*/
		dbConnection.setDebugLevel(10);
		Map<String, Object> row = dbConnection.executeSingleRowSelect(query.toString());
		Integer correctionNumber = SapereUtil.getIntValue(row, "corrections_number");
		logger.info("addPredictionError  : correctionNumber = " + correctionNumber);
		dbConnection.setDebugLevel(0);
		//refreshTransitionMatrixCell(predictionContext, timeWindowId);
		return correctionNumber;
	}


	public static Map<String, Object> consolidatePredictions(PredictionContext predictionContext
			//,Date minComputeDate
			//,Date maxComputeDate
			) throws HandlingException {
		String sep2 =  dbConnection.getReqSeparator2()+ CR;
		String reqSeparator2 = dbConnection.getReqSeparator2() + CR;
		long predictionContextId = predictionContext.getId();
		StringBuffer query0 = new StringBuffer();
		query0.append("SELECT MIN(creation_date) AS min_creation_date, MAX(creation_date) max_creation_date FROM prediction WHERE NOT link_done AND id_model_context = ").append(predictionContextId);
		Map<String, Object> rowDateMin = dbConnection.executeSingleRowSelect(query0.toString());
		Date minComputeDate = SapereUtil.getDateValue(rowDateMin, "min_creation_date", logger);
		Date maxComputeDate = SapereUtil.getDateValue(rowDateMin, "max_creation_date", logger);
		Date currentDate = predictionContext.getCurrentDate();
		Date maxTargetDate = UtilDates.shiftDateMinutes(currentDate, -1);
		StringBuffer query1 = new StringBuffer();
		String defaultBitValue = sqlite ? "0" : "b'0'";
		query1.append("DROP ").append(OP_TEMPORARY).append(" TABLE IF EXISTS TmpPrediction");
		query1.append(sep2);
		String opTemprary2 = isModeDebug? "" : "TEMPORARY";
		query1.append("CREATE ").append(opTemprary2).append(" TABLE TmpPrediction AS") // FOR DEBUG : ADD TEMPORARY !!!!
			.append(CR).append( "	SELECT id, horizon_minutes")
			.append(CR).append("	,p.creation_date")
			.append(CR).append("	,p.target_date")
			.append(CR).append("	,p.variable_name")
			.append(CR).append("	,p.id_target_state_histo")
			// sqlite? "DATETIME('now', '-1 hour', 'localtime')"	: "DATE_ADD(NOW(), INTERVAL -1 HOUR)";
			.append(CR).append(sqlite? "	,DATETIME(p.target_date, '-120 seconds') AS date_min"
									: "		,DATE_ADD(p.target_date, INTERVAL  -120 SECOND) AS date_min")
			.append(CR).append(sqlite? "	,DATETIME(p.target_date, '-120 seconds') AS date_max"
									: "		,DATE_ADD(p.target_date, INTERVAL  +120 SECOND) AS date_max")
			.append(CR).append("		,").append(unixTimeOp).append("(creation_date) AS ut_creation_date")
			.append(CR).append("		,").append(unixTimeOp).append("(target_date) AS ut_target_date")
			.append(CR).append("		,").append(unixTimeOp).append("(target_date) - 120 AS ut_target_date_min")
			.append(CR).append("		,").append(unixTimeOp).append("(target_date) + 120 AS ut_target_date_max")
			// day-hour of the target date
			.append(CR).append("		,").append(unixTimeOp).append("(target_date) - ")
											   .append(unixTimeOp).append("(target_date) % 3600 AS slot_target_date")
			.append(CR).append("	FROM prediction p")
		//		.append("		-- WHERE p.target_date  >= p_min_date AND p.target_date < p_max_date AND id_target_state_histo IS NULL AND NOT link_done")
			.append(CR).append("		WHERE p.id_model_context  = ").append(predictionContextId);
		if(minComputeDate != null) {
			String sqlMinComputeDate = addSingleQuotes(UtilDates.format_sql.format(minComputeDate));
			query1.append(CR).append(" 		AND p.creation_date  >= ").append(sqlMinComputeDate);
		}
		if(maxComputeDate != null) {
			String sqlMaxComputeDate =  addSingleQuotes(UtilDates.format_sql.format(maxComputeDate));
			query1.append(CR).append("		AND p.creation_date < ").append(sqlMaxComputeDate);
		}
		if(maxTargetDate != null) {
			String sqlMaxTargetDate = addSingleQuotes(UtilDates.format_sql.format(maxTargetDate));
			query1.append(CR).append("		AND p.target_date <= ").append(sqlMaxTargetDate);
		}
		query1.append(CR).append("		AND id_target_state_histo IS NULL AND NOT link_done");
		query1.append(sep2);
		//query1.append("ALTER TABLE TmpPrediction ADD COLUMN is_slot_ok BIT(1) DEFAULT ").append(defaultBitValue);
		//.query1.append(sep2);
		query1.append("ALTER TABLE TmpPrediction ADD COLUMN link_done BIT(1) DEFAULT ").append(defaultBitValue);
		query1.append(sep2);
		//query1.append("UPDATE TmpPrediction SET is_slot_ok = ut_target_date_min >= slot_target_date AND ut_target_date_max < slot_target_date + 3600");
		//query1.append(sep2);
		query1.append("SELECT MIN(target_date) AS v_min_target_date")
			.append(CR).append(", MIN(creation_date) AS v_min_creation_date")
			.append(CR).append("FROM TmpPrediction");
		Map<String, Object> row = dbConnection.executeSingleRowSelect(query1.toString());
		Date vMinTargetDate = SapereUtil.getDateValue(row, "v_min_target_date", logger);
		String sqlMinTargetDate =  (vMinTargetDate == null) ? "null" : addSingleQuotes(UtilDates.format_sql.format(vMinTargetDate));
		//Date vMinCreationDate = SapereUtil.getDateValue(row, "v_min_creation_date", logger);
		//String sqlMinCreationDate = (vMinCreationDate == null) ? "null" : addSingleQuotes(UtilDates.format_sql.format(vMinCreationDate));
		StringBuffer query2 = new StringBuffer();
		query2.append("DROP TABLE IF EXISTS TmpSH");
		query2.append(sep2);
		query2.append("DROP ").append(OP_TEMPORARY).append(" TABLE IF EXISTS TmpSH");
		query2.append(sep2);
		query2.append("CREATE ").append(opTemprary2).append(" TABLE TmpSH AS")
			.append(CR).append("	SELECT ID, date, ").append(unixTimeOp).append("(date) AS ut_date")
			.append(CR).append("	,creation_date")
			.append(CR).append("	,").append(unixTimeOp).append("(creation_date) AS ut_creation_date")
			.append(CR).append("	,").append(unixTimeOp).append("(date) - ").append(unixTimeOp).append("(date) % 3600 AS slot_date")
			.append(CR).append("	,variable_name, id_model_context, id_node_context")
			.append(CR).append("	FROM state_history AS sh")
			.append(CR).append("	WHERE sh.id_model_context = ").append(predictionContextId)
			.append(CR).append("		AND sh.date >= ").append(sqlMinTargetDate);
	//		.append(CR).append("		AND sh.creation_date  >= ").append(sqlMinCreationDate)
		if(LearningModelType.MARKOV_CHAINS.equals(predictionContext.getModelType())) {
			query2.append(CR).append("		AND NOT sh.observation_update IS NULL");
		}
		query2.append(reqSeparator2);
		query2.append("CREATE UNIQUE INDEX id_TmpSH ON TmpSH(id)");
		query2.append(reqSeparator2);
		if(sqlite) {
			query2.append("CREATE INDEX date_tmpsh ON TmpSH(date)");
		} else {
			query2.append("ALTER TABLE TmpSH ADD KEY(date)");
		}
		query2.append(reqSeparator2);
		if(sqlite) {
			query2.append("CREATE INDEX date_var_tmpsh ON TmpSH(ut_date, variable_name)");
		} else {
			query2.append("ALTER TABLE TmpSH ADD KEY(ut_date, variable_name)");
		}
		query2.append(reqSeparator2);
		if(sqlite) {
			query2.append("CREATE INDEX date2_var_tmpsh ON TmpSH(slot_date, ut_date, variable_name)");
		} else {
			query2.append("ALTER TABLE TmpSH ADD KEY(slot_date, ut_date, variable_name)");
		}
		query2.append(reqSeparator2);
		query2.append("DROP ").append(OP_TEMPORARY).append("TABLE IF EXISTS TmpReconciliation");
		query2.append(reqSeparator2);
		query2.append("CREATE ").append(opTemprary2).append(" TABLE TmpReconciliation AS")
			.append(CR).append("	SELECT p.id AS id_prediction, sh.id AS id_state_history")
			//.append(CR).append("	,is_slot_ok")
			//.append(CR).append("	,(p.slot_target_date = sh.slot_date) AS is_slot_ok2")
			.append(CR).append("	,(p.slot_target_date = sh.slot_date) AS same_slot")
			.append(CR).append("	,ABS(p.ut_target_date - sh.ut_date) AS distance")
			.append(CR).append("	,p.ut_target_date - sh.ut_date AS delta")
			.append(CR).append("	,sh.ut_date")
			.append(CR).append("	FROM TmpPrediction AS p")
			.append(CR).append("	JOIN TmpSH AS sh ON ABS(p.ut_target_date - sh.ut_date) < 120")
			.append(CR).append("		AND sh.variable_name  = p.variable_name")
			.append(CR).append("		AND ABS(p.ut_creation_date - sh.ut_creation_date) < 3600*24");
		//.append(CR).append("	ORDER BY ABS(p.ut_target_date - sh.ut_date)");
		query2.append(sep2);
		query2.append("ALTER TABLE TmpReconciliation ADD COLUMN is_slot_ok3 BIT(1) DEFAULT ").append(defaultBitValue);
		query2.append(sep2);
		//query2.append("UPDATE TmpReconciliation SET is_slot_ok3 = (is_slot_ok = is_slot_ok2) ");
		//query2.append(sep2);
		query2.append("UPDATE Prediction AS p SET id_target_state_histo = (SELECT rec.id_state_history")
			.append(CR).append("	FROM TmpReconciliation AS rec")
			.append(CR).append("	WHERE rec.id_prediction = p.id AND same_slot")
			//.append(CR).append("   ORDER BY rec.is_slot_ok DESC, rec.distance, rec.ut_date")
			.append(CR).append("   ORDER BY rec.distance, rec.ut_date")
			.append(CR).append("   LIMIT 0,1)")
			.append(CR).append("   WHERE p.id IN (SELECT id_prediction FROM TmpReconciliation)  AND p.id_target_state_histo IS NULL");
		query2.append(reqSeparator2);
		query2.append("UPDATE Prediction AS p SET delta_target_state_histo = (SELECT rec.delta FROM TmpReconciliation AS rec")
		.append(CR).append("		WHERE rec.id_prediction = p.id AND rec.id_state_history = p.id_target_state_histo LIMIT 0,1)")
		.append(CR).append(" 	WHERE p.id IN (SELECT id_prediction FROM TmpReconciliation)  AND p.id_target_state_histo IS NULL");
		query2.append(reqSeparator2);
		query2.append("UPDATE Prediction AS p SET link_done = 1 WHERE NOT link_done AND NOT id_target_state_histo IS NULL");
		query2.append(reqSeparator2);
		query2.append("SELECT ")
			.append(CR).append("	 (SELECT Count(*) FROM Prediction WHERE NOT id_target_state_histo IS NULL) AS nbTotalPredictionReconciled")
			.append(CR).append("	,(SELECT Count(*) FROM Prediction WHERE id IN (SELECT id_prediction FROM TmpReconciliation) AND NOT id_target_state_histo IS NULL) AS nbPredictionReconciled")
			.append(CR).append("	,(SELECT Count(*) FROM TmpReconciliation) AS NbTmpReconciliation")
			.append(CR).append("	,(SELECT Count(*) FROM TmpSH) AS NbTmpSH")
		;
		Map<String, Object> rowResult = dbConnection.executeSingleRowSelect(query2.toString());
		if(rowResult == null) {
			logger.warning("consolidatePredictions : sql error");
		} else {
			int nbTotalPredictionReconciled = SapereUtil.getIntValue(rowResult, "nbTotalPredictionReconciled");
			int nbTmpSH = SapereUtil.getIntValue(rowResult, "NbTmpSH");
			if(nbTotalPredictionReconciled == 0 || false) {
				List<String> errors  = new ArrayList<String>();
				errors.add("No prediciton has been reconciled with a state");
				if(nbTmpSH == 0) {
					errors.add("No row in TmpSH (no state with obs update after "+ sqlMinTargetDate + ")"); ;
				}
				throw new HandlingException("" + errors);
			}
		}
		for(String key1 : row.keySet()) {
			rowResult.put(key1, row.get(key1));
		}
		StringBuffer query3 = new StringBuffer();
		query3.append(CR).append("DROP ").append(OP_TEMPORARY).append(" TABLE IF EXISTS TmpStateDistrib2");
		query3.append(sep2);
		//query3.append("UPDATE Prediction SET sum_state_distrib=0,  vector_differential=NULL, cross_entropy_loss=NULL");
		//query3.append(sep2);
		query3.append("CREATE TEMPORARY TABLE TmpStateDistrib2 AS")
			.append(CR).append( "	SELECT id_model_context, ut_begin_slot, variable_name, state_idx, ratio")
			.append(CR).append( "	FROM v_state_distribution");
		query3.append(sep2);
		query3.append("CREATE INDEX _key_stateDistrib2 ON TmpStateDistrib2 (id_model_context, ut_begin_slot, variable_name)");
		query3.append(sep2);
		query3.append("UPDATE Prediction SET sum_state_distrib = (SELECT IFNULL(SUM(TmpStateDistrib2.ratio),0)")
			.append(CR).append("	FROM TmpStateDistrib2")
			.append(CR).append("	WHERE   TmpStateDistrib2.id_model_context = Prediction.id_model_context")
			.append(CR).append("		AND TmpStateDistrib2.ut_begin_slot = Prediction.ut_target_begin_slot")
			.append(CR).append("		AND TmpStateDistrib2.variable_name = Prediction.variable_name)")
			.append(CR).append("	WHERE ABS(IFNULL(sum_state_distrib,0) - 1) > 0.01")
			;
		query3.append(sep2);
		query3.append("UPDATE Prediction SET vector_differential = ")
			.append(CR).append("		(SELECT 0.5*SUM(ABS(predItem.proba - IFNULL(TmpStateDistrib2.ratio,0)))")
			.append(CR).append("		FROM prediction_item AS predItem ")
			.append(CR).append("		LEFT JOIN TmpStateDistrib2 ON ")
			.append(CR).append("		   		TmpStateDistrib2.id_model_context = Prediction.id_model_context")
			.append(CR).append("		    AND TmpStateDistrib2.ut_begin_slot = Prediction.ut_target_begin_slot")
			.append(CR).append("			AND TmpStateDistrib2.variable_name = Prediction.variable_name")
			.append(CR).append("			AND TmpStateDistrib2.state_idx = predItem.state_idx")
			.append(CR).append("		WHERE predItem.id_prediction = Prediction.id)")
			.append(CR).append("WHERE ABS(sum_state_distrib - 1) < 0.01");
		query3.append(sep2);
		// Cross entropy
		query3.append("UPDATE Prediction SET cross_entropy_loss = ")
			.append(CR).append("	 (SELECT -1*SUM(TmpStateDistrib2.ratio * ln(predItem.proba))")
			.append(CR).append("		FROM prediction_item AS predItem ")
			.append(CR).append("		LEFT JOIN TmpStateDistrib2 ON ")
			.append(CR).append("		   		TmpStateDistrib2.id_model_context = Prediction.id_model_context")
			.append(CR).append("		   	AND TmpStateDistrib2.ut_begin_slot = Prediction.ut_target_begin_slot")
			.append(CR).append("			AND TmpStateDistrib2.variable_name = Prediction.variable_name")
			.append(CR).append("			AND TmpStateDistrib2.state_idx = predItem.state_idx")
			.append(CR).append("		WHERE predItem.id_prediction = Prediction.id)")
			.append(CR).append("WHERE ABS(sum_state_distrib - 1) < 0.01");
		//query3.append(sep2);
		//query3.append(CR).append("_DROP ").append(OP_TEMPORARY).append(" TABLE IF EXISTS TmpStateDistrib2");
		//query3.append(sep2);
		long result = dbConnection.execUpdate(query3.toString());
		if(result<0) {
			logger.warning("consolidatePredictions : sql error");
		}
		return rowResult;
	}

	public static long setObservationUpdates(PredictionContext predictionContext) throws HandlingException {
		StringBuffer query = new StringBuffer();
		long predictionContextId = predictionContext.getId();
		Long sessionId = getSessionId();
		query.append(CR).append("UPDATE state_history SET observation_update = ").append(OP_CURRENT_DATETIME)
				.append(CR).append(" WHERE id_model_context = ").append(predictionContextId)
				.append(" AND id_session = " + sessionId + " AND  observation_update IS NULL");
		long result = dbConnection.execUpdate(query.toString());
		return result;
	}

	public static List<PredictionResult> retrieveListPredictionReults(
			 PredictionContext predictionContext
			 , Date computeDayFilter
			,TimeSlot targetTimeSLot)  throws HandlingException {
		PredictionScope scope = predictionContext.getScope();
		Date minDate = UtilDates.removeTime(targetTimeSLot.getBeginDate());
		Date maxDate = UtilDates.removeTime(targetTimeSLot.getEndDate());
		int minHour = UtilDates.getHourOfDay(targetTimeSLot.getBeginDate());
		int maxHour = UtilDates.getHourOfDay(targetTimeSLot.getEndDate());
		long timeShiftMS = predictionContext.getTimeShiftMS();
		Map<String, StatesStatistic> mapStateStatistics = retrieveStatesStatistics(predictionContext, minDate, maxDate, minHour, maxHour, null);
		if(mapStateStatistics.size() == 0) {
			return new ArrayList<>();
		}
		String sqlDateMin = UtilDates.format_sql.format(targetTimeSLot.getBeginDate());
		String sqlDateMax =  UtilDates.format_sql.format(targetTimeSLot.getEndDate());
		String sqlComputeDayFilter = (computeDayFilter == null) ?"1" : "Date(p.creation_date) = " + addSingleQuotes(UtilDates.format_sql.format(computeDayFilter));
		StringBuffer query = new StringBuffer();
		query.append("SELECT p.id_initial_transition_matrix, p.id")
			.append(CR).append(" ,p.variable_name ")
			.append(CR).append(" ,p.initial_date")
			.append(CR).append(" ,p.target_date")
			.append(CR).append(" ,DATE(p.creation_date) AS compute_day")
			.append(CR).append(" ,p.initial_state_idx AS initialStateIdx")
			.append(CR).append(" ,p.likely_state_idx AS predictedStateIdx")
			.append(CR).append(" ,p.likely_state_proba AS predictedStateProba")
			.append(CR).append(" ,(SELECT tr.id_time_window FROM mc_transition_matrix tr WHERE tr.id=p.id_target_transition_matrix) AS targetTWid")
			.append(CR).append(" ,sh.state_idx AS actualStateIdx ")
			.append(CR).append("FROM prediction p")
			//.append(CR).append("JOIN prediction_item pi2 ON pi2.id_prediction = p.id")
			.append(CR).append("JOIN state_history sh ON sh.id = p.id_target_state_histo")
			.append(CR).append("WHERE p.target_date >= ").append(addSingleQuotes(sqlDateMin))
			.append(CR).append(" AND p.target_date <= ").append(addSingleQuotes(sqlDateMax))
			.append(CR).append(" AND ").append(sqlComputeDayFilter)
			.append(CR).append(" AND p.use_corrections ")
			.append(CR).append(" AND NOT p.id_target_state_histo IS NULL ")
			//.append(CR).append(" AND NOT p.likely_state_idx = sh.state_idx")
			.append(CR).append(" AND p.id_correction IS NULL")
			.append(CR).append(" AND p.id_initial_transition_matrix = p.id_target_transition_matrix")
		;
		List<Map<String, Object>> rows = dbConnection.executeSelect(query.toString());
		Map<Long, PredictionResult> mapPredictions = new HashMap<>();
		for(Map<String, Object> row : rows) {
			//Long idTransitionMatrix = SapereUtil.getLongValue(row, "id_initial_transition_matrix");
			int initialStateIdx = SapereUtil.getIntValue(row, "initialStateIdx");
			//int predictedStateIdx = SapereUtil.getIntValue(row, "predictedStateIdx");
			int actualStateIdx = SapereUtil.getIntValue(row, "actualStateIdx");
			long predictionId = SapereUtil.getLongValue(row, "id");
			//double predictedStateProba = SapereUtil.getDoubleValue(row, "predictedStateProba");
			Date targetDate = SapereUtil.getDateValue(row, "target_date", logger);
			int targetHour = UtilDates.getHourOfDay(targetDate);
			Date initialDate = SapereUtil.getDateValue(row, "initial_date", logger);
			String variable = "" + row.get("variable_name");
			//Date computeDay = SapereUtil.getDateValue(row, "compute_day", logger);
			int targetTimeWindowId =  SapereUtil.getIntValue(row, "targetTWid");
			FeaturesKey targetFeaturesKey = predictionContext.getFeaturesKey(targetTimeWindowId);
			//String statKey = StatesStatistic.generateKey(computeDay, targetDate, targetHour, variable);
			String statKey = StatesStatistic.generateKey(scope, targetDate, targetHour, variable);
			StatesStatistic actualStatesStatistics = mapStateStatistics.get(statKey);
			//TransitionMatrixKey trMatrixKey = predictionContext.getTransitionMatrixKey(targetTimeWindowId, variable);
			VariableState initialState = NodeStates.getById(1+initialStateIdx);
			VariableState actualState = NodeStates.getById(1+actualStateIdx);
			PredictionResult predictionResult = new PredictionResult(initialDate, initialState, targetDate, variable, targetFeaturesKey, timeShiftMS);
			predictionResult.setActualTargetState(actualState);
			predictionResult.setPredictionId(predictionId);
			predictionResult.setActualStatesStatistic(actualStatesStatistics);
			mapPredictions.put(predictionId, predictionResult);
			logger.info("applyPredictionSelfCorrection : initialState = " + initialState);
		}
		if(mapPredictions.size() > 0) {
			List<String> listIds = new ArrayList<String>();
	        for (long id : mapPredictions.keySet()) {
				listIds.add("" + id);
	        }
			String slistIds = String.join(",", listIds) ;
			List<Map<String, Object>> rows2 = dbConnection.executeSelect("SELECT * FROM prediction_item WHERE id_prediction IN (" + slistIds + ")");
			for(Map<String, Object> row2 : rows2) {
				Integer stateIdx = SapereUtil.getIntValue(row2, "state_idx");
				Long predictionId = SapereUtil.getLongValue(row2, "id_prediction");
				Double proba = SapereUtil.getDoubleValue(row2, "proba", logger);
				if(mapPredictions.containsKey(predictionId)) {
					PredictionResult predictionResult = mapPredictions.get(predictionId);
					predictionResult.setStateProbabilities(stateIdx, proba);
				}
			}
		}
		List<PredictionResult> result = new ArrayList<>();
		for(PredictionResult nextPredictionResult : mapPredictions.values()) {
			// checkup : sum proba = 1
			List<Double> listProba = nextPredictionResult.getStateProbabilities();
		    Double sum = listProba.stream().reduce(0.0, Double::sum);
		    if(Math.abs(sum - 1) <= 0.0001) {
			    result.add(nextPredictionResult);
		    } else {
			    logger.warning("retrieveListPredictionReults : sum proba = " + sum + " for prediction result " + nextPredictionResult) ;
		    }
		}
		return result;
	}


	public static void savePredictionResult(PredictionData prediction) throws HandlingException {
		String reqSeparator2 = dbConnection.getReqSeparator2();
		PredictionContext predictionCtx = prediction.getPredictionContext();
		Date initialDate = prediction.getInitialDate();
		Date maxInitialDate = UtilDates.shiftDateMinutes(initialDate, 60);
		String sqlInitialDate = addSingleQuotes(UtilDates.format_sql.format(initialDate));
		String sqlMaxInitialDate = addSingleQuotes(UtilDates.format_sql.format(maxInitialDate));
		StringBuffer sqlCleanPredictions = new StringBuffer();
		String maxCreationDate = sqlite? "DATETIME('now', '-1 hour', 'localtime')"
										: "DATE_ADD(NOW(), INTERVAL -1 HOUR)";
		StringBuffer cleanFilter = new StringBuffer();
		cleanFilter.append(" initial_date BETWEEN ").append(sqlInitialDate).append(" AND ").append(sqlMaxInitialDate)
			.append(" AND creation_date < ").append(maxCreationDate)
			.append(" AND id_model_context = ").append(predictionCtx.getId())
			;
		sqlCleanPredictions.append(reqSeparator2);
		sqlCleanPredictions.append(CR).append("DELETE ").append(sqlite ? "" : "pi").append(" FROM prediction_item AS pi WHERE pi.id_prediction IN ")
			.append(" (SELECT id FROM Prediction WHERE ").append(cleanFilter).append(")");
		sqlCleanPredictions.append(reqSeparator2);
		sqlCleanPredictions.append(CR).append("DELETE ").append(sqlite ? "" : "p").append(" FROM prediction AS p WHERE p.id IN ")
			.append(" (SELECT id FROM Prediction WHERE ").append(cleanFilter).append(")");
		dbConnection.execUpdate(sqlCleanPredictions.toString());
		for(Date targetDate : prediction.getTargetDates()) {
			PredictionStep firstStep = prediction.getFirstStep();
			PredictionStep lastStep = prediction.getLastStep(targetDate);
			for(String variableName : prediction.getVariables()) {
				if(prediction.hasResult(variableName, targetDate)) {
					PredictionResult predictionResult = prediction.getResult(variableName, targetDate);
					long horizonMinutes = Math.round(predictionResult.getTimeHorizonMinutes());
					Map<String, String> predictionAffectation = new HashMap<>();
					predictionAffectation.put("variable_name", addSingleQuotes(variableName));
					predictionAffectation.put("id_model_context", ""+predictionCtx.getId());
					predictionAffectation.put("id_node_context", ""+predictionCtx.getNodeContext().getId());
					predictionAffectation.put("initial_date", sqlInitialDate);
					predictionAffectation.put("target_date", addSingleQuotes(UtilDates.format_sql.format(targetDate)));
					predictionAffectation.put("target_day", addSingleQuotes(UtilDates.format_sql_day.format(targetDate)));
					predictionAffectation.put("target_hour", addQuotes(UtilDates.getHourOfDay(targetDate)));
					predictionAffectation.put("horizon_minutes", ""+horizonMinutes);
					predictionAffectation.put("use_corrections", prediction.isUseCorrections()?"1":"0");
					if(firstStep != null) {
						Long initialTRid = firstStep.getUsedTransitionMatrixId(variableName);
						predictionAffectation.put("id_initial_transition_matrix", ""+ initialTRid );
					}
					if(lastStep != null) {
						Long initialTRid = lastStep.getUsedTransitionMatrixId(variableName);
						predictionAffectation.put("id_target_transition_matrix", ""+initialTRid);
					}
					TimeWindow initialTimeWindow = prediction.getInitialFeaturesKey().getTimeWindow();
					if(initialTimeWindow != null) {
						predictionAffectation.put("id_initial_time_window", ""+initialTimeWindow.getId());
					}
					VariableState initialState = predictionResult.getInitialState();
					if(initialState != null) {
						predictionAffectation.put("initial_state_idx", ""+initialState.getIndex());
						predictionAffectation.put("initial_state_name", addSingleQuotes(initialState.getName()));
					}
					List<Double> stateProbabilities = predictionResult.getStateProbabilities();
					VariableState randomState = predictionResult.getRandomTargetState();
					if(randomState == null) {
						logger.warning("savePredictionResult for debug : randomState is null");
					}
					if(randomState !=null) {
						int stateIdx = randomState.getIndex();
						String stateName = randomState.getName();
						double stateProba = predictionResult.getStateProbability(stateIdx);
						predictionAffectation.put("random_state_idx", ""+stateIdx);
						predictionAffectation.put("random_state_name", addSingleQuotes(stateName));
						predictionAffectation.put("random_state_proba", addQuotes(stateProba));
					}
					if(predictionResult.getLikelyTargetValue() != null) {
						predictionAffectation.put("likely_value", ""+ predictionResult.getLikelyTargetValue());
					}
					VariableState mostLikelyState = predictionResult.getMostLikelyState();
					if(mostLikelyState != null) {
						int stateIdx = mostLikelyState.getIndex();
						double stateProba = predictionResult.getStateProbability(stateIdx);
						predictionAffectation.put("likely_state_idx", ""+stateIdx);
						predictionAffectation.put("likely_state_name", addSingleQuotes(mostLikelyState.getName()));
						predictionAffectation.put("likely_state_proba", addQuotes(stateProba));
					}
					String sqlInsertPrediction = dbConnection.generateInsertQuery("prediction", predictionAffectation);
					long predictionId = dbConnection.execUpdate(sqlInsertPrediction);
					StringBuffer sqlInsertItems = new StringBuffer("INSERT INTO prediction_item(id_prediction,state_idx,state_name,proba) VALUES ");
					int stateIndex = 0;
					for(Double nextValue : stateProbabilities) {
						String stateName = "S"+(1+stateIndex);
						if(stateIndex>0) {
							sqlInsertItems.append(",");
						}
						sqlInsertItems.append("(").append(predictionId).append(",").append(stateIndex).append(",").append(addSingleQuotes(stateName))
							.append(",").append(nextValue).append(")");
						stateIndex++;
					}
					dbConnection.execUpdate(sqlInsertItems.toString());
				}
			}
		}
		StringBuffer querySetTargetBeginSlot = new StringBuffer();
		querySetTargetBeginSlot.append("UPDATE prediction SET ut_target_begin_slot = ")
				.append(unixTimeOp).append("(target_date) - ").append(unixTimeOp).append("(target_date) % 3600 ")
				.append(CR).append(" WHERE ut_target_begin_slot IS NULL");
		dbConnection.execUpdate(querySetTargetBeginSlot.toString());
	}

	public static Map<String, Double> evaluateAllStatesEntropie()  throws HandlingException {
		List<String> fields = initTmpStateProd();
		Map<String, Double> result = new TreeMap<String, Double>();
		List<List<String>> fieldsCombinations = SapereUtil.generateAllFieldsCombinations(fields);
		for(List<String> nextFieldsCombination : fieldsCombinations) {
			double nextEntropie = evaluateStateEntropie(nextFieldsCombination);
			result.put(String.join(",", nextFieldsCombination), nextEntropie);
		}
		for(String fieldsKey : result.keySet()) {
			logger.info("generalEntropie for field " + fieldsKey + " : "  + result.get(fieldsKey));
		}
		return result;
	}

	public static List<String> initTmpStateProd() throws HandlingException {
		String reqSeparator2 = dbConnection.getReqSeparator2();
		StringBuffer sqlQuery = new StringBuffer();
		sqlQuery.append("DROP TABLE IF EXISTS tmp_prod_states")
				.append(reqSeparator2)
				.append(CR).append( "CREATE TEMPORARY TABLE tmp_prod_states (")
				.append(CR).append("	 start_hour 	tinyint NULL")
				.append(CR).append("	,gh 			DECIMAL(10,5) NULL")
				.append(CR).append("	,ta 			DECIMAL(10,5) NULL")
				.append(CR).append("	,gh_class 		tinyint NULL")
				.append(CR).append("	,ta_class 		tinyint NULL")
				.append(CR).append(") AS ")
				.append(CR).append( "	SELECT 'FOO'")
				.append(CR).append( "			,mr.`timestamp`")
				.append(CR).append( "			,MONTH(mr.timestamp) 		AS month")
				.append(CR).append( "			,DATE(mr.timestamp) 		AS date")
				.append(CR).append( "			,HOUR(mr.timestamp) 		AS hour1")
				.append(CR).append( "			,0					 		AS start_hour")
				.append(CR).append( "			,dayofweek(mr.timestamp) 	AS day_of_week")
				.append(CR).append( "			,(SELECT id from time_window tw where tw.start_hour <= HOUR(mr.timestamp) and tw.end_hour > HOUR(mr.timestamp)) AS id_time_window")
				.append(CR).append( "			,(SELECT id from clemap_data_light.meteo_data ")
				.append(CR).append( "					WHERE meteo_data.ut_timestamp = ").append(unixTimeOp).append("(mr.timestamp) - ").append("(mr.timestamp) % 3600) AS id_meteo_data")
				.append(CR).append( "			,pmr.power_p as pv_production")
				.append(CR).append( "			,10*78*12 + pmr.power_p AS total_production")
				.append(CR).append( "			,((10*78*12 + pmr.power_p) / 60000) AS test1")
				.append(CR).append( "			,((10*78*12 + pmr.power_p) / 12000) AS test2")
				.append(CR).append( "			,1 + FLOOR((10*78*12 + pmr.power_p) / 12000) AS state_id")
				.append(CR).append( "			,NULL AS gh, NULL AS ta, NULL AS gh_class, NULL AS ta_class")
				.append(CR).append( "			FROM ").append(CLEMAPDATA_DBNAME).append(".measure_record mr")
				.append(CR).append( "			JOIN ").append(CLEMAPDATA_DBNAME).append(".phase_measure_record pmr on pmr.id_measure_record =mr.id")
				.append(CR).append( "			WHERE feature_type = '15_MN' AND sensor_number = 'CH1022501234500000000000000326365'")
				.append(CR).append( "				AND mr.timestamp >= '2022-06-01' -- AND timestamp < '2022-06-23'")
				;
		sqlQuery.append(reqSeparator2)
				.append(CR).append("UPDATE tmp_prod_states st")
				.append(CR).append( "	JOIN time_window tw on tw.id = st.id_time_window")
				.append(CR).append( "	SET st.start_hour = tw.start_hour")
				.append(reqSeparator2)
				.append(CR).append( "UPDATE tmp_prod_states st")
				.append(CR).append( "	JOIN  clemap_data_light.meteo_data on meteo_data.id = st.id_meteo_data")
				.append(CR).append( "	SET ")
				.append(CR).append( "		 st.gh 			= meteo_data.gh")
				.append(CR).append( "		,st.ta 			= meteo_data.ta")
				.append(reqSeparator2)
				.append(CR).append( "UPDATE tmp_prod_states SET")
				.append(CR).append( "		gh_class 	= FLOOR(gh/300)")
				.append(CR).append( "		,ta_class	= if(ta>-99,FLOOR(ta/10),null)")
				//.append(reqSeparator2)
		;
		dbConnection.execUpdate(sqlQuery.toString());
		return Arrays.asList("start_hour", "gh_class","day_of_week");
	}

	public static double evaluateStateEntropie(List<String> fields)  throws HandlingException {
		String reqSeparator2 = dbConnection.getReqSeparator2();
		StringBuffer sqlQuery = new StringBuffer();
		String sKey = String.join(",", fields);
		StringBuffer joinCriteria = new StringBuffer();
		String sep="";
		for(String field : fields) {
			joinCriteria.append(sep).append("total.").append(field).append(" = ").append("sub_total.").append(field);
			sep = " AND ";
		}
		sqlQuery.append(dbConnection.generateQueryDropTmpTable("Tmp_entropie"))
				.append(reqSeparator2)
				.append(CR).append("SET @total_nb=(select Count(*) FROM tmp_prod_states)")
				.append(reqSeparator2)
				.append(CR).append( "CREATE TEMPORARY TABLE Tmp_entropie AS ")
				.append(CR).append( "		SELECT ").append(sKey).append(", total_nb, SUM(ratio) AS ratio")
				.append(CR).append( "			, ROUND(SUM(-1*ratio*log2(ratio)),3) AS entropie")
				.append(CR).append( "			, total_nb/@total_nb AS weight")
				.append(CR).append( "			, total_nb/@total_nb * ROUND(SUM(-1*ratio*log2(ratio)),3) AS weighted_entropie")
				.append(CR).append( "			, GROUP_CONCAT(label order by ratio DESC) as Label")
				.append(CR).append( "			FROM")
				.append(CR).append( "			(")
				.append(CR).append( "				SELECT sub_total.*, total.total_nb ")
				.append(CR).append( "					, nb/total_nb as ratio")
				.append(CR).append( "					,CONCAT('S', state_id, ' : ', ROUND(nb/total_nb,2)) AS label")
				.append(CR).append( "				FROM")
				.append(CR).append( "					(")
				.append(CR).append( "						SELECT ").append(sKey).append(", state_id, count(*) as nb")
				.append(CR).append( "						FROM tmp_prod_states")
				.append(CR).append( "						WHERE not ta_class is NULL")
				.append(CR).append( "						group by ").append(sKey).append(", state_id")
				.append(CR).append( "					) AS sub_total")
				.append(CR).append( "					JOIN (")
				.append(CR).append( "						SELECT ").append(sKey).append(", count(*) as total_nb, GROUP_CONCAT(distinct(state_id))")
				.append(CR).append( "						FROM tmp_prod_states")
				.append(CR).append( "						GROUP BY ").append(sKey)
				.append(CR).append( "					) AS total")
				.append(CR).append( "					ON ").append(joinCriteria.toString())
				.append(CR).append( "			) AS sub_total2")
				.append(CR).append( "			GROUP BY ").append(sKey)
				.append(CR).append( "		")
				.append(reqSeparator2)
				.append(CR).append("SELECT SUM(weighted_entropie)  AS entropie FROM Tmp_entropie")
			;
			/*
			List<Map<String, Object>> test1 = dbConnection.executeSelect("SELECT * FROM Tmp_entropie");
			for(Map<String, Object> nextRow : test1) {
				logger.info("" + nextRow);
			}*/
			if(fields.size()==3) {
				logger.info("evaluateStateEntropie : for debug : ");
			}
			Map<String, Object> row = dbConnection.executeSingleRowSelect(sqlQuery.toString());
			double entropie = SapereUtil.getDoubleValue(row, "entropie", logger);
			return entropie;
	}

	public static synchronized Map<String, StatesStatistic> retrieveStatesStatistics(
			PredictionContext predictionContext
			//,Date minCreationDate
			//,Date maxCreationDate
			,Date minTargetDate
			,Date maxTargetDate
			,Integer minHour
			,Integer maxHour
			,String[] variablesFilter
			)  throws HandlingException {
		Long predictionCtxId = predictionContext.getId();
		PredictionScope scope = predictionContext.getScope();
		StringBuffer query3 = new StringBuffer();
		query3.append("SELECT * FROM v_state_distribution WHERE id_model_context  = ").append(predictionCtxId);
		if(minTargetDate != null) {
			String sqlMinTargetDate = addSingleQuotes(UtilDates.format_sql.format(minTargetDate));
			query3.append(CR).append("		AND hs_day >= DATE(").append(sqlMinTargetDate).append(")");
		}
		if(maxTargetDate != null) {
			String sqlMaxTargetDate = addSingleQuotes(UtilDates.format_sql.format(maxTargetDate));
			query3.append(CR).append("		AND hs_day <= DATE(").append(sqlMaxTargetDate).append(")");
		}
		if(variablesFilter != null && variablesFilter.length > 0) {
			String sVariablesFilter = "'" + String.join("','", variablesFilter) + "'";
			query3.append(CR).append("		AND variable_name IN (").append(sVariablesFilter).append(")");
		}
		List<Map<String, Object>> rows = dbConnection.executeSelect(query3.toString());
		Map<String, StatesStatistic> mapStateStatistics = new HashMap<>();
		for(Map<String, Object> nextRow : rows) {
			String variableName = "" + nextRow.get("variable_name");
			Date stateDate = SapereUtil.getDateValue(nextRow, "hs_day", logger);
			Date computeDay = SapereUtil.getDateValue(nextRow, "compute_day", logger);
			Integer hour = SapereUtil.getIntValue(nextRow, "hour");
			if((minHour == null || hour >= minHour) && (maxHour == null || hour <= maxHour)) {
				//stateStatistic.setGiniIndex(SapereUtil.getDoubleValue(nextRow, "gini_index"));
				//stateStatistic.setShannonEntropie(SapereUtil.getDoubleValue(nextRow, "shannon_entropie"));
				//String key = stateStatistic.getKey();// SapereUtil.format_day.format(stateDate)+ "." + hour + "." + variableName;
				String key = StatesStatistic.generateKey(scope, stateDate, hour, variableName);
				if(!mapStateStatistics.containsKey(key)) {
					StatesStatistic stateStatistic = new StatesStatistic();
					stateStatistic.setLocation(predictionContext.getMainServiceAddress());
					stateStatistic.setScenario(predictionContext.getScenario());
					stateStatistic.setVariable(variableName);
					stateStatistic.setDate(stateDate);
					stateStatistic.setComputeDay(computeDay);
					stateStatistic.setHour(hour);
					mapStateStatistics.put(key, stateStatistic);
				}
				boolean toLog = false;
				if(toLog) {
					logger.info("retrieveStatesStatistics : key = " + key);
				}
				if(mapStateStatistics.containsKey(key)) {
					StatesStatistic statesStatistic = mapStateStatistics.get(key);
					String stateName = "" + nextRow.get("state_name");
					Integer nb = SapereUtil.getIntValue(nextRow, "nb");
					statesStatistic.addStateNb(stateName, nb);
				}
			}
		}
		for(StatesStatistic nextStatistic : mapStateStatistics.values()) {
			nextStatistic.computeAll();
			logger.info("nextStatistic " + nextStatistic);
		}
		return mapStateStatistics;
	}


	private static String auxConstructFilter(StatisticsRequest statisticsRequest) {
		Date minComputeDate = statisticsRequest.getMinComputeDay();
		Date maxComputeDate = statisticsRequest.getMaxComputeDay();
		//Date current = getCurrentDate();
		Date minTargetDate = statisticsRequest.getMinTargetDay();
		Date maxTargetDate = statisticsRequest.getMaxTargetDay();
		String[] variablesFilter = statisticsRequest.getListVariableNames();
		Boolean useCorrectionFilter = statisticsRequest.getUseCorrectionFilter();
		Integer minHour = statisticsRequest.getMinTargetHour();
		Integer maxHour = statisticsRequest.getMaxTargetHour();
		Long predictionContextId = statisticsRequest.getPredictionContextId();
		//String[] fieldsToMerge = statisticsRequest.getFieldsToMerge();
		//Integer minPredictionsCount = statisticsRequest.getMinPredictionsCount();
		StringBuffer filter = new StringBuffer("1");
		String sqlMinComputeDate = (minComputeDate == null)? "NULL" : addSingleQuotes(UtilDates.format_sql_day.format(minComputeDate));
		String sqlMaxComputeDate = (maxComputeDate == null)? "NULL" : addSingleQuotes(UtilDates.format_sql_day.format(maxComputeDate));
		if(predictionContextId != null) {
			filter.append(CR).append(" 						AND p.id_model_context = ").append(predictionContextId);
		}
		if(minComputeDate != null) {
			filter.append(CR).append("			    		AND p.creation_day >=DATE(").append(sqlMinComputeDate).append(")");
		}
		if(maxComputeDate != null) {
			filter.append(CR).append("			    		AND p.creation_day <= DATE(").append(sqlMaxComputeDate).append(")");
		}
		String sqlMinTargetDate = (minTargetDate == null) ? "NULL" : addSingleQuotes(UtilDates.format_sql_day.format(minTargetDate));
		if(minTargetDate != null) {
			filter.append(CR).append("			    		AND p.target_day >= DATE(").append(sqlMinTargetDate).append(")");
		}
		String sqlMaxTargetDate = (maxTargetDate == null) ? "NULL" : addSingleQuotes(UtilDates.format_sql_day.format(maxTargetDate));
		if(sqlMaxTargetDate != null) {
			filter.append(CR).append("			    		AND p.target_day <= DATE(").append(sqlMaxTargetDate).append(")");
		}
		if(minHour != null) {
			filter.append(CR).append("			    		AND p.target_hour >= ").append(minHour);
		}
		if(maxHour != null) {
			filter.append(CR).append("			    		AND p.target_hour <= ").append(maxHour);
		}
		if(useCorrectionFilter != null) {
			int bUseCorrections = useCorrectionFilter.booleanValue() ? 1 :0;
			filter.append(CR).append("			    		AND p.use_corrections = ").append(bUseCorrections);
		}
		if(variablesFilter != null && variablesFilter.length > 0) {
			String sVariablesFilter = "'" + String.join("','", variablesFilter) + "'";
			filter.append(CR).append("			    		AND p.variable_name IN (").append(sVariablesFilter).append(")");
		}
		return filter.toString();
	}

	public static Map<String, PredictionStatistic> computePredictionStatistics(
			PredictionContext predictionContext, StatisticsRequest statisticsRequest
			) throws HandlingException {
		//Date minComputeDate = statisticsRequest.getMinComputeDay();
		//Date maxComputeDate = statisticsRequest.getMaxComputeDay();
		//Date current = getCurrentDate();
		PredictionScope scope = predictionContext.getScope();
		Date minTargetDate = statisticsRequest.getMinTargetDay();
		Date maxTargetDate = statisticsRequest.getMaxTargetDay();
		String[] variablesFilter = statisticsRequest.getListVariableNames();
		//Boolean useCorrectionFilter = statisticsRequest.getUseCorrectionFilter();
		Integer minHour = statisticsRequest.getMinTargetHour();
		Integer maxHour = statisticsRequest.getMaxTargetHour();
		String[] fieldsToMerge = statisticsRequest.getFieldsToMerge();
		Integer minPredictionsCount = statisticsRequest.getMinPredictionsCount();
		statisticsRequest.setPredictionContextId(predictionContext.getId());
		// retrieve state statistics
		Map<String, StatesStatistic> mapStatesStatistics = retrieveStatesStatistics(predictionContext, minTargetDate, maxTargetDate, minHour, maxHour, variablesFilter);
		isModeDebug = false;
		//String opTemprary2 = isModeDebug? "" : "TEMPORARY";
		consolidatePredictions(predictionContext);
		StringBuffer query = new StringBuffer();
		//String sep2 =  dbConnection.getReqSeparator2()+ CR;
		// Construct filter clause

		// Construct GROUP BY clause
		String groupByClause = "compute_day, target_day, variable_name";
		List<String> listFieldsToMerge = Arrays.asList(fieldsToMerge);
		boolean mergeHorizons = listFieldsToMerge.contains("horizon");
		boolean mergeUseCorrections =listFieldsToMerge.contains("useCorrection");
		boolean mergeHour  = listFieldsToMerge.contains("hour");
		if(!mergeHorizons) {
			groupByClause = groupByClause+",horizon";
		}
		if(!mergeUseCorrections) {
			groupByClause = groupByClause+",use_corrections";
		}
		if(!mergeHour) {
			groupByClause = groupByClause+",time_slot";
		}
		// Construct main query
		query.append("SELECT variable_name, compute_day, target_day")
			.append(CR).append( "	,GROUP_CONCAT(DISTINCT time_slot ) AS time_slot")
			.append(CR).append( "	,GROUP_CONCAT(DISTINCT horizon ) AS horizon")
			.append(CR).append( "	,GROUP_CONCAT(DISTINCT ").append(OP_IF).append("(use_corrections, 'True', 'False') ) AS use_corrections")
			.append(CR).append("	,DATETIME(target_day, '+'|| (MIN(time_slot)+0) ||' hours') AS date_begin")
			.append(CR).append("	,DATETIME(target_day, '+'|| (MAX(time_slot)+1) ||' hours') AS date_end")
			.append(CR).append( "	,AVG(rate_ok1)  AS rate_ok1")
			.append(CR).append( "	,AVG(rate_ok2)  AS rate_ok2")
			.append(CR).append( "	,AVG(vector_differential) AS vector_differential")
			.append(CR).append( "	,AVG(cross_entropy_loss) AS cross_entropy_loss")
			.append(CR).append( "	,SUM(nb_ok2)  AS nb_ok2")
			.append(CR).append( "   ,SUM(nb_total) AS nb_total")
			.append(CR).append( "   ,SUM(corrections_number) AS corrections_number")
			.append(CR).append( "	,AVG(proba1)  AS proba_avg1")
			.append(CR).append( "	,AVG(proba2)  AS proba_avg2")
//			.append(CR).append( "	,AVG(gini_index) AS gini_index")
//			.append(CR).append( "	,AVG(shannon_entropie) AS shannon_entropie")
			.append(CR).append( "	,Count(*) as nb_results")
			.append(CR).append( "	FROM (")
			.append(CR).append("			    SELECT id_model_context")
			.append(CR).append("			    	,id_node_context")
			.append(CR).append("			    	,variable_name")
			.append(CR).append("			    	,creation_day 							AS compute_day")
			.append(CR).append("			    	,target_day 							AS target_day")
			.append(CR).append("			    	,target_hour 							AS time_slot")
			.append(CR).append("			    	,horizon_minutes 						AS horizon")
			.append(CR).append("			    	,use_corrections")
			.append(CR).append("			    	,id_initial_time_window")
			.append(CR).append("			    	,COUNT(*) 								AS nb_total")
			.append(CR).append("			    	,SUM(is_ok1) 							AS nb_ok1")
			.append(CR).append("			    	,SUM(is_ok2) 							AS nb_ok2")
			.append(CR).append("			    	,SUM(is_ok1) / SUM(1) 					AS rate_ok1")
			.append(CR).append("			    	,SUM(is_ok2) / SUM(1) 					AS rate_ok2")
			.append(CR).append("			    	,SUM(has_correction) 					AS corrections_number")
			.append(CR).append("			    	,MIN(creation_date) 					AS creation_datetime")
			.append(CR).append("			    	,AVG(proba_random) 						AS proba1")
			.append(CR).append("			    	,AVG(proba_likely) 						AS proba2")
			.append(CR).append("			    	,AVG(vector_differential) 				AS vector_differential")
			.append(CR).append("			    	,AVG(cross_entropy_loss) 				AS cross_entropy_loss")
			//.append(CR).append("			    	,MAX(has_states_distrib) 				AS has_states_distrib")
			.append(CR).append("			    	,GROUP_CONCAT(distinct id_correction) 	AS list_id_correction")
			.append(CR).append("			    	FROM (")
			.append(CR).append("			    			 SELECT p.*")
			.append(CR).append("			    			    ,sh.`date`")
			.append(CR).append("			    			    ,sh.state_idx")
			.append(CR).append("			    			    ,sh.state_name")
			.append(CR).append("			    			    ,(p.random_state_idx = sh.state_idx) AS is_ok1")
			.append(CR).append("			    			    ,(p.likely_state_idx = sh.state_idx) AS is_ok2")
			.append(CR).append("			    			    ,p.random_state_proba AS proba_random")
			.append(CR).append("			    			    ,p.likely_state_proba AS proba_likely")
			.append(CR).append("			    			    ,").append(OP_IF).append("(p.id_correction IS NULL, 0, 1) AS has_correction")
			.append(CR).append("			    			 FROM prediction p")
			.append(CR).append("			    			 JOIN state_history AS sh ON sh.id = p.id_target_state_histo")
			.append(CR).append("			    			 WHERE ").append(auxConstructFilter(statisticsRequest));
		query.append(CR).append("			    ) AS Result")
			.append(CR).append("			    GROUP BY creation_day, target_day, target_hour, variable_name , horizon, use_corrections")
			.append(CR).append("			    HAVING nb_total >= ").append(minPredictionsCount)
			.append(CR).append("		) AS TmpPredictionStatistic")
			.append(CR).append( "	GROUP BY ").append(groupByClause)
			//.append(CR).append( "	ORDER BY rate_ok2, shannon_entropie DESC")
			.append(CR).append( "	ORDER BY date_begin, variable_name, horizon")
		;
		//logger.info("query3 = " + query3);
		Map<String,PredictionStatistic> result = new HashMap<String, PredictionStatistic>();
		List<Map<String, Object>> rows = dbConnection.executeSelect(query.toString());
		for(Map<String, Object> nextRow : rows) {
			PredictionStatistic nextStatistic = new PredictionStatistic();
			nextStatistic.setComputeDay(SapereUtil.getDateValue(nextRow, "compute_day", logger));
			nextStatistic.setLocation(predictionContext.getMainServiceAddress());
			nextStatistic.setScenario(predictionContext.getScenario());
			nextStatistic.setScope(predictionContext.getScope());
			if(predictionContext.isAggregationsActivated() ) {
				nextStatistic.setAggregationOperator(predictionContext.getAggregationOperator());
			}
			nextStatistic.setVariable("" + nextRow.get("variable_name"));
			nextStatistic.setNbOfPredictions(SapereUtil.getIntValue(nextRow, "nb_total"));
			nextStatistic.setNbOfSuccesses(SapereUtil.getIntValue(nextRow, "nb_ok2"));
			nextStatistic.setNbOfCorrections(SapereUtil.getIntValue(nextRow, "corrections_number"));
			if(nextRow.get("vector_differential") == null) {
				logger.info("vector_differential is null");
			} else {
				nextStatistic.setDifferential(SapereUtil.getDoubleValue(nextRow, "vector_differential", logger));
			}
			if(nextRow.get("cross_entropy_loss") == null) {
				logger.info("cross_entropy_loss is null");
			} else {
				nextStatistic.setCrossEntropyLoss(SapereUtil.getDoubleValue(nextRow, "cross_entropy_loss", logger));
			}
			Date dateBegin = SapereUtil.getDateValue(nextRow, "date_begin", logger);
			Date dateEnd = SapereUtil.getDateValue(nextRow, "date_end", logger);
			nextStatistic.setTimeSlot(new TimeSlot(dateBegin, dateEnd));
			nextStatistic.resetHorizons();
			String sHorizons = "" + nextRow.get("horizon");
			for(String sHorizon : sHorizons.split(",")) {
				nextStatistic.addHorizon(Integer.valueOf(sHorizon));
			}
			nextStatistic.resetUseOfCorrections();
			String sListUseOfCorrections = "" + nextRow.get("use_corrections");
			for(String sUseOfCorrections : sListUseOfCorrections.split(",")) {
				nextStatistic.addUseOfCorrections(Boolean.valueOf(sUseOfCorrections));
			}
			//logger.info("next vector_differential " + sHorizons + " : " + nextStatistic.getVectorDifferential());

			// TODO : row gini_index is not used
			//nextStatistic.setGiniIndex(SapereUtil.getDoubleValue(nextRow, "gini_index"));
			//nextStatistic.setShannonEntropie(SapereUtil.getDoubleValue(nextRow, "shannon_entropie"));
			String stateKey = nextStatistic.getKeyOfStateDistribution();
			if(mapStatesStatistics.containsKey(stateKey)) {
				StatesStatistic stateStatistic = mapStatesStatistics.get(stateKey);
				nextStatistic.setStatesStatistic(stateStatistic);
			} else {
				logger.warning("computePredictionStatistics : not states statistic for key " + stateKey
						+ " mapStatesStatistics size = " + mapStatesStatistics.size());
			}
			boolean trivialFilterOk =  !nextStatistic.isTrivial() || statisticsRequest.getIncludeTrivials();
			if(trivialFilterOk) {
				String statisticKey = stateKey + "."+(mergeHorizons? "*" : sHorizons) + "." + (mergeUseCorrections? "*": sListUseOfCorrections);
				result.put(statisticKey, nextStatistic);
			}
		}
		// add statistic detail lines
		// -- Retrieve average probability of each state
		StringBuffer query4 = new StringBuffer();
		query4.append("SELECT compute_day, target_day, hour, variable_name, state_name")
			.append(CR).append( "	,GROUP_CONCAT(DISTINCT horizon ) AS horizon")
			.append(CR).append( "	,GROUP_CONCAT(DISTINCT ").append(OP_IF).append("(use_corrections, 'True', 'False') ) AS use_corrections")
			.append(CR).append( "	,ROUND(AVG(proba),5) AS proba")
			.append(CR).append("  FROM (")
			.append(CR).append("			SELECT DATE(p.creation_date) AS compute_day")
			.append(CR).append("			, target_day  AS target_day")
			.append(CR).append("			, target_hour AS hour, p.variable_name")
			.append(CR).append("			, p.horizon_minutes AS horizon , p.use_corrections")
			.append(CR).append("			, pi2.state_idx, pi2.state_name")
			.append(CR).append("			, avg(pi2.proba) AS proba")
			.append(CR).append("			FROM prediction p")
			.append(CR).append("			JOIN prediction_item pi2 on pi2.id_prediction =p.id")
			.append(CR).append("			WHERE ").append(auxConstructFilter(statisticsRequest))
			.append(CR).append("			GROUP BY DATE(p.creation_date), p.target_day, p.target_hour, p.variable_name , p.horizon_minutes , p.use_corrections, pi2.state_idx")
			.append(CR).append("  		) AS TmpPredictionStatisticLine")
			.append(CR).append("  GROUP BY compute_day, target_day, hour, variable_name, state_name");
		if(!mergeHorizons) {
			query4.append(",horizon");
		}
		if(!mergeUseCorrections) {
			query4.append(",use_corrections");
		}
		List<Map<String, Object>> rows4 = dbConnection.executeSelect(query4.toString());
		for(Map<String, Object> nextRow : rows4) {
			String variableName = "" + nextRow.get("variable_name");
			Date date = SapereUtil.getDateValue(nextRow, "target_day", logger);
			int hour = SapereUtil.getIntValue(nextRow, "hour");
			String stateKey = StatesStatistic.generateKey(scope, date, hour, variableName);
			String sHorizons = "" + nextRow.get("horizon");
			String sListUseOfCorrections = "" + nextRow.get("use_corrections");
			String statisticKey = stateKey
					+ "." + (mergeHorizons? "*" : sHorizons)
					+ "." + (mergeUseCorrections? "*": sListUseOfCorrections);
			if(result.containsKey(statisticKey)) {
				PredictionStatistic predictionStatistic = result.get(statisticKey);
				String stateName = "" + nextRow.get("state_name");
				Double proba = SapereUtil.getDoubleValue(nextRow, "proba", logger);
				predictionStatistic.addMeanOfProba(stateName, proba);
				//logger.info("add state proba " + proba + " for state " + stateName + " in " + statisticKey);
			} else {
				logger.warning(statisticKey + " not contained in result map");
			}
		}
		logger.info("computePredictionStatistics : end");
		return result;
	}
}
