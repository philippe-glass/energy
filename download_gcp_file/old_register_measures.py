from mqtt_payload_decoder import PayloadDecoder
import sys
import pandas as pd
import matplotlib.pyplot as plt
# import mysql.connector
import mariadb
import datetime
from google.cloud import storage
import os


# sensorname='SE05000283', blob_filter='2022-04-27'
def retrieve_blobs_files(storage_client, bucket_name, sensorname, blob_filter, cursor, dbConnection, dictFeatureTypes, delimiter=None):
    """Lists all the blobs in the bucket."""
    prefix = sensorname + "/" + blob_filter

    # bucket_name = "your-bucket-name"


    # storage_client = storage.Client.from_service_account_json( 'smart-grid-333211-2e2d841b2d83.json')

    bucket = storage_client.bucket(bucket_name)

    # Note: Client.list_blobs requires at leastGOOGLE_APPLICATION_CREDENTIALS package version 1.17.0.
    blobs = storage_client.list_blobs(bucket_name, prefix=prefix, delimiter=delimiter)
    directory = "./"

    result = []
    for blob in blobs:
        print(blob.name)

        # Construct a client side representation of a blob.
        # Note `Bucket.blob` differs from `Bucket.get_blob` as it doesn't retrieve
        # any content from Google Cloud Storage. As we don't need additional data,
        # using `Bucket.blob` is preferred here.
        # blob = bucket.blob(source_blob_name)
        # directory_dest= "./SE05000283/"
        # directory_dest = blob.name
        print("blob.name", blob.name)

        nbRcords = get_nb_records(cursor, blob.name, sensorname)
        if (nbRcords > 0):
            print("register_file_content : " + str(
                nbRcords) + " records already in database for blob " + blob.name + " " + sensorname)
        else:
            # directory_dest = "SE05000283/download.raw"
            directory_dest = sensorname + "/download.raw"
            blob.download_to_filename(directory_dest)
            print(
                "Downloaded storage object {} from bucket {} to local file {}.".format(
                    blob.name, bucket_name, directory_dest
                )
            )
            register_file_content(directory_dest, blob.name, sensorname, cursor, dbConnection, dictFeatureTypes)
        result.append(blob.name)
    return result




def get_nb_records(cursor, blob_name,sensorname):
    cursor.execute("SELECT count(*) AS nbMR FROM measure_record "
                   + " JOIN phase_measure_record ON phase_measure_record.id_measure_record = measure_record.ID "
                     " WHERE blob_name = ? AND sensor_number = ?", (blob_name, sensorname))
    # fetch result
    record = cursor.fetchall()
    nbRcords = 0
    for row in record:
        # print(row)
        nbRcords = row[0]
    return nbRcords


def register_file_content(filename, blob_name, sensorname, cursor, dbConnection, dictFeatureTypes):
    '''
    try:
    except Exception as e:
        print(e)
    '''
    f = open(filename, 'rb')
    while True:
        binary = f.read(1)
        try:
            test = binary[0]
        except:
            print('end of file')
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
                    # print(sqlTimeStamp)
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
                            "INSERT INTO phase_measure_record (id_measure_record, phase,v,i,s,p,q,pf,phi,avg_energy) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            values)
                    elif featureType == 'TEN_SEC':  # 10 seconds
                        values = [(recordId, 'l1', data['p_l1'], data['q_l1']),
                                  (recordId, 'l2', data['p_l2'], data['q_l2']),
                                  (recordId, 'l3', data['p_l3'], data['q_l3']), ]
                        cursor.executemany(
                            "INSERT INTO phase_measure_record (id_measure_record, phase,p,q) VALUES (?, ?, ?, ?)",
                            values)

        dbConnection.commit()
        # print(f"Last Inserted ID: {cursor.lastrowid}")


def main(argv):


    try:

        print(sys.argv[1:])
        filenames = sys.argv[1:]
    except:
        print("usage: python3 mqtt_file_reader.py <filename of file with binary data>\n")
        exit()
    
    s_l1 = []
    index = []

    try:
        print("GOOGLE_APPLICATION_CREDENTIALS:", os.environ['GOOGLE_APPLICATION_CREDENTIALS'])
        storage_client = storage.Client()

        timestamp = datetime.datetime.fromtimestamp(1500000000)
        print(timestamp.strftime('%Y-%m-%d %H:%M:%S'))
        dbConnection = mariadb.connect(
            user="import_clemap",
            password="sql2537",
            host="localhost",
            port=3306,
            database="clemap_data"
        )
        # Get Cursor
        cursor = dbConnection.cursor()
        dictFeatureTypes = {16: 'TEN_SEC', 17: 'MN'}
        # list_blobs(bucket_name=sys.argv[1], prefix=sys.argv[2])#, delimiter=sys.argv[3])
        #  prefix='SE05000283/2022-04-27',
        # SE05000283, SE05000319,SE05000238,
        # SE05000160 pb : very big file
        # Empty : SE05000318, SE05000281, SE05000282, SE05000159, SE05000163

        list_sensors = ["SE05000283", "SE05000319","SE05000238", "SE05000160", "SE05000318", "SE05000281", "SE05000282", "SE05000159", "SE05000163"  ]
        # list_sensors = ["SE05000318", "SE05000281", "SE05000282","SE05000159", "SE05000163""]
        # list_sensors = ["SE05000318", "SE05000281", "SE05000282","SE05000159"]
        for next_sensor_number in list_sensors:
            path="/"+next_sensor_number
            isExist = os.path.exists(path)
            if not isExist:
                # Create a new directory because it does not exist
                os.makedirs(path)
            # print("call retrieve_blobs_files for sensor " + next_sensor_number)
            filenames1 = retrieve_blobs_files(storage_client=storage_client, bucket_name='data-grid',  sensorname=next_sensor_number, blob_filter='2022-06-', cursor=cursor, dbConnection=dbConnection
                                          , dictFeatureTypes=dictFeatureTypes)
            if(len(filenames1 )==0):
                print("### No data found for sensor " + next_sensor_number )
            else:
                print("--- for sensor " + next_sensor_number + " nb blobs = " + str(len(filenames1)))
        print("filenames1", filenames1)

    except mariadb.Error as e:
        print(f"Error connecting to MariaDB Platform: {e}")
        sys.exit(1)

    dbConnection.close()
    #df1.plot()
    #print(df1)
    #print(df1.sort_index())

if __name__ == '__main__':

    main(sys.argv[1:])















    # plt.show()

# usage exemple :
# python plot_power.py SE05000283/2022-04-30_09\:30.raw SE05000283/2022-04-30_10\:30.raw
