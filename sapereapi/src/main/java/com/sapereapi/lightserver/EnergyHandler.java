package com.sapereapi.lightserver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.model.OperationResult;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.Service;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.ExtendedNodeTotal;
import com.sapereapi.model.energy.MultiNodesContent;
import com.sapereapi.model.energy.NodeContent;
import com.sapereapi.model.energy.OptionItem;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.model.energy.forcasting.EndUserForcastingResult;
import com.sapereapi.model.energy.forcasting.ForcastingResult;
import com.sapereapi.model.energy.forcasting.input.EndUserForcastingRef;
import com.sapereapi.model.energy.forcasting.input.EndUserForcastingRequest;
import com.sapereapi.model.energy.forcasting.input.ForcastingRequest;
import com.sapereapi.model.energy.input.AgentFilter;
import com.sapereapi.model.energy.input.AgentInputForm;
import com.sapereapi.model.energy.input.OfferFilter;
import com.sapereapi.model.input.InitializationForm;
import com.sapereapi.model.markov.MarkovStateHistory;
import com.sapereapi.model.markov.NodeTransitionMatrices;
import com.sapereapi.model.prediction.FedAvgResult;
import com.sapereapi.model.prediction.MultiPredictionsData;
import com.sapereapi.model.prediction.PredictionData;
import com.sapereapi.model.prediction.PredictionStatistic;
import com.sapereapi.model.prediction.input.FedAvgCheckupRequest;
import com.sapereapi.model.prediction.input.MassivePredictionRequest;
import com.sapereapi.model.prediction.input.MatrixFilter;
import com.sapereapi.model.prediction.input.PredictionRequest;
import com.sapereapi.model.prediction.input.StateHistoryRequest;
import com.sapereapi.model.prediction.input.StatisticsRequest;
import com.sapereapi.util.UtilHttp;

import eu.sapere.middleware.node.NodeConfig;

public class EnergyHandler extends AbstractHandler {

