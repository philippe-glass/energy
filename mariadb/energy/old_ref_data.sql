DELIMITER �

-- Markov Time Windows

INSERT INTO time_window (start_hour,end_hour) VALUES
	(0,6),(6,7),(7,8),(8,9),(9,10),(10,11),(11,12),(12,13),(13,14),(14,15),(15,16),(16,17),(17,18),(18,19),(19,20),(20,21),(21,22),(22,23),(23,24)
�
UPDATE time_window SET days_of_week = '1,2,3,4,5,6,7'
�





DELIMITER �

TRUNCATE device
�
INSERT INTO device(name,power_min,power_max,avg_duration,category) VALUES
 ('Coffee Maker'					,600	,1200	,5		,'COOKING')
,('Keurig'							,200	,400	,6		,'COOKING')
,('Blender'							,300	,1000	,20		,'COOKING')
,('Microwave'						,600	,1000	,15		,'COOKING')
,('Waffle Iron'						,800	,1500	,12		,'COOKING')
,('Hot Plate'						,750	,1500	,15		,'COOKING')
,('Electric Skillet'				,1000	,1500	,13		,'COOKING')
,('Toaster Oven'					,700	,1200	,2		,'COOKING')
,('Toaster'							,800	,1500	,1.5	,'COOKING')
,('Furnace Fan'						,750	,1200	,15		,'COOKING')	-- Ventilateur de four
,('Vacuum Cleaner'					,300	,1500	,25		,'OTHER') -- hoover
,('toy1'							,100	,200	,25		,'OTHER') 
,('toy2'							,150	,300	,25		,'OTHER') 
,('toy3'							,200	,450	,25		,'OTHER') 
,('toy4'							,600	,1200	,25		,'OTHER') 
,('Household Fan'					,50		,120	,125	,'OTHER') -- Ventilateur 
,('Space Heater 1'					,750	,1500	,230	,'HEATING')
,('Lasko Personal Space Heater1' 	,100	,200 	,120 	,'HEATING')
,('Lasko Personal Space Heater2' 	,100	,200 	,120 	,'HEATING')
,('Lasko Personal Space Heater3' 	,100	,200 	,120 	,'HEATING')
,('Basics Ceramic  Heater'			,250	,500 	,120 	,'HEATING')
,('Honeywell Heat Bud  Heater'		,125	,250	,120 	,'HEATING')
,('Iseebiz Ceramic Space Heater'	,375	,750	,120  	,'HEATING')
,('Kloudic Ceramic Space Heater'	,450	,600	,120  	,'HEATING')
,('Clothes Iron'					,1000	,1500	,65		,'WASHING_DRYING')
,('Washing Machine'					,500	,1000	,75		,'WASHING_DRYING')
,('Refrigerator 1'					,375	,750	,0		,'COLD_APPLIANCES')
,('Refrigerator 2'					,450	,900	,0		,'COLD_APPLIANCES')
,('Chest Freezer 1'					,350	,700	,0		,'COLD_APPLIANCES')
,('Chest Freezer 2'					,250	,500	,0		,'COLD_APPLIANCES')
,('Clock Radio'						,10		,50	,25		,'AUDIOVISUAL')
,('Stereo'							,30		,100	,45		,'AUDIOVISUAL')
,('Cell Phone Charger 1'			,5		,10	,180	,'ICT')
,('Cell Phone Charger 2'			,5		,10	,180	,'ICT')
,('Cell Phone Charger 3'			,5		,10	,180	,'ICT')
,('Cell Phone Charger 4'			,5		,10	,180	,'ICT')
,('Laptop Computer 1'				,20		,75	,240	,'ICT')
,('Laptop Computer 2'				,20		,75	,240	,'ICT')
,('Laptop Computer 3'				,20		,75	,240	,'ICT')
,('MacBook Pro 1'					,30		,85	,240	,'ICT')
,('iPad / Tablet 1'					,10		,20	,54		,'ICT')
,('Desktop with Monitor'			,200	,400	,54		,'ICT')
,('Inkjet Printer'					,15		,75	,12*60 	,'ICT')
,('Laser Printer'					,500	,500	,12*60	,'ICT')
,('Satellite Dish / Receiver'		,20		,30	,0		,'ICT')
,('Photographic Strobe'				,200	,300	,45		,'AUDIOVISUAL')
,('TV 32" LED/LCD'					,30		,50		,90		,'AUDIOVISUAL')
,('TV 42" Plasma'					,140	,240	,90		,'AUDIOVISUAL')
,('Home Theater Projector'			,100	,200	,90		,'AUDIOVISUAL')
,('Blu-Ray or DVD Player'			,8		,15		,90		,'AUDIOVISUAL')
,('Video Game Console'				,40		,140	,75		,'AUDIOVISUAL')
,('Video Game Console2'				,45		,145	,75		,'AUDIOVISUAL')
,('Xbox one Console1'				,20		,104	,75		,'AUDIOVISUAL')
,('Xbox one Console2'				,20		,104	,75		,'AUDIOVISUAL')
,('Xbox one Console3'				,20		,104	,75		,'AUDIOVISUAL')
,('CFL/LED-W8  1'					,5		,8		,60		,'LIGHTING')
,('CFL/LED-W8 2'					,5		,8		,60		,'LIGHTING')
,('CFL/LED-W8 3'					,5		,8		,60		,'LIGHTING')
,('CFL/LED-W8 4'					,5		,8		,60		,'LIGHTING')
,('CFL/LED-W8 5'					,5		,8		,60		,'LIGHTING')
,('CFL/LED-W8 6'					,5		,8		,60		,'LIGHTING')
,('CFL/LED-W11 1'					,7		,11	,60		,'LIGHTING')
,('CFL/LED-W11 2'					,7		,11	,60		,'LIGHTING')
,('CFL/LED-W11 3'					,7		,11	,60		,'LIGHTING')
,('CFL/LED-W11 4'					,7		,11	,60		,'LIGHTING')
,('CFL/LED-W11 5'					,7		,11	,60		,'LIGHTING')
,('Incandescent light bulb W40 1'	,20	,40	,45		,'LIGHTING')
,('Incandescent light bulb W40 2'	,20	,40	,45		,'LIGHTING')
,('Incandescent light bulb W40 3'	,20	,40	,45		,'LIGHTING')
,('Incandescent light bulb W40 4'	,20	,40	,45		,'LIGHTING')
,('Incandescent light bulb W80 1'	,40	,80	,45		,'LIGHTING')
,('Incandescent light bulb W80 2'	,40	,80	,45		,'LIGHTING')
,('Incandescent light bulb W80 3'	,40	,80	,45		,'LIGHTING')
,('Incandescent light bulb W80 4'	,40	,80	,45		,'LIGHTING')
,('Incandescent light bulb W100 1'	,50	,100	,45		,'LIGHTING')
,('Incandescent light bulb W100 2'	,50	,100	,45		,'LIGHTING')
,('Incandescent light bulb W100 3'	,50	,100	,45		,'LIGHTING')
,('Incandescent light bulb W100 4'	,50	,100	,45		,'LIGHTING')
,('Incandescent light bulb W100 5'	,50	,100	,45		,'LIGHTING')
,('Incandescent light bulb W100 6'	,50	,100	,45		,'LIGHTING')
,('Incandescent light bulb W100 7'	,50	,100	,45		,'LIGHTING')
,('Incandescent light bulb W100 8'	,50	,100	,45		,'LIGHTING')
,('Incandescent light bulb W60 1'	,30	,60	,45		,'LIGHTING')
,('Incandescent light bulb W60 2'	,30	,60	,45		,'LIGHTING')
,('Incandescent light bulb W60 3'	,30	,60	,45		,'LIGHTING')
,('Halogen light bulbs 7 pc 1'		,10	,50 	,45		,'LIGHTING')
,('Halogen light bulbs 7 pc 2'		,10	,50 	,45		,'LIGHTING')
,('Halogen light bulbs 7 pc 3'		,10	,50 	,45		,'LIGHTING')
,('Halogen light bulbs 7 pc 4'		,10	,50 	,45		,'LIGHTING')
,('Energy-saving light bulb W11 1'	,6	,11	,45		,'LIGHTING')
,('Energy-saving light bulb W11 2'	,6	,11	,45		,'LIGHTING')
,('Energy-saving light bulb W11 3'	,6	,11	,45		,'LIGHTING')
,('Energy-saving light bulb W11 4'	,6	,11	,45		,'LIGHTING')
,('Energy-saving light bulb W18 1'	,11	,18	,45		,'LIGHTING')
,('Energy-saving light bulb W18 2'	,11	,18	,45		,'LIGHTING')
,('Energy-saving light bulb W18 3'	,11	,18	,45		,'LIGHTING')
,('Energy-saving light bulb W18 4'	,11	,18	,45		,'LIGHTING')
,('Energy-saving light bulb W18 5'	,11	,18	,45		,'LIGHTING')
,('Theem Water Heaters with leak guard 1',100,4500	,120	,'WATER_HEATING')
,('Theem Water Heaters with leak guard 2',66,3000	,120	,'WATER_HEATING')
,('800ML Dental Jet Water Flosser'	,9		,18		,5		,'SHOWERS')
,('800ML Dental Jet Water Flosser2'	,2.5	,5		,5		,'SHOWERS')
,('800ML Dental Jet Water Flosser3'	,2.5	,5		,5		,'SHOWERS')
,('800ML Dental Jet Water Flosser4'	,2.5	,5		,5		,'SHOWERS')
,('800ML Dental Jet Water Flosser5'	,2.5	,5		,5		,'SHOWERS')
,('Bathroom Fan 1'					,40		,80,	15		,'SHOWERS')
,('Bathroom Fan 2'					,40		,80,	15		,'SHOWERS')
,('Bathroom Fan 3'					,30		,60,	15		,'SHOWERS')
,('Hair Dryer 1'					,300	,600	,15		,'SHOWERS')
,('Hair Dryer 2'					,200	,400	,15		,'SHOWERS')
,('Boiler 100 l'					,1100	,2200	,45		,'SHOWERS')

