package com.saperetest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.db.DBConfig;
import com.sapereapi.db.DBConnection;
import com.sapereapi.db.DBConnectionFactory;
import com.sapereapi.db.SessionManager;
import com.sapereapi.log.SimulatorLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.energy.Device;
import com.sapereapi.model.energy.DeviceMeasure;
import com.sapereapi.model.energy.DeviceProperties;
import com.sapereapi.model.energy.node.NodeTotal;
import com.sapereapi.model.energy.input.SimulatorLog;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.PhaseNumber;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;
import com.saperetest.model.DeviceItem;

public class SimulatorDBHelper {
	private static DBConnection dbConnection = null;
	private static SimulatorDBHelper instance = null;
	private static DBConfig dbConfig = null;
	public final static String CR = System.getProperty("line.separator");	// Cariage return
	private static int debugLevel = 0;
	private static SimulatorLogger logger = SimulatorLogger.getInstance();
	public static String SIMULATOR_DB = "clemap";
	//public final static String CLEMAPDATA_DBNAME = "clemap_data_light";

	public static SimulatorDBHelper getInstance() {
		if (instance == null) {
			instance = new SimulatorDBHelper();
		}
		return instance;
	}

	public static void init(DBConfig aDBConfig) {
		dbConfig = aDBConfig;
		// initialise db connection
		instance = new SimulatorDBHelper();
	}

	public SimulatorDBHelper() {
		// initialise db connection
		DBConnectionFactory.init(SIMULATOR_DB, dbConfig, logger);
		dbConnection = DBConnectionFactory.getInstance(SIMULATOR_DB);// new DBConnection(dbConfig, logger);
		//dbConnectionClemapData = new DBConnection("jdbc:mariadb://129.194.10.168/clemap_data", "import_clemap", "sql2537");
	}

	public static String getSessionNumber() {
		return SessionManager.getSessionNumber();
	}

	public static List<Device> retrieveMeyrinDevices() throws Exception {
		List<Device> result = new ArrayList<>();
		String sqlRequest = "SELECT d.*"
				+ CR + ",GROUP_CONCAT(IFNULL(si.phase, '') ORDER BY si.phase) AS phases"
				+ CR + "FROM device d"
				+ CR + "LEFT JOIN sensor_input si  ON si.id_device = d.id"
				+ CR + "WHERE d.living_lab = 'Vergers' AND NOT IFNULL(si.is_disabled,0)"
				+ CR + "GROUP BY d.id";
		List<Map<String, Object>> rows = dbConnection.executeSelect(sqlRequest);
		for (Map<String, Object> row : rows) {
			List<String> listPhases = SapereUtil.getListStrValue(row, "phases");
			long deviceId = SapereUtil.getLongValue(row, "id");
			double powerMin = SapereUtil.getDoubleValue(row, "power_min", logger);
			double powerMax = SapereUtil.getDoubleValue(row, "power_max", logger);
			double avergaeDuration = SapereUtil.getDoubleValue(row, "avg_duration", logger);
			String deviceName = "" + row.get("name");
			DeviceCategory deviceCategory = SapereUtil.getDeviceCategoryValue(row, "category");
			//boolean isProducer = deviceCategory.isProducer();
			EnvironmentalImpact envImpact = SapereUtil.getEnvironmentalImpactValue(row, "environmental_impact");
			DeviceProperties nextDeviceProperties = new DeviceProperties(deviceName, deviceCategory, envImpact);
			nextDeviceProperties.setPriorityLevel(SapereUtil.getPriorityLevelValue(row, "priority_level"));
			//nextDeviceProperties.setProducer(isProducer);
			nextDeviceProperties.setLocation("" + row.get("location"));
			nextDeviceProperties.setElectricalPanel("" + row.get("panel_input"));
			nextDeviceProperties.setSensorNumber("" + row.get("serial_number"));
			Device nextDevice = new Device(deviceId, nextDeviceProperties, powerMin, powerMax, avergaeDuration);
			for (String sphase : listPhases) {
				if(sphase.length() == 0) {
					// do nothing
					logger.info("retrieveMeyrinDevices : pahse is empty for device " + deviceName);
				} else  {
					try {
						PhaseNumber phase = PhaseNumber.valueOf(sphase);
						nextDeviceProperties.addPhase(phase);
					} catch (Throwable e) {
						logger.error(e);
					}
				}
			}
			result.add(nextDevice);
		}
		return result;
	}

