disable_run = True
from predict import *
from data_store import *
from datetime import datetime
from flask import Flask,jsonify,request

sc = MinMaxScaler(feature_range=(0, 1))


_test_list = [154.4, 453.12, 45.786, 98.45]
test_list = [2089.943, 4419.941, 7066.509, 4950.206, 3094.322, 2746.792, 2459.32,  2482.92, 3404.921, 2159.72 ]

x_norm = (test_list-np.min(test_list))/(np.max(test_list)-np.min(test_list))

test1 = np.array(test_list)
#test1 = np.reshape(test1, newshape=(1, len(test_list)))

test1 = np.reshape(1, -1)

#test3 = np.expand_dims(test1, axis=2)


test2 = sc.fit_transform(test1)



print(test2)
