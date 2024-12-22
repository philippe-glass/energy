package com.sapereapi.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sapereapi.agent.energy.EnergyAgent;
import com.sapereapi.db.DBConfig;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.AgentState;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.PredictionSetting;
import com.sapereapi.model.ServerConfig;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.energy.ConfirmationItem;
import com.sapereapi.model.energy.DeviceMeasure;
import com.sapereapi.model.energy.EnergyRequest;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.ReducedContract;
import com.sapereapi.model.energy.RegulationWarning;
import com.sapereapi.model.energy.SingleNodeStateItem;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.model.energy.pricing.ComposedRate;
import com.sapereapi.model.energy.pricing.PricingTable;
import com.sapereapi.model.learning.LearningModelType;
import com.sapereapi.model.learning.PredictionScope;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;
import com.sapereapi.model.referential.PriorityLevel;

import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.lsa.SyntheticPropertyName;
import eu.sapere.middleware.node.NodeLocation;

public class SapereUtil {
	public final static int EVENT_INIT_DECAY = 2;
	public final static String CR = System.getProperty("line.separator"); // Cariage return
	public final static String DOUBLE_QUOTE = "\"";
	public final static String SINGLE_QUOTE = "'";
	public final static int NB_DEC_POWER = 3;

	private static Pattern patternOption = Pattern.compile("^-(?<param>[0-9a-zA-Z_.]+):(?<value>[0-9a-zA-Z_.:;\\-/,]*)$");


	/**/
	public static AgentState parseState(String sState) throws HandlingException {
		AgentState result = new AgentState();
		if ("".equals(sState) || sState == null) {
			// throw new Exception("parseState : empty state given");
			return result;
		}
		String[] splitPipe = sState.split("\\|");
		if (splitPipe.length > 0) {
			String sinputs = splitPipe[0];
			String souputs = (splitPipe.length > 1) ? splitPipe[1] : "";
			StringTokenizer stInput = new StringTokenizer(sinputs, ",");
			while (stInput.hasMoreTokens()) {
				String input = stInput.nextToken();
				if (!"".equals(input)) {
					result.addInput(input);
				}
			}
			StringTokenizer stOutput = new StringTokenizer(souputs, ",");
			while (stOutput.hasMoreTokens()) {
				String output = stOutput.nextToken();
				if (!"".equals(output)) {
					result.addOutput(output);
				}
			}
			return result;
		} else {
			throw new HandlingException("parseState : wrong state format : the given state ('" + sState
					+ "') must contain one (and only one) pipe character");
		}
	}

	public static String addOutputsToState(String stateValue, String[] outputs) throws HandlingException {
		if ("".equals(stateValue)) {
			// Empty state : do nothing
			return stateValue;
		}
		AgentState state = parseState(stateValue);
		for (String output : outputs) {
			state.addOutput(output);
		}
		return state.toString();
	}

	public static String addOutputsToLsaState(Lsa lsa, String[] outputs) throws HandlingException {
		String stateStr = lsa.getSyntheticProperty(SyntheticPropertyName.STATE).toString();
		AgentState state = parseState(stateStr);
		for (String output : outputs) {
			state.addOutput(output);
		}
		return state.toString();
	}

	public static String fusionStates(String[] state1, String[] state2) {
		String result = "";
		for (int i = 0; i < state1.length; i++) {
			if (state2.length > i && state1[i].equals(state2[i])) {
				result += state1[i] + ",";
			} else if (state2.length > i && !state1[i].equals(state2[i])) {
				for (int j = i; j < state1.length; j++) {
					result += state1[j] + ",";
				}
				for (int j = i; j < state2.length; j++) {
					result += state2[j] + ",";
				}
				break;
			} else if (state2.length <= i) {
				for (int j = i; j < state1.length; j++) {
					result += state1[j] + ",";
				}
				break;
			}
			if (i == state1.length - 1 && state2.length > i) {
				for (int j = i + 1; j < state2.length; j++) {
					result += state2[j] + ",";
				}
				break;
			}
		}
		return result.substring(0, result.length() - 1);
	}

	public static String getPreviousState(String newState, String[] output) {
		String[] state = newState.split(",");
		String[] newArray = Arrays.copyOfRange(state, 0, state.length - output.length);
		return String.join(",", newArray);
	}

	public static String implode(List<String> _list, String sep) {
		StringBuffer buffBonds = new StringBuffer();
		String sep2 = "";
		for (String item : _list) {
			buffBonds.append(sep2).append(item);
			sep2 = sep;
		}
		return buffBonds.toString();
	}

	public static String implode(Set<String> _list, String sep) {
		StringBuffer buffBonds = new StringBuffer();
		String sep2 = "";
		for (String item : _list) {
			buffBonds.append(sep2).append(item);
			sep2 = sep;
		}
		return buffBonds.toString();
	}

	public static boolean isInStrArray(String[] strArray, String tofind) {
		for (String next : strArray) {
			if (next.equals(tofind)) {
				return true;
			}
		}
		return false;
	}

	/*
	public static boolean isInDeviceCategoryrArray(DeviceCategory[] strArray, DeviceCategory tofind) {
		for (DeviceCategory next : strArray) {
			if (next.equals(tofind)) {
				return true;
			}
		}
		return false;
	}
	*/

	public static Object getNext(Object[] array, int idx) {
		if (array.length == 0) {
			return "";
		}
		int idx2 = idx;
		if (idx2 >= array.length) {
			idx2 = array.length - 1;
		}
		return array[idx2];
	}





	public static RegulationWarning getExpiredWarning(List<RegulationWarning> warnings) {
		for (RegulationWarning warning : warnings) {
			if (warning.hasReceptionExpired()) {
				return warning;
			}
		}
		return null;
	}

	public static String getExpiredOfferKey(Map<String, SingleOffer> tableSingleOffers, int marginSeconds) {
		return getExpiredOfferKey(tableSingleOffers, null, marginSeconds);
	}

	public static String getExpiredOfferKey(Map<String, SingleOffer> tableWaitingOffers,
			Map<String, ReducedContract> tableContracts, int marginSeconds) {
		for (String consumerKey : tableWaitingOffers.keySet()) {
			SingleOffer offer = tableWaitingOffers.get(consumerKey);
			if (offer.hasExpired(marginSeconds)) {
				return consumerKey;
			} else if (tableContracts != null && tableContracts.containsKey(consumerKey)) {
				return consumerKey;
			}
			/*
			 * if (offer.hasExpired(marginSeconds) || (tableContracts != null &&
			 * tableContracts.containsKey(consumerKey))) { return consumerKey; }
			 */
		}
		return null;
	}

	public static SingleOffer getOfferToCancel(Map<String, List<SingleOffer>> tableSingleOffers, int marginSeconds) {
		for (String consumerKey : tableSingleOffers.keySet()) {
			List<SingleOffer> listOffers = tableSingleOffers.get(consumerKey);
			for(SingleOffer offer : listOffers) {
				if (!offer.hasExpired(marginSeconds) && offer.getPriorityLevel().isLowerThan(PriorityLevel.HIGH)) {
					return offer;
				}
			}
		}
		return null;
	}

	public static ReducedContract getContractToCancel(Map<String, List<ReducedContract>> tableValidContracts) {
		for (String consumerKey : tableValidContracts.keySet()) {
			List<ReducedContract> listContract = tableValidContracts.get(consumerKey);
			for(ReducedContract reducedContract : listContract) {
				if (!reducedContract.hasExpired() && reducedContract.hasAllAgreements()
						&& reducedContract.getRequest().getPriorityLevel().isLowerThan(PriorityLevel.HIGH)) {
					return reducedContract;
				}
			}
		}
		return null;
	}

	public static Map<String, Float> filterRepartition(Map<String, Float> mapValues, List<String> keysFilter) {
		Map<String, Float> result = new HashMap<String, Float>();
		for (String key : mapValues.keySet()) {
			if (keysFilter.contains(key)) {
				result.put(key, mapValues.get(key));
			}
		}
		return result;
	}

