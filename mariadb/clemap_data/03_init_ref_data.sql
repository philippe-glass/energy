DELIMITER �


DELETE FROM sensor_input
�
DELETE FROM sensor
�
DELETE FROM device
�
DELETE FROM device_statistic
�

INSERT INTO `device_statistic` VALUES
 ('UNKNOWN',0,1,72.00000),('UNKNOWN',1,2,61.00000),('UNKNOWN',2,3,60.00000),('UNKNOWN',3,4,57.00000),('UNKNOWN',4,5,58.00000),('UNKNOWN',5,6,59.00000),('UNKNOWN',6,7,60.00000),('UNKNOWN',7,8,79.00000),('UNKNOWN',8,9,102.00000),('UNKNOWN',9,10,105.00000),('UNKNOWN',10,11,107.00000),('UNKNOWN',11,12,103.00000),('UNKNOWN',12,13,92.00000),('UNKNOWN',13,14,104.00000),('UNKNOWN',14,15,93.00000),('UNKNOWN',15,16,95.00000),('UNKNOWN',16,17,98.00000),('UNKNOWN',17,18,113.00000),('UNKNOWN',18,19,137.00000),('UNKNOWN',19,20,130.00000),('UNKNOWN',20,21,126.00000),('UNKNOWN',21,22,125.00000),('UNKNOWN',22,23,115.00000),('UNKNOWN',23,24,105.00000),('WATER_HEATING',0,1,3.00000),('WATER_HEATING',1,2,3.00000),('WATER_HEATING',2,3,4.00000),('WATER_HEATING',3,4,16.00000),('WATER_HEATING',4,5,6.00000),('WATER_HEATING',5,6,36.00000),('WATER_HEATING',6,7,6.00000),('WATER_HEATING',7,8,13.00000),('WATER_HEATING',8,9,9.00000),('WATER_HEATING',9,10,8.00000),('WATER_HEATING',10,11,8.00000),('WATER_HEATING',11,12,8.00000),('WATER_HEATING',12,13,7.00000),('WATER_HEATING',13,14,8.00000),('WATER_HEATING',14,15,9.00000),('WATER_HEATING',15,16,9.00000),('WATER_HEATING',16,17,9.00000),('WATER_HEATING',17,18,10.00000),('WATER_HEATING',18,19,10.00000),('WATER_HEATING',19,20,10.00000),('WATER_HEATING',20,21,12.00000),('WATER_HEATING',21,22,12.00000),('WATER_HEATING',22,23,8.00000),('WATER_HEATING',23,24,7.00000),('HEATING',0,1,64.00000),('HEATING',1,2,35.00000),('HEATING',2,3,39.00000),('HEATING',3,4,25.00000)
