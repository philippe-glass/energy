import logging

from flask import Flask
from flask_restful import Api, Resource
# pip install Flask
# pip install Flask-RESTful
disable_run = True
from predict import *
from data_store import *
from data_loader import *
from datetime import datetime
from datetime import timedelta
from flask import Flask,jsonify,request
import time
import math
from time import gmtime, strftime

app = Flask(__name__)

data_store = DataStore()

@app.route('/returnjson', methods = ['GET'])
def ReturnJSON():
    if(request.method == 'GET'):
        data = {
            "Modules" : 15,
            "Subject" : "Data Structures and Algorithms",
        }
        return jsonify(data)


def test_load_allnodes_history(node_name):
    date_current = datetime.strptime('2023-01-15', '%Y-%m-%d')
    nb_of_days = 4
    node_by_location = {"":"N2" ,"Gymnase":"N2" ,"Sous-sol":"N2" ,"Ecole primaire":"N1","Parascolaire":"N2"}
    set_node_in_measure_data(date_current, nb_of_days, node_by_location, node_name)
    history_data = load_allnodes_history_data(date_current, 4,  None)
    df_bis = extract_node_history_data(history_data, node_name)
    print("df_bis = ", df_bis)


def train_model(model_key, model_df, is_additional):
    if not data_store.has_model(model_key):
        # init a new model
        X_depth,Y_depth = 10,10
        input_shape = (X_depth,1)
        load_existing_weights = False
        new_regressor, sampling_nb, sc = data_store.init_model_LSTM(input_shape, Y_depth, model_key, load_existing_weights)
        #data_store.put_model(new_regressor, 0, model_key, sc)

    regressor, sampling_nb, sc = data_store.get_model(model_key)
    train_depth = 60
    train_depth = 10
    #sc = MinMaxScaler(feature_range=(0, 1))
    if is_additional:
        #X_Train, Y_Train, indexes_train = prepare_data_additional(model_df, train_depth, sc)
        test_rate = 0.2
        X_Train, X_test, Y_Train, Y_test, indexes_train, indexes_test = prepare_data(model_df, train_depth, test_rate, sc)
    else:
        test_rate = 0.2
        X_Train, X_test, Y_Train, Y_test, indexes_train, indexes_test = prepare_data(model_df, train_depth, test_rate, sc)
    if debug_level > 0:
        print("X_Train.shape", X_Train.shape)
    X_Train = np.reshape(X_Train, newshape=(X_Train.shape[0], X_Train.shape[1], 1))
    if debug_level >= 0:
        logger.info("train_model " + model_key.generate_key() + " X_Train.shape = " + str(X_Train.shape))
    Y_Train = np.reshape(Y_Train, newshape=(Y_Train.shape[0]))

    # Train the model
    variable = model_key.get_variable()
    logging.info("Train model " + model_key.generate_key())
    t_before = time.time()
    if True or variable == "consumed":
        regressor.fit(X_Train, Y_Train, epochs = data_store.nb_epochs, batch_size = 32)
    t_after = time.time()
    time_elapsed_sec = t_after - t_before
    logging.info("After train model " + model_key.generate_key() + " : time elapsed (sec) = " + str(time_elapsed_sec))
    sampling_nb = sampling_nb + X_Train.shape[0]
    data_store.put_model(regressor, sampling_nb, model_key, sc)
    data_store.dump_model(model_key)
    model_info = data_store.get_model_info2(model_key)
    return model_info


def aux_load_node_history(json_request):
    scope = json_request.get('scopeEnum')
    node_name = json_request.get('nodeName')
    s_date_min, s_date_max = json_request.get('dateMin'), json_request.get('dateMax')
    date_min, date_max = None, None
    if not s_date_min is None:
        date_min = datetime.strptime(s_date_min, '%Y-%m-%d %H:%M:%S%z')
    if not s_date_max is None:
        date_max = datetime.strptime(s_date_max, '%Y-%m-%d %H:%M:%S%z')
    df_node_history = loadNodeHistory(scope, node_name, date_min, date_max)
    return df_node_history, date_min, date_max

@app.route('/init_node_history', methods = ['GET','POST'])
def initNodeHistory():
    logging.info("initNodeHistory : begin")
    json_request = request.json
    list_variables = json_request.get('listVariables')
    scope = json_request.get('scopeEnum')
    node_name = json_request.get('nodeName')
    result = []
    df_node_history, date_min, date_max = aux_load_node_history(json_request)
    logger.info("initNodeHistory : history length = "+ str(len(df_node_history.index)))
    #df = prepare_df(request.json, list_variables)

    # Test load from database
    """
    call_test_load_allnodes_history = False
    if call_test_load_allnodes_history:
        test_load_allnodes_history(node_name)
    """
    try:
        for variable in list_variables:
            #list_valariables2 = [variable]
            model_key = ModelKey(node_name, scope, variable)
            df2 = df_node_history[[variable]].resample('1min').mean()
            df3 = df2.dropna(subset=[variable])
            model_info = train_model(model_key, df3, False)
            model_info2 = df2.dropna(subset=[variable])
            result.append(model_info)
    except Exception as err:
        print("Exception", err)
        traceback.print_exc(file=sys.stdout)
    logging.info("initNodeHistory : end")
    return jsonify(result)


