package com.sapereapi.db;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.sapereapi.exception.DoublonException;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.Session;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.TimestampedValue;
import com.sapereapi.model.energy.CompositeOffer;
import com.sapereapi.model.energy.Contract;
import com.sapereapi.model.energy.DeviceProperties;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.ExtendedEnergyEvent;
import com.sapereapi.model.energy.MissingRequest;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.ProsumerItem;
import com.sapereapi.model.energy.ProsumerProperties;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.model.energy.input.NodeHistoryFilter;
import com.sapereapi.model.energy.input.OfferFilter;
import com.sapereapi.model.energy.node.ExtendedNodeTotal;
import com.sapereapi.model.energy.node.NodeContent;
import com.sapereapi.model.energy.node.NodeTotal;
import com.sapereapi.model.energy.pricing.DiscountItem;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.EventMainCategory;
import com.sapereapi.model.referential.EventObjectType;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.model.referential.WarningType;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.node.NodeLocation;
import eu.sapere.middleware.node.NodeManager;

public class EnergyDbHelper {
	private static DBConnection dbConnection = null;
	// private static DBConnection dbConnectionClemapData = null;
	private static EnergyDbHelper instance = null;
	public final static String CR = System.getProperty("line.separator"); // Cariage return
	private static int debugLevel = 0;
	private static SapereLogger logger = SapereLogger.getInstance();
	// public final static String CLEMAPDATA_DBNAME = "clemap_data_light";
	private static Map<String, NodeLocation> cashNodeLocations = new HashMap<>();
	private static boolean sqlite = false;
	//private static String OP_DATETIME = null;
	private static String OP_CURRENT_DATETIME = null;
	private static String OP_TEMPORARY = null;
	private static String OP_GREATEST = null;
	private static String OP_LEAST = null;
	private static String OP_IF = null;

	public static void init() {
		// initialise db connection
		instance = new EnergyDbHelper();
	}

	public static EnergyDbHelper getInstance() {
		if (instance == null) {
			instance = new EnergyDbHelper();
		}
		return instance;
	}

	public static Map<String, NodeLocation> getCashNodeLocation2() {
		return cashNodeLocations;
	}

	public EnergyDbHelper() {
		// initialise db connection
		dbConnection = DBConnectionFactory.getInstance(Sapere.DB_SERVER);
		// dbConnectionClemapData = new
		// DBConnection("jdbc:mariadb://129.194.10.168/clemap_data", "import_clemap",
		// "sql2537");
		sqlite = dbConnection.useSQLLite();
		//OP_DATETIME = DBConnection.getOP_DATETIME();
		OP_CURRENT_DATETIME = dbConnection.getOP_CURRENT_DATETIME();
		OP_TEMPORARY = dbConnection.getOP_TEMPORARY();
		OP_GREATEST = dbConnection.getOP_GREATEST();
		OP_LEAST = dbConnection.getOP_LEAST();
		OP_IF = dbConnection.getOP_IF();
	}

	public static Long getSessionId() {
		return SessionManager.getSessionId();
	}

	public static DBConnection getDbConnection() {
		return dbConnection;
	}

	public static void checkNodeLocations(Collection<NodeLocation> listNodeLocation) throws HandlingException {
		for(NodeLocation nodeLocation : listNodeLocation) {
			String name = nodeLocation.getName();
			if(cashNodeLocations.containsKey( name)) {
				registerNodeLocation(nodeLocation);
			}
		}
	}

	public static void checkNodeLocation(NodeLocation nodeLocation) throws HandlingException {
		String name = nodeLocation.getName();
		if(!cashNodeLocations.containsKey( name)) {
			logger.info("checkNodeLocation : " + name + " : call registerNodeLocation");
			registerNodeLocation(nodeLocation);
		}
	}

	public static NodeLocation registerNodeLocation(NodeLocation nodeLocation) throws HandlingException {
		Map<String, String> affectation = new HashMap<>();
		affectation.put("name", addSingleQuotes(nodeLocation.getName()));
		affectation.put("host", addSingleQuotes(nodeLocation.getHost()));
		affectation.put("main_port", addQuotes(nodeLocation.getMainPort()));
		affectation.put("rest_port", addQuotes(nodeLocation.getRestPort()));
		Map<String, String> confictAffectation = new HashMap<>();
		confictAffectation.put("creation_date", "creation_date");
		logger.info("registerNodeLocation : " + nodeLocation);
		String queryInsert = dbConnection.generateInsertQuery("node_location", affectation, confictAffectation);
		Long id = dbConnection.execUpdate(queryInsert);
		logger.info("registerNodeLocation : id of new config = " + id);
		StringBuffer sqlReq = new StringBuffer();
		sqlReq.append("SELECT * FROM node_location WHERE ").append("name = ")
				.append(addSingleQuotes(nodeLocation.getName())).append(" AND host = ")
				.append(addSingleQuotes(nodeLocation.getHost())).append(" AND main_port = ")
				.append(nodeLocation.getMainPort());
		Map<String, Object> row = dbConnection.executeSingleRowSelect(sqlReq.toString());
		NodeLocation result = aux_retrieveNodeLocation(row);
		return result;
	}

	private static NodeLocation aux_retrieveNodeLocation(Map<String, Object> row) {
		if (row == null) {
			return null;
		}
		NodeLocation result = new NodeLocation();
		result.setHost("" + row.get("host"));
		result.setName("" + row.get("name"));
		result.setMainPort(SapereUtil.getIntValue(row, "main_port"));
		result.setRestPort(SapereUtil.getIntValue(row, "rest_port"));
		updateCash(result);
		return result;
	}

	private static void updateCash(NodeLocation nodeLocation) {
		if (nodeLocation != null) {
			String name = nodeLocation.getName();
			if (!cashNodeLocations.containsKey(name)) {
				cashNodeLocations.put(name, nodeLocation);
			}
		}
	}

	public static NodeLocation retrieveNodeLocationByName(String name) throws HandlingException {
		if (cashNodeLocations.containsKey(name)) {
			return cashNodeLocations.get(name);
		}
		Map<String, Object> row = dbConnection
				.executeSingleRowSelect("SELECT * FROM node_location WHERE name = " + addSingleQuotes(name));
		NodeLocation result = aux_retrieveNodeLocation(row);
		return result;
	}

	public static List<NodeLocation> retrieveAllNodeLocations(List<String> namesToExclude) throws HandlingException {
		List<NodeLocation> result = new ArrayList<>();
		StringBuffer filter = new StringBuffer();// filter = "1";
		if (namesToExclude.size() > 0) {
			filter.append("name NOT IN (");
			String sep = "";
			for (String nextName : namesToExclude) {
				filter.append(sep).append(addSingleQuotes(nextName));
				sep = ",";
			}
			filter.append(")");
		} else {
			filter.append("1");
		}
		List<Map<String, Object>> rows = dbConnection
				.executeSelect("SELECT * FROM node_location WHERE " + filter.toString());
		for (Map<String, Object> nextRow : rows) {
			NodeLocation nodeLocation = aux_retrieveNodeLocation(nextRow);
			result.add(nodeLocation);
		}
		return result;
	}

	public static List<NodeLocation> retrieveNeighbourNodeLocations(NodeContext nodeContext) throws HandlingException {
		Long nodeContextId = nodeContext.getId();
		List<NodeLocation> result = new ArrayList<>();
		List<Map<String, Object>> rows = dbConnection.executeSelect("SELECT * FROM context_neighbour "
				+ CR+ " JOIN node_location ON node_location.name = context_neighbour.node"
				+ CR+ " WHERE context_neighbour.id_node_context = " + nodeContextId);
		for (Map<String, Object> nextRow : rows) {
			NodeLocation nodeLocation = aux_retrieveNodeLocation(nextRow);
			result.add(nodeLocation);
		}
		return result;
	}

	public static NodeContext updateNeighbours(NodeContext nodeContext, List<String> listNeighbourNodes) throws HandlingException {
		Long nodeContextId = nodeContext.getId();
		StringBuffer query = new StringBuffer();
		query.append("DELETE ").append(sqlite ? "" : " context_neighbour")
				.append(" FROM context_neighbour WHERE id_node_context = ").append(nodeContextId).append("");
		query.append(dbConnection.getReqSeparator2());
		if (listNeighbourNodes.size() > 0) {
			query.append(CR).append("INSERT INTO context_neighbour(id_node_context, node) VALUES").append(CR);
			String sep = "";
			for (String nextNode : listNeighbourNodes) {
				query.append(sep).append("(").append(nodeContextId).append(",").append(addSingleQuotes(nextNode)).append(")");
				sep = ",";
			}
		}
		Long result = dbConnection.execUpdate(query.toString());
		logger.info("updateNeighbours : result = " + result);
		List<NodeLocation> neighbourConfig = retrieveNeighbourNodeLocations(nodeContext);
		nodeContext.resetNeighbourNodeLocations();
		for (NodeLocation nodeLocation : neighbourConfig) {
			nodeContext.addNeighbourNodeLocation(nodeLocation);
		}
		return nodeContext;
	}

	public static NodeContext registerNodeContext(NodeContext nodeContext, List<NodeLocation> listNeighbourNodeLocation) throws HandlingException {
		Map<String, String> affectation = new HashMap<>();
		String node = nodeContext.getNodeLocation().getName();
		Long sessionId = nodeContext.getSession().getId();
		affectation.put("node", addSingleQuotes(node));
		affectation.put("scenario", addSingleQuotes(nodeContext.getScenario()));
		affectation.put("learning_agent", addSingleQuotes(nodeContext.getLearningAgentName()));
		affectation.put("regulator_agent", addSingleQuotes(nodeContext.getRegulatorAgentName()));
		affectation.put("last_id_session", addQuotes(sessionId));
		affectation.put("last_time_shift_ms", "" + nodeContext.getTimeShiftMS());
		Map<String, String> confilctAffectation = new HashMap<>();
		confilctAffectation.put("last_id_session", addQuotes(nodeContext.getSession().getId()));
		confilctAffectation.put("last_time_shift_ms", "" + nodeContext.getTimeShiftMS());
		confilctAffectation.put("node", addSingleQuotes(nodeContext.getNodeLocation().getName()));
		// TODO : use query1
		String query1 = dbConnection.generateInsertQuery("node_context", affectation, confilctAffectation);
		long result1 = dbConnection.execUpdate(query1);
		logger.info("registerNodeContext : nodeContextId(1) = " + result1);
		if(result1 == 0) {
			logger.error("registerNodeContext : returned id of created node_context is 0");
		}
		Map<String, Object>  row = dbConnection.executeSingleRowSelect("SELECT id FROM node_context WHERE last_id_session = " + sessionId + " AND node = " + addSingleQuotes(node) + " AND scenario = " + addSingleQuotes(nodeContext.getScenario()));
		long nodeContextId = SapereUtil.getLongValue(row, "id");
		logger.info("registerNodeContext : nodeContextId(2) = " + nodeContextId);
		nodeContext.setId(nodeContextId);
		// Add link to the neightbour node configs
		if (listNeighbourNodeLocation != null) {
			List<String> listNeighbourNodes = new ArrayList<String>();
			for (NodeLocation nextNodeLocation : listNeighbourNodeLocation) {
				nextNodeLocation = EnergyDbHelper.registerNodeLocation(nextNodeLocation);
				listNeighbourNodes.add(nextNodeLocation.getName());
			}
			nodeContext = EnergyDbHelper.updateNeighbours(nodeContext, listNeighbourNodes);
		}
		// add links to the neighbour nodes
		nodeContext.resetNeighbourNodeLocations();
		List<NodeLocation> neighbourConfig = retrieveNeighbourNodeLocations(nodeContext);
		for (NodeLocation nodeLocation : neighbourConfig) {
			nodeContext.addNeighbourNodeLocation(nodeLocation);
		}
		return nodeContext;
	}

	public static EnergyEvent registerEvent(EnergyEvent event, String log) throws HandlingException {
		return registerEvent(event, null, log);
	}

	private static EnergyEvent auxGetEvent(Map<String, Object> row) throws HandlingException {
		String agent = "" + row.get("agent_name");
		String sCategory = "" + row.get("device_category");
		DeviceCategory category = DeviceCategory.getByName(sCategory);
		EnvironmentalImpact envImpact = SapereUtil.getEnvironmentalImpactValue(row, "environmental_impact");
		Double power = SapereUtil.getDoubleValue(row, "power", logger);
		Double powerMin = SapereUtil.getDoubleValue(row, "power_min", logger);
		Double powerMax = SapereUtil.getDoubleValue(row, "power_max", logger);
		String comment = "" + row.get("comment");
		long timeShiftMS = SapereUtil.getLongValue(row, "time_shift_ms");
		String node = "" + row.get("node");
		NodeLocation nodeLocation = retrieveNodeLocationByName(node);
		int issuerDistance = SapereUtil.getIntValue(row, "distance");
		boolean isComplementary = SapereUtil.getBooleantValue(row, "is_complementary");
		//boolean isProducer = EventObjectType.PRODUCTION.equals(type.getObjectType());
		String sObjectType  = "" + row.get("object_type");
		EventObjectType objectType = EventObjectType.valueOf(sObjectType);
		String sMainCategory = "" + row.get("main_category");
		EventMainCategory mainCategory = EventMainCategory.valueOf(sMainCategory);
		EventType type = EventType.retrieve(objectType, mainCategory);
		DeviceProperties deviceProperties = new DeviceProperties("" + row.get("device_name"), category, envImpact);
		ProsumerProperties issuerProperties = new ProsumerProperties(agent, nodeLocation, issuerDistance, timeShiftMS, deviceProperties);
		double firstRate = SapereUtil.getDoubleValue(row, "first_rate", logger);
		if(Double.isNaN(firstRate)) {
			firstRate = 0;
		}
		//firstRate = 0;
		PowerSlot powerSlot = new PowerSlot(power, powerMin, powerMax);
		EnergyEvent result = new EnergyEvent(type, issuerProperties, isComplementary, powerSlot,
				SapereUtil.getDateValue(row, "begin_date", logger),
				SapereUtil.getDateValue(row, "expiry_date", logger), comment, firstRate);
		result.setId(SapereUtil.getLongValue(row, "id"));
		Double powerUpdate = SapereUtil.getDoubleValue(row, "power_update", logger);
		Double powerMinUpdate = SapereUtil.getDoubleValue(row, "power_min_update", logger);
		Double powerMaxUpdate = SapereUtil.getDoubleValue(row, "power_max_update", logger);
		result.setPowerUpates(powerUpdate, powerMinUpdate, powerMaxUpdate);
		result.setHistoId(SapereUtil.getLongValue(row, "id_history"));
		if (row.get("warning_type") != null) {
			String sWarningType = "" + row.get("warning_type");
			if(sWarningType.length() > 0) {
				result.setWarningType(WarningType.valueOf(sWarningType));
			}
		}
		// result.setIsComplementary();
		return result;
	}

