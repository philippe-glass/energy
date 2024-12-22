package com.saperetest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sapereapi.db.DBConfig;
import com.sapereapi.log.SimulatorLogger;
import com.sapereapi.model.LaunchConfig;
import com.sapereapi.model.OperationResult;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.Device;
import com.sapereapi.model.energy.DeviceMeasure;
import com.sapereapi.model.energy.DeviceProperties;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.node.NodeContent;
import com.sapereapi.model.energy.node.NodeTotal;
import com.sapereapi.model.energy.input.AgentFilter;
import com.sapereapi.model.energy.input.AgentInputForm;
import com.sapereapi.model.energy.input.SimulatorLog;
import com.sapereapi.model.input.HistoryInitializationRequest;
import com.sapereapi.model.input.InitializationForm;
import com.sapereapi.model.learning.LearningModelType;
import com.sapereapi.model.learning.PredictionScope;
import com.sapereapi.model.learning.VariableStateHistory;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.model.learning.prediction.input.PredictionScopeFilter;
import com.sapereapi.model.learning.prediction.input.StateHistoryRequest;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.util.UtilDates;
import com.sapereapi.util.UtilHttp;
import com.sapereapi.util.UtilJsonParser;

import eu.sapere.middleware.lsa.values.IAggregateable;
import eu.sapere.middleware.node.NodeLocation;

public class TestSimulator {
	//protected static String baseUrl = "http://localhost:9090/energy/";
	protected static String defaultbaseUrl = "http://localhost:9090/energy/";
	protected static SimpleDateFormat format_datetime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	public final static String QUOTE = "\"";
	protected static int debugLevel = 0;
	protected static SimulatorLogger logger = SimulatorLogger.getInstance();
	protected static List<Device> allDevices = new ArrayList<Device>();
	protected static NodeContent nodeContent = null;
	protected static Map<String, Boolean> mapIsServiceInitialized = new HashMap<String, Boolean>();
	static Random random = new Random();
	protected static Map<Integer,Integer> datetimeShifts = new HashMap<Integer, Integer>();
	protected static DBConfig dbConfig = new DBConfig(
			"org.mariadb.jdbc.Driver"
			,"jdbc:mariadb://localhost/clemap_data_light"
			, "learning_agent", "sql2537");

	public final static double statisticPowerCoeffConsumer = 3.0;
	public final static double statisticPowerCoeffProducer = 12.0;//6.0;
	public final static double devicePowerCoeffProducer = 2.0;
	public final static double devicePowerCoeffConsumer = 0.25;

	protected static void initDb() {
		SimulatorDBHelper.init(dbConfig);
	}

	protected static void init(String args[]) throws Exception {
		String baseUrlTag = "-baseUrl:";
		for(String arg : args) {
			if(arg.startsWith(baseUrlTag)) {
				int len = baseUrlTag.length();
				String content = arg.substring(len);
				defaultbaseUrl = content;
				// -baseUrl:http://localhost:9191/energy/
			}
		}
		initDb();
		allDevices = getNodeDevices();
		nodeContent = getNodeContent(defaultbaseUrl);
		datetimeShifts = new HashMap<Integer, Integer>();
	}

	protected static int getCurrentHour() {
		Calendar aCalendar = Calendar.getInstance();
		if(datetimeShifts.containsKey(Calendar.HOUR_OF_DAY)) {
			aCalendar.add(Calendar.HOUR_OF_DAY, datetimeShifts.get(Calendar.HOUR_OF_DAY));
		}
		return aCalendar.get(Calendar.HOUR_OF_DAY);
	}

	protected static Device chooseSleepingDevice(NodeContent partialNodeContent, boolean isProducer, DeviceCategory category, double powerMin,
			double powerMax, List<String> excludedDevices) {
		List<Device> devices = getDevices(partialNodeContent, isProducer, category, Device.STATUS_SLEEPING, powerMin, powerMax, excludedDevices);
		if (devices.size() > 0) {
			Collections.shuffle(devices);
			return devices.get(0);
		}
		return null;
	}

	protected static Device findDeviceToRemove(List<Device> listDevices, double minPowerLimit) {
		for (Device device : listDevices) {
			if (device.getPowerMin() > minPowerLimit) {
				return device;
			}
		}
		return null;
	}

