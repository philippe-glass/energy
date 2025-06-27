package com.saperetest;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.sapereapi.exception.MissingMeasureException;
import com.sapereapi.model.EnergyStorageSetting;
import com.sapereapi.model.LaunchConfig;
import com.sapereapi.model.PredictionSetting;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.Device;
import com.sapereapi.model.energy.DeviceMeasure;
import com.sapereapi.model.energy.StorageType;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.energy.input.AgentInputForm;
import com.sapereapi.model.energy.input.SimulatorLog;
import com.sapereapi.model.energy.node.NodeContent;
import com.sapereapi.model.energy.node.NodeTotal;
import com.sapereapi.model.input.InitializationForm;
import com.sapereapi.model.learning.ILearningModel;
import com.sapereapi.model.learning.LearningModelType;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.learning.PredictionScope;
import com.sapereapi.model.learning.VariableStateHistory;
import com.sapereapi.model.learning.prediction.LearningAggregationOperator;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.model.learning.prediction.PredictionData;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.node.NodeLocation;

public class LivingLabSimulator extends TestSimulator {

	static Map<DeviceCategory, Double> deviceStatistics = new HashMap<DeviceCategory, Double>();
	static Map<Date, List<DeviceMeasure>> deviceMeasures = new HashMap<Date, List<DeviceMeasure>>();
	static int loopCounter = 0;
	final static String CR = System.getProperty("line.separator");
	static SimulatorLog simulatorLog = null;
	static DeviceCategory[] producersCategoryFilter = new DeviceCategory[] {DeviceCategory.EXTERNAL_ENG};
			// new DeviceCategory[] {DeviceCategory.EXTERNAL_ENG, DeviceCategory.SOLOR_ENG};
	static Integer staticticHour = null;
	//static Integer dayShift = 0;
	//static Integer dayShiftProducer = 0;
	static Date dateCurrent = null;
	static Date dateCurrentProducer = null;
	static double maxTotalPower = 10*NodeStates.DEFAULT_NODE_MAX_TOTAL_POWER;
	static boolean testMultiNodes = false;
	static LaunchConfig launchConfig = null;
	static Map<String, NodeLocation> mapNodes = new HashMap<String, NodeLocation>();
	static Map<String, String> mapBaseUrl = new HashMap<String, String>();
	static List<String> listBaseUrl = new ArrayList<String>();
	static Map<String, NodeLocation> mapNodeLocationByDevice = new HashMap<String, NodeLocation>();
	static Map<String, String> mapNodeByLocation = new HashMap<String, String>();
	static String scenario = LivingLabSimulator.class.getSimpleName();
	static Date argTargetDate = null;
	static Integer argTargetHour = null;
	/*
	public static Map<String, RestNodeLocation> allNodeLocations = new HashMap<String, RestNodeLocation>() {
		private static final long serialVersionUID = 1L; {
		put("N1", new RestNodeLocation("N1", "localhost", "9191"));
		put("N2", new RestNodeLocation("N2", "localhost","9292"));
		put("N3", new RestNodeLocation("N3", "localhost","9393"));
		put("N4", new RestNodeLocation("N4", "localhost","9494"));
		put("N1r", new RestNodeLocation("N1", "129.194.10.168","9191"));
		put("N2r", new RestNodeLocation("N2", "129.194.10.168","9292"));
		put("N3r", new RestNodeLocation("N3", "129.194.10.168","9393"));
		put("N4r", new RestNodeLocation("N4", "129.194.10.168","9494"));
	}};

	public static Map<String, String> locationByNode1  = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;{
	    put("Ecole primaire", "N1");
	    put("Parascolaire", "N1");
	    put("Gymnase", "N1");
	    put("Sous-sol", "N1");
	    put("", "N1");
	}};
	public static Map<String, String> locationByNode2  = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L; {
	    put("Ecole primaire", "N1");
	    put("Parascolaire", "N1");
	    put("Gymnase", "N2");
	    put("Sous-sol", "N2");
	    put("", "N2");
	}};
	public static Map<String, String> locationByNode3  = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L; {
	    put("Ecole primaire", "N1");
	    put("Parascolaire", "N2");
	    put("Gymnase", "N3");
	    put("Sous-sol", "N3");
	    put("", "N3");
	}};
	public static Map<String, String> locationByNode4  = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;{
	    put("Ecole primaire", "N1");
	    put("Parascolaire", "N2");
	    put("Gymnase", "N3");
	    put("Sous-sol", "N4");
	    put("", "N4");
	}};
	public static Map<String, String> locationByNode5  = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;{
	    put("Ecole primaire", "N1");
	    put("Parascolaire", "N2");
	    put("Gymnase", "N3");
	    put("Sous-sol", "N4");
	    put("", "N5");
	}};
	public static Map<Integer, Map<String, String>> locationMapByNodeNb  = new HashMap<Integer, Map<String, String>>() {
		private static final long serialVersionUID = 1L;{
	    put(1, locationByNode1);
	    put(2, locationByNode2);
	    put(3, locationByNode3);
	    put(4, locationByNode4);
	    put(5, locationByNode5);
	}};
	*/

	private static String getDeviceBaseUrl(String deviceName) {
		String baseUrl = defaultbaseUrl;
		 if(mapNodeLocationByDevice.containsKey(deviceName)) {
			 NodeLocation nodeLocation = mapNodeLocationByDevice.get(deviceName);
			 baseUrl = nodeLocation.getUrl();
		 }
		 return baseUrl;
	}

