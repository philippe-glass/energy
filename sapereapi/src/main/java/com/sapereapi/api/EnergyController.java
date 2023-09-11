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

import com.sapereapi.db.DBConfig;
import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.db.PredictionDbHelper;
import com.sapereapi.model.OperationResult;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.Service;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.ExtendedNodeTotal;
import com.sapereapi.model.energy.MultiNodesContent;
import com.sapereapi.model.energy.NodeContent;
import com.sapereapi.model.energy.OptionItem;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.model.energy.forcasting.ForcastingResult;
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

import eu.sapere.middleware.node.NodeConfig;

@RestController
@RequestMapping("/energy")
public class EnergyController {
	 @Autowired
	 private Environment environment;

	private NodeConfig getNodeConfigFromEnvironment() {
		Integer lsaServerPort = Integer.valueOf(environment.getProperty("lsa_server.port"));
		String lsaServerHost = String.valueOf(environment.getProperty("lsa_server.host"));
		String nodeName = String.valueOf(environment.getProperty("lsa_server.name"));
		Integer restServerPort = Integer.valueOf(environment.getProperty("server.port"));
		NodeConfig nodeConfig = new NodeConfig(nodeName, lsaServerHost, lsaServerPort, restServerPort);
		return nodeConfig;
	}

	private DBConfig getDBConfigFromEnvironment() {
		String driverClassName = environment.getProperty("spring.datasource.driver-class-name");
		String url = environment.getProperty("spring.datasource.url");
		String user = environment.getProperty("spring.datasource.username");
		String password = environment.getProperty("spring.datasource.password");
		DBConfig dbConfig = new DBConfig(driverClassName, url, user, password);
		return dbConfig;
	}

	 @PostConstruct
	 public void init() {
	    System.out.println("environment = " + environment);
	    Sapere.setLocation(getNodeConfigFromEnvironment());
	    DBConfig dbConfig = getDBConfigFromEnvironment();
	    EnergyDbHelper.init(dbConfig);
	    PredictionDbHelper.init(dbConfig);
	 }


	@GetMapping(value = "/restartLastNodeContent")
	public NodeContent restartLastNodeContent() {
		return Sapere.getInstance().restartLastNodeContent();
	}

	@GetMapping(value = "/retrieveNodeContent")
	public NodeContent retrieveNodeContent(AgentFilter filter) {
		return Sapere.getInstance().retrieveNodeContent(filter);
	}

	@GetMapping(value = "/retrieveAllNodesContent")
	public MultiNodesContent retrieveAllNodesContent(AgentFilter filter) {
		return Sapere.getInstance().retrieveAllNodesContent(filter);
	}

	@GetMapping(value = "/allNodeTransitionMatrices")
	public List<NodeTransitionMatrices> getAllNodeTransitionMatrices(MatrixFilter matrixFilter) {
		return Sapere.getInstance().getAllNodeTransitionMatrices(matrixFilter);
	}

	@GetMapping(value = "/nodeTotalHistory")
	public List<ExtendedNodeTotal> retrieveNodeTotalHistory() {
		return EnergyDbHelper.retrieveNodeTotalHistory();
	}

	@GetMapping(value = "/retrieveCurrentNodeTransitionMatrices")
	public NodeTransitionMatrices retrieveCurrentNodeTransitionMatrices() {
		return Sapere.getInstance().getCurrentNodeTransitionMatrices();
	}

	@GetMapping(value = "/getNodeConfig")
	public NodeConfig getNodeConfig() {
		return Sapere.getInstance().getNodeConfig();
	}

	@GetMapping(value = "/getMapAllNodeConfigs")
	public Map<String, NodeConfig> getMapAllNodeConfigs() {
		return Sapere.getInstance().getMapAllNodeConfigs(true);
	}

	@GetMapping(value = "/getStateDates")
	public List<OptionItem> getStateDates() {
		return Sapere.getInstance().getStateDates();
	}

	/*
	@PostMapping(value = "/allNodeTransitionMatrices")
	public List<NodeTransitionMatrices> getAllNodeTransitionMatrices(@RequestBody MatrixFilter matrixFilter) {
		return Sapere.getInstance().getAllNodeTransitionMatrices(matrixFilter);
	}*/

