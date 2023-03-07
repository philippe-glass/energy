package com.sapereapi.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
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
import com.sapereapi.api.ConfigRepository;
import com.sapereapi.db.DBConnection;
import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.db.PredictionDbHelper;
import com.sapereapi.helper.PredictionHelper;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.energy.AgentFilter;
import com.sapereapi.model.energy.Device;
import com.sapereapi.model.energy.DeviceFilter;
import com.sapereapi.model.energy.DeviceMeasure;
import com.sapereapi.model.energy.DeviceProperties;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.EnergySupply;
import com.sapereapi.model.energy.ExtendedEnergyEvent;
import com.sapereapi.model.energy.NodeContent;
import com.sapereapi.model.energy.OptionItem;
import com.sapereapi.model.energy.PricingTable;
import com.sapereapi.model.energy.RegulationWarning;
import com.sapereapi.model.energy.policy.BasicProducerPolicy;
import com.sapereapi.model.energy.policy.IConsumerPolicy;
import com.sapereapi.model.energy.policy.IProducerPolicy;
import com.sapereapi.model.energy.policy.LowestDemandPolicy;
import com.sapereapi.model.energy.policy.LowestPricePolicy;
import com.sapereapi.model.markov.MarkovState;
import com.sapereapi.model.markov.MarkovStateHistory;
import com.sapereapi.model.markov.MarkovTimeWindow;
import com.sapereapi.model.markov.NodeMarkovStates;
import com.sapereapi.model.markov.NodeTransitionMatrices;
import com.sapereapi.model.prediction.MatrixFilter;
import com.sapereapi.model.prediction.MultiPredictionsData;
import com.sapereapi.model.prediction.PredictionContext;
import com.sapereapi.model.prediction.PredictionData;
import com.sapereapi.model.prediction.PredictionStatistic;
import com.sapereapi.model.referential.AgentType;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.util.PasswordUtils;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;
import com.sapereapi.util.UtilHttp;
import com.sapereapi.util.UtilJsonParser;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.agent.SapereAgent;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.LsaType;
import eu.sapere.middleware.node.NodeManager;

public class Sapere {
	private List<QueryAgent> querys;
	public static List<SapereAgent> ServiceAgents;
	private static Sapere instance = null;
	public NodeManager nodeManager;

	private Map<String, AgentAuthentication> mapAgentAuthentication = null;
	private Map<String, Integer> mapDistance = null;
	private Set<AgentAuthentication> authentifiedAgentsCash = null;

	protected final static double devicePowerCoeffProducer = 2.0;
	protected final static double devicePowerCoeffConsumer = 0.25;
	protected final static double statisticPowerCoeffConsumer = 3.0;
	protected final static double statisticPowerCoeffProducer = 12.0;//6.0;
	public static String learningAgentName = generateAgentName(AgentType.LEARNING_AGENT);
	public static String regulatorAgentName = generateAgentName(AgentType.REGULATOR);
	public static String[] DEFAULT_VARIABLES =  {"requested", "produced", "consumed", "provided", "available", "missing"};
	private String salt = null;
	private static int nextConsumerId = 1;
	private static int nextProducerId = 1;
	private boolean useSecuresPasswords = false;
	// Node context
	private static NodeContext nodeContext = null;
	private static SapereLogger logger = SapereLogger.getInstance();
	int debugLevel = 0;
	private static boolean supervisionDisabled;
	public static boolean allowComplementaryRequests = true;
	public static boolean activateAggregation = true;
	private final static double YEAR_DURATION_MIN = 1.0 * 24 * 60 * 365;
	public static boolean isRunning = false;

	public static Sapere getInstance() {
		if (instance == null) {
			instance = new Sapere();
		}
		return instance;
	}

	public static void enableSupervision() {
		supervisionDisabled = false;
	}

	public Sapere() {
		nodeManager = NodeManager.instance();
		querys = new ArrayList<QueryAgent>();
		ServiceAgents = new ArrayList<SapereAgent>();
		authentifiedAgentsCash = new HashSet<>();
		// Generate Salt. The generated value can be stored.
        salt = PasswordUtils.getSalt(2);
		mapAgentAuthentication = new HashMap<String, AgentAuthentication>();
		mapDistance = new HashMap<String, Integer>();
	}

	public void updateDistance(String location, int lastDistance) {
		if(lastDistance > 0 && lastDistance < 255) {
			int newDistance = 1+lastDistance;
			if(mapDistance.containsKey(location)) {
				if(mapDistance.get(location)>newDistance) {
					mapDistance.put(location, newDistance);
				}
			} else {
				mapDistance.put(location,newDistance);
			}
		}
	}

	public int getDistance(String location) {
		if(mapDistance.containsKey(location)) {
			return mapDistance.get(location);
		}
		return 255;
	}

	public int getDistance(String location, int lastDistance) {
		updateDistance(location, lastDistance);
		return getDistance(location);
	}

	public String getInfo() {
		return NodeManager.getNodeName() + " - " + NodeManager.getLocation() + " -: "
				+ Arrays.toString(NodeManager.instance().networkDeliveryManager.getNeighbours());
	}

	public List<OptionItem> getLocations() {
		List<OptionItem> result = new ArrayList<>();
		result.add(new OptionItem(NodeManager.getLocation(), "home : " + NodeManager.getLocation()));
		int neighborIdx = 1;
		for(String neighbor : NodeManager.instance().networkDeliveryManager.getNeighbours()) {
			result.add(new OptionItem(neighbor, "neighbor " + neighborIdx + " : " + neighbor));
			neighborIdx++;
		}
		return result;
	}

	public List<OptionItem> getStateDates() {
		return PredictionDbHelper.getStateDates();
	}

