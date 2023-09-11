package com.sapereapi.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sapereapi.model.LaunchConfig;
import com.sapereapi.model.OperationResult;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.Device;
import com.sapereapi.model.energy.DeviceMeasure;
import com.sapereapi.model.energy.DeviceProperties;
import com.sapereapi.model.energy.NodeContent;
import com.sapereapi.model.energy.NodeTotal;
import com.sapereapi.model.energy.OptionItem;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.TimestampedValue;
import com.sapereapi.model.energy.forcasting.ForcastingResult;
import com.sapereapi.model.energy.input.AgentFilter;
import com.sapereapi.model.markov.MarkovStateHistory;
import com.sapereapi.model.markov.MarkovTimeWindow;
import com.sapereapi.model.markov.NodeTransitionMatrices;
import com.sapereapi.model.markov.TransitionMatrix;
import com.sapereapi.model.markov.TransitionMatrixKey;
import com.sapereapi.model.referential.AgentType;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;

import Jama.Matrix;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.node.NodeConfig;

public class UtilJsonParser {
	public static Pattern JAVA_DATE_PATTERN = Pattern.compile("^(?<dow>[a-zA-Z]{3}) (?<month>[a-zA-Z]{3}) (?<day>[0-9]{2}) (?<hours>[0-9]{2}):(?<minutes>[0-9]{2}):(?<sec>[0-9]{2}) (?<tz>(CET|CEST)) (?<year>[0-9]{4})$");
	public static Pattern JAVA_JSON_DATE_PATTERN1 = Pattern.compile("^(?<year>[0-9]{4})-(?<month>[0-9]{2})-(?<day>[0-9]{2}) (?<hours>[0-9]{2}):(?<minutes>[0-9]{2}):(?<sec>[0-9]{2})(?<tz1>(\\-|\\+))(?<tz2>[0-9]{4})$");
	public static Pattern JAVA_JSON_DATE_PATTERN2 = Pattern.compile("^(?<year>[0-9]{4})-(?<month>[0-9]{2})-(?<day>[0-9]{2}) (?<hours>[0-9]{2}):(?<minutes>[0-9]{2}):(?<sec>[0-9]{2})(?<tz1>(\\-|\\+))(?<tz2>[0-9]{2}):(?<tz3>[0-9]{2})$");


	final static String QUOTE = "\"";
	final static String ESCAPED_QUOTE = "\\" + QUOTE;
	final static List<String> excludeMethodList = Arrays.asList("toString", "getClass", "wait", "hashCode", "notify", "getDeclaringClass", "notifyAll", "getRowPackedCopy", "getColumnPackedCopy", "getArrayCopy");

	public static NodeConfig parseNodeConfig(JSONObject jsonNodeConfig, AbstractLogger logger) {
		NodeConfig nodeConfig = new NodeConfig();
		parseJSONObject(nodeConfig, jsonNodeConfig, logger);
		return nodeConfig;
	}

	public static TimestampedValue parseTimeStampedValue(JSONObject jsonTimestampedValue, AbstractLogger logger) {
		TimestampedValue timestampedValue = new TimestampedValue();
		parseJSONObject(timestampedValue, jsonTimestampedValue, logger);
		return timestampedValue;
	}

	public static NodeContent parseNodeContent(JSONObject jsonNodeContent, AbstractLogger logger) {
		long timeShiftMS = jsonNodeContent.getLong("timeShiftMS");
		NodeConfig nodeConfig = parseNodeConfig(jsonNodeContent.getJSONObject("nodeConfig"), logger);
		NodeContent result = new NodeContent(nodeConfig, new AgentFilter(), timeShiftMS);
		JSONObject jsonNodeTotal = jsonNodeContent.getJSONObject("total");
		try {
			NodeTotal nodeTotal = parseNodeTotal(jsonNodeTotal, logger);
			result.setTotal(nodeTotal);
		} catch (Exception e1) {
			logger.error(e1);
		}

		JSONArray json_producers = jsonNodeContent.getJSONArray("producers");
		for (int i = 0; i < json_producers.length(); i++) {
			JSONObject jsonAgentForm = json_producers.getJSONObject(i);
			try {
				AgentForm producer = parseAgentForm(jsonAgentForm, logger);
				result.addProducer(producer);
			} catch (Exception e) {
				logger.error(e);
			}
		}
		JSONArray json_consumers = jsonNodeContent.getJSONArray("consumers");
		for (int i = 0; i < json_consumers.length(); i++) {
			JSONObject jsonAgentForm = json_consumers.getJSONObject(i);
			try {
				AgentForm consumer = parseAgentForm(jsonAgentForm, logger);
				result.addConsumer(consumer);
			} catch (Exception e) {
				logger.error(e);
			}
		}
		return result;
	}

