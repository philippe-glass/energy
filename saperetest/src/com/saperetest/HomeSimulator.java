package com.saperetest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.sapereapi.model.PredictionSetting;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.Device;
import com.sapereapi.model.energy.node.NodeContent;
import com.sapereapi.model.energy.node.NodeTotal;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.energy.input.AgentInputForm;
import com.sapereapi.model.energy.input.SimulatorLog;
import com.sapereapi.model.input.InitializationForm;
import com.sapereapi.model.learning.LearningModelType;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

public class HomeSimulator extends TestSimulator {

	static Map<DeviceCategory, Double> deviceStatistics = new HashMap<DeviceCategory, Double>();
	static int loopCounter = 0;
	final static String CR = System.getProperty("line.separator");
	static SimulatorLog simulatorLog = null;
	// Define device category filter
	static DeviceCategory[] categoryFilter = new DeviceCategory[] {};

	static List<String> updatedDevices = new ArrayList<String>();
	static List<String> stoppedDevices = new ArrayList<String>();
	static List<String> startedDevices = new ArrayList<String>();
	static double maxTotalPower = NodeStates.DEFAULT_NODE_MAX_TOTAL_POWER;
	static NodeContent partialNodeContent = null;
	//static Integer shiftHourOfDay = null;

	private static AgentInputForm generateAgentForm(Device aDevice, double targetPower, AgentForm existingForm) {
		double duration = 24 * 60 * 365;
		if (aDevice.getAverageDurationMinutes() > 5.0) {
			duration = aDevice.getAverageDurationMinutes() * (1 + 0.10 * random.nextGaussian());
		}
		PriorityLevel priority = aDevice.getPriorityLevel();
		double power = aDevice.getCorrectedTargePower(targetPower);
		if (!aDevice.isProducer()) {
			// power = (float) 0.1 * power;
		}
		Date current = getCurrentDate();
		ProsumerRole prosumerRole = aDevice.isProducer() ? ProsumerRole.PRODUCER : ProsumerRole.CONSUMER;
		DeviceCategory deviceCategory = aDevice.getCategory();
		// DeviceCategory test = DeviceCategory.getByName("WASHING_DRYING");
		Date endDate = UtilDates.shiftDateMinutes(current, duration);
		long timeShiftMS = UtilDates.computeTimeShiftMS(datetimeShifts);
		PricingTable pricingTable = new PricingTable(timeShiftMS);//new PricingTable(current, endDate, 0);
		AgentInputForm result = new AgentInputForm(prosumerRole, "", aDevice.getName(), deviceCategory, aDevice.getEnvironmentalImpact(), pricingTable.getMapPrices(), power, current, endDate,
				priority, duration, timeShiftMS);
		if (existingForm != null) {
			result.setAgentName(existingForm.getAgentName());
			result.setId(existingForm.getId());
		}
		return result;
	}

	private static double computeCurrentCategoryPower(DeviceCategory deviceCategory, boolean isProducer) {
		partialNodeContent = getPartialNodeContent(defaultbaseUrl, deviceCategory, isProducer);
		return aux_computeCurrentCategoryPower(partialNodeContent, deviceCategory, isProducer);
	}

	private static double aux_computeCurrentCategoryPower(NodeContent nodeContent, DeviceCategory deviceCategory, boolean isProducer) {
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

	public static void adjustRunningDevices(boolean isProducer, DeviceCategory category, double powerTarget, List<String> excludedDevices) {
		if(debugLevel>0) {
			logger.info("Loop #" + loopCounter + " " + category + "  adjustExistingPowers begin");
		}
		AgentForm agentForm = null;
		//List<Device> result = new ArrayList<Device>();
		partialNodeContent = getPartialNodeContent(defaultbaseUrl, category, isProducer);
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
							AgentInputForm agentInputForm = generateAgentForm(nextDevice, powerToSet, agentForm);
							logger.info("Loop #" + loopCounter + " " + category + " adjustExistingPowers modify " + agentInputForm.getAgentName() + " : " + powerBefore + " -> " + powerToSet);
							modifyAgent(defaultbaseUrl, agentInputForm);
							// Refresh currentTotal
							partialNodeContent.getAgentByDeviceName(nextDevice.getName());
							partialNodeContent = getPartialNodeContent(defaultbaseUrl, category, isProducer);
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

	public static void stopExistingDevices(boolean isProducer, DeviceCategory category, double powerTarget, boolean applyMaxFilter) {
		AgentForm agentForm = null;
		//List<String> removedDevices = new ArrayList<String>();
		//refreshNodeContent();
		partialNodeContent= getPartialNodeContent(defaultbaseUrl, category, isProducer);
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
				AgentInputForm agentInputForm = agentForm.generateInputForm();
				auxStopAgent(agentInputForm);
				//currentTotal =  getCurrentCategoryPower(nodeContent, category, isProducer);
				partialNodeContent = getPartialNodeContent(defaultbaseUrl, category, isProducer);
				double newTotal = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
				if(Math.abs(lastTotal - newTotal) < 0.001) {
					logger.warning("Loop #" + loopCounter + " " + category + " stopExistingDevices stop device '" + agentInputForm.getDeviceName() + "' did not work");
				} else {
					// It worked
					stoppedDevices.add(agentInputForm.getDeviceName());
					logger.info("Loop #" + loopCounter + " " + category + " stopExistingDevices stop device '" + agentInputForm.getDeviceName() + " OK");
				}
			} else {
				logger.warning("Step456 : stopExistingDevices : chosen agentForm is KO ");
			}
			nbTry++;
		}
	}