,('HEATING',4,5,23.00000),('HEATING',5,6,21.00000),('HEATING',6,7,21.00000),('HEATING',7,8,22.00000),('HEATING',8,9,25.00000),('HEATING',9,10,24.00000),('HEATING',10,11,25.00000),('HEATING',11,12,24.00000),('HEATING',12,13,19.00000),('HEATING',13,14,21.00000),('HEATING',14,15,22.00000),('HEATING',15,16,22.00000),('HEATING',16,17,25.00000),('HEATING',17,18,38.00000),('HEATING',18,19,25.00000),('HEATING',19,20,27.00000),('HEATING',20,21,36.00000),('HEATING',21,22,28.00000),('HEATING',22,23,21.00000),('HEATING',23,24,39.00000),('COOKING',0,1,1.00000),('COOKING',1,2,1.00000),('COOKING',2,3,1.00000),('COOKING',3,4,2.00000),('COOKING',4,5,2.00000),('COOKING',5,6,2.00000),('COOKING',6,7,8.00000),('COOKING',7,8,38.00000),('COOKING',8,9,64.00000),('COOKING',9,10,59.00000),('COOKING',10,11,47.00000),('COOKING',11,12,48.00000),('COOKING',12,13,66.00000),('COOKING',13,14,79.00000),('COOKING',14,15,46.00000),('COOKING',15,16,43.00000),('COOKING',16,17,66.00000),('COOKING',17,18,121.00000),('COOKING',18,19,123.00000),('COOKING',19,20,118.00000),('COOKING',20,21,80.00000),('COOKING',21,22,51.00000),('COOKING',22,23,37.00000),('COOKING',23,24,25.00000),('SHOWERS',0,1,2.00000),('SHOWERS',1,2,2.00000),('SHOWERS',2,3,2.00000),('SHOWERS',3,4,2.00000)
,('SHOWERS',4,5,2.00000),('SHOWERS',5,6,2.00000),('SHOWERS',6,7,2.00000),('SHOWERS',7,8,22.00000),('SHOWERS',8,9,37.00000),('SHOWERS',9,10,30.00000),('SHOWERS',10,11,22.00000),('SHOWERS',11,12,19.00000),('SHOWERS',12,13,22.00000),('SHOWERS',13,14,14.00000),('SHOWERS',14,15,8.00000),('SHOWERS',15,16,11.00000)
,('SHOWERS',16,17,10.00000),('SHOWERS',17,18,8.00000),('SHOWERS',18,19,17.00000),('SHOWERS',19,20,18.00000),('SHOWERS',20,21,17.00000),('SHOWERS',21,22,10.00000),('SHOWERS',22,23,7.00000),('SHOWERS',23,24,6.00000),('WASHING_DRYING',0,1,40.00000),('WASHING_DRYING',1,2,22.00000),('WASHING_DRYING',2,3,14.00000),('WASHING_DRYING',3,4,14.00000),('WASHING_DRYING',4,5,7.00000),('WASHING_DRYING',5,6,6.00000),('WASHING_DRYING',6,7,6.00000),('WASHING_DRYING',7,8,15.00000),('WASHING_DRYING',8,9,60.00000),('WASHING_DRYING',9,10,78.00000),('WASHING_DRYING',10,11,84.00000),('WASHING_DRYING',11,12,78.00000),('WASHING_DRYING',12,13,81.00000),('WASHING_DRYING',13,14,77.00000),('WASHING_DRYING',14,15,62.00000),('WASHING_DRYING',15,16,68.00000),('WASHING_DRYING',16,17,65.00000),('WASHING_DRYING',17,18,63.00000),('WASHING_DRYING',18,19,60.00000),('WASHING_DRYING',19,20,61.00000),('WASHING_DRYING',20,21,73.00000)
,('WASHING_DRYING',21,22,62.00000),('WASHING_DRYING',22,23,40.00000),('WASHING_DRYING',23,24,41.00000),('LIGHTING',0,1,50.00000),('LIGHTING',1,2,36.00000),('LIGHTING',2,3,22.00000),('LIGHTING',3,4,20.00000),('LIGHTING',4,5,18.00000),('LIGHTING',5,6,17.00000),('LIGHTING',6,7,18.00000),('LIGHTING',7,8,23.00000),('LIGHTING',8,9,49.00000),('LIGHTING',9,10,44.00000),('LIGHTING',10,11,33.00000),('LIGHTING',11,12,28.00000),('LIGHTING',12,13,29.00000),('LIGHTING',13,14,28.00000),('LIGHTING',14,15,25.00000),('LIGHTING',15,16,29.00000),('LIGHTING',16,17,30.00000),('LIGHTING',17,18,62.00000),('LIGHTING',18,19,114.00000),('LIGHTING',19,20,120.00000)
,('LIGHTING',20,21,121.00000),('LIGHTING',21,22,129.00000),('LIGHTING',22,23,140.00000),('LIGHTING',23,24,121.00000),('AUDIOVISUAL',0,1,50.00000),('AUDIOVISUAL',1,2,36.00000),('AUDIOVISUAL',2,3,30.00000),('AUDIOVISUAL',3,4,26.00000),('AUDIOVISUAL',4,5,24.00000),('AUDIOVISUAL',5,6,21.00000),('AUDIOVISUAL',6,7,21.00000),('AUDIOVISUAL',7,8,25.00000),('AUDIOVISUAL',8,9,47.00000),('AUDIOVISUAL',9,10,48.00000),('AUDIOVISUAL',10,11,47.00000),('AUDIOVISUAL',11,12,49.00000),('AUDIOVISUAL',12,13,52.00000),('AUDIOVISUAL',13,14,60.00000),('AUDIOVISUAL',14,15,62.00000),('AUDIOVISUAL',15,16,62.00000),('AUDIOVISUAL',16,17,68.00000),('AUDIOVISUAL',17,18,65.00000),('AUDIOVISUAL',18,19,104.00000),('AUDIOVISUAL',19,20,106.00000),('AUDIOVISUAL',20,21,117.00000),('AUDIOVISUAL',21,22,118.00000),('AUDIOVISUAL',22,23,112.00000),('AUDIOVISUAL',23,24,90.00000),('COLD_APPLIANCES',0,1,70.00000),('COLD_APPLIANCES',1,2,60.00000),('COLD_APPLIANCES',2,3,59.00000),('COLD_APPLIANCES',3,4,60.00000),('COLD_APPLIANCES',4,5,59.00000),('COLD_APPLIANCES',5,6,60.00000),('COLD_APPLIANCES',6,7,60.00000),('COLD_APPLIANCES',7,8,62.00000),('COLD_APPLIANCES',8,9,62.00000),('COLD_APPLIANCES',9,10,61.00000),('COLD_APPLIANCES',10,11,61.00000),('COLD_APPLIANCES',11,12,61.00000),('COLD_APPLIANCES',12,13,62.00000),('COLD_APPLIANCES',13,14,62.00000),('COLD_APPLIANCES',14,15,62.00000),('COLD_APPLIANCES',15,16,63.00000),('COLD_APPLIANCES',16,17,64.00000),('COLD_APPLIANCES',17,18,66.00000),('COLD_APPLIANCES',18,19,66.00000),('COLD_APPLIANCES',19,20,66.00000),('COLD_APPLIANCES',20,21,66.00000)
,('COLD_APPLIANCES',21,22,64.00000),('COLD_APPLIANCES',22,23,64.00000),('COLD_APPLIANCES',23,24,64.00000),('ICT',0,1,15.00000),('ICT',1,2,16.00000),('ICT',2,3,13.00000),('ICT',3,4,14.00000),('ICT',4,5,15.00000),('ICT',5,6,13.00000),('ICT',6,7,15.00000),('ICT',7,8,15.00000),('ICT',8,9,17.00000),('ICT',9,10,19.00000),('ICT',10,11,25.00000),('ICT',11,12,26.00000),('ICT',12,13,27.00000),('ICT',13,14,27.00000),('ICT',14,15,28.00000),('ICT',15,16,31.00000),('ICT',16,17,29.00000),('ICT',17,18,33.00000),('ICT',18,19,35.00000),('ICT',19,20,37.00000),('ICT',20,21,36.00000),('ICT',21,22,37.00000),('ICT',22,23,26.00000),('ICT',23,24,25.00000),('OTHER',0,1,10.00000),('OTHER',1,2,15.00000),('OTHER',2,3,13.00000),('OTHER',3,4,10.00000),('OTHER',4,5,11.00000),('OTHER',5,6,13.00000),('OTHER',6,7,13.00000),('OTHER',7,8,14.00000),('OTHER',8,9,20.00000),('OTHER',9,10,20.00000),('OTHER',10,11,20.00000),('OTHER',11,12,21.00000),('OTHER',12,13,21.00000),('OTHER',13,14,21.00000),('OTHER',14,15,22.00000),('OTHER',15,16,24.00000),('OTHER',16,17,25.00000),('OTHER',17,18,22.00000),('OTHER',18,19,23.00000),('OTHER',19,20,22.00000),('OTHER',20,21,19.00000),('OTHER',21,22,19.00000),('OTHER',22,23,17.00000),('OTHER',23,24,18.00000),('WIND_ENG',0,1,20.47000),('WIND_ENG',1,2,19.66000),('WIND_ENG',2,3,17.89000),('WIND_ENG',3,4,16.80000),('WIND_ENG',4,5,15.15000),('WIND_ENG',5,6,12.48000),('WIND_ENG',6,7,10.99000),('WIND_ENG',7,8,9.86000),('WIND_ENG',8,9,8.34000),('WIND_ENG',9,10,7.76000),('WIND_ENG',10,11,8.07000),('WIND_ENG',11,12,8.42000),('WIND_ENG',12,13,9.85000)
,('WIND_ENG',13,14,12.88000),('WIND_ENG',14,15,16.06000),('WIND_ENG',15,16,17.14000),('WIND_ENG',16,17,17.97000),('WIND_ENG',17,18,18.89000),('WIND_ENG',18,19,20.03000),('WIND_ENG',19,20,21.49000),('WIND_ENG',20,21,23.51000),('WIND_ENG',21,22,25.43000),('WIND_ENG',22,23,26.59000),('WIND_ENG',23,24,25.67000),('SOLOR_ENG',0,1,0.82000),('SOLOR_ENG',1,2,0.82000),('SOLOR_ENG',2,3,0.84000),('SOLOR_ENG',3,4,1.58000),('SOLOR_ENG',4,5,4.22000),('SOLOR_ENG',5,6,11.07000),('SOLOR_ENG',6,7,23.16000),('SOLOR_ENG',7,8,38.09000),('SOLOR_ENG',8,9,50.85000),('SOLOR_ENG',9,10,58.76000)
,('SOLOR_ENG',10,11,61.15000),('SOLOR_ENG',11,12,61.14000),('SOLOR_ENG',12,13,58.45000),('SOLOR_ENG',13,14,52.22000),('SOLOR_ENG',14,15,43.31000),('SOLOR_ENG',15,16,33.20000),('SOLOR_ENG',16,17,23.65000),('SOLOR_ENG',17,18,14.59000),('SOLOR_ENG',18,19,6.41000),('SOLOR_ENG',19,20,1.76000),('SOLOR_ENG',20,21,0.83000),('SOLOR_ENG',21,22,0.79000),('SOLOR_ENG',22,23,0.82000),('SOLOR_ENG',23,24,0.82000),('EXTERNAL_ENG',0,1,78.03000),('EXTERNAL_ENG',1,2,77.91000),('EXTERNAL_ENG',2,3,78.00000),('EXTERNAL_ENG',3,4,78.00000),('EXTERNAL_ENG',4,5,78.00000),('EXTERNAL_ENG',5,6,78.00000),('EXTERNAL_ENG',6,7,78.00000),('EXTERNAL_ENG',7,8,78.00000),('EXTERNAL_ENG',8,9,78.00000),('EXTERNAL_ENG',9,10,78.00000),('EXTERNAL_ENG',10,11,78.00000),('EXTERNAL_ENG',11,12,78.00000),('EXTERNAL_ENG',12,13,78.00000),('EXTERNAL_ENG',13,14,78.00000),('EXTERNAL_ENG',14,15,78.00000),('EXTERNAL_ENG',15,16,78.00000),('EXTERNAL_ENG',16,17,78.00000),('EXTERNAL_ENG',17,18,78.00000),('EXTERNAL_ENG',18,19,78.00000),('EXTERNAL_ENG',19,20,78.00000),('EXTERNAL_ENG',20,21,78.00000),('EXTERNAL_ENG',21,22,78.00000),('EXTERNAL_ENG',22,23,78.00000),('EXTERNAL_ENG',23,24,78.00000),('BIOMASS_ENG',0,1,9.69000),('BIOMASS_ENG',1,2,9.63000),('BIOMASS_ENG',2,3,9.61000),('BIOMASS_ENG',3,4,9.60000),('BIOMASS_ENG',4,5,9.60000),('BIOMASS_ENG',5,6,9.68000),('BIOMASS_ENG',6,7,9.68000),('BIOMASS_ENG',7,8,9.65000),('BIOMASS_ENG',8,9,9.71000),('BIOMASS_ENG',9,10,9.71000)
,('BIOMASS_ENG',10,11,9.69000),('BIOMASS_ENG',11,12,9.63000),('BIOMASS_ENG',12,13,9.63000),('BIOMASS_ENG',13,14,9.66000),('BIOMASS_ENG',14,15,9.68000),('BIOMASS_ENG',15,16,9.65000),('BIOMASS_ENG',16,17,9.59000),('BIOMASS_ENG',17,18,9.58000),('BIOMASS_ENG',18,19,9.73000),('BIOMASS_ENG',19,20,9.61000),('BIOMASS_ENG',20,21,9.59000),('BIOMASS_ENG',21,22,9.58000),('BIOMASS_ENG',22,23,9.68000),('BIOMASS_ENG',23,24,9.66000),('HYDRO_ENG',0,1,55.00000),('HYDRO_ENG',1,2,55.87000),('HYDRO_ENG',2,3,57.57000),('HYDRO_ENG',3,4,58.03000),('HYDRO_ENG',4,5,63.01000),('HYDRO_ENG',5,6,76.51000),('HYDRO_ENG',6,7,83.77000),('HYDRO_ENG',7,8,80.10000),('HYDRO_ENG',8,9,77.76000),('HYDRO_ENG',9,10,75.64000),('HYDRO_ENG',10,11,73.32000),('HYDRO_ENG',11,12,71.11000),('HYDRO_ENG',12,13,70.90000),('HYDRO_ENG',13,14,69.49000),('HYDRO_ENG',14,15,71.94000),('HYDRO_ENG',15,16,74.56000),('HYDRO_ENG',16,17,78.62000),('HYDRO_ENG',17,18,81.00000),('HYDRO_ENG',18,19,79.39000),('HYDRO_ENG',19,20,74.18000)
,('HYDRO_ENG',20,21,67.84000),('HYDRO_ENG',21,22,59.90000),('HYDRO_ENG',22,23,52.70000),('HYDRO_ENG',23,24,50.98000)

