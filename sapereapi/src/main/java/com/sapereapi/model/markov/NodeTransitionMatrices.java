package com.sapereapi.model.markov;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.prediction.PredictionContext;
import com.sapereapi.util.UtilDates;

import Jama.Matrix;
import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.lsa.values.IAggregateable;

public class NodeTransitionMatrices implements Serializable, IAggregateable {
	private static final long serialVersionUID = 447701L;
	public final static String CR = System.getProperty("line.separator"); // Carriage return
	private String learningAgentName = null;
	protected String variables[] = {};
	protected String location;
	protected String scenario;
	protected MarkovTimeWindow timeWindow;
	protected Map<String, TransitionMatrix> mapMatrices = new HashMap<String, TransitionMatrix>();
	protected Date computeDate;
	//protected boolean useCorrections;

	public static Matrix initTransitionMatrix() {
		int statesNb = NodeMarkovStates.getNbOfStates();
		Matrix result = new Matrix(statesNb, statesNb);
		return result;
	}

	public NodeTransitionMatrices() {
		super();
	}

	public boolean hasMatrixId(String variable) {
		if(mapMatrices.containsKey(variable)) {
			Long id = (this.mapMatrices.get(variable)).getKey().getId();
			return (id != null);
		}
		return false;
	}

	public Long getMatrixId(String variable) {
		if(mapMatrices.containsKey(variable)) {
			return (this.mapMatrices.get(variable)).getKey().getId();
		}
		return null;
	}

	public void setMatrixKey(String variable, TransitionMatrixKey trKey) {
		if(mapMatrices.containsKey(variable)) {
			TransitionMatrix trMatrix = this.mapMatrices.get(variable);
			trMatrix.setKey(trKey);
		}
	}

	public void setLearningAgentName(String learningAgentName) {
		this.learningAgentName = learningAgentName;
	}

	public void setVariables(String[] variables) {
		this.variables = variables;
	}

	public void setTimeWindow(MarkovTimeWindow timeWindow) {
		this.timeWindow = timeWindow;
	}

	public Map<String, TransitionMatrix> getMapMatrices() {
		return mapMatrices;
	}

	public void setMapMatrices(Map<String, TransitionMatrix> mapMatrices) {
		this.mapMatrices = mapMatrices;
	}

