import os

os.environ['TF_ENABLE_ONEDNN_OPTS'] = '0'
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '1'

import functools
import numpy as np
import tensorflow as tf
import pandas as pd
from tensorflow import feature_column
from tensorflow.keras import layers
from sklearn.model_selection import train_test_split

# Creating variables for urls of datasets.
TRAIN_DATA_URL = "https://storage.googleapis.com/tf-datasets/titanic/train.csv"
TEST_DATA_URL = "https://storage.googleapis.com/tf-datasets/titanic/eval.csv"


# Creating variables for paths of datasets.
train_file_path = tf.keras.utils.get_file("train.csv", TRAIN_DATA_URL)
test_file_path = tf.keras.utils.get_file("eval.csv",  TEST_DATA_URL)

#Converting train_file_path into pandas dataframe.
train_df = pd.read_csv(train_file_path, header='infer')
test_df = pd.read_csv(test_file_path, header='infer')



#Take a look titanic dataset.
print(train_df.head())


#Creating the target and featrues variables.
LABEL_COLUMN = 'survived'
LABELS = [0, 1]
# Let's specify file path, batch size, label name, missing value parameters in make_csv_dataset method.
train_ds = tf.data.experimental.make_csv_dataset(
        train_file_path,
        batch_size = 3,
        label_name=LABEL_COLUMN,
        na_value="?",
        num_epochs= 1,
        ignore_errors=True)
# Let's create test dataset as above.
test_ds = tf.data.experimental.make_csv_dataset(
        test_file_path,
        batch_size=3,
        label_name=LABEL_COLUMN,
        na_value="?",
        num_epochs=1,
        ignore_errors=True)

for batch, label in train_ds.take(1):
    print(label)
    for key, value in batch.items():
        print(f"{key}: {value.numpy()}")

# Setting numeric columns
feature_columns = []
# numeric columns
for header in ['age', 'n_siblings_spouses', 'parch', 'fare']:
    feature_columns.append(feature_column.numeric_column(header))

titanic_df = pd.read_csv(train_file_path, header='infer')
titanic_df.describe()


# Bucketizing age columns
age = feature_column.numeric_column('age')
age_buckets = feature_column.bucketized_column(age, boundaries=[23, 28, 35])


#Deteriming categorical columns
h = {}
for col in titanic_df:
    if col in ['sex', 'class', 'deck', 'embark_town', 'alone']:
        print(col, ':', titanic_df[col].unique())
        h[col] = titanic_df[col].unique()

# Converting categorical columns and encoding unique categorical values
sex_type = feature_column.categorical_column_with_vocabulary_list(
      'Type', h.get('sex').tolist())
sex_type_one_hot = feature_column.indicator_column(sex_type)

class_type = feature_column.categorical_column_with_vocabulary_list(
      'Type', h.get('class').tolist())
class_type_one_hot = feature_column.indicator_column(class_type)

deck_type = feature_column.categorical_column_with_vocabulary_list(
      'Type', h.get('deck').tolist())
deck_type_one_hot = feature_column.indicator_column(deck_type)

embark_town_type = feature_column.categorical_column_with_vocabulary_list(
      'Type', h.get('embark_town').tolist())
embark_town_type_one_hot = feature_column.indicator_column(embark_town_type)

alone_type = feature_column.categorical_column_with_vocabulary_list(
      'Type', h.get('alone').tolist())
alone_one_hot = feature_column.indicator_column(alone_type)



# Embeding the "deck" column and reducing its dimension to 3.
deck = feature_column.categorical_column_with_vocabulary_list(
      'deck', titanic_df.deck.unique())
deck_embedding = feature_column.embedding_column(deck, dimension=3)



# Reducing class column
class_hashed = feature_column.categorical_column_with_hash_bucket(
      'class', hash_bucket_size=4)

cross_type_feature = feature_column.crossed_column(['sex', 'class'], hash_bucket_size=5)


feature_columns = []

# appending numeric columns
for header in ['age', 'n_siblings_spouses', 'parch', 'fare']:
    feature_columns.append(feature_column.numeric_column(header))

# appending bucketized columns
age = feature_column.numeric_column('age')
age_buckets = feature_column.bucketized_column(age, boundaries=[23, 28, 35])
feature_columns.append(age_buckets)

# appending categorical columns
indicator_column_names = ['sex', 'class', 'deck', 'embark_town', 'alone']
for col_name in indicator_column_names:
    categorical_column = feature_column.categorical_column_with_vocabulary_list(
        col_name, titanic_df[col_name].unique())
    indicator_column = feature_column.indicator_column(categorical_column)
    feature_columns.append(indicator_column)

# appending embedding columns
deck = feature_column.categorical_column_with_vocabulary_list(
      'deck', titanic_df.deck.unique())
deck_embedding = feature_column.embedding_column(deck, dimension=3)
feature_columns.append(deck_embedding)

# appending crossed columns
feature_columns.append(feature_column.indicator_column(cross_type_feature))

feature_layer = tf.keras.layers.DenseFeatures(feature_columns)
