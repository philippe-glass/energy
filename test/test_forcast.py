import requests, json
import sys

#host = ""
arg = sys.argv
print(arg)

#url = "http://" + host + ":9191/energy/getForcasting"
url = "http://127.0.0.1:5000/predict"
host = "127.0.0.1"
print("host:",  host, "url:", url)
params = {"2021-09-11 12:30:00+0000":0.129,"2021-09-11 13:00:00+0000":0.066,"2021-09-11 11:00:00+0000":0.128,"2021-09-11 12:00:00+0000":0.071,"2021-09-11 11:30:00+0000":0.067,"2021-09-11 12:45:00+0000":0.124,"2021-09-11 10:45:00+0000":0.124,"2021-09-11 11:15:00+0000":0.107,"2021-09-11 11:45:00+0000":0.066,"2021-09-11 12:15:00+0000":0.132}
data1 = json.dumps(params)
print("data1 = ", data1)
header = { "Content-Type": "application/json" , "charset":"utf-8"}
response_decoded_json = requests.post(url, data=data1, headers=header)
response_json = response_decoded_json.json()
print("response", response_json)