def check_model_df(model_df, variable):
    nb_of_nan = 0
    for index, row in model_df.iterrows():
        value = row[variable]
        if math.isnan(value):
            nb_of_nan+=1
            print("check_model_df", index, ":", value)
    return nb_of_nan


def correct_model_df(model_df, variable):
    nb_of_corrections = 0
    value_before = None
    for index, row in model_df.iterrows():
        value = row[variable]
        if math.isnan(value):
            if not value_before is None:
                model_df.loc[index, variable] = value_before
                nb_of_corrections+=1
                logger.info("correct_model_df " + str(index) + ": set value " + str(value_before))
        else:
            value_before = value
    logger.info("correct_model_df nb_of_corrections : nb_of_corrections = "+ str(nb_of_corrections))
    return model_df

# Train the model with the added history
@app.route('/add_node_history', methods = ['GET','POST'])
def add_node_history():
    logging.info("add_node_history : begin")
    json_request = request.json
    list_variables = json_request.get('listVariables')
    scope = json_request.get('scopeEnum')
    node_name = json_request.get('nodeName')
    result = []
    df_node_history, date_min, date_max = aux_load_node_history(json_request)
    #df = prepare_df(request.json, list_variables)
    logger.info("add_node_history : history length = " + str(len(df_node_history.index)))
    try:
        for variable in list_variables:
            #list_valariables2 = [variable]
            model_key = ModelKey(node_name, scope, variable)
            df2 = df_node_history[[variable]].resample('1min').mean()
            # For debug :
            nan_count = check_model_df(df2, variable)
            if nan_count > 0:
                 logger.info("add_node_history : nan_count = " + str(nan_count) + " for key " + model_key.generate_key())
                 df2 = correct_model_df(df2, variable)
                 """ 
                 if variable == "consumed" or variable == "requested" or variable == "provided":
                    df2 = correct_model_df(df2, variable)
                 """
            # Remove Nan values
            df3 = df2.dropna(subset=[variable])
            model_info = train_model(model_key, df3, True)
            result.append(model_info)
    except Exception as err:
        print("Exception", err)
        traceback.print_exc(file=sys.stdout)
    logging.info("add_node_history : end")
    return jsonify(result)



@app.route('/model_info', methods = ['GET','POST'])
def getModelInfo():
    data = request.json
    list_variables =  data.get('listVariables')
    scope = data.get('scope')
    node_name = data.get('nodeName')
    load_existing_weights = data.get('loadExistingWeights')
    result = []
    for variable in list_variables:
        model_key = ModelKey(node_name, scope, variable)
        if not data_store.has_model(model_key):
            # init a new model
            X_depth,Y_depth = 10,10
            input_shape = (X_depth,1)
            #sc = MinMaxScaler(feature_range=(0, 1))
            new_regressor, sampling_nb, sc = data_store.init_model_LSTM(input_shape, Y_depth, model_key, load_existing_weights)
            #data_store.put_model(new_regressor, sampling_nb, model_key, sc)
        #regressor, sampling_nb = data_store.get_model(model_key)
        #model_info = data_store.get_model_info2(sampling_nb, model_key)
        model_info = data_store.get_model_info2(model_key)
        result.append(model_info)
    return jsonify(result)


@app.route('/update_model_weights', methods = ['GET','POST'])
def update_model_weights():
    data = request.json
    result = {}
    scope = data.get('scope')
    node_name = data.get('nodeName')
    map_model_wiehgts1 =  data.get('mapModeleWeights')
    if not map_model_wiehgts1 is None:
        for variable in map_model_wiehgts1:
            map_model_wiehgts2 = map_model_wiehgts1[variable]
            model_key = ModelKey(node_name, scope, variable)
            is_ok = data_store.update_model_weights(model_key, map_model_wiehgts2)
            result[variable] = is_ok
    # For debug
    model_key2 = ModelKey(node_name, scope, "requested")
    model_info = data_store.get_model_info2(model_key2)
    map_matrices = model_info["mapMatrices"]
    layers = model_info["layers"]
    if len(layers) > 0 :
        first_layer = layers[0]
        id_matrix = first_layer["layerName"] + "#" + "w"
        if id_matrix in map_matrices:
            matrix = map_matrices[id_matrix]
            #logging.info("update_model_weights debug : model_key2 = (" + str(node_name) + ":" + str(scope) + ":" + "requested) " + ",matix_id = " + str(id_matrix) + ", content = " + str(matrix))
    # end for debug
    return jsonify(result)

