
import pandas as pd
import mariadb
import sys

site = 'Prairie'
year = '2022'
csv_url = 'http://www.cuepe.ch/html/meteo/data-zip/prairie/Prairie_2022-P35.CSV'
df = pd.read_csv(csv_url, sep=',' , skiprows=64)
print(df)

dbname = "clemap_data"

is_light = False
print("argv:", sys.argv[1:])
for x in sys.argv:
    print("Argument: ", x)
    if(x=='-light'):
        is_light = True
        print("Light option is required")

dbname = "clemap_data"
if(is_light):
    dbname = "clemap_data_light"


dbConnection = mariadb.connect(
             user="import_clemap",
             password="sql2537",
             host="localhost",
             port=3306,
             database=dbname
         )

cursor = dbConnection.cursor()


print(df.columns.tolist())

meteo_data = []
for index, row in df.iterrows():
   #print(row)
    test = df.at[index,' doy']
    # print(df.at[index,' doy'], df.at[index,' time'], df.at[index,'   Gh '])
    # print(row['doy'], row['time'], row['Gh'])
    nextTuple =  (int(df.at[index,' doy'])
                  ,int(df.at[index,' time'])
                  ,float(df.at[index,'   se '])
                  , float(df.at[index,'     az '])
                  ,float(df.at[index,'   Gh '])
                  ,float(df.at[index, '   Dh '])
                  ,float(df.at[index, '   Bn '])
                  ,float(df.at[index, '    Ta '])
                  ,float(df.at[index, ' HR '])
                  ,float(df.at[index, '    w  '])
                  ,float(df.at[index, '   vv '])
                  ,float(df.at[index,  ' dv '])
                  ,float(df.at[index,  '   pr   '])
                  ,float(df.at[index,  '  Gn '])
                  ,float(df.at[index,  '   Ge '])
                  ,float(df.at[index,  '   Gs '])
                  ,float(df.at[index,  '   Gw '])
                  ,float(df.at[index,  ' G35e'])
                  ,float(df.at[index,  '  G35s'])
                  ,float(df.at[index,  '  G35w'])
                  ,float(df.at[index, '  G45s'])
                  ,float(df.at[index, ' Gtrack'])
                  ,float(df.at[index, 'G35_ 45'])
                  ,float(df.at[index, 'min sun'])
                  ,float(df.at[index, '    IR '])
                  ,float(df.at[index, '   IRd '])
                  ,float(df.at[index, '  IRup '])
                  )

    meteo_data.append(nextTuple)
# print(meteo_data[0], meteo_data[1])

# meteo_data2 = [(1, 1, 0), (1, 2, 0)]
# print("tuple len = " , len(nextTuple))
print("meteo_data", meteo_data[0:2])



sql = "INSERT INTO meteo_data (doy, hour,  se, az, gh, dh, bn, ta, hr, w, vv, dv, pr, Gn,Ge, gs, gw, g35e, g35s, g35w, g45s, gtrack, g35_45, min_sun, ir, ird, irup)"
sql = sql+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
try:
    cursor.execute("TRUNCATE meteo_data")
    result = cursor.executemany(sql,meteo_data )
    cursor.execute("UPDATE meteo_data SET site='" + site + "', year="+year + ", timestamp=DATE_ADD(MAKEDATE(" + year + ", doy), INTERVAL hour-1  HOUR)")
    cursor.execute("UPDATE meteo_data SET ut_timestamp = UNIX_TIMESTAMP(timestamp)")
    print("result = ", result)
except Exception as e:
    print(e)




dbConnection.commit()
dbConnection.close()



'''

import csv
cr = csv.reader(open(csv_url,"rb"))
for row in cr:
    print(row)
'''