	public static List<ExtendedEnergyEvent> retrieveLastSessionEvents(Date currentDateFilter) throws HandlingException {
		List<ExtendedEnergyEvent> result = new ArrayList<ExtendedEnergyEvent>();
		Map<String, Object> row = dbConnection
				.executeSingleRowSelect("SELECT id_session FROM event ORDER BY ID DESC LIMIT 0,1)");
		if (row != null) {
			Long sessionId = SapereUtil.getLongValue(row, "id_session");
			return retrieveSessionEvents(sessionId, currentDateFilter, null);
		}
		return result;
	}

	public static List<ExtendedEnergyEvent> retrieveCurrentSessionEvents(Date currentDateFilter) throws HandlingException {
		Long sessionId = getSessionId();
		return retrieveSessionEvents(sessionId, currentDateFilter, null);
	}

	public static List<ExtendedEnergyEvent> retrieveSessionEvents(Long sessionId, Date currentDateFilter, String agentFilter) throws HandlingException {
		List<ExtendedEnergyEvent> result = new ArrayList<ExtendedEnergyEvent>();
		StringBuffer query = new StringBuffer("");
		query.append(CR).append("SELECT event.*")
				// effective_end_date")
				.append(CR).append(" ,").append(OP_LEAST).append("(event.expiry_date")
				.append(CR).append(" , IFNULL(event.interruption_date,'3000-01-01')")
				.append(CR).append(" , IFNULL(event.cancel_date,'3000-01-01')) AS  effective_end_date")
				.append(CR).append(" FROM event")
				.append(CR).append(" WHERE event.id_session = ").append(sessionId);
		if (currentDateFilter != null) {
			String sqlCurrentTime = addSingleQuotes(UtilDates.format_sql.format(currentDateFilter));
			query.append(CR).append(" AND expiry_date > ").append(sqlCurrentTime)
				 .append(CR).append(" AND IFNULL(interruption_date,'3000-01-01')  > ").append(sqlCurrentTime)
				 .append(CR).append(" AND IFNULL(cancel_date,'3000-01-01')  > ").append(sqlCurrentTime);
		}
		if(agentFilter != null && !agentFilter.equals("")) {
			query.append(CR).append(" AND agent_name = ").append(addSingleQuotes(agentFilter));
		}
		query.append(CR).append(" ORDER BY id_history, event.agent_name, event.creation_time ");
		dbConnection.setDebugLevel(0);
		List<Map<String, Object>> rows = dbConnection.executeSelect(query.toString());
		dbConnection.setDebugLevel(0);
		Map<String, ExtendedEnergyEvent> mapContractEvents = new HashMap<String, ExtendedEnergyEvent>();
		for (Map<String, Object> row : rows) {
			EnergyEvent nextEvent = auxGetEvent(row);
			ProsumerProperties nextEventIssuer = nextEvent.getIssuerProperties();
			ExtendedEnergyEvent nextEvent2 = new ExtendedEnergyEvent(nextEvent.getType(), nextEventIssuer,
					nextEvent.isComplementary(), nextEvent.getPowerSlot(), nextEvent.getBeginDate(),
					nextEvent.getEndDate(), nextEvent.getComment(), nextEvent.getFirstRate());
			nextEvent2.setEffectiveEndDate(SapereUtil.getDateValue(row, "effective_end_date", logger));
			nextEvent2.setHistoId(nextEvent.getHistoId());
			nextEvent2.setWarningType(nextEvent.getWarningType());
			nextEvent2.setPowerUpdateSlot(nextEvent.getPowerUpdateSlot());
			result.add(nextEvent2);
			EventType evtType = nextEvent2.getType();
			if(EventObjectType.CONTRACT.equals(evtType.getObjectType()) && !evtType.getIsEnding()) {
				mapContractEvents.put(""+nextEvent.getId(), nextEvent2);
			}
		}
		if(mapContractEvents.size() > 0) {
			String slistContractEventId = String.join(",", mapContractEvents.keySet());
			StringBuffer query2 = new StringBuffer("SELECT lea.*")
					.append(CR).append("  ,(SELECT agent.node FROM agent WHERE agent.id_session = lea.id_session AND agent.name = lea.agent_name) AS agent_node")
					.append(CR).append("  FROM event ")
					.append(CR).append("  JOIN link_event_agent lea ON event.id  = lea.id_event")
					.append(CR).append("  WHERE event.ID IN (").append(slistContractEventId).append(")");
			List<Map<String, Object>> rows2 = dbConnection.executeSelect(query2.toString());
			for (Map<String, Object> row : rows2) {
				String idEvent = "" + row.get("id_event");
				if (mapContractEvents.containsKey(idEvent)) {
					ExtendedEnergyEvent evtToUpdate = mapContractEvents.get(idEvent);
					double power = SapereUtil.getDoubleValue(row, "power", logger);
					double powerMin = SapereUtil.getDoubleValue(row, "power_min", logger);
					double powerMax = SapereUtil.getDoubleValue(row, "power_max", logger);
					PowerSlot powerSlot = new PowerSlot(power, powerMin, powerMax);
					String sRole = "" + row.get("agent_role");
					ProsumerRole role = ProsumerRole.valueOf(sRole);
					if (role != null) {
						String node = "" + row.get("agent_node");
						NodeLocation nodeLocation = retrieveNodeLocationByName(node);
						String agentName = "" + row.get("agent_name");
						ProsumerItem prosumerItem = new ProsumerItem(agentName, role, powerSlot, nodeLocation);
						if (ProsumerRole.CONSUMER.equals(role)) {
							evtToUpdate.setLinkedConsumer(prosumerItem);
						} else if (ProsumerRole.PRODUCER.equals(role)) {
							evtToUpdate.addProvider(prosumerItem);
						}
					}
				}
			}
		}
		return result;
	}

	public static Map<String, Double> retrieveProposedPower(Date currentDateFilter) throws HandlingException {
		StringBuffer query = new StringBuffer();
		Long sessionId = getSessionId();
		String sqlCurrentTime = addSingleQuotes(UtilDates.format_sql.format(currentDateFilter));
		query.append("SELECT producer_agent, SUM(power) AS power")
			.append(CR).append(" , DATETIME('now', 'localtime') AS current_time")
			.append(CR).append(" , GROUP_CONCAT(consumer_agent) AS list_consumer_agent")
			.append(CR).append("FROM single_offer")
			.append(CR).append("WHERE id_session = ").append(sessionId)
			.append(CR).append("   AND date <= ").append(sqlCurrentTime)
			.append(CR).append("   AND DATETIME(deadline , '10 second')  >= ").append(sqlCurrentTime)
			.append(CR).append("   AND contract_event_id IS NULL")
			.append(CR).append("GROUP BY producer_agent ");
		List<Map<String, Object>> rows = dbConnection.executeSelect(query.toString());
		Map<String, Double> result = new HashMap<String, Double>();
		for (Map<String, Object> row : rows) {
			double power = SapereUtil.getDoubleValue(row, "power", logger);
			String producer = ""+row.get("producer_agent");
			result.put(producer, power);
		}
		return result;
	}

	public static Map<String, Map<String, Map<Long, Double>>> retrieveContractResponseTimes() throws HandlingException {
		// TODO : retrieve response time for each contract :
		StringBuffer query = new StringBuffer();
		String sqlProducerRole = addSingleQuotes(ProsumerRole.PRODUCER.name());
		String sqlConsumerRole = addSingleQuotes(ProsumerRole.CONSUMER.name());
		query.append("SELECT contract.id AS id_contract_event")
				.append(CR).append("	 ,link_consumer.agent_name AS consumer")
				.append(CR).append("	 ,(SELECT MAX(lhae2.warning_duration)")
				.append(CR).append("	 		FROM link_history_active_event lhae2")
				.append(CR).append("	 		WHERE lhae2.id_event = link_request.id_event) AS max_warning_duration")
				.append(CR).append("	 ,(SELECT GROUP_CONCAT(link_producer.agent_name) ")
				.append(CR).append("	 		FROM link_event_agent AS link_producer ")
				.append(CR).append("	 		WHERE link_producer.id_event =  contract.ID AND link_producer.agent_role = ").append(sqlProducerRole).append(" ) AS list_producers")
				.append(CR).append("  FROM event AS contract ")
				.append(CR).append("  JOIN link_event_agent AS link_consumer ON contract.id  = link_consumer.id_event AND link_consumer.agent_role = ").append(sqlConsumerRole)
				.append(CR).append("  JOIN link_history_active_event AS link_request ON link_request.id_history = contract.id_history")
				.append(CR).append("  WHERE contract.object_type ='CONTRACT' AND NOT contract.is_ending ")
				.append(CR).append("  			AND link_request.agent_name = link_consumer.agent_name")
				.append(CR).append("  			AND link_request.type IN ('REQUEST_START', 'REQUEST_UPDATE', 'REQUEST_SWITCH')");
		List<Map<String, Object>> rows = dbConnection.executeSelect(query.toString());
		Map<String, Map<String, Map<Long, Double>>> allResponseTimes = new HashMap<String, Map<String, Map<Long, Double>>>();
		for (Map<String, Object> row : rows) {
			long contractId = SapereUtil.getLongValue(row, "id_contract_event");
			long responseTime = SapereUtil.getLongValue(row, "max_warning_duration");
			String sProducerList = "" + row.get("list_producers");
			String consumer = "" + row.get("consumer");
			String[] listProducers = sProducerList.split(",");
			for (String producer : listProducers) {
				if (!allResponseTimes.containsKey(producer)) {
					allResponseTimes.put(producer, new HashMap<String, Map<Long, Double>>());
				}
				Map<String, Map<Long, Double>> producerResponseTimes = allResponseTimes.get(producer);
				if (!producerResponseTimes.containsKey(consumer)) {
					producerResponseTimes.put(consumer, new HashMap<Long, Double>());
				}
				Map<Long, Double> responseTimeByContract = producerResponseTimes.get(consumer);
				responseTimeByContract.put(contractId, (double) responseTime);
			}
		}
		// TODO : load in a map : contract id and the corresponding response time
		return allResponseTimes;
	}

	public static EnergyEvent retrieveEventById(Long id) throws HandlingException {
		List<Map<String, Object>> rows = dbConnection.executeSelect("SELECT * FROM event WHERE id= " + id);
		if (rows.size() > 0) {
			Map<String, Object> row = rows.get(0);
			EnergyEvent result = auxGetEvent(row);
			return result;
		}
		return null;
	}

	public static EnergyEvent retrieveEvent(EventType evtType, String agentName, Boolean isComplementary,
			Date beginDate) throws HandlingException {
		String sBeginDate = UtilDates.format_sql.format(beginDate);
		StringBuffer query = new StringBuffer();
		query.append("SELECT * FROM event WHERE ").append(" begin_date = ").append(addSingleQuotes(sBeginDate))
				.append(CR).append(" AND object_type = ").append(addSingleQuotes("" + evtType.getObjectType()))
				.append(CR).append(" AND main_category = ").append(addSingleQuotes("" + evtType.getMainCategory()))
				.append(CR).append(" AND agent_name = ").append(addSingleQuotes(agentName))
				.append(CR).append(" AND is_complementary = ").append((isComplementary ? "1" : "0"));
		List<Map<String, Object>> rows = dbConnection.executeSelect(query.toString());
		if (rows.size() > 0) {
			Map<String, Object> row = rows.get(0);
			EnergyEvent result = auxGetEvent(row);
			Long originId = null;
			if (row.get("id_origin") != null) {
				originId = SapereUtil.getLongValue(row, "id_origin");
			}
			if (originId != null) {
				EnergyEvent originEvent = retrieveEventById(originId);
				result.setOriginEvent(originEvent);
			}
			return result;
		}
		return null;
	}

	public static EnergyEvent registerEvent2(EnergyEvent event, String log) throws HandlingException {
		return registerEvent2(event, null, log);
	}

	public static EnergyEvent registerEvent2(EnergyEvent event, Contract contract, String log) throws HandlingException {
		EnergyEvent result = null;
		try {
			result = EnergyDbHelper.registerEvent(event, contract, log);
		} catch (DoublonException e) {
			logger.error(e);
			String evtIssuer = event.getIssuerProperties() == null ? "" : event.getIssuerProperties().getAgentName();
			result = retrieveEvent(event.getType(), evtIssuer, event.isComplementary(), event.getBeginDate());
		}
		return result;
	}

	public static void registerAgent(ProsumerProperties issuerProperties) throws HandlingException {
		Date begin = new Date();
		Map<String, String> affectation = new HashMap<>();
		affectation.put("id_session", "" + getSessionId());
		affectation.put("name", addSingleQuotes(issuerProperties.getAgentName()));
		affectation.put("node", addSingleQuotes(issuerProperties.getLocation().getName()));
		affectation.put("distance", addQuotes(issuerProperties.getDistance()));
		affectation.put("device_name", addSingleQuotes(issuerProperties.getDeviceProperties().getName()));
		affectation.put("device_category", addSingleQuotes(issuerProperties.getDeviceProperties().getCategory().name()));// getLabel
		affectation.put("environmental_impact",
				addQuotes(issuerProperties.getDeviceProperties().getEnvironmentalImpact().getLevel()));
		Map<String, String> confilctAffectation = new HashMap<>();
		confilctAffectation.put("distance", addQuotes(issuerProperties.getDistance()));
		String query = dbConnection.generateInsertQuery("agent", affectation, confilctAffectation);
		long result = dbConnection.execUpdate(query.toString());
		Date end = new Date();
		long timeSpentMS = end.getTime() - begin.getTime();
		logger.info("registerAgent : result = " + result + ", timeSpentMS = " + timeSpentMS);
	}

