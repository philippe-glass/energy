import sys
import os
sys.path.append(os.path.abspath('/home/glassp/.local/lib/python3.10/site-packages'))
print (sys.path)

import subprocess
import time
from util_register import *


# sensorname='SE05000283', blob_filter='2022-04-27'
def retrieve_blobs_files_clemap(root_dir, sensorname, blob_filter, cursor, dbConnection, dictFeatureTypes, nbRecordsByBlobAndSensor, dictPersitentIncompleteImports, dictExistingNbOfRecords, delimiter=None , is_test=False, log_level=0):
    """Lists all the blobs in the bucket."""
    prefix = sensorname + "/" + blob_filter

    # bucket_name = "your-bucket-name"
    # local_dir = '$HOME/exoscale/' + sensorname
    # local_dir = '/home/philippe/exoscale/' + sensorname
    local_dir = root_dir + "/" + sensorname
    # remote_filter = 's3://data-smart-grid/' + sensorname + '/2022-06-2*'
    remote_filter = 's3://data-smart-grid/' + sensorname + '/' + blob_filter
    # next_command = 's3cmd --config unige-meyrin-bucket-key.s3cfg get ' + remote_filter + ' ' + local_dir + '/ --force'
    next_command = 's3cmd get ' + remote_filter + ' ' + local_dir + '/ --skip-existing'
    print("next_command = " + next_command)
    res = os.system(next_command)


    # res = command.run([next_command])
    print("step1", sensorname, local_dir)

    filenames = []
    with os.popen("ls " + local_dir) as f:
        # filenames.append(f)
        print("f1", f, f.name)


    # dirs=directories
    files_filter = local_dir + "/" + blob_filter
    files = glob.glob(files_filter)
    # print("_files:", files)
    # for path in os.listdir(local_dir ):
    for path in files:
        # print("next file path", path)
        if os.path.isfile(os.path.join(local_dir, path)):
            file_name = os.path.basename(path)
            filenames.append(file_name)

    # print(res.output)
    if(log_level > 999):
        print("filenames", filenames)

    # retrieve nb of records by sensor and blob name
    # blob_filter='2022-07-12'
    # blob_filter2 = blob_filter.replace("*", "")
    if(log_level > 9990):
        print("existingNbOfRecords2", blob_filter, dictExistingNbOfRecords)

    result = []
    tuples_to_insert = []
    logs_to_insert = []
    for filename in filenames:
        # print(filename)

        # Construct a client side representation of a blob.
        # Note `Bucket.blob` differs from `Bucket.get_blob` as it doesn't retrieve
        # any content from Google Cloud Storage. As we don't need additional data,
        # using `Bucket.blob` is preferred here.
        # blob = bucket.blob(source_blob_name)
        # directory_dest= "./SE05000283/"
        # directory_dest = blob.name
        # print("filename = ", filename)
        record_key = sensorname + "." + filename
        nbRcords = 0
        # print("record_key = ", record_key)
        if (record_key in dictExistingNbOfRecords):
            nbRcords = dictExistingNbOfRecords[record_key]
        # nbRcords = get_nb_records(cursor, filename, sensorname)
        sNbRecords = str(nbRcords) + "/" + str(nbRecordsByBlobAndSensor)
        if (nbRcords == nbRecordsByBlobAndSensor):
            if(log_level>99990):
                print("retrieve_blobs_files_clemap : " + sNbRecords + " records already in database for blob " + filename + " " + sensorname)
        elif(nbRcords > nbRecordsByBlobAndSensor):
            print("### retrieve_blobs_files_clemap : " + sNbRecords + " records already in database for blob " + filename + " " + sensorname)
        elif(record_key in dictPersitentIncompleteImports):
            print("retrieve_blobs_files_clemap : " + sNbRecords + " persistent and incomplete records already in database for blob " + filename + " " + sensorname)
        else:
            # directory_dest = "SE05000283/download.raw"
            directory_dest = sensorname + "/" + filename
            file_path = local_dir + "/" + filename
            if(True):
                # Clean
                simport_time = time.strftime('%Y-%m-%d %H:%M:%S')
                test_clean_measures(cursor, filename, sensorname)
                to_add = retrieve_from_raw_file(file_path, filename, sensorname, dictFeatureTypes)
                tuples_to_insert = tuples_to_insert + to_add
                # print("retrieve_blobs_files_clemap", filename, sensorname, "nbRcords", nbRcords)
                next_log = (filename, sensorname, len(to_add), nbRecordsByBlobAndSensor, 1, simport_time, len(to_add))
                logs_to_insert.append(next_log)
                print("retrieve_blobs_files_clemap tuples_to_insert len = ", len(tuples_to_insert))
                if (len(tuples_to_insert) > 10 * 1000):
                    test_register_tuples(cursor, dbConnection, tuples_to_insert, logs_to_insert)
                    tuples_to_insert = []
                    logs_to_insert = []
            else:
                if (nbRcords > 0):
                    print("### register_file_content : " + sNbRecords + " blob no completed : " + filename + " " + sensorname)
                    reqDelete1 = "DELETE phase_measure_record FROM phase_measure_record WHERE id_measure_record IN (SELECT id FROM measure_record WHERE blob_name='" + filename + "' AND sensor_number='" + sensorname + "')"
                    reqDelete2 = "DELETE measure_record FROM measure_record WHERE blob_name='" + filename + "' AND sensor_number='" + sensorname + "'"
                    print("retrieve_blobs_files_clemap : reqDelete1 = ", reqDelete1)
                    cursor.execute(reqDelete1)
                    print("retrieve_blobs_files_clemap : reqDelete2 = ", reqDelete2)
                    cursor.execute(reqDelete2)
                register_file_content(file_path, filename, sensorname, cursor, dbConnection, dictFeatureTypes)
                nbNewRecords = get_nb_records1(cursor, filename, sensorname)
                print("retrieve_blobs_files_clemap : nbNewRecords = ", nbNewRecords)
                if(nbNewRecords > 0):
                    if(nbNewRecords < nbRecordsByBlobAndSensor or nbRcords < nbRecordsByBlobAndSensor):
                        sqlLogImport = "INSERT INTO log_import_clemap(blob_name,sensor_number,nb_records,nb_records_required,nb_imports,last_import_date)"
                        sqlLogImport = sqlLogImport + " VALUES ('" + filename + "' ,  '" + sensorname + "', " + str(nbNewRecords) + "," + str(nbRecordsByBlobAndSensor) + ",1, NOW())"
                        sqlLogImport = sqlLogImport + " ON DUPLICATE KEY UPDATE nb_records = " + str(nbNewRecords) + ", nb_imports=1+nb_imports, last_import_date=NOW()"
                        print("sqlLogImport = " + sqlLogImport)
                        cursor.execute(sqlLogImport)
        result.append(filename)
    # End
    if(True and len(tuples_to_insert) > 0):
        test_register_tuples(cursor, dbConnection, tuples_to_insert, logs_to_insert)
    return result










