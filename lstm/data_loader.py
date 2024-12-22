import json
#import mysql.connector
import pandas as pd
import mariadb
import sys
import os
import functools
from datetime import datetime,timedelta

current_node = None


config_file = open('config.json')
dbconfig = json.load(config_file)
print(dbconfig)


def connect_to_db_default():
    return connect_to_db(None)

def connect_to_db(dbname):
    dbname2 = dbconfig["database"]
    if dbname is not None:
        dbname2 = dbname
    print("dbname2", dbname2)

    # Connect to MariaDB Platform
    try:
        connection = mariadb.connect(
            user=dbconfig["user"],
            password=dbconfig["password"],
            host=dbconfig["host"],
            port=dbconfig["port"],
            database=dbname2)
    except mariadb.Error as e:
        print(f"Error connecting to MariaDB Platform: {e}")
        sys.exit(1)


    return connection




def get_node_distance(request, current_node):
    distance = 0
    if not current_node is None:
        if not current_node == request["node"]:
            distance = 1
    return distance

def compare_request(request1, request2):
    dist1, dist2 = request1["current_distance"], request2["current_distance"]
    if not dist1 == dist2:
        return dist1 - dist2
    w1, w2 = 1000*request1["requested"],  1000*request2["requested"]
    return int(w1 - w2)

def aux_select_requests(list_request, current_node, current_available):
    result = []
    for next_request in list_request:
        if next_request["missing"] > 0 and next_request["missing"] <= current_available:
            next_request["current_distance"] = get_node_distance(next_request, current_node )
            result.append(next_request)
    return result


def simulate_transactions(allnodes_histo):
    list_request = allnodes_histo["list_request"]
    allnodes_histo["available"] = allnodes_histo["produced"] # total of all nodes available
    for node in allnodes_histo["nodes"]:
        node_histo = allnodes_histo["nodes"][node]
        #available = next_histo["available"]
        if node_histo["available"] > 0:
            #  sort request by distance with current node and increasing power
            selected_list_request = aux_select_requests(list_request, node, allnodes_histo["available"])
            selected_list_request = sorted(selected_list_request, key=functools.cmp_to_key(compare_request))
            for next_request in selected_list_request:
                # simulate a transation
                if node_histo["available"] > 0.00001:
                    w_exchange = float(min(next_request["missing"], node_histo["available"]))
                    node_histo["available"] = node_histo["available"] - w_exchange
                    allnodes_histo["available"] = allnodes_histo["available"] - w_exchange
                    node_histo["provided"] =  node_histo["provided"] + w_exchange
                    next_request["missing"] = next_request["missing"] - w_exchange
                    if next_request["missing"] > 0.0 and next_request["missing"] <= 0.00001:
                        next_request["missing"] = 0.0
                    next_request["satisfied"] = (next_request["missing"] <= 0.0)
                    node2 = next_request["node"]
                    node_histo_data2 = allnodes_histo["nodes"][node2]
                    node_histo_data2["consumed"] = node_histo_data2["consumed"] + w_exchange
                    node_histo_data2["missing"] = node_histo_data2["missing"] - w_exchange
    # chckup :
    for request in list_request:
        if request["missing"] > 0 and allnodes_histo["produced"] >= allnodes_histo["requested"]:
            print("### not satisfied request ", request, "produced:", allnodes_histo["produced"], "requested:", allnodes_histo["requested"])
    return allnodes_histo