	protected static void init(String args[]) throws Exception {
		initDb();
		Map<String,String> options = SapereUtil.retrieveArgsOptions(args);
		if(options.containsKey("baseUrl")) {
			defaultbaseUrl = options.get("baseUrl");
			NodeLocation nodeLocation = retrieveNodeLocation(defaultbaseUrl);
			nodeLocation.setName("N1");
			mapNodes.put(nodeLocation.getName(), nodeLocation);
			launchConfig = new LaunchConfig();
			launchConfig.addNodeLocation(nodeLocation);
		} else if(options.containsKey("launchConfig")) {
			String configPath = options.get("launchConfig"); // "run_configs/config_4nodes.json"
			launchConfig = laodLaunchConfig(configPath);
			mapNodes = launchConfig.getMapNodes();
			mapNodeByLocation = launchConfig.getMapNodeByLocation();
			defaultbaseUrl = launchConfig.getBaseUrl();
			int nodesNb = mapNodes.size();
			testMultiNodes = ( nodesNb > 0);
			scenario = LivingLabSimulator.class.getSimpleName() + ".mult" + nodesNb;
		}
		// Initialize mapBaseUrl
		listBaseUrl.clear();
		mapBaseUrl.clear();
		for (String node : mapNodes.keySet()) {
			NodeLocation nodeLocation = mapNodes.get(node);
			mapBaseUrl.put(node, nodeLocation.getUrl());
			if(!listBaseUrl.contains(nodeLocation.getUrl())) {
				listBaseUrl.add(nodeLocation.getUrl());
			}
		}
		// Init argTargetDate
		argTargetDate = null;
		if(options.containsKey("targetDate")) {
			String sTargetDate = options.get("targetDate");
			try {
				argTargetDate = UtilDates.format_sql_day.parse(sTargetDate);
			} catch (ParseException e) {
				logger.error(e);
			}
		}
		argTargetHour = null;
		if(options.containsKey("targetHour")) {
			String sTargetHour = options.get("targetHour");
			try {
				argTargetHour = Integer.valueOf(sTargetHour);
				int hourShift = argTargetHour - UtilDates.getHourOfDay(new Date());
				datetimeShifts.put(Calendar.HOUR_OF_DAY, hourShift);
				//argTargetDate.setTime(argTargetDate.getTime() + 60*60*1000*hourShift);
			} catch (Exception e) {
				logger.error(e);
			}
		}

		allDevices = getMeyrinDevices();
		if(mapNodes.size()>0) {
			for(Device device : allDevices) {
				String deviceName = device.getName();
				String location = device.getLocation()==null? "" : device.getLocation();
				if(mapNodeByLocation.containsKey(location)) {
					String nodeName = mapNodeByLocation.get(location);
					mapNodeLocationByDevice.put(deviceName, mapNodes.get(nodeName));
				} else if(mapNodes.size() > 1){
					// if multiple nodes exist, each device location must be linked to a node
					logger.error("init : no node found for device/location " + deviceName + "/" + location);
				}
				/*
				if(device.isProducer()) {
					test_mapNodeLocationByDevice.put(deviceName, test_mapNodes.get("N2"));
				} else {
					test_mapNodeLocationByDevice.put(deviceName, test_mapNodes.get("N1"));
				}*/
			}
		}

		nodeContent = getNodeContent();
		initDates(argTargetDate);
	}

	private static void initDates(Date argTargetDate1) throws Exception {
		// dateCurrent should be before 1er June
		Date maxDate = null;
		if(argTargetDate1 != null) {
			maxDate = UtilDates.shiftDateDays(argTargetDate1, 1);
		}
		DeviceMeasure lastConsumerDeviceMeasure = retrieveLastDevicesMeasure("MN", null, maxDate);
		//Date dateLastMonth = UtilDates.shiftDateMonth(UtilDates.getNewDate(0, datetimeShifts), -1);
		DeviceMeasure lastProducerDeviceMeasure = retrieveLastDevicesMeasure("15_MN", null, maxDate /*dateLastMonth*/);
		Date lastMeasureDate = lastConsumerDeviceMeasure.getDatetime();
		if(lastProducerDeviceMeasure.getDatetime().before(lastMeasureDate)) {
			lastMeasureDate = lastProducerDeviceMeasure.getDatetime();
		}
		Date targetDate = UtilDates.removeTime(lastMeasureDate);
		//Date targetDateProducer = UtilDates.removeTime(lastProducerDeviceMeasure.getDatetime());
		dateCurrent = UtilDates.getNewDate(datetimeShifts);
		try {
			//Date date28June = SapereUtil.format_day.parse("20220628");
			while(!dateCurrent.before(targetDate)) {
				//dayShift-=1;
				int currentDayShift = datetimeShifts.containsKey(Calendar.DAY_OF_MONTH) ? datetimeShifts.get(Calendar.DAY_OF_MONTH) : 0;
				currentDayShift-=1;
				datetimeShifts.put(Calendar.DAY_OF_MONTH, currentDayShift);
				dateCurrent = UtilDates.getNewDate(datetimeShifts);
			}
			//dayShiftProducer = dayShift;
			dateCurrentProducer = dateCurrent;
			//UtilDates.updateDateConstraints(datetimeConstraints, dateCurrent, new ArrayList<>(Arrays.asList(Calendar.DAY_OF_MONTH, Calendar.MONTH)));
			Date test = UtilDates.getNewDate(datetimeShifts);
			logger.info("test = "+ test);
		} catch (Exception e) {
			logger.error(e);
		}
		deviceStatistics = retrieveDeviceStatistics(producersCategoryFilter, datetimeShifts);
		staticticHour = getCurrentHour();
		refreshDeviceMeasures();
	}

