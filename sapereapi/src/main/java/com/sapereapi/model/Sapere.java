package com.sapereapi.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import com.sapereapi.agent.energy.ConsumerAgent;
import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.agent.energy.IEnergyAgent;
import com.sapereapi.agent.energy.LearningAgent;
import com.sapereapi.agent.energy.ProducerAgent;
import com.sapereapi.agent.energy.RegulatorAgent;
import com.sapereapi.db.DBConnection;
import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.db.PredictionDbHelper;
import com.sapereapi.helper.PredictionHelper;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.DeviceProperties;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.ExtendedEnergyEvent;
import com.sapereapi.model.energy.MultiNodesContent;
import com.sapereapi.model.energy.NodeContent;
import com.sapereapi.model.energy.OptionItem;
import com.sapereapi.model.energy.PricingTable;
import com.sapereapi.model.energy.RegulationWarning;
import com.sapereapi.model.energy.forcasting.EndUserForcastingResult;
import com.sapereapi.model.energy.forcasting.ForcastingResult;
import com.sapereapi.model.energy.forcasting.input.EndUserForcastingRef;
import com.sapereapi.model.energy.forcasting.input.EndUserForcastingRequest;
import com.sapereapi.model.energy.input.AgentFilter;
import com.sapereapi.model.energy.input.AgentInputForm;
import com.sapereapi.model.energy.policy.BasicProducerPolicy;
import com.sapereapi.model.energy.policy.IConsumerPolicy;
import com.sapereapi.model.energy.policy.IProducerPolicy;
import com.sapereapi.model.energy.policy.LowestDemandPolicy;
import com.sapereapi.model.energy.policy.LowestPricePolicy;
import com.sapereapi.model.input.InitializationForm;
import com.sapereapi.model.input.NeighboursUpdateRequest;
import com.sapereapi.model.input.Query;
import com.sapereapi.model.markov.MarkovState;
import com.sapereapi.model.markov.MarkovStateHistory;
import com.sapereapi.model.markov.MarkovTimeWindow;
import com.sapereapi.model.markov.NodeMarkovStates;
import com.sapereapi.model.markov.NodeTransitionMatrices;
import com.sapereapi.model.markov.TransitionMatrix;
import com.sapereapi.model.prediction.FedAvgResult;
import com.sapereapi.model.prediction.MultiPredictionsData;
import com.sapereapi.model.prediction.PredictionContext;
import com.sapereapi.model.prediction.PredictionData;
import com.sapereapi.model.prediction.PredictionStatistic;
import com.sapereapi.model.prediction.input.FedAvgCheckupRequest;
import com.sapereapi.model.prediction.input.MassivePredictionRequest;
import com.sapereapi.model.prediction.input.MatrixFilter;
import com.sapereapi.model.prediction.input.PredictionRequest;
import com.sapereapi.model.prediction.input.StateHistoryRequest;
import com.sapereapi.model.prediction.input.StatisticsRequest;
import com.sapereapi.model.referential.AgentType;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.PriorityLevel;
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
import eu.sapere.middleware.node.NodeConfig;
import eu.sapere.middleware.node.NodeManager;

