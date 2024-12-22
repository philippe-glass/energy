package com.sapereapi.model.learning.lstm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.learning.FeaturesKey;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.learning.VariableState;
import com.sapereapi.model.learning.lstm.request.LSTMPredictionRequest;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.model.learning.prediction.PredictionData;
import com.sapereapi.model.learning.prediction.PredictionResult;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;
import com.sapereapi.util.UtilHttp;
import com.sapereapi.util.UtilJsonParser;
import com.sapereapi.util.matrix.DoubleMatrix;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;

public class VariableLSTMModel implements Serializable {
	private static final long serialVersionUID = 1L;
	private List<INNLayer> layers = new ArrayList<INNLayer>();
	private Integer samplingNb;

	public List<INNLayer> getLayers() {
		return layers;
	}

	public void setLayers(List<INNLayer> layers) {
		this.layers = layers;
	}

	/*
	public VariableLSTMModel(String path, int numLSTMLayers) throws HandlingException {
		layers.clear();
		for (int i = 0; i < numLSTMLayers; i++) {
			boolean returnSequence = i == 0;
			LSTMLayer lstmLayer = new LSTMLayer(path, i, returnSequence);
			lstmLayer.setRecurrentActivation(ActivationFunction.FUNCTION_HARD_SIGMOID_0);
			layers.add(lstmLayer);
		}
		layers.add(new DenseLayer(path));
	}
	*/

	public void setRecurrentActivation(String recurrentActivation) {
		for (INNLayer layer : layers) {
			if (layer instanceof LSTMLayer) {
				LSTMLayer lstmLayer = (LSTMLayer) layer;
				lstmLayer.setRecurrentActivation(recurrentActivation);
			}
		}
	}

	public Integer getSamplingNb() {
		return samplingNb;
	}

	public void setSamplingNb(Integer samplingNb) {
		this.samplingNb = samplingNb;
	}

	public VariableLSTMModel(String modelDirectory, List<LayerDefinition> listLayerDefinition)
			throws HandlingException {
		boolean returnSequence = true;
		int layerIndex = 0;
		layers.clear();
		for (LayerDefinition nextLayerDefinition : listLayerDefinition) {
			if ("Dense".equals(nextLayerDefinition.getLayerType())) {
				System.out.print("SimpleLSTMPropagator : for  debug");
			}
			Map<ParamType, String> layerFilePaths = nextLayerDefinition.getMapFilepath(modelDirectory);
			if ("LSTM".equals(nextLayerDefinition.getLayerType())) {
				if (!layerFilePaths.containsKey(ParamType.w)) {
					throw new IllegalArgumentException(
							"LSTMLayer " + layerIndex + " : layerFiles doew not contain the path for w param");
				}
				if (!layerFilePaths.containsKey(ParamType.u)) {
					throw new IllegalArgumentException("LSTMLayer : layerFiles doew not contain the path for u param");
				}
				if (!layerFilePaths.containsKey(ParamType.b)) {
					throw new IllegalArgumentException("LSTMLayer : layerFiles doew not contain the path for b param");
				}
				Map<ParamType, DoubleMatrix> layerMatrices = new HashMap<ParamType, DoubleMatrix>();
				// load matrix of W params
				String pathWparams = layerFilePaths.get(ParamType.w);
				DoubleMatrix allW = DoubleMatrix.loadMatrixFromFile(pathWparams);
				layerMatrices.put(ParamType.w, allW);
				// Load matrix of U params
				String pathUparams = layerFilePaths.get(ParamType.u);
				DoubleMatrix allU = DoubleMatrix.loadMatrixFromFile(pathUparams);
				layerMatrices.put(ParamType.u, allU);
				// Load matrix of b params
				String pathBparams = layerFilePaths.get(ParamType.b);
				DoubleMatrix allB = DoubleMatrix.loadMatrixFromFile(pathBparams);
				layerMatrices.put(ParamType.b, allB);
				//LSTMLayer lstmLayer = new LSTMLayer(nextLayerDefinition, layerIndex, returnSequence, layerFilePaths);
				LSTMLayer lstmLayer = new LSTMLayer(nextLayerDefinition, layerIndex, returnSequence, layerMatrices);
				layers.add(lstmLayer);
				returnSequence = false;
			} else if ("Dropout".equals(nextLayerDefinition.getLayerType())) {
				layers.add(new DropoutLayer(nextLayerDefinition, 0.2));
			} else if ("Dense".equals(nextLayerDefinition.getLayerType())) {
				String pathW = layerFilePaths.get(ParamType.w);
				Map<ParamType, DoubleMatrix> layerMatrices = new HashMap<ParamType, DoubleMatrix>();
				layerMatrices.put(ParamType.w, DoubleMatrix.loadMatrixFromFile(pathW));
				String pathB = layerFilePaths.get(ParamType.b);
				layerMatrices.put(ParamType.b, DoubleMatrix.loadMatrixFromFile(pathB));
				layers.add(new DenseLayer(nextLayerDefinition, layerMatrices));
			}
			layerIndex++;
		}
	}

