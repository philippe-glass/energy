package com.saperetest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeMap;

import com.sapereapi.model.PredictionSetting;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.energy.input.AgentInputForm;
import com.sapereapi.model.energy.policy.PolicyFactory;
import com.sapereapi.model.input.InitializationForm;
import com.sapereapi.model.learning.LearningModelType;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class AwardsSimulator extends TestSimulator {
	static double maxTotalPower = NodeStates.DEFAULT_NODE_MAX_TOTAL_POWER;
	static TreeMap<String, List<Double>> mapPowers  = new TreeMap<String, List<Double>>();
	static int loopIdx = 0;
	static int powerIdx = 0;
	static PricingTable pricingTable = null;
	static {
		mapPowers  = new TreeMap<String, List<Double>>();
		/*
		Double[] powersFreeRider1 = {151.0, 130.0, 129.0, 115.0, -13.0, -25.0, -30.0, -75.0, -95.0, 105.0};
		Double[] powersAltruist1  = { -39.0, 47.0, 12.0, -45.0, -49.0, -25.0, -12.0, 10.0, 13.0, 12.0};
		Double[] powersAltruist2  = { -40.0, -42.0, -45.0, -41.0, -30.0, -15.0, 25.0, 46.0, 95.0, 94.0};
		*/
		List<Double> listFreeRider = new ArrayList<Double>();
		List<Double> listAltruist1 = new ArrayList<Double>();
		List<Double> listAltruist2 = new ArrayList<Double>();
		/*
		for(int idx = 0; idx<100; idx++) {
			Double[] powers = {51., -50.};
			listAltruist1.add(powers[idx % (powers.length)]);
			listAltruist2.add(powers[(1+idx) % (powers.length)]);
		}*/
		List<Double> powerCycle = new ArrayList<Double>();
		for(int idxCycle = 0; idxCycle < 8; idxCycle++) {
			double radians = 2*Math.PI * idxCycle/8;
			powerCycle.add(SapereUtil.round(100 * Math.cos(radians) + 25, 3));
		}
		int cycleLen = powerCycle.size();
		for (int idx = 0; idx < 100; idx++) {
			// Double[] powers = {150.0, 125.0, 100.0, 75.0, 50.0, 25.0, 0.0, -25.0, -50.0,
			// -100.0};
			listFreeRider.add(powerCycle.get(idx % cycleLen));
			listAltruist1.add(powerCycle.get((3 + idx) % cycleLen));
			listAltruist2.add(powerCycle.get((6 + idx) % cycleLen));
		}
		mapPowers.put("FreeRider-1", listFreeRider);
		mapPowers.put("Altruist-1", listAltruist1);
		mapPowers.put("Altruist-2", listAltruist2);
		//mapPowers.put("FreeRider-1", Arrays.asList(powersFreeRider1)	);
		//mapPowers.put("Altruist-1", Arrays.asList(powersAltruist1)	);
		//mapPowers.put("Altruist-2", Arrays.asList(powersAltruist2)	);
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
		String scenario = AwardsSimulator.class.getSimpleName();
		InitializationForm initForm = new InitializationForm();
		initForm.setScenario(scenario);
		initForm.setMaxTotalPower(maxTotalPower);
		initForm.setNodePredicitonSetting(new PredictionSetting(false, null, LearningModelType.MARKOV_CHAINS));
		initForm.setClusterPredictionSetting(new PredictionSetting(false, null, LearningModelType.MARKOV_CHAINS));
		initForm.setActivateAwards(true);
		initForm.setActivateEnergyStorage(false);
		initEnergyService(defaultbaseUrl, initForm);
		format_datetime.setTimeZone(TimeZone.getTimeZone("GMT"));
		nodeContent = getNodeContent(defaultbaseUrl);
		pricingTable = new PricingTable(nodeContent.getTimeShiftMS());
		Date current = getCurrentDate();
		long timeShiftMS = UtilDates.computeTimeShiftMS(datetimeShifts);
		for (String agentName : mapPowers.keySet()) {
			List<Double> listPowers = mapPowers.get(agentName);
			double power = listPowers.get(powerIdx);
			boolean isFreeRider = agentName.startsWith("Free");
			ProsumerRole prosumerRole = power > 0 ? ProsumerRole.PRODUCER : ProsumerRole.CONSUMER;
			EnvironmentalImpact envImpact = isFreeRider ? EnvironmentalImpact.MEDIUM : EnvironmentalImpact.LOW;
			DeviceCategory category = power > 0 ? DeviceCategory.BIOMASS_ENG : DeviceCategory.COOKING;
			category = DeviceCategory.HYBRID;
			double durantionMinutes = 60;
			Date endDate = UtilDates.shiftDateMinutes(current, durantionMinutes);
			AgentInputForm inputForm = new AgentInputForm(prosumerRole, "", agentName, category,
					envImpact, pricingTable.getMapPrices(), Math.abs(power), current, endDate, PriorityLevel.MEDIUM,
					durantionMinutes, timeShiftMS);
			if (isFreeRider) {
				inputForm.setProducerPolicyId(PolicyFactory.POLICY_FREE_RIDING);
			} else {
				inputForm.setProducerPolicyId(PolicyFactory.POLICY_ALTRUIST);
			}
			inputForm.setUseAwardCredits(true);
			addAgent(defaultbaseUrl, inputForm);
		}
		Date endOfScenario = UtilDates.shiftDateMinutes(current, 180 * 1);
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
			/*
			NodeTotal nodeTotal = nodeContent.getTotal();
			double prodMargin = Math.max(0, maxTotalPower - nodeTotal.getProduced());
			double prodDiscountFactor = prodMargin / maxTotalPower;
			double requestMargin = Math.max(0,1.5 * maxTotalPower - nodeTotal.getRequested());
			double requestDiscountFactor = requestMargin / ( 1.5 *maxTotalPower);
			*/

			// create random object
			//Random r = new Random();

			// Modify the agent
			for(AgentForm nextAgentForm : nodeContent.getAgents()) {
				AgentInputForm agentInputForm = nextAgentForm.generateInputForm();
				String agentName = nextAgentForm.getDeviceName();
				if(mapPowers.containsKey(agentName)) {
					List<Double> listPowers = mapPowers.get(agentName);
					double powerBalance = listPowers.get(powerIdx);
					//double randomIsProducer = Math.random();
					//boolean isProducer = (randomIsProducer < 0.4);
					//double discountFactor = (isProducer ? prodDiscountFactor : requestDiscountFactor);
					//double amp1 =  discountFactor * 0.1 * maxTotalPower;
					//double power = amp1 * Math.abs(r.nextGaussian());
					//power = ((Math.round(power * 100))) / 100;
					ProsumerRole prosumerRoleBefore = nextAgentForm.getProsumerRole();
					ProsumerRole prosumerRole = powerBalance > 0 ? ProsumerRole.PRODUCER : ProsumerRole.CONSUMER;
					if(!prosumerRole.equals(prosumerRoleBefore)) {
						logger.info(agentName + " : switch from " + prosumerRoleBefore + " to " +  prosumerRole);
					}
					String baseUrl = defaultbaseUrl;
					agentInputForm.setProsumerRole(prosumerRole);
					agentInputForm.setPower(Math.abs(powerBalance));
					double durantionMinutes = 5; //.60;
					Date beginDate = getCurrentDate();
					Date endDate = UtilDates.shiftDateMinutes(beginDate, durantionMinutes);
					agentInputForm.setBeginDate(beginDate);
					agentInputForm.setEndDate(endDate);
					//agentInputForm.setDelayToleranceRatio(1.0);
					agentInputForm.setDelayToleranceMinutes(durantionMinutes);
					logger.info("applyLoop : before modifyAgent : agentInputForm = " + agentInputForm);
					modifyAgent(baseUrl, agentInputForm);
				}
			}
			/*
			double durationMinutes = 0 + 60 * Math.abs(r.nextGaussian());
			Date endDate = UtilDates.shiftDateMinutes(current, durationMinutes);
			AgentInputForm agentForm = new AgentInputForm();
			agentForm.setPowers(power, power, power);
			agentForm.setBeginDate(current);
			agentForm.setEndDate(endDate);
			agentForm.setDuration(durationMinutes);
			agentForm.setDelayToleranceMinutes(durationMinutes);
			agentForm.setEnvironmentalImpact(EnvironmentalImpact.LOW.getLevel());
			agentForm.setProsumerRole(prosumerRole);
			agentForm.setDeviceName("");
			if (ProsumerRole.CONSUMER.equals(prosumerRole)) {
				double rPriority = Math.random();
				PriorityLevel priority = (rPriority < 0.1) ? PriorityLevel.HIGH : PriorityLevel.LOW;
				// : (rPriority < 0.3 ? PriorityLevel.MEDIUM : PriorityLevel.LOW);
				priority = PriorityLevel.LOW;
				agentForm.setPriorityLevel(priority.getLabel());
				Device chosenDevice = chooseSleepingDevice(nodeContent, false, null, power / 1.5, power * 1.5, null);
				if (chosenDevice != null) {
					agentForm.setDeviceName(chosenDevice.getName());
					agentForm.setDeviceCategory(chosenDevice.getCategory().getOptionItem());
				} else {
					agentForm.setDeviceCategory(DeviceCategory.UNKNOWN.getOptionItem());
					agentForm.setDeviceName("Not-identified");
					logger.info(" device is null");
				}
			} else {
				int nb = 1+nodeContent.getProducers().size();
				agentForm.setDeviceName("Wind turbin #" + nb);
				agentForm.setDeviceCategory(DeviceCategory.WIND_ENG.getOptionItem());
			}
			int nbActiveAgents = nodeContent.getNbActiveAgents(prosumerRole);
			double probaDoOperation = Math.exp(-0.1 * nbActiveAgents);
			if (Math.random() < probaDoOperation) {
				// restart agent
				int nbExpiredAgents = nodeContent.getNbExpiredAgents(prosumerRole);
				double probaAddNewAgent = Math.exp(-0.5 * nbExpiredAgents);
				if (Math.random() < probaAddNewAgent) {
					addAgent(defaultbaseUrl, agentForm);
				} else {
					AgentForm agentFormToRestart = isProducer ? nodeContent.getRandomInactiveProducer()
							: nodeContent.getRandomInactiveConsumer();
					agentForm.setAgentName(agentFormToRestart.getAgentName());
					agentForm.setDeviceName(agentFormToRestart.getDeviceName());
					agentForm.setDeviceCategory(agentFormToRestart.getDeviceCategory());
					agentForm.setId(agentFormToRestart.getId());
					restartAgent(defaultbaseUrl, agentForm);
				}
			}*/
			refreshNodeContent(defaultbaseUrl, false);
		} catch (Throwable e) {
			logger.error(e);
		}
	}
}
