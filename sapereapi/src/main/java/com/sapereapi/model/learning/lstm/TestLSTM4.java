package com.sapereapi.model.learning.lstm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.NodeContext;
import com.sapereapi.model.energy.SingleNodeStateItem;
import com.sapereapi.model.input.HistoryInitializationForm;
import com.sapereapi.model.input.HistoryInitializationRequest;
import com.sapereapi.model.learning.FeaturesKey;
import com.sapereapi.model.learning.LearningModelType;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.learning.PredictionScope;
import com.sapereapi.model.learning.VariableState;
import com.sapereapi.model.learning.lstm.request.LSTMPredictionRequest;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.model.learning.prediction.PredictionResult;
import com.sapereapi.util.UtilDates;
import com.sapereapi.util.UtilHttp;
import com.sapereapi.util.UtilJsonParser;

import eu.sapere.middleware.node.NodeLocation;

public class TestLSTM4 {
	static SapereLogger logger = SapereLogger.getInstance();

	public static PredictionContext initContext() {
		//String[] listValariables = { "requested", "produced", "consumed", "provided", "available", "missing" };
		String[] listValariables = { "requested" };
		NodeContext nodeContext = new NodeContext();
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, 2023);
		calendar.set(Calendar.MONTH, 0);
		calendar.set(Calendar.DAY_OF_MONTH, 15);
		Date current = calendar.getTime();
		long datetimeShiftMS = UtilDates.computeTimeShiftMS(current, new Date());
		nodeContext.setNodeLocation(new NodeLocation("N1", "localhost", 10001, 9191));
		nodeContext.setTimeShiftMS(datetimeShiftMS);
		nodeContext.setVariables(listValariables);
		PredictionContext predictionContet = new PredictionContext();
		PredictionScope scope = PredictionScope.NODE;
		predictionContet.setScope(scope);
		predictionContet.setModelType(LearningModelType.LSTM);
		predictionContet.setNodeContext(nodeContext);
		return predictionContet;
	}

	public static void main(String[] args) throws IOException {
		int debugLevel = 0;
		String csvPath = "../lstm/history_data/dump_history_N1.csv";
		String rootUrlLSTM = "http://127.0.0.1:5000/";
		PredictionContext predictionContext = initContext();
		Date current = predictionContext.getCurrentDate();
		Date dateMin = null;//UtilDates.shiftDateDays(current, -1000);
		Date dateMax = UtilDates.shiftDateDays(current, -1);

		NodeContext nodeContext = predictionContext.getNodeContext();
		String nodeName = nodeContext.getNodeLocation().getName();
		List<SingleNodeStateItem> nodeStateItems = new ArrayList<SingleNodeStateItem>();
		try {
			nodeStateItems = SingleNodeStateItem.loadFromCsvFile(csvPath, ",", nodeContext.getVariables());
			HistoryInitializationRequest historyInitRequest = HistoryInitializationRequest.generateHistoryInitReuqest(predictionContext);
			historyInitRequest.setDateMin(dateMin);
			historyInitRequest.setDateMax(dateMax);
			String urlInitHistory = rootUrlLSTM + "init_node_history";
			String postResponse1 = UtilHttp.sendPostRequest(urlInitHistory, historyInitRequest, logger, debugLevel);
			/*
			JSONArray jsonListModelInfo = new JSONArray(postResponse1);
			List<LSTMModelInfo> listModelInfo = UtilJsonParser.parseListLSTMModelInfo(jsonListModelInfo, logger);
			Map<String, VariableLSTMModel> mapModels = new HashMap<String, VariableLSTMModel>();
			for (LSTMModelInfo modelInfo : listModelInfo) {
				mapModels.put(modelInfo.getVariable(), new VariableLSTMModel(modelInfo));
			}*/
			boolean testAdditionalTraining = true;
			if (testAdditionalTraining) {
				HistoryInitializationForm initHistoryForm = SingleNodeStateItem.initHistoryForm(nodeStateItems,
						predictionContext);
				Date dateMax2 = UtilDates.shiftDateDays(current, +1);
				initHistoryForm.setNodeName(nodeName);
				initHistoryForm.setListVariables(nodeContext.getVariables());
				initHistoryForm.setDateMin(dateMin);
				initHistoryForm.setDateMax(dateMax2);
				String urlAddHistory = rootUrlLSTM + "add_node_history";
				String postResponse2 = UtilHttp.sendPostRequest(urlAddHistory, initHistoryForm, logger, debugLevel);
				JSONArray jsonListModelInfo2 = new JSONArray(postResponse2);
				List<LSTMModelInfo> listModelInfo2 = UtilJsonParser.parseListLSTMModelInfo(jsonListModelInfo2, logger);
				Map<String, VariableLSTMModel> mapModels = new HashMap<String, VariableLSTMModel>();
				for (LSTMModelInfo modelInfo : listModelInfo2) {
					mapModels.put(modelInfo.getVariable(), new VariableLSTMModel(modelInfo));
				}
			}
		} catch (Throwable e) {
			logger.error(e);
		}
		try {
			String variable = "requested";
			// String urlPrediction = rootUrlLSTM + "prediction1";
			processPrediction(nodeStateItems, variable, rootUrlLSTM, debugLevel, nodeName);
			// VariableState initState = VariableState.get();
			logger.info("-- Test ended --");
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			logger.error(e);
		}
	}

	private static List<PredictionResult> processPrediction(List<SingleNodeStateItem> nodeStateItems, String variable,
			String rootUrlLSTM, int debugLevel, String nodeName) {
		String urlPrediction = rootUrlLSTM + "prediction1";
		int nbOfItems = 3;
		int depthX = 10;
		int depthY = 10;
		double maxTotalPower = 30000;
		NodeStates.initialize(maxTotalPower);
		Integer[] horizons = { 5, 10, 30, 60 };
		List<PredictionResult> listPredictionResult = new ArrayList<PredictionResult>();

		// int len = nodeStateItems.size();
		// List<SingleNodeStateItem> lastNodeStateItems =
		// nodeStateItems.subList(len-nbOfItems-depth+1, len);
		SingleNodeStateItem lastItem = nodeStateItems.get(nodeStateItems.size() - 1);
		Date initDate = lastItem.getDate();
		try {
			LSTMPredictionRequest request = SingleNodeStateItem.preparePredictParameters1(nodeStateItems, initDate, depthX,
					depthY, nbOfItems, nodeName, PredictionScope.NODE, variable, horizons, logger);
			Double initValue = request.getInitialValue();
			StringBuffer sParams = UtilJsonParser.toJsonStr(request, SapereLogger.getInstance(), 0);
			logger.info("send POST request to LSTM service " + urlPrediction + " params=" + sParams.toString());
			String postResponse = UtilHttp.sendPostRequest(urlPrediction, request, logger, debugLevel);
			// System.out.println("predictedValues = " + postResponse);
			JSONObject jsonPredictionResult = new JSONObject(postResponse);
			// logger.info("jsonPredictionResult = " + jsonPredictionResult);
			LSTMPredictionResult lstmPredictionResult = UtilJsonParser.parseLSTMPredictionResult(jsonPredictionResult,
					logger);
			logger.info("predictionResult = " + lstmPredictionResult);
			// int idxLast1 = request.getListDates().size() -1;
			// List<Double> initValues = request.getListX().get(0);
			// int idxLastDate = lstmPredictionResult.getListDates().size() -1;
			List<Date> targetDates = request.getTargetDates();
			// Date targetDate = lstmPredictionResult.getListDates().get(idxLastDate);
			List<Double> targetValues = lstmPredictionResult.getListPredicted().get(nbOfItems - 1);
			List<Date> listPredictionDates = lstmPredictionResult.getPredictionDates();
			int idxDate = 0;
			for (Date nextPredictioDate : listPredictionDates) {
				if (targetDates.contains(nextPredictioDate)) {
					Double targetValue = targetValues.get(idxDate);
					VariableState initState = NodeStates.getVariablState(initValue);
					VariableState targetState = NodeStates.getVariablState(targetValue);
					int targetStateIdx = targetState.getIndex();
					FeaturesKey currentFeaturesKey = new FeaturesKey();
					PredictionResult predictionResult = new PredictionResult(initDate, initState, nextPredictioDate,
							variable, currentFeaturesKey, debugLevel);
					List<Double> listStateProba = new ArrayList<Double>();
					for (int stateIdx = 0; stateIdx < NodeStates.getStatesList().size(); stateIdx++) {
						double nextProba = (stateIdx == targetStateIdx) ? 1 : 0;
						listStateProba.add(nextProba);
					}
					predictionResult.setStateProbabilities(listStateProba);
					listPredictionResult.add(predictionResult);
				}
				idxDate++;
			}
		} catch (HandlingException e) {
			logger.error(e);
		} catch (JSONException e) {
			logger.error(e);
		}
		return listPredictionResult;
	}
}