@app.route('/prediction1', methods = ['GET','POST'])
def prediction1():
    logging.info("prediction1 : begin")
    debug_level = 0
    json_request = request.json
    s_initial_date = json_request.get('initialDate')
    list_dates_X = json_request.get('listDatesX')
    #list_target_dates = json_request.get('targetDates')
    list_horizons = json_request.get('listHorizons')
    last_horizon = 0
    for next_horizon in list_horizons:
        if next_horizon > last_horizon:
            last_horizon = next_horizon
    list_X = json_request.get('listX')
    list_true = json_request.get('listTrue')
    scope = json_request.get('scope')
    variable = json_request.get('variable')
    node_name = json_request.get('nodeName')
    model_key = ModelKey(node_name, scope, variable)
    x_predict_2d = np.array(list_X)
    y_predict_2d = np.array(list_true)
    len_y= len(list_true)
    y_predict_2d = np.reshape(y_predict_2d, newshape=(len_y, 1))

    #old_sc = MinMaxScaler(feature_range=(0, 1))
    regressor, sampling_nb, sc = data_store.get_model(model_key)

    """
    _test_list = [154.4, 453.12, 45.786, 98.45]
    test_list = [2089.943, 4419.941, 7066.509, 4950.206, 3094.322, 2746.792, 2459.32,  2482.92, 3404.921, 2159.72 ]
    test1 = np.array(test_list)
    test1 = np.reshape(test1, newshape=(1, len(test_list)))
    test2 = sc.fit_transform(test1)
    """
    apply_normalization = True

    print("x_predict_2d = ", x_predict_2d)
    backup_x_predict_2d = x_predict_2d
    if apply_normalization:
        x_predict_2d = sc.fit_transform(x_predict_2d)
        if len(y_predict_2d) > 0:
            y_predict_2d = sc.fit_transform(y_predict_2d)

    x_predict_3d = np.expand_dims(x_predict_2d, axis=2)

    if debug_level > 0:
        print("x_predict_3d = ", x_predict_3d)

    X_test = x_predict_3d
    Y_test = y_predict_2d
    #nb_epoch = 1
    X_depth,Y_depth = 10,10
    input_shape = (X_depth,1)
    #regressor = data_store.map_models[scope]
    # Pass to Model
    if debug_level > 0:
        print("X_test.shape = ", X_test.shape)

    print("step1")
    print("before regressor.predict")
    initial_date = datetime.strptime(s_initial_date, '%Y-%m-%d %H:%M:%S%z')
    #target_date = datetime.strptime(s_target_date, '%Y-%m-%d %H:%M:%S%z')
    Y_predicted, Y_predicted_next = None, None

    next_horizon = 0
    horizons, prediction_dates, prediction_str_dates = [], [], []
    dim1, dim2 = 0, 0
    try:
        while next_horizon < last_horizon:
            if Y_predicted_next is None:
                X_test_next = X_test
            else:
                X_test_next = Y_predicted_next.reshape(dim1, dim2, 1)
            Y_predicted_next = regressor.predict(X_test_next, verbose=0)
            if apply_normalization:
                Y_predicted_next = sc.inverse_transform(Y_predicted_next)
            dim1, dim2 = Y_predicted_next.shape
            prediction_dates = []
            for x in range(0, dim2):
                next_horizon = next_horizon+1
                horizons.append(next_horizon)
                next_date = initial_date + timedelta(minutes=next_horizon)
                prediction_dates.append(next_date)
                sdate = next_date.strftime('%Y-%m-%d %H:%M:%S%z')
                prediction_str_dates.append(sdate)
            if Y_predicted is None:
                Y_predicted = Y_predicted_next
            else:
                Y_predicted = np.concatenate((Y_predicted, Y_predicted_next), axis=1)
    except Exception as err:
        print("Exception", err)
        traceback.print_exc(file=sys.stdout)
    print("after regressor.predict")

    if apply_normalization:
        #Y_predicted = sc.inverse_transform(Y_predicted)
        if len(Y_test) > 0:
            Y_test = sc.inverse_transform(Y_test)

    X_test_list = X_test.tolist()
    Y_predicted_list =  Y_predicted.tolist()
    Y_test_list = Y_test.tolist()
    current_time = str(datetime.now())
    result = {"listDatesX": list_dates_X, "listY":list_true, "listPredicted":Y_predicted_list, "current_time":current_time, "horizons":horizons, "predictionDates":prediction_str_dates}
    logging.info("prediction1 : end")
    return jsonify(result)


if __name__=='__main__':
    app.run(debug=True)