	public static DeviceMeasure retrieveLastDevicesMeasure(String featureType, Date dateBegin, Date dateEnd) throws Exception {
		// TODO : transfer this request in the python code that fill measure_record table.
		dbConnection.execUpdate("UPDATE measure_record SET id_sensor = (SELECT sensor.id FROM sensor where sensor.serial_number = measure_record.sensor_number) where id_sensor is NULL");
		String filterFeatureType = (featureType == null)? "1" : "measure_record.feature_type = " + addSingleQuotes(featureType);
		String filterDateBegin = dateBegin==null? "1" : "measure_record.timeStamp >=" + addSingleQuotes(UtilDates.format_sql.format(dateBegin));
		String filterEndDate = dateEnd==null? "1" :   "measure_record.timeStamp <" + addSingleQuotes(UtilDates.format_sql.format(dateEnd));
		String sqlQuery = "SELECT  measure_record.* "
					// Set seconds = 0 to timestamp3
				+CR + ",TIMESTAMPADD(MINUTE,"
				+CR + "    TIMESTAMPDIFF(MINUTE,DATE(timestamp),timestamp),"
				+CR + "    DATE(timestamp)) AS timestamp3"
				+CR	+",sensor.serial_number AS sensor_number"
				+CR	+",sensor.location"
				+CR	+",(SELECT device.name from device where device.id = sensor_input.id_device  ) AS device_name"
				+CR + "FROM measure_record"
				//+CR	+ "JOIN sensor on sensor.serial_number = measure_record.sensor_number"
				+CR	+ "JOIN sensor on sensor.id = measure_record.id_sensor"
				+CR	+ "JOIN phase_measure_record AS phase_mr ON phase_mr.id_measure_record = measure_record.id"
				+CR	+ "JOIN sensor_input ON  sensor_input.id_sensor = sensor.id AND sensor_input.phase=phase_mr.phase"
				+CR + "WHERE " + filterDateBegin + " AND " + filterEndDate + " AND " + filterFeatureType
				+CR + "ORDER BY timestamp DESC LIMIT 0,1";
		List<Map<String, Object>> rows = dbConnection.executeSelect(sqlQuery);
		if(rows.size() > 0) {
			Map<String, Object> row = rows.get(0);
			DeviceMeasure result = new DeviceMeasure();
			Date date = (Date) row.get("timestamp3");
			result.setDatetime(date);
			String deviceName = "" + row.get("device_name");
			result.setDeviceName(deviceName);
			return result;
		}
		return null;
	}


