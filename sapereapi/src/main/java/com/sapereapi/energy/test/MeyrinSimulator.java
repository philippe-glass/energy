package com.sapereapi.energy.test;

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
import com.sapereapi.model.InitializationForm;
import com.sapereapi.model.NodeConfig;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.Device;
import com.sapereapi.model.energy.DeviceMeasure;
import com.sapereapi.model.energy.NodeContent;
import com.sapereapi.model.energy.PricingTable;
import com.sapereapi.model.energy.SimulatorLog;
import com.sapereapi.model.markov.MarkovStateHistory;
import com.sapereapi.model.markov.NodeMarkovStates;
import com.sapereapi.model.markov.NodeTransitionMatrices;
import com.sapereapi.model.prediction.MatrixFilter;
import com.sapereapi.model.referential.AgentType;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import Jama.Matrix;

public class MeyrinSimulator extends TestSimulator {

	static Map<String, Double> deviceStatistics = new HashMap<String, Double>();
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
	static double maxTotalPower = 20*NodeMarkovStates.DEFAULT_NODE_MAX_TOTAL_POWER;
	static boolean testMultiNodes = false;
	static Map<String, NodeConfig> test_mapNodes = new HashMap<String, NodeConfig>();
	static Map<String, NodeConfig> test_mapNodeConfigByDevice = new HashMap<String, NodeConfig>();
	static Map<String, String> test_nodesAttribution = new HashMap<String, String>();
	static String scenario = MeyrinSimulator.class.getSimpleName();
	static Date argTargetDate = null;

	protected static void init(String args[]) {
		Map<String,String> options = SapereUtil.retrieveArgsOptions(args);
		if(options.containsKey("baseUrl")) {
			baseUrl = options.get("baseUrl");
		} else if(options.containsKey("listBaseUrl")) {
			testMultiNodes = true;
			test_mapNodes.clear();
			String listBaseUrl =  options.get("listBaseUrl");
			int nodeIndex = 1;
			for(String nextUrl : listBaseUrl.split(",")) {
				String nodeName = "N"+nodeIndex;
				test_mapNodes.put(nodeName, new NodeConfig(nodeName,nextUrl));
				if(nodeIndex==1) {
					baseUrl = nextUrl;
				}
				nodeIndex++;
			}
			int nodesNb = test_mapNodes.size();
			scenario = MeyrinSimulator.class.getSimpleName() + ".mult" + nodesNb;
			test_nodesAttribution.clear();
			nodeIndex = 1;
			// Node attribution by location
			test_nodesAttribution.put("Ecole primaire", "N"+nodeIndex);
			if(nodesNb >= 4) {
				nodeIndex++;
			}
			test_nodesAttribution.put("Parascolaire", "N"+nodeIndex);
			nodeIndex++;
			test_nodesAttribution.put("Gymnase",  "N"+nodeIndex);
			nodeIndex++;
			test_nodesAttribution.put("Sous-sol",  "N"+nodeIndex);
			test_nodesAttribution.put("",  "N"+nodeIndex);
		}
		argTargetDate = null;
		if(options.containsKey("targetDate")) {
			String sTargetDate = options.get("targetDate");
			try {
				argTargetDate = UtilDates.format_sql_day.parse(sTargetDate);
			} catch (ParseException e) {
				logger.error(e);
			}
		}
		allDevices = getMeyrinDevices();
		if(test_mapNodes.size()>0) {
			for(Device device : allDevices) {
				String deviceName = device.getName();
				String location = device.getLocation()==null? "" : device.getLocation();
				if(test_nodesAttribution.containsKey(location)) {
					String nodeName = test_nodesAttribution.get(location);
					test_mapNodeConfigByDevice.put(deviceName, test_mapNodes.get(nodeName));
				} else {
					logger.error("no node found for device/location " + deviceName + "/" + location);
				}
				/*
				if(device.isProducer()) {
					test_mapNodeConfigByDevice.put(deviceName, test_mapNodes.get("N2"));
				} else {
					test_mapNodeConfigByDevice.put(deviceName, test_mapNodes.get("N1"));
				}*/
			}
		}

		nodeContent = getNodeContent();
		initDates(argTargetDate);
	}

