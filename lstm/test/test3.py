import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers

model = keras.Sequential()
model.add(layers.LSTM(64, input_shape=(None, 28)))
model.add(layers.BatchNormalization())
model.add(layers.Dense(10))
print(model.summary())


mnist = keras.datasets.mnist
print("mnist = ", mnist)
(x_train, y_train), (x_test, y_test) = mnist.load_data()
x_train, x_test = x_train/255.0, x_test/255.0
x_validate, y_validate = x_test[:-10], y_test[:-10]
x_test, y_test = x_test[-10:], y_test[-10:]

model.compile(
    loss=keras.losses.SparseCategoricalCrossentropy(from_logits=True),
    optimizer="sgd",
    metrics=["accuracy"],
)

model.fit(x_train, y_train, validation_data=(x_test, y_test), batch_size=64, epochs=1)
#model.fit(x_train, y_train, validation_data=(x_validate, y_validate), batch_size=64, epochs=10)

for i in range(10):
    next_x_test = tf.expand_dims(x_test[i], 0)
    y_prediction1 = model.predict(next_x_test)
    y_prediction2 = tf.argmax(y_prediction1, axis=1)
    y_prediction3 = y_prediction2.numpy()
    print("y_prediction3 = ", y_prediction3)
    #predictions = np.argmax(model.predict(x_test),axis=1)
    print("predicted : ", y_prediction3, "true : ", y_test[i])


# Generate generalization metrics
score = model.evaluate(x_test, y_test, verbose=1)
print(f'Test loss: {score[0]} / Test accuracy: {score[1]}')