	public static List<DeviceMeasure> retrieveDevicesMeasures(
			List<DeviceCategory> categoryFilter
			, String featureTypeFilter
			, Date dateBegin
			, Date dateEnd) throws Exception {
		// Map<Date, List<DeviceMeasure>>  result = new HashMap<Date, List<DeviceMeasure>>();
		List<DeviceMeasure>  result = new ArrayList<DeviceMeasure>();
		String sCategoryFilter = SapereUtil.generateFilterCategory(categoryFilter, "sensor_input.device_category");
		String filterDateBegin = dateBegin==null? "1" : "measure_record.timeStamp >=" + addSingleQuotes(UtilDates.format_sql.format(dateBegin));
		String filterEndDate = dateEnd==null? "1" :   "measure_record.timeStamp <" + addSingleQuotes(UtilDates.format_sql.format(dateEnd));
		String sqlFilterFeatureType = featureTypeFilter==null? "1": "feature_type=" + addSingleQuotes(featureTypeFilter);
		String sqlQuery = "SELECT distinct measure_record.timestamp"
			//+CR +",DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:59') AS timestamp2 "
			+CR + ",TIMESTAMPADD(MINUTE,"
			+CR + "    TIMESTAMPDIFF(MINUTE,DATE(timestamp),timestamp),"
			+CR + "    DATE(timestamp)) AS timestamp3"
			+CR	+",sensor.serial_number AS sensor_number"
			+CR	+",sensor.location"
			+CR	+",(SELECT device.category FROM device WHERE device.id = sensor_input.id_device) AS device_category"
			+CR	+",(SELECT device.name FROM device WHERE device.id = sensor_input.id_device) AS device_name"
			+CR	+",phase_mr.phase"
			+CR	+",ABS(phase_mr.power_p) AS power_p"
			+CR	+",ABS(phase_mr.power_q) AS power_q"
			+CR	+",ABS(phase_mr.power_s) AS power_s"
			+CR	+"FROM measure_record"
			//+CR	+"JOIN sensor on sensor.serial_number = measure_record.sensor_number"
			+CR	+"JOIN sensor on sensor.id = measure_record.id_sensor"
			+CR	+"JOIN phase_measure_record AS phase_mr ON phase_mr.id_measure_record = measure_record.id"
			+CR	+"JOIN sensor_input ON  sensor_input.id_sensor = sensor.id AND sensor_input.phase=phase_mr.phase"
			+CR	+"WHERE  " + filterDateBegin + " AND " + filterEndDate + " AND " + sCategoryFilter + " AND " + sqlFilterFeatureType + " AND NOT sensor_input.is_disabled"
			+CR + "	ORDER BY measure_record.timestamp";
		List<Map<String, Object>> rows = dbConnection.executeSelect(sqlQuery);
		logger.info("retrieveDevicesMeasures : sqlQuery = " + sqlQuery);
		Date lastDate = null;
		Map<String, DeviceMeasure> mapMeasures = new HashMap<String,DeviceMeasure>();
		for(Map<String, Object> row: rows) {
			String deviceName = "" + row.get("device_name");
			Date date = (Date) row.get("timestamp3");
			if(lastDate==null || lastDate.getTime()!=date.getTime()) {
				for(DeviceMeasure measure : mapMeasures.values()) {
					result.add(measure);
				}
				mapMeasures = new HashMap<String,DeviceMeasure>();
			}
			String sPhase = "" + row.get("phase");
			PhaseNumber phaseNumber = PhaseNumber.valueOf(sPhase);
			Double power_p = SapereUtil.getDoubleValue(row, "power_p", logger);
			Double power_q = SapereUtil.getDoubleValue(row, "power_q", logger);
			Double power_s = SapereUtil.getDoubleValue(row, "power_s", logger);
			if(!mapMeasures.containsKey(deviceName)) {
				DeviceMeasure nextMeasure = new DeviceMeasure(date, deviceName, phaseNumber, power_p, power_q, power_s);
				mapMeasures.put(deviceName, nextMeasure);
			} else {
				DeviceMeasure nextMeasure = mapMeasures.get(deviceName);
				nextMeasure.addPhaseMeasures(phaseNumber, power_p, power_q, power_s);
			}
			lastDate = date;
			/*
			if(!result.containsKey(date)) {
				result.put(date, new ArrayList<DeviceMeasure>());
			}
			List<DeviceMeasure> listMeasures = result.get(date);
			listMeasures.add(nextMeasure);
			*/
			//result.add(nextMeasure);
		}
		for(DeviceMeasure measure : mapMeasures.values()) {
			result.add(measure);
		}
		return result;
	}