	private static void initDates(Date argTargetDate1) {
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
			/*
			dateCurrentProducer = UtilDates.getNewDate(dayShiftProducer, forcedHourOfDay);
			while(!dateCurrentProducer.before(targetDateProducer)) {
				dayShiftProducer-=1;
				dateCurrentProducer = UtilDates.getNewDate(dayShiftProducer, forcedHourOfDay);
			}*/
			// dateCurrentProducer = SapereUtil.getNewDate(dayShiftProducer, forcedHourOfDay);
		} catch (Exception e) {
			logger.error(e);
		}
		deviceStatistics = retrieveDeviceStatistics(producersCategoryFilter, datetimeShifts);
		staticticHour = getCurrentHour();
		refreshDeviceMeasures();
	}

	private static AgentForm generateAgentForm(Device aDevice, double targetPower, AgentForm existingForm) {
		double duration = Double.valueOf(24 * 60 * 365);
		if (aDevice.getAverageDurationMinutes() > 0) {
			duration = aDevice.getAverageDurationMinutes() * (1 + 0.10 * random.nextGaussian());
		}
		PriorityLevel priority = PriorityLevel.LOW;
		if (aDevice.getPriorityLevel() > 1) {
			priority = PriorityLevel.HIGH;
		}
		boolean activateDateShift = true;
		Date current = activateDateShift ? getCurrentDate() : getCurrentDateBidon();
		AgentType agentType = aDevice.isProducer() ? AgentType.PRODUCER : AgentType.CONSUMER;
		//DeviceCategory deviceCategory = DeviceCategory.getByName(aDevice.getCategory());
		EnvironmentalImpact envImpact = aDevice.getEnvironmentalImpact();
		Date endDate = UtilDates.shiftDateMinutes(current, duration);
		long timeShiftMS = UtilDates.computeTimeShiftMS(datetimeShifts);
		PricingTable pricingTable = new PricingTable(current, endDate, 0);
		AgentForm result = new AgentForm(agentType, "", aDevice.getName(), aDevice.getCategory(), envImpact, pricingTable.getMapPrices(), targetPower, current, endDate,
				priority, duration, timeShiftMS);
		result.updateDeviceProperties(aDevice.getProperties());
		//result.setMapPrices(new HashMap<>());
		if (existingForm != null) {
			result.setAgentName(existingForm.getAgentName());
			result.setId(existingForm.getId());
		}
		return result;
	}

	private static Date selectNextMeasuresDate(boolean takeLastDate) {
		if(deviceMeasures.isEmpty()) {
			return null;
		}
		// For debug
		boolean logMeasures = false;
		if(logMeasures) {
			StringBuffer log = new StringBuffer();
			for(Date nextDate1 : deviceMeasures.keySet()) {
				 List<DeviceMeasure> measures = deviceMeasures.get(nextDate1);
				 log.append(CR + UtilDates.formatTimeOrDate(nextDate1) + " : " + measures.size());
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
		logger.info("selectNextMeasuresDate dateCurrent = " + UtilDates.formatTimeOrDate(dateCurrent) + ", selectedDate = " +   UtilDates.formatTimeOrDate(nextDate));
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
					String deviceCategoryCode = "" + nextDevice.getCategory();
					double avgPower = deviceStatistics.containsKey(deviceCategoryCode) ? coeff*deviceStatistics.get(deviceCategoryCode) : 0;
					double powerTarget = SapereUtil.round(avgPower * (1 + 0*0.05 * random.nextGaussian()),2);
					result.put(nextDevice.getName(), powerTarget);
				}
			}
		}
		return result;
	}

	private static boolean refreshDeviceMeasures() {
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
		boolean takeLastDate = (loopCounter==1); // For the first loop, take the last date
		Date dateNextMeasure = selectNextMeasuresDate(takeLastDate);
		//Date dateNextMeasureMax = SapereUtil.shiftDateMinutes(dateCurrent, 2);
		Date dateNextMeasureMax = UtilDates.shiftDateSec(dateCurrent, 70);
		if(dateNextMeasure==null || dateNextMeasure.after(dateNextMeasureMax)) {
			logger.info("executeLoop : refresh device measures");
			boolean isRefreshOK = refreshDeviceMeasures();
			if(!isRefreshOK) {
				throw new MissingMeasureException("Missing device measure at " + UtilDates.format_date_time.format(dateCurrent));
			}
			dateNextMeasure = selectNextMeasuresDate(takeLastDate);
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
						dateNextMeasure = selectNextMeasuresDate(takeLastDate);
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
			enableSupervision();
		}
		// Check if all current agent devices are contained in the target table
		List<String> listAgentToStop = new ArrayList<>();
		for (AgentForm agent : nodeContent.getAgents()) {
			String deviceName = agent.getDeviceName();
			if(!targetsByDevice.containsKey(deviceName)) {
				// Agent device not contained in target table : send a request to stop the agent
				AgentForm agentForm = nodeContent.getAgentByDeviceName(deviceName);
				listAgentToStop.add(agentForm.getAgentName());
			}
		}
		if(listAgentToStop.size() > 0) {
			boolean resultStop = stopListAgents(listAgentToStop);
			if(!resultStop) {
				for (AgentForm agent : nodeContent.getAgents()) {
					String deviceName = agent.getDeviceName();
					if(!targetsByDevice.containsKey(deviceName)) {
						// Agent device not contained in target table : send a request to stop the agent
						AgentForm agentForm = nodeContent.getAgentByDeviceName(deviceName);
						if(agentForm!=null) {
							stopAgent(agentForm);
						}
					}
				}
			}
		}
		for (Device nextDevice : allDevices) {
			 String deviceName = nextDevice.getName();
			 if(targetsByDevice.containsKey(deviceName)) {
				 if(test_mapNodeConfigByDevice.containsKey(deviceName)) {
					 NodeConfig nodeConfig = test_mapNodeConfigByDevice.get(deviceName);
					 baseUrl = nodeConfig.getUrl();
				 }
				 double powerToSet = targetsByDevice.get(deviceName);
				 AgentForm agentForm = nodeContent.getAgentByDeviceName(deviceName);
				 if(agentForm==null) {
					 // Send a request a create a new agent
					 AgentForm newAgentForm = generateAgentForm(nextDevice, powerToSet, null);
					 addAgent(newAgentForm);
				 } else {
					 // Send a request to modify the agent
					 if( powerToSet == agentForm.getPower()) {
						 // do nothing
						 logger.info("no power change for device " + nextDevice.getName());
					 } else {
						 agentForm = generateAgentForm(nextDevice, powerToSet, agentForm);
						 modifyAgent(agentForm);
					 }
				 }
			} else {
				// Send a request to stop the agent
				AgentForm agentForm = nodeContent.getAgentByDeviceName(deviceName);
				if(agentForm!=null) {
					stopAgent(agentForm);
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
		if(test_mapNodes.size()>0) {
			return TestSimulator.getAllNodesContent(test_mapNodes);
		} else {
			return TestSimulator.getNodeContent();
		}
	}

	private static boolean isStateReached(Date startDate, String variableName, int stateId, boolean obserationUpdated) {
		List<MarkovStateHistory> stateHistory = retrieveLastMarkovHistoryStates(startDate, variableName, obserationUpdated);
		for(MarkovStateHistory nextState : stateHistory ) {
			if(stateId == nextState.getStateId()) {
				return true;
			}
		}
		return false;
	}

	public static void testMarkovStates() {
		InitializationForm initForm = new InitializationForm(scenario, maxTotalPower, datetimeShifts);
		NodeTransitionMatrices nodeTransitionMatrices = getCurrentNodeTransitionMatrices();
		int cpt = 0;
		while(!nodeTransitionMatrices.isComplete() && cpt < 10) {
			String[] listVar = {"consumed","requested", "produced",  "provided", "available", "missing"};
			for(String variable : listVar) {
				if(!isServiceInitialized) {
					initEnergyService(initForm);
				}
				// Refresh node transition matrix
				nodeTransitionMatrices = getCurrentNodeTransitionMatrices();
				if(!nodeTransitionMatrices.isComplete(variable)) {
					testMarkovStates(variable, nodeTransitionMatrices);
				}
			}
			cpt++;
			if(!isServiceInitialized) {
				initEnergyService(initForm);
			}
			nodeTransitionMatrices = getCurrentNodeTransitionMatrices();
		}
	}

	public static boolean checkStateTransition(String tagBegin, Date minCreationDate, String variableName) {
		List<MarkovStateHistory> stateHistory = retrieveLastMarkovHistoryStates(minCreationDate, variableName, true);
		if(stateHistory.size() == 0) {
			return false;
		}
		for(MarkovStateHistory nextStateHiso : stateHistory) {
			if(nextStateHiso.isStationary() && nextStateHiso.getStateId() >= 3 ) {
				logger.warning("### " + tagBegin + " Stationary state : " + nextStateHiso);
			}
		}
		for(MarkovStateHistory nextStateHiso : stateHistory) {
			if(!nextStateHiso.isStationary()) {
				return true;
			} else if(nextStateHiso.getId().intValue() < 4) {
				return true;
			}
		}
		return false;
	}

	public static void testMarkovStates(String variableName, NodeTransitionMatrices nodeTransitionMatrices) {
		InitializationForm initForm = new InitializationForm(scenario, maxTotalPower, datetimeShifts);
		Matrix observations = nodeTransitionMatrices.getAllObsMatrice(variableName);
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
			if(isServiceInitialized) {
				stopEnergyService();
			}
			Date minCreationDate = getCurrentDate();
			initEnergyService(initForm);
			int loopIdx = 0;
			boolean stateRechaed = false;
			while(!(stateRechaed = isStateReached(minCreationDate, variableName, stateId, false) )&& loopIdx < 10)  {
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
				boolean transitionReached = checkStateTransition(tagBegin, minCreationDate, variableName);
				loopIdx = 0;
				while(!transitionReached && loopIdx < 10) {
					try {
						executeLoop(isSupervisionEnabled);
					} catch (Exception e) {
						logger.error(e);
					}
					transitionReached = checkStateTransition(tagBegin, minCreationDate, variableName);
					if(!transitionReached) {
						logger.info(tagBegin + "state transition still not done from state " + stateId);
					}
				}
				if(transitionReached) {
					logger.info("*** testMarkovStates " + variableName + ": state transition done from state " + stateId);
				}
			}
			stopEnergyService();
		}
	}

	public static void doMarkovMatricesCompletion(InitializationForm initForm) {
		Map<Integer,Integer> backupDatetimeShifts = new HashMap<>();
		for(Integer field : datetimeShifts.keySet()) {
			backupDatetimeShifts.put(field, datetimeShifts.get(field));
		}
		int currentHourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		initEnergyService(initForm);
		MatrixFilter matrixFilter = new MatrixFilter();
		List<NodeTransitionMatrices> allNodeTransitionMatrices = retrieveAllNodeTransitionMatrices(matrixFilter);
		for(NodeTransitionMatrices nextNodeTM : allNodeTransitionMatrices) {
			int nextHourOfDay = nextNodeTM.getTimeWindow().getStartHour();
			int nextShiftHourOfDay = nextHourOfDay - currentHourOfDay;
			datetimeShifts.put(Calendar.HOUR_OF_DAY, nextShiftHourOfDay);//
			if(!nextNodeTM.isComplete()) {
				initDates(argTargetDate);
				if(isServiceInitialized) {
					stopEnergyService();
				}
				initForm = new InitializationForm(scenario, maxTotalPower, datetimeShifts);
				if(test_mapNodes.size()>0) {
					for(NodeConfig nodeConfig : test_mapNodes.values()) {
						baseUrl = nodeConfig.getUrl();
						initEnergyService(initForm);
					}
					refreshNodeContent();
				} else {
					initEnergyService(initForm);
				}
				testMarkovStates();
			}
		}
		// After
		if(isServiceInitialized) {
			stopEnergyService();
		}
		datetimeShifts.clear();
		for(Integer field : backupDatetimeShifts.keySet()) {
			datetimeShifts.put(field, backupDatetimeShifts.get(field));
		}
		initDates(argTargetDate);
	}

	public static void main(String args[]) {
		debugLevel = 0;
		if (debugLevel > 0) {
			logger.info("Main " + args);
		}
		boolean completeMarkovMatrices = true;
		completeMarkovMatrices = false; // TO DELETE !!!!!!!!
		datetimeShifts.clear();
		Integer forcedHourOfDay = null;
		int currentHourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		//forcedHourOfDay = 15;
		if(forcedHourOfDay != null) {
			datetimeShifts.put(Calendar.HOUR_OF_DAY, forcedHourOfDay - currentHourOfDay);
		}
		InitializationForm initForm = new InitializationForm(scenario, maxTotalPower, datetimeShifts);
		format_datetime.setTimeZone(TimeZone.getTimeZone("GMT"));
		init(args);
		if(completeMarkovMatrices) {
			doMarkovMatricesCompletion(initForm);
		}
		initForm = new InitializationForm(scenario, maxTotalPower, datetimeShifts);
		boolean isSupervisionEnabled = true;
		if(!isServiceInitialized) {
			if(test_mapNodes.size()>0) {
				for(NodeConfig nodeConfig : test_mapNodes.values()) {
					baseUrl = nodeConfig.getUrl();
					initEnergyService(initForm);
				}
				refreshNodeContent();
			} else {
				initEnergyService(initForm);
			}
		}
		Date startDate = dateCurrent;
		//int waitItNb = 60;
		resetSimulatorLogs();
		boolean retrieveLastContent = false;
		try {
			if (retrieveLastContent) {
				nodeContent = restartLastNodeContent();
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
