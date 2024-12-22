package com.sapereapi.model.learning.lstm;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.learning.AbstractCompactedModel;
import com.sapereapi.model.learning.ICompactedModel;
import com.sapereapi.model.learning.ILearningModel;
import com.sapereapi.model.learning.LearningModelType;
import com.sapereapi.model.learning.prediction.PredictionStep;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.matrix.DoubleMatrix;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.values.IAggregateable;
import eu.sapere.middleware.node.NodeLocation;

public class CompactLSTMModel extends AbstractCompactedModel implements ICompactedModel {
	private static final long serialVersionUID = 1L;
	// first variable and then featuresKey code
	private Map<String, List<String>> mapLayerDefinitions = new HashMap<String, List<String>>();
	private Integer samplingNb;

	public Integer getSamplingNb() {
		return samplingNb;
	}

	public void setSamplingNb(Integer samplingNb) {
		this.samplingNb = samplingNb;
	}

	public Map<String, List<String>> getMapLayerDefinitions() {
		return mapLayerDefinitions;
	}

	public void setMapLayerDefinitions(Map<String, List<String>> mapLayerDefinitions) {
		this.mapLayerDefinitions = mapLayerDefinitions;
	}

	public void putListLayerDefinitions(String variable, List<String> layerDefinitions) {
		this.mapLayerDefinitions.put(variable, layerDefinitions);
	}

	public String getIterationMatrix(String variable, PredictionStep predictionStep) {
		return getIterationMatrix2(variable, predictionStep.getFeaturesKey().getCode());
	}

	public boolean canAggregate(IAggregateable other) {
		return other instanceof CompactLSTMModel;
	}

	@Override
	public CompactLSTMModel aggregate(String operator, Map<String, IAggregateable> mapObjects,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		return aggregate2(operator, mapObjects, agentAuthentication, logger);
	}

