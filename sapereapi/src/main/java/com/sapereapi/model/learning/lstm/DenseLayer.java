package com.sapereapi.model.learning.lstm;

import java.util.HashMap;
import java.util.Map;

import com.sapereapi.model.HandlingException;
import com.sapereapi.util.matrix.DoubleMatrix;

import eu.sapere.middleware.agent.AgentAuthentication;
import eu.sapere.middleware.log.AbstractLogger;

public class DenseLayer extends AbstractNNLayer implements INNLayer {
	private static final long serialVersionUID = 1L;
	private DoubleMatrix W_dense;
	private DoubleMatrix b_dense;

	public DoubleMatrix getW_dense() {
		return W_dense;
	}

	public DoubleMatrix getB_dense() {
		return b_dense;
	}


	private int realSize;
/*
	public DenseLayer(String path) throws HandlingException {
		this.W_dense = DoubleMatrix.loadMatrixFromFile(path + "2_param_0.txt");
		this.b_dense = DoubleMatrix.loadMatrixFromFile(path + "2_param_1.txt");
	}
*/
/*
	public DenseLayer(LayerDefinition layerDefinition, Map<ParamType, String> layerFiles) throws HandlingException {
		this.layerDefinition = layerDefinition;
		String pathW = layerFiles.get(ParamType.w);
		this.W_dense = DoubleMatrix.loadMatrixFromFile(pathW);
		String pathU = layerFiles.get(ParamType.u);
		this.b_dense = DoubleMatrix.loadMatrixFromFile(pathU);
	}
*/
	public DenseLayer(LayerDefinition layerDefinition, Map<ParamType, DoubleMatrix> layerMatrices) throws HandlingException {
		this.layerDefinition = layerDefinition;
		this.W_dense = layerMatrices.get(ParamType.w);
		this.b_dense = layerMatrices.get(ParamType.b);
	}

	public DoubleMatrix forwardStep(DoubleMatrix X) {
		return ActivationFunction.softmax(this.W_dense.transpose().multiplyByMatrix(X).addColumnVector(this.b_dense));
	}

	@Override
	public void setRealSize(int realSize) {

	}

	public static DenseLayer auxAggregate(Map<String, DenseLayer> mapLayers, Map<String, Double> weightsTable,
			AgentAuthentication agentAuthentication, AbstractLogger logger) throws HandlingException {
		Map<ParamType, DoubleMatrix> paramAggregatedMatrices = new HashMap<ParamType, DoubleMatrix>();
		if(mapLayers.size()==0) {
			return null;
		}
		for (ParamType paramType : ParamType.values()) {
			Map<String, DoubleMatrix> mapMatrices = new HashMap<String, DoubleMatrix>();
			for (String agentName : mapLayers.keySet()) {
				DenseLayer layer = mapLayers.get(agentName);
				// Concatenate all gate matrices of the same parameter type
				if (ParamType.w.equals(paramType)) {
					mapMatrices.put(agentName, layer.getW_dense());
				}
				if (ParamType.b.equals(paramType)) {
					mapMatrices.put(agentName, layer.getB_dense());
				}
			}
			DoubleMatrix paramAggregatedMatrix = DoubleMatrix.auxAggregate(mapMatrices, weightsTable,
					agentAuthentication, logger);
			paramAggregatedMatrices.put(paramType, paramAggregatedMatrix);
		}
		LayerDefinition layerDefinition = mapLayers.values().iterator().next().getLayerDefinition();
		DenseLayer result = new DenseLayer(layerDefinition, paramAggregatedMatrices);
		return result;
	}

	@Override
	public INNLayer copy() throws HandlingException {
		Map<ParamType, DoubleMatrix> mapMatrices = new HashMap<ParamType, DoubleMatrix>();
		// Concatenate all gate matrices of the same parameter type
		mapMatrices.put(ParamType.w, W_dense);
		mapMatrices.put(ParamType.b, b_dense);
		DenseLayer result = new DenseLayer(layerDefinition.clone(), mapMatrices);
		return result;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		try {
			return copy();
		} catch (HandlingException e) {
			throw new CloneNotSupportedException(e.getMessage());
		}
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("DenseLayer:");
		String sep = "";
		result.append(sep).append(ParamType.w)
				.append(" : matrix[" + W_dense.getRowDimension() + " X " + W_dense.getColumnDimension() + "]");
		sep = " , ";
		result.append(sep).append(ParamType.b)
				.append(" : matrix[" + b_dense.getRowDimension() + " X " + b_dense.getColumnDimension() + "]");
		return result.toString();
	}

	@Override
	public Map<ParamType, DoubleMatrix> getMapMatrices() {
		Map<ParamType, DoubleMatrix> result = new HashMap<ParamType, DoubleMatrix>();
		result.put(ParamType.w, W_dense);
		result.put(ParamType.b, b_dense);
		return result;
	}

	public boolean copyFromOther(INNLayer other) {
		boolean hasChanged = false;
		if(this.getLayerDefinition().getKey2().equals(other.getLayerDefinition().getKey2())) {
			if (other instanceof DenseLayer) {
				DenseLayer denseLayerOther = (DenseLayer) other;
				hasChanged = !this.W_dense.equals(denseLayerOther.getW_dense()) || !this.b_dense.equals(denseLayerOther.getB_dense());
				this.W_dense = denseLayerOther.getW_dense().copy();
				this.b_dense = denseLayerOther.getB_dense().copy();
			}
		}
		return hasChanged;
	}
}