	public VariableLSTMModel(LSTMModelInfo modelInfo) throws HandlingException {
		Map<String, DoubleMatrix> mapMatrices = modelInfo.getMapMatrices();
		boolean returnSequence = true;
		layers.clear();
		this.samplingNb = modelInfo.getSamplingNb();
		for (LayerDefinition nextLayerDefinition : modelInfo.getLayers()) {
			if ("Dense".equals(nextLayerDefinition.getLayerType())) {
				System.out.print("SimpleLSTMPropagator : for  debug");
			}
			Map<ParamType, DoubleMatrix> layerMatrices = nextLayerDefinition.extractMatrices(mapMatrices);
			if ("LSTM".equals(nextLayerDefinition.getLayerType())) {
				int layerIndex = nextLayerDefinition.getLayerIndex();
				layers.add(new LSTMLayer(nextLayerDefinition, layerIndex, returnSequence, layerMatrices));
				returnSequence = false;
			} else if ("Dropout".equals(nextLayerDefinition.getLayerType())) {
				layers.add(new DropoutLayer(nextLayerDefinition, 0.2));
			} else if ("Dense".equals(nextLayerDefinition.getLayerType())) {
				layers.add(new DenseLayer(nextLayerDefinition, layerMatrices));
			}
		}
	}

	public DoubleMatrix forward_propagate_full(DoubleMatrix X) throws HandlingException {
		int realSize = X.getRowDimension();
		DoubleMatrix intermediateResult = X;
		for (INNLayer layer : layers) {
			layer.setRealSize(realSize);
			intermediateResult = layer.forwardStep(intermediateResult);
		}
		return intermediateResult;
	}

