package com.sapereapi.util;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sapereapi.log.SimulatorLogger;
import com.sapereapi.model.OperationResult;
import com.sapereapi.model.energy.AgentFilter;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.Device;
import com.sapereapi.model.energy.DeviceMeasure;
import com.sapereapi.model.energy.DeviceProperties;
import com.sapereapi.model.energy.NodeContent;
import com.sapereapi.model.energy.NodeTotal;
import com.sapereapi.model.energy.OptionItem;
import com.sapereapi.model.energy.PowerSlot;
import com.sapereapi.model.markov.MarkovStateHistory;
import com.sapereapi.model.markov.MarkovTimeWindow;
import com.sapereapi.model.markov.NodeTransitionMatrices;
import com.sapereapi.model.markov.TransitionMatrix;
import com.sapereapi.model.markov.TransitionMatrixKey;
import com.sapereapi.model.referential.DeviceCategory;
import com.sapereapi.model.referential.EnvironmentalImpact;

import Jama.Matrix;

public class UtilJsonParser {
	private static SimpleDateFormat json_date_format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

	static {
		json_date_format.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	public static NodeContent parseNodeContent(JSONObject jsonNodeContent) {
		long timeShiftMS = jsonNodeContent.getLong("timeShiftMS");
		NodeContent result = new NodeContent(new AgentFilter(), timeShiftMS);
		JSONObject jsonNodeTotal = jsonNodeContent.getJSONObject("total");
		try {
			NodeTotal nodeTotal = parseNodeTotal(jsonNodeTotal);
			result.setTotal(nodeTotal);
		} catch (Exception e1) {
			SimulatorLogger.getInstance().error(e1);
		}

		JSONArray json_producers = jsonNodeContent.getJSONArray("producers");
		for (int i = 0; i < json_producers.length(); i++) {
			JSONObject jsonAgentForm = json_producers.getJSONObject(i);
			try {
				AgentForm producer = parseAgentForm(jsonAgentForm);
				result.addProducer(producer);
			} catch (Exception e) {
				SimulatorLogger.getInstance().error(e);
			}
		}
		JSONArray json_consumers = jsonNodeContent.getJSONArray("consumers");
		for (int i = 0; i < json_consumers.length(); i++) {
			JSONObject jsonAgentForm = json_consumers.getJSONObject(i);
			try {
				AgentForm consumer = parseAgentForm(jsonAgentForm);
				result.addConsumer(consumer);
			} catch (Exception e) {
				SimulatorLogger.getInstance().error(e);
			}
		}
		return result;
	}

	public static Map<String, Double> parseJsonMap(JSONObject jsonobj) throws Exception {
		Map<String, Double> map = new HashMap<String, Double>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Double value = jsonobj.getDouble(key);
			map.put(key, value);
		}
		return map;
	}

