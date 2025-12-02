package com.sapereapi.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import org.json.JSONObject;

import com.sapereapi.agent.AgentBloodSearch;
import com.sapereapi.agent.AgentTransport;
import com.sapereapi.agent.QueryAgent;
import com.sapereapi.agent.ServiceAgent;
import com.sapereapi.agent.ServiceAgentWeb;
import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.agent.energy.IEnergyAgent;
import com.sapereapi.agent.energy.LearningAgent;
import com.sapereapi.agent.energy.ProsumerAgent;
import com.sapereapi.agent.energy.RegulatorAgent;
import com.sapereapi.db.ClemapDbHelper;
import com.sapereapi.db.DBConfig;
import com.sapereapi.db.DBConnectionFactory;
import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.db.PredictionDbHelper;
import com.sapereapi.db.SessionManager;
import com.sapereapi.helper.PredictionHelper;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.ChangeRequest;
import com.sapereapi.model.energy.DeviceProperties;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.EnergyFlow;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.ExtendedEnergyEvent;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.ProsumerProperties;
import com.sapereapi.model.energy.RegulationWarning;
import com.sapereapi.model.energy.StorageType;
import com.sapereapi.model.energy.forcasting.EndUserForcastingResult;
import com.sapereapi.model.energy.forcasting.ForcastingResult1;
import com.sapereapi.model.energy.forcasting.ForcastingResult2;
import com.sapereapi.model.energy.forcasting.input.EndUserForcastingRef;
import com.sapereapi.model.energy.forcasting.input.EndUserForcastingRequest;
import com.sapereapi.model.energy.input.AgentFilter;
import com.sapereapi.model.energy.input.AgentInputForm;
import com.sapereapi.model.energy.node.MultiNodesContent;
import com.sapereapi.model.energy.node.NodeContent;
import com.sapereapi.model.energy.policy.EmptyPricePolicy;
import com.sapereapi.model.energy.policy.IConsumerPolicy;
import com.sapereapi.model.energy.policy.IProducerPolicy;
import com.sapereapi.model.energy.policy.LowestPricePolicy;
import com.sapereapi.model.energy.policy.PolicyFactory;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.input.HistoryInitializationForm;
import com.sapereapi.model.input.HistoryInitializationRequest;
import com.sapereapi.model.input.InitializationForm;
import com.sapereapi.model.input.NeighboursUpdateRequest;
import com.sapereapi.model.input.Query;
import com.sapereapi.model.learning.ILearningModel;
import com.sapereapi.model.learning.LearningModelType;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.learning.PredictionScope;
import com.sapereapi.model.learning.VariableState;
import com.sapereapi.model.learning.VariableStateHistory;
import com.sapereapi.model.learning.aggregation.AbstractAggregationResult;
import com.sapereapi.model.learning.prediction.LearningAggregationOperator;
import com.sapereapi.model.learning.prediction.LearningAggregationType;
import com.sapereapi.model.learning.prediction.MultiPredictionsData;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.model.learning.prediction.PredictionData;
import com.sapereapi.model.learning.prediction.PredictionStatistic;
import com.sapereapi.model.learning.prediction.input.AggregationCheckupRequest;
import com.sapereapi.model.learning.prediction.input.MassivePredictionRequest;
import com.sapereapi.model.learning.prediction.input.MatrixFilter;
import com.sapereapi.model.learning.prediction.input.PredictionRequest;
import com.sapereapi.model.learning.prediction.input.PredictionScopeFilter;
import com.sapereapi.model.learning.prediction.input.StateHistoryRequest;
import com.sapereapi.model.learning.prediction.input.StatisticsRequest;
import com.sapereapi.model.referential.AgentType;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.model.referential.WarningType;
import com.sapereapi.util.PasswordUtils;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;
import com.sapereapi.util.UtilHttp;
import com.sapereapi.util.UtilJsonParser;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.agent.SapereAgent;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.lsa.Property;
import eu.sapere.middleware.node.NodeLocation;
import eu.sapere.middleware.node.NodeManager;

public class Sapere {
	public final static double YEAR_DURATION_MIN = 1.0 * 24 * 60 * 365;
	public final static String DB_SERVER = "energy";
	public final static String DB_CLEMAP = "clemap";
	private static List<QueryAgent> querys;
	public static List<SapereAgent> serviceAgents;
	private static Sapere instance = null;
	public static NodeManager nodeManager;
	private static Map<String, AgentAuthentication> mapAgentAuthentication = null;
	private static Set<AgentAuthentication> authentifiedAgentsCash = null;
	private static String salt = null;
	//private static int nextConsumerId = 1;
	//private static int nextProducerId = 1;
	private static int nextProsumerId = 1;
	private static boolean useSecuresPasswords = false;
	private static NodeContext nodeContext = null;	// Node context
	private static SapereLogger logger = null;
	public static boolean isRunning = false;
	private static PolicyFactory policyFactory = null; //new PolicyFactory();

	public static Sapere getInstance() {
		if (instance == null) {
			instance = new Sapere();
		}
		return instance;
	}

	public static OperationResult enableSupervision() {
		nodeContext.setSupervisionDisabled(false);
		OperationResult result = new OperationResult(true, "");
		return result;
	}

	public static boolean isActivatePrediction() {
		return nodeContext._isPredictionsActivated();
	}

	public static PolicyFactory getPolicyFactory() {
		return policyFactory;
	}

	public Sapere() {
		nodeManager = NodeManager.instance();
		logger = SapereLogger.getInstance();
		querys = new ArrayList<QueryAgent>();
		serviceAgents = new ArrayList<SapereAgent>();
		authentifiedAgentsCash = new HashSet<>();
		// Generate Salt. The generated value can be stored.
		salt = PasswordUtils.getSalt(2);
		mapAgentAuthentication = new HashMap<String, AgentAuthentication>();
		policyFactory = new PolicyFactory();
	}


	public static void init(ServerConfig serverConfig) throws HandlingException {
		//String defaultTimeZoneId = "GMT";
		//Map<Integer, Integer> dateTimeShift = new HashMap<Integer, Integer>();
		//logger.info("initEnergyService : scenario = " + defaultScenario);
		// initNodeManager(repository, environment);
		NodeManager.setConfiguration(serverConfig.getNodeLocation());
		logger = SapereLogger.getInstance();
		DBConfig dbConfig = serverConfig.getDbConfig();
		DBConnectionFactory.init(DB_SERVER, dbConfig, logger);
		DBConfig dbClemapConfig = serverConfig.getClemapDbConfig();
		DBConnectionFactory.init(DB_CLEMAP, dbClemapConfig, logger);
		EnergyDbHelper.init();
		ClemapDbHelper.init();
		PredictionDbHelper.init();
		if(serverConfig.getInitSqlScripts() != null) {
			for (String nextInitSqlSript : serverConfig.getInitSqlScripts()) {
				try {
					String scriptContent = SapereUtil.loadRessourceContent(nextInitSqlSript);
					logger.info("exec sql script " + serverConfig.getInitSqlScripts());
					DBConnectionFactory.getInstance(DB_SERVER).execUpdate(scriptContent);
				} catch (IOException e) {
					logger.error(e);
				}
			}
		}
		SessionManager.registerSession();
		NodeLocation nodeLocation = EnergyDbHelper.registerNodeLocation(serverConfig.getNodeLocation());
		nodeContext = new NodeContext();
		nodeContext.setMaxTotalPower(NodeStates.DEFAULT_NODE_MAX_TOTAL_POWER);
		nodeContext.setNodeLocation(nodeLocation);
		nodeContext.setDebugLevel(serverConfig.getDebugLevel());
		nodeContext.setScenario(serverConfig.getScenario());
		nodeContext.setTimeShiftMS(0);
		nodeContext.setDatetimeShifts(new HashMap<Integer, Integer>());
		nodeContext.setDebugLevel(serverConfig.getDebugLevel());
		nodeContext.setUrlForcasting(serverConfig.getUrlForcasting());
		nodeContext.setNodePredicitonSetting(serverConfig.getNodePredicitonSetting().clone());
		nodeContext.setClusterPredictionSetting(serverConfig.getClusterPredictionSetting().clone());
		String learningAgentName = generateAgentName(AgentType.LEARNING_AGENT);
		nodeContext.setLearningAgentName(learningAgentName);
		String regulatorAgentName = generateAgentName(AgentType.REGULATOR);
		nodeContext.setRegulatorAgentName(regulatorAgentName);
		Session session = SessionManager.getSession();
		nodeContext.setSession(session);
		TimeZone timeZone = TimeZone.getTimeZone(nodeContext.getTimeZoneId());
		UtilDates.setTimezone(timeZone);
		nodeContext = EnergyDbHelper.registerNodeContext(nodeContext, serverConfig.getDefaultNeighbours());
		initNodeManager2();
		PredictionHelper.setMaxTotalPower(nodeContext.getMaxTotalPower());
		PredictionHelper.setVariables(nodeContext.getVariables());
		PolicyFactory.setNodeContext(nodeContext);
	}

	public String getInfo() {
		return NodeManager.getNodeName() + " - " + NodeManager.getLocationAddress() + " -: "
				+ Arrays.toString(NodeManager.networkDeliveryManager.getNeighbours());
	}

	public NodeLocation getNodeLocation() {
		return nodeContext.getNodeLocation();
	}
/*
	public static int getDebugLevel() {
		return debugLevel;
	}

	public static void setDebugLevel(int debugLevel) {
		Sapere.debugLevel = debugLevel;
	}
*/
	public Map<String, NodeLocation> getMapAllNodeLocations(boolean addCurrentNode) {
		return NodeManager.getMapLocationsByNode(addCurrentNode);
	}

	public List<OptionItem> getStateDates(PredictionScopeFilter scopeFilter) throws HandlingException {
		PredictionContext predictionContext = getPredictionContext(scopeFilter);
		if (predictionContext != null && predictionContext.isPredictionsActivated()) {
			return PredictionDbHelper.getStateDates(predictionContext);
		}
		return new ArrayList<OptionItem>();
	}

	public String diffuseLsa(String lsaName, int hops) {
		for (SapereAgent service : serviceAgents) {
			if (lsaName.equals(service.getAgentName())) {
				service.addGradient(hops);
				break;
			}
		}
		return getLsa(lsaName);
	}

	public List<String> getLsa() {
		List<String> lsaList = new ArrayList<String>();

		for (Lsa lsa : NodeManager.instance().getSpace().getAllLsa().values()) {
			lsaList.add(lsa.toVisualString());
		}
		return lsaList;
	}

	public List<LsaForm> getLsasObj() {
		List<LsaForm> result = new ArrayList<LsaForm>();
		for (Lsa lsa : NodeManager.instance().getSpace().getAllLsa().values()) {
			LsaForm lsaForm = new LsaForm(lsa);
			result.add(lsaForm);
		}
		return result;
	}