	protected static List<Device> aux_selectDevices(List<Device> listDevices, double powerTarget) {
		double powerMax = 0;
		List<Device> devices = new ArrayList<Device>();
		for (Device device : listDevices) {
			if (device.getPowerMin() <= powerTarget) {
				devices.add(device);
			}
		}
		List<Device> selectedDevices = new ArrayList<Device>();
		// while(powerMax < powerTarget && devices.size() > 0) {
		while (powerMax < powerTarget && devices.size() > 0) {
			double maxPowerMin = powerTarget - getPowerMin(selectedDevices);
			Device toRemove = null;
			while ((toRemove = findDeviceToRemove(devices, maxPowerMin)) != null) {
				devices.remove(toRemove);
			}
			// Remove devices where powerMin > toAdd
			if (devices.size() > 0) {
				Collections.shuffle(devices);
				Device selected = devices.remove(0);
				selectedDevices.add(selected);
				powerMax += selected.getPowerMax();
			}
		}
		return selectedDevices;
	}

	protected static double getPowerMin(List<Device> listDevices) {
		double powerMin = 0;
		for (Device device : listDevices) {
			powerMin += device.getPowerMin();
		}
		return powerMin;
	}

	protected static double getPowerMax(List<Device> listDevices) {
		double powerMax = 0;
		for (Device device : listDevices) {
			powerMax += device.getPowerMax();
		}
		return powerMax;
	}

	protected static double getCurrentPower(Collection<Device> listDevices) {
		double powerMin = 0;
		for (Device device : listDevices) {
			powerMin += device.getCurrentPower();
		}
		return powerMin;
	}

	protected static List<String> getDeviceNames(List<Device> listDevices) {
		List<String> result = new ArrayList<String>();
		for (Device device : listDevices) {
			result.add(device.getName());
		}
		return result;
	}

	protected static boolean checkup(List<Device> listDevices, double powerTarget) {
		return powerTarget >= getPowerMin(listDevices) && powerTarget <= getPowerMax(listDevices);
	}


	protected static List<Device> chooseListDevices(NodeContent partialNodeContent, boolean isProducer, DeviceCategory category, String deviceStatus, double powerTarget, List<String> excludedDevices) {
		List<Device> devices = getDevices(partialNodeContent, isProducer, category, deviceStatus, 0, powerTarget, excludedDevices);
		if (getPowerMax(devices) < powerTarget) {
			// not enough available power
			logger.info(
					"chooseListSleepingDevices " + category + " : not enough power available to reach "
							+ powerTarget + " watts. available : " + getPowerMax(devices) + "  in " + devices);
			List<Device> selected = aux_selectDevices(devices, powerTarget);
			return selected;
			// return new ArrayList<Device>();
		}
		List<Device> selected = aux_selectDevices(devices, powerTarget);
		boolean isOK = checkup(selected, powerTarget);
		int nbTry = 1;

		while (!isOK && nbTry < 100 && selected.size() > 0) {
			selected = aux_selectDevices(devices, powerTarget);
			logger.info("power min = " + getPowerMin(selected));
			logger.info("power max = " + getPowerMax(selected));
			isOK = checkup(selected, powerTarget);
			nbTry++;
		}

		if (isOK) {
			return selected;
		}
		return selected;
	}

	protected static AgentForm chooseRunningAgent(NodeContent partialContent, boolean isProducer, DeviceCategory deviceCategory, double minPowerfilter,
			double maxPowerFilter, List<String> deviceNamesToExclude ) {
		Map<String, AgentForm> mapRunningAgents = partialContent.getMapRunningAgents();
		ProsumerRole prosumerRoleFilter = isProducer ? ProsumerRole.PRODUCER : ProsumerRole.CONSUMER;
		if (mapRunningAgents.size() > 0) {
			List<AgentForm> runningAgents = new ArrayList<AgentForm>();
			for (AgentForm agentForm : mapRunningAgents.values()) {
				if (agentForm.getPower()> 0) {
					if (prosumerRoleFilter.equals(agentForm.getProsumerRole())) {
						if (deviceCategory == null || agentForm.hasDeviceCategory(deviceCategory)) {
							if (minPowerfilter <= 0 || agentForm.getPower() >= minPowerfilter) {
								if (maxPowerFilter <= 0 || agentForm.getPower() <= maxPowerFilter) {
									if(deviceNamesToExclude==null || !deviceNamesToExclude.contains(agentForm.getDeviceName())) {
										runningAgents.add(agentForm);
									}
								}
							}
						}
					}
				}
			}
			if (runningAgents.size() > 0) {
				Collections.shuffle(runningAgents);
				return runningAgents.get(0);
			}
		}
		return null;
	}

	protected static boolean aux_isRuinning(Device aDevice, Map<String, AgentForm> mapRunningAgents) {
		if (!mapRunningAgents.containsKey(aDevice.getName())) {
			return false;
		}
		AgentForm agentForm = mapRunningAgents.get(aDevice.getName());
		if(agentForm.getHasExpired()) {
			return false;
		}
		if(agentForm.getIsDisabled()) {
			return false;
		}
		return true;
	}

