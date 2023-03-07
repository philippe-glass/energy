package com.sapereapi.energy.test;

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

import org.json.JSONArray;
import org.json.JSONObject;

import com.sapereapi.log.SimulatorLogger;
import com.sapereapi.model.InitializationForm;
import com.sapereapi.model.NodeConfig;
import com.sapereapi.model.NodesAddresses;
import com.sapereapi.model.OperationResult;
import com.sapereapi.model.TimeSlotFilter;
import com.sapereapi.model.energy.AgentFilter;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.Device;
import com.sapereapi.model.energy.DeviceMeasure;
import com.sapereapi.model.energy.EnergyEvent;
import com.sapereapi.model.energy.NodeContent;
import com.sapereapi.model.energy.SimulatorLog;
import com.sapereapi.model.markov.MarkovStateHistory;
import com.sapereapi.model.markov.NodeTransitionMatrices;
import com.sapereapi.model.prediction.MatrixFilter;
import com.sapereapi.model.prediction.StateHistoryRequest;
import com.sapereapi.model.referential.AgentType;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EventType;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;
import com.sapereapi.util.UtilHttp;
import com.sapereapi.util.UtilJsonParser;

public class TestSimulator {
	protected static String baseUrl = "http://localhost:9090/energy/";
	protected static SimpleDateFormat format_datetime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	public final static String QUOTE = "\"";
	protected static int debugLevel = 0;
	protected static SimulatorLogger logger = SimulatorLogger.getInstance();
	protected static List<Device> allDevices = new ArrayList<Device>();
	protected static NodeContent nodeContent = null;
	protected static boolean isServiceInitialized = false;
	static Random random = new Random();
	protected static Map<Integer,Integer> datetimeShifts = new HashMap<Integer, Integer>();


	protected static void init(String args[]) {
		String baseUrlTag = "-baseUrl:";
		for(String arg : args) {
			if(arg.startsWith(baseUrlTag)) {
				int len = baseUrlTag.length();
				String content = arg.substring(len);
				baseUrl = content;
				// -baseUrl:http://localhost:9191/energy/
			}
		}
		allDevices = getNodeDevices();
		nodeContent = getNodeContent();
		datetimeShifts = new HashMap<Integer, Integer>();
	}

	protected static int getCurrentHour() {
		Calendar aCalendar = Calendar.getInstance();
		if(datetimeShifts.containsKey(Calendar.HOUR_OF_DAY)) {
			aCalendar.add(Calendar.HOUR_OF_DAY, datetimeShifts.get(Calendar.HOUR_OF_DAY));
		}
		return aCalendar.get(Calendar.HOUR_OF_DAY);
	}