	/**
	 * Test method to investigate problems in JSON outputs of getLsasObj for the
	 * display of LSA
	 * 
	 * @return
	 */
	public List<LsaForm> __getLsasObj() {
		List<LsaForm> result = new ArrayList<LsaForm>();
		for (Lsa lsa : NodeManager.instance().getSpace().getAllLsa().values()) {
			if (lsa.getAgentName().equals("Learning_agent_N1")) {
				Lsa lsa2 = lsa.copy();
				List<Property> toSet = new ArrayList<>();
				Property pTest = null;
				for (Property prop : lsa.getProperties()) {
					if (prop.getName().equals("MODEL") || prop.getName().equals("_PRED")) {
						prop.setValue("test");
					} else if (prop.getName().equals("PRED")) {
						Object value = prop.getValue();
						//PredictionData pd = (PredictionData) value;
						/*
						 * pd.setMapResults(new HashMap<>()); pd.getTargetDates().clear();
						 * pd.getMapLastResults().clear(); pd.setListSteps(new ArrayList<>()); pd = new
						 * PredictionData(); prop.setValue(pd); prop.setValue(new PredictionData());
						 * prop.setValue(""+pd.toString());
						 * prop.setValue("PredictionData [scenario = null : , initialStates=null\r\n");
						 * prop.setValue("PredictionData scenario = null : , initialStates=null");
						 * prop.setValue("FOO");
						 */
						//String svalue = "PredictionData [scenario = null : , initialStates=null\r\n";
						// svalue = svalue.replace("\r\n", "");
						// value = new PredictionData();
						pTest = new Property(prop.getName(), value);
						//StringBuffer testJson = UtilJsonParser.toJsonStr(prop, logger, 0);
						toSet.add(prop);
					}
				}
				lsa2.removeAllProperties();
				if (pTest != null) {
					lsa2.addProperty(pTest);
				}
				LsaForm lsaForm = new LsaForm(lsa2);
				result.add(lsaForm);
			}
		}
		return result;
	}

	public List<Service> getLsas() {
		List<Service> serviceList = new ArrayList<Service>();
		for (SapereAgent service : serviceAgents) {
			serviceList.add(new Service(service));
		}
		return serviceList;
	}

	public Map<String, Double[]> getQtable(String name) {
		for (SapereAgent serviceAgent : serviceAgents) {
			if (serviceAgent.getAgentName().equals(name)) {
				return serviceAgent.getQ();
			}
		}
		return null;
	}

	public String getLsa(String name) {
		String visualLsa = "";
		for (Lsa lsa : NodeManager.instance().getSpace().getAllLsa().values()) {
			if (name.equals(lsa.getAgentName())) {
				visualLsa = lsa.toVisualString();
				break;
			}
		}
		return visualLsa;
	}

	public String generateSimulation(Generate generate) {
		int number = generate.getNumber();
		String set = generate.getSet();
		String[] alph = set.split("-");
		if (alph.length == 2) {
			int size = nodeManager.getSpace().getAllLsa().size();
			List<String> alphabetSet = new ArrayList<String>();
			for (char c = alph[0].charAt(0); c <= alph[1].charAt(0); c++) {
				alphabetSet.add(Character.toString(c));
			}
			Random rand = new Random();
			for (int j = size; j < size + number; j++) {
				int input = rand.nextInt(alphabetSet.size());
				int output = input;
				while (output == input)
					output = rand.nextInt(alphabetSet.size() - 1);
				Service service = new Service("s" + j, new String[] { alphabetSet.get(input) },
						new String[] { alphabetSet.get(output) }, "", "");
				addServiceGeneric(service);
				startService(service.getName());
			}
			return "ok";
		} else
			return "set error";
	}

	public String updateAgents(Simulation simulation) {
		logger.info("Simulation updated");
		int size = nodeManager.getSpace().getAllLsa().size();
		for (int i = size; i < size + simulation.getNumber(); i++) {
			Service service = new Service("s" + i, simulation.getInput(), simulation.getOutput(), "", "");
			addServiceGeneric(service);
		}
		return "ok";
	}

	// Add and start a service
	public List<Service> addServiceGeneric(Service service) {
		AgentAuthentication authentication = generateAgentAuthentication(AgentType.GENERIC_SERVICE);
		authentication.setAgentName(service.getName());
		serviceAgents.add(new ServiceAgent(authentication, service.getInput(), service.getOutput(), LsaType.Service,
				nodeContext.isQualityOfServiceActivated()));
		startService(service.getName());
		return getLsas();
	}

	public void addServiceRest(Service service) {
		AgentAuthentication authentication = generateAgentAuthentication(AgentType.WEB_SERVICE);
		authentication.setAgentName(service.getName());
		serviceAgents.add(new ServiceAgentWeb(service.getUrl(), authentication, service.getInput(), service.getOutput(),
				service.getAppid(), LsaType.Service, nodeContext.isQualityOfServiceActivated()));
		startService(service.getName());
	}

	public void addServiceBlood(Service service) {
		addServiceBlood(service.getName(), null);
	}

	public void addServiceBlood(String name, String type) {
		AgentAuthentication authentication = generateAgentAuthentication(AgentType.BLOOD_SEARCH);
		authentication.setAgentName(name);
		serviceAgents.add(new AgentBloodSearch(authentication, new String[] { "Blood" }, new String[] { "Position" },
				LsaType.Service, nodeContext.isQualityOfServiceActivated()));
		startService(name);
	}

	public void addServiceTransport(Service service) {
		addServiceTransport(service.getName(), null);
	}

	public void addServiceTransport(String name, String type) {
		AgentAuthentication authentication = generateAgentAuthentication(AgentType.TRANSPORT);
		authentication.setAgentName(name);
		serviceAgents.add(new AgentTransport(authentication, new String[] { "Position", "Destination" },
				new String[] { "Transport" }, LsaType.Service, nodeContext.isQualityOfServiceActivated()));
		startService(name);
	}

	public List<Service> getServices() {
		List<Service> services = new ArrayList<Service>();
		for (SapereAgent service : serviceAgents) {
			services.add(new Service(service));
		}
		return services;
	}

	public List<String> getNodes() {
		HashSet<String> nodeSet = new HashSet<>();
		for (SapereAgent service : serviceAgents) {
			nodeSet.add(service.getInput()[0]);
			nodeSet.add(service.getOutput()[0]);
		}
		List<String> nodes = new ArrayList<String>(nodeSet);
		return nodes;
	}

	public static void startService(String name) {
		for (SapereAgent serviceAgent : serviceAgents) {
			if (serviceAgent.getAgentName().equals(name)) {
				serviceAgent.setInitialLSA();
			}
		}
	}

	public List<Service> addQuery(Query request) {
		AgentAuthentication authentication = generateAgentAuthentication(AgentType.GENERIC_QUERY);
		authentication.setAgentName(request.getName());
		QueryAgent queryAgent = new QueryAgent(request.getName(), authentication, request.getWaiting(),
				request.getProp(), request.getValues(), LsaType.Query, nodeContext.isQualityOfServiceActivated());
		querys.add(queryAgent);
		return getLsas();
	}

	public QueryAgent getQueryByName(String name) {
		for (QueryAgent query : querys) {
			if (query.getAgentName().equals(name)) {
				return query;
			}
		}
		return null;
	}

	public static List<QueryAgent> getQuerys() {
		return querys;
	}

	public static void setQuerys(List<QueryAgent> _querys) {
		querys = _querys;
	}

	// Energy
	private static SapereAgent getAgent(String agentName) {
		for (SapereAgent agent : serviceAgents) {
			if (agent.getAgentName().equals(agentName)) {
				return agent;
			}
		}
		// Agent not found : it can be a query agent
		for (QueryAgent agent : querys) {
			if (agent.getAgentName().equals(agentName)) {
				return agent;
			}
		}
		return null;
	}


	public static void initNodeManager2() {
		NodeManager.setConfiguration(nodeContext.getNodeLocation());
		List<String> listNegibours = nodeContext.getNeighbourNMainServiceAddresses();
		String[] arrayNegibours = new String[listNegibours.size()];
		listNegibours.toArray(arrayNegibours);
		NodeManager.networkDeliveryManager.setNeighbours(arrayNegibours);
		NodeManager.instance().clearMapDistance();
	}

	public static NodeContext getNodeContext() {
		return nodeContext;
	}


	public InitializationForm initEnergyService(ServerConfig serverConfig, InitializationForm initForm) throws HandlingException {
		logger.info("initEnergyService : scenario = " + initForm.getScenario());
		TimeZone timeZone = TimeZone.getTimeZone(initForm.getTimeZoneId());
		Double maxTotalPower = (initForm.getMaxTotalPower() == null) ? NodeStates.DEFAULT_NODE_MAX_TOTAL_POWER
				: initForm.getMaxTotalPower();
		boolean activateComplementaryRequests = true;
		//String sessionId = DBConnection.getSessionId();
		// Update node context
		nodeContext.setScenario(initForm.getScenario());
		nodeContext.setDatetimeShifts(initForm.generateDatetimeShifts());
		if(initForm.getDisableSupervision() != null) {
			nodeContext.setSupervisionDisabled(initForm.getDisableSupervision());
		}
		if(initForm.getNodePredicitonSetting() != null) {
			nodeContext.setNodePredicitonSetting(initForm.getNodePredicitonSetting());
		}
		if(initForm.getClusterPredictionSetting() != null) {
			nodeContext.setClusterPredictionSetting(initForm.getClusterPredictionSetting());
		}
		nodeContext.setTimeZoneId(initForm.getTimeZoneId());
		if(initForm.getUrlForcasting() != null) {
			nodeContext.setUrlForcasting(initForm.getUrlForcasting());
		}
		if(initForm.getActivateAwards() != null) {
			nodeContext.setAwardsActivated(initForm.getActivateAwards());
		}
		if (initForm.getEnergyStorageSetting() != null) {
			nodeContext.setGlobalEnergyStorageSetting(initForm.getEnergyStorageSetting());
		}
		nodeContext.setcomplementaryRequestsActivated(activateComplementaryRequests);
		nodeContext.setMaxTotalPower(maxTotalPower);
		UtilDates.setTimezone(timeZone);
		// TODO : debug case with scenario change and no scenario change
		nodeContext = EnergyDbHelper.registerNodeContext(nodeContext, serverConfig.getDefaultNeighbours());
		PolicyFactory.setNodeContext(nodeContext);
		PredictionHelper.setMaxTotalPower(nodeContext.getMaxTotalPower());
		PredictionHelper.setVariables(nodeContext.getVariables());
		if(nodeContext.getLearningAgentName() == null) {
			String learningAgentName = generateAgentName(AgentType.LEARNING_AGENT);
			nodeContext.setLearningAgentName(learningAgentName);
		}
		if (nodeContext.getRegulatorAgentName() == null) {
			String regulatorAgentName = generateAgentName(AgentType.REGULATOR);
			nodeContext.setRegulatorAgentName(regulatorAgentName);
		}
		SapereAgent toRemove = getAgent(nodeContext.getLearningAgentName());
		if (toRemove != null) {
			serviceAgents.remove(toRemove);
		}
		nextProsumerId = 1;
		//nextConsumerId = 1;
		//nextProducerId = 1;
		addServiceLearningAgent(nodeContext.getLearningAgentName());
		addServiceRegulatorAgent(nodeContext.getRegulatorAgentName());

		// Init stateVariable if requested in initForm
		String stateVariable = initForm.getInitialStateVariable();
		Integer stateId = initForm.getInitialStateId();
		if (stateId != null && stateVariable != null && !"".equals(stateVariable)) {
			VariableState targetState = NodeStates.getById(stateId);
			if (targetState != null) {
				Boolean disableSupervision = initForm.getDisableSupervision();
				initState(stateVariable, targetState, disableSupervision);
			}
		}
		// add storage agent if necessary
		if(nodeContext.getGlobalEnergyStorageSetting() != null && nodeContext.getGlobalEnergyStorageSetting().isCommon()) {
			addServiceStorage();
		}
		isRunning = true;
		return initForm;
	}


	public List<NodeLocation> retrieveAllNodeLocations() throws HandlingException{
		List<String> toExclude = new ArrayList<String>();
		toExclude.add(nodeContext.getNodeLocation().getName());
		return EnergyDbHelper.retrieveAllNodeLocations(toExclude);
	}

