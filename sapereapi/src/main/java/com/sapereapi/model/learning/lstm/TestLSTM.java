package com.sapereapi.model.learning.lstm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.model.HandlingException;
import com.sapereapi.util.matrix.DoubleMatrix;

import eu.sapere.middleware.log.AbstractLogger;

public class TestLSTM {

	static SapereLogger logger = SapereLogger.getInstance();

	static DoubleMatrix initMatrix(String dir, int layerIdx, ParamType paramType, String[] listFileNames)
			throws HandlingException {
		List<DoubleMatrix> matricesToConcatenate = new ArrayList<DoubleMatrix>();
		for (String nextFilename : listFileNames) {
			DoubleMatrix nextMatrix = DoubleMatrix.loadMatrixFromFile(dir + layerIdx + nextFilename);
			matricesToConcatenate.add(nextMatrix);
		}
		boolean concatenateByColumns = LSTMLayer.getOperateByColumn(paramType);
		DoubleMatrix result = DoubleMatrix.concatenateMatrices(matricesToConcatenate, concatenateByColumns);
		return result;
	}

	static Map<String, DoubleMatrix> initLSTMLayerMatrices2(Map<String, DoubleMatrix> mapMatrices, String dir,
			LayerDefinition layerDefinition, int layerIdx) throws HandlingException {
		// NB : The order of forgate and cell parametric files are interchanged (ex. _param_6.txt and _param_3.txt)
		String[] params_w = { "_param_0.txt", "_param_6.txt", "_param_3.txt", "_param_9.txt" };
		String[] params_u = { "_param_1.txt", "_param_7.txt", "_param_4.txt", "_param_10.txt" };
		String[] params_b = { "_param_2.txt", "_param_8.txt", "_param_5.txt", "_param_11.txt" };

		// Map<String, DoubleMatrix> mapMatrices = new HashMap<String, DoubleMatrix>();
		mapMatrices.put(layerDefinition.generateMatrixKey(ParamType.w),
				initMatrix(dir, layerIdx, ParamType.w, params_w));
		mapMatrices.put(layerDefinition.generateMatrixKey(ParamType.u),
				initMatrix(dir, layerIdx, ParamType.u, params_u));
		mapMatrices.put(layerDefinition.generateMatrixKey(ParamType.b),
				initMatrix(dir, layerIdx, ParamType.b, params_b));
		return mapMatrices;
	}

	static Map<String, DoubleMatrix> initDenseLayerMatrices2(Map<String, DoubleMatrix> mapMatrices, String dir,
			LayerDefinition layerDefinition, int layerIdx) throws HandlingException {
		String[] params_w = { "_param_0.txt" };
		String[] params_b = { "_param_1.txt" };
		mapMatrices.put(layerDefinition.generateMatrixKey(ParamType.w),
				initMatrix(dir, layerIdx, ParamType.w, params_w));
		mapMatrices.put(layerDefinition.generateMatrixKey(ParamType.b),
				initMatrix(dir, layerIdx, ParamType.b, params_b));
		return mapMatrices;
	}

	public static void main(String[] args) throws IOException {
		double[][] x = new double[][] {
				{ 0.0, 3.0, 0.0, 0.0, 0.4849019607843267, 0.14588235294117696, 0.0147, 0.0, 0.5, 0.52, 2.0 },
				{ 1.0, 3.0, 0.0, 1.0, 0.34590000000000004, 0.0855, 0.0147, 0.0, 0.0, 0.467, 0.0 },
				{ 1.0, 3.0, 0.0, 1.0, 0.34590000000000004, 0.0855, 0.0147, 0.0, 0.0, 0.467, 0.0 } };
		DoubleMatrix X = new DoubleMatrix(x);
		try {
			String dir = "LSTM_TEST" + AbstractLogger.FILE_SEP + "situation0" + AbstractLogger.FILE_SEP;
			List<LayerDefinition> listLayerDefinition = new ArrayList<LayerDefinition>();
			LayerDefinition layer0 = new LayerDefinition(LSTMLayer.class, 1, 50);
			listLayerDefinition.add(layer0);
			Map<String, DoubleMatrix> mapMatrices = initLSTMLayerMatrices2(new HashMap<String, DoubleMatrix>(), dir,
					layer0, 0);
			LayerDefinition layer1 = new LayerDefinition(LSTMLayer.class, 3, 1);
			listLayerDefinition.add(layer1);
			mapMatrices = initLSTMLayerMatrices2(mapMatrices, dir, layer1, 1);
			LayerDefinition layer2 = new LayerDefinition(DenseLayer.class, 1);
			mapMatrices = initDenseLayerMatrices2(mapMatrices, dir, layer2, 2);
			listLayerDefinition.add(layer2);
			LSTMModelInfo modelInfo = new LSTMModelInfo();
			modelInfo.setLayers(listLayerDefinition);
			modelInfo.setMapMatrices(mapMatrices);
			Map<String, List<Integer>> mapShapes = LSTMModelInfo.generateMapSize(mapMatrices);
			modelInfo.setMapShapes(mapShapes);
			/*
			 * propagator1 = new VariableLSTMModel( "LSTM_TEST" + AbstractLogger.FILE_SEP +
			 * "situation0" + AbstractLogger.FILE_SEP, 2);
			 */
			VariableLSTMModel propagator2 = new VariableLSTMModel(modelInfo);
			propagator2.setRecurrentActivation(ActivationFunction.FUNCTION_HARD_SIGMOID_0);
			/*
			 * LSTMLayer testLayer1 = (LSTMLayer) propagator1.getLayers().get(0);
			 * DoubleMatrix matrix1 = testLayer1.getMatrix(LSTMGate.CELL, ParamType.w);
			 * 
			 * LSTMLayer testLayer2 = (LSTMLayer) propagator2.getLayers().get(0);
			 * DoubleMatrix matrix2 = testLayer2.getMatrix(LSTMGate.CELL, ParamType.w);
			 * 
			 * boolean equal = matrix1.equals(matrix2);
			 */
			DoubleMatrix prediction = propagator2.forward_propagate_full(X);
			System.out.println("Prediction : " + prediction);
			System.out.println("Expected Prediction : [[0.377 0.377 0.247]] ");
			// Expected prediction : [[0.377 0.377 0.247]]
		} catch (HandlingException e) {
			logger.error(e);
		}
	}
}
