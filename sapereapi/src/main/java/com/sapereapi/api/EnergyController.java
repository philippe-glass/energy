package com.sapereapi.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.sapereapi.agent.energy.ConsumerAgent;
import com.sapereapi.agent.energy.ProducerAgent;
import com.sapereapi.db.EnergyDbHelper;
import com.sapereapi.db.PredictionDbHelper;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.InitializationForm;
import com.sapereapi.model.NodesAddresses;
import com.sapereapi.model.OperationResult;
import com.sapereapi.model.Sapere;
import com.sapereapi.model.Service;
import com.sapereapi.model.TimeFilter;
import com.sapereapi.model.TimeSlot;
import com.sapereapi.model.TimeSlotFilter;
import com.sapereapi.model.energy.AgentFilter;
import com.sapereapi.model.energy.AgentForm;
import com.sapereapi.model.energy.Device;
import com.sapereapi.model.energy.DeviceFilter;
import com.sapereapi.model.energy.DeviceMeasure;
import com.sapereapi.model.energy.ExtendedNodeTotal;
import com.sapereapi.model.energy.NodeContent;
import com.sapereapi.model.energy.OptionItem;
import com.sapereapi.model.energy.RegulationWarning;
import com.sapereapi.model.energy.SimulatorLog;
import com.sapereapi.model.energy.SingleOffer;
import com.sapereapi.model.energy.policy.IProducerPolicy;
import com.sapereapi.model.markov.MarkovState;
import com.sapereapi.model.markov.MarkovStateHistory;
import com.sapereapi.model.markov.NodeMarkovStates;
import com.sapereapi.model.markov.NodeTransitionMatrices;
import com.sapereapi.model.prediction.MassivePredictionRequest;
import com.sapereapi.model.prediction.MatrixFilter;
import com.sapereapi.model.prediction.MultiPredictionsData;
import com.sapereapi.model.prediction.PredictionData;
import com.sapereapi.model.prediction.PredictionRequest;
import com.sapereapi.model.prediction.PredictionStatistic;
import com.sapereapi.model.prediction.StateHistoryRequest;
import com.sapereapi.model.prediction.StatisticsRequest;
import com.sapereapi.model.referential.PriorityLevel;
import com.sapereapi.model.referential.WarningType;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import eu.sapere.middleware.agent.SapereAgent;
import eu.sapere.middleware.node.NodeManager;

@RestController
@RequestMapping("/energy")
public class EnergyController {
	 @Autowired
	 private Environment environment;
	 @Autowired
	 private ConfigRepository repository;

	 @PostConstruct
	 public void init() {
	    System.out.println("environment = " + environment);
	    Sapere.setLocation(repository);
	    EnergyDbHelper.init(environment);
	    PredictionDbHelper.init(environment);
	 }

	private void resolveAgentsNotInsapce() {
		for(String agentName : Sapere.getInstance().checkupNotInSpace()) {
			Sapere.getInstance().callSetInitialLSA(agentName);
		}
	}

	private AgentForm generateAgentForm(SapereAgent agent) {
		boolean isInSpace = true;
		AgentForm result = null;
		if(agent instanceof ConsumerAgent) {
			ConsumerAgent consumer = (ConsumerAgent) agent;
			isInSpace = Sapere.getInstance().isInSpace(consumer.getAgentName());
			result = new AgentForm(consumer,isInSpace);
		} else if(agent instanceof ProducerAgent) {
			ProducerAgent producer = (ProducerAgent) agent;
			isInSpace = Sapere.getInstance().isInSpace(producer.getAgentName());
			result = new AgentForm(producer, isInSpace);
		}
		return result;
	}


	@GetMapping(value = "/restartLastNodeContent")
	public NodeContent restartLastNodeContent() {
		return Sapere.getInstance().restartLastNodeContent();
	}

	@GetMapping(value = "/allNodesContent")
	public NodeContent allNodesContent(NodesAddresses nodeAddresses) {
		return Sapere.getInstance().retrieveAllNodesContent(nodeAddresses);
	}

	@GetMapping(value = "/retrieveNodeContent")
	public NodeContent retrieveNodeContent(AgentFilter filter) {
		return Sapere.getInstance().retrieveNodeContent(filter);
	}

