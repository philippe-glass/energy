package com.sapereapi.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.env.Environment;

import com.sapereapi.exception.DoublonException;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.energy.CompositeOffer;
import com.sapereapi.model.energy.Contract;
import com.sapereapi.model.energy.Device;
import com.sapereapi.model.energy.DeviceMeasure;
import com.sapereapi.model.energy.DeviceProperties;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.ExtendedEnergyEvent;
import com.sapereapi.model.energy.ExtendedNodeTotal;
import com.sapereapi.model.energy.MissingRequest;
import com.sapereapi.model.energy.NodeTotal;
import com.sapereapi.model.energy.PricingTable;
import com.sapereapi.model.energy.SimulatorLog;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.model.markov.MarkovTimeWindow;
import com.sapereapi.model.markov.TransitionMatrixKey;
import com.sapereapi.model.prediction.PredictionContext;
import com.sapereapi.model.referential.AgentType;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.EventObjectType;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.PhaseNumber;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.model.referential.WarningType;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.node.NodeManager;

public class EnergyDbHelper {
	private static Environment environment = null;
	private static DBConnection dbConnection = null;
	//private static DBConnection dbConnectionClemapData = null;
	private static EnergyDbHelper instance = null;
	public final static String CR = System.getProperty("line.separator");	// Cariage return
	private static int debugLevel = 0;
	private static SapereLogger logger = SapereLogger.getInstance();
	public final static String CLEMAPDATA_DBNAME = "clemap_data_light";

	public static void init(Environment _environment) {
		environment = _environment;
		// initialise db connection
		instance = new EnergyDbHelper();
	}
	public static EnergyDbHelper getInstance() {
		if (instance == null) {
			instance = new EnergyDbHelper();
		}
		return instance;
	}