	public NodeContext updateNeighbours(NeighboursUpdateRequest request) throws HandlingException {
		List<String> listNeighboursNodes = new ArrayList<String>();
		if (request.getNeighbourNodes() != null) {
			listNeighboursNodes = Arrays.asList(request.getNeighbourNodes());
		}
		nodeContext = EnergyDbHelper.updateNeighbours(nodeContext, listNeighboursNodes);
		initNodeManager2();
		return nodeContext;
	}

	public String updateNodename(String nodename) throws HandlingException {
		nodeContext.getNodeLocation().setName(nodename);
		NodeLocation nodeLocation = nodeContext.getNodeLocation();
		nodeLocation = EnergyDbHelper.registerNodeLocation(nodeLocation);
		NodeManager.setConfiguration(nodeLocation);
		return "ok";
	}

	public void stopEnergyService() {
		// stopAllAgents1();
		nodeContext.setLearningAgentName(null);
		nodeContext.setRegulatorAgentName(null);
		nodeContext.setStorageAgentName(null);
		serviceAgents.clear();
		querys.clear();
		// nodeManager.stopServices();
		mapAgentAuthentication.clear();
		authentifiedAgentsCash.clear();
		nextProsumerId = 0;
		//nextConsumerId = 0;
		//nextProducerId = 0;
		isRunning = false;
		SessionManager.changeSessionNumber();
	}

	public void checkInitialisation() throws HandlingException {
		if (!isRunning) {
			return;
		}
		if(nodeContext == null) {
			return;
		}
		String learningAgentName = nodeContext.getLearningAgentName();
		if (getAgent(learningAgentName) == null) {
			addServiceLearningAgent(learningAgentName);
		}
		String regulatorAgentName = nodeContext.getRegulatorAgentName();
		if (getAgent(regulatorAgentName) == null) {
			addServiceRegulatorAgent(regulatorAgentName);
		}
	}

	public EnergySupply generateSupply(Double power, Date beginDate, Double durationMinutes,
			DeviceProperties deviceProperties, PricingTable pricingTable) {
		Date endDate = UtilDates.shiftDateMinutes(beginDate, durationMinutes);
		return generateSupply(power, beginDate, endDate, deviceProperties, pricingTable);
	}

	public EnergySupply generateSupply(Double power, Date beginDate, Date endDate, DeviceProperties deviceProperties,
			PricingTable pricingTable) {
		String issuer = generateAgentName(AgentType.PROSUMER);
		int issuerDistance = 0;
		long timeShiftMS = nodeContext.getTimeShiftMS();
		if (!deviceProperties.isProducer()) {
			logger.error("generateRequest : device should be a producer");
		}
		ProsumerProperties producerProperties = new ProsumerProperties(issuer, nodeContext.getNodeLocation(), issuerDistance, timeShiftMS, deviceProperties);
		return new EnergySupply(producerProperties, false, PowerSlot.create(power), beginDate, endDate, pricingTable, false);
	}

	public EnergyRequest generateRequest(Double power, Date beginDate, Double durationMinutes,
			Double delayToleranceMinutes, PriorityLevel _priority, DeviceProperties deviceProperties,
			PricingTable pricingTable) {
		Date endDate = UtilDates.shiftDateMinutes(beginDate, durationMinutes);
		if (!SapereUtil.checkPowerRounded(power, logger)) {
			logger.warning("generateRequest : power with more than 2 dec : " + power);
		}
		if (deviceProperties.isProducer()) {
			logger.error("generateRequest : device should not be a producer");
		}
		return generateRequest(power, beginDate, endDate, delayToleranceMinutes, _priority, deviceProperties);
	}

	public EnergyRequest generateRequest(Double power, Date beginDate, Date endDate, Double delayToleranceMinutes,
			PriorityLevel priority, DeviceProperties deviceProperties) {
		if (!SapereUtil.checkPowerRounded(power, logger)) {
			logger.warning("generateRequest : power with more than 2 dec : " + power);
		}
		String issuer = generateAgentName(AgentType.PROSUMER);
		long timeShiftMS = nodeContext.getTimeShiftMS();
		int issuerDistance = 0;
		ProsumerProperties consumerProperties = new ProsumerProperties(issuer, nodeContext.getNodeLocation(), issuerDistance, timeShiftMS, deviceProperties);
		return new EnergyRequest(consumerProperties, false, PowerSlot.create(power), beginDate, endDate,
				delayToleranceMinutes, priority, false);
	}

	public static String generateAgentName(AgentType agentType) {
		String radical = agentType.getPreffix() + "_" + NodeManager.getNodeName();
		if (AgentType.PROSUMER.equals(agentType)) {
			return radical + "_" + nextProsumerId;
		}
		/*
		if (AgentType.PRODUCER.equals(agentType)) {
			return radical + "_" + nextProducerId;
		} else if (AgentType.CONSUMER.equals(agentType)) {
			return radical + "_" + nextConsumerId;
		}*/
		return radical;
	}

	private static AgentAuthentication generateAgentAuthentication(AgentType agentType) {
		String agentName = generateAgentName(agentType);
		String authenticationKey = PasswordUtils.generateAuthenticationKey();
		String securedKey = useSecuresPasswords ? PasswordUtils.generateSecurePassword(authenticationKey, salt)
				: authenticationKey;
		AgentAuthentication authentication = new AgentAuthentication(agentName, agentType.name(), securedKey,
				nodeContext.getNodeLocation());
		mapAgentAuthentication.put(authentication.getAgentName(), authentication);
		return authentication;
	}

	private static InitializationForm generateDefaultInitForm(String scenario) {
		EnergyStorageSetting energyStorageSetting = new EnergyStorageSetting(false, false, StorageType.PRIVATE, 0.0, 0.0);
		LearningAggregationOperator aggregationOp = new LearningAggregationOperator(LearningAggregationType.MODEL, ILearningModel.OP_SAMPLING_NB, 5);
		PredictionSetting nodePredictionSetting = new PredictionSetting(true, aggregationOp, LearningModelType.MARKOV_CHAINS, 1);
		PredictionSetting clusterPredictionSetting = new PredictionSetting(false, null, null, 1);
		InitializationForm initForm = new InitializationForm(scenario
				,NodeStates.DEFAULT_NODE_MAX_TOTAL_POWER
				,new HashMap<Integer, Integer>()
				,energyStorageSetting,nodePredictionSetting, clusterPredictionSetting);
		initForm.setDisableSupervision(false);
		return initForm;
	}

	public List<Service> test1(ServerConfig serverConfig) throws HandlingException {
		// Add producer agents
		InitializationForm initForm = generateDefaultInitForm("test1");
		initForm.getNodePredicitonSetting().setActivated(false);
		initForm.getClusterPredictionSetting().setActivated(false);
		serverConfig.setDebugLevel(10);
		initEnergyService(serverConfig, initForm);
		Date current = getCurrentDate(); // getCurrentMinute();
		PricingTable pricingTable = new PricingTable(current, 0, getTimeShiftMS());
		EnergyStorageSetting energyStorageSetting = null;
		addServiceProducer(generateSupply(30., current, YEAR_DURATION_MIN,
				new DeviceProperties("SIG", DeviceCategory.EXTERNAL_ENG, EnvironmentalImpact.MEDIUM),
				pricingTable), energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());
		addServiceProducer(generateSupply(15., current, 40.,
				new DeviceProperties("wind turbine1", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW),
				pricingTable), energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());
		addServiceProducer(generateSupply(30., current, 30.,
				new DeviceProperties("wind turbine2", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW),
				pricingTable), energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());
		addServiceProducer(generateSupply(100., current, 6.,
				new DeviceProperties("wind turbine3", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW),
				pricingTable), energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());

		// Add query
		addServiceConsumer(generateRequest(43.1, current, YEAR_DURATION_MIN, YEAR_DURATION_MIN, PriorityLevel.LOW,
				new DeviceProperties("Refrigerator", DeviceCategory.COLD_APPLIANCES, EnvironmentalImpact.MEDIUM),
				pricingTable), energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());
		/*
		 * addQueryConsumer(generateRequest(27.7, current, 0.1, 150., PriorityLevel.LOW,
		 * "Laptop Compute", DeviceCategory.ICT));
		 * addQueryConsumer(generateRequest(72.7, current, 0.1, 80., PriorityLevel.LOW,
		 * " MacBook Pro ", DeviceCategory.ICT)); addQueryConsumer(generateRequest(10.0,
		 * current, 0.1, 10., PriorityLevel.LOW, "Led1", DeviceCategory.LIGHTING));
		 */
		// restartConsumer("Consumer_3", new Float("11"), current,
		// SapereUtil.shiftDateMinutes(current, 10), new Float(01));
		return getLsas();
	}

	public List<Service> test1bis(ServerConfig serverConfig) throws HandlingException {
		// Add producer agents
		initEnergyService(serverConfig, generateDefaultInitForm("test1bis"));
		Date current = getCurrentDate(); // getCurrentMinute();
		// addServiceProducer(generateSupply(new Float(30.0), current, new
		PricingTable pricingTable = new PricingTable(current, 0, getTimeShiftMS());
		IProducerPolicy producerPolicy = policyFactory.initDefaultProducerPolicy();
		EnergyStorageSetting energyStorageSetting = null;
		addServiceProducer(generateSupply(150.0, current, 200.0,
				new DeviceProperties("SIG", DeviceCategory.EXTERNAL_ENG, EnvironmentalImpact.MEDIUM),
				pricingTable), energyStorageSetting, producerPolicy, policyFactory.initDefaultConsumerPolicy());

		// Add query
		addServiceConsumer(generateRequest(43.1, current, YEAR_DURATION_MIN, YEAR_DURATION_MIN, PriorityLevel.LOW,
				new DeviceProperties("Refrigerator", DeviceCategory.COLD_APPLIANCES, EnvironmentalImpact.MEDIUM),
				pricingTable), energyStorageSetting, producerPolicy, policyFactory.initDefaultConsumerPolicy());

		return getLsas();
	}

	public List<Service> test1ter(ServerConfig serverConfig) throws HandlingException {
		// Add producer agents
		initEnergyService(serverConfig, generateDefaultInitForm("test1ter"));
		Date current = getCurrentDate(); // getCurrentMinute();
		PricingTable pricingTable = new PricingTable(current, 0, getTimeShiftMS());
		EnergyStorageSetting energyStorageSetting = null;
		addServiceProducer(generateSupply(2000.0, current, YEAR_DURATION_MIN,
				new DeviceProperties("SIG", DeviceCategory.EXTERNAL_ENG, EnvironmentalImpact.MEDIUM),
				pricingTable), energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());
		addServiceProducer(generateSupply(150.0, current, 120.0,
				new DeviceProperties("wind turbine1", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW),
				pricingTable), energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());
		addServiceProducer(generateSupply(300.0, current, 120.0,
				new DeviceProperties("wind turbine2", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW),
				pricingTable), energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());
		addServiceProducer(generateSupply(200.0, current, 120.0,
				new DeviceProperties("wind turbine3", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW),
				pricingTable), energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());
		/**/

		// Add query
		addServiceConsumer(generateRequest(43.1, current, YEAR_DURATION_MIN, YEAR_DURATION_MIN, PriorityLevel.HIGH,
				new DeviceProperties("Refrigerator", DeviceCategory.COLD_APPLIANCES, EnvironmentalImpact.MEDIUM),
				pricingTable), energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());
		addServiceConsumer(generateRequest(270.7, current, 150.0, 150., PriorityLevel.LOW,
				new DeviceProperties("Household Fan ", DeviceCategory.OTHER, EnvironmentalImpact.MEDIUM),
				pricingTable), energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());
		addServiceConsumer(generateRequest(720.7, current, 80., 80., PriorityLevel.LOW,
				new DeviceProperties(" Toaster", DeviceCategory.COOKING, EnvironmentalImpact.MEDIUM),
				pricingTable), energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());
		addServiceConsumer(generateRequest(100.0, current, 50., 50., PriorityLevel.LOW,
				new DeviceProperties("iPad / Tablet", DeviceCategory.ICT, EnvironmentalImpact.MEDIUM),
				pricingTable), energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());
		/**/

		// restartConsumer("Consumer_3", new Float("11"), current,
		// SapereUtil.shiftDateMinutes(current, 10), new Float(01));
		return getLsas();
	}

