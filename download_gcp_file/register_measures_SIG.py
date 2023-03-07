import csv, sys
import os
sys.path.append(os.path.abspath('/home/glassp/.local/lib/python3.10/site-packages'))
import mariadb
import sys
import subprocess
import util_register
import time

from util_register import *




def register_file_content_SIG(dictExistingNbOfRecords):
    nbRecords = 0
    if filename in dictExistingNbOfRecords:
        nbRecords = dictExistingNbOfRecords[filename]
    print("register_file_content_SIG : filename = ", filename, "nbRecords = ", nbRecords)
    table_tuples_to_insert = {}
    minRequired = 3 * 28 * 96  # = 8064 = 3 * 28 * 96 : (nb of sensors) (min day nb in a month) * (nb of measyre per day)
    if (nbRecords < minRequired):
        test_clean_measures2(cursor, filename)
        with open(path) as csvfile:
            print("read csv file ", path)
            reader = csv.DictReader(csvfile, delimiter=';')
            try:
                for row in reader:
                    sensor_number = row['Point de mesure']
                    date_key = 'Date de début'
                    hour_key = 'Heure de début'
                    if is_windows:
                        date_key = 'Date de dÃ©but'
                        date_key = 'Date de dÃ©but'
                    date_dMY = row[date_key]
                    date_YMd = date_dMY[6:10] + '-' + date_dMY[3:5] + '-' + date_dMY[0:2]
                    sqlTimeStamp = date_YMd + " " + row[hour_key]
                    value1 = row['Valeur']
                    value2 = value1.replace(",", ".")
                    energy_KWH = float(value2)
                    power_W = 4 * 1000 * energy_KWH
                    next_tuple = (sqlTimeStamp, filename, '15_MN', sensor_number, '', power_W)
                    if not (sensor_number in table_tuples_to_insert):
                        # print("step1 : sensor_number = ", sensor_number)
                        new_tab = []
                        table_tuples_to_insert[sensor_number] = new_tab
                    tuples_to_insert = table_tuples_to_insert[sensor_number]
                    tuples_to_insert.append(next_tuple)
                    # print("next_tuple", next_tuple)

            except csv.Error as e:
                sys.exit('file {}, line {}: {}'.format(filename, reader.line_num, e))
        for sensor_number in table_tuples_to_insert:
            tuples_to_insert = table_tuples_to_insert[sensor_number]
            print(sensor_number, len(tuples_to_insert))
            simport_time = time.strftime('%Y-%m-%d %H:%M:%S')
            next_log = (filename, sensor_number, len(tuples_to_insert), 96 * 30, 1, simport_time, len(tuples_to_insert))
            logs_to_insert = []
            logs_to_insert.append(next_log)
            register_tuples_phase1(cursor, dbConnection, tuples_to_insert, logs_to_insert)
    return table_tuples_to_insert

is_light = False
is_test = False
print("argv:", sys.argv[1:])
for next_arg in sys.argv:
    print("Argument: ", next_arg)
    if(next_arg == '-light'):
        is_light = True
        print("Light option is required")
    elif (next_arg == '-test'):
        is_test = True
        print("Test option is demanded")

dbname = "clemap_data"
if(is_light):
    dbname = "clemap_data_light"
    if (is_test):
        dbname = "clemap_test"


print("dbname = ", dbname)



dbConnection = mariadb.connect(
             user="import_clemap",
             password="sql2537",
             host="localhost",
             port=3306,
             database=dbname
         )

cursor = dbConnection.cursor()

is_windows = sys.platform.startswith('win')




blob_filter = 'SIG*'
sql_blob_filter = blob_filter.replace("*", "")

dictExistingNbOfRecords = get_nb_records_by_blob(cursor, sql_blob_filter)
print("existingNbOfRecords B", dictExistingNbOfRecords)

# blob_filter = '2022-05/SIG*'
root_dir = '/home/glassp/exoscale/raw_files'
local_dir = root_dir + '/sig_vergers_school/'



next_command = 's3cmd ls s3://data-smart-grid/sig_vergers_school/*'
print("next_command = " + next_command)


result1 = subprocess.check_output(next_command, shell=True)
result1b = result1.decode("UTF8")
result1c = result1b.split("\n")

print("result1b=", result1b)




list_local_dir = []
for resultItem in result1c:
    # print("resultItem=", resultItem)
    idxDir = resultItem.find("DIR")
    if idxDir!=-1:
        idx1 = idxDir+3
        idx2 = len(resultItem)
        resultItem2 = resultItem[idx1:idx2]
        remote_dir = resultItem2.replace(" ", "")
        # print("resultItem=", resultItem, idxDir, "resultItem2=", resultItem2, resultItem2.replace(" ", ""))
        local_dir = root_dir + remote_dir.replace("s3://data-smart-grid", "")
        isExist = os.path.exists(local_dir)
        if not isExist:
            # Create a new directory because it does not exist
            os.makedirs(local_dir)
        print("remote_dir = ", remote_dir, "local_dir = ", local_dir)
        # remote_filter = 's3://data-smart-grid/sig_vergers_school/' + blob_filter
        next_command = 's3cmd get ' + remote_dir + 'SIG* ' + local_dir + ' --skip-existing'
        print("next_command = " + next_command)
        res = os.system(next_command)
        list_local_dir.append(local_dir)



for local_dir in list_local_dir:
    print("next local_dir", local_dir)
    filenames = []
    files_filter = local_dir + "/" + "SIG*"
    files = glob.glob(files_filter)
    # print("_files:", files)
    # for path in os.listdir(local_dir ):
    for path in files:
        # print("next file path", path)
        if os.path.isfile(os.path.join(local_dir, path)):
            filename = os.path.basename(path)
            filenames.append(filename)
            print("filename", filename)
            register_file_content_SIG(dictExistingNbOfRecords)

dbConnection.close()
