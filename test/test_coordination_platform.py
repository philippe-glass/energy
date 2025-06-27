import requests, json
import sys

host, timestamp, samplingNb = "localhost",  "2021-10-27 21:15:00+0000", 4
arg = sys.argv
print(arg)

if len(arg) > 1:
        host = arg[1]
if len(arg) > 2:
        timestamp = arg[2]
if len(arg) > 3:
        samplingNb = arg[3]

url = "http://" + host + ":9191/energy/getForcasting"
print("host:",  host, "url:", url)
params = {"timestamp": timestamp, "samplingNb": samplingNb}
data1 = json.dumps(params)
print("data1 = ", data1)
header = { "Content-Type": "application/json" , "charset":"utf-8"}
response_decoded_json = requests.post(url, data=data1, headers=header)
response_json = response_decoded_json.json()
print("response", response_json)
