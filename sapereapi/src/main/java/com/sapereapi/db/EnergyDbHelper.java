package com.sapereapi.db;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.sapereapi.exception.DoublonException;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.energy.CompositeOffer;
import com.sapereapi.model.energy.Contract;
import com.sapereapi.model.energy.DeviceProperties;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.ExtendedEnergyEvent;
import com.sapereapi.model.energy.ExtendedNodeTotal;
import com.sapereapi.model.energy.MissingRequest;
import com.sapereapi.model.energy.NodeTotal;
import com.sapereapi.model.energy.PricingTable;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.model.energy.TimestampedValue;
import com.sapereapi.model.energy.input.OfferFilter;
import com.sapereapi.model.markov.MarkovTimeWindow;
import com.sapereapi.model.markov.TransitionMatrixKey;
import com.sapereapi.model.prediction.PredictionContext;
import com.sapereapi.model.referential.AgentType;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.EventObjectType;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.model.referential.WarningType;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.node.NodeConfig;
import eu.sapere.middleware.node.NodeManager;

public class EnergyDbHelper {
	private static DBConfig dbConfig = null;
	private static DBConnection dbConnection = null;
	// private static DBConnection dbConnectionClemapData = null;
	private static EnergyDbHelper instance = null;
	public final static String CR = System.getProperty("line.separator"); // Cariage return
	private static int debugLevel = 0;
	private static SapereLogger logger = SapereLogger.getInstance();
	// public final static String CLEMAPDATA_DBNAME = "clemap_data_light";
	private static Map<Long, NodeConfig> cashNodeConfig1 = new HashMap<>();
	private static Map<String, NodeConfig> cashNodeConfig2 = new HashMap<>();
	private static boolean sqlite = false;
	private static String OP_DATETIME = "NOW";
	private static String OP_CURRENT_DATETIME = "NOW()";
	private static String OP_TEMPORARY = "TEMPORARY";
	private static String OP_GREATEST = "GREATEST";
	private static String OP_LEAST = "LEAST";
	private static String OP_IF = "IF";

	public static void init(DBConfig aDBConfig) {
		dbConfig = aDBConfig;
		// initialise db connection
		instance = new EnergyDbHelper();
	}

	public static EnergyDbHelper getInstance() {
		if (instance == null) {
			instance = new EnergyDbHelper();
		}
		return instance;
	}

	public static Map<String, NodeConfig> getCashNodeConfig2() {
		return cashNodeConfig2;
	}

	public EnergyDbHelper() {
		// initialise db connection
		dbConnection = new DBConnection(dbConfig, logger);
		// dbConnectionClemapData = new
		// DBConnection("jdbc:mariadb://129.194.10.168/clemap_data", "import_clemap",
		// "sql2537");
		sqlite = dbConnection.useSQLLite();
		OP_DATETIME = (sqlite ? "datetime" : "NOW");
		OP_CURRENT_DATETIME = (sqlite ? "DATETIME('now', 'localtime')" : "NOW()");
		OP_TEMPORARY = (sqlite ? "" : "TEMPORARY");
		OP_GREATEST = (sqlite ? "MAX" : "GREATEST");
		OP_LEAST = (sqlite ? "MIN" : "LEAST");
		OP_IF = (sqlite ? "IIF" : "IF");
	}

	public static String getSessionId() {
		return DBConnection.getSessionId();
	}

	public static DBConnection getDbConnection() {
		return dbConnection;
	}

	public static NodeConfig registerNodeConfig(NodeConfig nodeConfig) {
		// StringBuffer query = new StringBuffer();
		Map<String, String> affectation = new HashMap<>();
		affectation.put("name", addSingleQuotes(nodeConfig.getName()));
		affectation.put("host", addSingleQuotes(nodeConfig.getHost()));
		affectation.put("main_port", addQuotes(nodeConfig.getMainPort()));
		affectation.put("rest_port", addQuotes(nodeConfig.getRestPort()));
		Map<String, String> confictAffectation = new HashMap<>();
		confictAffectation.put("creation_date", "creation_date");
		logger.info("registerNodeConfig : " + nodeConfig);
		String queryInsert = dbConnection.generateInsertQuery("node_config", affectation, confictAffectation);
		Long id = dbConnection.execUpdate(queryInsert);
		logger.info("registerNodeConfig : id of new config = " + id);
		StringBuffer sqlReq = new StringBuffer();
		sqlReq.append("SELECT * FROM node_config WHERE ").append("name = ")
				.append(addSingleQuotes(nodeConfig.getName())).append(" AND host = ")
				.append(addSingleQuotes(nodeConfig.getHost())).append(" AND main_port = ")
				.append(nodeConfig.getMainPort());
		Map<String, Object> row = dbConnection.executeSingleRowSelect(sqlReq.toString());
		NodeConfig result = aux_retrieveNodeConfig(row);
		return result;
	}

	private static NodeConfig aux_retrieveNodeConfig(Map<String, Object> row) {
		if (row == null) {
			return null;
		}
		NodeConfig result = new NodeConfig();
		result.setId(SapereUtil.getLongValue(row, "id"));
		result.setHost("" + row.get("host"));
		result.setName("" + row.get("name"));
		result.setMainPort(SapereUtil.getIntValue(row, "main_port"));
		result.setRestPort(SapereUtil.getIntValue(row, "rest_port"));
		updateCash(result);
		return result;
	}

	private static void updateCash(NodeConfig nodeConfig) {
		if (nodeConfig != null) {
			Long id = nodeConfig.getId();
			if (!cashNodeConfig1.containsKey(id)) {
				cashNodeConfig1.put(id, nodeConfig);
			}
			String name = nodeConfig.getName();
			if (!cashNodeConfig2.containsKey(name)) {
				cashNodeConfig2.put(name, nodeConfig);
			}
		}
	}

	public static NodeConfig retrieveNodeConfigById(Long id) {
		if (cashNodeConfig1.containsKey(id)) {
			return cashNodeConfig1.get(id);
		}
		Map<String, Object> row = dbConnection.executeSingleRowSelect("SELECT * FROM node_config WHERE id = " + id);
		NodeConfig result = aux_retrieveNodeConfig(row);
		return result;
	}

	public static NodeConfig retrieveNodeConfigByName(String name) {
		if (cashNodeConfig2.containsKey(name)) {
			return cashNodeConfig2.get(name);
		}
		Map<String, Object> row = dbConnection
				.executeSingleRowSelect("SELECT * FROM node_config WHERE name = " + addSingleQuotes(name));
		NodeConfig result = aux_retrieveNodeConfig(row);
		return result;
	}

	public static List<NodeConfig> retrieveAllNodeConfigs(List<Long> idsToExclude) {
		List<NodeConfig> result = new ArrayList<>();
		StringBuffer filter = new StringBuffer();// filter = "1";
		if (idsToExclude.size() > 0) {
			filter.append("id NOT IN (");
			String sep = "";
			for (Long nextId : idsToExclude) {
				filter.append(sep).append(nextId);
				sep = ",";
			}
			filter.append(")");
		} else {
			filter.append("1");
		}
		List<Map<String, Object>> rows = dbConnection
				.executeSelect("SELECT * FROM node_config WHERE " + filter.toString());
		for (Map<String, Object> nextRow : rows) {
			NodeConfig nodeConfig = aux_retrieveNodeConfig(nextRow);
			result.add(nodeConfig);
		}
		return result;
	}

	public static List<NodeConfig> retrieveNeighbourNodeConfigs(NodeContext nodeContext) {
		Long contextId = nodeContext.getId();
		List<NodeConfig> result = new ArrayList<>();
		List<Map<String, Object>> rows = dbConnection.executeSelect("SELECT * FROM context_neighbour " + CR
				+ " JOIN node_config ON node_config.id = context_neighbour.id_node_config" + CR
				+ " WHERE context_neighbour.id_context = " + contextId);
		for (Map<String, Object> nextRow : rows) {
			NodeConfig nodeConfig = aux_retrieveNodeConfig(nextRow);
			result.add(nodeConfig);
		}
		return result;
	}

	public static NodeContext updateNeighbours(NodeContext nodeContext, List<Long> listNeighbourConfigIds) {
		Long contextId = nodeContext.getId();
		StringBuffer query = new StringBuffer();
		query.append("DELETE ").append(sqlite ? "" : " context_neighbour")
				.append(" FROM context_neighbour WHERE id_context = ").append(contextId).append("");
		query.append(DBConnection.getReqSeparator2());
		if (listNeighbourConfigIds.size() > 0) {
			query.append(CR).append("INSERT INTO context_neighbour(id_context, id_node_config) VALUES").append(CR);
			String sep = "";
			for (Long nextId : listNeighbourConfigIds) {
				query.append(sep).append("(").append(contextId).append(",").append(nextId).append(")");
				sep = ",";
			}
		}
		Long result = dbConnection.execUpdate(query.toString());
		List<NodeConfig> neighbourConfig = retrieveNeighbourNodeConfigs(nodeContext);
		nodeContext.resetNeighbourNodeConfig();
		for (NodeConfig nodeConfig : neighbourConfig) {
			nodeContext.addNeighbourNodeConfig(nodeConfig);
		}
		return nodeContext;
	}

	public static NodeContext registerContext(NodeContext nodeContext) {
		// TODO : register node config into node_location
		Map<String, String> affectation = new HashMap<>();
		affectation.put("location", addSingleQuotes(nodeContext.getMainServiceAddress()));
		affectation.put("id_node_config", "" + nodeContext.getNodeConfig().getId());
		affectation.put("scenario", addSingleQuotes(nodeContext.getScenario()));
		affectation.put("last_id_session", addSingleQuotes(nodeContext.getSessionId()));
		affectation.put("last_time_shift_ms", "" + nodeContext.getTimeShiftMS());

		Map<String, String> confilctAffectation = new HashMap<>();
		confilctAffectation.put("last_id_session", addSingleQuotes(nodeContext.getSessionId()));
		confilctAffectation.put("last_time_shift_ms", "" + nodeContext.getTimeShiftMS());
		confilctAffectation.put("id_node_config", "" + nodeContext.getNodeConfig().getId());
		// TODO : use query1
		String query1 = dbConnection.generateInsertQuery("context", affectation, confilctAffectation);
		/*
		 * StringBuffer query = new StringBuffer();
		 * query.append("INSERT INTO context SET")
		 * .append(CR).append("  location = ").append(addSingleQuotes(nodeContext.
		 * getMainServiceAddress()))
		 * .append(CR).append(", id_node_config = ").append(nodeContext.getNodeConfig().
		 * getId())
		 * .append(CR).append(", scenario = ").append(addSingleQuotes(nodeContext.
		 * getScenario()))
		 * .append(CR).append(", last_id_session = ").append(addSingleQuotes(nodeContext
		 * .getSessionId()))
		 * .append(CR).append(", last_time_shift_ms = ").append(nodeContext.
		 * getTimeShiftMS()) .append(CR).append(" ON DUPLICATE KEY UPDATE ")
		 * .append(CR).append(" last_id_session = ").append(addSingleQuotes(nodeContext.
		 * getSessionId()))
		 * .append(CR).append(", last_time_shift_ms = ").append(nodeContext.
		 * getTimeShiftMS())
		 * .append(CR).append(", id_node_config = ").append(nodeContext.getNodeConfig().
		 * getId()) ;
		 */
		Long contextId = dbConnection.execUpdate(query1);
		nodeContext.setId(contextId);
		// add links to the neighbour nodes
		nodeContext.resetNeighbourNodeConfig();
		List<NodeConfig> neighbourConfig = retrieveNeighbourNodeConfigs(nodeContext);
		for (NodeConfig nodeConfig : neighbourConfig) {
			nodeContext.addNeighbourNodeConfig(nodeConfig);
		}
		return nodeContext;
	}

