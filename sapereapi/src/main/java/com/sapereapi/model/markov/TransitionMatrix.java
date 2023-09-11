package com.sapereapi.model.markov;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sapereapi.exception.IncompleteMatrixException;
import com.sapereapi.log.SapereLogger;
import com.sapereapi.util.SapereUtil;
import com.sapereapi.util.UtilDates;

import Jama.Matrix;
import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.lsa.values.IAggregateable;

public class TransitionMatrix implements Serializable, IAggregateable {
	private static final long serialVersionUID = 1L;
	public final static String CR = System.getProperty("line.separator"); // Carriage return
	protected TransitionMatrixKey key = null;
	protected String learningAgent = null;
	protected Integer nbOfObservations = 0;
	protected Integer nbOfCorrections = 0;
	protected Integer nbOfIterations = 0;
	protected Matrix iterObsMatrix = null;
	protected Matrix iterCorrectionsMatrix = null;
	protected Matrix allObsMatrix = null;
	protected Matrix allCorrectionsMatrix = null;
	protected Matrix normalizedMatrix1 = null;	// without corrections
	protected Matrix normalizedMatrix2 = null;	// with corrections
	protected Date computeDate;
	//protected boolean useCorrections = true;

	public static Matrix initTransitionMatrix() {
		int statesNb = NodeMarkovStates.getNbOfStates();
		Matrix result = new Matrix(statesNb, statesNb);
		return result;
	}

