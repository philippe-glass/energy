package com.sapereapi.model.learning.markov;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.exception.IncompleteMatrixException;
import com.sapereapi.model.HandlingException;
import com.sapereapi.model.learning.FeaturesKey;
import com.sapereapi.model.learning.NodeStates;
import com.sapereapi.model.learning.NodeStatesTransitions;
import com.sapereapi.model.learning.VariableFeaturesKey;
import com.sapereapi.model.learning.VariableState;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.matrix.DoubleMatrix;
import com.sapereapi.util.matrix.IterationMatrix;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;
import eu.sapere.middleware.lsa.IPropertyObject;
import eu.sapere.middleware.lsa.Lsa;
import eu.sapere.middleware.node.NodeLocation;

public class TransitionMatrix implements IPropertyObject {
	private static final long serialVersionUID = 1L;
	public final static String CR = System.getProperty("line.separator"); // Carriage return
	protected VariableFeaturesKey key = null;
	protected IterationMatrix completeObsMatrix = null;
	protected IterationMatrix completeCorrectionsMatrix = null;
	protected DoubleMatrix allObsMatrix = null;
	protected DoubleMatrix allCorrectionsMatrix = null;
	protected DoubleMatrix normalizedMatrix1 = null; // without corrections
	protected DoubleMatrix normalizedMatrix2 = null; // with corrections
	//protected Integer nbOfIterations = 0;
	//protected Map<Integer, Date> mapIterationDates = null;
	//List<Integer> iterations = new ArrayList<Integer>();
	//protected Date computeDate;
	// protected boolean useCorrections = true;

	public static DoubleMatrix initBasicMatrix() {
		int statesNb = NodeStates.getNbOfStates();
		DoubleMatrix result = new DoubleMatrix(statesNb, statesNb);
		return result;
	}

	public static IterationMatrix initIterationMatrix() {
		int statesNb = NodeStates.getNbOfStates();
		IterationMatrix result = new IterationMatrix(statesNb, statesNb);
		return result;
	}

	private static double getRowSum(DoubleMatrix aMatrix, int rowIdx) {
		if (aMatrix == null) {
			return 0;
		}
		double rowSum = 0;
		if (rowIdx < aMatrix.getRowDimension()) {
			for (int colIdx = 0; colIdx < aMatrix.getColumnDimension(); colIdx++) {
				rowSum += aMatrix.get(rowIdx, colIdx);
			}
		}
		return rowSum;
	}

	public TransitionMatrix() {
		super();
	}

	public FeaturesKey getFeaturesKey() {
		if (key != null) {
			return key.getFeaturesKey();
		}
		return null;
	}

	public VariableFeaturesKey getKey() {
		return key;
	}

	public void setKey(VariableFeaturesKey key) {
		this.key = key;
	}

	public Double getNbOfObservations() {
		if(completeObsMatrix != null) {
			return this.completeObsMatrix.getSum();
		}
		return 0.0;
	}

	public Double getNbOfCorrections() {
		if(allCorrectionsMatrix != null) {
			return this.completeCorrectionsMatrix.getSum();
		}
		return 0.0;
	}

	public DoubleMatrix getAllObsMatrix() {
		return allObsMatrix;
	}

	public DoubleMatrix getAllCorrectionsMatrix() {
		return allCorrectionsMatrix;
	}

	public void setAllCorrectionsMatrix(DoubleMatrix allCorrectionsMatrix) {
		this.allCorrectionsMatrix = allCorrectionsMatrix;
	}

	public DoubleMatrix getNormalizedMatrix(boolean useCorrection) {
		return useCorrection ? normalizedMatrix2 : normalizedMatrix1;
	}

	public DoubleMatrix getNormalizedMatrix1() {
		return normalizedMatrix1;
	}

	public void setAllObsMatrix(DoubleMatrix allObsMatrix) {
		this.allObsMatrix = allObsMatrix;
	}