�
/*!40000 ALTER TABLE `device_statistic` ENABLE KEYS */
�
UNLOCK TABLES
�



INSERT INTO device (id,name,category,environmental_impact,power_min,power_max,avg_duration,is_producer,priority_level) VALUES
	 (1,'Coffee Maker','COOKING',0,600.00000,1200.00000,5.00000,0,1),
	 (2,'Keurig','COOKING',0,200.00000,400.00000,6.00000,0,1),
	 (3,'Blender','COOKING',0,300.00000,1000.00000,20.00000,0,1),
	 (4,'Microwave','COOKING',0,600.00000,1000.00000,15.00000,0,1),
	 (5,'Waffle Iron','COOKING',0,800.00000,1500.00000,12.00000,0,1),
	 (6,'Hot Plate','COOKING',0,750.00000,1500.00000,15.00000,0,1),
	 (7,'Electric Skillet','COOKING',0,1000.00000,1500.00000,13.00000,0,1),
	 (8,'Toaster Oven','COOKING',0,700.00000,1200.00000,2.00000,0,1),
	 (9,'Toaster','COOKING',0,800.00000,1500.00000,1.50000,0,1),
	 (10,'Furnace Fan','COOKING',0,750.00000,1200.00000,15.00000,0,1),
	 (11,'Vacuum Cleaner','OTHER',0,300.00000,1500.00000,25.00000,0,1),
	 (12,'toy1','OTHER',0,100.00000,200.00000,25.00000,0,1),
	 (13,'toy2','OTHER',0,150.00000,300.00000,25.00000,0,1),
	 (14,'toy3','OTHER',0,200.00000,450.00000,25.00000,0,1),
	 (15,'toy4','OTHER',0,600.00000,1200.00000,25.00000,0,1),
	 (16,'Household Fan','OTHER',0,50.00000,120.00000,125.00000,0,1),
	 (17,'Space Heater 1','HEATING',0,750.00000,1500.00000,230.00000,0,1),
	 (18,'Lasko Personal Space Heater1','HEATING',0,100.00000,200.00000,120.00000,0,1),
	 (19,'Lasko Personal Space Heater2','HEATING',0,100.00000,200.00000,120.00000,0,1),
	 (20,'Lasko Personal Space Heater3','HEATING',0,100.00000,200.00000,120.00000,0,1),
	 (21,'Basics Ceramic  Heater','HEATING',0,250.00000,500.00000,120.00000,0,1),
	 (22,'Honeywell Heat Bud  Heater','HEATING',0,125.00000,250.00000,120.00000,0,1),
	 (23,'Iseebiz Ceramic Space Heater','HEATING',0,375.00000,750.00000,120.00000,0,1),
	 (24,'Kloudic Ceramic Space Heater','HEATING',0,450.00000,600.00000,120.00000,0,1),
	 (25,'Clothes Iron','WASHING_DRYING',0,1000.00000,1500.00000,65.00000,0,1),
	 (26,'Washing Machine','WASHING_DRYING',0,500.00000,1000.00000,75.00000,0,1),
	 (27,'Refrigerator 1','COLD_APPLIANCES',0,375.00000,750.00000,0.00000,0,1),
	 (28,'Refrigerator 2','COLD_APPLIANCES',0,450.00000,900.00000,0.00000,0,1),
	 (29,'Chest Freezer 1','COLD_APPLIANCES',0,350.00000,700.00000,0.00000,0,1),
	 (30,'Chest Freezer 2','COLD_APPLIANCES',0,250.00000,500.00000,0.00000,0,1),
	 (31,'Clock Radio','AUDIOVISUAL',0,10.00000,50.00000,25.00000,0,1),
	 (32,'Stereo','AUDIOVISUAL',0,30.00000,100.00000,45.00000,0,1),
	 (33,'Cell Phone Charger 1','ICT',0,5.00000,10.00000,180.00000,0,1),
	 (34,'Cell Phone Charger 2','ICT',0,5.00000,10.00000,180.00000,0,1),
	 (35,'Cell Phone Charger 3','ICT',0,5.00000,10.00000,180.00000,0,1),
	 (36,'Cell Phone Charger 4','ICT',0,5.00000,10.00000,180.00000,0,1),
	 (37,'Laptop Computer 1','ICT',0,20.00000,75.00000,240.00000,0,1),
	 (38,'Laptop Computer 2','ICT',0,20.00000,75.00000,240.00000,0,1),
	 (39,'Laptop Computer 3','ICT',0,20.00000,75.00000,240.00000,0,1),
	 (40,'MacBook Pro 1','ICT',0,30.00000,85.00000,240.00000,0,1),
	 (41,'iPad / Tablet 1','ICT',0,10.00000,20.00000,54.00000,0,1),
	 (42,'Desktop with Monitor','ICT',0,200.00000,400.00000,54.00000,0,1),
	 (43,'Inkjet Printer','ICT',0,15.00000,75.00000,720.00000,0,1),
	 (44,'Laser Printer','ICT',0,500.00000,500.00000,720.00000,0,1),
	 (45,'Satellite Dish / Receiver','ICT',0,20.00000,30.00000,0.00000,0,2),
	 (46,'Photographic Strobe','AUDIOVISUAL',0,200.00000,300.00000,45.00000,0,1),
	 (47,'TV 32" LED/LCD','AUDIOVISUAL',0,30.00000,50.00000,90.00000,0,1),
	 (48,'TV 42" Plasma','AUDIOVISUAL',0,140.00000,240.00000,90.00000,0,1),
	 (49,'Home Theater Projector','AUDIOVISUAL',0,100.00000,200.00000,90.00000,0,1),
	 (50,'Blu-Ray or DVD Player','AUDIOVISUAL',0,8.00000,15.00000,90.00000,0,1),
	 (51,'Video Game Console','AUDIOVISUAL',0,40.00000,140.00000,75.00000,0,1),
	 (52,'Video Game Console2','AUDIOVISUAL',0,45.00000,145.00000,75.00000,0,1),
	 (53,'Xbox one Console1','AUDIOVISUAL',0,20.00000,104.00000,75.00000,0,1),
	 (54,'Xbox one Console2','AUDIOVISUAL',0,20.00000,104.00000,75.00000,0,1),
	 (55,'Xbox one Console3','AUDIOVISUAL',0,20.00000,104.00000,75.00000,0,1),
	 (56,'CFL/LED-W8  1','LIGHTING',0,5.00000,8.00000,60.00000,0,1),
	 (57,'CFL/LED-W8 2','LIGHTING',0,5.00000,8.00000,60.00000,0,1),
	 (58,'CFL/LED-W8 3','LIGHTING',0,5.00000,8.00000,60.00000,0,1),
	 (59,'CFL/LED-W8 4','LIGHTING',0,5.00000,8.00000,60.00000,0,1),
	 (60,'CFL/LED-W8 5','LIGHTING',0,5.00000,8.00000,60.00000,0,1),
	 (61,'CFL/LED-W8 6','LIGHTING',0,5.00000,8.00000,60.00000,0,1),
	 (62,'CFL/LED-W11 1','LIGHTING',0,7.00000,11.00000,60.00000,0,1),
	 (63,'CFL/LED-W11 2','LIGHTING',0,7.00000,11.00000,60.00000,0,1),
	 (64,'CFL/LED-W11 3','LIGHTING',0,7.00000,11.00000,60.00000,0,1),
	 (65,'CFL/LED-W11 4','LIGHTING',0,7.00000,11.00000,60.00000,0,1),
	 (66,'CFL/LED-W11 5','LIGHTING',0,7.00000,11.00000,60.00000,0,1),
	 (67,'Incandescent light bulb W40 1','LIGHTING',0,20.00000,40.00000,45.00000,0,1),
	 (68,'Incandescent light bulb W40 2','LIGHTING',0,20.00000,40.00000,45.00000,0,1),
	 (69,'Incandescent light bulb W40 3','LIGHTING',0,20.00000,40.00000,45.00000,0,1),
	 (70,'Incandescent light bulb W40 4','LIGHTING',0,20.00000,40.00000,45.00000,0,1),
	 (71,'Incandescent light bulb W80 1','LIGHTING',0,40.00000,80.00000,45.00000,0,1),
	 (72,'Incandescent light bulb W80 2','LIGHTING',0,40.00000,80.00000,45.00000,0,1),
	 (73,'Incandescent light bulb W80 3','LIGHTING',0,40.00000,80.00000,45.00000,0,1),
	 (74,'Incandescent light bulb W80 4','LIGHTING',0,40.00000,80.00000,45.00000,0,1),
	 (75,'Incandescent light bulb W100 1','LIGHTING',0,50.00000,100.00000,45.00000,0,1),
	 (76,'Incandescent light bulb W100 2','LIGHTING',0,50.00000,100.00000,45.00000,0,1),
	 (77,'Incandescent light bulb W100 3','LIGHTING',0,50.00000,100.00000,45.00000,0,1),
	 (78,'Incandescent light bulb W100 4','LIGHTING',0,50.00000,100.00000,45.00000,0,1),
	 (79,'Incandescent light bulb W100 5','LIGHTING',0,50.00000,100.00000,45.00000,0,1),
	 (80,'Incandescent light bulb W100 6','LIGHTING',0,50.00000,100.00000,45.00000,0,1),
	 (81,'Incandescent light bulb W100 7','LIGHTING',0,50.00000,100.00000,45.00000,0,1),
	 (82,'Incandescent light bulb W100 8','LIGHTING',0,50.00000,100.00000,45.00000,0,1),
	 (83,'Incandescent light bulb W60 1','LIGHTING',0,30.00000,60.00000,45.00000,0,1),
	 (84,'Incandescent light bulb W60 2','LIGHTING',0,30.00000,60.00000,45.00000,0,1),
	 (85,'Incandescent light bulb W60 3','LIGHTING',0,30.00000,60.00000,45.00000,0,1),
	 (86,'Halogen light bulbs 7 pc 1','LIGHTING',0,10.00000,50.00000,45.00000,0,1),
	 (87,'Halogen light bulbs 7 pc 2','LIGHTING',0,10.00000,50.00000,45.00000,0,1),
	 (88,'Halogen light bulbs 7 pc 3','LIGHTING',0,10.00000,50.00000,45.00000,0,1),
	 (89,'Halogen light bulbs 7 pc 4','LIGHTING',0,10.00000,50.00000,45.00000,0,1),
	 (90,'Energy-saving light bulb W11 1','LIGHTING',0,6.00000,11.00000,45.00000,0,1),
	 (91,'Energy-saving light bulb W11 2','LIGHTING',0,6.00000,11.00000,45.00000,0,1),
	 (92,'Energy-saving light bulb W11 3','LIGHTING',0,6.00000,11.00000,45.00000,0,1),
	 (93,'Energy-saving light bulb W11 4','LIGHTING',0,6.00000,11.00000,45.00000,0,1),
	 (94,'Energy-saving light bulb W18 1','LIGHTING',0,11.00000,18.00000,45.00000,0,1),
	 (95,'Energy-saving light bulb W18 2','LIGHTING',0,11.00000,18.00000,45.00000,0,1),
	 (96,'Energy-saving light bulb W18 3','LIGHTING',0,11.00000,18.00000,45.00000,0,1),
	 (97,'Energy-saving light bulb W18 4','LIGHTING',0,11.00000,18.00000,45.00000,0,1),
	 (98,'Energy-saving light bulb W18 5','LIGHTING',0,11.00000,18.00000,45.00000,0,1),
	 (99,'Theem Water Heaters with leak guard 1','WATER_HEATING',0,100.00000,4500.00000,120.00000,0,1),
	 (100,'Theem Water Heaters with leak guard 2','WATER_HEATING',0,66.00000,3000.00000,120.00000,0,1)