,('Not identified 10-1'				,10		,50		,60,'UNKNOWN')
,('Not identified 10-2'				,10		,50		,60,'UNKNOWN')
,('Not identified 10-3'				,10		,50		,60,'UNKNOWN')
,('Not identified 10-4'				,10		,50		,60,'UNKNOWN')
,('Not identified 10-5'				,10		,50		,60,'UNKNOWN')
,('Not identified 50-1'				,50		,100	,60,'UNKNOWN')
,('Not identified 50-2'				,50		,100	,60,'UNKNOWN')
,('Not identified 50-3'				,50		,100	,60,'UNKNOWN')
,('Not identified 50-4'				,50		,100	,60,'UNKNOWN')
,('Not identified 50-5'				,50		,100	,60,'UNKNOWN')
,('Not identified 100-1'			,100	,500	,60,'UNKNOWN')
,('Not identified 100-2'			,100	,500	,60,'UNKNOWN')
,('Not identified 100-3'			,100	,500	,60,'UNKNOWN')
,('Not identified 100-4'			,100	,500	,60,'UNKNOWN')
,('Not identified 100-5'			,100	,500	,60,'UNKNOWN')
,('Not identified 500-1'			,500	,1000	,60,'UNKNOWN')
,('Not identified 500-2'			,500	,1000	,60,'UNKNOWN')
,('Not identified 500-3'			,500	,1000	,60,'UNKNOWN')
,('Not identified 500-4'			,500	,1000	,60,'UNKNOWN')
,('Not identified 500-5'			,500	,1000	,60,'UNKNOWN')

