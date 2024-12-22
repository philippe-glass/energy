package com.sapereapi.api;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.OperationResult;
import com.sapereapi.model.OptionItem;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.ServerConfig;
import com.sapereapi.model.Service;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.model.energy.forcasting.ForcastingResult2;
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

import eu.sapere.middleware.node.NodeLocation;

@RestController
@RequestMapping("/energy")
public class EnergyController {
	 @Autowired
	 private Environment environment;

	private ServerConfig getServerConfig() {
		return SapereAPIApplication.getServerConfig();
	}

	 @PostConstruct
	 public void init() {
	    System.out.println("environment = " + environment);
	 }


	@GetMapping(value = "/restartLastNodeContent")
	public NodeContent restartLastNodeContent() throws HandlingException {
		return Sapere.getInstance().restartLastNodeContent();
	}

	@GetMapping(value = "/retrieveNodeContent")
	public NodeContent retrieveNodeContent(AgentFilter filter) {
		return Sapere.getInstance().retrieveNodeContent(filter);
	}

	@GetMapping(value = "/retrieveAllNodesContent")
	public MultiNodesContent retrieveAllNodesContent(AgentFilter filter) throws HandlingException {
		return Sapere.getInstance().retrieveAllNodesContent(filter);
	}

	@GetMapping(value = "/allNodeTransitionMatrices")
	public ILearningModel getAllNodeTransitionMatrices(MatrixFilter matrixFilter) {
		return Sapere.getInstance().getLearningModel(matrixFilter);
	}

	@GetMapping(value = "/nodeTotalHistory")
	public List<ExtendedNodeTotal> retrieveNodeTotalHistory(NodeHistoryFilter historyFilter) throws HandlingException {
		return EnergyDbHelper.retrieveNodeTotalHistory(historyFilter);
	}

	@GetMapping(value = "/getPredictionContext")
	public PredictionContext getPredictionContext(PredictionScopeFilter scopeFilter) {
		return Sapere.getInstance().getPredictionContext(scopeFilter);
	}

	@GetMapping(value = "/getNodeLocation")
	public NodeLocation getNodeLocation() {
		return Sapere.getInstance().getNodeLocation();
	}

	@GetMapping(value = "/getMapAllNodeLocations")
	public Map<String, NodeLocation> getMapAllNodeLocations() {
		return Sapere.getInstance().getMapAllNodeLocations(true);
	}

	@GetMapping(value = "/getStateDates")
	public List<OptionItem> getStateDates(PredictionScopeFilter scopeFilter) throws HandlingException {
		return Sapere.getInstance().getStateDates(scopeFilter);
	}

	@PostMapping(value = "/initEnergyService")
	public InitializationForm initEnergyService(@RequestBody InitializationForm initForm) throws HandlingException {
		return Sapere.getInstance().initEnergyService(SapereAPIApplication.getServerConfig(), initForm);
	}

	@PostMapping(value = "/initNodeHistory2")
	public ILearningModel initNodeHistory2(@RequestBody HistoryInitializationRequest historyInitRequest) throws HandlingException {
		return Sapere.getInstance().initNodeHistory(historyInitRequest);
	}

	@GetMapping(value = "/enableSupervision")
	public OperationResult enableSupervision() {
		return Sapere.enableSupervision();
	}

	@GetMapping(value = "/stopAllAgents")
	public OperationResult stopAllAgents1() {
		return Sapere.getInstance().stopAllAgents();
	}

	@GetMapping(value = "/stopEnergyService")
	public void stopEnergyService() {
		Sapere.getInstance().stopEnergyService();
	}

	@GetMapping(value = "/retrieveLastHistoryStates")
	List<VariableStateHistory> retrieveLastHistoryStates(StateHistoryRequest stateHistoryRequest ) throws HandlingException {
		return Sapere.getInstance().retrieveLastHistoryStates(stateHistoryRequest);
	}