	public static long refreshTmpMeasureData(Date dateCurrent, int nbOfDays,  Map<String, String> nodeByLocation , String defaultNode) throws Exception {
		Date dateBegin = UtilDates.shiftDateMinutes(
					UtilDates.shiftDateDays(dateCurrent, -1*nbOfDays)
				, -60);
		String sqlDateBegin = addSingleQuotes(UtilDates.format_sql.format(dateBegin));
		String sqlDateEnd =  addSingleQuotes(UtilDates.format_sql.format(dateCurrent));
		String reqSeparator2 = CR + dbConnection.getReqSeparator2();
		StringBuffer sqlRequest1 = new StringBuffer();
		sqlRequest1.append("TRUNCATE device_measure")
			.append(reqSeparator2);
		sqlRequest1.append(CR).append("INSERT INTO device_measure(timestamp3,id_device, power_p)")
			.append(CR).append("	SELECT ")
		//+ CR + "			-- // Set seconds = 0 to timestamp3"
			.append(CR).append("		DATE_add(DATE(timestamp), interval (ROUND(TIME_TO_SEC(timestamp)/60)) * 60 SECOND) AS timestamp3")
			.append(CR).append("		,id_device")
		//	.append(CR).append("		,timestamp")
			.append(CR).append("		,SUM(ABS(phase_mr.power_p))  AS power_p")
			.append(CR).append( "		FROM phase_measure_record as phase_mr")
			.append(CR).append("		JOIN measure_record ON phase_mr.id_measure_record = measure_record.id")
			.append(CR).append("		JOIN sensor_input ON sensor_input.id_sensor = measure_record.id_sensor AND sensor_input.phase=phase_mr.phase")
			.append(CR).append("		WHERE 1 -- feature_type ='MN' -- AND sensor.location = 'Parascolaire'")
			.append(CR).append("				AND timestamp > " + sqlDateBegin + "")
			.append(CR).append("				AND timestamp < " + sqlDateEnd + "")
			.append(CR).append("				AND NOT sensor_input.is_disabled")
			.append(CR).append("		GROUP BY timestamp3, id_device")
		;
		sqlRequest1.append(reqSeparator2);
		sqlRequest1.append(CR).append("DROP TEMPORARY TABLE IF EXISTS Tmp_ToComplete");
		sqlRequest1.append(reqSeparator2);
		sqlRequest1.append(CR).append("CREATE TEMPORARY TABLE Tmp_ToComplete")
			.append(CR).append("	SELECT timestamp3, MAX(device.is_producer) AS has_producer ")
			.append(CR).append("	, DATE_ADD(timestamp3, interval -(MINUTE(timestamp3) % 15) MINUTE) AS prod_timestamp")
			.append(CR).append( "	FROM device_measure")
			.append(CR).append( "	JOIN device ON device.id = device_measure.id_device")
			.append(CR).append( "	GROUP BY timestamp3")
			.append(CR).append( "	HAVING has_producer = 0");
		sqlRequest1.append(reqSeparator2);
		sqlRequest1.append(CR).append("ALTER TABLE Tmp_ToComplete ADD key _timestamp3(timestamp3)");
		sqlRequest1.append(reqSeparator2);
		sqlRequest1.append(CR).append("INSERT INTO device_measure(timestamp3, id_device, power_p, added)")
			.append(CR).append("	SELECT Tmp_ToComplete.timestamp3,  id_device, power_p, 1")
			.append(CR).append("	FROM Tmp_ToComplete")
			.append(CR).append("	JOIN device_measure ON device_measure.timestamp3 = Tmp_ToComplete.prod_timestamp")
			.append(CR).append("	WHERE device_measure.id_device IN (SELECT id FROM device WHERE is_producer)")
		;
		sqlRequest1.append(reqSeparator2);
		sqlRequest1.append(CR).append("INSERT INTO device_measure(timestamp3, id_device, power_p, added)")
			.append(CR).append( "SELECT timestamp3")
			.append(CR).append("	,(SELECT device.id FROM device where name = 'SIG')  AS id_device")
			.append(CR).append("	,(SELECT 120*ds.power FROM device_statistic ds WHERE ds.device_category = 'EXTERNAL_ENG' AND ds.start_hour=hour(device_measure.timestamp3)) AS power_p")
			.append(CR).append( "	,1 AS added")
			.append(CR).append("	FROM device_measure")
			.append(CR).append( "	GROUP BY timestamp3")
			;
			//.append(CR).append( 	"WHERE is_producer");
		sqlRequest1.append(reqSeparator2);
		sqlRequest1.append("UPDATE device_measure SET hour = HOUR(timestamp3), date = DATE(timestamp3)");
		sqlRequest1.append(reqSeparator2);
		// assign node numbers based on location:
		long result = dbConnection.execUpdate(sqlRequest1.toString());
		setNodeInMeasureData(dateCurrent, nbOfDays, nodeByLocation, defaultNode);
		return result;
	}

	public static long setNodeInMeasureData(Date dateCurrent, int nbOfDays, Map<String, String> nodeByLocation, String defaultNode) throws Exception {
		StringBuffer sqlRequest1= new StringBuffer();
		String reqSeparator2 = "";// dbConnection.getReqSeparator2() + CR;
		if(nodeByLocation.size() > 0) {
			sqlRequest1.append(reqSeparator2).append("UPDATE Device SET `Node` = CASE");
			for (String location : nodeByLocation.keySet()) {
				String node = nodeByLocation.get(location);
				sqlRequest1.append(reqSeparator2).append(CR).append("      WHEN location = " ).append(addSingleQuotes(location)).append(" THEN ").append(addSingleQuotes(node));
			}
			sqlRequest1.append(CR).append("      ELSE ''");
			sqlRequest1.append(CR).append("END");
		} else {
			sqlRequest1.append(reqSeparator2).append("UPDATE Device SET `Node` = ").append(addSingleQuotes(defaultNode));
		}
		sqlRequest1.append(CR).append("		WHERE living_lab = 'Vergers'")
		;
		long result = dbConnection.execUpdate(sqlRequest1.toString());
		return result;
	}

