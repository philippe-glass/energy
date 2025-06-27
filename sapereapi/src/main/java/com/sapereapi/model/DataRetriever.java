package com.sapereapi.model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.Device;
import com.sapereapi.model.energy.DeviceMeasure;
import com.sapereapi.model.energy.DeviceProperties;
import com.sapereapi.model.energy.StorageType;
import com.sapereapi.model.energy.input.AgentInputForm;
import com.sapereapi.model.energy.policy.IConsumerPolicy;
import com.sapereapi.model.energy.policy.IProducerPolicy;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.input.InitializationForm;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.PhaseNumber;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.model.referential.ProsumerRole;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.SapereAgent;
import eu.sapere.middleware.log.AbstractLogger;

public class DataRetriever extends Thread {
	protected ServerConfig serverConfig = null;
	private static AbstractLogger logger = null;
	private Map<String, Device> mapDevices = new HashMap<>();
	private Map<Integer, Integer> datetimeShifts = new HashMap<Integer, Integer>();
	static Random random = new Random();
	public final static int CSV_FORMAT_LESVERGERS = 1;
	public final static int CSV_FORMAT_HACKDAY = 2;
	private static int fileFormat = CSV_FORMAT_HACKDAY;

	public DataRetriever( ServerConfig _serverConfig, AbstractLogger aLogger) {
		super();
		serverConfig = _serverConfig;
		logger = aLogger;
	}

	private void aux_addMeasure(Map<String, Object> row, Date measureDate, Map<String, DeviceMeasure> mapMeasures,
			boolean replaceAll) {
		PhaseNumber phaseNumber = PhaseNumber.valueOf("" + row.get("phase"));
		Double power_p = SapereUtil.roundPower(Math.abs(SapereUtil.getDoubleValue(row, "active_power", logger))); // active power
		// (power_p)
		Double power_q = SapereUtil.roundPower(Math.abs(SapereUtil.getDoubleValue(row, "reactive_power", logger))); // reactive
		// power
		// (power_q)
		// apparent power (power_s) : power_s^2 = power_p^2 + power_q^2
		Double power_s = SapereUtil.roundPower(Math.abs(SapereUtil.getDoubleValue(row, "apparent_power", logger)));
		String deviceName = "" + row.get("description");
		if (replaceAll && mapMeasures.containsKey(deviceName)) {
			DeviceMeasure existingMeasure = mapMeasures.get(deviceName);
			if (existingMeasure.getDatetime().before(measureDate)) {
				mapMeasures.remove(deviceName);
			}
		}
		if (!mapMeasures.containsKey(deviceName)) {
			DeviceMeasure nextMeasure = new DeviceMeasure(measureDate, deviceName, phaseNumber, power_p, power_q,
					power_s);
			mapMeasures.put(deviceName, nextMeasure);
		} else {
			DeviceMeasure nextMeasure = mapMeasures.get(deviceName);
			nextMeasure.addPhaseMeasures(phaseNumber, power_p, power_q, power_s);
		}
	}

	private Map<String, Object> aux_retrieveRow(String[] header, String line, String separator) {
		Map<String, Object> row = new HashMap<>();
		String[] values = line.split(separator);
		for (int rowIdx = 0; rowIdx < values.length; rowIdx++) {
			String field = header[rowIdx];
			String value = values[rowIdx];
			row.put(field, value);
		}
		return row;
	}