	private static AgentInputForm generateAgentForm(Device aDevice, double targetPower, AgentForm existingForm) {
		double duration = Double.valueOf(24 * 60 * 365);
		if (aDevice.getAverageDurationMinutes() > 0) {
			duration = aDevice.getAverageDurationMinutes() * (1 + 0.10 * random.nextGaussian());
		}
		PriorityLevel priority = aDevice.getPriorityLevel();
		boolean activateDateShift = true;
		Date current = activateDateShift ? getCurrentDate() : getCurrentDateBidon();
		ProsumerRole prosumerRole = aDevice.isProducer() ? ProsumerRole.PRODUCER : ProsumerRole.CONSUMER;
		EnvironmentalImpact envImpact = aDevice.getEnvironmentalImpact();
		EnergyStorageSetting energyStorageSetting = null;
		Date endDate = UtilDates.shiftDateMinutes(current, duration);
		long timeShiftMS = UtilDates.computeTimeShiftMS(datetimeShifts);
		PricingTable pricingTable = new PricingTable(current, 0.0, timeShiftMS);
		AgentInputForm result = new AgentInputForm(prosumerRole, "", aDevice.getName(), aDevice.getCategory(), envImpact, pricingTable.getMapPrices(), targetPower, current, endDate,
				priority, duration, energyStorageSetting, timeShiftMS);
		result.updateDeviceProperties(aDevice.getProperties());
		//result.setMapPrices(new HashMap<>());
		if (existingForm != null) {
			result.setAgentName(existingForm.getAgentName());
			result.setId(existingForm.getId());
		}
		return result;
	}

	private static Date selectNextMeasuresDate(boolean takeLastDate, long timeShiftMS) {
		if(deviceMeasures.isEmpty()) {
			return null;
		}
		// For debug
		boolean logMeasures = false;
		if(logMeasures) {
			StringBuffer log = new StringBuffer();
			for(Date nextDate1 : deviceMeasures.keySet()) {
				 List<DeviceMeasure> measures = deviceMeasures.get(nextDate1);
				 log.append(CR + UtilDates.formatTimeOrDate(nextDate1, timeShiftMS) + " : " + measures.size());
			}
			logger.info("selectDateDeviceMeasures :" + log.toString());
		}
		Date nextDate = null;
		Date lastDate = null;
		Iterator<Date> dateIterator = deviceMeasures.keySet().iterator();
		nextDate = dateIterator.next();
		while(nextDate.before(dateCurrent) && dateIterator.hasNext()) {
			lastDate =  nextDate;
			nextDate = dateIterator.next();
		}
		logger.info("selectNextMeasuresDate dateCurrent = " + UtilDates.formatTimeOrDate(dateCurrent, timeShiftMS)
			+ ", selectedDate = " +   UtilDates.formatTimeOrDate(nextDate, timeShiftMS));
		if(takeLastDate) {
			if(lastDate!=null && lastDate.before(dateCurrent)) {
				return lastDate;
			}
		} else {
			if(!nextDate.before(dateCurrent)) {
				return nextDate;
			}
		}
		return null;
	}

	private static Map<String, Double> getTargetPowersByDevice(Date selectedDate) {
		Map<String, Double> result = new HashMap<>();
		//Date selectedDate = selectNextMeasuresDate();
		if(selectedDate!=null) {
			List<DeviceMeasure> measures = deviceMeasures.get(selectedDate);
			for(DeviceMeasure nextMeasure : measures) {
				String deviceName = nextMeasure.getDeviceName();
				if(!result.containsKey(deviceName)) {
					result.put(deviceName, SapereUtil.round(nextMeasure.computeTotalPower_p(),2));
				}
			}
			// Complete with device statistics
			for(Device nextDevice : allDevices) {
				if(nextDevice.isProducer() && !result.containsKey(nextDevice.getName())) {
					double coeff = 3.0;
					if(DeviceCategory.EXTERNAL_ENG.equals(nextDevice.getCategory())) {
						coeff = 10.0;
					}
					DeviceCategory deviceCategory = nextDevice.getCategory();
					double avgPower = deviceStatistics.containsKey(deviceCategory) ? coeff*deviceStatistics.get(deviceCategory) : 0;
					double powerTarget = SapereUtil.round(avgPower * (1 + 0*0.05 * random.nextGaussian()),2);
					result.put(nextDevice.getName(), powerTarget);
				}
			}
		}
		return result;
	}

	private static boolean refreshDeviceMeasures() throws Exception {
		boolean allOK = true;
		dateCurrent = UtilDates.getNewDate(datetimeShifts);
		Date minDate =  UtilDates.shiftDateMinutes(dateCurrent, -1); // One minute before
		//Date auxDateEnd = SapereUtil.shiftDateDays(SapereUtil.getCurrentHour(), dayShift);
		//Date maxDate = SapereUtil.shiftDateMinutes(auxDateEnd, 65); // add 1 hour and 5 minutes
		Date maxDate = UtilDates.shiftDateMinutes(minDate, 60); // add 1 hour and 5 minutes
		// Retrieve 1MM Measures
		deviceMeasures = retrieveDevicesMeasures(new DeviceCategory [] {}, "MN", minDate, maxDate);
		allOK = (deviceMeasures.size() > 0);
		if (allOK) {
			// ADD producer measures
			dateCurrentProducer = UtilDates.getNewDate(datetimeShifts);
			Date mindate2 = UtilDates.shiftDateMinutes(dateCurrentProducer, -30);
			Date maxdate2 = UtilDates.shiftDateMinutes(dateCurrentProducer, 60+30);
			Map<Date, List<DeviceMeasure>> mapMeasures15MN =  retrieveDevicesMeasures(new DeviceCategory [] {}, "15_MN", mindate2, maxdate2);
			int dayCorrection = 0;//dayShift - dayShiftProducer;
			Map<Date, List<DeviceMeasure>> mapMeasures15MN2 = SapereUtil.shiftMeasureDates(mapMeasures15MN, dayCorrection);
			allOK = (mapMeasures15MN2.size() > 0);
			// Merge 15MB with 1MM measure
			if(allOK) {
				deviceMeasures = SapereUtil.auxMergeMapMeasures15MN(deviceMeasures, mapMeasures15MN2, logger);
			}
		}
		if(allOK) {
			// checkup of PV brute measure
			String pvDeviceName = "Production PV brute";
			for(Date nextDate : deviceMeasures.keySet()) {
				List<DeviceMeasure> listDeviceMeasure = deviceMeasures.get(nextDate);
				boolean deviceFound = false;
				double totalPower = 0;
				for(DeviceMeasure nextMeasure : listDeviceMeasure) {
					if(pvDeviceName.equals(nextMeasure.getDeviceName())) {
						deviceFound = true;
						totalPower = nextMeasure.computeTotalPower_p();
						if(totalPower==0) {
							logger.warning("refreshDeviceMeasures : no power for device "+ pvDeviceName + " at " + UtilDates.format_date_time.format(nextDate));
						}
					}
				}
				if(!deviceFound) {
					allOK = false;
					logger.error("refreshDeviceMeasures : device "+ pvDeviceName + " not found in merged map at " + UtilDates.format_date_time.format(nextDate));
				}
				logger.info("refreshDeviceMeasures " +  pvDeviceName + " : W= " + totalPower + " at " + UtilDates.format_date_time.format(nextDate));
			}
		}
		return allOK;
	}

