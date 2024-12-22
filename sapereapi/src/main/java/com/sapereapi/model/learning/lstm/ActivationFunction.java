package com.sapereapi.model.learning.lstm;

import com.sapereapi.util.matrix.DoubleMatrix;

/**
 * Created by Alex on 09.06.2016. Class for activation functions used for
 * forward propagation
 */

public class ActivationFunction {
	public final static String FUNCTION_TANH = "tanh";
	public final static String FUNCTION_SOFTMAX = "softmax";
	public final static String FUNCTION_HARD_SIGMOID_0 = "hardSigmoid_0";
	public final static String FUNCTION_HARD_SIGMOID_1 = "hardSigmoid_1";

	public final static String FUNCTION_SIGMOID = "sigmoid";

	public static DoubleMatrix applyFunction(DoubleMatrix x, String functionName) {
		if (FUNCTION_TANH.equals(functionName)) {
			return x.tanh();
		} else if (FUNCTION_SOFTMAX.equals(functionName)) {
			return softmax(x);
		} else if (FUNCTION_SIGMOID.equals(functionName)) {
			return DoubleMatrix.sigmoid(x);
		} else if (FUNCTION_HARD_SIGMOID_0.equals(functionName)) {
			return DoubleMatrix.hardSigmoid_0(x);
			//Prediction : [[0.369 0.369 0.261]]
		} else if (FUNCTION_HARD_SIGMOID_1.equals(functionName)) {
			return DoubleMatrix.hardSigmoid_1(x);
			//Prediction : [[0.369 0.369 0.261]]
		}
		return x;
	}

	public static DoubleMatrix tanh(DoubleMatrix X) {
		return X.tanh();
	}

	public static DoubleMatrix softmax(DoubleMatrix X) {
		X = X.transpose();
		DoubleMatrix expM = X.exp();// MatrixFunctions.exp(X);
		// DoubleMatrix result = expM.copy();
		return expM.normalize();
	}

}