	@PostMapping(value = "/initEnergyService")
	public InitializationForm initEnergyService(@RequestBody InitializationForm initForm) {
		return Sapere.getInstance().initEnergyService(getNodeConfigFromEnvironment(), initForm, null);
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

	@GetMapping(value = "/retrieveLastMarkovHistoryStates")
	List<MarkovStateHistory> retrieveLastMarkovHistoryStates(StateHistoryRequest stateHistoryRequest ) {
		return Sapere.getInstance().retrieveLastMarkovHistoryStates(stateHistoryRequest);
	}

	@GetMapping(value = "/checkupFedAVG")
	FedAvgResult checkupFedAVG(FedAvgCheckupRequest fedAvgCheckupRequest ) {
		return Sapere.getInstance().checkupFedAVG(fedAvgCheckupRequest);
	}

	@GetMapping(value = "/getPrediction")
	public PredictionData getPrediction(@DateTimeFormat(pattern = "yyyy-MM-dd") PredictionRequest predictionRequest) {
		return Sapere.getInstance().getPrediction(predictionRequest);
	}

	@GetMapping(value = "/getMassivePredictions")
	public MultiPredictionsData getMassivePredictions(MassivePredictionRequest massivePredictionRequest) {
		return Sapere.getInstance().generateMassivePredictions(massivePredictionRequest);
	}

	@GetMapping(value = "/computePredictionStatistics")
	public List<PredictionStatistic> computePredictionStatistics(StatisticsRequest statisticsRequest) {
		return Sapere.getInstance().computePredictionStatistics(statisticsRequest);
	}

	@PostMapping(value = "/retrieveOffers")
	public List<SingleOffer> retrieveOffers(@RequestBody OfferFilter offerFilter) {
		return EnergyDbHelper.retrieveOffers(offerFilter);
	}

	@PostMapping(value = "/addAgent")
	public AgentForm addAgent(@RequestBody AgentInputForm agentInputForm) {
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
	public AgentForm modifyAgent(@RequestBody AgentInputForm agentInputForm) {
		return Sapere.getInstance().modifyEnergyAgent(agentInputForm);
	}

	@PostMapping(value = "/restartAgent")
	public AgentForm restartAgent(@RequestBody AgentInputForm agentInputForm) {
		return Sapere.getInstance().restartEnergyAgent(agentInputForm);
	}

	@GetMapping(path = "/test1")
	public @ResponseBody Iterable<Service> test1() {
		return Sapere.getInstance().test1(getNodeConfigFromEnvironment());
	}

	@GetMapping(path = "/test2")
	public @ResponseBody Iterable<Service> test2() {
		return Sapere.getInstance().test2(getNodeConfigFromEnvironment());
	}

	@GetMapping(path = "/test3")
	public @ResponseBody Iterable<Service> test3() {
		return Sapere.getInstance().test3(getNodeConfigFromEnvironment());
	}

	@GetMapping(path = "/test4")
	public @ResponseBody Iterable<Service> test4() {
		return Sapere.getInstance().test4(getNodeConfigFromEnvironment());
	}

	@GetMapping(path = "/test5")
	public @ResponseBody Iterable<Service> test5() {
		return Sapere.getInstance().test5(getNodeConfigFromEnvironment());
	}

	@GetMapping(path = "/test6")
	public @ResponseBody Iterable<Service> test6() {
		return Sapere.getInstance().test6(getNodeConfigFromEnvironment());
	}

	@GetMapping(path = "/testTragedyOfTheCommons")
	public @ResponseBody Iterable<Service> testTragedyOfTheCommons() {
		return Sapere.getInstance().testTragedyOfTheCommons(getNodeConfigFromEnvironment(), true);
	}

	@PostMapping(value = "/doForcasting(")
	public ForcastingResult doForcasting(@RequestBody Map<String, Double> /*ForcastingRequest*/ forcastingRequest) {
		return Sapere.getInstance().generateMockForcasting(forcastingRequest);
	}

}
