

import numpy as np # linear algebra
import pandas as pd # data processing, CSV file I/O (e.g. pd.read_csv)
import warnings
import sys, traceback
warnings.filterwarnings("ignore") # hide warnings

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


debug_level = 0

#df = pd.read_csv("/kaggle/input/hourly-energy-consumption/AEP_hourly.csv")
#dataset = pd.read_csv('../sapereapi/dump_history_N1.csv', parse_dates=True, index_col='date_time', low_memory=False, date_format="%d/%m/%Y %H:%M:%S")
df = pd.read_csv("../history_data/dump_history_N1.csv")
if debug_level > 0:
    print("="*50)
    print("First Five Rows ","\n")
    print(df.head(2),"\n")

    print("="*50)
    print("Information About Dataset","\n")
    print(df.info(),"\n")

    print("="*50)
    print("Describe the Dataset ","\n")
    print(df.describe(),"\n")

    print("="*50)
    print("Null Values t ","\n")
    print(df.isnull().sum(),"\n")


# Extract all Data Like Year MOnth Day Time etc
dataset = df
#dataset["Datetime"] = pd.to_datetime(df["date_time"])
# "format='%Y-%m-%d %H:%M:%S'"
dataset["Month"] = pd.to_datetime(df["date_time"], format='%d/%m/%Y %H:%M:%S').dt.month
dataset["Year"] = pd.to_datetime(df["date_time"], format='%d/%m/%Y %H:%M:%S').dt.year
dataset["Date"] = pd.to_datetime(df["date_time"], format='%d/%m/%Y %H:%M:%S').dt.date
dataset["Time"] = pd.to_datetime(df["date_time"], format='%d/%m/%Y %H:%M:%S').dt.time
dataset["Week"] = pd.to_datetime(df["date_time"], format='%d/%m/%Y %H:%M:%S').dt.isocalendar().week
dataset["Day"] = pd.to_datetime(df["date_time"], format='%d/%m/%Y %H:%M:%S').dt.day_name()
dataset = df.set_index("date_time")
dataset.index = pd.to_datetime(dataset.index, format='%d/%m/%Y %H:%M:%S' )
# "%m/%d/%Y %H:%M:%S"

if debug_level > 0:
    print(dataset.head(10))
    # How many Unique Year do we Have in Dataset
    print(df.Year.unique(),"\n")
    print("Total Number of Unique Year", df.Year.nunique(), "\n")



show_fig1 = False
if show_fig1:
    fig = plt.figure()
    ax1 = plt.subplot2grid((1,1), (0,0))

    style.use('ggplot')

    sns.lineplot(x=df["Year"], y=df["AEP_MW"], data=df)
    sns.set(rc={'figure.figsize':(20,10)})

    plt.title("Energy consumptionnin Year 2004")
    plt.xlabel("Date")
    plt.ylabel("Energy in MW")
    plt.grid(True)
    #plt.legend()
    plt.legend(["This is my legend"], fontsize="x-large")

    for label in ax1.xaxis.get_ticklabels():
        label.set_rotation(90)

    plt.title("Energy Consumption According to Year")
    plt.show()


show_fig2 = False
if show_fig2:
    fig = plt.figure()

    ax1= fig.add_subplot(311)
    ax2= fig.add_subplot(312)
    ax3= fig.add_subplot(313)


    style.use('ggplot')

    y_2004 = dataset.loc["2004"]["AEP_MW"].to_list()
    x_2004 = dataset.loc["2004"]["Date"].to_list()
    ax1.plot(x_2004,y_2004, color="green", linewidth=1.7)


    y_2005 = dataset.loc["2005"]["AEP_MW"].to_list()
    x_2005 = dataset.loc["2005"]["Date"].to_list()
    ax2.plot(x_2005, y_2005, color="green", linewidth=1)


    y_2006 = dataset.loc["2006"]["AEP_MW"].to_list()
    x_2006 = dataset.loc["2006"]["Date"].to_list()
    ax3.plot(x_2006, y_2006, color="green", linewidth=1)


    plt.rcParams["figure.figsize"] = (20,10)
    plt.title("Energy consumptionnin")
    plt.xlabel("Date")
    plt.ylabel("Energy in MW")
    plt.grid(True, alpha=1)
    plt.legend()

    for label in ax1.xaxis.get_ticklabels():
        label.set_rotation(90)

    plt.show()

show_fig3 = False
if show_fig3:
    sns.distplot(dataset["AEP_MW"])
    plt.title("Energy Distribution")
    plt.show()


# print(dataset)
show_fig4 = False
if show_fig4:
    fig = plt.figure()
    ax1= fig.add_subplot(111)

    sns.lineplot(x = df["Month"], y = df["AEP_MW"], data = df)
    # sns.relplot(data=df, x="Time", y="AEP_MW", kind="line")
    plt.title("Energy Consumption vs Time ")
    plt.xlabel("Time")
    plt.grid(True, alpha=1)
    plt.legend()

    for label in ax1.xaxis.get_ticklabels():
        label.set_rotation(90)
    plt.show()

#NewDataSet = dataset[['requested', 'Month', 'Year', 'Week']].resample('D').mean()
NewDataSet = dataset[['requested', 'Month', 'Year', 'Week']].resample('1min').mean()

if debug_level > 0:
    print("Old Dataset ",dataset.shape )
    print("New  Dataset ",NewDataSet.shape)


train_depth = 60
train_depth = 10

sc = MinMaxScaler(feature_range=(0, 1))
#sc = MinMaxScaler(feature_range=(0, 1))

X_Train, X_test, Y_Train, Y_test, indexes_train, indexes_test = prepare_data(NewDataSet, train_depth, 0.002, sc)

