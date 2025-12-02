# LSTM for international airline passengers problem with regression framing
import numpy as np
import matplotlib.pyplot as plt
from pandas import read_csv
import math
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense
from tensorflow.keras.layers import LSTM
from keras.models import Sequential
from keras.layers import Dense, Activation
from sklearn.preprocessing import MinMaxScaler
from sklearn.metrics import mean_squared_error
look_back = 1

model = Sequential()
emb = (11,15)
hidden = (15,15)
model.add(LSTM(4, activation='softmax', input_shape=(1, look_back)))
model.add(LSTM(4, activation='softmax', input_shape=(1, look_back)))
#model.add(LSTM(input_shape = (emb,), input_dim=emb, output_dim=hidden, return_sequences=True))
#model.add(LSTM(input_shape = (emb,), input_dim=emb, output_dim=hidden, return_sequences=False))
model.add(Dense(3))
model.add(Activation("softmax"))
model.compile(loss="mse", optimizer="adam")
#return model
