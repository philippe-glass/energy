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

import org.springframework.core.env.Environment;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.energy.NodeTotal;
import com.sapereapi.model.energy.OptionItem;
import com.sapereapi.model.markov.MarkovState;
import com.sapereapi.model.markov.MarkovStateHistory;
import com.sapereapi.model.markov.MarkovTimeWindow;
import com.sapereapi.model.markov.NodeMarkovStates;
import com.sapereapi.model.markov.NodeMarkovTransitions;
import com.sapereapi.model.markov.NodeTransitionMatrices;
import com.sapereapi.model.markov.TransitionMatrixKey;
import com.sapereapi.model.prediction.PredictionContext;
import com.sapereapi.model.prediction.PredictionData;
import com.sapereapi.model.prediction.PredictionDeviation;
import com.sapereapi.model.prediction.PredictionResult;
import com.sapereapi.model.prediction.PredictionStatistic;
import com.sapereapi.model.prediction.PredictionStep;
import com.sapereapi.model.prediction.StatesStatistic;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import Jama.Matrix;

public class PredictionDbHelper {
	private static Environment environment = null;
	private static DBConnection dbConnection = null;
	//private static DBConnection dbConnectionClemapData = null;
	private static PredictionDbHelper instance = null;
	public final static String CR = System.getProperty("line.separator");	// Cariage return
	private static int debugLevel = 0;
	private static SapereLogger logger = SapereLogger.getInstance();
	public final static String CLEMAPDATA_DBNAME = "clemap_data_light";


	public static void init(Environment _environment) {
		environment = _environment;
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
		String url = environment.getProperty("spring.datasource.url");
		String user = environment.getProperty("spring.datasource.username");
		String password = environment.getProperty("spring.datasource.password");
		dbConnection = new DBConnection(url, user, password);
		//dbConnectionClemapData = new DBConnection("jdbc:mariadb://129.194.10.168/clemap_data", "import_clemap", "sql2537");
	}

	public static String getSessionId() {
		return DBConnection.getSessionId();
	}

	public static DBConnection getDbConnection() {
		return dbConnection;
	}

	public static List<OptionItem> getStateDates() {
		List<OptionItem> result = new ArrayList<>();
		List<Map<String, Object>> rows = dbConnection.executeSelect("SELECT DATE(date) AS day"
				+ CR +  " FROM state_history sh GROUP BY day"
				+ CR + " ORDER BY day DESC");
		for(Map<String, Object> nextRow : rows) {
			Date day = (Date) nextRow.get("day");
			String sday = UtilDates.format_sql_day.format(day);
			result.add(new OptionItem(sday, sday));
		}
		return result;
	}

	public static Map<String, Long> saveHistoryStates(NodeTotal nodeTotal, NodeMarkovStates nodeMarkovStates, NodeContext nodeContext) {
		Map<String, Long> result = new HashMap<String, Long>();
		if(!nodeTotal.hasActivity()) {
			//return result;
		}
		String sessionId1 = getSessionId();
		String sessionId2 = addSingleQuotes(sessionId1);
		Long ctxId = nodeContext.getId();
		String scenario = nodeContext.getScenario();
		String location = nodeTotal.getLocation();
		Map<String, MarkovState> mapStates = nodeMarkovStates.getMapStates();
		Map<String, Double> mapValues = nodeMarkovStates.getMapValues();
		StringBuffer query1 = new StringBuffer();
		String sqlDate1 = UtilDates.format_sql.format(nodeTotal.getDate());
		String sqlDate2 = addSingleQuotes(sqlDate1);
		// Clean existing data in state_history for the same dates
		query1.append("DROP TEMPORARY TABLE IF EXISTS TmpCleanSH");
		query1.append(CR).append("§");
		query1.append("CREATE TEMPORARY TABLE TmpCleanSH AS")
			.append(CR).append("SELECT id FROM state_history WHERE date BETWEEN ").append(sqlDate2).append(" AND DATE_ADD(").append(sqlDate2).append(", INTERVAL 1 HOUR)")
			.append(CR).append(" AND id_context = ").append(ctxId)
			.append(CR).append(" AND NOT id_session = ").append(sessionId2);
		query1.append(CR).append("§");
		query1.append("DROP TEMPORARY TABLE IF EXISTS TmpSH2");
		query1.append(CR).append("§");
		query1.append("CREATE TEMPORARY TABLE TmpSH2 AS "
				+ " SELECT sh.id FROM TmpCleanSH"
				+ " JOIN state_history sh  on sh.id_last  = TmpCleanSH.id");
		query1.append(CR).append("§");
		query1.append("UPDATE TmpSH2"
				+ CR + " JOIN state_history sh ON sh.id = TmpSH2.id"
				+ CR + " SET sh.id_last = NULL");
		query1.append(CR).append("§");
		query1.append("DROP TEMPORARY TABLE IF EXISTS TmpPrediction2");
		query1.append(CR).append("§");
		query1.append("CREATE TEMPORARY TABLE TmpPrediction2 AS "
				+ CR + " SELECT p.id FROM TmpCleanSH"
				+ CR + " JOIN prediction p ON p.id_target_state_histo  = TmpCleanSH.id");
		query1.append(CR).append("§");
		query1.append("UPDATE TmpPrediction2"
				+ CR + " JOIN prediction p ON p.id = TmpPrediction2.id"
				+ CR + " SET p.id_target_state_histo = NULL");
		query1.append(CR).append("§");
		query1.append("DELETE state_history FROM state_history WHERE id IN (SELECT id FROM TmpCleanSH)");
		dbConnection.execUpdate(query1.toString());

		// Insert data into state_history table
		StringBuffer query2 = new StringBuffer();
		query2.append("SET @date_last = (SELECT MAX(date) FROM state_history WHERE id_context = " + ctxId
			+ " AND date < " + sqlDate2
			+ " AND id_session = " + sessionId2
			+ ")");
		query2.append(CR).append("§");
		query2.append(CR).append("INSERT INTO state_history(id_context,date,date_last,id_session,variable_name,location,scenario,state_idx,state_name,value,id_last) VALUES ");
		boolean stateAdded = false;
		for(String variable : nodeMarkovStates.getVariables()) {
			if(mapStates.containsKey(variable)) {
				MarkovState state = mapStates.get(variable);
				double value = -1;
				if(mapValues.containsKey(variable)) {
					value = mapValues.get(variable);
				}
				int stateIdx = state.getId() -1;
				String stateName = "S"+ state.getId();
				if(stateAdded) {
					query2.append(", ");
				}
				query2.append("(")
					.append(ctxId)
					.append(",").append(sqlDate2)
					.append(",@date_last")
					.append(",").append(sessionId2)
					.append(",").append(addSingleQuotes(variable))
					.append(",").append(addSingleQuotes(location)).append(",").append(addSingleQuotes(scenario))
					.append(",").append(stateIdx).append(",").append(addSingleQuotes(stateName))
					.append(",").append(addQuotes(value))
					.append(",").append("(SELECT lastSH.id FROM state_history AS lastSH WHERE lastSH.id_session = " + sessionId2
											+ " AND lastSH.date = @date_last AND lastSH.variable_name = " + addSingleQuotes(variable) + " LIMIT 0,1)")
					.append(")");
				stateAdded = true;
			}
		}
		query2.append(CR).append(" ON DUPLICATE KEY UPDATE value = value");
		query2.append(CR).append("§");
		query2.append(CR).append("UPDATE state_history SET ")
			.append(CR).append("    state_idx_last =  (SELECT MAX(last.state_idx) FROM state_history AS last WHERE last.id = state_history.id_last)" )
			.append(CR).append("   ,state_name_last =  (SELECT MAX(last.state_name) FROM state_history AS last WHERE last.id = state_history.id_last)" )
			.append(CR).append(" WHERE id_session = " + sessionId2 + " AND  date = " + sqlDate2 + " AND NOT id_last IS NULL");
		query2.append(CR).append("§");
		query2.append(CR).append("UPDATE state_history SET ")
		.append(CR).append("date_next =  " + sqlDate2)
		.append(CR).append(" WHERE id_session = " + sessionId2 + " AND  date = @date_last");
/*
		query2.append(CR).append("UPDATE state_history SET ")
				.append(CR).append("date_next = (SELECT next_sh.date FROM state_history as next_sh")
				.append(CR).append("     WHERE next_sh.date > state_history.date AND next_sh.variable_name  = state_history.variable_name")
				.append(CR).append("    	AND next_sh.id_context  = state_history.id_context")
				.append(CR).append(" 	ORDER BY next_sh.date LIMIT 0,1)")
				.append(CR).append(" WHERE id_context = " + ctxId + " AND  state_history.date >= DATE_ADD(@date_last, INTERVAL -10 MINUTE) AND date < " + addQuotes(sqlDate));
*/
		if(stateAdded) {
			//dbConnection.execUpdate(query1.toString());
			dbConnection.execUpdate(query2.toString());
			List<Map<String, Object>> rows = dbConnection.executeSelect("SELECT * FROM state_history WHERE id_context = " + ctxId + " AND  date = " + sqlDate2);
			for(Map<String, Object> row: rows) {
				Long id = SapereUtil.getLongValue(row, "id");
				String variableName = "" + row.get("variable_name");
				result.put(variableName, id);
			}
		}
		return result;
	}