	@GetMapping(value = "/nodeTotalHistory")
	public List<ExtendedNodeTotal> retrieveNodeTotalHistory() {
		return EnergyDbHelper.retrieveNodeTotalHistory();
	}

	@GetMapping(value = "/getNodeDevices")
	public static List<Device> getNodeDevices() {
		return Sapere.getInstance().getNodeDevices();
	}

	@GetMapping(value = "/retrieveMeyrinDevices")
	public static List<Device> retrieveMeyrinDevices() {
		return Sapere.getInstance().retrieveMeyrinDevices();
	}

	@GetMapping(value = "/retrieveDeviceStatistics")
	public Map<String, Double> retrieveDeviceStatistics(TimeFilter timeFilter) {
		DeviceFilter deviceFilter = new DeviceFilter();
		return Sapere.getInstance().retrieveDeviceStatistics(deviceFilter, timeFilter);
	}

	@GetMapping(value = "/retrieveDevicesMeasures")
	public  List<DeviceMeasure> retrieveDevicesMeasures(TimeSlotFilter timeSloteFilter) {
		DeviceFilter deviceFilter = new DeviceFilter();
		if(timeSloteFilter.getLongDateBegin()==null ||timeSloteFilter.getLongDateEnd()==null ) {
			return new ArrayList<DeviceMeasure>();
		}
		String featureType = timeSloteFilter.getFeatureType();
		Date dateBegin =  new Date(timeSloteFilter.getLongDateBegin());
		Date dateEnd =  new Date(timeSloteFilter.getLongDateEnd());
		return Sapere.getInstance().retrieveDevicesMeasures(deviceFilter, featureType, dateBegin, dateEnd);
	}

	@GetMapping(value = "/retrieveLastDevicesMeasure")
	public DeviceMeasure retrieveLastDevicesMeasure(TimeSlotFilter timeSloteFilter) {
		String featureType = timeSloteFilter.getFeatureType();
		Date dateBegin =  timeSloteFilter.getLongDateBegin()==null? null : new Date(timeSloteFilter.getLongDateBegin());
		Date dateEnd =  timeSloteFilter.getLongDateEnd()==null? null : new Date(timeSloteFilter.getLongDateEnd());
		return Sapere.getInstance().retrieveLastDevicesMeasure(featureType, dateBegin, dateEnd);
	}

	@GetMapping(value = "/retrieveCurrentNodeTransitionMatrices")
	public NodeTransitionMatrices retrieveCurrentNodeTransitionMatrices() {
		return Sapere.getInstance().getCurrentNodeTransitionMatrices();
	}

	@GetMapping(value = "/getLocations")
	public List<OptionItem> getLocations() {
		return Sapere.getInstance().getLocations();
	}

	@GetMapping(value = "/getStateDates")
	public List<OptionItem> getStateDates() {
		return Sapere.getInstance().getStateDates();
	}

	@PostMapping(value = "/allNodeTransitionMatrices")
	public List<NodeTransitionMatrices> getAllNodeTransitionMatrices(@RequestBody MatrixFilter matrixFilter) {
		if("".equals(matrixFilter.getLocation()) || matrixFilter.getLocation() == null) {
			// Default location : localhost
			matrixFilter.setLocation(NodeManager.getLocation());
		}
		return Sapere.getInstance().getAllNodeTransitionMatrices(matrixFilter);
	}

	@PostMapping(value = "/initEnergyService")
	public InitializationForm initEnergyService(@RequestBody InitializationForm initForm) {
		SapereLogger.getInstance().info("initEnergyService : scenario = " +  initForm.getScenario());
		Boolean disableSupervision = initForm.getDisableSupervision();
		Sapere.getInstance().initEnergyService(repository, initForm.getScenario(), initForm.getMaxTotalPower(), disableSupervision, initForm.generateDatetimeShifts());
		String stateVariable = initForm.getInitialStateVariable();
		Integer stateId = initForm.getInitialStateId();
		if(stateId!=null && stateVariable!=null && !"".equals(stateVariable)) {
			MarkovState targetState = NodeMarkovStates.getById(stateId);
			if(targetState!=null) {
				Sapere.getInstance().initState(repository, stateVariable, targetState, disableSupervision);
			}
		}
		return initForm;
	}

	@GetMapping(value = "/enableSupervision")
	public OperationResult enableSupervision() {
		Sapere.enableSupervision();
		OperationResult result = new OperationResult(true, "");
		return result;
	}