	public static boolean executeLoop(boolean isSupervisionEnabled) throws Exception {
		loopCounter++;
		//int nbTry = 0;
		logger.info("executeLoop " + loopCounter + " begin");
		dateCurrent = UtilDates.getNewDate(datetimeShifts);
		long timeShiftMS = UtilDates.computeTimeShiftMS(datetimeShifts);
		boolean takeLastDate = (loopCounter==1); // For the first loop, take the last date
		Date dateNextMeasure = selectNextMeasuresDate(takeLastDate, timeShiftMS);
		//Date dateNextMeasureMax = SapereUtil.shiftDateMinutes(dateCurrent, 2);
		Date dateNextMeasureMax = UtilDates.shiftDateSec(dateCurrent, 70);
		if(dateNextMeasure==null || dateNextMeasure.after(dateNextMeasureMax)) {
			logger.info("executeLoop : refresh device measures");
			boolean isRefreshOK = refreshDeviceMeasures();
			if(!isRefreshOK) {
				throw new MissingMeasureException("Missing device measure at " + UtilDates.format_date_time.format(dateCurrent));
			}
			dateNextMeasure = selectNextMeasuresDate(takeLastDate, timeShiftMS);
		}
		if(dateNextMeasure==null) {
			logger.warning("dateNextMeasure not found");
			return false;
		}
		Map<String, Double> targetsByDevice = getTargetPowersByDevice(dateNextMeasure);
		if(targetsByDevice.size()==0) {
			logger.warning("### loop " + loopCounter + " : no target values found ##########");
		}
		// wait untill next mesaure date
		int waitCpt = 0;
		while(dateCurrent.before(dateNextMeasure)) {
			try {
				Thread.sleep(1000);
				if(waitCpt % 10 == 0) {
					logger.info("--- loop " + loopCounter + " " + UtilDates.format_time.format(dateCurrent) + " : waiting for next measure date " + UtilDates.format_time.format(dateNextMeasure));
					dateNextMeasureMax = UtilDates.shiftDateSec(dateCurrent, 70);
					if(dateNextMeasure.after(dateNextMeasureMax)) {
						logger.info("executeLoop : refresh device measures");
						refreshDeviceMeasures();
						dateNextMeasure = selectNextMeasuresDate(takeLastDate, timeShiftMS);
					}
				}
				waitCpt++;
			} catch (InterruptedException e) {
				logger.error(e);
			}
			dateCurrent = UtilDates.getNewDate(datetimeShifts);
		}
		nodeContent = getNodeContent();
		int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		if(currentHour != staticticHour) {
			deviceStatistics = retrieveDeviceStatistics(producersCategoryFilter, datetimeShifts);
			staticticHour = getCurrentHour();
		}
		if(!isSupervisionEnabled) {
			logger.info("--- loop " + loopCounter + " activate supervision");
			for(String baseUrl : listBaseUrl) {
				enableSupervision(baseUrl);
			}
		}
		// Check if all current agent devices are contained in the target table
		Map<String, List<String>> mapAgentToStop = new HashMap<String, List<String>>();
		for (AgentForm agent : nodeContent.getAgents()) {
			String deviceName = agent.getDeviceName();
			String baseUrl = getDeviceBaseUrl(deviceName);
			if(!targetsByDevice.containsKey(deviceName)) {
				// Agent device not contained in target table : send a request to stop the agent
				AgentForm agentForm = nodeContent.getAgentByDeviceName(deviceName);
				if(!mapAgentToStop.containsKey(baseUrl)) {
					mapAgentToStop.put(baseUrl, new ArrayList<String>());
				}
				List<String> listAgentToStop = mapAgentToStop.get(baseUrl);
				listAgentToStop.add(agentForm.getAgentName());
			}
		}
		if(mapAgentToStop.size() > 0) {
			for (String baseUrl : mapAgentToStop.keySet()) {
				List<String> listAgentToStop = mapAgentToStop.get(baseUrl);
				if(listAgentToStop.size() > 0) {
					boolean resultStop = stopListAgents(baseUrl, listAgentToStop);
					if(!resultStop) {
						for (AgentForm agent : nodeContent.getAgents()) {
							String deviceName = agent.getDeviceName();
							if(!targetsByDevice.containsKey(deviceName)) {
								// Agent device not contained in target table : send a request to stop the agent
								AgentForm agentForm = nodeContent.getAgentByDeviceName(deviceName);
								if(agentForm!=null) {
									stopAgent(baseUrl, agentForm);
								}
							}
						}
					}
				}
			}
		}
		for (Device nextDevice : allDevices) {
			 String deviceName = nextDevice.getName();
			 String baseUrl = getDeviceBaseUrl(deviceName);
			 if(targetsByDevice.containsKey(deviceName)) {
				 double powerToSet = targetsByDevice.get(deviceName);
				 AgentForm agentForm = nodeContent.getAgentByDeviceName(deviceName);
				 if(agentForm==null) {
					 // Send a request a create a new agent
					 AgentInputForm newAgentForm = generateAgentForm(nextDevice, powerToSet, null);
					 addAgent(baseUrl, newAgentForm);
				 } else {
					 // Send a request to modify the agent
					 if( powerToSet == agentForm.getPower()) {
						 // do nothing
						 logger.info("no power change for device " + nextDevice.getName());
					 } else {
						 AgentInputForm agentInputForm = generateAgentForm(nextDevice, powerToSet, agentForm);
						 modifyAgent(baseUrl, agentInputForm);
					 }
				 }
			} else {
				// Send a request to stop the agent
				AgentForm agentForm = nodeContent.getAgentByDeviceName(deviceName);
				if(agentForm!=null) {
					stopAgent(baseUrl, agentForm);
				}
			}
		}
		try {
			int sleepMS = 100*targetsByDevice.size();
			Thread.sleep(sleepMS);
		} catch (InterruptedException e) {
			logger.error(e);
		}
		nodeContent = getNodeContent();
		// Check if target is OK
		Map<String, Boolean> targetKO = new HashMap<String, Boolean>();
		for(String deviceName : targetsByDevice.keySet()) {
			double target = targetsByDevice.get(deviceName);
			AgentForm agent = nodeContent.getAgentByDeviceName(deviceName);
			if(agent == null) {
				logger.warning(deviceName + " has not been initialized");
			}
			double delta = Math.abs((agent==null? 0 : agent.getPower()) - target);
			if(delta>=0.01) {
				targetKO.put(deviceName, true);
			}
		}
		String result = " *** All is OK *** ";
		if(targetKO.size()>0) {
			result = "### target not reached for the following devices : " + targetKO.keySet();
			logger.warning("executeLoop " + loopCounter + " : end " + result);
		} else {
			logger.info("executeLoop " + loopCounter + " : end " + result);
		}
		return targetKO.size()==0;
	}