def set_node_in_measure_data(date_current, nb_of_days, node_by_location, default_node):
    connection = connect_to_db_default()
    cursor = connection.cursor()
    sql_req = ""
    date_begin = date_current + timedelta(days=-1*nb_of_days)
    sql_date_bein = "'" + date_begin.strftime('%Y/%m/%d') + "'"
    sql_date_end =  "'" + date_current.strftime('%Y/%m/%d') + "'"
    if(len(node_by_location) > 0) :
        sql_req = "UPDATE TmpMeasure_data SET `Node` = CASE";
        for location in node_by_location:
            node = node_by_location[location]
            sql_req = sql_req +  os.linesep + "      WHEN location = '"  + location + "' THEN '" + node + "'"
        sql_req = sql_req + os.linesep + "      ELSE ''"
        sql_req = sql_req + os.linesep + "END"
    else :
        sql_req = "UPDATE TmpMeasure_data SET `Node` = '" + default_node + "'"
    sql_req = sql_req + os.linesep + "		WHERE date >= " + sql_date_bein
    sql_req = sql_req + os.linesep + "	AND date <= " + sql_date_end
    cursor.execute(sql_req)
    return


def load_allnodes_history_data(date_current, nb_of_days, node_filter):
    list_s_dates = []
    for day_idx in range (nb_of_days):
        next_date = date_current + timedelta(days=-1*day_idx)
        next_s_date =  next_date.strftime('%Y-%m-%d')
        list_s_dates.append(next_s_date)
    # reverse this list
    list_s_dates = list_s_dates[::-1]
    # convert it to a string to prepare the sql "IN" filter
    list_sql_dates = "'" + "','".join(list_s_dates) + "'"
    connection = connect_to_db_default()
    cursor = connection.cursor()
    sql_node_filter = "1"
    if not node_filter is None:
        sql_node_filter = "node = '" + node_filter + "'"
    sql_req = """SELECT timestamp3, Node
    , DAYOFYEAR(timestamp3) 	AS DayOfYear
    , Count(distinct device_name) 	AS count_devices
    , GROUP_CONCAT(distinct serial_number) 	AS serial_number
    , GROUP_CONCAT( power_p 	order by 1*power_p SEPARATOR ',') AS list_power_p
    , GROUP_CONCAT( is_producer order by 1*power_p SEPARATOR ',') AS list_is_producer
    , Count(distinct serial_number) 		AS count_serial_number
    , SUM(IF(is_producer,power_p,0)) 		AS power_p_produced
    , SUM(IF(is_producer,0,power_p))		AS power_p_requested
    , MAX(IF(is_producer,0,power_p))		AS power_p_max_requested
    , Count(*) 								AS test_count
    , MAX(is_producer) 						AS has_producer
    FROM TmpMeasure_data
    -- WHERE date IN ( '2023-01-12','2023-01-13','2023-01-14','2023-01-15')
    -- WHERE date IN ( '2023-01-12')
    WHERE date IN ( """ + list_sql_dates + """)
    GROUP BY timestamp3, Node
    HAVING """ + sql_node_filter + """
    ORDER by timestamp3, Node"""
    cursor.execute(sql_req)
    history_data = {}
    for timestamp3, node, day_of_year, count_devices, serial_number, s_list_power_p, s_list_is_producer, count_serial_number, power_p_produced1, power_p_requested1, power_p_max_requested, test_count, has_producer in cursor:
        print("next row", timestamp3, node, day_of_year, count_devices)
        power_p_produced = float(power_p_produced1)
        power_p_requested = float(power_p_requested1)
        list_power_p = s_list_power_p.split(",")
        list_is_producer = s_list_is_producer.split(",")
        print("list_is_producer", list_is_producer)
        print("list_power_p", list_power_p)
        if not timestamp3 in history_data:
            history_data[timestamp3] = {"produced":0.0, "requested":0.0, "list_request":[], "nodes":{}, "day_of_year":day_of_year}
        next_histo = history_data[timestamp3]
        next_node_histo = {"produced": power_p_produced, "requested": power_p_requested, "available":power_p_produced, "consumed":0.0, "missing":power_p_requested, "provided":0.0}
        next_histo["nodes"][node] = next_node_histo
        next_histo["produced"] = next_histo["produced"] + power_p_produced
        next_histo["requested"] = next_histo["requested"] + power_p_requested
        list_requested = next_histo["list_request"]
        for idx, is_producer in enumerate(list_is_producer):
            if is_producer=='0':
                w_request = float(list_power_p[idx])
                request = {"requested":w_request, "missing":w_request, "node":node, "satisfied":False}
                list_requested.append(request)
        next_histo["list_request"] = list_requested
    for next_datetime in history_data:
        next_histo = history_data[next_datetime]
        next_histo = simulate_transactions(next_histo)
    return history_data