�
INSERT INTO device (id,name,category,environmental_impact,power_min,power_max,avg_duration,is_producer,priority_level) VALUES
	 (101,'800ML Dental Jet Water Flosser','SHOWERS',0,9.00000,18.00000,5.00000,0,1),
	 (102,'800ML Dental Jet Water Flosser2','SHOWERS',0,2.50000,5.00000,5.00000,0,1),
	 (103,'800ML Dental Jet Water Flosser3','SHOWERS',0,2.50000,5.00000,5.00000,0,1),
	 (104,'800ML Dental Jet Water Flosser4','SHOWERS',0,2.50000,5.00000,5.00000,0,1),
	 (105,'800ML Dental Jet Water Flosser5','SHOWERS',0,2.50000,5.00000,5.00000,0,1),
	 (106,'Bathroom Fan 1','SHOWERS',0,40.00000,80.00000,15.00000,0,1),
	 (107,'Bathroom Fan 2','SHOWERS',0,40.00000,80.00000,15.00000,0,1),
	 (108,'Bathroom Fan 3','SHOWERS',0,30.00000,60.00000,15.00000,0,1),
	 (109,'Hair Dryer 1','SHOWERS',0,300.00000,600.00000,15.00000,0,1),
	 (110,'Hair Dryer 2','SHOWERS',0,200.00000,400.00000,15.00000,0,1),
	 (111,'Boiler 100 l','SHOWERS',0,1100.00000,2200.00000,45.00000,0,1),
	 (112,'Not identified 10-1','UNKNOWN',0,10.00000,50.00000,60.00000,0,1),
	 (113,'Not identified 10-2','UNKNOWN',0,10.00000,50.00000,60.00000,0,1),
	 (114,'Not identified 10-3','UNKNOWN',0,10.00000,50.00000,60.00000,0,1),
	 (115,'Not identified 10-4','UNKNOWN',0,10.00000,50.00000,60.00000,0,1),
	 (116,'Not identified 10-5','UNKNOWN',0,10.00000,50.00000,60.00000,0,1),
	 (117,'Not identified 50-1','UNKNOWN',0,50.00000,100.00000,60.00000,0,1),
	 (118,'Not identified 50-2','UNKNOWN',0,50.00000,100.00000,60.00000,0,1),
	 (119,'Not identified 50-3','UNKNOWN',0,50.00000,100.00000,60.00000,0,1),
	 (120,'Not identified 50-4','UNKNOWN',0,50.00000,100.00000,60.00000,0,1),
	 (121,'Not identified 50-5','UNKNOWN',0,50.00000,100.00000,60.00000,0,1),
	 (122,'Not identified 100-1','UNKNOWN',0,100.00000,500.00000,60.00000,0,1),
	 (123,'Not identified 100-2','UNKNOWN',0,100.00000,500.00000,60.00000,0,1),
	 (124,'Not identified 100-3','UNKNOWN',0,100.00000,500.00000,60.00000,0,1),
	 (125,'Not identified 100-4','UNKNOWN',0,100.00000,500.00000,60.00000,0,1),
	 (126,'Not identified 100-5','UNKNOWN',0,100.00000,500.00000,60.00000,0,1),
	 (127,'Not identified 500-1','UNKNOWN',0,500.00000,1000.00000,60.00000,0,1),
	 (128,'Not identified 500-2','UNKNOWN',0,500.00000,1000.00000,60.00000,0,1),
	 (129,'Not identified 500-3','UNKNOWN',0,500.00000,1000.00000,60.00000,0,1),
	 (130,'Not identified 500-4','UNKNOWN',0,500.00000,1000.00000,60.00000,0,1),
	 (131,'Not identified 500-5','UNKNOWN',0,500.00000,1000.00000,60.00000,0,1),
	 (144,'EDF','EXTERNAL_ENG',0,0.00000,1500.00000,0.00000,1,1),
	 (145,'Wind turbine 1','WIND_ENG',0,0.00000,1000.00000,60.00000,1,1),
	 (146,'Wind turbine 2','WIND_ENG',0,0.00000,1000.00000,60.00000,1,1),
	 (147,'Wind turbine 3','WIND_ENG',0,0.00000,1000.00000,60.00000,1,1),
	 (148,'Solar panel 1','SOLOR_ENG',0,0.00000,1000.00000,60.00000,1,1),
	 (149,'biomass prod-01','BIOMASS_ENG',0,0.00000,1000.00000,60.00000,1,1),
	 (150,'hydro prod_01','HYDRO_ENG',0,0.00000,1000.00000,60.00000,1,1)