	public static EnergyEvent registerEvent(EnergyEvent event) throws DoublonException {
		return registerEvent(event, null);
	}

	private static EnergyEvent auxGetEvent(Map<String, Object> row) {
		String agent = "" + row.get("agent");
		// String location = ""+ row.get("location");
		String typeLabel = "" + row.get("type");
		EventType type = EventType.getByLabel(typeLabel);
		String categoryLabel = "" + row.get("device_category");
		DeviceCategory category = DeviceCategory.getByName(categoryLabel);
		EnvironmentalImpact envImpact = SapereUtil.getEnvironmentalImpactValue(row, "environmental_impact");
		Double power = SapereUtil.getDoubleValue(row, "power");
		Double powerMin = SapereUtil.getDoubleValue(row, "power_min");
		Double powerMax = SapereUtil.getDoubleValue(row, "power_max");
		String comment = "" + row.get("comment");
		long timeShiftMS = SapereUtil.getLongValue(row, "time_shift_ms");
		Long nodeConfigId = SapereUtil.getLongValue(row, "id_node_config");
		NodeConfig nodeLocation = retrieveNodeConfigById(nodeConfigId);
		int issuerDistance = SapereUtil.getIntValue(row, "distance");
		PricingTable pricingTable = new PricingTable(timeShiftMS);
		boolean isComplementary = SapereUtil.getBooleantValue(row, "is_complementary");
		boolean isProducer = EventObjectType.PRODUCTION.equals(type.getObjectType());
		DeviceProperties deviceProperties = new DeviceProperties("" + row.get("device_name"), category, envImpact,
				isProducer);
		EnergyEvent result = new EnergyEvent(type, agent, nodeLocation, issuerDistance, isComplementary, power,
				powerMin, powerMax, SapereUtil.getDateValue(row, "begin_date", logger),
				SapereUtil.getDateValue(row, "expiry_date", logger), deviceProperties, pricingTable, comment,
				timeShiftMS);
		result.setId(SapereUtil.getLongValue(row, "id"));
		Double powerUpdate = SapereUtil.getDoubleValue(row, "power_update");
		Double powerMinUpdate = SapereUtil.getDoubleValue(row, "power_min_update");
		Double powerMaxUpdate = SapereUtil.getDoubleValue(row, "power_max_update");
		result.setPowerUpates(powerUpdate, powerMinUpdate, powerMaxUpdate);
		result.setHistoId(SapereUtil.getLongValue(row, "id_histo"));
		if (row.get("warning_type") != null) {
			result.setWarningType(WarningType.getByLabel("" + row.get("warning_type")));
		}
		// result.setIsComplementary();
		return result;
	}

	public static List<ExtendedEnergyEvent> retrieveLastSessionEvents() {
		List<ExtendedEnergyEvent> result = new ArrayList<ExtendedEnergyEvent>();
		Map<String, Object> row = dbConnection
				.executeSingleRowSelect("SELECT id_session FROM event ORDER BY ID DESC LIMIT 0,1)");
		if (row != null) {
			String sessionId = "" + row.get("id_session");
			return retrieveSessionEvents(sessionId, true);
		}
		return result;
	}

	public static List<ExtendedEnergyEvent> retrieveCurrentSessionEvents() {
		String sessionId = getSessionId();
		return retrieveSessionEvents(sessionId, true);
	}

	public static List<ExtendedEnergyEvent> retrieveSessionEvents(String sessionId, boolean onlyActive) {
		List<ExtendedEnergyEvent> result = new ArrayList<ExtendedEnergyEvent>();
		StringBuffer query = new StringBuffer("");
		query.append(CR).append("SELECT event.*")
				// .append(CR).append(" ,GET_EFFECTIVE_END_DATE(event.id) as
				// effective_end_date")
				.append(CR).append(" ,").append(OP_LEAST).append("(event.expiry_date").append(CR)
				.append(" 		, IFNULL(event.interruption_date,'3000-01-01')").append(CR)
				.append(" 		, IFNULL(event.cancel_date,'3000-01-01')) AS  effective_end_date").append(CR)
				.append(" ,").append(OP_IF).append("(event.Type IN ('CONTRACT', 'CONTRACT_UPDATE') ").append(CR)
				.append("	,(SELECT consumer.agent_name FROM link_event_agent AS consumer ").append(CR)
				.append("			WHERE consumer.id_event = event.id AND consumer.agent_type = 'Consumer' LIMIT 0,1)")
				.append(CR).append("	, NULL) AS linked_consumer").append(CR).append(" ,").append(OP_IF)
				.append("(event.Type IN ('CONTRACT', 'CONTRACT_UPDATE') ").append(CR)
				.append("	,(SELECT consumer.agent_id_node_config FROM link_event_agent AS consumer ").append(CR)
				.append("			WHERE consumer.id_event = event.id AND consumer.agent_type = 'Consumer' LIMIT 0,1)")
				.append(CR).append("	, NULL) AS linked_consumer_location").append(CR).append(" FROM event")
				.append(CR).append(" WHERE event.id_session = ").append(addSingleQuotes(sessionId));
		if (onlyActive) {
			query.append(CR).append(" AND expiry_date > ").append(OP_CURRENT_DATETIME).append(CR)
					.append(" AND IFNULL(interruption_date,'3000-01-01')  > ").append(OP_CURRENT_DATETIME).append(CR)
					.append(" AND IFNULL(cancel_date,'3000-01-01')  > ").append(OP_CURRENT_DATETIME);
			// queryappend(CR).append(" AND GET_EFFECTIVE_END_DATE(event.id) >
			// ").append(OP_CURRENT_DATETIME);
		}
		// query.append(CR).append(" ORDER BY event.type, 1*event.agent,
		// event.creation_time ");
		query.append(CR).append(" ORDER BY id_histo, event.agent, event.creation_time ");
		dbConnection.setDebugLevel(0);
		List<Map<String, Object>> rows = dbConnection.executeSelect(query.toString());
		dbConnection.setDebugLevel(0);
		for (Map<String, Object> row : rows) {
			EnergyEvent nextEvent = auxGetEvent(row);
			PricingTable pricingTable = new PricingTable(nextEvent.getTimeShiftMS());
			ExtendedEnergyEvent nextEvent2 = new ExtendedEnergyEvent(nextEvent.getType(), nextEvent.getIssuer(),
					nextEvent.getIssuerLocation(), nextEvent.getIssuerDistance(), nextEvent.isComplementary(),
					nextEvent.getPower(), nextEvent.getPowerMin(), nextEvent.getPowerMax(), nextEvent.getBeginDate(),
					nextEvent.getEndDate(), nextEvent.getDeviceProperties(), pricingTable, nextEvent.getComment(),
					nextEvent.getTimeShiftMS());
			nextEvent2.setEffectiveEndDate(SapereUtil.getDateValue(row, "effective_end_date", logger));
			// nextEvent2.setIsComplementary(nextEvent.isComplementary());
			nextEvent2.setHistoId(nextEvent.getHistoId());
			nextEvent2.setWarningType(nextEvent.getWarningType());
			// nextEvent2.setIsComplementary(nextEvent.isComplementary());
			nextEvent2.setPowerUpateSlot(nextEvent.getPowerUpateSlot());
			if (row.get("linked_consumer") != null) {
				nextEvent2.setLinkedConsumer("" + row.get("linked_consumer"));
			}
			if (row.get("linked_consumer_location") != null) {
				Long idLinkedConsumerLocation = SapereUtil.getLongValue(row, "linked_consumer_location");
				NodeConfig linkedConsumerLocation = retrieveNodeConfigById(idLinkedConsumerLocation);
				nextEvent2.setLinkedConsumerLocation(linkedConsumerLocation);
			}
			result.add(nextEvent2);
		}
		return result;
	}

	public static EnergyEvent retrieveEventById(Long id) {
		List<Map<String, Object>> rows = dbConnection.executeSelect("SELECT * FROM event WHERE id= " + id);
		if (rows.size() > 0) {
			Map<String, Object> row = rows.get(0);
			EnergyEvent result = auxGetEvent(row);
			return result;
		}
		return null;
	}