	private static double getRowSum(Matrix aMatrix, int rowIdx) {
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

	public String getLearningAgent() {
		return learningAgent;
	}

	public void setLearningAgent(String learningAgent) {
		this.learningAgent = learningAgent;
	}

	public TransitionMatrixKey getKey() {
		return key;
	}

	public void setKey(TransitionMatrixKey key) {
		this.key = key;
	}

	public Integer getNbOfObservations() {
		return nbOfObservations;
	}

	public Integer getNbOfIterations() {
		return nbOfIterations;
	}

	public Integer getNbOfCorrections() {
		return nbOfCorrections;
	}

	public void setNbOfCorrections(Integer nbOfCorrections) {
		this.nbOfCorrections = nbOfCorrections;
	}

	public void setNbOfIterations(Integer nbOfIterations) {
		this.nbOfIterations = nbOfIterations;
	}

	public Matrix getIterObsMatrix() {
		return iterObsMatrix;
	}

	public Matrix getIterCorrectionsMatrix() {
		return iterCorrectionsMatrix;
	}

	public Matrix getAllObsMatrix() {
		return allObsMatrix;
	}

	public Matrix getAllCorrectionsMatrix() {
		return allCorrectionsMatrix;
	}

	public void setAllCorrectionsMatrix(Matrix allCorrectionsMatrix) {
		this.allCorrectionsMatrix = allCorrectionsMatrix;
	}

	public Matrix getNormalizedMatrix(boolean useCorrection) {
		return useCorrection ? normalizedMatrix2 : normalizedMatrix1;
	}

	public Matrix getNormalizedMatrix1() {
		return normalizedMatrix1;
	}

	public void setNbOfObservations(Integer nbOfObservations) {
		this.nbOfObservations = nbOfObservations;
	}

	public void setIterObsMatrix(Matrix iterObsMatrix) {
		this.iterObsMatrix = iterObsMatrix;
	}

	public void setIterCorrectionsMatrix(Matrix iterCorrectionsMatrix) {
		this.iterCorrectionsMatrix = iterCorrectionsMatrix;
	}

	public void setAllObsMatrix(Matrix allObsMatrix) {
		this.allObsMatrix = allObsMatrix;
	}

	public void setNormalizedMatrix1(Matrix normalizedMatrix) {
		this.normalizedMatrix1 = normalizedMatrix;
	}

	public Matrix getNormalizedMatrix2() {
		return normalizedMatrix2;
	}

	public void setNormalizedMatrix2(Matrix normalizedMatrix2) {
		this.normalizedMatrix2 = normalizedMatrix2;
	}

	public TransitionMatrix(String _learningAgent, TransitionMatrixKey _key/*, boolean _useCorrections*/) {
		super();
		this.key = _key;
		this.learningAgent = _learningAgent;
		//this.useCorrections = _useCorrections;
		reset();
	}

	public List<MarkovState> getStatesList() {
		return NodeMarkovStates.getStatesList();
	}

	public void setValue(int rowIdx, int columnIndx, double iterationObservationNb, double iterationCorrectionNb,
			double cellNbOfObservations, double cellNbOfCorrections) {
		iterObsMatrix.set(rowIdx, columnIndx, iterationObservationNb);
		iterCorrectionsMatrix.set(rowIdx, columnIndx, iterationCorrectionNb);
		allObsMatrix.set(rowIdx, columnIndx, cellNbOfObservations);
		allCorrectionsMatrix.set(rowIdx, columnIndx, cellNbOfCorrections);
		refreshNormalizedMatrix(rowIdx);
		nbOfObservations = nbOfObservations + (Double.valueOf(cellNbOfObservations)).intValue();
		nbOfCorrections = nbOfCorrections +  (Double.valueOf(cellNbOfCorrections)).intValue();
	}

	public void reset() {
		this.nbOfObservations = 0;
		this.nbOfCorrections = 0;
		this.nbOfIterations = 0;
		iterObsMatrix = initTransitionMatrix();
		iterCorrectionsMatrix = initTransitionMatrix();
		allObsMatrix = initTransitionMatrix();
		allCorrectionsMatrix = initTransitionMatrix();
		normalizedMatrix1 = initTransitionMatrix();
		normalizedMatrix2 = initTransitionMatrix();
	}

	private boolean updateTransiationMatrix(MarkovState lastState, MarkovState currentState) {
		boolean result = false;
		if (lastState != null && currentState != null) {
			int fromState = lastState.getId();
			int toState = currentState.getId();
			int rowIdx = fromState - 1;
			int columnIdx = toState - 1;
			if (rowIdx < iterObsMatrix.getRowDimension() && columnIdx < iterObsMatrix.getColumnDimension()) {
				double currentValue = iterObsMatrix.get(rowIdx, columnIdx);
				iterObsMatrix.set(rowIdx, columnIdx, 1 + currentValue);
			}
			if (rowIdx < allObsMatrix.getRowDimension() && columnIdx < allObsMatrix.getColumnDimension()) {
				// Increment the number of transitions of state indx i to state index j
				double currentValue = allObsMatrix.get(rowIdx, columnIdx);
				allObsMatrix.set(rowIdx, columnIdx, 1 + currentValue);
				refreshNormalizedMatrix(rowIdx);
				result = true;
			}
		}
		return result;
	}

	public void refreshNormalizedMatrix() {
		for(int rowIdx = 0; rowIdx < allObsMatrix.getRowDimension(); rowIdx++) {
			refreshNormalizedMatrix(rowIdx);
		}
	}

	public void refreshNormalizedMatrix(int rowIdx) {
		// Refresh all item of row number rowItem
		// refresh of first normalized matrix (without corrections)
		double rowSum1 = getRowSum(allObsMatrix, rowIdx);
		for (int colIdx = 0; colIdx < normalizedMatrix1.getRowDimension(); colIdx++) {
			double cellObsNumber1 = allObsMatrix.get(rowIdx, colIdx);
			// To normalize : divide each item by row sum
			normalizedMatrix1.set(rowIdx, colIdx, rowSum1 == 0 ? 0 : cellObsNumber1 / rowSum1);
		}
		// refresh of second normalized matrix (with corrections)
		double rowSum2 = rowSum1 + getRowSum(allCorrectionsMatrix, rowIdx);
		for (int colIdx = 0; colIdx < normalizedMatrix2.getRowDimension(); colIdx++) {
			double cellObsNumber2 = allObsMatrix.get(rowIdx, colIdx) + allCorrectionsMatrix.get(rowIdx, colIdx);
			// To normalize : divide each item by row sum
			normalizedMatrix2.set(rowIdx, colIdx, rowSum2 == 0 ? 0 : cellObsNumber2 / rowSum2);
		}
	}

	public boolean updateMatrices(Date registerDate, NodeMarkovTransitions transition) {
		boolean result = false;
		try {
			this.computeDate = registerDate;
			String variable = key.getVariable();
			if (transition.hasTransition(variable)) {
				MarkovState lastState = transition.getLastState(variable);
				MarkovState currentState = transition.getCurrentState(variable);
				boolean result2 = updateTransiationMatrix(lastState, currentState);
				result = result || result2;
				if (lastState.getId().equals(currentState.getId()) && currentState.getId() >= 4) {
					SapereLogger.getInstance().warning("updateMatrices : observation on stationary transition "
							+ variable + " " + currentState.getLabel());
				}
				this.nbOfObservations++;
			}
		} catch (Exception e) {
			SapereLogger.getInstance().error(e);
		}
		return result;
	}

	public double getObsRowSum(int rowIdx) {
		return getRowSum(allObsMatrix, rowIdx);
	}

	public double getObsAndCorrectionsSum(int rowIdx) {
		return getRowSum(allObsMatrix, rowIdx) + getRowSum(allCorrectionsMatrix, rowIdx);
	}

	public static Matrix normalize(Matrix aMatrix) {
		Matrix result = new Matrix(aMatrix.getRowDimension(), aMatrix.getColumnDimension());
		for (int rowIdx = 0; rowIdx < aMatrix.getRowDimension(); rowIdx++) {
			// Compute row sum
			double rowSum = getRowSum(aMatrix, rowIdx);
			for (int colIdx = 0; colIdx < aMatrix.getRowDimension(); colIdx++) {
				// To normalize : divide each item by row sum
				result.set(rowIdx, colIdx, rowSum == 0 ? 0 : aMatrix.get(rowIdx, colIdx) / rowSum);
			}
		}
		return result;
	}

	public String matrix2str(Matrix aMatrix) {
		// Matrix aMatrixNorm = normalize(aMatrix);
		StringBuffer result = new StringBuffer();
		result.append("[");
		String sep1="";
		for (int rowIdx = 0; rowIdx < aMatrix.getRowDimension(); rowIdx++) {
			result.append(sep1);
			result.append("[");
			String cellSeparator = "";
			for (int colIdx = 0; colIdx < aMatrix.getColumnDimension(); colIdx++) {
				double value = aMatrix.get(rowIdx, colIdx);
				if(true || value >0 ) {
					result.append(cellSeparator);
					//MarkovState sIn = NodeMarkovStates.getById(1+rowIdx);
					//MarkovState sMut =  NodeMarkovStates.getById(1+colIdx);
					//result.append(sIn).append("->").append(sMut).append(":");
					result.append(UtilDates.df3.format(value));
					cellSeparator = "   ";
				}
			}
			result.append("]");
			sep1 = CR;
			//result.append(CR);
		}
		result.append("]");
		return result.toString();
	}

	public Date getComputeDate() {
		return computeDate;
	}

	public void setComputeDate(Date computeDate) {
		this.computeDate = computeDate;
	}

	public boolean isComplete() {
		int nbCol = normalizedMatrix1.getColumnDimension();
		for (int rowIdx = 0; rowIdx < normalizedMatrix1.getRowDimension(); rowIdx++) {
			double matrixSum = SapereUtil.getSum(normalizedMatrix1.getMatrix(rowIdx, rowIdx, 0, nbCol - 1));
			if (matrixSum == 0) {
				return false;
			}
		}
		return true;
	}

	public boolean checkCompletion(Matrix predictionRow) throws Exception {
		int nbCol = predictionRow.getColumnDimension();
		double[] predictionArray = predictionRow.getRowPackedCopy();
		for (int rowIdx = 0; rowIdx < predictionArray.length; rowIdx++) {
			double nextProbabilityItem = predictionArray[rowIdx];
			if (nextProbabilityItem > 0) {
				// check the sum at the corresponding transition matrix row
				double matrixSum = SapereUtil.getSum(normalizedMatrix1.getMatrix(rowIdx, rowIdx, 0, nbCol - 1));
				String sRow = "S" + (rowIdx + 1);
				String variable2 = "\"" + key.getVariable() + "\"";
				if (matrixSum == 0) {
					throw new IncompleteMatrixException("Cannot compute prediction of " + variable2
							+ " : Transition matrix row " + sRow + " is empty for key : " + key.toString());
				}
				if (Math.abs(matrixSum - 1.0) > 0.0001) {
					throw new Exception("Cannot compute prediction : Sum of transition matrix row " + sRow
							+ " is not equals to 1 for key : " + key.toString());
				}
			}
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		//result.append("{");
		if (computeDate != null) {
			if(key != null) {
				//result.append("variable ").append(key.getVariable());
			}
			//result.append(" time ").append(UtilDates.format_time.format(this.computeDate));
			/*
			result.append(CR).append("cumulative obs : ").append(matrix2str(this.allObsMatrix));
			result.append(CR).append("cumulative corrections : ").append(matrix2str(this.allCorrectionsMatrix));
			result.append(CR).append("normalized matrices without corrections : ").append(matrix2str(this.normalizedMatrix1));
			*/
			result.append("")
				//.append("(").append("normMatrix) : ")
				.append(matrix2str(this.normalizedMatrix2));
			/*
			result.append(CR).append("Obs matrix:").append(CR)
				.append(matrix2str(this.allObsMatrix));
			result.append(CR).append("Corr matrix:").append(CR)
			.append(matrix2str(this.allCorrectionsMatrix));
			*/

		}
		//result.append("}");
		return result.toString();
	}

	public static Matrix auxApplyDivisor2(Matrix matrix, double divisor) {
		Matrix result = matrix.copy();
		for(int rowIdx = 0; rowIdx < matrix.getRowDimension(); rowIdx++) {
			for(int colIdx = 0; colIdx < matrix.getColumnDimension(); colIdx++) {
				double toSet = Math.round(matrix.get(rowIdx, colIdx) / divisor);
				result.set(rowIdx, colIdx, toSet);
			}
		}
		return result;
	}

	/*
	public void auxAddContent(TransitionMatrix other) {
		nbOfObservations+= other.getNbOfObservations();
		nbOfCorrections+=other.getNbOfCorrections();
		nbOfIterations+=other.getNbOfIterations();
		iterObsMatrix = iterObsMatrix.plus(other.getIterObsMatrix());
		iterCorrectionsMatrix = iterCorrectionsMatrix.plus(other.getIterCorrectionsMatrix());
		allObsMatrix = allObsMatrix.plus(other.getAllObsMatrix());
		allCorrectionsMatrix = allCorrectionsMatrix.plus(other.getAllCorrectionsMatrix());
		//normalizedMatrix1.plus(other.getNormalizedMatrix1());
		//normalizedMatrix2.plus(other.getNormalizedMatrix2());
		for(int rowIdx = 0; rowIdx < allObsMatrix.getRowDimension(); rowIdx++) {
			refreshNormalizedMatrix(rowIdx);
		}
	}

	public void auxApplyDivisor(double divisor) {
		nbOfObservations= (int) (nbOfObservations/divisor);
		nbOfCorrections =  (int) (nbOfCorrections/divisor);
		nbOfIterations = (int) (nbOfIterations/divisor);
		iterObsMatrix = auxApplyDivisor2(iterObsMatrix, divisor);
		iterCorrectionsMatrix = auxApplyDivisor2(iterCorrectionsMatrix, divisor);
		allObsMatrix = auxApplyDivisor2(allObsMatrix, divisor);
		allCorrectionsMatrix = auxApplyDivisor2(allCorrectionsMatrix, divisor);
		for(int rowIdx = 0; rowIdx < allObsMatrix.getRowDimension(); rowIdx++) {
			refreshNormalizedMatrix(rowIdx);
		}
	}*/

	private static Matrix auxComputeSumMatrix(List<Matrix> listMatrix) {
		int statesNb = NodeMarkovStates.getNbOfStates();
		Matrix sumMatrix = new Matrix(statesNb, statesNb);
		for(Matrix nextMatrix : listMatrix) {
			sumMatrix = sumMatrix.plus(nextMatrix);
		}
		return sumMatrix;
	}

	private static Matrix auxComputeAvgMatrix(List<Matrix> listMatrix) {
		Matrix sumMatrix = auxComputeSumMatrix(listMatrix);
		int divisor = listMatrix.size();
		Matrix result = auxApplyDivisor2(sumMatrix, divisor);
		return result;
	}

	public static Double auxComputeAvg(List<Double> listDouble) {
		if(listDouble.size()>0) {
			Double sum = listDouble.stream().reduce(0.0, Double::sum);
			double result = sum/listDouble.size();
			return result;
		}
		return null;
	}

	public static double auxComputeSum(Matrix aMatrix) {
		double result = 0;
		for(double doAdd : aMatrix.getColumnPackedCopy()) {
			result+=doAdd;
		}
		return result;
	}

	@Override
	public TransitionMatrix aggregate(String operator, List<IAggregateable> listObjects,
			AgentAuthentication agentAuthentication) {
		int debugLevel = 0;
		if("avg".equals(operator)) {
			TransitionMatrix result = new TransitionMatrix();
			result.reset();
			List<Matrix> listIterObsMatrix = new ArrayList<>();
			List<Matrix> listIterCorrectionsMatrix = new ArrayList<>();
			List<Matrix> listAllObsMatrix = new ArrayList<>();
			List<Matrix> listAllCorrectionsMatrix = new ArrayList<>();
			List<Double> ListNbOfIterations = new ArrayList<>();
			TransitionMatrixKey lastKey = null;
			for(Object nextObj : listObjects) {
				if(nextObj instanceof TransitionMatrix) {
					TransitionMatrix trMatrix = (TransitionMatrix) nextObj;
					listIterObsMatrix.add(trMatrix.getIterObsMatrix());
					listIterCorrectionsMatrix.add(trMatrix.getIterCorrectionsMatrix());
					listAllObsMatrix.add(trMatrix.getAllObsMatrix());
					listAllCorrectionsMatrix.add(trMatrix.getAllCorrectionsMatrix());
					ListNbOfIterations.add((double) trMatrix.getNbOfIterations());
					lastKey = trMatrix.getKey();
				}
			}
			if("requested".equals(lastKey.getVariable())) {
				Matrix avgMatrix = auxComputeAvgMatrix(listAllObsMatrix);
				Matrix sumMatrix = auxComputeSumMatrix(listAllObsMatrix);
				int i = 0;
				for(Matrix nextMatrix : listAllObsMatrix) {
					i++;
					if(debugLevel >= 10) {
						SapereLogger.getInstance().info("TransitionMatrix.aggregate for debug : nextMatrix (" + i +") = "
							+ matrix2str(nextMatrix));
					}
				}
				if(debugLevel >= 10) {
					SapereLogger.getInstance().info("TransitionMatrix.aggregate for debug : nbMatrix = "
						+ listAllObsMatrix.size()
						+ ", sumMatrix = "
						+ matrix2str(sumMatrix)
						);
				}
			}
			result.setKey(new TransitionMatrixKey(null, null, lastKey.getVariable(), lastKey.getTimeWindow()));
			result.setIterObsMatrix(auxComputeSumMatrix(listIterObsMatrix));// auxComputeAvgMatrix
			result.setIterCorrectionsMatrix(auxComputeSumMatrix(listIterCorrectionsMatrix)); // auxComputeAvgMatrix
			result.setAllObsMatrix(auxComputeSumMatrix(listAllObsMatrix)); // auxComputeAvgMatrix
			result.setAllCorrectionsMatrix(auxComputeSumMatrix(listAllCorrectionsMatrix)); // auxComputeAvgMatrix
			result.setNbOfObservations((int) auxComputeSum(result.getAllObsMatrix()));
			result.setNbOfCorrections((int) auxComputeSum(result.getAllCorrectionsMatrix()));
			result.setNbOfIterations((int) auxComputeSum(result.getAllObsMatrix()));
			result.refreshNormalizedMatrix();
			Double avgNbOfIterations = auxComputeAvg(ListNbOfIterations);
			if(avgNbOfIterations != null) {
				result.setNbOfIterations(avgNbOfIterations.intValue());
			}
			result.setComputeDate(new Date());
			return result;
		}
		return null;
	}
}