	public static CompactLSTMModel aggregate2(String operator, Map<String, IAggregateable> mapObjects,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		try {
			long timeBegin = new Date().getTime();
			CompactLSTMModel result = null;
			boolean activateAggregation = true;
			if (activateAggregation) {
				String localNode = agentAuthentication.getNodeLocation().getName();
				// Retrieve all learning models and convert them into "complete" LSTM model and
				// put them in a map.
				Map<String, CompleteLSTMModel> mapCompleteLstmModel = new HashMap<String, CompleteLSTMModel>();
				Map<String, Double> weightsTable = new HashMap<String, Double>();
				for (String nextNode : mapObjects.keySet()) {
					boolean isLocalNode = localNode.equals(nextNode);
					weightsTable.put(nextNode, isLocalNode ? 10.0 : 1.0);
					IAggregateable nextObj = mapObjects.get(nextNode);
					if (nextObj instanceof CompactLSTMModel) {
						// retrieve next compact model
						CompactLSTMModel nextCompactLstmModel = (CompactLSTMModel) nextObj;
						if (nextCompactLstmModel.getMapLayerDefinitions().size() == 0) {
							logger.info("CompactLSTMModel.aggregate2 received empty model from node " + nextNode);
							return null;
						}
						if (!nextCompactLstmModel.checkUp(logger)) {
							logger.error("CompactLSTMModel.aggregate2 received model is not consistent");
							return null;
						}
						// convert the compact model into a complete model
						CompleteLSTMModel nextLstmModel = nextCompactLstmModel.generateCompleteLSTMModel(logger);
						// Check if the received model is complete
						if (!nextLstmModel.isComplete()) {
							logger.info("CompactLSTMModel.aggregate2 received incomplete model from node " + nextNode
									+ " : " + nextLstmModel);
							return null;
						}
						// Put the next model in the result map
						mapCompleteLstmModel.put(nextNode, nextLstmModel);
					}
				}
				// Call the complete models aggregator with the new constructed map.
				CompleteLSTMModel aggregatedModel = CompleteLSTMModel.aggregate2(operator, mapCompleteLstmModel,
						agentAuthentication, logger);
				if (aggregatedModel == null) {
					return null;
				}
				result = aggregatedModel.generateCompactedModel(logger);
			}
			long timeEnd = new Date().getTime();
			long timeSpent = timeEnd - timeBegin;
			logger.info("CompactLSTMModel.aggregate2 time Spent (MS) " + timeSpent);
			return result;
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}

	public boolean hasCodeFeaturesKey(String variable, String codeFeaturesKey) {
		Map<String, String> varContent = getVariableSubContent(variable);
		return varContent.containsKey(codeFeaturesKey);
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("CompactLSTMModel: totalKeyNb = ").append(getTotalKeyNb());
		result.append(",").append("samplingNb:").append(samplingNb);
		String sepLayer = "";
		for (String variable : mapLayerDefinitions.keySet()) {
			result.append(SapereUtil.CR).append("layer definition of ").append(variable).append(":");
			List<String> listLayerDefinitions = mapLayerDefinitions.get(variable);
			for (String layerDefinition : listLayerDefinitions) {
				result.append(sepLayer).append(layerDefinition);
				sepLayer = ", ";
			}
		}
		result.append("}");
		result.append(SapereUtil.CR).append("matrices:");
		// int idx = 0;
		for (String variable : zippedContent.keySet()) {
			if (false || variable.equals("produced")) {
				result.append(SapereUtil.CR);
				result.append("{").append(variable).append("}:");
				Map<String, String> varContent = zippedContent.get(variable);
				for (String nextKey : varContent.keySet()) {
					String nextKeyLC = nextKey.toLowerCase();
					if (true || nextKeyLC.contains("lstm") && nextKeyLC.contains("#w")) {
						String sMatrix = varContent.get(nextKey);
						try {
							DoubleMatrix matrix = DoubleMatrix.unzip(sMatrix);
							result.append(", ").append(nextKeyLC).append(":").append("matrix ")
									.append(matrix.getRowDimension()).append("X").append(matrix.getColumnDimension());
						} catch (HandlingException e) {
							SapereLogger.getInstance().error(e);
						}
					}
				}
			}
		}
		result.append(SapereUtil.CR).append(super.toString());
		return result.toString();
	}

	public CompactLSTMModel copyForLSA(AbstractLogger logger) {
		// compactModelCopy = super.copyForLSA(logger);
		CompactLSTMModel result = new CompactLSTMModel();
		super.auxCopyForLSA(result, logger);
		// Copy sampling number
		result.setSamplingNb(samplingNb);
		// Copy of Map of layer definitions
		Map<String, List<String>> copyOfMapLayerDefinitions = new HashMap<String, List<String>>();
		for (String variable : mapLayerDefinitions.keySet()) {
			List<String> listZippedLayerDefinitions = mapLayerDefinitions.get(variable);
			List<String> copyOfLayerDefinitions = new ArrayList<String>();
			for (String layerDefinition : listZippedLayerDefinitions) {
				copyOfLayerDefinitions.add(layerDefinition);
			}
			copyOfMapLayerDefinitions.put(variable, copyOfLayerDefinitions);
		}
		result.setMapLayerDefinitions(copyOfMapLayerDefinitions);
		return result;
	}

	public boolean checkUp(AbstractLogger logger) {
		for (String variable : zippedContent.keySet()) {
			List<String> listZippedLayerDefinitions = mapLayerDefinitions.get(variable);
			for (String nextZippedLayerDefinition : listZippedLayerDefinitions) {
				Map<String, String> varContent = zippedContent.get(variable);
				LayerDefinition nextLayerDefinition = LayerDefinition.unzip(nextZippedLayerDefinition, logger);
				for (ParamType parmType : nextLayerDefinition.getParamTypes()) {
					String matrixKey = nextLayerDefinition.generateMatrixKey(parmType);
					if (!varContent.containsKey(matrixKey)) {
						logger.error("CompactSTMModel.checkUp " + variable + " matrix not found for key " + matrixKey);
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public ILearningModel generateCompleteModel(AbstractLogger logger) {
		return generateCompleteLSTMModel(logger);
	}

	public CompleteLSTMModel generateCompleteLSTMModel(AbstractLogger logger) {
		CompleteLSTMModel result = new CompleteLSTMModel(this.predictionContext);
		super.fillLearninModel(result);
		for (String variable : zippedContent.keySet()) {
			Map<String, String> varContent = zippedContent.get(variable);
			LSTMModelInfo varModelnfo = new LSTMModelInfo();
			varModelnfo.setVariable(variable);
			varModelnfo.setModelName(LearningModelType.LSTM.toString());
			List<LayerDefinition> listlayerDefnitions = new ArrayList<LayerDefinition>();
			Map<String, DoubleMatrix> mapMatrices = new HashMap<String, DoubleMatrix>();
			Map<String, List<Integer>> mapShapes = new HashMap<String, List<Integer>>();
			List<String> listZippedLayerDefinitions = mapLayerDefinitions.get(variable);
			for (String nextZiipedLayerDefinition : listZippedLayerDefinitions) {
				LayerDefinition nextLayerDefinition = LayerDefinition.unzip(nextZiipedLayerDefinition, logger);
				listlayerDefnitions.add(nextLayerDefinition);
				for (ParamType parmType : nextLayerDefinition.getParamTypes()) {
					String matrixKey = nextLayerDefinition.generateMatrixKey(parmType);
					if (varContent.containsKey(matrixKey)) {
						String sMatrix = varContent.get(matrixKey);
						try {
							DoubleMatrix matrix = DoubleMatrix.unzip(sMatrix);
							mapMatrices.put(matrixKey, matrix);
							mapShapes.put(matrixKey, matrix.getDimensions());
						} catch (HandlingException e) {
							logger.error(e);
						}
					} else {
						logger.error(
								"generateCompleteLSTMModel " + variable + " matrix not found for key " + matrixKey);
					}
				}
			}
			if (mapMatrices.size() == 0) {
				logger.error("### generateCompleteLSTMModel : varModel has no matrix : varContent keys = "
						+ varContent.keySet());
			}
			varModelnfo.setLayers(listlayerDefnitions);
			varModelnfo.setMapShapes(mapShapes);
			varModelnfo.setMapMatrices(mapMatrices);
			varModelnfo.setSamplingNb(samplingNb);
			try {
				VariableLSTMModel varLstmModel = new VariableLSTMModel(varModelnfo);
				result.putModel(variable, varLstmModel);
			} catch (HandlingException e) {
				logger.error(e);
			}
		}
		return result;
	}

	@Override
	public List<NodeLocation> retrieveInvolvedLocations() {
		return super.retrieveInvolvedLocations();
	}
}