	public static EnergyEvent registerEvent(EnergyEvent event, Contract contract, String log) throws HandlingException {
		String sBeginDate = UtilDates.format_sql.format(event.getBeginDate());
		String sExpiryDate = UtilDates.format_sql.format(event.getEndDate());
		if (event.getIssuerProperties() == null) {
			throw new HandlingException("registerEvent : issuerProperties not set on event " + event.toString());
		} else if (!event.checkLocation()) {
			throw new HandlingException("registerEvent : location is not set on event " + event.toString());
		} else if (contract != null && !contract.checkLocation()) {
			throw new HandlingException("registerEvent : location is not set on contract " + contract.toString());
		}
		// Check duplicate event
		String evtIssuer = event.getIssuer();
		EnergyEvent doublonCheck = retrieveEvent(event.getType(), evtIssuer, event.isComplementary(),
				event.getBeginDate());
		if (doublonCheck != null) {
			throw new DoublonException("Event doublon : The client program tries to insert the following event twice : "
					+ event.toString() + ", log :" + log);
		}
		registerAgent(event.getIssuerProperties());
		Long sessionId = getSessionId();
		Map<String, String> affectation = new HashMap<>();
		affectation.put("begin_date", addSingleQuotes(sBeginDate));
		affectation.put("expiry_date", addSingleQuotes(sExpiryDate));
		affectation.put("id_session", "" + sessionId);
		affectation.put("object_type", addSingleQuotes("" + event.getType().getObjectType()));
		affectation.put("main_category", addSingleQuotes("" + event.getType().getMainCategory()));
		affectation.put("warning_type",
				event.getWarningType() == null ? "''" : addSingleQuotes(event.getWarningType().name()));
		affectation.put("power", addSingleQuotes("" + event.getPower()));
		affectation.put("power_min", addQuotes(event.getPowerMin()));
		affectation.put("power_max", addQuotes(event.getPowerMax()));
		affectation.put("power_update", addSingleQuotes("" + event.getPowerUpdate()));
		affectation.put("power_min_update", addQuotes(event.getPowerMinUpdate()));
		affectation.put("power_max_update", addQuotes(event.getPowerMaxUpdate()));
		if(event.getAdditionalPower() > 0) {
			affectation.put("additional_power",  addQuotes(event.getAdditionalPower()));
		}
		affectation.put("time_shift_ms", addQuotes(event.getTimeShiftMS()));
		affectation.put("agent_name", addSingleQuotes(evtIssuer));
		affectation.put("is_cancel", event.getType().getIsCancel() ? "1" : "0");
		affectation.put("is_ending", event.getType().getIsEnding() ? "1" : "0");
		affectation.put("id_origin", event.getOriginEvent() == null ? "NULL" : "" + event.getOriginEvent().getId());
		affectation.put("is_complementary", event.isComplementary() ? "1" : "0");
		affectation.put("comment", addSingleQuotes(event.getComment()));
		if(Double.isNaN(event.getFirstRate())) {
			logger.error("registerEvent : firstRate is NAN in event " + event);
		} else {
			affectation.put("first_rate", addQuotes(event.getFirstRate()));
		}
		String query = dbConnection.generateInsertQuery("event", affectation);
		long eventId = dbConnection.execUpdate(query.toString());
		if(eventId < 1  ) {
			logger.error("registerEvent : eventId = " + eventId + ", event = " + event);
			// Try to retrieve the new created Event
			EnergyEvent existingEvent = retrieveEvent(event.getType(), evtIssuer, event.isComplementary(), event.getBeginDate() );
			logger.error("registerEvent : existingEvent = " + existingEvent);
			if(existingEvent != null) {
				eventId = existingEvent.getId();
			}
		}
		EnergyEvent result = event;
		result.setId(eventId);
		if (contract != null) {
			EventType eventType = event.getType();
			if(EventObjectType.CONTRACT.equals(eventType.getObjectType()) &&  !eventType.getIsEnding()) {
				registerLinksEventAgent(eventId, contract);
			}
		}
		// Cancel all events from the same issuer that are before the insered event
		// dbConnection.setDebugLevel(10);
		String shortType = "" + event.getType().getObjectType();
		boolean isComplementary = event.isComplementary();
		dbConnection.setDebugLevel(0);
		StringBuffer rqUpdateEvt = new StringBuffer();
		rqUpdateEvt.append("UPDATE event SET cancel_date = ").append(addSingleQuotes(sBeginDate)).append(" WHERE ")
				.append(" agent_name = ").append(addSingleQuotes(evtIssuer))
				.append(" AND object_type = ").append(addSingleQuotes(shortType))
				.append(" AND begin_date < ").append(addSingleQuotes(sBeginDate))
				.append((isComplementary ? " AND is_complementary" : "")) // No not cancel previous main contracts event
																			// if the new event is complementary
				.append(" AND cancel_date IS NULL");
		dbConnection.execUpdate(rqUpdateEvt.toString());
		dbConnection.setDebugLevel(0);
		if (event.getOriginEvent() != null && event.getType().getIsCancel()) {
			// set the interruption date on the origin event
			long originEventId = event.getOriginEvent().getId();
			dbConnection.setDebugLevel(0);
			StringBuffer rqUpdateEvt2 = new StringBuffer();
			rqUpdateEvt2.append("UPDATE event SET interruption_date = ").append(OP_LEAST)
					.append("(IFNULL(interruption_date,'3000-01-01'),").append(addSingleQuotes(sBeginDate))
					.append(") WHERE id = ").append(addQuotes(originEventId));
			dbConnection.execUpdate(rqUpdateEvt2.toString());
			dbConnection.setDebugLevel(0);
		}
		return result;
	}

	public static void registerLinksEventAgent(Long eventId, Contract contract) throws HandlingException {
		// Consumer agent
		if(eventId == null || eventId == 0) {
			logger.error("registerLinksEventAgent " + contract + ", eventId not set");
		}
		Double globalPower = contract.getPower();
		Double globalPowerMax = contract.getPowerMax();
		Double globalPowerMin = contract.getPowerMin();
		Long sessionId = getSessionId();
		Map<String, String> affectation = new HashMap<>();
		affectation.put("id_event", addQuotes(eventId));
		affectation.put("id_session", addQuotes(sessionId));
		affectation.put("agent_role", addSingleQuotes(ProsumerRole.CONSUMER.name()));
		affectation.put("agent_name", addSingleQuotes(contract.getConsumerAgent()));
		affectation.put("power", addQuotes(globalPower));
		affectation.put("power_min", addQuotes(globalPowerMin));
		affectation.put("power_max", addQuotes(globalPowerMax));
		;
		String query1 = dbConnection.generateInsertQuery("link_event_agent", affectation);
		dbConnection.execUpdate(query1);
		// Producer agents
		for (String producer : contract.getProducerAgents()) {
			PowerSlot powerSlot = contract.getPowerSlotFromAgent(producer);
			Map<String, String> affectation2 = new HashMap<>();
			affectation2.put("id_event", addQuotes(eventId));
			affectation2.put("id_session", addQuotes(sessionId));
			affectation2.put("agent_role", addSingleQuotes(ProsumerRole.PRODUCER.name()));
			affectation2.put("agent_name", addSingleQuotes(producer));
			affectation2.put("power", addQuotes(powerSlot.getCurrent()));
			affectation2.put("power_min", addQuotes(powerSlot.getMin()));
			affectation2.put("power_max", addQuotes(powerSlot.getMax()));
			String query2 = dbConnection.generateInsertQuery("link_event_agent", affectation2);
			dbConnection.execUpdate(query2);
		}
		// Check gap
		if (eventId > 0) {
			Map<String, Object> rowCheckGap = dbConnection.executeSingleRowSelect(
					"SELECT IFNULL(SUM(lea.power),0) AS provided FROM link_event_agent AS lea WHERE lea.id_event = "
							+ addQuotes(eventId) + " AND lea.agent_role = " + addSingleQuotes(ProsumerRole.PRODUCER.name()));
			double provided = SapereUtil.getDoubleValue(rowCheckGap, "provided", logger);
			if (Math.abs(globalPower - provided) > 0.001) {
				double contractGap = contract.computeGap();
				logger.warning("Gap found in new contract " + rowCheckGap + " contractGap = " + contractGap);
			}
		}
	}