def extract_node_history_data(history_data, node):
    list_dates, list_requested, list_produced, list_consumed, list_missing, list_provided, list_available = [],[],[],[],[],[],[]
    for next_datetime in history_data:
        next_histo = history_data[next_datetime]
        if node in next_histo["nodes"]:
            next_node_histo = next_histo["nodes"][node]
            list_dates.append(next_datetime)
            list_requested.append(next_node_histo["requested"])
            list_produced.append(next_node_histo["produced"])
            list_provided.append(next_node_histo["provided"])
            list_consumed.append(next_node_histo["consumed"])
            list_available.append(next_node_histo["available"])
            list_missing.append(next_node_histo["missing"])
    data_node = {"requested":list_requested, "produced":list_produced, "consumed":list_consumed, "missing":list_missing, "provided":list_provided, "available":list_available}
    result = pd.DataFrame(data_node)
    result.index = data_node["listDates"]
    return result



def loadNodeHistory(scope, node, date_min, date_max):
    list_dates, list_requested, list_produced, list_consumed, list_missing, list_provided, list_available = [],[],[],[],[],[],[]
    date_filter = "1"
    if not date_min is None:
        date_min_sql = date_min.strftime('%Y-%m-%d %H:%M:%S')
        date_filter = date_filter + " AND date >= '" + date_min_sql + "'"
    if not date_max is None:
        date_max_sql = date_max.strftime('%Y-%m-%d %H:%M:%S')
        date_filter = date_filter + " AND date <= '" + date_max_sql + "'"

    sql_req = ""
    if (scope == "CLUSTER"):
        sql_req = """SELECT date
            ,SUM(produced) AS produced
            ,SUM(requested) AS requested
            ,SUM(consumed) AS consumed
            ,SUM(provided) AS provided
            ,SUM(available) AS available
            ,SUM(missing) AS missing
            FROM simulated_node_history WHERE """ + date_filter + """
            GROUP BY date"""
    else:
        sql_req = "SELECT date, produced, requested, consumed, provided, available, missing"\
                  + " FROM simulated_node_history"\
                  + " WHERE node = '" + node + "' AND " + date_filter + " ORDER BY date"
    connection = connect_to_db_default()
    cursor = connection.cursor()
    cursor.execute(sql_req)
    for date, produced, requested, consumed, provided, available, missing in cursor:
        list_dates.append(date)
        list_requested.append(requested)
        list_produced.append(produced)
        list_consumed.append(consumed)
        list_provided.append(provided)
        list_available.append(available)
        list_missing.append(missing)
    data_node = {"requested":list_requested, "produced":list_produced, "consumed":list_consumed, "missing":list_missing, "provided":list_provided, "available":list_available}
    result = pd.DataFrame(data_node)
    result.index = list_dates
    return result





def test_default_case():
    date_current = datetime.strptime('2023-01-15', '%Y-%m-%d')
    nb_of_days = 4
    node_by_location = {"":"N2" ,"Gymnase":"N2" ,"Sous-sol":"N2" ,"Ecole primaire":"N1"  ,"Parascolaire":"N2"}
    default_node= "N1"
    set_node_in_measure_data(date_current, nb_of_days, node_by_location, default_node)
    list_dates = ['2023-01-12','2023-01-13','2023-01-14','2023-01-15']
    #history_data = load_data(list_dates, "N1")
    history_data = load_allnodes_history_data(date_current, 4,  None)
    data_N1 = extract_node_history_data(history_data, "N1")
    data_N2 = extract_node_history_data(history_data, "N2")

    print("data_N2 = ", data_N2)