�

INSERT INTO device (id,name,category,serial_number,panel_input,location,living_lab,node,environmental_impact,power_min,power_max,avg_duration,is_producer,priority_level) VALUES
	 (188,'Armoire �lectrique de hepia','ELECTRICAL_PANEL','SE05000160','','Hepia','Vergers','',3,0.00000,0.00000,0.00000,0,1),
	 (189,'Ballon ECS triphas�','WATER_HEATING','SE05000281','','Sous-sol','Vergers','N2',3,0.00000,0.00000,0.00000,0,1)
�
INSERT INTO device (id,name,category,serial_number,panel_input,location,living_lab,node,environmental_impact,power_min,power_max,avg_duration,is_producer,priority_level) VALUES
	 (190,'Consommation SIG','EXTERNAL_ENG','CH1022501234500000000000000324224', '','','Vergers','N2',3,0.00000,0.00000,0.00000,1,1),
	 (191,'�clairage atelier arts visuels BEC R11','LIGHTING','SE05000163','17Q7','Ecole primaire','Vergers','N1',3,0.00000,0.00000,0.00000,0,1),
	 (192,'�clairage BEC 105 Gr.401 1er','LIGHTING','SE05000163','19Q1','Ecole primaire','Vergers','N1',3,0.00000,0.00000,0.00000,0,1),
	 (193,'�clairage BEC 106 Gr.402 1er','LIGHTING','SE05000163','19Q4','Ecole primaire','Vergers','N1',3,0.00000,0.00000,0.00000,0,1),
	 (194,'Entr� du tableau TSC.01','ELECTRICAL_PANEL','SE05000238','','Parascolaire','Vergers','N2',3,0.00000,0.00000,0.00000,0,1),
	 (195,'Entr� du tableau TSG.03','ELECTRICAL_PANEL','SE05000283','','Gymnase','Vergers','N2',3,0.00000,0.00000,0.00000,0,1),
	 (196,'Entr� du tableau TSP.02','ELECTRICAL_PANEL','SE05000159','','Ecole primaire','Vergers','N1',3,0.00000,0.00000,0.00000,0,1),
	 (197,'Entr� tableau TE-BEC-1','ELECTRICAL_PANEL','SE05000318','','Sous-sol','Vergers','N2',3,0.00000,0.00000,0.00000,0,1),
	 (198,'Production PV achet�e par SIG','SOLOR_ENG','CH1022501234500000000000000326325','','','Vergers','N2',2,0.00000,0.00000,0.00000,1,1),
	 (199,'Production PV brute','SOLOR_ENG','CH1022501234500000000000000326365','','Parascolaire','Vergers','N2',2,0.00000,0.00000,0.00000,1,1)
