import sys
import os
sys.path.append(os.path.abspath('/home/glassp/.local/lib/python3.10/site-packages'))
print (sys.path)
from mqtt_payload_decoder import PayloadDecoder
import sys
import mariadb
import datetime
import os
import command
import glob




def get_nb_records1(cursor, blob_name,sensorname):
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


def get_nb_records_by_blob(cursor, blob_filter):
    sqlReq = "SELECT blob_name, count(*) AS nbMR  FROM measure_record "     \
             + " JOIN phase_measure_record ON phase_measure_record.id_measure_record = measure_record.ID "    \
             + " WHERE blob_name LIKE '"+ blob_filter + "%'"     \
             + " GROUP BY blob_name"
    cursor.execute(sqlReq)
    # fetch result
    record = cursor.fetchall()
    nbRecords = 0
    result = {}
    for row in record:
        # print("get_nb_records2", row)
        nbRecords = row[1]
        key = row[0]
        # print("key:", key)
        result[key] = nbRecords
    return result

def get_nb_records_by_blob_sensor(cursor, blob_filter):
    cursor.execute("SELECT sensor_number, blob_name, count(*) AS nbMR "
                   + " FROM measure_record "
                   + " JOIN phase_measure_record ON phase_measure_record.id_measure_record = measure_record.ID "
                   + " WHERE blob_name LIKE '"+ blob_filter + "%'"
                   + " GROUP BY blob_name, sensor_number")
    # fetch result
    record = cursor.fetchall()
    nbRecords = 0
    result = {}
    for row in record:
        # print("get_nb_records2", row)
        nbRecords = row[2]
        key = row[0] + "." + row[1]
        # print("key:", key)
        result[key] = nbRecords
    return result



def get_persitent_incomplete_imports(cursor, blob_filter):
    cursor.execute("SELECT sensor_number, blob_name, nb_records"
        + " FROM log_import_clemap "
        + " WHERE blob_name LIKE '" + blob_filter + "%' AND nb_imports >= 10 and nb_records < nb_records_required"
		+ " AND  TIMESTAMPDIFF(HOUR,creation_date, last_import_date)  >= 24")
    record = cursor.fetchall()
    nbRecords = 0
    result = {}
    for row in record:
        # print("get_nb_records2", row)
        nbRecords = row[2]
        key = row[0] + "." + row[1]
        # print("key:", key)
        result[key] = nbRecords
    return result


def test_clean_measures2(cursor, blob_name):
    cursor.execute("DELETE phase_measure_record FROM phase_measure_record WHERE id_measure_record IN (SELECT id FROM measure_record WHERE blob_name='" + blob_name + "' AND feature_type = '15_MN') ")
    cursor.execute("DELETE measure_record FROM measure_record WHERE blob_name='" + blob_name + "' AND feature_type = '15_MN'")

def test_clean_measures(cursor, blob_name, sensorname):
    # Clean
    reqDelete1 = "DELETE phase_measure_record FROM phase_measure_record WHERE id_measure_record IN (SELECT id FROM measure_record WHERE blob_name='" + blob_name + "' AND sensor_number='" + sensorname + "')"
    reqDelete2 = "DELETE measure_record FROM measure_record WHERE blob_name='" + blob_name + "' AND sensor_number='" + sensorname + "'"
    cursor.execute(reqDelete1)
    cursor.execute(reqDelete2)