	public void setNormalizedMatrix1(DoubleMatrix normalizedMatrix) {
		this.normalizedMatrix1 = normalizedMatrix;
	}

	public DoubleMatrix getNormalizedMatrix2() {
		return normalizedMatrix2;
	}

	public void setNormalizedMatrix2(DoubleMatrix normalizedMatrix2) {
		this.normalizedMatrix2 = normalizedMatrix2;
	}

	public IterationMatrix getCompleteObsMatrix() {
		return completeObsMatrix;
	}

	public void setCompleteObsMatrix(IterationMatrix completeObsMatrix) {
		this.completeObsMatrix = completeObsMatrix;
	}

	public IterationMatrix getCompleteCorrectionsMatrix() {
		return completeCorrectionsMatrix;
	}

	public void setCompleteCorrectionsMatrix(IterationMatrix completeCorrectionsMatrix) {
		this.completeCorrectionsMatrix = completeCorrectionsMatrix;
	}

	public TransitionMatrix(VariableFeaturesKey _key/* , boolean _useCorrections */) {
		super();
		this.key = _key;
		// this.useCorrections = _useCorrections;
		reset();
	}

	public List<VariableState> getStatesList() {
		return NodeStates.getStatesList();
	}

	public void setValueAtIteration(int iterationNumber /*, Date iterationDate*/, int rowIdx, int columnIndx,
			double iterationObservationNb, double iterationCorrectionsNb) {
		completeObsMatrix.set(rowIdx, columnIndx, iterationNumber, iterationObservationNb);
		completeCorrectionsMatrix.set(rowIdx, columnIndx, iterationNumber, iterationCorrectionsNb);
		//aux_updateIteration(iterationDate, iterationNumber);
	}


	public void reset() {
		//this.mapIterationDates = new HashMap<Integer, Date>();
		//this.iterations = new ArrayList<Integer>();
		this.allObsMatrix = initBasicMatrix();
		this.allCorrectionsMatrix = initBasicMatrix();
		this.normalizedMatrix1 = initBasicMatrix();
		this.normalizedMatrix2 = initBasicMatrix();
		this.completeObsMatrix = initIterationMatrix();
		this.completeCorrectionsMatrix = initIterationMatrix();
	}

	public void refreshAllMatrices() {
		allObsMatrix = completeObsMatrix.generateTotalMatrix();
		if(completeCorrectionsMatrix == null) {
			completeCorrectionsMatrix = initIterationMatrix();
		}
		allCorrectionsMatrix = completeCorrectionsMatrix.generateTotalMatrix();
		normalizedMatrix1 = allObsMatrix.normalize();
		DoubleMatrix allOMatrix2 = allObsMatrix.plus(allCorrectionsMatrix);
		normalizedMatrix2 = allOMatrix2.normalize();
		//refreshNormalizedMatrix();
		//double nbOfObservations = allObsMatrix.getSum();
		//if (nbOfObservations > 0 && iterations.isEmpty()) {
		//	System.err.println("refreshAllMatrices : iterations is empty");
		//}
	}


	private boolean updateTransiationMatrix2(VariableState lastState, VariableState currentState, int ieterationId) {
		boolean result = false;
		if (lastState != null && currentState != null) {
			int fromState = lastState.getId();
			int toState = currentState.getId();
			int rowIdx = fromState - 1;
			int columnIdx = toState - 1;
			result = completeObsMatrix.incrementObsNumber(rowIdx, columnIdx, ieterationId);
		}
		return result;
	}