	public static EnergyEvent retrieveEvent(EventType evtType, String agentName, Boolean isComplementary,
			Date beginDate) {
		String sBeginDate = UtilDates.format_sql.format(beginDate);
		StringBuffer query = new StringBuffer();
		query.append("SELECT * FROM event WHERE ").append(" begin_date = ").append(addSingleQuotes(sBeginDate))
				.append(" AND type = ").append(addSingleQuotes("" + evtType)).append(" AND agent = ")
				.append(addSingleQuotes(agentName)).append(" AND is_complementary = ")
				.append((isComplementary ? "1" : "0"));
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

	public static EnergyEvent registerEvent2(EnergyEvent event) {
		return registerEvent2(event, null);
	}

	public static EnergyEvent registerEvent2(EnergyEvent event, Contract contract) {
		EnergyEvent result = null;
		try {
			result = EnergyDbHelper.registerEvent(event, contract);
		} catch (DoublonException e) {
			logger.error(e);
			result = retrieveEvent(event.getType(), event.getIssuer(), event.isComplementary(), event.getBeginDate());
		}
		return result;
	}

	public static EnergyEvent registerEvent(EnergyEvent event, Contract contract) throws DoublonException {
		String sBeginDate = UtilDates.format_sql.format(event.getBeginDate());
		String sExpiryDate = UtilDates.format_sql.format(event.getEndDate());
		if (event.getIssuerLocation() == null) {
			throw new RuntimeException("registerEvent : location is not set on event " + event.toString());
		} else if (!event.checkLocationId()) {
			throw new RuntimeException("registerEvent : location as no id on event " + event.toString());
		} else if (contract != null && !contract.checkLocationId()) {
			throw new RuntimeException("registerEvent : location as no id on contract " + contract.toString());
		}
		// Check duplicate event
		EnergyEvent doublonCheck = retrieveEvent(event.getType(), event.getIssuer(), event.isComplementary(),
				event.getBeginDate());
		if (doublonCheck != null) {
			throw new DoublonException("Event doublon : The client program tries to insert the following event twice : "
					+ event.toString());
		}
		String sessionId = getSessionId();
		Map<String, String> affectation = new HashMap<>();
		affectation.put("begin_date", addSingleQuotes(sBeginDate));
		affectation.put("expiry_date", addSingleQuotes(sExpiryDate));
		affectation.put("id_session", addSingleQuotes(sessionId));
		affectation.put("type", addSingleQuotes("" + event.getType()));
		affectation.put("object_type", addSingleQuotes("" + event.getType().getObjectType()));
		affectation.put("main_category", addSingleQuotes("" + event.getType().getMainCategory()));
		affectation.put("warning_type",
				event.getWarningType() == null ? "''" : addSingleQuotes(event.getWarningType().getLabel()));
		affectation.put("power", addSingleQuotes("" + event.getPower()));
		affectation.put("power_min", addQuotes(event.getPowerMin()));
		affectation.put("power_max", addQuotes(event.getPowerMax()));
		affectation.put("power_update", addSingleQuotes("" + event.getPowerUpdate()));
		affectation.put("power_min_update", addQuotes(event.getPowerMinUpdate()));
		affectation.put("power_max_update", addQuotes(event.getPowerMaxUpdate()));
		affectation.put("time_shift_ms", addQuotes(event.getTimeShiftMS()));
		affectation.put("agent", addSingleQuotes(event.getIssuer()));
		affectation.put("location", addSingleQuotes(event.getIssuerLocation().getMainServiceAddress()));
		affectation.put("id_node_config", "" + event.getIssuerLocation().getId());
		affectation.put("distance", addQuotes(event.getIssuerDistance()));
		affectation.put("device_name", addSingleQuotes(event.getDeviceProperties().getName()));
		affectation.put("device_category", addSingleQuotes(event.getDeviceProperties().getCategory().name()));// getLabel
		affectation.put("environmental_impact",
				addQuotes(event.getDeviceProperties().getEnvironmentalImpact().getLevel()));
		affectation.put("is_cancel", event.getType().getIsCancel() ? "1" : "0");
		affectation.put("is_ending", event.getType().getIsEnding() ? "1" : "0");
		affectation.put("id_origin", event.getOriginEvent() == null ? "NULL" : "" + event.getOriginEvent().getId());
		affectation.put("is_complementary", event.isComplementary() ? "1" : "0");
		affectation.put("comment", addSingleQuotes(event.getComment()));
		String query = dbConnection.generateInsertQuery("event", affectation);
		Long eventId = dbConnection.execUpdate(query.toString());
		EnergyEvent result = event;
		result.setId(eventId);
		if (contract != null) {
			if (EventType.CONTRACT_START.equals(event.getType()) || EventType.CONTRACT_UPDATE.equals(event.getType())) {
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
				.append(" agent = ").append(addSingleQuotes(event.getIssuer())).append(" AND object_type = ")
				.append(addSingleQuotes(shortType)).append(" AND begin_date < ").append(addSingleQuotes(sBeginDate))
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

	public static void registerLinksEventAgent(Long eventId, Contract contract) {
		// Consumer agent
		Double globalPower = contract.getPower();
		Double globalPowerMax = contract.getPowerMax();
		Double globalPowerMin = contract.getPowerMin();
		Map<String, String> affectation = new HashMap<>();
		affectation.put("id_event", addQuotes(eventId));
		affectation.put("agent_type", addSingleQuotes(AgentType.CONSUMER.getLabel()));
		affectation.put("agent_name", addSingleQuotes(contract.getConsumerAgent()));
		affectation.put("agent_location", addSingleQuotes(contract.getConsumerLocation().getMainServiceAddress()));
		affectation.put("agent_id_node_config", addQuotes(contract.getConsumerLocation().getId()));
		affectation.put("power", addQuotes(globalPower));
		affectation.put("power_min", addQuotes(globalPowerMin));
		affectation.put("power_max", addQuotes(globalPowerMax));
		;
		String query1 = dbConnection.generateInsertQuery("link_event_agent", affectation);
		dbConnection.execUpdate(query1);
		// Producer agents
		for (String producer : contract.getProducerAgents()) {
			Double power = contract.getPowerFromAgent(producer);
			Double powerMin = contract.getPowerMinFromAgent(producer);
			Double powerMax = contract.getPowerMaxFromAgent(producer);
			NodeConfig nodeConfig = contract.getLocationFromAgent(producer);
			Map<String, String> affectation2 = new HashMap<>();
			affectation2.put("id_event", addQuotes(eventId));
			affectation2.put("agent_type", addSingleQuotes(AgentType.PRODUCER.getLabel()));
			affectation2.put("agent_name", addSingleQuotes(producer));
			affectation2.put("agent_location", addSingleQuotes(nodeConfig.getMainServiceAddress()));
			affectation2.put("agent_id_node_config", addQuotes(nodeConfig.getId()));
			affectation2.put("power", addQuotes(power));
			affectation2.put("power_min", addQuotes(powerMin));
			affectation2.put("power_max", addQuotes(powerMax));
			String query2 = dbConnection.generateInsertQuery("link_event_agent", affectation2);
			dbConnection.execUpdate(query2);
		}
		// Check gap
		if (eventId > 0) {
			Map<String, Object> rowCheckGap = dbConnection.executeSingleRowSelect(
					"SELECT IFNULL(SUM(lea.power),0) AS provided FROM link_event_agent AS lea WHERE lea.id_event = "
							+ addQuotes(eventId) + " AND lea.agent_type = 'Producer'");
			double provided = SapereUtil.getDoubleValue(rowCheckGap, "provided");
			if (Math.abs(globalPower - provided) > 0.001) {
				double contractGap = contract.computeGap();
				logger.warning("Gap found in new contract " + rowCheckGap + " contractGap = " + contractGap);
			}
		}
	}

	public static NodeTotal generateNodeTotal(Date computeDate, Long timeShiftMS, EnergyEvent linkedEvent, String url,
			String agentName, NodeConfig nodeConfig) {
		String location = nodeConfig.getMainServiceAddress();
		int distance = NodeManager.instance().getDistance(nodeConfig);
		// boolean local = (distance==0);
		NodeTotal nodeTotal = new NodeTotal();
		nodeTotal.setTimeShiftMS(timeShiftMS);
		nodeTotal.setDate(computeDate);
		nodeTotal.setNodeConfig(nodeConfig);
		nodeTotal.setDistance(distance);
		// nodeTotal.setIdLast(idLast);
		nodeTotal.setAgentName(agentName);
		String sComputeDate = UtilDates.format_sql.format(computeDate);
		String quotedComputeDate = addSingleQuotes(sComputeDate);
		// String distanceFilter = local ? "event.distance=0" : "event.distance > 0";
		boolean debugTmpTables = false; // debug mode : to replace TEMPORARY tables by tables
		String reqSeparator2 = DBConnection.getReqSeparator2();
		StringBuffer query = new StringBuffer();
		query.append("DROP ").append(OP_TEMPORARY).append(" TABLE IF EXISTS TmpEvent");
		query.append(reqSeparator2);
		if (!sqlite) {
			query.append(CR).append("DROP TABLE IF EXISTS TmpEvent");
			query.append(reqSeparator2);
			query.append(CR).append("DROP ").append((debugTmpTables ? "" : "TEMPORARY "))
					.append("TABLE IF EXISTS TmpEvent");
			query.append(reqSeparator2);
		}
		query.append(CR).append("CREATE TEMPORARY TABLE TmpEvent(").append(CR).append("	 id						")
				.append((sqlite ? "INTEGER " : "INT(11) ")).append((sqlite ? "" : "UNSIGNED")).append(" NOT NULL ")
				.append((sqlite ? " PRIMARY KEY AUTOINCREMENT" : " AUTO_INCREMENT")).append(CR)
				.append("	,begin_date				DATETIME").append(CR)
				.append("	,id_origin				INT(11) DEFAULT NULL").append(CR)
				.append("	,effective_end_date 	DATETIME").append(CR)
				.append("	,type 					VARCHAR(32)").append(CR)
				.append("	,agent 					VARCHAR(100) NOT NULL").append(CR)
				.append("	,is_complementary 		BIT(1) NOT NULL DEFAULT ").append((sqlite ? "0" : "b'0'"))
				.append(CR).append("	,location 				VARCHAR(32) NOT NULL DEFAULT ''").append(CR)
				.append("	,distance 				TINYINT UNSIGNED NOT NULL DEFAULT 0.0").append(CR)
				.append("	,is_selected 			BIT(1) NOT NULL DEFAULT ").append((sqlite ? "0" : "b'0'"))
				.append(CR).append("	,is_selected_location	BIT(1) NOT NULL DEFAULT ")
				.append((sqlite ? "0" : "b'0'")).append(CR)
				.append("	,is_request 			BIT(1) NOT NULL DEFAULT ").append((sqlite ? "0" : "b'0'"))
				.append(CR).append("	,is_producer 			BIT(1) NOT NULL DEFAULT ")
				.append((sqlite ? "0" : "b'0'")).append(CR)
				.append("	,is_contract 			BIT(1) NOT NULL DEFAULT ").append((sqlite ? "0" : "b'0'"))
				.append(CR).append("	,is_location_ok			BIT(1) NOT NULL DEFAULT ")
				.append((sqlite ? "0" : "b'0'")).append(CR).append("	,id_contract_evt 		INT(11) NULL")
				.append(CR).append("	,power					DECIMAL(15,3) NOT NULL DEFAULT 0.0").append(CR)
				.append("	,power_margin			DECIMAL(15,3) NOT NULL DEFAULT 0.0").append(CR)
				.append("	,provided 				DECIMAL(15,3) NOT NULL DEFAULT 0.0").append(CR)
				.append("	,provided_margin		DECIMAL(15,3) NOT NULL DEFAULT 0.0").append(CR)
				.append("	,consumed 				DECIMAL(15,3) NOT NULL DEFAULT 0.0").append(CR)
				.append("	,missing 				DECIMAL(15,3) NOT NULL DEFAULT 0.0").append(CR)
				.append((sqlite ? "" : "  ,PRIMARY KEY (`id`)")).append(CR).append("	)");
		query.append(reqSeparator2).append(CR).append(
				"INSERT INTO TmpEvent(id, begin_date, id_origin,effective_end_date,type,agent,is_complementary,location,power,distance")
				.append(CR).append(",power_margin,is_request,is_producer,is_contract,is_location_ok)").append(CR)
				.append("	SELECT id, begin_date, id_origin").append(CR).append(" 		,")
				.append((sqlite ? "MIN" : "LEAST")).append("(event.expiry_date").append(CR)
				.append("  			,IFNULL(event.interruption_date,'3000-01-01')").append(CR)
				.append("  			,IFNULL(event.cancel_date,'3000-01-01')) 	AS  effective_end_date").append(CR)
				.append("		,type,agent,is_complementary,location,power,distance").append(CR)
				.append("		,(power_max - power)							AS power_margin").append(CR)
				.append("		,object_type = 'REQUEST'						AS is_request").append(CR)
				.append("		,object_type = 'PRODUCTION'						AS is_producer").append(CR)
				.append("		,object_type = 'CONTRACT'				 		AS is_contract").append(CR)
				.append(" 		,location=").append(addSingleQuotes(location)).append(" 		AS is_location_ok")
				.append(CR).append("	FROM event").append(CR)
				.append("	WHERE NOT event.is_ending AND IFNULL(event.cancel_date,'3000-01-01') > ")
				.append(quotedComputeDate);
		query.append(reqSeparator2);
		query.append(CR).append("	UPDATE TmpEvent SET is_selected = 1 WHERE begin_date<=").append(quotedComputeDate)
				.append(" AND effective_end_date > ").append(quotedComputeDate);
		query.append(reqSeparator2);
		query.append(CR).append("	UPDATE TmpEvent SET is_selected_location = is_selected AND is_location_ok");
		query.append(reqSeparator2);
		query.append(CR).append("DROP ").append(OP_TEMPORARY).append(" TABLE IF EXISTS TmpRequestEvent");
		query.append(reqSeparator2);
		query.append(CR).append("CREATE TEMPORARY TABLE TmpRequestEvent AS").append(CR)
				.append("  SELECT TmpEvent.id, TmpEvent.agent AS consumer, power, is_location_ok").append(CR)
				.append("	  FROM TmpEvent ").append(CR).append("  WHERE is_selected AND is_request");
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
		query.append(CR).append("DROP ").append(OP_TEMPORARY).append(" TABLE IF EXISTS TmpContractEvent");
		query.append(reqSeparator2);
		query.append(CR).append("CREATE TEMPORARY TABLE TmpContractEvent AS").append(CR).append(
				" 	SELECT TmpEvent.id, consumer.agent_name AS consumer, TmpEvent.is_location_ok, TmpEvent.power")
				.append(CR).append(" 	FROM TmpEvent ").append(CR)
				.append(" 	JOIN link_event_agent AS consumer ON consumer.id_event = TmpEvent.id AND consumer.agent_type='Consumer'")
				.append(CR).append("    JOIN TmpRequestEvent ON TmpRequestEvent.consumer = consumer.agent_name ")
				.append(CR).append("  	WHERE is_selected AND is_contract");
		if (sqlite) {
			query.append(reqSeparator2);
			query.append(CR).append("UPDATE TmpEvent SET is_selected_location = 0, is_selected = 0").append(CR)
					.append("	WHERE TmpEvent.is_selected AND is_contract  ").append(CR)
					.append(" AND NOT EXISTS (SELECT 1 FROM TmpContractEvent WHERE TmpContractEvent.id = TmpEvent.id)");
			query.append(reqSeparator2);
			query.append(CR).append("DROP INDEX IF EXISTS _consumer2");
			query.append(reqSeparator2);
			query.append(CR).append("CREATE INDEX _consumer2 ON TmpContractEvent(consumer)");
		} else {
			query.append(reqSeparator2);
			query.append(CR).append("UPDATE TmpEvent ").append(CR)
					.append("	LEFT JOIN TmpContractEvent ON TmpContractEvent.id = TmpEvent.id").append(CR)
					.append("	SET TmpEvent.is_selected_location = 0, TmpEvent.is_selected = 0").append(CR)
					.append("	WHERE TmpEvent.is_selected AND is_contract  AND TmpContractEvent.id is NULL");
			query.append(reqSeparator2);
			query.append(CR).append("ALTER TABLE TmpContractEvent ADD KEY (consumer)");
		}
		query.append(reqSeparator2);
		// .append(CR).append("UPDATE TmpEvent ")
		// .append(CR).append(" JOIN TmpContractEvent ON TmpContractEvent.consumer =
		// TmpEvent.agent")
		// .append(CR).append(" SET TmpEvent.id_contract_evt = TmpContractEvent.id ")
		// .append(CR).append(" WHERE TmpEvent.is_selected AND TmpEvent.is_request")
		// .append( reqSeparator2)
		query.append(CR).append("UPDATE TmpEvent SET consumed = (SELECT IFNULL(SUM(TmpContractEvent.power),0) ")
				.append(CR).append("   		FROM TmpContractEvent ").append(CR)
				.append("	 	WHERE TmpContractEvent.consumer = TmpEvent.agent )").append(CR)
				.append("	WHERE TmpEvent.is_selected AND TmpEvent.is_request");
		query.append(reqSeparator2);
		query.append(CR).append("UPDATE TmpEvent SET missing = ").append(OP_GREATEST)
				.append("(0,power - consumed) WHERE is_request");
		query.append(reqSeparator2);
		query.append(CR).append("UPDATE TmpEvent ").append(CR)
				.append(" 	SET provided = (SELECT IFNULL(SUM(lea.power),0) ").append(CR)
				.append("   		FROM link_event_agent AS lea").append(CR)
				.append("    	JOIN TmpContractEvent ON TmpContractEvent.id = lea.id_event").append(CR)
				.append("		WHERE lea.agent_name = TmpEvent.agent)").append(CR)
				.append("	, provided_margin = (SELECT IFNULL(SUM(lea.power_max - lea.power),0) ").append(CR)
				.append("   		FROM link_event_agent AS lea").append(CR)
				.append("    	JOIN TmpContractEvent ON TmpContractEvent.id = lea.id_event").append(CR)
				.append("		WHERE lea.agent_name = TmpEvent.agent)").append(CR)
				.append("	WHERE TmpEvent.is_selected_location AND TmpEvent.is_producer");
		query.append(reqSeparator2);
		if (sqlite) {
			// query.append(CR).append("UPDATE TmpEvent SET missing = ROUND(missing,3)");
			query.append(CR).append(
					"UPDATE TmpEvent SET power = ROUND(power, 3), missing = ROUND(missing,3), consumed = ROUND(consumed, 3)");
			// power, power_margin, provided, provided_margin
			query.append(reqSeparator2);
		}
		query.append(CR).append("SELECT ").append(quotedComputeDate).append(" AS date ").append(CR)
				.append(",IFNULL(SUM(TmpEvent.power),0) AS sum_all").append(CR).append(",IFNULL(SUM(").append(OP_IF)
				.append("(TmpEvent.is_request, TmpEvent.power,0.0)),0) AS total_requested").append(CR)
				.append(",IFNULL(SUM(").append(OP_IF)
				.append("(TmpEvent.is_producer, TmpEvent.power,0.0)),0) AS total_produced").append(CR)
				.append(",IFNULL(SUM(").append(OP_IF)
				.append("(TmpEvent.is_producer, TmpEvent.provided,0.0)),0) AS total_provided").append(CR)
				.append(",IFNULL(SUM(").append(OP_IF)
				.append("(TmpEvent.is_request, TmpEvent.consumed,0.0)),0) AS total_consumed")
				// .append(CR).append(",IFNULL(SUM().append(OP_IF).append("(TmpEvent.is_contract,
				// TmpEvent.power_margin,0.0)),0) AS old_total_margin")
				.append(CR).append(",IFNULL(SUM(").append(OP_IF)
				.append("(TmpEvent.is_producer, TmpEvent.provided_margin,0.0)),0) AS total_provided_margin").append(CR)
				.append(",IFNULL(MIN(").append(OP_IF)
				.append("(TmpEvent.is_request AND TmpEvent.missing > 0, TmpEvent.missing, 999999.0)),0) AS min_request_missing")
				.append(CR).append("	 FROM TmpEvent WHERE is_selected_location");
		dbConnection.setDebugLevel(0);
		List<Map<String, Object>> sqlResult = dbConnection.executeSelect(query.toString());
		// dbConnection.setDebugLevel(0);
		if (sqlResult.size() > 0) {
			Map<String, Object> row = sqlResult.get(0);
			double requested = SapereUtil.getDoubleValue(row, "total_requested");
			double produced = SapereUtil.getDoubleValue(row, "total_produced");
			double provided = SapereUtil.getDoubleValue(row, "total_provided");
			double providedMargin = SapereUtil.getDoubleValue(row, "total_provided_margin");
			double consumed = SapereUtil.getDoubleValue(row, "total_consumed");
			double minRequestMissing = SapereUtil.getDoubleValue(row, "min_request_missing");
			if (linkedEvent != null && EventType.REQUEST_EXPIRY.equals(linkedEvent.getType())) {
				logger.info("Request expiry");
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
							+ " WHERE TmpContractEvent.is_selected AND is_location_ok";
					List<Map<String, Object>> rowsTestGap = dbConnection.executeSelect(testGap1);
					double totalConsumed = 0;
					double totalRequested = 0;
					for (Map<String, Object> nextRow : rowsTestGap) {
						double nextConsumed = SapereUtil.getDoubleValue(nextRow, "power");
						double nextRequested = SapereUtil.getDoubleValue(nextRow, "requested");
						if (nextConsumed > nextRequested) {
							logger.info("nextRow = " + nextRow);
							logger.info("Gap found for contract " + nextRow.get("agent") + " conumed = " + nextConsumed
									+ ", nextRequested = " + nextRequested);
						}
						totalConsumed += nextConsumed;
						totalRequested += nextRequested;
					}
					logger.info("totalConsumed = " + totalConsumed + ", totalProvided = " + totalRequested);
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
							+ "		WHERE link2.id_event = ctr.id and link2.agent_type = 'Producer') AS provided"
							+ " FROM TmpEvent AS ctr " + " WHERE ctr.is_selected_location AND ctr.is_contract";
					List<Map<String, Object>> rowsTestGap = dbConnection.executeSelect(testGap1);
					double totalConsumed = 0;
					double totalProvided = 0;
					for (Map<String, Object> nextRow : rowsTestGap) {
						double nextConsumed = SapereUtil.getDoubleValue(nextRow, "power");
						double nextProvided = SapereUtil.getDoubleValue(nextRow, "provided");
						if (Math.abs(nextConsumed - nextProvided) > 0.0001) {
							logger.info("nextRow = " + nextRow);
							logger.info("Gap found for contract " + nextRow.get("agent") + " conumed = " + nextConsumed
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
			// nodeTotal.setId(id);
			Date timeBefore = new Date();
			double available = nodeTotal.getAvailable();
			String idLast2 = nodeTotal.getIdLast() == null ? "NULL" : "" + nodeTotal.getIdLast();
			StringBuffer queryInsertLHE = new StringBuffer();
			String var_new_id_histo = sqlite ? "(SELECT int_value FROM _variables WHERE name = 'new_id_histo')"
					: "@new_id_histo";
			queryInsertLHE.append(CR).append("UPDATE link_history_active_event SET id_last=NULL WHERE id_history = ")
					.append(var_new_id_histo);
			queryInsertLHE.append(reqSeparator2);
			queryInsertLHE.append(CR).append(
					"UPDATE link_history_active_event SET id_last=NULL WHERE id_last IN (SELECT id FROM link_history_active_event WHERE id_history = ")
					.append(var_new_id_histo).append(")");
			queryInsertLHE.append(reqSeparator2);
			queryInsertLHE.append(CR).append("DELETE FROM link_history_active_event WHERE id_history = ")
					.append(var_new_id_histo);
			queryInsertLHE.append(reqSeparator2);
			queryInsertLHE.append(CR).append(
					"INSERT INTO link_history_active_event(id_history,id_event,id_event_origin,date,type,agent,location,power,provided,consumed,missing,is_request,is_producer,is_contract,is_complementary,has_warning_req,id_last)");
			queryInsertLHE.append(CR).append("SELECT ").append(var_new_id_histo).append(", id, id_origin,")
					.append(quotedComputeDate)
					.append(",type,agent,location,power,provided,consumed,missing,is_request,is_producer,is_contract,is_complementary");
			// ).append(CR).append(" ,(is_request AND id_contract_evt IS NULL AND power>0
			// AND power < '").append(available).append("') AS has_warning_req"
			queryInsertLHE.append(CR).append("	,(is_request AND missing > 0 AND missing < '").append(available)
					.append("') AS has_warning_req");
			queryInsertLHE.append(CR)
					.append("	,(SELECT (last.id) FROM link_history_active_event AS last WHERE last.id_history=")
					.append(idLast2).append(" AND last.id_event = TmpEvent.id) AS id_last");
			queryInsertLHE.append(CR).append(" FROM TmpEvent");
			queryInsertLHE.append(CR).append(" WHERE TmpEvent.is_selected_location");
			queryInsertLHE.append(reqSeparator2);
			queryInsertLHE.append(CR).append("UPDATE link_history_active_event SET id_last = ");
			queryInsertLHE.append(CR).append("	 (SELECT (last.id) ").append(CR)
					.append(" FROM link_history_active_event AS last WHERE last.id_history=").append(idLast2)
					.append(" AND last.id_event = link_history_active_event.id_event_origin)");
			queryInsertLHE.append(CR).append("   WHERE link_history_active_event.id_history = ")
					.append(var_new_id_histo).append(" AND link_history_active_event.id_last IS NULL");
			queryInsertLHE.append(reqSeparator2);
			queryInsertLHE.append(CR).append("UPDATE link_history_active_event SET warning_duration = ");
			// queryInsertLHE.append(CR).append(" JOIN link_history_active_event AS last ON
			// last.id = link_history_active_event.id_last");
			if (sqlite) {
				queryInsertLHE.append(CR).append(
						"	(SELECT last.warning_duration + strftime('%s',link_history_active_event.date) - strftime('%s' ,last.date)");
			} else {
				queryInsertLHE.append(CR).append(
						"	(SELECT last.warning_duration + UNIX_TIMESTAMP(link_history_active_event.date) - UNIX_TIMESTAMP(last.date)");
			}
			queryInsertLHE.append(CR).append(
					" FROM link_history_active_event AS last WHERE last.id =  link_history_active_event.id_last AND last.has_warning_req)");
			queryInsertLHE.append(CR).append("  WHERE id_history = ").append(var_new_id_histo).append(CR)
					.append(" AND link_history_active_event.has_warning_req").append(CR)
					.append("  AND EXISTS (SELECT 1 FROM link_history_active_event AS last WHERE last.id = link_history_active_event.id_last AND last.has_warning_req)");
			long result = dbConnection.execUpdate(queryInsertLHE.toString());
			if (result < 0) {
				// ONLY FOR DEBUG IF THERE IS AN SQL ERROR
			}
			if (result >= 0 && nodeTotal.getMissing() > 0 && nodeTotal.getAvailable() > 0) {
				StringBuffer queryUpdateHisto = new StringBuffer();
				queryUpdateHisto.append("UPDATE history SET ").append(CR).append(
						" max_warning_duration = (SELECT IFNULL(MAX(lhe.warning_duration),0) FROM link_history_active_event AS lhe WHERE lhe.id_history = ")
						.append(var_new_id_histo).append(")  ").append(CR)
						.append(",max_warning_consumer = (SELECT lhae.agent FROM link_history_active_event AS lhae WHERE ")
						.append(CR).append("          lhae.id_history = ").append(var_new_id_histo)
						.append(" AND lhae.has_warning_req ORDER BY warning_duration DESC, power LIMIT 0,1)").append(CR)
						.append(" WHERE history.id=").append(var_new_id_histo);
				result = dbConnection.execUpdate(queryUpdateHisto.toString());
			}
			long duration = new Date().getTime() - timeBefore.getTime();
			if (duration > 50) {
				logger.info("queryInsertLinkeHistoEvent duration (MS) : " + duration);
			}
		}
		dbConnection.setDebugLevel(0);
		return nodeTotal;
	}

	private static void logVariables() {
		if (sqlite) {
			List<Map<String, Object>> rows = dbConnection.executeSelect("SELECT * FROM _variables");
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

	private static NodeTotal registerNodeTotal(NodeTotal nodeTotal, EnergyEvent linkedEvent) {
		String sessionId = getSessionId();
		String reqSeparator2 = DBConnection.getReqSeparator2();
		NodeConfig nodeConfig = nodeTotal.getNodeConfig();
		String nodeTotalLocation = nodeConfig.getMainServiceAddress();
		String location2 = addSingleQuotes(nodeTotalLocation);
		String sHistoryDate = nodeTotal.getDate() == null ? "CURRENT_TIMESTAMP()"
				: addSingleQuotes(UtilDates.format_sql.format(nodeTotal.getDate()));
		StringBuffer queryClean = new StringBuffer();
		queryClean.append("DROP ").append(OP_TEMPORARY).append(" TABLE IF EXISTS TmpCleanHisto");
		queryClean.append(reqSeparator2);
		queryClean.append(CR).append("CREATE ").append(OP_TEMPORARY)
				.append(" TABLE TmpCleanHisto AS SELECT h.id FROM history h WHERE h.date > ").append(sHistoryDate)
				.append(" AND h.id_session = ").append(addSingleQuotes(sessionId))
				.append(" AND NOT EXISTS (SELECT 1 FROM event e WHERE e.id_histo = h.id)");
		queryClean.append(reqSeparator2);
		queryClean.append(CR)
				.append("UPDATE history SET id_next = NULL WHERE id_next IN (SELECT id FROM TmpCleanHisto)");
		queryClean.append(reqSeparator2);
		queryClean.append(CR)
				.append("UPDATE history SET id_last = NULL WHERE id_last IN (SELECT id FROM TmpCleanHisto)");
		queryClean.append(reqSeparator2);
		queryClean.append(CR)
				.append("UPDATE single_offer SET id_history = NULL WHERE id_history IN (SELECT id FROM TmpCleanHisto)");
		queryClean.append(reqSeparator2);
		queryClean.append(CR).append("UPDATE link_history_active_event SET id_last = NULL WHERE id_last IN").append(
				" (SELECT ID FROM link_history_active_event link_h_e WHERE id_history IN (SELECT id FROM TmpCleanHisto))");
		queryClean.append(reqSeparator2);
		queryClean.append(CR).append("DELETE ").append((sqlite ? "" : "link_h_e")).append(
				" FROM link_history_active_event AS link_h_e WHERE id_history IN (SELECT id FROM TmpCleanHisto)");
		queryClean.append(reqSeparator2);
		queryClean.append(CR).append("DELETE ").append((sqlite ? "" : "h"))
				.append(" FROM history AS h WHERE id IN (SELECT id FROM TmpCleanHisto)");
		long resultClean = dbConnection.execUpdate(queryClean.toString());
		if (resultClean < 0) {
			logger.error("registerNodeTotal resultClean = " + resultClean);
		} else {
			logger.info("registerNodeTotal resultClean = " + resultClean);
		}
		// String sHistoryDate = linkedEvent == null ? "CURRENT_TIMESTAMP()": "'" +
		// SapereUtil.format_sql.format(linkedEvent.getBeginDate()) + "'";
		StringBuffer query = new StringBuffer();
		if (sqlite) {
			query.append(DBConnection.generateQueryCreateVariableTable());
			query.append(reqSeparator2);
		}
		// Variable histo_date
		if (sqlite) {
			query.append(CR).append("INSERT INTO _Variables (name, date_value) VALUES ('histo_date',")
					.append(sHistoryDate).append(")");
		} else {
			query.append(CR).append("SET @histo_date = ").append(sHistoryDate);
		}
		query.append(reqSeparator2);
		String var_histo_date = sqlite ? "(SELECT date_value FROM _variables WHERE name = 'histo_date' )"
				: "@histo_date";
		// Variable id_last
		if (sqlite) {
			query.append(CR).append("INSERT INTO _variables (name, int_value) ").append(CR)
					.append("SELECT 'id_last', ID FROM history WHERE history.date < ").append(var_histo_date).append(CR)
					.append("		AND location = ").append(location2).append(CR)
					.append(" ORDER BY date DESC LIMIT 0,1");
		} else {
			query.append(CR).append("SET @id_last = (SELECT ID FROM history WHERE date < ").append(var_histo_date)
					.append(" AND location = ").append(location2).append(CR).append(" ORDER BY date DESC LIMIT 0,1)");
		}
		query.append(reqSeparator2);
		String var_id_last = sqlite ? "(SELECT int_value FROM _variables WHERE name = 'id_last' )" : "@id_last";
		/*
		 * query.append("INSERT INTO history SET ") .append(" date = @histo_date")
		 * .append(", id_last = @id_last")
		 * .append(", learning_agent = ").append(addSingleQuotes(nodeTotal.getAgentName(
		 * ))) .append(", location = ").append(addSingleQuotes(nodeTotalLocation))
		 * .append(", distance = ").append(addQuotes(nodeTotal.getDistance()))
		 * .append(", time_shift_ms = ").append(addQuotes(nodeTotal.getTimeShiftMS()))
		 * .append(", id_session = ").append(addSingleQuotes(sessionId))
		 * .append(", id_node_config = ").append(nodeTotal.getNodeConfig().getId())
		 * .append(", total_requested = ").append(addSingleQuotes(""+nodeTotal.
		 * getRequested()))
		 * .append(", total_consumed = ").append(addSingleQuotes(""+nodeTotal.
		 * getConsumed()))
		 * .append(", total_produced = ").append(addSingleQuotes(""+nodeTotal.
		 * getProduced()))
		 * .append(", total_provided = ").append(addSingleQuotes(""+nodeTotal.
		 * getProvided()))
		 * .append(", total_available = ").append(addSingleQuotes(""+nodeTotal.
		 * getAvailable()))
		 * .append(", total_missing = ").append(addSingleQuotes(""+nodeTotal.getMissing(
		 * ))) .append(", min_request_missing = ").append(addSingleQuotes(""+nodeTotal.
		 * getMinRequestMissing()))
		 * .append(", total_margin = ").append(addSingleQuotes(""+nodeTotal.
		 * getProvidedMargin())) .append(CR).append(" ON DUPLICATE KEY UPDATE")
		 * .append(CR).append("  total_requested = ").append(addSingleQuotes(""+
		 * nodeTotal.getRequested()))
		 * .append(CR).append(", total_consumed = ").append(addSingleQuotes(""+nodeTotal
		 * .getConsumed()))
		 * .append(CR).append(", total_produced = ").append(addSingleQuotes(""+nodeTotal
		 * .getProduced()))
		 * .append(CR).append(", total_provided = ").append(addSingleQuotes(""+nodeTotal
		 * .getProvided()))
		 * .append(CR).append(", total_available = ").append(addSingleQuotes(""+
		 * nodeTotal.getAvailable()))
		 * .append(CR).append(", total_missing = ").append(addSingleQuotes(""+nodeTotal.
		 * getMissing()))
		 * .append(", min_request_missing = ").append(addSingleQuotes(""+nodeTotal.
		 * getMinRequestMissing()))
		 * .append(CR).append(", total_margin = ").append(addSingleQuotes(""+nodeTotal.
		 * getProvidedMargin())) .append(CR).append(", id_last = @id_last") ;
		 */

		Map<String, String> defaultAffectation = new HashMap<>();
		defaultAffectation.put("date", var_histo_date);
		defaultAffectation.put("id_last", var_id_last);
		defaultAffectation.put("learning_agent", addSingleQuotes(nodeTotal.getAgentName()));
		defaultAffectation.put("location", addSingleQuotes(nodeTotalLocation));
		defaultAffectation.put("distance", addQuotes(nodeTotal.getDistance()));
		defaultAffectation.put("time_shift_ms", addQuotes(nodeTotal.getTimeShiftMS()));
		defaultAffectation.put("id_session", addSingleQuotes(sessionId));
		defaultAffectation.put("id_node_config", "" + nodeTotal.getNodeConfig().getId());
		defaultAffectation.put("total_requested", addSingleQuotes("" + nodeTotal.getRequested()));
		defaultAffectation.put("total_consumed", addSingleQuotes("" + nodeTotal.getConsumed()));
		defaultAffectation.put("total_produced", addSingleQuotes("" + nodeTotal.getProduced()));
		defaultAffectation.put("total_provided", addSingleQuotes("" + nodeTotal.getProvided()));
		defaultAffectation.put("total_available", addSingleQuotes("" + nodeTotal.getAvailable()));
		defaultAffectation.put("total_missing", addSingleQuotes("" + nodeTotal.getMissing()));
		defaultAffectation.put("min_request_missing", addSingleQuotes("" + nodeTotal.getMinRequestMissing()));
		defaultAffectation.put("total_margin", addSingleQuotes("" + nodeTotal.getProvidedMargin()));

		Map<String, String> confilctAffectation = new HashMap<>();
		confilctAffectation.put("total_requested", addSingleQuotes("" + nodeTotal.getRequested()));
		defaultAffectation.put("total_consumed", addSingleQuotes("" + nodeTotal.getConsumed()));
		defaultAffectation.put("total_produced", addSingleQuotes("" + nodeTotal.getProduced()));
		defaultAffectation.put("total_provided", addSingleQuotes("" + nodeTotal.getProvided()));
		defaultAffectation.put("total_available", addSingleQuotes("" + nodeTotal.getAvailable()));
		defaultAffectation.put("total_missing", addSingleQuotes("" + nodeTotal.getMissing()));
		defaultAffectation.put("min_request_missing", addSingleQuotes("" + nodeTotal.getMinRequestMissing()));
		defaultAffectation.put("total_margin", addSingleQuotes("" + nodeTotal.getProvidedMargin()));
		defaultAffectation.put("id_last", var_id_last);

		String queryInsert = dbConnection.generateInsertQuery("history", defaultAffectation, confilctAffectation);
		query.append(queryInsert);
		query.append(reqSeparator2);
		if (sqlite) {
			query.append(CR).append("INSERT INTO _variables (name, int_value)  ").append(CR)
					.append(" SELECT 'new_id_histo', MAX(ID) FROM history").append(CR)
					.append("JOIN _variables AS v_histo_date ON v_histo_date.name = 'histo_date'").append(CR)
					.append(" WHERE date = v_histo_date.date_value AND location = ").append(location2).append("");
		} else {
			query.append(CR).append("SET @new_id_histo = (SELECT MAX(ID) FROM history WHERE date = ")
					.append(var_histo_date).append(" AND location = ").append(location2).append(")");
		}
		String var_new_id_histo = sqlite ? "(SELECT int_value FROM _variables WHERE name = 'new_id_histo')"
				: "@new_id_histo";
		if (linkedEvent != null) {
			query.append(reqSeparator2);
			long evtId = linkedEvent.getId();
			query.append(CR).append("UPDATE event SET id_histo=").append(var_new_id_histo).append(" WHERE id = ")
					.append(addQuotes(evtId));
		}
		// Correction of history.id_next on recent rows
		query.append(reqSeparator2);
		if (sqlite) {
			query.append(CR).append("INSERT INTO _variables(name, date_value)").append(CR)
					.append(" SELECT 'date_last', date FROM history WHERE history.id=").append(var_id_last);
		} else {
			query.append(CR).append("SET @date_last = (SELECT date FROM history WHERE id=").append(var_id_last)
					.append(")");
		}
		query.append(reqSeparator2);
		String var_date_last = sqlite ? "(SELECT date_value FROM _variables WHERE name = 'date_last')" : "@date_last";
		query.append(CR).append("UPDATE history ").append(CR).append(
				" SET id_next = (SELECT h2.ID FROM history h2 WHERE h2.date > history.date AND h2.location = history.location ORDER BY h2.date LIMIT 0,1)")
				.append(CR).append(" WHERE history.date >= ").append(var_date_last).append(CR)
				.append(" AND location = ").append(location2).append(CR).append(" AND id_session = ")
				.append(addSingleQuotes(sessionId)).append(CR)
				.append(" AND IFNULL(id_next,0) <> (SELECT h2.ID FROM history h2 WHERE h2.date > history.date AND h2.location = history.location ORDER BY h2.date LIMIT 0,1)");
		query.append(reqSeparator2);
		// Correction of history.id_last on recent rows
		query.append(CR).append("DROP ").append(OP_TEMPORARY).append(" TABLE IF EXISTS TmpCorrectIdLast");
		query.append(reqSeparator2);
		query.append(CR).append("CREATE TEMPORARY TABLE TmpCorrectIdLast AS").append(CR)
				.append("SELECT id, id_last, id_last_toset FROM").append(CR).append("	( SELECT id, id_last,")
				.append(CR)
				.append("		 	(SELECT h2.ID FROM history h2 WHERE h2.date < h.date AND h2.location = h.location")
				.append(CR).append("				ORDER BY h2.date DESC LIMIT 0,1) AS id_last_toset").append(CR)
				.append(" 			FROM history h").append(CR).append("			WHERE h.date > ")
				.append(var_histo_date).append(" AND location = ").append(location2).append(" AND id_session = ")
				.append(addSingleQuotes(sessionId)).append(CR).append(" 		) AS TmpRecentHistory").append(CR)
				.append("	WHERE NOT TmpRecentHistory.id_last = TmpRecentHistory.id_last_toset");
		query.append(reqSeparator2);
		if (sqlite) {
			query.append(CR).append("UPDATE history SET id_last = (SELECT TmpCorrectIdLast.id_last_toset").append(CR)
					.append("	FROM TmpCorrectIdLast ").append(CR)
					.append("	WHERE TmpCorrectIdLast.id = history.id)").append(CR)
					.append(" WHERE id IN (SELECT id FROM TmpCorrectIdLast) ");
		} else {
			query.append(CR).append("UPDATE TmpCorrectIdLast").append(CR)
					.append("	JOIN history ON history.id = TmpCorrectIdLast.id").append(CR)
					.append("	SET history.id_last = TmpCorrectIdLast.id_last_toset");
		}
		// correction of link_history_active_event.id_last WHERE history.id_last is
		// corrected
		query.append(reqSeparator2);
		if (sqlite) {
			query.append(CR).append("DROP ").append(OP_TEMPORARY).append(" TABLE IF EXISTS TmpCorrectIdLastLink1");
			query.append(reqSeparator2);
			query.append(CR).append("CREATE TEMPORARY TABLE TmpCorrectIdLastLink1 AS").append(CR)
					.append("SELECT current.id, current.id_last, last.id AS id_last_toset").append(CR)
					.append(" FROM TmpCorrectIdLast ").append(CR)
					.append(" JOIN link_history_active_event AS current ON current.id_history = TmpCorrectIdLast.id")
					.append(CR)
					.append(" JOIN link_history_active_event AS last ON last.id_history = TmpCorrectIdLast.id_last_toset AND last.id_event = current.id_event");
//					.append(CR).append(" WHERE NOT current.id_last = ");
			query.append(reqSeparator2);
			query.append(CR).append(
					"UPDATE link_history_active_event SET id_last = (SELECT TmpCorrectIdLastLink1.id_last_toset")
					.append(CR)
					.append(" FROM TmpCorrectIdLastLink1 WHERE TmpCorrectIdLastLink1.id = link_history_active_event.id)")
					.append(CR).append(" WHERE id IN (SELECT id FROM TmpCorrectIdLastLink1)");
			query.append(reqSeparator2);
			query.append(CR).append("DROP ").append(OP_TEMPORARY).append(" TABLE IF EXISTS TmpCorrectIdLastLink2");
			query.append(reqSeparator2);
			query.append(CR).append("CREATE TEMPORARY TABLE TmpCorrectIdLastLink2 AS").append(CR)
					.append("SELECT current.id, current.id_last, last.id AS id_last_toset").append(CR)
					.append(" FROM TmpCorrectIdLast ").append(CR)
					.append(" JOIN link_history_active_event AS current ON current.id_history = TmpCorrectIdLast.id")
					.append(CR)
					.append(" JOIN link_history_active_event AS last ON last.id_history = TmpCorrectIdLast.id_last_toset AND last.id_event = current.id_event_origin")
					.append(CR).append(" WHERE current.id_last IS NULL");
			query.append(reqSeparator2);
			query.append(CR).append(
					"UPDATE link_history_active_event SET id_last = (SELECT TmpCorrectIdLastLink2.id_last_toset")
					.append(CR)
					.append(" FROM TmpCorrectIdLastLink2 WHERE TmpCorrectIdLastLink2.id = link_history_active_event.id)")
					.append(CR).append(" WHERE id IN (SELECT id FROM TmpCorrectIdLastLink2)");
		} else {
			query.append(CR).append("UPDATE TmpCorrectIdLast").append(CR)
					.append("	JOIN link_history_active_event AS current ON current.id_history = TmpCorrectIdLast.id")
					.append(CR)
					.append("	SET current.id_last = (SELECT (last.id) FROM link_history_active_event AS last ")
					.append(CR)
					.append("			WHERE last.id_history=TmpCorrectIdLast.id_last_toset AND last.id_event = current.id_event)");
			query.append(reqSeparator2);
			query.append(CR).append("UPDATE TmpCorrectIdLast").append(CR)
					.append("	JOIN link_history_active_event AS current ON current.id_history = TmpCorrectIdLast.id")
					.append(CR)
					.append("	SET current.id_last = (SELECT (last.id) FROM link_history_active_event AS last ")
					.append(CR)
					.append("			WHERE last.id_history=TmpCorrectIdLast.id_last_toset AND last.id_event = current.id_event_origin)")
					.append(CR).append("	WHERE current.id_last IS NULL");
		}
		// Set historyId on single offers
		query.append(reqSeparator2);
		if (sqlite) {
			query.append(CR).append(
					"UPDATE single_offer SET id_history=(SELECT h.ID FROM history h WHERE h.date <= single_offer.date ORDER BY h.date DESC LIMIT 0,1)")
					.append(CR).append(" WHERE single_offer.date >= (SELECT IFNULL(").append(var_date_last)
					.append(",'2000-01-01'))");
		} else {
			query.append(CR).append(
					"UPDATE single_offer SET id_history=(SELECT h.ID FROM history h WHERE h.date <= single_offer.date ORDER BY h.date DESC LIMIT 0,1)")
					.append(CR).append(" WHERE single_offer.date >= (SELECT IFNULL(").append(var_date_last)
					.append(",'2000-01-01'))");
		}
		// query.append("UPDATE single_offer SET id_history=(SELECT h.ID from history h
		// where h.date <= single_offer.creation_time ORDER BY h.date DESC LIMIT 0,1)
		// WHERE single_offer.creation_time >='2000-01-01'");
		dbConnection.execUpdate(query.toString());
		logVariables();
		Map<String, Object> row = dbConnection.executeSingleRowSelect("SELECT " + var_new_id_histo + " AS id");
		Long id = SapereUtil.getLongValue(row, "id");
		return retrieveNodeTotalById(id);
	}

	public static void cleanHistoryDB() {
		dbConnection.execUpdate("DELETE FROM single_offer");
		dbConnection.execUpdate("DELETE FROM link_event_agent");
		dbConnection.execUpdate("UPDATE link_history_active_event SET id_last=NULL");
		dbConnection.execUpdate("DELETE FROM link_history_active_event");
		dbConnection.execUpdate("DELETE FROM event");
		dbConnection.execUpdate("UPDATE history SET id_last=NULL, id_next=NULL");
		dbConnection.execUpdate("DELETE FROM history");
	}

	public static long registerSingleOffer(SingleOffer offer, EnergyEvent productionEvent, long currentHistoId) {
		String sessionId = getSessionId();
		String prodEvtId = (productionEvent == null) ? "NULL" : "" + productionEvent.getId();
		String sDate = UtilDates.format_sql.format(offer.getCreationTime());
		String sExpiryDate = UtilDates.format_sql.format(offer.getDeadline());
		String reqEvtId = (offer.getRequest() == null || offer.getRequest().getEventId() == null) ? "NULL"
				: "" + offer.getRequest().getEventId();
		/*
		 * StringBuffer query"new StringBuffer("INSERT INTO single_offer SET ");
		 * query.append("deadline = ").append(addSingleQuotes(sExpiryDate))
		 * .append(" ,date = ").append(addSingleQuotes(sDate))
		 * .append(" ,id_session = ").append(addSingleQuotes(sessionId))
		 * .append(" ,producer_agent = ").append(addSingleQuotes(offer.getProducerAgent(
		 * )))
		 * .append(" ,consumer_agent = ").append(addSingleQuotes(offer.getConsumerAgent(
		 * ))) .append(" ,power = ").append( addQuotes(offer.getPower()))
		 * .append(" ,power_min = ").append( addQuotes(offer.getPowerMin()))
		 * .append(" ,power_max = ").append( addQuotes(offer.getPowerMax()))
		 * .append(" ,production_event_id = ").append(prodEvtId)
		 * .append(" ,request_event_id =  ").append(reqEvtId)
		 * .append(" ,log = ").append(addSingleQuotes(offer.getLog()))
		 * .append(" ,time_shift_ms = ").append(addQuotes(offer.getTimeShiftMS()))
		 * .append(" ,is_complementary = ").append(offer.isComplementary() ? "1" : "0")
		 * //.append(" ,log2 = ").append(addQuotes("currentHistoId:").append(
		 * currentHistoId)) //.append(" ,id_history = ").
		 * append("(SELECT history.ID FROM history WHERE date <=CURRENT_TIMESTAMP() ORDER by date DESC LIMIT 0,1)"
		 * ) ;
		 */
		Map<String, String> affectation = new HashMap<>();
		affectation.put("deadline", addSingleQuotes(sExpiryDate));
		affectation.put("date", addSingleQuotes(sDate));
		affectation.put("id_session", addSingleQuotes(sessionId));
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
		String insertQuery = dbConnection.generateInsertQuery("single_offer", affectation);
		return dbConnection.execUpdate(insertQuery);
	}

	public static long setSingleOfferAcquitted(SingleOffer offer, EnergyEvent requestEvent, boolean used) {
		if (offer != null) {
			StringBuffer query = new StringBuffer("UPDATE single_offer SET ");
			// query.append("request_event_id = ").append(requestEvtId)
			query.append(" acquitted=1").append(" ,used=").append(used ? "1" : "0").append(" ,used_time=")
					.append(used ? OP_CURRENT_DATETIME : "NULL").append(" WHERE id=").append(offer.getId());
			return dbConnection.execUpdate(query.toString());
		}
		return 0;
	}

	public static long setSingleOfferAccepted(CompositeOffer globalOffer) {
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

	public static long setSingleOfferLinkedToContract(Contract contract, EnergyEvent startEvent) {
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

	public static long setSingleOfferCanceled(Contract contract, String comment) {
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

	public static NodeTotal retrieveLastNodeTotal() {
		List<Map<String, Object>> sqlResult = dbConnection
				.executeSelect("SELECT * FROM history ORDER BY history.date DESC LIMIT 0,1");
		if (sqlResult.size() > 0) {
			Map<String, Object> row = sqlResult.get(0);
			return auxRetrieveNodeTotal(row);
		}
		return null;
	}

	public static NodeTotal retrieveNodeTotalById(long id) {
		List<Map<String, Object>> sqlResult = dbConnection
				.executeSelect("SELECT * FROM history WHERE id = " + addQuotes(id));
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
		nodeTotal.setConsumed(SapereUtil.getDoubleValue(row, "total_consumed"));
		nodeTotal.setProduced(SapereUtil.getDoubleValue(row, "total_produced"));
		nodeTotal.setRequested(SapereUtil.getDoubleValue(row, "total_requested"));
		nodeTotal.setAvailable(SapereUtil.getDoubleValue(row, "total_available"));
		nodeTotal.setMissing(SapereUtil.getDoubleValue(row, "total_missing"));
		nodeTotal.setProvided(SapereUtil.getDoubleValue(row, "total_provided"));
		nodeTotal.setProvidedMargin(SapereUtil.getDoubleValue(row, "total_margin"));
		nodeTotal.setMinRequestMissing(SapereUtil.getDoubleValue(row, "min_request_missing"));
		nodeTotal.setMaxWarningDuration(SapereUtil.getLongValue(row, "max_warning_duration"));
		if (row.get("max_warning_consumer") != null) {
			nodeTotal.setMaxWarningConsumer("" + row.get("max_warning_consumer"));
		}
		nodeTotal.setDate(SapereUtil.getDateValue(row, "date", logger));
		// nodeTotal.setDate(SapereUtil.getDateValue(row, "date", logger));
		nodeTotal.setAgentName("" + row.get("learning_agent"));
		String location = "" + row.get("location");
		String[] locationArray = location.split(":");
		if (locationArray.length >= 2) {
			NodeConfig nodeConfig = new NodeConfig();
			nodeConfig.setHost(locationArray[0]);
			nodeConfig.setMainPort(Integer.valueOf(locationArray[1]));
			if (NodeManager.isLocal(nodeConfig)) {
				nodeConfig.setName(NodeManager.getNodeName());
			}
			nodeTotal.setNodeConfig(nodeConfig);
		}
		nodeTotal.setDistance(SapereUtil.getIntValue(row, "distance"));
		return nodeTotal;
	}

	public static List<ExtendedNodeTotal> retrieveNodeTotalHistory() {
		return aux_retrieveNodeTotalHistory(null);
	}

	public static ExtendedNodeTotal retrieveNodeTotalHistoryById(Long historyId) {
		List<ExtendedNodeTotal> listHistory = aux_retrieveNodeTotalHistory(historyId);
		if (listHistory.size() > 0) {
			return listHistory.get(0);
		}
		return null;
	}

	private static List<ExtendedNodeTotal> aux_retrieveNodeTotalHistory(Long filterHistoryId) {
		// correctHisto();
		String sessionId = getSessionId();
		long beginTime = new Date().getTime();
		String location = NodeManager.getLocation();
		List<ExtendedNodeTotal> result = new ArrayList<ExtendedNodeTotal>();
		StringBuffer sqlFilterHistoryId1 = new StringBuffer();
		if (filterHistoryId != null) {
			sqlFilterHistoryId1.append("histo_req.id_history=").append(addQuotes(filterHistoryId)).append(" AND ");
		}
		StringBuffer sqlFilterHistoryId2 = new StringBuffer();
		if (filterHistoryId != null) {
			sqlFilterHistoryId2.append("history.id=").append(addQuotes(filterHistoryId)).append(" AND ");
		}
		StringBuffer query2 = new StringBuffer();
		query2.append(
				"SELECT history.id, history.id_last, history.id_next, history.date, history.total_produced, history.total_requested, history.total_consumed ");
		query2.append(CR).append(
				",history.total_available, history.total_missing, history.total_provided, history.min_request_missing, history.total_margin, history.location, history.distance, history.max_warning_duration,history.max_warning_consumer");
		query2.append(CR).append(",IFNULL(hNext.date,").append(OP_CURRENT_DATETIME).append(") AS date_next");
		query2.append(CR).append(",IFNULL(TmpUnReqByHisto.nb_missing_request,0) AS nb_missing_request");
		query2.append(CR).append(",IFNULL(TmpUnReqByHisto.list_missing_requests,'') AS list_missing_requests");
		query2.append(CR).append(",IFNULL(TmpUnReqByHisto.sum_warning_missing1,0) AS sum_warning_missing1");
		if (sqlite) {
			query2.append(CR).append(",0.0 AS sum_warning_missing");
		} else {
			query2.append(CR).append(",IF(IFNULL(TmpUnReqByHisto.sum_warning_missing1,0) <= history.total_available");
			query2.append(CR).append("		,IFNULL(TmpUnReqByHisto.sum_warning_missing1,0)");
			query2.append(CR).append(
					"      	,COMPUTE_WARNING_SUM4(history.id, history.total_available, IFNULL(TmpUnReqByHisto.sum_warning_missing1,0))");
			query2.append(CR).append("	) AS sum_warning_missing");
		}
		query2.append(CR).append(" FROM history ");
		query2.append(CR).append(" LEFT JOIN history AS hNext ON hNext.id = history.id_next ");
		query2.append(CR).append(" LEFT JOIN (SELECT ");
		query2.append(CR).append("	     UnReq.id_histo");
		query2.append(CR).append("		,Count(*) 	AS nb_missing_request");
		query2.append(CR).append(" 		,SUM(UnReq.warning_missing) AS sum_warning_missing1");
		if (sqlite) {
			query2.append(CR).append("		,GROUP_CONCAT(UnReq.Label3,  ', ') AS list_missing_requests");
		} else {
			query2.append(CR).append(
					"		,GROUP_CONCAT(UnReq.Label3  ORDER BY UnReq.warning_duration DESC, UnReq.power SEPARATOR ', ') AS list_missing_requests");
		}
		query2.append(CR).append("	FROM (");
		query2.append(CR).append("		SELECT");
		query2.append(CR).append("			 histo_req.id_history AS id_histo");
		query2.append(CR).append("			,histo_req.agent AS consumer");
		query2.append(CR).append("			,histo_req.power");
		query2.append(CR).append("			,histo_req.missing");
		query2.append(CR).append("			,").append(OP_IF)
				.append("(warning_duration > 0 , histo_req.missing, 0) AS warning_missing");
		if (sqlite) {
			query2.append(CR).append("			,histo_req.agent || '(' ||  histo_req.power ||  ')'  AS Label");
			query2.append(CR).append(
					"			,histo_req.agent || '#' || histo_req.missing ||  '#' || IIF(histo_req.has_warning_req,1,0) || '#' || histo_req.warning_duration AS Label3");
		} else {
			query2.append(CR).append("			,CONCAT(histo_req.agent, '(',  histo_req.power, ')'  ) AS Label");
			query2.append(CR).append(
					"			,CONCAT(histo_req.agent, '#',  histo_req.missing, '#', IF(histo_req.has_warning_req,1,0) , '#' ,histo_req.warning_duration) AS Label3");
		}
		query2.append(CR).append("			,histo_req.warning_duration");
		query2.append(CR).append("		FROM link_history_active_event AS histo_req");
		query2.append(CR).append("		WHERE ").append(sqlFilterHistoryId1).append("histo_req.location =  ")
				.append(addSingleQuotes(location));
		query2.append(CR).append("			AND is_request > 0");
		query2.append(CR).append("			AND missing > 0");
		query2.append(CR).append("		) AS UnReq");
		query2.append(CR).append("	GROUP BY UnReq.id_histo");
		query2.append(CR).append(") AS TmpUnReqByHisto ON TmpUnReqByHisto.id_histo = history.id");
		query2.append(CR).append(" WHERE ").append(sqlFilterHistoryId2).append("history.id_session =  ")
				.append(addSingleQuotes(sessionId)).append(" AND history.location =").append(addSingleQuotes(location));
		query2.append(CR).append(" ORDER BY history.date, history.ID ");
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
			nextTotal.setAgentName("" + nextRow.get("learning_agent"));
			nextTotal.setDate(SapereUtil.getDateValue(nextRow, "date", logger));
			String location2 = "" + nextRow.get("location");
			String[] locationArray = location2.split(":");
			if (locationArray.length >= 2) {
				NodeConfig nodeConfig = new NodeConfig();
				nodeConfig.setHost(locationArray[0]);
				nodeConfig.setMainPort(Integer.valueOf(locationArray[1]));
				if (NodeManager.isLocal(nodeConfig)) {
					nodeConfig.setName(NodeManager.getNodeName());
				}
				nextTotal.setNodeConfig(nodeConfig);
			}
			nextTotal.setDistance(SapereUtil.getIntValue(nextRow, "distance"));
			nextTotal.setConsumed(SapereUtil.getDoubleValue(nextRow, "total_consumed"));
			nextTotal.setProduced(SapereUtil.getDoubleValue(nextRow, "total_produced"));
			nextTotal.setProvided(SapereUtil.getDoubleValue(nextRow, "total_provided"));
			nextTotal.setProvidedMargin(SapereUtil.getDoubleValue(nextRow, "total_margin"));
			nextTotal.setRequested(SapereUtil.getDoubleValue(nextRow, "total_requested"));
			nextTotal.setAvailable(SapereUtil.getDoubleValue(nextRow, "total_available"));
			nextTotal.setMissing(SapereUtil.getDoubleValue(nextRow, "total_missing"));
			nextTotal.setMinRequestMissing(SapereUtil.getDoubleValue(nextRow, "min_request_missing"));
			nextTotal.setId(SapereUtil.getLongValue(nextRow, "id"));
			nextTotal.setIdLast(SapereUtil.getLongValue(nextRow, "id_last"));
			nextTotal.setIdNext(SapereUtil.getLongValue(nextRow, "id_next"));
			if (nextRow.get("date_next") != null) {
				nextTotal.setDateNext(SapereUtil.getDateValue(nextRow, "date_next", null));
			}
			// nextTotal.setNbOffers((Long) nextRow.get("nb_offers")) ;
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
				nextTotal.setSumWarningPower(SapereUtil.getDoubleValue(nextRow, "sum_warning_missing"));
			}
			nextTotal.setMinRequestMissing(SapereUtil.getDoubleValue(nextRow, "min_request_missing"));
			nextTotal.setMaxWarningDuration(SapereUtil.getLongValue(nextRow, "max_warning_duration"));
			if (nextRow.get("max_warning_consumer") != null) {
				nextTotal.setMaxWarningConsumer("" + nextRow.get("max_warning_consumer"));
			}
			nextTotal.setNbMissingRequests(SapereUtil.getLongValue(nextRow, "nb_missing_request"));
			result.add(nextTotal);
		}
		if (filterHistoryId == null) {
			// Retrieve events
			List<ExtendedEnergyEvent> listEvents = retrieveSessionEvents(sessionId, false);
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
			OfferFilter filter = new OfferFilter();
			filter.setDateMin(dateMin);
			filter.setDateMax(dateMax);
			List<SingleOffer> listOffers = retrieveOffers(filter);
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
					// List<SingleOffer> offers = mapHistoOffers.get(histoId);
					nextTotal.setOffers(mapHistoOffers.get(histoId));
				}
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

	public static void correctHisto() {
		String sessionId = getSessionId();
		// Correction of history.id_last
		dbConnection.execUpdate(
				"UPDATE history h  SET id_last = (SELECT h2.ID FROM history h2 WHERE h2.date < h.date AND h2.location = h.location ORDER BY h2.date DESC LIMIT 0,1)"
						+ " WHERE id_session=" + addSingleQuotes(sessionId)
						+ " AND id_last <> (SELECT h2.ID FROM history h2 WHERE h2.date < h.date AND h2.location = h.location ORDER BY h2.date DESC LIMIT 0,1)");
		// Correction of history.id_next
		dbConnection.execUpdate(
				"UPDATE history h  SET id_next = (SELECT h2.ID FROM history h2 WHERE h2.date > h.date AND h2.location = h.location ORDER BY h2.date LIMIT 0,1)"
						+ " WHERE id_session=" + addSingleQuotes(sessionId)
						+ " AND IFNULL(id_next,0) <> (SELECT h2.ID FROM history h2 WHERE h2.date > h.date AND h2.location = h.location ORDER BY h2.date LIMIT 0,1)");

	}

	public static boolean isOfferAcuitted(Long id) {
		String sId = "" + id;
		Map<String, Object> row = dbConnection
				.executeSingleRowSelect("SELECT id,acquitted FROM single_offer WHERE id = " + addSingleQuotes(sId));
		if (row != null && row.get("acquitted") instanceof Boolean) {
			Boolean acquitted = (Boolean) row.get("acquitted");
			return acquitted;
		}
		return false;
	}

	public static void addLogOnOffer(Long offerId, String textToAdd) {
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

	public static List<SingleOffer> retrieveOffers(OfferFilter offerFilter) {
		SapereLogger.getInstance().info("retrieveOffers : offerFilter = " + offerFilter);
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
		StringBuffer query = new StringBuffer();
		query.append("SELECT single_offer.* ").append(CR).append(" ,prodEvent.begin_date AS prod_begin_date").append(CR)
				.append(" ,prodEvent.expiry_date AS prod_expiry_date").append(CR)
				.append(" ,prodEvent.power AS prod_power").append(CR).append(" ,prodEvent.power_min AS prod_power_min")
				.append(CR).append(" ,prodEvent.power_max AS prod_power_max").append(CR)
				.append(" ,prodEvent.device_name AS prod_device_name").append(CR)
				.append(" ,prodEvent.device_category AS prod_device_category").append(CR)
				.append(" ,prodEvent.environmental_impact AS prod_env_impact").append(CR)
				.append(" ,prodEvent.time_shift_ms AS prod_time_shift_ms").append(CR)
				.append(" ,requestEvent.begin_date AS req_begin_date").append(CR)
				.append(" ,requestEvent.expiry_date AS  req_expiry_date").append(CR)
				.append(" ,requestEvent.is_complementary AS req_is_complementary").append(CR)
				.append(" ,requestEvent.agent AS req_agent").append(CR).append(" ,requestEvent.power AS req_power")
				.append(CR).append(" ,requestEvent.power_min AS req_power_min").append(CR)
				.append(" ,requestEvent.power_max AS req_power_max").append(CR)
				.append(" ,requestEvent.device_name AS  req_device_name").append(CR)
				.append(" ,requestEvent.environmental_impact AS req_env_impact").append(CR)
				.append(" ,requestEvent.device_category AS  req_device_category").append(CR)
				.append(" ,requestEvent.time_shift_ms AS req_time_shift_ms")
				// .append(CR).append(" ,(select h.ID from history h where h.date <=
				// single_offer.creation_time order by h.date desc limit 0,1) as id_histo")
				.append(CR).append(" FROM single_offer ").append(CR)
				.append(" LEFT JOIN event AS prodEvent ON prodEvent.id = single_offer.production_event_id").append(CR)
				.append(" LEFT JOIN event AS requestEvent ON requestEvent.id = single_offer.request_event_id")
				.append(CR).append(" WHERE single_offer.date >= ").append(addSingleQuotes(sDateMin)).append(CR)
				.append("    AND  single_offer.date < ").append(addSingleQuotes(sDateMax)).append(CR).append("    AND ")
				.append(sConsumerFilter).append(" AND ").append(sProducerFilter).append(CR).append("    AND ")
				.append(sAaddedFilter).append(CR).append(" ORDER BY single_offer.creation_time, single_offer.ID ");
		List<Map<String, Object>> rows = dbConnection.executeSelect(query.toString());
		for (Map<String, Object> row : rows) {
			EnergyRequest request = null;
			Boolean accepted = SapereUtil.getBooleantValue(row, "accepted");
			Boolean used = SapereUtil.getBooleantValue(row, "used");
			Boolean acquitted = SapereUtil.getBooleantValue(row, "acquitted");
			Double reqPower = SapereUtil.getDoubleValue(row, "req_power");
			Double reqPowerMin = SapereUtil.getDoubleValue(row, "req_power_min");
			Double reqPowerMax = SapereUtil.getDoubleValue(row, "req_power_max");
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
				PricingTable pricingTable = new PricingTable(reqTimeShiftMS);
				DeviceProperties deviceProperties = new DeviceProperties("" + row.get("req_device_name"),
						deviceCategory, envImpact, false);
				int issuerDistance = 0;
				request = new EnergyRequest("" + row.get("req_agent"), NodeManager.getNodeConfig(), issuerDistance,
						resIsComplementary, reqPower, reqPowerMin, reqPowerMax, reqBeginDate, reqExpiryDate, tolerance,
						PriorityLevel.LOW, deviceProperties, pricingTable, reqTimeShiftMS);
			}
			EnergySupply supply = null;
			Double power = SapereUtil.getDoubleValue(row, "power");
			Double powerMin = SapereUtil.getDoubleValue(row, "power_min");
			Double powerMax = SapereUtil.getDoubleValue(row, "power_max");
			if (power != null && request != null) {
				String producerAgent = "" + row.get("producer_agent");
				String sDeviceCat = "" + row.get("prod_device_category");
				DeviceCategory deviceCategory = DeviceCategory.getByName(sDeviceCat);
				EnvironmentalImpact envImpact = SapereUtil.getEnvironmentalImpactValue(row, "prod_env_impact");
				long timeShiftMS = SapereUtil.getLongValue(row, "prod_time_shift_ms");
				PricingTable pricingTable = new PricingTable(timeShiftMS);
				DeviceProperties deviceProperties = new DeviceProperties("" + row.get("prod_device_name"),
						deviceCategory, envImpact, true);
				int issuerDistance = 0;
				supply = new EnergySupply(producerAgent, NodeManager.getNodeConfig(), issuerDistance, false, power,
						powerMin, powerMax, SapereUtil.getDateValue(row, "prod_begin_date", logger),
						SapereUtil.getDateValue(row, "prod_expiry_date", logger), deviceProperties, pricingTable,
						timeShiftMS);
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
		return result;
	}

	public static Map<Integer, Map<String, TransitionMatrixKey>> loadMapNodeTransitionMatrixKeys(
			PredictionContext context) {
		Map<Integer, Map<String, TransitionMatrixKey>> result = new HashMap<Integer, Map<String, TransitionMatrixKey>>();
		List<TransitionMatrixKey> listTrKeys = loadListNodeTransitionMatrixKeys(context);
		for (TransitionMatrixKey nextTrKey : listTrKeys) {
			Integer timeWindowId = nextTrKey.getTimeWindowId();
			if (!result.containsKey(timeWindowId)) {
				result.put(timeWindowId, new HashMap<String, TransitionMatrixKey>());
			}
			Map<String, TransitionMatrixKey> map1 = result.get(timeWindowId);
			String variable = nextTrKey.getVariable();
			map1.put(variable, nextTrKey);
		}
		return result;
	}

	public static List<TransitionMatrixKey> loadListNodeTransitionMatrixKeys(PredictionContext context) {
		List<TransitionMatrixKey> result = new ArrayList<TransitionMatrixKey>();
		List<Map<String, Object>> rows1 = dbConnection.executeSelect(
				"SELECT * FROM transition_matrix WHERE location = " + addSingleQuotes(context.getMainServiceAddress())
						+ " AND scenario = " + addSingleQuotes(context.getScenario()));
		for (Map<String, Object> row : rows1) {
			String variable = "" + row.get("variable_name");
			Long idTM = SapereUtil.getLongValue(row, "id");
			int timeWindowId = SapereUtil.getIntValue(row, "id_time_window");
			MarkovTimeWindow timeWindow = context.getMarkovTimeWindow(timeWindowId);
			TransitionMatrixKey trKey = new TransitionMatrixKey(idTM, context.getId(), variable, timeWindow);
			result.add(trKey);
		}
		return result;
	}

	public void saveTValues(List<TimestampedValue> tvalues) {
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
			Double value = SapereUtil.getDoubleValue(row, "value");
			TimestampedValue tvalue = new TimestampedValue(nextDate, value);
			result.add(tvalue);
		}
		return result;
	}

	public TimeSlot retrieveTValueInterval() {
		Map<String, Object> rows1 = dbConnection.executeSingleRowSelect("SELECT * FROM t_value ORDER BY date LIMIT 0,1");
		Map<String, Object> rows2 = dbConnection.executeSingleRowSelect("SELECT * FROM t_value ORDER BY date DESC LIMIT 0,1");
		Date date1 = SapereUtil.getDateValue(rows1, "date", logger);
		Date date2 = SapereUtil.getDateValue(rows2, "date", logger);
		return new TimeSlot(date1, date2);
	}

	public List<TimestampedValue> retrieveLastValues(Date aDate, int nbValues) {
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

	public List<TimestampedValue> retrieveNextTValues(Date aDate, int nbValues) {
		String sDate = UtilDates.format_sql.format(aDate);
		String request = "SELECT * FROM t_value WHERE date >= " + addSingleQuotes(sDate) + " ORDER BY date LIMIT 0,"
				+ (nbValues - 1);
		List<Map<String, Object>> rows1 = dbConnection.executeSelect(request);
		return aux_constructTValues(rows1);
	}

}