def test_register_tuples(cursor, dbConnection, tuples_to_insert, logs_to_insert ):
    print("test_register_tuples : tuples_to_insert length = ", len(tuples_to_insert))
    cursor.execute("TRUNCATE tmp_import_measure")
    if (len(tuples_to_insert) > 0):
        sqlReq = "INSERT INTO tmp_import_measure (timestamp,blob_name, feature_type,sensor_number2, sensor_number"
        sqlReq = sqlReq + ",l1_voltage, l1_intensity, l1_power_s, l1_power_p, l1_power_q, l1_power_factor, l1_power_phi, l1_avg_energy"
        sqlReq = sqlReq + ",l2_voltage, l2_intensity, l2_power_s, l2_power_p, l2_power_q, l2_power_factor, l2_power_phi, l2_avg_energy"
        sqlReq = sqlReq + ",l3_voltage, l3_intensity, l3_power_s, l3_power_p, l3_power_q, l3_power_factor, l3_power_phi, l3_avg_energy" + ")"
        sqlReq = sqlReq + "VALUES (?, ?, ?, ? , ?       ,?, ?, ?, ?, ?, ?, ?, ?     ,?, ?, ?, ?, ?, ?, ?, ?     ,?, ?, ?, ?, ?, ?, ?, ?)"
        cursor.executemany(sqlReq, tuples_to_insert)
    cursor.execute("INSERT INTO measure_record (timestamp, blob_name, feature_type,sensor_number2, sensor_number)"
                   + " SELECT timestamp, blob_name, feature_type,sensor_number2, sensor_number FROM tmp_import_measure GROUP BY timestamp, feature_type")
    # Set id_measure_record
    cursor.execute(
        "UPDATE tmp_import_measure tmp_import SET id_measure_record = (SELECT mr.id FROM measure_record mr WHERE"
        "          mr.blob_name = tmp_import.blob_name "
        "      AND mr.sensor_number = tmp_import.sensor_number "
        "      AND mr.feature_type = tmp_import.feature_type "
        "      AND mr.timestamp = tmp_import.timestamp  LIMIT 0,1 )")
    if(len(logs_to_insert) > 0):
        cursor.execute(
            "INSERT INTO phase_measure_record (id_measure_record, phase, voltage, intensity, power_s, power_p, power_q, power_factor, power_phi, avg_energy)"
            "        SELECT id_measure_record, 'l1', l1_voltage, l1_intensity, l1_power_s, l1_power_p, l1_power_q, l1_power_factor, l1_power_phi, l1_avg_energy FROM tmp_import_measure"
            " UNION  SELECT id_measure_record, 'l2', l2_voltage, l2_intensity, l2_power_s, l2_power_p, l2_power_q, l2_power_factor, l2_power_phi, l2_avg_energy FROM tmp_import_measure"
            " UNION  SELECT id_measure_record, 'l3', l3_voltage, l3_intensity, l3_power_s, l3_power_p, l3_power_q, l3_power_factor, l3_power_phi, l3_avg_energy FROM tmp_import_measure")
        # Insert logs
        cursor.executemany("INSERT INTO log_import_clemap(blob_name,sensor_number,nb_records,nb_records_required,nb_imports,last_import_date) "
                           "VALUES (?, ?, ?, ? , ?, ?)"
                           " ON DUPLICATE KEY UPDATE nb_records = ?, nb_imports=1+nb_imports, last_import_date=NOW()", logs_to_insert)
    dbConnection.commit()










def register_tuples_phase1 (cursor, dbConnection, tuples_to_insert, logs_to_insert ):
    print("test_register_tuples : tuples_to_insert length = ", len(tuples_to_insert))
    cursor.execute("TRUNCATE tmp_import_measure")
    if (len(tuples_to_insert) > 0):
        sqlReq = "INSERT INTO tmp_import_measure (timestamp,blob_name, feature_type, sensor_number, sensor_number2, l1_power_p)"
        sqlReq = sqlReq + "VALUES (?, ?, ?, ?, ? ,? )"
        cursor.executemany(sqlReq, tuples_to_insert)
    cursor.execute("INSERT INTO measure_record (timestamp, blob_name, feature_type,sensor_number2, sensor_number)"
                   + " SELECT timestamp, blob_name, feature_type,sensor_number2, sensor_number FROM tmp_import_measure GROUP BY timestamp, feature_type")
    # Set id_measure_record
    cursor.execute(
        "UPDATE tmp_import_measure tmp_import SET id_measure_record = (SELECT mr.id FROM measure_record mr WHERE"
        "          mr.blob_name = tmp_import.blob_name "
        "      AND mr.sensor_number = tmp_import.sensor_number "
        "      AND mr.feature_type = tmp_import.feature_type "
        "      AND mr.timestamp = tmp_import.timestamp  LIMIT 0,1 )")
    if(len(logs_to_insert) > 0):
        cursor.execute(
            "INSERT INTO phase_measure_record (id_measure_record, phase, power_p)"
            "        SELECT id_measure_record, 'l1', l1_power_p FROM tmp_import_measure")
        # Insert logs
        cursor.executemany("INSERT INTO log_import_clemap(blob_name,sensor_number,nb_records,nb_records_required,nb_imports,last_import_date) "
                           "VALUES (?, ?, ?, ? , ?, ?)"
                           " ON DUPLICATE KEY UPDATE nb_records = ?, nb_imports=1+nb_imports, last_import_date=NOW()", logs_to_insert)
    dbConnection.commit()