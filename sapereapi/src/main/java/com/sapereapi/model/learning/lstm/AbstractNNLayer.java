package com.sapereapi.model.learning.lstm;

public abstract class AbstractNNLayer implements INNLayer {
	private static final long serialVersionUID = 1L;
	protected LayerDefinition layerDefinition;

	public LayerDefinition getLayerDefinition() {
		return layerDefinition;
	}

	public void setLayerDefinition(LayerDefinition layerDefinition) {
		this.layerDefinition = layerDefinition;
	}
}