�
UPDATE device SET priority_level = 2 WHERE name  IN ('Refrigerator','Chest Freezer','Satellite Dish / Receiver')

�

INSERT INTO device(is_producer,name,power_min,power_max,avg_duration,category) VALUES
	 (1,'EDF'					,0	,1500  ,0		,'EXTERNAL_ENG')
	,(1,'Wind turbine 1'		,0	,1000  	,60		,'WIND_ENG'				)
	,(1,'Wind turbine 2'		,0	,1000  	,60		,'WIND_ENG'				)
	,(1,'Wind turbine 3'		,0	,1000  	,60		,'WIND_ENG'				)
	,(1,'Solar panel 1'			,0	,1000	,60		,'SOLOR_ENG'			)
	,(1,'biomass prod-01'		,0	,1000	,60		,'BIOMASS_ENG'			)
	,(1, 'hydro prod_01'		,0	,1000	,60		,'HYDRO_ENG'			)


DELIMITER �
�
TRUNCATE device_statistic
�
insert into device_statistic values
 	 ('HEATING'			,0,1	,64	)
	,('WATER_HEATING'	,0,1	,3	)
	,('SHOWERS'			,0,1	,2	)
	,('WASHING_DRYING'	,0,1	,40	)
	,('COOKING'			,0,1	,1	)
	,('LIGHTING'		,0,1	,50	)
	,('COLD_APPLIANCES'	,0,1	,70	)
	,('ICT'				,0,1	,15	)
	,('AUDIOVISUAL'		,0,1	,50	)
	,('OTHER'			,0,1	,10	)
	,('UNKNOWN'			,0,1	,72	)
	-- Energy
	,('EXTERNAL_ENG'	,0,1	,78)
	,('WIND_ENG'		,0,1	,20.47)
	,('SOLOR_ENG'		,0,1	,0.82)
	,('BIOMASS_ENG'		,0,1	,9.69)
	,('HYDRO_ENG'		,0,1	,55 )
