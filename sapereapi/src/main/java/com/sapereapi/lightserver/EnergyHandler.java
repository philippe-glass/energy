package com.sapereapi.lightserver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.OperationResult;
import com.sapereapi.model.OptionItem;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.ServerConfig;
import com.sapereapi.model.Service;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.model.energy.forcasting.EndUserForcastingResult;
import com.sapereapi.model.energy.forcasting.ForcastingResult1;
import com.sapereapi.model.energy.forcasting.ForcastingResult2;
import com.sapereapi.model.energy.forcasting.input.EndUserForcastingRef;
import com.sapereapi.model.energy.forcasting.input.EndUserForcastingRequest;
import com.sapereapi.model.energy.input.AgentFilter;
import com.sapereapi.model.energy.input.AgentInputForm;
import com.sapereapi.model.energy.input.NodeHistoryFilter;
import com.sapereapi.model.energy.input.OfferFilter;
import com.sapereapi.model.energy.node.ExtendedNodeTotal;
import com.sapereapi.model.energy.node.MultiNodesContent;
import com.sapereapi.model.energy.node.NodeContent;
import com.sapereapi.model.input.HistoryInitializationRequest;
import com.sapereapi.model.input.InitializationForm;
import com.sapereapi.model.learning.ILearningModel;
import com.sapereapi.model.learning.VariableStateHistory;
import com.sapereapi.model.learning.aggregation.AbstractAggregationResult;
import com.sapereapi.model.learning.prediction.MultiPredictionsData;
import com.sapereapi.model.learning.prediction.PredictionContext;
import com.sapereapi.model.learning.prediction.PredictionData;
import com.sapereapi.model.learning.prediction.PredictionStatistic;
import com.sapereapi.model.learning.prediction.input.AggregationCheckupRequest;
import com.sapereapi.model.learning.prediction.input.MassivePredictionRequest;
import com.sapereapi.model.learning.prediction.input.MatrixFilter;
import com.sapereapi.model.learning.prediction.input.PredictionRequest;
import com.sapereapi.model.learning.prediction.input.PredictionScopeFilter;
import com.sapereapi.model.learning.prediction.input.StateHistoryRequest;
import com.sapereapi.model.learning.prediction.input.StatisticsRequest;
import com.sapereapi.util.UtilHttp;

import eu.sapere.middleware.node.NodeLocation;

public class EnergyHandler extends AbstractHandler {

	public EnergyHandler(String uri, ServerConfig _serverConfig) {
		super();
		this.uri = uri;
		this.serverConfig = _serverConfig;
		this.handlerTable = new HashMap<>();
		initHandlerTable();
		logger.info("end init EnergyHandler");
	}

	/**
	 *
	 * @return
	 */
	@Route(value = "/retrieveNodeContent")
	public NodeContent retrieveNodeContent() {
		AgentFilter filter = new AgentFilter();
		UtilHttp.fillObject(filter, httpMethod, httpInput, logger);
		return Sapere.getInstance().retrieveNodeContent(filter);
	}

	public MultiNodesContent retrieveAllNodesContent() throws HandlingException {
		AgentFilter filter = new AgentFilter();
		return Sapere.getInstance().retrieveAllNodesContent(filter);
	}

	public InitializationForm initEnergyService() throws HandlingException {
		InitializationForm initForm = new InitializationForm();
		UtilHttp.fillObject(initForm, httpMethod, httpInput, logger);
		return Sapere.getInstance().initEnergyService(serverConfig, initForm);
	}

	public ILearningModel initNodeHistory2() throws HandlingException {
		HistoryInitializationRequest historyInitRequest = new HistoryInitializationRequest();
		UtilHttp.fillObject(historyInitRequest, httpMethod, httpInput, logger);
		return Sapere.getInstance().initNodeHistory(historyInitRequest);
	}

	public AgentForm addAgent() throws HandlingException {
		AgentInputForm agentInputForm = new AgentInputForm();
		UtilHttp.fillObject(agentInputForm, httpMethod, httpInput, logger);
		if(debugLevel >= 0) {
			logger.info("addAgent httpInput = " + httpInput);
			logger.info("addAgent agentInputForm : power = " + agentInputForm.getPower() + ", begindate = " + agentInputForm.getBeginDate());
		}
		return Sapere.getInstance().addEnergyAgent(agentInputForm);
	}

	public AgentForm stopAgent() {
		AgentInputForm agentInputForm = new AgentInputForm();
		UtilHttp.fillObject(agentInputForm, httpMethod, httpInput, logger);
		return Sapere.getInstance().stopAgent(agentInputForm);
	}

	public OperationResult stopListAgents() {
		AgentInputForm agentInputForm = new AgentInputForm();
		UtilHttp.fillObject(agentInputForm, httpMethod, httpInput, logger);
		return Sapere.getInstance().stopListAgents(agentInputForm);
	}

	public AgentForm modifyAgent() throws HandlingException {
		AgentInputForm agentInputForm = new AgentInputForm();
		UtilHttp.fillObject(agentInputForm, httpMethod, httpInput, logger);
		return Sapere.getInstance().modifyEnergyAgent(agentInputForm);
	}

	public AgentForm restartAgent() {
		AgentInputForm agentInputForm = new AgentInputForm();
		UtilHttp.fillObject(agentInputForm, httpMethod, httpInput, logger);
		return Sapere.getInstance().restartEnergyAgent(agentInputForm);
	}

	public Iterable<Service> test1() throws HandlingException {
		return Sapere.getInstance().test1(serverConfig);
	}

	public Iterable<Service> test2() throws HandlingException {
		return Sapere.getInstance().test2(serverConfig);
	}

