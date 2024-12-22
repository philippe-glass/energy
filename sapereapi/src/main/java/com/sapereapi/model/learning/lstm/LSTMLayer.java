package com.sapereapi.model.learning.lstm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.model.HandlingException;
import com.sapereapi.util.matrix.DoubleMatrix;
import com.sapereapi.util.matrix.MatrixWindow;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;

public class LSTMLayer extends AbstractNNLayer implements INNLayer {
	private static final long serialVersionUID = 1L;
	private Map<ParamType, DoubleMatrix> paramMatrices = new HashMap<ParamType, DoubleMatrix>();
	private Map<ParamType, Map<LSTMGate, MatrixWindow>> paramGateWindows = new HashMap<ParamType, Map<LSTMGate,MatrixWindow>>();
	// private DoubleMatrix W_i;
	// private DoubleMatrix U_i;
	// private DoubleMatrix b_i;
	// private DoubleMatrix W_c;
	// private DoubleMatrix U_c;
	// private DoubleMatrix b_c;
	// private DoubleMatrix W_f;
	// private DoubleMatrix U_f;
	// private DoubleMatrix b_f;
	// private DoubleMatrix W_o;
	// private DoubleMatrix U_o;
	// private DoubleMatrix b_o;
	private int realSize;
	private int layerNum;
	private boolean returnSequence;
	private String activation =  ActivationFunction.FUNCTION_TANH;
	private String recurrentActivation = ActivationFunction.FUNCTION_SIGMOID;

	public int getLayerNum() {
		return layerNum;
	}

	public void setLayerNum(int layerNum) {
		this.layerNum = layerNum;
	}

	public boolean isReturnSequence() {
		return returnSequence;
	}

	public void setReturnSequence(boolean returnSequence) {
		this.returnSequence = returnSequence;
	}

	public int getRealSize() {
		return realSize;
	}

	public String getActivation() {
		return activation;
	}

	public void setActivation(String activation) {
		this.activation = activation;
	}

	public String getRecurrentActivation() {
		return recurrentActivation;
	}

	public void setRecurrentActivation(String recurrentActivation) {
		this.recurrentActivation = recurrentActivation;
	}


	public LSTMLayer(LayerDefinition layerDefinition, int layerNum, boolean returnSequence, Map<ParamType, DoubleMatrix> layerMatrices) throws HandlingException {
		this.layerDefinition = layerDefinition;
		this.layerNum = layerNum;
		this.returnSequence = returnSequence;
		initMatrices(layerMatrices);
	}

	private void initMatrices(Map<ParamType, DoubleMatrix> layerMatrices) throws HandlingException {
		if (!layerMatrices.containsKey(ParamType.w)) {
			throw new HandlingException(
					"LSTMLayer " + this.layerNum + " : layerFiles doew not contain the matrix for w param");
		}
		if (!layerMatrices.containsKey(ParamType.u)) {
			throw new HandlingException("LSTMLayer : layerFiles doew not contain the matrix for u param");
		}
		if (!layerMatrices.containsKey(ParamType.b)) {
			throw new HandlingException("LSTMLayer : layerFiles doew not contain the matrix for b param");
		}
		paramMatrices = layerMatrices;
		paramGateWindows.clear();
		// load W params (kernel)
		DoubleMatrix allW = layerMatrices.get(ParamType.w);
		Map<LSTMGate, MatrixWindow> mapWindowsW = splitMatriceWindow(allW, ParamType.w);
		paramGateWindows.put(ParamType.w, mapWindowsW);
		// Load U params (recursive kernel)
		DoubleMatrix allU = layerMatrices.get(ParamType.u);
		Map<LSTMGate, MatrixWindow> mapWindowsU = splitMatriceWindow(allU, ParamType.u);
		paramGateWindows.put(ParamType.u, mapWindowsU);
		// Load b params (bias)
		DoubleMatrix allB = layerMatrices.get(ParamType.b);
		Map<LSTMGate, MatrixWindow> mapWindowsB = splitMatriceWindow(allB, ParamType.b);
		paramGateWindows.put(ParamType.b, mapWindowsB);
	}

	public Map<ParamType, DoubleMatrix> getGateMatrices(LSTMGate gate) throws HandlingException {
		Map<ParamType, DoubleMatrix> result = new HashMap<ParamType, DoubleMatrix>();
		for (ParamType paramType : ParamType.values()) {
			try {
				result.put(paramType, getMatrix(gate, paramType));
			} catch (Exception e) {
				throw new HandlingException(e.getMessage());
			}
		}
		return result;
	}

