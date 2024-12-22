

import numpy as np # linear algebra
import pandas as pd # data processing, CSV file I/O (e.g. pd.read_csv)
import warnings
import sys, traceback
warnings.filterwarnings("ignore") # hide warnings
from datetime import datetime,timedelta
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
import pprint
#matplotlib inline
from matplotlib import style
from sklearn.preprocessing import MinMaxScaler
from sklearn.preprocessing import MinMaxScaler
from matplotlib import style

from keras.models import Sequential
from keras.layers import Dense
from keras.layers import LSTM
from keras.layers import Dropout
from sklearn.model_selection import train_test_split
from util import *
from data_store import *
import copy








def get_option_params():
    sX_default = "175.909,173.88,174.572,173.335,175.59,177.199,176.651,175.567,172.43,172.406" \
             "|173.88,174.572,173.335,175.59,177.199,176.651,175.567,172.43,172.406,172.659" \
             "|174.572,173.335,175.59,177.199,176.651,175.567,172.43,172.406,172.659,173.15"
    sY_default = "172.659|173.15|173.665"
    sindexes_default = "15/01/2023 18:38:00,15/01/2023 18:39:00,15/01/2023 18:40:00"

    sX, sY, sindexes, display_result = None, None, None, True

    map_options = get_options()
    #print("map_options = ", map_options)
    if "X" in map_options:
        sX = map_options["X"]
    if "Y" in map_options:
        sY = map_options["Y"]
    if "INDEXES" in map_options:
        sindexes = map_options["INDEXES"]
    if "display_result" in map_options:
        print("map_options['display_result'] = ", map_options["display_result"])
        display_result = (map_options["display_result"] == "True")
        print("display_result = ", display_result)

    if sX is None or sY is None or sindexes is None:
        print("one of the parameters is not set : select X,Y,indexes by default")
        sX, sY, sindexes = sX_default, sY_default, sindexes_default
    return sX, sY, sindexes, display_result







def prepara_test_data(sX, sY, sindexes,  debug_level):
    indexes_test = []
    array_index = sindexes.split(",")
    for s_index in array_index:
        next_timestamp = pd.Timestamp(s_index, tz=None)
        indexes_test.append(next_timestamp)

    x_predict_2d = load_2d_array(sX, "|", ",")
    y_predict_2d =  load_2d_array(sY, "|", ",")

    sc = MinMaxScaler(feature_range=(0, 1))

    x_predict_2d = sc.fit_transform(x_predict_2d)
    y_predict_2d = sc.fit_transform(y_predict_2d)

    x_predict_3d = np.expand_dims(x_predict_2d, axis=2)
    #y_predict_3d = np.expand_dims(y_predict_2d, axis=2)

    if debug_level > 0:
        print("x_predict_3d = ", x_predict_3d)

    X_test = x_predict_3d
    Y_test = y_predict_2d


    #X_Train_shape = X_Train.shape
    #X_Train_shape = (43162, 10, 1)


    if debug_level > 0:
        Y_test_d2 =  np.reshape(Y_test, newshape=(Y_test.shape[0], Y_test.shape[1]))
        sY_test = dump_2d_array(Y_test_d2, "|", ",")
        print("Y_test = ", sY_test)

    return X_test, Y_test, indexes_test, sc




test_locals = locals()
test_globals = globals()
print("test_locals", test_locals)
print("test_globals", test_globals)
disable_run = ('disable_run' in test_globals)
print("disable_run = ", disable_run)




debug_level = 0


def main():
    nb_epoch = 1
    X_depth,Y_depth = 10,10
    input_shape = (X_depth,1)
    node_name, scope, variable = "N1", "NODE", "consumed"
    model_key = ModelKey(node_name, scope, variable )
    load_existing_weights= True
    data_store = DataStore()
    regressor, sampling_nb, sc = data_store.init_model_LSTM(input_shape, Y_depth, model_key, load_existing_weights)
    sX, sY, sindexes, display_result = get_option_params()
    X_test, Y_test, indexes_test, sc = prepara_test_data( sX, sY, sindexes, debug_level)
    # Pass to Model
    if debug_level > 0:
        print("X_test.shape = ", X_test.shape)

    print("step1")

    """ 
    X_test2 = X_test[0:3,:,:]
    if debug_level > 0:
        print("X_test2.shape = ",X_test2.shape)
        print("X_test2 = ",X_test2)
    X_test2b =  np.reshape(X_test2, newshape=(X_test2.shape[0], X_test2.shape[1]))
    print("step2")
    np.savetxt("X_test2b.txt", X_test2b, fmt='%.8f')
    """

    print("before regressor.predict")
    Y_predicted, Y_predicted2 = None, None
    try:
        Y_predicted = regressor.predict(X_test, verbose=0)
        #Y_predicted2 = regressor.predict(X_test2, verbose=0)
        if debug_level >= 0:
            print("Y_predicted2 = ",Y_predicted2)
    except Exception as err:
        print("Exception", err)
        traceback.print_exc(file=sys.stdout)
    print("after regressor.predict")


    #Y_predicted = regressor.predict(X_test, batch_size = 64)


    # Do inverse Transformation to get Values

    print("Y_predicted.shape = ", Y_predicted.shape)
    if len(Y_predicted.shape) > 2:
        Y_predicted = np.reshape(Y_predicted, newshape=(Y_predicted.shape[0], Y_predicted.shape[1]))
        print("Y_predicted.shape(2) = ", Y_predicted.shape)
    Y_predicted = sc.inverse_transform(Y_predicted)
    Y_true = sc.inverse_transform(Y_test)
    print(Y_predicted.shape)
    list_predicted = [x[0] for x in Y_predicted]
    list_true = [x[0] for x in Y_true]
    if display_result:
        display_prediction_results(indexes_test, list_true, list_predicted)
    print("list_predicted", list_predicted)

    """ """
    # Evaluate the model
    #loss, acc = regressor.evaluate(X_test, Y_test, verbose=2)
    #print("Trained model, accuracy: {:5.2f}%".format(100 * acc))

    print("---- ended ----")



if __name__ == "__main__":
   # stuff only to run when not called via 'import' here
   main()