def retrieve_from_raw_file(file_path, blob_name, sensorname, dictFeatureTypes):
    # print("retrieve_from_raw_file "  + blob_name + " (" + sensorname + ") "  )
    f = open(file_path, 'rb')
    list_tuples = []
    while True:
        binary = f.read(1)
        try:
            test = binary[0]
        except:
            # print("_end of file " + blob_name + " (" + sensorname + ") ")
            break
        err, data = PayloadDecoder().decode_msg_type(binary)
        if not err:
            binary += f.read(data - 1)
            # print(len(binary))
            err, data = PayloadDecoder().decode_feature(binary)
            if not err:
                id_feature_type = data['feature_type']
                if id_feature_type in dictFeatureTypes.keys():
                    featureType = dictFeatureTypes[id_feature_type]
                    # print("data step2", data)
                    timestamp = datetime.datetime.fromtimestamp(data['timestamp'])
                    sqlTimeStamp = timestamp.strftime('%Y-%m-%d %H:%M:%S')
                    # print(sqlTimeStamp, cursor)
                    sensor_number2 = str(data['sensor_id'])
                    if featureType == 'MN':  # one minute
                        next_tuple = (sqlTimeStamp, blob_name, featureType, str(data['sensor_id']), sensorname
                                     , data['v_l1'], data['i_l1'], data['s_l1'], data['p_l1'], data['q_l1'], data['pf_l1'],
                                     data['phi_l1'], data['avg_energy_l1']
                                     , data['v_l2'], data['i_l2'], data['s_l2'], data['p_l2'], data['q_l2'], data['pf_l2'],
                                     data['phi_l2'], data['avg_energy_l2']
                                     , data['v_l3'], data['i_l3'], data['s_l3'], data['p_l3'], data['q_l3'], data['pf_l3'],
                                     data['phi_l3'], data['avg_energy_l3'])
                        list_tuples.append(next_tuple)
                    elif featureType == 'TEN_SEC':  # 10 seconds
                        next_tuple = (sqlTimeStamp, blob_name, featureType, str(data['sensor_id']), sensorname
                                      , None, None, None, data['p_l1'], data['q_l1'],None, None, None
                                      , None, None, None, data['p_l2'], data['q_l2'],None, None, None
                                      , None, None, None, data['p_l3'], data['q_l3'],None, None, None)
                        list_tuples.append(next_tuple)
    f.close()
    # print("retrieve_from_raw_file " + blob_name + " (" + sensorname + ") ", "tuple len ", len(list_tuples))
    return list_tuples