	public static String formaMapValues(Map<String, Double> mapValues, DecimalFormat decFormat) {
		if (mapValues == null) {
			return "";
		}
		StringBuffer result = new StringBuffer();
		String sep = "";
		// Set<String> keys = mapValues.keySet();
		List<String> listKeys = new ArrayList<>();
		for (String key : mapValues.keySet()) {
			listKeys.add(key);
		}
		Collections.sort(listKeys);
		for (String key : listKeys) {
			Double value = mapValues.get(key);
			result.append(sep);
			result.append(key).append("(").append(decFormat.format(value)).append(")");
			sep = ",";
		}
		return result.toString();
	}

	public static int getIntValue(Object oValue) {
		if (oValue instanceof BigDecimal) {
			BigDecimal bdValue = (BigDecimal) oValue;
			return bdValue.intValue();
		} else if (oValue instanceof Long) {
			Long longValue = (Long) oValue;
			return longValue.intValue();
		} else if (oValue instanceof Integer) {
			Integer intValue = (Integer) oValue;
			return intValue.intValue();
		}
		return 0;
	}

	public static double getDoubleValue(Object oValue) {
		if (oValue instanceof BigDecimal) {
			BigDecimal bdValue = (BigDecimal) oValue;
			return bdValue.doubleValue();
		} else if (oValue instanceof Long) {
			Long longValue = (Long) oValue;
			return longValue.doubleValue();
		} else if (oValue instanceof Integer) {
			Integer intValue = (Integer) oValue;
			return intValue.doubleValue();
		} else if (oValue instanceof Double) {
			Double doubleValue = (Double) oValue;
			return doubleValue.doubleValue();
		} else if (oValue instanceof Float) {
			Float floatValue = (Float) oValue;
			return floatValue.doubleValue();
		}
		return 0;
	}

	public static double getDoubleValue(Map<String, Object> row, String columnName, AbstractLogger logger) {
		Object oValue = row.get(columnName);
		if (oValue instanceof BigDecimal) {
			BigDecimal bdValue = (BigDecimal) oValue;
			return bdValue.doubleValue();
		} else if (oValue instanceof Long) {
			Long longValue = (Long) oValue;
			return longValue.doubleValue();
		} else if (oValue instanceof Integer) {
			Integer intValue = (Integer) oValue;
			return intValue.doubleValue();
		} else if (oValue instanceof Double) {
			Double doubleValue = (Double) oValue;
			return doubleValue.doubleValue();
		} else if (oValue instanceof String) {
			String sValue = (String) oValue;
			if(!"".equals(sValue)) {
				try {
					return Double.valueOf(sValue);
				} catch (NumberFormatException e) {
					logger.error(e);
				}
			}
		}
		return 0;
	}

	public static boolean getBooleantValue(Map<String, Object> row, String columnName) {
		Object oValue = row.get(columnName);
		if (oValue instanceof BigDecimal) {
			BigDecimal bdValue = (BigDecimal) oValue;
			return bdValue.intValue() > 0;
		} else if (oValue instanceof Long) {
			Long longValue = (Long) oValue;
			return longValue.intValue() > 0;
		} else if (oValue instanceof Integer) {
			Integer intValue = (Integer) oValue;
			return intValue.intValue() > 0;
		} else if (oValue instanceof Boolean) {
			Boolean boolValue = (Boolean) oValue;
			return boolValue.booleanValue();
		}
		return false;
	}

	public static int getIntValue(Map<String, Object> row, String columnName) {
		Object oValue = row.get(columnName);
		if (oValue instanceof BigDecimal) {
			BigDecimal bdValue = (BigDecimal) oValue;
			return bdValue.intValue();
		} else if (oValue instanceof Long) {
			Long longValue = (Long) oValue;
			return longValue.intValue();
		} else if (oValue instanceof Integer) {
			Integer intValue = (Integer) oValue;
			return intValue.intValue();
		} else if (oValue instanceof String) {
			String sValue = (String) oValue;
			return Integer.valueOf(sValue);
		}
		return 0;
	}

	public static long getLongValue(Map<String, Object> row, String columnName) {
		Object oValue = row.get(columnName);
		if (oValue instanceof BigDecimal) {
			BigDecimal bdValue = (BigDecimal) oValue;
			return bdValue.longValue();
		} else if (oValue instanceof Long) {
			Long longValue = (Long) oValue;
			return longValue.longValue();
		} else if (oValue instanceof Integer) {
			Integer intValue = (Integer) oValue;
			return intValue.longValue();
		} else if (oValue instanceof BigInteger) {
			BigInteger intValue = (BigInteger) oValue;
			return intValue.longValue();
		} else if (oValue instanceof String) {
			String sValue = (String) oValue;
			return Long.valueOf(sValue);
		}
		return 0;
	}

	public static float getFloatValue(Map<String, Object> row, String columnName) {
		Object oValue = row.get(columnName);
		if (oValue instanceof BigDecimal) {
			BigDecimal bdValue = (BigDecimal) oValue;
			return bdValue.floatValue();
		} else if (oValue instanceof Long) {
			Long longValue = (Long) oValue;
			return longValue.floatValue();
		} else if (oValue instanceof Integer) {
			Integer intValue = (Integer) oValue;
			return intValue.floatValue();
		}
		return 0;
	}

	public static Date getDateValue(Map<String, Object> row, String columnName, AbstractLogger logger) {
		Object oValue = row.get(columnName);
		if(oValue instanceof Date) {
			Date dateValue = (Date) oValue;
			return new Date(dateValue.getTime());
			//return dateValue;
		} else if (oValue instanceof String) {
			String sValue = (String) oValue;
			try {
				if(sValue.length() == 10) {
					return UtilDates.format_sql_day.parse(sValue);
				} else if (sValue.length() > 10) {
					// TODO check if sValue.length() == 19 works
					return UtilDates.format_sql.parse(sValue);
				}
			} catch (Exception e) {
				logger.error("ParseException rejected in SapereUtil.getDateValue  : sValue = " + sValue + " (len=" + sValue.length() + ")");
				logger.error(e);
			}
		}
		return null;
	}

	public static DeviceCategory getDeviceCategoryValue(Map<String, Object> row, String columnName) {
		String sDeviceCategory = "" + row.get(columnName);
		return DeviceCategory.getByName(sDeviceCategory);
	}

	public static EnvironmentalImpact getEnvironmentalImpactValue(Map<String, Object> row, String columnName) {
		Integer level = getIntValue(row, columnName);
		return EnvironmentalImpact.getByLevel(level);
	}

	public static PriorityLevel getPriorityLevelValue(Map<String, Object> row, String columnName) {
		Integer level = getIntValue(row, columnName);
		return PriorityLevel.getByLevel(level);
	}

	public static List<String> getListStrValue(Map<String, Object> row, String columnName) {
		List<String> listValue = new ArrayList<String>();
		Object oValue = row.get(columnName);
		String sValue = "" + oValue;
		for (String nextissingRequest : sValue.split(",")) {
			listValue.add(nextissingRequest);
		}
		return listValue;
	}

