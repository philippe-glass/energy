package com.sapereapi.energy.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.sapereapi.model.InitializationForm;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.Device;
import com.sapereapi.model.energy.NodeContent;
import com.sapereapi.model.energy.NodeTotal;
import com.sapereapi.model.energy.PricingTable;
import com.sapereapi.model.energy.SimulatorLog;
import com.sapereapi.model.markov.NodeMarkovStates;
import com.sapereapi.model.referential.AgentType;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class HomeSimulator extends TestSimulator {

	static Map<String, Double> deviceStatistics = new HashMap<String, Double>();
	static int loopCounter = 0;
	final static String CR = System.getProperty("line.separator");
	static SimulatorLog simulatorLog = null;
	// Define device category filter
	static DeviceCategory[] categoryFilter = new DeviceCategory[] {};

	static List<String> updatedDevices = new ArrayList<String>();
	static List<String> stoppedDevices = new ArrayList<String>();
	static List<String> startedDevices = new ArrayList<String>();
	static double maxTotalPower = NodeMarkovStates.DEFAULT_NODE_MAX_TOTAL_POWER;
	static NodeContent partialNodeContent = null;
	//static Integer shiftHourOfDay = null;

	private static AgentForm generateAgentForm(Device aDevice, double targetPower, AgentForm existingForm) {
		double duration = Double.valueOf(24 * 60 * 365);
		if (aDevice.getAverageDurationMinutes() > 0) {
			duration = aDevice.getAverageDurationMinutes() * (1 + 0.10 * random.nextGaussian());
		}
		PriorityLevel priority = PriorityLevel.LOW;
		if (aDevice.getPriorityLevel() > 1) {
			priority = PriorityLevel.HIGH;
		}
		double power = aDevice.getPowerMin();
		if (targetPower > 0 && targetPower >= aDevice.getPowerMin() && targetPower <= aDevice.getPowerMax()) {
			power = (float) targetPower;
		} else {
			double powerRandom = Math.random();
			power = aDevice.getPowerMin() + (float) (powerRandom * (aDevice.getPowerMax() - aDevice.getPowerMin()));
		}
		if (!aDevice.isProducer()) {
			// power = (float) 0.1 * power;
		}
		Date current = getCurrentDate();
		AgentType agentType = aDevice.isProducer() ? AgentType.PRODUCER : AgentType.CONSUMER;
		DeviceCategory deviceCategory = aDevice.getCategory();
		// DeviceCategory test = DeviceCategory.getByName("WASHING_DRYING");
		Date endDate = UtilDates.shiftDateMinutes(current, duration);
		long timeShiftMS = UtilDates.computeTimeShiftMS(datetimeShifts);
		PricingTable pricingTable = new PricingTable();//new PricingTable(current, endDate, 0);
		AgentForm result = new AgentForm(agentType, "", aDevice.getName(), deviceCategory, aDevice.getEnvironmentalImpact(), pricingTable.getMapPrices(), power, current, endDate,
				priority, duration, timeShiftMS);
		if (existingForm != null) {
			result.setAgentName(existingForm.getAgentName());
			result.setId(existingForm.getId());
		}
		return result;
	}

	private static double computeCurrentCategoryPower(String deviceCategory, boolean isProducer) {
		partialNodeContent = getPartialNodeContent(deviceCategory, isProducer);
		return aux_computeCurrentCategoryPower(partialNodeContent, deviceCategory, isProducer);
	}

	private static double aux_computeCurrentCategoryPower(NodeContent nodeContent, String deviceCategory, boolean isProducer) {
		if(nodeContent==null) {
			logger.warning("Loop #" + loopCounter + " " + deviceCategory + " computeCurrentCategoryPower : nodeContent is null");
			return 0;
		}
		NodeTotal nodeTotal = nodeContent.getPartialTotal(deviceCategory);
		if(nodeTotal==null) {
			return 0;
		}
		return isProducer? nodeTotal.getProduced().doubleValue() : nodeTotal.getRequested().doubleValue();
	}

	private static boolean isMarginSufficient(List<Device> devices, double powerTarget) {
		return getPowerMin(devices) <= powerTarget && getPowerMax(devices) >= powerTarget;
	}

	public static void adjustRunningDevices(boolean isProducer, String category, double powerTarget, List<String> excludedDevices) {
		if(debugLevel>0) {
			logger.info("Loop #" + loopCounter + " " + category + "  adjustExistingPowers begin");
		}
		AgentForm agentForm = null;
		//List<Device> result = new ArrayList<Device>();
		partialNodeContent = getPartialNodeContent(category, isProducer);
		double currentPower = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
		double powerTargetMin = powerTarget * 0.99;
		double powerTargetMax = powerTarget * 1.01;
		double toCorrect = powerTarget - currentPower;
		List<Device> devices = getDevices(partialNodeContent, isProducer, category, Device.STATUS_RUNNING, 0, powerTargetMax, excludedDevices);
		Collections.shuffle(devices);
		List<Device> chosenDevices = new ArrayList<>();
		if(!isMarginSufficient(devices, powerTarget)) {
			logger.info(loopCounter + " " + category + " : adjustExistingPowers : not enough margin left to reach target "
					+ UtilDates.df.format(powerTarget)
					+ " from " + currentPower);
			logger.info(" ( Existing devices : " + devices + " )" );
			chosenDevices = devices;
		} else {
			for(Device nextDevice : devices) {
				if(!isMarginSufficient(chosenDevices, powerTarget)) {
					chosenDevices.add(nextDevice);
				} else {
					break;
				}
			}
		}
		for (Device nextDevice : chosenDevices) {
			if (currentPower < powerTargetMin  || currentPower > powerTargetMax) {
				toCorrect = powerTarget - currentPower;
				if (toCorrect != 0) {
					agentForm = partialNodeContent.getAgentByDeviceName(nextDevice.getName());
					if(agentForm != null && aux_isRuinning(nextDevice, partialNodeContent.getMapRunningAgents())) {
					//if(agentForm != null && agentForm.getIsSatisfied() != null && agentForm.getIsSatisfied()) {
						double powerToSet = SapereUtil.round(agentForm.getPower() + toCorrect,2);
						if (powerToSet > nextDevice.getPowerMax()) {
							powerToSet = nextDevice.getPowerMax();
						}
						if (powerToSet < nextDevice.getPowerMin()) {
							powerToSet = nextDevice.getPowerMin();
						}
						if (Math.abs(powerToSet - agentForm.getPower()) >= 0.001) {
							double lastTotal = aux_computeCurrentCategoryPower(partialNodeContent,category, isProducer);
							// Modify agent
							double powerBefore = agentForm.getPower();
							agentForm = generateAgentForm(nextDevice, powerToSet, agentForm);
							logger.info("Loop #" + loopCounter + " " + category + " adjustExistingPowers modify " + agentForm.getAgentName() + " : " + powerBefore + " -> " + powerToSet);
							modifyAgent(agentForm);
							// Refresh currentTotal
							partialNodeContent.getAgentByDeviceName(nextDevice.getName());
							partialNodeContent = getPartialNodeContent(category, isProducer);
							double newTotal = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
							if(Math.abs(newTotal - lastTotal) < 0.001) {
								logger.warning("Loop #" + loopCounter + " " + category + " adjustExistingPowers : same total before and after modifyAgent");
							} else {
								updatedDevices.add(nextDevice.getName());
							}
							logger.info("Loop #" + loopCounter + " " + category + " adjustExistingPowers : lastTotal = " + lastTotal + ", toAdd= " + toCorrect + ", new total = " + newTotal);
						}
					}
				}
			}
		}
		if (currentPower >= powerTargetMin && currentPower <= powerTargetMax) {
			logger.info("Loop #" + loopCounter + " " + category + "  adjustExistingPowers success ");
		}
		if(debugLevel>0) {
			logger.info("Loop #" + loopCounter + " " + category + "  adjustExistingPowers end");
		}
	}

	public static void stopExistingDevices(boolean isProducer, String category, double powerTarget, boolean applyMaxFilter) {
		AgentForm agentForm = null;
		//List<String> removedDevices = new ArrayList<String>();
		//refreshNodeContent();
		partialNodeContent= getPartialNodeContent(category, isProducer);
		double currentPower = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
		int nbTry = 0;
		double powerTargetMin = powerTarget * 0.95;
		logger.info("#loop "+ loopCounter + " "  + category + " stopExistingDevices target = " + UtilDates.df.format(powerTarget) +  " current "+ currentPower);
		while (currentPower > powerTarget
				&& (agentForm = chooseRunningAgent(partialNodeContent, isProducer, category, 0, applyMaxFilter ? currentPower - powerTargetMin : 0,  stoppedDevices)) != null
				&& nbTry < 100) {
			logger.info("Loop #" + loopCounter + " " + category + " stop device" + agentForm.getDeviceName()
					+ "(" + agentForm.getPower() + "W) " + " : current = " + currentPower
					+ " target : " + powerTarget);
			double lastTotal = currentPower;
			if (!applyMaxFilter || (currentPower - agentForm.getPower() >= powerTargetMin)) {
				auxStopAgent(agentForm);
				//currentTotal =  getCurrentCategoryPower(nodeContent, category, isProducer);
				partialNodeContent = getPartialNodeContent(category, isProducer);
				double newTotal = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
				if(Math.abs(lastTotal - newTotal) < 0.001) {
					logger.warning("Loop #" + loopCounter + " " + category + " stopExistingDevices stop device '" + agentForm.getDeviceName() + "' did not work");
				} else {
					// It worked
					stoppedDevices.add(agentForm.getDeviceName());
					logger.info("Loop #" + loopCounter + " " + category + " stopExistingDevices stop device '" + agentForm.getDeviceName() + " OK");
				}
			} else {
				logger.warning("Step456 : stopExistingDevices : chosen agentForm is KO ");
			}
			nbTry++;
		}
	}

	public static boolean auxStartDevice(boolean isProducer, Device nextDevice, double powerToSet) {
		String category = ""+nextDevice.getCategory();
		if(!SapereUtil.checkIsRound(powerToSet, 2, logger)) {
			logger.warning("auxStartDevice powerToSet not rounded !!! powerToSet = " + powerToSet);
		}
		nextDevice.setCurrentPower((float) powerToSet);
		boolean result = false;
		partialNodeContent = getPartialNodeContent(category, isProducer);
		double currentPower = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
		if (partialNodeContent.hasDevice(nextDevice.getName())) {
			// Device already contained in node content but disabled : we have to restart it
			AgentForm agentForm = partialNodeContent.getAgentByDeviceName(nextDevice.getName());
			agentForm = generateAgentForm(nextDevice, powerToSet, agentForm);
			double totalBefore = currentPower;
			agentForm = restartAgent(agentForm);
			partialNodeContent = getPartialNodeContent(category, isProducer);
			currentPower = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
			if(Math.abs(currentPower - totalBefore) < 0.001) {
				logger.warning("loop #" + loopCounter + " " + category + " auxStartDevice restartAgent " + agentForm.getDeviceName() + " did not work ");
			} else {
				result = true;
			}
			//logger.info("loop #" + loopCounter + " " + category + " auxStartDevice restartAgent W=" + agentForm.getPower() + " " + agentForm.getDeviceName());
		} else {
			// Device not contained in node content : we have to start it
			AgentForm agentForm = generateAgentForm(nextDevice, powerToSet, null);
			double totalBefore = currentPower;
			agentForm = addAgent(agentForm);
			logger.info("loop #" + loopCounter + " " + category + " auxStartDevice addAgent  W=" + SapereUtil.round(agentForm.getPower(),5) + " " + agentForm.getDeviceName());
			partialNodeContent = getPartialNodeContent(category, isProducer);
			currentPower = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
			if(Math.abs(currentPower - totalBefore) < 0.001) {
				logger.warning("loop #" + loopCounter + " " + category + " auxStartDevice addAgent " + agentForm.getDeviceName() + " did not work ");
			} else {
				result = true;
			}
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error(e);
		}
		return result;
	}
	public static void startDevices(boolean isProducer, String category, double powerTarget, List<String> excludedDevices) {
		double powerTargetMax = powerTarget * 1.05;
		partialNodeContent = getPartialNodeContent(category, isProducer);
		double currentPower = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
		//List<Device> result = new ArrayList<Device>();
		//targetOK.put(category, false);
		if (currentPower < powerTarget) {
			if (hasDevice(partialNodeContent, isProducer, category, Device.STATUS_SLEEPING, 0,
					powerTargetMax - currentPower, excludedDevices)) {
				// Add producer device
				// Add consumer devices until power is high enough
				// choose some sleeping device
				List<Device> selectedDevices = chooseListDevices(partialNodeContent, isProducer, category, Device.STATUS_SLEEPING, powerTarget, excludedDevices);
				if (selectedDevices.size() > 0) {
					double sumPowerMin = getPowerMin(selectedDevices);
					double sumPowerMax = getPowerMax(selectedDevices);
					if (powerTarget <= powerTargetMax) {
						double toAdd = Math.min(sumPowerMax - sumPowerMin, powerTarget - sumPowerMin);
						double toAddRatio = toAdd / (sumPowerMax - sumPowerMin);
						for (Device nextDevice : selectedDevices) {
							double powerToSet = nextDevice.getPowerMin()
									+ toAddRatio * (nextDevice.getPowerMax() - nextDevice.getPowerMin());
							powerToSet = SapereUtil.round(powerToSet, 2);
							boolean isStarted = auxStartDevice(isProducer, nextDevice, powerToSet);
							if(isStarted) {
								startedDevices.add(nextDevice.getName());
							}
						}
					}
				} else {
					logger.warning("list is empty for categroy " + category);
				}
			} else {
				/*
				logger.warning(
						"Loop #" + loopCounter + " " + category + " No device found for category " + category
								+ " and power max <= " + (powerTargetMax - currentTotal.getRequested()));
								*/
			}
		}
	}

	public static Map<String, Device> findTargetDevicesCombination(NodeContent partialNodeContent, boolean isProducer, String category, double powerTarget /*, List<String> excludedDevices*/) {
		Map<String,Device> result = new HashMap<String, Device>();
		List<String> excludedDevices = new ArrayList<String>();
		if(debugLevel>0) {
			logger.info( "Loop #" + loopCounter + " " + category + " findTargetDevicesCombination : begin");
		}
		double powerTargetMax = powerTarget * 1.05;
		if (hasDevice(partialNodeContent, isProducer, category, null, 0, powerTargetMax , null)) {
			// Add devices until power is high enough
			List<Device> selectedDevices = chooseListDevices(partialNodeContent, isProducer, category, null, powerTarget, excludedDevices);
			if (selectedDevices.size() > 0) {
				double sumPowerMin = getPowerMin(selectedDevices);
				double sumPowerMax = getPowerMax(selectedDevices);
				if(sumPowerMin <= powerTarget &&  sumPowerMax >= powerTarget) {
					double toAdd = Math.min(sumPowerMax - sumPowerMin, powerTarget - sumPowerMin);
					double toAddRatio = toAdd / (sumPowerMax - sumPowerMin);
					for (Device nextDevice : selectedDevices) {
						double powerToSet = nextDevice.getPowerMin()
								+ toAddRatio * (nextDevice.getPowerMax() - nextDevice.getPowerMin());
						Device deviceCopy = nextDevice.clone();
						deviceCopy.setCurrentPower((float) powerToSet);
						//result.add(deviceCopy);
						result.put(deviceCopy.getName(), deviceCopy);
					}
				} else {
					logger.info( "Loop #" + loopCounter + " " + category + " findTargetDevicesCombination : no combination found ");
				}
			} else {
				logger.warning("list is empty for categroy " + category);
			}
		} else {
			/*
			logger.warning(
					"Loop #" + loopCounter + " " + category + " No device found for category " + category
							+ " and power max <= " + (powerTargetMax - currentTotal.getRequested()));
			*/
		}
		return result;
	}

	public static boolean restartAllDevices(boolean isProducer, String category, double powerTarget) {
		// Still not OK : try to find a device combination from zero
		if(debugLevel>0) {
			logger.info("Loop #" + loopCounter + " " + category + " restartAllDevices begin");
		}
		partialNodeContent = getPartialNodeContent(category, isProducer);
		boolean targetDeviceCombinationFound = false;
		Map<String, Device>  mapTargetDevices = findTargetDevicesCombination(partialNodeContent, isProducer, category, powerTarget);
		if(mapTargetDevices.size()>0) {
			double sumPower = getCurrentPower(mapTargetDevices.values());
			double powerTargetMin = powerTarget * 0.95;
			double powerTargetMax = powerTarget * 1.05;
			double currentPower = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
			if(sumPower>= powerTargetMin && sumPower <= powerTargetMax) {
				targetDeviceCombinationFound = true;
				logger.info("Loop #" + loopCounter + " " + category + " restartAllDevices : " + mapTargetDevices.keySet() + " power " + sumPower);
				// Stop device not included in this list
				List<Device> runningDevices =  getDevices(partialNodeContent, isProducer, category, Device.STATUS_RUNNING, 0, 0, null);
				List<Device> sleepingDevices =  getDevices(partialNodeContent, isProducer, category, Device.STATUS_SLEEPING, 0, 0, null);
				List<String> sleepingDeviceNames = getDeviceNames(sleepingDevices);
				for(Device device : runningDevices) {
					AgentForm agentForm = nodeContent.getAgentByDeviceName(device.getName());
					if(agentForm!=null) {
						double totaBefore = currentPower;
						if(mapTargetDevices.containsKey(device.getName())) {
							// modify the device power
							Device targetDevice = mapTargetDevices.get(device.getName());
							double powerToSet = SapereUtil.round(targetDevice.getCurrentPower(),2);
							double powerBefore = agentForm.getPower();
							if(Math.abs(powerToSet - powerBefore) >= 0.001) {
								agentForm = generateAgentForm(device, powerToSet, agentForm);
								logger.info("Loop #" + loopCounter + " " + category + " restartAllDevices : modify " + powerBefore + " -> " + powerToSet);
								modifyAgent(agentForm);
								partialNodeContent = getPartialNodeContent(category, isProducer);
								currentPower = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
								double newTotal = currentPower;
								if(Math.abs(newTotal - totaBefore) < 0.001) {
									logger.warning("Loop #" + loopCounter + " " + category + " restartAllDevices : same total before and after modifyAgent");
								} else {
									updatedDevices.add(agentForm.getDeviceName());
								}
							}
						} else {
							// Remove the device
							auxStopAgent(agentForm);
							partialNodeContent = getPartialNodeContent(category, isProducer);
							currentPower = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
							double newTotal = currentPower;
							if(Math.abs(totaBefore - newTotal) < 0.001) {
								logger.warning("Loop #" + loopCounter + " " + category + "  restartAllDevices stop device '" + agentForm.getDeviceName() + "' did not work");
							} else {
								// It worked
								logger.info("Loop #" + loopCounter + " " + category + "  restartAllDevicese '" + agentForm.getDeviceName() + " OK");
								stoppedDevices.add(agentForm.getDeviceName());
							}
						}
					}
				}
				// Add devices
				for(String targetDeviceName : mapTargetDevices.keySet()) {
					if(sleepingDeviceNames.contains(targetDeviceName)) {
						// start the device
						Device targetDevice = mapTargetDevices.get(targetDeviceName);
						double powerToSet = SapereUtil.round(targetDevice.getCurrentPower(),2);
						boolean isStarted = auxStartDevice(isProducer, targetDevice, powerToSet);
						if(isStarted) {
							startedDevices.add(targetDevice.getName());
						}
					}
				}
			}
			refreshNodeContent();
		}
		if(debugLevel>0) {
			logger.info("Loop #" + loopCounter + " " + category + " restartAllDevices end : targetDeviceCombinationFound = " + targetDeviceCombinationFound);
		}
		return targetDeviceCombinationFound;
	}

	public static void executeLoop() {
		loopCounter++;
		//int nbTry = 0;
		logger.info("executeLoop " + loopCounter + " begin");
		nodeContent = getNodeContent();
		deviceStatistics = retrieveDeviceStatistics(categoryFilter, datetimeShifts);
		Map<String, Boolean> targetKO = new HashMap<String, Boolean>();
		try {
			for (String category : deviceStatistics.keySet()) {
				boolean isProducer = category.endsWith("_ENG");
				if(isProducer) {
					logger.info("Loop #" + loopCounter + " " + category + " : producer");
				}
				logger.info("Loop #" + loopCounter + " " + category + " : step0");
				updatedDevices = new ArrayList<String>();
				stoppedDevices = new ArrayList<String>();
				startedDevices = new ArrayList<String>();

				if("ICT".equals(category)) {
					logger.info("for debug :  category = " + category);
				}

				double avgPower = deviceStatistics.get(category);
				double powerTarget = SapereUtil.round(avgPower * (1 + 0.05 * random.nextGaussian()),2);
				double powerTargetMin = powerTarget * 0.95;
				double powerTargetMax = powerTarget * 1.05;
				boolean targetDeviceCombinationFound = false;
				// First : try to adjust power with running devices
				adjustRunningDevices(isProducer, category, powerTarget, null);
				partialNodeContent = getPartialNodeContent(category, isProducer);
				double currentPower = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
				if (currentPower >= powerTargetMin && currentPower < powerTargetMax) {
					// Target reached : OK
				}

				// Remove devices if current total is over target max
				if (currentPower > powerTargetMax) {
					// we have to reduce power
					stopExistingDevices(isProducer, category, powerTargetMax, true);
					partialNodeContent = getPartialNodeContent(category, isProducer);
					currentPower = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
					logger.info("Loop #" + loopCounter + " " + category + " after stopExistingDevices1 : target  = "
							+ UtilDates.df.format(powerTarget) + " , current = " + UtilDates.df.format(currentPower));

				}
				if (currentPower > powerTargetMax) {
					// still over target : stop deivces whith not filter no filter
					stopExistingDevices(isProducer, category, powerTargetMax, false);
					partialNodeContent = getPartialNodeContent(category, isProducer);
					currentPower = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
					logger.info("Loop #" + loopCounter + " " + category + " after stopExistingDevices2 : target  = "
							+ UtilDates.df.format(powerTarget) + " , current = " + UtilDates.df.format(currentPower));
				}
				// Add devices if current total is under target min
				if (currentPower < powerTargetMin) {
					startDevices(isProducer, category, powerTarget, stoppedDevices);
					partialNodeContent = getPartialNodeContent(category, isProducer);
					currentPower = aux_computeCurrentCategoryPower(partialNodeContent,category, isProducer);
					logger.info("Loop #" + loopCounter + " " + category + " after startDevices : target  = "
							+ UtilDates.df.format(powerTarget) + " , current = " + UtilDates.df.format(currentPower));
				}
				// Check if OK
				if (currentPower < powerTargetMin || currentPower > powerTargetMax) {
					// Still not OK : try to adjust with running agents
					adjustRunningDevices(isProducer, category, powerTarget, stoppedDevices);
					currentPower = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
				}
				if (currentPower < powerTargetMin || currentPower > powerTargetMax) {
					// Still not OK : try to find a device combination from zero
					Thread.sleep(5*1000);
					targetDeviceCombinationFound = restartAllDevices(isProducer, category, powerTarget);
					partialNodeContent = getPartialNodeContent(category, isProducer);
					currentPower = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
				}
				if (currentPower < powerTargetMin || currentPower > powerTargetMax) {
					// NOT OK : log warning
					StringBuffer msgBuffer = new StringBuffer();
					msgBuffer.append("Sleeping devices");
					List<Device> sleepingDevices = getDevices(partialNodeContent, isProducer, category, Device.STATUS_SLEEPING, 0, 0, null);
					for(Device device : sleepingDevices) {
						msgBuffer.append(CR).append(device.toString2());
					}
					msgBuffer.append(CR).append("Running devices");
					List<Device> runningDevices = getDevices(partialNodeContent, isProducer, category, Device.STATUS_RUNNING, 0, 0, null);
					for(Device device : runningDevices) {
						msgBuffer.append(CR).append(device.toString2());
					}
					logger.warning(
							"Loop #" + loopCounter + " " + category + " target not reached : powerTarget = "
									+ UtilDates.df.format(powerTarget) + " , reached = " + UtilDates.df.format(currentPower)
									+ CR + msgBuffer.toString());
				}
				boolean isOK = (currentPower >= powerTargetMin) && (currentPower < powerTargetMax);
				int nbDevices = isProducer? partialNodeContent.getProducers().size()
							:partialNodeContent.getConsumers().size();
				simulatorLog = new SimulatorLog(loopCounter,  category, powerTarget, powerTargetMin,
						 powerTargetMax, currentPower, isOK, nbDevices);
				simulatorLog.setNbModified(updatedDevices.size());
				simulatorLog.setNbStarted(startedDevices.size());
				simulatorLog.setNbStopped(stoppedDevices.size());
				simulatorLog.setTargetDeviceCombinationFound(isOK || targetDeviceCombinationFound);
				addSimulatorLog(simulatorLog);
				if(!isOK) {
					targetKO.put(category, false);
					logger.warning("For debug simulatorLog = " + simulatorLog);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
			logger.error(e);
		}
		String result = " *** All is OK *** ";
		if(targetKO.size()>0) {
			result = "target not reached for the following categories : " + targetKO.keySet();
		}
		logger.info("executeLoop " + loopCounter + " : end " + result);
	}

	public static void auxStopAgent(AgentForm agentForm) {
		String category = agentForm.getDeviceCategory().getValue();
		boolean isProducer = AgentType.PRODUCER.getLabel().equals(agentForm.getAgentType());
		double currentPower = computeCurrentCategoryPower(category, isProducer);
		double totalRequestedBeforeStop = currentPower;
		boolean stopOK = false;
		int nbTry = 0;
		while(!stopOK && nbTry<5) {
			boolean result = stopAgent(agentForm);
			if(!result) {
				logger.info("Loop #" + loopCounter + " " + category + " auxStopAgent " + agentForm.getDeviceName() + " stopAgent returns false");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					logger.error(e);
				}
			}
			int refreshTry = 0;
			currentPower = computeCurrentCategoryPower(category, isProducer);
			stopOK = currentPower < totalRequestedBeforeStop;
			while (  !stopOK && refreshTry < 10) {
				// Refresh node content
				if (refreshTry > 1) {
					//logger.warning("auxStopAgent " + agentForm.getDeviceName() + " : nbTry = " + nbTry + ", refreshTry = " + refreshTry);
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					logger.error(e);
				}
				nodeContent = getNodeContent();
				currentPower = computeCurrentCategoryPower(category, isProducer);
				stopOK = currentPower < totalRequestedBeforeStop;
				refreshTry++;
			}
			logger.info("Loop #" + loopCounter + " " + category + " auxStopAgent " + agentForm.getDeviceName() + " nbTry = " + nbTry );
			nbTry++;
		}
	}

	public static void main(String args[]) {
		debugLevel = 0;
		if (debugLevel > 0) {
			logger.info("Main " + args);
		}
		init(args);
		format_datetime.setTimeZone(TimeZone.getTimeZone("GMT"));
		String scenario = HomeSimulator.class.getSimpleName();
		datetimeShifts.clear();
		InitializationForm initForm = new InitializationForm(scenario, maxTotalPower, datetimeShifts);
		//initForm.setInitialState("consumed", 7);
		initEnergyService(initForm);
		deviceStatistics = retrieveDeviceStatistics(categoryFilter, datetimeShifts);
		Date current = getCurrentDate();
		int waitItNb = 60;
		resetSimulatorLogs();
		boolean retrieveLastContent = false;
		if (retrieveLastContent) {
			nodeContent = restartLastNodeContent();
		} else {
			executeLoop();
			for(int idx = 0; idx<waitItNb ; idx++) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					logger.error(e);
				}
			}
		}
		Date end = UtilDates.shiftDateMinutes(current, 1800 * 1);
		while (current.before(end)) {
			current = getCurrentDate();
			//double random = Math.random();
			try {
				executeLoop();
				logger.info("after executeLoop ");
				Thread.sleep(60 * 1000);
			} catch (InterruptedException e) {
				logger.error(e);
			}
		}
	}
}