�
insert into device_statistic values
 	 ('HEATING'				,1,2	, 35	)
	,('WATER_HEATING'		,1,2	, 3	)
	,('SHOWERS'				,1,2	, 2	)
	,('WASHING_DRYING'		,1,2	, 22	)
	,('COOKING'				,1,2	, 1	)
	,('LIGHTING'			,1,2	, 36	)
	,('COLD_APPLIANCES'		,1,2	, 60	)
	,('ICT'					,1,2	, 16	)
	,('AUDIOVISUAL'			,1,2	, 36	)
	,('OTHER'				,1,2	, 15	)
	,('UNKNOWN'				,1,2	, 61	)
	-- Energy
	,('EXTERNAL_ENG'	,1,2	,78)
	,('WIND_ENG'		,1,2	,19.66)
	,('SOLOR_ENG'		,1,2	,0.82)
	,('BIOMASS_ENG'		,1,2	,9.63)
	,('HYDRO_ENG'		,1,2	,55.87)
�
insert into device_statistic values
 	 ('HEATING'	, 2,3	, 39	)
	,('WATER_HEATING'	, 2,3	, 4	)
	,('SHOWERS'	, 2,3	, 2	)
	,('WASHING_DRYING'	, 2,3	, 14	)
	,('COOKING'	, 2,3	, 1	)
	,('LIGHTING'	, 2,3	, 22	)
	,('COLD_APPLIANCES'	, 2,3	, 59	)
	,('ICT'	, 2,3	, 13	)
	,('AUDIOVISUAL'	, 2,3	, 30	)
	,('OTHER'	, 2,3	, 13	)
	,('UNKNOWN'	, 2,3	, 60	)
	-- Energy
	,('EXTERNAL_ENG'	,2,3	,	78	)
	,('WIND_ENG'		,2,3	,	17.89	)
	,('SOLOR_ENG'		,2,3	,	0.84	)
	,('BIOMASS_ENG'		,2,3	,	9.61	)
	,('HYDRO_ENG'		,2,3	,	57.57	)
�
insert into device_statistic values
 	 ('HEATING'	, 3,4	, 25	)
	,('WATER_HEATING'	, 3,4	, 16	)
	,('SHOWERS'	, 3,4	, 2	)
	,('WASHING_DRYING'	, 3,4	, 14	)
	,('COOKING'	, 3,4	, 2	)
	,('LIGHTING'	, 3,4	, 20	)
	,('COLD_APPLIANCES'	, 3,4	, 60	)
	,('ICT'	, 3,4	, 14	)
	,('AUDIOVISUAL'	, 3,4	, 26	)
	,('OTHER'	, 3,4	, 10	)
	,('UNKNOWN'	, 3,4	, 57	)
	-- Energy
	,('EXTERNAL_ENG'	,3,4	,	78	)
	,('WIND_ENG'		,3,4	,	16.8	)
	,('SOLOR_ENG'		,3,4	,	1.58	)
	,('BIOMASS_ENG'		,3,4	,	9.6	)
	,('HYDRO_ENG'		,3,4	,	58.03	)
�
insert into device_statistic values
 	 ('HEATING'	, 4,5	, 23	)
	,('WATER_HEATING'	, 4,5	, 6	)
	,('SHOWERS'	, 4,5	, 2	)
	,('WASHING_DRYING'	, 4,5	, 7	)
	,('COOKING'	, 4,5	, 2	)
	,('LIGHTING'	, 4,5	, 18	)
	,('COLD_APPLIANCES'	, 4,5	, 59	)
	,('ICT'	, 4,5	, 15	)
	,('AUDIOVISUAL'	, 4,5	, 24	)
	,('OTHER'	, 4,5	, 11	)
	,('UNKNOWN'	, 4,5	, 58	)
	-- Energy
	,('EXTERNAL_ENG'	,4,5	,	78	)
	,('WIND_ENG'		,4,5	,	15.15	)
	,('SOLOR_ENG'		,4,5	,	4.22	)
	,('BIOMASS_ENG'		,4,5	,	9.6	)
	,('HYDRO_ENG'		,4,5	,	63.01	)
�
insert into device_statistic values
 	 ('HEATING'	, 5,6	, 21	)
	,('WATER_HEATING'	, 5,6	, 36	)
	,('SHOWERS'	, 5,6	, 2	)
	,('WASHING_DRYING'	, 5,6	, 6	)
	,('COOKING'	, 5,6	, 2	)
	,('LIGHTING'	, 5,6	, 17	)
	,('COLD_APPLIANCES'	, 5,6	, 60	)
	,('ICT'	, 5,6	, 13	)
	,('AUDIOVISUAL'	, 5,6	, 21	)
	,('OTHER'	, 5,6	, 13	)
	,('UNKNOWN'	, 5,6	, 59	)
	-- Energy
	,('EXTERNAL_ENG'	,5,6	,	78	)
	,('WIND_ENG'		,5,6	,	12.48	)
	,('SOLOR_ENG'		,5,6	,	11.07	)
	,('BIOMASS_ENG'		,5,6	,	9.68	)
	,('HYDRO_ENG'		,5,6	,	76.51	)
