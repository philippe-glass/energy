

TRUNCATE device_measure

;
INSERT INTO device_measure(timestamp3,id_device, power_p)
	SELECT 
		DATE_add(DATE(timestamp), interval (ROUND(TIME_TO_SEC(timestamp)/60)) * 60 SECOND) AS timestamp3
		,id_device
		,SUM(ABS(phase_mr.power_p))  AS power_p
		FROM phase_measure_record as phase_mr
		JOIN measure_record ON phase_mr.id_measure_record = measure_record.id
		JOIN sensor_input ON sensor_input.id_sensor = measure_record.id_sensor AND sensor_input.phase=phase_mr.phase
		WHERE 1 -- feature_type ='MN' -- AND sensor.location = 'Parascolaire'
				-- AND timestamp > '2023-01-12 17:56:45'
				-- AND timestamp < '2023-01-15 18:56:45'
				AND NOT sensor_input.is_disabled
		GROUP BY timestamp3, id_device

;
DROP TEMPORARY TABLE IF EXISTS Tmp_ToComplete

;
CREATE TEMPORARY TABLE Tmp_ToComplete
	SELECT timestamp3, MAX(device.is_producer) AS has_producer 
	, DATE_ADD(timestamp3, interval -(MINUTE(timestamp3) % 15) MINUTE) AS prod_timestamp
	FROM device_measure
	JOIN device ON device.id = device_measure.id_device
	GROUP BY timestamp3
	HAVING has_producer = 0

;
ALTER TABLE Tmp_ToComplete ADD key _timestamp3(timestamp3)

;
INSERT INTO device_measure(timestamp3, id_device, power_p, added)
	SELECT Tmp_ToComplete.timestamp3,  id_device, power_p, 1
	FROM Tmp_ToComplete
	JOIN device_measure ON device_measure.timestamp3 = Tmp_ToComplete.prod_timestamp
	WHERE device_measure.id_device IN (SELECT id FROM device WHERE is_producer)

;
INSERT INTO device_measure(timestamp3, id_device, power_p, added)
SELECT timestamp3
	,(SELECT device.id FROM device where name = 'SIG')  AS id_device
	,(SELECT 120*ds.power FROM device_statistic ds WHERE ds.device_category = 'EXTERNAL_ENG' AND ds.start_hour=hour(device_measure.timestamp3)) AS power_p
	,1 AS added
	FROM device_measure
	GROUP BY timestamp3

;
UPDATE device_measure SET hour = HOUR(timestamp3), date = DATE(timestamp3)

;

select * from device_measure

SEC_TO_TIME((ROUND(TIME_TO_SEC(toStampActual)/60)) * 60)


select NOW(), CURRENT_TIME() , TIME_TO_SEC(CURRENT_TIME()) , 1+ TIME_TO_SEC(CURRENT_TIME())







 
 
 
 
  select count(*), DATE(timestamp3) from device_measure group by DATE(timestamp3)
  
  
  select * from device_measure where is_producer and device_name   = 'SIG' and power_p = 0
  
  
  where power_p is null 
  
 select * from device_measure where timestamp3='2023-01-14 10:01:00.000' order by 1*power_p
 
 0.42600#0.50500#0.79200#1.68000#1.93800#2.10000#2.13400#2.24400#41.59400#51.06900#59.95400#65.27300#82.72100#140.32700#302.54900#440.43600#756.12100#950.40600#1021.14200#1037.89500#1056.23000#3300.00000
 SIG#Ballon ECS triphasé#Ballon ECS triphasé#Ballon ECS triphasé#ventil. Extraction WC Filles (marron)#éclairage BEC 106 Gr.402 1er#éclairage atelier arts visuels BEC R11#éclairage BEC 105 Gr.401 1er#ventil. Ecutoires (gris)#ventil. Extraction WC Garçons (noir)#Entré du tableau TSP.02#Entré du tableau TSP.02#Entré du tableau TSP.02#Entré tableau TE-BEC-1#Entré tableau TE-BEC-1#Entré du tableau TSG.03#Entré tableau TE-BEC-1#Entré du tableau TSC.01#Entré du tableau TSG.03#Entré du tableau TSG.03#Entré du tableau TSC.01#Entré du tableau TSC.01#Production PV brute
 1#0#0#0#0#0#0#0#0#0#0#0#0#0#0#0#0#0#0#0#0#0#1
 