	@GetMapping(value = "/stopAllAgents")
	public OperationResult stopAllAgents1() {
		boolean isOK = Sapere.getInstance().stopAllAgents();
		OperationResult result = new OperationResult(isOK, "");
		return result;
	}

	@GetMapping(value = "/stopEnergyService")
	public void stopEnergyService() {
		Sapere.getInstance().stopEnergyService();
	}

	@PostMapping(value = "/retrieveLastMarkovHistoryStates")
	List<MarkovStateHistory> retrieveLastMarkovHistoryStates(@RequestBody StateHistoryRequest stateHistoryRequest ) {
		Date minCreationDate = stateHistoryRequest.getMinDate();
		String variableName = stateHistoryRequest.getVariableName();
		boolean observationUpdated = stateHistoryRequest.getObservationUpdated();
		return Sapere.getInstance().retrieveLastMarkovHistoryStates(minCreationDate, variableName, observationUpdated);
	}

	@PostMapping(value = "/getPrediction")
	public PredictionData getPrediction(@RequestBody PredictionRequest predictionRequest) {
		Date initDate = predictionRequest.getInitDate();
		Date targetDate = predictionRequest.getTargetDate();
		String location = predictionRequest.getLocation();
		boolean useCorrections = predictionRequest.isUseCorrections();
		SapereLogger.getInstance().info("targetDate = " + targetDate);
		PredictionData prediction = Sapere.getInstance().getPrediction(initDate, targetDate, location, useCorrections);
		return prediction;
	}

	@PostMapping(value = "/getMassivePredictions")
	public MultiPredictionsData getMassivePredictions(@RequestBody MassivePredictionRequest massivePredictionRequest) {
		TimeSlot targetDateSlot = massivePredictionRequest.getTimeSlot();
		String location = massivePredictionRequest.getLocation();
		String variableName = massivePredictionRequest.getVariableName();
		SapereLogger.getInstance().info("targetDateMin = " + targetDateSlot.getBeginDate() + ", targetDateMax = " + targetDateSlot.getEndDate());
		int horizonInMinutes = massivePredictionRequest.getHorizonInMinutes();
		boolean useCorrections = massivePredictionRequest.isUseCorrections();
		boolean generateCorrections = massivePredictionRequest.isGenerateCorrections();
		MultiPredictionsData result = Sapere.getInstance()
				.generateMassivePredictions(targetDateSlot, location, variableName, horizonInMinutes, useCorrections, generateCorrections);
		return result;
	}

	@PostMapping(value = "/computePredictionStatistics")
	public List<PredictionStatistic> computePredictionStatistics(@RequestBody StatisticsRequest statisticsRequest) {
		Date minComputeDay = statisticsRequest.getMinComputeDay();
		Date maxComputeDay = statisticsRequest.getMaxComputeDay();
		Date minTargetDate = statisticsRequest.getMinTargetDay();
		Date maxTargetDate = statisticsRequest.getMaxTargetDay();
		String location = statisticsRequest.getLocation();
		String variableName = statisticsRequest.getVariableName();
		Boolean useCorrectionFilter = statisticsRequest.getUseCorrectionFilter();
		Integer minHour = statisticsRequest.getMinTargetHour();
		Integer maxHour = statisticsRequest.getMaxTargetHour();
		List<String> fieldsToMerge = statisticsRequest.getFieldsToMerge();
		return Sapere.getInstance().computePredictionStatistics(minComputeDay, maxComputeDay,minTargetDate, maxTargetDate, location, minHour, maxHour, useCorrectionFilter, variableName, fieldsToMerge);
	}

	@PostMapping(value = "/retrieveOffers")
	public List<SingleOffer> retrieveOffers(@RequestBody ExtendedNodeTotal nodeTotalHistory) {
		SapereLogger.getInstance().info("retrieveOffers : nodeTotalHistory = " + nodeTotalHistory);
		return EnergyDbHelper.retrieveOffers(nodeTotalHistory.getDate(), nodeTotalHistory.getDateNext(), null, null, null);
	}