if debug_level > 0:
    print("X_Train.shape", X_Train.shape)
X_Train = np.reshape(X_Train, newshape=(X_Train.shape[0], X_Train.shape[1], 1))
if debug_level > 0:
    print("X_Train.shape", X_Train.shape)
Y_Train = np.reshape(Y_Train, newshape=(Y_Train.shape[0]))



regressor = Sequential()
#                                 = 4*(x+51)*50
# LSTM : x=1 h=200      nb_params = 4*(x+h+1)*h = 4* [200+1+1]*200 = 4*202*200 = 161 600
# DENSE :  h=50, o=1   nb_params  h*o+o = 50*1 + 1=51
useLightModel = True
addSeondLayer = True
if useLightModel:
    #regressor.add(LSTM(units = 50, return_sequences = True, activation = 'tanh', input_shape=(X_Train.shape[1], 1)))
    if debug_level > 0:
        print("LSTM input shape = ",1000*X_Train.shape[1],1)
    regressor.add(LSTM(units = 50, activation = 'tanh', return_sequences = True, input_shape = (X_Train.shape[1], 1)))
    # h=units
    # x=second component of input_shape = nb of features (first component of input shape = nb of time steps
    # LSTM x=1   , h=50     nb_params = 4*(x+h+1)*h = 4* [50+1+1]*50 = 4*52*50 = 10 400
    # LSTM x=1000, h=50     nb_params = 4*(x+h+1)*h = 4* [50+1000+1]*50 = 210 200
    regressor.add(Dropout(0.2))
    if addSeondLayer:
        regressor.add(LSTM(units = 50 ))
        # LSTM x=50, h=10    nb_params = 4*(x+h+1)*h = 4*61*10 = 2440
        regressor.add(Dropout(0.2))

    regressor.add(Dense(units = 1)) # ,activation='sigmoid'
    # Dense : in=10 out=1   nb_params = in*out + out = 10*1+1 = 11
else:
    # Adding the first LSTM layer and some Dropout regularisation
    regressor.add(LSTM(units = 50, return_sequences = True, input_shape = (X_Train.shape[1], 1)))
    regressor.add(Dropout(0.2))

    # Adding a second LSTM layer and some Dropout regularisation
    regressor.add(LSTM(units = 50, return_sequences = True))
    regressor.add(Dropout(0.2))

    # Adding a third LSTM layer and some Dropout regularisation
    regressor.add(LSTM(units = 50, return_sequences = True))
    regressor.add(Dropout(0.2))

    # Adding a fourth LSTM layer and some Dropout regularisation
    regressor.add(LSTM(units = 50))
    regressor.add(Dropout(0.2))

    # Adding the output layer
    regressor.add(Dense(units = 1))


# print(regressor.summary())


# Compiling the RNN
regressor.compile(optimizer = 'adam', loss = 'mean_squared_error')   # , metrics = 'accuracy'

#dump_model(regressor, nb_epochs)

nb_epochs = 100
try_load_model = True
load_ok = False
node_name, scope, variable = "N1", "NODE", "requested"
if try_load_model:
    #regressor, load_ok = test_function(regressor, nb_epochs)
    #load_model(regressor, nb_epochs)
    #print("before load_model")
    regressor, load_ok = load_model_weights(regressor, nb_epochs, node_name, scope, variable)
    print("after load_model : load_ok = ", load_ok)


if not load_ok:
    regressor.fit(X_Train, Y_Train, epochs = nb_epochs, batch_size = 32)
    #regressor.fit(X_Train, Y_Train, epochs = 20, batch_size = 32)
    dump_model(regressor, nb_epochs)



# Pass to Model
if debug_level > 0:
    print("X_test.shape = ", X_test.shape)

print("step1")

X_test2 = X_test[0:3,:,:]
if debug_level > 0:
    print("X_test2.shape = ",X_test2.shape)
    print("X_test2 = ",X_test2)
X_test2b =  np.reshape(X_test2, newshape=(X_test2.shape[0], X_test2.shape[1]))
print("step2")
np.savetxt("X_test2b.txt", X_test2b, fmt='%.8f')

print("before regressor.predict")
Y_predicted, Y_predicted2 = None, None
try:
    Y_predicted = regressor.predict(X_test, verbose=0)
    Y_predicted2 = regressor.predict(X_test2, verbose=0)
    if debug_level >= 0:
        print("Y_predicted2 = ",Y_predicted2)
except Exception as err:
    print("Exception", err)
    traceback.print_exc(file=sys.stdout)
print("after regressor.predict")

#Y_predicted = regressor.predict(X_test, batch_size = 64)

display_result = True

# Do inverse Transformation to get Values

if display_result:
    print("Y_predicted.shape = ", Y_predicted.shape)
    if len(Y_predicted.shape) > 2:
        Y_predicted = np.reshape(Y_predicted, newshape=(Y_predicted.shape[0], Y_predicted.shape[1]))
        print("Y_predicted.shape(2) = ", Y_predicted.shape)
    Y_predicted = sc.inverse_transform(Y_predicted)
    Y_true = sc.inverse_transform(Y_test)
    print(Y_predicted.shape)

    show_fig5 = True
    if show_fig5:
        list_true = [x[0] for x in Y_true]
        dates = indexes_test
        list_predicted = [x[0] for x in Y_predicted]
        display_prediction_results(dates, list_true, list_predicted)
    print("list_predicted", list_predicted)

""" """


# Evaluate the model
#loss, acc = regressor.evaluate(X_test, Y_test, verbose=2)
#print("Trained model, accuracy: {:5.2f}%".format(100 * acc))

print("---- ended ----")