	protected static NodeContent getNodeContent() {
		if(listBaseUrl.size() > 1) {
			return TestSimulator.getAllNodesContent(defaultbaseUrl);
		} else {
			return TestSimulator.getNodeContent(defaultbaseUrl);
		}
	}
	/*
	private static boolean isStateReached(String baseUrl, Date startDate, String variableName, int stateId, boolean obserationUpdated) {
		List<VariableStateHistory> stateHistory = retrieveLastHistoryStates(PredictionScope.NODE, baseUrl, startDate, variableName, obserationUpdated);
		for(VariableStateHistory nextState : stateHistory ) {
			if(stateId == nextState.getStateId()) {
				return true;
			}
		}
		return false;
	}*/


	public static boolean checkStateTransition(String baseUrl, String tagBegin, Date minCreationDate, String variableName) {
		List<VariableStateHistory> stateHistory = retrieveLastHistoryStates(PredictionScope.NODE, baseUrl, minCreationDate, variableName, true);
		if(stateHistory.size() == 0) {
			return false;
		}
		for(VariableStateHistory nextStateHiso : stateHistory) {
			if(nextStateHiso.isStationary() && nextStateHiso.getStateId() >= 3 ) {
				logger.warning("### " + tagBegin + " Stationary state : " + nextStateHiso);
			}
		}
		for(VariableStateHistory nextStateHiso : stateHistory) {
			if(!nextStateHiso.isStationary()) {
				return true;
			} else if(nextStateHiso.getId().intValue() < 4) {
				return true;
			}
		}
		return false;
	}

	/*
	public static void testMarkovStates(String baseUrl) {
		 PredictionSetting nodePredicitonSetting = new PredictionSetting(true, null, LearningModelType.MARKOV_CHAINS);
		 PredictionSetting clusterPredictionSetting = new PredictionSetting(false, null, LearningModelType.MARKOV_CHAINS);
		InitializationForm initForm = new InitializationForm(scenario, maxTotalPower, datetimeShifts, nodePredicitonSetting, clusterPredictionSetting);
		//initForm.setActivateAggregations(false);initForm.setActivatePredictions(false); // TO DELETE !!!!!
		NodeTransitionMatrices nodeTransitionMatrices = getCurrentNodeTransitionMatrices(baseUrl);
		int cpt = 0;
		while(!nodeTransitionMatrices.isComplete() && cpt < 10) {
			String[] listVar = {"consumed","requested", "produced",  "provided", "available", "missing"};
			for(String variable : listVar) {
				if(!isServiceInitialized(baseUrl)) {
					initEnergyService(baseUrl, initForm);
				}
				// Refresh node transition matrix
				nodeTransitionMatrices = getCurrentNodeTransitionMatrices(baseUrl);
				if(!nodeTransitionMatrices.isComplete(variable)) {
					testMarkovStates(baseUrl, variable, nodeTransitionMatrices);
				}
			}
			cpt++;
			if(!isServiceInitialized(baseUrl)) {
				initEnergyService(baseUrl, initForm);
			}
			nodeTransitionMatrices = getCurrentNodeTransitionMatrices(baseUrl);
		}
	}*/
	/*
	public static void testMarkovStates(String baseUrl, String variableName, NodeTransitionMatrices nodeTransitionMatrices) {
		PredictionSetting nodePredicitonSetting = new PredictionSetting(true, null, LearningModelType.MARKOV_CHAINS);
		PredictionSetting clusterPredictionSetting = new PredictionSetting(false, null, LearningModelType.MARKOV_CHAINS);
		InitializationForm initForm = new InitializationForm(scenario, maxTotalPower, datetimeShifts, nodePredicitonSetting, clusterPredictionSetting);
		DoubleMatrix observations = nodeTransitionMatrices.getAllObsMatrice(variableName);
		List<Integer> listOfStates = new ArrayList<>();
		int nbOfStates = observations.getRowDimension();
		for(int stateId=nbOfStates; stateId>0; stateId--) {
			int rowIdx = stateId-1;
			double rowSum = nodeTransitionMatrices.getObsAndCorrectionsSum(variableName,  rowIdx);
			if(rowSum == 0) {
				listOfStates.add(stateId);
			}
		}
		String shiftHourOfDay = (datetimeShifts.containsKey(Calendar.HOUR_OF_DAY))? "" + datetimeShifts.get(Calendar.HOUR_OF_DAY) : "";
		String tagBegin = "testMarkovStates " + variableName + " " + shiftHourOfDay + "H : ";
		logger.info(tagBegin + "list of origin states to test : " + listOfStates);
		for(int stateId : listOfStates) {
			logger.info(tagBegin +  "try to reach state " + stateId);
			initForm.setInitialState(variableName, stateId);
			if(isServiceInitialized(baseUrl)) {
				stopEnergyService(baseUrl);
			}
			Date minCreationDate = getCurrentDate();
			initEnergyService(baseUrl, initForm);
			int loopIdx = 0;
			boolean stateRechaed = false;
			while(!(stateRechaed = isStateReached(baseUrl, minCreationDate, variableName, stateId, false) )&& loopIdx < 10)  {
				try {
					Thread.sleep(1*1000);
					loopIdx++;
				} catch (InterruptedException e) {
					logger.error(e);
				}
			}
			if(stateRechaed) {
				logger.info("*** " + tagBegin + "state " + stateId + " reached ");
			} else {
				logger.warning("### " + tagBegin + "state " + stateId + " not reached ");
			}
			// startDate = getCurrentDate();
			if(stateRechaed) {
				//enableSupervision();
				boolean isSupervisionEnabled = false;
				boolean isOK = false;
				int cpt = 0;
				while(!isOK && cpt < 10) {
					try {
						isOK = executeLoop(isSupervisionEnabled);
					} catch (Exception e) {
						logger.error(e);
					}
					cpt++;
					isSupervisionEnabled = true;
				}
				boolean transitionReached = checkStateTransition(baseUrl, tagBegin, minCreationDate, variableName);
				loopIdx = 0;
				while(!transitionReached && loopIdx < 10) {
					try {
						executeLoop(isSupervisionEnabled);
					} catch (Exception e) {
						logger.error(e);
					}
					transitionReached = checkStateTransition(baseUrl, tagBegin, minCreationDate, variableName);
					if(!transitionReached) {
						logger.info(tagBegin + "state transition still not done from state " + stateId);
					}
				}
				if(transitionReached) {
					logger.info("*** testMarkovStates " + variableName + ": state transition done from state " + stateId);
				}
			}
			stopEnergyService(baseUrl);
		}
	}*/
	public static int GOSSIP_ENSEMBLE_LEARNING = 1;
	public static int GOSSIP_FEDERATED_LEARNING = 2;