	protected static List<Device> getDevices(NodeContent partialNodeContent, boolean isProducer, DeviceCategory filterDeviceCateogry, String statusFilter,
			double minPowerfilter, double maxPowerFilter, List<String> excludedDevices) {
		List<Device> result = new ArrayList<Device>();
		Map<String, AgentForm> mapRunningAgents = partialNodeContent.getMapRunningAgents();
		//DeviceCategory filterDeviceCateogry = sDeviceCategory == null ? null : DeviceCategory.valueOf(sDeviceCategory);
		for (Device nextDevice : allDevices) {
			boolean isRunning = aux_isRuinning(nextDevice, mapRunningAgents);
			String deviceStatus = isRunning ? Device.STATUS_RUNNING : Device.STATUS_SLEEPING;
			if (nextDevice.isProducer() == isProducer) {
				if (filterDeviceCateogry == null || filterDeviceCateogry.equals(nextDevice.getCategory())) {
					if (statusFilter == null || statusFilter.equals(deviceStatus)) {
						if (minPowerfilter <= 0 || nextDevice.getPowerMin() >= minPowerfilter) {
							if (maxPowerFilter <= 0 || nextDevice.getPowerMin() <= maxPowerFilter)
								if(excludedDevices==null || !excludedDevices.contains(nextDevice.getName())) {
									result.add(nextDevice);
								}
						}
					}
				}
			}
		}
		return result;
	}

	protected static Device getDeviceByName(String name) {
		if (name != null) {
			for (Device device : allDevices) {
				if (name.equals(device.getName())) {
					return device;
				}
			}
		}
		return null;
	}

	protected static boolean hasDevice(NodeContent nodeContent, boolean isProducer, DeviceCategory category, String statusFilter, double minPowerFilter,
			double maxPowerFilter, List<String> excludedDevices) {
		List<Device> devices = getDevices(nodeContent, isProducer, category, statusFilter, minPowerFilter, maxPowerFilter, excludedDevices);
		return !devices.isEmpty();
	}

	protected static void executeScenario(String baseUrl, List<EnergyEvent> scenario) {
		List<EnergyEvent> eventQueue = new ArrayList<EnergyEvent>();
		for (EnergyEvent event : scenario) {
			eventQueue.add(event);
		}
		Date current = getCurrentDate();
		while (eventQueue.size() > 0) {
			List<EnergyEvent> toRemove = new ArrayList<EnergyEvent>();
			for (EnergyEvent event : eventQueue) {
				AgentInputForm agentForm = new AgentInputForm();
				agentForm.setPowers(event.getPower(), event.getPowerMin(), event.getPowerMax());
				agentForm.setBeginDate(event.getBeginDate());
				agentForm.setEndDate(event.getEndDate());
				agentForm.setDuration(event.getDuration());
				DeviceProperties deviceProperties = event.getIssuerProperties().getDeviceProperties();
				agentForm.setDeviceName(deviceProperties.getName());
				agentForm.setDeviceCategory(deviceProperties.getCategory());
				if (EventType.PRODUCTION_START.equals(event.getType())) {
					agentForm.setProsumerRole(ProsumerRole.PRODUCER);
				} else if (EventType.REQUEST_START.equals(event.getType())) {
					agentForm.setProsumerRole(ProsumerRole.CONSUMER);
					double tolerance = UtilDates.computeDurationMinutes(event.getBeginDate(), event.getEndDate());
					agentForm.setDelayToleranceMinutes(tolerance);
					agentForm.setPriorityLevel(PriorityLevel.LOW);
				}
				if (event.getBeginDate().before(current)) {
					addAgent(baseUrl, agentForm);
					toRemove.add(event);
				}
			}
			for (EnergyEvent event : toRemove) {
				eventQueue.remove(event);
			}
		}
	}

	protected static NodeContent restartLastNodeContent(String baseUrl) {
		String postResponse = UtilHttp.sendGetRequest(baseUrl + "restartLastNodeContent", logger, debugLevel);
		JSONObject jsonNodeContent = new JSONObject(postResponse);
		if (debugLevel > 2) {
			logger.info("obj : " + jsonNodeContent);
		}
		NodeContent result = UtilJsonParser.parseNodeContent(jsonNodeContent, logger);
		return result;
	}

	protected static List<Device> getMeyrinDevices() throws Exception {
		List<Device> result = SimulatorDBHelper.retrieveMeyrinDevices();
		return result;
	}

	protected static List<Device> getNodeDevices() throws Exception {
		List<Device> devices = SimulatorDBHelper.retrieveNodeDevices(
				TestSimulator.devicePowerCoeffProducer,
				TestSimulator.devicePowerCoeffConsumer);
		return devices;
	}

