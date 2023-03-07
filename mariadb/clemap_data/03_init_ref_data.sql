DELIMITER §

DELETE FROM sensor_input
§
DELETE FROM sensor
§
INSERT INTO sensor(name,firmeware_version,serial_number,location,electrical_panel) VALUES
	 ('SE05000163'		,''								,'SE05000163'	,'Ecole primaire'	,'TSP.02')
	,('SE05000159'		,'fw2.0.0p-3-g06078c8-master'	,'SE05000159' 	,'Ecole primaire'	,'TSP.02')
	,('SE05000238 125A'	,'fw2.1.2p-1-g6c200c7-HEAD'		,'SE05000238'	,'Parascolaire'		,'TSC.01')
	,('SE05000283 42A'	,'fw2.1.2p-1-g6c200c7-HEAD'		,'SE05000283'	,'Gymnase'			,'TSG.03')
	,('SE05000281 42A'	,'fw2.1.2p-master'				,'SE05000281'	,'Sous-sol' 		,'')
	,('SE05000282 42A'	,'fw2.1.2p-master'				,'SE05000282'	,'Sous-sol' 		,'')
	,('SE05000318 80A'	,'fw2.1.2p-master'				,'SE05000318'	,'Sous-sol' 		,'TE-BEC-1')
	,('SE05000319 80A'	,'fw2.1.2p-1-g6c200c7-HEAD'		,'SE05000319'	,''					,'')
	,('CH1022501234500000000000000324224', '', 'CH1022501234500000000000000324224', '', '') -- SIG
	,('CH1022501234500000000000000326325', '', 'CH1022501234500000000000000326325', '', '') -- vente SIG
	,('CH1022501234500000000000000326365', '', 'CH1022501234500000000000000326365', 'Parascolaire', '')
§
SET @id_SE05000163 = (SELECT id FROM sensor WHERE serial_number='SE05000163')
§
INSERT INTO sensor_input(id_sensor,phase,panel_input,device_category, description) VALUES
		 (@id_SE05000163, 'l1', '19Q4', 'LIGHTING', 'éclairage BEC 106 Gr.402 1er')
		,(@id_SE05000163, 'l2', '17Q7', 'LIGHTING', 'éclairage atelier arts visuels BEC R11')
		,(@id_SE05000163, 'l3', '19Q1', 'LIGHTING', 'éclairage BEC 105 Gr.401 1er')
§
SET @id_SE05000281 = (SELECT id FROM sensor WHERE serial_number='SE05000281')
§
INSERT INTO sensor_input(id_sensor,phase,panel_input,device_category, description) VALUES
	 (@id_SE05000281, 'l1'	,'',	'WATER_HEATING', 'Ballon ECS triphasé')
	,(@id_SE05000281, 'l2'	,'',	'WATER_HEATING', 'Ballon ECS triphasé')
	,(@id_SE05000281, 'l3'	,'',	'WATER_HEATING', 'Ballon ECS triphasé')
§
SET @id_SE05000282 = 	(SELECT id FROM sensor WHERE serial_number='SE05000282')
§
INSERT INTO sensor_input(id_sensor,phase,panel_input,device_category, description) VALUES
	 (@id_SE05000282, 'l1'	,'220F1',	'COLD_APPLIANCES', 'ventil. Extraction WC Filles (marron)')
	,(@id_SE05000282, 'l2'	,'210F1',	'COLD_APPLIANCES', 'ventil. Extraction WC Garçons (noir)')
	,(@id_SE05000282, 'l3'	,'250F0',	'COLD_APPLIANCES', 'ventil. Ecutoires (gris)')
§
SET @id_SE05000283 = 	(SELECT id FROM sensor WHERE serial_number='SE05000283')
§
INSERT INTO sensor_input(id_sensor,phase,panel_input,device_category, description) VALUES
	 (@id_SE05000283, 'l1'	,'',	 'ELECTRICAL_PANEL', 'Entré du tableau TSG.03')
	,(@id_SE05000283, 'l2'	,'',	 'ELECTRICAL_PANEL', 'Entré du tableau TSG.03')
	,(@id_SE05000283, 'l3'	,'',	 'ELECTRICAL_PANEL', 'Entré du tableau TSG.03')
§
SET @id_SE05000238 = (SELECT id from sensor where serial_number='SE05000238')
§
INSERT INTO sensor_input(id_sensor,phase,panel_input,device_category, description) VALUES
	 (@id_SE05000238, 'l1'	,'',	 'ELECTRICAL_PANEL', 'Entré du tableau TSC.01')
	,(@id_SE05000238, 'l2'	,'',	 'ELECTRICAL_PANEL', 'Entré du tableau TSC.01')
	,(@id_SE05000238, 'l3'	,'',	 'ELECTRICAL_PANEL', 'Entré du tableau TSC.01')
§
SET @id_SE05000159 = (SELECT id from sensor where serial_number='SE05000159')
§
INSERT INTO sensor_input(id_sensor,phase,panel_input,device_category, description) VALUES
	 (@id_SE05000159, 'l1'	,'',	 'ELECTRICAL_PANEL', 'Entré du tableau TSP.02')
	,(@id_SE05000159, 'l2'	,'',	 'ELECTRICAL_PANEL', 'Entré du tableau TSP.02')
	,(@id_SE05000159, 'l3'	,'',	 'ELECTRICAL_PANEL', 'Entré du tableau TSP.02')
§
SET @id_SE05000318 = (SELECT id from sensor where serial_number='SE05000318')
§
INSERT INTO sensor_input(id_sensor,phase,panel_input,device_category, description) VALUES
	 (@id_SE05000318, 'l1'	,'',	 'ELECTRICAL_PANEL', 'Entré tableau TE-BEC-1')
	,(@id_SE05000318, 'l2'	,'',	 'ELECTRICAL_PANEL', 'Entré tableau TE-BEC-1')
	,(@id_SE05000318, 'l3'	,'',	 'ELECTRICAL_PANEL', 'Entré tableau TE-BEC-1')
§
SET @id_SE05000160 = (SELECT id from sensor where serial_number='SE05000160')
§
INSERT INTO sensor(name,firmeware_version,serial_number,location,electrical_panel) VALUES
	 ('SE05000160'		,''								,'SE05000160'	,'Hepia'	,'???')
§
INSERT INTO sensor_input(id_sensor,phase,panel_input,device_category, description,is_disabled) VALUES
	 (@id_SE05000160, 'l1'	,'',	 'ELECTRICAL_PANEL', 'Armoire électrique de hepia', 1)
	,(@id_SE05000160, 'l2'	,'',	 'ELECTRICAL_PANEL', 'Armoire électrique de hepia', 1)
	,(@id_SE05000160, 'l3'	,'',	 'ELECTRICAL_PANEL', 'Armoire électrique de hepia', 1)
§

SET @id_324224 = (SELECT id FROM sensor WHERE serial_number='CH1022501234500000000000000324224')
§
SET @id_326325 = (SELECT id FROM sensor WHERE serial_number='CH1022501234500000000000000326325')
§
SET @id_326365 = (SELECT id FROM sensor WHERE serial_number='CH1022501234500000000000000326365')
§
INSERT INTO sensor_input(id_sensor,phase,panel_input,device_category, description,is_disabled) VALUES
	 (@id_324224, 'l1'	,'',	'EXTERNAL_ENG', 'Consommation SIG',1)
	,(@id_326325, 'l1'	,'',	'SOLOR_ENG', 'Production PV achetée par SIG',1)
	,(@id_326365, 'l1'	,'',	'SOLOR_ENG', 'Production PV brute',0)
§