	public List<Service> test2(ServerConfig serverConfig) throws HandlingException {
		initEnergyService(serverConfig, generateDefaultInitForm("test2"));
		Date current = nodeContext.getCurrentDate(); // getCurrentMinute();
		PricingTable pricingTable = new PricingTable(current, 0, getTimeShiftMS());
		EnergyStorageSetting energyStorageSetting = null;
		int nbAgents = 10;
		for (int i = 0; i < nbAgents; i++) {
			IProducerPolicy producerPolicy = policyFactory.initDefaultProducerPolicy();
			addServiceProducer(generateSupply(25.0, current, 60.,
					new DeviceProperties("wind turbine " + i, DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW),
					pricingTable), energyStorageSetting, producerPolicy, policyFactory.initDefaultConsumerPolicy());
		}
		for (int i = 0; i < nbAgents; i++) {
			IProducerPolicy producerPolicy = policyFactory.initDefaultProducerPolicy();
			addServiceConsumer(generateRequest(30 + 0.1 * i, current, 120., 120., PriorityLevel.LOW,
					new DeviceProperties("Laptop " + i, DeviceCategory.ICT, EnvironmentalImpact.MEDIUM),
					pricingTable), energyStorageSetting, producerPolicy, policyFactory.initDefaultConsumerPolicy());
		}
		return getLsas();
	}

	public List<Service> test3(ServerConfig serverConfig) throws HandlingException {
		initEnergyService(serverConfig, generateDefaultInitForm("test3"));
		Date current = nodeContext.getCurrentDate(); // getCurrentMinute();
		PricingTable pricingTable = new PricingTable(current, 0, getTimeShiftMS());
		IProducerPolicy producerPolicy = policyFactory.initDefaultProducerPolicy();
		EnergyStorageSetting energyStorageSetting = null;
		addServiceProducer(generateSupply(30.0, current, YEAR_DURATION_MIN,
				new DeviceProperties("SIG", DeviceCategory.EXTERNAL_ENG, EnvironmentalImpact.MEDIUM),
				pricingTable), energyStorageSetting, producerPolicy, policyFactory.initDefaultConsumerPolicy());
		addServiceProducer(generateSupply(25.0, current, 60.,
				new DeviceProperties("wind turbine1", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW),
				pricingTable), energyStorageSetting, producerPolicy, policyFactory.initDefaultConsumerPolicy());
		addServiceProducer(generateSupply(25.0, current, 60.,
				new DeviceProperties("wind turbine2", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW),
				pricingTable), energyStorageSetting, producerPolicy, policyFactory.initDefaultConsumerPolicy());
		addServiceProducer(generateSupply(25.0, current, 60.,
				new DeviceProperties("wind turbine3", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW),
				pricingTable), energyStorageSetting, producerPolicy, policyFactory.initDefaultConsumerPolicy());
		addServiceConsumer(generateRequest(24.7, current, 150., 150., PriorityLevel.LOW,
				new DeviceProperties("Laptop 1", DeviceCategory.ICT, EnvironmentalImpact.MEDIUM), pricingTable),
				energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());
		return getLsas();
	}

	public List<Service> test4(ServerConfig serverConfig) throws HandlingException {
		initEnergyService(serverConfig, generateDefaultInitForm("test4"));
		Date current = nodeContext.getCurrentDate(); // getCurrentMinute();
		PricingTable pricingTable = new PricingTable(current,  0, getTimeShiftMS());
		// addServiceProducer(generateSupply(new Float(30.0), current,
		// YEAR_DURATION_MIN, "SIG", DeviceCategory.EXTERNAL_ENG));
		EnergyStorageSetting energyStorageSetting = null;
		DeviceProperties solorPanel = new DeviceProperties("solar panel1", DeviceCategory.SOLOR_ENG,
				EnvironmentalImpact.LOW);
		IProducerPolicy producerPolicy = policyFactory.initDefaultProducerPolicy();
		addServiceProducer(generateSupply(50.0, current, 10., solorPanel, pricingTable)
				, energyStorageSetting, producerPolicy, policyFactory.initDefaultConsumerPolicy());
		DeviceProperties tvLCD = new DeviceProperties("TV 32 LED/LCD ", DeviceCategory.AUDIOVISUAL,
				EnvironmentalImpact.MEDIUM);
		addServiceConsumer(generateRequest(97.7, current, 150., 150., PriorityLevel.LOW, tvLCD, pricingTable)
				, energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());
		int nbAgents = 5;
		for (int i = 0; i < nbAgents; i++) {
			DeviceProperties lapTop = new DeviceProperties("Laptop " + i, DeviceCategory.ICT,
					EnvironmentalImpact.MEDIUM);
			addServiceConsumer(
					generateRequest(30 + 0.1 * i, current, 120., 120., PriorityLevel.LOW, lapTop, pricingTable)
					, energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());
		}
		return getLsas();
	}

	public List<Service> test5(ServerConfig serverConfig) throws HandlingException {
		initEnergyService(serverConfig, generateDefaultInitForm("test5"));
		// Add producer agents
		Date current = getCurrentDate();
		EnergyStorageSetting energyStorageSetting = null;
		PricingTable pricingTable = new PricingTable(current, 0, getTimeShiftMS());
		DeviceProperties devicePropeties1 = new DeviceProperties("SIG", DeviceCategory.EXTERNAL_ENG,
				EnvironmentalImpact.MEDIUM);
		addServiceProducer(generateSupply(2700.0, current, YEAR_DURATION_MIN, devicePropeties1, pricingTable),
				energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());
		// Add query
		/*
		 * addQueryConsumer( generateRequest(new Float(10.0), current, new Float(0.5),
		 * new Float(0.5), PriorityLevel.LOW, "Led1", DeviceCategory.LIGHTING));
		 */
		return getLsas();
	}



	public static PricingTable initTragedyOfComPricingTable(int priceDurationMinutes) {
		Map<Integer, Double> simplePicingTable = new HashMap<Integer, Double>();
		if (nodeContext != null) {
			Date current = nodeContext.getCurrentDate();
			// Date end = UtilDates.shiftDateMinutes(current, 60);
			int time = 0;

			// simplePicingTable.put(time, 10.0);
			// time += 1;
			// simplePicingTable.put(time, 10.0);
			// time += 1;
			simplePicingTable.put(time, 6.0);
			time += priceDurationMinutes;
			simplePicingTable.put(time, 7.0);
			time += priceDurationMinutes;
			simplePicingTable.put(time, 8.0);
			time += priceDurationMinutes;
			simplePicingTable.put(time, 9.0);
			time += priceDurationMinutes;
			simplePicingTable.put(time, 10.0);
			time += 20 * priceDurationMinutes;
			simplePicingTable.put(time, 10.0);
			// Date lastStepDate = null;
			PricingTable pricingTable = new PricingTable(nodeContext.getTimeShiftMS());
			SortedSet<Integer> keys = new TreeSet<>(simplePicingTable.keySet());
			for (int step : keys) {
				Double rate = simplePicingTable.get(step);
				Date nextStepDate = UtilDates.shiftDateMinutes(current, step);
				pricingTable.putRate(nextStepDate, rate, null);
				// lastStepDate = nextStepDate;
			}
			return pricingTable;
		}
		return new PricingTable(0);
	}

	// Tragedy of the commons
	public List<Service> testTragedyOfTheCommons(ServerConfig serverConfig, boolean useDynamicPricingPolicy) throws HandlingException {
		initEnergyService(serverConfig, generateDefaultInitForm("testTragedyOfTheCommons"));
		/*
		int minutesByStep = 3;
		PricingTable pricingTable = initPricingTable(minutesByStep);// new PricingTable();
		*/
		String policyId = useDynamicPricingPolicy? PolicyFactory.POLICY_LOWEST_DEMAND : null;
		int priceDurationMinutes = 30;// 3;
		PricingTable pricingTable = initTragedyOfComPricingTable(priceDurationMinutes);
		IProducerPolicy producerPolicy = policyFactory.initProducerPolicy(policyId, false, 0);
		producerPolicy.setDefaultPricingTable(pricingTable);
		EnergyStorageSetting energyStorageSetting = null;
		/*
		IProducerPolicy producerPolicy = useDynamicPricingPolicy
				? new LowestDemandPolicy(pricingTable, IProducerPolicy.POLICY_PRIORITIZATION)
				: policyFactory.initDefaultProducerPolicy();
				*/
		DeviceProperties deviceSupplier = new DeviceProperties("SIG", DeviceCategory.EXTERNAL_ENG,
				EnvironmentalImpact.MEDIUM);
		Date current = nodeContext.getCurrentDate(); // getCurrentMinute();
		addServiceProducer(generateSupply(33.0, current, 180., deviceSupplier, producerPolicy.getDefaultPricingTable())
				, energyStorageSetting, producerPolicy, policyFactory.initDefaultConsumerPolicy());
		IConsumerPolicy consumerPolicy = useDynamicPricingPolicy ? new LowestPricePolicy() : new EmptyPricePolicy();
		int nbAgents = 10;
		//int priceDurationMinutes = PolicyFactory.getPriceDurationMinutes();
		//nbAgents = 1;
		for (int i = 0; i < nbAgents; i++) {
			Date dateBegin = UtilDates.shiftDateSec(current, 5);
			logger.info("testTragedyOfTheCommons current = " + UtilDates.format_date_time.format(current)
					+ ", dateBegin = " + UtilDates.format_date_time.format(dateBegin));
			//Date lastPriceDate = producerPolicy.getDefaultPricingTable().getEndDate();
			//Date dateEnd = UtilDates.shiftDateSec(lastPriceDate, priceDurationMinutes * 30);
			Date dateEnd = UtilDates.shiftDateSec(dateBegin, priceDurationMinutes * 60);
			Double power = 10.0;
			DeviceProperties devicePropeties2 = new DeviceProperties("Battery-" + (1+i), DeviceCategory.OTHER,
					EnvironmentalImpact.MEDIUM);
			EnergyRequest request = generateRequest(power, dateBegin, dateEnd, 120., PriorityLevel.LOW, devicePropeties2);
			addServiceConsumer(request, energyStorageSetting, policyFactory.initDefaultProducerPolicy() , consumerPolicy);
		}
		return getLsas();
	}