public class Sapere {
	public final static int NB_DEC_POWER = 3;
	private List<QueryAgent> querys;
	public static List<SapereAgent> serviceAgents;
	private static Sapere instance = null;
	public NodeManager nodeManager;
	private Map<String, AgentAuthentication> mapAgentAuthentication = null;
	private Set<AgentAuthentication> authentifiedAgentsCash = null;
	public static String learningAgentName = generateAgentName(AgentType.LEARNING_AGENT);
	public static String regulatorAgentName = generateAgentName(AgentType.REGULATOR);
	public static String[] DEFAULT_VARIABLES = { "requested", "produced", "consumed", "provided", "available",
			"missing" };
	private String salt = null;
	private static int nextConsumerId = 1;
	private static int nextProducerId = 1;
	private boolean useSecuresPasswords = false;
	// Node context
	private static NodeContext nodeContext = null;
	private static SapereLogger logger = null;
	static int debugLevel = 0;
	public final static double YEAR_DURATION_MIN = 1.0 * 24 * 60 * 365;
	public static boolean isRunning = false;
	private boolean activateQoS = false;

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
		return nodeContext.isPredictionsActivated();
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
	}

	public String getInfo() {
		return NodeManager.getNodeName() + " - " + NodeManager.getLocation() + " -: "
				+ Arrays.toString(NodeManager.networkDeliveryManager.getNeighbours());
	}

	public NodeConfig getNodeConfig() {
		return nodeContext.getNodeConfig();
	}

	public Map<String, NodeConfig> getMapAllNodeConfigs(boolean addCurrentNode) {
		LearningAgent learningAgent = getLearningAgent();
		return learningAgent.getMapAllNodeConfigs(addCurrentNode);
	}

	public List<OptionItem> getStateDates() {
		if (nodeContext.isPredictionsActivated()) {
			return PredictionDbHelper.getStateDates();
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
						PredictionData pd = (PredictionData) value;
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
				activateQoS));
		startService(service.getName());
		return getLsas();
	}

	public void addServiceRest(Service service) {
		AgentAuthentication authentication = generateAgentAuthentication(AgentType.WEB_SERVICE);
		authentication.setAgentName(service.getName());
		serviceAgents.add(new ServiceAgentWeb(service.getUrl(), authentication, service.getInput(), service.getOutput(),
				service.getAppid(), LsaType.Service, activateQoS));
		startService(service.getName());
	}

	public void addServiceBlood(Service service) {
		addServiceBlood(service.getName(), null);
	}

	public void addServiceBlood(String name, String type) {
		AgentAuthentication authentication = generateAgentAuthentication(AgentType.BLOOD_SEARCH);
		authentication.setAgentName(name);
		serviceAgents.add(new AgentBloodSearch(authentication, new String[] { "Blood" }, new String[] { "Position" },
				LsaType.Service, activateQoS));
		startService(name);
	}

	public void addServiceTransport(Service service) {
		addServiceTransport(service.getName(), null);
	}

	public void addServiceTransport(String name, String type) {
		AgentAuthentication authentication = generateAgentAuthentication(AgentType.TRANSPORT);
		authentication.setAgentName(name);
		serviceAgents.add(new AgentTransport(authentication, new String[] { "Position", "Destination" },
				new String[] { "Transport" }, LsaType.Service, activateQoS));
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
				request.getProp(), request.getValues(), LsaType.Query, activateQoS);
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

	public List<QueryAgent> getQuerys() {
		return querys;
	}

	public void setQuerys(List<QueryAgent> querys) {
		this.querys = querys;
	}

	// Energy
	private SapereAgent getAgent(String agentName) {
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

	public static void setLocation(NodeConfig envNodeConfig) {
		/*
		 * Integer lsaServerPort =
		 * Integer.valueOf(environment.getProperty("lsa_server.port")); String
		 * lsaServerHost = String.valueOf(environment.getProperty("lsa_server.host"));
		 * String nodeName = String.valueOf(environment.getProperty("lsa_server.name"));
		 * Integer restServerPort =
		 * Integer.valueOf(environment.getProperty("server.port")); NodeConfig
		 * nodeConfig = new NodeConfig(nodeName, lsaServerHost, lsaServerPort,
		 * restServerPort);
		 */
		NodeManager.setConfiguration(envNodeConfig);
	}

	public void initNodeManager2() {
		NodeManager.setConfiguration(nodeContext.getNodeConfig());
		List<String> listNegibours = nodeContext.getNeighbourNMainServiceAddresses();
		String[] arrayNegibours = new String[listNegibours.size()];
		listNegibours.toArray(arrayNegibours);
		NodeManager.networkDeliveryManager.setNeighbours(arrayNegibours);
	}

	public static NodeContext getNodeContext() {
		return nodeContext;
	}

	public InitializationForm initEnergyService(NodeConfig envNodeConfig, InitializationForm initForm,
			List<NodeConfig> defaultNeighbours) {
		logger.info("initEnergyService : scenario = " + initForm.getScenario());
		// initNodeManager(repository, environment);
		TimeZone timeZone = TimeZone.getTimeZone(initForm.getTimeZoneId());
		DBConnection.changeSessionId();
		Double maxTotalPower = (initForm.getMaxTotalPower() == null) ? NodeMarkovStates.DEFAULT_NODE_MAX_TOTAL_POWER
				: initForm.getMaxTotalPower();
		/*
		 * String nodeName = NodeManager.getNodeName(); String host =
		 * NodeManager.getLocalIP(); int mainServerPort = NodeManager.getLocalPort();
		 * Integer restServerPort =
		 * Integer.valueOf(environment.getProperty("server.port")); NodeConfig
		 * nodeConfig = new NodeConfig(nodeName, host, mainServerPort, restServerPort);
		 */
		NodeConfig nodeConfig = EnergyDbHelper.registerNodeConfig(envNodeConfig);
		NodeManager.getNodeConfig().setId(nodeConfig.getId());
		boolean activateComplementaryRequests = true;
		boolean activateAggregation = true;
		nodeContext = new NodeContext(null, nodeConfig, initForm.getScenario(), initForm.generateDatetimeShifts(),
				maxTotalPower, DBConnection.getSessionId(), DEFAULT_VARIABLES, initForm.getDisableSupervision(),
				activateComplementaryRequests, activateAggregation, initForm.isActivatePredictions(),
				timeZone, initForm.getUrlForcasting());
		UtilDates.setTimezone(timeZone);
		nodeContext = EnergyDbHelper.registerContext(nodeContext);
		if (defaultNeighbours != null) {
			for (NodeConfig nextNodeConfig : defaultNeighbours) {
				EnergyDbHelper.registerNodeConfig(nextNodeConfig);
				nodeContext.addNeighbourNodeConfig(nextNodeConfig);
			}
		}
		initNodeManager2();
		PredictionHelper.setMaxTotalPower(nodeContext.getMaxTotalPower());
		PredictionHelper.setVariables(nodeContext.getVariables());
		learningAgentName = generateAgentName(AgentType.LEARNING_AGENT);
		regulatorAgentName = generateAgentName(AgentType.REGULATOR);
		SapereAgent toRemove = getAgent(learningAgentName);
		if (toRemove != null) {
			serviceAgents.remove(toRemove);
		}
		nextConsumerId = 1;
		nextProducerId = 1;
		addServiceLearningAgent(learningAgentName);
		addServiceRegulatorAgent(regulatorAgentName);
		// Init markov state if requested in initForm
		String stateVariable = initForm.getInitialStateVariable();
		Integer stateId = initForm.getInitialStateId();
		if (stateId != null && stateVariable != null && !"".equals(stateVariable)) {
			MarkovState targetState = NodeMarkovStates.getById(stateId);
			if (targetState != null) {
				Boolean disableSupervision = initForm.getDisableSupervision();
				initState(stateVariable, targetState, disableSupervision);
			}
		}
		isRunning = true;
		return initForm;
	}

	public List<NodeConfig> retrieveAllNodeConfigs() {
		List<Long> toExclude = new ArrayList<>();
		toExclude.add(nodeContext.getNodeConfig().getId());
		return EnergyDbHelper.retrieveAllNodeConfigs(toExclude);
	}

	public NodeContext updateNeighbours(NeighboursUpdateRequest request) {
		List<Long> listNeighboursConfigId = new ArrayList<>();
		if (request.getNeighboursConfigId() != null) {
			listNeighboursConfigId = Arrays.asList(request.getNeighboursConfigId());
		}
		nodeContext = EnergyDbHelper.updateNeighbours(nodeContext, listNeighboursConfigId);
		initNodeManager2();
		return nodeContext;
	}

	public String updateNodename(String nodename) {
		nodeContext.getNodeConfig().setName(nodename);
		NodeConfig nodeConfig = nodeContext.getNodeConfig();
		nodeConfig = EnergyDbHelper.registerNodeConfig(nodeConfig);
		NodeManager.setConfiguration(nodeConfig);
		return "ok";
	}

	public void stopEnergyService() {
		// stopAllAgents1();
		learningAgentName = null;
		regulatorAgentName = null;
		serviceAgents.clear();
		querys.clear();
		// nodeManager.stopServices();
		mapAgentAuthentication.clear();
		authentifiedAgentsCash.clear();
		nextConsumerId = 0;
		nextProducerId = 0;
		isRunning = false;
		DBConnection.changeSessionId();
	}

	public void checkInitialisation() {
		if (!isRunning) {
			return;
		}
		if (getAgent(learningAgentName) == null) {
			addServiceLearningAgent(learningAgentName);
		}
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
		String issuer = generateAgentName(AgentType.PRODUCER);
		int issuerDistance = 0;
		long timeShiftMS = nodeContext.getTimeShiftMS();
		if (!deviceProperties.isProducer()) {
			logger.error("generateRequest : device should be a producer");
		}
		return new EnergySupply(issuer, nodeContext.getNodeConfig(), issuerDistance, false, power, power, power,
				beginDate, endDate, deviceProperties, pricingTable, timeShiftMS);
	}

	public EnergyRequest generateRequest(Double power, Date beginDate, Double durationMinutes,
			Double _delayToleranceMinutes, PriorityLevel _priority, DeviceProperties deviceProperties,
			PricingTable pricingTable) {
		Date endDate = UtilDates.shiftDateMinutes(beginDate, durationMinutes);
		if (!SapereUtil.checkIsRound(power, NB_DEC_POWER, logger)) {
			logger.warning("generateRequest : power with more than 2 dec : " + power);
		}
		if (deviceProperties.isProducer()) {
			logger.error("generateRequest : device should not be a producer");
		}
		return generateRequest(power, beginDate, endDate, _delayToleranceMinutes, _priority, deviceProperties,
				pricingTable);
	}

	public EnergyRequest generateRequest(Double power, Date beginDate, Date endDate, Double _delayToleranceMinutes,
			PriorityLevel _priority, DeviceProperties deviceProperties, PricingTable pricingTable) {
		if (!SapereUtil.checkIsRound(power, NB_DEC_POWER, logger)) {
			logger.warning("generateRequest : power with more than 2 dec : " + power);
		}
		String issuer = generateAgentName(AgentType.CONSUMER);
		long timeShiftMS = nodeContext.getTimeShiftMS();
		int issuerDistance = 0;
		return new EnergyRequest(issuer, nodeContext.getNodeConfig(), issuerDistance, false, power, power, power,
				beginDate, endDate, _delayToleranceMinutes, _priority, deviceProperties, pricingTable, timeShiftMS);
	}

	public static String generateAgentName(AgentType agentType) {
		String radical = agentType.getPreffix() + "_" + NodeManager.getNodeName();
		if (AgentType.PRODUCER.equals(agentType)) {
			return radical + "_" + nextProducerId;
		} else if (AgentType.CONSUMER.equals(agentType)) {
			return radical + "_" + nextConsumerId;
		}
		return radical;
	}

	private AgentAuthentication generateAgentAuthentication(AgentType agentType) {
		String agentName = generateAgentName(agentType);
		String authenticationKey = PasswordUtils.generateAuthenticationKey();
		String securedKey = useSecuresPasswords ? PasswordUtils.generateSecurePassword(authenticationKey, salt)
				: authenticationKey;
		AgentAuthentication authentication = new AgentAuthentication(agentName, agentType.getLabel(), securedKey,
				nodeContext.getNodeConfig());
		this.mapAgentAuthentication.put(authentication.getAgentName(), authentication);
		return authentication;
	}

	private static InitializationForm generateDefaultInitForm(String scenario) {
		boolean activatePredictions = true;
		boolean activateAggregation = true;
		InitializationForm initForm = new InitializationForm(scenario, NodeMarkovStates.DEFAULT_NODE_MAX_TOTAL_POWER,
				new HashMap<Integer, Integer>(), activatePredictions, activateAggregation);
		initForm.setDisableSupervision(false);
		return initForm;
	}

	public List<Service> test1(NodeConfig envNodeConfig) {
		// Add producer agents
		InitializationForm initForm = generateDefaultInitForm("test1");
		initForm.setActivatePredictions(false);
		initEnergyService(envNodeConfig, initForm, new ArrayList<NodeConfig>());
		Date current = getCurrentDate(); // getCurrentMinute();
		PricingTable pricingTable = new PricingTable(current, UtilDates.shiftDateDays(current, 365), 0,
				getTimeShiftMS());
		IProducerPolicy producerPolicy = initDefaultProducerPolicy();
		addServiceProducer(generateSupply(30., current, YEAR_DURATION_MIN,
				new DeviceProperties("EDF", DeviceCategory.EXTERNAL_ENG, EnvironmentalImpact.MEDIUM, true),
				pricingTable), producerPolicy);
		addServiceProducer(generateSupply(15., current, 40.,
				new DeviceProperties("wind turbine1", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true),
				pricingTable), producerPolicy);
		addServiceProducer(generateSupply(30., current, 30.,
				new DeviceProperties("wind turbine2", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true),
				pricingTable), producerPolicy);
		addServiceProducer(generateSupply(100., current, 6.,
				new DeviceProperties("wind turbine3", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true),
				pricingTable), producerPolicy);

		// Add query
		addServiceConsumer(generateRequest(43.1, current, YEAR_DURATION_MIN, YEAR_DURATION_MIN, PriorityLevel.LOW,
				new DeviceProperties("Refrigerator", DeviceCategory.COLD_APPLIANCES, EnvironmentalImpact.MEDIUM, false),
				pricingTable), null);
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

	public List<Service> test1bis(NodeConfig envNodeConfig) {
		// Add producer agents
		initEnergyService(envNodeConfig, generateDefaultInitForm("test1bis"), new ArrayList<NodeConfig>());
		Date current = getCurrentDate(); // getCurrentMinute();
		// addServiceProducer(generateSupply(new Float(30.0), current, new
		PricingTable pricingTable = new PricingTable(current, UtilDates.shiftDateDays(current, 365), 0,
				getTimeShiftMS());
		IProducerPolicy producerPolicy = initDefaultProducerPolicy();
		addServiceProducer(generateSupply(150.0, current, 200.0,
				new DeviceProperties("EDF", DeviceCategory.EXTERNAL_ENG, EnvironmentalImpact.MEDIUM, true),
				pricingTable), producerPolicy);

		// Add query
		addServiceConsumer(generateRequest(43.1, current, YEAR_DURATION_MIN, YEAR_DURATION_MIN, PriorityLevel.LOW,
				new DeviceProperties("Refrigerator", DeviceCategory.COLD_APPLIANCES, EnvironmentalImpact.MEDIUM, false),
				pricingTable), null);

		return getLsas();
	}

	public List<Service> test1ter(NodeConfig envNodeConfig) {
		// Add producer agents
		initEnergyService(envNodeConfig, generateDefaultInitForm("test1ter"), new ArrayList<NodeConfig>());
		Date current = getCurrentDate(); // getCurrentMinute();
		PricingTable pricingTable = new PricingTable(current, UtilDates.shiftDateDays(current, 365), 0,
				getTimeShiftMS());
		IProducerPolicy producerPolicy = initDefaultProducerPolicy();
		addServiceProducer(generateSupply(2000.0, current, YEAR_DURATION_MIN,
				new DeviceProperties("EDF", DeviceCategory.EXTERNAL_ENG, EnvironmentalImpact.MEDIUM, true),
				pricingTable), producerPolicy);
		addServiceProducer(generateSupply(150.0, current, 120.0,
				new DeviceProperties("wind turbine1", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true),
				pricingTable), producerPolicy);
		addServiceProducer(generateSupply(300.0, current, 120.0,
				new DeviceProperties("wind turbine2", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true),
				pricingTable), producerPolicy);
		addServiceProducer(generateSupply(200.0, current, 120.0,
				new DeviceProperties("wind turbine3", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true),
				pricingTable), producerPolicy);
		/**/

		// Add query
		addServiceConsumer(generateRequest(43.1, current, YEAR_DURATION_MIN, YEAR_DURATION_MIN, PriorityLevel.HIGH,
				new DeviceProperties("Refrigerator", DeviceCategory.COLD_APPLIANCES, EnvironmentalImpact.MEDIUM, false),
				pricingTable), null);
		addServiceConsumer(generateRequest(270.7, current, 150.0, 150., PriorityLevel.LOW,
				new DeviceProperties("Household Fan ", DeviceCategory.OTHER, EnvironmentalImpact.MEDIUM, false),
				pricingTable), null);
		addServiceConsumer(generateRequest(720.7, current, 80., 80., PriorityLevel.LOW,
				new DeviceProperties(" Toaster", DeviceCategory.COOKING, EnvironmentalImpact.MEDIUM, false),
				pricingTable), null);
		addServiceConsumer(generateRequest(100.0, current, 50., 50., PriorityLevel.LOW,
				new DeviceProperties("iPad / Tablet", DeviceCategory.ICT, EnvironmentalImpact.MEDIUM, false),
				pricingTable), null);
		/**/

		// restartConsumer("Consumer_3", new Float("11"), current,
		// SapereUtil.shiftDateMinutes(current, 10), new Float(01));
		return getLsas();
	}

	public List<Service> test2(NodeConfig envNodeConfig) {
		initEnergyService(envNodeConfig, generateDefaultInitForm("test2"), new ArrayList<NodeConfig>());
		Date current = nodeContext.getCurrentDate(); // getCurrentMinute();
		PricingTable pricingTable = new PricingTable(current, UtilDates.shiftDateDays(current, 365), 0,
				getTimeShiftMS());
		int nbAgents = 10;
		for (int i = 0; i < nbAgents; i++) {
			IProducerPolicy producerPolicy = initDefaultProducerPolicy();
			addServiceProducer(generateSupply(25.0, current, 60.,
					new DeviceProperties("wind turbine " + i, DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true),
					pricingTable), producerPolicy);
		}
		for (int i = 0; i < nbAgents; i++) {
			addServiceConsumer(generateRequest(30 + 0.1 * i, current, 120., 120., PriorityLevel.LOW,
					new DeviceProperties("Laptop " + i, DeviceCategory.ICT, EnvironmentalImpact.MEDIUM, false),
					pricingTable), null);
		}
		return getLsas();
	}

	public List<Service> test3(NodeConfig envNodeConfig) {
		initEnergyService(envNodeConfig, generateDefaultInitForm("test3"), new ArrayList<NodeConfig>());
		Date current = nodeContext.getCurrentDate(); // getCurrentMinute();
		PricingTable pricingTable = new PricingTable(current, UtilDates.shiftDateDays(current, 365), 0,
				getTimeShiftMS());
		IProducerPolicy producerPolicy = initDefaultProducerPolicy();
		addServiceProducer(generateSupply(30.0, current, YEAR_DURATION_MIN,
				new DeviceProperties("EDF", DeviceCategory.EXTERNAL_ENG, EnvironmentalImpact.MEDIUM, true),
				pricingTable), producerPolicy);
		addServiceProducer(generateSupply(25.0, current, 60.,
				new DeviceProperties("wind turbine1", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true),
				pricingTable), producerPolicy);
		addServiceProducer(generateSupply(25.0, current, 60.,
				new DeviceProperties("wind turbine2", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true),
				pricingTable), producerPolicy);
		addServiceProducer(generateSupply(25.0, current, 60.,
				new DeviceProperties("wind turbine3", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true),
				pricingTable), producerPolicy);
		addServiceConsumer(generateRequest(24.7, current, 150., 150., PriorityLevel.LOW,
				new DeviceProperties("Laptop 1", DeviceCategory.ICT, EnvironmentalImpact.MEDIUM, false), pricingTable),
				null);
		return getLsas();
	}

	public List<Service> test4(NodeConfig envNodeConfig) {
		initEnergyService(envNodeConfig, generateDefaultInitForm("test4"), new ArrayList<NodeConfig>());
		Date current = nodeContext.getCurrentDate(); // getCurrentMinute();
		PricingTable pricingTable = new PricingTable(current, UtilDates.shiftDateDays(current, 365), 0,
				getTimeShiftMS());
		// addServiceProducer(generateSupply(new Float(30.0), current,
		// YEAR_DURATION_MIN, "EDF", DeviceCategory.EXTERNAL_ENG));
		DeviceProperties solorPanel = new DeviceProperties("solar panel1", DeviceCategory.SOLOR_ENG,
				EnvironmentalImpact.LOW, true);
		IProducerPolicy producerPolicy = initDefaultProducerPolicy();
		addServiceProducer(generateSupply(50.0, current, 10., solorPanel, pricingTable), producerPolicy);
		DeviceProperties tvLCD = new DeviceProperties("TV 32 LED/LCD ", DeviceCategory.AUDIOVISUAL,
				EnvironmentalImpact.MEDIUM, false);
		addServiceConsumer(generateRequest(97.7, current, 150., 150., PriorityLevel.LOW, tvLCD, pricingTable), null);
		int nbAgents = 5;
		for (int i = 0; i < nbAgents; i++) {
			DeviceProperties lapTop = new DeviceProperties("Laptop " + i, DeviceCategory.ICT,
					EnvironmentalImpact.MEDIUM, false);
			addServiceConsumer(
					generateRequest(30 + 0.1 * i, current, 120., 120., PriorityLevel.LOW, lapTop, pricingTable), null);
		}
		return getLsas();
	}

	public List<Service> test5(NodeConfig envNodeConfig) {
		initEnergyService(envNodeConfig, generateDefaultInitForm("test5"), new ArrayList<NodeConfig>());
		// Add producer agents
		Date current = getCurrentDate();
		PricingTable pricingTable = new PricingTable(current, UtilDates.shiftDateDays(current, 365), 0,
				getTimeShiftMS());
		DeviceProperties devicePropeties1 = new DeviceProperties("EDF", DeviceCategory.EXTERNAL_ENG,
				EnvironmentalImpact.MEDIUM, true);
		IProducerPolicy producerPolicy = initDefaultProducerPolicy();
		addServiceProducer(generateSupply(2700.0, current, YEAR_DURATION_MIN, devicePropeties1, pricingTable),
				producerPolicy);
		// Add query
		/*
		 * addQueryConsumer( generateRequest(new Float(10.0), current, new Float(0.5),
		 * new Float(0.5), PriorityLevel.LOW, "Led1", DeviceCategory.LIGHTING));
		 */
		return getLsas();
	}

	private PricingTable initPricingTable(int minutesByStep) {
		Date current = nodeContext.getCurrentDate();
		// Date end = UtilDates.shiftDateMinutes(current, 60);
		int time = 0;
		Map<Integer, Double> simplePicingTable = new HashMap<Integer, Double>();
		simplePicingTable.put(time, 10.0);
		time += 1;
		simplePicingTable.put(time, 10.0);
		time += 1;
		simplePicingTable.put(time, 6.0);
		time += minutesByStep;
		simplePicingTable.put(time, 7.0);
		time += minutesByStep;
		simplePicingTable.put(time, 8.0);
		time += minutesByStep;
		simplePicingTable.put(time, 9.0);
		time += minutesByStep;
		simplePicingTable.put(time, 10.0);
		Date lastStepDate = null;
		PricingTable pricingTable = new PricingTable(nodeContext.getTimeShiftMS());
		SortedSet<Integer> keys = new TreeSet<>(simplePicingTable.keySet());
		for (int step : keys) {
			Double rate = simplePicingTable.get(step);
			Date nextStepDate = UtilDates.shiftDateMinutes(current, step);
			if (lastStepDate != null) {
				pricingTable.addPrice(lastStepDate, nextStepDate, rate);
			}
			lastStepDate = nextStepDate;
		}
		return pricingTable;
	}

	public IProducerPolicy initDefaultProducerPolicy() {
		IProducerPolicy result = new BasicProducerPolicy(nodeContext, 0.0, IProducerPolicy.POLICY_PRIORIZATION);
		return result;
	}

	// Tragedy of the commons
	public List<Service> testTragedyOfTheCommons(NodeConfig envNodeConfig, boolean useProducerPolicy) {
		initEnergyService(envNodeConfig, generateDefaultInitForm("testTragedyOfTheCommons"),
				new ArrayList<NodeConfig>());
		Date current = nodeContext.getCurrentDate(); // getCurrentMinute();
		int minutesByStep = 3;
		PricingTable pricingTable = initPricingTable(minutesByStep);// new PricingTable();
		/*
		 * Date end = UtilDates.shiftDateMinutes(current, 60); int step1Minutes = 1;
		 * pricingTable.addPrice(UtilDates.shiftDateMinutes(current, 0) ,
		 * UtilDates.shiftDateMinutes(current, step1Minutes), 10);
		 * pricingTable.addPrice(UtilDates.shiftDateMinutes(current, step1Minutes) ,
		 * UtilDates.shiftDateMinutes(current, step1Minutes+5), 7);
		 * pricingTable.addPrice(UtilDates.shiftDateMinutes(current, step1Minutes+5),
		 * end, 10);
		 */
		// addServiceProducer(generateSupply(new Float(30.0), current,
		// YEAR_DURATION_MIN, "EDF", DeviceCategory.EXTERNAL_ENG));
		IProducerPolicy producerPolicy = useProducerPolicy
				? new LowestDemandPolicy(pricingTable, IProducerPolicy.POLICY_PRIORIZATION)
				: initDefaultProducerPolicy();
		DeviceProperties devicePropeties1 = new DeviceProperties("SIG", DeviceCategory.EXTERNAL_ENG,
				EnvironmentalImpact.MEDIUM, true);
		addServiceProducer(generateSupply(33.0, current, 60., devicePropeties1, pricingTable), producerPolicy);
		IConsumerPolicy lowestPricePolicy = new LowestPricePolicy();
		int nbAgents = 10;
		// nbAgents = 1;
		for (int i = 0; i < nbAgents; i++) {
			Date dateBegin = UtilDates.shiftDateSec(current, 10);
			Date dateEnd = UtilDates.shiftDateSec(dateBegin, minutesByStep * 60);
			Double power = 10.0;
			DeviceProperties devicePropeties2 = new DeviceProperties("Battery " + i, DeviceCategory.OTHER,
					EnvironmentalImpact.MEDIUM, false);
			EnergyRequest request = generateRequest(power, dateBegin, dateEnd, 120., PriorityLevel.LOW,
					devicePropeties2, new PricingTable(getTimeShiftMS()));
			addServiceConsumer(request, lowestPricePolicy);
		}
		return getLsas();
	}

	public List<Service> test6(NodeConfig envNodeConfig) {
		initEnergyService(envNodeConfig, generateDefaultInitForm("test6"), new ArrayList<NodeConfig>());
		Date current = nodeContext.getCurrentDate(); // getCurrentMinute();
		// addServiceProducer(generateSupply(new Float(30.0), current,
		// YEAR_DURATION_MIN, "EDF", DeviceCategory.EXTERNAL_ENG));
		PricingTable pricingTable = new PricingTable(current, UtilDates.shiftDateDays(current, 365), 0,
				getTimeShiftMS());
		DeviceProperties devicePropeties1 = new DeviceProperties("solar panel1", DeviceCategory.SOLOR_ENG,
				EnvironmentalImpact.LOW, true);
		IProducerPolicy producerPolicy = initDefaultProducerPolicy();
		addServiceProducer(generateSupply(33.0, current, 10., devicePropeties1, null), producerPolicy);
		int nbAgents = 10;
		for (int i = 0; i < nbAgents; i++) {
			Date dateBegin = UtilDates.shiftDateSec(current, 5 + 65 * i);
			Date dateEnd = UtilDates.shiftDateSec(dateBegin, 60);
			DeviceProperties devicePropeties2 = new DeviceProperties("Battery " + (1 + i), DeviceCategory.OTHER,
					EnvironmentalImpact.MEDIUM, false);
			EnergyRequest request = generateRequest(10.0, dateBegin, dateEnd, 120., PriorityLevel.LOW, devicePropeties2,
					pricingTable);
			addServiceConsumer(request, null);
		}
		return getLsas();
	}

	public List<Service> initState(String variable, MarkovState targetState, boolean _supervisionDisabled) {
		// initEnergyService(repository, "test5");
		// Add producer agents
		Date current = getCurrentDate();
		PricingTable pricingTable = new PricingTable(current, UtilDates.shiftDateDays(current, 365), 0,
				getTimeShiftMS());
		double minPower = targetState.getMinValue();
		double maxPower = targetState.getMaxValue() == null ? 1.7 * minPower : targetState.getMaxValue();
		double powerRandom = Math.random();
		double targetPower = minPower + (powerRandom * (maxPower - minPower));
		nodeContext.setSupervisionDisabled(_supervisionDisabled);
		if ("produced".equals(variable) || "available".equals(variable) || "consumed".equals(variable)
				|| "provided".equals(variable)) {
			IProducerPolicy producerPolicy = initDefaultProducerPolicy();
			DeviceProperties prodDeviceProperties = new DeviceProperties("wind turbine 0", DeviceCategory.WIND_ENG,
					EnvironmentalImpact.LOW, true);
			addServiceProducer(generateSupply(targetPower, current, 60., prodDeviceProperties, pricingTable),
					producerPolicy);
		}
		if ("requested".equals(variable) || "missing".equals(variable) || "consumed".equals(variable)
				|| "provided".equals(variable)) {
			DeviceProperties consumerDeviceProperties = new DeviceProperties("Heat pump 0", DeviceCategory.HEATING,
					EnvironmentalImpact.MEDIUM, false);
			addServiceConsumer(generateRequest(targetPower, current, 120., 120., PriorityLevel.LOW,
					consumerDeviceProperties, pricingTable), null);
		}
		try {
			LearningAgent learningAgent = getLearningAgent();
			learningAgent.refreshHistory(null);
			learningAgent.refreshMarkovChains(true, false);
			MarkovState currentState = learningAgent.getCurrentMarkovState(variable);
			boolean stateReached = currentState != null && targetState.getId().equals(currentState.getId());
			int cpt = 0;
			while (!stateReached && cpt < 10) {
				Thread.sleep(5 * 1000);
				learningAgent.refreshHistory(null);
				learningAgent.refreshMarkovChains(true, false);
				currentState = learningAgent.getCurrentMarkovState(variable);
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
		if (this.mapAgentAuthentication.containsKey(agentName)) {
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

	private void addServiceLearningAgent(
			String agentName /* , String scenario, Map<Integer,Integer> _datetimeShifts */) {
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

	private void addServiceRegulatorAgent(String agentName) {
		AgentAuthentication authentication = generateAgentAuthentication(AgentType.REGULATOR);
		RegulatorAgent regulatorAgent = new RegulatorAgent(agentName, authentication, nodeContext);
		serviceAgents.add(regulatorAgent);
		startService(agentName);
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

	public AgentForm addEnergyAgent(AgentInputForm agentInputForm) {
		resolveAgentsNotInsapce();
		AgentForm result = null;
		Date current = getCurrentDate();
		if (agentInputForm.getEndDate() != null && agentInputForm.getEndDate().before(current)) {
			logger.warning("#### addAgent : enddate = " + agentInputForm.getEndDate());
		}
		if (!SapereUtil.checkIsRound(agentInputForm.getPower(), NB_DEC_POWER, logger)) {
			logger.warning("#### addAgent : power = " + SapereUtil.round(agentInputForm.getPower(), 3));
			double powerToSet = SapereUtil.round(agentInputForm.getPower(), Sapere.NB_DEC_POWER);
			agentInputForm.setPowers(powerToSet, powerToSet, powerToSet);
		}
		try {
			EnergyAgent newAgent = null;
			if (agentInputForm.isConsumer()) {
				agentInputForm.generateSimplePricingTable(0);
				PriorityLevel priority = PriorityLevel.getByLabel(agentInputForm.getPriorityLevel());
				double delayToleranceMinutes = agentInputForm.getDelayToleranceMinutes();
				if (delayToleranceMinutes == 0 && agentInputForm.getDelayToleranceRatio() != null) {
					delayToleranceMinutes = agentInputForm.getDelayToleranceRatio() * UtilDates
							.computeDurationMinutes(agentInputForm.getBeginDate(), agentInputForm.getEndDate());
				}
				newAgent = addServiceConsumer(agentInputForm.getPower(), agentInputForm.getBeginDate(),
						agentInputForm.getEndDate(), delayToleranceMinutes, priority,
						agentInputForm.retrieveDeviceProperties(), agentInputForm.generatePricingTable(), null);
			} else if (agentInputForm.isProducer()) {
				agentInputForm.generateSimplePricingTable(0);
				IProducerPolicy producerPolicy = initDefaultProducerPolicy();
				newAgent = addServiceProducer(agentInputForm.getPower(), agentInputForm.getBeginDate(),
						agentInputForm.getEndDate(), agentInputForm.retrieveDeviceProperties(),
						agentInputForm.generatePricingTable(), producerPolicy);
			}
			if (newAgent != null) {
				Thread.sleep(1 * 1000);
				result = generateAgentForm(newAgent);
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

	public ConsumerAgent addServiceConsumer(Double power, Date beginDate, Date endDate, Double _delayToleranceMinutes,
			PriorityLevel _priority, DeviceProperties deviceProperties, PricingTable pricingTable,
			IConsumerPolicy consumerPolicy) {
		EnergyRequest request = generateRequest(power, beginDate, endDate, _delayToleranceMinutes, _priority,
				deviceProperties, pricingTable);
		return addServiceConsumer(request, consumerPolicy);
	}

	private ConsumerAgent addServiceConsumer(EnergyRequest need, IConsumerPolicy consumerPolicy) {
		checkInitialisation();
		try {
			need.checkBeginNotPassed();
			synchronized (mapAgentAuthentication) {
				AgentAuthentication authentication = generateAgentAuthentication(AgentType.CONSUMER);
				ConsumerAgent consumerAgent = new ConsumerAgent(nextConsumerId, authentication, need, consumerPolicy);
				synchronized (consumerAgent) {
					consumerAgent.setInitialLSA();
				}
				serviceAgents.add(consumerAgent);
				// addServiceContract(consumerAgent);
				logger.info("Add new consumer " + consumerAgent.getAgentName());
				nextConsumerId++;
				return consumerAgent;
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return null;
	}

	private ProducerAgent addServiceProducer(EnergySupply supply, IProducerPolicy producerPolicy) {
		if (!isRunning) {
			return null;
		}
		if (producerPolicy == null) {
			logger.error("addServiceProducer : producerPolicy is not set");
		}
		checkInitialisation();
		supply.checkBeginNotPassed();
		synchronized (mapAgentAuthentication) {
			AgentAuthentication authentication = generateAgentAuthentication(AgentType.PRODUCER);
			ProducerAgent producerAgent = new ProducerAgent(nextProducerId, authentication, supply, producerPolicy);
			nextProducerId++;
			serviceAgents.add(producerAgent);
			synchronized (producerAgent) {
				producerAgent.setInitialLSA();
			}
			logger.info("Add new producer " + producerAgent.getAgentName());
			return producerAgent;
		}
	}

	public ProducerAgent addServiceProducer(Double power, Date beginDate, Date endDate,
			DeviceProperties deviceProperties, PricingTable pricingTable, IProducerPolicy producerPolicy) {
		if (!isRunning) {
			return null;
		}
		EnergySupply supply = generateSupply(power, beginDate, endDate, deviceProperties, pricingTable);
		return this.addServiceProducer(supply, producerPolicy);
	}

	public AgentForm restartEnergyAgent(AgentInputForm agentInputForm) {
		resolveAgentsNotInsapce();
		AgentForm result = null;
		Date current = getCurrentDate();
		if (agentInputForm.getEndDate() != null && agentInputForm.getEndDate().before(current)) {
			logger.warning("#### restartAgent : endadate = " + agentInputForm.getEndDate());
		}
		if (!SapereUtil.checkIsRound(agentInputForm.getPower(), NB_DEC_POWER, logger)) {
			logger.warning("#### restartAgent : power = " + agentInputForm.getPower());
			double powerToSet = SapereUtil.round(agentInputForm.getPower(), NB_DEC_POWER);
			agentInputForm.setPowers(powerToSet, powerToSet, powerToSet);
		}
		try {
			SapereAgent agent = null;
			if (agentInputForm.isConsumer()) {
				// Restart consumer agent
				agent = restartConsumer(agentInputForm.getAgentName(), agentInputForm.retrieveEnergyRequest());
			} else if (agentInputForm.isProducer()) {
				// Restart producer agent
				agent = restartProducer(agentInputForm.getAgentName(), agentInputForm.retrieveEnergySupply());
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

	public ConsumerAgent restartConsumer(String consumerAgentName, EnergyRequest need) {
		if (!isRunning) {
			return null;
		}
		if (!(this.getAgent(consumerAgentName) instanceof ConsumerAgent)) {
			return null;
		}
		if (need.getIssuerLocation() == null) {
			need.setIssuerLocation(nodeContext.getNodeConfig());
		}
		if (!isInSpace(consumerAgentName)) {
			logger.info("restartConsumer : restart agent " + consumerAgentName);
			// Restart consuer agent
			SapereAgent agent = this.getAgent(consumerAgentName);
			NodeManager.instance().getNotifier().unsubscribe(consumerAgentName);
			AgentAuthentication consumerAuthentication = agent.getAuthentication();
			need.checkBeginNotPassed();
			ConsumerAgent consumerAgent = (ConsumerAgent) agent;
			synchronized (consumerAgent) {
				try {
					// Re-initialize consumer agent
					Integer id = consumerAgent.getId();
					consumerAgent.reinitialize(id, consumerAuthentication, need);
					consumerAgent.initFields(need, consumerAgent.getConsumerPolicy());
					if (!serviceAgents.contains(consumerAgent)) {
						serviceAgents.add(consumerAgent);
					}
					consumerAgent.setInitialLSA();
					// Wait untill contract agent is in space
				} catch (Exception e) {
					logger.error(e);
				}
			}
		} else {
			logger.info("restartConsumer : " + consumerAgentName + " is already in tupple splace");
		}

		// Wait untill consumer agent is in sapce
		int nbWait = 0;
		while (!isInSpace(consumerAgentName) && nbWait < 30) {
			logger.info("restartConsumer : agent " + consumerAgentName + " not in sapce : Waiting ");
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				logger.error(e);
			}
			nbWait++;
		}
		logger.info("restartConsumer : agent " + consumerAgentName + " isInSpace = " + isInSpace(consumerAgentName));
		nbWait = 0;
		// Return consumer agent
		SapereAgent consumerAgent1 = getAgent(consumerAgentName);
		if (consumerAgent1 instanceof ConsumerAgent) {
			return (ConsumerAgent) (getAgent(consumerAgentName));
		}
		return null;
	}

	private void setAgentExpired(String agentName, RegulationWarning warning) {
		SapereAgent agent = this.getAgent(agentName);
		EnergyEvent stopEvent = this.getStopEvent(agentName);
		if (agent instanceof EnergyAgent) {
			EnergyAgent energyAgent = (EnergyAgent) agent;
			energyAgent.getEnergySupply().setEndDate(getCurrentDate());
		}
		if (stopEvent == null) {
			generateStopEvent(agentName, warning);
		}
	}

	public boolean isAgentStopped(String agentName) {
		SapereAgent agent = this.getAgent(agentName);
		boolean result = false;
		if (agent instanceof IEnergyAgent) {
			result = ((IEnergyAgent) agent).hasExpired();
		}
		return result;
	}

	public EnergyEvent getStopEvent(String agentName) {
		SapereAgent agent = this.getAgent(agentName);
		EnergyEvent result = null;
		if (agent instanceof IEnergyAgent) {
			result = ((IEnergyAgent) agent).getStopEvent();
		}
		return result;
	}

	public EnergyEvent generateStopEvent(String agentName, RegulationWarning warning) {
		SapereAgent agent = this.getAgent(agentName);
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
		SapereAgent regAgent = getAgent(regulatorAgentName);
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
			return this.getAgent(agentName);
		}
		// Use the regulator agent to send a user interruption
		SapereAgent regAgent = getAgent(regulatorAgentName);
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
		return this.getAgent(agentName);
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
			SapereAgent regAgent = getAgent(regulatorAgentName);
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
		SapereAgent agent = this.getAgent(agentName);
		if (agent instanceof EnergyAgent) {
			result = ((EnergyAgent) agent).getEnergySupply();
		}
		if (result != null) {
			return result.clone();
		}
		return result;
	}

	public AgentForm modifyEnergyAgent(AgentInputForm agentInputForm) {
		resolveAgentsNotInsapce();
		String agentName = agentInputForm.getAgentName();
		EnergySupply energySupply = agentInputForm.retrieveEnergyRequest();
		if (!SapereUtil.checkIsRound(agentInputForm.getPower(), NB_DEC_POWER, logger)) {
			logger.info("---- modifyAgent : power = " + agentInputForm.getPower());
			double powerToSet = SapereUtil.round(agentInputForm.getPower(), NB_DEC_POWER);
			agentInputForm.setPowers(powerToSet, powerToSet, powerToSet);
		}
		agentInputForm.setTimeShiftMS(getTimeShiftMS());
		// Use the regulator agent to send a user interruption
		if (energySupply.getIssuerLocation() == null) {
			energySupply.setIssuerLocation(nodeContext.getNodeConfig());
		}
		SapereAgent regAgent = getAgent(regulatorAgentName);
		if (regAgent instanceof RegulatorAgent) {
			RegulatorAgent regulatorAgent = (RegulatorAgent) regAgent;
			regulatorAgent.modifyAgent(agentName, energySupply);
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
				e.printStackTrace();
				logger.error(e);
			}
		}
		SapereAgent agent = this.getAgent(agentName);
		return generateAgentForm(agent);
	}

	public PredictionData getPrediction(PredictionRequest predictionRequest) {
		checkInitialisation();
		if (nodeContext.isPredictionsActivated()) {
			LearningAgent learningAgent = this.getLearningAgent();
			Date targetDate = new Date(predictionRequest.getLongTargetDate());
			List<Date> targetDates = new ArrayList<Date>();
			targetDates.add(targetDate);
			Date initDate = new Date(predictionRequest.getLongInitDate());
			boolean useCorrections = predictionRequest.isUseCorrections();
			logger.info("targetDate = " + targetDate);
			PredictionContext predictionContext = learningAgent.getPredictionContextCopy();
			PredictionData result = PredictionHelper.getInstance().computePrediction2(predictionContext, initDate,
					targetDates, learningAgent.getVariables(), useCorrections);
			return result;
		}
		return new PredictionData();
	}

	public MultiPredictionsData generateMassivePredictions(MassivePredictionRequest massivePredictionRequest) {
		checkInitialisation();
		if (nodeContext.isPredictionsActivated()) {
			LearningAgent learningAgent = this.getLearningAgent();
			PredictionContext predictionContext = learningAgent.getPredictionContextCopy();
			TimeSlot targetDateSlot = massivePredictionRequest.getTimeSlot();
			// NodeConfig nodeLocation = massivePredictionRequest.getNodeLocation();
			String variableName = massivePredictionRequest.getVariableName();
			logger.info("targetDateMin = " + targetDateSlot.getBeginDate() + ", targetDateMax = "
					+ targetDateSlot.getEndDate());
			int horizonInMinutes = massivePredictionRequest.getHorizonInMinutes();
			boolean useCorrections = massivePredictionRequest.isUseCorrections();
			boolean generateCorrections = massivePredictionRequest.isGenerateCorrections();
			return PredictionHelper.getInstance().generateMassivePredictions(predictionContext, targetDateSlot,
					horizonInMinutes, variableName, useCorrections, generateCorrections);
		}
		return new MultiPredictionsData();
	}

	public List<PredictionStatistic> computePredictionStatistics(StatisticsRequest statisticsRequest) {
		List<PredictionStatistic> result = new ArrayList<PredictionStatistic>();
		if (nodeContext.isPredictionsActivated()) {
			try {
				LearningAgent learningAgent = this.getLearningAgent();
				PredictionContext predictionContext = learningAgent.getPredictionContextCopy();
				Date minComputeDay = new Date(statisticsRequest.getLongMinComputeDay());
				Date maxComputeDay = new Date(statisticsRequest.getLongMaxComputeDay());
				Date minTargetDate = new Date();// statisticsRequest.getLongMinTargetDay());
				Date maxTargetDate = new Date();// statisticsRequest.getLongMaxTargetDay());
				// NodeConfig nodeLocation = statisticsRequest.getNodeLocation();
				String variableName = statisticsRequest.getVariableName();
				Boolean useCorrectionFilter = statisticsRequest.getUseCorrectionFilter();
				Integer minHour = statisticsRequest.getMinTargetHour();
				Integer maxHour = statisticsRequest.getMaxTargetHour();
				String[] fieldsToMerge = statisticsRequest.getFieldsToMerge();
				Map<String, PredictionStatistic> mapStatistics = PredictionHelper.getInstance()
						.computePredictionStatistics(predictionContext, minComputeDay, maxComputeDay, minTargetDate,
								maxTargetDate, minHour, maxHour, useCorrectionFilter, variableName, fieldsToMerge);
				for (PredictionStatistic stat : mapStatistics.values()) {
					result.add(stat);
				}
				Collections.sort(result, new Comparator<PredictionStatistic>() {
					public int compare(PredictionStatistic predictionStatistic1,
							PredictionStatistic predictionStatistic2) {
						return predictionStatistic1.compareTo(predictionStatistic2);
					}
				});
			} catch (Exception e) {
				logger.error(e);
			}
		}
		return result;
	}

	public FedAvgResult checkupFedAVG(FedAvgCheckupRequest fedAvgCheckupRequest) {
		// Map<String, TransitionMatrix> result = new HashMap<String,
		// TransitionMatrix>();
		FedAvgResult result = new FedAvgResult();
		String variableName = fedAvgCheckupRequest.getVariableName();
		result.setVariableName(variableName);
		// Map<String, TransitionMatrix> mapAggregation = new HashMap<>();
		boolean aggregationSet = false;
		if (nodeContext.isPredictionsActivated()) {
			try {
				for (Lsa lsa : NodeManager.instance().getSpace().getAllLsa().values()) {
					if (AgentType.LEARNING_AGENT.getLabel().equals(lsa.getAgentAuthentication().getAgentType())) {
						String agentNode = lsa.getAgentAuthentication().getNodeLocation().getName();
						for (Property prop : lsa.getProperties()) {
							Object value = prop.getValue();
							if (value instanceof NodeTransitionMatrices) {
								NodeTransitionMatrices nodeTrMatrices = (NodeTransitionMatrices) value;
								TransitionMatrix trMatrix = nodeTrMatrices.getTransitionMatrix(variableName);
								String propName = prop.getName();
								if ("MODEL_AGGR".equals(propName)) {
									if (nodeContext.getNodeConfig().getName().equals(agentNode)) {
										result.setAggregateTransitionMatrix(trMatrix);
										logger.info("checkupFedAVG : add AGGREGATION");
										aggregationSet = true;
									}
								} else if ("MODEL".equals(propName)) {
									result.addNodeTransitionMatrix(agentNode, trMatrix);
									logger.info("checkupFedAVG : add MODEL of " + propName);
								}
							}
						}
					}
				}
			} catch (Throwable e) {
				logger.error(e);
			}
		}
		if (!aggregationSet && result.getNodeTransitionMatrices().size() == 1) {
			for (TransitionMatrix trMatrix : result.getNodeTransitionMatrices().values()) {
				result.setAggregateTransitionMatrix(trMatrix);
			}
		}
		logger.info("after loop1");
		return result;
	}

	public List<MarkovStateHistory> retrieveLastMarkovHistoryStates(StateHistoryRequest stateHistoryRequest) {
		checkInitialisation();
		if (nodeContext.isPredictionsActivated()) {
			LearningAgent learningAgent = this.getLearningAgent();
			Date minCreationDate = stateHistoryRequest.getMinDate();
			String variableName = stateHistoryRequest.getVariableName();
			boolean observationUpdated = stateHistoryRequest.getObservationUpdated();
			List<MarkovStateHistory> result = learningAgent.retrieveLastMarkovHistoryStates(minCreationDate,
					variableName, observationUpdated);
			return result;
		}
		return new ArrayList<MarkovStateHistory>();
	}

	public ProducerAgent restartProducer(String agentName, EnergySupply supply) {
		logger.info("restartProducer " + agentName);
		if (supply.getIssuerLocation() == null) {
			supply.setIssuerLocation(nodeContext.getNodeConfig());
			// supply.setIssuerLocation(NodeManager.getNodeConfig());
		}
		if (!isInSpace(agentName)) {
			supply.checkBeginNotPassed();
			NodeManager.instance().getNotifier().unsubscribe(agentName);
			SapereAgent agent = this.getAgent(agentName);
			if (agent instanceof ProducerAgent) {
				ProducerAgent producerAgent = (ProducerAgent) agent;
				synchronized (producerAgent) {
					int id = producerAgent.getId();
					// ServiceAgents.remove(producerAgent);
					AgentAuthentication authentication = producerAgent.getAuthentication();
					producerAgent.reinitialize(id, authentication, supply);
					producerAgent.initFields(supply, producerAgent.getProducerPolicy());
					if (!serviceAgents.contains(producerAgent)) {
						serviceAgents.add(producerAgent);
					}
					producerAgent.setInitialLSA();
				}
			}
		}
		// Wait untill contract agent is in sapce
		int nbWait = 0;
		while (!isInSpace(agentName) && nbWait < 30) {
			logger.info("restartProducer : agent " + agentName + " not in sapce : Waiting ");
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				logger.error(e);
			}
			nbWait++;
		}
		logger.info("restartProducer : end : " + agentName + " in space = " + isInSpace(agentName));
		SapereAgent agent = this.getAgent(agentName);
		if (agent instanceof ProducerAgent) {
			return (ProducerAgent) agent;
		}
		return null;
	}

	public boolean isLocalAgent(String agentName) {
		boolean isLocal = false;
		if (mapAgentAuthentication.containsKey(agentName)) {
			AgentAuthentication authentication = this.mapAgentAuthentication.get(agentName);
			isLocal = (NodeManager.getLocation().equals(authentication.getNodeLocation().getMainServiceAddress()));
		}
		return isLocal;
	}

	public boolean isInSpace(String agentName) {
		boolean isLocal = isLocalAgent(agentName);
		String agentName2 = agentName + (isLocal ? "" : "*");
		boolean result = NodeManager.instance().getSpace().getAllLsa().containsKey(agentName2);
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
		int distance = NodeManager.instance().getDistance(agent.getAuthentication().getNodeLocation());
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

	public MultiNodesContent retrieveAllNodesContent(AgentFilter filter) {
		if (nodeContext == null) {
			return new MultiNodesContent(new NodeConfig(), null, 0);
		}
		long nodeTimeShiftMS = nodeContext.getTimeShiftMS();
		// NodeContent content = new NodeContent(null, nodeContext==null? 0 :
		// nodeContext.getTimeShiftMS());
		MultiNodesContent content = new MultiNodesContent(nodeContext.getNodeConfig(), filter, nodeTimeShiftMS);
		content.setMapNeighborNodes(getMapAllNodeConfigs(false));
		NodeContent currentNodeContent = retrieveNodeContent(filter);
		content.merge(currentNodeContent, 0);
		LearningAgent learningAgent = getLearningAgent();
		String[] filterNodes = filter.getNeighborNodeNames();
		Set<String> setFilterNodes = new HashSet<String>();
		if (filterNodes != null) {
			setFilterNodes = new HashSet<String>(Arrays.asList(filterNodes));
		}
		/*
		 * if(setFilterNodes.size() > 0) {
		 * logger.info("retrieveAllNodesContent : for debug"); }
		 */
		Collection<NodeConfig> neighborNodeConfigs = learningAgent.getMapNeighborNodeConfigs().values();
		for (NodeConfig nextNodeConfig : neighborNodeConfigs) {
			// call distant web service
			String nextNodeName = nextNodeConfig.getName();
			if (setFilterNodes.contains(nextNodeName) || setFilterNodes.size() == 0) {
				String nextNodeUrl = nextNodeConfig.getUrl();
				if (nextNodeUrl != null) {
					Map<String, Object> params = UtilHttp.generateRequestParams(filter, UtilDates.format_json_datetime,
							logger, UtilHttp.METHOD_GET);
					// params = new HashMap<>();
					String postResponse = UtilHttp.sendGetRequest(nextNodeUrl + "retrieveNodeContent", params, logger,
							debugLevel);
					if (postResponse != null) {
						JSONObject jsonNodeContent = new JSONObject(postResponse);
						NodeContent neighbourNodeContant = UtilJsonParser.parseNodeContent(jsonNodeContent, logger);
						int neighbourDistance = NodeManager.instance()
								.getDistance(neighbourNodeContant.getNodeConfig());
						content.merge(neighbourNodeContant, neighbourDistance);
					}
				}
			}
		}
		return content;
	}

	public NodeContent retrieveNodeContent(AgentFilter filter) {
		if (nodeContext == null) {
			return new NodeContent(new NodeConfig(), null, 0);
		}
		long nodeTimeShiftMS = nodeContext == null ? 0 : nodeContext.getTimeShiftMS();
		NodeContent content = new NodeContent(nodeContext.getNodeConfig(), filter, nodeTimeShiftMS);
		content.setMapNeighborNodes(getMapAllNodeConfigs(false));
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
				NodeConfig agentLocation = agent.getAuthentication().getNodeLocation();
				// boolean isLocal = nodeContext.getNodeConfig().getName().equals(agentNode);
				int distance = NodeManager.instance().getDistance(agentLocation);
				boolean inSpace = lsaInSpace.containsKey(agent.getAgentName());
				if (agent instanceof ProducerAgent) {
					ProducerAgent producer = (ProducerAgent) agent;
					// producer.setInSpace(inSpace);
					content.addProducer(producer, inSpace, distance);
				} else if (agent instanceof ConsumerAgent) {
					ConsumerAgent consumer = (ConsumerAgent) agent;
					// consumer.getLinkedAgents();
					// consumer.setInSpace(inSpace);
					content.addConsumer(consumer, inSpace, distance);
				}
			} else {
				content.setNoFilter(false);
			}
		}
		content.sortAgents();
		content.computeTotal();
		return content;
	}

	public NodeContent restartLastNodeContent() {
		List<ExtendedEnergyEvent> events = EnergyDbHelper.retrieveLastSessionEvents();
		Date current = getCurrentDate();
		this.checkInitialisation();
		for (EnergyEvent event : events) {
			if (EventType.PRODUCTION_START.equals(event.getType())) {
				EnergySupply supply = generateSupply(event.getPower(), getCurrentDate(), event.getEndDate(),
						event.getDeviceProperties(), event.getPricingTable());
				IProducerPolicy producerPolicy = initDefaultProducerPolicy();
				this.addServiceProducer(supply, producerPolicy);
			} else if (EventType.REQUEST_START.equals(event.getType())) {
				current = getCurrentDate();
				double _delayToleranceMinutes = UtilDates.computeDurationMinutes(current, event.getEndDate());
				EnergyRequest request = generateRequest(event.getPower(), current, event.getEndDate(),
						_delayToleranceMinutes, PriorityLevel.LOW, event.getDeviceProperties(),
						event.getPricingTable());
				this.addServiceConsumer(request, null);
			}
		}
		return this.retrieveNodeContent();
	}

	public ConsumerAgent getConsumerAgent(String agentName) {
		SapereAgent agent = getAgent(agentName);
		if (agent instanceof ConsumerAgent) {
			ConsumerAgent consumer = (ConsumerAgent) agent;
			return consumer;
		}
		return null;
	}

	public boolean isConsumerSatified(String agentName) {
		ConsumerAgent agent = getConsumerAgent(agentName);
		if (agent != null) {
			return agent.isSatisfied();
		}
		return false;
	}
	/*
	 * public String getConsumerConfirmTag(String agentName) { ConsumerAgent agent =
	 * getConsumerAgent(agentName); if(agent!=null) { return agent.getConfirmTag();
	 * } return null; }
	 */

	public NodeTransitionMatrices getCurrentNodeTransitionMatrices() {
		LearningAgent learningAgent = getLearningAgent();
		if (learningAgent != null && nodeContext.isPredictionsActivated()) {
			return learningAgent.getNodeTransitionMatrices();
		}
		return null;
	}

	private static final Comparator<NodeTransitionMatrices> timeWindowComparator = new Comparator<NodeTransitionMatrices>() {
		public int compare(NodeTransitionMatrices trMatrix1, NodeTransitionMatrices trMatrix2) {
			return trMatrix1.getTimeWindowId() - trMatrix2.getTimeWindowId();
		}
	};

	public List<NodeTransitionMatrices> getAllNodeTransitionMatrices(MatrixFilter matrixFilter) {
		if (matrixFilter.getNodeName() == null || matrixFilter.getNodeName() == "" && nodeContext != null) {
			matrixFilter.setNodeName(nodeContext.getNodeConfig().getName());
		}
		LearningAgent learningAgent = getLearningAgent();
		if (learningAgent != null && nodeContext.isPredictionsActivated()) {
			List<NodeTransitionMatrices> result = new ArrayList<NodeTransitionMatrices>();
			List<MarkovTimeWindow> listTimeWindows = new ArrayList<MarkovTimeWindow>();
			for (MarkovTimeWindow nextTimeWindow : LearningAgent.ALL_TIME_WINDOWS) {
				if (matrixFilter.applyFilter(nextTimeWindow)) {
					listTimeWindows.add(nextTimeWindow);
				}
			}
			PredictionContext predictionCtx = learningAgent.getPredictionContextCopy();
			String[] variables = learningAgent.getVariables();
			if (matrixFilter.getVariableName() != null && !"".equals(matrixFilter.getVariableName())) {
				String[] filterVariables = new String[] { matrixFilter.getVariableName() };
				variables = filterVariables;
			}
			Map<Integer, NodeTransitionMatrices> mapResult = PredictionDbHelper
					.loadListNodeTransitionMatrice(predictionCtx, variables, listTimeWindows, getCurrentDate());
			for (NodeTransitionMatrices next : mapResult.values()) {
				result.add(next);
			}
			Collections.sort(result, timeWindowComparator);
			return result;
		}
		return new ArrayList<NodeTransitionMatrices>();
	}

	public void logProdAgents() {
		for (SapereAgent agent : serviceAgents) {
			if (agent instanceof ProducerAgent) {
				ProducerAgent prodAgent = (ProducerAgent) agent;
				prodAgent.logAgent();
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

	public void checkupProdAgents() {
		for (SapereAgent agent : serviceAgents) {
			if (agent instanceof ProducerAgent) {
				ProducerAgent prodAgent = (ProducerAgent) agent;
				prodAgent.checkup();
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

	public EndUserForcastingResult getForcasting(EndUserForcastingRequest endUserForcastingRequest) {
		LearningAgent learningAgent = getLearningAgent();
		return learningAgent.getForcasting(endUserForcastingRequest);
	}

	public ForcastingResult generateMockForcasting(Map<String, Double> /*ForcastingRequest*/ forcastingRequest) {
		LearningAgent learningAgent = getLearningAgent();
		return learningAgent.generateMockForcasting(forcastingRequest);
	}

	public EndUserForcastingRef getEndUserForcastingRef() {
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
		result.setDatesInterval(interval);
		// List of years
		List<OptionItem> listOfYears = new ArrayList<>();
		calendar.setTime(interval.getBeginDate());
		int year = calendar.get(Calendar.YEAR);
		while(calendar.getTime().before(interval.getEndDate())) {
			listOfYears.add(new OptionItem(""+year, ""+year));
			calendar.add(Calendar.YEAR, 1);
			year = calendar.get(Calendar.YEAR);
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