�
insert into device_statistic values
 	 ('HEATING'	, 6,7	, 21	)
	,('WATER_HEATING'	, 6,7	, 6	)
	,('SHOWERS'	, 6,7	, 2	)
	,('WASHING_DRYING'	, 6,7	, 6	)
	,('COOKING'	, 6,7	, 8	)
	,('LIGHTING'	, 6,7	, 18	)
	,('COLD_APPLIANCES'	, 6,7	, 60	)
	,('ICT'	, 6,7	, 15	)
	,('AUDIOVISUAL'	, 6,7	, 21	)
	,('OTHER'	, 6,7	, 13	)
	,('UNKNOWN'	, 6,7	, 60	)
	-- Energy
	,('EXTERNAL_ENG'	,6,7	,	78	)
	,('WIND_ENG'		,6,7	,	10.99	)
	,('SOLOR_ENG'		,6,7	,	23.16	)
	,('BIOMASS_ENG'		,6,7	,	9.68	)
	,('HYDRO_ENG'		,6,7	,	83.77	)
�
insert into device_statistic values
 	 ('HEATING'	, 7,8	, 22	)
	,('WATER_HEATING'	, 7,8	, 13	)
	,('SHOWERS'	, 7,8	, 22	)
	,('WASHING_DRYING'	, 7,8	, 15	)
	,('COOKING'	, 7,8	, 38	)
	,('LIGHTING'	, 7,8	, 23	)
	,('COLD_APPLIANCES'	, 7,8	, 62	)
	,('ICT'	, 7,8	, 15	)
	,('AUDIOVISUAL'	, 7,8	, 25	)
	,('OTHER'	, 7,8	, 14	)
	,('UNKNOWN'	, 7,8	, 79	)
	-- Energy
	,('EXTERNAL_ENG'	,7,8	,	78	)
	,('WIND_ENG'		,7,8	,	9.86	)
	,('SOLOR_ENG'		,7,8	,	38.09	)
	,('BIOMASS_ENG'		,7,8	,	9.65	)
	,('HYDRO_ENG'		,7,8	,	80.1	)
�
insert into device_statistic values
 	 ('HEATING'	, 8,9	, 25	)
	,('WATER_HEATING'	, 8,9	, 9	)
	,('SHOWERS'	, 8,9	, 37	)
	,('WASHING_DRYING'	, 8,9	, 60	)
	,('COOKING'	, 8,9	, 64	)
	,('LIGHTING'	, 8,9	, 49	)
	,('COLD_APPLIANCES'	, 8,9	, 62	)
	,('ICT'	, 8,9	, 17	)
	,('AUDIOVISUAL'	, 8,9	, 47	)
	,('OTHER'	, 8,9	, 20	)
	,('UNKNOWN'	, 8,9	, 102	)
	-- Energy
	,('EXTERNAL_ENG'	,8,9	,	78	)
	,('WIND_ENG'		,8,9	,	8.34	)
	,('SOLOR_ENG'		,8,9	,	50.85	)
	,('BIOMASS_ENG'		,8,9	,	9.71	)
	,('HYDRO_ENG'		,8,9	,	77.76	)
�
insert into device_statistic values
 	 ('HEATING'	, 9,10	, 24	)
	,('WATER_HEATING'	, 9,10	, 8	)
	,('SHOWERS'	, 9,10	, 30	)
	,('WASHING_DRYING'	, 9,10	, 78	)
	,('COOKING'	, 9,10	, 59	)
	,('LIGHTING'	, 9,10	, 44	)
	,('COLD_APPLIANCES'	, 9,10	, 61	)
	,('ICT'	, 9,10	, 19	)
	,('AUDIOVISUAL'	, 9,10	, 48	)
	,('OTHER'	, 9,10	, 20	)
	,('UNKNOWN'	, 9,10	, 105	)
	-- Energy
	,('EXTERNAL_ENG'	,9,10	,	78	)
	,('WIND_ENG'		,9,10	,	7.76	)
	,('SOLOR_ENG'		,9,10	,	58.76	)
	,('BIOMASS_ENG'		,9,10	,	9.71	)
	,('HYDRO_ENG'		,9,10	,	75.64	)
