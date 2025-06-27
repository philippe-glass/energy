package com.saperetest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeMap;

import com.sapereapi.model.EnergyStorageSetting;
import com.sapereapi.model.PredictionSetting;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.StorageType;
import com.sapereapi.model.energy.input.AgentInputForm;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.input.InitializationForm;
import com.sapereapi.model.learning.LearningModelType;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class StorageSimulator extends TestSimulator {
	static double maxTotalPower = 100*NodeStates.DEFAULT_NODE_MAX_TOTAL_POWER;
	static TreeMap<String, List<Double>> mapPowers  = new TreeMap<String, List<Double>>();
	static int loopIdx = 0;
	static int powerIdx = 0;

	final static int SCENARIO_NO_STORAGE = 1;
	final static int SCENARIO_PRIVATE_STORAGE = 10;
	final static int SCENARIO_PRIVATE_STORAGE_EASY = 12;
	final static int SCENARIO_COMMON_STORAGE = 20;
	final static int SCENARIO_COMMON_STORAGE_EASY = 25;
	final static int SCENARIO_COMMON_STORAGE_CRITICAL = 26;

	static int runSceanario = SCENARIO_COMMON_STORAGE;
	static PricingTable pricingTable = null;
	static boolean useCommonStorage = (runSceanario==SCENARIO_COMMON_STORAGE || runSceanario == SCENARIO_COMMON_STORAGE_EASY || runSceanario == SCENARIO_COMMON_STORAGE_CRITICAL);
	static boolean usePrivateStorage = (runSceanario==SCENARIO_PRIVATE_STORAGE || runSceanario == SCENARIO_PRIVATE_STORAGE_EASY);
	//static double privateStorageCapacity = 20;// 100;
	static double[] privateStorageCapacities = {10, 20, 70};
	static TreeMap<String, Double> mapPrivateStorageCapacity = new TreeMap<String, Double>();
	static double commonStorageCapacity = 200;//500;
	static boolean useAwardCredits = false;
	static String deviceOfHishPriorityDemand = null;

	static {
		mapPowers = initPowerHistory();
		//mapPrivateStorageCapacity = initStorageCapacity();
	}

	/*
	private static TreeMap<String, Double> initStorageCapacity() {
		int agentIndex = 0;
		TreeMap<String, Double> result = new TreeMap<String, Double>();
		for (String deviceName : mapPowers.keySet()) {
			if (deviceName.toLowerCase().contains("local")) {
				agentIndex++;
				if (agentIndex >= privateStorageCapacities.length) {
					agentIndex = 0;
				}
				double nextValue = privateStorageCapacities[agentIndex];
				result.put(deviceName, nextValue);
			}
		}
		return result;
	}*/

	private static TreeMap<String, List<Double>> initPowerHistory() {
		TreeMap<String, List<Double>> result  = new TreeMap<String, List<Double>>();
		/*
		Double[] powersFreeRider1 = {151.0, 130.0, 129.0, 115.0, -13.0, -25.0, -30.0, -75.0, -95.0, 105.0};
		Double[] powersAltruist1  = { -39.0, 47.0, 12.0, -45.0, -49.0, -25.0, -12.0, 10.0, 13.0, 12.0};
		Double[] powersAltruist2  = { -40.0, -42.0, -45.0, -41.0, -30.0, -15.0, 25.0, 46.0, 95.0, 94.0};
		*/
		List<Double> listExternal = new ArrayList<Double>();
		List<Double> listLocal1 = new ArrayList<Double>();
		List<Double> listLocal2 = new ArrayList<Double>();
		List<Double> listLocal3 = new ArrayList<Double>();
		/*
		for(int idx = 0; idx<100; idx++) {
			Double[] powers = {51., -50.};
			listAltruist1.add(powers[idx % (powers.length)]);
			listAltruist2.add(powers[(1+idx) % (powers.length)]);
		}*/
		List<Double> powerCycle = new ArrayList<Double>();
		for(int idxCycle = 0; idxCycle < 8; idxCycle++) {
			double radians = 2*Math.PI * idxCycle/8;
			powerCycle.add(SapereUtil.round(100 * Math.cos(radians) - 0.*40.0, 3));
		}
		int cycleLen = powerCycle.size();
		for (int idx = 0; idx < 100; idx++) {
			// Double[] powers = {150.0, 125.0, 100.0, 75.0, 50.0, 25.0, 0.0, -25.0, -50.0,
			// -100.0};
			int nbDozen = idx / 10;
			//boolean hasShortage = (nbDozen % 5 ) == 1;
			//hasShortage = idx > 0;
			int beginShortage=3;int endShortage=4;
			// TODELETE
			if(runSceanario == SCENARIO_PRIVATE_STORAGE_EASY || runSceanario == SCENARIO_COMMON_STORAGE_EASY) {
				beginShortage=0;endShortage=1;
			}
			// END TOELETE
			boolean hasShortage = (nbDozen >=beginShortage && nbDozen <=endShortage);
			double power2 = hasShortage ? -75.0 : Math.max(-75.0, (15-8*nbDozen)*10.0);
			listExternal.add(hasShortage  ? 0.0001 :  1000.0);
			if(nbDozen > endShortage) {
				listLocal1.add(0.0001);
				listLocal2.add(0.0001);
				listLocal3.add(0.0001);
			} else {
				listLocal1.add(power2 + powerCycle.get(idx % cycleLen));
				listLocal2.add(power2 + powerCycle.get((3 + idx) % cycleLen));
				listLocal3.add(power2 + powerCycle.get((6 + idx) % cycleLen));
			}
		}
		result.put("External", listExternal);		
		result.put("Local-" + (usePrivateStorage ? (int) privateStorageCapacities[0] : 1), listLocal1);
		result.put("Local-" + (usePrivateStorage ? (int) privateStorageCapacities[1] : 2), listLocal2);
		result.put("Local-" + (usePrivateStorage ? (int) privateStorageCapacities[2] : 3), listLocal3);
		mapPrivateStorageCapacity.clear();
		if(usePrivateStorage) {
			for(double privateStorageCapacity : privateStorageCapacities) {
				mapPrivateStorageCapacity.put("Local-" + (int) privateStorageCapacity, privateStorageCapacity);
			}
		}
		//mapPowers.put("FreeRider-1", Arrays.asList(powersFreeRider1)	);
		//mapPowers.put("Altruist-1", Arrays.asList(powersAltruist1)	);
		//mapPowers.put("Altruist-2", Arrays.asList(powersAltruist2)	);
		return result;
	}

	public static void main(String args[]) {
		if (debugLevel > 0) {
			logger.info("Main " + args);
		}
		try {
			init(args);
		} catch (Exception e) {
			logger.error(e);
		}
		String scenario = StorageSimulator.class.getSimpleName();
		InitializationForm initForm = new InitializationForm();
		EnergyStorageSetting globalEnergyStorageSetting = null;
		if(useCommonStorage) {
			if(runSceanario == SCENARIO_COMMON_STORAGE_CRITICAL) {
				commonStorageCapacity = 70;
			}
			int initialStorageWH = runSceanario == SCENARIO_COMMON_STORAGE_EASY ? 1000 : 0;
			globalEnergyStorageSetting = new EnergyStorageSetting(true, true, StorageType.COMMON, commonStorageCapacity, initialStorageWH);			
		}
		initForm.setScenario(scenario);
		initForm.setMaxTotalPower(maxTotalPower);
		initForm.setNodePredicitonSetting(new PredictionSetting(false, null, LearningModelType.MARKOV_CHAINS, 1));
		initForm.setClusterPredictionSetting(new PredictionSetting(false, null, LearningModelType.MARKOV_CHAINS, 1));
		initForm.setActivateAwards(false);
		initForm.setEnergyStorageSetting(globalEnergyStorageSetting);
		initEnergyService(defaultbaseUrl, initForm);
		format_datetime.setTimeZone(TimeZone.getTimeZone("GMT"));
		nodeContent = getNodeContent(defaultbaseUrl);
		pricingTable = new PricingTable(nodeContent.getTimeShiftMS());
		Date current = getCurrentDate();
		//long timeShiftMS = UtilDates.computeTimeShiftMS(datetimeShifts);
		deviceOfHishPriorityDemand = null;
		for (String deviceName : mapPowers.keySet()) {
			List<Double> listPowers = mapPowers.get(deviceName);
			double power = listPowers.get(powerIdx);
			boolean isExternal = deviceName.startsWith("External");
			boolean setHighPriority = false;
			if (runSceanario == SCENARIO_COMMON_STORAGE_CRITICAL && !isExternal && deviceOfHishPriorityDemand == null) {
				deviceOfHishPriorityDemand = deviceName;
				setHighPriority = true;
			}
			AgentInputForm agentInputForm = generateInputForm(null, power, deviceName, setHighPriority);
			addAgent(defaultbaseUrl, agentInputForm);
		}
		Date endOfScenario = UtilDates.shiftDateMinutes(current, 180 * 1);
		//endOfScenario = UtilDates.shiftDateMinutes(current, 0);
		int periodSeconds = 30;
		periodSeconds = 60;
		//periodSeconds = 20;
		while (current.before(endOfScenario)) {
			try {
				Thread.sleep(periodSeconds * 1000);
			} catch (InterruptedException e) {
				logger.error(e);
			}
			applyLoop();
		}
	}

	public static void applyLoop() {
		//Date current = getCurrentDate();
		//double random = Math.random();
		int nbOfValues = mapPowers.values().iterator().next().size();
		loopIdx++;
		powerIdx++;
		if(powerIdx >= nbOfValues) {
			powerIdx = 0;
		}
		/*
		if(loopIdx > 1) {
			return;
		}*/
		try {
			// Refresh node content
			nodeContent = getNodeContent(defaultbaseUrl);
			// Modify the agent
			for(AgentForm nextAgentForm : nodeContent.getAgents()) {
				String deviceName = nextAgentForm.getDeviceName();
				boolean isExternal = deviceName.startsWith("External");
				if(mapPowers.containsKey(deviceName) /*&& !isExternal*/) {
					List<Double> listPowers = mapPowers.get(deviceName);
					double powerBalance = listPowers.get(powerIdx);
					boolean isHighPriority = deviceOfHishPriorityDemand != null && (deviceOfHishPriorityDemand.equals(deviceName));
					if(isHighPriority) {
						logger.info("For debug : isHighPriority");
					}
					AgentInputForm agentInputForm = generateInputForm(nextAgentForm,  powerBalance, deviceName, isHighPriority);
					if(Math.abs(agentInputForm.getPower() - nextAgentForm.getPower()) >= 0.001 || !agentInputForm.getProsumerRole().equals(nextAgentForm.getProsumerRole())) {
						logger.info("applyLoop : before modifyAgent : agentInputForm = " + agentInputForm);
						String baseUrl = defaultbaseUrl;
						modifyAgent(baseUrl, agentInputForm);
					} else {
						logger.info("applyLoop : no change in agent : nextAgentForm = " + nextAgentForm + ", agentInputForm = " + agentInputForm);
					}
				}
			}
			refreshNodeContent(defaultbaseUrl, false);
		} catch (Throwable e) {
			logger.error(e);
		}
	}


	private static AgentInputForm generateInputForm(AgentForm nextAgentForm, double powerBalance, String deviceName, boolean isHishPriority) {
		AgentInputForm agentInputForm  = new AgentInputForm();
		ProsumerRole prosumerRole = powerBalance > 0 ? ProsumerRole.PRODUCER : ProsumerRole.CONSUMER;
		boolean isExternal = deviceName.startsWith("External");
		double durantionMinutes = isExternal ? 30 : 60;
		 durantionMinutes = 5; //.60;
		durantionMinutes = 180;
		Date beginDate = getCurrentDate();
		Date endDate = UtilDates.shiftDateMinutes(beginDate, durantionMinutes);
		if(nextAgentForm == null) {
			EnvironmentalImpact envImpact = isExternal ? EnvironmentalImpact.MEDIUM : EnvironmentalImpact.LOW;
			DeviceCategory category = powerBalance > 0 ? DeviceCategory.BIOMASS_ENG : DeviceCategory.COOKING;
			double storageCapacityWH = 0;
			if (usePrivateStorage && !isExternal && mapPrivateStorageCapacity.containsKey(deviceName)) {
				storageCapacityWH = mapPrivateStorageCapacity.get(deviceName);
			}
			//double storageCapacityWH = (isExternal || !usePrivateStorage) ? 0 : privateStorageCapacity;
			double initialStorageWH = (runSceanario == SCENARIO_PRIVATE_STORAGE_EASY) ? 100 : 0;
			EnergyStorageSetting privateEnergyStorageSetting = new EnergyStorageSetting(
					true, true, StorageType.PRIVATE, storageCapacityWH, initialStorageWH);
			category = isExternal ? DeviceCategory.EXTERNAL_ENG :  DeviceCategory.HYBRID;
			long timeShiftMS = UtilDates.computeTimeShiftMS(datetimeShifts);
			agentInputForm = new AgentInputForm(prosumerRole, "", deviceName, category,
					envImpact, pricingTable.getMapPrices(), Math.abs(powerBalance), beginDate, endDate, PriorityLevel.MEDIUM,
					durantionMinutes, privateEnergyStorageSetting, timeShiftMS);
		} else {
			agentInputForm = nextAgentForm.generateInputForm();
			ProsumerRole prosumerRoleBefore = nextAgentForm.getProsumerRole();
			if (prosumerRoleBefore != null && !prosumerRole.equals(prosumerRoleBefore)) {
				logger.info(
						agentInputForm.getDeviceName() + " : switch from " + prosumerRoleBefore + " to " + prosumerRole);
			}
			agentInputForm.setProsumerRole(prosumerRole);
			agentInputForm.setPower(Math.abs(powerBalance));
			agentInputForm.setBeginDate(beginDate);
			agentInputForm.setEndDate(endDate);
			agentInputForm.setDelayToleranceMinutes(durantionMinutes);
		}
		if(isHishPriority) {
			agentInputForm.setPriorityLevel(PriorityLevel.HIGH);
		}
		agentInputForm.setUseAwardCredits(useAwardCredits);
		return agentInputForm;
	}

}