	@GetMapping(value = "/checkupModelAggregation")
	AbstractAggregationResult checkupModelAggregation(AggregationCheckupRequest checkupRequest ) {
		return Sapere.getInstance().checkupModelAggregation(checkupRequest);
	}

	@GetMapping(value = "/getPrediction")
	public PredictionData getPrediction(@DateTimeFormat(pattern = "yyyy-MM-dd") PredictionRequest predictionRequest) throws HandlingException {
		return Sapere.getInstance().getPrediction(predictionRequest);
	}

	@GetMapping(value = "/getMassivePredictions")
	public MultiPredictionsData getMassivePredictions(MassivePredictionRequest massivePredictionRequest) throws HandlingException {
		return Sapere.getInstance().generateMassivePredictions(massivePredictionRequest);
	}

	@GetMapping(value = "/computePredictionStatistics")
	public List<PredictionStatistic> computePredictionStatistics(StatisticsRequest statisticsRequest) throws HandlingException {
		return Sapere.getInstance().computePredictionStatistics(statisticsRequest);
	}

	@PostMapping(value = "/retrieveOffers")
	public List<SingleOffer> retrieveOffers(@RequestBody OfferFilter offerFilter) throws HandlingException {
		return EnergyDbHelper.retrieveOffers(offerFilter);
	}

	@PostMapping(value = "/addAgent")
	public AgentForm addAgent(@RequestBody AgentInputForm agentInputForm) throws HandlingException {
		return Sapere.getInstance().addEnergyAgent(agentInputForm);
	}

	@PostMapping(value = "/stopAgent")
	public AgentForm stopAgent(@RequestBody AgentInputForm agentInputForm) {
		return Sapere.getInstance().stopAgent(agentInputForm);
	}

	@PostMapping(value = "/stopListAgents")
	public OperationResult stopListAgents(@RequestBody AgentInputForm agentInputForm) {
		return Sapere.getInstance().stopListAgents(agentInputForm);
	}

	@PostMapping(value = "/modifyAgent")
	public AgentForm modifyAgent(@RequestBody AgentInputForm agentInputForm) throws HandlingException {
		return Sapere.getInstance().modifyEnergyAgent(agentInputForm);
	}

	@PostMapping(value = "/restartAgent")
	public AgentForm restartAgent(@RequestBody AgentInputForm agentInputForm) {
		return Sapere.getInstance().restartEnergyAgent(agentInputForm);
	}

	@GetMapping(path = "/test1")
	public @ResponseBody Iterable<Service> test1() throws HandlingException {
		return Sapere.getInstance().test1(getServerConfig());
	}

	@GetMapping(path = "/test2")
	public @ResponseBody Iterable<Service> test2() throws HandlingException {
		return Sapere.getInstance().test2(getServerConfig());
	}

	@GetMapping(path = "/test3")
	public @ResponseBody Iterable<Service> test3() throws HandlingException {
		return Sapere.getInstance().test3(getServerConfig());
	}

	@GetMapping(path = "/test4")
	public @ResponseBody Iterable<Service> test4() throws HandlingException {
		return Sapere.getInstance().test4(getServerConfig());
	}

	@GetMapping(path = "/test5")
	public @ResponseBody Iterable<Service> test5() throws HandlingException {
		return Sapere.getInstance().test5(getServerConfig());
	}

	@GetMapping(path = "/test6")
	public @ResponseBody Iterable<Service> test6() throws HandlingException {
		return Sapere.getInstance().test6(getServerConfig()) ;
	}

	@GetMapping(path = "/testTragedyOfTheCommons")
	public @ResponseBody Iterable<Service> testTragedyOfTheCommons() throws HandlingException {
		return Sapere.getInstance().testTragedyOfTheCommons(getServerConfig(), true);
	}

	@PostMapping(value = "/doForcasting(")
	public ForcastingResult2 doForcasting(@RequestBody Map<String, Double> forcastingRequest) {
		return Sapere.getInstance().generateMockForcasting2(forcastingRequest);
	}

}