	public List<Service> test6(ServerConfig serverConfig) throws HandlingException {
		initEnergyService(serverConfig, generateDefaultInitForm("test6"));
		Date current = nodeContext.getCurrentDate(); // getCurrentMinute();
		EnergyStorageSetting energyStorageSetting = null;
		// addServiceProducer(generateSupply(new Float(30.0), current,
		// YEAR_DURATION_MIN, "SIG", DeviceCategory.EXTERNAL_ENG));
		//PricingTable pricingTable = new PricingTable(current, 0, getTimeShiftMS());
		DeviceProperties devicePropeties1 = new DeviceProperties("solar panel1", DeviceCategory.SOLOR_ENG,
				EnvironmentalImpact.LOW);
		IProducerPolicy producerPolicy = policyFactory.initDefaultProducerPolicy();
		addServiceProducer(generateSupply(33.0, current, 10., devicePropeties1, null), energyStorageSetting, producerPolicy, policyFactory.initDefaultConsumerPolicy());
		int nbAgents = 10;
		for (int i = 0; i < nbAgents; i++) {
			Date dateBegin = UtilDates.shiftDateSec(current, 5 + 65 * i);
			Date dateEnd = UtilDates.shiftDateSec(dateBegin, 60*3);
			DeviceProperties devicePropeties2 = new DeviceProperties("Battery " + (1 + i), DeviceCategory.OTHER,
					EnvironmentalImpact.MEDIUM);
			EnergyRequest request = generateRequest(10.0, dateBegin, dateEnd, 120., PriorityLevel.LOW, devicePropeties2);
			addServiceConsumer(request, energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());
		}
		return getLsas();
	}

	public List<Service> initState(String variable, VariableState targetState, boolean _supervisionDisabled) throws HandlingException {
		// initEnergyService(repository, "test5");
		// Add producer agents
		Date current = getCurrentDate();
		PricingTable pricingTable = new PricingTable(current, 0, getTimeShiftMS());
		EnergyStorageSetting energyStorageSetting = null;
		double minPower = targetState.getMinValue();
		double maxPower = targetState.getMaxValue() == null ? 1.7 * minPower : targetState.getMaxValue();
		double powerRandom = Math.random();
		double targetPower = minPower + (powerRandom * (maxPower - minPower));
		nodeContext.setSupervisionDisabled(_supervisionDisabled);
		if ("produced".equals(variable) || "available".equals(variable) || "consumed".equals(variable)
				|| "provided".equals(variable)) {
			IProducerPolicy producerPolicy = policyFactory.initDefaultProducerPolicy();
			DeviceProperties prodDeviceProperties = new DeviceProperties("wind turbine 0", DeviceCategory.WIND_ENG,
					EnvironmentalImpact.LOW);
			addServiceProducer(generateSupply(targetPower, current, 60., prodDeviceProperties, pricingTable),
					energyStorageSetting,
					producerPolicy, policyFactory.initDefaultConsumerPolicy());
		}
		if ("requested".equals(variable) || "missing".equals(variable) || "consumed".equals(variable)
				|| "provided".equals(variable)) {
			DeviceProperties consumerDeviceProperties = new DeviceProperties("Heat pump 0", DeviceCategory.HEATING,
					EnvironmentalImpact.MEDIUM);
			addServiceConsumer(generateRequest(targetPower, current, 120., 120., PriorityLevel.LOW,
					consumerDeviceProperties, pricingTable), energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());
		}
		try {
			LearningAgent learningAgent = getLearningAgent();
			learningAgent.refreshHistory(null);
			learningAgent.refreshLearningModel(PredictionScope.NODE, true, false);
			VariableState currentState = learningAgent.getCurrentVariableState(PredictionScope.NODE, variable);
			boolean stateReached = currentState != null && targetState.getId().equals(currentState.getId());
			int cpt = 0;
			while (!stateReached && cpt < 10) {
				Thread.sleep(5 * 1000);
				learningAgent.refreshHistory(null);
				learningAgent.refreshLearningModel(PredictionScope.NODE, true, false);
				currentState = learningAgent.getCurrentVariableState(PredictionScope.NODE, variable);
				stateReached = currentState != null && targetState.getId().equals(currentState.getId());
				cpt++;
			}
			if (stateReached) {
				logger.info("*** Sapere.initState : " + variable + " state " + currentState.getLabel() + " is reached");
			} else {
				logger.warning(
						"### Sapere.initState : " + variable + " state " + currentState.getLabel() + " is not reached");
			}
		} catch (Exception e) {
			logger.error(e);
		}
		// supervisionDisabled = false;

		// Add query
		return getLsas();
	}

	public boolean isAuthenticated(AgentAuthentication agentAuthentication) {
		String agentName = agentAuthentication.getAgentName();
		if (authentifiedAgentsCash.contains(agentAuthentication)) {
			return true;
		}
		String authenticationKey = agentAuthentication.getAuthenticationKey();
		boolean result = false;
		if (mapAgentAuthentication.containsKey(agentName)) {
			String key = (mapAgentAuthentication.get(agentName)).getAuthenticationKey();
			if (useSecuresPasswords) {
				String securedKey = PasswordUtils.generateSecurePassword(key, salt);
				result = securedKey.equals(authenticationKey);
			} else {
				result = key.equals(authenticationKey);
			}
		}
		if (result) {
			authentifiedAgentsCash.add(agentAuthentication);
		}
		return result;
	}

	private static void addServiceLearningAgent (
			String agentName
			// , String scenario, Map<Integer,Integer> _datetimeShifts
			) throws HandlingException{
		if (nodeContext == null) {
			throw new RuntimeException("addServiceLearningAgent : undefined node context");
		}
		String scenario = nodeContext.getScenario();
		if (scenario == null || scenario.length() == 0) {
			throw new RuntimeException("addServiceLearningAgent : undefined scenario");
		}
		AgentAuthentication authentication = generateAgentAuthentication(AgentType.LEARNING_AGENT);
		LearningAgent learningAgent = new LearningAgent(agentName, authentication, nodeContext);
		serviceAgents.add(learningAgent);
		startService(agentName);
	}

	private static void addServiceRegulatorAgent(String agentName) {
		AgentAuthentication authentication = generateAgentAuthentication(AgentType.REGULATOR);
		RegulatorAgent regulatorAgent = new RegulatorAgent(agentName, authentication, nodeContext);
		serviceAgents.add(regulatorAgent);
		startService(agentName);
	}

	private void addServiceStorage() {
		AgentAuthentication authentication = generateAgentAuthentication(AgentType.PROSUMER);
		Date current = getCurrentDate();
		PricingTable pricingTable = new PricingTable(current, 0, getTimeShiftMS());
		EnergySupply supply = generateSupply(0., current, 366 * 24 * 60.,
				new DeviceProperties("Common storage", DeviceCategory.BATTERY_ENG, EnvironmentalImpact.LOW),
				pricingTable);
		try {
			ProsumerAgent storageAgent = new ProsumerAgent(nextProsumerId, authentication, supply, null,
					nodeContext.getGlobalEnergyStorageSetting(), policyFactory.initDefaultProducerPolicy(),
					policyFactory.initDefaultConsumerPolicy(), nodeContext);
			nextProsumerId++;
			nodeContext.setStorageAgentName(storageAgent.getAgentName());
			serviceAgents.add(storageAgent);
			startService(storageAgent.getAgentName());
			logger.info(
					"addServiceStorage : add new " + storageAgent.computeRole() + " " + storageAgent.getAgentName());
		} catch (HandlingException e) {
			logger.error(e);
		}

	}

	private LearningAgent getLearningAgent() {
		for (SapereAgent agent : serviceAgents) {
			if (agent instanceof LearningAgent) {
				return (LearningAgent) agent;
			}
		}
		return null;
	}

	public RegulatorAgent getRegulatorAgent() {
		for (SapereAgent agent : serviceAgents) {
			if (agent instanceof RegulatorAgent) {
				return (RegulatorAgent) agent;
			}
		}
		return null;
	}

	public AgentForm addEnergyAgent(AgentInputForm agentInputForm) throws HandlingException {
		agentInputForm.checkContent();	// Throws an exception if the form is not complete
		resolveAgentsNotInsapce();
		AgentForm result = null;
		Date current = getCurrentDate();
		if (agentInputForm.getEndDate() != null && agentInputForm.getEndDate().before(current)) {
			logger.warning("#### addAgent : enddate = " + agentInputForm.getEndDate());
		}
		if (!SapereUtil.checkPowerRounded(agentInputForm.getPower(), logger)) {
			logger.warning("#### addAgent : power = " + SapereUtil.roundPower(agentInputForm.getPower()));
			double powerToSet = SapereUtil.roundPower(agentInputForm.getPower());
			agentInputForm.setPowers(powerToSet, powerToSet, powerToSet);
		}
		try {
			EnergyAgent newAgent = null;
			IProducerPolicy producerPolicy = policyFactory.initProducerPolicy(agentInputForm);
			IConsumerPolicy consumerPolicy = policyFactory.initConsumerPolicy(agentInputForm);
			if (agentInputForm.isProducer()) {
				agentInputForm.generateSimplePricingTable(0);
				newAgent = addServiceProducer(agentInputForm.getPower(), agentInputForm.getBeginDate(),
						agentInputForm.getEndDate(), agentInputForm.retrieveDeviceProperties(),
						agentInputForm.generatePricingTable(), agentInputForm.getEnergyStorageSetting()
						, producerPolicy, consumerPolicy);
			} else if (agentInputForm.isConsumer()) {
				agentInputForm.generateSimplePricingTable(0);
				PriorityLevel priority = agentInputForm.getPriorityLevel();
				double delayToleranceMinutes = agentInputForm.getDelayToleranceMinutes();
				if (delayToleranceMinutes == 0 && agentInputForm.getDelayToleranceRatio() != null) {
					delayToleranceMinutes = agentInputForm.getDelayToleranceRatio() * UtilDates
							.computeDurationMinutes(agentInputForm.getBeginDate(), agentInputForm.getEndDate());
				}
				newAgent = addServiceConsumer(agentInputForm.getPower(), agentInputForm.getBeginDate(),
						agentInputForm.getEndDate(), delayToleranceMinutes, priority,
						agentInputForm.retrieveDeviceProperties(), agentInputForm.generatePricingTable()
						,agentInputForm.getEnergyStorageSetting()
						,producerPolicy, consumerPolicy);
			}
			if (newAgent != null) {
				Thread.sleep(1 * 1000);
				result = generateAgentForm(newAgent);
			}
		} catch (Throwable e) {
			logger.error(e);
		}
		/*
		 * if(false && result!=null &&!result.checkInSpace()) {
		 * resolveAgentsNotInsapce(); }
		 */
		return result;
	}

	public ProsumerAgent addServiceConsumer(Double power, Date beginDate, Date endDate
			, Double delayToleranceMinutes,
			PriorityLevel priority, DeviceProperties deviceProperties, PricingTable pricingTable, EnergyStorageSetting energyStorageSetting
			,IProducerPolicy producerPolicy, IConsumerPolicy consumerPolicy) throws HandlingException {
		if(!deviceProperties.isConsumer()) {
			throw new HandlingException("addServiceConsumer : the givent category " +  deviceProperties.getCategory() + " is not a consumer category");
		}
		EnergyRequest request = generateRequest(power, beginDate, endDate, delayToleranceMinutes, priority,	deviceProperties);
		return addServiceConsumer(request, energyStorageSetting, producerPolicy, consumerPolicy);
	}

	private ProsumerAgent addServiceConsumer(EnergyRequest need, EnergyStorageSetting energyStorageSetting, IProducerPolicy producerPolicy, IConsumerPolicy consumerPolicy) throws HandlingException {
		EnergySupply supply = null;
		return  addServiceProsumer(supply, need, energyStorageSetting, producerPolicy, consumerPolicy);
	}

	private ProsumerAgent addServiceProducer(EnergySupply supply, EnergyStorageSetting energyStorageSetting, IProducerPolicy producerPolicy, IConsumerPolicy consumerPolicy) throws HandlingException {
		EnergyRequest need = null;
		return addServiceProsumer(supply, need, energyStorageSetting, producerPolicy, consumerPolicy);
	}