	public void diffuseLsa(String lsaName, int hops) {
		for (SapereAgent service : ServiceAgents) {
			if (lsaName.equals(service.getAgentName())) {
				service.addGradient(hops);
				break;
			}
		}
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

	public List<Service> getLsas() {
		List<Service> serviceList = new ArrayList<Service>();
		for (SapereAgent service : ServiceAgents) {
			serviceList.add(new Service(service));
		}
		return serviceList;
	}

	public Map<String, Double[]> getQtable(String name) {
		for (SapereAgent serviceAgent : ServiceAgents) {
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

	public int getLoactionIndx(String location) {
		List<OptionItem> loactionItems = getLocations();
		int idx=0;
		for(OptionItem nextItem : loactionItems ) {
			if(nextItem.getValue().equals(location)) {
				return idx;
			}
			idx++;
		}
		return -1;
	}

	// Add and start a service
	public void addServiceGeneric(Service service) {
		AgentAuthentication authentication = generateAgentAuthentication(AgentType.GENERIC_SERVICE);
		authentication.setAgentName(service.getName());
		ServiceAgents.add(new ServiceAgent(authentication, service.getInput(), service.getOutput(), LsaType.Service));
		startService(service.getName());
	}

	public void addServiceRest(Service service) {
		AgentAuthentication authentication = generateAgentAuthentication(AgentType.WEB_SERVICE);
		authentication.setAgentName(service.getName());
		ServiceAgents.add(new ServiceAgentWeb(service.getUrl(), authentication, service.getInput(),
				service.getOutput(), service.getAppid(), LsaType.Service));
		startService(service.getName());
	}

	public void addServiceBlood(Service service) {
		addServiceBlood(service.getName(), null);
	}

	public void addServiceBlood(String name, String type) {
		AgentAuthentication authentication = generateAgentAuthentication(AgentType.BLOOD_SEARCH);
		authentication.setAgentName(name);
		ServiceAgents.add(
				new AgentBloodSearch(authentication, new String[] { "Blood" }, new String[] { "Position" }, LsaType.Service));
		startService(name);
	}

	public void addServiceTransport(Service service) {
		addServiceTransport(service.getName(), null);
	}

	public void addServiceTransport(String name, String type) {
		AgentAuthentication authentication = generateAgentAuthentication(AgentType.TRANSPORT);
		authentication.setAgentName(name);
		ServiceAgents.add(new AgentTransport(authentication, new String[] { "Position", "Destination" },
				new String[] { "Transport" }, LsaType.Service));
		startService(name);
	}

	public List<Service> getServices() {
		List<Service> services = new ArrayList<Service>();
		for (SapereAgent service : ServiceAgents) {
			services.add(new Service(service));
		}
		return services;
	}

	public List<String> getNodes() {
		HashSet<String> nodeSet = new HashSet<>();
		for (SapereAgent service : ServiceAgents) {
			nodeSet.add(service.getInput()[0]);
			nodeSet.add(service.getOutput()[0]);
		}
		List<String> nodes = new ArrayList<String>(nodeSet);
		return nodes;
	}

	public static void startService(String name) {
		for (SapereAgent serviceAgent : ServiceAgents) {
			if (serviceAgent.getAgentName().equals(name)) {
				serviceAgent.setInitialLSA();
			}
		}
	}

	public void addQuery(Query request) {
		querys.add(new QueryAgent(request.getName(), null, request.getWaiting(), request.getProp(), request.getValues(),
				LsaType.Query));
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
		for (SapereAgent agent : ServiceAgents) {
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

	public static void setLocation(ConfigRepository repository) {
		List<Config> listConfig = repository.findAll();
		if(listConfig.size()>0) {
			Config config = listConfig.get(0);
			int localPost = Integer.valueOf(config.getLocalport());
			NodeManager.setConfiguration(config.getName(), config.getLocalip(), localPost);
		}
	}

	public void initNodeManager(ConfigRepository repository) {
		setLocation(repository);
		List<Config> listConfig = repository.findAll();
		if(listConfig.size()>0) {
			Config config = listConfig.get(0);
			String[] neighs = new String[config.getNeighbours().size()];
			int k = 0;
			for (String s : config.getNeighbours()) {
				neighs[k++] = s;
			}
			nodeManager = NodeManager.instance();
			nodeManager.getNetworkDeliveryManager().setNeighbours(neighs);
		}
		mapDistance = new HashMap<String, Integer>();
		mapDistance.put(NodeManager.getLocation(), 0);
		for(String nextNeighbour : NodeManager.instance().getNetworkDeliveryManager().getNeighbours()) {
			mapDistance.put(nextNeighbour, 1);
		}
	}

	public static NodeContext getNodeContext() {
		return nodeContext;
	}

	public void initEnergyService(ConfigRepository repository, String _scenario, Double _maxTotalPower, boolean _disableSupervision
			, Map<Integer,Integer> _datetimeShifts) {
		initNodeManager(repository);
		SapereLogger.getInstance();
		DBConnection.changeSessionId();
		Double maxTotalPower = (_maxTotalPower==null)? NodeMarkovStates.DEFAULT_NODE_MAX_TOTAL_POWER : _maxTotalPower;
		nodeContext = new NodeContext(null, NodeManager.getLocation(), _scenario, _datetimeShifts, maxTotalPower, DBConnection.getSessionId(), DEFAULT_VARIABLES);
		EnergyDbHelper.registerContext(nodeContext);
		PredictionHelper.setMaxTotalPower(nodeContext.getMaxTotalPower());
		PredictionHelper.setVariables(nodeContext.getVariables());
		learningAgentName = generateAgentName(AgentType.LEARNING_AGENT);
		regulatorAgentName = generateAgentName(AgentType.REGULATOR);
		SapereAgent toRemove = getAgent(learningAgentName);
		if (toRemove!= null) {
			ServiceAgents.remove(toRemove);
		}
		nextConsumerId = 1;
		nextProducerId = 1;
		addServiceLearningAgent(learningAgentName);
		addServiceRegulatorAgent(regulatorAgentName);
		isRunning = true;
		supervisionDisabled = _disableSupervision;
	}

	public void stopEnergyService() {
		//stopAllAgents1();
		learningAgentName = null;
		regulatorAgentName = null;
		ServiceAgents.clear();
		querys.clear();
		//nodeManager.stopServices();
		mapAgentAuthentication.clear();
		mapDistance.clear();
		authentifiedAgentsCash.clear();
		nextConsumerId = 0;
		nextProducerId = 0;
		isRunning = false;
		DBConnection.changeSessionId();
	}

	public void checkInitialisation() {
		if(!isRunning) {
			return;
		}
		if (getAgent(learningAgentName) == null) {
			addServiceLearningAgent(learningAgentName);
		}
		if (getAgent(regulatorAgentName) == null) {
			addServiceRegulatorAgent(regulatorAgentName);
		}
	}

	public EnergySupply generateSupply(Double power, Date beginDate, Double durationMinutes,DeviceProperties deviceProperties, PricingTable pricingTable) {
		Date endDate = UtilDates.shiftDateMinutes(beginDate, durationMinutes);
		return generateSupply(power, beginDate, endDate, deviceProperties, pricingTable);
	}

	public EnergySupply generateSupply(Double power, Date beginDate, Date endDate, DeviceProperties deviceProperties, PricingTable pricingTable) {
		String issuer = generateAgentName(AgentType.PRODUCER);
		long timeShiftMS = nodeContext.getTimeShiftMS();
		if(!deviceProperties.isProducer()) {
			logger.error("generateRequest : device should be a producer");
		}
		return new EnergySupply(issuer, NodeManager.getLocation(), false, power, power, power, beginDate, endDate, deviceProperties, pricingTable, timeShiftMS);
	}

	public EnergyRequest generateRequest(Double power, Date beginDate, Double durationMinutes,
			Double _delayToleranceMinutes, PriorityLevel _priority, DeviceProperties deviceProperties, PricingTable pricingTable) {
		Date endDate = UtilDates.shiftDateMinutes(beginDate, durationMinutes);
		if(!SapereUtil.checkIsRound(power, 2, logger)) {
			logger.warning("generateRequest : power with more than 2 dec : " + power);
		}
		if(deviceProperties.isProducer()) {
			logger.error("generateRequest : device should not be a producer");
		}
		return generateRequest(power, beginDate, endDate, _delayToleranceMinutes, _priority, deviceProperties, pricingTable);
	}

	public EnergyRequest generateRequest(Double power, Date beginDate, Date endDate, Double _delayToleranceMinutes,
			PriorityLevel _priority, DeviceProperties deviceProperties, PricingTable pricingTable) {
		if(!SapereUtil.checkIsRound(power, 2, logger)) {
			logger.warning("generateRequest : power with more than 2 dec : " + power);
		}
		String issuer = generateAgentName(AgentType.CONSUMER);
		long timeShiftMS = nodeContext.getTimeShiftMS();
		return new EnergyRequest(issuer, NodeManager.getLocation(), false, power, power, power, beginDate, endDate, _delayToleranceMinutes, _priority, deviceProperties, pricingTable, timeShiftMS);
	}

	public static String generateAgentName(AgentType agentType) {
		String radical = agentType.getPreffix() + "_" + NodeManager.getNodeName();
		if(AgentType.PRODUCER.equals(agentType)) {
			return radical + "_" + nextProducerId;
		} else if(AgentType.CONSUMER.equals(agentType) || AgentType.CONTRACT.equals(agentType)) {
			return radical + "_" + nextConsumerId;
		}
		return radical;
	}

	private AgentAuthentication generateAgentAuthentication(AgentType agentType) {
		String agentName = generateAgentName(agentType);
		String authenticationKey = PasswordUtils.generateAuthenticationKey();
		String securedKey = useSecuresPasswords ? PasswordUtils.generateSecurePassword(authenticationKey, salt) : authenticationKey;
		AgentAuthentication authentication = new AgentAuthentication(agentName, agentType.getLabel(), securedKey, NodeManager.getNodeName(), NodeManager.getLocation());
		this.mapAgentAuthentication.put(authentication.getAgentName(), authentication);
		return authentication;
	}

	public List<Service> test1(ConfigRepository repository) {
		// Add producer agents
		initEnergyService(repository, "test1", NodeMarkovStates.DEFAULT_NODE_MAX_TOTAL_POWER, false, new HashMap<Integer,Integer>());
		Date current = getCurrentDate(); // getCurrentMinute();
		PricingTable pricingTable = new PricingTable(current, UtilDates.shiftDateDays(current, 365), 0);
		IProducerPolicy producerPolicy = initDefaultProducerPolicy();
		addServiceProducer(generateSupply(30., current, YEAR_DURATION_MIN,  new DeviceProperties("EDF", DeviceCategory.EXTERNAL_ENG, EnvironmentalImpact.MEDIUM, true), pricingTable), producerPolicy);
		addServiceProducer(generateSupply(15., current, 40., new DeviceProperties("wind turbine1", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true), pricingTable), producerPolicy);
		addServiceProducer(generateSupply(30., current, 30., new DeviceProperties("wind turbine2", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true), pricingTable), producerPolicy);
		addServiceProducer(generateSupply(100., current, 6.,new DeviceProperties("wind turbine3", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true), pricingTable), producerPolicy);

		// Add query
		addServiceConsumer(generateRequest(43.1, current, YEAR_DURATION_MIN, YEAR_DURATION_MIN , PriorityLevel.LOW, new DeviceProperties("Refrigerator", DeviceCategory.COLD_APPLIANCES, EnvironmentalImpact.MEDIUM, false), pricingTable)
				, null);
		/*
		addQueryConsumer(generateRequest(27.7, current, 0.1, 150., PriorityLevel.LOW, "Laptop Compute", DeviceCategory.ICT));
		addQueryConsumer(generateRequest(72.7, current, 0.1, 80., PriorityLevel.LOW, " MacBook Pro ", DeviceCategory.ICT));
		addQueryConsumer(generateRequest(10.0, current, 0.1, 10., PriorityLevel.LOW, "Led1", DeviceCategory.LIGHTING));
		 */
		// restartConsumer("Consumer_3", new Float("11"), current,
		// SapereUtil.shiftDateMinutes(current, 10), new Float(01));
		return getLsas();
	}

	public List<Service> test1bis(ConfigRepository repository) {
		// Add producer agents
		initEnergyService(repository, "test1bis", NodeMarkovStates.DEFAULT_NODE_MAX_TOTAL_POWER, false, new HashMap<Integer,Integer>());
		Date current = getCurrentDate(); // getCurrentMinute();
		// addServiceProducer(generateSupply(new Float(30.0), current, new
		PricingTable pricingTable = new PricingTable(current, UtilDates.shiftDateDays(current, 365), 0);
		IProducerPolicy producerPolicy = initDefaultProducerPolicy();
		addServiceProducer(generateSupply(150.0, current, 200.0, new DeviceProperties("EDF", DeviceCategory.EXTERNAL_ENG, EnvironmentalImpact.MEDIUM, true), pricingTable)
				, producerPolicy);

		// Add query
		addServiceConsumer(generateRequest(43.1, current, YEAR_DURATION_MIN,
				YEAR_DURATION_MIN, PriorityLevel.LOW, new DeviceProperties("Refrigerator", DeviceCategory.COLD_APPLIANCES, EnvironmentalImpact.MEDIUM, false), pricingTable)
				, null);

		return getLsas();
	}

	public List<Service> test1ter(ConfigRepository repository) {
		// Add producer agents
		initEnergyService(repository, "test1ter", NodeMarkovStates.DEFAULT_NODE_MAX_TOTAL_POWER, false, new HashMap<Integer,Integer>());
		Date current = getCurrentDate(); // getCurrentMinute();
		PricingTable pricingTable = new PricingTable(current, UtilDates.shiftDateDays(current, 365), 0);
		IProducerPolicy producerPolicy = initDefaultProducerPolicy();
		addServiceProducer(generateSupply(2000.0, current, YEAR_DURATION_MIN, new DeviceProperties("EDF", DeviceCategory.EXTERNAL_ENG, EnvironmentalImpact.MEDIUM, true), pricingTable)
				, producerPolicy);
		addServiceProducer(generateSupply(150.0, current, 120.0, new DeviceProperties("wind turbine1", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true), pricingTable)
				, producerPolicy);
		addServiceProducer(generateSupply(300.0, current, 120.0, new DeviceProperties("wind turbine2", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true), pricingTable)
				, producerPolicy);
		addServiceProducer(generateSupply(200.0, current, 120.0, new DeviceProperties("wind turbine3", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true), pricingTable)
				, producerPolicy);
		/**/

		// Add query
		addServiceConsumer(generateRequest(43.1, current, YEAR_DURATION_MIN,
				YEAR_DURATION_MIN, PriorityLevel.HIGH, new DeviceProperties("Refrigerator", DeviceCategory.COLD_APPLIANCES, EnvironmentalImpact.MEDIUM, false), pricingTable)
				, null);
		addServiceConsumer(generateRequest(270.7, current, 150.0, 150., PriorityLevel.LOW,  new DeviceProperties("Household Fan ", DeviceCategory.OTHER, EnvironmentalImpact.MEDIUM, false), pricingTable)
				, null);
		addServiceConsumer(generateRequest(720.7, current, 80., 80., PriorityLevel.LOW, new DeviceProperties(" Toaster", DeviceCategory.COOKING, EnvironmentalImpact.MEDIUM, false), pricingTable)
				, null);
		addServiceConsumer(generateRequest(100.0, current, 50., 50., PriorityLevel.LOW, new DeviceProperties("iPad / Tablet", DeviceCategory.ICT, EnvironmentalImpact.MEDIUM, false), pricingTable)
				, null);
		/**/

		// restartConsumer("Consumer_3", new Float("11"), current,
		// SapereUtil.shiftDateMinutes(current, 10), new Float(01));
		return getLsas();
	}

	public List<Service> test2(ConfigRepository repository) {
		initEnergyService(repository, "test2", NodeMarkovStates.DEFAULT_NODE_MAX_TOTAL_POWER, false, new HashMap<Integer,Integer>());
		Date current = nodeContext.getCurrentDate(); // getCurrentMinute();
		PricingTable pricingTable = new PricingTable(current, UtilDates.shiftDateDays(current, 365), 0);
		int nbAgents = 10;
		for(int i = 0; i < nbAgents; i++) {
			IProducerPolicy producerPolicy = initDefaultProducerPolicy();
			addServiceProducer(
				generateSupply(25.0, current, 60., new DeviceProperties("wind turbine " + i, DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true), pricingTable)
				, producerPolicy);
		}
		for(int i = 0; i < nbAgents; i++) {
			addServiceConsumer(
				generateRequest(30+0.1*i, current, 120., 120., PriorityLevel.LOW,  new DeviceProperties("Laptop "+i, DeviceCategory.ICT, EnvironmentalImpact.MEDIUM, false), pricingTable)
				, null);
		}
		return getLsas();
	}

	public List<Service> test3(ConfigRepository repository) {
		initEnergyService(repository, "test3", NodeMarkovStates.DEFAULT_NODE_MAX_TOTAL_POWER, false, new HashMap<Integer,Integer>());
		Date current = nodeContext.getCurrentDate(); // getCurrentMinute();
		PricingTable pricingTable = new PricingTable(current, UtilDates.shiftDateDays(current, 365), 0);
		IProducerPolicy producerPolicy = initDefaultProducerPolicy();
		addServiceProducer(
			generateSupply(30.0, current, YEAR_DURATION_MIN, new DeviceProperties("EDF", DeviceCategory.EXTERNAL_ENG, EnvironmentalImpact.MEDIUM, true), pricingTable)
			, producerPolicy);
		addServiceProducer(
			generateSupply(25.0, current, 60., new DeviceProperties("wind turbine1", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true), pricingTable)
			, producerPolicy);
		addServiceProducer(
			generateSupply(25.0, current, 60., new DeviceProperties("wind turbine2", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true), pricingTable)
			, producerPolicy);
		addServiceProducer(generateSupply(25.0, current, 60., new DeviceProperties("wind turbine3", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true), pricingTable)
			, producerPolicy);
		addServiceConsumer(
			generateRequest(24.7, current, 150., 150., PriorityLevel.LOW, new DeviceProperties("Laptop 1", DeviceCategory.ICT, EnvironmentalImpact.MEDIUM, false), pricingTable)
			,null);
		return getLsas();
	}

	public List<Service> test4(ConfigRepository repository) {
		initEnergyService(repository, "test4", NodeMarkovStates.DEFAULT_NODE_MAX_TOTAL_POWER, false, new HashMap<Integer,Integer>());
		Date current = nodeContext.getCurrentDate(); // getCurrentMinute();
		PricingTable pricingTable = new PricingTable(current, UtilDates.shiftDateDays(current, 365), 0);
		//addServiceProducer(generateSupply(new Float(30.0), current, YEAR_DURATION_MIN, "EDF", DeviceCategory.EXTERNAL_ENG));
		DeviceProperties solorPanel = new DeviceProperties("solar panel1", DeviceCategory.SOLOR_ENG, EnvironmentalImpact.LOW, true);
		IProducerPolicy producerPolicy = initDefaultProducerPolicy();
		addServiceProducer(
			generateSupply(50.0, current, 10., solorPanel, pricingTable)
			, producerPolicy);
		DeviceProperties tvLCD = new DeviceProperties("TV 32 LED/LCD ", DeviceCategory.AUDIOVISUAL, EnvironmentalImpact.MEDIUM, false);
		addServiceConsumer(
			generateRequest(97.7, current, 150., 150., PriorityLevel.LOW, tvLCD, pricingTable)
			, null);
		int nbAgents = 5;
		for(int i = 0; i < nbAgents; i++) {
			DeviceProperties lapTop = new DeviceProperties( "Laptop "+i, DeviceCategory.ICT, EnvironmentalImpact.MEDIUM, false);
			addServiceConsumer(
				generateRequest(30+0.1*i, current, 120., 120., PriorityLevel.LOW, lapTop, pricingTable)
				, null);
		}
		return getLsas();
	}


	public List<Service> test5(ConfigRepository repository) {
		initEnergyService(repository, "test5", NodeMarkovStates.DEFAULT_NODE_MAX_TOTAL_POWER, false, new HashMap<Integer,Integer>());
		// Add producer agents
		Date current = getCurrentDate();
		PricingTable pricingTable = new PricingTable(current, UtilDates.shiftDateDays(current, 365), 0);
		DeviceProperties devicePropeties1 = new DeviceProperties("EDF", DeviceCategory.EXTERNAL_ENG, EnvironmentalImpact.MEDIUM, true);
		IProducerPolicy producerPolicy = initDefaultProducerPolicy();
		addServiceProducer(
			generateSupply(2700.0, current, YEAR_DURATION_MIN, devicePropeties1, pricingTable)
			, producerPolicy);
		// Add query
		/*
		addQueryConsumer(
				generateRequest(new Float(10.0), current, new Float(0.5), new Float(0.5), PriorityLevel.LOW, "Led1", DeviceCategory.LIGHTING));
		*/
		return getLsas();
	}


	private PricingTable initPricingTable(int minutesByStep) {
		Date current = nodeContext.getCurrentDate();
		Date end = UtilDates.shiftDateMinutes(current, 60);
		int time = 0;
		Map<Integer, Double> simplePicingTable  = new HashMap<Integer, Double>();
		simplePicingTable.put(time, 10.0);
		time+=1;
		simplePicingTable.put(time, 10.0);
		time+=1;
		simplePicingTable.put(time, 6.0);
		time+=minutesByStep;
	    simplePicingTable.put(time, 7.0);
	    time+=minutesByStep;
	    simplePicingTable.put(time, 8.0);
	    time+=minutesByStep;
	    simplePicingTable.put(time, 9.0);
	    time+=minutesByStep;
	    simplePicingTable.put(time, 10.0);
		Date lastStepDate = null;
		PricingTable pricingTable = new PricingTable();
		SortedSet<Integer> keys = new TreeSet<>(simplePicingTable.keySet());
		for(int step : keys) {
			Double rate = simplePicingTable.get(step);
			Date nextStepDate = UtilDates.shiftDateMinutes(current, step);
			if(lastStepDate != null) {
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
	public List<Service> testTragedyOfTheCommons(ConfigRepository repository, boolean useProducerPolicy) {
		initEnergyService(repository, "testTragedyOfTheCommons", NodeMarkovStates.DEFAULT_NODE_MAX_TOTAL_POWER, false, new HashMap<Integer,Integer>());
		Date current = nodeContext.getCurrentDate(); // getCurrentMinute();
		int minutesByStep = 3;
		PricingTable pricingTable = initPricingTable(minutesByStep);//new PricingTable();
		/*
		 * Date end = UtilDates.shiftDateMinutes(current, 60);
		int step1Minutes = 1;
		pricingTable.addPrice(UtilDates.shiftDateMinutes(current, 0)			, UtilDates.shiftDateMinutes(current, step1Minutes), 10);
		pricingTable.addPrice(UtilDates.shiftDateMinutes(current, step1Minutes)	, UtilDates.shiftDateMinutes(current, step1Minutes+5), 7);
		pricingTable.addPrice(UtilDates.shiftDateMinutes(current, step1Minutes+5), end, 10);
		*/
		//addServiceProducer(generateSupply(new Float(30.0), current, YEAR_DURATION_MIN, "EDF", DeviceCategory.EXTERNAL_ENG));
		IProducerPolicy producerPolicy =
				useProducerPolicy
					? new LowestDemandPolicy(pricingTable, IProducerPolicy.POLICY_PRIORIZATION)
					: initDefaultProducerPolicy();
		DeviceProperties devicePropeties1 = new DeviceProperties("SIG", DeviceCategory.EXTERNAL_ENG, EnvironmentalImpact.MEDIUM, true);
		addServiceProducer(
			generateSupply(33.0, current, 60., devicePropeties1, pricingTable)
			, producerPolicy);
		IConsumerPolicy lowestPricePolicy = new LowestPricePolicy();
		int nbAgents = 10;
		//nbAgents = 1;
		for(int i = 0; i < nbAgents; i++) {
			Date dateBegin = UtilDates.shiftDateSec(current, 10);
			Date dateEnd = UtilDates.shiftDateSec(dateBegin, minutesByStep*60);
			Double power = 10.0;
			DeviceProperties devicePropeties2 = new DeviceProperties("Battery "+i, DeviceCategory.OTHER, EnvironmentalImpact.MEDIUM, false);
			EnergyRequest request = generateRequest(power, dateBegin, dateEnd, 120., PriorityLevel.LOW,  devicePropeties2, new PricingTable());
			addServiceConsumer(request, lowestPricePolicy);
		}
		return getLsas();
	}


	public List<Service> test6(ConfigRepository repository) {
		initEnergyService(repository, "test6", NodeMarkovStates.DEFAULT_NODE_MAX_TOTAL_POWER, false, new HashMap<Integer,Integer>());
		Date current = nodeContext.getCurrentDate(); // getCurrentMinute();
		//addServiceProducer(generateSupply(new Float(30.0), current, YEAR_DURATION_MIN, "EDF", DeviceCategory.EXTERNAL_ENG));
		PricingTable pricingTable = new PricingTable(current, UtilDates.shiftDateDays(current, 365), 0);
		DeviceProperties devicePropeties1 = new DeviceProperties("solar panel1", DeviceCategory.SOLOR_ENG, EnvironmentalImpact.LOW, true);
		IProducerPolicy producerPolicy = initDefaultProducerPolicy();
		addServiceProducer(generateSupply(33.0, current, 10., devicePropeties1, null)
			, producerPolicy);
		int nbAgents = 10;
		for(int i = 0; i < nbAgents; i++) {
			Date dateBegin = UtilDates.shiftDateSec(current, 5 + 65*i);
			Date dateEnd = UtilDates.shiftDateSec(dateBegin, 60);
			DeviceProperties devicePropeties2 = new DeviceProperties("Battery "+(1+i), DeviceCategory.OTHER,EnvironmentalImpact.MEDIUM, false);
			EnergyRequest request = generateRequest(10.0, dateBegin, dateEnd, 120., PriorityLevel.LOW,  devicePropeties2, pricingTable);
			addServiceConsumer(request, null);
		}
		return getLsas();
	}

	public List<Service> initState(ConfigRepository repository, String variable, MarkovState targetState, boolean _supervisionDisabled) {
		//initEnergyService(repository, "test5");
		// Add producer agents
		List<Device> devices = getNodeDevices();
		Collections.shuffle(devices);
		Date current = getCurrentDate();
		PricingTable pricingTable = new PricingTable(current, UtilDates.shiftDateDays(current, 365), 0);
		double minPower = targetState.getMinValue();
		double maxPower = targetState.getMaxValue()==null? 1.7*minPower:targetState.getMaxValue();
		double powerRandom = Math.random();
		double targetPower = minPower +  ( powerRandom * (maxPower - minPower));
		int nbAgents = (targetPower==0)? 0 : 5;
		supervisionDisabled = _supervisionDisabled;
		if("produced".equals(variable) || "available".equals(variable) || "consumed".equals(variable) || "provided".equals(variable)) {
			List<String> devicesProd = new ArrayList<>();
			for(Device device : devices) {
				if(DeviceCategory.WIND_ENG.equals(device.getCategory())
					|| DeviceCategory.SOLOR_ENG.equals(device.getCategory())
					|| DeviceCategory.BIOMASS_ENG.equals(device.getCategory())
							) {
					devicesProd.add(device.getName());
				}
			}
			for(int i = 0; i < nbAgents; i++) {
				double agentPower = SapereUtil.round(targetPower/nbAgents, 2);
				DeviceProperties devicePropeties = new DeviceProperties( "wind turbine " + i, DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, true);
				IProducerPolicy producerPolicy = initDefaultProducerPolicy();
				addServiceProducer(
					generateSupply(agentPower, current, 60., devicePropeties, pricingTable)
					, producerPolicy);
			}
		}
		if ("requested".equals(variable) || "missing".equals(variable) || "consumed".equals(variable) || "provided".equals(variable)) {
			List<Device> consumerDevices = new ArrayList<>();
			for(Device device : devices) {
				//if(DeviceCategory.ICT.getLabel().equals(device.getCategory())) {
				if(!device.isProducer()) {
					consumerDevices.add(device);
				}
			}
			for(int i = 0; i < nbAgents; i++) {
				double agentPower = SapereUtil.round(targetPower/nbAgents, 2);
				Device nextDevice = consumerDevices.get(i);
				addServiceConsumer(
					generateRequest(agentPower, current, 120., 120., PriorityLevel.LOW, nextDevice.getProperties(), pricingTable)
					,null);
			}
		}
		try {
			LearningAgent learningAgent  = getLearningAgent();
			learningAgent.refreshHistory(null);
			learningAgent.refreshMarkovChains(true, false);
			MarkovState currentState = learningAgent.getCurrentMarkovState(variable);
			boolean stateReached = currentState!=null && targetState.getId().equals(currentState.getId());
			int cpt = 0;
			while (!stateReached && cpt < 10) {
				Thread.sleep(5*1000);
				learningAgent.refreshHistory(null);
				learningAgent.refreshMarkovChains(true, false);
				currentState = learningAgent.getCurrentMarkovState(variable);
				stateReached = currentState!=null && targetState.getId().equals(currentState.getId());
				cpt++;
			}
			if(stateReached) {
				logger.info("*** Sapere.initState : " + variable + " state " + currentState.getLabel() + " is reached");
			} else {
				logger.warning("### Sapere.initState : " + variable + " state " + currentState.getLabel() + " is not reached");
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
		if(authentifiedAgentsCash.contains(agentAuthentication)) {
			return true;
		}
		String authenticationKey = agentAuthentication.getAuthenticationKey();
		boolean result = false;
		if(this.mapAgentAuthentication.containsKey(agentName)) {
			String key = (mapAgentAuthentication.get(agentName)).getAuthenticationKey();
			if(useSecuresPasswords) {
				String securedKey =  PasswordUtils.generateSecurePassword(key, salt);
				result = securedKey.equals(authenticationKey);
			} else {
				result = key.equals(authenticationKey);
			}
		}
		if(result) {
			authentifiedAgentsCash.add(agentAuthentication);
		}
		return result;
	}

	private void addServiceLearningAgent(String agentName /*, String scenario, Map<Integer,Integer> _datetimeShifts*/) {
		if(nodeContext==null ) {
			throw new RuntimeException("addServiceLearningAgent : undefined node context");
		}
		String scenario = nodeContext.getScenario();
		if(scenario==null || scenario.length()==0) {
			throw new RuntimeException("addServiceLearningAgent : undefined scenario");
		}
		AgentAuthentication authentication = generateAgentAuthentication(AgentType.LEARNING_AGENT);
		LearningAgent learningAgent = new LearningAgent(agentName, authentication, nodeContext);
		ServiceAgents.add(learningAgent);
		startService(agentName);
	}

	private void addServiceRegulatorAgent(String agentName) {
		AgentAuthentication authentication = generateAgentAuthentication(AgentType.REGULATOR);
		RegulatorAgent regulatorAgent = new RegulatorAgent(agentName, authentication, nodeContext);
		ServiceAgents.add(regulatorAgent);
		startService(agentName);
	}

	private LearningAgent getLearningAgent() {
		for(SapereAgent agent : ServiceAgents) {
			if(agent instanceof LearningAgent) {
				return (LearningAgent) agent;
			}
		}
		return null;
	}

	public RegulatorAgent getRegulatorAgent() {
		for(SapereAgent agent : ServiceAgents) {
			if(agent instanceof RegulatorAgent) {
				return (RegulatorAgent) agent;
			}
		}
		return null;
	}

	public ConsumerAgent addServiceConsumer(Double power, Date beginDate, Date endDate, Double _delayToleranceMinutes,
			PriorityLevel _priority, DeviceProperties deviceProperties, PricingTable pricingTable, IConsumerPolicy consumerPolicy) {
		EnergyRequest request = generateRequest(power, beginDate, endDate, _delayToleranceMinutes, _priority, deviceProperties, pricingTable);
		return addServiceConsumer(request, consumerPolicy);
	}

	private ConsumerAgent addServiceConsumer(EnergyRequest need, IConsumerPolicy consumerPolicy) {
		checkInitialisation();
		try {
			need.checkBeginNotPassed();
			synchronized (mapAgentAuthentication) {
				AgentAuthentication authentication = generateAgentAuthentication(AgentType.CONSUMER);
				ConsumerAgent consumerAgent = new ConsumerAgent(nextConsumerId, authentication, need, consumerPolicy);
				synchronized(consumerAgent) {
					consumerAgent.setInitialLSA();
				}
				ServiceAgents.add(consumerAgent);
				//addServiceContract(consumerAgent);
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
		if(!isRunning) {
			return null;
		}
		if(producerPolicy == null) {
			logger.error("addServiceProducer : producerPolicy is not set");
		}
		checkInitialisation();
		supply.checkBeginNotPassed();
		synchronized (mapAgentAuthentication) {
			AgentAuthentication authentication = generateAgentAuthentication(AgentType.PRODUCER);
			ProducerAgent producerAgent = new ProducerAgent(nextProducerId, authentication, supply, producerPolicy);
			nextProducerId++;
			ServiceAgents.add(producerAgent);
			synchronized(producerAgent) {
				producerAgent.setInitialLSA();
			}
			SapereLogger.getInstance().info("Add new producer " + producerAgent.getAgentName());
			return producerAgent;
		}
	}

	public ProducerAgent addServiceProducer(Double power, Date beginDate, Date endDate, DeviceProperties deviceProperties, PricingTable pricingTable, IProducerPolicy producerPolicy) {
		if(!isRunning) {
			return null;
		}
		EnergySupply supply = generateSupply(power, beginDate, endDate, deviceProperties, pricingTable);
		return this.addServiceProducer(supply, producerPolicy);
	}

	public ConsumerAgent restartConsumer(String consumerAgentName, EnergyRequest need) {
		if(!isRunning) {
			return null;
		}
		if(!(this.getAgent(consumerAgentName) instanceof ConsumerAgent)) {
			return null;
		}
		if(need.getIssuerLocation()==null) {
			need.setIssuerLocation(NodeManager.getLocation());
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
					if(!ServiceAgents.contains(consumerAgent)) {
						ServiceAgents.add(consumerAgent);
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
		while(!isInSpace(consumerAgentName) && nbWait<30) {
			logger.info("restartConsumer : agent " + consumerAgentName + " not in sapce : Waiting ");
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				logger.error(e);
			}
			nbWait++;
		}
		nbWait = 0;
		// Return consumer agent
		SapereAgent consumerAgent1 = getAgent(consumerAgentName);
		if(consumerAgent1 instanceof ConsumerAgent) {
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
		if(stopEvent == null) {
			generateStopEvent(agentName, warning);
		}
	}

	public boolean isAgentStopped(String agentName) {
		SapereAgent agent = this.getAgent(agentName);
		boolean result = false;
		if(agent instanceof IEnergyAgent) {
			result = ((IEnergyAgent) agent).hasExpired();
		}
		return result;
	}

	public EnergyEvent getStopEvent(String agentName) {
		SapereAgent agent = this.getAgent(agentName);
		EnergyEvent result = null;
		if(agent instanceof IEnergyAgent) {
			result = ((IEnergyAgent) agent).getStopEvent();
		}
		return result;
	}

	public EnergyEvent generateStopEvent(String agentName, RegulationWarning warning) {
		SapereAgent agent = this.getAgent(agentName);
		EnergyEvent result = null;
		try {
			if(agent instanceof IEnergyAgent) {
				result = ((IEnergyAgent) agent).generateStopEvent(warning, "");
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return result;
	}

	public boolean stopListAgents(List<String> listAgentName, RegulationWarning warning) {
		List<String> agentsToStop = new ArrayList<String>();
		for(String agentName : listAgentName) {
			if(!isInSpace(agentName)) {
				// Agent already out of tuple space
				setAgentExpired(agentName, warning);
				//return this.getAgent(agentName);
			} else {
				agentsToStop.add(agentName);
			}
		}
		if(agentsToStop.size() == 0) {
			return true;
		}
		// Use the regulator agent to send a user interruption
		SapereAgent regAgent = getAgent(regulatorAgentName);
		if(regAgent instanceof RegulatorAgent) {
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
		for(String agentName : agentsToStop) {
			if(isInSpace(agentName)) {
				logger.info("stopAllAgents : agent " + agentName  + " is in space");
				nbNotStopped++;
			} else {
				NodeManager.instance().getNotifier().unsubscribe(agentName);
				nbStopped++;
			}
		}
		logger.info("stopListAgents : nbStopped = " + nbStopped + ", nbNotStopped = " + nbNotStopped);
		return nbNotStopped==0;
	}

	public SapereAgent stopAgent(String agentName, RegulationWarning warning) {
		if(!isInSpace(agentName)) {
			// Agent already out of tuple space
			setAgentExpired(agentName, warning);
			return this.getAgent(agentName);
		}
		// Use the regulator agent to send a user interruption
		SapereAgent regAgent = getAgent(regulatorAgentName);
		if(regAgent instanceof RegulatorAgent) {
			RegulatorAgent regulatorAgent = (RegulatorAgent) regAgent;
			regulatorAgent.interruptAgent(agentName);
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			logger.error(e1);
		}
		int nbWaiting=0;
		// Wait untill the agent is stopped
		while(isInSpace(agentName) &&  nbWaiting<20) {
			try {
				Thread.sleep(100);
				nbWaiting++;
			} catch (InterruptedException e) {
				logger.error(e);
			}
		}
		logger.info("stopAgent " + agentName + ": isInSpace = " + isInSpace(agentName) );
		// Wait untill the agent is stopped
		while( !isAgentStopped(agentName) &&  nbWaiting<20) {
			try {
				Thread.sleep(100);
				if(!isAgentStopped(agentName) && !isInSpace(agentName)) {
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
		for(SapereAgent nextAgent : ServiceAgents) {
			String agentName = nextAgent.getAgentName();
			if(isInSpace(agentName)) {
				logger.info("getRunningServiceAgents : agent " + nextAgent.getAgentName()+ " is in space");
				result.add(agentName);
			}
		}
		return result;
	}

	public boolean stopAllAgents() {
		// Stop energy agents
		// Use the regulator agent to send a user interruption
		List<String> listRunningAgents = getRunningServiceAgents();
		if(listRunningAgents.size() > 0) {
			SapereAgent regAgent = getAgent(regulatorAgentName);
			synchronized (regAgent) {
				if(regAgent instanceof RegulatorAgent) {
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
			for(SapereAgent nextAgent : ServiceAgents) {
				if(isInSpace(nextAgent.getAgentName())) {
					logger.info("stopAllAgents : agent " + nextAgent.getAgentName()+ " is in space");
					nbNotStopped++;
				} else {
					NodeManager.instance().getNotifier().unsubscribe(nextAgent.getAgentName());
					nbStopped++;
				}
			}
			logger.info("stopAllAgents1 : nbStopped = " + nbStopped + ", nbNotStopped = " + nbNotStopped);
			return nbNotStopped==0;
		} else {
			return true;
		}
	}


	public EnergySupply getAgentSupply(String agentName) {
		EnergySupply result = null;
		SapereAgent agent = this.getAgent(agentName);
		if(agent instanceof EnergyAgent) {
			result = ((EnergyAgent) agent).getEnergySupply();
		}
		if(result!=null) {
			return result.clone();
		}
		return result;
	}

	public SapereAgent modifyAgent(String agentName, EnergySupply energySupply) {
		// Use the regulator agent to send a user interruption
		if(energySupply.getIssuerLocation()==null) {
			energySupply.setIssuerLocation(NodeManager.getLocation());
		}
		SapereAgent regAgent = getAgent(regulatorAgentName);
		if(regAgent instanceof RegulatorAgent) {
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
		boolean updateDone = newAgentSpply!=null && (Math.abs(newAgentSpply.getPower() - energySupply.getPower()) < 0.001);
		while(!updateDone && nbWait < 10) {
			try {
				Thread.sleep(200);
				// Refresh agent supply
				newAgentSpply = this.getAgentSupply(agentName);
				// Refresh updateD
				updateDone = newAgentSpply!=null && (Math.abs(newAgentSpply.getPower() - energySupply.getPower()) < 0.001);
				nbWait++;
			} catch (InterruptedException e) {
				e.printStackTrace();
				logger.error(e);
			}
		}
		return this.getAgent(agentName);
	}

	public PredictionData getPrediction(Date initDate, Date targetDate, String location, boolean useCorrections) {
		checkInitialisation();
		LearningAgent learningAgent = this.getLearningAgent();
		List<Date> targetDates = new ArrayList<Date>();
		targetDates.add(targetDate);
		//PredictionData result = learningAgent.computePrediction(initDate, targetDates, location);
		PredictionContext predictionContext = learningAgent.getPredictionContextCopy();
		PredictionData result = PredictionHelper.getInstance().computePrediction2(predictionContext, initDate, targetDates, learningAgent.getVariables(), useCorrections);
		return result;
	}

	public MultiPredictionsData generateMassivePredictions(TimeSlot targetDateSlot, String location, String variableName, int horizon, boolean useCorrections, boolean generateCorrections) {
		checkInitialisation();
		LearningAgent learningAgent = this.getLearningAgent();
		PredictionContext predictionContext = learningAgent.getPredictionContextCopy();
		return PredictionHelper.getInstance().generateMassivePredictions(predictionContext, targetDateSlot, horizon
				, variableName, useCorrections, generateCorrections);
	}

	public List<PredictionStatistic> computePredictionStatistics(
			Date minComputeDay, Date maxComputeDay,
			Date minTargetDate, Date maxTargetDate, String location, Integer minHour, Integer maxHour, Boolean useCorrectionFilter, String variableName, List<String> fieldsToMerge) {
		List<PredictionStatistic> result = new ArrayList<PredictionStatistic>();
		try {
			LearningAgent learningAgent = this.getLearningAgent();
			PredictionContext predictionContext = learningAgent.getPredictionContextCopy();
			Map<String, PredictionStatistic> mapStatistics = PredictionHelper.getInstance().computePredictionStatistics(predictionContext,
					minComputeDay, maxComputeDay, minTargetDate, maxTargetDate, minHour, maxHour, useCorrectionFilter, variableName, fieldsToMerge);
			for(PredictionStatistic stat : mapStatistics.values()) {
				result.add(stat);
			}
			Collections.sort(result,  new Comparator<PredictionStatistic>() {
			    public int compare(PredictionStatistic predictionStatistic1, PredictionStatistic predictionStatistic2) {
			        return predictionStatistic1.compareTo(predictionStatistic2);
			    }
			   });
		} catch (Exception e) {
			logger.error(e);
		}
		return result;
	}

	public List<MarkovStateHistory> retrieveLastMarkovHistoryStates(Date minCreationDate, String variableName, boolean observationUpdated) {
		checkInitialisation();
		LearningAgent learningAgent = this.getLearningAgent();
		List<MarkovStateHistory> result = learningAgent.retrieveLastMarkovHistoryStates(minCreationDate, variableName, observationUpdated);
		return result;
	}

	public ProducerAgent restartProducer(String agentName, EnergySupply supply) {
		logger.info("restartProducer " + agentName);
		if(supply.getIssuerLocation()==null) {
			supply.setIssuerLocation(NodeManager.getLocation());
		}
		if(!isInSpace(agentName)) {
			supply.checkBeginNotPassed();
			NodeManager.instance().getNotifier().unsubscribe(agentName);
			SapereAgent agent = this.getAgent(agentName);
			if (agent instanceof ProducerAgent) {
				ProducerAgent producerAgent = (ProducerAgent) agent;
				synchronized (producerAgent) {
					int id = producerAgent.getId();
					//ServiceAgents.remove(producerAgent);
					AgentAuthentication authentication = producerAgent.getAuthentication();
					producerAgent.reinitialize(id, authentication, supply);
					if(!ServiceAgents.contains(producerAgent)) {
						ServiceAgents.add(producerAgent);
					}
					producerAgent.setInitialLSA();
				}
			}
		}
		// Wait untill contract agent is in sapce
		int nbWait = 0;
		while(!isInSpace(agentName) && nbWait<30) {
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
		if(agent instanceof ProducerAgent) {
			return (ProducerAgent) agent;
		}
		return null;
	}

	public boolean isLocalAgent(String agentName) {
		boolean isLocal = false;
		if(mapAgentAuthentication.containsKey(agentName)) {
			AgentAuthentication authentication = this.mapAgentAuthentication.get(agentName);
			isLocal = (NodeManager.getLocation().equals(authentication.getAgentLocation()));
		}
		return isLocal;
	}

	public boolean isInSpace(String agentName) {
		boolean isLocal = isLocalAgent(agentName);
		String agentName2 = agentName + (isLocal?"":"*");
		boolean result = NodeManager.instance().getSpace().getAllLsa().containsKey(agentName2);
		if(!result) {
			//logger.info("isInSpace : for debug : " + agentName + " not in space");
		}
		return result;
	}

	public NodeContent retrieveNodeContent() {
		AgentFilter filter = new AgentFilter();
		return retrieveNodeContent(filter);
	}

	public NodeContent retrieveAllNodesContent(NodesAddresses nodeAddresses) {
		NodeContent content = new NodeContent(null, nodeContext.getTimeShiftMS());
		String[] listNodeBaseUrl = nodeAddresses.getListNodeBaseUrl();
		List<String> listNeighborUrl = new ArrayList<>();
		for(String nextNodeUrl : listNodeBaseUrl) {
			listNeighborUrl.add(nextNodeUrl);
		}
		//for(String neighborLocation : NodeManager.instance().getNetworkDeliveryManager().getNeighbours()) {
		for(String nextNodeUrl : listNeighborUrl) {
			// call distant web service
			//Properties envProperties = SapereUtil.loadPropertyFile(neighborLocation, logger);
			if(nextNodeUrl!=null) {
				//String baseUrl = "http://localhost:9191/energy/";
				//String baseUrl = "http://localhost:" + envProperties.getProperty("server.port") + "/energy/";
				String postResponse = UtilHttp.sendGetRequest(nextNodeUrl + "retrieveNodeContent", logger, debugLevel);
				if(postResponse!=null) {
					JSONObject jsonNodeContent = new JSONObject(postResponse);
					NodeContent neighbourNodeContant = UtilJsonParser.parseNodeContent(jsonNodeContent);
					content.merge(neighbourNodeContant);
				}
			}
		}
		return content;
	}

	public NodeContent retrieveNodeContent(AgentFilter filter) {
		long nodeTimeShiftMS = nodeContext == null? 0 : nodeContext.getTimeShiftMS();
		NodeContent content = new NodeContent(filter, nodeTimeShiftMS);
		Map<String, Lsa> lsaInSpace = NodeManager.instance().getSpace().getAllLsa();
		content.setNoFilter(true);
		for (SapereAgent agent : ServiceAgents) {
			if(filter.applyFilter(agent)) {
				boolean inSpace = lsaInSpace.containsKey(agent.getAgentName());
				if (agent instanceof ProducerAgent) {
					ProducerAgent producer = (ProducerAgent) agent;
					//producer.setInSpace(inSpace);
					content.addProducer(producer, inSpace);
				} else if (agent instanceof ConsumerAgent) {
					ConsumerAgent consumer = (ConsumerAgent) agent;
					// consumer.getLinkedAgents();
					//consumer.setInSpace(inSpace);
					content.addConsumer(consumer, inSpace);
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
		for(EnergyEvent event : events) {
			if(EventType.PRODUCTION_START.equals(event.getType())) {
				EnergySupply supply = generateSupply(event.getPower(), getCurrentDate(), event.getEndDate(), event.getDeviceProperties(), event.getPricingTable());
				IProducerPolicy producerPolicy = initDefaultProducerPolicy();
				this.addServiceProducer(supply, producerPolicy);
			} else if (EventType.REQUEST_START.equals(event.getType())) {
				current = getCurrentDate();
				double _delayToleranceMinutes = UtilDates.computeDurationMinutes(current, event.getEndDate());
				EnergyRequest request = generateRequest(event.getPower(), current, event.getEndDate(), _delayToleranceMinutes
						, PriorityLevel.LOW, event.getDeviceProperties(), event.getPricingTable());
				this.addServiceConsumer(request, null);
			}
		}
		return this.retrieveNodeContent();
	}

	public ConsumerAgent getConsumerAgent(String agentName) {
		SapereAgent agent = getAgent(agentName);
		if(agent instanceof ConsumerAgent ) {
			ConsumerAgent consumer = (ConsumerAgent) agent;
			return consumer;
		}
		return null;
	}
	public boolean isConsumerSatified(String agentName) {
		ConsumerAgent agent = getConsumerAgent(agentName);
		if(agent!=null) {
			return agent.isSatisfied();
		}
		return false;
	}
/*
	public String getConsumerConfirmTag(String agentName) {
		ConsumerAgent agent = getConsumerAgent(agentName);
		if(agent!=null) {
			return agent.getConfirmTag();
		}
		return null;
	}*/

	public NodeTransitionMatrices getCurrentNodeTransitionMatrices() {
		LearningAgent learningAgent = getLearningAgent();
		if(learningAgent!=null) {
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
		LearningAgent learningAgent = getLearningAgent();
		if(learningAgent!=null) {
			List<NodeTransitionMatrices> result = new ArrayList<NodeTransitionMatrices>();
			List<MarkovTimeWindow> listTimeWindows = new ArrayList<MarkovTimeWindow>();
			for(MarkovTimeWindow nextTimeWindow : LearningAgent.ALL_TIME_WINDOWS) {
				if(matrixFilter.applyFilter(nextTimeWindow)) {
					listTimeWindows.add(nextTimeWindow);
				}
			}
			PredictionContext predictionCtx = learningAgent.getPredictionContextCopy();
			predictionCtx.setLocation(matrixFilter.getLocation());
			String[] variables = learningAgent.getVariables();
			if(matrixFilter.getVariableName() != null && !"".equals(matrixFilter.getVariableName())) {
				String[] filterVariables = new String[] {matrixFilter.getVariableName()};
				variables = filterVariables;
			}
			Map<Integer,NodeTransitionMatrices> mapResult =
				PredictionDbHelper.loadListNodeTransitionMatrice(predictionCtx
						, variables
						, listTimeWindows, getCurrentDate());
			for(NodeTransitionMatrices next : mapResult.values()) {
				result.add(next);
			}
			Collections.sort(result, timeWindowComparator);
			return result;
		}
		return new ArrayList<NodeTransitionMatrices>();
	}

	public List<Device> retrieveMeyrinDevices() {
		List<Device> devices = EnergyDbHelper.retrieveMeyrinDevices();
		return devices;
	}

	public List<Device> getNodeDevices() {
		List<Device> devices = EnergyDbHelper.retrieveNodeDevices(devicePowerCoeffProducer,
				devicePowerCoeffConsumer);
		return devices;
	}

	private static List<DeviceCategory> deviceFilterToList(DeviceFilter deviceFilter) {
		List<DeviceCategory> categories = new ArrayList<DeviceCategory>();
		if(deviceFilter.getDeviceCategories() !=null) {
			for(String sCateogry : deviceFilter.getDeviceCategories()) {
				categories.add(DeviceCategory.getByName(sCateogry));
			}
		}
		return categories;
	}
	public Map<String, Double> retrieveDeviceStatistics(DeviceFilter deviceFilter, TimeFilter timeFilter) {
		List<DeviceCategory> categories = deviceFilterToList(deviceFilter);
		Calendar calendar = Calendar.getInstance();
		int hourOfDay = (timeFilter.getHourOfDay()==null) ? calendar.get(Calendar.HOUR_OF_DAY) : timeFilter.getHourOfDay();
		Map<String, Double>  deviceStatistics = EnergyDbHelper.retrieveDeviceStatistics(statisticPowerCoeffConsumer, statisticPowerCoeffProducer, categories, hourOfDay);
		 return deviceStatistics;
	}

	public  List<DeviceMeasure> retrieveDevicesMeasures(DeviceFilter deviceFilter, String featureType,  Date dateBegin, Date dateEnd) {
		List<DeviceCategory> categories = deviceFilterToList(deviceFilter);
		 List<DeviceMeasure> devicePower = EnergyDbHelper.retrieveDevicesMeasures(categories, featureType, dateBegin, dateEnd);
		 return devicePower;
	}

	public DeviceMeasure retrieveLastDevicesMeasure(String featureType, Date dateBegin, Date dateEnd) {
		return EnergyDbHelper.retrieveLastDevicesMeasure(featureType, dateBegin, dateEnd);
	}

	public void logProdAgents() {
		for (SapereAgent agent : ServiceAgents) {
			if(agent instanceof ProducerAgent) {
				ProducerAgent prodAgent = (ProducerAgent) agent;
				prodAgent.logAgent();
			}
		}
	}

	public void callSetInitialLSA(String agentName) {
		if(!isInSpace(agentName)) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error(e);
			}
			if(!isInSpace(agentName)) {
				SapereAgent agent = getAgent(agentName);
				synchronized (agent) {
					agent.setInitialLSA();
				}
			}
		}
	}

	public List<String> checkupNotInSpace() {
		List<String>  result = new ArrayList<>();
		for (SapereAgent agent : ServiceAgents) {
			if(!isInSpace(agent.getAgentName())) {
				boolean hasExpired = true;
				if(agent instanceof EnergyAgent) {
					hasExpired =  ((EnergyAgent) agent).hasExpired();
				}
				if(!hasExpired) {
					result.add(agent.getAgentName());
				}
			}
		}
		return result;
	}

	/**
	 * Cautious : do not call this method from an agent (causes a ConcurrentModificationException)
	 * THis mtho
	 */
	public void cleanSubscriptions() {
		Map<String, Integer> nbSubscriptions = NodeManager.instance().getNotifier().getNbSubscriptionsByAgent();
		for (SapereAgent agent : ServiceAgents) {
			if(!isInSpace(agent.getAgentName()) && isAgentStopped(agent.getAgentName())) {
				if(nbSubscriptions.containsKey(agent.getAgentName())) {
					NodeManager.instance().getNotifier().unsubscribe(agent.getAgentName());
				}
			}
		}
		for (QueryAgent agent : querys) {
			if(!isInSpace(agent.getAgentName()) && isAgentStopped(agent.getAgentName())) {
				if(nbSubscriptions.containsKey(agent.getAgentName())) {
					NodeManager.instance().getNotifier().unsubscribe(agent.getAgentName());
				}
			}
		}
		int totalSubscriptions =  NodeManager.instance().getNotifier().getNbSubscriptions();
		logger.info("after cleanSubscriptions : nbSubscriptions = " + totalSubscriptions);
	}
	public void checkupProdAgents() {
		for (SapereAgent agent : ServiceAgents) {
			if(agent instanceof ProducerAgent) {
				ProducerAgent prodAgent = (ProducerAgent) agent;
				prodAgent.checkup();
			}
		}
	}

	public static boolean isSupervisionDisabled() {
		return supervisionDisabled;
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
}