�
INSERT INTO device (id,name,category,serial_number,panel_input,location,living_lab,node,environmental_impact,power_min,power_max,avg_duration,is_producer,priority_level) VALUES
	 (200,'ventil. Ecutoires (gris)','COLD_APPLIANCES','SE05000282','250F0','Sous-sol','Vergers','N2',3,0.00000,0.00000,0.00000,0,1),
	 (201,'ventil. Extraction WC Filles (marron)','COLD_APPLIANCES','SE05000282','220F1','Sous-sol','Vergers','N2',3,0.00000,0.00000,0.00000,0,1),
	 (202,'ventil. Extraction WC Gar�ons (noir)','COLD_APPLIANCES','SE05000282','210F1','Sous-sol','Vergers','N2',3,0.00000,0.00000,0.00000,0,1),
	 (203,'SIG','EXTERNAL_ENG','','','','Vergers','N2',3,0.00000,0.00000,0.00000,1,1)


�
UPDATE device SET is_consumer = 1 WHERE NOT is_producer
�
UPDATE device SET environmental_impact = 3
�
UPDATE device SET environmental_impact = 2 WHERE category IN ('WIND_ENG', 'SOLOR_ENG', 'BIOMASS_ENG', 'HYDRO_ENG')



�


INSERT INTO sensor (id,serial_number,name,firmeware_version,location,comment,electrical_panel) VALUES
	 (1,'SE05000163','SE05000163','','Ecole primaire','','TSP.02'),
	 (2,'SE05000159','SE05000159','fw2.0.0p-3-g06078c8-master','Ecole primaire','','TSP.02'),
	 (3,'SE05000238','SE05000238 125A','fw2.1.2p-1-g6c200c7-HEAD','Parascolaire','','TSC.01'),
	 (4,'SE05000283','SE05000283 42A','fw2.1.2p-1-g6c200c7-HEAD','Gymnase','','TSG.03'),
	 (5,'SE05000281','SE05000281 42A','fw2.1.2p-master','Sous-sol','',''),
	 (6,'SE05000282','SE05000282 42A','fw2.1.2p-master','Sous-sol','',''),
	 (7,'SE05000318','SE05000318 80A','fw2.1.2p-master','Sous-sol','','TE-BEC-1'),
	 (8,'SE05000319','SE05000319 80A','fw2.1.2p-1-g6c200c7-HEAD','','',''),
	 (9,'CH1022501234500000000000000324224','CH1022501234500000000000000324224','','','',''),
	 (10,'CH1022501234500000000000000326325','CH1022501234500000000000000326325','','','',''),
	 (11,'CH1022501234500000000000000326365','CH1022501234500000000000000326365','','Parascolaire','',''),
	 (12,'SE05000160','SE05000160','','Hepia','','???'),
	 (13,'SE05000540','SE05000540','','?','',''),
	 (14,'SE05000550','SE05000550','','?','',''),
	 (15,'SE05000555','SE05000555','','?','',''),
	 (16,'SE05000759','SE05000759','','?','','')

