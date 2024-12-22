package com.sapereapi.model.learning.lstm;

import java.io.Serializable;
import java.util.Map;

import com.sapereapi.model.HandlingException;
import com.sapereapi.util.matrix.DoubleMatrix;

public interface INNLayer extends Cloneable, Serializable {
	public DoubleMatrix forwardStep(DoubleMatrix X) throws HandlingException;

	public void setRealSize(int realSize);

	public INNLayer copy() throws HandlingException;

	public Map<ParamType, DoubleMatrix> getMapMatrices();

	public LayerDefinition getLayerDefinition();

	public void setLayerDefinition(LayerDefinition layerDefinition);

	public boolean copyFromOther(INNLayer other) throws HandlingException;;
}
