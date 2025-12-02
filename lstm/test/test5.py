
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from numpy import nan

from tensorflow.keras import Sequential
from tensorflow.keras.layers import LSTM, Dense
from tensorflow.keras.losses import SparseCategoricalCrossentropy

from sklearn.metrics import mean_squared_error
from sklearn.preprocessing import MinMaxScaler
from tensorflow.python.estimator import keras
from util import *

def evaluate_model(y_true, y_predicted):
    scores = []

    # calculate scores for each day
    for i in range(y_true.shape[1]):
        mse = mean_squared_error(y_true[:, i], y_predicted[:, i])
        rmse = np.sqrt(mse)
        scores.append(rmse)

    # calculate score for whole prediction
    total_mse = 0
    for row in range(y_true.shape[0]):
        for col in range(y_predicted.shape[1]):
            total_mse = total_mse + (y_true[row, col] - y_predicted[row, col])**2
    nb_of_items = y_true.shape[0]*y_predicted.shape[1]
    mean_mse = total_mse//nb_of_items
    sqrt_mean_mse = np.sqrt(mean_mse)
    return sqrt_mean_mse, scores

# dataset = pd.read_csv('../sapereapi/dump_history_N1.csv', parse_dates=True, index_col='date_time', low_memory=False, date_format="yyyy-MM-dd HH:mm:ss")
dataset = pd.read_csv('../history_data/dump_history_N1.csv', parse_dates=True, index_col='date_time', low_memory=False, date_format="%d/%m/%Y %H:%M:%S")


print(dataset.head())

#data = dataset.resample('1min').mean()
data = dataset.resample('1min').mean()

print(data.head())
print(data)

#loss_sparse = tensorflow.keras.losses.SparseCategoricalCrossentropy()


data.to_csv('history_data/resampled_dump_history_N1.csv')


#data = dataset


activate_plot1 = False
if activate_plot1:
    fig, ax = plt.subplots(figsize=(18,18))

    for i in range(len(data.columns)):
        #ax.remove()
        plt.subplot(len(data.columns), 1, i+1)
        name = data.columns[i]
        time_serie = data[name]
        #time_serie = time_serie[0:1000]
        plt.plot(time_serie)
        plt.title(name, y=0, loc='right')
        plt.yticks([])
    plt.show()
    fig.tight_layout()


#test1 = data.loc[:'2023-01-14 23:59', :][['requested', 'cls_requested']]
variable = 'requested'
variable_class = 'cls_' + variable
predict_class = False

data_train = data.loc[:'2023-01-14 23:59', :][[variable, variable_class]]
data_test  = data.loc['2023-01-15 00:00':, :][[variable, variable_class]]

print(data_train.shape)
print(data_test.shape)

data_train = np.array(data_train)
X_train, y_train = [], []
x_index, y_index = 0, 0
if predict_class:
    y_index = 1

test_light_prediction = False
deepness_x, deepness_y = 7,7


for i in range(deepness_x, len(data_train) - deepness_y):
    X_train.append(data_train[i - deepness_x:i, x_index])
    if test_light_prediction:
        y_train.append(data_train[i, y_index])
    else:
        y_train.append(data_train[i:i + deepness_y, y_index])



print("step 1 : len X_train.shape ", len(X_train) )

# Convert into NumPy arrays:
X_train, y_train = np.array(X_train), np.array(y_train)
if predict_class:
    y_train = y_train.astype(int)
print("X_train", X_train)
print("y_train", y_train)

# Check shape of arrays with:
print(X_train.shape, y_train.shape)

# Print y_train
print(pd.DataFrame(y_train).head())

# Normalize dataset between 0 and 1 with MinMaxScaler:
x_scaler = MinMaxScaler()
X_train = x_scaler.fit_transform(X_train)

y_scaler = MinMaxScaler()

if not predict_class:
    y_train = y_scaler.fit_transform(y_train)

values, counts = np.unique(y_train, return_counts=True)


# Check normalized X_train
print(pd.DataFrame(X_train).head())

# Convert to 3-D array:
print("before reshape : X_train  = ", X_train, X_train.shape)
dim1, dim2 = X_train.shape
X_train = X_train.reshape(dim1, dim2, 1)