def register_file_content(file_path, blob_name, sensorname, cursor, dbConnection, dictFeatureTypes):
    print("register_file_content "  + blob_name + " (" + sensorname + ") "  )
    f = open(file_path, 'rb')
    while True:
        binary = f.read(1)
        try:
            test = binary[0]
        except:
            print("end of file " + blob_name + " (" + sensorname + ") " )
            break
        err, data = PayloadDecoder().decode_msg_type(binary)
        if not err:
            binary += f.read(data - 1)
            # print(len(binary))
            err, data = PayloadDecoder().decode_feature(binary)
            if not err:
                id_feature_type = data['feature_type']
                if id_feature_type in dictFeatureTypes.keys():
                    featureType = dictFeatureTypes[id_feature_type]
                    # print("data step2", data)
                    timestamp = datetime.datetime.fromtimestamp(data['timestamp'])
                    sqlTimeStamp = timestamp.strftime('%Y-%m-%d %H:%M:%S')
                    # print(sqlTimeStamp, cursor)
                    sensor_number2 = str(data['sensor_id'])

                    cursor.execute(
                        "INSERT INTO measure_record (timestamp,blob_name, feature_type,sensor_number2, sensor_number) VALUES (?, ?, ?, ? , ?)",
                        (sqlTimeStamp, blob_name, featureType, str(data['sensor_id']), sensorname))
                    recordId = cursor.lastrowid
                    # print('recordId', recordId)
                    if featureType == 'MN':  # one minute
                        values = [(recordId, 'l1', data['v_l1'], data['i_l1'], data['s_l1'], data['p_l1'], data['q_l1'],
                                   data['pf_l1'], data['phi_l1'], data['avg_energy_l1']),
                                  (recordId, 'l2', data['v_l2'], data['i_l2'], data['s_l2'], data['p_l2'], data['q_l2'],
                                   data['pf_l2'], data['phi_l2'], data['avg_energy_l2']),
                                  (recordId, 'l3', data['v_l3'], data['i_l3'], data['s_l3'], data['p_l3'], data['q_l3'],
                                   data['pf_l3'], data['phi_l3'], data['avg_energy_l3']), ]
                        cursor.executemany(
                            "INSERT INTO phase_measure_record (id_measure_record, phase, voltage, intensity, power_s, power_p, power_q, power_factor, power_phi, avg_energy) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            values)
                    elif featureType == 'TEN_SEC':  # 10 seconds
                        values = [(recordId, 'l1', data['p_l1'], data['q_l1']),
                                  (recordId, 'l2', data['p_l2'], data['q_l2']),
                                  (recordId, 'l3', data['p_l3'], data['q_l3']), ]
                        cursor.executemany(
                            "INSERT INTO phase_measure_record (id_measure_record, phase, power_p, power_q) VALUES (?, ?, ?, ?)",
                            values)

        dbConnection.commit()
        # print(f"Last Inserted ID: {cursor.lastrowid}")


def get_list_sensors():
    result = []
    next_command = 's3cmd ls s3://data-smart-grid/SE*'
    print("next_command = " + next_command)
    result1 = subprocess.check_output(next_command, shell=True)
    result1b = result1.decode("UTF8")
    result1c = result1b.split("\n")
    for remote_dir in result1c:
        index = remote_dir.find("s3://data-smart-grid")
        if index>=0:
            index2 = index + len("s3://data-smart-grid")
            next_item = remote_dir[index2:]
            next_item = next_item.replace("/", "")
            # next_item = remote_dir.replace("s3://data-smart-grid", "")
            print("get_list_sensors : remote_dir ", remote_dir, " next_item", next_item)
            result.append(next_item)
    return result

def main(argv):
    today = datetime.date.today()
    print(today)
    # Options by default :
    year = today.year
    month = today.month
    is_light = False
    is_test = False
    try:
        print("argv:", sys.argv[1:])
        for next_arg in sys.argv:
            print("Argument: ", next_arg)
            if(next_arg=='-light'):
                is_light = True
                print("Light option is demanded")
            elif(next_arg == '-test'):
                is_test = True
                print("Test option is demanded")
            elif (next_arg.startswith("-") and (next_arg.find(':') > 0) ):
                next_arg2 = next_arg[1:]
                tab = next_arg2.split(':')
                option = tab[0]
                if(len(tab) > 1) :
                    svalue = tab[1]
                    if(option == "month"):
                        month = int(svalue)
                        print('month option set to ' + str(month))
                    elif (option == "year"):
                        year = int(svalue)
                        print('year option set to ' + str(year))
                    elif(option == "log_level"):
                        log_level = int(svalue)
                        print('log level is set to ' + str(log_level))

        filenames = sys.argv[1:]
    except  Exception as e:
        print(e)
        print("usage: python3 register_measures_clemap.py <filename of file with binary data>\n")
        exit()

    s_l1 = []
    index = []

    try:

        root_dir = '/home/philippe/exoscale'
        root_dir = '~/exoscale'
        root_dir = '/home/glassp/exoscale/raw_files'
        log_level = 0
        dictFeatureTypes = {16: 'TEN_SEC', 17: 'MN'}
        nbRecordsByBlobAndSensor = 3*60*7
        dbname = "clemap_data"
        if(is_light):
            dbname = "clemap_data_light"
            if(is_test):
                dbname = "clemap_test"
            dictFeatureTypes = {17: 'MN'}
            nbRecordsByBlobAndSensor = 3*60*1
        timestamp = datetime.datetime.fromtimestamp(1500000000)
        print(timestamp.strftime('%Y-%m-%d %H:%M:%S'))
        dbConnection = mariadb.connect(
            user="import_clemap",
            password="sql2537",
            host="localhost",
            port=3306,
            database=dbname + ""
        )
        # Get Cursor
        cursor = dbConnection.cursor()
        # list_blobs(bucket_name=sys.argv[1], prefix=sys.argv[2])#, delimiter=sys.argv[3])
        #  prefix='SE05000283/2022-04-27',
        # SE05000283, SE05000319,SE05000238,
        # SE05000160 pb : very big file
        # Empty : SE05000318, SE05000281, SE05000282, SE05000159, SE05000163

        # blob_filter='2022-09'
        smonth = str(month).zfill(2)
        blob_filter = str(year) + '-' + smonth

        dictPersitentIncompleteImports = get_persitent_incomplete_imports(cursor, blob_filter)
        print("dictPersitentIncompleteImports", dictPersitentIncompleteImports)
        dictExistingNbOfRecords = get_nb_records_by_blob_sensor(cursor, blob_filter)
        if(log_level>10):
            print("dictExistingNbOfRecords", dictExistingNbOfRecords)
        blob_filter2 = blob_filter + "*"
        list_sensors = get_list_sensors()
        print("list_sensors = ", list_sensors)
        # list_sensors = ["SE05000283", "SE05000319","SE05000238", "SE05000160", "SE05000318", "SE05000281", "SE05000282", "SE05000159", "SE05000163"]
        # list_sensors = ["SE05000318", "SE05000281", "SE05000282","SE05000159", "SE05000163""]
        # list_sensors = ["SE05000318", "SE05000281", "SE05000282","SE05000159"]
        for next_sensor_number in list_sensors:
            path = root_dir + "/"+next_sensor_number
            isExist = os.path.exists(path)
            if not isExist:
                # Create a new directory because it does not exist
                os.makedirs(path)
            # print("call retrieve_blobs_files for sensor " + next_sensor_number)
            filenames1 = retrieve_blobs_files_clemap(root_dir=root_dir, sensorname=next_sensor_number, blob_filter=blob_filter2, cursor=cursor, dbConnection=dbConnection
                                          , dictFeatureTypes=dictFeatureTypes, nbRecordsByBlobAndSensor=nbRecordsByBlobAndSensor
                                         , dictPersitentIncompleteImports=dictPersitentIncompleteImports
                                         , dictExistingNbOfRecords=dictExistingNbOfRecords, is_test=is_test, log_level=log_level)
            if(len(filenames1 )==0):
                print("### No data found for sensor " + next_sensor_number )
            else:
                print("--- for sensor " + next_sensor_number + " nb blobs = " + str(len(filenames1)))
        if(log_level > 0) :
            print("filenames1", filenames1)

    except mariadb.Error as e:
        print(f"Error connecting to MariaDB Platform: {e}")
        sys.exit(1)

    dbConnection.close()
    #df1.plot()
    #print(df1)
    #print(df1.sort_index())

    e = datetime.datetime.now()
    dump_file="/home/glassp/exoscale/dump_" + e.strftime("%Y-%m-%d_%H-%M-%S") + ".sql"
    dump_file="./dump/dump_" + e.strftime("%Y-%m-%d_%H-%M-%S") + ".sql"
    dump_file="./dump/dump_last.sql"
    if(False):
        dump_file="./dump/dump_light_last.sql"
        dump_command="mysqldump " + dbname + " --user=import_clemap --password=sql2537 >> " + dump_file
        print("dump_command=", dump_command)
        res = os.system(dump_command)
    # dump_command2 = "cp " + dump_file + " /home/glassp/exoscale/dump_last.sql" 
    # dump_command2 = "cp " + dump_file + " ./dump/dump_last.sql"
    # print("dump_command2 =", dump_command2)
    # res = os.system(dump_command2)

if __name__ == '__main__':

    main(sys.argv[1:])




    # plt.show()

# usage exemple :
# python plot_power.py SE05000283/2022-04-30_09\:30.raw SE05000283/2022-04-30_10\:30.raw
