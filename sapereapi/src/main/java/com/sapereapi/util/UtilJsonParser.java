package com.sapereapi.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.text.ParseException;
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

import com.sapereapi.lightserver.DisableJson;
import com.sapereapi.model.EnergyStorageSetting;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.LaunchConfig;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.OperationResult;
import com.sapereapi.model.OptionItem;
import com.sapereapi.model.PredictionSetting;
import com.sapereapi.model.TimestampedValue;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.Device;
import com.sapereapi.model.energy.DeviceMeasure;
import com.sapereapi.model.energy.DeviceProperties;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.energy.forcasting.ForcastingResult1;
import com.sapereapi.model.energy.forcasting.ForcastingResult2;
import com.sapereapi.model.energy.input.AgentFilter;
import com.sapereapi.model.energy.node.NodeContent;
import com.sapereapi.model.energy.node.NodeTotal;
import com.sapereapi.model.learning.FeaturesKey;
import com.sapereapi.model.learning.TimeWindow;
import com.sapereapi.model.learning.VariableFeaturesKey;
import com.sapereapi.model.learning.VariableStateHistory;
import com.sapereapi.model.learning.lstm.LSTMModelInfo;
import com.sapereapi.model.learning.lstm.LSTMPredictionResult;
import com.sapereapi.model.learning.lstm.LayerDefinition;
import com.sapereapi.model.learning.lstm.ParamType;
import com.sapereapi.model.learning.markov.TransitionMatrix;
import com.sapereapi.model.learning.prediction.LearningAggregationOperator;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.util.matrix.DoubleMatrix;
import com.sapereapi.util.matrix.IterationMatrix;
import com.sapereapi.util.matrix.IterationObsNb;

import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.node.NodeLocation;

public class UtilJsonParser {
	public static Pattern JAVA_DATE_PATTERN = Pattern.compile("^(?<dow>[a-zA-Z]{3}) (?<month>[a-zA-Z]{3}) (?<day>[0-9]{2}) (?<hours>[0-9]{2}):(?<minutes>[0-9]{2}):(?<sec>[0-9]{2}) (?<tz>(CET|CEST)) (?<year>[0-9]{4})$");
	public static Pattern JAVA_JSON_DATE_PATTERN1 = Pattern.compile("^(?<year>[0-9]{4})-(?<month>[0-9]{2})-(?<day>[0-9]{2}) (?<hours>[0-9]{2}):(?<minutes>[0-9]{2}):(?<sec>[0-9]{2})(?<tz1>(\\-|\\+))(?<tz2>[0-9]{4})$");
	public static Pattern JAVA_JSON_DATE_PATTERN2 = Pattern.compile("^(?<year>[0-9]{4})-(?<month>[0-9]{2})-(?<day>[0-9]{2}) (?<hours>[0-9]{2}):(?<minutes>[0-9]{2}):(?<sec>[0-9]{2})(?<tz1>(\\-|\\+))(?<tz2>[0-9]{2}):(?<tz3>[0-9]{2})$");


	final static String QUOTE = "\"";
	final static String ESCAPED_QUOTE = "\\" + QUOTE;
	final static List<String> excludeMethodList = Arrays.asList("toString", "getClass", "wait", "hashCode", "notify", "getDeclaringClass", "notifyAll", "getRowPackedCopy", "getColumnPackedCopy", "getArrayCopy");

	public static NodeLocation parseNodeLocationg(JSONObject jsonNodeLocation, AbstractLogger logger) {
		NodeLocation nodeLocation = new NodeLocation();
		parseJSONObject(nodeLocation, jsonNodeLocation, logger);
		return nodeLocation;
	}

	public static PredictionSetting parsePredictionSetting(JSONObject jsonPredSettings, AbstractLogger logger) {
		PredictionSetting predictionSetting = new PredictionSetting();
		parseJSONObject(predictionSetting, jsonPredSettings, logger);
		if(jsonPredSettings.has("aggregator")) {
			LearningAggregationOperator aggregator = parseLearningAggregationOperator(jsonPredSettings.getJSONObject("aggregator"), logger);
			predictionSetting.setAggregator(aggregator);
		}
		return predictionSetting;
	}

	public static EnergyStorageSetting parseEnergyStorageSetting(JSONObject jsonPredSettings, AbstractLogger logger) {
		EnergyStorageSetting predictionSetting = new EnergyStorageSetting();
		parseJSONObject(predictionSetting, jsonPredSettings, logger);
		return predictionSetting;
	}

	public static LearningAggregationOperator parseLearningAggregationOperator(JSONObject jsonNodeAggregOp, AbstractLogger logger) {
		LearningAggregationOperator aggregationOperator = new LearningAggregationOperator();
		parseJSONObject(aggregationOperator, jsonNodeAggregOp, logger);
		return aggregationOperator;
	}

	public static TimestampedValue parseTimeStampedValue(JSONObject jsonTimestampedValue, AbstractLogger logger) {
		TimestampedValue timestampedValue = new TimestampedValue();
		parseJSONObject(timestampedValue, jsonTimestampedValue, logger);
		return timestampedValue;
	}