	private ProsumerAgent addServiceProsumer(EnergySupply supply, EnergyRequest need, EnergyStorageSetting energyStorageSetting
			, IProducerPolicy producerPolicy, IConsumerPolicy consumerPolicy) throws HandlingException {
		if (!isRunning) {
			return null;
		}
		if (producerPolicy == null) {
			logger.error("addServiceProsumer : producerPolicy is not set");
		}
		checkInitialisation();
		if(supply != null)  {
			supply.checkBeginNotPassed();
		}
		if(need != null) {
			need.checkBeginNotPassed();
		}
		synchronized (mapAgentAuthentication) {
			AgentAuthentication authentication = generateAgentAuthentication(AgentType.PROSUMER);
			ProsumerAgent prosumerAgent = new ProsumerAgent(nextProsumerId, authentication, supply, need, energyStorageSetting, producerPolicy, consumerPolicy, nodeContext);
			synchronized (prosumerAgent) {
				prosumerAgent.setInitialLSA();
			}
			nextProsumerId++;
			serviceAgents.add(prosumerAgent);
			logger.info("addServiceProsumer : add new " + prosumerAgent.computeRole() + " " +  prosumerAgent.getAgentName());
			return prosumerAgent;
		}
	}

	public ProsumerAgent addServiceProducer(Double power, Date beginDate, Date endDate,
			DeviceProperties deviceProperties
			, PricingTable pricingTable, EnergyStorageSetting energyStorageSetting
			, IProducerPolicy producerPolicy,  IConsumerPolicy consumerPolicy) throws HandlingException {
		if (!isRunning) {
			throw new HandlingException("Service is not running");
		}
		if(!deviceProperties.isProducer()) {
			throw new HandlingException("addServiceProducer : the givent category " +  deviceProperties.getCategory() + " is not a producer category");
		}
		EnergySupply supply = generateSupply(power, beginDate, endDate, deviceProperties, pricingTable);
		return this.addServiceProducer(supply, energyStorageSetting, producerPolicy, consumerPolicy);
	}

	public AgentForm restartEnergyAgent(AgentInputForm agentInputForm) {
		resolveAgentsNotInsapce();
		AgentForm result = null;
		Date current = getCurrentDate();
		if (agentInputForm.getEndDate() != null && agentInputForm.getEndDate().before(current)) {
			logger.warning("#### restartAgent : endadate = " + agentInputForm.getEndDate());
		}
		if (!SapereUtil.checkPowerRounded(agentInputForm.getPower(), logger)) {
			logger.warning("#### restartAgent : power = " + agentInputForm.getPower());
			double powerToSet = SapereUtil.roundPower(agentInputForm.getPower());
			agentInputForm.setPowers(powerToSet, powerToSet, powerToSet);
		}
		try {
			SapereAgent agent = null;
			if (agentInputForm.isConsumer()) {
				// Restart consumer agent
				agent = restartConsumer(agentInputForm.getAgentName(), agentInputForm.retrieveEnergyRequest(), agentInputForm.getEnergyStorageSetting());
			} else if (agentInputForm.isProducer()) {
				// Restart producer agent
				agent = restartProducer(agentInputForm.getAgentName(), agentInputForm.retrieveEnergySupply(), agentInputForm.getEnergyStorageSetting());
			}
			if (agent != null) {
				result = generateAgentForm(agent);
			}
		} catch (Exception e) {
			logger.error(e);
		}
		/*
		 * if(false && result!=null &&!result.checkInSpace()) {
		 * resolveAgentsNotInsapce(); }
		 */
		return result;
	}


	private void setAgentExpired(String agentName, RegulationWarning warning) {
		SapereAgent agent = getAgent(agentName);
		EnergyEvent stopEvent = this.getStopEvent(agentName);
		if (agent instanceof EnergyAgent) {
			EnergyAgent energyAgent = (EnergyAgent) agent;
			energyAgent.setEndDate(getCurrentDate());
		}
		if (stopEvent == null) {
			generateStopEvent(agentName, warning);
		}
	}

	public boolean isAgentStopped(String agentName) {
		SapereAgent agent = getAgent(agentName);
		boolean result = false;
		if (agent instanceof IEnergyAgent) {
			result = ((IEnergyAgent) agent).hasExpired();
		}
		return result;
	}

	public EnergyEvent getStopEvent(String agentName) {
		SapereAgent agent = getAgent(agentName);
		EnergyEvent result = null;
		if (agent instanceof IEnergyAgent) {
			result = ((IEnergyAgent) agent).getStopEvent();
		}
		return result;
	}

	public EnergyEvent generateStopEvent(String agentName, RegulationWarning warning) {
		SapereAgent agent = getAgent(agentName);
		EnergyEvent result = null;
		try {
			if (agent instanceof IEnergyAgent) {
				result = ((IEnergyAgent) agent).generateStopEvent(warning, "");
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return result;
	}

	public OperationResult stopListAgents(AgentInputForm agentInputForm) {
		resolveAgentsNotInsapce();
		Date current = getCurrentDate();
		long timeShiftMS = getTimeShiftMS();
		RegulationWarning warning = new RegulationWarning(WarningType.USER_INTERRUPTION, current, timeShiftMS);
		List<String> agentsToStop = new ArrayList<>();
		boolean isOK = false;
		String sListAgent = agentInputForm.getAgentName();
		for (String agentName : sListAgent.split(",")) {
			warning.addAgent(agentName);
			if (!isInSpace(agentName)) {
				// Agent already out of tuple space
				setAgentExpired(agentName, warning);
				// return this.getAgent(agentName);
			} else {
				agentsToStop.add(agentName);
			}
		}
		if (agentsToStop.size() == 0) {
			return new OperationResult(true, "");
		}
		// Use the regulator agent to send a user interruption
		SapereAgent regAgent = getAgent(nodeContext.getRegulatorAgentName());
		if (regAgent instanceof RegulatorAgent) {
			RegulatorAgent regulatorAgent = (RegulatorAgent) regAgent;
			regulatorAgent.interruptListAgents(agentsToStop);
		}
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			logger.error(e1);
		}
		int nbStopped = 0;
		int nbNotStopped = 0;
		for (String agentName : agentsToStop) {
			if (isInSpace(agentName)) {
				logger.info("stopAllAgents : agent " + agentName + " is in space");
				nbNotStopped++;
			} else {
				NodeManager.instance().getNotifier().unsubscribe(agentName);
				nbStopped++;
			}
		}
		logger.info("stopListAgents : nbStopped = " + nbStopped + ", nbNotStopped = " + nbNotStopped);
		isOK = (nbNotStopped == 0);
		return new OperationResult(isOK, "");
	}

	public AgentForm stopAgent(AgentInputForm agentInputForm) {
		resolveAgentsNotInsapce();
		AgentForm result = null;
		Date current = getCurrentDate();
		long timeShiftMS = getTimeShiftMS();
		RegulationWarning warning = new RegulationWarning(WarningType.USER_INTERRUPTION, current, timeShiftMS);
		warning.addAgent(agentInputForm.getAgentName());
		try {
			SapereAgent agent = auxStopAgent(agentInputForm.getAgentName(), warning);
			result = generateAgentForm(agent);
		} catch (Exception e) {
			logger.error(e);
		}
		cleanSubscriptions();
		return result;
	}

	public SapereAgent auxStopAgent(String agentName, RegulationWarning warning) {
		if (!isInSpace(agentName)) {
			// Agent already out of tuple space
			setAgentExpired(agentName, warning);
			return getAgent(agentName);
		}
		// Use the regulator agent to send a user interruption
		SapereAgent regAgent = getAgent(nodeContext.getRegulatorAgentName());
		if (regAgent instanceof RegulatorAgent) {
			RegulatorAgent regulatorAgent = (RegulatorAgent) regAgent;
			regulatorAgent.interruptAgent(agentName);
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			logger.error(e1);
		}
		int nbWaiting = 0;
		// Wait untill the agent is stopped
		while (isInSpace(agentName) && nbWaiting < 20) {
			try {
				Thread.sleep(100);
				nbWaiting++;
			} catch (InterruptedException e) {
				logger.error(e);
			}
		}
		logger.info("stopAgent " + agentName + ": isInSpace = " + isInSpace(agentName));
		// Wait untill the agent is stopped
		while (!isAgentStopped(agentName) && nbWaiting < 20) {
			try {
				Thread.sleep(100);
				if (!isAgentStopped(agentName) && !isInSpace(agentName)) {
					setAgentExpired(agentName, warning);
				}
				nbWaiting++;
			} catch (InterruptedException e) {
				logger.error(e);
			}
		}
		return getAgent(agentName);
	}

	public List<String> getRunningServiceAgents() {
		List<String> result = new ArrayList<String>();
		for (SapereAgent nextAgent : serviceAgents) {
			String agentName = nextAgent.getAgentName();
			if (isInSpace(agentName)) {
				logger.info("getRunningServiceAgents : agent " + nextAgent.getAgentName() + " is in space");
				result.add(agentName);
			}
		}
		return result;
	}

	public OperationResult stopAllAgents() {
		// Stop energy agents
		// Use the regulator agent to send a user interruption
		List<String> listRunningAgents = getRunningServiceAgents();
		if (listRunningAgents.size() > 0) {
			SapereAgent regAgent = getAgent(nodeContext.getRegulatorAgentName());
			synchronized (regAgent) {
				if (regAgent instanceof RegulatorAgent) {
					RegulatorAgent regulatorAgent = (RegulatorAgent) regAgent;
					regulatorAgent.interruptAllAgents(listRunningAgents);
				}
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				logger.error(e1);
			}
			int nbStopped = 0;
			int nbNotStopped = 0;
			for (SapereAgent nextAgent : serviceAgents) {
				if (isInSpace(nextAgent.getAgentName())) {
					logger.info("stopAllAgents : agent " + nextAgent.getAgentName() + " is in space");
					nbNotStopped++;
				} else {
					NodeManager.instance().getNotifier().unsubscribe(nextAgent.getAgentName());
					nbStopped++;
				}
			}
			logger.info("stopAllAgents1 : nbStopped = " + nbStopped + ", nbNotStopped = " + nbNotStopped);
			return new OperationResult(nbNotStopped == 0, "");
		} else {
			return new OperationResult(true, "");
		}
	}

	public EnergySupply getAgentSupply(String agentName) {
		EnergySupply result = null;
		SapereAgent agent = getAgent(agentName);
		if (agent instanceof EnergyAgent) {
			result = ((EnergyAgent) agent).getGlobalProduction();
		}
		if (result != null) {
			return result.clone();
		}
		return result;
	}

	public AgentForm modifyEnergyAgent(AgentInputForm agentInputForm) throws HandlingException {
		agentInputForm.checkContent();
		resolveAgentsNotInsapce();
		String agentName = agentInputForm.getAgentName();
		ProsumerRole prosumerRole = agentInputForm.getProsumerRole();
		EnergyFlow energySupply = (ProsumerRole.PRODUCER.equals(prosumerRole)) ?
				  agentInputForm.retrieveEnergySupply()
				: agentInputForm.retrieveEnergyRequest();
		if (!SapereUtil.checkPowerRounded(agentInputForm.getPower(), logger)) {
			logger.info("---- modifyAgent : power = " + agentInputForm.getPower());
			double powerToSet = SapereUtil.roundPower(agentInputForm.getPower());
			agentInputForm.setPowers(powerToSet, powerToSet, powerToSet);
		}
		agentInputForm.setTimeShiftMS(getTimeShiftMS());
		if (energySupply.getIssuerProperties().getLocation() == null) {
			energySupply.getIssuerProperties().setLocation(nodeContext.getNodeLocation());
		}
		SapereAgent regAgent = getAgent(nodeContext.getRegulatorAgentName());
		if (regAgent instanceof RegulatorAgent) {
			// Use the regulator agent to send a user interruption
			RegulatorAgent regulatorAgent = (RegulatorAgent) regAgent;
			ChangeRequest changeRequest = new ChangeRequest(agentName, energySupply, prosumerRole);
			regulatorAgent.modifyAgent(changeRequest);
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			logger.error(e);
		}
		int nbWait = 0;
		EnergySupply newAgentSpply = this.getAgentSupply(agentName);
		boolean updateDone = newAgentSpply != null
				&& (Math.abs(newAgentSpply.getPower() - energySupply.getPower()) < 0.001);
		while (!updateDone && nbWait < 10) {
			try {
				Thread.sleep(200);
				// Refresh agent supply
				newAgentSpply = this.getAgentSupply(agentName);
				// Refresh updateD
				updateDone = newAgentSpply != null
						&& (Math.abs(newAgentSpply.getPower() - energySupply.getPower()) < 0.001);
				nbWait++;
			} catch (InterruptedException e) {
				logger.error(e);
			}
		}
		SapereAgent agent = getAgent(agentName);
		return generateAgentForm(agent);
	}

	public PredictionData getPrediction(PredictionRequest predictionRequest) throws HandlingException {
		checkInitialisation();
		if (nodeContext.isPredictionsActivated(predictionRequest.getScopeEnum())) {
			LearningAgent learningAgent = this.getLearningAgent();
			return learningAgent.computePrediction2(predictionRequest);
		}
		return new PredictionData();
	}

	public MultiPredictionsData generateMassivePredictions(MassivePredictionRequest massivePredictionRequest) throws HandlingException {
		checkInitialisation();
		if (nodeContext.isPredictionsActivated(massivePredictionRequest.getScopeEnum())) {
			LearningAgent learningAgent = this.getLearningAgent();
			return learningAgent.generateMassivePredictions(massivePredictionRequest);
		}
		return new MultiPredictionsData();
	}

	public List<PredictionStatistic> computePredictionStatistics(StatisticsRequest statisticsRequest) throws HandlingException {
		List<PredictionStatistic> result = new ArrayList<PredictionStatistic>();
		if (nodeContext.isPredictionsActivated(statisticsRequest.getScopeEnum())) {
			try {
				LearningAgent learningAgent = this.getLearningAgent();
				result = learningAgent.computePredictionStatistics(statisticsRequest);
			} catch (Exception e) {
				logger.error(e);
				throw e;
			}
		}
		return result;
	}

	public AbstractAggregationResult checkupModelAggregation(AggregationCheckupRequest fedAvgCheckupRequest) {
		LearningAgent learningAgent = getLearningAgent();
		return learningAgent.checkupModelAggregation(fedAvgCheckupRequest);
	}

	public List<VariableStateHistory> retrieveLastHistoryStates(StateHistoryRequest stateHistoryRequest) throws HandlingException {
		checkInitialisation();
		PredictionScope scope = stateHistoryRequest.getScopeEnum();
		if (nodeContext.isPredictionsActivated(scope)) {
			LearningAgent learningAgent = this.getLearningAgent();
			Date minCreationDate = stateHistoryRequest.getMinDate();
			String variableName = stateHistoryRequest.getVariableName();
			boolean observationUpdated = stateHistoryRequest.getObservationUpdated();
			List<VariableStateHistory> result = learningAgent.retrieveLastHistoryStates(
					stateHistoryRequest.getScopeEnum(),
					minCreationDate,
					variableName, observationUpdated);
			return result;
		}
		return new ArrayList<VariableStateHistory>();
	}

	public ProsumerAgent restartProsumer(String agentName, EnergySupply supply, EnergyRequest need, EnergyStorageSetting energyStorageSetting) {
		if (!isRunning) {
			return null;
		}
		if (!(getAgent(agentName) instanceof ProsumerAgent)) {
			return null;
		}
		if (supply != null && supply.getIssuerProperties() != null && supply.getIssuerProperties().getLocation() == null) {
			supply.getIssuerProperties().setLocation(nodeContext.getNodeLocation());
		}
		if (need != null && need.getIssuerProperties() != null && need.getIssuerProperties().getLocation() == null) {
			need.getIssuerProperties().setLocation(nodeContext.getNodeLocation());
		}
		if (!isInSpace(agentName)) {
			logger.info("restartProsumer : restart agent " + agentName);
			// Restart consuer agent
			SapereAgent agent = getAgent(agentName);
			NodeManager.instance().getNotifier().unsubscribe(agentName);
			AgentAuthentication prosumerAuthentication = agent.getAuthentication();
			if(supply != null) {
				supply.checkBeginNotPassed();
			}
			if(need != null) {
				need.checkBeginNotPassed();
			}
			ProsumerAgent prosumerAgent = (ProsumerAgent) agent;
			IProducerPolicy producerPolicy = prosumerAgent.getProducerPolicy();
			IConsumerPolicy consumerPolicy = prosumerAgent.getConsumerPolicy();
			synchronized (prosumerAgent) {
				try {
					// Re-initialize consumer agent
					Integer id = prosumerAgent.getId();
					prosumerAgent.reinitialize(id, prosumerAuthentication, supply, need, energyStorageSetting, producerPolicy, consumerPolicy,
							nodeContext);
					if (!serviceAgents.contains(prosumerAgent)) {
						serviceAgents.add(prosumerAgent);
					}
					prosumerAgent.setInitialLSA();
					// Wait untill contract agent is in space
				} catch (Exception e) {
					logger.error(e);
				}
			}
		} else {
			logger.info("restartProsumer : " + agentName + " is already in tupple splace");
		}

		// Wait untill consumer agent is in sapce
		int nbWait = 0;
		while (!isInSpace(agentName) && nbWait < 30) {
			logger.info("restartProsumer : agent " + agentName + " not in sapce : Waiting ");
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				logger.error(e);
			}
			nbWait++;
		}
		logger.info("restartProsumer : agent " + agentName + " isInSpace = " + isInSpace(agentName));
		nbWait = 0;
		// Return consumer agent
		SapereAgent consumerAgent1 = getAgent(agentName);
		if (consumerAgent1 instanceof ProsumerAgent) {
			return (ProsumerAgent) (getAgent(agentName));
		}
		return null;
	}