	/*
	protected static NodeTransitionMatrices getCurrentNodeTransitionMatrices(String baseUrl) {
		try {
			String getResponse = UtilHttp.sendGetRequest(baseUrl + "getCurrentNodeTransitionMatrices", logger, debugLevel);
			JSONObject jsonTransitionMatrices = new JSONObject(getResponse);
			NodeTransitionMatrices result = UtilJsonParser.parseNodeTransitionMatrices(jsonTransitionMatrices, logger);
			return result;
		} catch (Exception e1) {
			logger.error(e1);;
		}
		return null;
	}*/

	public static PredictionContext getPredictionContext(String baseUrl, PredictionScope predictionScope) {
		try {
			PredictionScopeFilter scopeFilter = new PredictionScopeFilter();
			scopeFilter.setScope(predictionScope.toOptionItem());
			Map<String, Object> params = UtilHttp.generateRequestParams(scopeFilter, format_datetime, logger, UtilHttp.METHOD_POST);
			String getResponse = UtilHttp.sendPostRequest(baseUrl + "getPredictionContext", params, logger, debugLevel);
			if(getResponse != null) {
				JSONObject jsonTransitionMatrices = new JSONObject(getResponse);
				PredictionContext result = UtilJsonParser.parsePredictionContext(jsonTransitionMatrices, logger);
				return result;
			}
		} catch (Exception e1) {
			logger.error(e1);;
		}
		return null;
	}

	/*
	public static List<NodeTransitionMatrices> retrieveAllNodeTransitionMatrices(String baseUrl, MatrixFilter matrixFilter) {
		List<NodeTransitionMatrices> result = new ArrayList<>();
		Map<String, Object> params = UtilHttp.generateRequestParams(matrixFilter, format_datetime, logger, UtilHttp.METHOD_POST);
		String response;
		try {
			response = UtilHttp.sendGetRequest(baseUrl + "allNodeTransitionMatrices", params, logger, debugLevel);
			JSONArray jsonListTransitionMatrices = new JSONArray(response);
			for (int i = 0; i < jsonListTransitionMatrices.length(); i++) {
				JSONObject jsonStateHistory = jsonListTransitionMatrices.getJSONObject(i);
				NodeTransitionMatrices tm = UtilJsonParser.parseNodeTransitionMatrices(jsonStateHistory, logger);
				result.add(tm);
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return result;
	}*/

	protected static void enableSupervision(String baseUrl) {
		String getResponse = null;
		try {
			getResponse = UtilHttp.sendGetRequest(baseUrl + "enableSupervision", logger, debugLevel);
			logger.info("enableSupervision : getResponse = " + getResponse);
		} catch (Exception e1) {
			logger.error(e1);
		}
	}

	protected static List<VariableStateHistory> retrieveLastHistoryStates(PredictionScope scope, String baseUrl, Date minDate, String variableName, boolean observationUpdated) {
		List<VariableStateHistory> result = new ArrayList<VariableStateHistory>();
		StateHistoryRequest stateHistoryRequest = new StateHistoryRequest(scope, minDate.getTime(), variableName, observationUpdated);
		//Map<String, Object> params = UtilHttp.generateRequestParams(stateHistoryRequest, format_datetime, logger, UtilHttp.METHOD_POST);
		String getResponse = null;
		try {
			getResponse = UtilHttp.sendPostRequest(baseUrl + "retrieveLastHistoryStates", stateHistoryRequest, logger, debugLevel);
		} catch (Exception e1) {
			logger.error(e1);;
		}
		if(getResponse==null) {
			return result;
		}
		JSONArray jsonStates = new JSONArray(getResponse);
		for (int i = 0; i < jsonStates.length(); i++) {
			JSONObject jsonNodeStateHistory = jsonStates.getJSONObject(i);
			try {
				result.add(UtilJsonParser.parseVariableStateHistory(jsonNodeStateHistory, logger));
			} catch (Exception e) {
				logger.error(e);
			}
		}
		return result;
	}

	protected static Map<String, Double> filterKeys(Map<String, Double> resultToFolter, List<String> listCategoryNames) {
		Map<String, Double> result = new HashMap<>();
		for(String category : resultToFolter.keySet()) {
			if(listCategoryNames.size()==0 || listCategoryNames.contains(category)) {
				result.put(category, resultToFolter.get(category));
			}
		}
		return result;
	}

	protected static Map<DeviceCategory, Double> retrieveDeviceStatistics(
			DeviceCategory[] categories
			, Map<Integer,Integer> datetimeShifts) throws Exception {
		Calendar calendar = Calendar.getInstance();
		List<DeviceCategory> listCategories = new ArrayList<>();
		for(DeviceCategory category : categories) {
			listCategories.add(category);
		}
		int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
		if(datetimeShifts.containsKey(Calendar.HOUR_OF_DAY)) {
			hourOfDay = getCurrentHour();
		}
		Map<DeviceCategory, Double> deviceStatistics = SimulatorDBHelper.retrieveDeviceStatistics(
				statisticPowerCoeffConsumer, statisticPowerCoeffProducer, listCategories, hourOfDay);
		return deviceStatistics;
	}