�
insert into device_statistic values
 	 ('HEATING'	, 10,11	, 25	)
	,('WATER_HEATING'	, 10,11	, 8	)
	,('SHOWERS'	, 10,11	, 22	)
	,('WASHING_DRYING'	, 10,11	, 84	)
	,('COOKING'	, 10,11	, 47	)
	,('LIGHTING'	, 10,11	, 33	)
	,('COLD_APPLIANCES'	, 10,11	, 61	)
	,('ICT'	, 10,11	, 25	)
	,('AUDIOVISUAL'	, 10,11	, 47	)
	,('OTHER'	, 10,11	, 20	)
	,('UNKNOWN'	, 10,11	, 107	)
	-- Energy
	,('EXTERNAL_ENG'	,10,11	,	78	)
	,('WIND_ENG'		,10,11	,	8.07	)
	,('SOLOR_ENG'		,10,11	,	61.15	)
	,('BIOMASS_ENG'		,10,11	,	9.69	)
	,('HYDRO_ENG'		,10,11	,	73.32	)
�
insert into device_statistic values
 	 ('HEATING'	, 11,12	, 24	)
	,('WATER_HEATING'	, 11,12	, 8	)
	,('SHOWERS'	, 11,12	, 19	)
	,('WASHING_DRYING'	, 11,12	, 78	)
	,('COOKING'	, 11,12	, 48	)
	,('LIGHTING'	, 11,12	, 28	)
	,('COLD_APPLIANCES'	, 11,12	, 61	)
	,('ICT'	, 11,12	, 26	)
	,('AUDIOVISUAL'	, 11,12	, 49	)
	,('OTHER'	, 11,12	, 21	)
	,('UNKNOWN'	, 11,12	, 103	)
	-- Energy
	,('EXTERNAL_ENG'	,11,12	,	78	)
	,('WIND_ENG'		,11,12	,	8.42	)
	,('SOLOR_ENG'		,11,12	,	61.14	)
	,('BIOMASS_ENG'		,11,12	,	9.63	)
	,('HYDRO_ENG'		,11,12	,	71.11	)
�
insert into device_statistic values
 	 ('HEATING'	, 12,13	, 19	)
	,('WATER_HEATING'	, 12,13	, 7	)
	,('SHOWERS'	, 12,13	, 22	)
	,('WASHING_DRYING'	, 12,13	, 81	)
	,('COOKING'	, 12,13	, 66	)
	,('LIGHTING'	, 12,13	, 29	)
	,('COLD_APPLIANCES'	, 12,13	, 62	)
	,('ICT'	, 12,13	, 27	)
	,('AUDIOVISUAL'	, 12,13	, 52	)
	,('OTHER'	, 12,13	, 21	)
	,('UNKNOWN'	, 12,13	, 92	)
	-- Energy
	,('EXTERNAL_ENG'	,12,13	,	78	)
	,('WIND_ENG'		,12,13	,	9.85	)
	,('SOLOR_ENG'		,12,13	,	58.45	)
	,('BIOMASS_ENG'		,12,13	,	9.63	)
	,('HYDRO_ENG'		,12,13	,	70.9	)
�
insert into device_statistic values
 	 ('HEATING'	, 13,14	, 21	)
	,('WATER_HEATING'	, 13,14	, 8	)
	,('SHOWERS'	, 13,14	, 14	)
	,('WASHING_DRYING'	, 13,14	, 77	)
	,('COOKING'	, 13,14	, 79	)
	,('LIGHTING'	, 13,14	, 28	)
	,('COLD_APPLIANCES'	, 13,14	, 62	)
	,('ICT'	, 13,14	, 27	)
	,('AUDIOVISUAL'	, 13,14	, 60	)
	,('OTHER'	, 13,14	, 21	)
	,('UNKNOWN'	, 13,14	, 104	)
	-- Energy
	,('EXTERNAL_ENG'	,13,14	,	78	)
	,('WIND_ENG'		,13,14	,	12.88	)
	,('SOLOR_ENG'		,13,14	,	52.22	)
	,('BIOMASS_ENG'		,13,14	,	9.66	)
	,('HYDRO_ENG'		,13,14	,	69.49	)
�
insert into device_statistic values
 	 ('HEATING'	, 14,15	, 22	)
	,('WATER_HEATING'	, 14,15	, 9	)
	,('SHOWERS'	, 14,15	, 8	)
	,('WASHING_DRYING'	, 14,15	, 62	)
	,('COOKING'	, 14,15	, 46	)
	,('LIGHTING'	, 14,15	, 25	)
	,('COLD_APPLIANCES'	, 14,15	, 62	)
	,('ICT'	, 14,15	, 28	)
	,('AUDIOVISUAL'	, 14,15	, 62	)
	,('OTHER'	, 14,15	, 22	)
	,('UNKNOWN'	, 14,15	, 93	)
	-- Energy
	,('EXTERNAL_ENG'	,14,15	,	78	)
	,('WIND_ENG'		,14,15	,	16.06	)
	,('SOLOR_ENG'		,14,15	,	43.31	)
	,('BIOMASS_ENG'		,14,15	,	9.68	)
	,('HYDRO_ENG'		,14,15	,	71.94	)
