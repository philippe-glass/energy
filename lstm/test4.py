# TUTORIAL : https://hackmd.io/@j-chen/rJcDAmBDD

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from numpy import nan

from tensorflow.keras import Sequential
from tensorflow.keras.layers import LSTM, Dense

from sklearn.metrics import mean_squared_error
from sklearn.preprocessing import MinMaxScaler

build_data = False

pd.options.mode.copy_on_write = True

def replace_in_column(df, col_label, col_index, to_replace, reaplced_by):
    column_city = df[col_label]
    column_city.replace(to_replace, reaplced_by, inplace=True)
    df = df.drop(col_label, axis=1)
    df.insert(col_index, col_label, column_city)
    return df


def test_fill_missing():
    # Load data into a DataFrame
    data = {
        'Name': ['Alice', 'Bob', 'Charlie', 'David'],
        'Age': [25, 30, 35, 40],
        'City': ['New York', 'Los Angeles', 'San Francisco', 'Chicago']
    }
    df = pd.DataFrame(data)
    df = replace_in_column(df, "City", 2,  'New York', 'NY')
    print(df)




def fill_missing(df):
    print("fill_missing : begin : length = ", range(len(df)))
    one_day = 24*60
    for col_index, column in enumerate(df.columns):
        replace_value = df.iloc[one_day][column]
        print("fill_missing", col_index, column, "replace_value:", replace_value)
        df = reaplce_column(df, column, col_index, nan ,replace_value)
    print("end fill_missing")
    return df


def evaluate_model(y_true, y_predicted):
    scores = []

    # calculate scores for each day
    for i in range(y_true.shape[1]):
        mse = mean_squared_error(y_true[:, i], y_predicted[:, i])
        rmse = np.sqrt(mse)
        scores.append(rmse)

    # calculate score for whole prediction
    total_score = 0
    for row in range(y_true.shape[0]):
        for col in range(y_predicted.shape[1]):
            total_score = total_score + (y_true[row, col] - y_predicted[row, col])**2
    total_score = np.sqrt(total_score//(y_true.shape[0]*y_predicted.shape[1]))

    return total_score, scores





#test_fill_missing()

if build_data:
    data = pd.read_csv('household_power_consumption.txt', sep=';', parse_dates=True, low_memory=False)

    # Concatenate data and time columns to a single 'data_time' column"
    data['date_time'] = data['Date'].str.cat(data['Time'], sep=' ')
    data.drop(['Date', 'Time'], inplace=True, axis=1)

    # Use Dataframe.set_index to make the first index column the 'date_time' column
    data.set_index(['date_time'], inplace=True)

    # Replace all '?' missing values with a NaN float:
    data.replace('?', nan, inplace=True)

    #Makes data just one array of floating point values, as opposed to a bunch of mixed types:
    data = data.astype('float')

    data = fill_missing(data)

    print(data.head())
    print(data.info())
    print(np.isnan(data).sum())
    print(data.shape)
    data.to_csv('cleaned_data.csv')

dataset = pd.read_csv('cleaned_data.csv', parse_dates=True, index_col='date_time', low_memory=False)

data = dataset.resample('D').sum()

data.to_csv('resampled_data.csv')

print(data.head())

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




years = ['2007', '2008', '2009', '2010']


activate_plot2 = False
if activate_plot2:
    fig, ax = plt.subplots(figsize=(18, 18))
    active_power_ts = data['Global_active_power']
    for i in range(len(years)):
        plt.subplot(len(years), 1, i + 1)
        year = years[i]
        year_filter = str(year)+"-"
        active_power_ts2 = active_power_ts.filter(like=year_filter,  axis=0)
        #active_power_data = data[str(year)]
        plt.plot(active_power_ts2)
        plt.title(str(year), y=0, loc='left')
    plt.show()
    fig.tight_layout()

activate_plot3 = False
if activate_plot3:
    fig, ax = plt.subplots(figsize=(18, 18))
    active_power_ts = data['Global_active_power']
    for i in range(len(years)):
        plt.subplot(len(years), 1, i + 1)
        year = years[i]
        year_filter = str(year)+"-"
        active_power_ts2 = active_power_ts.filter(like=year_filter,  axis=0).hist(bins=200)
        #active_power_data = data[str(year)]
        #active_power_data['Global_active_power'].hist(bins=200)
        plt.title(str(year), y=0, loc='left')
    plt.show()
    fig.tight_layout()



data_train = data.loc[:'2009-12-31', :]['Global_active_power']
data_test  = data.loc['2010-01-01':'2010-12-31', :]['Global_active_power']

print(data_train.shape)
print(data_test.shape)


data_train = np.array(data_train)
X_train, y_train = [], []

for i in range(7, len(data_train) - 7):
    X_train.append(data_train[i - 7:i])
    y_train.append(data_train[i:i + 7])

print("step 1 : len X_train.shape ", len(X_train) )

# Convert into NumPy arrays:
X_train, y_train = np.array(X_train), np.array(y_train)

# Check shape of arrays with:
print(X_train.shape, y_train.shape)

# Print y_train
print(pd.DataFrame(y_train).head())

# Normalize dataset between 0 and 1 with MinMaxScaler:
x_scaler = MinMaxScaler()
X_train = x_scaler.fit_transform(X_train)

y_scaler = MinMaxScaler()
y_train = y_scaler.fit_transform(y_train)

# Check normalized X_train
print(pd.DataFrame(X_train).head())

# Convert to 3-D array:
print("before reshape : X_train  = ", X_train, X_train.shape)
dim1, dim2 = X_train.shape
X_train = X_train.reshape(dim1, dim2, 1)

# Build LSTM model:
# Build sequential model using Keras:
reg = Sequential()
reg.add(LSTM(units = 200, activation = 'tanh', input_shape=(7,1)))
reg.add(Dense(7))

# Decide mean square error as loss function and adam as optimizer:
reg.compile(loss='mse', optimizer='adam')

# Train model:
reg.fit(X_train, y_train, epochs=100)

X_test, y_test = [], []

# Split test data by week:
for i in range(7, len(data_test) - 7):
    added_x = data_test[i - 7:i]
    added_y = data_test[i:i + 7]
    X_test.append(added_x)
    y_test.append(added_y)

# Make data into NumPy array, transform using MinMaxScaler, and reshape into 3-D arrays:
X_test = np.array(X_test)
y_test = np.array(y_test)
X_test, y_test = np.array(X_test), np.array(y_test)

X_test = x_scaler.transform(X_test)
y_test = y_scaler.transform(y_test)

print("before reshape : X_test  = ", X_test)
#X_test = X_test.reshape(331, 7, 1)
dim1b, dim2b = X_test.shape

X_test = X_test.reshape(dim1b, 7, 1)


# Store prediction into y_pred:
y_pred = reg.predict(X_test)

# Bring y_pred values to their original forms using inverse_transform:
y_pred = y_scaler.inverse_transform(y_pred)
print(y_pred)
y_true = y_scaler.inverse_transform(y_test)
print(y_true)

# Show evaluation:
total_score, scores = evaluate_model(y_true, y_pred)
print("total_score=", total_score, "scores=", scores)

# Find standard deviation:
print(np.std(y_true[0]))

# Since the mean squared error is less than the standard deviation, the performance of this model can be considered good.


# Generate generalization metrics
score = reg.evaluate(X_test, y_test, verbose=1)
print("score = ", score)
#print(f'Test loss: {score[0]} / Test accuracy: {score[1]}')