	public Iterable<Service> test3() throws HandlingException {
		return Sapere.getInstance().test3(serverConfig);
	}

	public Iterable<Service> test4() throws HandlingException {
		return Sapere.getInstance().test4(serverConfig);
	}

	public Iterable<Service> test5() throws HandlingException {
		return Sapere.getInstance().test5(serverConfig);
	}

	public Iterable<Service> test6() throws HandlingException {
		return Sapere.getInstance().test6(serverConfig);
	}

	public Iterable<Service> testTragedyOfTheCommons() throws HandlingException {
		return Sapere.getInstance().testTragedyOfTheCommons(serverConfig, true);
	}

	public NodeContent restartLastNodeContent() throws HandlingException {
		return Sapere.getInstance().restartLastNodeContent();
	}

	@Route(value = "/nodeTotalHistory")
	public List<ExtendedNodeTotal> retrieveNodeTotalHistory() throws HandlingException {
		NodeHistoryFilter historyFilter = new NodeHistoryFilter();
		UtilHttp.fillObject(historyFilter, httpMethod, httpInput, logger);
		return EnergyDbHelper.retrieveNodeTotalHistory(historyFilter);
	}

	public PredictionContext getPredictionContext() {
		PredictionScopeFilter scopeFilter = new PredictionScopeFilter();
		UtilHttp.fillObject(scopeFilter, httpMethod, httpInput, logger);
		return Sapere.getInstance().getPredictionContext(scopeFilter);
	}

	public NodeLocation getNodeLocation() {
		return Sapere.getInstance().getNodeLocation();
	}

	public Map<String, NodeLocation> getMapAllNodeLocations() {
		return Sapere.getInstance().getMapAllNodeLocations(true);
	}

	public List<OptionItem> getStateDates() throws HandlingException {
		PredictionScopeFilter scopeFilter = new PredictionScopeFilter();
		UtilHttp.fillObject(scopeFilter, httpMethod, httpInput, logger);
		return Sapere.getInstance().getStateDates(scopeFilter);
	}

	@Route(value = "/allNodeTransitionMatrices")
	public ILearningModel getAllNodeTransitionMatrices() {
		MatrixFilter matrixFilter = new MatrixFilter();
		UtilHttp.fillObject(matrixFilter, httpMethod, httpInput, logger);
		return Sapere.getInstance().getLearningModel(matrixFilter);
	}

	public OperationResult enableSupervision() {
		return Sapere.enableSupervision();
	}

	public OperationResult stopAllAgents1() {
		return Sapere.getInstance().stopAllAgents();
	}

	public void stopEnergyService() {
		Sapere.getInstance().stopEnergyService();
	}

	List<VariableStateHistory> retrieveLastHistoryStates() throws HandlingException {
		StateHistoryRequest stateHistoryRequest = new StateHistoryRequest();
		UtilHttp.fillObject(stateHistoryRequest, httpMethod, httpInput, logger);
		return Sapere.getInstance().retrieveLastHistoryStates(stateHistoryRequest);
	}

	AbstractAggregationResult checkupModelAggregation() {
		AggregationCheckupRequest fedAvgCheckupRequest = new AggregationCheckupRequest();
		UtilHttp.fillObject(fedAvgCheckupRequest, httpMethod, httpInput, logger);
		return Sapere.getInstance().checkupModelAggregation(fedAvgCheckupRequest);
	}

	public PredictionData getPrediction() throws HandlingException {
		PredictionRequest predictionRequest = new PredictionRequest();
		UtilHttp.fillObject(predictionRequest, httpMethod, httpInput, logger);
		return Sapere.getInstance().getPrediction(predictionRequest);
	}

	public MultiPredictionsData getMassivePredictions() throws HandlingException  {
		MassivePredictionRequest massivePredictionRequest = new MassivePredictionRequest();
		UtilHttp.fillObject(massivePredictionRequest, httpMethod, httpInput, logger);
		return Sapere.getInstance().generateMassivePredictions(massivePredictionRequest);
	}

	public List<PredictionStatistic> computePredictionStatistics() throws HandlingException {
		StatisticsRequest statisticsRequest = new StatisticsRequest();
		UtilHttp.fillObject(statisticsRequest, httpMethod, httpInput, logger);
		return Sapere.getInstance().computePredictionStatistics(statisticsRequest);
	}

	public List<SingleOffer> retrieveOffers() throws HandlingException {
		OfferFilter offerFilter = new OfferFilter();
		UtilHttp.fillObject(offerFilter, httpMethod, httpInput, logger);
		return EnergyDbHelper.retrieveOffers(offerFilter);
	}

	public ForcastingResult1 predict1() {
		Map<String, Double> forcastingRequest = new HashMap<String, Double>();
		UtilHttp.fillObject(forcastingRequest, httpMethod, httpInput, logger);
		return Sapere.getInstance().generateMockForcasting1(forcastingRequest);
	}

	public ForcastingResult2 predict2() {
		Map<String, Double> forcastingRequest = new HashMap<String, Double>();
		UtilHttp.fillObject(forcastingRequest, httpMethod, httpInput, logger);
		return Sapere.getInstance().generateMockForcasting2(forcastingRequest);
	}

	public EndUserForcastingResult getForcasting() throws HandlingException {
		EndUserForcastingRequest euForcastingRequest = new EndUserForcastingRequest();
		UtilHttp.fillObject(euForcastingRequest, httpMethod, httpInput, logger);
		return Sapere.getInstance().getForcasting(euForcastingRequest);
	}

	public EndUserForcastingRef getEndUserForcastingRef() throws HandlingException {
		return Sapere.getInstance().getEndUserForcastingRef();
	}

}
