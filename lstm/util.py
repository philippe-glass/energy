import sys

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from datetime import datetime

import logging
#logFormatter = logging.Formatter("%(asctime)s [%(threadName)-12.12s] [%(levelname)-5.5s]  %(message)s")
logFormatter = logging.Formatter("%(asctime)s [%(levelname)-5.5s]  %(message)s")
logging.basicConfig(level=logging.INFO, format='%(asctime)s %(message)s')
logger = logging.getLogger()
#logger.setFormatter(logFormatter)
now = datetime.now()
stime = now.strftime('%Y-%m-%d_%H%M%S')
print("stime", stime)
log_path = 'logs/lstm_' + stime + '.log'
#log_path = 'lstm_' + stime + '.log'
#log_path = 'logs/lstm_' + '2024-07-16_120342' + '.log'
print("log_path", log_path)
file_handler = logging.FileHandler(log_path)
file_handler.setFormatter(logFormatter)
logger.addHandler(file_handler)

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
import copy

from sklearn.model_selection import train_test_split
import os



class ModelKey:
    node_name = None
    scope = None
    variable = None

    def __init__(self, node_name, scope, variable):
        self.node_name = node_name
        self.scope = scope
        self.variable = variable

    def get_node_name(self):
        return self.node_name
    def set_node_name(self, value):
        self.node_name = value
    def get_scopee(self):
        return self.scope
    def set_scope(self, value):
        self.scope = value
    def get_variable(self):
        return self.variable
    def set_variable(self, value):
        self.variable = value
    def generate_key(self):
        return self.node_name + "#" + self.scope + "#" + self.variable


map_matrix_paramtypes = {"LSTM" : ["w","u","b"], "Dense" : ["w","b"]}


def load_2d_array(scontent, sep1, sep2):
    tab1 = scontent.split(sep1)
    firstRow = (tab1[0]).split(sep2)
    dim1,dim2 = len(tab1), len(firstRow)
    test = np.zeros((2, 3))
    result = np.zeros((dim1, dim2))
    for row_index, sArray in enumerate(tab1):
        #print("", row_index, sArray)
        tab2 = sArray.split(",")
        for col_index, sFloat in enumerate(tab2):
            result[row_index, col_index] = float(sFloat)
    return result


def dump_2d_array(np_array, sep1, sep2):
    result="";
    _sep1="";
    for idx_row, row in enumerate(np_array):
        result+=_sep1;
        _sep2=""
        for idx_col, cell_value in enumerate(row):
            result+= (_sep2 + str(cell_value))
            _sep2 = sep2
        _sep1=sep1
    return result


def dump_1d_array(np_array, sep1):
    result="";
    _sep1="";
    for idx_row, item in enumerate(np_array):
        result+=_sep1;
        result+=str(item)
        _sep1=sep1
    return result


def prepare_data(NewDataSet, train_depth, test_size, sc):
    All_set = NewDataSet.iloc[:,0:1]
    #All_set = All_set.tail(200)
    #All_set = All_set[:-train_depth]
    All_set = sc.fit_transform(All_set)
    all_indexes = NewDataSet.index
    #test2 = list(All_set.index.values)
    #print(All_set.index)
    #print(list(All_set.index.values))

    #All_set = NewDataSet.iloc[:,0:1]
    #All_set = All_set[:-train_depth]
    #All_set = sc.fit_transform(All_set)
    X, Y, indexes = [], [], []
    # Range should be fromm 60 Values to END
    for i in range(train_depth, All_set.shape[0]):
        # X_Train 0-59
        next_X = All_set[i-train_depth:i]
        next_Y =  All_set[i-0:i+1]
        if next_X.shape[0] == train_depth:
            X.append(next_X)
            # Y Would be 60 th Value based on past 60 Values
            next_Y2 = np.reshape(next_Y, 1)
            Y.append(next_Y2)
            next_index = all_indexes[i]
            indexes.append(next_index)
            #print("next_Y2  = ", next_Y2)
            #All_set.index[]
        else:
            print("not in next_Y = ", next_Y)

    # Convert into Numpy Array
    X = np.array(X)
    Y = np.array(Y)

    testLast100a = NewDataSet.tail(100).iloc[:,0:1]
    testLast100b = Y[-100:,0:1]
    testLast100b = sc.inverse_transform(testLast100b)
    #X_train, X_test, Y_train, Y_test = train_test_split(X,Y,random_state=104, test_size=test_size, shuffle=True)
    X_train, X_test, Y_train, Y_test = train_test_split(X,Y, test_size=test_size, shuffle=False)
    nb_train, nb_test = X_train.shape[0], X_test.shape[0]
    indexes_train, indexes_test = indexes[0:nb_train-0], indexes[nb_train:nb_train+nb_test-0]
    testLast100C = Y_test[-100:,0:1]
    testLast100C =  sc.inverse_transform(testLast100C)
    return X_train, X_test, Y_train, Y_test, indexes_train, indexes_test