	public static List<MarkovTimeWindow> retrieveTimeWindows() {
		List<MarkovTimeWindow> result = new ArrayList<MarkovTimeWindow>();
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
			MarkovTimeWindow timeWindow = new MarkovTimeWindow(id, daysOfWeek, startHour, startMinute, endHour, endMinute);
			result.add(timeWindow);
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

	public static Map<Integer, Map<String, TransitionMatrixKey>> loadMapNodeTransitionMatrixKeys(
			PredictionContext context			) {
		Map<Integer, Map<String, TransitionMatrixKey>> result = new HashMap<Integer, Map<String, TransitionMatrixKey>>();
		List<TransitionMatrixKey> listTrKeys = loadListNodeTransitionMatrixKeys(context);
		for(TransitionMatrixKey nextTrKey : listTrKeys) {
			Integer timeWindowId = nextTrKey.getTimeWindowId();
			if(!result.containsKey(timeWindowId)) {
				result.put(timeWindowId, new HashMap<String, TransitionMatrixKey>());
			}
			Map<String, TransitionMatrixKey> map1 = result.get(timeWindowId);
			String variable = nextTrKey.getVariable();
			map1.put(variable, nextTrKey);
		}
		return result;
	}


	public static List<TransitionMatrixKey> loadListNodeTransitionMatrixKeys(
			PredictionContext context) {
		List<TransitionMatrixKey> result = new ArrayList<TransitionMatrixKey>();
		List<Map<String, Object>> rows1 = dbConnection.executeSelect(
				"SELECT * FROM transition_matrix WHERE id_context = " + context.getId());
		for(Map<String, Object> row : rows1) {
			String variable = "" + row.get("variable_name");
			Long idTM = SapereUtil.getLongValue(row, "id");
			int timeWindowId = SapereUtil.getIntValue(row, "id_time_window");
			MarkovTimeWindow timeWindow = context.getMarkovTimeWindow(timeWindowId);
			TransitionMatrixKey trKey = new TransitionMatrixKey(idTM, context.getId(), variable, timeWindow);
			result.add(trKey);
		}
		return result;
	}


	public static Map<Integer,NodeTransitionMatrices> loadListNodeTransitionMatrice(
			PredictionContext predictionContext
			, String[] _variables
			//, TransitionMatrixScope scope
			, List<MarkovTimeWindow> listTimeWindows
			, Date currentDate) {
		logger.info("loadListNodeTransitionMatrice : begin");
		Map<Integer,NodeTransitionMatrices> mapTransitionMatrices = new HashMap<Integer,NodeTransitionMatrices>();
		List<String> idsTimeWindows = new ArrayList<String>();
		for(MarkovTimeWindow timeWindow : listTimeWindows) {
			//MarkovTimeWindow timeWindow = timeSlot.getMarkovTimeWindow();
			NodeTransitionMatrices nodeTransitionMatrices = new NodeTransitionMatrices(predictionContext, _variables, timeWindow);
			mapTransitionMatrices.put(timeWindow.getId(), nodeTransitionMatrices);
			idsTimeWindows.add(""+timeWindow.getId());
		}
		String sCurrentDate = UtilDates.format_sql.format(currentDate);
		String sIdsTimeWindows = SapereUtil.implode(idsTimeWindows, ",");
		String sVariableNames = "'"+ SapereUtil.implode(Arrays.asList(_variables), "','") + "'";
		StringBuffer loadQuery1 = new StringBuffer();
		loadQuery1.append(CR).append( "DROP TEMPORARY TABLE IF EXISTS TmpTrMatrix")
			.append(CR).append("§");
		loadQuery1.append(CR).append( "CREATE TEMPORARY TABLE TmpTrMatrix AS")
			.append(CR).append( "SELECT transition_matrix.id")
			.append(CR).append(", transition_matrix.variable_name")
			.append(CR).append(", transition_matrix.location")
			.append(CR).append(", transition_matrix.scenario")
			.append(CR).append(", transition_matrix.last_update")
			.append(CR).append(", transition_matrix.id_time_window")
			.append(CR).append(", transition_matrix.iteration_number")
			.append(CR).append(", GET_ITERATION_ID(transition_matrix.id, ").append(addSingleQuotes(sCurrentDate)).append(" )  AS id_transition_matrix_iteration")
			.append(CR).append(" FROM transition_matrix ")
			.append(CR).append(" WHERE transition_matrix.id_time_window IN (").append(sIdsTimeWindows).append(")")
			.append(CR).append(" 	AND variable_name IN (").append(sVariableNames).append(")")
			.append(CR).append(" 	AND location = ").append(addSingleQuotes(predictionContext.getLocation()))
			.append(CR).append(" 	AND scenario = ").append(addSingleQuotes(predictionContext.getScenario()))
		;
		loadQuery1.append(CR).append("§");
		loadQuery1.append(CR).append("SELECT * FROM TmpTrMatrix");
		Date before = new Date();
		// At first, load the transition matrix headers
		List<Map<String, Object>> rows1 = dbConnection.executeSelect(loadQuery1.toString());
		for(Map<String, Object> row : rows1) {
			String variable = "" + row.get("variable_name");
			//Long idTM = SapereUtil.getLongValue(row, "id");
			Long timeWindowId1 = SapereUtil.getLongValue(row, "id_time_window");
			Long iterationNumber = SapereUtil.getLongValue(row, "iteration_number");
			Integer timeWindowId = Integer.valueOf(timeWindowId1.intValue());
			NodeTransitionMatrices nodeTransitionMatrices = mapTransitionMatrices.get(timeWindowId);
			if(nodeTransitionMatrices != null) {
				nodeTransitionMatrices.setNbOfIterations(variable, iterationNumber.intValue());
				TransitionMatrixKey tmKey = predictionContext.getTransitionMatrixKey(timeWindowId, variable);
				nodeTransitionMatrices.setMatrixKey(variable, tmKey);
			}
			Date updateDate = (Date) row.get("last_update");
			if(updateDate != null) {
				nodeTransitionMatrices.getMapMatrices().get(variable).setComputeDate(updateDate);
				if(nodeTransitionMatrices.getComputeDate() == null || nodeTransitionMatrices.getComputeDate().after(updateDate)) {
					nodeTransitionMatrices.setComputeDate(updateDate);
				}
			}
		}
		// Secondly, load all transition matrix cells contents
		StringBuffer loadQuery2 = new StringBuffer();
		loadQuery2.append(CR).append( "SELECT TmpTrMatrix.variable_name, TmpTrMatrix.id_time_window")
				.append(CR).append("	,cell.*")
				.append(CR).append("	,IFNULL(cellIt.obs_number,0) AS obs_number_iter")
				.append(CR).append("	,IFNULL(cellIt.corrections_number,0) AS corrections_number_iter")
				.append(CR).append(" FROM TmpTrMatrix ")
				.append(CR).append(" JOIN transition_matrix_cell AS cell ON cell.id_transition_matrix = TmpTrMatrix.id")
				.append(CR).append(" LEFT JOIN transition_matrix_cell_iteration  AS cellIt ON cellIt.id_transition_matrix_iteration = TmpTrMatrix.id_transition_matrix_iteration")
				.append(CR).append(" 			AND cellIt.row_idx = cell.row_idx AND cellIt.column_idx = cell.column_idx")
				.append(CR).append(" WHERE 1")
				.append(CR).append(" ORDER BY TmpTrMatrix.ID");
		//dbConnection.setDebugLevel(1);
		List<Map<String, Object>> rows2 = dbConnection.executeSelect(loadQuery2.toString());
		Date after = new Date();
		long loadTime = after.getTime() - before.getTime();
		logger.info("loadListNodeTransitionMatrice : request time = " + loadTime);
		for(Map<String, Object> row : rows2) {
			String variable = "" + row.get("variable_name");
			Long timeWindowId1 = SapereUtil.getLongValue(row, "id_time_window");
			Integer timeWindowId = Integer.valueOf(timeWindowId1.intValue());
			Integer rowIdx = SapereUtil.getIntValue(row,"row_idx");
			Integer columnIdx = SapereUtil.getIntValue(row,"column_idx");
			Object oNumberOfObservation = row.get("obs_number");
			if(oNumberOfObservation != null) {
				double nbOfObs = SapereUtil.getDoubleValue(row, "obs_number");
				double nbOfObsIteration = SapereUtil.getDoubleValue(row, "obs_number_iter");
				double nbOfCorrections = SapereUtil.getDoubleValue(row, "corrections_number");
				double nbOfCorrectionsIteration = SapereUtil.getDoubleValue(row, "corrections_number_iter");
				if(nbOfCorrectionsIteration>0 && debugLevel> 0) {
					logger.info("loadListNodeTransitionMatrice : " + variable + " " + timeWindowId1 + " nb of corrections : " + nbOfCorrections + " " + nbOfCorrectionsIteration);
				}
				NodeTransitionMatrices nodeTransitionMatrices = mapTransitionMatrices.get(timeWindowId);
				nodeTransitionMatrices.setValue(variable, rowIdx, columnIdx, nbOfObsIteration, nbOfCorrectionsIteration, nbOfObs, nbOfCorrections);
			}
		}
		Date after2 = new Date();
		long loadTime2 = after2.getTime() - after.getTime();
		logger.info("loadListNodeTransitionMatrice : end : loadTime2 = " + loadTime2);
		return mapTransitionMatrices;
	}

	public static NodeTransitionMatrices loadNodeTransitionMatrices(
			PredictionContext predictionContext,
			String[] _variables, MarkovTimeWindow timeWindow, Date loadDate) {
		 List<MarkovTimeWindow> listTimeWindows = new ArrayList<MarkovTimeWindow>();
		 listTimeWindows.add(timeWindow);
		 Map<Integer,NodeTransitionMatrices> mapResult = loadListNodeTransitionMatrice(predictionContext, _variables,  listTimeWindows, loadDate);
		 if(mapResult.size()>0) {
			 for(NodeTransitionMatrices transitionMatrices : mapResult.values()) {
				 // Return the first row
				 return transitionMatrices;
			 }
		 }
		 //MarkovTimeWindow timeWindow = timeSlot.getMarkovTimeWindow();
		 NodeTransitionMatrices nodeTransitionMatrices = new NodeTransitionMatrices(predictionContext, _variables, timeWindow);
		 nodeTransitionMatrices.reset(predictionContext);
		 return nodeTransitionMatrices;
	}

	public static Map<Date,NodeMarkovTransitions> loadMapMarkovTransition(
			PredictionContext predictionContext,
			TimeSlot timeSlot, String[] variableNames, double maxTotalPower) {
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
				+ CR + "WHERE sh.location = "+ addSingleQuotes(predictionContext.getLocation())
				+ CR + " AND sh.scenario = " + addSingleQuotes(predictionContext.getScenario())
				+ " AND sh.variable_name = " + addSingleQuotes(firstVariable)
				+ " AND sh.date >= " + addSingleQuotes(sqlDateMin)
				+ " AND sh.date <= " + addSingleQuotes(sqlDateMax)
		;
		List<Date> listStateDate = new ArrayList<>();
		List<Map<String, Object>> rows = dbConnection.executeSelect(query1);
		for(Map<String, Object> row : rows) {
			stateDate = (Date) row.get("date");
			listStateDate.add(stateDate);
		}
		Map<Date,NodeMarkovTransitions> mapNodeTransition = aux_loadMarkovTransition(predictionContext, listStateDate, variableNames, maxTotalPower);
		return mapNodeTransition;
	}

	public static NodeMarkovTransitions loadClosestMarkovTransition(
			PredictionContext predictionContext,
			Date aDate, String[] variableNames, double maxTotalPower) {
		if(variableNames.length == 0) {
			return null;
		}
		String firstVariable = variableNames[0];
		Date stateDate = null;
		String query1 = "SET @ut_date = UNIX_TIMESTAMP(" + addSingleQuotes(UtilDates.format_sql.format(aDate)) + ")"
				+ CR + "§"
				+ CR + "SELECT sh.date "
				+ CR + "FROM state_history sh "
				+ CR + "JOIN state_history AS last ON last.id = sh.id_last"
				+ CR + "WHERE sh.location = "+ addSingleQuotes(predictionContext.getLocation())
				+ " AND sh.scenario = " + addSingleQuotes(predictionContext.getScenario())
				+ " AND sh.variable_name = " + addSingleQuotes(firstVariable)
				+ " AND UNIX_TIMESTAMP(sh.date) >= @ut_date - 300"
				+ CR + " AND UNIX_TIMESTAMP(sh.date) <= @ut_date + 300"
				+ CR + "ORDER BY ABS(@ut_date - UNIX_TIMESTAMP(sh.date)), sh.date LIMIT 0,1";
		List<Map<String, Object>> rows = dbConnection.executeSelect(query1);
		if(rows.size()>0) {
			Map<String, Object> row = rows.get(0);
			stateDate = (Date) row.get("date");
		} else {
			return null;
		}
		List<Date> listStateDate = new ArrayList<>();
		listStateDate.add(stateDate);
		Map<Date,NodeMarkovTransitions> mapNodeTransition = aux_loadMarkovTransition(predictionContext, listStateDate, variableNames, maxTotalPower);
		if(mapNodeTransition.containsKey(stateDate)) {
			return mapNodeTransition.get(stateDate);
		}
		return null;
	}

	public static Map<Date,NodeMarkovTransitions> aux_loadMarkovTransition(
			PredictionContext predictionContext,
			List<Date> listStateDate, String[] variableNames, double maxTotalPower) {
		 Map<Date,NodeMarkovTransitions> result = new HashMap<Date,NodeMarkovTransitions>();
		String variableFilter = "'"+ String.join("','", variableNames) + "'";
		List<String> listSqlDate = new ArrayList<>();
		for(Date nextDate : listStateDate) {
			listSqlDate.add(UtilDates.format_sql.format(nextDate));
		}
		String dateFilter = "'" + String.join("','", listSqlDate) + "'";
		String query = "SELECT sh.date, sh.variable_name, sh.value, last.value AS value_last "
				+ CR + "FROM state_history sh "
				+ CR + "JOIN state_history AS last ON last.id = sh.id_last"
				+ CR + "WHERE sh.location = "+ addSingleQuotes(predictionContext.getLocation())
				+ " AND sh.scenario = " + addSingleQuotes(predictionContext.getScenario())
				+ CR + " AND sh.variable_name IN (" + variableFilter + ")"
				+ CR + " AND sh.date IN (" + dateFilter + ")"
				+ CR + " ORDER BY sh.date, sh.variable_name"
				;
		List<Map<String, Object>> rows = dbConnection.executeSelect(query);
		Date lastDate = null;
		Date currentDate = null;
		for(Map<String, Object> row : rows) {
			if(rows.size()>0) {
				currentDate = (Date) row.get("date");
				if(lastDate == null || lastDate.getTime() != currentDate.getTime()) {
					result.put(currentDate, new NodeMarkovTransitions(predictionContext, variableNames, currentDate, maxTotalPower));
				}
				NodeMarkovTransitions nodeTransition = result.get(currentDate);
				String variableName = "" + row.get("variable_name") ;
				double value = SapereUtil.getDoubleValue(row, "value");
				double valueLast = SapereUtil.getDoubleValue(row, "value_last");
				try {
					MarkovState stateCurrent = NodeMarkovStates.getMarkovState(value);
					MarkovState stateLast = NodeMarkovStates.getMarkovState(valueLast);
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

	public static List<MarkovStateHistory> retrieveLastMarkovHistoryStates(NodeMarkovStates nodeMarkoveState, Date minCreationDate, String variableName, boolean observationUpdated) {
		String sessionId = DBConnection.getSessionId();
		List<MarkovStateHistory> result = new ArrayList<MarkovStateHistory>();
		String query = "SELECT state_history.*, 1+state_idx AS state_id FROM state_history WHERE  location = " + addSingleQuotes(nodeMarkoveState.getLocation())
			+ " AND scenario = " + addSingleQuotes(nodeMarkoveState.getScenario())
			+ " AND variable_name = " + addSingleQuotes(variableName)
			+ " AND id_session = " + addSingleQuotes(sessionId)
			+ " AND creation_date >= " + addSingleQuotes(UtilDates.format_sql.format(minCreationDate));
		if(observationUpdated) {
			query = query +  " AND NOT observation_update IS NULL";
		}
		logger.info("retrieveLastMarkovHistoryStates : query = " + query);
		List<Map<String, Object>> rows = dbConnection.executeSelect(query);
		for(Map<String, Object> row: rows) {
			Long id = SapereUtil.getLongValue(row, "id");
			double value = SapereUtil.getDoubleValue(row, "value");
			try {
				MarkovStateHistory nextState = new MarkovStateHistory();
				nextState.setId(id);
				nextState.setStateId(SapereUtil.getIntValue(row, "state_id"));
				nextState.setValue(value);
				nextState.setDate((Date) row.get("date"));
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


	public static void saveNodeTransitionMatrices(PredictionContext predictionContext,
			NodeTransitionMatrices nodeTransitionMatrices, Map<String, Long> mapLastStateHistoryId) {
		StringBuffer query = new StringBuffer("");
		MarkovTimeWindow timeWindow = nodeTransitionMatrices.getTimeWindow();
		Integer timeWindowId = nodeTransitionMatrices.getTimeWindowId();
		//TransitionMatrixScope scope = nodeTransitionMatrices.getScope();
		String location = nodeTransitionMatrices.getLocation();
		String scenario = nodeTransitionMatrices.getScenario();
		if(timeWindowId>0) {
			boolean refreshTransitionMatrixCells = false;
			for(String variable : nodeTransitionMatrices.getMapMatrices().keySet()) {
				query = new StringBuffer("");
				query.append("INSERT INTO transition_matrix SET ")
				.append(CR).append(" id_time_window = " ).append(addQuotes(timeWindowId))
				.append(CR).append(",id_context = ").append(addQuotes(predictionContext.getId()))
				.append(CR).append(",variable_name = ").append(addSingleQuotes(variable))
				.append(CR).append(",location = ").append(addSingleQuotes(location))
				.append(CR).append(",scenario = ").append(addSingleQuotes(scenario))
				.append(CR).append(",learning_agent = ").append(addSingleQuotes(nodeTransitionMatrices.getLearningAgentName()))
				.append(CR).append(",last_update = current_timestamp()")
				.append(CR).append(" ON DUPLICATE KEY UPDATE last_update = current_timestamp()")
				;
				// Retrieve ID of transition matrix
				query.append(CR).append(" §")
					.append(CR).append("SET @id_transition_matrix = (SELECT ID FROM transition_matrix WHERE id_time_window=").append(addQuotes(timeWindowId))
					.append(" AND variable_name = ").append(addSingleQuotes(variable))
					.append(" AND location = ").append(addSingleQuotes(location))
					.append(" AND scenario = ").append(addSingleQuotes(scenario))
					.append(")");

				// Retrieve last iteration ID
				query.append(CR).append(" §")
					.append(CR).append("SET @last_id_iteration = (SELECT IFNULL(MAX(ID),0) FROM transition_matrix_iteration WHERE id_transition_matrix = @id_transition_matrix)");

				// Save iteration
				long timeShiftMS = predictionContext.getTimeShiftMS();
				String startDate = UtilDates.format_sql.format(timeWindow.getStartDate(timeShiftMS));
				String endDate = UtilDates.format_sql.format(timeWindow.getEndDate(timeShiftMS));
				query.append(CR).append(" §")
				.append(CR).append("INSERT INTO transition_matrix_iteration SET ")
				.append(CR).append("    id_time_window = " ).append(addQuotes(timeWindowId))
				.append(CR).append("   ,id_transition_matrix = @id_transition_matrix")
				.append(CR).append("   ,begin_date = ").append(addSingleQuotes(startDate))
				.append(CR).append("   ,end_date = ").append(addSingleQuotes(endDate))
				.append(CR).append("   ,last_update = current_timestamp()")
				.append(CR).append("   ,number = 1+ (SELECT IFNULL(MAX(number),0) FROM transition_matrix_iteration AS it ")
				.append(CR).append("       WHERE it.id_transition_matrix = @id_transition_matrix)")
				.append(CR).append(" ON DUPLICATE KEY UPDATE last_update = current_timestamp()")
				.append(CR).append(" §")
				.append(CR).append(" SET @IsNewIteration = LAST_INSERT_ID() > @last_id_iteration")
				;
				// Update number of iteration
				query.append(CR).append(" §")
					.append(CR).append("SET @id_iteration=(SELECT ID FROM transition_matrix_iteration WHERE id_transition_matrix = @id_transition_matrix")
					.append(CR).append(" AND end_date = ").append(addSingleQuotes(endDate)).append(")")
				;
				query.append(CR).append(" §")
					.append(CR).append(" UPDATE transition_matrix SET iteration_number = " )
					.append(CR).append("     (SELECT IFNULL(MAX(it.number),0) FROM transition_matrix_iteration AS it WHERE it.id_transition_matrix = transition_matrix.ID) ")
					.append(CR).append("     WHERE transition_matrix.ID =  @id_transition_matrix")
				;
				// Save matrix cells
				Matrix iterObsMatrix = nodeTransitionMatrices.getIterObsMatrice(variable);
				Matrix iterCorrectionMatrix = nodeTransitionMatrices.getIterCorrectionsMatrice(variable);
				if(iterObsMatrix!=null) {
					List<String> iterationCells = new ArrayList<String>();
					for(int rowIdx = 0; rowIdx < iterObsMatrix.getRowDimension(); rowIdx++) {
						for(int columnIdx = 0; columnIdx < iterObsMatrix.getColumnDimension(); columnIdx++) {
							double iterObsNb = iterObsMatrix.get(rowIdx, columnIdx);
							double iterCorrectionNb = iterCorrectionMatrix.get(rowIdx, columnIdx);
							double iterObAndCorrectionNb = iterObsNb + iterCorrectionNb;
							if(iterObAndCorrectionNb != 0) {
								StringBuffer buffCellIteration = new StringBuffer();
								buffCellIteration.append("(@id_iteration,@id_transition_matrix,").append(rowIdx).append(",").append(columnIdx).append(",").append(iterObsNb).append(",").append(iterCorrectionNb).append(")");
								iterationCells.add(buffCellIteration.toString());
							}
						}
					}
					if(iterationCells.size()>0) {
						query.append(CR).append(" §")
							.append(CR).append("DELETE transition_matrix_cell_iteration FROM transition_matrix_cell_iteration WHERE id_transition_matrix_iteration = @id_iteration");
						query.append(CR).append(" §")
							 .append(CR).append("INSERT INTO transition_matrix_cell_iteration(id_transition_matrix_iteration,id_transition_matrix ,row_idx,column_idx,obs_number,corrections_number) VALUES ")						
						;
						query.append(SapereUtil.implode(iterationCells, CR+","));
						if(mapLastStateHistoryId.containsKey(variable)) {
							Long lastStateHistoryId = mapLastStateHistoryId.get(variable);
							query.append(CR).append(" §")
								.append(CR).append("UPDATE state_history SET observation_update = NOW() WHERE id =" + addQuotes(lastStateHistoryId));
						}
						refreshTransitionMatrixCells = true;
					}
				}
				//logger.info("saveNodeTransitionMatrices " + query.toString());
				dbConnection.execUpdate(query.toString());
			} // end for
			if(refreshTransitionMatrixCells) {
				refreshTransitionMatrixCell(predictionContext,timeWindowId);
			}
		}
	}

	public static void refreshTransitionMatrixCell(PredictionContext predictionContext,int timeWindowId) {
		dbConnection.setDebugLevel(10);
		StringBuffer query = new StringBuffer();
		Long ctxId = predictionContext.getId();
		query.append("CALL REFRESH_TRANSITION_MATRIX_CELL2(").append(ctxId)
			.append(",").append(addQuotes(timeWindowId))
			.append(",").append(predictionContext.getLearningWindow()).append(")");
		dbConnection.execUpdate(query.toString());
		dbConnection.setDebugLevel(0);
	}


	public static Integer addPredictionCorrection(
			 PredictionContext predictionContext
			,PredictionDeviation deviation
			,String tag
			) {
		if(!deviation.isMeaningful()) {
			// nothing to do.
			return null;
		}
		logger.info("addPredictionCorrection : deviation = " + deviation);
		String sessionId = DBConnection.getSessionId();
		int initialStateIdx = deviation.getInitialState().getIndex();
		int overStateIdx = deviation.getStateOver().getIndex();
		int underStateIdx = deviation.getStateUnder().getIndex();
		long idTransitionMatrix = deviation.getTransitionMatrixKey().getId();
		int timeWindowId = deviation.getTransitionMatrixKey().getTimeWindowId();
		int rowIdx = overStateIdx; 	// first version : does not work very well
		double excess = deviation.getExcess();
		rowIdx = initialStateIdx; // works better ???
		StringBuffer query = new StringBuffer();
		query.append("SET @it_number = (SELECT iteration_number FROM transition_matrix tm where id = ")
			.append(idTransitionMatrix).append(")")
		;
		query.append(CR).append("§");
		query.append(CR).append("SET @it_id = (SELECT id FROM transition_matrix_iteration tmi WHERE tmi.id_transition_matrix = ")
			.append(idTransitionMatrix)
			.append(" AND tmi.number = @it_number)")
		;
		query.append(CR).append("§");
		query.append(CR).append("SET @row_sum = (SELECT IFNULL(SUM(obs_number + corrections_number),0) FROM transition_matrix_cell AS tmc ")
			.append(" WHERE tmc.id_transition_matrix = ").append(idTransitionMatrix)
			.append(" AND row_idx = ").append(rowIdx).append(")")
		;
		/*
		query.append(CR).append("§");
		query.append(CR).append("SET @cell_sum = (SELECT IFNULL(SUM(obs_number + corrections_number),0) FROM transition_matrix_cell AS tmc ")
			.append(" WHERE tmc.id_transition_matrix = ").append(idTransitionMatrix)
			.append(" AND row_idx = ").append(rowIdx).append(" AND column_idx = ").append(underStateIdx).append(")")
		;*/
		query.append(CR).append("§");
		//query.append(CR).append("SET @added_corrections_number = GREATEST(1, ROUND(0.1*(0.5*@row_sum - @cell_sum)))");
		//query.append(CR).append("§");
		query.append(CR).append("SET @added_corrections_number = GREATEST(1, ROUND(0.1*").append(excess).append(" * @row_sum))");
		query.append(CR).append("§");
		query.append(CR).append("INSERT INTO transition_matrix_cell_iteration SET")
				.append(CR).append(" id_transition_matrix_iteration = @it_id")
				.append(CR).append(",id_transition_matrix = ").append(idTransitionMatrix)
				.append(CR).append(",row_idx = ").append(rowIdx)
				.append(CR).append(",column_idx = ").append(overStateIdx)
				.append(CR).append(",corrections_number = -1*@added_corrections_number")
				.append(CR).append(" ON DUPLICATE KEY UPDATE corrections_number = -1*@added_corrections_number + corrections_number")
		;
		query.append(CR).append("§");
		query.append(CR).append("INSERT INTO transition_matrix_cell_iteration SET")
				.append(CR).append(" id_transition_matrix_iteration = @it_id")
				.append(CR).append(",id_transition_matrix = ").append(idTransitionMatrix)
				.append(CR).append(",row_idx = ").append(rowIdx)
				.append(CR).append(",column_idx = ").append(underStateIdx)
				.append(CR).append(",corrections_number = 1*@added_corrections_number")
				.append(CR).append(" ON DUPLICATE KEY UPDATE corrections_number = 1*@added_corrections_number + corrections_number")
		;
		query.append(CR).append("§");
		query.append(CR).append("UPDATE transition_matrix SET last_update=NOW() WHERE id = ").append(idTransitionMatrix);
		query.append(CR).append("§");
		query.append(CR).append("INSERT INTO log_self_correction SET")
			.append(CR).append(" id_session = ").append(addSingleQuotes(sessionId))
			.append(CR).append(",tag = ").append(addSingleQuotes(tag))
			.append(CR).append(",id_transition_matrix = ").append(idTransitionMatrix)
			.append(CR).append(",id_transition_matrix_iteration = @it_id")
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
		query.append(CR).append("§");
		query.append(CR).append("SET @id_correction = LAST_INSERT_ID()");
		String sPredictionIds = deviation.getStrListIdPredictions(",");
		if(sPredictionIds.length() > 0) {
			query.append(CR).append("§");
			query.append("UPDATE prediction SET id_correction = @id_correction WHERE id IN (").append(sPredictionIds).append(")");
		}
		query.append(CR).append("§");
		query.append(CR).append("SELECT * FROM log_self_correction WHERE id = @id_correction");
		/*
		query.append(CR).append("UPDATE transition_matrix_cell_iteration tmci SET tmci.errors_number = tmci.errors_number + 1 ")
			.append(" WHERE tmci.id_transition_matrix_iteration = @it_id AND tmci.row_idx = " + fromStateIdx + " AND tmci.column_idx = " + destStateIdx )
		;*/
		dbConnection.setDebugLevel(10);
		Map<String, Object> row = dbConnection.executeSingleRowSelect(query.toString());
		Integer correctionNumber = SapereUtil.getIntValue(row, "corrections_number");
		logger.info("addPredictionError  : correctionNumber = " + correctionNumber);
		dbConnection.setDebugLevel(0);
		refreshTransitionMatrixCell(predictionContext, timeWindowId);
		return correctionNumber;
	}


	public static void consolidatePredictions(PredictionContext predictionContext) {
		StringBuffer query = new StringBuffer();
		query.append("SET @min_date = (SELECT MIN(creation_date) FROM prediction WHERE NOT link_done")
			.append(" AND id_context = ").append(predictionContext.getId())
			.append(")");
		query.append(CR).append("§");
		query.append("SET @max_date = DATE_ADD(NOW() , INTERVAL -2 MINUTE)");
		query.append(CR).append("§");
		Long ctxId = predictionContext.getId();
		query.append(CR).append("CALL SP_CONSOLIDATE_PREDICTIONS(")
			 .append(ctxId).append(", @min_date, @max_date)");
		long result = dbConnection.execUpdate(query.toString());
		if(result<0) {
			logger.warning("consolidatePredictions : sql error");
		}
	}

	public static List<PredictionResult> retrieveListPredictionReults(
			 PredictionContext predictionContext
			 , Date computeDayFilter
			,TimeSlot targetTimeSLot) {
		Date minDate = UtilDates.removeTime(targetTimeSLot.getBeginDate());
		Date maxDate = UtilDates.removeTime(targetTimeSLot.getEndDate());
		int minHour = UtilDates.getHourOfDay(targetTimeSLot.getBeginDate());
		int maxHour = UtilDates.getHourOfDay(targetTimeSLot.getEndDate());
		Map<String, StatesStatistic> mapStateStatistics = retrieveStatesStatistics(predictionContext, computeDayFilter, computeDayFilter, minDate, maxDate, minHour, maxHour, null);
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
			.append(CR).append(" ,(SELECT tr.id_time_window FROM transition_matrix tr WHERE tr.id=p.id_target_transition_matrix) AS targetTWid")
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
			Date targetDate = (Date) row.get("target_date");
			int targetHour = UtilDates.getHourOfDay(targetDate);
			Date initialDate = (Date) row.get("initial_date");
			String variable = "" + row.get("variable_name");
			//Date computeDay = (Date) row.get("compute_day");
			int targetTimeWindowId =  SapereUtil.getIntValue(row, "targetTWid");
			MarkovTimeWindow targetTW = predictionContext.getMarkovTimeWindow(targetTimeWindowId);
			//String statKey = StatesStatistic.generateKey(computeDay, targetDate, targetHour, variable);
			String statKey = StatesStatistic.generateKey(targetDate, targetHour, variable);
			StatesStatistic actualStatesStatistics = mapStateStatistics.get(statKey);
			//TransitionMatrixKey trMatrixKey = predictionContext.getTransitionMatrixKey(targetTimeWindowId, variable);
			MarkovState initialState = NodeMarkovStates.getById(1+initialStateIdx);
			MarkovState actualState = NodeMarkovStates.getById(1+actualStateIdx);
			//MarkovState predictedState = NodeMarkovStates.getById(1+predictedStateIdx);
			PredictionResult predictionResult = new PredictionResult(initialDate, initialState, targetDate, variable, targetTW);
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
				Double proba = SapereUtil.getDoubleValue(row2, "proba");
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


	public static void savePredictionResult(PredictionData prediction) {
		Date initialDate = prediction.getInitialDate();
		String sqlInitialDate = addSingleQuotes(UtilDates.format_sql.format(initialDate));
		StringBuffer sqlCleanPredictions = new StringBuffer();
		sqlCleanPredictions.append("DROP TEMPORARY TABLE IF EXISTS TmpCleanPrediction");
		sqlCleanPredictions.append(CR).append("§");
		sqlCleanPredictions.append("CREATE TEMPORARY TABLE TmpCleanPrediction AS")
			.append(CR).append("SELECT id FROM prediction WHERE initial_date BETWEEN ").append(sqlInitialDate).append(" AND DATE_ADD(").append(sqlInitialDate).append(", INTERVAL 1 HOUR)")
			.append(CR).append(" AND creation_date < DATE_ADD(NOW(), INTERVAL -1 HOUR)");
		sqlCleanPredictions.append(CR).append("§");
		sqlCleanPredictions.append(CR).append("DELETE pi FROM prediction_item pi WHERE pi.id_prediction IN (SELECT id FROM TmpCleanPrediction)");
		sqlCleanPredictions.append(CR).append("§");
		sqlCleanPredictions.append(CR).append("DELETE p FROM prediction p WHERE p.id IN (SELECT id FROM TmpCleanPrediction)");
		dbConnection.execUpdate(sqlCleanPredictions.toString());
		/*
		sqlCleanPredictions.append("DROP TEMPORARY TABLE IF EXISTS TmpCleanPredictionItem");
		sqlCleanPredictions.append(CR).append("§");
		sqlCleanPredictions.append("CREATE TEMPORARY TABLE TmpCleanPredictionItem AS "
				+ " SELECT sh.id FROM TmpCleanPrediction"
				+ " JOIN prediction_item pi  on pi.id_last  = TmpCleanSH.id");)
		*/
		PredictionContext predictionCtx = prediction.getContext();
		for(Date targetDate : prediction.getTargetDates()) {
			PredictionStep firstStep = prediction.getFirstStep();
			PredictionStep lastStep = prediction.getLastStep(targetDate);
			for(String variableName : prediction.getVariables()) {
				if(prediction.hasResult(variableName, targetDate)) {
					PredictionResult predictionResult = prediction.getResult(variableName, targetDate);
					StringBuffer sqlInsertPrediction = new StringBuffer();
					long horizonMinutes = Math.round(predictionResult.getTimeHorizonMinutes());
					sqlInsertPrediction.append("INSERT INTO prediction SET variable_name=").append(addSingleQuotes(variableName))
						.append(", id_context =").append(predictionCtx.getId())
						.append(", location =").append(addSingleQuotes(predictionCtx.getLocation()))
						.append(", scenario =").append(addSingleQuotes(predictionCtx.getScenario()))
						.append(", initial_date=").append(sqlInitialDate)
						.append(", target_date=").append(addSingleQuotes(UtilDates.format_sql.format(targetDate)))
						.append(", target_day=").append(addSingleQuotes(UtilDates.format_sql_day.format(targetDate)))
						.append(", target_hour=").append(addQuotes(UtilDates.getHourOfDay(targetDate)))
						.append(", horizon_minutes=").append(horizonMinutes)
						.append(", learning_window=").append(predictionCtx.getLearningWindow())
						.append(", use_corrections=").append(prediction.isUseCorrections())
					;
					if(firstStep != null) {
						Long initialTRid = firstStep.getUsedTransitionMatrixId(variableName);
						sqlInsertPrediction.append(", id_initial_transition_matrix=").append(initialTRid);
					}
					if(lastStep != null) {
						Long initialTRid = lastStep.getUsedTransitionMatrixId(variableName);
						sqlInsertPrediction.append(", id_target_transition_matrix=").append(initialTRid);
					}
					MarkovTimeWindow initialTimeWindow = prediction.getInitialTimeWindow();
					if(initialTimeWindow != null) {
						sqlInsertPrediction.append(", id_initial_time_window=").append(initialTimeWindow.getId());
					}
					MarkovState initialState = predictionResult.getInitialState();
					if(initialState != null) {
						sqlInsertPrediction.append(", initial_state_idx=").append(initialState.getIndex())
						.append(", initial_state_name=").append(addSingleQuotes(initialState.getName()));
					}
					List<Double> stateProbabilities = predictionResult.getStateProbabilities();
					MarkovState randomState = predictionResult.getRadomTargetState();
					if(randomState !=null) {
						int stateIdx = randomState.getIndex();
						String stateName = randomState.getName();
						double stateProba = predictionResult.getStateProbability(stateIdx);
						sqlInsertPrediction.append(", random_state_idx=").append(stateIdx)
							.append(", random_state_name=").append(addSingleQuotes(stateName))
							.append(", random_state_proba=").append(addQuotes(stateProba));
					}
					MarkovState mostLikelyState = predictionResult.getMostLikelyState();
					if(mostLikelyState != null) {
						int stateIdx = mostLikelyState.getIndex();
						double stateProba = predictionResult.getStateProbability(stateIdx);
						sqlInsertPrediction.append(", likely_state_idx=").append(stateIdx)
							.append(", likely_state_name=").append(addSingleQuotes(mostLikelyState.getName()))
							.append(", likely_state_proba=").append(addQuotes(stateProba));
					}
					double predictionId = dbConnection.execUpdate(sqlInsertPrediction.toString());
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
	}

	public static Map<String, Double> evaluateAllStatesEntropie() {
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

	public static List<String> initTmpStateProd() {
		StringBuffer sqlQuery = new StringBuffer();
		sqlQuery.append("DROP TABLE IF EXISTS tmp_prod_states")
				.append(CR).append( "§")
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
				.append(CR).append( "					WHERE meteo_data.ut_timestamp =  UNIX_TIMESTAMP(mr.timestamp) - UNIX_TIMESTAMP(mr.timestamp) % 3600) AS id_meteo_data")
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
		sqlQuery.append(CR).append( "§")
				.append(CR).append("UPDATE tmp_prod_states st")
				.append(CR).append( "	JOIN time_window tw on tw.id = st.id_time_window")
				.append(CR).append( "	SET st.start_hour = tw.start_hour")
				.append(CR).append( "§")
				.append(CR).append( "UPDATE tmp_prod_states st")
				.append(CR).append( "	JOIN  clemap_data_light.meteo_data on meteo_data.id = st.id_meteo_data")
				.append(CR).append( "	SET ")
				.append(CR).append( "		 st.gh 			= meteo_data.gh")
				.append(CR).append( "		,st.ta 			= meteo_data.ta")
				.append(CR).append( "§")
				.append(CR).append( "UPDATE tmp_prod_states SET")
				.append(CR).append( "		gh_class 	= FLOOR(gh/300)")
				.append(CR).append( "		,ta_class	= if(ta>-99,FLOOR(ta/10),null)")
				//.append(CR).append( "§")
		;
		dbConnection.execUpdate(sqlQuery.toString());
		return Arrays.asList("start_hour", "gh_class","day_of_week");
	}

	public static double evaluateStateEntropie(List<String> fields) {
		StringBuffer sqlQuery = new StringBuffer();
		String sKey = String.join(",", fields);
		StringBuffer joinCriteria = new StringBuffer();
		String sep="";
		for(String field : fields) {
			joinCriteria.append(sep).append("total.").append(field).append(" = ").append("sub_total.").append(field);
			sep = " AND ";
		}
		sqlQuery.append("DROP TEMPORARY TABLE IF EXISTS Tmp_entropie")
				.append(CR).append( "§")
				.append(CR).append("SET @total_nb=(select Count(*) FROM tmp_prod_states)")
				.append(CR).append( "§")
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
				.append(CR).append("§")
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
			double entropie = SapereUtil.getDoubleValue(row, "entropie");
			return entropie;
	}

	public static synchronized Map<String, StatesStatistic> retrieveStatesStatistics(
			 PredictionContext predictionContext
			,Date minCreationDate
			,Date maxCreationDate
			,Date minTargetDate
			,Date maxTargetDate
			,Integer minHour
			,Integer maxHour
			,String variableFilter) {
		Long ctxId = predictionContext.getId();
		String location = predictionContext.getLocation();
		String scenario = predictionContext.getScenario();
		//List<StatesStatistic> result = new ArrayList<StatesStatistic>();
		String sqlMinCreationDate = minCreationDate==null ? "NULL" : addSingleQuotes(UtilDates.format_sql_day.format(minCreationDate));
		String sqlMaxCreationDate = maxCreationDate==null? "NULL" : addSingleQuotes(UtilDates.format_sql_day.format(maxCreationDate));
		String sqlMinTargetDate = minTargetDate==null? "NULL" : addSingleQuotes(UtilDates.format_sql.format(minTargetDate));
		String sqlMaxTargetDate = maxTargetDate==null? "NULL" : addSingleQuotes(UtilDates.format_sql.format(maxTargetDate));
		// Retieve states statistics header
		Map<String, StatesStatistic> mapStateStatistics = new HashMap<>();
		StringBuffer query3 = new StringBuffer();
		query3.append("CALL SP_COMPUTE_STATE_DISTRIBUTION(").append(ctxId)
		.append(",").append(sqlMinCreationDate)
		.append(",").append(sqlMaxCreationDate)
		.append(",").append(sqlMinTargetDate)
		.append(",").append(sqlMaxTargetDate)
		//.append(",").append((minHour==null )? "NULL" : minHour)
		//.append(",").append((maxHour==null )? "NULL" : maxHour)
		.append(",").append((variableFilter==null || "".equals(variableFilter))? "NULL" : addSingleQuotes(variableFilter))
		.append(")");
		query3.append(CR + "§");
		query3.append("SELECT * FROM TmpStateDistribution");
		logger.info("retrieveStatesStatistics query = " + CR + query3.toString());
		List<Map<String, Object>> rows = dbConnection.executeSelect(query3.toString());
		for(Map<String, Object> nextRow : rows) {
			String variableName = "" + nextRow.get("variable_name");
			Date stateDate = (Date) nextRow.get("date");
			Date computeDay = (Date) nextRow.get("compute_day");
			Integer hour = SapereUtil.getIntValue(nextRow, "hour");
			if((minHour == null || hour >= minHour) && (maxHour == null || hour <= maxHour)) {
				StatesStatistic stateStatistic = new StatesStatistic();
				stateStatistic.setLocation(location);
				stateStatistic.setScenario(scenario);
				stateStatistic.setVariable(variableName);
				stateStatistic.setDate(stateDate);
				stateStatistic.setComputeDay(computeDay);
				stateStatistic.setHour(hour);
				stateStatistic.setGiniIndex(SapereUtil.getDoubleValue(nextRow, "gini_index"));
				stateStatistic.setShannonEntropie(SapereUtil.getDoubleValue(nextRow, "shannon_entropie"));
				String key = stateStatistic.getKey();// SapereUtil.format_day.format(stateDate)+ "." + hour + "." + variableName;
				mapStateStatistics.put(key, stateStatistic);
				boolean toLog = false;
				if(toLog) {
					logger.info("retrieveStatesStatistics : key = " + key);
				}
			}
		}
		// Add states distribution
		String sqlVariableFilter = (variableFilter==null || "".equals(variableFilter))? "1": "variable_name=" + addSingleQuotes(variableFilter);
		//String sqlHourFilter1 = (minHour == null) ? "1" : "hour >= " + minHour;
		//String sqlHourFilter2 = (maxHour == null) ? "1" : "hour <= " + maxHour;
		String queryStateDistribution = "SELECT * FROM TmpStateDistrib1 WHERE " + sqlVariableFilter
				// + " AND " + sqlHourFilter1 + " AND " + sqlHourFilter2
				;
		List<Map<String, Object>> rows2 = dbConnection.executeSelect(queryStateDistribution.toString());
		for(Map<String, Object> nextRow : rows2) {
			Date stateDate = (Date) nextRow.get("date");
			//Date computeDay = (Date) nextRow.get("compute_day");
			Integer hour = SapereUtil.getIntValue(nextRow, "hour");
			String variableName = "" + nextRow.get("variable_name");
			//String key = StatesStatistic.generateKey(computeDay, stateDate, hour, variableName);
			String key = StatesStatistic.generateKey(stateDate, hour, variableName);
			if(mapStateStatistics.containsKey(key)) {
				StatesStatistic statesStatistic = mapStateStatistics.get(key);
				String stateName = "" + nextRow.get("state_name");
				Integer nb = SapereUtil.getIntValue(nextRow, "nb");
				statesStatistic.addStateNb(stateName, nb);
			}
		}
		logger.info("retrieveStatesStatistics : mapStateStatistics size = " + mapStateStatistics.size());
		return mapStateStatistics;
	}

	public static Map<String,PredictionStatistic> computePredictionStatistics(
			 PredictionContext predictionContext
			,Date minComputeDate
			,Date maxComputeDate
			,Date minTargetDate
			,Date maxTargetDate
			,Integer minHour
			,Integer maxHour
			,Boolean useCorrectionFilter
			,String variableFilter
			,List<String> fieldsToMerge
			) {
		logger.info("computePredictionStatistics : begin");
		consolidatePredictions(predictionContext);
		Long ctxId = predictionContext.getId();
		String location = predictionContext.getLocation();
		String scenario = predictionContext.getScenario();
		Map<String,PredictionStatistic> result = new HashMap<String, PredictionStatistic>();
		String sqlMinComputeDate = (minComputeDate == null)? "NULL" : addSingleQuotes(UtilDates.format_sql_day.format(minComputeDate));
		String sqlMaxComputeDate = (maxComputeDate == null)? "NULL" : addSingleQuotes(UtilDates.format_sql_day.format(maxComputeDate));
		String sqlMinTargetDate = (minTargetDate == null)? "NULL" : addSingleQuotes(UtilDates.format_sql_day.format(minTargetDate));
		String sqlMaxTargetDate = (maxTargetDate == null)? "NULL" : addSingleQuotes(UtilDates.format_sql_day.format(maxTargetDate));
		StringBuffer query1 = new StringBuffer();
		query1.append("CALL SP_COMPUTE_PREDICTION_STATISTICS(").append(ctxId)
			.append(",").append(sqlMinComputeDate)
			.append(",").append(sqlMaxComputeDate)
			.append(",").append(sqlMinTargetDate)
			.append(",").append(sqlMaxTargetDate)
			.append(",").append((minHour==null )? "NULL" : minHour)
			.append(",").append((maxHour==null )? "NULL" : maxHour)
			.append(",").append((useCorrectionFilter==null)? "NULL" : useCorrectionFilter)
			.append(",").append((variableFilter==null || "".equals(variableFilter))? "NULL" : addSingleQuotes(variableFilter))
			.append(")")
		;
		dbConnection.execUpdate(query1.toString());
		// Retrieve states distribution
		Map<String, StatesStatistic> mapStatesStatistics = retrieveStatesStatistics(predictionContext, minComputeDate, maxComputeDate, minTargetDate, maxTargetDate, minHour, maxHour, variableFilter);
		String groupByClause = "compute_day, target_day, variable_name";
		boolean mergeHorizons = fieldsToMerge.contains("horizon");
		boolean mergeUseCorrections = fieldsToMerge.contains("useCorrection");
		boolean mergeHour  = fieldsToMerge.contains("hour");
		if(!mergeHorizons) {
			groupByClause = groupByClause+",horizon";
		}
		if(!mergeUseCorrections) {
			groupByClause = groupByClause+",use_corrections";
		}
		if(!mergeHour) {
			groupByClause = groupByClause+",time_slot";
		}
		StringBuffer query3 = new StringBuffer();
		query3.append("SELECT variable_name, compute_day, target_day")
			.append(CR).append( "	,GROUP_CONCAT(DISTINCT time_slot ORDER BY time_slot) AS time_slot")
			.append(CR).append( "	,GROUP_CONCAT(DISTINCT horizon ORDER BY horizon) AS horizon")
			.append(CR).append( "	,GROUP_CONCAT(DISTINCT If(use_corrections, 'True', 'False') ORDER BY use_corrections) AS use_corrections")
			.append(CR).append("	,DATE_ADD(target_day, INTERVAL (MIN(time_slot)+0) HOUR) AS date_begin")
			.append(CR).append("	,DATE_ADD(target_day, INTERVAL (MAX(time_slot)+1) HOUR) AS date_end")
			.append(CR).append( "	,AVG(rate_ok1)  AS rate_ok1")
			.append(CR).append( "	,AVG(rate_ok2)  AS rate_ok2")
			.append(CR).append( "	,AVG(vector_differential) AS vector_differential")
			.append(CR).append( "	,SUM(nb_ok2)  AS nb_ok2")
			.append(CR).append( "   ,SUM(nb_total) AS nb_total")
			.append(CR).append( "   ,SUM(corrections_number) AS corrections_number")
			.append(CR).append( "	,AVG(proba1)  AS proba_avg1")
			.append(CR).append( "	,AVG(proba2)  AS proba_avg2")
			.append(CR).append( "	,AVG(gini_index) AS gini_index")
			.append(CR).append( "	,AVG(shannon_entropie) AS shannon_entropie")
			.append(CR).append( "	,Count(*) as nb_results")
			.append(CR).append( "	,(SELECT ID from transition_matrix tm WHERE tm.variable_name = TmpPredictionStatistic.variable_name AND id_time_window = TmpPredictionStatistic.id_initial_time_window ")
			.append(CR).append( "				AND tm.id_context = TmpPredictionStatistic.id_context) AS id_tm")
			.append(CR).append( "	FROM TmpPredictionStatistic")
			//.append(CR).append( "	WHERE date>=").append(addSingleQuotes( sqlMinDate))
			.append(CR).append( "	GROUP BY ").append(groupByClause)
			//.append(CR).append( "	ORDER BY rate_ok2, shannon_entropie DESC")
			.append(CR).append( "	ORDER BY date_begin, variable_name, horizon")
		;
		List<Map<String, Object>> rows = dbConnection.executeSelect(query3.toString());
		for(Map<String, Object> nextRow : rows) {
			PredictionStatistic nextStatistic = new PredictionStatistic();
			nextStatistic.setComputeDay((Date) nextRow.get("compute_day"));
			nextStatistic.setLocation(location);
			nextStatistic.setScenario(scenario);
			nextStatistic.setVariable("" + nextRow.get("variable_name"));
			nextStatistic.setNbOfPredictions(SapereUtil.getIntValue(nextRow, "nb_total"));
			nextStatistic.setNbOfSuccesses(SapereUtil.getIntValue(nextRow, "nb_ok2"));
			nextStatistic.setNbOfCorrections(SapereUtil.getIntValue(nextRow, "corrections_number"));
			if(nextRow.get("vector_differential") == null) {
				logger.info("vector_differential is null");
			} else {
				nextStatistic.setDifferential(SapereUtil.getDoubleValue(nextRow, "vector_differential"));
			}
			Date dateBegin = (Date) nextRow.get("date_begin");
			Date dateEnd = (Date) nextRow.get("date_end");
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
			String statisticKey = stateKey + "."+(mergeHorizons? "*" : sHorizons) + "." + (mergeUseCorrections? "*": sListUseOfCorrections);
			result.put(statisticKey, nextStatistic);
		}
		StringBuffer query4 = new StringBuffer();
		query4.append("SELECT compute_day, target_day, hour, variable_name, state_name")
			.append(CR).append( "	,GROUP_CONCAT(DISTINCT horizon ORDER BY horizon) AS horizon")
			.append(CR).append( "	,GROUP_CONCAT(DISTINCT If(use_corrections, 'True', 'False') ORDER BY use_corrections) AS use_corrections")
			.append(CR).append( "	,ROUND(AVG(proba),5) AS proba")
			.append(CR).append("  FROM TmpPredictionStatisticLine")
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
			//Date computeDay = (Date) nextRow.get("compute_day");
			Date date = (Date) nextRow.get("target_day");
			int hour = SapereUtil.getIntValue(nextRow, "hour");
			//String stateKey = StatesStatistic.generateKey(computeDay, date, hour, variableName);
			String stateKey = StatesStatistic.generateKey(date, hour, variableName);
			String sHorizons = "" + nextRow.get("horizon");
			String sListUseOfCorrections = "" + nextRow.get("use_corrections");
			String statisticKey = stateKey
					+ "." + (mergeHorizons? "*" : sHorizons)
					+ "." + (mergeUseCorrections? "*": sListUseOfCorrections);
			if(result.containsKey(statisticKey)) {
				PredictionStatistic predictionStatistic = result.get(statisticKey);
				String stateName = "" + nextRow.get("state_name");
				Double proba = SapereUtil.getDoubleValue(nextRow, "proba");
				predictionStatistic.addMeanOfProba(stateName, proba);
				//logger.info("add state proba " + proba + " for state " + stateName + " in " + statisticKey);
			} else {
				//logger.warning(statisticKey + " not contained in result map");
			}
		}
		logger.info("computePredictionStatistics : end");
		return result;
	}
}
