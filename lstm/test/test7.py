

import numpy as np # linear algebra
import pandas as pd # data processing, CSV file I/O (e.g. pd.read_csv)
import warnings
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

#df = pd.read_csv("/kaggle/input/hourly-energy-consumption/AEP_hourly.csv")
df = pd.read_csv("AEP_hourly.csv")
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
dataset["Month"] = pd.to_datetime(df["Datetime"]).dt.month
dataset["Year"] = pd.to_datetime(df["Datetime"]).dt.year
dataset["Date"] = pd.to_datetime(df["Datetime"]).dt.date
dataset["Time"] = pd.to_datetime(df["Datetime"]).dt.time
dataset["Week"] = pd.to_datetime(df["Datetime"]).dt.isocalendar().week
dataset["Day"] = pd.to_datetime(df["Datetime"]).dt.day_name()
dataset = df.set_index("Datetime")
dataset.index = pd.to_datetime(dataset.index)

print(dataset.head(10))


#test1 = dataset[['AEP_MW' ]].resample('D').mean()


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

NewDataSet = dataset[['AEP_MW', 'Month', 'Year', 'Week']].resample('D').mean()


print("Old Dataset ",dataset.shape )
print("New  Dataset ",NewDataSet.shape)





sc = MinMaxScaler(feature_range=(0, 1))

TestData = NewDataSet.tail(100)
Training_Set = NewDataSet.iloc[:,0:1]
Training_Set = Training_Set[:-60]

print("Training Set Shape ", Training_Set.shape)
print("Test Set Shape ", TestData.shape)






sc = MinMaxScaler(feature_range=(0, 1))
Train = sc.fit_transform(Training_Set)
print(Train)

X_Train = []
Y_Train = []

# Range should be fromm 60 Values to END
for i in range(60, Train.shape[0]):

    # X_Train 0-59
    X_Train.append(Train[i-60:i])

    # Y Would be 60 th Value based on past 60 Values
    Y_Train.append(Train[i])

# Convert into Numpy Array
X_Train = np.array(X_Train)
Y_Train = np.array(Y_Train)

print(X_Train.shape)
print(Y_Train.shape)




# Shape should be Number of [Datapoints , Steps , 1 )
# we convert into 3-d Vector or #rd Dimesnsion
X_Train = np.reshape(X_Train, newshape=(X_Train.shape[0], X_Train.shape[1], 1))
print(X_Train.shape)



regressor = Sequential()

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

# Compiling the RNN
regressor.compile(optimizer = 'adam', loss = 'mean_squared_error')


#regressor.fit(X_Train, Y_Train, epochs = 20, batch_size = 32)
regressor.fit(X_Train, Y_Train, epochs = 1, batch_size = 32)



print(TestData.head(5))

print(TestData.shape)
print(NewDataSet.shape)

Df_Total = pd.concat((NewDataSet[["AEP_MW"]], TestData[["AEP_MW"]]), axis=0)

inputs = Df_Total[len(Df_Total) - len(TestData) - 60:].values
print("input shape before reshape", inputs.shape)

# We need to Reshape
inputs = inputs.reshape(-1,1)

print("input shape after reshape", inputs.shape)

# Normalize the Dataset
inputs = sc.transform(inputs)

X_test = []
for i in range(60, 160):
    X_test.append(inputs[i-60:i])

# Convert into Numpy Array
X_test = np.array(X_test)

# Reshape before Passing to Network
X_test = np.reshape(X_test, (X_test.shape[0], X_test.shape[1], 1))

# Pass to Model
predicted_power = regressor.predict(X_test)

# Do inverse Transformation to get Values
predicted_power = sc.inverse_transform(predicted_power)
predicted_power.shape


True_MegaWatt = TestData["AEP_MW"].to_list()
Predicted_MegaWatt  = predicted_power
dates = TestData.index.to_list()

""" 
Machine_Df = pd.DataFrame(data={
    "Date":dates,
    "TrueMegaWatt": True_MegaWatt,
    "PredictedMeagWatt":[x[0] for x in Predicted_MegaWatt ]
})

print(Machine_Df)
"""




show_fig5 = True
if show_fig5:
    True_MegaWatt = TestData["AEP_MW"].to_list()
    Predicted_MegaWatt  = [x[0] for x in Predicted_MegaWatt ]
    dates = TestData.index.to_list()

    fig = plt.figure()
    ax1= fig.add_subplot(111)
    x = dates
    y = True_MegaWatt
    y1 = Predicted_MegaWatt
    plt.plot(x,y, color="red", label = "Original")
    plt.plot(x,y1, color="blue", label = "Predicted")
    # beautify the x-labels
    plt.gcf().autofmt_xdate()
    plt.xlabel('Dates')
    plt.ylabel("Power in MW")
    plt.title("Predicted values")

    plt.legend()
    plt.show()