	public static NodeTotal generateNodeTotal(Date computeDate, NodeContext nodeContext, EnergyEvent linkedEvent, boolean isAdditionalRefresh)
			throws HandlingException {
		NodeLocation nodeLocation = nodeContext.getNodeLocation();
		int distance = NodeManager.getDistance(nodeLocation);
		Long timeShiftMS = nodeContext.getTimeShiftMS();
		NodeTotal nodeTotal = new NodeTotal();
		nodeTotal.setTimeShiftMS(timeShiftMS);
		nodeTotal.setDate(computeDate);
		nodeTotal.setNodeLocation(nodeLocation);
		nodeTotal.setDistance(distance);
		nodeTotal.setAdditionalRefresh(isAdditionalRefresh);
		String sComputeDate = UtilDates.format_sql.format(computeDate);
		String quotedComputeDate = addSingleQuotes(sComputeDate);
		boolean debugTmpTables = false; // debug mode : to replace TEMPORARY tables by tables
		String reqSeparator2 = dbConnection.getReqSeparator2();
		StringBuffer query = new StringBuffer();
		if(debugTmpTables) {
			query.append(dbConnection.generateQueryDropTable("TmpEvent"));
		} else {
			query.append(dbConnection.generateQueryDropTmpTable("TmpEvent"));
		}
		query.append(reqSeparator2);
		String sqlNode = addSingleQuotes(nodeLocation.getName());
		query.append(CR).append("CREATE ").append(debugTmpTables ? "" : "TEMPORARY").append(" TABLE TmpEvent(")
			.append(CR).append("	 id						")
			.append((sqlite ? "INTEGER " : "INT(11) ")).append((sqlite ? "" : "UNSIGNED"))
			.append(" NOT NULL ")
			.append((sqlite ? " PRIMARY KEY AUTOINCREMENT" : " AUTO_INCREMENT"))
			.append(CR).append("	,begin_date				DATETIME")
			.append(CR).append("	,id_origin				INT(11) DEFAULT NULL")
			.append(CR).append("	,effective_end_date 	DATETIME")
			.append(CR).append("	,object_type 			VARCHAR(32) CHECK(`object_type` IN ('PRODUCTION','REQUEST','CONTRACT')) DEFAULT NULL")
			.append(CR).append("	,main_category 			VARCHAR(32) CHECK(`main_category` IN ('START','STOP','EXPIRY','UPDATE', 'SWITCH')) DEFAULT NULL")
			.append(CR).append("	,agent_name 			VARCHAR(32) NOT NULL")
			.append(CR).append("	,is_complementary 		BIT(1) NOT NULL DEFAULT ").append((sqlite ? "0" : "b'0'"))
			.append(CR).append("	,node 					VARCHAR(16) NOT NULL DEFAULT ''")
			.append(CR).append("	,distance 				TINYINT UNSIGNED NOT NULL DEFAULT 0.0")
			.append(CR).append("	,is_selected 			BIT(1) NOT NULL DEFAULT ").append((sqlite ? "0" : "b'0'"))
			.append(CR).append("	,is_request 			BIT(1) NOT NULL DEFAULT ").append((sqlite ? "0" : "b'0'"))
			.append(CR).append("	,is_producer 			BIT(1) NOT NULL DEFAULT ").append((sqlite ? "0" : "b'0'"))
			.append(CR).append("	,is_contract 			BIT(1) NOT NULL DEFAULT ").append((sqlite ? "0" : "b'0'"))
			.append(CR).append("	,power					DECIMAL(15,3) NOT NULL DEFAULT 0.0")
			.append(CR).append("	,power_margin			DECIMAL(15,3) NOT NULL DEFAULT 0.0")
			.append(CR).append("	,provided 				DECIMAL(15,3) NOT NULL DEFAULT 0.0")
			.append(CR).append("	,provided_margin		DECIMAL(15,3) NOT NULL DEFAULT 0.0")
			.append(CR).append("	,consumed 				DECIMAL(15,3) NOT NULL DEFAULT 0.0")
			.append(CR).append("	,missing 				DECIMAL(15,3) NOT NULL DEFAULT 0.0")
			.append(CR).append((sqlite ? "" : "  ,PRIMARY KEY (`id`)"))
			.append(CR).append("	)");
		/*
		query.append(reqSeparator2)
			.append(CR).append("INSERT INTO TmpEvent")
			.append(CR).append("(id, begin_date, id_origin,effective_end_date,object_type,main_category,agent_name,node,distance,is_complementary,power,power_margin,is_request,is_producer,is_contract)")
			.append(CR).append("	SELECT id, begin_date, id_origin")
			.append(CR).append(" 		,").append((sqlite ? "MIN" : "LEAST")).append("(event.expiry_date")
			.append(CR).append("  			,IFNULL(event.interruption_date,'3000-01-01')")
			.append(CR).append("  			,IFNULL(event.cancel_date,'3000-01-01')) 	AS  effective_end_date")
			.append(CR).append("		,object_type,main_category,agent_name")
			.append(CR).append("		,agent.node")
			.append(CR).append("		,agent.distance")
			.append(CR).append("		,is_complementary")
			.append(CR).append("		,power")
			.append(CR).append("		,(power_max - power)							AS power_margin")
			.append(CR).append("		,object_type = 'REQUEST'						AS is_request")
			.append(CR).append("		,object_type = 'PRODUCTION'						AS is_producer")
			.append(CR).append("		,object_type = 'CONTRACT'				 		AS is_contract")
			.append(CR).append("	FROM event")
			.append(CR).append("	JOIN agent ON agent.id_session = event.id_session AND agent.name = event.agent_name")
			.append(CR).append("	WHERE NOT event.is_ending AND IFNULL(event.cancel_date,'3000-01-01') > ").append(quotedComputeDate)
			.append(CR).append("	AND agent.node = ").append(sqlNode)
			;
		query.append(reqSeparator2);
		query.append(CR).append("	UPDATE TmpEvent SET is_selected = 1 WHERE begin_date<=").append(quotedComputeDate)
				.append(" AND effective_end_date > ").append(quotedComputeDate);
		*/
		query.append(reqSeparator2)
		.append(CR).append("INSERT INTO TmpEvent")
		.append(CR).append("(id, begin_date, id_origin,effective_end_date,object_type,main_category,agent_name,node,distance,is_complementary,power,power_margin,is_request,is_producer,is_contract, is_selected)")
		.append(CR).append("	SELECT id, begin_date, id_origin")
		.append(CR).append(" 		,effective_end_date")
		.append(CR).append("		,object_type,main_category,agent_name")
		.append(CR).append("		,agent.node")
		.append(CR).append("		,agent.distance")
		.append(CR).append("		,is_complementary")
		.append(CR).append("		,power")
		.append(CR).append("		,(power_max - power)				AS power_margin")
		.append(CR).append("		,object_type = 'REQUEST'			AS is_request")
		.append(CR).append("		,object_type = 'PRODUCTION'			AS is_producer")
		.append(CR).append("		,object_type = 'CONTRACT'			AS is_contract")
		.append(CR).append("		,1 									AS is_selected")
		.append(CR).append("	FROM (")
		.append(CR).append("			SELECT event.*")
		.append(CR).append(" 				,").append((sqlite ? "MIN" : "LEAST")).append("(event.expiry_date")
		.append(CR).append("  					,IFNULL(event.interruption_date,'3000-01-01')")
		.append(CR).append("  					,IFNULL(event.cancel_date,'3000-01-01')) 	AS  effective_end_date")
		.append(CR).append("			FROM event")
		.append(CR).append("			WHERE NOT event.is_ending AND IFNULL(event.cancel_date,'3000-01-01') > ").append(quotedComputeDate)
		.append(CR).append("	) AS tmp_event_selected")
		.append(CR).append("	JOIN agent ON agent.id_session = tmp_event_selected.id_session AND agent.name = tmp_event_selected.agent_name")
		.append(CR).append("	WHERE begin_date<=").append(quotedComputeDate)
		.append(CR).append(" 		AND effective_end_date > ").append(quotedComputeDate)
		.append(CR).append("		AND agent.node = ").append(sqlNode)
		;
		query.append(reqSeparator2);
		if(debugTmpTables) {
			query.append(dbConnection.generateQueryDropTable("TmpRequestEvent"));
		} else {
			query.append(dbConnection.generateQueryDropTmpTable("TmpRequestEvent"));
		}
		query.append(reqSeparator2);
		query.append(CR).append("CREATE ").append(debugTmpTables ? "" : "TEMPORARY").append(" TABLE TmpRequestEvent AS")
			.append(CR)	.append("  SELECT TmpEvent.id, TmpEvent.agent_name AS consumer, power")
			.append(CR).append("	  FROM TmpEvent ")
			.append(CR).append("  WHERE is_selected AND object_type = 'REQUEST'");
		if (sqlite) {
			query.append(reqSeparator2);
			query.append(CR).append("DROP INDEX IF EXISTS _consumer");
			query.append(reqSeparator2);
			query.append(CR).append("CREATE INDEX _consumer ON `TmpRequestEvent` (consumer)");
		} else {
			query.append(reqSeparator2);
			query.append(CR).append("ALTER TABLE TmpRequestEvent ADD KEY (consumer)");
		}
		query.append(reqSeparator2);
		if (debugTmpTables) {
			query.append(dbConnection.generateQueryDropTable("TmpContractEvent"));
		} else {
			query.append(dbConnection.generateQueryDropTmpTable("TmpContractEvent"));
		}
		query.append(reqSeparator2);
		String sqlProducerRole = addSingleQuotes(ProsumerRole.PRODUCER.name());
		String sqlConsumerRole = addSingleQuotes(ProsumerRole.CONSUMER.name());
		query.append(CR).append("CREATE ").append(debugTmpTables ? "" : "TEMPORARY ").append(" TABLE TmpContractEvent AS")
			.append(CR).append(	" 	SELECT TmpEvent.id, consumer.agent_name AS consumer, TmpEvent.power")
			.append(CR).append(" 	FROM TmpEvent ")
			.append(CR).append(" 	JOIN link_event_agent AS consumer ON consumer.id_event = TmpEvent.id AND consumer.agent_role=").append(sqlConsumerRole)
			.append(CR).append("    JOIN TmpRequestEvent ON TmpRequestEvent.consumer = consumer.agent_name ")
			.append(CR).append("  	WHERE is_selected AND is_contract");
		if (sqlite) {
			query.append(reqSeparator2);
			query.append(CR).append("UPDATE TmpEvent SET is_selected = 0")
				.append(CR).append("	WHERE TmpEvent.is_selected AND is_contract  ")
				.append(CR).append(" AND NOT EXISTS (SELECT 1 FROM TmpContractEvent WHERE TmpContractEvent.id = TmpEvent.id)");
			query.append(reqSeparator2);
			query.append(CR).append("DROP INDEX IF EXISTS _consumer2");
			query.append(reqSeparator2);
			query.append(CR).append("CREATE INDEX _consumer2 ON TmpContractEvent(consumer)");
		} else {
			query.append(reqSeparator2);
			query.append(CR).append("UPDATE TmpEvent ")
					.append(CR).append("	LEFT JOIN TmpContractEvent ON TmpContractEvent.id = TmpEvent.id")
					.append(CR).append("	SET TmpEvent.is_selected = 0")
					.append(CR).append("	WHERE TmpEvent.is_selected AND is_contract  AND TmpContractEvent.id is NULL");
			query.append(reqSeparator2);
			query.append(CR).append("ALTER TABLE TmpContractEvent ADD KEY (consumer)");
		}
		query.append(reqSeparator2);
		query.append(CR).append("UPDATE TmpEvent SET consumed = (SELECT IFNULL(SUM(TmpContractEvent.power),0) ")
			.append(CR).append("   		FROM TmpContractEvent ")
			.append(CR).append("	 	WHERE TmpContractEvent.consumer = TmpEvent.agent_name )")
			.append(CR).append("	WHERE TmpEvent.is_selected AND TmpEvent.is_request");
		query.append(reqSeparator2);
		query.append(CR).append("UPDATE TmpEvent SET missing = ").append(OP_GREATEST)
				.append("(0,power - consumed) WHERE is_request");
		query.append(reqSeparator2);
		query.append(CR).append("UPDATE TmpEvent ")
			.append(CR).append(" 	SET provided = (SELECT IFNULL(SUM(lea.power),0) ")
			.append(CR).append("   		FROM link_event_agent AS lea")
			.append(CR).append("    	JOIN TmpContractEvent ON TmpContractEvent.id = lea.id_event")
			.append(CR).append("		WHERE lea.agent_name = TmpEvent.agent_name)")
			.append(CR).append("	, provided_margin = (SELECT IFNULL(SUM(lea.power_max - lea.power),0) ")
			.append(CR).append("   		FROM link_event_agent AS lea")
			.append(CR).append("    	JOIN TmpContractEvent ON TmpContractEvent.id = lea.id_event")
			.append(CR).append("		WHERE lea.agent_name = TmpEvent.agent_name)")
			.append(CR).append("	WHERE TmpEvent.is_selected AND TmpEvent.is_producer");
		query.append(reqSeparator2);
		if (sqlite) {
			query.append(CR).append(
					"UPDATE TmpEvent SET power = ROUND(power, 3), missing = ROUND(missing,3), consumed = ROUND(consumed, 3)");
			query.append(reqSeparator2);
		}
		query.append(CR).append("SELECT ").append(quotedComputeDate).append(" AS date ")
			.append(CR).append(",IFNULL(SUM(TmpEvent.power),0) AS sum_all")
			.append(CR).append(",IFNULL(SUM(").append(OP_IF).append("(TmpEvent.is_request, TmpEvent.power,0.0)),0) AS total_requested")
			.append(CR).append(",IFNULL(SUM(").append(OP_IF).append("(TmpEvent.is_producer, TmpEvent.power,0.0)),0) AS total_produced")
			.append(CR).append(",IFNULL(SUM(").append(OP_IF).append("(TmpEvent.is_producer, TmpEvent.provided,0.0)),0) AS total_provided")
			.append(CR).append(",IFNULL(SUM(").append(OP_IF).append("(TmpEvent.is_request, TmpEvent.consumed,0.0)),0) AS total_consumed")
			// .append(CR).append(",IFNULL(SUM().append(OP_IF).append("(TmpEvent.is_contract,
			// TmpEvent.power_margin,0.0)),0) AS old_total_margin")
			.append(CR).append(",IFNULL(SUM(").append(OP_IF).append("(TmpEvent.is_producer, TmpEvent.provided_margin,0.0)),0) AS total_provided_margin")
			.append(CR).append(",IFNULL(MIN(").append(OP_IF).append("(TmpEvent.is_request AND TmpEvent.missing > 0, TmpEvent.missing, 999999.0)),0) AS min_request_missing")
			.append(CR).append("	 FROM TmpEvent WHERE is_selected");
		dbConnection.setDebugLevel(0);
		List<Map<String, Object>> sqlResult = dbConnection.executeSelect(query.toString());
		// dbConnection.setDebugLevel(0);
		if (sqlResult.size() > 0) {
			Map<String, Object> row = sqlResult.get(0);
			double requested = SapereUtil.getDoubleValue(row, "total_requested", logger);
			double produced = SapereUtil.getDoubleValue(row, "total_produced", logger);
			double provided = SapereUtil.getDoubleValue(row, "total_provided", logger);
			double providedMargin = SapereUtil.getDoubleValue(row, "total_provided_margin", logger);
			double consumed = SapereUtil.getDoubleValue(row, "total_consumed", logger);
			double minRequestMissing = SapereUtil.getDoubleValue(row, "min_request_missing", logger);
			if (linkedEvent != null && EventType.REQUEST_EXPIRY.equals(linkedEvent.getType())) {
				logger.info("generateNodeTotal : Request expiry");
			}
			// For debug
			boolean toDebug = false;
			if(toDebug) {
				NodeContent nodeContent = Sapere.getInstance().retrieveNodeContent();
				NodeTotal nodeTotal2 = nodeContent.getTotal();
				if(Math.abs(provided - nodeTotal2.getProvided()) >= 500  ) {
					logger.error("generateNodeTotal : big difference of provided computed from TmpEvent and from retrieveNodeContent");					
				}
			}
			boolean checkGaps = false;
			if (checkGaps) {
				logger.warning("generateNodeTotal step 12345 : consumed = " + consumed + ", provided = " + provided);
				if (consumed > requested + 0.01) {
					logger.warning(
							"Consumed greated then requested : consumed = " + consumed + ", requested = " + requested);
					String testGap1 = "SELECT ctr.*,IFNULL(TmpRequestEvent.power,0) AS requested "
							+ " FROM TmpContractEvent "
							+ " LEFT JOIN TmpRequestEvent ON TmpRequestEvent.consumer = TmpContractEvent.consumer "
							+ " WHERE TmpContractEvent.is_selected";
					List<Map<String, Object>> rowsTestGap = dbConnection.executeSelect(testGap1);
					double totalConsumed = 0;
					double totalRequested = 0;
					for (Map<String, Object> nextRow : rowsTestGap) {
						double nextConsumed = SapereUtil.getDoubleValue(nextRow, "power", logger);
						double nextRequested = SapereUtil.getDoubleValue(nextRow, "requested", logger);
						if (nextConsumed > nextRequested) {
							logger.info("nextRow = " + nextRow);
							logger.info("Gap found for contract " + nextRow.get("agent_name") + " conumed = " + nextConsumed
									+ ", nextRequested = " + nextRequested);
						}
						totalConsumed += nextConsumed;
						totalRequested += nextRequested;
					}
					logger.info("totalConsumed = " + totalConsumed + ", totalProvidefd = " + totalRequested);
				}
			}
			// Consumption cannot be greater than request
			if (consumed > requested) {
				consumed = requested;
			}
			// Provided cannot be greater than produced
			if (provided > produced) {
				provided = produced;
			}
			if (checkGaps) {
				if (Math.abs(consumed - provided) >= 0.99) {
					logger.warning("Gap between provided and consumed : " + Math.abs(consumed - provided));
					String testGap1 = "SELECT ctr.* "
							+ " ,(SELECT IFNULL(sum(link2.power),0) FROM link_event_agent AS link2 "
							+ "		WHERE link2.id_event = ctr.id and link2.agent_role = " + sqlProducerRole + ") AS provided"
							+ " FROM TmpEvent AS ctr " + " WHERE ctr.is_selected AND ctr.is_contract";
					List<Map<String, Object>> rowsTestGap = dbConnection.executeSelect(testGap1);
					double totalConsumed = 0;
					double totalProvided = 0;
					for (Map<String, Object> nextRow : rowsTestGap) {
						double nextConsumed = SapereUtil.getDoubleValue(nextRow, "power", logger);
						double nextProvided = SapereUtil.getDoubleValue(nextRow, "provided", logger);
						if (Math.abs(nextConsumed - nextProvided) > 0.0001) {
							logger.info("nextRow = " + nextRow);
							logger.info("Gap found for contract " + nextRow.get("agent_name") + " conumed = " + nextConsumed
									+ ", nextProvided = " + nextProvided);
						}
						totalConsumed += nextConsumed;
						totalProvided += nextProvided;
					}
					logger.info("totalConsumed = " + totalConsumed + ", totalProvided = " + totalProvided);
				}
			}
			nodeTotal.setRequested(requested);
			nodeTotal.setProduced(produced);
			nodeTotal.setConsumed(consumed);
			nodeTotal.setProvided(provided);
			nodeTotal.setProvidedMargin(providedMargin); // pb contract of other agents
			nodeTotal.setAvailable(produced - provided - providedMargin);
			nodeTotal.setMissing(requested - consumed);
			if (minRequestMissing <= requested) {
				nodeTotal.setMinRequestMissing(minRequestMissing);
			}
		}
		if (nodeTotal.hasActivity()) {
			nodeTotal = registerNodeTotal(nodeTotal, linkedEvent);
			long newHistoryId = nodeTotal.getId() == null ? -1 : nodeTotal.getId();
			// nodeTotal.setId(id);
			Date timeBefore = new Date();
			double available = nodeTotal.getAvailable();
			String idLast2 = nodeTotal.getIdLast() == null ? "NULL" : "" + nodeTotal.getIdLast();
			StringBuffer queryInsertLHE = new StringBuffer();
			queryInsertLHE.append(CR).append("UPDATE link_history_active_event SET id_last=NULL WHERE id_history = ").append(newHistoryId);
			queryInsertLHE.append(reqSeparator2);
			queryInsertLHE.append(CR).append(
					"UPDATE link_history_active_event SET id_last=NULL WHERE id_last IN (SELECT id FROM link_history_active_event WHERE id_history = ").append(newHistoryId).append(")");
			queryInsertLHE.append(reqSeparator2);
			queryInsertLHE.append(CR).append("DELETE FROM link_history_active_event WHERE id_history = ").append(newHistoryId);
			queryInsertLHE.append(reqSeparator2);
			queryInsertLHE.append(CR).append("INSERT INTO link_history_active_event(id_history,id_event,date,provided,provided_margin,consumed,missing,is_request,is_producer,is_contract,has_warning_req,id_last)")
			.append(CR).append("SELECT ").append(newHistoryId).append(", id,")
			.append(CR).append(quotedComputeDate)
			.append(CR).append(",provided,provided_margin,consumed,missing,is_request,is_producer,is_contract")
			.append(CR).append(",(is_request AND missing > 0 AND missing < '").append(available).append("') AS has_warning_req")
			.append(CR).append("	,(SELECT (last.id) FROM link_history_active_event AS last WHERE last.id_history=")
			.append(idLast2).append(" AND last.id_event = TmpEvent.id) AS id_last")
			.append(CR).append(" FROM TmpEvent")
			.append(CR).append(" WHERE TmpEvent.is_selected");
			queryInsertLHE.append(reqSeparator2);
			queryInsertLHE.append(CR).append("UPDATE link_history_active_event SET id_last = ")
				.append(CR).append("	 (SELECT (last.id) ")
				.append(CR).append(" 		FROM event AS current_event, link_history_active_event AS last")
				.append(CR).append(" 			WHERE last.id_history=").append(idLast2)
				.append(CR).append(" 			AND current_event.id = link_history_active_event.id_event ")
				.append(CR).append(" 			AND last.id_event = current_event.id_origin)")
				.append(CR).append("  	WHERE link_history_active_event.id_history = ").append(newHistoryId).append(CR).append(" AND link_history_active_event.id_last IS NULL");
			queryInsertLHE.append(reqSeparator2);
			queryInsertLHE.append(CR).append("UPDATE link_history_active_event SET warning_duration = ");
			if (sqlite) {
				queryInsertLHE.append(CR).append("	(SELECT last.warning_duration + strftime('%s',link_history_active_event.date) - strftime('%s' ,last.date)");
			} else {
				queryInsertLHE.append(CR).append("	(SELECT last.warning_duration + UNIX_TIMESTAMP(link_history_active_event.date) - UNIX_TIMESTAMP(last.date)");
			}
			queryInsertLHE.append(CR).append(" FROM link_history_active_event AS last WHERE last.id =  link_history_active_event.id_last AND last.has_warning_req)");
			queryInsertLHE.append(CR).append("  WHERE id_history = ").append(newHistoryId)
				.append(CR).append(" AND link_history_active_event.has_warning_req")
				.append(CR).append("  AND EXISTS (SELECT 1 FROM link_history_active_event AS last WHERE last.id = link_history_active_event.id_last AND last.has_warning_req)");
			long result = dbConnection.execUpdate(queryInsertLHE.toString());
			if (result < 0) {
				// ONLY FOR DEBUG IF THERE IS AN SQL ERROR
			}
			if (result >= 0 && nodeTotal.getMissing() > 0 && nodeTotal.getAvailable() > 0) {
				StringBuffer queryUpdateHisto = new StringBuffer();
				queryUpdateHisto.append("UPDATE node_history SET ")
						.append(CR).append(" max_warning_duration = (SELECT IFNULL(MAX(lhe.warning_duration),0) FROM link_history_active_event AS lhe ")
						.append(CR).append(" 			WHERE lhe.id_history = ").append(newHistoryId).append(")  ")
						.append(CR).append(",max_warning_consumer = (SELECT current_event.agent_name FROM link_history_active_event AS lhae, event AS current_event ")
						.append(CR).append(" 			WHERE lhae.id_history = ").append(newHistoryId)
						.append(CR).append("          	AND current_event.id = lhae.id_event")
						.append(CR).append("			 AND lhae.has_warning_req ORDER BY warning_duration DESC, power LIMIT 0,1)")
						.append(CR).append(" WHERE node_history.id=").append(newHistoryId);
				result = dbConnection.execUpdate(queryUpdateHisto.toString());
			}
			long duration = new Date().getTime() - timeBefore.getTime();
			if (duration > 50) {
				logger.info("queryInsertLinkeHistoEvent duration (MS) : " + duration);
			}
		}
		nodeTotal.refreshLastUpdate();
		dbConnection.setDebugLevel(0);
		return nodeTotal;
	}