	public static void doInitLearningModels(InitializationForm initForm) throws Exception {
	int nbOfDays = 30;
		//Date dateBegin = UtilDates.shiftDateDays(dateCurrent, - 1);
		//nbOfDays = 10;
		nbOfDays = 3;
		boolean refreshAll = false;
		long resul1 = 0;
		if(refreshAll) {
			resul1 = SimulatorDBHelper.refreshTmpMeasureData(dateCurrent, nbOfDays, launchConfig.getMapNodeByLocation(), "N1" );
		} else {
			resul1 = SimulatorDBHelper.setNodeInMeasureData(dateCurrent, nbOfDays, mapNodeByLocation, "N1");
		}
		logger.info("doInitLearningModels : resul1 = " + resul1);
		//Map<String, LearningModelType> mapModelTypes = new HashMap<String, LearningModelType>();
		//LearningModelType modelType = LearningModelType.MARKOV_CHAINS;
		//LearningModelType modelType = LearningModelType.LSTM;
		initForm.setDisableSupervision(true);

		// Initialize the aggregator for node predictions
		LearningAggregationOperator nodeAggregator = null;
		int usedDistributedLearning = GOSSIP_ENSEMBLE_LEARNING;
		LearningModelType defaultModelType = LearningModelType.MARKOV_CHAINS;// MARKOV_CHAINS or LSTM
		int aggregationWaitingTimeMinutes = (defaultModelType == LearningModelType.LSTM) ? 15 : 5;
		if(usedDistributedLearning == GOSSIP_ENSEMBLE_LEARNING) {
			nodeAggregator = LearningAggregationOperator.createPredictionAggregationOperator(PredictionData.OP_SAMPLING_NB, aggregationWaitingTimeMinutes); // OP_SAMPLING_NB, OP_DISTANCE_CURRENT_POWER, OP_DISTANCE_POWER_PROFILE
		}
		if(usedDistributedLearning == GOSSIP_FEDERATED_LEARNING) {
			nodeAggregator = LearningAggregationOperator.createModelAggregationOperator(ILearningModel.OP_MIN_LOSS, aggregationWaitingTimeMinutes);// OP_SAMPLING_NB, OP_POWER_LOSS, OP_MIN_LOSS 
		}
		nodeAggregator = null;
		int nbOfSamplingsBeforeTraining = (defaultModelType == LearningModelType.LSTM) ? 30 : 0;// 100;// 20;// 300;
		//nbOfSamplingsBeforeTraining = 5; // TO TEST if the flask server falls down after 30 min
		PredictionSetting nodePredicitonSetting = new PredictionSetting(true, nodeAggregator, defaultModelType, nbOfSamplingsBeforeTraining);
		initForm.setNodePredicitonSetting(nodePredicitonSetting);

		// initialize the aggregator for cluster predicitons
		LearningAggregationOperator clusterAggregator = null;
		if(usedDistributedLearning == GOSSIP_ENSEMBLE_LEARNING) {
			clusterAggregator = LearningAggregationOperator.createPredictionAggregationOperator(PredictionData.OP_SAMPLING_NB, aggregationWaitingTimeMinutes); // OP_SAMPLING_NB, OP_DISTANCE_CURRENT_POWER, OP_DISTANCE_POWER_PROFILE
		}
		if(usedDistributedLearning == GOSSIP_FEDERATED_LEARNING) {
			clusterAggregator = LearningAggregationOperator.createModelAggregationOperator(ILearningModel.OP_MIN_LOSS, aggregationWaitingTimeMinutes);
		}
		//clusterAggregator = null;
		PredictionSetting clusterPredictionSetting = new PredictionSetting(true, clusterAggregator, defaultModelType, nbOfSamplingsBeforeTraining);
		initForm.setClusterPredictionSetting(clusterPredictionSetting);
		Map <String, PredictionContext> mapLocalPredictionContext = new HashMap <String, PredictionContext>();
		Map <String, PredictionContext> mapClusterPredictionContext = new HashMap <String, PredictionContext>();
		int nodeIdx = 0;
		for(String baseUrl : listBaseUrl) {
			// For ensemble leaarning (one none with MC, the other with LSTM)
			LearningModelType modelType =  defaultModelType; // TO DELETE
			if(usedDistributedLearning == GOSSIP_ENSEMBLE_LEARNING) {
				modelType = (nodeIdx % 2 == 0) ? LearningModelType.MARKOV_CHAINS : LearningModelType.LSTM;
			}
			//modelType = LearningModelType.MARKOV_CHAINS;
			initForm.getClusterPredictionSetting().setUsedModel(modelType);
			initForm.getNodePredicitonSetting().setUsedModel(modelType);
			initEnergyService(baseUrl, initForm);
			PredictionContext localPredictionContext = getPredictionContext(baseUrl, PredictionScope.NODE);
			if(localPredictionContext != null) {
				localPredictionContext.setModelType(modelType);
				mapLocalPredictionContext.put(localPredictionContext.getNodeLocation().getName(), localPredictionContext);
			}
			PredictionContext clusterPredictionContext = getPredictionContext(baseUrl, PredictionScope.CLUSTER);
			if(clusterPredictionContext != null) {
				clusterPredictionContext.setModelType(modelType);
				mapClusterPredictionContext.put(clusterPredictionContext.getNodeLocation().getName(), clusterPredictionContext);
			}
			nodeIdx++;
		}
		//Date dateIteration = UtilDates.shiftDateDays(dateCurrent, -1*nbOfDays);
		//Map<String, List<NodeTransitionMatrices>> mapNodeTransitionMatrices = new HashMap<String, List<NodeTransitionMatrices>>();
		Map<Integer, Date> mapIterations = new HashMap<Integer, Date>();
		for (int dayIdx = 0; dayIdx<=nbOfDays ; dayIdx++) {
			Date dateIteration = UtilDates.shiftDateDays(dateCurrent, dayIdx-nbOfDays);
			Integer iterationNumber = 1+dayIdx;
			mapIterations.put(iterationNumber, dateIteration);
		}
		Map<String, List<NodeTotal>> historyByNode = SimulatorDBHelper.loadNodesHistory(mapLocalPredictionContext, mapIterations);
		/*
		Map<String, List<NodeTotal>> historyByNode = SimulatorDBHelper.loadNodesHistory(mapLocalPredictionContext, mapIterations);
		for(String node : mapLocalPredictionContext.keySet()) {
			List<NodeTotal> listHistory = historyByNode.get(node);
			List<NodeTotal> listHistory2 = historyByNode2.get(node);
			if(listHistory.size() == listHistory2.size()) {
				int idx = 0;
				for(NodeTotal nodeTotal : listHistory) {
					NodeTotal nodeTotal2 = listHistory2.get(idx);
					boolean areEqual = nodeTotal.getDate().equals(nodeTotal2.getDate())
							&& (Math.abs(nodeTotal.getRequested() - nodeTotal2.getRequested()) <= 0.00001)
							&& (Math.abs(nodeTotal.getProduced() - nodeTotal2.getProduced()) <= 0.00001)
							&& (Math.abs(nodeTotal.getConsumed() - nodeTotal2.getConsumed()) <= 0.00001)
							&& (Math.abs(nodeTotal.getAvailable() - nodeTotal2.getAvailable()) <= 0.00001)
							&& (Math.abs(nodeTotal.getMissing() - nodeTotal2.getMissing()) <= 0.00001)
							&& (Math.abs(nodeTotal.getProvided() - nodeTotal2.getProvided()) <= 0.00001)
							;
					if(!areEqual) {
						logger.error("NodeTotal are different ");
						logger.error("nodeTotal1 =  " + nodeTotal);
						logger.error("nodeTotal2 =  " + nodeTotal2);
					}
					idx++;
				}
				logger.info(node + " all items are equals");
			} else {
				logger.error(node + " listHistory and listHistor2 have different size " +listHistory.size()+ " " + historyByNode2.size() );
			}
		}*/
		SimulatorDBHelper.saveNodesTotal(historyByNode);
		for(String node : mapLocalPredictionContext.keySet()) {
			String baseUrl = mapBaseUrl.get(node);
			//initNodeHistory(baseUrl, PredictionScope.NODE ,modelType, historyByNode.get(node));
			PredictionContext localPredictionContext = mapLocalPredictionContext.get(node);
			initNodeHistory2(baseUrl, localPredictionContext);

		}
		if(initForm.getClusterPredictionSetting().isActivated()) {
			// Aggregate au node totals in one single nodeTotal
			//List<NodeTotal> aggregatedHistory = aggregateNodeHistory(mapAllNodesPredictionContext, historyByNode);
			for(String node : mapClusterPredictionContext.keySet()) {
				String baseUrl = mapBaseUrl.get(node);
				//initNodeHistory(baseUrl, PredictionScope.CLUSTER, modelType, aggregatedHistory);
				PredictionContext clusterPredictionContext = mapClusterPredictionContext.get(node);
				initNodeHistory2(baseUrl, clusterPredictionContext);
			}
		}
		// Enable supervision
		for(String baseUrl : listBaseUrl) {
			//PredictionContext predictionContext = getPredictionContext(baseUrl);
			//List<NodeTransitionMatrices> nodeTransitionMatrices = new ArrayList<NodeTransitionMatrices>();
			enableSupervision(baseUrl);
			//initForm.setActivateAggregations(true);
			//initForm.setActivatePredictions(true);
			//initEnergyService(baseUrl, initForm);
		}
	}