	public TreeMap<Date, Map<String, DeviceMeasure>> aux_retrieveDataLesVergers(Date dateBegin, Date dateEnd,
			String csvfilepath, String separator) {
		TreeMap<Date, Map<String, DeviceMeasure>> result = new TreeMap<>();
		Map<String, DeviceMeasure> mapMeasures = new HashMap<String, DeviceMeasure>();
		Map<String, DeviceMeasure> mapLastProdMeasures = new HashMap<String, DeviceMeasure>();
		mapDevices = new HashMap<>();
		long deviceId = 1;
		BufferedReader br = null;
		try {
			br = new BufferedReader(
					new InputStreamReader(new FileInputStream(csvfilepath), StandardCharsets.ISO_8859_1));
			String headerLine = br.readLine();
			String[] header = headerLine.split(separator);
			String line;
			// Date lastDate = null;
			while ((line = br.readLine()) != null) {
				Map<String, Object> row = aux_retrieveRow(header, line, separator);
				String sDate = "" + row.get("timestamp");
				Date measureDate = UtilDates.format_sql.parse(sDate);
				//int tzOffset = measureDate.getTimezoneOffset();
				String scategory = "" + row.get("device_category");
				DeviceCategory category = DeviceCategory.valueOf(scategory);
				boolean isAdditionalProdMeasure = category.isProducer() && measureDate.before(dateBegin);
				if (!measureDate.before(dateBegin) && measureDate.before(dateEnd) || isAdditionalProdMeasure) {
					String deviceName = "" + row.get("description");
					if (!mapDevices.containsKey(deviceName)) {
						// create Device
						Device device = new Device();
						device.setId(deviceId);
						deviceId++;
						DeviceProperties deviceProp = new DeviceProperties(deviceName, category,
								EnvironmentalImpact.MEDIUM);
						if (category == DeviceCategory.BIOMASS_ENG || category == DeviceCategory.WIND_ENG
								|| category == DeviceCategory.SOLOR_ENG) {
							deviceProp.setEnvironmentalImpact(EnvironmentalImpact.LOW);
						}
						device.setProperties(deviceProp);
						mapDevices.put(deviceName, device);
					}
					if (isAdditionalProdMeasure) {
						aux_addMeasure(row, measureDate, mapLastProdMeasures, true);
					} else {
						if (!result.containsKey(measureDate)) {
							result.put(measureDate, new HashMap<String, DeviceMeasure>());
						}
						mapMeasures = result.get(measureDate);
						aux_addMeasure(row, measureDate, mapMeasures, false);
					}
					// lastDate = measureDate;
				}
			}
		} catch (Throwable e) {
			logger.error(e);
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				logger.error(e);
			}
		}
		// Complete Prod Measures
		DeviceProperties sigProperties = new DeviceProperties("SIG", DeviceCategory.EXTERNAL_ENG,
				EnvironmentalImpact.MEDIUM);
		sigProperties.setThreePhases();
		Device prodSig = new Device(deviceId, sigProperties, 0, 1500, 0);
		deviceId++;
		mapDevices.put(prodSig.getName(), prodSig);