	protected static Device chooseSleepingDevice(NodeContent partialNodeContent, boolean isProducer, String category, double powerMin,
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


	protected static List<Device> chooseListDevices(NodeContent partialNodeContent, boolean isProducer, String category, String deviceStatus, double powerTarget, List<String> excludedDevices) {
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

	protected static AgentForm chooseRunningAgent(NodeContent partialContent, boolean isProducer, String deviceCategory, double minPowerfilter,
			double maxPowerFilter, List<String> deviceNamesToExclude ) {
		Map<String, AgentForm> mapRunningAgents = partialContent.getMapRunningAgents();
		AgentType agentTypeFilter = isProducer ? AgentType.PRODUCER : AgentType.CONSUMER;
		if (mapRunningAgents.size() > 0) {
			List<AgentForm> runningAgents = new ArrayList<AgentForm>();
			for (AgentForm agentForm : mapRunningAgents.values()) {
				if (agentForm.getPower()> 0) {
					if (agentTypeFilter.getLabel().equals(agentForm.getAgentType())) {
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

	protected static List<Device> getDevices(NodeContent partialNodeContent, boolean isProducer, String sDeviceCategory, String statusFilter,
			double minPowerfilter, double maxPowerFilter, List<String> excludedDevices) {
		List<Device> result = new ArrayList<Device>();
		Map<String, AgentForm> mapRunningAgents = partialNodeContent.getMapRunningAgents();
		DeviceCategory filterDeviceCateogry = DeviceCategory.getByName(sDeviceCategory);
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

	protected static boolean hasDevice(NodeContent nodeContent, boolean isProducer, String category, String statusFilter, double minPowerFilter,
			double maxPowerFilter, List<String> excludedDevices) {
		List<Device> devices = getDevices(nodeContent, isProducer, category, statusFilter, minPowerFilter, maxPowerFilter, excludedDevices);
		return !devices.isEmpty();
	}

	protected static void executeScenario(List<EnergyEvent> scenario) {
		List<EnergyEvent> eventQueue = new ArrayList<EnergyEvent>();
		for (EnergyEvent event : scenario) {
			eventQueue.add(event);
		}
		Date current = getCurrentDate();
		while (eventQueue.size() > 0) {
			List<EnergyEvent> toRemove = new ArrayList<EnergyEvent>();
			for (EnergyEvent event : eventQueue) {
				AgentForm agentForm = new AgentForm();
				agentForm.setPowers(event.getPower(), event.getPowerMin(), event.getPowerMax());
				agentForm.setBeginDate(event.getBeginDate());
				agentForm.setEndDate(event.getEndDate());
				agentForm.setDuration(event.getDuration());
				agentForm.setDeviceName(event.getDeviceProperties().getName());
				agentForm.setDeviceCategory(event.getDeviceProperties().getCategory().getOptionItem());
				if (EventType.PRODUCTION_START.equals(event.getType())) {
					agentForm.setAgentType(AgentType.PRODUCER.getLabel());
				} else if (EventType.REQUEST_START.equals(event.getType())) {
					agentForm.setAgentType(AgentType.CONSUMER.getLabel());
					double tolerance = UtilDates.computeDurationMinutes(event.getBeginDate(), event.getEndDate());
					agentForm.setDelayToleranceMinutes(tolerance);
					agentForm.setPriorityLevel(PriorityLevel.LOW.getLabel());
				}
				if (event.getBeginDate().before(current)) {
					addAgent(agentForm);
					toRemove.add(event);
				}
			}
			for (EnergyEvent event : toRemove) {
				eventQueue.remove(event);
			}
		}
	}

	protected static NodeContent restartLastNodeContent() {
		String postResponse = UtilHttp.sendGetRequest(baseUrl + "restartLastNodeContent", logger, debugLevel);
		JSONObject jsonNodeContent = new JSONObject(postResponse);
		if (debugLevel > 2) {
			logger.info("obj : " + jsonNodeContent);
		}
		NodeContent result = UtilJsonParser.parseNodeContent(jsonNodeContent);
		return result;
	}

	protected static List<Device> getMeyrinDevices() {
		String postResponse = UtilHttp.sendGetRequest(baseUrl + "retrieveMeyrinDevices", logger, debugLevel);
		List<Device> result = new ArrayList<Device>();
		if(postResponse==null) {
			return result;
		}
		JSONArray jsonNodeDevices = new JSONArray(postResponse);
		if (debugLevel > 2) {
			logger.info("obj : " + jsonNodeDevices);
		}
		for (int i = 0; i < jsonNodeDevices.length(); i++) {
			JSONObject jsonDevice= jsonNodeDevices.getJSONObject(i);
			try {
				result.add(UtilJsonParser.parseDevice(jsonDevice));
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e);
			}
		}
		return result;
	}

	protected static List<Device> getNodeDevices() {
		String getResponse = UtilHttp.sendGetRequest(baseUrl + "getNodeDevices", logger, debugLevel);
		List<Device> result = new ArrayList<Device>();
		if(getResponse==null) {
			return result;
		}
		JSONArray jsonNodeDevices = new JSONArray(getResponse);
		if (debugLevel > 2) {
			logger.info("obj : " + jsonNodeDevices);
		}
		for (int i = 0; i < jsonNodeDevices.length(); i++) {
			JSONObject jsonDevice= jsonNodeDevices.getJSONObject(i);
			try {
				result.add(UtilJsonParser.parseDevice(jsonDevice));
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e);
			}
		}
		return result;
	}

	protected static NodeTransitionMatrices getCurrentNodeTransitionMatrices() {
		try {
			String getResponse = UtilHttp.sendGetRequest(baseUrl + "getCurrentNodeTransitionMatrices", logger, debugLevel);
			JSONObject jsonTransitionMatrices = new JSONObject(getResponse);
			NodeTransitionMatrices result = UtilJsonParser.parseNodeTransitionMatrices(jsonTransitionMatrices);
			return result;
		} catch (Exception e1) {
			logger.error(e1);;
		}
		return null;
	}

	public static List<NodeTransitionMatrices> retrieveAllNodeTransitionMatrices(MatrixFilter matrixFilter) {
		List<NodeTransitionMatrices> result = new ArrayList<>();
		Map<String, Object> params = UtilHttp.generateRequestParams(matrixFilter, format_datetime, logger);
		String response;
		try {
			response = UtilHttp.sendPostRequest(baseUrl + "allNodeTransitionMatrices", params, logger, debugLevel);
			JSONArray jsonListTransitionMatrices = new JSONArray(response);
			for (int i = 0; i < jsonListTransitionMatrices.length(); i++) {
				JSONObject jsonMarkovStateHistory = jsonListTransitionMatrices.getJSONObject(i);
				NodeTransitionMatrices tm = UtilJsonParser.parseNodeTransitionMatrices(jsonMarkovStateHistory);
				result.add(tm);
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return result;
	}

	protected static void enableSupervision() {
		String getResponse = null;
		try {
			getResponse = UtilHttp.sendGetRequest(baseUrl + "enableSupervision", logger, debugLevel);
			logger.info("enableSupervision : getResponse = " + getResponse);
		} catch (Exception e1) {
			logger.error(e1);;
		}
	}

	protected static List<MarkovStateHistory> retrieveLastMarkovHistoryStates(Date minDate, String variableName, boolean observationUpdated) {
		List<MarkovStateHistory> result = new ArrayList<MarkovStateHistory>();
		StateHistoryRequest stateHistoryRequest = new StateHistoryRequest(minDate.getTime(), variableName, observationUpdated);
		Map<String, Object> params = UtilHttp.generateRequestParams(stateHistoryRequest, format_datetime, logger);
		String getResponse = null;
		try {
			getResponse = UtilHttp.sendPostRequest(baseUrl + "retrieveLastMarkovHistoryStates", params, logger, debugLevel);
		} catch (Exception e1) {
			logger.error(e1);;
		}
		if(getResponse==null) {
			return result;
		}
		JSONArray jsonStates = new JSONArray(getResponse);
		for (int i = 0; i < jsonStates.length(); i++) {
			JSONObject jsonMarkovStateHistory = jsonStates.getJSONObject(i);
			try {
				result.add(UtilJsonParser.parseMarkovStateHistory(jsonMarkovStateHistory));
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e);
			}
		}
		return result;
	}

	private static Map<String, Double> filterKeys(Map<String, Double> resultToFolter, List<String> listCategoryNames) {
		Map<String, Double> result = new HashMap<>();
		for(String category : resultToFolter.keySet()) {
			if(listCategoryNames.size()==0 || listCategoryNames.contains(category)) {
				result.put(category, resultToFolter.get(category));
			}
		}
		return result;
	}
	protected static Map<String, Double> retrieveDeviceStatistics(DeviceCategory[] categories, Map<Integer,Integer> datetimeShifts) {
		List<String> listCategoryNames = new ArrayList<String>();
		for(DeviceCategory deviceCategory : categories) {
			listCategoryNames.add(deviceCategory.name());
		}
		Map<String, String> params = new HashMap<String, String>();
		if(datetimeShifts.containsKey(Calendar.HOUR_OF_DAY)) {
			int hourOfDay = getCurrentHour();
			params.put("hourOfDay", ""+hourOfDay);
		}
		String postResponse = UtilHttp.sendGetRequest(baseUrl + "retrieveDeviceStatistics", logger, debugLevel);
		if(postResponse==null) {
			return new HashMap<String, Double>();
		}
		JSONObject jsonMap = new JSONObject(postResponse);
		try {
			Map<String, Double> resultToFolter = UtilJsonParser.parseJsonMap2(jsonMap);
			Map<String, Double> result = filterKeys(resultToFolter, listCategoryNames);
			return result;
		} catch (Throwable e) {
			e.printStackTrace();
			logger.error(e);
		}
		return new HashMap<String, Double>();
	}

	protected static DeviceMeasure retrieveLastDevicesMeasure(String featureType, Date dateBegin, Date dateEnd) {
		TimeSlotFilter timeSlotFilter = new TimeSlotFilter();
		timeSlotFilter.setFeatureType(featureType);
		if(dateBegin != null) {
			timeSlotFilter.setLongDateBegin(dateBegin.getTime());
		}
		if(dateEnd != null) {
			timeSlotFilter.setLongDateEnd(dateEnd.getTime());
		}
		Map<String, Object> params = UtilHttp.generateRequestParams(timeSlotFilter, format_datetime, logger);
		String response = UtilHttp.sendGetRequest(baseUrl + "retrieveLastDevicesMeasure",params,  logger, debugLevel);
		if(response==null) {
			return null;
		}
		try {
			JSONObject jsonDeviceMeasure = new JSONObject(response);
			return UtilJsonParser.parseDeviceMeasure(jsonDeviceMeasure);
		} catch (Exception e) {
			logger.error(e);
		}
		return null;
	}

	protected static Map<Date, List<DeviceMeasure>> retrieveDevicesMeasures(DeviceCategory[] categories, String featureType, Date dateBegin, Date dateEnd) {
		Map<Date, List<DeviceMeasure>>  result = new TreeMap<Date, List<DeviceMeasure>>();
		List<String> listCategoryNames = new ArrayList<String>();
		for(DeviceCategory deviceCategory : categories) {
			listCategoryNames.add(deviceCategory.name());
		}
		Map<String, Object> params = new HashMap<String, Object>();
		if(dateBegin!=null) {
			//params.put("dateBegin", ""+SapereUtil.format_date_time.format(dateBegin));
			//params.put("dateBegin", ""+SapereUtil.format_sql.format(dateBegin));
			//params.put("dateBegin", "" + "2022-05-30 10:00:00");
			params.put("dateBegin",format_datetime.format(dateBegin));
			params.put("dateBegin","" + dateBegin.getTime());
			params.put("longDateBegin","" + "" + dateBegin.getTime());
			params.put("featureType", featureType);
		}
		if(dateEnd!=null) {
			params.put("longDateEnd","" + "" + dateEnd.getTime());
		}
		String postResponse = UtilHttp.sendGetRequest(baseUrl + "retrieveDevicesMeasures", params, logger, debugLevel);
		if(postResponse==null) {
			return new HashMap<Date, List<DeviceMeasure>>();
		}
		try {
			JSONArray json_listMeasures = new JSONArray(postResponse);
			for (int i = 0; i < json_listMeasures.length(); i++) {
				JSONObject jsonMeasure = json_listMeasures.getJSONObject(i);
				try {
					DeviceMeasure nextMeasure = UtilJsonParser.parseDeviceMeasure(jsonMeasure);
					Date date =  nextMeasure.getDatetime();
					if(!result.containsKey(date)) {
						result.put(date, new ArrayList<DeviceMeasure>());
					}
					List<DeviceMeasure> listMeasures = result.get(date);
					listMeasures.add(nextMeasure);
				} catch (Exception e) {
					e.printStackTrace();
					logger.error(e);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
			logger.error(e);
		}
		logger.info("retrieveDevicesMeasures nb dates found = " + result.size());
		return result;
	}

	protected static NodeContent getPartialNodeContent(String category, boolean isProducer) {
		AgentFilter filter = new AgentFilter();
		String[] consumerDeviceCategories= {};
		String[] producerDeviceCategories = {};
		if(isProducer) {
			producerDeviceCategories = new String[] {category};
		} else {
			consumerDeviceCategories = new String[] {category};
		}
		filter.setConsumerDeviceCategories(consumerDeviceCategories);
		filter.setProducerDeviceCategories(producerDeviceCategories);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("producerDeviceCategories", isProducer ? category : "");
		params.put("consumerDeviceCategories", isProducer ? "" : category );
		String postResponse = UtilHttp.sendGetRequest(baseUrl + "retrieveNodeContent", params, logger, debugLevel);
		if(postResponse==null) {
			return null;
		}
		JSONObject jsonNodeContent = new JSONObject(postResponse);
		if (debugLevel > 2) {
			logger.info("obj : " + jsonNodeContent);
		}
		NodeContent result = UtilJsonParser.parseNodeContent(jsonNodeContent);
		if(result==null) {
			logger.warning("getPartialNodeContent result is null");
		}
		return result;
	}

	protected static NodeContent getAllNodesContent(Map<String, NodeConfig> mapNodes) {
		NodesAddresses nodesAddresses = new NodesAddresses();
		for(NodeConfig nextNode : mapNodes.values()) {
			nodesAddresses.addNodeConfig(nextNode);
		}
		//Map<String, String> params = UtilHttp.generateRequestParams(nodesAddresses, format_datetime, logger);
		Map<String, Object> params = new HashMap<>();
		String sListNodeBaseUrl = String.join(",", nodesAddresses.getListNodeBaseUrl()) ; //+ "" + nodesAddresses.getListNodeBaseUrl();
		params.put("listNodeBaseUrl", sListNodeBaseUrl);
		baseUrl = "http://localhost:9191/energy/";
		String postResponse = UtilHttp.sendGetRequest(baseUrl + "allNodesContent", params, logger, debugLevel);
		if(postResponse==null) {
			return null;
		}
		JSONObject jsonNodeContent = new JSONObject(postResponse);
		if (debugLevel > 2) {
			logger.info("obj : " + jsonNodeContent);
		}
		NodeContent result = UtilJsonParser.parseNodeContent(jsonNodeContent);
		if(result==null) {
			logger.warning("getAllNodesContent : result is null");
		}
		return result;
	}

	protected static NodeContent getNodeContent() {
		String postResponse = UtilHttp.sendGetRequest(baseUrl + "retrieveNodeContent", logger, debugLevel);
		if(postResponse==null) {
			return null;
		}
		JSONObject jsonNodeContent = new JSONObject(postResponse);
		if (debugLevel > 2) {
			logger.info("obj : " + jsonNodeContent);
		}
		NodeContent result = UtilJsonParser.parseNodeContent(jsonNodeContent);
		if(result==null) {
			logger.warning("getNodeContent : result is null");
		}
		return result;
	}

	protected static AgentForm addAgent(AgentForm agentForm) {
		try {
			//agentForm.setDeviceCategory(null);
			Map<String, Object>  params = generateAgentFormParams(agentForm);
			params.remove("mapPrices");
			params.remove("_deviceCategory");
			String postResponse =  UtilHttp.sendPostRequest(baseUrl + "addAgent", params, logger, debugLevel);
			if (postResponse == null) {
				logger.warning("addAgent : For debug : postResponse is null ");
			}
			JSONObject jsonAgent = new JSONObject(postResponse);
			agentForm = UtilJsonParser.parseAgentForm(jsonAgent);
		} catch (Throwable e) {
			e.printStackTrace();
			logger.error(e);
		}
		return agentForm;
	}

	protected static void refreshNodeContent() {
		nodeContent = getNodeContent();
		if(nodeContent==null)  {
			logger.warning("refreshNodeContent : nodeContent is null");
			int nbTry=0;
			while(nodeContent==null &&  nbTry<10) {
				nodeContent = getNodeContent();
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

	public static Map<String, Object> generateInitializationParams(InitializationForm initForm) {
		Map<String, Object> params1 = UtilHttp.generateRequestParams(initForm, format_datetime, logger);
		return params1;
	}

	public static Map<String, Object> generateAgentFormParams(AgentForm agentForm) {
		if(!SapereUtil.checkIsRound(agentForm.getPower(), 2, logger)) {
			logger.warning("generateAgentFormParams for debug");
		}
		// return params;
		Map<String, Object> params1 = UtilHttp.generateRequestParams(agentForm, format_datetime, logger);
		params1.remove("pricingTable");
		// logger.info("generateParams : agentName = " + agentForm.getAgentName() + ",
		return params1;
	}



	public static Map<String, Object> generateSimulatorLogParams(SimulatorLog simulatorLog) {
		Map<String, Object> params1 = UtilHttp.generateRequestParams(simulatorLog, format_datetime, logger);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("loopNumber", "" + simulatorLog.getLoopNumber());
		params.put("deviceCategoryCode", "" + simulatorLog.getDeviceCategoryCode());
		params.put("powerTarget", "" + simulatorLog.getPowerTarget());
		params.put("powerTargetMin", ""+simulatorLog.getPowerTargetMin());
		params.put("powerTargetMax", ""+simulatorLog.getPowerTargetMax());
		params.put("power", "" + simulatorLog.getPower());
		params.put("reached", ""+simulatorLog.isReached());
		params.put("nbStarted", ""+simulatorLog.getNbStarted());
		params.put("nbModified", ""+simulatorLog.getNbModified());
		params.put("nbStopped", ""+simulatorLog.getNbStopped());
		params.put("nbDevices", ""+simulatorLog.getNbDevices());
		params.put("targetDeviceCombinationFound", ""+simulatorLog.isTargetDeviceCombinationFound());
		params.put("class", ""+simulatorLog.getClass());
		// logger.info("generateParams : agentName = " + agentForm.getAgentName() + ",
		// originEndDate = " + agentForm.getEndDate() + " , result endDate = " +
		// params.get("endDate") + " timezone = " + format_datetime.getTimeZone());
		// {,  ,  , , ,   , , class=class com.energy.model.SimulatorLog}
		boolean test = params1.equals(params);
		if(!test) {
			logger.error("generateSimulatorLogParams Not equals");
		}
		return params1;
	}


	public static void initEnergyService(InitializationForm initForm ) {
		try {
			if(isServiceInitialized) {
				logger.error("initEnergyService : service is already initialized");
			}
			Map<String, Object> params = generateInitializationParams(initForm);
			String postResponse =  UtilHttp.sendPostRequest(baseUrl + "initEnergyService", params, logger, debugLevel);
			if(postResponse==null) {
				logger.info("initEnergyService : no post response");
			} else {
				isServiceInitialized = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e);
		}
	}

	protected static boolean stopListAgents(List<String> listAgentName) {
		String agentNames = String.join(",", listAgentName);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("agentName", "" + agentNames);
		boolean isOk = false;
		for(int callIdx =0; (callIdx<15 && !isOk); callIdx++) {
			try {
				String postResponse =  UtilHttp.sendPostRequest(baseUrl + "stopListAgents", params, logger, debugLevel);
				JSONObject jsonResult = new JSONObject(postResponse);
				OperationResult resultStop = UtilJsonParser.parseOperationResult(jsonResult);
				isOk = resultStop.getIsSuccessful();
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e);
			}
		}
		if(!isOk) {
			logger.error("stopListAgents " + listAgentName + ": stop failed");
		}
		//refreshNodeContent();
		return isOk;
	}

	public static boolean stopEnergyService( ) {
		try {
			boolean isOk = false;
			for(int callIdx =0; (callIdx<15 && !isOk); callIdx++) {
				//logger.info("call stopEnergyService idx = " +callIdx);
				String getResponse =  UtilHttp.sendGetRequest(baseUrl + "stopAllAgents", logger, debugLevel);
				if(getResponse==null) {
					logger.info("stopEnergyService : no post response");
				} else {
					JSONObject jsonAgent = new JSONObject(getResponse);
					OperationResult result = UtilJsonParser.parseOperationResult(jsonAgent);
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
				isServiceInitialized = false;
			} else {
				logger.error("stopEnergyService : stop failed");
			}
			return isOk;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e);
		}
		return false;
	}

	protected static AgentForm modifyAgent(AgentForm agentForm) {
		try {
			Map<String, Object> params = generateAgentFormParams(agentForm);
			//Thread.sleep(5000);
			String postResponse =  UtilHttp.sendPostRequest(baseUrl + "modifyAgent", params, logger, debugLevel);
			logger.info("modifyAgent : " + agentForm.getAgentName() + " , power = " + params.get("power"));
			JSONObject jsonAgent = new JSONObject(postResponse);
			AgentForm result = UtilJsonParser.parseAgentForm(jsonAgent);
			Thread.sleep(1000);
			double powerToSet = agentForm.getPower();
			int nbWaits = 0;
			while((Math.abs(result.getPower() - powerToSet) > 0.001) && nbWaits<3) {
				Thread.sleep(1000);
				NodeContent partialContent = getPartialNodeContent(agentForm.getDeviceCategory().getValue(), agentForm.isProducer());
				result = partialContent.getAgent(agentForm.getAgentName());
				nbWaits++;
			}
			/*
			result  = nodeContent.getAgent(agentForm.getAgentName());
			*/
			return result;
		} catch (Throwable e) {
			e.printStackTrace();
			logger.error(e);
		}
		return null;
	}

	protected static AgentForm restartAgent(AgentForm agentForm) {
		try {
			Map<String, Object> params = generateAgentFormParams(agentForm);
			//Thread.sleep(5000);
			String postResponse =  UtilHttp.sendPostRequest(baseUrl + "restartAgent", params, logger, debugLevel);
			logger.info("restartAgent : " + agentForm.getAgentName() + " , endDate = " + params.get("endDate"));
			JSONObject jsonAgent = new JSONObject(postResponse);
			AgentForm result = UtilJsonParser.parseAgentForm(jsonAgent);
			return result;
		} catch (Throwable e) {
			e.printStackTrace();
			logger.error(e);
		}
		return null;
	}

	protected static boolean stopAgent(AgentForm agentForm) {
		String agentName = agentForm.getAgentName();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("agentName", "" + agentName);
		boolean isOK = false;
		try {
			String postResponse =  UtilHttp.sendPostRequest(baseUrl + "stopAgent", params, logger, debugLevel);
			JSONObject jsonAgent = new JSONObject(postResponse);
			AgentForm stopedAgent = UtilJsonParser.parseAgentForm(jsonAgent);
			isOK = stopedAgent.getHasExpired();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e);
		}
		return isOK;
	}


	protected static void addSimulatorLog(SimulatorLog simulatorLog) {
		Map<String, Object> params = generateSimulatorLogParams(simulatorLog);
		try {
			String postResponse =  UtilHttp.sendPostRequest(baseUrl + "addSimulatorLog", params, logger, debugLevel);
			if(postResponse==null) {
				logger.info("addSimulatorLog : no post response");
			}
		} catch (Exception e) {
			logger.error(e);
		}
	}

	protected static void resetSimulatorLogs() {
		try {
			Map<String, Object> params = new HashMap<String, Object>();
			String postResponse =  UtilHttp.sendPostRequest(baseUrl + "resetSimulatorLogs", params, logger, debugLevel);
			if(postResponse==null) {
				logger.info("resetSimulatorLogs : no post response");
			}
		} catch (Exception e) {
			logger.error(e);
		}
	}
/*
	protected static String sendGetRequest(String url) {
		 Map<String, String> params = new HashMap<String, String>();
		 return UtilHttp.sendGetRequest(url, params, logger, debugLevel);
	}
*/
	protected static String quote(String str) {
		return QUOTE + str + QUOTE;
	}

	protected static Date getCurrentDate() {
		return UtilDates.getNewDate(datetimeShifts);
	}

	protected static Date getCurrentDateBidon() {
		return UtilDates.getNewDate(new HashMap<>());
	}

/*
	protected static String  sendPostRequest(String sUrl, Map<String, String> params) throws Exception {
		return UtilHttp.sendPostRequest(sUrl, params, logger, debugLevel);
	}
*/
}