	public NodeTransitionMatrices(PredictionContext predictionContext, String[] _variables, MarkovTimeWindow aTimeWindow) {
		super();
		this.learningAgentName = predictionContext.getLearningAgentName();
		this.variables = _variables;
		this.timeWindow = aTimeWindow;
		this.location = predictionContext.getLocation();
		this.scenario = predictionContext.getScenario();
		//this.useCorrections = predictionContext.isUseCorrections();
		reset(predictionContext);
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getScenario() {
		return scenario;
	}

	public void setScenario(String scenario) {
		this.scenario = scenario;
	}

	public TransitionMatrix getTransitionMatrix(String variable) {
		if(mapMatrices.containsKey(variable)) {
			return this.mapMatrices.get(variable);
		}
		return null;
	}

	public Matrix getAllObsMatrice(String variable) {
		if(mapMatrices.containsKey(variable)) {
			TransitionMatrix trMatrix = this.mapMatrices.get(variable);
			return trMatrix.getAllObsMatrix();
		}
		return null;
	}

	public Matrix getIterObsMatrice(String variable) {
		if(mapMatrices.containsKey(variable)) {
			TransitionMatrix trMatrix = this.mapMatrices.get(variable);
			return trMatrix.getIterObsMatrix();
		}
		return null;
	}

	public Matrix getIterCorrectionsMatrice(String variable) {
		if(mapMatrices.containsKey(variable)) {
			TransitionMatrix trMatrix = this.mapMatrices.get(variable);
			return trMatrix.getIterCorrectionsMatrix();
		}
		return null;
	}

	public Matrix getNormalizedMatrix(String variable, boolean useCorrections) {
		if(mapMatrices.containsKey(variable)) {
			TransitionMatrix trMatrix = this.mapMatrices.get(variable);
			return trMatrix.getNormalizedMatrix(useCorrections);
		}
		return null;
	}

	public Integer getNbOfObservations(String variable) {
		if(mapMatrices.containsKey(variable)) {
			TransitionMatrix trMatrix = this.mapMatrices.get(variable);
			return trMatrix.getNbOfObservations();
		}
		return null;
	}

	public Integer getNbOfIterations(String variable) {
		if(mapMatrices.containsKey(variable)) {
			TransitionMatrix trMatrix = this.mapMatrices.get(variable);
			return trMatrix.getNbOfIterations();
		}
		return null;
	}

	public void setNbOfIterations(String variable, Integer nbOfIteraitons) {
		if(mapMatrices.containsKey(variable)) {
			TransitionMatrix trMatrix = this.mapMatrices.get(variable);
			trMatrix.setNbOfIterations(nbOfIteraitons);
		}
	}

	public List<MarkovState> getStatesList() {
		return NodeMarkovStates.getStatesList();
	}

	public void setValue(String variable, int rowIdx, int columnIndx
			, double iterationObservationNb, double iterationCorrectionNb
			, double nbOfObservations, double nbOfCorrections) {
		if(mapMatrices.containsKey(variable)) {
			TransitionMatrix trMatrix = mapMatrices.get(variable);
			trMatrix.setValue(rowIdx, columnIndx, iterationObservationNb, iterationCorrectionNb, nbOfObservations, nbOfCorrections);
		}
	}

	private TransitionMatrixKey generateTransitionMatrixKey(PredictionContext context, String variable) {
		return context.getTransitionMatrixKey(timeWindow, variable);
	}

	public void reset(PredictionContext context) {
		mapMatrices.clear();
		for (String variable : this.variables) {
			TransitionMatrixKey trMatricKey = generateTransitionMatrixKey(context, variable);
			mapMatrices.put(variable, new TransitionMatrix(learningAgentName, trMatricKey));
		}
	}

	public String[] getVariables() {
		return variables;
	}


	public boolean updateMatrices(Date registerDate, NodeMarkovTransitions transition) {
		boolean result = false;
		try {
			this.computeDate = registerDate;
			for (String variable : mapMatrices.keySet()) {
				if(transition.hasTransition(variable)) {
					MarkovState lastState = transition.getLastState(variable);
					MarkovState currentState = transition.getCurrentState(variable);
					TransitionMatrix trMatrix = mapMatrices.get(variable);
					boolean result2 = trMatrix.updateMatrices(registerDate, transition);
					result = result || result2;
					if(lastState.getId().equals(currentState.getId()) && currentState.getId() >=4) {
						SapereLogger.getInstance().warning("updateMatrices : observation on stationary transition " + variable + " " +  currentState.getLabel());
					}
				}
			}
		} catch (Exception e) {
			SapereLogger.getInstance().error(e);
		}
		return result;
	}



	public String matrix2str(Matrix aMatrix) {
		// Matrix aMatrixNorm = normalize(aMatrix);
		StringBuffer result = new StringBuffer();
		for (int rowIdx = 0; rowIdx < aMatrix.getRowDimension(); rowIdx++) {
			String sep = "";
			for (int colIdx = 0; colIdx < aMatrix.getColumnDimension(); colIdx++) {
				result.append(sep);
				result.append(UtilDates.df.format(aMatrix.get(rowIdx, colIdx)));
				sep = " ";
			}
			result.append(CR);
		}
		return result.toString();
	}

	public MarkovTimeWindow getTimeWindow() {
		return timeWindow;
	}

	public Date getComputeDate() {
		return computeDate;
	}

	public void setComputeDate(Date computeDate) {
		this.computeDate = computeDate;
	}

	public int getTimeWindowId() {
		return this.timeWindow.getId();
	}

	public String getLearningAgentName() {
		return learningAgentName;
	}

	public boolean isComplete(String variable) {
		if(mapMatrices.containsKey(variable)) {
			TransitionMatrix trMatrix = mapMatrices.get(variable);
			return trMatrix.isComplete();
		}
		return false;
	}

	public boolean isComplete() {
		for(String variable : mapMatrices.keySet()) {
			TransitionMatrix trMatrix = mapMatrices.get(variable);
			if(!trMatrix.isComplete()) {
				return false;
			}
		}
		return true;
	}

	public double getObsAndCorrectionsSum(String variable, int rowIdx) {
		if(mapMatrices.containsKey(variable)) {
			TransitionMatrix trMatrix = mapMatrices.get(variable);
			return trMatrix.getObsAndCorrectionsSum(rowIdx);
		}
		return 0;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		if (computeDate == null)  {
			SapereLogger.getInstance().error("NodeTransitionMatrices.toString() : computeDate is null");
		}
		if (computeDate != null) {
			result.append("time ").append(UtilDates.format_time.format(this.computeDate));
			result.append(CR).append("");
			for (String variable : mapMatrices.keySet()) {
				TransitionMatrix trMatrix = mapMatrices.get(variable);
				result.append(CR).append(variable).append(":").append(CR)
						.append(trMatrix);
			}
			result.append(CR).append("normalized matrices");
		}
		return result.toString();
	}

	public boolean canAggregate(NodeTransitionMatrices other) {
		if(
				timeWindow.getId() == other.getTimeWindowId()
			&& scenario.equals(other.getScenario())
			&& variables.length == other.getVariables().length
			&& mapMatrices.size() == other.getMapMatrices().size())
		{
			// check variables
			for (String variable : mapMatrices.keySet()) {
				if(!other.getMapMatrices().containsKey(variable)) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public NodeTransitionMatrices aggregate(String operator, List<IAggregateable> listObjects,
			AgentAuthentication agentAuthentication) {
	if("avg".equals(operator)) {
			List<NodeTransitionMatrices> listNodeTransitionMatrices = new ArrayList<>();
			for(Object nextObj : listObjects) {
				if(nextObj instanceof NodeTransitionMatrices) {
					NodeTransitionMatrices nextNodeTransitionMatrices = (NodeTransitionMatrices) nextObj;
					if(	canAggregate(nextNodeTransitionMatrices)) {
						listNodeTransitionMatrices.add(nextNodeTransitionMatrices);
					}
				}
			}
			NodeTransitionMatrices result = new NodeTransitionMatrices();
			result.setComputeDate(new Date());
			result.setScenario(scenario);
			result.setVariables(variables);
			for(String variable : mapMatrices.keySet()) {
				TransitionMatrix transitionMatrix =  mapMatrices.get(variable);
				List<IAggregateable> listTransitionMatrix = new ArrayList<>();
				for(NodeTransitionMatrices nextNodeTransitionMatrices : listNodeTransitionMatrices) {
					listTransitionMatrix.add(nextNodeTransitionMatrices.getTransitionMatrix(variable));
				}
				TransitionMatrix aggregatedTrMatrix = transitionMatrix.aggregate(operator, listTransitionMatrix, agentAuthentication);
				result.getMapMatrices().put(variable, aggregatedTrMatrix);
			}
			return result;
		}
		return null;
	}

}
