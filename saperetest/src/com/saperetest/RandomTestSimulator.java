package com.saperetest;

import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

import com.sapereapi.model.EnergyStorageSetting;
import com.sapereapi.model.PredictionSetting;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.Device;
import com.sapereapi.model.energy.node.NodeTotal;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.energy.input.AgentInputForm;
import com.sapereapi.model.input.InitializationForm;
import com.sapereapi.model.learning.LearningModelType;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.util.UtilDates;

public class RandomTestSimulator extends TestSimulator {
	static double maxTotalPower = NodeStates.DEFAULT_NODE_MAX_TOTAL_POWER;
	public static void main(String args[]) {
		if (debugLevel > 0) {
			logger.info("Main " + args);
		}
		try {
			init(args);
		} catch (Exception e) {
			logger.error(e);
		}
		String scenario = RandomTestSimulator.class.getSimpleName();
		EnergyStorageSetting energyStorageSetting = new EnergyStorageSetting();
		PredictionSetting nodePredictionSetting = new PredictionSetting(false, null, LearningModelType.MARKOV_CHAINS, 1);
		PredictionSetting clusterPredictionSetting = new PredictionSetting(false, null, LearningModelType.MARKOV_CHAINS, 1);
		InitializationForm initForm = new InitializationForm(scenario, maxTotalPower, datetimeShifts, energyStorageSetting, nodePredictionSetting, clusterPredictionSetting);
		initEnergyService(defaultbaseUrl, initForm);

		format_datetime.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date current = getCurrentDate();
		boolean retrieveLastContent = false;
		if (retrieveLastContent) {
			nodeContent = restartLastNodeContent(defaultbaseUrl);
		} else {
			long timeShiftMS = UtilDates.computeTimeShiftMS(datetimeShifts);
			PricingTable pricingTable = new PricingTable(nodeContent.getTimeShiftMS());
			// Add producer agent
			long duration = 24 * 60 * 365;
			addAgent(defaultbaseUrl,
					new AgentInputForm(ProsumerRole.PRODUCER, "", "SIG", DeviceCategory.EXTERNAL_ENG, EnvironmentalImpact.MEDIUM, pricingTable.getMapPrices(), 31.0, current
					,UtilDates.shiftDateMinutes(current, 24 * 60 * 365)
					, PriorityLevel.LOW, 24. * 60 * 365, energyStorageSetting, timeShiftMS));
			addAgent(defaultbaseUrl
					, new AgentInputForm(ProsumerRole.PRODUCER, "", "Wind Turbin-1", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, pricingTable.getMapPrices(), 18.,current
					,UtilDates.shiftDateMinutes(current, 60)
					, PriorityLevel.LOW, 24. * 60 * 365, energyStorageSetting, timeShiftMS));
			// Add consumer agent
			addAgent(defaultbaseUrl
					, new AgentInputForm(ProsumerRole.CONSUMER, "", "Refrigerator", DeviceCategory.COLD_APPLIANCES, EnvironmentalImpact.MEDIUM, pricingTable.getMapPrices(), 23.1, current
					,UtilDates.shiftDateMinutes(current, 24 * 60 * 365),
					PriorityLevel.HIGH, 24. * 60 * 365, energyStorageSetting, timeShiftMS));
			/*
			 * addAgent( new AgentForm(AgentType.CONSUMER, "", new Float(2000), current,
			 * SapereUtil.shiftDateMinutes(current, new Float(60)) , PriorityLevel.LOW, new
			 * Float(60)) );
			 */
			nodeContent = getNodeContent(defaultbaseUrl);
		}
		Date end = UtilDates.shiftDateMinutes(current, 180 * 1);
		while (current.before(end)) {
			current = getCurrentDate();
			double random = Math.random();
			try {
				if (random < 10*0.1) {
					// Refresh node content
					nodeContent = getNodeContent(defaultbaseUrl);
					NodeTotal nodeTotal = nodeContent.getTotal();
					double prodMargin = Math.max(0, maxTotalPower - nodeTotal.getProduced());
					double prodDiscountFactor = prodMargin / maxTotalPower;
					double requestMargin = Math.max(0,1.5 * maxTotalPower - nodeTotal.getRequested());
					double requestDiscountFactor = requestMargin / ( 1.5 *maxTotalPower);

					// create random object
					Random r = new Random();
					double randomIsProducer = Math.random();
					boolean isProducer = (randomIsProducer < 0.4);
					ProsumerRole prosumerRole = isProducer ? ProsumerRole.PRODUCER : ProsumerRole.CONSUMER;
					// Generate random agent
					double discountFactor = (isProducer ? prodDiscountFactor : requestDiscountFactor);
					double amp1 =  discountFactor * 0.1 * maxTotalPower;
					double power = amp1 * Math.abs(r.nextGaussian());
					power = ((Math.round(power * 100))) / 100;
					if (power < 0.01) {
						// Do not accept very small powers
						logger.info("For debug : value = " + power);
					} else {
						double durationMinutes = 0 + 60 * Math.abs(r.nextGaussian());
						Date endDate = UtilDates.shiftDateMinutes(current, durationMinutes);
						AgentInputForm agentForm = new AgentInputForm();
						agentForm.setPowers(power, power, power);
						agentForm.setBeginDate(current);
						agentForm.setEndDate(endDate);
						agentForm.setDuration(durationMinutes);
						agentForm.setDelayToleranceMinutes(durationMinutes);
						agentForm.setEnvironmentalImpact(EnvironmentalImpact.LOW);
						agentForm.setProsumerRole(prosumerRole);
						agentForm.setDeviceName("");
						if (ProsumerRole.CONSUMER.equals(prosumerRole)) {
							double rPriority = Math.random();
							PriorityLevel priority = (rPriority < 0.1) ? PriorityLevel.HIGH : PriorityLevel.LOW;
							// : (rPriority < 0.3 ? PriorityLevel.MEDIUM : PriorityLevel.LOW);
							priority = PriorityLevel.LOW;
							agentForm.setPriorityLevel(priority);
							Device chosenDevice = chooseSleepingDevice(nodeContent, false, null, power / 1.5, power * 1.5, null);
							if (chosenDevice != null) {
								agentForm.setDeviceName(chosenDevice.getName());
								agentForm.setDeviceCategory(chosenDevice.getCategory());
							} else {
								agentForm.setDeviceCategory(DeviceCategory.UNKNOWN);
								agentForm.setDeviceName("Not-identified");
								logger.info(" device is null");
							}
						} else {
							int nb = 1+nodeContent.getProducers().size();
							agentForm.setDeviceName("Wind turbin #" + nb);
							agentForm.setDeviceCategory(DeviceCategory.WIND_ENG);
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
						}
						refreshNodeContent(defaultbaseUrl, false);
					}
				}
				Thread.sleep(1000);
			} catch (Throwable e) {
				logger.error(e);
			}
		}
	}

}