	private static NodeTotal registerNodeTotal(NodeTotal nodeTotal, EnergyEvent linkedEvent) throws HandlingException {
		Long sessionId = getSessionId();
		String reqSeparator2 = dbConnection.getReqSeparator2();
		String node2 = addSingleQuotes(nodeTotal.getNodeLocation().getName());
		if(nodeTotal.getDate() == null) {
			throw new HandlingException("registerNodeTotal : the date bust be set in nodeTotal");
		}
		String sHistoryDate = addSingleQuotes(UtilDates.format_sql.format(nodeTotal.getDate()));
		StringBuffer queryClean = new StringBuffer();
		queryClean.append("DROP ").append(OP_TEMPORARY).append(" TABLE IF EXISTS TmpCleanHisto");
		queryClean.append(reqSeparator2);
		queryClean.append(CR).append("CREATE ").append(OP_TEMPORARY)
				.append(" TABLE TmpCleanHisto AS SELECT h.id FROM node_history h WHERE h.date > ").append(sHistoryDate)
				.append(" AND h.id_session = ").append(sessionId)
				.append(" AND NOT EXISTS (SELECT 1 FROM event e WHERE e.id_history = h.id)");
		queryClean.append(reqSeparator2);
		queryClean.append(CR).append("UPDATE node_history SET id_next = NULL WHERE id_next IN (SELECT id FROM TmpCleanHisto)");
		queryClean.append(reqSeparator2);
		queryClean.append(CR).append("UPDATE node_history SET id_last = NULL WHERE id_last IN (SELECT id FROM TmpCleanHisto)");
		queryClean.append(reqSeparator2);
		queryClean.append(CR).append("UPDATE single_offer SET id_history = NULL WHERE id_history IN (SELECT id FROM TmpCleanHisto)");
		queryClean.append(reqSeparator2);
		queryClean.append(CR).append("UPDATE link_history_active_event SET id_last = NULL WHERE id_last IN").append(
				" (SELECT ID FROM link_history_active_event link_h_e WHERE id_history IN (SELECT id FROM TmpCleanHisto))");
		queryClean.append(reqSeparator2);
		queryClean.append(CR).append("DELETE ").append((sqlite ? "" : "link_h_e")).append(
				" FROM link_history_active_event AS link_h_e WHERE id_history IN (SELECT id FROM TmpCleanHisto)");
		queryClean.append(reqSeparator2);
		queryClean.append(CR).append("DELETE ").append((sqlite ? "" : "h"))
				.append(" FROM node_history AS h WHERE id IN (SELECT id FROM TmpCleanHisto)");
		long resultClean = dbConnection.execUpdate(queryClean.toString());
		if (resultClean < 0) {
			logger.error("registerNodeTotal resultClean = " + resultClean);
		} else {
			logger.info("registerNodeTotal resultClean = " + resultClean);
		}
		// Variable id_last
		Map<String, String> defaultAffectation = new HashMap<>();
		defaultAffectation.put("date", sHistoryDate);
		defaultAffectation.put("distance", addQuotes(nodeTotal.getDistance()));
		defaultAffectation.put("time_shift_ms", addQuotes(nodeTotal.getTimeShiftMS()));
		defaultAffectation.put("id_session", ""+ sessionId);
		defaultAffectation.put("node", node2);
		defaultAffectation.put("total_requested", addSingleQuotes("" + nodeTotal.getRequested()));
		defaultAffectation.put("total_consumed", addSingleQuotes("" + nodeTotal.getConsumed()));
		defaultAffectation.put("total_produced", addSingleQuotes("" + nodeTotal.getProduced()));
		defaultAffectation.put("total_provided", addSingleQuotes("" + nodeTotal.getProvided()));
		defaultAffectation.put("total_available", addSingleQuotes("" + nodeTotal.getAvailable()));
		defaultAffectation.put("total_missing", addSingleQuotes("" + nodeTotal.getMissing()));
		defaultAffectation.put("min_request_missing", addSingleQuotes("" + nodeTotal.getMinRequestMissing()));
		defaultAffectation.put("total_margin", addSingleQuotes("" + nodeTotal.getProvidedMargin()));
		defaultAffectation.put("is_additional_refresh", nodeTotal.isAdditionalRefresh()? "1" : "0");
		defaultAffectation.put("id_last", "(SELECT ID FROM node_history WHERE date < " + sHistoryDate + " AND node = " + node2 + " ORDER BY date DESC LIMIT 0,1)");
		defaultAffectation.put("id_next", "(SELECT ID FROM node_history WHERE date > " + sHistoryDate + " AND node = " + node2 + " ORDER BY date LIMIT 0,1)");

		Map<String, String> confilctAffectation = new HashMap<>();
		confilctAffectation.put("total_requested", addSingleQuotes("" + nodeTotal.getRequested()));
		confilctAffectation.put("total_consumed", addSingleQuotes("" + nodeTotal.getConsumed()));
		confilctAffectation.put("total_produced", addSingleQuotes("" + nodeTotal.getProduced()));
		confilctAffectation.put("total_provided", addSingleQuotes("" + nodeTotal.getProvided()));
		confilctAffectation.put("total_available", addSingleQuotes("" + nodeTotal.getAvailable()));
		confilctAffectation.put("total_missing", addSingleQuotes("" + nodeTotal.getMissing()));
		confilctAffectation.put("min_request_missing", addSingleQuotes("" + nodeTotal.getMinRequestMissing()));
		confilctAffectation.put("total_margin", addSingleQuotes("" + nodeTotal.getProvidedMargin()));
		confilctAffectation.put("is_additional_refresh", nodeTotal.isAdditionalRefresh()? "1" : "0");
		confilctAffectation.put("id_last", "(SELECT ID FROM node_history WHERE date < " + sHistoryDate + " AND node = " + node2 + " ORDER BY date DESC LIMIT 0,1)");
		confilctAffectation.put("id_next", "(SELECT ID FROM node_history WHERE date > " + sHistoryDate + " AND node = " + node2 + " ORDER BY date LIMIT 0,1)");
		String queryInsertHistory = dbConnection.generateInsertQuery("node_history", defaultAffectation, confilctAffectation);
		long result1 = dbConnection.execUpdate(queryInsertHistory.toString());
		Map<String, Object> rowHisto = dbConnection.executeSingleRowSelect("SELECT MAX(ID) AS ID FROM node_history WHERE date = " + sHistoryDate + " AND node = " + node2);
		/*
		long newHistoId = dbConnection.execUpdate(queryInsert.toString());
		if(newHistoId <= 0) {
			logger.error("registerNodeTotal : newHistoId returned IS NULL");
			Map<String, Object> rowHisto = dbConnection.executeSingleRowSelect("SELECT MAX(ID) AS ID FROM node_history WHERE date = " + sHistoryDate + " AND node = " + node2);
			newHistoId = SapereUtil.getLongValue(rowHisto, "ID");
		}*/
		long newHistoryId = SapereUtil.getLongValue(rowHisto, "ID");
		NodeTotal newHistory = retrieveNodeTotalById(newHistoryId);
		if(newHistory == null) {
			long test = dbConnection.execUpdate(queryInsertHistory.toString());
			logger.error("test = " + test);
			throw new HandlingException("registerNodeTotal : newHistory is null : newHistoId = " + newHistoryId + ", sHistoryDate = " + sHistoryDate);
		}
		long idLast = newHistory.getIdLast();
		Map<String, Object> rowDateLast = dbConnection.executeSingleRowSelect("SELECT date FROM node_history WHERE id = " + idLast);
		Date dateLast = null;
		if(rowDateLast != null) {
			dateLast = SapereUtil.getDateValue(rowDateLast, "date", logger);
		}
		String sDateLast = (dateLast == null) ? "NULL" :  addSingleQuotes(UtilDates.format_sql.format(dateLast));
		StringBuffer query = new StringBuffer();
		if (linkedEvent != null) {
			long evtId = linkedEvent.getId();
			query.append(CR).append("UPDATE event SET id_history =").append(newHistoryId).append(" WHERE id = ").append(addQuotes(evtId));
			query.append(reqSeparator2);
		}
		// Correction of node_history.id_next on recent rows
		query.append(CR).append("UPDATE node_history ")
			.append(CR).append(" SET id_next = (SELECT h2.ID FROM node_history h2 WHERE h2.date > node_history.date AND h2.node = node_history.node ORDER BY h2.date LIMIT 0,1)")
				.append(CR).append(" WHERE node_history.date >= ").append(sDateLast)
				.append(CR).append(" 	AND node = ").append(node2)
				.append(CR).append(" 	AND id_session = ").append(sessionId)
				.append(CR).append(" 	AND IFNULL(id_next,0) <> (SELECT h2.ID FROM node_history h2 WHERE h2.date > node_history.date AND h2.node = node_history.node ORDER BY h2.date LIMIT 0,1)");
		query.append(reqSeparator2);
		// Correction of node_history.id_last on recent rows
		query.append(CR).append("DROP ").append(OP_TEMPORARY).append(" TABLE IF EXISTS TmpCorrectIdLast");
		query.append(reqSeparator2);
		query.append(CR).append("CREATE TEMPORARY TABLE TmpCorrectIdLast AS")
			.append(CR).append("SELECT id, id_last, id_last_toset FROM")
			.append(CR).append("	( SELECT id, id_last,")
			.append(CR).append("		 	(SELECT h2.ID FROM node_history h2 WHERE h2.date < h.date AND h2.node = h.node")
			.append(CR).append("				ORDER BY h2.date DESC LIMIT 0,1) AS id_last_toset")
			.append(CR).append(" 			FROM node_history h")
			.append(CR).append("			WHERE h.date > ").append(sHistoryDate)
			.append(CR).append(" 				AND node = ").append(node2)
			.append(CR).append(" 				AND id_session = ").append(sessionId)
			.append(CR).append(" 		) AS TmpRecentHistory")
			.append(CR).append("	WHERE NOT TmpRecentHistory.id_last = TmpRecentHistory.id_last_toset");
		query.append(reqSeparator2);
		if (sqlite) {
			query.append(CR).append("UPDATE node_history SET id_last = (SELECT TmpCorrectIdLast.id_last_toset")
				.append(CR).append("	FROM TmpCorrectIdLast ")
				.append(CR).append("	WHERE TmpCorrectIdLast.id = node_history.id)")
				.append(CR).append(" WHERE id IN (SELECT id FROM TmpCorrectIdLast) ");
		} else {
			query.append(CR).append("UPDATE TmpCorrectIdLast")
				.append(CR).append("	JOIN node_history ON node_history.id = TmpCorrectIdLast.id")
				.append(CR).append("	SET node_history.id_last = TmpCorrectIdLast.id_last_toset");
		}
		// correction of link_history_active_event.id_last WHERE node_history.id_last is
		// corrected
		query.append(reqSeparator2);
		if (sqlite) {
			query.append(CR).append("DROP ").append(OP_TEMPORARY).append(" TABLE IF EXISTS TmpCorrectIdLastLink1");
			query.append(reqSeparator2);
			query.append(CR).append("CREATE TEMPORARY TABLE TmpCorrectIdLastLink1 AS")
				.append(CR).append("SELECT current.id, current.id_last, last.id AS id_last_toset")
				.append(CR).append(" FROM TmpCorrectIdLast ")
				.append(CR).append(" JOIN link_history_active_event AS current ON current.id_history = TmpCorrectIdLast.id")
				.append(CR)
				.append(" JOIN link_history_active_event AS last ON last.id_history = TmpCorrectIdLast.id_last_toset AND last.id_event = current.id_event");
//					.append(CR).append(" WHERE NOT current.id_last = ");
			query.append(reqSeparator2);
			query.append(CR).append("UPDATE link_history_active_event SET id_last = (SELECT TmpCorrectIdLastLink1.id_last_toset")
				.append(CR).append(" FROM TmpCorrectIdLastLink1 WHERE TmpCorrectIdLastLink1.id = link_history_active_event.id)")
				.append(CR).append(" WHERE id IN (SELECT id FROM TmpCorrectIdLastLink1)");
			query.append(reqSeparator2);
			query.append(CR).append("DROP ").append(OP_TEMPORARY).append(" TABLE IF EXISTS TmpCorrectIdLastLink2");
			query.append(reqSeparator2);
			query.append(CR).append("CREATE TEMPORARY TABLE TmpCorrectIdLastLink2 AS")
				.append(CR).append("SELECT current.id, current.id_last, last.id AS id_last_toset").append(CR)
				.append(" FROM TmpCorrectIdLast ")
				.append(CR).append(" JOIN link_history_active_event AS current ON current.id_history = TmpCorrectIdLast.id")
				.append(CR).append(" JOIN Event AS current_event ON current_event.id = current.id_event")
				.append(CR).append(" JOIN link_history_active_event AS last ON last.id_history = TmpCorrectIdLast.id_last_toset AND last.id_event = current_event.id_origin")
				.append(CR).append(" WHERE current.id_last IS NULL");
			query.append(reqSeparator2);
			query.append(CR).append("UPDATE link_history_active_event SET id_last = (SELECT TmpCorrectIdLastLink2.id_last_toset")
				.append(CR).append(" FROM TmpCorrectIdLastLink2 WHERE TmpCorrectIdLastLink2.id = link_history_active_event.id)")
				.append(CR).append(" WHERE id IN (SELECT id FROM TmpCorrectIdLastLink2)");
		} else {
			query.append(CR).append("UPDATE TmpCorrectIdLast")
				.append(CR)	.append("	JOIN link_history_active_event AS current ON current.id_history = TmpCorrectIdLast.id")
				.append(CR).append(" 	JOIN Event AS current_event ON current_event.id = current.id_event")
				.append(CR).append("	SET current.id_last = (SELECT (last.id) FROM link_history_active_event AS last ")
				.append(CR).append("			WHERE last.id_history=TmpCorrectIdLast.id_last_toset AND last.id_event = current.id_event)");
			query.append(reqSeparator2);
			query.append(CR).append("UPDATE TmpCorrectIdLast")
				.append(CR).append("	JOIN link_history_active_event AS current ON current.id_history = TmpCorrectIdLast.id")
				.append(CR).append(" 	JOIN Event AS current_event ON current_event.id = current.id_event")
				.append(CR).append("	SET current.id_last = (SELECT (last.id) FROM link_history_active_event AS last ")
				.append(CR).append("			WHERE last.id_history=TmpCorrectIdLast.id_last_toset AND last.id_event = current_event.id_origin)")
				.append(CR).append("	WHERE current.id_last IS NULL");
		}
		// Set node_historyId on single offers
		query.append(reqSeparator2);
		if (sqlite) {
			query.append(CR).append(
					"UPDATE single_offer SET id_history=(SELECT h.ID FROM node_history h WHERE h.date <= single_offer.date ORDER BY h.date DESC LIMIT 0,1)")
					.append(CR).append(" WHERE single_offer.date >= (SELECT IFNULL(").append(sDateLast)
					.append(",'2000-01-01'))");
		} else {
			query.append(CR).append(
					"UPDATE single_offer SET id_history=(SELECT h.ID FROM node_history h WHERE h.date <= single_offer.date ORDER BY h.date DESC LIMIT 0,1)")
					.append(CR).append(" WHERE single_offer.date >= (SELECT IFNULL(").append(sDateLast)
					.append(",'2000-01-01'))");
		}
		// query.append("UPDATE single_offer SET id_history=(SELECT h.ID from node_history h
		// where h.date <= single_offer.creation_time ORDER BY h.date DESC LIMIT 0,1)
		// WHERE single_offer.creation_time >='2000-01-01'");
		dbConnection.execUpdate(query.toString());
		return newHistory;
	}

