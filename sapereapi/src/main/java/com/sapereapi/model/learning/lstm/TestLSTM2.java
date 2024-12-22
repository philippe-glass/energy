package com.sapereapi.model.learning.lstm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sapereapi.log.SapereLogger;
import com.sapereapi.util.matrix.DoubleMatrix;

public class TestLSTM2 {
	static SapereLogger logger = SapereLogger.getInstance();

	public static void main(String[] args) throws IOException {
		String modelReference = "LSTM_2_12840-Dense_1_11#epochs1";
		modelReference = "LSTM_1_10400-Dense_1_51#epochs1";
		String modelDirectory = "../lstm/dump_models/" + modelReference + "/";
		// Test the directory
		File fModelDirectory = new File(modelDirectory);
		if (!fModelDirectory.exists()) {
			String userDirectory = System.getProperty("user.dir");
			System.err.println("Directoy " + modelDirectory + " does not exists; userDirectory = " + userDirectory);
		}
		List<LayerDefinition> listLayerDefinition = new ArrayList<LayerDefinition>();
		// LayerDefinition test = new LayerDefinition(0, LSTMLayer.class, 1, 50);
		listLayerDefinition.add(new LayerDefinition(LSTMLayer.class, 1, 50));
		// listLayerDefinition.add(new LayerDefinition(DropoutLayer.class));
		/*
		 * listLayerDefinition.add(new LayerDefinition(LSTMLayer.class, 50, 10));
		 * listLayerDefinition.add(new LayerDefinition(DropoutLayer.class));
		 */
		listLayerDefinition.add(new LayerDefinition(DenseLayer.class, 1));
		// layerIndex++;
		/*
		double[][] OLD_x = new double[][] {
				{ 0.0, 3.0, 0.0, 0.0, 0.4849019607843267, 0.14588235294117696, 0.0147, 0.0, 0.5, 0.52, 2.0 },
				{ 1.0, 3.0, 0.0, 1.0, 0.34590000000000004, 0.0855, 0.0147, 0.0, 0.0, 0.467, 0.0 },
				{ 1.0, 3.0, 0.0, 1.0, 0.34590000000000004, 0.0855, 0.0147, 0.0, 0.0, 0.467, 0.0 } };
		*/
		double[][] __x = new double[][] { { 0.01005748, 0.00152088, 0.00463013, 0.00941155, 0.01105027, 0.01116489,
				0.01118645, 0.00877873, 0.01212566, 0.01784801, 0.02387695, 0.05179104, 0.05582682, 0.05578326,
				0.05549493, 0.05234915, 0.05180874, 0.05169957, 0.05166497, 0.0516583, 0.05157929, 0.05145551,
				0.05115801, 0.0512118, 0.05115442, 0.05117656, 0.05674403, 0.05860661, 0.06323875, 0.09712269,
				0.09712978, 0.09937081, 0.0975136, 0.09883441, 0.11867816, 0.15875774, 0.16952914, 0.18341151,
				0.20446726, 0.16659512, 0.16451885, 0.16707751, 0.16506793, 0.13524011, 0.12249688, 0.08866281,
				0.07151317, 0.03759628, 0.03145494, 0.02698835, 0.02313582, 0.02743585, 0.07264815, 0.0670238,
				0.05924975, 0.05371724, 0.0513949, 0.05127011, 0.05143079, 0.05149376 } };
		double[][] x = new double[][] {
				{ 0.02313582, 0.02743585, 0.07264815, 0.06702380, 0.05924975, 0.05371724, 0.05139490, 0.05127011,
						0.05143079, 0.05149376 },
				{ 0.02743585, 0.07264815, 0.06702380, 0.05924975, 0.05371724, 0.05139490, 0.05127011, 0.05143079,
						0.05149376, 0.05142943 },
				{ 0.07264815, 0.06702380, 0.05924975, 0.05371724, 0.05139490, 0.05127011, 0.05143079, 0.05149376,
						0.05142943, 0.04997954 } };
		DoubleMatrix X = new DoubleMatrix(x);
		// X = X.transpose();
		/*
		 * DoubleMatrix X2 = X.getMatrix(0,49,0,0);
		 */
		try {
			VariableLSTMModel propagator = new VariableLSTMModel(modelDirectory, listLayerDefinition);
			DoubleMatrix prediction = propagator.forward_propagate_full(X);
			System.out.println("Prediction : " + prediction);
		} catch (Throwable e) {
			logger.error(e);
		}
	}
}