	public DoubleMatrix getParamsWholeMatrix(ParamType paramType) {
		return this.paramMatrices.get(paramType);
	}

	public DoubleMatrix getMatrix(LSTMGate gate, ParamType paramType) throws HandlingException {
		if(paramGateWindows.containsKey(paramType)) {
			Map<LSTMGate, MatrixWindow> mapParamindows = paramGateWindows.get(paramType);
			if(mapParamindows.containsKey(gate)) {
				MatrixWindow window = mapParamindows.get(gate);
				DoubleMatrix paramMatrix = paramMatrices.get(paramType);
				try {
					return paramMatrix.getSubMatrix(window);
				} catch (Exception e) {
					throw new HandlingException(e.getMessage());
				}
			}
		}
		return null;
	}

	public DoubleMatrix applyGate(LSTMGate gate, DoubleMatrix x_t, DoubleMatrix h_t_1, String activationFunction) throws HandlingException {
		//Map<ParamType, DoubleMatrix> gateMatrices = getGateMatrices(gate);
		DoubleMatrix w = getMatrix(gate, ParamType.w);	// [size inpout2 X size output] = [1 X 50]
		DoubleMatrix u = getMatrix(gate, ParamType.u); // [size output X size output] = [50 X 50]
		DoubleMatrix b = getMatrix(gate, ParamType.b); // [1 X size output] = [50 X 50]
		if (w != null && u!= null && b!= null ) {
			DoubleMatrix sum = w.transpose().multiplyByMatrix(x_t);	//  w.transpose : [50 X 1] [1 X 1]
			sum = sum.addColumnVector(u.transpose().multiplyByMatrix(h_t_1));
			sum = sum.addColumnVector(b); // [size output=50, 1]
			return ActivationFunction.applyFunction(sum, activationFunction);
		}
		return null;
	}

	public void setRealSize(int realSize) {
		this.realSize = realSize;
	}