	/*
	@Deprecated
	public static void doMarkovMatricesCompletion(InitializationForm initForm) throws Exception {
		Map<Integer,Integer> backupDatetimeShifts = new HashMap<>();
		for(Integer field : datetimeShifts.keySet()) {
			backupDatetimeShifts.put(field, datetimeShifts.get(field));
		}
		int currentHourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		for(String baseUrl : listBaseUrl) {
			initEnergyService(baseUrl, initForm);
			MatrixFilter matrixFilter = new MatrixFilter();
			List<NodeTransitionMatrices> allNodeTransitionMatrices = retrieveAllNodeTransitionMatrices(baseUrl, matrixFilter);
			for(NodeTransitionMatrices nextNodeTM : allNodeTransitionMatrices) {
				int nextHourOfDay = nextNodeTM.getFeaturesKey().getTimeWindow().getStartHour();
				int nextShiftHourOfDay = nextHourOfDay - currentHourOfDay;
				datetimeShifts.put(Calendar.HOUR_OF_DAY, nextShiftHourOfDay);//
				if(!nextNodeTM.isComplete()) {
					initDates(argTargetDate);
					if(isServiceInitialized(baseUrl)) {
						stopEnergyService(baseUrl);
					}
					PredictionSetting nodePredicitonSetting = new PredictionSetting(true, null, LearningModelType.MARKOV_CHAINS);
					PredictionSetting clusterPredictionSetting = new PredictionSetting(true, null, LearningModelType.MARKOV_CHAINS);
					initForm = new InitializationForm(scenario, maxTotalPower, datetimeShifts, nodePredicitonSetting, clusterPredictionSetting);
					//initForm.setActivateAggregations(false);initForm.setActivatePredictions(true); // TO DELETE !!!!!
					for(NodeLocation nodeLocation : mapNodes.values()) {
						baseUrl = nodeLocation.getUrl();
						initEnergyService(baseUrl, initForm);
					}
					refreshNodeContent(baseUrl, listBaseUrl.size() > 0);
					testMarkovStates(baseUrl);
				}
			}
		}
		// After
		for(String baseUrl : listBaseUrl) {
			if(isServiceInitialized(baseUrl)) {
				stopEnergyService(baseUrl);
			}
		}
		datetimeShifts.clear();
		for(Integer field : backupDatetimeShifts.keySet()) {
			datetimeShifts.put(field, backupDatetimeShifts.get(field));
		}
		initDates(argTargetDate);
	}*/