		for (String deviceName : mapLastProdMeasures.keySet()) {
			DeviceMeasure lastdMeasure = mapLastProdMeasures.get(deviceName);
			for (Date measureDate : result.keySet()) {
				mapMeasures = result.get(measureDate);
				if (mapMeasures.containsKey(deviceName)) {
					if (measureDate.after(lastdMeasure.getDatetime())) {
						lastdMeasure = mapMeasures.get(deviceName);
					}
				} else {
					DeviceMeasure copy = lastdMeasure.clone();
					copy.setDatetime(measureDate);
					mapMeasures.put(deviceName, copy);
				}
				// add SIG Measure
				double power_p = SapereUtil.roundPower(100 * 78 * (1 + 0 * 0.05 * random.nextGaussian()));
				DeviceMeasure sigMeasure = new DeviceMeasure(measureDate, prodSig.getName(), PhaseNumber.l1, power_p,
						0., 0.);
				mapMeasures.put(prodSig.getName(), sigMeasure);
			}
		}
		// TreeMap to store values of HashMap
		return result;
	}

	private void initDevicesHackDay(String consumerDeviceName, String producerDeviceName) {
		mapDevices.clear();
		// Create a consumer device
		DeviceProperties deviceProp1 = new DeviceProperties(consumerDeviceName, DeviceCategory.LIGHTING,
				EnvironmentalImpact.MEDIUM);
		long deviceId = 1;
		Device deviceConsumer = new Device(deviceId, deviceProp1, 0., 1000., 60.0);
		deviceId++;
		mapDevices.put(deviceConsumer.getName(), deviceConsumer);
		// Create a producer device
		DeviceProperties deviceProp2 = new DeviceProperties(producerDeviceName, DeviceCategory.SOLOR_ENG,
				EnvironmentalImpact.LOW);
		Device deviceProducer = new Device(deviceId, deviceProp2, 0., 1000., 60.0);
		deviceId++;
		mapDevices.put(deviceProducer.getName(), deviceProducer);
	}

	public TreeMap<Date, Map<String, DeviceMeasure>> aux_retrieveDataHackDay(Date dateBegin, Date dateEnd,
			String csvfilepath, String separator) throws HandlingException {
		int lineIndx = 1;
		logger.info("aux_retrieveDataHackDay : begin");
		Date begin = new Date();
		List<TimestampedValue> csvData = new ArrayList<>();
		TimestampedValue lastTValue = null;
		TreeMap<Date, Map<String, DeviceMeasure>> result = new TreeMap<>();
		String consumerDeviceName = "Lamp-01";
		String producerDeviceName = "SolarPanel-01";
		initDevicesHackDay(consumerDeviceName, producerDeviceName);
		BufferedReader br = null;
		try {
			br = new BufferedReader(
					new InputStreamReader(new FileInputStream(csvfilepath), StandardCharsets.ISO_8859_1));
			String headerLine = br.readLine();
			String[] header = headerLine.split(separator);
			String sHeaderValues = header[1];
			String line;
			int maxBuffSize = 5000;
			while ((line = br.readLine()) != null) {
				Map<String, Object> row = aux_retrieveRow(header, line, separator);
				String sDate = "" + row.get("DATETIME");
				sDate = sDate.substring(0, 19);
				Date measureDate1 = UtilDates.format_sql.parse(sDate);
				int tzOffset = 0;// tz.getOffset(measureDate1.getDate());
				Date measureDate = new Date(measureDate1.getTime() + tzOffset);
				double power_p_balance = SapereUtil.roundPower(SapereUtil.getDoubleValue(row, sHeaderValues, logger));
				double power_p_consumer = 0.;
				double power_p_producer = 0.;
				if (power_p_balance > 0) {
					power_p_consumer += power_p_balance;
				} else {
					power_p_producer += power_p_balance;
				}
				lastTValue = new TimestampedValue(measureDate, power_p_balance);
				csvData.add(lastTValue);
				// Retrieve by data portion of maxBuffSize
				if (csvData.size() >= maxBuffSize) {
					logger.info("aux_retrieveDataHackDay lineIndx " + lineIndx + " " + lastTValue);
					EnergyDbHelper.getInstance().saveTValues(csvData);
					csvData.clear();
				}

				Map<String, DeviceMeasure> mapMeasures = new HashMap<>();
				if (!measureDate.before(dateBegin) && measureDate.before(dateEnd)) {
					DeviceMeasure consumerMeasure = new DeviceMeasure(measureDate, consumerDeviceName, PhaseNumber.l1,
							power_p_consumer, 0., 0.);
					mapMeasures.put(consumerDeviceName, consumerMeasure);
					DeviceMeasure producerMeasure = new DeviceMeasure(measureDate, producerDeviceName, PhaseNumber.l1,
							power_p_producer, 0., 0.);
					mapMeasures.put(producerDeviceName, producerMeasure);
					result.put(measureDate, mapMeasures);
				}
				lineIndx++;
			}
		} catch (Throwable e) {
			logger.error(e);
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				logger.error(e);
			}
		}
		EnergyDbHelper.getInstance().saveTValues(csvData);
		Date end = new Date();
		long timeSpent = end.getTime() - begin.getTime();
		logger.info("aux_retrieveDataHackDay : end lineIndx " + lineIndx + " " + lastTValue + ", time spent (MS) = " + timeSpent);
		// TreeMap to store values of HashMap
		return result;
	}

	@Override
	public synchronized void start() {
		super.start();
		fileFormat = CSV_FORMAT_LESVERGERS;
		String csvFilePath = serverConfig.getCsvFile();
		if (csvFilePath != null) {
			if (csvFilePath.contains("dfDevice.csv") || csvFilePath.contains("dfTest.csv")) {
				fileFormat = CSV_FORMAT_HACKDAY;
			}
		}
		logger.info("DataRetriever.start : csvFilePath = "+ csvFilePath + " fileFormat = " + CSV_FORMAT_HACKDAY);
		try {
			initService();
			Date dateCurrent = Sapere.getInstance().getCurrentDate();
			if (csvFilePath != null) {
				Date dateMin = UtilDates.shiftDateMinutes(dateCurrent, -15);
				Date dateMax = UtilDates.shiftDateMinutes(dateCurrent, 60);
				TreeMap<Date, Map<String, DeviceMeasure>> measurementData = new TreeMap<>();
				if (fileFormat == CSV_FORMAT_LESVERGERS) {
					dateMin = UtilDates.shiftDateMinutes(dateCurrent, -1);
					measurementData = aux_retrieveDataLesVergers(dateMin, dateMax, csvFilePath, ";");
				}
				if (fileFormat == CSV_FORMAT_HACKDAY) {
					dateMin = UtilDates.shiftDateMinutes(dateCurrent, -15);
					dateMax = UtilDates.shiftDateMinutes(dateCurrent, 200);
					measurementData = aux_retrieveDataHackDay(dateMin, dateMax, csvFilePath, ",");
				}
				for (Date nextMeasureDate : measurementData.keySet()) {
					executeLoop(nextMeasureDate, measurementData.get(nextMeasureDate));
				}
			} else {
				logger.error("csvFilePath not given");
			}
		} catch (Exception e) {
			logger.error(e);
		}
	}

	private void initService() throws HandlingException {
		EnergyStorageSetting energyStorageSetting = new EnergyStorageSetting(false, false, StorageType.PRIVATE, 0.0, 0.0);
		PredictionSetting nodePredictionSetting = new PredictionSetting(false, null, null, 1);
		PredictionSetting clusterPredictionSetting = new PredictionSetting(false, null, null, 1);
		String scenario = "auto";
		Date current = new Date();
		// long currentMS = current.getTime();
		Calendar calendar = Calendar.getInstance();
		if (fileFormat == CSV_FORMAT_HACKDAY) {
			calendar.set(Calendar.MONTH, Calendar.JANUARY);
			calendar.set(Calendar.DAY_OF_MONTH, 4);
			calendar.set(Calendar.YEAR, 2021);
		} else {
			calendar.set(Calendar.MONTH, Calendar.FEBRUARY);
			calendar.set(Calendar.DAY_OF_MONTH, 18);
		}
		Date dateBegin = calendar.getTime();
		long timeZoneShift = UtilDates.computeTimeZoneShift(dateBegin, current);
		long datetimeShiftMS = UtilDates.computeTimeShiftMS(dateBegin, current);
		double datetimeShiftDays = (datetimeShiftMS / UtilDates.MS_IN_DAY);
		long timeShiftMS = (datetimeShiftMS % UtilDates.MS_IN_DAY) + timeZoneShift;
		calendar.setTime(new Date());
		// Date test = UtilDates.shiftDateDays(new Date(), datetimeShiftDays);
		datetimeShifts = new HashMap<Integer, Integer>();
		datetimeShifts.put(Calendar.DAY_OF_YEAR, (int) datetimeShiftDays);
		double timeShiftHour = timeShiftMS / UtilDates.MS_IN_HOUR;
		datetimeShifts.put(Calendar.HOUR, (int) (1 * timeShiftHour));
		// long testDateLong = UtilDates.computeTimeShiftMS(datetimeShifts);
		// Date testDate = new Date(testDateLong + currentMS);
		InitializationForm initForm = new InitializationForm(scenario
				,100 * NodeStates.DEFAULT_NODE_MAX_TOTAL_POWER
				,new HashMap<Integer, Integer>()
				,energyStorageSetting
				,nodePredictionSetting, clusterPredictionSetting
				);
		initForm.setShiftDayOfMonth(datetimeShifts.get(Calendar.DAY_OF_YEAR));
		initForm.setShiftHourOfDay(datetimeShifts.get(Calendar.HOUR));
		initForm.setDisableSupervision(false);
		initForm.setTimeZoneId("GMT");
		initForm.setUrlForcasting(serverConfig.getUrlForcasting());
		Sapere.getInstance().initEnergyService(serverConfig, initForm);
	}

	private void executeLoop(Date measureDate, Map<String, DeviceMeasure> mapMeasures) throws HandlingException {
		Date current = Sapere.getInstance().getCurrentDate();
		int cpt = 0;
		while (current.before(measureDate)) {
			try {
				if (cpt == 30) {
					logger.info(UtilDates.format_time.format(measureDate) + " : wait until next measure "
							+ UtilDates.format_time.format(measureDate));
					cpt = 0;
				}
				Thread.sleep(1 * 1000);
				current = Sapere.getInstance().getCurrentDate();
				cpt++;
			} catch (InterruptedException e) {
				logger.error(e);
			}
		}
		PricingTable pricingTable = new PricingTable(current, 0, Sapere.getInstance().getTimeShiftMS());
		double balanceValue = 0;
		for (String deviceName : mapMeasures.keySet()) {
			DeviceMeasure measure = mapMeasures.get(deviceName);
			Device device = mapDevices.get(deviceName);
			Date endDate = UtilDates.shiftDateMinutes(measureDate, 60);
			ProsumerRole prosumerRole = device.isProducer() ? ProsumerRole.PRODUCER : ProsumerRole.CONSUMER;
			EnvironmentalImpact envImpact = device.getEnvironmentalImpact();
			EnergyStorageSetting energyStorageSetting = null;
			double power_p = measure.computeTotalPower_p();
			boolean isRunning = Device.STATUS_RUNNING.equals(device.getStatus());
			double duration = Sapere.YEAR_DURATION_MIN;
			balanceValue += (device.isProducer() ? -1 : 1) * power_p;
			AgentForm result = null;
			if (isRunning) {
				long timeShiftMS = Sapere.getNodeContext().getTimeShiftMS();
				AgentInputForm agentInputForm = new AgentInputForm(prosumerRole,
						device.getRunningAgentName(), device.getName(), device.getCategory(), envImpact,
						pricingTable.getMapPrices(), power_p, measureDate, endDate, PriorityLevel.LOW, duration,
						energyStorageSetting, timeShiftMS);
				agentInputForm.updateDeviceProperties(device.getProperties());
				result = Sapere.getInstance().modifyEnergyAgent(agentInputForm);
			} else {
				SapereAgent agent = null;
				IProducerPolicy producerPolicy = Sapere.getPolicyFactory().initDefaultProducerPolicy();
				IConsumerPolicy consumerPolicy = Sapere.getPolicyFactory().initDefaultConsumerPolicy();
				if (device.isProducer()) {
					agent = Sapere.getInstance()
							.addServiceProducer(
									power_p, current, endDate, new DeviceProperties(device.getName(),
											device.getCategory(), device.getEnvironmentalImpact()),
									pricingTable, energyStorageSetting, producerPolicy, consumerPolicy);

				} else {
					agent = Sapere.getInstance().addServiceConsumer(power_p, current, endDate, duration,
							PriorityLevel.LOW, device.getProperties(), pricingTable, energyStorageSetting,
							Sapere.getPolicyFactory().initDefaultProducerPolicy(), consumerPolicy);
				}
				result = Sapere.getInstance().generateAgentForm(agent);
			}
			if (result != null && result.isRunning()) {
				device.setStatus(Device.STATUS_RUNNING);
				device.setRunningAgentName(result.getAgentName());
			} else {
				device.setStatus(Device.STATUS_SLEEPING);
			}
		}
		/*
		 * TimestampedValue nextValue = new TimestampedValue(measureDate, balanceValue);
		 * all_tvalues.add(nextValue); List<TimestampedValue> last_tvalues = new
		 * ArrayList<>(); int nbTValues = 2; for(int idx=0; idx < nbTValues ; idx++) {
		 * int idx2 = all_tvalues.size() - 1 - idx; if(idx2>=0) {
		 * last_tvalues.add(all_tvalues.get(idx2)); } } // Call the forcasting service
		 * Sapere.getInstance().callForcastingService(last_tvalues);
		 */
	}

}