	public boolean updateMatrices2(Date registerDate, Integer iterationNumber, NodeStatesTransitions transition,
			boolean refreshAll, AbstractLogger logger) {
		boolean result = false;
		try {
			// this.computeDate = registerDate;
			String variable = key.getVariable();
			if (transition.hasTransition(variable)) {
				VariableState lastState = transition.getLastState(variable);
				VariableState currentState = transition.getCurrentState(variable);
				boolean result2 = updateTransiationMatrix2(lastState, currentState, iterationNumber);
				result = result || result2;
				if (lastState.getId().equals(currentState.getId()) && currentState.getId() >= 4) {
					logger.warning("updateMatrices : observation on stationary transition "
							+ variable + " " + currentState.getLabel());
				}
				if(refreshAll) {
					this.refreshAllMatrices();
				}
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return result;
	}

	public double getObsRowSum(int rowIdx) {
		return getRowSum(allObsMatrix, rowIdx);
	}

	public double getObsAndCorrectionsSum(int rowIdx) {
		return getRowSum(allObsMatrix, rowIdx) + getRowSum(allCorrectionsMatrix, rowIdx);
	}

	public boolean isComplete() {
		if(completeObsMatrix == null) {
			return false;
		}
		return this.completeObsMatrix.isComplete();
	}

	private int aux_choseRandomRow(double[] satesProba) {
		double random = Math.random();
		double sumProba = 0;
		for (int rowIdx = 0; rowIdx < satesProba.length; rowIdx++) {
			sumProba += satesProba[rowIdx];
			if (random <= sumProba) {
				return rowIdx;
			}
		}
		return -1;
	}

	public int completeMatrix(Integer interationNumber) {
		int nbCompletion = 0;
		double total = completeObsMatrix.getSum();
		if (total > 0) {
			double[] rowSums = completeObsMatrix.getRowSums();
			double[] satesProba = new double[rowSums.length];
			for (int rowIdx = 0; rowIdx < rowSums.length; rowIdx++) {
				satesProba[rowIdx] = rowSums[rowIdx] / total;
			}
			for (int rowIdx = 0; rowIdx < rowSums.length; rowIdx++) {
				if (rowSums[rowIdx] == 0.0) {
					int targetStateIdx = aux_choseRandomRow(satesProba);
					if (targetStateIdx >= 0) {
						completeObsMatrix.incrementObsNumber(rowIdx, targetStateIdx, interationNumber);
						nbCompletion++;
					}
				}
			}
		}
		refreshAllMatrices();
		return nbCompletion;
	}

	public boolean checkCompletion(DoubleMatrix predictionRow) throws HandlingException {
		// int nbCol = predictionRow.getColumnDimension();
		double[] predictionArray = predictionRow.getRowPackedCopy();
		for (int rowIdx = 0; rowIdx < predictionArray.length; rowIdx++) {
			double nextProbabilityItem = predictionArray[rowIdx];
			if (nextProbabilityItem > 0) {
				// check the sum at the corresponding transition matrix row
				double matrixSum = normalizedMatrix1.getRowSum(rowIdx); // SapereUtil.getSum(normalizedMatrix1.getMatrix(rowIdx,
																		// rowIdx, 0, nbCol - 1));
				String sRow = "S" + (rowIdx + 1);
				String variable2 = "\"" + key.getVariable() + "\"";
				if (matrixSum == 0) {
					throw new IncompleteMatrixException("Cannot compute prediction of " + variable2
							+ " : Transition matrix row " + sRow + " is empty for key : " + key.toString());
				}
				if (Math.abs(matrixSum - 1.0) > 0.0001) {
					throw new HandlingException("Cannot compute prediction : Sum of transition matrix row " + sRow
							+ " is not equals to 1 for key : " + key.toString());
				}
			}
		}
		return true;
	}

	@Override
	public TransitionMatrix copyForLSA(AbstractLogger logger) {
		TransitionMatrix result = new TransitionMatrix();
		result.setKey(key);
		result.setCompleteObsMatrix(completeObsMatrix.copy());
		result.refreshAllMatrices();
		return result;
	}
	public TransitionMatrix copy() {
		TransitionMatrix result = new TransitionMatrix();
		result.setKey(key.clone());
		result.setCompleteObsMatrix(completeObsMatrix.clone());
		result.setCompleteCorrectionsMatrix(completeCorrectionsMatrix);
		return result;
	}

	@Override
	public void completeInvolvedLocations(Lsa bondedLsa, Map<String, NodeLocation> mapNodeLocation, AbstractLogger logger) {
		// TODO Auto-generated method stub
	}

	@Override
	public List<NodeLocation> retrieveInvolvedLocations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		// result.append("{");
		if (true) {
			if (key != null) {
				// result.append("variable ").append(key.getVariable());
			}
			// result.append(" time
			// ").append(UtilDates.format_time.format(this.computeDate));
			/*
			 * result.append(CR).append("cumulative obs : ").append(this.allObsMatrix.
			 * format3d()));
			 * result.append(CR).append("cumulative corrections : ").append(this.
			 * allCorrectionsMatrix.format3d()));
			 * result.append(CR).append("normalized matrices without corrections : ").append
			 * (this.normalizedMatrix1.format3d()));
			 */
			List<Integer> iterations = completeObsMatrix.generateIterations();
			result.append("")
					.append("detail:")
					//.append("(iterations:").append(iterations).append(")").append(CR)
					.append(this.completeObsMatrix.format3d(iterations)).append(CR)
					.append("sum:").append(CR).append(this.allObsMatrix.format3d()).append(CR).append("normlized:")
					.append(CR).append(this.normalizedMatrix2.format3d());
		}
		return result.toString();
	}

	/*
	 * public static BasicMatrix auxApplyDivisor2(BasicMatrix matrix, double
	 * divisor) { BasicMatrix result = matrix.copy(); for(int rowIdx = 0; rowIdx <
	 * matrix.getRowDimension(); rowIdx++) { for(int colIdx = 0; colIdx <
	 * matrix.getColumnDimension(); colIdx++) { double toSet =
	 * Math.round(matrix.get(rowIdx, colIdx) / divisor); result.set(rowIdx, colIdx,
	 * toSet); } } return result; }
	 */

	/*
	 * public void auxAddContent(TransitionMatrix other) { nbOfObservations+=
	 * other.getNbOfObservations(); nbOfCorrections+=other.getNbOfCorrections();
	 * nbOfIterations+=other.getNbOfIterations(); iterObsMatrix =
	 * iterObsMatrix.plus(other.getIterObsMatrix()); iterCorrectionsMatrix =
	 * iterCorrectionsMatrix.plus(other.getIterCorrectionsMatrix()); allObsMatrix =
	 * allObsMatrix.plus(other.getAllObsMatrix()); allCorrectionsMatrix =
	 * allCorrectionsMatrix.plus(other.getAllCorrectionsMatrix());
	 * //normalizedMatrix1.plus(other.getNormalizedMatrix1());
	 * //normalizedMatrix2.plus(other.getNormalizedMatrix2()); for(int rowIdx = 0;
	 * rowIdx < allObsMatrix.getRowDimension(); rowIdx++) {
	 * refreshNormalizedMatrix(rowIdx); } }
	 * 
	 * public void auxApplyDivisor(double divisor) { nbOfObservations= (int)
	 * (nbOfObservations/divisor); nbOfCorrections = (int)
	 * (nbOfCorrections/divisor); nbOfIterations = (int) (nbOfIterations/divisor);
	 * iterObsMatrix = auxApplyDivisor2(iterObsMatrix, divisor);
	 * iterCorrectionsMatrix = auxApplyDivisor2(iterCorrectionsMatrix, divisor);
	 * allObsMatrix = auxApplyDivisor2(allObsMatrix, divisor); allCorrectionsMatrix
	 * = auxApplyDivisor2(allCorrectionsMatrix, divisor); for(int rowIdx = 0; rowIdx
	 * < allObsMatrix.getRowDimension(); rowIdx++) {
	 * refreshNormalizedMatrix(rowIdx); } }
	 */

	public boolean copyFromOther(TransitionMatrix other) {
		String sOther = other.getCompleteObsMatrix().zip();
		String sThis = completeObsMatrix.zip();
		IterationMatrix otherCompleteObsMatrix = other.getCompleteObsMatrix();
		completeObsMatrix = otherCompleteObsMatrix.copy();
		refreshAllMatrices();
		boolean hasChanged = !sOther.equals(sThis);
		return hasChanged;
	}

	/*
	 * public static double auxComputeSum(Matrix aMatrix) { double result = 0;
	 * for(double doAdd : aMatrix.getColumnPackedCopy()) { result+=doAdd; } return
	 * result; }
	 */
/*
	public TransitionMatrix aggregate1(String operator, Map<String, IAggregateable> mapObjects,
			AgentAuthentication agentAuthentication, AbstractLogger logger) {
		return aggregate2(operator, mapObjects, agentAuthentication, logger);
	}
*/
	public static TransitionMatrix auxAggregate(Map<String, TransitionMatrix> mapObjects
			, Map<String, Double> weightsTable
			, AgentAuthentication agentAuthentication
			, AbstractLogger logger
			, boolean refreshALlMatrices) {
		int debugLevel = 0;
		TransitionMatrix result = new TransitionMatrix();
		result.reset();
		Map<String, IterationMatrix> mapCompleteMatrix = new HashMap<String, IterationMatrix>();
		VariableFeaturesKey lastKey = null;
		for(String nextNode : mapObjects.keySet()) {
			TransitionMatrix nextObj = mapObjects.get(nextNode);
			if (nextObj instanceof TransitionMatrix) {
				TransitionMatrix trMatrix = (TransitionMatrix) nextObj;
				mapCompleteMatrix.put(nextNode, trMatrix.getCompleteObsMatrix());
				lastKey = trMatrix.getKey();
			}
		}
		if (lastKey != null && "produced".equals(lastKey.getVariable())) {
			debugLevel = 0;
		}
		if (debugLevel >= 10) {
			// BasicMatrix avgMatrix = auxComputeAvgMatrix(listAllObsMatrix);
			// BasicMatrix sumMatrix = auxComputeSumMatrix(listAllObsMatrix);

			int i = 0;
			for (IterationMatrix nextCompleteMatrix : mapCompleteMatrix.values()) {
				i++;
				if (nextCompleteMatrix instanceof IterationMatrix) {
					DoubleMatrix sumMatrix = ((IterationMatrix) nextCompleteMatrix).generateTotalMatrix();
					if (debugLevel >= 10) {
						logger.info("TransitionMatrix.aggregate for debug : next sumMatrix (" + i
								+ ") = " + SapereUtil.CR + sumMatrix.format3d());
					}
				}
			}
		}
		FeaturesKey lastFeaturesKey = lastKey.getFeaturesKey();
		result.setKey(new VariableFeaturesKey(null, null, lastKey.getVariable(), lastFeaturesKey));
		IterationMatrix aggregatedIM = IterationMatrix.auxAggregate(mapCompleteMatrix, weightsTable, agentAuthentication, logger);
		// result.setCompleteObsMatrix(IterationMatrix.auxComputeAvgIterationMatrix(listCompleteMatrix));
		result.setCompleteObsMatrix(aggregatedIM);
		//result.setIterations(iterations);
		//result.setMapIterationDates(mapItertaionDates);
		if(refreshALlMatrices) {
			result.refreshAllMatrices();
		}
		/*
		 * Double avgNbOfIterations = auxComputeAvg(ListNbOfIterations);
		 * if(avgNbOfIterations != null) {
		 * result.setNbOfIterations(avgNbOfIterations.intValue()); }
		 */
		if (debugLevel >= 10) {
			DoubleMatrix sumMatrix = aggregatedIM.generateTotalMatrix();
			logger.info("TransitionMatrix.aggregate for debug : nbMatrix = "
					+ mapCompleteMatrix.size() + ", sumMatrix = " + SapereUtil.CR + sumMatrix.format3d());
		}
		return result;
	}

}