# Build LSTM model:
# Build sequential model using Keras:
reg = Sequential()
# reg.add(LSTM(unitsy_train = 200, activation = 'relu', input_shape=(deepness_x,1) , stateful=False ))

if predict_class:
    reg.add(LSTM(units = 200, activation = 'tanh', input_shape=(deepness_x,1) ))
    reg.add(Dense(deepness_y, activation='softmax'))
else:
    reg.add(LSTM(units = 200, activation = 'tanh', input_shape=(deepness_x,1) ))
    reg.add(Dense(deepness_y, activation='sigmoid'))

print("summary:")
reg.summary()


# Decide mean square error as loss function and adam as optimizer:
#reg.compile(loss='mse', optimizer='adam',  metrics=['accuracy'])
#
if predict_class:
   #reg.compile(loss='binary_crossentropy', optimizer='adam',  metrics=['accuracy'])
   #reg.compile(loss=SparseCategoricalCrossentropy(from_logits=True), optimizer='adam',  metrics=['accuracy'])
   reg.compile(loss='categorical_crossentropy', optimizer='adam',  metrics=['accuracy'])
else:
   reg.compile(loss='mse', optimizer='adam',  metrics=['accuracy'])




# Train model:
#reg.fit(X_train, y_train, epochs=100)
reg.fit(X_train, y_train, epochs=1)


X_test, y_test = [], []
indexes_test = []
"""
for i in range(deepness_x, len(data_train) - deepness_y):
    X_train.append(data_train[i - deepness_x:i, x_index])
    y_train.append(data_train[i:i + deepness_y, y_index])
"""


# Split test data by week:
list_index = data_test.index
data_test = np.array(data_test)
for i in range(deepness_x, len(data_test) - deepness_y):
    added_x = data_test[i - deepness_x:i, x_index]
    if test_light_prediction:
        added_y = data_test[i, y_index]
    else:
        added_y = data_test[i:i + deepness_y, y_index]
    X_test.append(added_x)
    y_test.append(added_y)
    next_index = list_index[i]
    indexes_test.append(next_index)



# Make data into NumPy array, transform using MinMaxScaler, and reshape into 3-D arrays:
X_test = np.array(X_test)
y_test = np.array(y_test)
X_test, y_test = np.array(X_test), np.array(y_test)

X_test = x_scaler.transform(X_test)
if predict_class:
    y_test = y_test.astype(int)
if not predict_class:
    y_test = y_scaler.transform(y_test)





print("before reshape : X_test  = ", X_test)
#X_test = X_test.reshape(331, 7, 1)
dim1b, dim2b = X_test.shape

X_test = X_test.reshape(dim1b, deepness_x, 1)

# Store prediction into y_pred:
y_pred = reg.predict(X_test)

if predict_class:
    y_prediction2 = np.argmax(y_pred, axis=1)
    print("y_prediction2 = ", y_prediction2)
    #y_prediction3 = y_prediction2.numpy()
    #print("y_prediction3 = ", y_prediction3)

# Bring y_pred values to their original forms using inverse_transform:
if not predict_class:
    y_pred = y_scaler.inverse_transform(y_pred)
print(y_pred)
y_true = y_test
if not predict_class:
    y_true = y_scaler.inverse_transform(y_test)
print(y_true)

# Show evaluation:
total_score, scores = evaluate_model(y_true, y_pred)
print("total_score=", total_score, "scores=", scores)

#accuracy = np.dot(0.0, np.equal(y_true, np.argmax(y_pred, axis=1)))
accuracy = np.sum(np.equal(y_true, y_pred)) / len(y_true)
print("accuracy = ", accuracy)


# Find standard deviation:
dim1c, dim2c = y_true.shape
array_std = []
for row_idx in range(y_true.shape[0]):
    std = np.std(y_true[row_idx])
    array_std.append(std)
print("standatd deviations = ", array_std)
array_std2 = np.array(array_std)
mean_std = np.mean(array_std2)
print("mean of standatd deviation = ", mean_std)


# Since the mean squared error is less than the standard deviation, the performance of this model can be considered good.

score, acc = reg.evaluate(X_test, y_test, batch_size=1)
print('Test score:', score)
print('Test accuracy:', acc)



display_prediction_results(indexes_test, y_true, y_test)