�
INSERT INTO sensor_input (id,id_sensor,phase,id_device,is_disabled) VALUES
	 (1,1,'l1',193,0),
	 (2,1,'l2',191,0),
	 (3,1,'l3',192,0),
	 (4,5,'l1',189,0),
	 (5,5,'l2',189,0),
	 (6,5,'l3',189,0),
	 (7,6,'l1',201,0),
	 (8,6,'l2',202,0),
	 (9,6,'l3',200,0),
	 (10,4,'l1',195,0),
	 (11,4,'l2',195,0),
	 (12,4,'l3',195,0),
	 (13,3,'l1',194,0),
	 (14,3,'l2',194,0),
	 (15,3,'l3',194,0),
	 (16,2,'l1',196,0),
	 (17,2,'l2',196,0),
	 (18,2,'l3',196,0),
	 (19,7,'l1',197,0),
	 (20,7,'l2',197,0)
�
INSERT INTO sensor_input (id,id_sensor,phase,id_device,is_disabled) VALUES
	 (21,7,'l3',197,0),
	 (22,12,'l1',188,1),
	 (23,12,'l2',188,1),
	 (24,12,'l3',188,1),
	 (25,9,'l1',190,1),
	 (26,10,'l1',198,1),
	 (27,11,'l1',199,0),
	 (28,13,'l1',NULL,1),
	 (29,13,'l2',NULL,1),
	 (30,13,'l3',NULL,1),
	 (31,14,'l1',NULL,1),
	 (32,14,'l2',NULL,1),
	 (33,14,'l3',NULL,1),
	 (34,15,'l1',NULL,1),
	 (35,15,'l2',NULL,1),
	 (36,15,'l3',NULL,1),
	 (37,16,'l1',NULL,1),
	 (38,16,'l2',NULL,1),
	 (39,16,'l3',NULL,1)