	public EnergyDbHelper() {
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

	public static NodeContext registerContext(NodeContext nodeContext) {
		StringBuffer query = new StringBuffer();
		query.append("INSERT INTO context SET")
			.append(CR).append("  location = ").append(addSingleQuotes(nodeContext.getLocation()))
			.append(CR).append(", scenario = ").append(addSingleQuotes(nodeContext.getScenario()))
			.append(CR).append(", last_id_session = ").append(addSingleQuotes(nodeContext.getSessionId()))
			.append(CR).append(", last_time_shift_ms = ").append(nodeContext.getTimeShiftMS())
			.append(CR).append(" ON DUPLICATE KEY UPDATE ")
			.append(CR).append(" last_id_session = ").append(addSingleQuotes(nodeContext.getSessionId()))
			.append(CR).append(", last_time_shift_ms = ").append(nodeContext.getTimeShiftMS())
		;
		Long contextId = dbConnection.execUpdate(query.toString());
		nodeContext.setId(contextId);
		return nodeContext;
	}

	public static EnergyEvent registerEvent(EnergyEvent event) throws DoublonException {
		return registerEvent(event, null);
	}

	private static EnergyEvent auxGetEvent(Map<String, Object> row) {
		String agent = ""+ row.get("agent");
		String location = ""+ row.get("location");
		String typeLabel = "" + row.get("type");
		EventType type = EventType.getByLabel(typeLabel);
		String categoryLabel = "" + row.get("device_category");
		DeviceCategory category = DeviceCategory.getByName(categoryLabel);
		EnvironmentalImpact envImpact = SapereUtil.getEnvironmentalImpactValue(row, "environmental_impact");
		Double power = SapereUtil.getDoubleValue(row, "power");
		Double powerMin = SapereUtil.getDoubleValue(row, "power_min");
		Double powerMax = SapereUtil.getDoubleValue(row, "power_max");
		String comment =  "" + row.get("comment");
		long timeShiftMS = SapereUtil.getLongValue(row, "time_shift_ms");
		PricingTable pricingTable = new PricingTable();
		boolean isComplementary = SapereUtil.getBooleantValue(row, "is_complementary");
		boolean isProducer = EventObjectType.PRODUCTION.equals(type.getObjectType());
		DeviceProperties deviceProperties = new DeviceProperties(""+row.get("device_name"), category, envImpact, isProducer);
		EnergyEvent result = new EnergyEvent(type, agent, location, isComplementary, power, powerMin, powerMax
				, (Date) row.get("begin_date") , (Date) row.get("expiry_date")
				, deviceProperties, pricingTable, comment
				, timeShiftMS
				);
		result.setId(SapereUtil.getLongValue(row, "id"));
		Double powerUpdate = SapereUtil.getDoubleValue(row, "power_update");
		Double powerMinUpdate = SapereUtil.getDoubleValue(row, "power_min_update");
		Double powerMaxUpdate = SapereUtil.getDoubleValue(row, "power_max_update");
		result.setPowerUpates(powerUpdate, powerMinUpdate, powerMaxUpdate);
		result.setHistoId(SapereUtil.getLongValue(row, "id_histo"));
		if(row.get("warning_type")!=null) {
			result.setWarningType(WarningType.getByLabel(""+row.get("warning_type")));
		}
		//result.setIsComplementary();
		return result;
	}

	public static List<ExtendedEnergyEvent> retrieveLastSessionEvents() {
		List<ExtendedEnergyEvent> result = new ArrayList<ExtendedEnergyEvent>();
		Map<String, Object> row = dbConnection.executeSingleRowSelect("SELECT id_session FROM event ORDER BY ID DESC LIMIT 0,1)");
		if(row!=null) {
			String sessionId = ""+row.get("id_session");
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
		//.append(CR).append(" ,GET_EFFECTIVE_END_DATE(event.id) as effective_end_date")
		.append(CR).append(" ,LEAST(event.expiry_date")
		.append(CR).append(" 		, IFNULL(event.interruption_date,'3000-01-01')")
		.append(CR).append(" 		, IFNULL(event.cancel_date,'3000-01-01')) AS  effective_end_date")
		.append(CR).append(" ,IF(event.Type IN ('CONTRACT', 'CONTRACT_UPDATE') ")
		.append(CR).append( "	,(SELECT consumer.agent_name FROM link_event_agent AS consumer ")
		.append(CR).append( "			WHERE consumer.id_event = event.id AND consumer.agent_type = 'Consumer' LIMIT 0,1)")
		.append(CR).append( "	, NULL) AS linked_consumer")
		.append(CR).append(" ,IF(event.Type IN ('CONTRACT', 'CONTRACT_UPDATE') ")
		.append(CR).append( "	,(SELECT consumer.agent_location FROM link_event_agent AS consumer ")
		.append(CR).append( "			WHERE consumer.id_event = event.id AND consumer.agent_type = 'Consumer' LIMIT 0,1)")
		.append(CR).append( "	, NULL) AS linked_consumer_location")
		.append(CR).append(" FROM event")
		.append(CR).append(" WHERE event.id_session = ").append(addSingleQuotes(sessionId));
		if(onlyActive) {
			query.append(CR).append(" AND expiry_date > NOW()")
				 .append(CR).append(" AND IFNULL(interruption_date,'3000-01-01')  > NOW()")
				 .append(CR).append(" AND IFNULL(cancel_date,'3000-01-01')  > NOW()");
			//queryappend(CR).append(" AND GET_EFFECTIVE_END_DATE(event.id) > NOW()");
		}
		//query.append(CR).append(" ORDER BY event.type, 1*event.agent, event.creation_time ");
		query.append(CR).append(" ORDER BY id_histo, event.agent, event.creation_time ");
		dbConnection.setDebugLevel(0);
		List<Map<String, Object>> rows = dbConnection.executeSelect(query.toString());
		dbConnection.setDebugLevel(0);
		for(Map<String, Object> row: rows) {
			EnergyEvent nextEvent = auxGetEvent(row);
			PricingTable pricingTable = new PricingTable();
			ExtendedEnergyEvent nextEvent2 = new ExtendedEnergyEvent(
					nextEvent.getType(),  nextEvent.getIssuer(), nextEvent.getIssuerLocation()
				,nextEvent.isComplementary()
				,nextEvent.getPower(), nextEvent.getPowerMin(), nextEvent.getPowerMax()
				,nextEvent.getBeginDate(), nextEvent.getEndDate()
				,nextEvent.getDeviceProperties(), pricingTable, nextEvent.getComment()
				,nextEvent.getTimeShiftMS());
			nextEvent2.setEffectiveEndDate((Date) row.get("effective_end_date"));
			//nextEvent2.setIsComplementary(nextEvent.isComplementary());
			nextEvent2.setHistoId(nextEvent.getHistoId());
			nextEvent2.setWarningType(nextEvent.getWarningType());
			//nextEvent2.setIsComplementary(nextEvent.isComplementary());
			nextEvent2.setPowerUpateSlot(nextEvent.getPowerUpateSlot());
			if(row.get("linked_consumer")!=null) {
				nextEvent2.setLinkedConsumer(""+row.get("linked_consumer"));
			}
			if(row.get("linked_consumer_location")!=null) {
				nextEvent2.setLinkedConsumerLocation(""+row.get("linked_consumer_location"));
			}
			result.add(nextEvent2);
		}
		return result;
	}


	public static EnergyEvent retrieveEventById(Long id) {
		List<Map<String, Object>> rows = dbConnection.executeSelect("SELECT * FROM event WHERE id= " + id);
		if (rows.size() > 0) {
			Map<String, Object> row = rows.get(0);
			EnergyEvent result= auxGetEvent(row);
			return result;
		}
		return null;
	}

	public static EnergyEvent retrieveEvent(EventType evtType, String agentName, Boolean isComplementary, Date beginDate) {
		String sBeginDate = UtilDates.format_sql.format(beginDate);
		List<Map<String, Object>> rows = dbConnection.executeSelect("SELECT * FROM event WHERE "
				+ " begin_date = " + addSingleQuotes(sBeginDate)
				+ " AND type = "	+ addSingleQuotes(""+evtType)
				+ " AND agent = " + addSingleQuotes(agentName)
				+ " AND is_complementary = " + (isComplementary? "1":"0"))
				;
		if (rows.size() > 0) {
			Map<String, Object> row = rows.get(0);
			EnergyEvent result= auxGetEvent(row);
			Long originId = null;
			if(row.get("id_origin")!=null) {
				originId = SapereUtil.getLongValue(row, "id_origin");
			}
			if(originId!=null) {
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
			result =  retrieveEvent(event.getType(), event.getIssuer(), event.isComplementary(),event.getBeginDate());
		}
		return result;
	}

	public static EnergyEvent registerEvent(EnergyEvent event, Contract contract) throws DoublonException {
		String sBeginDate = UtilDates.format_sql.format(event.getBeginDate());
		String sExpiryDate = UtilDates.format_sql.format(event.getEndDate());
		if(event.getIssuerLocation() == null || "".equals(event.getIssuerLocation())) {
			throw new RuntimeException("registerEvent : location is not set on event " + event.toString());
		}
		// Check duplicate event
		EnergyEvent doublonCheck = retrieveEvent(event.getType(), event.getIssuer(), event.isComplementary(), event.getBeginDate());
		if(doublonCheck!=null) {
			throw new DoublonException("Event doublon : The client program tries to insert the following event twice : " + event.toString());
		}
		String sessionId = getSessionId();
		StringBuffer query = new StringBuffer("INSERT INTO event SET ");
		query.append("begin_date = ").append(addSingleQuotes(sBeginDate))
				.append(", expiry_date = ").append(addSingleQuotes(sExpiryDate))
				.append(", id_session = ").append(addSingleQuotes(sessionId))
				.append(", type = ").append(addSingleQuotes(""+event.getType()))
				.append(", object_type = ").append(addSingleQuotes(""+event.getType().getObjectType()))
				.append(", main_category = ").append(addSingleQuotes(""+event.getType().getMainCategory()))
				.append(", warning_type = ").append(event.getWarningType()==null? "''" : addSingleQuotes(event.getWarningType().getLabel()))
				.append(", power = ").append(addSingleQuotes(""+event.getPower()))
				.append(" ,power_min = ").append( addQuotes(event.getPowerMin()))
				.append(" ,power_max = ").append( addQuotes(event.getPowerMax()))
				.append(", power_update = ").append(addSingleQuotes(""+event.getPowerUpdate()))
				.append(" ,power_min_update = ").append( addQuotes(event.getPowerMinUpdate()))
				.append(" ,power_max_update = ").append( addQuotes(event.getPowerMaxUpdate()))
				.append(" ,time_shift_ms = ").append(addQuotes(event.getTimeShiftMS()))
				.append(", agent = ").append(addSingleQuotes(event.getIssuer()))
				.append(", location = ").append(addSingleQuotes(event.getIssuerLocation()))
				.append(", distance = ").append(addQuotes(event.getIssuerDistance()))
				.append(", device_name = ").append(addSingleQuotes(event.getDeviceProperties().getName()))
				.append(", device_category = ").append(addSingleQuotes(event.getDeviceProperties().getCategory().name()))// getLabel
				.append(", environmental_impact = ").append(addQuotes(event.getDeviceProperties().getEnvironmentalImpact().getLevel()))
				.append(", is_cancel = ").append(event.getType().getIsCancel() ? "1" : "0")
				.append(", is_ending = ").append(event.getType().getIsEnding() ? "1" : "0")
				.append(", id_origin = ").append(event.getOriginEvent() == null ? "NULL" : event.getOriginEvent().getId())
				.append(", is_complementary = ").append(event.isComplementary() ? "1" : "0")
				.append(", comment = ").append(addSingleQuotes(event.getComment()))
		;
		Long eventId = dbConnection.execUpdate(query.toString());
		EnergyEvent result = event;
		result.setId(eventId);
		if(contract!=null) {
			if (EventType.CONTRACT_START.equals(event.getType()) || EventType.CONTRACT_UPDATE.equals(event.getType())) {
				registerLinksEventAgent(eventId, contract);
			}
		}
		// Cancel all events from the same issuer that are before the insered event
		//dbConnection.setDebugLevel(10);
		String shortType = "" + event.getType().getObjectType();
		boolean isComplementary = event.isComplementary();
		dbConnection.setDebugLevel(0);
		dbConnection.execUpdate("UPDATE event SET cancel_date = " + addSingleQuotes(sBeginDate) + " WHERE "
				+ " agent = " + addSingleQuotes(event.getIssuer())
				+ " AND object_type = " + addSingleQuotes(shortType)
				+ " AND begin_date < " + addSingleQuotes(sBeginDate)
				+ (isComplementary? " AND is_complementary" : "") 	// No not cancel previous main contracts event if the new event is complementary
				+ " AND cancel_date IS NULL");
		dbConnection.setDebugLevel(0);
		if(event.getOriginEvent()!=null && event.getType().getIsCancel()) {
			// set the interruption date on the origin event
			long originEventId =event.getOriginEvent().getId();
			dbConnection.setDebugLevel(0);
			dbConnection.execUpdate("UPDATE event SET interruption_date = LEAST(IFNULL(interruption_date,'3000-01-01')," + addSingleQuotes(sBeginDate) + ") WHERE id = " + addQuotes(originEventId));
			dbConnection.setDebugLevel(0);
		}
		return result;
	}

	public static void registerLinksEventAgent(Long eventId, Contract contract) {
		// Consumer agent
		Double globalPower = contract.getPower();
		Double globalPowerMax = contract.getPowerMax();
		Double globalPowerMin = contract.getPowerMin();
		StringBuffer query = new StringBuffer("INSERT INTO link_event_agent SET ");
		query.append("id_event = '").append(eventId).append("'")
				.append(", agent_type = ").append(addSingleQuotes(AgentType.CONSUMER.getLabel()))
				.append(", agent_name = ").append(addSingleQuotes(contract.getConsumerAgent()))
				.append(", agent_location = ").append(addSingleQuotes(contract.getConsumerLocation()))
				.append(", power = ").append(addQuotes(globalPower) )
				.append(", power_min = ").append(addQuotes(globalPowerMin) )
				.append(", power_max = ").append(addQuotes(globalPowerMax) )
				;
		dbConnection.execUpdate(query.toString());
		// Producer agents
		for (String producer : contract.getProducerAgents()) {
			query = new StringBuffer("INSERT INTO link_event_agent SET ");
			Double power = contract.getPowerFromAgent(producer);
			Double powerMin = contract.getPowerMinFromAgent(producer);
			Double powerMax = contract.getPowerMaxFromAgent(producer);
			String location = contract.getLocationFromAgent(producer);
			query.append("id_event = '").append(eventId).append("'")
					.append(", agent_type = ").append(addSingleQuotes(AgentType.PRODUCER.getLabel()))
					.append(", agent_name = ").append(addSingleQuotes(producer))
					.append(", agent_location = ").append(addSingleQuotes(location))
					.append(", power = ").append(addQuotes(power))
					.append(", power_min = ").append(addQuotes(powerMin))
					.append(", power_max = ").append(addQuotes(powerMax))
					;
			dbConnection.execUpdate(query.toString());
		}
		// Check gap
		if(eventId>0) {
			Map<String, Object> rowCheckGap = dbConnection.executeSingleRowSelect("SELECT IFNULL(SUM(lea.power),0) AS provided FROM link_event_agent AS lea WHERE lea.id_event = " + addQuotes(eventId)
				+ " AND lea.agent_type = 'Producer'");
			double provided = SapereUtil.getDoubleValue(rowCheckGap, "provided");
			if(Math.abs(globalPower - provided) > 0.0001) {
				double contractGap = contract.computeGap();
				logger.warning("Gap found in new contract " + rowCheckGap + " contractGap = " + contractGap);
			}
		}
	}

	public static NodeTotal generateNodeTotal(Date computeDate, Long timeShiftMS, EnergyEvent linkedEvent, String url, String agentName, String location) {
		int distance = Sapere.getInstance().getDistance(location);
		//boolean local = (distance==0);
		NodeTotal nodeTotal = new NodeTotal();
		nodeTotal.setTimeShiftMS(timeShiftMS);
		nodeTotal.setDate(computeDate);
		nodeTotal.setLocation(location);
		nodeTotal.setDistance(distance);
		//nodeTotal.setIdLast(idLast);
		nodeTotal.setAgentName(agentName);
		String sComputeDate = UtilDates.format_sql.format(computeDate);
		String quotedComputeDate = addSingleQuotes(sComputeDate);
		//String distanceFilter = local ? "event.distance=0" : "event.distance > 0";

		boolean debugTmpTables = false;	// debug mode : to replace TEMPORARY tables by tables
		String query = "DROP TABLE IF EXISTS TmpEvent"
				+ CR + "§"
				+ CR + "DROP TEMPORARY TABLE IF EXISTS TmpEvent"
				+ CR + "§"
				+ CR + "DROP " + (debugTmpTables?"": "TEMPORARY ") + "TABLE IF EXISTS TmpEvent"
				+ CR + "§"
				+ CR + "CREATE TEMPORARY TABLE TmpEvent("
				+ CR + "	 effective_end_date 	DATETIME"
				+ CR + "	,is_selected 			BIT(1) NOT NULL DEFAULT b'0'"
				+ CR + "	,is_selected_location	BIT(1) NOT NULL DEFAULT b'0'"
				//+ CR + "	,has_warning_req		BIT(1) NOT NULL DEFAULT b'0'"
				+ CR + "	,id_contract_evt 		INT(11) NULL"
				+ CR + "	,power					DECIMAL(15,3) NOT NULL DEFAULT 0.0"
				+ CR + "	,power_margin			DECIMAL(15,3) NOT NULL DEFAULT 0.0"
				+ CR + "	,provided 				DECIMAL(15,3) NOT NULL DEFAULT 0.0"
				+ CR + "	,provided_margin		DECIMAL(15,3) NOT NULL DEFAULT 0.0"
				+ CR + "	,consumed 				DECIMAL(15,3) NOT NULL DEFAULT 0.0"
				+ CR + "	,missing 				DECIMAL(15,3) NOT NULL DEFAULT 0.0"
				+ CR + "	) AS"
				+ CR + "	SELECT ID, begin_date, id_origin"
				+ CR + " 		,LEAST(event.expiry_date"
				+ CR + "  			,IFNULL(event.interruption_date,'3000-01-01')"
				+ CR + "  			,IFNULL(event.cancel_date,'3000-01-01')) 	AS  effective_end_date"
				+ CR + "		,type,agent,is_complementary,location,power,distance"
				+ CR + "		,(power_max - power)							AS power_margin"
				+ CR + "		,object_type = 'REQUEST'						AS is_request"
				+ CR + "		,object_type = 'PRODUCTION'						AS is_producer"
				+ CR + "		,object_type = 'CONTRACT'				 		AS is_contract"
				+ CR + "		,0 												AS is_selected"
				+ CR + "		,0 												AS is_selected_location"
				+ CR + "		,NULL 											AS id_contract_evt"
				+ CR + "		,0.0											AS missing"
				+ CR + "		,0.0 											AS provided"
				+ CR + "		,0.0 											AS consumed"
				+ CR + " 		,location="+addSingleQuotes(location) +" 		AS is_location_ok"
				+ CR + "	FROM event"
				+ CR + "	WHERE NOT event.is_ending AND IFNULL(event.cancel_date,'3000-01-01') > " + quotedComputeDate
				+ CR + "§"
				+ CR +"	UPDATE TmpEvent SET is_selected = 1 WHERE begin_date<=" + quotedComputeDate + " AND effective_end_date > "+ quotedComputeDate
				+ CR + "§"
				+ CR +"	UPDATE TmpEvent SET is_selected_location = is_selected AND is_location_ok"
				+ CR + "§"
				+ CR + "DROP TEMPORARY TABLE IF EXISTS TmpRequestEvent"
				+ CR + "§"
				+ CR + "CREATE TEMPORARY TABLE TmpRequestEvent AS"
				+ CR + "  SELECT TmpEvent.id, TmpEvent.agent AS consumer, power, is_location_ok"
				+ CR +"	  FROM TmpEvent "
				+ CR + "  WHERE is_selected AND is_request"
				+ CR + "§"
				+ CR + "ALTER TABLE TmpRequestEvent ADD KEY (consumer)"
				+ CR + "§"
				+ CR + "DROP TEMPORARY TABLE IF EXISTS TmpContractEvent"
				+ CR + "§"
				+ CR + "CREATE TEMPORARY TABLE TmpContractEvent AS"
				+ CR + " 	SELECT TmpEvent.id, consumer.agent_name AS consumer, TmpEvent.is_location_ok, TmpEvent.power"
				+ CR + " 	FROM TmpEvent "
				+ CR + " 	JOIN link_event_agent AS consumer ON consumer.id_event = TmpEvent.id AND consumer.agent_type='Consumer'"
				+ CR + "    JOIN TmpRequestEvent ON TmpRequestEvent.consumer = consumer.agent_name "
				+ CR+ "  	WHERE is_selected AND is_contract"
				+ CR + "§"
				+ CR + "UPDATE TmpEvent "
				+ CR + "	LEFT JOIN TmpContractEvent ON TmpContractEvent.id = tmpevent.ID"
				+ CR + "	SET TmpEvent.is_selected_location = 0, TmpEvent.is_selected = 0"
				+ CR + "	WHERE tmpevent.is_selected AND is_contract  AND TmpContractEvent.id is NULL"
				+ CR + "§"
				+ CR + "ALTER TABLE TmpContractEvent ADD KEY (consumer)"
				+ CR + "§"
				//+ CR + "UPDATE TmpEvent "
				//+ CR + "	JOIN TmpContractEvent ON TmpContractEvent.consumer = TmpEvent.agent"
				//+ CR + "	SET TmpEvent.id_contract_evt = TmpContractEvent.id "
				//+ CR + "	WHERE TmpEvent.is_selected AND TmpEvent.is_request"
				//+ CR + "§"
				+ CR + "UPDATE TmpEvent SET consumed = (SELECT IFNULL(SUM(TmpContractEvent.power),0) "
				+ CR + "   		FROM TmpContractEvent "
				+ CR + "	 	WHERE TmpContractEvent.consumer = TmpEvent.agent )"
				+ CR + "	WHERE TmpEvent.is_selected AND TmpEvent.is_request"
				+ CR + "§"
				+ CR + "UPDATE TmpEvent SET missing = GREATEST(0,power - consumed) WHERE is_request"
				+ CR + "§"
				+ CR + "UPDATE TmpEvent "
				+ CR + " 	SET provided = (SELECT IFNULL(SUM(lea.power),0) "
				+ CR + "   		FROM link_event_agent AS lea"
				+ CR + "    	JOIN TmpContractEvent ON TmpContractEvent.id = lea.id_event"
				+ CR + "		WHERE lea.agent_name = TmpEvent.agent)"
				+ CR + "	, provided_margin = (SELECT IFNULL(SUM(lea.power_max - lea.power),0) "
				+ CR + "   		FROM link_event_agent AS lea"
				+ CR + "    	JOIN TmpContractEvent ON TmpContractEvent.id = lea.id_event"
				+ CR + "		WHERE lea.agent_name = TmpEvent.agent)"
				+ CR + "	WHERE TmpEvent.is_selected_location AND TmpEvent.is_producer"
				+ CR + "§"
				+ CR + "SELECT " + quotedComputeDate + " AS date "
				+ CR +",IFNULL(SUM(TmpEvent.power),0) AS sum_all"
				+ CR +",IFNULL(SUM(IF(TmpEvent.is_request, TmpEvent.power,0.0)),0) AS total_requested"
				+ CR +",IFNULL(SUM(IF(TmpEvent.is_producer, TmpEvent.power,0.0)),0) AS total_produced"
				+ CR +",IFNULL(SUM(IF(TmpEvent.is_producer, TmpEvent.provided,0.0)),0) AS total_provided"
				+ CR +",IFNULL(SUM(IF(TmpEvent.is_request, TmpEvent.consumed,0.0)),0) AS total_consumed"
				//+ CR +",IFNULL(SUM(IF(TmpEvent.is_contract, TmpEvent.power_margin,0.0)),0) AS old_total_margin"
				+ CR +",IFNULL(SUM(IF(TmpEvent.is_producer, TmpEvent.provided_margin,0.0)),0) AS total_provided_margin"
				+ CR +",IFNULL(MIN(IF(TmpEvent.is_request AND TmpEvent.missing > 0, TmpEvent.missing, 999999.0)),0) AS min_request_missing"
				+ CR +"	 FROM TmpEvent WHERE is_selected_location"
				;
		dbConnection.setDebugLevel(0);
		List<Map<String, Object>> sqlResult = dbConnection.executeSelect(query.toString());
		//dbConnection.setDebugLevel(0);
		if (sqlResult.size() > 0) {
			Map<String, Object> row = sqlResult.get(0);
			double requested = SapereUtil.getDoubleValue(row, "total_requested");
			double produced = SapereUtil.getDoubleValue(row, "total_produced");
			double provided = SapereUtil.getDoubleValue(row, "total_provided");
			double providedMargin = SapereUtil.getDoubleValue(row, "total_provided_margin");
			double consumed = SapereUtil.getDoubleValue(row, "total_consumed");
			double minRequestMissing = SapereUtil.getDoubleValue(row, "min_request_missing");
			if(linkedEvent!=null && EventType.REQUEST_EXPIRY.equals(linkedEvent.getType())) {
				logger.info("Request expiry");
			}
			boolean checkGaps = false;
			if(checkGaps) {
				logger.warning("generateNodeTotal step 12345 : consumed = " + consumed + ", provided = " + provided);
				if(consumed > requested + 0.01) {
					logger.warning("Consumed greated then requested : consumed = " + consumed + ", requested = " + requested );
					String testGap1 = "SELECT ctr.*,IFNULL(TmpRequestEvent.power,0) AS requested "
							+ " FROM TmpContractEvent "
							+ " LEFT JOIN TmpRequestEvent ON TmpRequestEvent.consumer = TmpContractEvent.consumer "
							+ " WHERE TmpContractEvent.is_selected AND is_location_ok";
					List<Map<String, Object>> rowsTestGap = dbConnection.executeSelect(testGap1);
					double totalConsumed = 0;
					double totalRequested = 0;
					for(Map<String, Object> nextRow : rowsTestGap) {
						double nextConsumed = SapereUtil.getDoubleValue(nextRow, "power");
						double nextRequested = SapereUtil.getDoubleValue(nextRow, "requested");
						if(nextConsumed  > nextRequested ) {
							logger.info("nextRow = " + nextRow);
							logger.info("Gap found for contract " + nextRow.get("agent") + " conumed = " + nextConsumed + ", nextRequested = "+ nextRequested);
						}
						totalConsumed+=nextConsumed;
						totalRequested+=nextRequested;
					}
					logger.info("totalConsumed = " + totalConsumed + ", totalProvided = " + totalRequested);
				}
			}
			// Consumption cannot be greater than request
			if(consumed > requested) {
				consumed = requested;
			}
			// Provided cannot be greater than produced
			if(provided > produced) {
				provided = produced;
			}
			if(checkGaps) {
				if(Math.abs(consumed - provided) >= 0.99 ) {
					logger.warning("Gap between provided and consumed : " + Math.abs(consumed - provided));
					String testGap1 = "SELECT ctr.* "
							+ " ,(SELECT IFNULL(sum(link2.power),0) FROM link_event_agent AS link2 "
							+ "		WHERE link2.id_event = ctr.id and link2.agent_type = 'Producer') AS provided"
							+ " FROM TmpEvent AS ctr "
							+ " WHERE ctr.is_selected_location AND ctr.is_contract";
					List<Map<String, Object>> rowsTestGap = dbConnection.executeSelect(testGap1);
					double totalConsumed = 0;
					double totalProvided = 0;
					for(Map<String, Object> nextRow : rowsTestGap) {
						double nextConsumed = SapereUtil.getDoubleValue(nextRow, "power");
						double nextProvided = SapereUtil.getDoubleValue(nextRow, "provided");
						if(Math.abs(nextConsumed - nextProvided) > 0.0001 ) {
							logger.info("nextRow = " + nextRow);
							logger.info("Gap found for contract " + nextRow.get("agent") + " conumed = " + nextConsumed + ", nextProvided = "+ nextProvided);
						}
						totalConsumed+=nextConsumed;
						totalProvided+=nextProvided;
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
			if(minRequestMissing <= requested) {
				nodeTotal.setMinRequestMissing(minRequestMissing);
			}
		}
		if(nodeTotal.hasActivity()) {
			nodeTotal = registerNodeTotal(nodeTotal, linkedEvent);
			//nodeTotal.setId(id);
			Date timeBefore = new Date();
			double available = nodeTotal.getAvailable();
			String idLast2 = nodeTotal.getIdLast()==null? "NULL" :  ""+nodeTotal.getIdLast();
			String queryInsertLinkeHistoEvent = "UPDATE link_history_active_event SET id_last=NULL WHERE id_history = @new_id_histo"
					+ CR + "§"
					+ "UPDATE link_history_active_event SET id_last=NULL WHERE id_last IN (SELECT id FROM link_history_active_event WHERE id_history = @new_id_histo)"
					+ CR + "§"
					+ "DELETE FROM link_history_active_event WHERE id_history = @new_id_histo"
					+ CR + "§"
					+ "INSERT INTO link_history_active_event(id_history,id_event,id_event_origin,date,type,agent,location,power,provided,consumed,missing,is_request,is_producer,is_contract,is_complementary,has_warning_req,id_last)"
					+ CR + "SELECT @new_id_histo, id, id_origin," + quotedComputeDate  + ",type,agent,location,power,provided,consumed,missing,is_request,is_producer,is_contract,is_complementary"
					//+ CR + "	,(is_request AND id_contract_evt IS NULL AND power>0 AND power < '" + available + "') AS has_warning_req"
					+ CR + "	,(is_request AND missing > 0 AND missing < '" + available + "') AS has_warning_req"
					+ CR +  "	,(SELECT (last.id) FROM link_history_active_event AS last WHERE last.id_history=" + idLast2 +" AND last.id_event = TmpEvent.id) AS id_last"
					+ CR + " FROM TmpEvent"
					+ CR + " WHERE TmpEvent.is_selected_location"
					+ CR + "§"
					+ CR + "UPDATE link_history_active_event AS current"
					+ CR + "	SET current.id_last = (SELECT (last.id) FROM link_history_active_event AS last WHERE last.id_history=" + idLast2 + " AND last.id_event = current.id_event_origin)"
					+ CR + "   WHERE current.id_history = @new_id_histo AND current.id_last IS NULL"
					+ CR + "§"
					+ CR + "UPDATE link_history_active_event AS current"
					+ CR +  "  JOIN link_history_active_event AS last ON last.id = current.id_last"
					+ CR + "	  SET current.warning_duration = last.warning_duration + UNIX_TIMESTAMP(current.date) - UNIX_TIMESTAMP(last.date)"
					+ CR + "   WHERE current.id_history = @new_id_histo AND current. has_warning_req AND last.has_warning_req";
			long result = dbConnection.execUpdate(queryInsertLinkeHistoEvent);
			if(result < 0) {
				// ONLY FOR DEBUG IF THERE IS AN SQL ERROR
				/*
				dbConnection.setDebugLevel(10);
				long result1 = dbConnection.execUpdate("UPDATE link_history_active_event SET id_last=NULL WHERE id_history = @new_id_histo");
				logger.info("For debug result1 "+ result1);
				long result2 = dbConnection.execUpdate("UPDATE link_history_active_event SET id_last=NULL WHERE id_last IN (SELECT id FROM link_history_active_event WHERE id_history = @new_id_histo)");
				logger.info("For debug result1 "+ result2);
				long result3 = dbConnection.execUpdate("DELETE FROM link_history_active_event WHERE id_history = @new_id_histo");
				logger.info("For debug result3 "+ result3);
				long result4 =  dbConnection.execUpdate("INSERT INTO link_history_active_event(id_history,id_event,date,type,agent,location,power,consumed,is_request,is_producer,is_contract,has_warning_req,id_last)"
						+ CR + "SELECT @new_id_histo, id ," + quotedComputeDate  + ",type,agent,location,power,consumed,is_request,is_producer,is_contract"
						//+ CR + "	,(is_request AND id_contract_evt IS NULL AND power>0 AND power < '" + available + "') AS has_warning_req"
						+ CR + "	,(is_request AND missing > 0 AND missing < '" + available + "') AS has_warning_req"
						+ CR +  "	,(SELECT (last.id) FROM link_history_active_event AS last WHERE last.id_history=" + idLast2 +" AND last.id_event = TmpEvent.id) AS id_last"
						+ CR + " FROM TmpEvent"
						+ CR + " WHERE TmpEvent.is_selected_location");
				logger.info("For debug result4 "+ result4);
				long result5 =  dbConnection.execUpdate("UPDATE link_history_active_event AS current"
				+ CR +  "  JOIN link_history_active_event AS last ON last.id = current.id_last"
				+ CR + "	  SET current.warning_duration = last.warning_duration + UNIX_TIMESTAMP(current.date) - UNIX_TIMESTAMP(last.date)"
				+ CR + "   WHERE current.id_history = @new_id_histo AND current. has_warning_req AND last.has_warning_req AND NOT current.id_last IS NULL");
				logger.info("For debug result5 "+ result5);
				*/
				/*
				List<Map<String, Object>> debugRows = dbConnection.executeSelect("SELECT @new_id_histo AS id_history, id AS id_event ," + quotedComputeDate  + " AS date,type,agent,location,power,is_request,is_producer,is_contract,id_contract_evt"
						+ "							,(is_request AND id_contract_evt IS NULL AND power>0 AND power < '" + available + "') AS has_warning_req"
						+ "							,(SELECT last.id FROM link_history_active_event AS last WHERE last.id_history=" + idLast2 +" AND id_event = TmpEvent.id) AS id_last"
						+ "					 FROM TmpEvent"
						+ "					 WHERE TmpEvent.is_selected_location");
				for(Map<String, Object> nextDebugRow  : debugRows) {
					//logger.info(" " + nextDebugRow);
					String sqlInsert = "  INSERT INTO link_history_active_event SET"
							+ "  	is_request=" +  nextDebugRow.get("is_request")
							+ "  	, agent=" + addQuotes(""+nextDebugRow.get("agent"))
							+ "  	, is_contract=" + nextDebugRow.get("is_contract")
							+ "  	, id_history=" + nextDebugRow.get("id_history")
							+ "  	, type=" + addQuotes(""+nextDebugRow.get("type"))
							+ "  	, date=" + addQuotes(""+nextDebugRow.get("date"))
							+ "  	, id_contract_evt=" + nextDebugRow.get("null")
							+ "  	, has_warning_req="+  nextDebugRow.get("has_warning_req")
							+ "  	, location=" + addQuotes(""+nextDebugRow.get("location"))
							+ "  	, id_last=" + nextDebugRow.get("id_last")
							+ "		, id_event="+ nextDebugRow.get("id_event")
							+ "		, power="+ addQuotes("" + nextDebugRow.get("power"))
							+ "		, is_producer="+ nextDebugRow.get("is_producer")
							;
					long test2 =  dbConnection.execUpdate(sqlInsert);
					logger.info(" sqlInsert = " + sqlInsert + ", test2 = " + test2);
				}
				*/
			}
			if(result >= 0 && nodeTotal.getMissing() > 0 && nodeTotal.getAvailable() > 0) {
				String queryUpdateHisto = "UPDATE history SET "
						+ CR + " max_warning_duration = (SELECT IFNULL(MAX(lhe.warning_duration),0) FROM link_history_active_event AS lhe WHERE lhe.id_history = @new_id_histo)  "
						+ CR + ",max_warning_consumer = (SELECT lhae.agent FROM link_history_active_event AS lhae WHERE "
						+ CR + "          lhae.id_history = @new_id_histo AND lhae.has_warning_req ORDER BY warning_duration DESC, power LIMIT 0,1)"
					+ CR + " WHERE history.id=@new_id_histo"
				;
				result = dbConnection.execUpdate(queryUpdateHisto);
			}
			long duration = new Date().getTime() - timeBefore.getTime();
			if(duration>50) {
				logger.info("queryInsertLinkeHistoEvent duration (MS) : " + duration);
			}
		}
		dbConnection.setDebugLevel(0);
		return nodeTotal;
	}

	public static void resetSimulatorLogs() {
		logger.info("resetSimulatorLogs ");
		dbConnection.execUpdate("DELETE simulator_log FROM simulator_log");
	}

	public static void registerSimulatorLog(SimulatorLog simulatorLog) {
		logger.info("registerSimulatorLog " + simulatorLog);
		String sessionId = getSessionId();
		StringBuffer query = new StringBuffer();
		query.append("INSERT INTO simulator_log SET ")
			.append(CR).append(" id_session = ").append(addSingleQuotes(sessionId))
			.append(CR).append(",device_category = ").append(addSingleQuotes(simulatorLog.getDeviceCategoryCode()))
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


	private static NodeTotal registerNodeTotal(NodeTotal nodeTotal, EnergyEvent linkedEvent) {
		String sessionId = getSessionId();
		String location = nodeTotal.getLocation();
		String location2 = addSingleQuotes(location);
		String sHistoryDate = nodeTotal.getDate() == null ? "CURRENT_TIMESTAMP()"
				: addSingleQuotes(UtilDates.format_sql.format(nodeTotal.getDate()));
		StringBuffer queryClean = new StringBuffer();
		queryClean.append("DROP TEMPORARY TABLE IF EXISTS TmpCleanHisto");
		queryClean.append(CR).append("§");
		queryClean.append("CREATE TEMPORARY TABLE TmpCleanHisto AS SELECT h.id FROM history h WHERE h.date > ").append(sHistoryDate)
			.append(" AND h.id_session = ").append(addSingleQuotes(sessionId))
			.append(" AND NOT EXISTS (SELECT 1 FROM Event e WHERE e.id_histo = h.id)");
		queryClean.append(CR).append("§");
		queryClean.append(CR).append("UPDATE history SET id_next = NULL WHERE id_next IN (SELECT id FROM TmpCleanHisto)");
		queryClean.append(CR).append("§");
		queryClean.append(CR).append("UPDATE history SET id_last = NULL WHERE id_last IN (SELECT id FROM TmpCleanHisto)");
		queryClean.append(CR).append("§");
		queryClean.append("UPDATE link_history_active_event SET id_last = NULL WHERE id_last IN"
				+ " (SELECT ID FROM link_history_active_event link_h_e WHERE id_history IN (SELECT id FROM TmpCleanHisto))"
				);
		queryClean.append(CR).append("§");
		queryClean.append("DELETE link_h_e FROM link_history_active_event link_h_e WHERE id_history IN (SELECT id FROM TmpCleanHisto)");
		queryClean.append(CR).append("§");
		queryClean.append("DELETE h FROM history h WHERE id IN (SELECT id FROM TmpCleanHisto)");
		long resultClean = dbConnection.execUpdate(queryClean.toString());
		if(resultClean < 0) {
			logger.error("registerNodeTotal resultClean = " + resultClean);
		} else {
			logger.info("registerNodeTotal resultClean = " + resultClean);
		}
		//String sHistoryDate = linkedEvent == null ? "CURRENT_TIMESTAMP()": "'" + SapereUtil.format_sql.format(linkedEvent.getBeginDate()) + "'";
		StringBuffer query = new StringBuffer();
		query.append("SET @histo_date = ").append(sHistoryDate)
			.append(CR).append("§")
			.append(CR).append("SET @id_last = (SELECT ID FROM history WHERE date < @histo_date AND location = " + location2 + " ORDER BY date DESC LIMIT 0,1)")
			.append(CR).append("§");
		query.append("INSERT INTO history SET ")
			.append(" date = @histo_date")
			.append(", id_last = @id_last")
			.append(", learning_agent = ").append(addSingleQuotes(nodeTotal.getAgentName()))
			.append(", location = ").append(addSingleQuotes(nodeTotal.getLocation()))
			.append(", distance = ").append(addQuotes(nodeTotal.getDistance()))
			.append(", time_shift_ms = ").append(addQuotes(nodeTotal.getTimeShiftMS()))
			.append(", id_session = ").append(addSingleQuotes(sessionId))
			.append(", total_requested = ").append(addSingleQuotes(""+nodeTotal.getRequested()))
			.append(", total_consumed = ").append(addSingleQuotes(""+nodeTotal.getConsumed()))
			.append(", total_produced = ").append(addSingleQuotes(""+nodeTotal.getProduced()))
			.append(", total_provided = ").append(addSingleQuotes(""+nodeTotal.getProvided()))
			.append(", total_available = ").append(addSingleQuotes(""+nodeTotal.getAvailable()))
			.append(", total_missing = ").append(addSingleQuotes(""+nodeTotal.getMissing()))
			.append(", min_request_missing = ").append(addSingleQuotes(""+nodeTotal.getMinRequestMissing()))
			.append(", total_margin = ").append(addSingleQuotes(""+nodeTotal.getProvidedMargin()))
			.append(CR).append(" ON DUPLICATE KEY UPDATE")
			.append(CR).append("  total_requested = ").append(addSingleQuotes(""+nodeTotal.getRequested()))
			.append(CR).append(", total_consumed = ").append(addSingleQuotes(""+nodeTotal.getConsumed()))
			.append(CR).append(", total_produced = ").append(addSingleQuotes(""+nodeTotal.getProduced()))
			.append(CR).append(", total_provided = ").append(addSingleQuotes(""+nodeTotal.getProvided()))
			.append(CR).append(", total_available = ").append(addSingleQuotes(""+nodeTotal.getAvailable()))
			.append(CR).append(", total_missing = ").append(addSingleQuotes(""+nodeTotal.getMissing()))
			.append(", min_request_missing = ").append(addSingleQuotes(""+nodeTotal.getMinRequestMissing()))
			.append(CR).append(", total_margin = ").append(addSingleQuotes(""+nodeTotal.getProvidedMargin()))
			.append(CR).append(", id_last = @id_last")
		;
		query.append(CR).append("§")
				.append(CR).append("SET @new_id_histo = (SELECT MAX(ID) FROM history WHERE date = @histo_date AND location = " + location2 + ")")
		;
		if(linkedEvent!=null) {
			query.append(CR).append("§");
			query.append("UPDATE event SET id_histo=@new_id_histo WHERE ID = '").append(linkedEvent.getId()).append("'");
		}
		// Correction of history.id_next on recent rows
		query.append(CR).append("§")
			 .append(CR).append("SET @date_last = (SELECT date FROM history WHERE id=@id_last)")
		;
		query.append(CR).append("§");
		query.append("UPDATE history h  SET id_next = (SELECT h2.ID FROM history h2 WHERE h2.date > h.date AND h2.location = h.location ORDER BY h2.date LIMIT 0,1)"
						+ " WHERE h.date >= @date_last AND location = " + location2 + " AND id_session = " + addSingleQuotes(sessionId)
								+ " AND IFNULL(id_next,0) <> (SELECT h2.ID FROM history h2 WHERE h2.date > h.date AND h2.location = h.location ORDER BY h2.date LIMIT 0,1)");
		query.append(CR).append("§");
		// Correction of history.id_last on recent rows
		query.append(CR).append("DROP TEMPORARY TABLE IF EXISTS TmpCorrectIdLast");
		query.append(CR).append("§");
		query.append("CREATE TEMPORARY TABLE TmpCorrectIdLast AS"
			+ CR + "SELECT id, id_last, id_last_toset FROM"
			+ CR + "	( SELECT id, id_last,"
			+ CR + "		 	(SELECT h2.ID FROM history h2 WHERE h2.date < h.date AND h2.location = h.location"
			+ CR + "				ORDER BY h2.date DESC LIMIT 0,1) AS id_last_toset"
			+ CR + " 			FROM history h"
			+ CR + "			WHERE h.date > @histo_date AND location = " + location2 + " AND id_session = " + addSingleQuotes(sessionId)
			+ CR + " 		) AS TmpRecentHistory"
			+ CR + "	WHERE NOT TmpRecentHistory.id_last = TmpRecentHistory.id_last_toset");
		query.append(CR).append("§");
		query.append("UPDATE TmpCorrectIdLast"
				+ CR + "	JOIN history ON history.id = TmpCorrectIdLast.id"
				+ CR + "	SET history.id_last = TmpCorrectIdLast.id_last_toset");
		//  correction of link_history_active_event.id_last WHERE history.id_last is corrected
		query.append(CR).append("§");
		query.append("UPDATE TmpCorrectIdLast"
				+ CR + "	JOIN link_history_active_event AS current ON current.id_history = TmpCorrectIdLast.id"
				+ CR + "	SET current.id_last =  (SELECT (last.id) FROM link_history_active_event AS last "
				+ CR + "			WHERE last.id_history=TmpCorrectIdLast.id_last_toset AND last.id_event = current.id_event)"
				);
		query.append(CR).append("§");
		query.append("UPDATE TmpCorrectIdLast"
				+ CR + "	JOIN link_history_active_event AS current ON current.id_history = TmpCorrectIdLast.id"
				+ CR + "	SET current.id_last =  (SELECT (last.id) FROM link_history_active_event AS last "
				+ CR + "			WHERE last.id_history=TmpCorrectIdLast.id_last_toset AND last.id_event = current.id_event_origin)"
				+ CR + "	WHERE current.id_last IS NULL"
				);
		// Set historyId on single offers
		query.append(CR).append("§");
		query.append("UPDATE single_offer SET id_history=(SELECT h.ID from history h where h.date <= single_offer.date ORDER BY h.date DESC LIMIT 0,1) WHERE single_offer.date >=IFNULL(@date_last,'2000-01-01')");
		//query.append("UPDATE single_offer SET id_history=(SELECT h.ID from history h where h.date <= single_offer.creation_time ORDER BY h.date DESC LIMIT 0,1) WHERE single_offer.creation_time >='2000-01-01'");
		dbConnection.execUpdate(query.toString());
		Map<String, Object> row = dbConnection.executeSingleRowSelect("SELECT @new_id_histo AS id");
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
		String prodEvtId = (productionEvent==null)? "NULL" : ""+productionEvent.getId();
		String sDate = UtilDates.format_sql.format(offer.getCreationTime());
		String sExpiryDate = UtilDates.format_sql.format(offer.getDeadline());
		String reqEvtId = (offer.getRequest()==null || offer.getRequest().getEventId()==null) ? "NULL" : "" + offer.getRequest().getEventId();
		StringBuffer query = new StringBuffer("INSERT INTO single_offer SET ");
		query.append("deadline = ").append(addSingleQuotes(sExpiryDate))
			.append(" ,date = ").append(addSingleQuotes(sDate))
			.append(" ,id_session = ").append(addSingleQuotes(sessionId))
			.append(" ,producer_agent = ").append(addSingleQuotes(offer.getProducerAgent()))
			.append(" ,consumer_agent = ").append(addSingleQuotes(offer.getConsumerAgent()))
			.append(" ,power = ").append( addQuotes(offer.getPower()))
			.append(" ,power_min = ").append( addQuotes(offer.getPowerMin()))
			.append(" ,power_max = ").append( addQuotes(offer.getPowerMax()))
			.append(" ,production_event_id = ").append(prodEvtId)
			.append(" ,request_event_id =  ").append(reqEvtId)
			.append(" ,log = ").append(addSingleQuotes(offer.getLog()))
			.append(" ,time_shift_ms = ").append(addQuotes(offer.getTimeShiftMS()))
			.append(" ,is_complementary = ").append(offer.isComplementary() ? "1" : "0")
			//.append(" ,log2 = ").append(addQuotes("currentHistoId:" + currentHistoId))
			//.append(" ,id_history = ").append("(SELECT history.ID FROM history WHERE date <=CURRENT_TIMESTAMP() ORDER by date DESC LIMIT 0,1)")
			;
/*
 * (select history.ID from history where history.date <= single_offer.creation_time order by Date desc limit 0,1)
 * */
		return dbConnection.execUpdate(query.toString());
	}

	public static long setSingleOfferAcquitted(SingleOffer offer,  EnergyEvent requestEvent, boolean used) {
		if(offer!=null) {
			//String requestEvtId = (requestEvent==null)? "NULL" : ""+requestEvent.getId();
			StringBuffer query = new StringBuffer("UPDATE single_offer SET ");
			//query.append("request_event_id = ").append(requestEvtId)
			query.append(" acquitted=1")
				.append(" ,used=").append(used? "1":"0")
				.append(" ,used_time=").append(used? "NOW()":"NULL")
				.append(" WHERE id=").append(offer.getId());
			return dbConnection.execUpdate(query.toString());
		}
		return 0;
	}

	public static long setSingleOfferAccepted(CompositeOffer globalOffer) {
		String offerids = globalOffer.getSingleOffersIdsStr();
		if(offerids.length()>0) {
			StringBuffer query = new StringBuffer("UPDATE single_offer SET accepted=1, acceptance_time = NOW()")
				.append(" WHERE id IN (").append(offerids).append(")");
			return dbConnection.execUpdate(query.toString());
		}
		return 0;
	}

	public static long setSingleOfferLinkedToContract(Contract contract, EnergyEvent startEvent) {
		String offerids = contract.getSingleOffersIdsStr();
		if(offerids.length()>0) {
			Long contractEventId = startEvent.getId();
			StringBuffer query = new StringBuffer("UPDATE single_offer SET contract_event_id=" + addQuotes(contractEventId) + ", contract_time = NOW()")
				.append(" WHERE id IN (").append(offerids).append(")");
			return dbConnection.execUpdate(query.toString());
		}
		return 0;
	}

	public static long setSingleOfferCanceled(Contract contract, String comment) {
		String offerids = contract.getSingleOffersIdsStr();
		if(offerids.length()>0) {
			StringBuffer query = new StringBuffer("UPDATE single_offer SET log_cancel =" + addSingleQuotes(comment) + ", contract_time = NOW()")
				.append(" WHERE id IN (").append(offerids).append(")");
			return dbConnection.execUpdate(query.toString());
		}
		return 0;
	}

	public static NodeTotal retrieveLastNodeTotal() {
		List<Map<String, Object>> sqlResult = dbConnection.executeSelect("SELECT * FROM history ORDER BY history.date DESC LIMIT 0,1");
		if(sqlResult.size()>0) {
			Map<String, Object> row = sqlResult.get(0);
			return auxRetrieveNodeTotal(row);
		}
		return null;
	}

	public static NodeTotal retrieveNodeTotalById(long id) {
		List<Map<String, Object>> sqlResult = dbConnection.executeSelect("SELECT * FROM history WHERE id = " + addQuotes(id));
		if(sqlResult.size()>0) {
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
		nodeTotal.setProduced(SapereUtil.getDoubleValue(row,"total_produced"));
		nodeTotal.setRequested(SapereUtil.getDoubleValue(row,"total_requested"));
		nodeTotal.setAvailable(SapereUtil.getDoubleValue(row,"total_available"));
		nodeTotal.setMissing(SapereUtil.getDoubleValue(row,"total_missing"));
		nodeTotal.setProvided(SapereUtil.getDoubleValue(row, "total_provided"));
		nodeTotal.setProvidedMargin(SapereUtil.getDoubleValue(row, "total_margin"));
		nodeTotal.setMinRequestMissing(SapereUtil.getDoubleValue(row,"min_request_missing"));
		nodeTotal.setMaxWarningDuration(SapereUtil.getLongValue(row, "max_warning_duration"));
		if(row.get("max_warning_consumer")!=null) {
			nodeTotal.setMaxWarningConsumer("" + row.get("max_warning_consumer"));
		}
		nodeTotal.setDate((Date) row.get("date"));
		nodeTotal.setAgentName("" + row.get("learning_agent"));
		nodeTotal.setLocation("" + row.get("location"));
		nodeTotal.setDistance(SapereUtil.getIntValue(row, "distance"));
		return nodeTotal;
	}

	public static List<ExtendedNodeTotal> retrieveNodeTotalHistory() {
		return aux_retrieveNodeTotalHistory(null);
	}

	public static ExtendedNodeTotal retrieveNodeTotalHistoryById(Long historyId) {
		List<ExtendedNodeTotal> listHistory = aux_retrieveNodeTotalHistory(historyId);
		if(listHistory.size()>0) {
			return listHistory.get(0);
		}
		return null;
	}

	private static List<ExtendedNodeTotal> aux_retrieveNodeTotalHistory(Long filterHistoryId) {
		//correctHisto();
		String sessionId = getSessionId();
		long beginTime = new Date().getTime();
		String location = NodeManager.getLocation();
		List<ExtendedNodeTotal> result = new ArrayList<ExtendedNodeTotal>();
		String sqlFilterHistoryId1 = (filterHistoryId==null)? "" : "histo_req.id_history=" + addQuotes(filterHistoryId) + " AND ";
		String sqlFilterHistoryId2 = (filterHistoryId==null)? "" : "history.id=" + addQuotes(filterHistoryId) + " AND ";
		StringBuffer query2 = new StringBuffer(
					  "SELECT history.id, history.id_last, history.id_next, history.date, history.total_produced, history.total_requested, history.total_consumed "
				+ CR + ",history.total_available, history.total_missing, history.total_provided, history.min_request_missing, history.total_margin, history.location, history.distance, history.max_warning_duration,history.max_warning_consumer"
				+ CR + ",IFNULL(hNext.date,NOW()) AS date_next"
				+ CR + ",IFNULL(TmpUnReqByHisto.nb_missing_request,0) AS nb_missing_request"
				+ CR + ",IFNULL(TmpUnReqByHisto.list_missing_requests,'') AS list_missing_requests"
				+ CR + ",IF(IFNULL(TmpUnReqByHisto.sum_warning_missing1,0) <= history.total_available"
				+ CR + "		,IFNULL(TmpUnReqByHisto.sum_warning_missing1,0)"
				+ CR + "      	,COMPUTE_WARNING_SUM4(history.id, history.total_available, IFNULL(TmpUnReqByHisto.sum_warning_missing1,0))"
				+ CR + "	) AS sum_warning_missing"
				+ CR + " FROM history "
				+ CR + " LEFT JOIN history AS hNext ON hNext.id = history.id_next "
				+ CR + " LEFT JOIN (SELECT "
				+ CR + "	     UnReq.id_histo"
				+ CR + "		,Count(*) 	AS nb_missing_request"
				+ CR +  " 		,SUM(UnReq.warning_missing) AS sum_warning_missing1"
				+ CR +	"		,GROUP_CONCAT(UnReq.Label3  ORDER BY UnReq.warning_duration DESC, UnReq.power SEPARATOR ', ') AS list_missing_requests"
				+ CR +	"	FROM ("
				+ CR +	"		SELECT"
				+ CR +	"			 histo_req.id_history AS id_histo"
				+ CR +	"			,histo_req.agent AS consumer"
				+ CR +	"			,histo_req.power"
				+ CR +	"			,histo_req.missing"
				+ CR +  "			,IF(warning_duration > 0 , histo_req.missing, 0) AS warning_missing"
				+ CR +	"			,CONCAT(histo_req.agent, '(',  histo_req.power, ')'  ) AS Label"
				+ CR +	"			,CONCAT(histo_req.agent, '#',  histo_req.missing, '#', IF(histo_req.has_warning_req,1,0) , '#' ,histo_req.warning_duration) AS Label3"
				+ CR + "			,histo_req.warning_duration"
				+ CR + "		FROM link_history_active_event AS histo_req"
				+ CR + "		WHERE " + sqlFilterHistoryId1 + "histo_req.location =  " + addSingleQuotes(location)
				+ CR + "			AND is_request > 0"
				+ CR + "			AND missing > 0"
				+ CR + "		) AS UnReq"
				+ CR + "	GROUP BY UnReq.id_histo"
				+ CR + ") AS TmpUnReqByHisto ON TmpUnReqByHisto.id_histo = history.id"
				+ CR + " WHERE " + sqlFilterHistoryId2 + "history.id_session =  " + addSingleQuotes(sessionId) + " AND history.location =" + 	addSingleQuotes(location)
				+ CR + " ORDER BY history.date, history.ID ");
			;
		Date dateMin = null;
		Date dateMax = null;
		/*
		if(filterHistoryId==null && false) {
			dbConnection.setDebugLevel(10);
		}*/
		//dbConnection.setDebugLevel(10);
		List<Map<String, Object>> sqlResult = dbConnection.executeSelect(query2.toString());
		dbConnection.setDebugLevel(0);
		for (Map<String, Object> nextRow : sqlResult) {
			ExtendedNodeTotal nextTotal = new ExtendedNodeTotal();
			Date nextDate = (Date) nextRow.get("date");
			if(dateMin==null) {
				dateMin = nextDate;
			}
			dateMax = nextDate;
			nextTotal.setAgentName("" + nextRow.get("learning_agent"));
			nextTotal.setDate((Date) nextRow.get("date"));
			nextTotal.setLocation("" + nextRow.get("location"));
			nextTotal.setDistance(SapereUtil.getIntValue(nextRow, "distance"));
			nextTotal.setConsumed(SapereUtil.getDoubleValue(nextRow, "total_consumed"));
			nextTotal.setProduced(SapereUtil.getDoubleValue(nextRow, "total_produced"));
			nextTotal.setProvided(SapereUtil.getDoubleValue(nextRow, "total_provided"));
			nextTotal.setProvidedMargin(SapereUtil.getDoubleValue(nextRow, "total_margin"));
			nextTotal.setRequested(SapereUtil.getDoubleValue(nextRow, "total_requested"));
			nextTotal.setAvailable(SapereUtil.getDoubleValue(nextRow, "total_available"));
			nextTotal.setMissing(SapereUtil.getDoubleValue(nextRow, "total_missing"));
			nextTotal.setMinRequestMissing(SapereUtil.getDoubleValue(nextRow, "min_request_missing"));
			nextTotal.setSumWarningPower(SapereUtil.getDoubleValue(nextRow, "sum_warning_missing"));
			nextTotal.setId(SapereUtil.getLongValue(nextRow,"id"));
			nextTotal.setIdLast(SapereUtil.getLongValue(nextRow,"id_last"));
			nextTotal.setIdNext(SapereUtil.getLongValue(nextRow,"id_next"));
			if(nextRow.get("date_next") != null) {
				nextTotal.setDateNext((Date) nextRow.get("date_next"));
			}
			//nextTotal.setNbOffers((Long) nextRow.get("nb_offers")) ;
			if(nextTotal.getContractDoublons()!=null && nextTotal.getContractDoublons().length()>0) {
				logger.warning("DEBUG ContractDoublons : " + nextTotal.getContractDoublons());
			}
			// Retrieve missing requests
			nextTotal.setListConsumerMissingRequests(new ArrayList<>());
			List<String> missingRequests = SapereUtil.getListStrValue(nextRow, "list_missing_requests");
			//String test = "" + nextRow.get("list_missing_requests");
			for(String sMissingRequest : missingRequests) {
				String[] arrayMissingRequest = sMissingRequest.split("#");
				if(arrayMissingRequest.length==4) {
					String issuer = arrayMissingRequest[0];
					Double power = Double.valueOf(arrayMissingRequest[1]);
					Boolean hasWarning = "1".equals(arrayMissingRequest[2]);
					Integer warningDurationSec = Integer.valueOf(arrayMissingRequest[3]);
					MissingRequest nextMissingRequest = new MissingRequest(issuer, power, hasWarning, warningDurationSec);
					nextTotal.addMissingRequest(nextMissingRequest);
				}
			}
			nextTotal.setMinRequestMissing(SapereUtil.getDoubleValue(nextRow, "min_request_missing"));
			nextTotal.setMaxWarningDuration(SapereUtil.getLongValue(nextRow, "max_warning_duration"));
			if(nextRow.get("max_warning_consumer")!=null) {
				nextTotal.setMaxWarningConsumer("" + nextRow.get("max_warning_consumer"));
			}
			nextTotal.setNbMissingRequests(SapereUtil.getLongValue(nextRow, "nb_missing_request"));
			result.add(nextTotal);
		}
		// Retrieve offers
		if(filterHistoryId==null) {
			// Retrieve events
			List<ExtendedEnergyEvent> listEvents = retrieveSessionEvents(sessionId, false);
			// Sort events by histoId
			Map<Long, List<EnergyEvent>> mapHistoEvents = new HashMap<Long, List<EnergyEvent>>();
			for(ExtendedEnergyEvent nextEvent : listEvents) {
				Long histoId = nextEvent.getHistoId();
				if(!mapHistoEvents.containsKey(histoId)) {
					mapHistoEvents.put(histoId, new ArrayList<EnergyEvent>());
				}
				(mapHistoEvents.get(histoId)).add(nextEvent);
			}
			// TODO : add historyId in retrieveOffers function
			List<SingleOffer> listOffers = retrieveOffers(dateMin, dateMax, null, null, null);
			// Sort offers by histoId
			Map<Long, List<SingleOffer>> mapHistoOffers = new HashMap<Long, List<SingleOffer>>();
			for(SingleOffer offer : listOffers) {
				Long histoId = offer.getHistoId();
				if(!mapHistoOffers.containsKey(histoId)) {
					mapHistoOffers.put(histoId, new ArrayList<SingleOffer>());
				}
				(mapHistoOffers.get(histoId)).add(offer);
			}
			for(ExtendedNodeTotal nextTotal : result) {
				Long histoId = nextTotal.getId();
				// Add events
				if(mapHistoEvents.containsKey(histoId)) {
					nextTotal.setLinkedEvents(mapHistoEvents.get(histoId));
				}
				// Add offers
				if(mapHistoOffers.containsKey(histoId)) {
					//List<SingleOffer> offers = mapHistoOffers.get(histoId);
					nextTotal.setOffers(mapHistoOffers.get(histoId));
				}
			}
		}
		long endTime = new Date().getTime();
		logger.info("aux_retrieveNodeTotalHistory filter histoID :" + filterHistoryId + " : load time MS = "+ (endTime - beginTime ));
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
		dbConnection.execUpdate("UPDATE history h  SET id_last = (SELECT h2.ID FROM history h2 WHERE h2.date < h.date AND h2.location = h.location ORDER BY h2.date DESC LIMIT 0,1)"
			+ " WHERE id_session=" + addSingleQuotes(sessionId) + " AND id_last <> (SELECT h2.ID FROM history h2 WHERE h2.date < h.date AND h2.location = h.location ORDER BY h2.date DESC LIMIT 0,1)");
		// Correction of history.id_next
		dbConnection.execUpdate("UPDATE history h  SET id_next = (SELECT h2.ID FROM history h2 WHERE h2.date > h.date AND h2.location = h.location ORDER BY h2.date LIMIT 0,1)"
				+ " WHERE id_session=" + addSingleQuotes(sessionId) + " AND IFNULL(id_next,0) <> (SELECT h2.ID FROM history h2 WHERE h2.date > h.date AND h2.location = h.location ORDER BY h2.date LIMIT 0,1)");

	}

	public static boolean isOfferAcuitted(Long id) {
		String sId = ""+id;
		Map<String, Object> row = dbConnection.executeSingleRowSelect("SELECT id,acquitted FROM single_offer WHERE id = " + addSingleQuotes(sId));
		if(row!=null && row.get("acquitted") instanceof Boolean)  {
			Boolean acquitted = (Boolean) row.get("acquitted");
			return acquitted;
		}
		return false;
	}

	public static void addLogOnOffer(Long offerId, String textToAdd) {
		String sId = ""+offerId;
		String query = "UPDATE single_offer SET log2 = CONCAT(log2, ' ', " + addSingleQuotes(textToAdd) + ") WHERE id = "+ addSingleQuotes(sId);
		dbConnection.execUpdate(query);
	}

	public static List<SingleOffer> retrieveOffers(Date dateMin, Date dateMax, String consumerFilter, String producerFilter, String addFilter) {
		List<SingleOffer> result = new ArrayList<SingleOffer>();
		if(dateMin==null) {
			return result;
		}
		String sDateMin = UtilDates.format_sql.format(dateMin);
		String sDateMax = UtilDates.format_sql.format(dateMax);
		String sConsumerFilter = consumerFilter==null? "1": "single_offer.consumer_agent = " +  addSingleQuotes(consumerFilter) + "";
		String sProducerFilter = producerFilter==null? "1": "single_offer.producer_agent = " +  addSingleQuotes(producerFilter) + "";
		String sAaddedFilter = (addFilter==null? "1" : addFilter);
		StringBuffer query = new StringBuffer();
		query.append( "SELECT single_offer.* ")
			 .append(CR).append(" ,prodEvent.begin_date AS prod_begin_date")
			 .append(CR).append(" ,prodEvent.expiry_date AS prod_expiry_date")
			 .append(CR).append(" ,prodEvent.power AS prod_power")
			 .append(CR).append(" ,prodEvent.power_min AS prod_power_min")
			 .append(CR).append(" ,prodEvent.power_max AS prod_power_max")
			 .append(CR).append(" ,prodEvent.device_name AS prod_device_name")
			 .append(CR).append(" ,prodEvent.device_category AS prod_device_category")
			 .append(CR).append(" ,prodEvent.environmental_impact AS prod_env_impact")
			 .append(CR).append(" ,prodEvent.time_shift_ms AS prod_time_shift_ms")
			 .append(CR).append(" ,requestEvent.begin_date AS req_begin_date")
			 .append(CR).append(" ,requestEvent.expiry_date AS  req_expiry_date")
			 .append(CR).append(" ,requestEvent.is_complementary AS req_is_complementary")
			 .append(CR).append(" ,requestEvent.agent AS req_agent")
			 .append(CR).append(" ,requestEvent.power AS req_power")
			 .append(CR).append(" ,requestEvent.power_min AS req_power_min")
			 .append(CR).append(" ,requestEvent.power_max AS req_power_max")
			 .append(CR).append(" ,requestEvent.device_name AS  req_device_name")
			 .append(CR).append(" ,requestEvent.environmental_impact AS req_env_impact")
			 .append(CR).append(" ,requestEvent.device_category AS  req_device_category")
			 .append(CR).append(" ,requestEvent.time_shift_ms AS req_time_shift_ms")
			 //.append(CR).append(" ,(select h.ID from history h where h.date <= single_offer.creation_time order by h.date desc limit 0,1) as id_histo")
			 .append(CR).append(" FROM single_offer ")
			 .append(CR).append(" LEFT JOIN event AS prodEvent ON prodEvent.id = single_offer.production_event_id")
			 .append(CR).append(" LEFT JOIN event AS requestEvent ON requestEvent.id = single_offer.request_event_id")
			 .append(CR).append(" WHERE single_offer.date >= ").append(addSingleQuotes(sDateMin))
			 .append(CR).append("    AND  single_offer.date < ") .append(addSingleQuotes(sDateMax))
			 .append(CR).append("    AND ").append(sConsumerFilter).append(" AND ").append(sProducerFilter)
			 .append(CR).append("    AND ").append(sAaddedFilter)
			 .append(CR).append(" ORDER BY single_offer.creation_time, single_offer.ID ");
		List<Map<String, Object>> rows = dbConnection.executeSelect(query.toString());
		for(Map<String, Object> row : rows) {
			EnergyRequest request  = null;
			Boolean accepted = (Boolean) row.get("accepted");
			Boolean used = (Boolean) row.get("used");
			Boolean acquitted = (Boolean) row.get("acquitted");
			Double reqPower = SapereUtil.getDoubleValue(row, "req_power");
			Double reqPowerMin = SapereUtil.getDoubleValue(row, "req_power_min");
			Double reqPowerMax = SapereUtil.getDoubleValue(row, "req_power_max");
			Date reqBeginDate =   (Date) row.get("req_begin_date");
			// Request is null if it comes from another node
			if(reqPower!=null && reqBeginDate != null) {
				Date reqExpiryDate = (Date) row.get("req_expiry_date");
				String sDeviceCat = "" + row.get("req_device_category");
				boolean resIsComplementary = SapereUtil.getBooleantValue(row, "req_is_complementary");
				long reqTimeShiftMS = SapereUtil.getLongValue(row, "req_time_shift_ms");
				DeviceCategory deviceCategory = DeviceCategory.getByName(sDeviceCat);
				EnvironmentalImpact envImpact = SapereUtil.getEnvironmentalImpactValue(row, "req_env_impact");
				Double tolerance = UtilDates.computeDurationMinutes(reqBeginDate, reqExpiryDate);
				PricingTable pricingTable = new PricingTable();
				DeviceProperties deviceProperties = new DeviceProperties( ""+ row.get("req_device_name"), deviceCategory, envImpact, false);
				request = new EnergyRequest("" + row.get("req_agent"), NodeManager.getLocation(), /*false */ resIsComplementary, reqPower, reqPowerMin, reqPowerMax
						, reqBeginDate, reqExpiryDate , tolerance, PriorityLevel.LOW, deviceProperties, pricingTable, reqTimeShiftMS);
			}
			EnergySupply supply = null;
			Double power = SapereUtil.getDoubleValue(row, "power");
			Double powerMin = SapereUtil.getDoubleValue(row, "power_min");
			Double powerMax = SapereUtil.getDoubleValue(row, "power_max");
			if(power!=null && request != null) {
				String producerAgent = "" + row.get("producer_agent");
				String sDeviceCat = "" + row.get("prod_device_category");
				DeviceCategory deviceCategory = DeviceCategory.getByName(sDeviceCat);
				EnvironmentalImpact envImpact = SapereUtil.getEnvironmentalImpactValue(row, "prod_env_impact");
				long timeShiftMS = SapereUtil.getLongValue(row, "prod_time_shift_ms");
				PricingTable pricingTable = new PricingTable();
				DeviceProperties deviceProperties = new DeviceProperties(""+row.get("prod_device_name"), deviceCategory, envImpact, true);
				supply = new EnergySupply(producerAgent, NodeManager.getLocation(), false, power, powerMin, powerMax
							, (Date) row.get("prod_begin_date") , (Date) row.get("prod_expiry_date"),deviceProperties, pricingTable, timeShiftMS);
				SingleOffer nextOffer = new SingleOffer(producerAgent, supply, 0, request);
				nextOffer.setDeadline((Date) row.get("deadline"));
				if( row.get("used_time")!=null) {
					nextOffer.setUsedTime((Date) row.get("used_time"));
				}
				if( row.get("used_time")!=null) {
					nextOffer.setUsedTime((Date) row.get("used_time"));
				}
				if( row.get("acceptance_time")!=null) {
					nextOffer.setAcceptanceTime((Date) row.get("acceptance_time"));
				}
				if( row.get("contract_time")!=null) {
					nextOffer.setContractTime((Date) row.get("contract_time"));
				}
				nextOffer.setCreationTime((Date) row.get("date"));
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
		List<Map<String, Object>> rows1 = dbConnection.executeSelect("SELECT * FROM transition_matrix WHERE location = "
					+ addSingleQuotes(context.getLocation())
					+ " AND scenario = " + addSingleQuotes(context.getScenario()));
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


	public static List<Device> retrieveNodeDevices(double powerCoeffProducer, double powerCoeffConsumer) {
		List<Device> result = new ArrayList<>();
		List<Map<String, Object>> rows = dbConnection.executeSelect("SELECT * FROM device");
		for(Map<String, Object> row: rows) {
			Long id = SapereUtil.getLongValue(row, "id");
			Boolean isProducer = (Boolean) row.get("is_producer");
			Double powerCoeff = isProducer.booleanValue() ?  powerCoeffProducer : powerCoeffConsumer;
			Double powerMin = powerCoeff*SapereUtil.getDoubleValue(row, "power_min");
			Double powerMax = powerCoeff*SapereUtil.getDoubleValue(row, "power_max");
			Double avergaeDuration =  SapereUtil.getDoubleValue(row, "avg_duration");
			DeviceCategory deviceCategory = SapereUtil.getDeviceCategoryValue(row, "category");
			EnvironmentalImpact envImpact = SapereUtil.getEnvironmentalImpactValue(row,  "environmental_impact");
			String deviceName = ""+row.get("name");
			DeviceProperties nextDeviceProperties = new DeviceProperties(deviceName, deviceCategory, envImpact, isProducer);
			nextDeviceProperties.setPriorityLevel(SapereUtil.getIntValue(row, "priority_level"));
			nextDeviceProperties.setProducer(isProducer);
			Device nextDevice = new Device(id, nextDeviceProperties, powerMin, powerMax, avergaeDuration);
			result.add(nextDevice);
		}
		return result;
	}

	public static Map<String, Double> retrieveDeviceStatistics(double powerCoeffConsumer, double powerCoeffProducer, List<DeviceCategory> categoryFilter, int hourOfDay) {
		Map<String, Double> result = new HashMap<String, Double>();
		String sCategoryFilter = SapereUtil.generateFilterCategory(categoryFilter, "device_category");
		List<Map<String, Object>> rows = dbConnection.executeSelect("SELECT * FROM device_statistic WHERE start_hour = " + hourOfDay + " AND " + sCategoryFilter);
		for(Map<String, Object> row: rows) {
			String category = "" + row.get("device_category");
			boolean isProducer = category.endsWith("_ENG");
			double powerCoeff = isProducer? powerCoeffProducer : powerCoeffConsumer;
			double power = powerCoeff*SapereUtil.getDoubleValue(row, "power");
			result.put(category, power);
		}
		return result;
	}

	public static List<Device> retrieveMeyrinDevices() {
		List<Map<String, Object>> rows = dbConnection.executeSelect("SELECT sensor.*"
				+ CR + ",sensor_input.*"
				+ CR + ",sensor_input.id AS id_sensor_input"
				+ CR + ",0 AS priority_level"
				+ CR + ",IF(device_category IN ('WIND_ENG', 'SOLOR_ENG',  'BIOMASS_ENG', 'HYDRO_ENG'), 2, 3) AS env_impact"
				+ CR + " FROM " + CLEMAPDATA_DBNAME + ".sensor "
				+ CR + " JOIN " + CLEMAPDATA_DBNAME + ".sensor_input on sensor_input.id_sensor = sensor.id"
				+ CR + " WHERE sensor.serial_number IN "
				//	+ CR + "      (SELECT sensor_number FROM " + CLEMAPDATA_DBNAME + ".measure_record mr WHERE `timestamp` >= DATE_ADD(NOW(), INTERVAL -10 DAY) GROUP BY sensor_number)");
				+ CR + "      (SELECT sensor_number FROM " + CLEMAPDATA_DBNAME + ".measure_record mr WHERE `timestamp` LIKE '2022-05-31%' OR `timestamp` LIKE '2022-06-28%' GROUP BY sensor_number)"
				+ CR + "	AND NOT sensor_input.is_disabled"
		);
		Map<String, Device> mapDevices = new HashMap<String, Device>();
		long maxId = 0*0;
		for(Map<String, Object> row: rows) {
			Long id = SapereUtil.getLongValue(row, "id_sensor_input");
			maxId = Math.max(id, maxId);
			//Boolean isProducer = SapereUtil.getBooleantValue (row,"is_producer");
			PhaseNumber phase = PhaseNumber.getByLabel(""+row.get("phase"));
			//double powerCoeff = 0;
			double powerMin = 0;
			double powerMax = 0;
			double avergaeDuration =  0;
			String deviceName = ""+row.get("description");
			if(mapDevices.containsKey(deviceName)) {
				Device device = mapDevices.get(deviceName);
				device.getProperties().addPhase(phase);
			} else {
				DeviceCategory deviceCategory = SapereUtil.getDeviceCategoryValue(row, "device_category");
				boolean isProducer = deviceCategory.isProducer();
				EnvironmentalImpact envImpact = SapereUtil.getEnvironmentalImpactValue(row,  "env_impact");
				DeviceProperties nextDeviceProperties = new DeviceProperties(deviceName, deviceCategory, envImpact, isProducer);
				nextDeviceProperties.setPriorityLevel(SapereUtil.getIntValue(row, "priority_level"));
				nextDeviceProperties.setProducer(isProducer);
				nextDeviceProperties.addPhase(phase);
				nextDeviceProperties.setLocation(""+row.get("location"));
				nextDeviceProperties.setElectricalPanel(""+row.get("panel_input"));
				nextDeviceProperties.setSensorNumber(""+row.get("serial_number"));
				Device nextDevice = new Device(id, nextDeviceProperties,powerMin, powerMax, avergaeDuration);
				mapDevices.put(deviceName, nextDevice);
				//result.add(nextDevice);
			}
		}
		List<Device> result = new ArrayList<>();
		for(Device nextDevice : mapDevices.values()) {
			result.add(nextDevice);
		}
		// Add 2 producers
		/*
		Device prodSolar = new Device(maxId+2, "Solar panel 1", DeviceCategory.SOLOR_ENG, EnvironmentalImpact.LOW, 0, 1000.00000, 0);
		prodSolar.setProducer(true);
		result.add(prodSolar);*/
		DeviceProperties prodSigProperties = new DeviceProperties("SIG", DeviceCategory.EXTERNAL_ENG, EnvironmentalImpact.MEDIUM, true);
		prodSigProperties.setThreePhases();
		Device prodSig = new Device(maxId+1, prodSigProperties, 0, 1500, 0);
		result.add(prodSig);
		return result;
	}

	public static DeviceMeasure retrieveLastDevicesMeasure(String featureType, Date dateBegin, Date dateEnd) {
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
				+CR	+",sensor_input.description  AS device_name"
				+CR + "FROM " + CLEMAPDATA_DBNAME +".measure_record"
				+CR	+ "JOIN " + CLEMAPDATA_DBNAME + ".sensor on sensor.serial_number = measure_record.sensor_number"
				+CR	+ "JOIN " + CLEMAPDATA_DBNAME + ".phase_measure_record AS phase_mr ON phase_mr.id_measure_record = measure_record.id"
				+CR	+ "JOIN " + CLEMAPDATA_DBNAME + ".sensor_input ON  sensor_input.id_sensor = sensor.id AND sensor_input.phase=phase_mr.phase"
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
			List<DeviceCategory> categoryFilter, String featureTypeFilter, Date dateBegin, Date dateEnd) {
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
			+CR	+",sensor_input.device_category"
			+CR	+",sensor_input.description  AS device_name"
			+CR	+",phase_mr.phase"
			+CR	+",ABS(phase_mr.power_p) AS power_p"
			+CR	+",ABS(phase_mr.power_q) AS power_q"
			+CR	+",ABS(phase_mr.power_s) AS power_s"
			+CR	+"FROM " + CLEMAPDATA_DBNAME +".measure_record"
			+CR	+"JOIN " + CLEMAPDATA_DBNAME + ".sensor on sensor.serial_number = measure_record.sensor_number"
			+CR	+"JOIN " + CLEMAPDATA_DBNAME + ".phase_measure_record AS phase_mr ON phase_mr.id_measure_record = measure_record.id"
			+CR	+"JOIN " + CLEMAPDATA_DBNAME + ".sensor_input ON  sensor_input.id_sensor = sensor.id AND sensor_input.phase=phase_mr.phase"
			+CR	+"WHERE  " + filterDateBegin + " AND " + filterEndDate + " AND " + sCategoryFilter + " AND " + sqlFilterFeatureType + " AND NOT sensor_input.is_disabled"
			+CR + "	ORDER BY measure_record.timestamp";
		List<Map<String, Object>> rows = dbConnection.executeSelect(sqlQuery);
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
			PhaseNumber phaseNumber = PhaseNumber.getByLabel("" + row.get("phase"));
			Double power_p = SapereUtil.getDoubleValue(row, "power_p");
			Double power_q = SapereUtil.getDoubleValue(row, "power_q");
			Double power_s = SapereUtil.getDoubleValue(row, "power_s");
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
}