	public static NodeContent parseNodeContent(JSONObject jsonNodeContent, AbstractLogger logger) {
		long timeShiftMS = jsonNodeContent.getLong("timeShiftMS");
		try {
			NodeContext nodeContext = parseNodeContext(jsonNodeContent.getJSONObject("nodeContext"), logger);
			NodeContent result = new NodeContent(nodeContext, new AgentFilter(), timeShiftMS);
			JSONObject jsonNodeTotal = jsonNodeContent.getJSONObject("total");
			NodeTotal nodeTotal = parseNodeTotal(jsonNodeTotal, logger);
			result.setTotal(nodeTotal);
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
		} catch (Exception e) {
			logger.error(e);
		}
		return null;
	}

	public static NodeContext parseNodeContext(JSONObject jsonNodeContext, AbstractLogger logger) throws HandlingException {
		NodeContext result = new NodeContext();
		parseJSONObject(result, jsonNodeContext, logger);
		JSONObject json_mapDatetimeShifts = jsonNodeContext.getJSONObject("datetimeShifts");
		Map<Integer, Integer> datetimeShifts = new HashMap<Integer, Integer>();
		Iterator<String> keys = json_mapDatetimeShifts.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Integer keyInt = Integer.valueOf(key);
			Integer intValue = json_mapDatetimeShifts.getInt(key);
			if(intValue != null) {
				datetimeShifts.put(keyInt, intValue);
				//TransitionMatrixKey matrixKey = parseTransitionMatrixKey(json_matrixKey, logger);
			}
		}
		result.setDatetimeShifts(datetimeShifts);
		JSONObject jsonNodeLocation = jsonNodeContext.getJSONObject("nodeLocation");
		result.setNodeLocation(parseNodeLocationg(jsonNodeLocation, logger));
		JSONArray jsonNeighbourConfigs = jsonNodeContext.getJSONArray("neighbourNodeLocations");
		List<NodeLocation> listNeighbourConfigs = new ArrayList<NodeLocation>();
		for (int i = 0; i < jsonNeighbourConfigs.length(); i++) {
			JSONObject jsonNextNodeLocation = jsonNeighbourConfigs.getJSONObject(i);
			try {
				NodeLocation nextNodeLocation = parseNodeLocationg(jsonNextNodeLocation, logger);
				listNeighbourConfigs.add(nextNodeLocation);
			} catch (Exception e) {
				logger.error(e);
			}
		}
		result.setNeighbourNodeLocations(listNeighbourConfigs);
		return result;
	}


	public static PredictionContext parsePredictionContext(JSONObject jsonPredictionContext, AbstractLogger logger) throws HandlingException {
		PredictionContext result = new PredictionContext();
		parseJSONObject(result, jsonPredictionContext, logger);
		JSONObject jsonNodeContext = jsonPredictionContext.getJSONObject("nodeContext");
		result.setNodeContext(parseNodeContext(jsonNodeContext, logger));
		JSONArray json_allTimeWindows = jsonPredictionContext.getJSONArray("allTimeWindows");
		List<TimeWindow> listTimeWindows = new ArrayList<TimeWindow>();
		for (int i = 0; i < json_allTimeWindows.length(); i++) {
			JSONObject jsonTimeWindows = json_allTimeWindows.getJSONObject(i);
			try {
				TimeWindow timeWindow = parseTimeWindow(jsonTimeWindows, logger);
				listTimeWindows.add(timeWindow);
			} catch (Exception e) {
				logger.error(e);
			}
		}
		result.setAllTimeWindows(listTimeWindows);
		//Map<Integer, Map<String, TransitionMatrixKey>> mapTransitionMatrixKeys = new HashMap<Integer, Map<String,TransitionMatrixKey>>();
		JSONObject json_mapTransitionMatrixKeys = jsonPredictionContext.getJSONObject("mapTransitionMatrixKeys");
		Iterator<String> keys = json_mapTransitionMatrixKeys.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			JSONObject json_map2 = json_mapTransitionMatrixKeys.getJSONObject(key);
			Iterator<String> keys2 = json_map2.keys();
			while (keys2.hasNext()) {
				JSONObject json_matrixKey = json_map2.getJSONObject(key);
				VariableFeaturesKey matrixKey = parseTransitionMatrixKey(json_matrixKey, logger);
			}
		}
		return result;
	}

	public static Map<String, String> parseJsonMapString(JSONObject jsonobj, AbstractLogger logger) throws HandlingException {
		Map<String, String> map = new HashMap<String, String>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			String value = jsonobj.getString(key);
			map.put(key, value);
		}
		return map;
	}

	public static Map<String, Double> parseJsonMapDouble(JSONObject jsonobj, AbstractLogger logger) throws HandlingException {
		Map<String, Double> map = new HashMap<String, Double>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Double value = jsonobj.getDouble(key);
			map.put(key, value);
		}
		return map;
	}

	public static Map<String, Integer> parseJsonMapInteger(JSONObject jsonobj, AbstractLogger logger) throws HandlingException {
		Map<String, Integer> map = new HashMap<String, Integer>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Integer value = jsonobj.getInt(key);
			map.put(key, value);
		}
		return map;
	}

	public static Map<String, Long> parseJsonMapLong(JSONObject jsonobj, AbstractLogger logger) throws HandlingException {
		Map<String, Long> map = new HashMap<String, Long>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Long value = jsonobj.getLong(key);
			map.put(key, value);
		}
		return map;
	}

	public static Map<String, Boolean> parseJsonMapBoolean(JSONObject jsonobj, AbstractLogger logger) throws HandlingException {
		Map<String, Boolean> map = new HashMap<String, Boolean>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Boolean value = jsonobj.getBoolean(key);
			map.put(key, value);
		}
		return map;
	}

	public static IterationObsNb parseIterationObsNb(JSONObject jsonobj, AbstractLogger logger) throws HandlingException {
		IterationObsNb result = new IterationObsNb();
		parseJSONObject(result, jsonobj, logger);
		//Map<Integer, Double> map = new HashMap<Integer, Double>();
		JSONObject jsonValues = jsonobj.getJSONObject("values");
		Iterator<String> keys = jsonValues.keys();
		while (keys.hasNext()) {
			Object key = keys.next();
			Double value = jsonValues.getDouble(""+key);
			//map.put(Integer.valueOf(key), value);
			result.setValue(Integer.valueOf(""+key), value);
		}
		return result;
	}

	public static IterationMatrix parseJsonIterationMatrix(JSONObject jsonobj, AbstractLogger logger) throws HandlingException {
		int nbOfColumns = SapereUtil.getIntValue(jsonobj.get("columnDimension"));
		int nbOfRows = SapereUtil.getIntValue(jsonobj.get("rowDimension"));
		IterationMatrix matrix = new IterationMatrix(nbOfRows,nbOfColumns);
		JSONArray jsonMatrix = jsonobj.getJSONArray("array");
		for (int rowIndex = 0; rowIndex < jsonMatrix.length(); rowIndex++) {
			JSONArray jsonRowArray = jsonMatrix.getJSONArray(rowIndex);
			for (int colIndex = 0; colIndex < jsonRowArray.length(); colIndex++) {
				JSONObject testObj = jsonRowArray.getJSONObject(colIndex);
				IterationObsNb iterationObsNb = parseIterationObsNb(testObj, logger);
				//double cellValue = SapereUtil.getDoubleValue(jsonRowArray.get(colIndex));
				matrix.set(rowIndex, colIndex, iterationObsNb);
			}
		}
		return matrix;
	}

	public static DoubleMatrix parseJsonMatrix(JSONObject jsonobj, AbstractLogger logger) throws HandlingException {
		int nbOfColumns = SapereUtil.getIntValue(jsonobj.get("columnDimension"));
		int nbOfRows = SapereUtil.getIntValue(jsonobj.get("rowDimension"));
		DoubleMatrix matrix = new DoubleMatrix(nbOfRows, nbOfColumns);
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

	public static DoubleMatrix parseJsonMatrix2(JSONArray jsonMatrix, AbstractLogger logger) throws HandlingException {
		// int nbOfColumns = SapereUtil.getIntValue(jsonobj.get("columnDimension"));
		// int nbOfRows = SapereUtil.getIntValue(jsonobj.get("rowDimension"));
		DoubleMatrix matrix = null;
		int nbOfRows = jsonMatrix.length();
		// DoubleMatrix matrix = new DoubleMatrix(nbOfRows,nbOfColumns);
		// JSONArray jsonMatrix = jsonobj.getJSONArray("array");
		for (int rowIndex = 0; rowIndex < jsonMatrix.length(); rowIndex++) {
			Object nextRow = jsonMatrix.get(rowIndex);
			if (nextRow instanceof JSONArray) {
				JSONArray jsonRowArray = jsonMatrix.getJSONArray(rowIndex);
				int nbOfColumns = jsonRowArray.length();
				if (matrix == null) {
					matrix = new DoubleMatrix(nbOfRows, nbOfColumns);
				}
				for (int colIndex = 0; colIndex < jsonRowArray.length(); colIndex++) {
					double cellValue = SapereUtil.getDoubleValue(jsonRowArray.get(colIndex));
					matrix.set(rowIndex, colIndex, cellValue);
				}
			} else if (nextRow instanceof Double || nextRow instanceof BigDecimal || nextRow instanceof Float) {
				Double nextValue = SapereUtil.getDoubleValue(nextRow);
				if (matrix == null) {
					matrix = new DoubleMatrix(nbOfRows, 1);
				}
				matrix.set(rowIndex, 0, nextValue);
			} else {
				logger.warning("nextRow = " + nextRow);
			}
		}
		return matrix;
	}

	public static Map<String, DoubleMatrix> parseJsonMapMatrix(JSONObject jsonobj, AbstractLogger logger) throws HandlingException {
		Map<String, DoubleMatrix> result = new HashMap<String, DoubleMatrix>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			JSONObject jsonValue = jsonobj.getJSONObject(key);
			int nbOfColumns = SapereUtil.getIntValue(jsonValue.get("columnDimension"));
			int nbOfRows = SapereUtil.getIntValue(jsonValue.get("rowDimension"));
			DoubleMatrix matrix = new DoubleMatrix(nbOfRows,nbOfColumns);
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

	public static Map<String, Double> parseJsonMap2(JSONObject jsonobj, AbstractLogger logger) throws HandlingException {
		Map<String, Double> map = new HashMap<String, Double>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Double value = jsonobj.getDouble(key);
			map.put(key, value);
		}
		return map;
	}

	public static Map<Integer, Double> parseJsonMapIntDouble(JSONObject jsonobj, AbstractLogger logger) throws HandlingException {
		Map<Integer, Double> map = new HashMap<Integer, Double>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Double value = jsonobj.getDouble(key);
			map.put(Integer.valueOf(key), value);
		}
		return map;
	}

	public static Map<String, PowerSlot> parseJsonMapPowerSlot(JSONObject jsonobj, AbstractLogger logger) throws HandlingException {
		Map<String, PowerSlot> map = new HashMap<String, PowerSlot>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			PowerSlot powerSlot = new PowerSlot();
			JSONObject jsonValue = jsonobj.getJSONObject(key);
			// parse the power slot
			parseJSONObject(powerSlot, jsonValue, logger);
			map.put(key, powerSlot);
		}
		return map;
	}

	public static DeviceMeasure parseDeviceMeasure(JSONObject jsonDeviceMeasure, AbstractLogger logger) throws HandlingException {
		DeviceMeasure mesasure = new DeviceMeasure();
		parseJSONObject(mesasure, jsonDeviceMeasure, logger);
		mesasure.setMap_power_p(parseJsonMapDouble(jsonDeviceMeasure.getJSONObject("map_power_p"), logger));
		mesasure.setMap_power_q(parseJsonMapDouble(jsonDeviceMeasure.getJSONObject("map_power_q"), logger));
		mesasure.setMap_power_s(parseJsonMapDouble(jsonDeviceMeasure.getJSONObject("map_power_s"), logger));
		return mesasure;
	}

	public static VariableStateHistory parseVariableStateHistory(JSONObject jsonDevice, AbstractLogger logger) throws HandlingException {
		VariableStateHistory variableStateHistory = new VariableStateHistory();
		parseJSONObject(variableStateHistory, jsonDevice, logger);
		return variableStateHistory;
	}

	public static Device parseDevice(JSONObject jsonDevice, AbstractLogger logger) throws HandlingException {
		Device device = new Device();
		parseJSONObject(device, jsonDevice, logger);
		DeviceProperties deviceProperties = parseDeviceProperties(jsonDevice.getJSONObject("properties"), logger);
		device.setProperties(deviceProperties);
		return device;
	}

	public static DeviceProperties parseDeviceProperties(JSONObject jsonDeviceProperties, AbstractLogger logger) throws HandlingException {
		DeviceProperties deviceProperties = new DeviceProperties();
		parseJSONObject(deviceProperties, jsonDeviceProperties, logger);
		return deviceProperties;
	}

	public static TimeWindow parseTimeWindow(JSONObject jsonTimeWindow, AbstractLogger logger) throws HandlingException {
		TimeWindow timeWinow = new TimeWindow();
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

	public static FeaturesKey parseFeaturesKey(JSONObject jsonFeaturesKey, AbstractLogger logger) throws HandlingException {
		FeaturesKey result = new FeaturesKey();
		parseJSONObject(result, jsonFeaturesKey, logger);
		result.setTimeWindow(parseTimeWindow(jsonFeaturesKey.getJSONObject("timeWindow"), logger));
		return result;
	}

	public static VariableFeaturesKey parseTransitionMatrixKey(JSONObject jsonTrMatrixKey, AbstractLogger logger) throws HandlingException {
		VariableFeaturesKey result = new VariableFeaturesKey();
		parseJSONObject(result, jsonTrMatrixKey, logger);
		result.setFeaturesKey(parseFeaturesKey(jsonTrMatrixKey.getJSONObject("featuresKey"), logger));
		return result;
	}

	private static Map<Integer, Date> aux_retrieveMapIterationDates(JSONObject jsonTrMatrix, AbstractLogger logger) throws HandlingException {
		JSONObject jsonMapIterationDates = jsonTrMatrix.getJSONObject("mapIterationDates");
		Iterator<String> keys = jsonMapIterationDates.keys();
		Map<Integer, Date> mapIterationDates = new HashMap<Integer, Date>();
		while (keys.hasNext()) {
			Object key = keys.next();
			Object value = jsonMapIterationDates.get(""+key);
			Date date = parseJsonDate(""+value);
			mapIterationDates.put(Integer.valueOf(""+key), date);
		}
		return mapIterationDates;
	}

	private static List<Integer> aux_retrieveIterations(JSONObject jsonTrMatrix, AbstractLogger logger) throws HandlingException {
		List<Integer> iterations = new ArrayList<Integer>();
		JSONArray jsonIterations = jsonTrMatrix.getJSONArray("iterations");
		for (int i = 0; i < jsonIterations.length(); i++) {
			iterations.add( jsonIterations.getInt(i));
		}
		return iterations;
	}

	public static TransitionMatrix parseTransitionMatrix(JSONObject jsonTrMatrix, AbstractLogger logger) throws HandlingException {
		TransitionMatrix result = new TransitionMatrix();
		parseJSONObject(result, jsonTrMatrix, logger);
		try {
			result.setKey(parseTransitionMatrixKey(jsonTrMatrix.getJSONObject("key"), logger));
			result.setCompleteObsMatrix(parseJsonIterationMatrix(jsonTrMatrix.getJSONObject("completeObsMatrix"), logger));
			result.setCompleteCorrectionsMatrix(parseJsonIterationMatrix(jsonTrMatrix.getJSONObject("completeCorrectionsMatrix"), logger));
			result.setAllObsMatrix(parseJsonMatrix(jsonTrMatrix.getJSONObject("allObsMatrix"), logger));			
			result.setAllCorrectionsMatrix(parseJsonMatrix(jsonTrMatrix.getJSONObject("allCorrectionsMatrix"), logger));
			result.setNormalizedMatrix1(parseJsonMatrix(jsonTrMatrix.getJSONObject("normalizedMatrix1"), logger));
			result.setNormalizedMatrix2(parseJsonMatrix(jsonTrMatrix.getJSONObject("normalizedMatrix2"), logger));
			Map<Integer, Date> mapIterationDates = aux_retrieveMapIterationDates(jsonTrMatrix, logger);
		} catch (JSONException e) {
			logger.error(e);
		} catch (Exception e) {
			logger.error(e);
		}
		return result;
	}

	/*
	public static NodeTransitionMatrices parseNodeTransitionMatrices(JSONObject jsonNodeTM, AbstractLogger logger) throws HandlingException {
		NodeTransitionMatrices result = new NodeTransitionMatrices();
		parseJSONObject(result, jsonNodeTM, logger);
		try {
			//JSONObject jsonNodeLocation = jsonNodeTM.getJSONObject("nodeLocation");
			//result.setNodeLocation(parseNodeLocation(jsonNodeLocation, logger));
			JSONArray jsonVariables = jsonNodeTM.getJSONArray("variables");
			List<String> listVariables = new ArrayList<>();
			for (int i = 0; i < jsonVariables.length(); i++) {
				listVariables.add("" + jsonVariables.get(i) );
			}
			String[] resultArray = new String[listVariables.size()];
			resultArray = listVariables.toArray(resultArray);
			//result.setVariables(resultArray);
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
			result.setFeaturesKey(parseFeaturesKey(jsonNodeTM.getJSONObject("featuresKey"), logger));
			Map<Integer, Date> mapIterationDates = aux_retrieveMapIterationDates(jsonNodeTM, logger);
			//result.setMapIterationDates(mapIterationDates);
			List<Integer> iterations = aux_retrieveIterations(jsonNodeTM, logger);
			//result.setIterations(iterations);
		} catch (JSONException e) {
			logger.error(e);
		} catch (Exception e) {
			logger.error(e);
		}
		return result;
	}*/

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

	public static Object auxGetEnumValue(Class enumClass, String fieldName, JSONObject jsonObject) {
		if (jsonObject != null && enumClass.isEnum()) {
			String svalue = jsonObject.getString(fieldName);
			Object result = Enum.valueOf(enumClass, svalue);
			return result;
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
					Object value = jsonObject.get(fieldName);
					Object valueToSet = null;
					Object enumValue = auxGetEnumValue(paramType, fieldName, jsonObject);
					try {
						if(enumValue != null) {
							valueToSet = enumValue;
						} else if(paramType.equals(String.class)) {
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
						// Added 1st June 2024
						} else if(String[].class.equals(paramType) && value instanceof JSONArray) {
							JSONArray jsonArray = (JSONArray) value;
							String[] toSet = new String[jsonArray.length()];
							for (int i = 0; i < jsonArray.length() ; i++) {
								Object valueItem = jsonArray.get(i);
								toSet[i] = ""+valueItem;
							}
							valueToSet = toSet;
						} else if(Date[].class.equals(paramType) && value instanceof JSONArray) {
							JSONArray jsonArray = (JSONArray) value;
							Date[] toSet = new Date[jsonArray.length()];
							for (int i = 0; i < jsonArray.length() ; i++) {
								Object valueItem = jsonArray.get(i);
								toSet[i] = UtilJsonParser.parseJsonDate(""+valueItem);
							}
							valueToSet = toSet;
						} else if(String[].class.equals(paramType) && !(value instanceof JSONArray)) {
							String sValue = ""+value;
							if(sValue.length() > 0) {
								String[] toSet = sValue.split(",");
								if(toSet.length > 0) {
									valueToSet = toSet;
								}
							}
						//} else if(paramType.equals(Map.class)) {
						} else if(Integer[].class.equals(paramType) && value instanceof JSONArray) {
							JSONArray jsonArray = (JSONArray) value;
							Integer[] toSet = new Integer[jsonArray.length()];
							for (int i = 0; i < jsonArray.length() ; i++) {
								Object valueItem = jsonArray.get(i);
								toSet[i] = Integer.valueOf(""+valueItem);
							}
							valueToSet = toSet;
						} else if(Double[].class.equals(paramType) && value instanceof JSONArray) {
							JSONArray jsonArray = (JSONArray) value;
							Double[] toSet = new Double[jsonArray.length()];
							for (int i = 0; i < jsonArray.length() ; i++) {
								Object valueItem = jsonArray.get(i);
								toSet[i] = Double.valueOf(""+valueItem);
							}
							valueToSet = toSet;
						} else if(Float[].class.equals(paramType) && value instanceof JSONArray) {
							logger.warning("fillObject : Float[] not tested ");
							JSONArray jsonArray = (JSONArray) value;
							Float[] toSet = new Float[jsonArray.length()];
							for (int i = 0; i < jsonArray.length() ; i++) {
								Object valueItem = jsonArray.get(i);
								toSet[i] = Float.valueOf(""+valueItem);
							}
						} else if(Long[].class.equals(paramType) && value instanceof JSONArray) {
							JSONArray jsonArray = (JSONArray) value;
							Long[] toSet = new Long[jsonArray.length()];
							for (int i = 0; i < jsonArray.length() ; i++) {
								Object valueItem = jsonArray.get(i);
								toSet[i] = Long.valueOf(""+valueItem);;
							}
							valueToSet = toSet;
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

	public static OperationResult parseOperationResult(JSONObject jsonAgent, AbstractLogger logger) throws HandlingException {
		OperationResult operationResult = new OperationResult();
		parseJSONObject(operationResult, jsonAgent, logger);
		return operationResult;
	}

	public static OptionItem parseOptionItem(JSONObject jsonOptionItem, AbstractLogger logger) throws HandlingException {
		OptionItem optionItem = new OptionItem();
		parseJSONObject(optionItem, jsonOptionItem, logger);
		return optionItem;
	}

	public static AgentForm parseAgentForm(JSONObject jsonAgent, AbstractLogger logger) throws HandlingException {
		AgentForm agentForm = new AgentForm();
		parseJSONObject(agentForm, jsonAgent, logger);
		Map<String, Double> offersRepartition2 = parseJsonMapDouble(jsonAgent.getJSONObject("offersRepartition"), logger);
		PowerSlot ongoingContractTotal = new PowerSlot();
		parseJSONObject(ongoingContractTotal, jsonAgent.getJSONObject("ongoingContractsTotalLocal"), logger);
		agentForm.setOngoingContractsTotal(ongoingContractTotal);
		agentForm.setOffersRepartition(offersRepartition2);
		Map<String, PowerSlot> contractsRepartition2 = parseJsonMapPowerSlot(jsonAgent.getJSONObject("ongoingContractsRepartition"), logger);
		agentForm.setOngoingContractsRepartition(contractsRepartition2);
		String sDeviceCategory = "" + jsonAgent.get("deviceCategory");
		DeviceCategory deviceCategory = DeviceCategory.valueOf(sDeviceCategory);
		agentForm.setDeviceCategory(deviceCategory);
		NodeLocation nodeLocation = parseNodeLocationg(jsonAgent.getJSONObject("location"), logger);
		agentForm.setLocation(nodeLocation);
		return agentForm;
	}

	public static ForcastingResult1 parseForcastingResult1(JSONObject jsonForcastingResult, AbstractLogger logger) throws HandlingException {
		ForcastingResult1 result = new ForcastingResult1();
		JSONArray json_values = jsonForcastingResult.getJSONArray("predictions");
		logger.info("parseForcastingResult : json_values = "+ json_values);
		List<Double> mainList = new ArrayList<>();
		for(int idx1=0; idx1 < json_values.length(); idx1++) {
			Object value = json_values.get(idx1);
			mainList.add(SapereUtil.getDoubleValue(value));
		}
		result.setPredictions(mainList);
		return result;
	}

	public static ForcastingResult2 parseForcastingResult2(JSONObject jsonForcastingResult, AbstractLogger logger) throws HandlingException {
		ForcastingResult2 result = new ForcastingResult2();
		JSONArray json_values = jsonForcastingResult.getJSONArray("predictions");
		//logger.info("parseForcastingResult : json_values = "+ json_values);
		List<List<Double>> mainList = new ArrayList<>();
		for(int idx1=0; idx1 < json_values.length(); idx1++) {
			Object json_obj2 = json_values.get(idx1);
			List<Double> subList = new ArrayList<Double>();
			if (json_obj2 instanceof JSONArray) {
				JSONArray json_array2 = (JSONArray) json_obj2;
				//logger.info("parseForcastingResult : json_array2 = "+ json_array2);
				for(int idx=0; idx < json_array2.length(); idx++) {
					Object value2 = json_array2.get(idx);
					subList.add(SapereUtil.getDoubleValue(value2));
				}
			} else if (json_obj2 instanceof double[]) {
				for(double nextDouble : (double[]) json_obj2) {
					subList.add(nextDouble);
				}
			} else if (json_obj2 instanceof float[]) {
				for(float nextDouble : (float[]) json_obj2) {
					subList.add((double) nextDouble);
				}
			}
			mainList.add(subList);
		}
		result.setPredictions(mainList);
		return result;
	}

	private static List<List<Double>> auxRetrieve2dimListOfDouble(JSONArray json_listOfList, AbstractLogger logger) {
		List<List<Double>> listOfList = new ArrayList<List<Double>>();
		for(int idx1=0; idx1 < json_listOfList.length(); idx1++) {
			Object json_obj2 = json_listOfList.get(idx1);
			List<Double> subList = new ArrayList<Double>();
			if (json_obj2 instanceof JSONArray) {
				JSONArray json_array2 = (JSONArray) json_obj2;
				//logger.info("parseForcastingResult : json_array2 = "+ json_array2);
				for(int idx=0; idx < json_array2.length(); idx++) {
					Object value2 = json_array2.get(idx);
					subList.add(SapereUtil.getDoubleValue(value2));
				}
			} else if (json_obj2 instanceof double[]) {
				for(double nextDouble : (double[]) json_obj2) {
					subList.add(nextDouble);
				}
			} else if (json_obj2 instanceof float[]) {
				for(float nextDouble : (float[]) json_obj2) {
					subList.add((double) nextDouble);
				}
			}
			listOfList.add(subList);
		}
		return listOfList;
	}

	public static List<Date> parseListDates(JSONArray json_dates, AbstractLogger logger) {
		List<Date> listDates = new ArrayList<Date>();
		for (int idx1 = 0; idx1 < json_dates.length(); idx1++) {
			String sNextDate = "" + json_dates.get(idx1);
			try {
				Date nextDate = parseJsonDate(sNextDate);
				listDates.add(nextDate);
			} catch (HandlingException e) {
				logger.error(e);
			}
		}
		return listDates;
	}

	public static List<List<Date>> parseListListDates(JSONArray json_dates, AbstractLogger logger) {
		List<List<Date>> result = new ArrayList<List<Date>>();
		for (int idx1 = 0; idx1 < json_dates.length(); idx1++) {
			JSONArray nextJsonDateList = json_dates.getJSONArray(idx1);
			List<Date> nextDateList = parseListDates(nextJsonDateList, logger);
			result.add(nextDateList);
		}
		return result;
	}

	public static List<Integer> parseListInteger(JSONArray json_dates, AbstractLogger logger) {
		List<Integer> listInteger = new ArrayList<Integer>();
		for (int idx1 = 0; idx1 < json_dates.length(); idx1++) {
			Object nextObj = json_dates.get(idx1);
			Integer nextInt = SapereUtil.getIntValue(nextObj);
			listInteger.add(nextInt);
		}
		return listInteger;
	}

	public static LSTMPredictionResult parseLSTMPredictionResult(JSONObject jsonPredictionResult,
			AbstractLogger logger) {
		LSTMPredictionResult result = new LSTMPredictionResult();
		parseJSONObject(result, jsonPredictionResult, logger);
		JSONArray json_predictedValues = jsonPredictionResult.getJSONArray("listPredicted");
		List<List<Double>> predictedValues = auxRetrieve2dimListOfDouble(json_predictedValues, logger);
		result.setListPredicted(predictedValues);
		List<Integer> listHorizon = parseListInteger(jsonPredictionResult.getJSONArray("horizons"), logger);
		result.setHorizons(listHorizon);
		List<List<Date>> listDatesX = parseListListDates(jsonPredictionResult.getJSONArray("listDatesX"), logger);
		result.setListDatesX(listDatesX);
		List<Date> predictionDates = parseListDates(jsonPredictionResult.getJSONArray("predictionDates"), logger);
		result.setPredictionDates(predictionDates);
		JSONArray json_listY = jsonPredictionResult.getJSONArray("listY");
		List<List<Double>> trueValues = auxRetrieve2dimListOfDouble(json_listY, logger);
		result.setListTrue(trueValues);
		return result;
	}

	public static LayerDefinition parseLayerDefinition(JSONObject jsonLayer, AbstractLogger logger) {
		LayerDefinition result = new LayerDefinition();
		parseJSONObject(result, jsonLayer, logger);
		JSONArray jsonParamsTypes = jsonLayer.getJSONArray("paramTypes");
		List<ParamType> listParamType = new ArrayList<ParamType>();
		for (int idx = 0; idx < jsonParamsTypes.length(); idx++) {
			String sParamType = jsonParamsTypes.getString(idx);
			listParamType.add(ParamType.valueOf(sParamType));
		}
		result.setParamTypes(listParamType);
		/*
		 * List<Integer> dimensions = new ArrayList<Integer>(); JSONArray
		 * json_dimensions = jsonLayer.getJSONArray("listPredicted"); for(int idx=0; idx
		 * < json_layers.length(); idx++) { JSONObject json_layer =
		 * json_layers.getJSONObject(idx); } layerDimension
		 */
		return result;
	}

	public static List<LSTMModelInfo> parseListLSTMModelInfo(JSONArray jsonListModelInfo, AbstractLogger logger)
			throws HandlingException {
		List<LSTMModelInfo> result = new ArrayList<LSTMModelInfo>();
		for (int idx = 0; idx < jsonListModelInfo.length(); idx++) {
			JSONObject json_modelInfo = jsonListModelInfo.getJSONObject(idx);
			result.add(parseLSTMModelInfo(json_modelInfo, logger));
		}
		return result;
	}

	public static LSTMModelInfo parseLSTMModelInfo(JSONObject jsonModelInfo, AbstractLogger logger)
			throws HandlingException {
		LSTMModelInfo result = new LSTMModelInfo();
		parseJSONObject(result, jsonModelInfo, logger);
		List<LayerDefinition> layers = new ArrayList<LayerDefinition>();
		JSONArray json_layers = jsonModelInfo.getJSONArray("layers");
		for (int idx = 0; idx < json_layers.length(); idx++) {
			JSONObject json_layer = json_layers.getJSONObject(idx);
			layers.add(parseLayerDefinition(json_layer, logger));
		}
		result.setLayers(layers);
		JSONObject jsonMatrices = jsonModelInfo.getJSONObject("mapMatrices");
		Map<String, DoubleMatrix> mapMatrices = new HashMap<String, DoubleMatrix>();
		Iterator<String> keys1 = jsonMatrices.keys();
		while (keys1.hasNext()) {
			String key = keys1.next();
			JSONArray jsonArray = jsonMatrices.getJSONArray(key);
			DoubleMatrix trMatrice = parseJsonMatrix2(jsonArray, logger);
			if(trMatrice == null) {
				logger.error("parseLSTMModelInfo : null matrix for key " + key + " resulting from jsonArray " + jsonArray);
			} else {
				mapMatrices.put(key, trMatrice);
			}
		}
		result.setMapMatrices(mapMatrices);
		Map<String, List<Integer>> mapShapes = new HashMap<String, List<Integer>>();
		JSONObject jsonShapes = jsonModelInfo.getJSONObject("mapShapes");
		Iterator<String> keys2 = jsonShapes.keys();
		while (keys2.hasNext()) {
			String key2 = keys2.next();
			JSONArray jsonArray = jsonShapes.getJSONArray(key2);
			List<Integer> dimensions = new ArrayList<Integer>();
			for (int idx = 0; idx < jsonArray.length(); idx++) {
				Integer nextDimension = SapereUtil.getIntValue(jsonArray.get(idx));
				dimensions.add(nextDimension);
			}
			mapShapes.put(key2, dimensions);
		}
		result.setMapShapes(mapShapes);
		return result;
	}

	private static NodeTotal parseNodeTotal(JSONObject jsonNodeTotal, AbstractLogger logger) throws HandlingException {
		NodeTotal nodeTotal = new NodeTotal();
		parseJSONObject(nodeTotal, jsonNodeTotal, logger);
		return nodeTotal;
	}

	public static Date parseJsonDate(String jsonDate) throws HandlingException {
		if(jsonDate.contains("T")) {
			String shortDate = jsonDate.substring(0, 19);
			UtilDates.format_json_datetime_prev.setTimeZone(TimeZone.getTimeZone("Europe/Zurich"));
			Date date1;
			try {
				date1 = UtilDates.format_json_datetime_prev.parse(shortDate);
			} catch (ParseException e) {
				throw new HandlingException(""+e);
			}
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
			Date date2;
			try {
				date2 = UtilDates.format_json_datetime.parse(jsonDate);
			} catch (ParseException e) {
				throw new HandlingException(""+e);
			}
			return date2;
		}
	}

	public static LaunchConfig parseLaunchConfig(JSONObject jsonLaunchConcig, AbstractLogger logger) throws HandlingException {
		LaunchConfig  launchConfig = new LaunchConfig();
		Map<String,String> mapNodeByLocation = parseJsonMapString(jsonLaunchConcig.getJSONObject("mapNodeByLocation"), logger);
		launchConfig.setMapNodeByLocation(mapNodeByLocation);
		JSONObject jsonMapNodes = jsonLaunchConcig.getJSONObject("mapNodes");
		Map<String, NodeLocation> mapNodeLocation = new HashMap<String, NodeLocation>();
		Iterator<String> keys = jsonMapNodes.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			JSONObject jsonNodeLocation = jsonMapNodes.getJSONObject(key);
			NodeLocation nodeLocation = new NodeLocation();
			parseJSONObject(nodeLocation, jsonNodeLocation, logger);
			mapNodeLocation.put(key, nodeLocation);
		}
		launchConfig.setMapNodes(mapNodeLocation);
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
						} else if (method.isAnnotationPresent(DisableJson.class)) {
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