	public static void main(String args[]) {
		debugLevel = 0;
		if (debugLevel > 0) {
			logger.info("Main " + args);
		}
		boolean initLearningModel = true;
		//initLearningModel = false; // TO DELETE !!!!!!!!
		datetimeShifts.clear();
		Integer forcedHourOfDay = null;
		int currentHourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		forcedHourOfDay = 8;//7;
		if(forcedHourOfDay != null) {
			datetimeShifts.put(Calendar.HOUR_OF_DAY, forcedHourOfDay - currentHourOfDay);
		}
		/*
		long test1 = UtilDates.computeTimeShiftMS(datetimeShifts);
		Integer forcedMinutes = null;
		forcedMinutes = 50;
		if(forcedMinutes != null) {
			int currentMinutes = Calendar.getInstance().get(Calendar.MINUTE);
			int shiftMinutes = forcedMinutes - currentMinutes;
			datetimeShifts.put(Calendar.MINUTE, shiftMinutes);
		}
		long test2 = UtilDates.computeTimeShiftMS(datetimeShifts);
		*/
		format_datetime.setTimeZone(TimeZone.getTimeZone("GMT"));
		try {
			init(args);
		} catch (Exception e) {
			logger.error(e);
		}
		int nbOfSamplingsBeforeTraining =  5;
		EnergyStorageSetting energyStorageSetting = new EnergyStorageSetting();
		PredictionSetting nodePredicitonSetting = new PredictionSetting(true, null, LearningModelType.MARKOV_CHAINS, nbOfSamplingsBeforeTraining);
		PredictionSetting clusterPredictionSetting = new PredictionSetting(false, null, LearningModelType.MARKOV_CHAINS, nbOfSamplingsBeforeTraining);
		InitializationForm initForm = new InitializationForm(scenario, maxTotalPower, datetimeShifts, energyStorageSetting, nodePredicitonSetting, clusterPredictionSetting);
		if(initLearningModel) {
			try {
				doInitLearningModels(initForm);
			} catch (Exception e) {
				logger.error(e);
			}
		}

		//initForm.setInitialState("requested", 4);
		boolean isSupervisionEnabled = true;
		for(String baseUrl : listBaseUrl) {
			if(!isServiceInitialized(baseUrl)) {
				initEnergyService(baseUrl, initForm);
			}
		}
		refreshNodeContent(defaultbaseUrl, listBaseUrl.size()>1);
		Date startDate = dateCurrent;
		//int waitItNb = 60;
		resetSimulatorLogs();
		boolean retrieveLastContent = false;
		try {
			if (retrieveLastContent) {
				for(String baseUrl : listBaseUrl) {
					restartLastNodeContent(baseUrl);
				}
				refreshNodeContent(defaultbaseUrl, listBaseUrl.size()>1);
			} else {
				executeLoop(isSupervisionEnabled);
			}
		} catch (Throwable e1) {
			logger.error(e1);
		}
		Date endDate = UtilDates.shiftDateMinutes(startDate, 30  * 60 * 1);
		boolean missingMeasures = false;
		while (dateCurrent.before(endDate) && !missingMeasures) {
			dateCurrent = UtilDates.getNewDate(datetimeShifts);
			//double random = Math.random();
			try {
				executeLoop(isSupervisionEnabled);
				logger.info("after executeLoop ");
				Thread.sleep(1 * 100);
			} catch (MissingMeasureException e1) {
				logger.error(e1);
				logger.error("######### MissingMeasureException has been thrown : stop the sceanrio");
				missingMeasures = true;
			} catch (Throwable e) {
				logger.error(e);
			}
		}
	}
}
