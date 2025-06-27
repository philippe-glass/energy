package com.sapereapi.model.learning;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.sapereapi.model.HandlingException;
import com.sapereapi.model.energy.SingleNodeStateItem;
import com.sapereapi.model.energy.node.NodeTotal;
import com.sapereapi.model.learning.aggregation.AbstractAggregationResult;
import com.sapereapi.model.learning.prediction.MultiPredictionsData;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.model.learning.prediction.PredictionData;
import com.sapereapi.model.learning.prediction.PredictionStep;
import com.sapereapi.model.learning.prediction.input.AggregationCheckupRequest;
import com.sapereapi.model.learning.prediction.input.MassivePredictionRequest;
import com.sapereapi.model.learning.prediction.input.MatrixFilter;
import com.sapereapi.model.learning.prediction.input.PredictionRequest;

import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.values.IAggregateable;

public interface ILearningModel extends IAggregateable {

	public static final String OP_SAMPLING_NB = "sampling_nb";
	public static final String OP_POWER_LOSS = "power_loss";
	public static final String OP_MIN_LOSS = "min_loss";
	public static final String OP_DISTANCE_POWER_PROFILE = "dist_power_hist";

	public PredictionContext getPredictionContext();

	public void setPredictionContext(PredictionContext predictionContext);

	public FeaturesKey getCurrentFeaturesKey();

	public void setCurrentFeaturesKey(FeaturesKey currentFeaturesKey);

	public List<SingleNodeStateItem> getStateHistory();

	public void setStateHistory(List<SingleNodeStateItem> stateHistory);

	public void addStateHistory(SingleNodeStateItem item);

	public VariableState getCurrentVariableState(String variable);

	public Map<String, Map<String, Double>> getAggregationWeights();

	public void setAggregationWeights(Map<String, Map<String, Double>> aggregationWeights);

	public boolean hasModelAggregator();

	public boolean hasPredictionAggregator();

	public boolean hasModelOrPredictionAggregator();

	public List<PredictionStep> computePredictionSteps(Date initialDate, Date targetDate) throws HandlingException;

	public PredictionData computeVariablePrediction(NodeStatesTransitions aNodeStateTransitions,
			PredictionData prediction, String variable, AbstractLogger logger) throws HandlingException;

	public void cleanHistory(long maxHistoryDurationMS, AbstractLogger logger);

	public int getSamplingNb(String variable);

	public PredictionData computePrediction(Date initDate, List<Date> targetDates, String[] variables,
			boolean useCorrections, AbstractLogger logger) throws HandlingException;

	public void initialize(Date aDate, PredictionContext aPredictionContext, AbstractLogger logger);

	public ILearningModel cloneWithFilter(MatrixFilter matrixFilter, AbstractLogger logger);

	public Map<Object, Boolean> copyFromOther(ILearningModel other, AbstractLogger logger);

	public void initNodeHistory(List<NodeTotal> nodeHistory, boolean completeMatrices, AbstractLogger logger);

	public boolean isComplete();

	public boolean isReadyForAggregation();

	public boolean auxRefreshTransitions(NodeTotal nodeTotal, boolean enableObsUpdate, AbstractLogger logger);

	public boolean refreshModel(NodeTotal nodeTotal, boolean enableObsUpdate, AbstractLogger logger);

	public NodeStatesTransitions getNodeStatesTransitions();

	public SingleNodeStateItem getClosestStateItem(Date aDate, AbstractLogger logger);

	public List<String> getUsedVariables();

	public ICompactedModel generateCompactedModel(AbstractLogger logger);

	public AbstractAggregationResult checkupModelAggregation(AggregationCheckupRequest fedAvgCheckupRequest,
			AbstractLogger logger, Map<String, ILearningModel> receivedModels);

	public PredictionData computePrediction2(PredictionRequest predictionRequest, AbstractLogger logger)
			throws HandlingException;

	public MultiPredictionsData generateMassivePredictions(MassivePredictionRequest massivePredictionRequest,
			AbstractLogger logger) throws HandlingException;

	public void load(Date currentDate) throws HandlingException;;

	public void loadPartially(String[] _variables, List<FeaturesKey> listFeaturesKey, Date currentDate)
			throws HandlingException;

	public long save(boolean onlyCurrentMatrix, boolean saveAllIterations, AbstractLogger logger)
			throws HandlingException;
}