	public EnergyHandler(String uri, NodeConfig nodeConfig, List<NodeConfig> _defaultNeighbours) {
		super();
		this.uri = uri;
		this.nodeConfig = nodeConfig;
		this.defaultNeighbours = _defaultNeighbours;
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

	public MultiNodesContent retrieveAllNodesContent() {
		AgentFilter filter = new AgentFilter();
		return Sapere.getInstance().retrieveAllNodesContent(filter);
	}

	public InitializationForm initEnergyService() {
		InitializationForm initForm = new InitializationForm();
		UtilHttp.fillObject(initForm, httpMethod, httpInput, logger);
		return Sapere.getInstance().initEnergyService(this.nodeConfig, initForm, defaultNeighbours);
	}

	public AgentForm addAgent() {
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

	public AgentForm modifyAgent() {
		AgentInputForm agentInputForm = new AgentInputForm();
		UtilHttp.fillObject(agentInputForm, httpMethod, httpInput, logger);
		return Sapere.getInstance().modifyEnergyAgent(agentInputForm);
	}

	public AgentForm restartAgent() {
		AgentInputForm agentInputForm = new AgentInputForm();
		UtilHttp.fillObject(agentInputForm, httpMethod, httpInput, logger);
		return Sapere.getInstance().restartEnergyAgent(agentInputForm);
	}

	public Iterable<Service> test1() {
		return Sapere.getInstance().test1(nodeConfig);
	}

	public Iterable<Service> test2() {
		return Sapere.getInstance().test2(nodeConfig);
	}

	public Iterable<Service> test3() {
		return Sapere.getInstance().test3(nodeConfig);
	}

	public Iterable<Service> test4() {
		return Sapere.getInstance().test4(nodeConfig);
	}

	public Iterable<Service> test5() {
		return Sapere.getInstance().test5(nodeConfig);
	}

	public Iterable<Service> test6() {
		return Sapere.getInstance().test6(nodeConfig);
	}

	public Iterable<Service> testTragedyOfTheCommons() {
		return Sapere.getInstance().testTragedyOfTheCommons(nodeConfig, true);
	}

	public NodeContent restartLastNodeContent() {
		return Sapere.getInstance().restartLastNodeContent();
	}

	@Route(value = "/nodeTotalHistory")
	public List<ExtendedNodeTotal> retrieveNodeTotalHistory() {
		return EnergyDbHelper.retrieveNodeTotalHistory();
	}

	public NodeTransitionMatrices retrieveCurrentNodeTransitionMatrices() {
		return Sapere.getInstance().getCurrentNodeTransitionMatrices();
	}

	public NodeConfig getNodeConfig() {
		return Sapere.getInstance().getNodeConfig();
	}

	public Map<String, NodeConfig> getMapAllNodeConfigs() {
		return Sapere.getInstance().getMapAllNodeConfigs(true);
	}

	public List<OptionItem> getStateDates() {
		return Sapere.getInstance().getStateDates();
	}

	@Route(value = "/allNodeTransitionMatrices")
	public List<NodeTransitionMatrices> getAllNodeTransitionMatrices() {
		MatrixFilter matrixFilter = new MatrixFilter();
		UtilHttp.fillObject(matrixFilter, httpMethod, httpInput, logger);
		return Sapere.getInstance().getAllNodeTransitionMatrices(matrixFilter);
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

	List<MarkovStateHistory> retrieveLastMarkovHistoryStates() {
		StateHistoryRequest stateHistoryRequest = new StateHistoryRequest();
		UtilHttp.fillObject(stateHistoryRequest, httpMethod, httpInput, logger);
		return Sapere.getInstance().retrieveLastMarkovHistoryStates(stateHistoryRequest);
	}

	FedAvgResult checkupFedAVG() {
		FedAvgCheckupRequest fedAvgCheckupRequest = new FedAvgCheckupRequest();
		UtilHttp.fillObject(fedAvgCheckupRequest, httpMethod, httpInput, logger);
		return Sapere.getInstance().checkupFedAVG(fedAvgCheckupRequest);
	}

	public PredictionData getPrediction() {
		PredictionRequest predictionRequest = new PredictionRequest();
		UtilHttp.fillObject(predictionRequest, httpMethod, httpInput, logger);
		return Sapere.getInstance().getPrediction(predictionRequest);
	}

	public MultiPredictionsData getMassivePredictions()  {
		MassivePredictionRequest massivePredictionRequest = new MassivePredictionRequest();
		UtilHttp.fillObject(massivePredictionRequest, httpMethod, httpInput, logger);
		return Sapere.getInstance().generateMassivePredictions(massivePredictionRequest);
	}

	public List<PredictionStatistic> computePredictionStatistics() {
		StatisticsRequest statisticsRequest = new StatisticsRequest();
		UtilHttp.fillObject(statisticsRequest, httpMethod, httpInput, logger);
		return Sapere.getInstance().computePredictionStatistics(statisticsRequest);
	}

	public List<SingleOffer> retrieveOffers() {
		OfferFilter offerFilter = new OfferFilter();
		UtilHttp.fillObject(offerFilter, httpMethod, httpInput, logger);
		return EnergyDbHelper.retrieveOffers(offerFilter);
	}

	public ForcastingResult computeForcasting() {
		//ForcastingRequest forcastingRequest = new ForcastingRequest();
		Map<String, Double> forcastingRequest2 = new HashMap<String, Double>();
		UtilHttp.fillObject(forcastingRequest2, httpMethod, httpInput, logger);
		return Sapere.getInstance().generateMockForcasting(forcastingRequest2);
	}

	public EndUserForcastingResult getForcasting() {
		EndUserForcastingRequest euForcastingRequest = new EndUserForcastingRequest();
		UtilHttp.fillObject(euForcastingRequest, httpMethod, httpInput, logger);
		return Sapere.getInstance().getForcasting(euForcastingRequest);
	}

	public EndUserForcastingRef getEndUserForcastingRef() {
		return Sapere.getInstance().getEndUserForcastingRef();
	}

}