	public ProsumerAgent restartConsumer(String consumerAgentName, EnergyRequest need, EnergyStorageSetting energyStorageSetting) {
		EnergySupply supply = null;
		logger.info("restartConsumer : needed power = " + need.getPower());
		return restartProsumer(consumerAgentName, supply, need, energyStorageSetting);
	}

	public ProsumerAgent restartProducer(String agentName, EnergySupply supply, EnergyStorageSetting energyStorageSetting) {
		EnergyRequest need = null;
		return restartProsumer(agentName, supply, need, energyStorageSetting);
	}

	public boolean isLocalAgent(String agentName) {
		boolean isLocal = false;
		if (mapAgentAuthentication.containsKey(agentName)) {
			AgentAuthentication authentication = mapAgentAuthentication.get(agentName);
			isLocal = (NodeManager.getLocationAddress().equals(authentication.getNodeLocation().getMainServiceAddress()));
		}
		return isLocal;
	}

	public boolean isInSpace(String agentName) {
		boolean result = NodeManager.instance().getSpace().getAllLsa().containsKey(agentName);
		if (!result) {
			// logger.info("isInSpace : for debug : " + agentName + " not in space");
		}
		return result;
	}

	public AgentForm generateAgentForm(SapereAgent agent) {
		if (agent == null) {
			return null;
		}
		boolean isInSpace = true;
		AgentForm result = null;
		// boolean isLocal =
		// NodeManager.isLocal(agent.getAuthentication().getNodeLocation());
		int distance = NodeManager.getDistance(agent.getAuthentication().getNodeLocation());
		if (agent instanceof EnergyAgent) {
			EnergyAgent energyAgent = (EnergyAgent) agent;
			isInSpace = isInSpace(energyAgent.getAgentName());
			result = new AgentForm(energyAgent, isInSpace, distance);
		}
		return result;
	}

	public NodeContent retrieveNodeContent() {
		AgentFilter filter = new AgentFilter();
		return retrieveNodeContent(filter);
	}

	public MultiNodesContent retrieveAllNodesContent(AgentFilter filter) throws HandlingException {
		if (nodeContext == null) {
			return new MultiNodesContent(new NodeContext(), null, 0);
		}
		long nodeTimeShiftMS = nodeContext.getTimeShiftMS();
		// NodeContent content = new NodeContent(null, nodeContext==null? 0 :
		// nodeContext.getTimeShiftMS());
		MultiNodesContent content = new MultiNodesContent(nodeContext, filter, nodeTimeShiftMS);
		content.setMapNeighborNodes(getMapAllNodeLocations(false));
		NodeContent currentNodeContent = retrieveNodeContent(filter);
		content.merge(currentNodeContent, 0);
		LearningAgent learningAgent = getLearningAgent();
		if(learningAgent == null) {
			throw new HandlingException("Service not initialized");
		}
		String[] filterNodes = filter.getNeighborNodeNames();
		Set<String> setFilterNodes = new HashSet<String>();
		if (filterNodes != null) {
			setFilterNodes = new HashSet<String>(Arrays.asList(filterNodes));
		}
		/*
		 * if(setFilterNodes.size() > 0) {
		 * logger.info("retrieveAllNodesContent : for debug"); }
		 */
		Collection<NodeLocation> neighborNodeLocations = NodeManager.getAllLocations(false);
		for (NodeLocation nextNodeLocation : neighborNodeLocations) {
			// call distant web service			
			String nextNodeName = nextNodeLocation.getName();
			if (setFilterNodes.contains(nextNodeName) || setFilterNodes.size() == 0) {
				String nextNodeUrl = nextNodeLocation.getUrl();
				if (nextNodeUrl != null) {
					Map<String, Object> params = UtilHttp.generateRequestParams(filter, UtilDates.format_json_datetime,
							logger, UtilHttp.METHOD_GET);
					// params = new HashMap<>();
					String postResponse = UtilHttp.sendGetRequest(nextNodeUrl + "retrieveNodeContent", params, logger,
							nodeContext.getDebugLevel());
					if (postResponse != null) {
						JSONObject jsonNodeContent = new JSONObject(postResponse);
						NodeContent neighbourNodeContant = UtilJsonParser.parseNodeContent(jsonNodeContent, logger);
						int neighbourDistance = NodeManager.getDistance(neighbourNodeContant.getNodeContext().getNodeLocation());
						content.merge(neighbourNodeContant, neighbourDistance);
					}
				}
			}
		}
		return content;
	}