	public static Map<String, List<NodeTotal>> loadNodesHistory(Map<String, PredictionContext> mapPredictionContext,
			Map<Integer, Date> mapIterations) throws Exception {
		List<ClusterTotal> clusterHistory = loadClusterHistory(mapPredictionContext, mapIterations);
		Map<String, List<NodeTotal>> result = new HashMap<String, List<NodeTotal>>();
		for (ClusterTotal nextCluster : clusterHistory) {
			nextCluster.simulateTransactions();
			if (!nextCluster.checkUp()) {
				nextCluster.resetTransacions();
				nextCluster.simulateTransactions();
			}
			for (String node : nextCluster.getNodes()) {
				NodeTotal nodeTotal = nextCluster.generateNodeTotal(node);
				if (nodeTotal != null) {
					Long timeShiftMS = mapPredictionContext.get(node).getNodeContext().getTimeShiftMS();
					nodeTotal.setTimeShiftMS(timeShiftMS);
					if (!result.containsKey(node)) {
						result.put(node, new ArrayList<NodeTotal>());
					}
					List<NodeTotal> listNodeTotal = result.get(node);
					listNodeTotal.add(nodeTotal);
				}
			}
		}
		return result;
	}

	public static List<ClusterTotal> loadClusterHistory (
			 Map <String, PredictionContext> mapPredictionContext
			,Map<Integer, Date> mapIterations
			) throws Exception {
		List<ClusterTotal> result = new ArrayList<ClusterTotal>();
		String list_sql_date = "";
		String dateSep="";
		for(Integer nextIteration : mapIterations.keySet()) {
			list_sql_date+= dateSep;
			list_sql_date+= addSingleQuotes(UtilDates.format_sql_day.format(mapIterations.get(nextIteration)));
			dateSep = ",";
		}
		Map<String, Date> mapDateMax = new HashMap<String, Date>();
		for(String node : mapPredictionContext.keySet()) {
			PredictionContext predictionContext = mapPredictionContext.get(node);
			mapDateMax.put(node, predictionContext.getCurrentDate());
		}
		ClusterTotal nextClusterTotal = null;
		Date lastDate = null;
		String sqlRequest2 = "SELECT timestamp3, Node"
				+ CR + ", DAYOFYEAR(timestamp3) 				AS DayOfYear"
				+ CR + ", Count(DISTINCT id_device) 			AS count_devices"
				+ CR + ", GROUP_CONCAT(distinct serial_number) 	AS serial_number"
				+ CR + ", GROUP_CONCAT( power_p 	ORDER by 1*power_p SEPARATOR ',') AS list_power_p"
				+ CR + ", GROUP_CONCAT( Device.name ORDER by 1*power_p SEPARATOR ',') AS list_device_name"
				+ CR + ", GROUP_CONCAT( IF(Device.is_producer, '1', '0') ORDER BY 1*power_p SEPARATOR ',') AS list_is_producer"
				+ CR + ", Count(DISTINCT Device.serial_number) 		AS count_serial_number"
				+ CR + ", SUM(IF(is_producer,power_p,0)) 		AS power_p_produced"
				+ CR + ", SUM(IF(is_producer,0,power_p))		AS power_p_requested"
				+ CR + ", MAX(IF(is_producer,0,power_p))		AS power_p_max_requested"
				+ CR + ", Count(*) 								AS test_count"
				+ CR + ", MAX(is_producer) 						AS has_producer"
				+ CR + "FROM device_measure "
				+ CR + "JOIN Device ON device.id = device_measure.id_device "
				+ CR + "WHERE date IN ( " + list_sql_date + ")"
				// + " AND node = " + addSingleQuotes(node)
				+ CR + "GROUP BY timestamp3, Device.Node"
				+ CR + "	-- HAVING node = 'N2'"
				+ CR + "ORDER by timestamp3, Node";
		List<Map<String, Object>> rows = dbConnection.executeSelect(sqlRequest2);
		for(Map<String, Object> row: rows) {
			try {
				Date nextDate = SapereUtil.getDateValue(row, "timestamp3", logger);
			    //int nextDayOfYear = SapereUtil.getIntValue(row, "DayOfYear");
				if (nextClusterTotal == null) {
					nextClusterTotal = new ClusterTotal(nextDate);
				} else if (lastDate != null && nextDate.getTime() > lastDate.getTime()) {
					result.add(nextClusterTotal);
					nextClusterTotal = new ClusterTotal(nextDate);
				}
				String node = "" + row.get("Node");
				if(!mapPredictionContext.containsKey(node)) {
					//logger.error(node + " is empty or not referenced");
				} else {
					Date dateMax = mapDateMax.get(node);
					if(!nextDate.after(dateMax)) {
						Double requested = SapereUtil.getDoubleValue(row, "power_p_requested", logger);
						Double produced = SapereUtil.getDoubleValue(row, "power_p_produced", logger);
						List<String> list_is_producer = SapereUtil.getListStrValue(row, "list_is_producer");
						List<String> list_power_p = SapereUtil.getListStrValue(row, "list_power_p");
						List<String> list_deviceNames = SapereUtil.getListStrValue(row, "list_device_name");
						int idx1 = 0;
						for(String sPower_p : list_power_p) {
							String is_producer = list_is_producer.get(idx1);
							boolean bIsProducer = "1".equals(is_producer);
							double power_p = Double.valueOf(sPower_p);
							//String deviceName = "";//list_deviceNames.get(idx1);
							String deviceName = list_deviceNames.get(idx1);
							DeviceItem deviceItem = new DeviceItem(deviceName, power_p, node, bIsProducer);
							nextClusterTotal.addDeviceItem(deviceItem);
							/*
							if(!bIsProducer) {
								DeviceItem deviceItem = new DeviceItem(deviceName, power_p, node, bIsProducer);
								nextClusterTotal.addDeviceItem(deviceItem);
							}
							*/
							idx1++;
						}
						Double consumed = 0.0;
						Double missing = requested;
						Double avaialble = produced;
						Double provided = 0.0;
						nextClusterTotal.setRequested(node, requested);
						nextClusterTotal.setProduced(node, produced);
						nextClusterTotal.setConsumed(node, consumed);
						nextClusterTotal.setProvided(node, provided);
						nextClusterTotal.setMissing(node, missing);
						nextClusterTotal.setAvailable(node, avaialble);
					}
					lastDate = nextDate;
				}
			}
			catch(Throwable e) {
				logger.error("EXCEPTION " + e);
			}
		}
		return result;
	}