	protected static DeviceMeasure retrieveLastDevicesMeasure(String featureType, Date dateBegin, Date dateEnd) throws Exception {
		DeviceMeasure deviceMeasure = SimulatorDBHelper.retrieveLastDevicesMeasure(featureType, dateBegin, dateEnd);
		return deviceMeasure;
	}

	protected static Map<Date, List<DeviceMeasure>> retrieveDevicesMeasures(
			DeviceCategory[] categories
			, String featureType
			, Date dateBegin
			, Date dateEnd) throws Exception {
		List<DeviceCategory> listCategories = new ArrayList<>();
		for(DeviceCategory category : categories) {
			listCategories.add(category);
		}
		List<DeviceMeasure> deviceMeasures = SimulatorDBHelper.retrieveDevicesMeasures(listCategories, featureType, dateBegin, dateEnd);
		Map<Date, List<DeviceMeasure>>  result = new TreeMap<Date, List<DeviceMeasure>>();
		for (DeviceMeasure nextMeasure : deviceMeasures) {
			Date date =  nextMeasure.getDatetime();
			if(!result.containsKey(date)) {
				result.put(date, new ArrayList<DeviceMeasure>());
			}
			List<DeviceMeasure> listMeasures = result.get(date);
			listMeasures.add(nextMeasure);
		}
		logger.info("retrieveDevicesMeasures nb dates found = " + result.size());
		return result;
	}

	protected static NodeContent getPartialNodeContent(String baseUrl, DeviceCategory category, boolean isProducer) {
		AgentFilter filter = new AgentFilter();
		String[] consumerDeviceCategories= {};
		String[] producerDeviceCategories = {};
		if(isProducer) {
			producerDeviceCategories = new String[] {category.name()};
		} else {
			consumerDeviceCategories = new String[] {category.name()};
		}
		filter.setConsumerDeviceCategories(consumerDeviceCategories);
		filter.setProducerDeviceCategories(producerDeviceCategories);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("producerDeviceCategories", isProducer ? category : "");
		params.put("consumerDeviceCategories", isProducer ? "" : category );
		String getResponse = UtilHttp.sendGetRequest(baseUrl + "retrieveNodeContent", params, logger, debugLevel);
		if(getResponse==null) {
			return null;
		}
		JSONObject jsonNodeContent = new JSONObject(getResponse);
		if (debugLevel > 2) {
			logger.info("obj : " + jsonNodeContent);
		}
		NodeContent result = UtilJsonParser.parseNodeContent(jsonNodeContent, logger);
		if(result==null) {
			logger.warning("getPartialNodeContent result is null");
		}
		return result;
	}

	protected static NodeContent getAllNodesContent(String baseUrl) {
		Map<String, Object> params = new HashMap<>();
		//params = UtilHttp.generateRequestParams(launchConfig, format_datetime, logger);
		String postResponse = null;
		try {
			postResponse = UtilHttp.sendGetRequest(baseUrl + "retrieveAllNodesContent", params, logger, debugLevel);
			if(postResponse==null) {
				return null;
			}
		} catch (Exception e) {
			logger.error(e);
		}
		JSONObject jsonNodeContent = new JSONObject(postResponse);
		if (debugLevel > 2) {
			logger.info("obj : " + jsonNodeContent);
		}
		NodeContent result = UtilJsonParser.parseNodeContent(jsonNodeContent, logger);
		if(result==null) {
			logger.warning("getAllNodesContent : result is null");
		}
		return result;
	}

	protected static NodeContent getNodeContent(String baseUrl) {
		String postResponse = UtilHttp.sendGetRequest(baseUrl + "retrieveNodeContent", logger, debugLevel);
		if(postResponse==null) {
			return null;
		}
		JSONObject jsonNodeContent = new JSONObject(postResponse);
		if (debugLevel > 2) {
			logger.info("obj : " + jsonNodeContent);
		}
		NodeContent result = UtilJsonParser.parseNodeContent(jsonNodeContent, logger);
		if(result==null) {
			logger.warning("getNodeContent : result is null");
		}
		return result;
	}

