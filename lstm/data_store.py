
from util import *



class DataStore():
    map_models = {}
    map_sampling_nb = {}
    map_sc = {} # map of min-max scalars
    last_call = None
    nb_epochs = 1

    def has_model(self, model_key):
        key = model_key.generate_key()
        return key in self.map_models

    def put_model(self, model, sampling_nb, model_key, sc):
        key = model_key.generate_key()
        self.map_models[key] = model
        self.map_sampling_nb[key] = sampling_nb
        self.map_sc[key] = sc

    def get_model(self, model_key):
        key = model_key.generate_key()
        if key in self.map_models:
            model = self.map_models[key]
            sampling_nb, sc = 0, None
            if key in self.map_sampling_nb:
                sampling_nb = self.map_sampling_nb[key]
            if key in self.map_sc:
                sc = self.map_sc[key]
            return model, sampling_nb, sc
        return None, None, None

    def get_model_info(self, model_key):
        regressor, sampling_nb, sc = self.get_model(model_key)
        layer_nb, layer_param_nb, map_matrices, map_index_by_type = {}, {}, {}, {}
        layers = []
        for idx_layer, layer in enumerate(regressor.layers):
            list_matrix = layer.get_weights()
            param_nb = get_layer_param_nb(layer)
            layer_type = layer.__class__.__name__
            if layer_type in map_index_by_type:
                map_index_by_type[layer_type] = 1+ map_index_by_type[layer_type]
            else:
                map_index_by_type[layer_type] = 0
            if (len(list_matrix) > 0):
                if not layer_type in layer_nb:
                    layer_nb[layer_type] = 0
                if not layer_type in layer_param_nb:
                    layer_param_nb[layer_type] = 0
                layer_nb[layer_type] = layer_nb[layer_type] + 1
                layer_param_nb[layer_type] = layer_param_nb[layer_type] + param_nb
            idx_layer2 = map_index_by_type[layer_type]
            param_types = []
            if layer_type in map_matrix_paramtypes:
                param_types = map_matrix_paramtypes[layer_type]
            next_layer = {"layerName": layer.name, "layerIndex": idx_layer, "layerType": layer_type,
                          "layerIndex2": idx_layer2, "nbOfParams": param_nb
                , "nbOfMatrices": len(list_matrix), "paramTypes": param_types}
            layers.append(next_layer)
        # Generate model name
        model_name, model_name_sep = "", ""
        for next_layer_name in layer_nb:
            model_name = model_name + model_name_sep + next_layer_name + "_" \
                         + str(layer_nb[next_layer_name]) + "_" + str(layer_param_nb[next_layer_name])
            model_name_sep = "-"
        model_name = model_name + model_key.generate_key() + "#epochs" + str(self.nb_epochs) + ""
        for idx_layer, layer in enumerate(regressor.layers):
            # for next_layer_name in layer_nb:
            list_matrix = layer.get_weights()
            for idx_matrix, next_matrix in enumerate(list_matrix):
                # matrix_name = generate_matrix_name(layer, idx_layer, next_matrix, idx_matrix)
                matrix_id = generate_matrix_identifiant(layer, idx_matrix)
                map_matrices[matrix_id] = next_matrix
                #matrice_param_nb = get_matrix_param_nb(next_matrix)
                #logging.info(str(idx_layer) + " next matrix : " + str(idx_matrix) + " " + str(next_matrix.shape))
        model_directory = "dump_models/" + model_name + "/"
        return model_name, layer_nb, layer_param_nb, model_directory, map_matrices, layers, sampling_nb

    # def get_model_info2(self, sampling_nb, model_key):
    def get_model_info2(self, model_key):
        model_name, layer_nb, layer_param_nb, model_directory, map_matrices, layers, sampling_nb = self.get_model_info(
            model_key)
        map_matrices2 = {}
        map_shapes = {}
        for key in map_matrices:
            next_matrix = map_matrices[key]
            map_matrices2[key] = next_matrix.tolist()
            map_shapes[key] = next_matrix.shape
        result = {"modelName": model_name, "samplingNb": sampling_nb, "variable": model_key.get_variable(),
                  "mapShapes": map_shapes, "mapMatrices": map_matrices2, "modelDirectory": model_directory,
                  "nbEpoch": self.nb_epochs, "layers": layers}
        return result

    def update_model_weights(self, model_key, map_matrices):
        regressor, sampling_nb, sc = self.get_model(model_key)
        is_ok = True
        for idx_layer, layer in enumerate(regressor.layers):
            list_matrix_before = layer.get_weights()
            list_matrix_after = []
            replacement_ok = True
            list_matrix_ids = []  # For debug
            for idx_matrix, next_matrix_before in enumerate(list_matrix_before):
                matrix_id = generate_matrix_identifiant(layer, idx_matrix)
                if matrix_id in map_matrices:
                    next_matrix_after1 = map_matrices[matrix_id]
                    try:
                        target_shape = next_matrix_before.shape
                        next_matrix_after2 = fill_np_maptrix(next_matrix_after1, target_shape)
                        list_matrix_after.append(next_matrix_after2)
                        list_matrix_ids.append(matrix_id)
                    except Exception as err:
                        print("Exception", err)
                        replacement_ok = False
                else:
                    replacement_ok = False
                    print("### update_model_weights : matrix identifier not found in the model ", model_key, ":",
                          matrix_id)
            if replacement_ok and (len(list_matrix_before) == len(list_matrix_after)):
                layer.set_weights(list_matrix_after)
                if len(list_matrix_after) > 99999990:
                    logging.info("update_model_weights : update done with the matrices ids " + str(list_matrix_ids))
            else:
                is_ok = False
        return is_ok

    def dump_model(self, model_key):
        # regressor.save_weights('my_model.h5')
        # regressor.save("my_model.keras")
        # regressor.export("exported_model")
        # Dump model weights
        model_name, layer_nb, layer_param_nb, model_directory, map_matrices, layers, sampling_nb = self.get_model_info(
            model_key)
        if not os.path.exists("dump_models/"):
            os.makedirs("dump_models/")
        if not os.path.exists(model_directory):
            os.makedirs(model_directory)
        for matrix_name in map_matrices:
            next_matrix = map_matrices[matrix_name]
            matrix_filepath = model_directory + matrix_name + ".txt"
            np.savetxt(matrix_filepath, next_matrix, fmt='%.5f')

    def load_model_weights(self, model_key):
        model_name, layer_nb, layer_param_nb, model_directory, map_matrices, layers, sampling_nb = self.get_model_info(
            model_key)
        regressor, sampling_nb, sc = self.get_model(model_key)
        load_ok = False
        if os.path.exists(model_directory):
            load_ok = True
            for idx_layer, layer in enumerate(regressor.layers):
                # for next_layer_name in layer_nb:
                list_matrix1 = layer.get_weights()
                list_matrix2 = []
                for idx_matrix, next_matrix1 in enumerate(list_matrix1):
                    matrix_name = generate_matrix_name(layer, idx_layer, next_matrix1, idx_matrix)
                    matrix_path = model_directory + matrix_name + ".txt"
                    if os.path.exists(matrix_path):
                        next_matrix2 = np.loadtxt(matrix_path, delimiter=' ')
                        next_matrix2 = np.reshape(next_matrix2, newshape=next_matrix1.shape)
                        """
                        print("after load : next_matrix = ", next_matrix2)
                        print("next_matrix1.shape", next_matrix1.shape, "next_matrix2.shape", next_matrix2.shape)
                        """
                        list_matrix2.append(next_matrix2)
                if len(list_matrix1) == len(list_matrix2):
                    layer.set_weights(list_matrix2)
                else:
                    load_ok = False
        return regressor, load_ok

    def init_model_LSTM(self, input_shape, y_depth, model_key, load_existing_weights):
        regressor = Sequential()
        #                                 = 4*(x+51)*50
        # LSTM : x=1 h=200      nb_params = 4*(x+h+1)*h = 4* [200+1+1]*200 = 4*202*200 = 161 600
        # DENSE :  h=50, o=1   nb_params  h*o+o = 50*1 + 1=51
        debug_level = 0
        useLightModel = True
        addSeondLayer = True
        deepness_y = 1
        if useLightModel:
            # regressor.add(LSTM(units = 50, return_sequences = True, activation = 'tanh', input_shape=(X_Train.shape[1], 1)))
            if debug_level > 0:
                print("LSTM input shape = ", input_shape)
            regressor.add(LSTM(units=50, activation='tanh', return_sequences=True, input_shape=input_shape))
            # h=units
            # x=second component of input_shape = nb of features (first component of input shape = nb of time steps
            # LSTM x=1   , h=50     nb_params = 4*(x+h+1)*h = 4* [50+1+1]*50 = 4*52*50 = 10 400
            # LSTM x=1000, h=50     nb_params = 4*(x+h+1)*h = 4* [50+1000+1]*50 = 210 200
            regressor.add(Dropout(0.2))
            if addSeondLayer:
                regressor.add(LSTM(units=50))
                # LSTM x=50, h=10    nb_params = 4*(x+h+1)*h = 4*61*10 = 2440
                regressor.add(Dropout(0.2))

            regressor.add(Dense(units=y_depth))  # ,activation='sigmoid'
            # Dense : in=10 out=1   nb_params = in*out + out = 10*1+1 = 11
        else:
            # Adding the first LSTM layer and some Dropout regularisation
            regressor.add(LSTM(units=50, return_sequences=True, input_shape=input_shape))
            regressor.add(Dropout(0.2))

            # Adding a second LSTM layer and some Dropout regularisation
            regressor.add(LSTM(units=50, return_sequences=True))
            regressor.add(Dropout(0.2))

            # Adding a third LSTM layer and some Dropout regularisation
            regressor.add(LSTM(units=50, return_sequences=True))
            regressor.add(Dropout(0.2))

            # Adding a fourth LSTM layer and some Dropout regularisation
            regressor.add(LSTM(units=50))
            regressor.add(Dropout(0.2))

            # Adding the output layer
            regressor.add(Dense(units=1))
        # Compiling the RNN
        regressor.compile(optimizer='adam', loss='mean_squared_error')  # , metrics = 'accuracy'
        sampling_nb, sc = 0, MinMaxScaler(feature_range=(0, 1))
        #if not self.has_model(model_key):
        self.put_model(regressor, sampling_nb, model_key, sc)

        if load_existing_weights:
            regressor, load_ok = self.load_model_weights(model_key)
            print("after load_model : load_ok = ", load_ok)
        return regressor, sampling_nb, sc