	public static void saveNodesTotal(Map<String, List<NodeTotal>> mapNodesTotal){
		String reqSeparator2 = CR + dbConnection.getReqSeparator2();
		StringBuffer request = new StringBuffer();
		request.append("DELETE FROM simulated_node_history");
		request.append(reqSeparator2);
		request.append("INSERT INTO simulated_node_history (date, node, requested, produced, consumed, provided, available, missing, is_simulation) VALUES ");
		String itemSep = "";
		for(String node : mapNodesTotal.keySet()) {
			List<NodeTotal> listNodeTotals = mapNodesTotal.get(node);
			for(NodeTotal nextTotal : listNodeTotals) {
				StringBuffer nextItem = new StringBuffer();
				nextItem.append("(")
					.append(addSingleQuotes(UtilDates.format_sql.format(nextTotal.getDate()))).append(",")
					.append(addSingleQuotes(node)).append(",")
					.append(nextTotal.getRequested()).append(",")
					.append(nextTotal.getProduced()).append(",")
					.append(nextTotal.getConsumed()).append(",")
					.append(nextTotal.getProvided()).append(",")
					.append(nextTotal.getAvailable()).append(",")
					.append(nextTotal.getMissing()).append(",")
					.append("1)");
				request.append(CR).append(itemSep).append(nextItem);
				itemSep = ",";
			}
		}
		try {
			long result = dbConnection.execUpdate(request.toString());
		} catch (HandlingException e) {
			logger.error(e);
		}
	}
	/*
	public static Map<String, Map<FeaturesKey, NodeTransitionMatrices>> generateTransitionMatrices(
			 Map <String, PredictionContext> mapPredictionContext
			,Map<String, List<NodeTransitionMatrices>> mapExistingNodeTransitionMatrices
			,Map<String, List<NodeTotal>> historyByNode) {
		Map<String, Map<FeaturesKey, NodeTransitionMatrices>> mapNodeTransitionMatrices = new HashMap<String, Map<FeaturesKey, NodeTransitionMatrices>>();
		Map<String, NodeStatesTransitions> mapNodeStateTransitions = new HashMap<String, NodeStatesTransitions>();
		for(String node : mapPredictionContext.keySet() ) {
			PredictionContext predictionContext = mapPredictionContext.get(node);
			if(predictionContext == null) {
				logger.error("predictionContext is null for node " + node);
			}
			//NodeContext nodeContext = predictionContext.getNodeContext();
			NodeStatesTransitions nodeStateTransitions = new NodeStatesTransitions(new Date());
			mapNodeStateTransitions.put(node, nodeStateTransitions);
			if(!mapNodeTransitionMatrices.containsKey(node)) {
				mapNodeTransitionMatrices.put(node, new HashMap<FeaturesKey, NodeTransitionMatrices>());
			}
			if(mapExistingNodeTransitionMatrices.containsKey(node)) {
				List<NodeTransitionMatrices> listExistingNodeTransitionMatrices = mapExistingNodeTransitionMatrices.get(node);
				for(NodeTransitionMatrices nextNodeTrMatrices : listExistingNodeTransitionMatrices) {
					Map<FeaturesKey, NodeTransitionMatrices> mapNodeTransitionMatrices2 = mapNodeTransitionMatrices.get(node);
					mapNodeTransitionMatrices2.put(nextNodeTrMatrices.getFeaturesKey(), nextNodeTrMatrices);
				}
			}
		}
		//Date lastDate = null;
		Integer iterationNumber = 1;
		for(String nextNode : historyByNode.keySet()) {
			Date lastDay = null;
			List<NodeTotal> nodeHistory = historyByNode.get(nextNode);
			for(NodeTotal nextNodeTotal: nodeHistory) {
				try {
					Date nextDate = nextNodeTotal.getDate();
					Date nextDay = UtilDates.removeTime(nextDate);
					if(lastDay!= null && nextDay.getTime() > lastDay.getTime()) {
						iterationNumber++;
					}
					// Update result
					PredictionContext predictionContext = mapPredictionContext.get(nextNode);
					NodeStatesTransitions nodeStateTransitions = mapNodeStateTransitions.get(nextNode);
					nodeStateTransitions.refreshTransitions(predictionContext, nextNodeTotal, true, logger);
					NodeContext nodeContext = predictionContext.getNodeContext();
					FeaturesKey featuresKey = predictionContext.getFeaturesKey2(nextDate);
					Map<FeaturesKey, NodeTransitionMatrices> mapNodeTransitionMatrices2 = mapNodeTransitionMatrices.get(nextNode);
					if(!mapNodeTransitionMatrices2.containsKey(featuresKey)) {
						NodeTransitionMatrices nodeTransitionMatrices = new NodeTransitionMatrices(predictionContext, featuresKey);
						mapNodeTransitionMatrices2.put(featuresKey, nodeTransitionMatrices);
					}
					NodeTransitionMatrices nodeTransitionMatrices = mapNodeTransitionMatrices2.get(featuresKey);
					//nodeTransitionMatrices.reset(predictionContext);
					boolean updateted = nodeTransitionMatrices.updateMatrices2(nextDate, iterationNumber, nodeStateTransitions, false, logger);
					// Clear all
					//mapNodeTotal.clear();
					lastDay = nextDay;
				}
				catch(Throwable e) {
					logger.error("EXCEPTION " + e);
				}
			}
		}
		return mapNodeTransitionMatrices;
	}*/