	public static void cleanHistoryDB() throws HandlingException {
		dbConnection.execUpdate("DELETE FROM single_offer");
		dbConnection.execUpdate("DELETE FROM link_event_agent");
		dbConnection.execUpdate("UPDATE link_history_active_event SET id_last=NULL");
		dbConnection.execUpdate("DELETE FROM link_history_active_event");
		dbConnection.execUpdate("DELETE FROM event");
		dbConnection.execUpdate("UPDATE node_history SET id_last=NULL, id_next=NULL");
		dbConnection.execUpdate("DELETE FROM node_history");
	}

	public static long registerSingleOffer(SingleOffer offer, EnergyEvent productionEvent, long currentHistoId) throws HandlingException {
		Long sessionId = getSessionId();
		String prodEvtId = (productionEvent == null) ? "NULL" : "" + productionEvent.getId();
		String sDate = UtilDates.format_sql.format(offer.getCreationTime());
		String sExpiryDate = UtilDates.format_sql.format(offer.getDeadline());
		String sBeginDate = UtilDates.format_sql.format(offer.getBeginDate());
		String sEndDate = UtilDates.format_sql.format(offer.getEndDate());
		String reqEvtId = (offer.getRequest() == null || offer.getRequest().getEventId() == null) ? "NULL"
				: "" + offer.getRequest().getEventId();
		if(offer.getRequest() == null || offer.getRequest().getEventId() == null) {
			logger.warning("registerSingleOffer : no request id : request = " + offer.getRequest());
		}
		DiscountItem discount = offer.getPricingTable() == null ? null : offer.getPricingTable().computeGlobalDiscount();
		Map<String, String> affectation = new HashMap<>();
		affectation.put("deadline", addSingleQuotes(sExpiryDate));
		affectation.put("date", addSingleQuotes(sDate));
		affectation.put("begin_date", addSingleQuotes(sBeginDate));
		affectation.put("end_date", addSingleQuotes(sEndDate));
		affectation.put("id_session", "" + sessionId);
		affectation.put("producer_agent", addSingleQuotes(offer.getProducerAgent()));
		affectation.put("consumer_agent", addSingleQuotes(offer.getConsumerAgent()));
		affectation.put("power", addQuotes(offer.getPower()));
		affectation.put("power_min", addQuotes(offer.getPowerMin()));
		affectation.put("power_max", addQuotes(offer.getPowerMax()));
		affectation.put("production_event_id", prodEvtId);
		affectation.put("request_event_id ", reqEvtId);
		affectation.put("log", addSingleQuotes(offer.getLog()));
		affectation.put("time_shift_ms", addQuotes(offer.getTimeShiftMS()));
		affectation.put("is_complementary", offer.isComplementary() ? "1" : "0");
		if(discount != null && Math.abs(discount.getCreditGrantedWH()) > 0) {
			affectation.put("credit_granted_wh", addQuotes(discount.getCreditGrantedWH()));
			affectation.put("credit_duration_sec", addQuotes(discount.getExpectedDurationSeconds()));
			affectation.put("discount_rate", addQuotes(discount.getValue()));
		}
		String insertQuery = dbConnection.generateInsertQuery("single_offer", affectation);
		return dbConnection.execUpdate(insertQuery);
	}

	public static long setSingleOfferAcquitted(SingleOffer offer, EnergyEvent requestEvent, boolean used) throws HandlingException {
		if (offer != null) {
			StringBuffer query = new StringBuffer("UPDATE single_offer SET ");
			// query.append("request_event_id = ").append(requestEvtId)
			query.append(" acquitted=1").append(" ,used=").append(used ? "1" : "0").append(" ,used_time=")
					.append(used ? OP_CURRENT_DATETIME : "NULL").append(" WHERE id=").append(offer.getId());
			return dbConnection.execUpdate(query.toString());
		}
		return 0;
	}

	public static long setSingleOfferAccepted(CompositeOffer globalOffer) throws HandlingException {
		if (globalOffer.hasSingleOffersIds()) {
			String offerids = globalOffer.getSingleOffersIdsStr();
			if (offerids.length() > 0) {
				StringBuffer query = new StringBuffer("UPDATE single_offer SET accepted=1, acceptance_time = ")
						.append(OP_CURRENT_DATETIME).append(" WHERE id IN (").append(offerids).append(")");
				return dbConnection.execUpdate(query.toString());
			}
		}
		return 0;
	}

	public static long setSingleOfferLinkedToContract(Contract contract, EnergyEvent startEvent) throws HandlingException {
		if (contract.hasSingleOffersIds()) {
			String offerids = contract.getSingleOffersIdsStr();
			if (offerids.length() > 0) {
				Long contractEventId = startEvent.getId();
				StringBuffer query = new StringBuffer();
				query.append("UPDATE single_offer SET contract_event_id=").append(addQuotes(contractEventId))
						.append(", contract_time = ").append(OP_CURRENT_DATETIME).append(" WHERE id IN (")
						.append(offerids).append(")");
				return dbConnection.execUpdate(query.toString());
			}
		}
		return 0;
	}

	public static long setSingleOfferCanceled(Contract contract, String comment) throws HandlingException {
		if (contract.hasSingleOffersIds()) {
			String offerids = contract.getSingleOffersIdsStr();
			if (offerids.length() > 0) {
				StringBuffer query = new StringBuffer();
				query.append("UPDATE single_offer SET log_cancel =").append(addSingleQuotes(comment))
						.append(", contract_time = ").append(OP_CURRENT_DATETIME).append(" WHERE id IN (")
						.append(offerids).append(")");
				return dbConnection.execUpdate(query.toString());
			}
		}
		return 0;
	}

	public static NodeTotal retrieveLastNodeTotal() throws HandlingException {
		List<Map<String, Object>> sqlResult = dbConnection
				.executeSelect("SELECT * FROM node_history ORDER BY node_history.date DESC LIMIT 0,1");
		if (sqlResult.size() > 0) {
			Map<String, Object> row = sqlResult.get(0);
			return auxRetrieveNodeTotal(row);
		}
		return null;
	}

	public static NodeTotal retrieveNodeTotalById(long id) throws HandlingException {
		List<Map<String, Object>> sqlResult = dbConnection
				.executeSelect("SELECT * FROM node_history WHERE id = " + addQuotes(id));
		if (sqlResult.size() > 0) {
			Map<String, Object> row = sqlResult.get(0);
			return auxRetrieveNodeTotal(row);
		}
		return null;
	}

	private static NodeTotal auxRetrieveNodeTotal(Map<String, Object> row) {
		NodeTotal nodeTotal = new NodeTotal();
		nodeTotal.setId(SapereUtil.getLongValue(row, "id"));
		nodeTotal.setIdLast(SapereUtil.getLongValue(row, "id_last"));
		nodeTotal.setTimeShiftMS(SapereUtil.getLongValue(row, "time_shift_ms"));
		nodeTotal.setConsumed(SapereUtil.getDoubleValue(row, "total_consumed", logger));
		nodeTotal.setProduced(SapereUtil.getDoubleValue(row, "total_produced", logger));
		nodeTotal.setRequested(SapereUtil.getDoubleValue(row, "total_requested", logger));
		nodeTotal.setAvailable(SapereUtil.getDoubleValue(row, "total_available", logger));
		nodeTotal.setMissing(SapereUtil.getDoubleValue(row, "total_missing", logger));
		nodeTotal.setProvided(SapereUtil.getDoubleValue(row, "total_provided", logger));
		nodeTotal.setProvidedMargin(SapereUtil.getDoubleValue(row, "total_margin", logger));
		nodeTotal.setMinRequestMissing(SapereUtil.getDoubleValue(row, "min_request_missing", logger));
		nodeTotal.setMaxWarningDuration(SapereUtil.getLongValue(row, "max_warning_duration"));
		if (row.get("max_warning_consumer") != null) {
			nodeTotal.setMaxWarningConsumer("" + row.get("max_warning_consumer"));
		}
		nodeTotal.setDate(SapereUtil.getDateValue(row, "date", logger));
		String node = "" + row.get("node");
		if (cashNodeLocations.containsKey(node)) {
			nodeTotal.setNodeLocation(cashNodeLocations.get(node));
		} else {
			logger.error("auxRetrieveNodeTotal : node " + node + " not found in nodeLocation cash");
		}
		nodeTotal.setDistance(SapereUtil.getIntValue(row, "distance"));
		return nodeTotal;
	}

	public static List<ExtendedNodeTotal> retrieveNodeTotalHistory(NodeHistoryFilter historyFilter) throws HandlingException {
		String agentFilter = historyFilter.getAgentName();
		return aux_retrieveNodeTotalHistory(null, agentFilter);
	}

	public static ExtendedNodeTotal retrieveNodeTotalHistoryById(Long historyId) throws HandlingException {
		List<ExtendedNodeTotal> listHistory = aux_retrieveNodeTotalHistory(historyId, null);
		if (listHistory.size() > 0) {
			return listHistory.get(0);
		}
		return null;
	}