	public NodeContent retrieveNodeContent(AgentFilter filter) {
		if (nodeContext == null) {
			return new NodeContent(new NodeContext(), null, 0);
		}
		long nodeTimeShiftMS = nodeContext == null ? 0 : nodeContext.getTimeShiftMS();
		NodeContent content = new NodeContent(nodeContext, filter, nodeTimeShiftMS);
		content.setMapNodeDistance(NodeManager.getMapDistanceByNode());
		content.setCurrentDate(getCurrentDate());
		content.setMapNeighborNodes(getMapAllNodeLocations(false));
		Map<String, Lsa> lsaInSpace = NodeManager.instance().getSpace().getAllLsa();
		content.setNoFilter(true);
		/*
		 * if(filter.getNeighborNodeNames() != null &&
		 * filter.getNeighborNodeNames().length > 0) {
		 * logger.info("retrieveNodeContent : for debug"); }
		 */
		for (SapereAgent agent : serviceAgents) {
			if (filter.applyFilter(agent)) {
				//String agentNode = agent.getAuthentication().getNodeLocation().getName();
				NodeLocation agentLocation = agent.getAuthentication().getNodeLocation();
				int distance = NodeManager.getDistance(agentLocation);
				boolean inSpace = lsaInSpace.containsKey(agent.getAgentName());
				if (agent instanceof ProsumerAgent) {
					ProsumerAgent prosumerAgent = (ProsumerAgent) agent;
					// producer.setInSpace(inSpace);
					content.addProsumer(prosumerAgent, inSpace, distance);
				}
				/*
				if (agent instanceof ProducerAgent) {
					ProducerAgent producer = (ProducerAgent) agent;
					// producer.setInSpace(inSpace);
					content.addProducer(producer, inSpace, distance);
				} else if (agent instanceof ConsumerAgent) {
					ConsumerAgent consumer = (ConsumerAgent) agent;
					// consumer.getLinkedAgents();
					// consumer.setInSpace(inSpace);
					content.addConsumer(consumer, inSpace, distance);
				}*/
			} else {
				content.setNoFilter(false);
			}
		}
		content.sortAgents();
		content.computeTotal();
		return content;
	}

	public NodeContent restartLastNodeContent() throws HandlingException {
		Date current = getCurrentDate();
		List<ExtendedEnergyEvent> events = EnergyDbHelper.retrieveLastSessionEvents(current);
		this.checkInitialisation();
		EnergyStorageSetting energyStorageSetting = null;
		for (EnergyEvent event : events) {
			if (EventType.PRODUCTION_START.equals(event.getType())) {
				PricingTable pricingTable = new PricingTable(nodeContext.getTimeShiftMS());
				EnergySupply supply = generateSupply(event.getPower(), getCurrentDate(), event.getEndDate(),
						event.getIssuerProperties().getDeviceProperties(), pricingTable);
				IProducerPolicy producerPolicy = policyFactory.initDefaultProducerPolicy();
				this.addServiceProducer(supply, energyStorageSetting, producerPolicy, policyFactory.initDefaultConsumerPolicy());
			} else if (EventType.REQUEST_START.equals(event.getType())) {
				current = getCurrentDate();
				double delayToleranceMinutes = UtilDates.computeDurationMinutes(current, event.getEndDate());
				//PricingTable pricingTable = new PricingTable(nodeContext.getTimeShiftMS());
				EnergyRequest request = generateRequest(event.getPower(), current, event.getEndDate(),
						delayToleranceMinutes, PriorityLevel.LOW, event.getIssuerProperties().getDeviceProperties());
				this.addServiceConsumer(request, energyStorageSetting, policyFactory.initDefaultProducerPolicy(), policyFactory.initDefaultConsumerPolicy());
			}
		}
		return this.retrieveNodeContent();
	}

	public ProsumerAgent getProsumerAgent(String agentName) {
		SapereAgent agent = getAgent(agentName);
		if (agent instanceof ProsumerAgent) {
			ProsumerAgent consumer = (ProsumerAgent) agent;
			return consumer;
		}
		return null;
	}

	public boolean isConsumerSatified(String agentName) {
		ProsumerAgent agent = getProsumerAgent(agentName);
		if (agent != null && agent.isConsumer()) {
			return agent.isSatisfied();
		}
		return false;
	}
	/*
	 * public String getConsumerConfirmTag(String agentName) { ConsumerAgent agent =
	 * getConsumerAgent(agentName); if(agent!=null) { return agent.getConfirmTag();
	 * } return null; }
	 */

	public PredictionContext getPredictionContext(PredictionScopeFilter scopeFilter) {
		LearningAgent learningAgent = getLearningAgent();
		PredictionScope scope = scopeFilter.getScopeEnum();
		if (learningAgent != null && nodeContext.isPredictionsActivated(scope)) {
			return learningAgent.getPredictionContext(scope);
		}
		return null;
	}

	public ILearningModel getLearningModel(MatrixFilter matrixFilter) {
		if (matrixFilter.getNodeName() == null || matrixFilter.getNodeName() == "" && nodeContext != null) {
			matrixFilter.setNodeName(nodeContext.getNodeLocation().getName());
		}
		LearningAgent learningAgent = getLearningAgent();
		if (learningAgent != null && nodeContext.isPredictionsActivated(matrixFilter.getScopeEnum())) {
			ILearningModel result = learningAgent.getLearningModelCopy(matrixFilter);
			return result;
		}
		return null;
	}

	public ILearningModel initNodeHistory(HistoryInitializationForm historyInitForm) throws HandlingException {
		LearningAgent learningAgent = getLearningAgent();
		if (learningAgent != null) {
			ILearningModel result1 = learningAgent.initNodeHistory(historyInitForm);
			return result1;
		}
		return null;
	}

	public ILearningModel initNodeHistory(HistoryInitializationRequest historyInitRequest) throws HandlingException {
		LearningAgent learningAgent = getLearningAgent();
		if (learningAgent != null) {
			ILearningModel result1 = learningAgent.initNodeHistory(historyInitRequest);
			return result1;
		}
		return null;
	}


	public void logProducerAgents(boolean onlyProducer) {
		for (SapereAgent agent : serviceAgents) {
			if (agent instanceof ProsumerAgent) {
				ProsumerAgent prosumerAgent = (ProsumerAgent) agent;
				if(!onlyProducer  || prosumerAgent.isProducer()) {
					prosumerAgent.logAgent();
				}
			}
		}
	}

	public void resolveAgentsNotInsapce() {
		for (String agentName : checkupNotInSpace()) {
			callSetInitialLSA(agentName);
		}
	}

	public void callSetInitialLSA(String agentName) {
		if (!isInSpace(agentName)) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error(e);
			}
			if (!isInSpace(agentName)) {
				SapereAgent agent = getAgent(agentName);
				synchronized (agent) {
					agent.setInitialLSA();
				}
			}
		}
	}

	public List<String> checkupNotInSpace() {
		List<String> result = new ArrayList<>();
		for (SapereAgent agent : serviceAgents) {
			if (!isInSpace(agent.getAgentName())) {
				boolean hasExpired = true;
				if (agent instanceof EnergyAgent) {
					hasExpired = ((EnergyAgent) agent).hasExpired();
				}
				if (!hasExpired) {
					result.add(agent.getAgentName());
				}
			}
		}
		return result;
	}

	/**
	 * Cautious : do not call this method from an agent (causes a
	 * ConcurrentModificationException) THis mtho
	 */
	public void cleanSubscriptions() {
		Map<String, Integer> nbSubscriptions = NodeManager.instance().getNotifier().getNbSubscriptionsByAgent();
		for (SapereAgent agent : serviceAgents) {
			if (!isInSpace(agent.getAgentName()) && isAgentStopped(agent.getAgentName())) {
				if (nbSubscriptions.containsKey(agent.getAgentName())) {
					NodeManager.instance().getNotifier().unsubscribe(agent.getAgentName());
				}
			}
		}
		for (QueryAgent agent : querys) {
			if (!isInSpace(agent.getAgentName()) && isAgentStopped(agent.getAgentName())) {
				if (nbSubscriptions.containsKey(agent.getAgentName())) {
					NodeManager.instance().getNotifier().unsubscribe(agent.getAgentName());
				}
			}
		}
		int totalSubscriptions = NodeManager.instance().getNotifier().getNbSubscriptions();
		logger.info("after cleanSubscriptions : nbSubscriptions = " + totalSubscriptions);
	}

	public void checkupProsumerAgents() {
		for (SapereAgent agent : serviceAgents) {
			if (agent instanceof ProsumerAgent) {
				ProsumerAgent prosumerAgent = (ProsumerAgent) agent;
				prosumerAgent.checkup();
			}
		}
	}

	public static boolean isSupervisionDisabled() {
		return nodeContext.isSupervisionDisabled();
	}

	public long getTimeShiftMS() {
		return nodeContext.getTimeShiftMS();
	}

	public Date getCurrentDate() {
		return nodeContext.getCurrentDate();
	}

	/*
	 * public void movePropertiesOnLSA(String agentName) { for (Lsa lsa:
	 * NodeManager.instance().getSpace().getAllLsa().values()) {
	 * if(lsa.getAgentName().equals(agentName)) { lsa.removeAllProperties(); } } }
	 */

	public EndUserForcastingResult getForcasting(EndUserForcastingRequest endUserForcastingRequest) throws HandlingException {
		LearningAgent learningAgent = getLearningAgent();
		return learningAgent.getForcasting(endUserForcastingRequest);
	}

	public ForcastingResult1 generateMockForcasting1(Map<String, Double> forcastingRequest) {
		LearningAgent learningAgent = getLearningAgent();
		return learningAgent.generateMockForcasting1(forcastingRequest);
	}

	public ForcastingResult2 generateMockForcasting2(Map<String, Double> forcastingRequest) {
		LearningAgent learningAgent = getLearningAgent();
		return learningAgent.generateMockForcasting2(forcastingRequest);
	}

	public EndUserForcastingRef getEndUserForcastingRef() throws HandlingException {
		String[] tab_min = {"00", "15", "30", "45"};
		EndUserForcastingRef result = new EndUserForcastingRef();
		List<OptionItem> listTimes = new ArrayList<>();
		for (int hourIdx = 0; hourIdx < 24; hourIdx++) {
			String hh = "" + (hourIdx < 10 ? "0": "") + hourIdx;
			for (String mm : tab_min) {
				String sTime = hh+":"+mm;
				listTimes.add(new OptionItem(sTime, sTime));
			}
		}
		result.setListOfTimes(listTimes);
		Calendar calendar = Calendar.getInstance();
		int minutes = calendar.get(Calendar.MINUTE);
		int rest = minutes % 15;
		int minutes2 = minutes - rest;
		calendar.set(Calendar.MINUTE, minutes2);
		if(rest > 7.5) {
			calendar.add(Calendar.MINUTE, 15);
		}
		Date defaultDate = calendar.getTime();
		String sDefaultDate = UtilDates.format_time.format(defaultDate);
		sDefaultDate = sDefaultDate.substring(0,5);
		result.setDefaultTime(sDefaultDate);
		TimeSlot interval = EnergyDbHelper.getInstance().retrieveTValueInterval();
		if(interval != null) {
			result.setDatesInterval(interval);
		}
		// List of years
		List<OptionItem> listOfYears = new ArrayList<>();
		if(interval != null) {
			calendar.setTime(interval.getBeginDate());
			int year = calendar.get(Calendar.YEAR);
			while(calendar.getTime().before(interval.getEndDate())) {
				listOfYears.add(new OptionItem(""+year, ""+year));
				calendar.add(Calendar.YEAR, 1);
				year = calendar.get(Calendar.YEAR);
			}
		}
		result.setListOfYears(listOfYears);
		// List of months
		List<OptionItem> listOfMonths = new ArrayList<>();
		for(int monthIdx = 1; monthIdx <= 12; monthIdx++) {
			String smonth = (monthIdx < 10 ? "0" : "") + monthIdx;
			listOfMonths.add(new OptionItem(smonth, smonth));
		}
		result.setListOfMonths(listOfMonths);
		// List of days
		List<OptionItem> listOfDays = new ArrayList<>();
		for(int dayIdx = 1; dayIdx <= 31; dayIdx++) {
			String sday = (dayIdx < 10 ? "0" : "") + dayIdx;
			listOfDays.add(new OptionItem(sday, sday));
		}
		result.setListOfDays(listOfDays);
		return result;
	}
}