	protected static AgentForm addAgent(String baseUrl, AgentInputForm agentInputForm) {
		AgentForm result = new AgentForm();
		try {
			//agentForm.setDeviceCategory(null);
			//Map<String, Object>  params = generateAgentFormParams(agentForm, UtilHttp.METHOD_POST);
			//params.remove("mapPrices");
			//params.remove("_deviceCategory");
			String postResponse =  UtilHttp.sendPostRequest(baseUrl + "addAgent", agentInputForm, logger, debugLevel);
			if (postResponse == null) {
				logger.warning("addAgent : For debug : postResponse is null ");
			}
			JSONObject jsonAgent = new JSONObject(postResponse);
			result = UtilJsonParser.parseAgentForm(jsonAgent, logger);
		} catch (Throwable e) {
			logger.error(e);
		}
		return result;
	}


	protected static void refreshNodeContent(String baseUrl, boolean allNodes) {
		nodeContent = getNodeContent(baseUrl);
		if(nodeContent==null)  {
			logger.warning("refreshNodeContent : nodeContent is null");
			int nbTry=0;
			while(nodeContent==null &&  nbTry<10) {
				if(allNodes) {
					nodeContent = getAllNodesContent(baseUrl);
				} else {
					nodeContent = getNodeContent(baseUrl);
				}
				 nbTry++;
				 try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					logger.error(e);
				}
			}
		} else {
			for(Device device : allDevices) {
				AgentForm agentForm = nodeContent.getAgentByDeviceName(device.getName());
				if(agentForm!=null)  {
					device.setCurrentPower(agentForm.getPower());
				} else {
					device.setCurrentPower(0);
				}
			}
		}
	}

	public static boolean isServiceInitialized(String baseUrl) {
		if(mapIsServiceInitialized.containsKey(baseUrl)) {
			Boolean isInitialized = mapIsServiceInitialized.get(baseUrl);
			return isInitialized;
		}
		return false;
	}

	public static void initEnergyService(String baseUrl, InitializationForm initForm) {
		try {
			if(isServiceInitialized(baseUrl)) {
				logger.error("initEnergyService : service is already initialized");
			}
			//Map<String, Object> params = generateInitializationParams(initForm, UtilHttp.METHOD_POST);
			String postResponse =  UtilHttp.sendPostRequest(baseUrl + "initEnergyService", initForm, logger, debugLevel);
			if(postResponse==null) {
				logger.info("initEnergyService : no post response");
			} else {
				mapIsServiceInitialized.put(baseUrl, Boolean.TRUE);
			}
		} catch (Exception e) {
			logger.error(e);
		}
	}

	protected static boolean stopAgent(String baseUrl, AgentForm agentForm) {
		//String agentName = agentForm.getAgentName();
		//Map<String, Object> params = new HashMap<String, Object>();
		//params.put("agentName", "" + agentName);
		boolean isOK = false;
		try {
			String postResponse =  UtilHttp.sendPostRequest(baseUrl + "stopAgent", agentForm, logger, debugLevel);
			JSONObject jsonAgent = new JSONObject(postResponse);
			AgentForm stopedAgent = UtilJsonParser.parseAgentForm(jsonAgent, logger);
			isOK = stopedAgent.getHasExpired();
		} catch (Exception e) {
			logger.error(e);
		}
		return isOK;
	}


	protected static List<NodeTotal> aggregateNodeHistory(
			 Map <String, PredictionContext> mapPredictionContext
			,Map<String, List<NodeTotal>> historyByNode) {
		// Aggregate au node totals in one single nodeTotal
		Map<Date, Map<String, NodeTotal>> historyByDateAndNode = new TreeMap<Date, Map<String,NodeTotal>>();
		for(String node : mapPredictionContext.keySet()) {
			List<NodeTotal> nodeHistory = historyByNode.get(node);
			for(NodeTotal nextNodeTotal : nodeHistory) {
				Date nextDate = nextNodeTotal.getDate();
				if(!historyByDateAndNode.containsKey(nextDate)) {
					historyByDateAndNode.put(nextDate, new HashMap<String, NodeTotal>());
				}
				Map<String, NodeTotal> dateMap = historyByDateAndNode.get(nextDate);
				dateMap.put(node, nextNodeTotal);
			}
		}
		List<NodeTotal> aggregatedHistory = new ArrayList<NodeTotal>();
		for(Date nextDate : historyByDateAndNode.keySet()) {
			Map<String, NodeTotal> dateMap = historyByDateAndNode.get(nextDate);
			Map<String, IAggregateable> dateMap2 = new HashMap<String, IAggregateable>();
			for(String node : dateMap.keySet()) {
				NodeTotal nodeTotal = dateMap.get(node);
				dateMap2.put(node, nodeTotal);
			}
			NodeTotal aggregatedTotal = NodeTotal.aggregate2(NodeTotal.OP_SUM, dateMap2, null, logger);
			aggregatedHistory.add(aggregatedTotal);
		}
		return aggregatedHistory;
	}