	public static Map<String, String> parseJsonMapString(JSONObject jsonobj, AbstractLogger logger) throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			String value = jsonobj.getString(key);
			map.put(key, value);
		}
		return map;
	}

	public static Map<String, Double> parseJsonMapDouble(JSONObject jsonobj, AbstractLogger logger) throws Exception {
		Map<String, Double> map = new HashMap<String, Double>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Double value = jsonobj.getDouble(key);
			map.put(key, value);
		}
		return map;
	}

	public static Map<String, Integer> parseJsonMapInteger(JSONObject jsonobj, AbstractLogger logger) throws Exception {
		Map<String, Integer> map = new HashMap<String, Integer>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Integer value = jsonobj.getInt(key);
			map.put(key, value);
		}
		return map;
	}

	public static Map<String, Long> parseJsonMapLong(JSONObject jsonobj, AbstractLogger logger) throws Exception {
		Map<String, Long> map = new HashMap<String, Long>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Long value = jsonobj.getLong(key);
			map.put(key, value);
		}
		return map;
	}

	public static Matrix parseJsonMatrix(JSONObject jsonobj, AbstractLogger logger) throws Exception {
		int nbOfColumns = SapereUtil.getIntValue(jsonobj.get("columnDimension"));
		int nbOfRows = SapereUtil.getIntValue(jsonobj.get("rowDimension"));
		Matrix matrix = new Matrix(nbOfRows,nbOfColumns);
		JSONArray jsonMatrix = jsonobj.getJSONArray("array");
		for (int rowIndex = 0; rowIndex < jsonMatrix.length(); rowIndex++) {
			JSONArray jsonRowArray = jsonMatrix.getJSONArray(rowIndex);
			for (int colIndex = 0; colIndex < jsonRowArray.length(); colIndex++) {
				double cellValue = SapereUtil.getDoubleValue(jsonRowArray.get(colIndex));
				matrix.set(rowIndex, colIndex, cellValue);
			}
		}
		return matrix;
	}

	public static Map<String, Matrix> parseJsonMapMatrix(JSONObject jsonobj, AbstractLogger logger) throws Exception {
		Map<String, Matrix> result = new HashMap<String, Matrix>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			JSONObject jsonValue = jsonobj.getJSONObject(key);
			int nbOfColumns = SapereUtil.getIntValue(jsonValue.get("columnDimension"));
			int nbOfRows = SapereUtil.getIntValue(jsonValue.get("rowDimension"));
			Matrix matrix = new Matrix(nbOfRows,nbOfColumns);
			JSONArray jsonMatrix = jsonValue.getJSONArray("array");
			for (int rowIndex = 0; rowIndex < jsonMatrix.length(); rowIndex++) {
				JSONArray jsonRowArray = jsonMatrix.getJSONArray(rowIndex);
				for (int colIndex = 0; colIndex < jsonRowArray.length(); colIndex++) {
					double cellValue = SapereUtil.getDoubleValue(jsonRowArray.get(colIndex));
					matrix.set(rowIndex, colIndex, cellValue);
				}
				result.put(key, matrix);
			}
		}
		return result;
	}

	public static Map<String, Double> parseJsonMap2(JSONObject jsonobj, AbstractLogger logger) throws Exception {
		Map<String, Double> map = new HashMap<String, Double>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Double value = jsonobj.getDouble(key);
			map.put(key, value);
		}
		return map;
	}

	public static Map<String, PowerSlot> parseJsonMapPowerSlot(JSONObject jsonobj, AbstractLogger logger) throws Exception {
		Map<String, PowerSlot> map = new HashMap<String, PowerSlot>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			PowerSlot powerSlot = new PowerSlot();
			// parse the power slot
			parseJSONObject(powerSlot, jsonobj, logger);
			map.put(key, powerSlot);
		}
		return map;
	}

	public static DeviceMeasure parseDeviceMeasure(JSONObject jsonDeviceMeasure, AbstractLogger logger) throws Exception {
		DeviceMeasure mesasure = new DeviceMeasure();
		parseJSONObject(mesasure, jsonDeviceMeasure, logger);
		mesasure.setMap_power_p(parseJsonMapDouble(jsonDeviceMeasure.getJSONObject("map_power_p"), logger));
		mesasure.setMap_power_q(parseJsonMapDouble(jsonDeviceMeasure.getJSONObject("map_power_q"), logger));
		mesasure.setMap_power_s(parseJsonMapDouble(jsonDeviceMeasure.getJSONObject("map_power_s"), logger));
		return mesasure;
	}

	public static MarkovStateHistory parseMarkovStateHistory(JSONObject jsonDevice, AbstractLogger logger) throws Exception {
		MarkovStateHistory markovStateHistory = new MarkovStateHistory();
		parseJSONObject(markovStateHistory, jsonDevice, logger);
		return markovStateHistory;
	}

	public static Device parseDevice(JSONObject jsonDevice, AbstractLogger logger) throws Exception {
		Device device = new Device();
		parseJSONObject(device, jsonDevice, logger);
		DeviceProperties deviceProperties = parseDeviceProperties(jsonDevice.getJSONObject("properties"), logger);
		device.setProperties(deviceProperties);
		return device;
	}

	public static DeviceProperties parseDeviceProperties(JSONObject jsonDeviceProperties, AbstractLogger logger) throws Exception {
		DeviceProperties deviceProperties = new DeviceProperties();
		parseJSONObject(deviceProperties, jsonDeviceProperties, logger);
		return deviceProperties;
	}

	public static MarkovTimeWindow parseMarkovTimeWindow(JSONObject jsonTimeWindow, AbstractLogger logger) throws Exception {
		MarkovTimeWindow timeWinow = new MarkovTimeWindow();
		parseJSONObject(timeWinow, jsonTimeWindow, logger);
		JSONArray jsonDaysOfWeek = jsonTimeWindow.getJSONArray("daysOfWeek");
		Set<Integer> daysOfWeek = new HashSet<Integer>();
		for (int index = 0; index < jsonDaysOfWeek.length(); index++) {
			int nextDay = SapereUtil.getIntValue(jsonDaysOfWeek.get(index));
			daysOfWeek.add(nextDay);
		}
		timeWinow.setDaysOfWeek(daysOfWeek);
		return timeWinow;
	}

	public static TransitionMatrixKey parseTransitionMatrixKey(JSONObject jsonTrMatrixKey, AbstractLogger logger) throws Exception {
		TransitionMatrixKey result = new TransitionMatrixKey();
		parseJSONObject(result, jsonTrMatrixKey, logger);
		result.setTimeWindow(parseMarkovTimeWindow(jsonTrMatrixKey.getJSONObject("timeWindow"), logger));
		return result;
	}

	public static TransitionMatrix parseTransitionMatrix(JSONObject jsonTrMatrix, AbstractLogger logger) throws Exception {
		TransitionMatrix result = new TransitionMatrix();
		parseJSONObject(result, jsonTrMatrix, logger);
		try {
			result.setKey(parseTransitionMatrixKey(jsonTrMatrix.getJSONObject("key"), logger));
			result.setIterObsMatrix(parseJsonMatrix(jsonTrMatrix.getJSONObject("iterObsMatrix"), logger));
			result.setAllObsMatrix(parseJsonMatrix(jsonTrMatrix.getJSONObject("allObsMatrix"), logger));
			result.setNormalizedMatrix1(parseJsonMatrix(jsonTrMatrix.getJSONObject("normalizedMatrix1"), logger));
			result.setAllCorrectionsMatrix(parseJsonMatrix(jsonTrMatrix.getJSONObject("allCorrectionsMatrix"), logger));
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static NodeTransitionMatrices parseNodeTransitionMatrices(JSONObject jsonNodeTM, AbstractLogger logger) throws Exception {
		NodeTransitionMatrices result = new NodeTransitionMatrices();
		parseJSONObject(result, jsonNodeTM, logger);
		try {
			JSONArray jsonVariables = jsonNodeTM.getJSONArray("variables");
			List<String> listVariables = new ArrayList<>();
			for (int i = 0; i < jsonVariables.length(); i++) {
				listVariables.add("" + jsonVariables.get(i) );
			}
			String[] resultArray = new String[listVariables.size()];
			resultArray = listVariables.toArray(resultArray);
			result.setVariables(resultArray);
			JSONObject jsonMatrices = jsonNodeTM.getJSONObject("mapMatrices");
			Map<String, TransitionMatrix> mapMatrices = new HashMap<String, TransitionMatrix>();
			Iterator<String> keys = jsonMatrices.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				JSONObject jsonValue = jsonMatrices.getJSONObject(key);
				TransitionMatrix trMatrice = parseTransitionMatrix(jsonValue, logger);
				mapMatrices.put(key, trMatrice);
			}
			result.setMapMatrices(mapMatrices);
			result.setTimeWindow(parseMarkovTimeWindow(jsonNodeTM.getJSONObject("timeWindow"), logger));
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Exception e) {
			logger.error(e);
		}
		return result;
	}

	public static Object parseJSONObject2(Class<?> objectClass, JSONObject jsonObject, AbstractLogger logger)  {
		try {
			Constructor<?> constructor = objectClass.getConstructor();
			Object targetObject = constructor.newInstance();
			parseJSONObject(targetObject, jsonObject, logger);
			return targetObject;
		} catch (Throwable e) {
			logger.error(e);
		}
		return null;
	}

	public static Object parseJSONObject(Object targetObject, JSONObject jsonObject, AbstractLogger logger)  {
		Class<?> targetObjectClass = targetObject.getClass();
		for(Method method : targetObjectClass.getMethods())  {
			if(1==method.getParameterCount() && method.getName().startsWith("set")) {
				Parameter param = method.getParameters()[0];
				String fieldName =  SapereUtil.firstCharToLower(method.getName().substring(3));
				if(jsonObject.has(fieldName) && !jsonObject.isNull(fieldName))  {
					Class<?> paramType = param.getType();
					Object valueToSet = null;
					try {
						if(paramType.equals(String.class)) {
							valueToSet = jsonObject.getString(fieldName);
						} else if(paramType.equals(Double.class) || paramType.equals(double.class)) {
							valueToSet = jsonObject.getDouble(fieldName);
						} else if(paramType.equals(Float.class) || paramType.equals(float.class)) {
							Double dbValue = jsonObject.getDouble(fieldName);
							if(dbValue !=null) {
								valueToSet = dbValue.floatValue();
							}
						} else if(paramType.equals(Integer.class) || paramType.equals(int.class)) {
							valueToSet = (int) jsonObject.getInt(fieldName);
						} else if(paramType.equals(Long.class) || paramType.equals(long.class)) {
							valueToSet = (long) jsonObject.getLong(fieldName);
						} else if(paramType.equals(Boolean.class) || paramType.equals(boolean.class))  {
							valueToSet = (boolean) jsonObject.getBoolean(fieldName);
						}else if(paramType.equals(Date.class))  {
							valueToSet = parseJsonDate(jsonObject.getString(fieldName));
						//} else if(paramType.equals(Map.class)) {
						} else if(paramType.equals(EnvironmentalImpact.class)) {
							String svalue = jsonObject.getString(fieldName);
							valueToSet = EnvironmentalImpact.getByName(svalue);
						} else if(paramType.equals(DeviceCategory.class)) {
							String svalue = jsonObject.getString(fieldName);
							valueToSet = DeviceCategory.getByName(svalue);
						} else if(paramType.equals(AgentType.class)) {
							String svalue = jsonObject.getString(fieldName);
							valueToSet = AgentType.getByName(svalue);
						}
					} catch (Throwable e) {
						logger.error(e);
					}
					if(valueToSet!=null)  {
						try {
							method.invoke(targetObject, valueToSet);
						} catch (Throwable e) {
							logger.error(e);
						}

					}
				}
			}
		}
		return targetObject;
	}

	public static OperationResult parseOperationResult(JSONObject jsonAgent, AbstractLogger logger) throws Exception {
		OperationResult operationResult = new OperationResult();
		parseJSONObject(operationResult, jsonAgent, logger);
		return operationResult;
	}

	public static OptionItem parseOptionItem(JSONObject jsonOptionItem, AbstractLogger logger) throws Exception {
		OptionItem optionItem = new OptionItem();
		parseJSONObject(optionItem, jsonOptionItem, logger);
		return optionItem;
	}

	public static AgentForm parseAgentForm(JSONObject jsonAgent, AbstractLogger logger) throws Exception {
		AgentForm agentForm = new AgentForm();
		parseJSONObject(agentForm, jsonAgent, logger);
		Map<String, Double> offersRepartition2 = parseJsonMapDouble(jsonAgent.getJSONObject("offersRepartition"), logger);
		agentForm.setOffersRepartition(offersRepartition2);
		Map<String, PowerSlot> contractsRepartition2 = parseJsonMapPowerSlot(jsonAgent.getJSONObject("ongoingContractsRepartition"), logger);
		agentForm.setOngoingContractsRepartition(contractsRepartition2);
		OptionItem deviceCategory = parseOptionItem(jsonAgent.getJSONObject("deviceCategory"), logger);
		agentForm.setDeviceCategory(deviceCategory);
		NodeConfig nodeConfig = parseNodeConfig(jsonAgent.getJSONObject("location"), logger);
		agentForm.setLocation(nodeConfig);
		return agentForm;
	}


	public static ForcastingResult parseForcastingResult(JSONObject jsonForcastingResult, AbstractLogger logger) throws Exception {
		ForcastingResult result = new ForcastingResult();
		JSONArray json_values = jsonForcastingResult.getJSONArray("values");
		List<Double> values = new ArrayList<>();
		for(int idx=0; idx < json_values.length(); idx++) {
			Object value = json_values.get(idx);
			values.add(SapereUtil.getDoubleValue(value));
		}
		result.setValues(values);
		return result;		
	}

	private static NodeTotal parseNodeTotal(JSONObject jsonNodeTotal, AbstractLogger logger) throws Exception {
		NodeTotal nodeTotal = new NodeTotal();
		parseJSONObject(nodeTotal, jsonNodeTotal, logger);
		return nodeTotal;
	}


	public static Date parseJsonDate(String jsonDate) throws Exception {
		if(jsonDate.contains("T")) {
			String shortDate = jsonDate.substring(0, 19);
			UtilDates.format_json_datetime_prev.setTimeZone(TimeZone.getTimeZone("Europe/Zurich"));
			Date date1 = UtilDates.format_json_datetime_prev.parse(shortDate);
			return date1;
		} else {
			if(jsonDate.length() == 25) {
				Matcher matcher = UtilJsonParser.JAVA_JSON_DATE_PATTERN2.matcher(jsonDate);
				if(matcher.matches()) {
					System.out.println("date with format " + UtilJsonParser.JAVA_JSON_DATE_PATTERN2);
					String day = matcher.group("year") + "-" + matcher.group("month") + "-" + matcher.group("day");
					String time = matcher.group("hours") + ":" + matcher.group("minutes") + ":" + matcher.group("sec");
					String timeZone =   matcher.group("tz1") + matcher.group("tz2") + matcher.group("tz3");
					jsonDate = day + " " + time + timeZone;
					//System.out.println("tz = " + jsonDate);
				}
			}
			Date date2 = UtilDates.format_json_datetime.parse(jsonDate);
			return date2;
		}
	}

	public static LaunchConfig parseLaunchConfig(JSONObject jsonLaunchConcig, AbstractLogger logger) throws Exception {
		LaunchConfig  launchConfig = new LaunchConfig();
		Map<String,String> mapLocationByNode = parseJsonMapString(jsonLaunchConcig.getJSONObject("mapLocationByNode"), logger);
		launchConfig.setMapLocationByNode(mapLocationByNode);
		JSONObject jsonMapNodes = jsonLaunchConcig.getJSONObject("mapNodes");
		Map<String, NodeConfig> mapNodeConfig = new HashMap<String, NodeConfig>();
		Iterator<String> keys = jsonMapNodes.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			JSONObject jsonNodeConfig = jsonMapNodes.getJSONObject(key);
			NodeConfig nodeConfig = new NodeConfig();
			parseJSONObject(nodeConfig, jsonNodeConfig, logger);
			mapNodeConfig.put(key, nodeConfig);
		}
		launchConfig.setMapNodes(mapNodeConfig);
		return launchConfig;
	}

	/**
	 *
	 * @param correctJsonDates
	 * @param logger
	 * @return first element : boolean : indicate if a correction is done . 2 element : changed element
	 */
	public static Object[] correctJsonDates(Object oToCheck, AbstractLogger logger) {
		Object[] result = new Object[2];
		// result by default : no change, same value
		result[0] = false;
		result[1] = oToCheck;
		if(oToCheck==null) {
			return result;
		} else if(oToCheck instanceof String) {
			String svalue = (String) oToCheck;
			if(svalue.contains(" CET ") || svalue.contains(" CEST ") ) {
				//svalue = "Sat May 06 13:23:23 CEST 2023"; // (?<key>[0-9a-zA-Z_\s.]+)
				Matcher testPattern = JAVA_DATE_PATTERN.matcher(svalue);
				if(testPattern.matches()) {
					String smonth = testPattern.group("month");
					String month = UtilDates.mapMonth.get(smonth);
					String day = testPattern.group("day");
					String hours = testPattern.group("hours");
					String minutes = testPattern.group("minutes");
					String seconds = testPattern.group("sec");
					String sYear = testPattern.group("year");
					//String timezone = testPattern.group("tz");
					String jsonDate = sYear+"-" + month + "-" + day + "T" + hours + ":" +  minutes + ":" + seconds;
					result[0] = true;
					result[1] = jsonDate;
				}
			}
			return result;
		} else if (oToCheck instanceof JSONObject) {
			try {
				JSONObject jsonObj = (JSONObject)  oToCheck;
				if(jsonObj.names() == null) {
					return result;
				}
				Map<String,Object> toReplace = new HashMap<>();
				for(Object key : jsonObj.names()) {
					String skey = key.toString();
					Object value1 = jsonObj.get(skey);
					Object[] resultItem = correctJsonDates(value1, logger);
					Boolean isItemChanged = (Boolean) resultItem[0];
					if(isItemChanged) {
						Object valueItem = resultItem[1];
						//logger.info("value1 = " + value1 + ", value2 = " + valueItem);
						toReplace.put(skey, valueItem);
					}
				}
				for(String key : toReplace.keySet()) {
					jsonObj.remove(key);
					Object replace = toReplace.get(key);
					jsonObj.putOnce(key, replace);
				}
				result[0] = (toReplace.size() > 0); // Change if there is a least one replacement
				result[1] = jsonObj; // updated json object
			} catch (Throwable e) {
				logger.error(e);
			}
			return result;
		} else if(oToCheck instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray) oToCheck;
			JSONArray jsonArray2 = new JSONArray();
			//boolean bToReplace = false;
			boolean isChanged = false;
			for (int i = 0; i < jsonArray.length(); i++) {
				Object obj = jsonArray.get(i)					;
				Object[] resultItem = correctJsonDates(obj, logger);
				Boolean isItemChanged = (Boolean) resultItem[0];
				Object valueItem =  resultItem[1];
				isChanged = isChanged || isItemChanged;
				jsonArray2.put(valueItem);
			}
			result[0] = isChanged;
			if(isChanged) {
				result[1] = jsonArray2;
			}
			return result;
		} else {
			return result;
		}
	}

	public static String aux_formatStr(Object object) {
		String sObject = ""+ object;
		if(sObject.contains(QUOTE)) {
			sObject = sObject.replace(QUOTE, ESCAPED_QUOTE);
		}
		String result = QUOTE + sObject + QUOTE;
		// Remove cariage returns
		if(result.contains("\r\n")) {
			result = result.replace("\r\n", "");
		}
		result.replace("0.0,", "0,");
		return result;
	}



	public static StringBuffer toJsonStr(Object object, AbstractLogger logger, int depth) {
		if(depth>100) {
			throw new RuntimeException("toJsonStr Infinite loop ...." + object);
		}
		StringBuffer result = new StringBuffer();
		if(object==null) {
			result.append("null");
			return result;
		} else if(object instanceof String) {
			result.append(aux_formatStr(object));
			return result;
		} else if(object instanceof Date) {
			Date aDate = (Date) object;
			result.append( "\"").append(UtilDates.format_json_datetime.format(aDate)).append("\"");
			return result;
		} else if(object.getClass().isPrimitive()
				|| object instanceof Boolean || object instanceof Character || object instanceof Byte
				|| object instanceof Short || object instanceof Integer || object instanceof Long || object instanceof Float || object instanceof Double || object instanceof Void) {
			result.append(""+object);
			return result;
		} else if(object instanceof Enum<?>) {
			result.append( "\"").append(object).append("\"");
			return result;
		} else if(object instanceof Object[]) {
			Object[] array = (Object[]) object;
			result.append("[");
			String sep = "";
			for(Object next : array) {
				result.append(sep).append(toJsonStr(next, logger, depth+1));
				sep = ",";
			}
			result.append("]");
			return result;
		} else if(object instanceof double[]) {
			double[] array = (double[]) object;
			result.append("[");
			String sep = "";
			for(double next : array) {
				String snext = ""+next;
				result.append(sep).append(snext);
				sep = ",";
			}
			result.append("]");
			return result;
		} else {
			if (object instanceof Collection<?>) {
				Collection collection = (Collection) object;
				Iterator<Object> iterator = collection.iterator();
				result.append("[");
				String sep = "";
				while(iterator.hasNext()) {
					Object next = iterator.next();
					result.append(sep).append(toJsonStr(next, logger, depth+1));
					sep = ",";
				}
				result.append("]");
				return result;
			} else if (object instanceof Map<?,?>) {
				Map map = (Map) object;
				result.append("{");
				String sep = "";
				for(Object key : map.keySet()) {
					Object value = map.get(key);
					result.append(sep)
							.append(aux_formatStr(key))
							.append(":")
							.append(toJsonStr(value, logger, depth+1))
							;
					sep = ",";
				}
				result.append("}");
				return result;
			} else {
				//logger.info("toJsonStr" + object.getClass());
				Map<String, Object> objectContent = new HashMap<>();
				boolean isEnum = object instanceof Enum<?>;
				List<String> enumFields = new ArrayList<>();
				if(isEnum) {
					for (Field field : object.getClass().getDeclaredFields()) {
						enumFields.add(field.getName());
					}
				}
				for(Method method : object.getClass().getMethods())  {
					if(method.getParameterCount()==0) {
						String methodName = method.getName();
						if(excludeMethodList.contains(methodName)) {
							// do nothing
						} else {
							//logger.info("method name : " + methodClass.getName() + "." + methodName + " " + depth);
							String fieldName = null;
							if(method.getName().startsWith("get")) {
								fieldName = SapereUtil.firstCharToLower(method.getName().substring(3));
							} else if(method.getName().startsWith("is")) {
								//System.out.print("For debug : is method");
								fieldName = SapereUtil.firstCharToLower(method.getName().substring(2));
							}
							if(fieldName!=null) {
								try {
									Object value = method.invoke(object);
									if(value!=null) {
										objectContent.put(fieldName, value);
									}
								} catch (Throwable e) {
									logger.error(e);
								}
							}
						}
					}
				}
				return toJsonStr(objectContent, logger, depth+1);
			}
		}
	}
}