	public static boolean auxStartDevice(boolean isProducer, Device nextDevice, double powerToSet) {
		DeviceCategory category = nextDevice.getCategory();
		if(!SapereUtil.checkPowerRounded(powerToSet, logger)) {
			logger.warning("auxStartDevice powerToSet not rounded !!! powerToSet = " + powerToSet);
		}
		nextDevice.setCurrentPower((float) powerToSet);
		boolean result = false;
		partialNodeContent = getPartialNodeContent(defaultbaseUrl, category, isProducer);
		double currentPower = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
		if (partialNodeContent.hasDevice(nextDevice.getName())) {
			// Device already contained in node content but disabled : we have to restart it
			AgentForm agentForm = partialNodeContent.getAgentByDeviceName(nextDevice.getName());
			AgentInputForm agentInputForm = agentForm.generateInputForm();
			double powerToSet2 = nextDevice.getCorrectedTargePower(powerToSet);
			agentInputForm.setPower(powerToSet2);
			double duration = Double.valueOf(24 * 60 * 365);
			if (nextDevice.getAverageDurationMinutes() >= 5.0) {
				duration = nextDevice.getAverageDurationMinutes() * (1 + 0.10 * random.nextGaussian());
			}
			Date current = getCurrentDate();
			agentInputForm.setBeginDate(current);
			agentInputForm.setEndDate( UtilDates.shiftDateMinutes(current, duration));
			//AgentInputForm agentInputForm = generateAgentForm(nextDevice, powerToSet, agentForm);
			double totalBefore = currentPower;
			logger.info("auxStartDevice : agentInputForm.power = " + agentInputForm.getPower() + ", powerToSet = " + powerToSet);
			agentForm = restartAgent(defaultbaseUrl, agentInputForm);
			partialNodeContent = getPartialNodeContent(defaultbaseUrl, category, isProducer);
			currentPower = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
			if(Math.abs(currentPower - totalBefore) < 0.001) {
				logger.warning("loop #" + loopCounter + " " + category + " auxStartDevice restartAgent " + agentForm.getDeviceName() + " did not work ");
			} else {
				result = true;
			}
			//logger.info("loop #" + loopCounter + " " + category + " auxStartDevice restartAgent W=" + agentForm.getPower() + " " + agentForm.getDeviceName());
		} else {
			// Device not contained in node content : we have to start it
			AgentInputForm agentInputForm = generateAgentForm(nextDevice, powerToSet, null);
			double totalBefore = currentPower;
			AgentForm agentForm = addAgent(defaultbaseUrl, agentInputForm);
			logger.info("loop #" + loopCounter + " " + category + " auxStartDevice addAgent  W=" + SapereUtil.round(agentForm.getPower(),5) + " " + agentForm.getDeviceName());
			partialNodeContent = getPartialNodeContent(defaultbaseUrl, category, isProducer);
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
			logger.error(e);
		}
		return result;
	}
	public static void startDevices(boolean isProducer, DeviceCategory category, double powerTarget, List<String> excludedDevices) {
		double powerTargetMax = powerTarget * 1.05;
		partialNodeContent = getPartialNodeContent(defaultbaseUrl, category, isProducer);
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

	public static Map<String, Device> findTargetDevicesCombination(NodeContent partialNodeContent, boolean isProducer, DeviceCategory category, double powerTarget /*, List<String> excludedDevices*/) {
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

	public static boolean restartAllDevices(boolean isProducer, DeviceCategory category, double powerTarget) {
		// Still not OK : try to find a device combination from zero
		if(debugLevel>0) {
			logger.info("Loop #" + loopCounter + " " + category + " restartAllDevices begin");
		}
		partialNodeContent = getPartialNodeContent(defaultbaseUrl, category, isProducer);
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
								AgentInputForm agentInputForm = generateAgentForm(device, powerToSet, agentForm);
								logger.info("Loop #" + loopCounter + " " + category + " restartAllDevices : modify " + powerBefore + " -> " + powerToSet);
								modifyAgent(defaultbaseUrl, agentInputForm);
								partialNodeContent = getPartialNodeContent(defaultbaseUrl, category, isProducer);
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
							AgentInputForm agentInputForm = agentForm.generateInputForm();
							auxStopAgent(agentInputForm);
							partialNodeContent = getPartialNodeContent(defaultbaseUrl, category, isProducer);
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
			refreshNodeContent(defaultbaseUrl, false);
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
		nodeContent = getNodeContent(defaultbaseUrl);
		Map<DeviceCategory, Boolean> targetKO = new HashMap<DeviceCategory, Boolean>();
		try {
			deviceStatistics = retrieveDeviceStatistics(categoryFilter, datetimeShifts);
			for (DeviceCategory category : deviceStatistics.keySet()) {
				boolean isProducer = category.isProducer();
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
				partialNodeContent = getPartialNodeContent(defaultbaseUrl, category, isProducer);
				double currentPower = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
				if (currentPower >= powerTargetMin && currentPower < powerTargetMax) {
					// Target reached : OK
				}

				// Remove devices if current total is over target max
				if (currentPower > powerTargetMax) {
					// we have to reduce power
					stopExistingDevices(isProducer, category, powerTargetMax, true);
					partialNodeContent = getPartialNodeContent(defaultbaseUrl, category, isProducer);
					currentPower = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
					logger.info("Loop #" + loopCounter + " " + category + " after stopExistingDevices1 : target  = "
							+ UtilDates.df.format(powerTarget) + " , current = " + UtilDates.df.format(currentPower));

				}
				if (currentPower > powerTargetMax) {
					// still over target : stop deivces whith not filter no filter
					stopExistingDevices(isProducer, category, powerTargetMax, false);
					partialNodeContent = getPartialNodeContent(defaultbaseUrl, category, isProducer);
					currentPower = aux_computeCurrentCategoryPower(partialNodeContent, category, isProducer);
					logger.info("Loop #" + loopCounter + " " + category + " after stopExistingDevices2 : target  = "
							+ UtilDates.df.format(powerTarget) + " , current = " + UtilDates.df.format(currentPower));
				}
				// Add devices if current total is under target min
				if (currentPower < powerTargetMin) {
					startDevices(isProducer, category, powerTarget, stoppedDevices);
					partialNodeContent = getPartialNodeContent(defaultbaseUrl, category, isProducer);
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
					partialNodeContent = getPartialNodeContent(defaultbaseUrl, category, isProducer);
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
			logger.error(e);
		}
		String result = " *** All is OK *** ";
		if(targetKO.size()>0) {
			result = "target not reached for the following categories : " + targetKO.keySet();
		}
		logger.info("executeLoop " + loopCounter + " : end " + result);
	}

	public static void auxStopAgent(AgentInputForm agentInputForm) {
		DeviceCategory category = agentInputForm.getDeviceCategory();
		boolean isProducer = agentInputForm.isProducer();
		double currentPower = computeCurrentCategoryPower(category, isProducer);
		double totalRequestedBeforeStop = currentPower;
		boolean stopOK = false;
		int nbTry = 0;
		List<String> listAgentName = new ArrayList<>();
		listAgentName.add(agentInputForm.getAgentName());
		while(!stopOK && nbTry<5) {
			boolean result = stopListAgents(defaultbaseUrl, listAgentName);
			//boolean result = stopAgent(agentForm);
			if(!result) {
				logger.info("Loop #" + loopCounter + " " + category + " auxStopAgent " + agentInputForm.getDeviceName() + " stopAgent returns false");
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
				nodeContent = getNodeContent(defaultbaseUrl);
				currentPower = computeCurrentCategoryPower(category, isProducer);
				stopOK = currentPower < totalRequestedBeforeStop;
				refreshTry++;
			}
			logger.info("Loop #" + loopCounter + " " + category + " auxStopAgent " + agentInputForm.getDeviceName() + " nbTry = " + nbTry );
			nbTry++;
		}
	}

	public static void main(String args[]) {
		debugLevel = 0;
		if (debugLevel > 0) {
			logger.info("Main " + args);
		}
		try {
			init(args);
		} catch (Exception e) {
			logger.error(e);
		}
		format_datetime.setTimeZone(TimeZone.getTimeZone("GMT"));
		String scenario = HomeSimulator.class.getSimpleName();
		datetimeShifts.clear();
		PredictionSetting nodePredicitonSetting = new PredictionSetting(Boolean.FALSE, null, LearningModelType.MARKOV_CHAINS);
		PredictionSetting clusterPredicitonSetting = new PredictionSetting(Boolean.FALSE, null, LearningModelType.MARKOV_CHAINS);
		InitializationForm initForm = new InitializationForm(scenario, maxTotalPower, datetimeShifts, nodePredicitonSetting, clusterPredicitonSetting);
		//initForm.setInitialState("consumed", 7);
		initEnergyService(defaultbaseUrl, initForm);
		try {
			deviceStatistics = retrieveDeviceStatistics(categoryFilter, datetimeShifts);
		}catch (Exception e) {
			logger.error(e);
		}
		Date current = getCurrentDate();
		int waitItNb = 60;
		resetSimulatorLogs();
		boolean retrieveLastContent = false;
		if (retrieveLastContent) {
			nodeContent = restartLastNodeContent(defaultbaseUrl);
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