	public static String stackTraceToString(Throwable e) {
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement element : e.getStackTrace()) {
			sb.append(element.toString());
			sb.append("\n");
		}
		return sb.toString();
	}

	public static double roundPower(double d) {
		return round(d, NB_DEC_POWER);
	}

	public static String auxLogMap(Map<String, Double> mapToLog, int nbDec) {
		StringBuffer result = new StringBuffer();
		String sep = "";
		for (String key : mapToLog.keySet()) {
			double value = mapToLog.get(key);
			result.append(sep).append(key).append(" = ").append(SapereUtil.round(value, nbDec));
			sep=", ";
		}
		return result.toString();
	}

	public static String auxLogMap2(Map<Date, Double> mapToLog, int nbDec, boolean useShortFormat) {
		StringBuffer result = new StringBuffer();
		String sep = "";
		Date lastDate = null;
		for (Date key : mapToLog.keySet()) {
			double value = mapToLog.get(key);
			double rondedValue = SapereUtil.round(value, nbDec);
			String sKey = UtilDates.format_time.format(key);
			if(useShortFormat && lastDate !=null && Math.abs(UtilDates.computeDurationSeconds(key, lastDate)) < 1.1) {
				result.append(sep).append(rondedValue);
			} else {
				result.append(sep).append(sKey).append(" : ").append(rondedValue);
			}
			sep=", ";
			lastDate = key;
		}
		return result.toString();
	}

	public static String auxLogComposedMap(Map<String, Map<Date, Double>> mapToLog, int nbDec, boolean useShortFormat) {
		StringBuffer result = new StringBuffer();
		String sep =CR + " . ";
		for (String key : mapToLog.keySet()) {
			Map<Date, Double> value = mapToLog.get(key);
			result.append(sep).append(key).append(" = ").append(auxLogMap2(value, nbDec, useShortFormat));
			//sep=CR;
		}
		return result.toString();
	}

	public static String auxLogMapPowers(Map<String, Double> mapToLog) {
		return auxLogMap(mapToLog, NB_DEC_POWER);
	}

	public static String auxLogComposedMapPowers(Map<String, Map<Date, Double>> mapToLog, boolean useShortFormat) {
		return auxLogComposedMap(mapToLog, NB_DEC_POWER, useShortFormat);
	}

	public static double round(double d, int decimalPlace) {
		double multiplier = Math.pow(10, decimalPlace);
		double result = Math.round(d * multiplier) / multiplier;
		return result;
		// return
		// BigDecimal.valueOf(d).setScale(decimalPlace,BigDecimal.ROUND_HALF_UP).doubleValue();
	}

	public static boolean checkPowerRounded(double d, AbstractLogger logger) {
		return checkRounded(d, NB_DEC_POWER, logger);
	}

	public static boolean checkRounded(double d, int decimalPlace, AbstractLogger logger) {
		double rounded = round(d, decimalPlace);
		double roundGap = Math.abs(d - rounded);
		if (roundGap >= 0.0001) {
			logger.warning("### checkIsRound " + d + " roundGap = " + roundGap);
		}
		return (roundGap < 0.0001);
	}

	public static String firstCharToLower(String aStr) {
		if (aStr != null && aStr.length() > 0) {
			char firstChar = Character.toLowerCase(aStr.charAt(0));
			String result = firstChar + aStr.substring(1);
			return result;
		}
		return aStr;
	}

	public static boolean checkParams(Map<String, String> toCheck, Map<String, String> paramsReference)
			throws HandlingException {
		for (String field : paramsReference.keySet()) {
			String refValue = paramsReference.get(field);
			if (refValue != null && !"null".equals(refValue)) {
				if (toCheck.containsKey(field)) {
					String tocheckValue = toCheck.get(field);
					if (!refValue.equals(tocheckValue)) {
						throw new HandlingException("generateAgentFormParams " + field + " values"
								+ " not equals in param and params1 " + refValue + " " + tocheckValue);
					}
				} else {
					throw new HandlingException("generateAgentFormParams " + field + " not in params1 ");
				}
			}
		}
		return true;
	}

	public static String generateFilterCategory(List<DeviceCategory> categoryFilter, String sqlFieldName) {
		StringBuffer sCategoryFilter = new StringBuffer("1");
		if (categoryFilter != null && categoryFilter.size() > 0) {
			String sep = "";
			sCategoryFilter = new StringBuffer().append(sqlFieldName).append(" IN (");
			for (DeviceCategory category : categoryFilter) {
				sCategoryFilter.append(sep).append("'").append(category).append("'");
				sep = ",";
			}
			sCategoryFilter.append(")");
		}
		return sCategoryFilter.toString();
	}

	public static String addDoubleQuote(String str) {
		return DOUBLE_QUOTE + str + DOUBLE_QUOTE;
	}

	public static String addSingleQuotes(String str) {
		StringBuffer quoted = new StringBuffer();
		quoted.append(SINGLE_QUOTE).append(str).append(SINGLE_QUOTE);
		return quoted.toString();
	}

	public static String addSingleQuotes(Integer number) {
		StringBuffer quoted = new StringBuffer();
		quoted.append(SINGLE_QUOTE).append(number).append(SINGLE_QUOTE);
		return quoted.toString();
	}

	public static String addSingleQuotes(Long number) {
		StringBuffer quoted = new StringBuffer();
		quoted.append(SINGLE_QUOTE).append(number).append(SINGLE_QUOTE);
		return quoted.toString();
	}

	public static String addSingleQuotes(Float number) {
		StringBuffer quoted = new StringBuffer();
		quoted.append(SINGLE_QUOTE).append(number).append(SINGLE_QUOTE);
		return quoted.toString();
	}

	public static String addSingleQuotes(Double number) {
		StringBuffer quoted = new StringBuffer();
		quoted.append(SINGLE_QUOTE).append(number).append(SINGLE_QUOTE);
		return quoted.toString();
	}

	public static Double auxComputeMapTotal(Map<String, Double> mapValues) {
		double result = 0;
		for (Double nextPower : mapValues.values()) {
			result += nextPower;
		}
		return result;
	}

	@SafeVarargs
	public static List<TimeSlot> mergeTimeSlots(List<TimeSlot> ... listOfListOfTimeSlots) {
		List<Date> listBeginDates = new ArrayList<Date>(); // TO DELETE !!!!!!!
		List<Date> listEndDates = new ArrayList<Date>(); // TO DELETE
		List<Date> listDates = new ArrayList<Date>();
		for(List<TimeSlot> listTimeSlots : listOfListOfTimeSlots) {
			for(TimeSlot nextTimeSlot : listTimeSlots) {
				Date nextBeginDate = nextTimeSlot.getBeginDate();
				if(!listBeginDates.contains(nextBeginDate)) {
					listBeginDates.add(nextBeginDate);
				}
				Date nextEndDate = nextTimeSlot.getEndDate();
				if(!listEndDates.contains(nextEndDate)) {
					listEndDates.add(nextEndDate);
				}
				if(!listDates.contains(nextBeginDate)) {
					listDates.add(nextBeginDate);
				}
				if(!listDates.contains(nextEndDate)) {
					listDates.add(nextEndDate);
				}
			}
		}
		// Sort list of begin date and list of end date by date order
		Collections.sort(listBeginDates);
		Collections.sort(listEndDates);
		Collections.sort(listDates);
		List<TimeSlot> listTimeSlots = new ArrayList<TimeSlot>();
		/*
		for(Date beginDate : listBeginDates) {
			Date endDate = null;
			for(Date nextEndDate : listEndDates) {
				if(endDate == null && beginDate.before(nextEndDate)) {
					endDate = nextEndDate;
				}
			}
			listTimeSlots.add(new TimeSlot(beginDate, endDate));
		}*/
		Date lastDate = null;
		for(Date nextDate : listDates) {
			if(lastDate != null) {
				listTimeSlots.add(new TimeSlot(lastDate, nextDate));
			};
			lastDate = nextDate;
		}
		return listTimeSlots;
	}

	public static ComposedRate auxComputeAvgRate(
			 Map<String, Double> mapPowers
			,Map<String, PricingTable> mapPricingTables
			,Date aDate) {
		double totalPower = 0;
		ComposedRate avgRate = new ComposedRate(0.0 , null);
		// Compute power total
		for(String key : mapPowers.keySet()) {
			if(mapPricingTables.containsKey(key)) {
				double power = mapPowers.get(key);
				totalPower+= power;
			}
		}
		// add rate of all pricing tables at beginDate
		if(totalPower > 0) {
			for(String key : mapPowers.keySet()) {
				if(mapPricingTables.containsKey(key)) {
					double power = mapPowers.get(key);
					PricingTable pricingTable = mapPricingTables.get(key);
					ComposedRate rate = pricingTable.getRate(aDate);
					if(rate == null) {
						SapereLogger.getInstance().error("auxComputeAvgRate : no rate found for the key " + key + " and the date " + UtilDates.format_time.format(aDate)
						+ SapereUtil.CR + ", mapPricingTables = " + mapPricingTables);
					} else {
						double weithtFactor = power/totalPower;
						avgRate.add(rate.applyRiseFactor(weithtFactor));
					}
				}
			}
			return avgRate;
		}
		return new ComposedRate(0);
	}

	public static PricingTable auxComputeMapPricingTable(
			 Map<String, Double> mapPowers
			,Map<String, PricingTable> mapPricingTables
			,long timeShiftMS) {
		if(mapPricingTables.size() == 0) {
			return new PricingTable(timeShiftMS);
		} else if(mapPricingTables.size() == 1) {
			String producer = mapPricingTables.keySet().iterator().next();
			return mapPricingTables.get(producer);
		} else {
			// Step 1 : merge date list
			List<Date> allDates = new ArrayList<>();
			Date maxBeginDate = null;
			for(PricingTable nextPricingTable : mapPricingTables.values()) {
				Date nextBeginDate = nextPricingTable.getBeginDate();
				if (maxBeginDate == null || nextBeginDate.after(maxBeginDate)) {
					maxBeginDate = nextBeginDate;
				}
				for(Date nextDate : nextPricingTable.getDates()) {
					if(!allDates.contains(nextDate)) {
						allDates.add(nextDate);
					}
				}
			}
			Collections.sort(allDates);
			PricingTable result = new PricingTable(timeShiftMS);
			for(Date nextDate : allDates) {
				// compute average rate of all pricing tables at beginDate
				if (maxBeginDate != null && nextDate.before(maxBeginDate)) {
					SapereLogger.getInstance()
							.info("auxComputeMapPricingTable : nextDate " + UtilDates.format_time.format(nextDate)
									+ " is before maxBeginDate " + UtilDates.format_time.format(maxBeginDate));
				} else {
					ComposedRate avgRate = auxComputeAvgRate(mapPowers, mapPricingTables, nextDate);
					result.putRate(nextDate, avgRate);
				}
			}
			return result;
		}
	}

	public static boolean areMapDifferent(Map<String, Double> map1, Map<String, Double> map2) {
		if (map1 == null) {
			return (map2 != null);
		}
		if (map2 == null) {
			return (map1 != null);
		}
		if (map1.size() != map2.size()) {
			return true;
		}
		for (String key : map1.keySet()) {
			Double power = map1.get(key);
			if (!map2.containsKey(key)) {
				return true;
			}
			Double newPower = map2.get(key);
			if (Math.abs(power - newPower) >= 0.0001) {
				return true;
			}
		}
		return false;
	}

	public static boolean areMapPowerSlotDifferent(
			 Map<String, PowerSlot> map1
			,Map<String, PowerSlot> map2) {
		if (map1 == null) {
			return (map2 != null);
		}
		if (map2 == null) {
			return (map1 != null);
		}
		if (map1.size() != map2.size()) {
			return true;
		}
		for (String key : map1.keySet()) {
			PowerSlot power = map1.get(key);
			if (!map2.containsKey(key)) {
				return true;
			}
			PowerSlot newPower = map2.get(key);
			if(power.areDifferent(newPower)) {
				return true;
			}
		}
		return false;
	}

	public static void adjustMapValues(Map<String, Double> mapCurrentValues, Double valueToSet
			,Map<String, Double> mapMinValues
			,Map<String, Double> mapMaxValues
			,AbstractLogger logger) {
		double currentTotalValue = auxComputeMapTotal(mapCurrentValues);
		double delta = valueToSet - currentTotalValue;
		double minValue = auxComputeMapTotal(mapMinValues);
		double maxValue = auxComputeMapTotal(mapMinValues);
		if(valueToSet < minValue) {
			logger.warning("adjustMapValues valueToSet " + valueToSet + " is bellow the min value " + minValue);
		}
		if(valueToSet > maxValue) {
			logger.warning("adjustMapValues valueToSet " + valueToSet + " is above the max value " + maxValue);
		}
		if (delta != 0 && Math.abs(delta) >= 0.0001) {
			// select keys where the associated value wont'be higher than the max value
			// after the correction
			List<String> keys = new ArrayList<String>();
			Map<String, PowerSlot> mapTolerance = new HashMap<>();
			for (String key : mapCurrentValues.keySet()) {
				Double min = null;
				Double max = null;
				if (mapMaxValues != null && mapMaxValues.containsKey(key)) {
					max = mapMaxValues.get(key);
				}
				if (mapMinValues != null && mapMinValues.containsKey(key)) {
					min = mapMinValues.get(key);
				}
				double currentValue = mapCurrentValues.get(key);
				double correctedValue = mapCurrentValues.get(key) + delta;
				if (min != null && max != null) {
					// if delta < 0 : new PowerSlot(currentValue, Math.max(min, correctedValue), currentValue
					// if delta > 0 : new PowerSlot(currentValue, currentValue, Math.min(max, correctedValue)
					PowerSlot totlerance = new PowerSlot(currentValue,
							delta < 0 ? Math.max(min, correctedValue) : currentValue,
							delta > 0 ? Math.min(max, correctedValue) : currentValue);
					mapTolerance.put(key, totlerance);
				}
				//check if the value to set is between min and max
				if ((min == null || correctedValue >= min) && (max == null || correctedValue <= max)) {
					keys.add(key);
				}
			}
			if (keys.size() > 0) {
				// Chose randomly a value to adjust
				Collections.shuffle(keys);
				String producer = keys.get(0);
				double newPower = mapCurrentValues.get(producer) + delta;
				mapCurrentValues.put(producer, newPower);
			} else {
				logger.warning("adjustMapValues correction not possible mapTolerance = " + mapTolerance);
			}
		}
	}

	public static Map<String, String> retrieveArgsOptions(String args[]) {
		Map<String, String> options = new HashMap<String, String>();
		for (String arg : args) {
			if (arg.startsWith("-") && arg.contains(":")) {
				String sOption = arg.substring(1);
				String[] array = sOption.split(":");
				if (array.length > 1) {
					String optionKey = array[0];
					String optionValue = sOption.substring(optionKey.length() + 1);
					options.put(optionKey, optionValue);
				}
			}
		}
		return options;
	}

	public static Properties loadPropertyFile(String node, AbstractLogger logger) {
		String path = "application.properties";
		if (node != null && node.length() > 0) {
			path = "application-" + node.toLowerCase() + ".properties";
		}
		try (InputStream input = new FileInputStream(path)) {
			Properties prop = new Properties();

			// load a properties file
			prop.load(input);

			// get the property value and print it out
			logger.info("server.port = " + prop.getProperty("server.port"));
			return prop;
		} catch (IOException ex) {
			logger.error(ex);
		}
		return null;
	}

	public static Map<Date, List<DeviceMeasure>> auxMergeMapMeasures15MN(Map<Date, List<DeviceMeasure>> mapMeasures,
			List<DeviceMeasure> listMeasures15M, Date dateMeasure15M, Date dateNextMeasure15M, AbstractLogger logger) {
		if (dateMeasure15M != null) {
			for (Date dateMM : mapMeasures.keySet()) {
				if (!dateMM.before(dateMeasure15M) && dateMM.before(dateNextMeasure15M)) {
					List<DeviceMeasure> measures = mapMeasures.get(dateMM);
					for (DeviceMeasure measure15M : listMeasures15M) {
						DeviceMeasure toAdd = measure15M.clone();
						toAdd.setDatetime(dateMM);
						measures.add(toAdd);
					}
				}
			}
		}
		return mapMeasures;
	}

	public static Map<Date, List<DeviceMeasure>> shiftMeasureDates(Map<Date, List<DeviceMeasure>> mapMeasures, int dayCorrection) {
		Map<Date, List<DeviceMeasure>> result = new HashMap<>();
		for(Date nextDate : mapMeasures.keySet()) {
			Date nextDate2 = UtilDates.shiftDateDays(nextDate, dayCorrection);
			List<DeviceMeasure> measures = new ArrayList<DeviceMeasure>();
			for(DeviceMeasure nextMeasure : mapMeasures.get(nextDate)) {
				Date measureDate = nextMeasure.getDatetime();
				nextMeasure.setDatetime(UtilDates.shiftDateDays(measureDate, dayCorrection));
				measures.add(nextMeasure);
			}
			result.put(nextDate2, measures);
		}
		return result;
	}

	// Check producers confirmations
	public static Set<String> getAgentNamesWithNoRecentConfirmations(
			EnergyAgent mainAgent
			,Set<String> listAgentNames
			,Map<String, ConfirmationItem> receivedConfirmations
			, int marginSec
			,AbstractLogger logger) {
		Set<String> result = new HashSet<String>();
		for (String agentName : listAgentNames) {
			if (receivedConfirmations.containsKey(agentName)) {
				ConfirmationItem producerConfirmation = receivedConfirmations.get(agentName);
				if (producerConfirmation.hasExpired(marginSec)) {
					logger.warning(mainAgent.getAgentName() + " : no recent confirmation from " + agentName
							+ " : last received : " + producerConfirmation);
					result.add(agentName);
				}
			} else {
				logger.warning(mainAgent.getAgentName() + " : no confirmation from " + agentName);
				result.add(agentName);
			}
		}
		return result;
	}

	public static Map<Date, List<DeviceMeasure>> auxMergeMapMeasures15MN(Map<Date, List<DeviceMeasure>> mapMeasures,
			Map<Date, List<DeviceMeasure>> mapMeasures15MN, AbstractLogger logger) {
		List<TimeSlot> listTimeSlots15MN = new ArrayList<TimeSlot>();
		List<Date> listTimes15MN = new ArrayList<Date> (mapMeasures15MN.keySet());
		Collections.sort(listTimes15MN);
		Date lastDate = null;
		for (Date nextDate : listTimes15MN) {
			if (lastDate != null) {
				listTimeSlots15MN.add(new TimeSlot(lastDate, nextDate));
			}
			lastDate = nextDate;
		}
		for (TimeSlot slot15MN : listTimeSlots15MN) {
			Date date15MN_min = slot15MN.getBeginDate();
			Date date15MN_max = slot15MN.getEndDate();
			List<DeviceMeasure> listMeasures15M = mapMeasures15MN.get(date15MN_min);
			mapMeasures = auxMergeMapMeasures15MN(mapMeasures, listMeasures15M, date15MN_min, date15MN_max, logger);
		}
		return mapMeasures;
	}

	public static Set<String> cloneSetStr(Set<String> toCopy) {
		Set<String> copy = new HashSet<>();
		for (String next : toCopy) {
			copy.add(next);
		}
		return copy;
	}

	public static PowerSlot add(PowerSlot slot1, PowerSlot slot2) {
		if(slot1==null) {
			return slot2;
		}
		PowerSlot result = new PowerSlot(slot1.getCurrent(), slot1.getMin(), slot1.getMax());
		result.add(slot2);
		return result;
	}

	public static Set<String> mergeSetStr(Set<String> set1, Set<String> set2) {
		Set<String> result = new HashSet<String>();
		for(String str : set1) {
			result.add(str);
		}
		for(String str : set2) {
			result.add(str);
		}
		return result;
	}

	public static List<String> mergeListStr(List<String> list1, List<String> list2) {
		List<String> result = new ArrayList<>();
		for(String str : list1) {
			if(!result.contains(str)) {
				result.add(str);
			}
		}
		for(String str : list2) {
			if(!result.contains(str)) {
				result.add(str);
			}
		}
		return result;
	}

	public static Map<String, Double> mergeMapStrDouble(Map<String, Double> map1, Map<String, Double> map2) {
		Map<String, Double> result = new HashMap<String, Double>();
		for(String key : map1.keySet()) {
			Double value = Double.valueOf(map1.get(key));
			result.put(key, value);
		}
		for(String key : map2.keySet()) {
			Double value = map2.get(key);
			if(result.containsKey(key)) {
				Double valueToUpdate = result.get(key) + value;
				result.put(key, valueToUpdate);
			} else {
				result.put(key, value);
			}
		}
		return result;
	}

	public static Map<String, PowerSlot> mergeMapStrPowerSlot(Map<String, PowerSlot> map1, Map<String, PowerSlot> map2) {
		Map<String, PowerSlot> result = new HashMap<String, PowerSlot>();
		for(String key : map1.keySet()) {
			PowerSlot slot = map1.get(key);
			result.put(key, new PowerSlot(slot.getCurrent(), slot.getMin(), slot.getMax()));
		}
		for(String key : map2.keySet()) {
			PowerSlot slot = map2.get(key);
			if(result.containsKey(key)) {
				PowerSlot slotToUpdate = result.get(key);
				slotToUpdate.add(slot);
			} else {
				result.put(key, new PowerSlot(slot.getCurrent(), slot.getMin(), slot.getMax()));
			}
		}
		return result;
	}

	public static Map<String, List<EnergyRequest>> mergeMapStrRequest(
			Map<String, EnergyRequest> map1,
			Map<String, EnergyRequest> map2,
			AbstractLogger logger) {
		Map<String, List<EnergyRequest>> result = new HashMap<String, List<EnergyRequest>>();
		for(String key : map1.keySet()) {
			EnergyRequest request = map1.get(key);
			List<EnergyRequest> listRequests = new ArrayList<EnergyRequest>();
			listRequests.add(request);
			result.put(key, listRequests);
		}
		for(String key : map2.keySet()) {
			if(!result.containsKey(key)) {
				result.put(key, new ArrayList<EnergyRequest>());
			}
			List<EnergyRequest> listRequests = result.get(key);
			EnergyRequest request = map2.get(key);
			listRequests.add(request.clone());
		}
		return result;
	}

	public static Map<String, List<SingleOffer>> mergeMapStrOffer(
			Map<String, SingleOffer> map1,
			Map<String, SingleOffer> map2,
			AbstractLogger logger) {
		Map<String, List<SingleOffer>> result = new HashMap<String, List<SingleOffer>>();
		for(String key : map1.keySet()) {
			SingleOffer offer = map1.get(key);
			List<SingleOffer> listOffers = new ArrayList<SingleOffer>();
			listOffers.add(offer);
			result.put(key, listOffers);
		}
		for(String key : map2.keySet()) {
			if(!result.containsKey(key)) {
				result.put(key, new ArrayList<SingleOffer>());
			}
			List<SingleOffer> listOffers = result.get(key);
			SingleOffer offer = map2.get(key);
			listOffers.add(offer.clone());
		}
		return result;
	}

	public static Map<String, List<ReducedContract>> mergeMapStrRContract(
			Map<String, ReducedContract> map1, Map<String, ReducedContract> map2,
			AbstractLogger logger) {
		Map<String, List<ReducedContract>> result = new HashMap<String, List<ReducedContract>>();
		for(String key : map1.keySet()) {
			ReducedContract contract = map1.get(key);
			List<ReducedContract> listContracts = new ArrayList<ReducedContract>();
			listContracts.add(contract);
			result.put(key, listContracts);
		}
		for(String key : map2.keySet()) {
			if(!result.containsKey(key)) {
				result.put(key, new ArrayList<ReducedContract>());
			}
			List<ReducedContract> listContracts = result.get(key);
			ReducedContract contract = map2.get(key);
			listContracts.add(contract.clone());
		}
		return result;
	}

	public static Collection<EnergyRequest> mergeCollectionRequests(Collection<EnergyRequest> col1,  Collection<EnergyRequest> col2) {
		List<EnergyRequest> result = new ArrayList<EnergyRequest>();
		for(EnergyRequest contrat : col1) {
			result.add(contrat);
		}
		for(EnergyRequest contrat : col2) {
			result.add(contrat);
		}
		return result;
	}

	public static Collection<SingleOffer> mergeCollectionOffers( Collection<SingleOffer> col1,  Collection<SingleOffer> col2) {
		List<SingleOffer> result = new ArrayList<SingleOffer>();
		for(SingleOffer offer : col1) {
			result.add(offer);
		}
		for(SingleOffer offer : col2) {
			result.add(offer);
		}
		return result;
	}

	public static Collection<ReducedContract> mergeCollectionContracts( Collection<ReducedContract> col1,  Collection<ReducedContract> col2) {
		List<ReducedContract> result = new ArrayList<ReducedContract>();
		for(ReducedContract contrat : col1) {
			result.add(contrat);
		}
		for(ReducedContract contrat : col2) {
			result.add(contrat);
		}
		return result;
	}

	public static Date getClosestDate(Date targetDate, Collection<Date> listDates) {
		Date closestDate = null;
		for (Date nextDate : listDates) {
			//double nextGapMin = (Math.abs(nextDate.getTime() - targetDate.getTime()))/60000;
			if (closestDate == null || (
					Math.abs(nextDate.getTime() - targetDate.getTime()) <
					Math.abs(closestDate.getTime() - targetDate.getTime()))) {
				closestDate = nextDate;
			}
		}
		return closestDate;
	}

	public static Map<String, Double> computeProfileDistance(
			List<SingleNodeStateItem> listStateHistory1,
			List<SingleNodeStateItem> listStateHistory2,
			AbstractLogger logger) {
		// TreeMap<Date, SingleNodeStateItem> mapHistory1 =
		// selectMapHistory(listStateHistory1, null, null);
		if(listStateHistory2.isEmpty()) {
			logger.error("computeProfileDistance : listStateHistory2 is empty ");
			return new HashMap<String, Double>();
		}
		TreeMap<Date, SingleNodeStateItem> mapHistory2 = selectMapHistory(listStateHistory2, null, null);
		Map<String, List<Double>> mapAllDistances = new HashMap<String, List<Double>>();
		int nbReconciliations = 0;
		for (SingleNodeStateItem stateHistory : listStateHistory1) {
			Date date = stateHistory.getDate();
			Date date2 = getClosestDate(date, mapHistory2.keySet());
			double datesDistanceMin = (Math.abs(date2.getTime() - date.getTime())) / (60 * 1000);
			if (datesDistanceMin <= 3) {
				nbReconciliations++;
				SingleNodeStateItem stateHistory2 = mapHistory2.get(date2);
				for (String variable : stateHistory.getMapValues().keySet()) {
					double powerDifferential = Math.abs(stateHistory.getValue(variable) - stateHistory2.getValue(variable));
					if (!mapAllDistances.containsKey(variable)) {
						mapAllDistances.put(variable, new ArrayList<Double>());
					}
					List<Double> listDistances = mapAllDistances.get(variable);
					listDistances.add(powerDifferential);
				}
			}
		}
		if(nbReconciliations == 0) {
			logger.error("computeProfileDistance : no Reconciliations found between " + listStateHistory1 + " and " + listStateHistory2);
		}
		Map<String, Double> result = new HashMap<String, Double>();
		for (String variable : mapAllDistances.keySet()) {
			List<Double> listDistances = mapAllDistances.get(variable);
			double distanceAvg = auxComputeAvg(listDistances);
			result.put(variable, distanceAvg);
		}
		return result;
	}

	public static TreeMap<Date, SingleNodeStateItem> selectMapHistory(List<SingleNodeStateItem> listeNodeStateItem, Date initDate, Date lastDate) {
		TreeMap<Date, SingleNodeStateItem> result = new TreeMap<Date, SingleNodeStateItem>();
		Date minDate = (initDate == null)? null : UtilDates.shiftDateMinutes(initDate, -3);
		Date maxDate = (lastDate == null)? null : UtilDates.shiftDateMinutes(lastDate, 3);
		for(SingleNodeStateItem nextState : listeNodeStateItem) {
			Date nextDate = nextState.getDate();
			// Chek if next Date if between initDate and lastDate
			if(minDate == null || !nextDate.before(minDate)) {
				if(maxDate == null || nextDate.before(maxDate)) {
					result.put(nextDate, nextState.clone());
				}
			}
		}
		return result;
	}

	public static SingleNodeStateItem selectClosestStateItem(List<SingleNodeStateItem> localStateHistory,
			Date targetDate, AbstractLogger logger) {
		TreeMap<Date, SingleNodeStateItem> selectMapHistory = selectMapHistory(localStateHistory
				,UtilDates.shiftDateMinutes(targetDate, -60)
				,UtilDates.shiftDateMinutes(targetDate, 60));
		Date closestDate = getClosestDate(targetDate, selectMapHistory.keySet());
		if(closestDate != null) {
			return selectMapHistory.get(closestDate);
		}
		return null;
	}

	public static double computeShannonEntropie(Map<String, Integer> distribution) {
		// Compute total cardinality
		int totalNb = 0;
		for(Integer nextCardinality : distribution.values()) {
			totalNb+= nextCardinality;
		}
		double result = 0;
		for(Integer nextCardinality : distribution.values()) {
			double itemWeight = ((double) nextCardinality)/totalNb;
			result+=-1*itemWeight*Math.log(itemWeight)/Math.log(2);
		}
		return result;
	}

	public static Double computeCrossEntroy(AbstractLogger logger
			, List<Double> listValues1
			, List<Double> listValues2) {
		if(listValues1 != null && listValues2 != null && listValues1.size() > 0 && listValues2.size() > 0) {
			if(listValues2.size() == listValues2.size()) {
				double result = 0;
				for(int stateIdx=0; stateIdx < listValues1.size(); stateIdx++) {
					double value1 = listValues1.get(stateIdx);
					double value2 = listValues2.get(stateIdx);
					if(value1 == 0 && value2 ==0) {
						// Nothing to do (add 0.0)
					} else {
						double toAdd = -value1 * Math.log(value2);
						if(Double.isNaN(toAdd)) {
							logger.warning("computeCrossEntroy : NaN result : nextStateRatioTrue = " + value1 + ", nextStateRatioPredicted = " + value2);
						} else if (Double.isInfinite(toAdd)) {
							logger.warning("computeCrossEntroy : infinity result : nextStateRatioTrue = " + UtilDates.df3.format(value1)
								+ ", nextStateRatioPredicted = " + UtilDates.df3.format(value2));
						}
						result+= toAdd;
					}
				}
				return result;
			}
		}
		return null;
	}

	/**
	 * Generates all combinations of a sub-list of r elements of a list (r=1,2 .... fields.size)
	 * @param fields
	 * @return
	 */
	public static List<List<String> > generateAllFieldsCombinations(List<String> fields) {
		List<List<String>> result = new ArrayList<>();
		int maxFieldsNb = fields.size();
		for(int nbFields = 1; nbFields <= maxFieldsNb; nbFields++) {
			List<int[]> combinations = generateIndexCombinations(maxFieldsNb, nbFields);
			for(int[] nextCombination : combinations) {
				List<String> usedFields = new ArrayList<>();
				for(int fieldIndex : nextCombination) {
					usedFields.add(fields.get(fieldIndex));
				}
				result.add(usedFields);
			}
		}
		return result;
	}

	/**
	 * Generates all combinations of a subset of r elements in a set of n elements (n>= r)
	 * @param n
	 * @param r
	 * @return
	 */
	public static List<int[]> generateIndexCombinations(int n, int r) {
	    List<int[]> combinations = new ArrayList<>();
	    int[] combination = new int[r];

	    // initialize with lowest lexicographic combination
	    for (int i = 0; i < r; i++) {
	        combination[i] = i;
	    }

	    while (combination[r - 1] < n) {
	        combinations.add(combination.clone());

	         // generate next combination in lexicographic order
	        int t = r - 1;
	        while (t != 0 && combination[t] == n - r + t) {
	            t--;
	        }
	        combination[t]++;
	        for (int i = t + 1; i < r; i++) {
	            combination[i] = combination[i - 1] + 1;
	        }
	    }

	    return combinations;
	}

	public static String loadRessourceContent(String propFileName) throws IOException {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		InputStream stream = loader.getResourceAsStream(propFileName);
		if(stream == null) {
			stream = loader.getResourceAsStream("resources/"+propFileName);
			System.out.println("loadProperties : step2 : stream = " + stream);
		}
		if(stream == null) {
			System.out.println("### resource " +  propFileName + " not loaded");
		}
		StringBuilder textBuilder = new StringBuilder();
		try(Reader reader = new BufferedReader(new InputStreamReader(stream))) {
	        int c = 0;
	        while ((c = reader.read()) != -1) {
	            textBuilder.append((char) c);
	        }
		} catch (IOException e) {
			throw e;
		}
	    return textBuilder.toString();
	}

	public static Properties loadProperties(String propFileName) throws IOException {
		Properties prop = new Properties();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		InputStream stream = loader.getResourceAsStream(propFileName);
		if(stream == null) {
			stream = loader.getResourceAsStream("resources/"+propFileName);
			System.out.println("loadProperties : step2 : stream = " + stream);
		}
		if(stream == null) {
			System.out.println("### resource " +  propFileName + " not loaded");
		}
		prop.load(stream);
		System.out.println("loadProperties : loaded properties = " + prop);
		return prop;
	}


	public static Properties addParamProperties(Properties initialProperties, String args[]) {
		Properties prop = initialProperties;
		for (String arg : args) {
			Matcher matcher = patternOption.matcher(arg);
			if (matcher.find()) {
				//int groupCouht = matcher.groupCount();
				String param = matcher.group("param");
				String value = matcher.group("value");
				prop.put(param, value);
			}
		}
		return prop;
	}


	public static NodeLocation loadNodeLocation(Properties prop) {
		String nodeName = prop.getProperty("lsa_server.name");
		String host = prop.getProperty("lsa_server.host");
		String sMainPort = prop.getProperty("lsa_server.port");
		Integer mainPort = Integer.valueOf(sMainPort);
		String sRestport = prop.getProperty("server.port");
		Integer restPort = Integer.valueOf(sRestport);
		NodeLocation result = new NodeLocation(nodeName, host, mainPort, restPort);
		return result;
	}

	public static DBConfig loadDBConfig(Properties prop) {
		String driverClassName = prop.getProperty("spring.datasource.driver-class-name");
		String url = prop.getProperty("spring.datasource.url");
		String username = prop.getProperty("spring.datasource.username");
		String password = prop.getProperty("spring.datasource.password");
		//DBConfig dbConfig = new DBConfig("jdbc:mariadb://localhost/energy1", "learning_agent", "sql2537");
		DBConfig dbConfig = new DBConfig(driverClassName, url, username, password);
		return dbConfig;
	}

	public static DBConfig loadClemapDBConfig(Properties prop) {
		String driverClassName = prop.getProperty("clemap_db.driver-class-name");//clemap_db.url
		String url = prop.getProperty("clemap_db.url");
		String username = prop.getProperty("clemap_db.username");
		String password = prop.getProperty("clemap_db.password");
		//DBConfig dbConfig = new DBConfig("jdbc:mariadb://localhost/energy1", "learning_agent", "sql2537");
		DBConfig dbConfig = new DBConfig(driverClassName, url, username, password);
		return dbConfig;
	}

	public static List<NodeLocation> loadDefaultNeighbours(Properties prop) {
		List<NodeLocation> defaultNeighbours = new ArrayList<>();
		if (prop.containsKey("lsa.server.neighbours")) {
			String sDefaultNeighbours = prop.getProperty("lsa.server.neighbours");
			String[] listNodeLocation = sDefaultNeighbours.split(",");
			for(String sNextNodeLocation : listNodeLocation) {
				String[] nextNodeLocation2 = sNextNodeLocation.split(":");
				if(nextNodeLocation2.length == 4) {
					String node = nextNodeLocation2[0];
					String host = nextNodeLocation2[1];
					Integer post = Integer.valueOf(nextNodeLocation2[2]);
					Integer restPort = Integer.valueOf(nextNodeLocation2[3]);
					NodeLocation nextNodeLocation = new NodeLocation(node, host, post, restPort);
					defaultNeighbours.add(nextNodeLocation);
				}
			}
		}
		return defaultNeighbours;
	}
	public static ServerConfig initServerConfig(String[] args) {
		ServerConfig serverConfig = new ServerConfig();
		System.out.println("initServerConfig : args=" + Arrays.asList(args));
		String propFileName = "application.properties";
		String environement = null;
		if(args.length>0) {
			environement = args[0];
			propFileName = "application-" + environement + ".properties";
			System.setProperty("spring.profiles.active", environement);
		}
		serverConfig.setEnvironment(environement);
		serverConfig.setPropertiesFileName(propFileName);
		Properties prop = new Properties();
		try {
			prop = loadProperties(propFileName);
		} catch (IOException e) {
			System.err.println(e);
		}
		prop = SapereUtil.addParamProperties(prop, args);
		NodeLocation nodeLocation = loadNodeLocation(prop);
		serverConfig.setNodeLocation(nodeLocation);
		// Just for debug :
		if (prop.containsKey("lsa.server.neighbours")) {
			System.out.println("initServerConfig : loadDefaultNeighbours : lsa.server.neighbours = " + prop.getProperty("lsa.server.neighbours"));
		}
		List<NodeLocation> defaultNeighbours = loadDefaultNeighbours(prop);
		serverConfig.setDefaultNeighbours(defaultNeighbours);
		DBConfig dbConfig = loadDBConfig(prop);
		serverConfig.setDbConfig(dbConfig);
		DBConfig clemapDbConfig = loadClemapDBConfig(prop);
		serverConfig.setClemapDbConfig(clemapDbConfig);
		if(prop.containsKey("init_script")) {
			String initSqlScripts = prop.getProperty("init_script");
			String[] arrayInitScripts = initSqlScripts.split(",");
			List<String> listInitScripts = Arrays.asList(arrayInitScripts);
			serverConfig.setInitSqlScripts(listInitScripts);
		}
		boolean modeAuto = false;
		if(prop.containsKey("mode_auto")) {
			String sModeAuto = prop.getProperty("mode_auto");
			modeAuto = ("1".equalsIgnoreCase(sModeAuto) || "true".equalsIgnoreCase(sModeAuto));
		}
		if(prop.containsKey("activatePredictions")) {
			String sActivatePredictions = prop.getProperty("activatePredictions");
			boolean activatePredictions = "1".equalsIgnoreCase(sActivatePredictions) || "true".equalsIgnoreCase(sActivatePredictions);
			serverConfig.setNodePredicitonSetting(new PredictionSetting(activatePredictions, null, LearningModelType.LSTM));
		}
		serverConfig.setModeAuto(modeAuto);
		if(prop.containsKey("csv_file")) {
			String csvFile = prop.getProperty("csv_file");
			serverConfig.setCsvFile(csvFile);
		}
		if(prop.containsKey("url_forcasting")) {
			String urlForcasting = prop.getProperty("url_forcasting");
			serverConfig.setUrlForcasting(urlForcasting);
		}
		if(prop.containsKey("scenario")) {
			String scenario = prop.getProperty("scenario");
			serverConfig.setScenario(scenario);
		}
		if(prop.containsKey("debug_level")) {
			String sDebugLevel = prop.getProperty("debug_level");
			int debugLevel = Integer.valueOf(sDebugLevel);
			serverConfig.setDebugLevel(debugLevel);
		}
		return serverConfig;
	}

	public static Double auxComputeAvg(List<Double> listDouble) {
		if(listDouble.size()>0) {
			Double sum = listDouble.stream().reduce(0.0, Double::sum);
			double result = sum/listDouble.size();
			return result;
		}
		return null;
	}

	public static List<String> cloneListString(List<String> iterations) {
		List<String> cloneList = new ArrayList<String>();
		if(iterations != null) {
			for(String nextIt : iterations) {
				cloneList.add(nextIt);
			}
		}
		return cloneList;
	}

	public static List<Integer> cloneListInteger(List<Integer> iterations) {
		List<Integer> cloneListIntegers = new ArrayList<Integer>();
		if(iterations != null) {
			for(Integer nextIt : iterations) {
				cloneListIntegers.add(nextIt);
			}
		}
		return cloneListIntegers;
	}

	public static List<Long> cloneListLong(List<Long> iterations) {
		List<Long> cloneListIntegers = new ArrayList<Long>();
		if(iterations != null) {
			for(Long nextIt : iterations) {
				cloneListIntegers.add(nextIt);
			}
		}
		return cloneListIntegers;
	}

	public static List<Double> cloneListDouble(List<Double> iterations) {
		List<Double> cloneListIntegers = new ArrayList<Double>();
		if(iterations != null) {
			for(Double nextIt : iterations) {
				cloneListIntegers.add(nextIt);
			}
		}
		return cloneListIntegers;
	}

	public static SortedSet<Date> cloneSetDate(SortedSet<Date> iterations) {
		SortedSet<Date> cloneSet = new TreeSet<Date>();
		if(iterations != null) {
			for(Date nextIt : iterations) {
				cloneSet.add(nextIt);
			}
		}
		return cloneSet;
	}

	public static Map<Integer, Date> cloneMapIntegerDate(Map<Integer, Date> mapIterationDates) {
		Map<Integer, Date> cloneMapIterationDates = new HashMap<Integer, Date>();
		if(mapIterationDates != null) {
			for(Integer nextIt : mapIterationDates.keySet()) {
				cloneMapIterationDates.put(nextIt, mapIterationDates.get(nextIt));
			}
		}
		return cloneMapIterationDates;
	}

	public static Map<String, Map<String, Double>> cloneMap2StringDouble(Map<String, Map<String, Double>> toCopy) {
		Map<String, Map<String, Double>> result = new HashMap<String, Map<String,Double>>();
		if(toCopy != null) {
			for(String nextIt : toCopy.keySet()) {
				Map<String, Double> itemToCopy = toCopy.get(nextIt);
				result.put(nextIt, cloneMapStringDouble(itemToCopy));
			}
		}
		return result;
	}

	public static Map<String, Double> cloneMapStringDouble(Map<String, Double> toCopy) {
		Map<String, Double> result = new HashMap<String, Double>();
		if(toCopy != null) {
			for(String nextIt : toCopy.keySet()) {
				result.put(nextIt, toCopy.get(nextIt));
			}
		}
		return result;
	}

	public static List<Integer> extractDimensions(String content, String separator) throws HandlingException {
		String[] dimensionArray =  content.split(separator);
		List<Integer> result = new ArrayList<Integer>();
		for(String nextDim : dimensionArray) {
			if(nextDim.length()>0) {
				try {
					result.add(Integer.valueOf(nextDim));
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					throw new HandlingException(e.getMessage());
				}
			}
		}
		return result;
	}

	public static Map<Integer, Date> correctMapIterationDate(
			Map<Integer, Date> localMapIterationDates
			,Map<Integer, Date> remoteMapIterationDates) {
		Map<Integer, Date> result = new HashMap<Integer, Date>() ;//cloneMapIntegerDate(remoteMapIterationDates);
		Map<Date, Integer> localInversedMap = new HashMap<Date, Integer>();
		for(Integer nextIt : localMapIterationDates.keySet()) {
			Date nextDate = localMapIterationDates.get(nextIt);
			localInversedMap.put(nextDate, nextIt);
		}
		//Collection<Date> localDates = localMapIterationDates.values();
		//Collection<Date> remoteDates = remoteMapIterationDates.values();
		// Evaluate the gap in iteration number for the same date
		int iterationGap = 0;
		for(Integer nextRemoteIt : remoteMapIterationDates.keySet()) {
			Date nextRemoteDate = remoteMapIterationDates.get(nextRemoteIt);
			if(localInversedMap.containsKey(nextRemoteDate)) {
				Integer localIt = localInversedMap.get(nextRemoteDate);
				if(nextRemoteIt.intValue()  !=  localIt.intValue()) {
					iterationGap = nextRemoteIt - localIt;
				}
			}
		}
		for(Integer nextRemoteIt : remoteMapIterationDates.keySet()) {
			Date nextRemoteDate = remoteMapIterationDates.get(nextRemoteIt);
			Integer nextCorrectedIt = nextRemoteIt - iterationGap;
			result.put(nextRemoteIt, nextRemoteDate);
		}
		return result;
	}


	public static List<String> callPythonScript(String scriptPath, Map<String, String> mapOptionParams) throws HandlingException {
		String programDir = System.getProperty("user.dir") + "\\..\\lstm\\";
		String programPath = programDir + "predict.py";
		List<String> listArguments = new ArrayList<String>();
		listArguments.add(programPath);
		ProcessBuilder pb = new ProcessBuilder("C:\\Python312\\python.exe",  scriptPath);
		for(String paramName : mapOptionParams.keySet()) {
			String paramValue = mapOptionParams.get(paramName);
			pb.command().add(paramName+ "=" + paramValue);
		}
		List<String> result;
		int exitCode;
		try {
			File userDir = new File(programDir);
			if (userDir.exists()) {
				pb = pb.directory(userDir);
				File temp = pb.directory();
				String currentWorkingDirectory = "Current working directory: " + temp.toString();
				System.out.println("currentWorkingDirectory = " + currentWorkingDirectory);
				pb.start();
			}
			Process process = pb.start();
			BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			result = new ArrayList<String>();
			String line = null;
			System.out.println("returned lines :");
			while ((line = in.readLine()) != null) {
				System.out.println(line);
				result.add(line);
			}
			exitCode = process.waitFor();
		} catch (Exception e) {
			throw new HandlingException(e.getMessage());
		}
		System.out.println("exitCode: " + exitCode);
		return result;
	}

	public static String generateDumpHistoryFilename(String directory, PredictionScope scope, String node) {
		String perimeter = node;
		if(PredictionScope.CLUSTER.equals(scope)) {
			perimeter = "cluster_" + node;
		}
		String csvFileName = directory + "dump_history_" + perimeter + ".csv";
		return csvFileName;
	}

	public static void dumpAllHistoryStates(AbstractLogger logger, String directory, PredictionScope scope, String node, String[] variables
			, List<SingleNodeStateItem> stateHistory) throws HandlingException {
		String lineSep = System.getProperty("line.separator");
		try {
			String csvFileName = generateDumpHistoryFilename(directory, scope, node);
			File csvFile = new File(csvFileName);
			String csvSeparator =  ",";
			FileWriter fileWriter = new FileWriter(csvFile);
			//write header line here
			fileWriter.write("date_time");
			fileWriter.write(csvSeparator);
			for(String variable : variables) {
				fileWriter.write(variable);
				fileWriter.write(csvSeparator);
			}
			int varIndex = 1;
			for(String variable : variables) {
				fileWriter.write("cls_" + variable);
				if (varIndex < variables.length) {
					fileWriter.write(csvSeparator);
				}
				varIndex++;
			}
			fileWriter.write(lineSep);
			for (SingleNodeStateItem stateItem : stateHistory) {
			    stateItem.dumpValuesCsv(fileWriter, csvSeparator, variables);
			    fileWriter.append(lineSep);
			}
			fileWriter.close();
		} catch (IOException e) {
			logger.error(e);
			throw new HandlingException(e.getMessage());
		}
	}

	public static String[] toStrArray(List<String> listTags) {
		// Storing it inside array of strings
		String[] array = new String[listTags.size()];

		// Converting ArrayList to Array
		for (int i = 0; i < listTags.size(); i++)
			array[i] = listTags.get(i);
		return array;
	}
}
