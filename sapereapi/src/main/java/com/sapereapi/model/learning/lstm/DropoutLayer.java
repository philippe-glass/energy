package com.sapereapi.model.learning.lstm;

import java.util.HashMap;
import java.util.Map;

import com.sapereapi.model.HandlingException;
import com.sapereapi.util.matrix.DoubleMatrix;

public class DropoutLayer extends AbstractNNLayer implements INNLayer {
	private static final long serialVersionUID = 1L;
	private double rate;

	// private int realSize;

	public double getRate() {
		return rate;
	}

	public void setRate(double rate) {
		this.rate = rate;
	}

	public DropoutLayer(LayerDefinition layerDefinition, double _rate) throws HandlingException {
		this.layerDefinition = layerDefinition;
		this.rate = _rate;
	}

	public DoubleMatrix forwardStep(DoubleMatrix X) {
		return DoubleMatrix.applyDropout(X, rate);
	}

	@Override
	public void setRealSize(int realSize) {

	}

	@Override
	public INNLayer copy() {
		try {
			return new DropoutLayer(layerDefinition.clone(), rate);
		} catch (HandlingException e) {
			return null;
		}
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return copy();
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("DropoutLayer:rate=").append(rate);
		return result.toString();
	}

	@Override
	public Map<ParamType, DoubleMatrix> getMapMatrices() {
		Map<ParamType, DoubleMatrix> result = new HashMap<ParamType, DoubleMatrix>();
		return result;
	}

	@Override
	public boolean copyFromOther(INNLayer other) {
		return false;
	}
}
