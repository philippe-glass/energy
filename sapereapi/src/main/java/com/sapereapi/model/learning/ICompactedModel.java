package com.sapereapi.model.learning;

import java.util.List;
import java.util.Map;

import com.sapereapi.model.learning.prediction.PredictionContext;

import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.values.IAggregateable;

public interface ICompactedModel  extends IAggregateable {
	public int getTotalKeyNb();
	public List<String> getKeys();
	public ILearningModel generateCompleteModel(AbstractLogger logger);
	public PredictionContext getPredictionContext();
	public Map<String, Map<String, Double>> getAggregationWeights();
	public Map<String, ILearningModel> retrieveSourceModels(boolean refreshAllMatrices, AbstractLogger logger);
}