	private static List<ExtendedNodeTotal> aux_retrieveNodeTotalHistory(Long filterHistoryId, String agentFilter) throws HandlingException {
		// correctHisto();
		Long sessionId = getSessionId();
		long beginTime = new Date().getTime();
		String node2 = addSingleQuotes(NodeManager.getNodeLocation().getName());
		List<ExtendedNodeTotal> result = new ArrayList<ExtendedNodeTotal>();
		StringBuffer sqlFilterHistoryId1 = new StringBuffer();
		if (filterHistoryId != null) {
			sqlFilterHistoryId1.append("histo_req.id_history=").append(addQuotes(filterHistoryId)).append(" AND ");
		}
		StringBuffer sqlFilterHistoryId2 = new StringBuffer();
		if (filterHistoryId != null) {
			sqlFilterHistoryId2.append("node_history.id=").append(addQuotes(filterHistoryId)).append(" AND ");
		}
		StringBuffer query2 = new StringBuffer();
		if(agentFilter != null && !"".equals(agentFilter)) {
			query2.append("");
			query2.append("SELECT ")
			.append(CR).append(" h.id, h.node, h.distance, h.id_last, h.id_next, h.date")
			.append(CR).append(",tmp_by_histo.*")
			.append(CR).append("")
			.append(CR).append("FROM(")
			.append(CR).append("	SELECT SUM(produced)  AS total_produced")
			.append(CR).append("		,SUM(requested) AS total_requested")
			.append(CR).append("		,SUM(consumed) AS total_consumed	")
			.append(CR).append("		,SUM(provided) AS total_provided")
			.append(CR).append("		,SUM(available) AS total_available")
			.append(CR).append("		,SUM(missing) AS total_missing	")
			.append(CR).append("		,SUM(margin) AS total_margin	")
			.append(CR).append("		,MAX(request_missing) AS min_request_missing")
			.append(CR).append("		,GROUP_CONCAT(Label3,  ', ') AS list_missing_requests")
			.append(CR).append("		,IIF(SUM(missing)>0, 1, 0) AS nb_missing_request")
			.append(CR).append("		,SUM(warning_missing) AS sum_warning_missing1")
			.append(CR).append("		,MAX(warning_consumer) AS max_warning_consumer")
			.append(CR).append("		,MAX(warning_duration) AS max_warning_duration")
			.append(CR).append("		,id_history")
			.append(CR).append("		FROM")
			.append(CR).append("				(")
			.append(CR).append("					SELECT")
			.append(CR).append("						histo_req.id_history")
			.append(CR).append("						,IIF(is_producer, req.power, 0) AS produced")
			.append(CR).append("						,IIF(is_request , req.power, 0) AS requested")
			.append(CR).append("						,IIF(is_request , histo_req.consumed, 0) AS consumed")
			.append(CR).append("						,IIF(is_producer, histo_req.provided, 0) AS provided")
			.append(CR).append("						,IIF(is_producer, req.power - histo_req.provided, 0) AS available")
			.append(CR).append("						,IIF(is_request , req.power - histo_req.consumed, 0) AS missing	")
			.append(CR).append("						,IIF(is_request , req.power - histo_req.consumed, 0) AS request_missing")
			.append(CR).append("						,IIF(is_producer, histo_req.provided_margin, 0) AS margin")
			.append(CR).append("						,IIF(missing>0 AND warning_duration > 0,req.agent_name,'') AS warning_consumer")
			.append(CR).append("						,req.power")
			.append(CR).append("						,histo_req.missing")
			.append(CR).append("						,histo_req.warning_duration")
			.append(CR).append("						,IIF(warning_duration > 0 , histo_req.missing, 0) AS warning_missing")
			.append(CR).append("						,IIF(missing > 0, req.agent_name || '#' || histo_req.missing || '#' || IIF(histo_req.has_warning_req,1,0) || '#' || histo_req.warning_duration, '') AS Label3")
			.append(CR).append("						,histo_req.warning_duration")
			.append(CR).append("					FROM link_history_active_event AS histo_req")
			.append(CR).append("					JOIN event AS req ON req.id = histo_req.id_event")
			.append(CR).append("					JOIN agent AS requester ON requester.id_session = req.id_session  AND requester.name = req.agent_name")
			.append(CR).append("					WHERE 1 ") // histo_req.node = ").append(node2)
			.append(CR).append("						AND req.agent_name = ").append(addSingleQuotes(agentFilter))
			.append(CR).append("				) AS tmp_evt")
			.append(CR).append("			GROUP BY tmp_evt.id_history")
			.append(CR).append("			")
			.append(CR).append("			")
			.append(CR).append("		) AS tmp_by_histo")
			.append(CR).append("		JOIN node_history h ON h.id = tmp_by_histo.id_history")
			;
		} else {
			query2.append(
					"SELECT h.id, h.id_last, h.id_next, h.date, h.total_produced, h.total_requested, h.total_consumed ")
				.append(CR).append(",h.total_available, h.total_missing, h.total_provided")
				.append(CR).append(", h.min_request_missing, h.total_margin, h.node, h.distance, h.max_warning_duration,h.max_warning_consumer")
				.append(CR).append(",IFNULL(TmpUnReqByHisto.nb_missing_request,0) AS nb_missing_request")
				.append(CR).append(",IFNULL(TmpUnReqByHisto.list_missing_requests,'') AS list_missing_requests")
				.append(CR).append(",IFNULL(TmpUnReqByHisto.sum_warning_missing1,0) AS sum_warning_missing1")
				.append(CR).append(" FROM node_history h")
				.append(CR).append(" LEFT JOIN (SELECT ")
				.append(CR).append("	     UnReq.id_history")
				.append(CR).append("		,Count(*) 	AS nb_missing_request")
				.append(CR).append(" 		,SUM(UnReq.warning_missing) AS sum_warning_missing1");
			if (sqlite) {
				query2.append(CR).append("		,GROUP_CONCAT(UnReq.Label3,  ', ') AS list_missing_requests");
			} else {
				query2.append(CR).append("		,GROUP_CONCAT(UnReq.Label3  ORDER BY UnReq.warning_duration DESC, UnReq.power SEPARATOR ', ') AS list_missing_requests");
			}
			query2.append(CR).append("	FROM (")
				.append(CR).append("		SELECT")
				.append(CR).append("			 histo_req.id_history")
				.append(CR).append("			,req.power")
				.append(CR).append("			,histo_req.missing")
				.append(CR).append("			,").append(OP_IF).append("(warning_duration > 0 , histo_req.missing, 0) AS warning_missing");
			if (sqlite) {
				query2.append(CR).append("			,req.agent_name || '#' || histo_req.missing || '#' || IIF(histo_req.has_warning_req,1,0) || '#' || histo_req.warning_duration AS Label3");
			} else {
				query2.append(CR).append("			,CONCAT(req.agent_name, '#', histo_req.missing, '#', IF(histo_req.has_warning_req,1,0) , '#' ,histo_req.warning_duration) AS Label3");
			}
			query2.append(CR).append("			,histo_req.warning_duration")
				.append(CR).append("		FROM link_history_active_event AS histo_req")
				.append(CR).append("		JOIN event AS req ON req.id = histo_req.id_event")
				.append(CR).append("		JOIN agent AS requester ON requester.id_session = req.id_session  AND requester.name = req.agent_name")
				.append(CR).append("		WHERE ").append(sqlFilterHistoryId1).append("requester.node =  ").append(node2)
				.append(CR).append("			AND is_request > 0 AND missing > 0")
				.append(CR).append("		) AS UnReq")
				.append(CR).append("	GROUP BY UnReq.id_history")
				.append(CR).append(") AS TmpUnReqByHisto ON TmpUnReqByHisto.id_history = h.id")
				.append(CR).append(" WHERE ").append(sqlFilterHistoryId2).append("h.id_session =  ").append(sessionId)
				.append(CR).append(" AND h.node =").append(node2)
				.append(CR).append(" ORDER BY h.date, h.ID ");
		}
		Date dateMin = null;
		Date dateMax = null;
		/*
		 * if(filterHistoryId==null && false) { dbConnection.setDebugLevel(10); }
		 */
		// dbConnection.setDebugLevel(10);
		List<Map<String, Object>> sqlResult = dbConnection.executeSelect(query2.toString());
		dbConnection.setDebugLevel(0);
		for (Map<String, Object> nextRow : sqlResult) {
			ExtendedNodeTotal nextTotal = new ExtendedNodeTotal();
			Date nextDate = SapereUtil.getDateValue(nextRow, "date", logger);
			if (dateMin == null) {
				dateMin = nextDate;
			}
			dateMax = nextDate;
			//nextTotal.setAgentName("" + nextRow.get("learning_agent"));
			nextTotal.setDate(SapereUtil.getDateValue(nextRow, "date", logger));
			String node = "" + nextRow.get("node");
			if(cashNodeLocations.containsKey(node)) {
				nextTotal.setNodeLocation(cashNodeLocations.get(node));
			} else {
				logger.error("aux_retrieveNodeTotalHistory : node " + node + " not found in nodeLocation cash");
			}
			nextTotal.setDistance(SapereUtil.getIntValue(nextRow, "distance"));
			nextTotal.setConsumed(SapereUtil.getDoubleValue(nextRow, "total_consumed", logger));
			nextTotal.setProduced(SapereUtil.getDoubleValue(nextRow, "total_produced", logger));
			nextTotal.setProvided(SapereUtil.getDoubleValue(nextRow, "total_provided", logger));
			nextTotal.setProvidedMargin(SapereUtil.getDoubleValue(nextRow, "total_margin", logger));
			nextTotal.setRequested(SapereUtil.getDoubleValue(nextRow, "total_requested", logger));
			nextTotal.setAvailable(SapereUtil.getDoubleValue(nextRow, "total_available", logger));
			nextTotal.setMissing(SapereUtil.getDoubleValue(nextRow, "total_missing", logger));
			nextTotal.setMinRequestMissing(SapereUtil.getDoubleValue(nextRow, "min_request_missing", logger));
			nextTotal.setId(SapereUtil.getLongValue(nextRow, "id"));
			nextTotal.setIdLast(SapereUtil.getLongValue(nextRow, "id_last"));
			nextTotal.setIdNext(SapereUtil.getLongValue(nextRow, "id_next"));
			if (nextTotal.getContractDoublons() != null && nextTotal.getContractDoublons().length() > 0) {
				logger.warning("DEBUG ContractDoublons : " + nextTotal.getContractDoublons());
			}
			// Retrieve missing requests
			nextTotal.setListConsumerMissingRequests(new ArrayList<>());
			List<String> missingRequests = SapereUtil.getListStrValue(nextRow, "list_missing_requests");
			// String test = "" + nextRow.get("list_missing_requests");
			for (String sMissingRequest : missingRequests) {
				String[] arrayMissingRequest = sMissingRequest.split("#");
				if (arrayMissingRequest.length == 4) {
					String issuer = arrayMissingRequest[0];
					Double power = Double.valueOf(arrayMissingRequest[1]);
					if (power < 0.00001) {
						logger.info("for debug : power = " + power);
					}
					Boolean hasWarning = "1".equals(arrayMissingRequest[2]);
					Integer warningDurationSec = Integer.valueOf(arrayMissingRequest[3]);
					MissingRequest nextMissingRequest = new MissingRequest(issuer, power, hasWarning,
							warningDurationSec);
					nextTotal.addMissingRequest(nextMissingRequest);
				}
			}
			if (sqlite) {
				nextTotal.computeSumWarningPower();
			} else {
				nextTotal.setSumWarningPower(SapereUtil.getDoubleValue(nextRow, "sum_warning_missing", logger));
			}
			nextTotal.setMinRequestMissing(SapereUtil.getDoubleValue(nextRow, "min_request_missing", logger));
			nextTotal.setMaxWarningDuration(SapereUtil.getLongValue(nextRow, "max_warning_duration"));
			if (nextRow.get("max_warning_consumer") != null) {
				nextTotal.setMaxWarningConsumer("" + nextRow.get("max_warning_consumer"));
			}
			nextTotal.setNbMissingRequests(SapereUtil.getLongValue(nextRow, "nb_missing_request"));
			result.add(nextTotal);
		}
		if (filterHistoryId == null) {
			// Retrieve events
			List<ExtendedEnergyEvent> listEvents = retrieveSessionEvents(sessionId, null, agentFilter);
			// Sort events by histoId
			Map<Long, List<EnergyEvent>> mapHistoEvents = new HashMap<Long, List<EnergyEvent>>();
			for (ExtendedEnergyEvent nextEvent : listEvents) {
				Long histoId = nextEvent.getHistoId();
				if (!mapHistoEvents.containsKey(histoId)) {
					mapHistoEvents.put(histoId, new ArrayList<EnergyEvent>());
				}
				(mapHistoEvents.get(histoId)).add(nextEvent);
			}
			// TODO : add historyId in retrieveOffers function
			// Retrieve offers
			OfferFilter offerFilter = new OfferFilter();
			offerFilter.setDateMin(dateMin);
			offerFilter.setDateMax(dateMax);
			if(agentFilter != null && !"".equals(agentFilter)) {
				offerFilter.setConsumerFilter(agentFilter);
			}
			List<SingleOffer> listOffers = retrieveOffers(offerFilter);
			// Sort offers by histoId
			Map<Long, List<SingleOffer>> mapHistoOffers = new HashMap<Long, List<SingleOffer>>();
			for (SingleOffer offer : listOffers) {
				Long histoId = offer.getHistoId();
				if (!mapHistoOffers.containsKey(histoId)) {
					mapHistoOffers.put(histoId, new ArrayList<SingleOffer>());
				}
				(mapHistoOffers.get(histoId)).add(offer);
			}
			for (ExtendedNodeTotal nextTotal : result) {
				Long histoId = nextTotal.getId();
				// Add events
				if (mapHistoEvents.containsKey(histoId)) {
					nextTotal.setLinkedEvents(mapHistoEvents.get(histoId));
				}
				// Add offers
				if (mapHistoOffers.containsKey(histoId)) {
					nextTotal.setOffers(mapHistoOffers.get(histoId));
				}
			}
			if (agentFilter != null && !"".equals(agentFilter)) {
				result = ExtendedNodeTotal.filterHasChanged(result, logger);
			}
		}
		long endTime = new Date().getTime();
		logger.info("aux_retrieveNodeTotalHistory filter histoID :" + filterHistoryId + " : load time MS = "
				+ (endTime - beginTime));
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

	public static void correctHisto() throws HandlingException {
		Long sessionId = getSessionId();
		// Correction of node_history.id_last
		dbConnection.execUpdate(
				"UPDATE node_history h  SET id_last = (SELECT h2.ID FROM node_history h2 WHERE h2.date < h.date AND h2.node = h.node ORDER BY h2.date DESC LIMIT 0,1)"
						+ " WHERE id_session=" + sessionId
						+ " AND id_last <> (SELECT h2.ID FROM node_history h2 WHERE h2.date < h.date AND h2.node = h.node ORDER BY h2.date DESC LIMIT 0,1)");
		// Correction of node_history.id_next
		dbConnection.execUpdate(
				"UPDATE node_history h  SET id_next = (SELECT h2.ID FROM node_history h2 WHERE h2.date > h.date AND h2.node = h.node ORDER BY h2.date LIMIT 0,1)"
						+ " WHERE id_session=" + sessionId
						+ " AND IFNULL(id_next,0) <> (SELECT h2.ID FROM node_history h2 WHERE h2.date > h.date AND h2.node = h.node ORDER BY h2.date LIMIT 0,1)");

	}

	public static boolean isOfferAcuitted(Long id) throws HandlingException {
		String sId = "" + id;
		Map<String, Object> row = dbConnection
				.executeSingleRowSelect("SELECT id,acquitted FROM single_offer WHERE id = " + addSingleQuotes(sId));
		if (row != null && row.get("acquitted") instanceof Boolean) {
			Boolean acquitted = (Boolean) row.get("acquitted");
			return acquitted;
		}
		return false;
	}

	public static void addLogOnOffer(Long offerId, String textToAdd) throws HandlingException {
		String sId = "" + offerId;
		StringBuffer query = new StringBuffer();
		query.append("UPDATE single_offer SET log2 = ");
		if (sqlite) {
			query.append("CONCAT(log2, ' ', ").append(addSingleQuotes(textToAdd)).append(")");
		} else {
			query.append("log2 || ' ' || ").append(addSingleQuotes(textToAdd)).append("");
		}
		query.append("WHERE id = ").append(addSingleQuotes(sId));
		dbConnection.execUpdate(query.toString());
	}

	public static List<SingleOffer> retrieveOffers(OfferFilter offerFilter) throws HandlingException {
		logger.info("retrieveOffers : offerFilter = " + offerFilter);
		List<SingleOffer> result = new ArrayList<SingleOffer>();
		if (offerFilter.getDateMin() == null) {
			return result;
		}
		String sDateMin = UtilDates.format_sql.format(offerFilter.getDateMin());
		String sDateMax = UtilDates.format_sql.format(offerFilter.getDateMax());
		StringBuffer sConsumerFilter = new StringBuffer("");
		if (offerFilter.getConsumerFilter() != null) {
			sConsumerFilter.append("single_offer.consumer_agent = ")
					.append(addSingleQuotes(offerFilter.getConsumerFilter()));
		} else {
			sConsumerFilter.append("1");
		}
		StringBuffer sProducerFilter = new StringBuffer("");
		if (offerFilter.getProducerFilter() != null) {
			sProducerFilter.append("single_offer.producer_agent = ")
					.append(addSingleQuotes(offerFilter.getProducerFilter()));
		} else {
			sProducerFilter.append("1");
		}
		StringBuffer sAaddedFilter = new StringBuffer("");
		if (offerFilter.getAdditionalFilter() != null) {
			sAaddedFilter.append(offerFilter.getAdditionalFilter());
		} else {
			sAaddedFilter.append("1");
		}
		long sessionId = getSessionId();
		StringBuffer query = new StringBuffer();
		query.append("SELECT single_offer.* ")
				.append(CR).append(" ,prodEvent.begin_date AS prod_begin_date")
				.append(CR).append(" ,prodEvent.expiry_date AS prod_expiry_date")
				.append(CR).append(" ,prodEvent.power AS prod_power")
				.append(CR).append(" ,prodEvent.power_min AS prod_power_min")
				.append(CR).append(" ,prodEvent.power_max AS prod_power_max")
				.append(CR).append(" ,producer.device_name AS prod_device_name")
				.append(CR).append(" ,producer.device_category AS prod_device_category")
				.append(CR).append(" ,producer.environmental_impact AS prod_env_impact")
				.append(CR).append(" ,prodEvent.time_shift_ms AS prod_time_shift_ms")
				.append(CR).append(" ,requestEvent.begin_date AS req_begin_date")
				.append(CR).append(" ,requestEvent.expiry_date AS  req_expiry_date")
				.append(CR).append(" ,requestEvent.is_complementary AS req_is_complementary")
				.append(CR).append(" ,requestEvent.agent_name AS req_agent")
				.append(CR).append(" ,requestEvent.power AS req_power")
				.append(CR).append(" ,requestEvent.power_min AS req_power_min")
				.append(CR).append(" ,requestEvent.power_max AS req_power_max")
				.append(CR).append(" ,consumer.device_name AS  req_device_name")
				.append(CR).append(" ,consumer.environmental_impact AS req_env_impact")
				.append(CR).append(" ,consumer.device_category AS  req_device_category")
				.append(CR).append(" ,requestEvent.time_shift_ms AS req_time_shift_ms")
				// .append(CR).append(" ,(select h.ID from node_history h where h.date <=
				// single_offer.creation_time order by h.date desc limit 0,1) as id_history")
				.append(CR).append(" FROM single_offer ")
				.append(CR).append(" LEFT JOIN event AS prodEvent ON prodEvent.id = single_offer.production_event_id")
				.append(CR).append(" LEFT JOIN agent AS producer ON producer.id_session = ").append(sessionId).append(" AND producer.name = prodEvent.agent_name")
				.append(CR).append(" LEFT JOIN event AS requestEvent ON requestEvent.id = single_offer.request_event_id")
				.append(CR).append(" LEFT JOIN agent AS consumer ON consumer.id_session = ").append(sessionId).append(" AND consumer.name = requestEvent.agent_name")
				.append(CR).append(" WHERE single_offer.date >= ").append(addSingleQuotes(sDateMin))
				.append(CR).append("    AND  single_offer.date < ").append(addSingleQuotes(sDateMax))
				.append(CR).append("    AND ").append(sConsumerFilter).append(" AND ").append(sProducerFilter)
				.append(CR).append("    AND ").append(sAaddedFilter)
				.append(CR).append(" ORDER BY single_offer.creation_time, single_offer.ID ");
		List<Map<String, Object>> rows = dbConnection.executeSelect(query.toString());
		for (Map<String, Object> row : rows) {
			EnergyRequest request = null;
			Boolean accepted = SapereUtil.getBooleantValue(row, "accepted");
			Boolean used = SapereUtil.getBooleantValue(row, "used");
			Boolean acquitted = SapereUtil.getBooleantValue(row, "acquitted");
			Double reqPower = SapereUtil.getDoubleValue(row, "req_power", logger);
			Double reqPowerMin = SapereUtil.getDoubleValue(row, "req_power_min", logger);
			Double reqPowerMax = SapereUtil.getDoubleValue(row, "req_power_max", logger);
			Date reqBeginDate = SapereUtil.getDateValue(row, "req_begin_date", logger);
			// Request is null if it comes from another node
			if (reqPower != null && reqBeginDate != null) {
				Date reqExpiryDate = SapereUtil.getDateValue(row, "req_expiry_date", logger);
				String sDeviceCat = "" + row.get("req_device_category");
				boolean resIsComplementary = SapereUtil.getBooleantValue(row, "req_is_complementary");
				long reqTimeShiftMS = SapereUtil.getLongValue(row, "req_time_shift_ms");
				DeviceCategory deviceCategory = DeviceCategory.getByName(sDeviceCat);
				EnvironmentalImpact envImpact = SapereUtil.getEnvironmentalImpactValue(row, "req_env_impact");
				Double tolerance = UtilDates.computeDurationMinutes(reqBeginDate, reqExpiryDate);
				//PricingTable pricingTable = new PricingTable(reqTimeShiftMS);
				DeviceProperties deviceProperties = new DeviceProperties("" + row.get("req_device_name"),
						deviceCategory, envImpact);
				int issuerDistance = 0;
				ProsumerProperties reqIssuerProperties = new ProsumerProperties(
						"" + row.get("req_agent")
						, NodeManager.getNodeLocation()
						, issuerDistance, reqTimeShiftMS, deviceProperties);
				PowerSlot resPowerSlot = new PowerSlot(reqPower, reqPowerMin, reqPowerMax);
				request = new EnergyRequest(reqIssuerProperties,
						resIsComplementary, resPowerSlot, reqBeginDate, reqExpiryDate, tolerance,
						PriorityLevel.LOW);
			}
			EnergySupply supply = null;
			Double power = SapereUtil.getDoubleValue(row, "power", logger);
			Double powerMin = SapereUtil.getDoubleValue(row, "power_min", logger);
			Double powerMax = SapereUtil.getDoubleValue(row, "power_max", logger);
			if (power != null && request != null) {
				String producerAgent = "" + row.get("producer_agent");
				String sDeviceCat = "" + row.get("prod_device_category");
				DeviceCategory deviceCategory = DeviceCategory.getByName(sDeviceCat);
				EnvironmentalImpact envImpact = SapereUtil.getEnvironmentalImpactValue(row, "prod_env_impact");
				long timeShiftMS = SapereUtil.getLongValue(row, "prod_time_shift_ms");
				PricingTable pricingTable = new PricingTable(timeShiftMS);
				DeviceProperties deviceProperties = new DeviceProperties("" + row.get("prod_device_name"),
						deviceCategory, envImpact);
				int issuerDistance = 0;
				Date beginDate = SapereUtil.getDateValue(row, "prod_begin_date", logger);
				Date endDate = SapereUtil.getDateValue(row, "prod_expiry_date", logger);
				if (beginDate != null) {
					// TODO : use cashNodeLocation to retrieve the right nodeLocation.
					ProsumerProperties producerProperties = new ProsumerProperties(producerAgent, NodeManager.getNodeLocation(), issuerDistance, timeShiftMS, deviceProperties);
					PowerSlot supplyPowerSlot = new PowerSlot(power, powerMin, powerMax);
					supply = new EnergySupply(producerProperties, false, supplyPowerSlot, beginDate, endDate, pricingTable);
					SingleOffer nextOffer = new SingleOffer(producerAgent, supply, 0, request);
					nextOffer.setDeadline(SapereUtil.getDateValue(row, "deadline", logger));
					if (row.get("used_time") != null) {
						nextOffer.setUsedTime(SapereUtil.getDateValue(row, "used_time", logger));
					}
					if (row.get("used_time") != null) {
						nextOffer.setUsedTime(SapereUtil.getDateValue(row, "used_time", logger));
					}
					if (row.get("acceptance_time") != null) {
						nextOffer.setAcceptanceTime(SapereUtil.getDateValue(row, "acceptance_time", logger));
					}
					if (row.get("contract_time") != null) {
						nextOffer.setContractTime(SapereUtil.getDateValue(row, "contract_time", logger));
					}
					nextOffer.setCreationTime(SapereUtil.getDateValue(row, "date", logger));
					boolean isComplementary = SapereUtil.getBooleantValue(row, "is_complementary");
					nextOffer.setIsComplementary(isComplementary);
					nextOffer.setUsed(used);
					nextOffer.setLog("" + row.get("log"));
					nextOffer.setLog2("" + row.get("log2"));
					nextOffer.setLogCancel("" + row.get("log_cancel"));
					nextOffer.setHistoId(SapereUtil.getLongValue(row, "id_history"));
					nextOffer.setAcquitted(acquitted);
					nextOffer.setAccepted(accepted);
					nextOffer.setId(SapereUtil.getLongValue(row, "id"));
					nextOffer.setContractEventId(SapereUtil.getLongValue(row, "contract_event_id"));
					result.add(nextOffer);
				}
			}
		}
		return result;
	}


	public void saveTValues(List<TimestampedValue> tvalues) throws HandlingException {
		// Check doublons
		List<Date> list_dates = new ArrayList<Date>();
		/*
		 * for(TimestampedValue tvalue : tvalues) { Date timestamp =
		 * tvalue.getTimestamp(); if(list_dates.contains(timestamp)) {
		 * logger.warning("saveTValues : doublon at " +
		 * UtilDates.format_sql.format(timestamp)); } } list_dates.clear();
		 */
		SimpleDateFormat format_sql2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		format_sql2.setTimeZone(TimeZone.getTimeZone("GMT"));
		StringBuffer queryInsert = new StringBuffer("INSERT INTO t_value(date, value) VALUES ");
		String sep = "";
		//int idx = 0;
		for (TimestampedValue tvalue : tvalues) {
			Date timestamp = tvalue.getTimestamp();
			if (list_dates.contains(timestamp)) {
				logger.warning("saveTValues : doublon at " + UtilDates.format_sql.format(timestamp));
			}
			queryInsert.append(sep).append("(").append(addSingleQuotes(format_sql2.format(timestamp))).append(",")
					.append(addQuotes(tvalue.getValue())).append(")");
			sep = ",";
			//idx++;
		}
		// queryInsert.append(" ON CONFLICT DO UPDATE SET value = 1000+value");
		Long id = dbConnection.execUpdate(queryInsert.toString());
		logger.info("saveTValues id:" + id);
	}

	private List<TimestampedValue> aux_constructTValues(List<Map<String, Object>> rows) {
		List<TimestampedValue> result = new ArrayList<TimestampedValue>();
		for (Map<String, Object> row : rows) {
			Date nextDate = SapereUtil.getDateValue(row, "date", logger);
			Double value = SapereUtil.getDoubleValue(row, "value", logger);
			TimestampedValue tvalue = new TimestampedValue(nextDate, value);
			result.add(tvalue);
		}
		return result;
	}

	public TimeSlot retrieveTValueInterval() throws HandlingException {
		Map<String, Object> row1 = dbConnection.executeSingleRowSelect("SELECT * FROM t_value ORDER BY date LIMIT 0,1");
		Map<String, Object> row2 = dbConnection.executeSingleRowSelect("SELECT * FROM t_value ORDER BY date DESC LIMIT 0,1");
		if(row1 != null && row2 != null) {
			Date date1 = SapereUtil.getDateValue(row1, "date", logger);
			Date date2 = SapereUtil.getDateValue(row2, "date", logger);
			return new TimeSlot(date1, date2);
		}
		return null;
	}

	public Map<Date, TimestampedValue> retrieveValuesByDates(List<Date> listDates) throws HandlingException {
		Map<Date, TimestampedValue> result = new HashMap<>();
		if (listDates != null && listDates.size() > 0) {
			String sep = "";
			StringBuffer sqlReq = new StringBuffer("SELECT * FROM t_value WHERE date IN (");
			for (Date nextDate : listDates) {
				String sDate = UtilDates.format_sql.format(nextDate);
				sqlReq.append(sep).append(addSingleQuotes(sDate));
				sep = ",";
			}
			sqlReq.append(")");
			List<Map<String, Object>> rows1 = dbConnection.executeSelect(sqlReq.toString());
			List<TimestampedValue> listValues = aux_constructTValues(rows1);
			for(TimestampedValue nextValue : listValues) {
				result.put(nextValue.getTimestamp(), nextValue);
			}
		}
		return result;
	}

	public List<TimestampedValue> retrieveLastValues(Date aDate, int nbValues) throws HandlingException {
		String sDate = UtilDates.format_sql.format(aDate);
		String request = "SELECT * FROM t_value WHERE date <= " + addSingleQuotes(sDate)
				+ " ORDER BY date DESC LIMIT 0," + (nbValues);
		List<Map<String, Object>> rows1 = dbConnection.executeSelect(request);
		List<TimestampedValue> result = aux_constructTValues(rows1);
		Collections.sort(result, new Comparator<TimestampedValue>() {
			@Override
			public int compare(TimestampedValue tvaluel, TimestampedValue tvalue2) {
				return tvaluel.getTimestamp().compareTo(tvalue2.getTimestamp());
			}
		});
		return result;
	}

	public List<TimestampedValue> retrieveNextTValues(Date aDate, int nbValues) throws HandlingException {
		String sDate = UtilDates.format_sql.format(aDate);
		String request = "SELECT * FROM t_value WHERE date >= " + addSingleQuotes(sDate) + " ORDER BY date LIMIT 0,"
				+ (nbValues - 1);
		List<Map<String, Object>> rows1 = dbConnection.executeSelect(request);
		return aux_constructTValues(rows1);
	}

	public static void logCreditUsedWH(Long contractEventId, Long requestEventId, double creditUsedWH, DiscountItem globalDiscount
			, TimeSlot usageSlot, Date expectedEndDate, double firstRateDiscount, double timeSpentSec, String stepLog) throws HandlingException {
		String sBeginUsage = UtilDates.format_sql.format(usageSlot.getBeginDate());
		String sEndUsage = UtilDates.format_sql.format(usageSlot.getEndDate());
		String sExpectedEndDate = UtilDates.format_sql.format(expectedEndDate);
		Map<String, String> affectation = new HashMap<>();
		affectation.put("id_contract_event", addQuotes(contractEventId));
		affectation.put("id_request_event", addQuotes(requestEventId));
		affectation.put("credit_granted_wh", addQuotes(globalDiscount == null ? 0.0 : globalDiscount.getCreditGrantedWH()));
		affectation.put("credit_used_wh", addQuotes(creditUsedWH));
		affectation.put("begin_date", addSingleQuotes(sBeginUsage));
		affectation.put("end_date", addSingleQuotes(sEndUsage));
		affectation.put("expected_end_date", addSingleQuotes(sExpectedEndDate));
		affectation.put("first_rate_discount", addQuotes(firstRateDiscount));
		affectation.put("time_spent_sec", addQuotes(timeSpentSec));
		affectation.put("time_spent_expected_sec", addQuotes(globalDiscount == null ? 0.0 : globalDiscount.getExpectedDurationSeconds()));
		affectation.put("log", addSingleQuotes(stepLog));
		//affectation.put("update_nb", "1");
		Map<String, String> confictAffectation = new HashMap<>();
		confictAffectation.put("credit_used_wh", addQuotes(creditUsedWH));
		//confictAffectation.put("begin_date", addSingleQuotes(sBeginUsage));
		confictAffectation.put("end_date", addSingleQuotes(sEndUsage));
		confictAffectation.put("time_spent_sec", addQuotes(timeSpentSec));
		confictAffectation.put("time_spent_expected_sec", addQuotes(globalDiscount == null ? 0.0 : globalDiscount.getExpectedDurationSeconds()));
		confictAffectation.put("log", addSingleQuotes(stepLog));
		confictAffectation.put("last_update", "datetime(CURRENT_TIMESTAMP, 'localtime')");
		confictAffectation.put("update_nb", "1 + update_nb");
		String queryLogInsert = dbConnection.generateInsertQuery("log_credit_usage", affectation, confictAffectation);
		logger.info("logCreditUsedWH : contractEventId = " + contractEventId + ", queryLogInsert = " + queryLogInsert);
		dbConnection.execUpdate(queryLogInsert);
	}

	public static Session registerSession(String sessionNumber) throws HandlingException {
		Map<String, String> affectation = new HashMap<String, String>();
		String sqlSessionNumber = SapereUtil.addSingleQuotes(sessionNumber);
		affectation.put("number", sqlSessionNumber);
		String insertQuery = dbConnection.generateInsertQuery("session", affectation, affectation);
		dbConnection.execUpdate(insertQuery);
		Map<String, Object>  row = dbConnection.executeSingleRowSelect("SELECT * FROM session WHERE number = " + sqlSessionNumber);
		Long id = SapereUtil.getLongValue(row, "id");
		Date creationDate = SapereUtil.getDateValue(row, "creation_time", logger);
		Session retrievedSession = new Session(id, sessionNumber, creationDate);
		logger.info("registerSession : retrievedSession = " + retrievedSession);
		return retrievedSession;
	}
}