	public static Map<String, Integer> parseJsonMapInteger(JSONObject jsonobj) throws Exception {
		Map<String, Integer> map = new HashMap<String, Integer>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Integer value = jsonobj.getInt(key);
			map.put(key, value);
		}
		return map;
	}

	public static Map<String, Long> parseJsonMapLong(JSONObject jsonobj) throws Exception {
		Map<String, Long> map = new HashMap<String, Long>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Long value = jsonobj.getLong(key);
			map.put(key, value);
		}
		return map;
	}

	public static Matrix parseJsonMatrix(JSONObject jsonobj) throws Exception {
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

	public static Map<String, Matrix> parseJsonMapMatrix(JSONObject jsonobj) throws Exception {
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

	public static Map<String, Double> parseJsonMap2(JSONObject jsonobj) throws Exception {
		Map<String, Double> map = new HashMap<String, Double>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Double value = jsonobj.getDouble(key);
			map.put(key, value);
		}
		return map;
	}

	public static Map<String, PowerSlot> parseJsonMapPowerSlot(JSONObject jsonobj) throws Exception {
		Map<String, PowerSlot> map = new HashMap<String, PowerSlot>();
		Iterator<String> keys = jsonobj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			PowerSlot powerSlot = new PowerSlot();
			// parse the power slot
			parseJSONObject(powerSlot, jsonobj);
			map.put(key, powerSlot);
		}
		return map;
	}

	public static DeviceMeasure parseDeviceMeasure(JSONObject jsonDeviceMeasure) throws Exception {
		DeviceMeasure mesasure = new DeviceMeasure();
		parseJSONObject(mesasure, jsonDeviceMeasure);
		mesasure.setMap_power_p(parseJsonMap(jsonDeviceMeasure.getJSONObject("map_power_p")));
		mesasure.setMap_power_q(parseJsonMap(jsonDeviceMeasure.getJSONObject("map_power_q")));
		mesasure.setMap_power_s(parseJsonMap(jsonDeviceMeasure.getJSONObject("map_power_s")));
		return mesasure;
	}

	public static MarkovStateHistory parseMarkovStateHistory(JSONObject jsonDevice) throws Exception {
		MarkovStateHistory markovStateHistory = new MarkovStateHistory();
		parseJSONObject(markovStateHistory, jsonDevice);
		return markovStateHistory;
	}

	public static Device parseDevice(JSONObject jsonDevice) throws Exception {
		Device device = new Device();
		parseJSONObject(device, jsonDevice);
		DeviceProperties deviceProperties = parseDeviceProperties(jsonDevice.getJSONObject("properties"));
		device.setProperties(deviceProperties);
		return device;
	}

	public static DeviceProperties parseDeviceProperties(JSONObject jsonDeviceProperties) throws Exception {
		DeviceProperties deviceProperties = new DeviceProperties();
		parseJSONObject(deviceProperties, jsonDeviceProperties);
		return deviceProperties;
	}

	public static MarkovTimeWindow parseMarkovTimeWindow(JSONObject jsonTimeWindow) throws Exception {
		MarkovTimeWindow timeWinow = new MarkovTimeWindow();
		parseJSONObject(timeWinow, jsonTimeWindow);
		JSONArray jsonDaysOfWeek = jsonTimeWindow.getJSONArray("daysOfWeek");
		Set<Integer> daysOfWeek = new HashSet<Integer>();
		for (int index = 0; index < jsonDaysOfWeek.length(); index++) {
			int nextDay = SapereUtil.getIntValue(jsonDaysOfWeek.get(index));
			daysOfWeek.add(nextDay);
		}
		timeWinow.setDaysOfWeek(daysOfWeek);
		return timeWinow;
	}

	public static TransitionMatrixKey parseTransitionMatrixKey(JSONObject jsonTrMatrixKey) throws Exception {
		TransitionMatrixKey result = new TransitionMatrixKey();
		parseJSONObject(result, jsonTrMatrixKey);
		result.setTimeWindow(parseMarkovTimeWindow(jsonTrMatrixKey.getJSONObject("timeWindow")));
		return result;
	}

	public static TransitionMatrix parseTransitionMatrix(JSONObject jsonTrMatrix) throws Exception {
		TransitionMatrix result = new TransitionMatrix();
		parseJSONObject(result, jsonTrMatrix);
		try {
			result.setKey(parseTransitionMatrixKey(jsonTrMatrix.getJSONObject("key")));
			result.setIterObsMatrix(parseJsonMatrix(jsonTrMatrix.getJSONObject("iterObsMatrix")));
			result.setAllObsMatrix(parseJsonMatrix(jsonTrMatrix.getJSONObject("allObsMatrix")));
			result.setNormalizedMatrix1(parseJsonMatrix(jsonTrMatrix.getJSONObject("normalizedMatrix1")));
			result.setAllCorrectionsMatrix(parseJsonMatrix(jsonTrMatrix.getJSONObject("allCorrectionsMatrix")));
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static NodeTransitionMatrices parseNodeTransitionMatrices(JSONObject jsonNodeTM) throws Exception {
		NodeTransitionMatrices result = new NodeTransitionMatrices();
		parseJSONObject(result, jsonNodeTM);
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
				TransitionMatrix trMatrice = parseTransitionMatrix(jsonValue);
				mapMatrices.put(key, trMatrice);
			}
			result.setMapMatrices(mapMatrices);
			result.setTimeWindow(parseMarkovTimeWindow(jsonNodeTM.getJSONObject("timeWindow")));
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Exception e) {
			SimulatorLogger.getInstance().error(e);
		}
		return result;
	}

	public static Object parseJSONObject(Object targetObject, JSONObject jsonObject)  {
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
						}
					} catch (Throwable e) {
						SimulatorLogger.getInstance().error(e);
					}
					if(valueToSet!=null)  {
						try {
							method.invoke(targetObject, valueToSet);
						} catch (Throwable e) {
							SimulatorLogger.getInstance().error(e);
						}

					}
				}
			}
		}
		return targetObject;
	}

	public static OperationResult parseOperationResult(JSONObject jsonAgent) throws Exception {
		OperationResult operationResult = new OperationResult();
		parseJSONObject(operationResult, jsonAgent);
		return operationResult;
	}

	public static OptionItem parseOptionItem(JSONObject jsonOptionItem) throws Exception {
		OptionItem optionItem = new OptionItem();
		parseJSONObject(optionItem, jsonOptionItem);
		return optionItem;
	}

	public static AgentForm parseAgentForm(JSONObject jsonAgent) throws Exception {
		AgentForm agentForm = new AgentForm();
		parseJSONObject(agentForm, jsonAgent);
		Map<String, Double> offersRepartition2 = parseJsonMap(jsonAgent.getJSONObject("offersRepartition"));
		agentForm.setOffersRepartition(offersRepartition2);
		Map<String, PowerSlot> contractsRepartition2 = parseJsonMapPowerSlot(jsonAgent.getJSONObject("ongoingContractsRepartition"));
		agentForm.setOngoingContractsRepartition(contractsRepartition2);
		OptionItem deviceCategory = parseOptionItem(jsonAgent.getJSONObject("deviceCategory"));
		agentForm.setDeviceCategory(deviceCategory);
		return agentForm;
	}

	private static NodeTotal parseNodeTotal(JSONObject jsonNodeTotal) throws Exception {
		NodeTotal nodeTotal = new NodeTotal();
		parseJSONObject(nodeTotal, jsonNodeTotal);
		return nodeTotal;
	}


	private static Date parseJsonDate(String jsonDate) throws Exception {
		//String testDate = jsonDate.substring(0, 19).replace('T', ' ');
		String shortDate = jsonDate.substring(0, 19);
		Date date1 = json_date_format.parse(shortDate);
		return date1;
	}

}
