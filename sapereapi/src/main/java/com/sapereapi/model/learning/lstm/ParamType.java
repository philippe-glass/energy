package com.sapereapi.model.learning.lstm;

import java.io.Serializable;

public enum ParamType implements Serializable {
	w, // Weight beetwen input/ouput layers
	u, // Weight beetwen hidden layer
	b // Bias
}