	// first component of input shape = row nb = nb of time steps
	// Second componen of input shape = column nb = nb of features
	public DoubleMatrix forwardStep(DoubleMatrix X) throws HandlingException {

		// If our input is shorter then defined by architecture (15),
		// let's add some zero vectors to it to allow adequate matrix multiplication
		if (this.layerNum == 0) {
			X = inputFix(X);
			/*
			double[][] x= new double[][] {
	               {0.02314, 0.02744 ,0.07265 ,0.06702 ,0.05925 ,0.05372 ,0.05139 ,0.05127 ,0.05143 ,0.05149}
	              ,{0.02744, 0.07265 ,0.06702 ,0.05925 ,0.05372 ,0.05139 ,0.05127 ,0.05143 ,0.05149 ,0.05143}
	              ,{0.07265, 0.06702 ,0.05925 ,0.05372 ,0.05139 ,0.05127 ,0.05143 ,0.05149 ,0.05143 ,0.04998}};
	        x= new double[][] {{0.02313582, 0.02743585 ,0.07264815}};
	        x= new double[][] {{0.02314, 0.02744 ,0.07265 ,0.06702 ,0.05925 ,0.05372 ,0.05139 ,0.05127 ,0.05143 ,0.05149}};
	        x= new double[][] {
	               {0.02314, 0.02744 ,0.07265 ,0.06702 ,0.05925 ,0.05372 ,0.05139 ,0.05127 ,0.05143 ,0.05149, 0.02744, 0.07265 ,0.06702 ,0.05925 ,0.05372 ,0.05139 ,0.05127 ,0.05143 ,0.05149 ,0.05143, 0.07265, 0.06702 ,0.05925 ,0.05372 ,0.05139 ,0.05127 ,0.05143 ,0.05149 ,0.05143 ,0.04998}};
			X = new DoubleMatrix( x);
			//X = X.transpose();
			*/
		}

		List<DoubleMatrix> outputs = new ArrayList<DoubleMatrix>();

		// Let's define previous cell output and hidden state
		DoubleMatrix W_i = getMatrix(LSTMGate.INPUT, ParamType.w);
		DoubleMatrix h_t_1 = DoubleMatrix.zeros(W_i.getColumnDimension(), 1);
		DoubleMatrix C_t_1 = DoubleMatrix.zeros(W_i.getColumnDimension(), 1);

		for (int i = 0; i < X.getColumnDimension(); i++) {
			// Weights update for every cell step-by-step.
			DoubleMatrix x_t = X.getColumnMatrix(i);
			/*
			DoubleMatrix U_i = getMatrix(LSTMGate.INPUT, ParamType.u);
			DoubleMatrix b_i = getMatrix(LSTMGate.INPUT, ParamType.b);
			DoubleMatrix W_i_mul_x = W_i.transpose().multiplyByMatrix(x_t);
			DoubleMatrix U_i_mul_h_1 = U_i.transpose().multiplyByMatrix(h_t_1);
			DoubleMatrix i_t1 = DoubleMatrix.hardSigmoid(W_i_mul_x.addColumnVector(U_i_mul_h_1).addColumnVector(b_i));
			*/
			// For more details check out: http://deeplearning.net/tutorial/lstm.html
			// Input gate activation vector : i_t = Sigmoid (W_i* Xt + U_i * Ht-1 + b_i)
			DoubleMatrix i_t = applyGate(LSTMGate.INPUT, x_t, h_t_1, recurrentActivation);
			//boolean isInputOk = i_t1.getSum() == i_t.getSum();

			/*
			DoubleMatrix W_c = getMatrix(LSTMGate.CELL, ParamType.w);
			DoubleMatrix U_c = getMatrix(LSTMGate.CELL, ParamType.u);
			DoubleMatrix b_c = getMatrix(LSTMGate.CELL, ParamType.b);
			DoubleMatrix W_c_mul_x = W_c.transpose().multiplyByMatrix(x_t);
			DoubleMatrix U_c_mul_h_1 = U_c.transpose().multiplyByMatrix(h_t_1);
			DoubleMatrix C_tilda1 = ActivationFunction.tanh(W_c_mul_x.addColumnVector(U_c_mul_h_1).addColumnVector(b_c));
			*/
			// Cell gate activation vector : c~_t = tanh * (W_c* Xt + U_c * Ht-1 + b_c)
			DoubleMatrix C_tilda = applyGate(LSTMGate.CELL, x_t, h_t_1, activation);
			//boolean isCellOk = C_tilda1.getSum() == C_tilda.getSum();

			/*
			DoubleMatrix W_f = getMatrix(LSTMGate.FORGATE, ParamType.w);
			DoubleMatrix U_f = getMatrix(LSTMGate.FORGATE, ParamType.u);
			DoubleMatrix b_f = getMatrix(LSTMGate.FORGATE, ParamType.b);
			DoubleMatrix W_f_mul_x = W_f.transpose().multiplyByMatrix(x_t);
			DoubleMatrix U_f_mul_h_1 = U_f.transpose().multiplyByMatrix(h_t_1);
			DoubleMatrix f_t1 = DoubleMatrix.hardSigmoid(W_f_mul_x.addColumnVector(U_f_mul_h_1).addColumnVector(b_f));
			*/
			// Forget gate activation vector : f_t = Sigmoid (W_f* Xt + U_f * Ht-1 + b_f)
			DoubleMatrix f_t = applyGate(LSTMGate.FORGATE, x_t, h_t_1, recurrentActivation);
			//boolean isForgateOk = f_t1.getSum() == f_t.getSum();

			// C_t = c~_t * i_t 	+ 	C_t-1 * f_t
			DoubleMatrix C_t = (i_t.simpleMultiplyByMatrix(C_tilda)).plus(f_t.simpleMultiplyByMatrix(C_t_1));

			// Output gate activation vector : o_t = Sigmoid (W_o* Xt + U_o * Ht-1 + b_o)
			/*
			DoubleMatrix W_o = getMatrix(LSTMGate.OUTPUT, ParamType.w);
			DoubleMatrix U_o = getMatrix(LSTMGate.OUTPUT, ParamType.u);
			DoubleMatrix b_o = getMatrix(LSTMGate.OUTPUT, ParamType.b);
			DoubleMatrix W_o_mul_x = W_o.transpose().multiplyByMatrix(x_t);
			DoubleMatrix U_o_mul_h_1 = U_o.transpose().multiplyByMatrix(h_t_1);
			DoubleMatrix o_t1 = DoubleMatrix.hardSigmoid(W_o_mul_x.addColumnVector(U_o_mul_h_1).addColumnVector(b_o));
			*/
			DoubleMatrix o_t = applyGate(LSTMGate.OUTPUT, x_t, h_t_1, recurrentActivation);
			//boolean isOutputOk = o_t1.getSum() == o_t.getSum();

			// h_t = o_t * tanh(c_t)
			DoubleMatrix h_t = o_t.simpleMultiplyByMatrix(ActivationFunction.tanh(C_t));

			outputs.add(h_t);
			h_t_1 = h_t;
			C_t_1 = C_t;

		}

		if (this.returnSequence) {

			// We return out sequence corresponding to our input,
			// which has length of this.realSize.
			// We will restore it in next layer again using fixInput()
			int rows = outputs.get(0).getRowDimension();
			DoubleMatrix result = DoubleMatrix.zeros(rows, this.realSize);
			for (int i = 0; i < outputs.size(); i++) {
				DoubleMatrix nextOutput = outputs.get(i);
				for (int j = 0; j < this.realSize; j++) {
					// nextOutput.get(i, 1);
					// result.put(i, j, nextOutput.get(i,1));
					result.set(i, j, nextOutput.get(i, 0));
				}
			}
			return result;

		} else {
			// If we don't want to return sequence of outputs from every cell,
			// but only for the last one (for the last LSTM layer), use this.
			return outputs.get(outputs.size() - 1);
		}

	}

