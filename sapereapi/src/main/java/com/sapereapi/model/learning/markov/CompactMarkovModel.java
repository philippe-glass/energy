package com.sapereapi.model.learning.markov;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.model.learning.AbstractCompactedModel;
import com.sapereapi.model.learning.FeaturesKey;
import com.sapereapi.model.learning.VariableFeaturesKey;
import com.sapereapi.model.learning.prediction.PredictionStep;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;
import com.sapereapi.util.matrix.IterationMatrix;
//import com.sapereapi.util.matrix.IterationMatrix2;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.values.IAggregateable;
import eu.sapere.middleware.node.NodeLocation;

public class CompactMarkovModel extends AbstractCompactedModel implements IAggregateable {
	private static final long serialVersionUID = 1L;
	// first variable and then featuresKey code
	private Map<Integer, Date> mapIterationDates = null;
	private List<Integer> iterations = null;

	public Map<Integer, Date> getMapIterationDates() {
		return mapIterationDates;
	}

	public void setMapIterationDates(Map<Integer, Date> mapIterationDates) {
		this.mapIterationDates = mapIterationDates;
	}

	public List<Integer> getIterations() {
		return iterations;
	}

	public void setIterations(List<Integer> iterations) {
		this.iterations = iterations;
	}

	public String getIterationMatrix(String variable, PredictionStep predictionStep) {
		return getIterationMatrix2(variable, predictionStep.getFeaturesKey().getCode());
	}

	public boolean canAggregate(IAggregateable other) {
		return other instanceof CompactMarkovModel;
	}


	@Override
	public CompactMarkovModel aggregate(String operator, Map<String, IAggregateable> mapObjects,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		return aggregate2(operator, mapObjects, agentAuthentication, logger);
	}

	public static CompactMarkovModel aggregate2(String operator, Map<String, IAggregateable> mapObjects,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		try {
			//logger.info("CompactMarkovModel.aggregate2 : begin");
			long timeBegin = new Date().getTime();
			CompactMarkovModel result = null;
			boolean activateAggregation = true;
			if(activateAggregation) {
				String localAgentName = agentAuthentication.getAgentName();
				// Retrieve all markov models and convert them into "complete" markov model and put them in a map.
				Map<String, CompleteMarkovModel> mapCompleteMarkovModel = new HashMap<String, CompleteMarkovModel>();
				Map<String, Double> weightsTable = new HashMap<String, Double>();
				for (String agentName : mapObjects.keySet()) {
					boolean isLocalNode = localAgentName.equals(agentName);
					weightsTable.put(agentName, isLocalNode ? 10.0 : 1.0);
					IAggregateable nextObj = mapObjects.get(agentName);
					if(nextObj instanceof CompactMarkovModel) {
						// retrieve next compact model
						CompactMarkovModel nextCompactMarkovModel = (CompactMarkovModel) nextObj;
						if(nextCompactMarkovModel.getIterations().size()==0) {
							logger.info("CompactMarkovModel.aggregate2 received empty model from node " + agentName);
							return null;
						}
						// convert the compact model into a complete model
						CompleteMarkovModel nextMarkovModel = nextCompactMarkovModel.generateCompleteModel(logger);
						// Check if the received model is complete
						if(!nextMarkovModel.isComplete()) {
							logger.info("CompactMarkovModel.aggregate2 received incomplete model from node " + agentName + " : " + nextMarkovModel);
							return null;
						}
						// Put the next model in the result map
						mapCompleteMarkovModel.put(agentName, nextMarkovModel);
					}
				}
				// Call the complete models aggregator with the new constructed map.
				CompleteMarkovModel aggregatedModel = CompleteMarkovModel.aggregate2(operator, mapCompleteMarkovModel, agentAuthentication, logger);
				if(aggregatedModel == null) {
					return null;
				}
				result = aggregatedModel.generateCompactedModel(logger);
			}
			long timeEnd = new Date().getTime();
			long timeSpent = timeEnd - timeBegin;
			logger.info("CompactMarkovModel.aggregate2 time Spent (MS) " + timeSpent);
			return result;
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}


	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		if(this.isAggregated()) {
			result.append(super.toString());
			result.append(SapereUtil.CR);
		}
		result.append("CompactMarkovModel: totalKeyNb = ").append(getTotalKeyNb());
		result.append(SapereUtil.CR).append("ierations:{");
		String sepItDate = "";
		for(Integer iteration : mapIterationDates.keySet()) {
			Date itDate = mapIterationDates.get(iteration);
			result.append(sepItDate).append(iteration).append(" : ").append(UtilDates.format_sql_day.format(itDate));
			sepItDate = ", ";
		}
		result.append("}");
		result.append(SapereUtil.CR).append("matrices:");
		//int idx = 0;
		for (String variable : zippedContent.keySet()) {
			if (false || variable.equals("produced")) {
				result.append(SapereUtil.CR);
				result.append("{").append(variable).append("}:");
				Map<String, String> varContent = zippedContent.get(variable);
				for (String nextKey : varContent.keySet()) {
					if("0".equals(nextKey)) {
						String itMatrix = varContent.get(nextKey);
						IterationMatrix itMatrix2 = IterationMatrix.unzip(itMatrix);
						TransitionMatrix  transitionMatrix = new TransitionMatrix();
						transitionMatrix.setCompleteObsMatrix(itMatrix2);
						transitionMatrix.refreshAllMatrices();
						result.append(variable).append(":").append(SapereUtil.CR).append(transitionMatrix.getNormalizedMatrix1());
					}
				}
			}
		}
		return result.toString();
	}


	public CompactMarkovModel copyForLSA(AbstractLogger logger) {
		//CompactModel compactModelCopy = super.copyForLSA(logger);
		CompactMarkovModel result = new CompactMarkovModel();
		super.auxCopyForLSA(result, logger);
		//compactModelCopy.fillCompactModel(result);
		result.setIterations(SapereUtil.cloneListInteger(iterations));
		result.setMapIterationDates(SapereUtil.cloneMapIntegerDate(mapIterationDates));
		return result;
	}

	@Override
	public CompleteMarkovModel generateCompleteModel(AbstractLogger logger) {
		//return generateCompleteMarkovModel(false);
		CompleteMarkovModel result = new CompleteMarkovModel();
		super.fillLearninModel(result);
		for (String variable : zippedContent.keySet()) {
			VariableMarkovModel varModel = new VariableMarkovModel();
			varModel.setVariable(variable);
			Map<String, String> varContent = zippedContent.get(variable);
			for (String nextKeyStr : varContent.keySet()) {
				String sNextItMatrix = varContent.get(nextKeyStr);
				FeaturesKey nextFeaturesKey = predictionContext.parseFeaturesKey(nextKeyStr);
				IterationMatrix nextItMatrix = IterationMatrix.unzip(sNextItMatrix);
				TransitionMatrix transitionMatrix = new TransitionMatrix();
				transitionMatrix.setCompleteObsMatrix(nextItMatrix);
				VariableFeaturesKey trKey = new VariableFeaturesKey(null, predictionContext.getId(), variable,
						nextFeaturesKey);
				transitionMatrix.setKey(trKey);
				varModel.addTransitionMatrix(transitionMatrix, logger);
			}
			result.putVariableMarkovModel(variable, varModel);
		}
		return result;
	}


	@Override
	public List<NodeLocation> retrieveInvolvedLocations() {
		return super.retrieveInvolvedLocations();
	}
}