	public static String addSingleQuotes(String str) {
		return SapereUtil.addSingleQuotes(str);
	}

	public static Map<DeviceCategory, Double> retrieveDeviceStatistics(
			double powerCoeffConsumer
			, double powerCoeffProducer
			, List<DeviceCategory> categoryFilter
			, int hourOfDay) throws Exception {
		Map<DeviceCategory, Double> result = new HashMap<DeviceCategory, Double>();
		String sCategoryFilter = SapereUtil.generateFilterCategory(categoryFilter, "device_category");
		List<Map<String, Object>> rows = dbConnection.executeSelect("SELECT * FROM device_statistic WHERE start_hour = " + hourOfDay + " AND " + sCategoryFilter);
		for(Map<String, Object> row: rows) {
			String sCategory = "" + row.get("device_category");
			DeviceCategory category = DeviceCategory.valueOf(sCategory);
			boolean isProducer = category.isProducer();
			double powerCoeff = isProducer? powerCoeffProducer : powerCoeffConsumer;
			double power = powerCoeff*SapereUtil.getDoubleValue(row, "power", logger);
			result.put(category, power);
		}
		return result;
	}

	public static void resetSimulatorLogs() throws Exception {
		logger.info("resetSimulatorLogs ");
		dbConnection.execUpdate("DELETE simulator_log FROM simulator_log");
	}