	// TODO : function to test and to review
	public DoubleMatrix inputFix(DoubleMatrix X) throws HandlingException {
		DoubleMatrix W_i = getMatrix(LSTMGate.INPUT, ParamType.w);
		DoubleMatrix result = DoubleMatrix.zeros(W_i.getRowDimension(), W_i.getColumnDimension());
		/*
		 * for (int i = 0; i < X.getRowDimension(); i++) { res.putColumn(i,
		 * X.getRowMatrix(i)); }
		 */
		for (int i = 0; i < X.getRowDimension(); i++) {
			for (int j = 0; j < X.getColumnDimension(); j++) {
				if (j >= result.getRowDimension()) {
					System.err.println("inputFix : " + j + " >= W_i.rowNb = " + result.getRowDimension());
				}
				if (i >= result.getColumnDimension()) {
					System.err.println("inputFix : " + i + " >= W_i.colNb = " + result.getColumnDimension());
				}
				result.set(j, i, X.get(i, j));
				// res.putColumn(i, X.getRowMatrix(i));
			}
		}
		return result;
	}

	public DoubleMatrix inputFix2(DoubleMatrix X) {
		//DoubleMatrix W_i = getMatrix(LSTMGate.INPUT, ParamType.w);
		DoubleMatrix result = X.copy();
		return result;
	}

	public static boolean getOperateByColumn(ParamType paramType) {
		boolean splitByColumns = true;
		if (ParamType.b.equals(paramType)) {
			splitByColumns = false;
		}
		return splitByColumns;
	}

	public static Map<LSTMGate, MatrixWindow> splitMatriceWindow(DoubleMatrix bigMatrix, ParamType paramType) {
		Map<LSTMGate, MatrixWindow> result2 = new HashMap<LSTMGate, MatrixWindow>();
		int nbOfGates = LSTMGate.values().length;
		boolean splitByColumns = getOperateByColumn(paramType);
		List<MatrixWindow> result1 = DoubleMatrix.splitMatrixWindow(bigMatrix, nbOfGates, splitByColumns);
		// Order : Input(i), Forget(f), Cell(c), Output(o)
		for (LSTMGate lstmGate : LSTMGate.values()) {
			int gateIndex = lstmGate.getIndex();
			MatrixWindow gateMatrix = result1.get(gateIndex);
			result2.put(lstmGate, gateMatrix);
		}
		return result2;
	}

	public static Map<LSTMGate, DoubleMatrix> splitMatrices2(DoubleMatrix bigMatrix, ParamType paramType) {
		Map<LSTMGate, DoubleMatrix> result2 = new HashMap<LSTMGate, DoubleMatrix>();
		int nbOfGates = LSTMGate.values().length;
		boolean splitByColumns = getOperateByColumn(paramType);
		List<DoubleMatrix> result1 = DoubleMatrix.splitMatrices(bigMatrix, nbOfGates, splitByColumns);
		// Order : Input(i), Forget(f), Cell(c), Output(o)
		for (LSTMGate lstmGate : LSTMGate.values()) {
			int gateIndex = lstmGate.getIndex();
			DoubleMatrix gateMatrix = result1.get(gateIndex);
			result2.put(lstmGate, gateMatrix);
		}
		return result2;
	}