	public PredictionData computeVariablePrediction(LSTMPredictionRequest request, PredictionContext predictionContext,
			PredictionData prediction
			, String variable, AbstractLogger logger) throws HandlingException {
		int debugLevel = 0;
		int nbOfItems = 3;
		double stateZeroMargin = NodeStates.getFirstNonNullBondary()*0.01;
		String urlPrediction = predictionContext.getNodeContext().getUrlForcasting() + "prediction1";
		StringBuffer sParams = UtilJsonParser.toJsonStr(request, SapereLogger.getInstance(), 0);
		logger.info("send POST request to LSTM service " + urlPrediction + " params=" + sParams.toString());
		String postResponse = UtilHttp.sendPostRequest(urlPrediction, request, logger, debugLevel);
		JSONObject jsonPredictionResult = new JSONObject(postResponse);
		// logger.info("jsonPredictionResult = " + jsonPredictionResult);
		LSTMPredictionResult lstmPredictionResult = UtilJsonParser.parseLSTMPredictionResult(jsonPredictionResult,
				logger);
		Double initValue = request.getInitialValue();
		Date initDate = request.getInitialDate();
		List<Date> targetDates = request.getTargetDates();
		List<Double> targetValues = lstmPredictionResult.getListPredicted().get(nbOfItems - 1);
		List<Date> listPredictionDates = lstmPredictionResult.getPredictionDates();
		int idxDate = 0;
		if("missing".equals(variable) || "_requested".equals(variable)) {
			logger.info("VariableLSTMModel.computeVariablePrediction : targetValues of " + variable + " = " + targetValues);
		}
		for(Date targetdate : targetDates) {
			logger.info("computeVariablePrediction : next targetdate = " + UtilDates.format_sql.format(targetdate) + " - long=" + targetdate.getTime());
		}
		for (Date nextPredictioDate : listPredictionDates) {
			//logger.info("computeVariablePrediction : nextPredictioDate = " + UtilDates.format_sql.format(nextPredictioDate) + " - long=" + nextPredictioDate.getTime());			
			if (targetDates.contains(nextPredictioDate)) {
				Double targetValue = targetValues.get(idxDate);
				VariableState initState = NodeStates.getVariablState(initValue);
				if(targetValue < stateZeroMargin) {
					targetValue = 0.0;
				}
				VariableState targetState = NodeStates.getVariablState(targetValue);
				int targetStateIdx = targetState.getIndex();
				FeaturesKey currentFeaturesKey = predictionContext.getFeaturesKey2(nextPredictioDate);
				PredictionResult predictionResult = new PredictionResult(initDate, initState, nextPredictioDate,
						variable, currentFeaturesKey, debugLevel);
				List<Double> listStateProba = new ArrayList<Double>();
				for (int stateIdx = 0; stateIdx < NodeStates.getStatesList().size(); stateIdx++) {
					double nextProba = (stateIdx == targetStateIdx) ? 1 : 0;
					listStateProba.add(nextProba);
				}
				predictionResult.setStateProbabilities(listStateProba);
				predictionResult.setLikelyTargetValue(targetValue);
				// listPredictionResult.add(predictionResult);
				prediction.setResult(variable, nextPredictioDate, predictionResult);
			}
			idxDate++;
		}
		return prediction;
	}

	public static VariableLSTMModel auxAggregate(Map<String, VariableLSTMModel> mapModels,
			Map<String, Double> weightsTable, AgentAuthentication agentAuthentication, AbstractLogger logger)
			throws HandlingException {
		VariableLSTMModel firstModel = null;
		LSTMModelInfo modelInfo = new LSTMModelInfo();
		VariableLSTMModel result = new VariableLSTMModel(modelInfo);
		String localAgent = agentAuthentication.getAgentName();
		if(mapModels.containsKey(localAgent)) {
			firstModel = mapModels.get(localAgent);
		} else {
			for (String nextAgentName : mapModels.keySet()) {
				VariableLSTMModel nextObj = mapModels.get(nextAgentName);
				if (firstModel == null) {
					firstModel = nextObj;
				}
			}
		}
		result.setSamplingNb(firstModel.getSamplingNb());
		// result.setComputeDate(new Date());
		// result.setFeaturesKey(firstModel.getFeaturesKey());
		List<INNLayer> resultLayser = new ArrayList<INNLayer>();
		for (int layerIdx = 0; layerIdx < firstModel.getLayers().size(); layerIdx++) {
			INNLayer firstLayer = firstModel.getLayers().get(layerIdx);
			Map<String, LSTMLayer> mapLSTMLayers = new HashMap<String, LSTMLayer>();
			Map<String, DenseLayer> mapDenseLayers = new HashMap<String, DenseLayer>();
			Map<String, DropoutLayer> mapDropoutLayers = new HashMap<String, DropoutLayer>();
			for (String agentName : mapModels.keySet()) {
				VariableLSTMModel nextModel = mapModels.get(agentName);
				INNLayer nextLayer = nextModel.getLayers().get(layerIdx);
				if (nextLayer instanceof LSTMLayer) {
					mapLSTMLayers.put(agentName, (LSTMLayer) nextLayer);
				}
				if (nextLayer instanceof DenseLayer) {
					mapDenseLayers.put(agentName, (DenseLayer) nextLayer);
				}
				if (nextLayer instanceof DropoutLayer) {
					mapDropoutLayers.put(agentName, (DropoutLayer) nextLayer);
				}
			}
			INNLayer aggregatedLayer = null;
			if (firstLayer instanceof LSTMLayer) {
				aggregatedLayer = LSTMLayer.auxAggregate(mapLSTMLayers, weightsTable, agentAuthentication, logger);
			} else if (firstLayer instanceof DenseLayer) {
				aggregatedLayer = DenseLayer.auxAggregate(mapDenseLayers, weightsTable, agentAuthentication, logger);
			} else if (firstLayer instanceof DropoutLayer) {
				Double rate = ((DropoutLayer) firstLayer).getRate();
				aggregatedLayer = new DropoutLayer(firstLayer.getLayerDefinition(), rate);
			}
			if (aggregatedLayer != null) {
				resultLayser.add(aggregatedLayer);
			}
		}
		result.setLayers(resultLayser);
		return result;
	}