	@PostMapping(value = "/addAgent")
	public AgentForm addAgent(@RequestBody AgentForm agentForm) {
		resolveAgentsNotInsapce();
		AgentForm result = null;
		Date current = Sapere.getInstance().getCurrentDate();
		if(agentForm.getEndDate()!=null &&  agentForm.getEndDate().before(current)) {
			SapereLogger.getInstance().warning("#### addAgent : enddate = " + agentForm.getEndDate());
		}
		if(!SapereUtil.checkIsRound(agentForm.getPower(), 2, SapereLogger.getInstance())) {
			SapereLogger.getInstance().warning("#### addAgent : power = " + SapereUtil.round(agentForm.getPower(), 5));
			double powerToSet = SapereUtil.round(agentForm.getPower(), 2);
			agentForm.setPowers(powerToSet,powerToSet,powerToSet);
		}
		//DeviceCategory deviceCategory = DeviceCategory.getByName(agentForm.getDeviceCategoryCode());
		//EnvironmentalImpact envImpact = EnvironmentalImpact.getByLevel(agentForm.getEnvironmentalImpact());
		try {
			SapereAgent newAgent = null;
			if(agentForm.isConsumer()) {
				agentForm.generateSimplePricingTable(0);
				PriorityLevel priority = PriorityLevel.getByLabel(agentForm.getPriorityLevel());
				double delayToleranceMinutes = agentForm.getDelayToleranceMinutes();
				if(delayToleranceMinutes == 0  && agentForm.getDelayToleranceRatio() != null) {
					delayToleranceMinutes = agentForm.getDelayToleranceRatio() * UtilDates.computeDurationMinutes(agentForm.getBeginDate(), agentForm.getEndDate());
				}
				newAgent = Sapere.getInstance().addServiceConsumer(agentForm.getPower(), agentForm.getBeginDate(),
						agentForm.getEndDate(), delayToleranceMinutes, priority, agentForm.retrieveDeviceProperties()
						, agentForm.generatePricingTable(), null);
			} else if (agentForm.isProducer()) {
				agentForm.generateSimplePricingTable(0);
				IProducerPolicy producerPolicy = Sapere.getInstance().initDefaultProducerPolicy();
				newAgent = Sapere.getInstance().addServiceProducer(agentForm.getPower(), agentForm.getBeginDate(),
						agentForm.getEndDate(), agentForm.retrieveDeviceProperties(), agentForm.generatePricingTable()
						, producerPolicy);
			}
			if(newAgent!=null) {
				Thread.sleep(1*1000);
				result = generateAgentForm(newAgent);
			}
		} catch (Exception e) {
			SapereLogger.getInstance().error(e);
		}
		/*
		if(false && result!=null &&!result.checkInSpace()) {
			resolveAgentsNotInsapce();
		}*/
		return result;
	}

	@PostMapping(value = "/stopAgent")
	public AgentForm stopAgent(@RequestBody AgentForm agentForm) {
		resolveAgentsNotInsapce();
		AgentForm result = null;
		Date current = Sapere.getInstance().getCurrentDate();
		long timeShiftMS = Sapere.getInstance().getTimeShiftMS();
		RegulationWarning warning = new RegulationWarning(WarningType.USER_INTERRUPTION, current, timeShiftMS);
		warning.addAgent(agentForm.getAgentName());
		try {
			SapereAgent agent = Sapere.getInstance().stopAgent(agentForm.getAgentName(), warning);
			result = generateAgentForm(agent);
		} catch (Exception e) {
			SapereLogger.getInstance().error(e);
		}
		Sapere.getInstance().cleanSubscriptions();
		return result;
	}

	@PostMapping(value = "/stopListAgents")
	public OperationResult stopListAgents(@RequestBody AgentForm agentForm) {
		resolveAgentsNotInsapce();
		Date current = Sapere.getInstance().getCurrentDate();
		long timeShiftMS = Sapere.getInstance().getTimeShiftMS();
		RegulationWarning warning = new RegulationWarning(WarningType.USER_INTERRUPTION, current, timeShiftMS);
		List<String> agentsToStop = new ArrayList<>();
		boolean isOK = false;
		String sListAgent = agentForm.getAgentName();
		for(String agentName : sListAgent.split(",")) {
			warning.addAgent(agentName);
			agentsToStop.add(agentName);
		}
		try {
			isOK = Sapere.getInstance().stopListAgents(agentsToStop, warning);
		} catch (Exception e) {
			SapereLogger.getInstance().error(e);
		}
		OperationResult result = new OperationResult(isOK, "");
		return result;
	}

