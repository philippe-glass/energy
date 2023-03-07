package com.sapereapi.energy.test;

import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

import com.sapereapi.model.InitializationForm;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.Device;
import com.sapereapi.model.energy.NodeTotal;
import com.sapereapi.model.energy.PricingTable;
import com.sapereapi.model.markov.NodeMarkovStates;
import com.sapereapi.model.referential.AgentType;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.util.UtilDates;

public class RandomTestSimulator extends TestSimulator {
	static double maxTotalPower = NodeMarkovStates.DEFAULT_NODE_MAX_TOTAL_POWER;
	public static void main(String args[]) {
		if (debugLevel > 0) {
			logger.info("Main " + args);
		}
		init(args);
		String scenario = RandomTestSimulator.class.getSimpleName();
		InitializationForm initForm = new InitializationForm();
		initForm.setScenario(scenario);
		initForm.setMaxTotalPower(maxTotalPower);
		initEnergyService(initForm);

		format_datetime.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date current = getCurrentDate();
		boolean retrieveLastContent = false;
		if (retrieveLastContent) {
			nodeContent = restartLastNodeContent();
		} else {
			long timeShiftMS = UtilDates.computeTimeShiftMS(datetimeShifts);
			PricingTable pricingTable = new PricingTable();
			// Add producer agent
			addAgent(new AgentForm(AgentType.PRODUCER, "", "EDF", DeviceCategory.EXTERNAL_ENG, EnvironmentalImpact.MEDIUM, pricingTable.getMapPrices(), 31.0, current
					,UtilDates.shiftDateMinutes(current, 24 * 60 * 365), timeShiftMS));
			addAgent(new AgentForm(AgentType.PRODUCER, "", "Wind Turbin-1", DeviceCategory.WIND_ENG, EnvironmentalImpact.LOW, pricingTable.getMapPrices(), 18.,current
					,UtilDates.shiftDateMinutes(current, 60), timeShiftMS));
			// Add consumer agent
			addAgent(new AgentForm(AgentType.CONSUMER, "", "Refrigerator", DeviceCategory.COLD_APPLIANCES, EnvironmentalImpact.MEDIUM, pricingTable.getMapPrices(), 23.1, current
					,UtilDates.shiftDateMinutes(current, 24 * 60 * 365),
					PriorityLevel.HIGH, 24. * 60 * 365, timeShiftMS));
			/*
			 * addAgent( new AgentForm(AgentType.CONSUMER, "", new Float(2000), current,
			 * SapereUtil.shiftDateMinutes(current, new Float(60)) , PriorityLevel.LOW, new
			 * Float(60)) );
			 */
			nodeContent = getNodeContent();
		}
		Date end = UtilDates.shiftDateMinutes(current, 180 * 1);
		while (current.before(end)) {
			current = getCurrentDate();
			double random = Math.random();
			try {
				if (random < 10*0.1) {
					// Refresh node content
					nodeContent = getNodeContent();
					NodeTotal nodeTotal = nodeContent.getTotal();
					double prodMargin = Math.max(0, maxTotalPower - nodeTotal.getProduced());
					double prodDiscountFactor = prodMargin / maxTotalPower;
					double requestMargin = Math.max(0,1.5 * maxTotalPower - nodeTotal.getRequested());
					double requestDiscountFactor = requestMargin / ( 1.5 *maxTotalPower);

					// create random object
					Random r = new Random();
					double randomIsProducer = Math.random();
					boolean isProducer = (randomIsProducer < 0.4);
					AgentType agentType = isProducer ? AgentType.PRODUCER : AgentType.CONSUMER;
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
						AgentForm agentForm = new AgentForm();
						agentForm.setPowers(power, power, power);
						agentForm.setBeginDate(current);
						agentForm.setEndDate(endDate);
						agentForm.setDuration(durationMinutes);
						agentForm.setDelayToleranceMinutes(durationMinutes);
						agentForm.setAgentType(agentType.getLabel());
						agentForm.setDeviceName("");
						if (AgentType.CONSUMER.equals(agentType)) {
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
						int nbActiveAgents = nodeContent.getNbActiveAgents(agentType);
						double probaDoOperation = Math.exp(-0.1 * nbActiveAgents);
						if (Math.random() < probaDoOperation) {
							// restart agent
							int nbExpiredAgents = nodeContent.getNbExpiredAgents(agentType);
							double probaAddNewAgent = Math.exp(-0.5 * nbExpiredAgents);
							if (Math.random() < probaAddNewAgent) {
								addAgent(agentForm);
							} else {
								AgentForm agentFormToRestart = isProducer ? nodeContent.getRandomInactiveProducer()
										: nodeContent.getRandomInactiveConsumer();
								agentForm.setAgentName(agentFormToRestart.getAgentName());
								agentForm.setDeviceName(agentFormToRestart.getDeviceName());
								agentForm.setDeviceCategory(agentFormToRestart.getDeviceCategory());
								agentForm.setId(agentFormToRestart.getId());
								restartAgent(agentForm);
							}
						}
						refreshNodeContent();
					}
				}
				Thread.sleep(1000);
			} catch (Throwable e) {
				e.printStackTrace();
				logger.error(e);
			}
		}
	}

}