	public VariableLSTMModel copy(AbstractLogger logger) {
		try {
			VariableLSTMModel result = new VariableLSTMModel(new LSTMModelInfo());
			result.setSamplingNb(samplingNb);
			List<INNLayer> copyLayers = new ArrayList<INNLayer>();
			for(INNLayer nextLayer : this.layers) {
				copyLayers.add(nextLayer.copy());
			}
			result.setLayers(copyLayers);
			return result;
		} catch (HandlingException e) {
			logger.error(e);
		}
		return null;
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("VariableLSTMModel:");
		for(INNLayer nextLayer : this.layers) {
			result.append(SapereUtil.CR).append(nextLayer);
		}
		return result.toString();
	}

	public INNLayer getLayer(String layerId) {
		for (INNLayer nextLayer : layers) {
			String nextLayerKey = nextLayer.getLayerDefinition().getKey2();
			if (layerId.equals(nextLayerKey)) {
				return nextLayer;
			}
		}
		return null;
	}

	public boolean copyFromOther(VariableLSTMModel other) throws HandlingException {
		boolean result = false;
		if(other != null) {
			for (INNLayer nextLayer : layers) {
				String layerKey = nextLayer.getLayerDefinition().getKey2();
				INNLayer otherLayer = other.getLayer(layerKey);
				if (otherLayer != null) {
					boolean nextHasChanged = nextLayer.copyFromOther(otherLayer);
					result = result || nextHasChanged;
				}
			}
		}
		return result;
	}

	public List<LayerDefinition> getCopyOfLayerDefinition() {
		List<LayerDefinition> result = new ArrayList<LayerDefinition>();
		for (INNLayer nextLayer : this.layers) {
			LayerDefinition layerDefinition = nextLayer.getLayerDefinition();
			result.add(layerDefinition.clone());
		}
		return result;
	}

	public Map<String, DoubleMatrix> getMapMatrices() {
		Map<String, DoubleMatrix> result = new HashMap<String, DoubleMatrix>();
		for (INNLayer nextLayer : this.layers) {
			LayerDefinition layerDefinition = nextLayer.getLayerDefinition();
			Map<ParamType, DoubleMatrix> layerMatrices = nextLayer.getMapMatrices();
			for (ParamType parmType : layerMatrices.keySet()) {
				String matrixKey = layerDefinition.generateMatrixKey(parmType);
				DoubleMatrix matrix = layerMatrices.get(parmType);
				result.put(matrixKey, matrix);

			}
		}
		return result;
	}

	public Map<String, String> getZippedContent() {
		Map<String, DoubleMatrix> modelMatrices = getMapMatrices();
		Map<String, String> result = new HashMap<String, String>();
		for (String nextKey : modelMatrices.keySet()) {
			DoubleMatrix nextatrix = modelMatrices.get(nextKey);
			String sTrMatrix = nextatrix.zip();
			result.put(nextKey, sTrMatrix);
		}
		return result;
	}
}