	@PostMapping(value = "/modifyAgent")
	public AgentForm modifyAgent(@RequestBody AgentForm agentForm) {
		resolveAgentsNotInsapce();
		AgentForm result =  null;
		try {
			if(!SapereUtil.checkIsRound(agentForm.getPower(), 2, SapereLogger.getInstance())) {
				SapereLogger.getInstance().info("---- modifyAgent : power = " + agentForm.getPower());
				double powerToSet = SapereUtil.round(agentForm.getPower(), 2);
				agentForm.setPowers(powerToSet, powerToSet, powerToSet);
			}
			agentForm.setTimeShiftMS(Sapere.getInstance().getTimeShiftMS());
			SapereAgent agent = Sapere.getInstance().modifyAgent(agentForm.getAgentName(), agentForm.getEnergyRequest());
			result = generateAgentForm(agent);
		} catch (Exception e) {
			SapereLogger.getInstance().error(e);
		}
		/*
		if(false && result!=null &&!result.checkInSpace()) {
			resolveAgentsNotInsapce();
		}*/
		return result;
	}

	@PostMapping(value = "/restartAgent")
	public AgentForm restartAgent(@RequestBody AgentForm agentForm) {
		resolveAgentsNotInsapce();
		AgentForm result = null;
		Date current = Sapere.getInstance().getCurrentDate();
		if( agentForm.getEndDate()!=null &&  agentForm.getEndDate().before(current)) {
			SapereLogger.getInstance().warning("#### restartAgent : endadate = " + agentForm.getEndDate());
		}
		if(!SapereUtil.checkIsRound(agentForm.getPower(), 2, SapereLogger.getInstance())) {
			SapereLogger.getInstance().warning("#### restartAgent : power = " + agentForm.getPower());
			double powerToSet = SapereUtil.round(agentForm.getPower(), 2);
			agentForm.setPowers(powerToSet, powerToSet, powerToSet);
		}
		try {
			SapereAgent agent = null;
			if (agentForm.isConsumer()) {
				// Restart consumer agent
				agent = Sapere.getInstance().restartConsumer(agentForm.getAgentName(), agentForm.getEnergyRequest());
			} else if (agentForm.isProducer()) {
				// Restart producer agent
				agent = Sapere.getInstance().restartProducer(agentForm.getAgentName(), agentForm.getEnergySupply());
			}
			if(agent!=null) {
				result = generateAgentForm(agent);
			}
		} catch (Exception e) {
			SapereLogger.getInstance().error(e);
		}
		/*
		if(false && result!=null &&!result.checkInSpace()) {
			resolveAgentsNotInsapce();
		}*/
		return result;
	}

	@PostMapping(value = "/resetSimulatorLogs")
	public void resetSimulatorLogs() {
		EnergyDbHelper.resetSimulatorLogs();
	}

	@PostMapping(value = "/addSimulatorLog")
	public void addSimulatorLog(@RequestBody SimulatorLog simulatorLog) {
		EnergyDbHelper.registerSimulatorLog(simulatorLog);
	}

	@GetMapping(path = "/test1")
	public @ResponseBody Iterable<Service> test1() {
		return Sapere.getInstance().test1ter(repository);
	}

	@GetMapping(path = "/test2")
	public @ResponseBody Iterable<Service> test2() {
		return Sapere.getInstance().test2(repository);
	}

	@GetMapping(path = "/test3")
	public @ResponseBody Iterable<Service> test3() {
		return Sapere.getInstance().test3(repository);
	}

	@GetMapping(path = "/test4")
	public @ResponseBody Iterable<Service> test4() {
		return Sapere.getInstance().test4(repository);
	}

	@GetMapping(path = "/test5")
	public @ResponseBody Iterable<Service> test5() {
		return Sapere.getInstance().test5(repository);
	}

	@GetMapping(path = "/test6")
	public @ResponseBody Iterable<Service> test6() {
		return Sapere.getInstance().test6(repository);
	}

	@GetMapping(path = "/testTragedyOfTheCommons")
	public @ResponseBody Iterable<Service> testTragedyOfTheCommons() {
		return Sapere.getInstance().testTragedyOfTheCommons(repository, true);
	}
}