/*
	protected static Map<Integer, NodeTransitionMatrices> initNodeHistory1(
			 String baseUrl
			,PredictionScope predictionScope
			,LearningModelType usedModel
			,List<NodeTotal> listNodeTotal) {
		HistoryInitializationForm historyInitForm = NodeTotal.initHistoryForm(listNodeTotal, predictionScope, usedModel);
		//modelContent.setNodeTransitionMatrices(arrayNodeTransitionMatrices);
		//modelContent.setCompleteMatrices(Boolean.TRUE);
		boolean isOK = false;
		Map<Integer, NodeTransitionMatrices> result = new HashMap<Integer, NodeTransitionMatrices>();
		try {
			String postResponse =  UtilHttp.sendPostRequest(baseUrl + "initNodeHistory1", historyInitForm, logger, debugLevel);
			JSONArray jsonListTransitionMatrices = new JSONArray(postResponse);
			for (int i = 0; i < jsonListTransitionMatrices.length(); i++) {
				JSONObject jsonMarkovStateHistory = jsonListTransitionMatrices.getJSONObject(i);
				NodeTransitionMatrices tm = UtilJsonParser.parseNodeTransitionMatrices(jsonMarkovStateHistory, logger);
				result.put(tm.getTimeWindowId(), tm);
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return result;
	}
*/

	protected static void initNodeHistory2(
			 String baseUrl
			,PredictionContext predictionContext
			) {
		HistoryInitializationRequest historyInitRequest = new HistoryInitializationRequest();
		String node = predictionContext.getNodeLocation().getName();
		LearningModelType usedModel = predictionContext.getModelType();
		PredictionScope predictionScope = predictionContext.getScope();
		historyInitRequest.setScope(predictionScope.toOptionItem());
		historyInitRequest.setUsedModel(usedModel);
		historyInitRequest.setNodeName(node);
		historyInitRequest.setCompleteMatrices(Boolean.TRUE);
		boolean isOK = false;
		//Map<Integer, NodeTransitionMatrices> result = new HashMap<Integer, NodeTransitionMatrices>();
		try {
			String postResponse =  UtilHttp.sendPostRequest(baseUrl + "initNodeHistory2", historyInitRequest, logger, debugLevel);
			if(postResponse != null) {
				logger.info("initNodeHistory2 : postResponse length = " + postResponse.length());
			}
			/*
			JSONArray jsonListTransitionMatrices = new JSONArray(postResponse);
			for (int i = 0; i < jsonListTransitionMatrices.length(); i++) {
				JSONObject jsonMarkovStateHistory = jsonListTransitionMatrices.getJSONObject(i);
				NodeTransitionMatrices tm = UtilJsonParser.parseNodeTransitionMatrices(jsonMarkovStateHistory, logger);
				result.put(tm.getTimeWindowId(), tm);
			}*/
		} catch (Exception e) {
			logger.error(e);
		}
		//return result;
	}



	protected static boolean stopListAgents(String baseUrl, List<String> listAgentName) {
		String agentNames = String.join(",", listAgentName);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("agentName", "" + agentNames);
		AgentInputForm agentInputForm = new AgentInputForm();
		agentInputForm.setAgentName(agentNames);
		agentInputForm.setProsumerRole(null);
		agentInputForm.setBeginDate(new Date());
		agentInputForm.setEndDate(new Date());
		//StringBuffer testBuff = UtilJsonParser.toJsonStr(agentInputForm, logger, 0);
		boolean isOk = false;
		for(int callIdx =0; (callIdx<15 && !isOk); callIdx++) {
			try {
				String postResponse = UtilHttp.sendPostRequest(baseUrl + "stopListAgents", agentInputForm, logger, debugLevel);
				JSONObject jsonResult = new JSONObject(postResponse);
				OperationResult resultStop = UtilJsonParser.parseOperationResult(jsonResult, logger);
				isOk = resultStop.getIsSuccessful();
			} catch (Exception e) {
				logger.error(e);
			}
		}
		if(!isOk) {
			logger.error("stopListAgents " + listAgentName + ": stop failed");
		}
		//refreshNodeContent();
		return isOk;
	}

	public static boolean stopEnergyService(String baseUrl) {
		try {
			boolean isOk = false;
			for(int callIdx =0; (callIdx<15 && !isOk); callIdx++) {
				//logger.info("call stopEnergyService idx = " +callIdx);
				String getResponse =  UtilHttp.sendGetRequest(baseUrl + "stopAllAgents", logger, debugLevel);
				if(getResponse==null) {
					logger.info("stopEnergyService : no post response");
				} else {
					JSONObject jsonAgent = new JSONObject(getResponse);
					OperationResult result = UtilJsonParser.parseOperationResult(jsonAgent, logger);
					isOk = result.getIsSuccessful();
					logger.info("call stopEnergyService result : isOk = " + isOk);
					if(!isOk) {
						Thread.sleep(2000);
					}
				}
			}
			if(isOk) {
				String getResponse2 =  UtilHttp.sendGetRequest(baseUrl + "stopEnergyService", logger, debugLevel);
				logger.info("stopEnergyService : getResponse2 = " + getResponse2);
				mapIsServiceInitialized.remove(baseUrl);
			} else {
				logger.error("stopEnergyService : stop failed");
			}
			return isOk;
		} catch (Exception e) {
			logger.error(e);
		}
		return false;
	}

	protected static AgentForm modifyAgent(String baseUrl, AgentInputForm agentInputForm) {
		try {
			//Map<String, Object> params = generateAgentFormParams(agentInputForm, UtilHttp.METHOD_POST);
			String postResponse =  UtilHttp.sendPostRequest(baseUrl + "modifyAgent", agentInputForm, logger, debugLevel);
			logger.info("modifyAgent : " + agentInputForm.getAgentName() + " , power = " + agentInputForm.getPower());
			JSONObject jsonAgent = new JSONObject(postResponse);
			AgentForm result = UtilJsonParser.parseAgentForm(jsonAgent, logger);
			Thread.sleep(1000);
			double powerToSet = agentInputForm.getPower();
			int nbWaits = 0;
			while((Math.abs(result.getPower() - powerToSet) > 0.001) && nbWaits<3) {
				Thread.sleep(1000);
				NodeContent partialContent = getPartialNodeContent(baseUrl, agentInputForm.getDeviceCategory(), agentInputForm.isProducer());
				result = partialContent.getAgent(agentInputForm.getAgentName());
				nbWaits++;
			}
			/*
			result  = nodeContent.getAgent(agentForm.getAgentName());
			*/
			return result;
		} catch (Throwable e) {
			logger.error(e);
		}
		return null;
	}

	protected static AgentForm restartAgent(String baseUrl, AgentInputForm agentForm) {
		try {
			//Map<String, Object> params = generateAgentFormParams(agentForm, UtilHttp.METHOD_POST);
			//Thread.sleep(5000);
			String postResponse =  UtilHttp.sendPostRequest(baseUrl + "restartAgent", agentForm, logger, debugLevel);
			logger.info("restartAgent : " + agentForm.getAgentName() + " , power = " + agentForm.getPower() + ", endDate = " + agentForm.getEndDate());
			JSONObject jsonAgent = new JSONObject(postResponse);
			AgentForm result = UtilJsonParser.parseAgentForm(jsonAgent, logger);
			return result;
		} catch (Throwable e) {
			logger.error(e);
		}
		return null;
	}

	protected static void addSimulatorLog(SimulatorLog simulatorLog) throws Exception {
		SimulatorDBHelper.registerSimulatorLog(simulatorLog);
	}

	protected static void resetSimulatorLogs() {
		try {
			SimulatorDBHelper.resetSimulatorLogs();
		} catch (Exception e) {
			logger.error(e);
		}
	}

	protected static String quote(String str) {
		return QUOTE + str + QUOTE;
	}

	protected static Date getCurrentDate() {
		return UtilDates.getNewDate(datetimeShifts);
	}

	protected static Date getCurrentDateBidon() {
		return UtilDates.getNewDate(new HashMap<>());
	}

	protected static LaunchConfig laodLaunchConfig(String jsonFilePath) {
		LaunchConfig result = null;
		try {
			String lineSep = System.getProperty("line.separator");
			InputStream is = new FileInputStream(new File(jsonFilePath));
			String jsonTxt = new BufferedReader(
				      new InputStreamReader(is, StandardCharsets.UTF_8))
				        .lines()
				        .collect(Collectors.joining(lineSep));
			System.out.println(jsonTxt);
			JSONObject jsonLaunchConfig = new JSONObject(jsonTxt);
			result = UtilJsonParser.parseLaunchConfig(jsonLaunchConfig, logger);
		} catch (FileNotFoundException e) {
			logger.error(e);
		} catch (JSONException e) {
			logger.error(e);
		} catch (Exception e) {
			logger.error(e);
		}
		return result;
	}

	public static NodeLocation retrieveNodeLocation(String url) {
		NodeLocation nodeLocation = new NodeLocation();
		//nodeLocation.setUrl(baseUrl); // ,"url":"http://localhost:9191/energy/"
		Pattern pattern = Pattern.compile("^(https?|http)://(?<host>[0-9a-zA-Z_.]+):(?<port>[0-9]+)/energy/");
		Matcher matcher = pattern.matcher(url);
		if(matcher.matches()) {
			String host = matcher.group("host");
			String port = matcher.group("port");
			nodeLocation.setHost(host);
			nodeLocation.setRestPort(Integer.valueOf(port));
		}
		return nodeLocation;
	}
}