	public static LSTMLayer auxAggregate(Map<String, LSTMLayer> mapLayers, Map<String, Double> weightsTable,
			AgentAuthentication agentAuthentication, AbstractLogger logger) throws HandlingException {
		LSTMLayer firstLayer = mapLayers.values().iterator().next();
		LayerDefinition firstLayerDef = firstLayer.getLayerDefinition();
		int layerNum = firstLayer.getLayerNum();
		boolean returnSequence = firstLayer.isReturnSequence();
		Map<ParamType, DoubleMatrix> paramAggregatedMatrices = new HashMap<ParamType, DoubleMatrix>();
		for (ParamType paramType : ParamType.values()) {
			Map<String, DoubleMatrix> mapMatrices = new HashMap<String, DoubleMatrix>();
			for (String agentName : mapLayers.keySet()) {
				LSTMLayer layer = mapLayers.get(agentName);
				// Concatenate all gate matrices of the same parameter type
				DoubleMatrix paramMatrix = layer.getParamsWholeMatrix(paramType);
				mapMatrices.put(agentName, paramMatrix);
			}
			DoubleMatrix paramAggregatedMatrix = DoubleMatrix.auxAggregate(mapMatrices, weightsTable,
					agentAuthentication, logger);
			paramAggregatedMatrices.put(paramType, paramAggregatedMatrix);
		}
		LSTMLayer result = new LSTMLayer(firstLayerDef, layerNum, returnSequence, paramAggregatedMatrices);
		// For debug
		if(firstLayerDef.getLayerIndex2() ==0) {
			for (String agentName : mapLayers.keySet()) {
				LSTMLayer layer = mapLayers.get(agentName);
				logger.info("LSTMLayer.auxAggregate w_forgate of " + agentName + " = " + layer.getMatrix(LSTMGate.FORGATE, ParamType.w));
			}
			DoubleMatrix aggregatedForgateW = result.getMatrix(LSTMGate.FORGATE, ParamType.w);
			logger.info("LSTMLayer.auxAggregate w_forgate of aggregation = " + aggregatedForgateW);
			logger.info("LSTMLayer.auxAggregate : firstLayerDef = " + firstLayerDef);
		}
		return result;
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("LSTMLayer:");
		String sep = "";
		for (ParamType paramType : ParamType.values()) {
			DoubleMatrix matrix = getParamsWholeMatrix(paramType);
			result.append(sep).append(paramType).append(" : matrix[" +matrix.getRowDimension() + " X " + matrix.getColumnDimension()+ "]");
			sep = " , ";
		}
		return result.toString();
	}

	public LSTMLayer copy() throws HandlingException {
		Map<ParamType, DoubleMatrix> mapMatrices = new HashMap<ParamType, DoubleMatrix>();
		for (ParamType paramType : ParamType.values()) {
			// Concatenate all gate matrices of the same parameter type
			DoubleMatrix paramMatrix = getParamsWholeMatrix(paramType);
			mapMatrices.put(paramType, paramMatrix);
		}
		LSTMLayer result = new LSTMLayer(layerDefinition.clone(),layerNum, returnSequence, mapMatrices);
		return result;
	}

	@Override
	public Map<ParamType, DoubleMatrix> getMapMatrices() {
		Map<ParamType, DoubleMatrix> result = new HashMap<ParamType, DoubleMatrix>();
		for (ParamType paramType : ParamType.values()) {
			if(paramMatrices.containsKey(paramType)) {
				result.put(paramType, paramMatrices.get(paramType));
			}
			/*
			for (LSTMGate gate : LSTMGate.values()) {
				String key = paramType + "#" + gate.toString().toLowerCase();
				result.put(key, getMatrix(gate, paramType));
			}
			*/
		}
		return result;
	}

	public boolean copyFromOther(INNLayer other) throws HandlingException {
		boolean hasChanged = false;
		if(this.getLayerDefinition().getKey2().equals(other.getLayerDefinition().getKey2())) {
			if (other instanceof LSTMLayer) {
				LSTMLayer otherLSTMlayer = (LSTMLayer) other;
				Map<ParamType, DoubleMatrix> otherParamMatrices = new HashMap<ParamType, DoubleMatrix>();
				for (ParamType paramType : ParamType.values()) {
					DoubleMatrix paramMatrixThis = getParamsWholeMatrix(paramType);
					DoubleMatrix paramMatrixOther = otherLSTMlayer.getParamsWholeMatrix(paramType);
					boolean hasChangedNext = !paramMatrixThis.equals(paramMatrixOther);
					otherParamMatrices.put(paramType, paramMatrixOther.copy());
					hasChanged = hasChanged || hasChangedNext;
				}
				if(hasChanged) {
					initMatrices(otherParamMatrices);
				}
			}
		}
		return hasChanged;
	}
}