�
insert into device_statistic values
 	 ('HEATING'	, 15,16	, 22	)
	,('WATER_HEATING'	, 15,16	, 9	)
	,('SHOWERS'	, 15,16	, 11	)
	,('WASHING_DRYING'	, 15,16	, 68	)
	,('COOKING'	, 15,16	, 43	)
	,('LIGHTING'	, 15,16	, 29	)
	,('COLD_APPLIANCES'	, 15,16	, 63	)
	,('ICT'	, 15,16	, 31	)
	,('AUDIOVISUAL'	, 15,16	, 62	)
	,('OTHER'	, 15,16	, 24	)
	,('UNKNOWN'	, 15,16	, 95	)
	-- Energy
	,('EXTERNAL_ENG'	,15,16	,	78	)
	,('WIND_ENG'		,15,16	,	17.14	)
	,('SOLOR_ENG'		,15,16	,	33.2	)
	,('BIOMASS_ENG'		,15,16	,	9.65	)
	,('HYDRO_ENG'		,15,16	,	74.56	)
�
insert into device_statistic values
 	 ('HEATING'	, 16,17	, 25	)
	,('WATER_HEATING'	, 16,17	, 9	)
	,('SHOWERS'	, 16,17	, 10	)
	,('WASHING_DRYING'	, 16,17	, 65	)
	,('COOKING'	, 16,17	, 66	)
	,('LIGHTING'	, 16,17	, 30	)
	,('COLD_APPLIANCES'	, 16,17	, 64	)
	,('ICT'	, 16,17	, 29	)
	,('AUDIOVISUAL'	, 16,17	, 68	)
	,('OTHER'	, 16,17	, 25	)
	,('UNKNOWN'	, 16,17	, 98	)
	-- Energy
	,('EXTERNAL_ENG'	,16,17	,	78	)
	,('WIND_ENG'		,16,17	,	17.97	)
	,('SOLOR_ENG'		,16,17	,	23.65	)
	,('BIOMASS_ENG'		,16,17	,	9.59	)
	,('HYDRO_ENG'		,16,17	,	78.62	)
�
insert into device_statistic values
 	 ('HEATING'	, 17,18	, 38	)
	,('WATER_HEATING'	, 17,18	, 10	)
	,('SHOWERS'	, 17,18	, 8	)
	,('WASHING_DRYING'	, 17,18	, 63	)
	,('COOKING'	, 17,18	, 121	)
	,('LIGHTING'	, 17,18	, 62	)
	,('COLD_APPLIANCES'	, 17,18	, 66	)
	,('ICT'	, 17,18	, 33	)
	,('AUDIOVISUAL'	, 17,18	, 65	)
	,('OTHER'	, 17,18	, 22	)
	,('UNKNOWN'	, 17,18	, 113	)
	-- Energy
	,('EXTERNAL_ENG'	,17,18	,	78	)
	,('WIND_ENG'		,17,18	,	18.89	)
	,('SOLOR_ENG'		,17,18	,	14.59	)
	,('BIOMASS_ENG'		,17,18	,	9.58	)
	,('HYDRO_ENG'		,17,18	,	81	)
�
insert into device_statistic values
 	 ('HEATING'	, 18,19	, 25	)
	,('WATER_HEATING'	, 18,19	, 10	)
	,('SHOWERS'	, 18,19	, 17	)
	,('WASHING_DRYING'	, 18,19	, 60	)
	,('COOKING'	, 18,19	, 123	)
	,('LIGHTING'	, 18,19	, 114	)
	,('COLD_APPLIANCES'	, 18,19	, 66	)
	,('ICT'	, 18,19	, 35	)
	,('AUDIOVISUAL'	, 18,19	, 104	)
	,('OTHER'	, 18,19	, 23	)
	,('UNKNOWN'	, 18,19	, 137	)
	-- Energy
	,('EXTERNAL_ENG'	,18,19	,	78	)
	,('WIND_ENG'		,18,19	,	20.03	)
	,('SOLOR_ENG'		,18,19	,	6.41	)
	,('BIOMASS_ENG'		,18,19	,	9.73	)
	,('HYDRO_ENG'		,18,19	,	79.39	)
�
insert into device_statistic values
 	 ('HEATING'	, 19,20	, 27	)
	,('WATER_HEATING'	, 19,20	, 10	)
	,('SHOWERS'	, 19,20	, 18	)
	,('WASHING_DRYING'	, 19,20	, 61	)
	,('COOKING'	, 19,20	, 118	)
	,('LIGHTING'	, 19,20	, 120	)
	,('COLD_APPLIANCES'	, 19,20	, 66	)
	,('ICT'	, 19,20	, 37	)
	,('AUDIOVISUAL'	, 19,20	, 106	)
	,('OTHER'	, 19,20	, 22	)
	,('UNKNOWN'	, 19,20	, 130	)
	-- Energy
	,('EXTERNAL_ENG'	,19,20	,	78	)
	,('WIND_ENG'		,19,20	,	21.49	)
	,('SOLOR_ENG'		,19,20	,	1.76	)
	,('BIOMASS_ENG'		,19,20	,	9.61	)
	,('HYDRO_ENG'		,19,20	,	74.18	)