def prepare_data_additional(NewDataSet, train_depth, sc):
    All_set = NewDataSet.iloc[:,0:1]
    All_set = sc.fit_transform(All_set)
    all_indexes = NewDataSet.index
    X, Y, indexes = [], [], []
    # Range should be fromm 60 Values to END
    for i in range(train_depth, All_set.shape[0]):
        # X_Train 0-59
        next_X = All_set[i-train_depth:i]
        next_Y =  All_set[i-0:i+1]
        # TODO : check if next_X contains NaN values
        if next_X.shape[0] == train_depth:
            X.append(next_X)
            # Y Would be 60 th Value based on past 60 Values
            next_Y2 = np.reshape(next_Y, 1)
            Y.append(next_Y2)
            next_index = all_indexes[i]
            indexes.append(next_index)
            #print("next_Y2  = ", next_Y2)
            #All_set.index[]
        else:
            print("not in next_Y = ", next_Y)

    # Convert into Numpy Array
    X = np.array(X)
    Y = np.array(Y)
    return X, Y, indexes








def fill_np_maptrix(matrix_dictionary, target_shape):
    row_nb, col_nb = int(matrix_dictionary["rowDimension"]), int(matrix_dictionary["columnDimension"])
    result = np.zeros((row_nb, col_nb))
    content = matrix_dictionary["array"]
    for idx_row , next_row in enumerate(content):
        result[idx_row:,]= next_row
    if len(target_shape) == 1:
        result = result.flatten()
    result_shape = result.shape
    are_equal = np.array_equal(target_shape, result_shape)
    if not are_equal:
        print("fill_np_maptrix obtain shape does not comply to the target shape result:", result_shape, "target:", target_shape)
    return result


""" 
test_dict = {"rowDimension":2, "columnDimension":3, "array":[[1.0,2.0,3.0], [1.1,2.1,3.1]]}
target_shape = [6]
test_np = fill_np_maptrix(test_dict, target_shape)
logging.warning("this is a test warning")
logging.info("this is a test info")
logging.warning(test_dict)
logging.info(target_shape)
"""

def get_options():
  result={}
  for next_arg in sys.argv:
        option_val = next_arg.split("=")
        if len(option_val) == 2:
            option_name, value = copy.copy(option_val[0]), copy.copy(option_val[1])
            result[option_name] = value
  return result

def display_prediction_results(dates, y_true, y_predicted):
    fig = plt.figure()
    ax1= fig.add_subplot(111)
    x = dates
    y = y_true
    plt.plot(x,y, color="red", label = "Original")
    plt.plot(x,y_predicted, color="blue", label = "Predicted")
    # beautify the x-labels
    plt.gcf().autofmt_xdate()
    plt.xlabel('Dates')
    plt.ylabel("Power in MW")
    plt.title("Predicted values")

    plt.legend()
    plt.show()

def get_matrix_param_nb(matrix):
    next_shape = matrix.shape
    result = 1
    for next_dim in next_shape:
        result = result*next_dim
    return result

def get_layer_param_nb(layer):
    list_matrix = layer.get_weights()
    nb_param = 0
    for idx_matrix, next_matrix in enumerate(list_matrix):
        nb_param = nb_param + get_matrix_param_nb(next_matrix)
    return nb_param





def generate_matrix_name(layer, idx_layer, matrix, idx_matrix):
    next_shape = matrix.shape
    s_shape = ""
    dim_sep = ""
    for next_dim in next_shape:
        s_shape = s_shape + dim_sep + str(next_dim)
        dim_sep = "X"
    layer_name = layer.name
    #TODO(?) : remove the part after "_"
    #x = layer_name.index("_")
    layer_type = layer.__class__.__name__
    param_types = map_matrix_paramtypes[layer_type]
    matrix_name = "Layer_" + str(idx_layer) + "_" + layer_name + "_" + param_types[idx_matrix] + "_"  + s_shape
    return matrix_name


def generate_matrix_identifiant(layer, idx_matrix):
    layer_type = layer.__class__.__name__
    param_types = map_matrix_paramtypes[layer_type]
    matrix_name = layer.name + "#" + param_types[idx_matrix]
    return matrix_name



def prepare_df(json_data, list_valariables):
    list_dates = json_data.get('listDates')
    #scope = json_data.get('scopeEnum')
    list_dates2 = []
    df = None
    try:
        for sdate in list_dates:
            next_date = datetime.strptime(sdate, '%Y-%m-%d %H:%M:%S%z')
            list_dates2.append(next_date)
        #df_data = {'requested': data.get('requested'), 'consumed': data.get('consumed')}
        df_data = {}
        for variable in list_valariables:
            df_data[variable] = json_data.get(variable)
        df = pd.DataFrame(df_data)
        df.index = list_dates2
    except Exception as err:
        print("Exception", err)
    return df
