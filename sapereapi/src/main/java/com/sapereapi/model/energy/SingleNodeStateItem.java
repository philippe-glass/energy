package com.sapereapi.model.energy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sapereapi.model.HandlingException;
import com.sapereapi.model.input.HistoryInitializationForm;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.learning.PredictionScope;
import com.sapereapi.model.learning.VariableState;
import com.sapereapi.model.learning.lstm.request.LSTMPredictionRequest;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.log.AbstractLogger;

public class SingleNodeStateItem implements Serializable {
	private static final long serialVersionUID = 1L;
	Date date;
	Map<String, Double> mapValues;
	Map<String, Integer> mapStateIds;

	public SingleNodeStateItem() {
		super();
		mapValues = new HashMap<String, Double>();
		mapStateIds = new HashMap<String, Integer>();
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Map<String, Double> getMapValues() {
		return mapValues;
	}

	public Double getValue(String variable) {
		if (!mapValues.containsKey(variable)) {
			return null;
		}
		return mapValues.get(variable);
	}

	public void setMapValues(Map<String, Double> mapValues) {
		this.mapValues = mapValues;
	}

	public Map<String, Integer> getMapStateIds() {
		return mapStateIds;
	}

	public void setMapStateIds(Map<String, Integer> mapStateIds) {
		this.mapStateIds = mapStateIds;
	}

	public Integer getStateId(String variable) {
		if (!mapStateIds.containsKey(variable)) {
			return null;
		}
		return mapStateIds.get(variable);
	}

	public void setStateId(String variable, Integer stateId) {
		mapStateIds.put(variable, stateId);
	}

	public void setValue(String variable, Double value) {
		mapValues.put(variable, value);
	}

	public Map<String, SingleStateItem> generateMapSingleStateItem() {
		Map<String, SingleStateItem> result = new HashMap<String, SingleStateItem>();
		for (String variable : mapValues.keySet()) {
			Double value = mapValues.get(variable);
			Integer stateId = null;
			if (mapStateIds.containsKey(variable)) {
				stateId = mapStateIds.get(variable);
			}
			result.put(variable, new SingleStateItem(date, variable, value, stateId));
		}
		return result;
	}

	public SingleStateItem getSingleState(String variable) {
		Double value = getValue(variable);
		Integer stateId = getStateId(variable);
		SingleStateItem result = new SingleStateItem(date, variable, value, stateId);
		return result;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(UtilDates.format_time.format(date));
		result.append("{");
		for (String variable : mapStateIds.keySet()) {
			Integer stateId = mapStateIds.get(variable);
			result.append(variable).append(":S").append(stateId);
		}
		result.append("}");
		return result.toString();
	}

	public void dumpValuesCsv(FileWriter fileWriter, String separator, String[] listValariables) throws IOException {
		//StringBuffer result = new StringBuffer();
		fileWriter.write(UtilDates.format_date_time_py.format(date));
		fileWriter.write(separator);
		for (String variable : listValariables) {
			Double nextValue = .0;
			if(mapValues.containsKey(variable)) {
				nextValue = mapValues.get(variable);
			}
			fileWriter.write(UtilDates.df3.format(nextValue));
			fileWriter.write(separator);
		}
		int varIndex = 1;
		for (String variable : listValariables) {
			String nextState = "";
			if(mapStateIds.containsKey(variable)) {
				Integer stateId = mapStateIds.get(variable);
				nextState = "" + stateId;
			}
			fileWriter.write(nextState);
			if (varIndex < listValariables.length) {
				fileWriter.write(separator);
			}
			varIndex++;
		}
	}

	public static List<SingleNodeStateItem> loadFromCsvFile(String csvPath, String separator, String[] listValariables)
			throws ParseException, IOException {
		List<SingleNodeStateItem> result = new ArrayList<SingleNodeStateItem>();
		try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("date_time")) {
					// this is the header line : ignore it
				} else if (line.length() > 0) {
					SingleNodeStateItem nextItem = loadFromCsvLine(line, separator, listValariables);
					result.add(nextItem);
				}
			}
		}
		return result;
	}

	public static SingleNodeStateItem loadFromCsvLine(String csvLine, String separator, String[] listValariables) throws ParseException {
		// 16/12/2022 17:42:00,3276.43,0,0,0,0,3276.43,2,1,1,1,1,2
		SingleNodeStateItem result = new SingleNodeStateItem();
		String[] content = csvLine.split(separator);
		String sDate = content[0];
		Date date = UtilDates.format_date_time_py.parse(sDate);
		result.setDate(date);
		int variableIdx = 0;
		for(String variable : listValariables) {
			if(1+listValariables.length + variableIdx < content.length) {
				String sValue = content[1+variableIdx];
				String sStateId = content[1+listValariables.length + variableIdx];
				Double nextValue = Double.valueOf(sValue);
				Integer nextStateId = Integer.valueOf(sStateId);
				result.setValue(variable, nextValue);
				result.setStateId(variable, nextStateId);
			}
			variableIdx++;
		}
		return result;
	}

	public SingleNodeStateItem clone() {
		SingleNodeStateItem result = new SingleNodeStateItem();
		result.setDate(date);
		for(String variable : mapStateIds.keySet()) {
			result.setStateId(variable, mapStateIds.get(variable));
		}
		for(String variable : mapValues.keySet()) {
			result.setValue(variable, mapValues.get(variable));
		}
		return result;
	}

	public static LSTMPredictionRequest preparePredictParameters1(
			List<SingleNodeStateItem> listAllNodeStateItems,
			Date initDate, int depthX, int depthY, int nbOfItems, String nodeName
			, PredictionScope scope,
			String variable, Integer[] horizons
			, AbstractLogger logger) throws HandlingException {
		long msRemain = initDate.getTime() % 1000;
		if (msRemain != 0) {
			throw new HandlingException("SingleNodeStateItem.preparePredictParameters1 : msReamin = " + msRemain
					+ " is not null in the given init date " + initDate);
		}
		SingleNodeStateItem initDateItem = SapereUtil.selectClosestStateItem(listAllNodeStateItems, initDate, logger);
		if (initDateItem == null) {
			throw new HandlingException(
					"SingleNodeStateItem.preparePredictParameters1 : no elements in listAllNodeStateItems list arround the given init date "
							+ initDate);
		}
		TreeMap<Date, SingleNodeStateItem> mapSelectedStateHistory = SapereUtil.selectMapHistory(listAllNodeStateItems,
				null, initDateItem.getDate());
		List<Date> allDatesBefore = new ArrayList<Date>();
		for (Date nextDate : mapSelectedStateHistory.keySet()) {
			allDatesBefore.add(nextDate);
		}
		int len = allDatesBefore.size();
		if (len - nbOfItems - depthX + 1 < 0) {
			throw new HandlingException(
					"SingleNodeStateItem.preparePredictParameters1 : not enough elements in listAllNodeStateItems list "
							+ listAllNodeStateItems.size());
		}
		List<Date> selectedDatesBefore = new ArrayList<Date>();
		selectedDatesBefore = allDatesBefore.subList(len - nbOfItems - depthX + 1, len);
		LSTMPredictionRequest result = new LSTMPredictionRequest();
		result.setInitialDate(initDate);
		Double initialValue = initDateItem.getValue(variable);
		result.setInitialValue(initialValue);
		try {
			VariableState initialState = NodeStates.getVariablState(initialValue);
			result.setInitialState(initialState);
		} catch (HandlingException e) {
			logger.error(e);
			throw new HandlingException(e.getMessage());
		}
		List<Date> targetDates = new ArrayList<Date>();
		for (int nextHirizonMinutes : horizons) {
			targetDates.add(UtilDates.shiftDateMinutes(initDate, nextHirizonMinutes));
		}
		List<Integer> listHorizons = Arrays.asList(horizons);
		result.setListHorizons(listHorizons);
		result.setTargetDates(targetDates);
		result.setNodeName(nodeName);
		result.setVariable(variable);
		result.setScope(scope);
		List<Double> values = new ArrayList<Double>();
		List<Date> dates = new ArrayList<Date>();
		for (Date nextDate : selectedDatesBefore) {
			SingleNodeStateItem nextStateItem = mapSelectedStateHistory.get(nextDate);
			Double value = nextStateItem.getValue(variable);
			values.add(value);
			dates.add(nextStateItem.getDate());
		}
		List<List<Double>> listX = new ArrayList<List<Double>>();
		List<List<Double>> listTrue = new ArrayList<List<Double>>();
		List<List<Date>> listDatesX = new ArrayList<List<Date>>();
		for (int itemIdx = 0; itemIdx < nbOfItems; itemIdx++) {
			List<Double> lastValues = new ArrayList<Double>();
			List<Date> lastDates = new ArrayList<Date>();
			// Date lastDate = null;
			for (int depthXIdx = 0; depthXIdx < depthX; depthXIdx++) {
				int valueIdx = itemIdx + depthXIdx;
				if (valueIdx < values.size()) {
					Double nextValue = values.get(valueIdx);
					lastValues.add(nextValue);
				}
				if (valueIdx < dates.size()) {
					Date nextDate = dates.get(valueIdx);
					lastDates.add(nextDate);
				}
			}
			listX.add(lastValues);
			listDatesX.add(lastDates);
		}
		result.setListDatesX(listDatesX);
		result.setListX(listX);
		result.setListTrue(listTrue);
		return result;
	}

	public static String dumpListOfList(List<List<Double>> d2List, String separator) {
		List<String> listX2 = new ArrayList<String>();
		for(List<Double> nextListX : d2List) {
			String sNextListX = "";
			String sep="";
			for(Double nextItem : nextListX) {
				sNextListX+= sep + nextItem;
				sep=",";
			}
			listX2.add(String.join(",", sNextListX));
		}
		return String.join(separator, listX2);
	}

	public static String dumpListOfDates(List<Date> listofDate, String separator) {
		String result = "";
		String sep="";
		for(Date date : listofDate) {
			String sDate = UtilDates.format_date_time_py.format(date);
			result+=sep + sDate;
		}
		return result;
	}

	public static String dumpListOfListOfDates(List<List<Date>> d2List, String separator) {
		List<String> listOfDatesStr = new ArrayList<String>();
		for(List<Date> nextListOfDates : d2List) {
			String next = dumpListOfDates(nextListOfDates, ",");
			listOfDatesStr.add(next);
		}
		return String.join(separator, listOfDatesStr);
	}

	public static HistoryInitializationForm initHistoryForm(List<SingleNodeStateItem> listNodeStateItems,
			PredictionContext preidctionContext) {
		HistoryInitializationForm historyInitForm = new HistoryInitializationForm();
		int historyLen = listNodeStateItems.size();
		Date[] dates = new Date[historyLen];
		Double[] produced = new Double[historyLen];
		Double[] requested = new Double[historyLen];
		Double[] available = new Double[historyLen];
		Double[] missing = new Double[historyLen];
		Double[] consumed = new Double[historyLen];
		Double[] provided = new Double[historyLen];
		int idx = 0;
		for (SingleNodeStateItem nextStateItem : listNodeStateItems) {
			dates[idx] = nextStateItem.getDate();
			produced[idx] = nextStateItem.getValue("produced");
			requested[idx] = nextStateItem.getValue("requested");
			available[idx] = nextStateItem.getValue("available");
			missing[idx] = nextStateItem.getValue("missing");
			consumed[idx] = nextStateItem.getValue("consumed");
			provided[idx] = nextStateItem.getValue("provided");
			idx++;
		}
		historyInitForm.setScope(preidctionContext.getScope().toOptionItem());
		historyInitForm.setUsedModel(preidctionContext.getModelType());
		historyInitForm.setNodeName(preidctionContext.getNodeLocation().getName());
		historyInitForm.setListVariables(preidctionContext.getNodeContext().getVariables());
		historyInitForm.setListDates(dates);
		historyInitForm.setProduced(produced);
		historyInitForm.setRequested(requested);
		historyInitForm.setAvailable(available);
		historyInitForm.setMissing(missing);
		historyInitForm.setProvided(provided);
		historyInitForm.setConsumed(consumed);
		historyInitForm.setCompleteMatrices(Boolean.TRUE);
		return historyInitForm;
	}

	public static Map<String, String> preparePredictParameters2(List<SingleNodeStateItem> listNodeStateItems,
			Date initDate, int depthX, int depthY, int nbOfItems, String nodeName, PredictionScope scope,
			String variable, Integer[] horizons, AbstractLogger logger) throws HandlingException {
		LSTMPredictionRequest result1 = preparePredictParameters1(listNodeStateItems, initDate, depthX, depthY,
				nbOfItems, nodeName, scope, variable, horizons, logger);
		Map<String, String> mapParams = new HashMap<String, String>();
		mapParams.put("X", dumpListOfList(result1.getListX(), "|"));
		mapParams.put("Y", dumpListOfList(result1.getListTrue(), "|"));
		List<List<Date>> datesX = result1.getListDatesX();
		mapParams.put("INDEXES", dumpListOfListOfDates(result1.getListDatesX(), ","));
		mapParams.put("display_result", "False");
		return mapParams;
	}
}