�
insert into device_statistic values
 	 ('HEATING'			, 20,21	, 36	)
	,('WATER_HEATING'	, 20,21	, 12	)
	,('SHOWERS'			, 20,21	, 17	)
	,('WASHING_DRYING'	, 20,21	, 73	)
	,('COOKING'			, 20,21	, 80	)
	,('LIGHTING'		, 20,21	, 121	)
	,('COLD_APPLIANCES'	, 20,21	, 66	)
	,('ICT'				, 20,21	, 36	)
	,('AUDIOVISUAL'		, 20,21	, 117	)
	,('OTHER'			, 20,21	, 19	)
	,('UNKNOWN'			, 20,21	, 126	)
	-- Energy
	,('EXTERNAL_ENG'	,20,21	,	78	)
	,('WIND_ENG'		,20,21	,	23.51	)
	,('SOLOR_ENG'		,20,21	,	0.83	)
	,('BIOMASS_ENG'		,20,21	,	9.59	)
	,('HYDRO_ENG'		,20,21	,	67.84	)
�
insert into device_statistic values
 	 ('HEATING'	, 21,22	, 28	)
	,('WATER_HEATING'	, 21,22	, 12	)
	,('SHOWERS'	, 21,22	, 10	)
	,('WASHING_DRYING'	, 21,22	, 62	)
	,('COOKING'	, 21,22	, 51	)
	,('LIGHTING'	, 21,22	, 129	)
	,('COLD_APPLIANCES'	, 21,22	, 64	)
	,('ICT'	, 21,22	, 37	)
	,('AUDIOVISUAL'	, 21,22	, 118	)
	,('OTHER'	, 21,22	, 19	)
	,('UNKNOWN'	, 21,22	, 125	)
	-- Energy
	,('EXTERNAL_ENG'	,21,22	,	78	)
	,('WIND_ENG'		,21,22	,	25.43	)
	,('SOLOR_ENG'		,21,22	,	0.79	)
	,('BIOMASS_ENG'		,21,22	,	9.58	)
	,('HYDRO_ENG'		,21,22	,	59.9	)
�
insert into device_statistic values
 	 ('HEATING'	, 22,23	, 21	)
	,('WATER_HEATING'	, 22,23	, 8	)
	,('SHOWERS'	, 22,23	, 7	)
	,('WASHING_DRYING'	, 22,23	, 40	)
	,('COOKING'	, 22,23	, 37	)
	,('LIGHTING'	, 22,23	, 140	)
	,('COLD_APPLIANCES'	, 22,23	, 64	)
	,('ICT'	, 22,23	, 26	)
	,('AUDIOVISUAL'	, 22,23	, 112	)
	,('OTHER'	, 22,23	, 17	)
	,('UNKNOWN'	, 22,23	, 115	)
	-- Energy
	,('EXTERNAL_ENG'	,22,23	,	78	)
	,('WIND_ENG'		,22,23	,	26.59	)
	,('SOLOR_ENG'		,22,23	,	0.82	)
	,('BIOMASS_ENG'		,22,23	,	9.68	)
	,('HYDRO_ENG'		,22,23	,	52.7	)
�
insert into device_statistic values
 	 ('HEATING'	, 23,24	, 39	)
	,('WATER_HEATING'	, 23,24	, 7	)
	,('SHOWERS'	, 23,24	, 6	)
	,('WASHING_DRYING'	, 23,24	, 41	)
	,('COOKING'	, 23,24	, 25	)
	,('LIGHTING'	, 23,24	, 121	)
	,('COLD_APPLIANCES'	, 23,24	, 64	)
	,('ICT'	, 23,24	, 25	)
	,('AUDIOVISUAL'	, 23,24	, 90	)
	,('OTHER'	, 23,24	, 18	)
	,('UNKNOWN'	, 23,24	, 105	)
	-- Energy
	,('EXTERNAL_ENG'	,23,24	,	78	)
	,('WIND_ENG'		,23,24	,	25.67	)
	,('SOLOR_ENG'		,23,24	,	0.82	)
	,('BIOMASS_ENG'		,23,24	,	9.66	)
	,('HYDRO_ENG'		,23,24	,	50.98	)
�
select start_hour, SUM(power) from device_statistic group by start_hour