	public static void registerSimulatorLog(SimulatorLog simulatorLog) throws Exception {
		logger.info("registerSimulatorLog " + simulatorLog);
		String sessionId = getSessionNumber();
		StringBuffer query = new StringBuffer();
		query.append("INSERT INTO simulator_log SET ")
			.append(CR).append(" id_session = ").append(addSingleQuotes(sessionId))
			.append(CR).append(",device_category = ").append(addSingleQuotes(simulatorLog.getDeviceCategory().name()))
			.append(CR).append(",loop_Number = ").append(addSingleQuotes(""+simulatorLog.getLoopNumber()))
			.append(CR).append(",power_target = ").append(addSingleQuotes(""+simulatorLog.getPowerTarget()))
			.append(CR).append(",power_target_min = ").append(addSingleQuotes(""+""+simulatorLog.getPowerTargetMin()))
			.append(CR).append(",power_target_max = ").append(addSingleQuotes(""+simulatorLog.getPowerTargetMax()))
			.append(CR).append(",power = ").append(addSingleQuotes(""+simulatorLog.getPower()))
			.append(CR).append(",is_reached = ").append(simulatorLog.isReached()?1:0)
			.append(CR).append(",nb_started = ").append(addSingleQuotes(""+simulatorLog.getNbStarted()))
			.append(CR).append(",nb_modified = ").append(addSingleQuotes(""+simulatorLog.getNbModified()))
			.append(CR).append(",nb_stopped = ").append(addSingleQuotes(""+simulatorLog.getNbStopped()))
			.append(CR).append(",nb_devices = ").append(addSingleQuotes(""+simulatorLog.getNbDevices()))
			.append(CR).append(",target_device_combination_found = ").append(simulatorLog.isTargetDeviceCombinationFound())
		;
		dbConnection.execUpdate(query.toString());
	}

	public static List<Device> retrieveNodeDevices(double powerCoeffProducer, double powerCoeffConsumer) throws Exception {
		List<Device> result = new ArrayList<>();
		List<Map<String, Object>> rows = dbConnection.executeSelect("SELECT * FROM device WHERE living_lab=''");
		for(Map<String, Object> row: rows) {
			Long id = SapereUtil.getLongValue(row, "id");
			Boolean isProducer = (Boolean) row.get("is_producer");
			Double powerCoeff = isProducer.booleanValue() ?  powerCoeffProducer : powerCoeffConsumer;
			Double powerMin = powerCoeff*SapereUtil.getDoubleValue(row, "power_min", logger);
			Double powerMax = powerCoeff*SapereUtil.getDoubleValue(row, "power_max", logger);
			Double avergaeDuration =  SapereUtil.getDoubleValue(row, "avg_duration", logger);
			DeviceCategory deviceCategory = SapereUtil.getDeviceCategoryValue(row, "category");
			EnvironmentalImpact envImpact = SapereUtil.getEnvironmentalImpactValue(row,  "environmental_impact");
			String deviceName = ""+row.get("name");
			DeviceProperties nextDeviceProperties = new DeviceProperties(deviceName, deviceCategory, envImpact);
			nextDeviceProperties.setPriorityLevel(SapereUtil.getPriorityLevelValue(row, "priority_level"));
			//nextDeviceProperties.setProducer(isProducer);
			Device nextDevice = new Device(id, nextDeviceProperties, powerMin, powerMax, avergaeDuration);
			result.add(nextDevice);
		}
		return result;
	}
}
