
select * from measure_record where blob_name like 'SE05000283/2022-04-19_%.raw' AND feature_type='MN' order by timestamp 


select * from sensor
 join sensor_input si  on si.id_sensor  = sensor.id

 'CH1022501234500000000000000326365' - 'CH1022501234500000000000000326325'
 
select * from sensor_input si 

alter table sensor_input  add `is_disabled` 			BIT(1) NOT NULL DEFAULT b'0'


use clemap_data
use clemap_data_light
-- truncate meteo_data
-- delete from meteo_data


select
	Count(*) as NB
	,SUM(se) as se
	,SUM(az) as az
	,SUM(gh) as gh
	,SUM(Dh) as Dh
	,SUM(Bn) as Bn
	,SUM(Ta) as Ta
	,SUM(HR) as HR
	,SUM(w) as w
	,SUM(vv) as vv
	,SUM(dv) as dv
	,SUM(pr) as pr
	,SUM(Gn) as Gn
	,SUM(Ge) as Ge
	,SUM(Gs) as Gs
	,SUM(Gw) as Gw
	,SUM(G35e) as G35e
	,SUM(G35s) as G35s
	,SUM(G35w) as G35w
	,SUM(G45s) as G45s
	,SUM(Gtrack) as Gtrack
	,SUM(G35_45) as G35_45
	,SUM(min_sun) as min_sun
	,SUM(IR) as IR
	,SUM(IRd) as IRd
	,SUM(IRup) as IRup
	from  meteo_data

select ProdTT.*
		,ProdTT.p as p_tt
		,ProdSold.p as p_sold
		,ProdTT.p - ProdSold.p as p_not_sold
	FROM
	(select `timestamp` ,p from measure_record mr
		join phase_measure_record pmr on pmr.id_measure_record  = mr.id	
		where mr.sensor_number = 'CH1022501234500000000000000326365'
	) as ProdTT	
join 
	(
	select `timestamp` ,p from measure_record mr
		join phase_measure_record pmr on pmr.id_measure_record  = mr.id	
		where mr.sensor_number = 'CH1022501234500000000000000326325' 
	) as ProdSold	on ProdSold.timestamp = ProdTT.timestamp
	where ProdTT.p - ProdSold.p < 0.001

select * from sensor_input

select count(*) from phase_measure_record pmr 

use clemap_data_exoscale

INSERT INTO sensor(name,firmeware_version,serial_number,location,electrical_panel) VALUES
	 ('SE05000160'		,''	,'SE05000160'	,'Hepia'	,'???')
;

select * from sensor_input si where id_sensor = 121

select sensor_number, COunt(*) from measure_record mr group by sensor_number

select sensor_number, COunt(*)  from measure_record mr  where sensor_number not in (select serial_number  from sensor)
	group by  sensor_number

select * from sensor s 
	left join sensor_input si ON si.id_sensor = s.id 


select count(*) from clemap_data.measure_record mr 			2608473
select count(*) from clemap_data_old.measure_record mr 		2608473

select count(*) from clemap_data.phase_measure_record pmr  	7807563
select count(*) from clemap_data_old.phase_measure_record pmr	7807563

use clemap_data_light
use clemap_data

select * from sensor  where serial_number  like 'CH%'

select count(*) from measure_record mr where  feature_type = 'MN' 
297721

SELECT TIMESTAMPADD(MINUTE,
				 TIMESTAMPDIFF(MINUTE,DATE(timestamp),timestamp),
				DATE(timestamp)) AS timestamp3
		,TIMESTAMPDIFF(MINUTE,DATE(timestamp),timestamp) as test_diff
		,mr.*
		FROM measure_record mr where 1


feature_type = '15_MN' 

select count(*), feature_type from clemap_data.measure_record mr where not feature_type = 'TEN_SEC' group by feature_type 


alter table measure_record add key _blob_name_sensor_number(blob_name, sensor_number);

SELECT  blob_name, sensor_number , count(*) AS nbMR FROM measure_record
            -- JOIN phase_measure_record ON phase_measure_record.id_measure_record = measure_record.ID
            where blob_name like '2022-07-%'
               group by  blob_name, sensor_number

SELECT  blob_name, sensor_number , count(*) AS nbMR FROM measure_record
            -- JOIN phase_measure_record ON phase_measure_record.id_measure_record = measure_record.ID
            where blob_name like '2022-07-%'
               group by  blob_name, sensor_number

SELECT  blob_name, sensor_number , count(*) AS nbMR FROM measure_record
            JOIN phase_measure_record ON phase_measure_record.id_measure_record = measure_record.ID
            where blob_name like '2022-07-%'
               group by  blob_name, sensor_number

SELECT  measure_record.* 
,TIMESTAMPADD(MINUTE,
    TIMESTAMPDIFF(MINUTE,DATE(timestamp),timestamp),
    DATE(timestamp)) AS timestamp3
,sensor.serial_number AS sensor_number
,sensor.location
FROM clemap_data.measure_record
 JOIN clemap_data.sensor on sensor.serial_number = measure_record.sensor_number
 where serial_number  like 'CH%'
 ORDER BY timestamp DESC LIMIT 0,1 
 
 
mysql --user=import_clemap --password=sql2537 cleamap_data_old < C:\Users\phili\git\stage\smartgrids\energy2\mariadb\clemap_data_old.sql
GRANT drop, create,alter,SELECT, INSERT, UPDATE, DELETE, REFERENCES, INDEX, CREATE TEMPORARY TABLES, LOCK TABLES, EXECUTE, SHOW VIEW, CREATE ROUTINE, ALTER ROUTINE ON clemap_data.* TO 'import_clemap'@'%';
GRANT drop, create,alter,SELECT, INSERT, UPDATE, DELETE, REFERENCES, INDEX, CREATE TEMPORARY TABLES, LOCK TABLES, EXECUTE, SHOW VIEW, CREATE ROUTINE, ALTER ROUTINE ON clemap_data_light.* TO 'import_clemap'@'%';
GRANT drop, create,alter,SELECT, INSERT, UPDATE, DELETE, REFERENCES, INDEX, CREATE TEMPORARY TABLES, LOCK TABLES, EXECUTE, SHOW VIEW, CREATE ROUTINE, ALTER ROUTINE ON clemap_data_light.* TO 'learning_agent'@'%';

grant all privileges on `clemap_data`.`*` to 'import_clemap'@'%';
use clemap_data_light
grant all privileges on `clemap_data_light`.`*` to 'import_clemap'@'%';
flush privileges

GRANT ALL PRIVILEGES ON *.* TO 'import_clemap'@'%' IDENTIFIED BY 'sql2537' WITH GRANT OPTION;
FLUSH PRIVILEGES;


select SumByDateAndSensor.date
	, SUM(1) as nbSensors
	, GROUP_CONCAT(SumByDateAndSensor.sensor_number order by sensor_number)
	, SUM(SumByDateAndSensor.nbMeasures) as nbMeasures 
from
	(
			select Date(timestamp) as Date, sensor_number, count(*) as nbMeasures from measure_record 
			where timestamp >= '2022-05-01'  and not sensor_number like 'CH%'
			group by  Date(timestamp), sensor_number
	) as SumByDateAndSensor
	group by SumByDateAndSensor.date

	
	select count(*) from phase_measure_record
	
	select * from sensor s 
		join sensor_input si  on si.id_sensor  = s.id 

	select * from phase_measure_record pmr where id_measure_record  = 1583454


	-- Extract avg oiwer
	select DATE(timestamp), avg(watt) as avg_watt , dayOfWeek(timestamp)
	from 
	(
		select  mr.id,`timestamp` , SUM(p) as watt, dayofweek(`timestamp`) as dayOfWeek
		from phase_measure_record
		join measure_record mr  on mr.id = phase_measure_record.id_measure_record 
		where  sensor_number ='SE05000238' AND feature_type = 'MN'
			 -- and dayofweek(`timestamp`)=7ZZ
			--  and timestamp like '2022-05-08 %'
		group by mr.id		
	) record1
	group by DATE(timestamp)
	order by avg_watt DESC
	
	
	
	
	select * from 
	(
		select  mr.id,`timestamp` , SUM(p) as watt, dayofweek(`timestamp`) as dayOfWeek
		from phase_measure_record
		join measure_record mr  on mr.id = phase_measure_record.id_measure_record 
		where  sensor_number ='SE05000238' AND feature_type = 'MN'
			 and dayofweek(`timestamp`)=7
			--  and timestamp like '2022-05-08 %'
		group by mr.id		
	) record1
	where watt>2000
	order by watt DESC

	
	
select * from measure_record mr  where blob_name = 'SE05000238/2022-06-01_01:37.raw' order by `timestamp` DESC
select * from measure_record mr  where sensor_number  = 'SE05000238' order by `timestamp` DESC
select sensor_number, blob_name, count(*) from measure_record where timestamp like '2022-05-29%' group by sensor_name, blob_name

alter table sensor modify column `serial_number`			VARCHAR(64) NOT NULL

alter table measure_record modify  column `feature_type` ENUM('MN', '15_MN', 'TEN_SEC')

alter table measure_record modify column `sensor_number` VARCHAR(64) NOT NULL

alter table measure_record modify column `blob_name` VARCHAR(64) NOT null

DELIMITER §
SET @id_324224 = (SELECT id FROM sensor WHERE serial_number='CH1022501234500000000000000324224')
§
SET @id_326325 = (SELECT id FROM sensor WHERE serial_number='CH1022501234500000000000000326325')
§
SET @id_326365 = (SELECT id FROM sensor WHERE serial_number='CH1022501234500000000000000326365')
§
INSERT INTO sensor_input(id_sensor,phase,panel_input,device_category, description) VALUES
	 (@id_324224, 'l1'	,'',	'EXTERNAL_ENG', 'Consommation SIG')
	,(@id_326325, 'l1'	,'',	'SOLOR_ENG', 'Production PV achetée par SIG')
	,(@id_326365, 'l1'	,'',	'SOLOR_ENG', 'Production PV brutte''')
§


desc measure_record

select * from phase_measure_record pmr where p>0  order by id desc

select * from measure_record mr where feature_type = '15_MN' 

select * from sensor

desc sensor

select * from sensor s 
	join sensor_input on sensor_input.id_sensor  = s.id



select * from sensor_input si 

select * from measure_record mr 
select * from phase_measure_record pmr 


select * from 

select max(timestamp) from measure_record where sensor_name = 'SE05000163' and timestamp like '2022-04-%' 


alter table Sensor modify `comment`				TEXT NOT NULL DEFAULT ''

select * from sensor

select * from sensor_input

alter table sensor_input  add `device_category` 		ENUM ('UNKNOWN','WATER_HEATING', 'HEATING', 'COOKING', 'SHOWERS', 'WASHING_DRYING', 'LIGHTING'
							, 'AUDIOVISUAL', 'COLD_APPLIANCES', 'ICT', 'OTHER', 'ELECTRICAL_PANEL'
							, 'WIND_ENG', 'SOLOR_ENG', 'EXTERNAL_ENG', 'BIOMASS_ENG', 'HYDRO_ENG')
							 DEFAULT 'UNKNOWN' 
							 after panel_input
				 
select * from sensor s 		 
					# SE05000163,SE05000238,SE05000283,SE05000319
							 
DELETE FROM sensor_input
;

;

SET @id_SE05000238 = (select id from sensor where serial_number='SE05000238')
;
INSERT INTO sensor_input(id_sensor,phase,panel_input,device_category, description) values
	 (@id_SE05000238, 'l1'	,'',	 'ELECTRICAL_PANEL', 'Entré du tableau TSC.01')
	,(@id_SE05000238, 'l2'	,'',	 'ELECTRICAL_PANEL', 'Entré du tableau TSC.01')
	,(@id_SE05000238, 'l3'	,'',	 'ELECTRICAL_PANEL', 'Entré du tableau TSC.01')
;

alter table measure_record ADD KEY _timestamp(timestamp)
select DATE_ADD(NOW(), interval 2 day)

select sensor_number from measure_record mr where `timestamp` >= DATE_ADD(NOW(), interval -1 day) group by sensor_number 


select * from measure_record mr where `timestamp` >= DATE_ADD(NOW(), interval -1 day) group by sensor_number 
GRANT SELECT, REFERENCES, INDEX, CREATE TEMPORARY TABLES, LOCK TABLES, EXECUTE, SHOW VIEW   ON clemap_data.* TO 'learning_agent'@'%';

select * from sensor 	
	join sensor_input on sensor_input.id_sensor = sensor.id
	where sensor.serial_number in
		(select sensor_number from measure_record mr where `timestamp` >= DATE_ADD(NOW(), interval -1 day) group by sensor_number)
	
select * from phase_measure_record pmr 

select * from measure_record where `timestamp` like '2022-05-09%'

alter table measure_record add 	 `sensor_name`			VARCHAR(32) NOT null after blob_name

select distinct measure_record.timestamp
	,sensor.serial_number as sensor
	,sensor.location
	,(select Description from sensor_input where sensor_input.id_sensor = sensor.id and sensor_input.phase='l1') as desc_phase1
	,phase1.p AS 'p_l1'
	,phase1.q AS 'q_l1'
	,phase1.s AS 's_l1'
	,(select Description from sensor_input where sensor_input.id_sensor = sensor.id and sensor_input.phase='l2') as desc_phase2
	,phase2.p AS 'p_l2'
	,phase2.q AS 'q_l2'
	,phase2.s AS 's_l2'
	,(select Description from sensor_input where sensor_input.id_sensor = sensor.id and sensor_input.phase='l3') as desc_phase3
	,phase3.p AS 'p_l3'	
	,phase3.q AS 'q_l3'	
	,phase3.s AS 's_l3'	
	from measure_record
	JOIN sensor on sensor.serial_number = measure_record.sensor_number 
	left join phase_measure_record as phase1 on phase1.id_measure_record = measure_record.id and phase1.phase = 'l1'
	left join phase_measure_record as phase2 on phase2.id_measure_record = measure_record.id and phase2.phase = 'l2'
	left join phase_measure_record as phase3 on phase3.id_measure_record = measure_record.id and phase3.phase = 'l3'
	where  feature_type='MN' and TimeStamp >= '2022-05-29 00:00:00' 
	and TimeStamp < '2022-06-30 00:00:00'
	-- and phase1.p < 89
	order by TimeStamp

	

select  count(*), SUM(p) as sum_p 
		from  clemap_data.measure_record
		JOIN clemap_data.phase_measure_record AS phase_mr ON phase_mr.id_measure_record = measure_record.id
		where measure_record.`timestamp`  = '2022-05-31 10:22:59.000'
		-- and blob_name='SE05000283/2022-05-31_08:51.raw'
	
	
select 	timestamp, count(*), SUM(p) as sum_p FROM
	(
	SELECT distinct measure_record.timestamp
	,sensor.serial_number AS sensor_number
	,sensor.location
	,sensor_input.device_category
	,sensor_input.description  AS device_name
	,phase_mr.phase, phase_mr.p, phase_mr.q, phase_mr.s
		FROM clemap_data.measure_record
		JOIN clemap_data.sensor on sensor.serial_number = measure_record.sensor_number
		JOIN clemap_data.phase_measure_record AS phase_mr ON phase_mr.id_measure_record = measure_record.id
		JOIN clemap_data.sensor_input ON  sensor_input.id_sensor = sensor.id AND sensor_input.phase=phase_mr.phase
		WHERE  measure_record.timeStamp >='2022-05-31 10:22:00' AND measure_record.timeStamp <'2022-05-31 11:00:00' AND 1 AND feature_type='MN'
			-- and serial_number = 'SE05000163'
			-- and phase_mr.phase = 'l1'
		ORDER BY measure_record.timestamp
	) as FOO
	group by  timestamp
	
SELECT distinct measure_record.timestamp, 2
,sensor.serial_number AS sensor_number
,sensor.location
,sensor_input.device_category
,sensor_input.description  AS device_name
,phase_mr.phase, phase_mr.p, phase_mr.q, phase_mr.s
	FROM clemap_data.measure_record
	JOIN clemap_data.sensor on sensor.serial_number = measure_record.sensor_number
	JOIN clemap_data.phase_measure_record AS phase_mr ON phase_mr.id_measure_record = measure_record.id
	JOIN clemap_data.sensor_input ON  sensor_input.id_sensor = sensor.id AND sensor_input.phase=phase_mr.phase
	WHERE  measure_record.timeStamp >='2022-05-31 18:00:00' AND measure_record.timeStamp <'2022-05-31 19:00:00' AND 1 AND feature_type='MN'  
	
select * from sensor s 
	
select SUM(Foo.p_l1) AS SUM_P1 
	, SUM(Foo.p_l2) AS SUM_P2 
	, SUM(Foo.p_l3) AS SUM_P3 
from 

(
	select distinct measure_record.timestamp
		,phase1.p AS 'p_l1'
		,phase2.p AS 'p_l2'
		,phase3.p AS 'p_l3'
	--	,SUM(1*phase1.p) as SUM_P1
	--	,SUM(phase2.p) as SUM_P2
	--	,SUM(phase3.p) as SUM_P3
		from measure_record
		left join phase_measure_record as phase1 on phase1.id_measure_record = measure_record.id and phase1.phase = 'l1'
		left join phase_measure_record as phase2 on phase2.id_measure_record = measure_record.id and phase2.phase = 'l2'
		left join phase_measure_record as phase3 on phase3.id_measure_record = measure_record.id and phase3.phase = 'l3'
		where  feature_type='MN' and TimeStamp >= '2022-04-29 00:00:00' and TimeStamp < '2022-04-30 00:00:00'  -- and phase1.p < 89
		order by phase1.p 
) as Foo

select measure_record.*
	,phase1.p AS 'p_l1'
	,phase1.q AS 'q_l1'
	,phase2.p AS 'p_l2'
	,phase2.q AS 'q_l2'
	,phase3.p AS 'p_l3'
	,phase3.q AS 'q_l3'
	from measure_record
	left join phase_measure_record as phase1 on phase1.id_measure_record = measure_record.id and phase1.phase = 'l1'
	left join phase_measure_record as phase2 on phase2.id_measure_record = measure_record.id and phase2.phase = 'l2'
	left join phase_measure_record as phase3 on phase3.id_measure_record = measure_record.id and phase3.phase = 'l3'
	where measure_record.id  =1681
  select * from event e2  where expiry_date IS null


SELECT measure_record.*
	,phase1.v AS 'v_l1'
	,phase1.i AS 'i_l1'
	,phase1.s AS 's_l1'
	,phase1.p AS 'p_l1'
	,phase1.q AS 'q_l1'
	,phase1.pf AS 'pf_l1'
	,phase1.phi AS 'phi_l1'
	,phase2.v AS 'v_l2'
	,phase2.i AS 'i_l2'
	,phase2.s AS 's_l2'
	,phase2.p AS 'p_l2'
	,phase2.q AS 'q_l2'
	,phase2.pf AS 'pf_l2'
	,phase2.phi AS 'phi_l2'
	,phase3.v AS 'v_l3'
	,phase3.i AS 'i_l3'
	,phase3.s AS 's_l3'
	,phase3.p AS 'p_l3'
	,phase3.q AS 'q_l3'
	,phase3.pf AS 'pf_l3'
	,phase3.phi AS 'phi_l3'
	,phase1.avg_energy AS 'avg_energy_l1'
	,phase2.avg_energy AS 'avg_energy_l2'
	,phase3.avg_energy AS 'avg_energy_l3'
	FROM measure_record
	LEFT JOIN phase_measure_record AS phase1 ON phase1.id_measure_record = measure_record.id AND phase1.phase = 'l1'
	LEFT JOIN phase_measure_record AS phase2 ON phase2.id_measure_record = measure_record.id AND phase2.phase = 'l2'
	LEFT JOIN phase_measure_record AS phase3 ON phase3.id_measure_record = measure_record.id AND phase3.phase = 'l3'
	WHERE measure_record.id  =1681
;	

SELECT distinct measure_record.timestamp
,TIMESTAMPADD(MINUTE,
    TIMESTAMPDIFF(MINUTE,DATE(timestamp),timestamp),
    DATE(timestamp)) AS timestamp3
,sensor.serial_number AS sensor_number
,sensor.location
,sensor_input.device_category
,sensor_input.description  AS device_name
,phase_mr.phase, phase_mr.p, phase_mr.q, phase_mr.s
,feature_type
	FROM clemap_data.measure_record
	JOIN clemap_data.sensor on sensor.serial_number = measure_record.sensor_number
	JOIN clemap_data.phase_measure_record AS phase_mr ON phase_mr.id_measure_record = measure_record.id
	JOIN clemap_data.sensor_input ON  sensor_input.id_sensor = sensor.id AND sensor_input.phase=phase_mr.phase
	WHERE  measure_record.timeStamp >='2022-05-31 10:15:58' AND measure_record.timeStamp <'2022-05-31 11:15:58' 
		 AND feature_type='MN'
		AND NOT sensor_input.is_disabled
	ORDER BY measure_record.timestamp
;

use clemap_data_light

SELECT * FROM tmp_import_phase_measure_record

SELECT * FROM test_measure_record


                    
                    
UPDATE tmp_import_phase_measure_record tmp_import SET id_measure_record = (SELECT mr.id FROM test_measure_record mr WHERE
                    mr.sensor_number = tmp_import.sensor_number 
                    AND mr.blob_name = tmp_import.blob_name
                    AND  mr.timestamp = tmp_import.timestamp
                    AND mr.feature_type = tmp_import.feature_type  LIMIT 0,1 )

SELECT id_measure_record, 'l1', l1_voltage, l1_intensity, l1_power_s, l1_power_p, l1_power_q, l1_power_factor, l1_power_phi, l1_avg_energy FROM tmp_import_phase_measure_record
        UNION SELECT id_measure_record, 'l2', l2_voltage, l2_intensity, l2_power_s, l2_power_p, l2_power_q, l2_power_factor, l2_power_phi, l2_avg_energy  FROM tmp_import_phase_measure_record
		UNION SELECT id_measure_record, 'l3', l3_voltage, l3_intensity, l3_power_s, l3_power_p, l3_power_q, l3_power_factor, l3_power_phi, l3_avg_energy FROM tmp_import_phase_measure_record
                   


select log_import_clemap.*
	, sensor_number, blob_name, nb_records
	, TIMESTAMPDIFF(HOUR,creation_date, last_import_date) as diff
	from log_import_clemap where nb_imports >= 10 and nb_records < nb_records_required
		AND  TIMESTAMPDIFF(HOUR,creation_date, last_import_date)  >= 3

		SELECT * FROM measure_record
			JOIN phase_measure_record pmr ON pmr.id_measure_record = measure_record.id
			WHERE blob_name = '2023-02-21_09:50.raw'

		SELECT * FROM test_measure_record
			LEFT JOIN test_phase_measure_record pmr ON pmr.id_measure_record = test_measure_record.id
			WHERE blob_name = '2023-02-21_09:50.raw'
		
		SELECT * FROM tmp_import_phase_measure_record WHERE  blob_name = '2023-02-21_09:50.raw'
		
		
		
		UPDATE tmp_import_phase_measure_record SET id_measure_record = (SELECT mr.id FROM test_measure_record mr WHERE
                    mr.timestamp = tmp_import_phase_measure_record.timestamp AND mr.feature_type = tmp_import_phase_measure_record.feature_type  )

CREATE TABLE test_measure_record LIKE measure_record

CREATE TABLE test_phase_measure_record LIKE phase_measure_record
SELECT * FROM test_phase_measure_record

DROP TABLE tmp_import_phase_measure_record

TRUNCATE tmp_import_phase_measure_record

use clemap_data_light

SELECT blob_name, sensor_number , Count(*) FROM measure_record mr where blob_name LIKE 'SIG_%' 
	-- and blob_name = 'SIG_CourbeCharge_20221001_20221031_d101653d-13c3-40ca-97bb-7a74e313eb72.csv' 
	GROUP BY blob_name , sensor_number 
	
	
SELECT blob_name,  Count(*) FROM measure_record mr where blob_name LIKE 'SIG_%' 
	-- and blob_name = 'SIG_CourbeCharge_20221001_20221031_d101653d-13c3-40ca-97bb-7a74e313eb72.csv' 
	GROUP BY blob_name  
	
SEL
	
	SELECT id_measure_record , phase , count(*) AS nb FROM phase_measure_record GROUP BY id_measure_record , phase HAVING nb > 1



§
 CREATE TABLE phase_measure_record(
	`id` 					INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	`id_measure_record`		INT(11) UNSIGNED NOT NULL,
	`phase`					ENUM('l1','l2','l3') COMMENT 'phase : 1 or 2 or 3',
	`voltage`				DECIMAL(10,5) NULL COMMENT 'voltage',
	`intensity`				DECIMAL(10,5) NULL COMMENT 'current intensity ',
	`power_s`				DECIMAL(10,5) NULL COMMENT 'apparent power : s^2 = p^2 + q^2',
	`power_p`				DECIMAL(10,5) NULL COMMENT 'real(or active) power',
	`power_q`				DECIMAL(10,5) NULL COMMENT 'reactive power',
	`power_factor`			DECIMAL(10,5) NULL COMMENT 'power factor',
	`power_phi`				DECIMAL(10,5) NULL COMMENT 'power phase angle',
	`avg_energy`			DECIMAL(10,5) NULL COMMENT '',
	PRIMARY KEY (`id`),
	CONSTRAINT `fk_pmr_measure_record` FOREIGN KEY (`id_measure_record`) REFERENCES `measure_record` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8


		
		
		
		
select * from clemap_data.sensor 
select * from clemap_data_light.sensor 
;

use clemap_data_light
;
select * from sensor s 
	JOIN sensor_input si ON si.id_sensor = s.id
	where not si.is_disabled
;

-- nb records per day and sensor 60 * 3 * 24 * IF(is_light, 1, 7)
-- EXTRACTION for Subanki Suntharanaathan
select  mr.`timestamp` , mr.feature_type , mr.blob_name , s.*, si.*
	-- , mr.*
	,pmr.voltage	AS voltage
	,pmr.intensity 	AS current
	,pmr.power_s	AS power -- apparent power
	,pmr.power_p	AS active_power
	,pmr.power_q	AS reactive_power
	,pmr.power_factor AS power_factor
	,pmr.power_phi	AS power_phase_angle
	,pmr.avg_energy
	from measure_record mr
	JOIN phase_measure_record pmr on pmr.id_measure_record  = mr.id
	LEFT JOIN sensor s on s.serial_number = mr.sensor_number
	JOIN sensor_input si ON si.id_sensor = s.id and si.phase = pmr.phase 
	where mr.`timestamp` >= '2023-02-18' and `timestamp` < '2023-02-23'
		and not si.is_disabled 
		-- and s.serial_number  = 'SE05000159'
		-- and pmr.phase = 'l1'
		order by timestamp, serial_number, pmr.phase
		
;
-- 135564


SELECT  mr.sensor_number , count(*) , SUM(pmr.voltage), SUM(pmr.intensity), SUM(pmr.power_s), SUM(pmr.power_p), SUM(pmr.power_q), SUM(pmr.power_factor), SUM(pmr.power_phi), SUM(pmr.avg_energy)
	FROM measure_record mr	 
	JOIN phase_measure_record pmr on pmr.id_measure_record  = mr.id
	LEFT JOIN sensor s on s.serial_number = mr.sensor_number 
	JOIN sensor_input si ON si.id_sensor = s.id and si.phase = pmr.phase 
	where mr.`timestamp` >= '2023-02-18' and `timestamp` < '2023-02-23'
		-- and `timestamp` < '2023-02-21'
		and not si.is_disabled 
		GROUP BY mr.sensor_number 
		-- and s.serial_number  = 'SE05000159'
		-- and pmr.phase = 'l1'
;
select  mr.sensor_number ,pmr.phase,  count(*) from measure_record mr	 
	JOIN phase_measure_record pmr on pmr.id_measure_record  = mr.id
	LEFT JOIN sensor s on s.serial_number = mr.sensor_number 
	JOIN sensor_input si ON si.id_sensor = s.id and si.phase = pmr.phase 
	where mr.`timestamp` >= '2023-02-20' -- AND s.serial_number IS NULL
		and `timestamp` < '2023-02-21'
		and not si.is_disabled 
		-- and s.serial_number  = 'SE05000159'
		 and pmr.phase = 'l1'
		GROUP BY mr.sensor_number ,pmr.phase
;

-- s3cmd get s3://data-smart-grid/


SELECT * FROM log_import_clemap
;
TRUNCATE log_import_clemap
;
SELECT * FROM measure_record mr2  WHERE blob_name ='2023-02-22_11:08.raw' and sensor_number = 'SE05000318'
select sensor_number , blob_name , count(*) AS nb
	from measure_record mr	
	JOIN phase_measure_record pmr on pmr.id_measure_record  = mr.id
	where mr.`timestamp` >= '2023-02-01' -- AND s.serial_number IS NULL
		GROUP BY blob_name, sensor_number
		HAVING nb < 180
;
select min(id), max(id), blob_name , sensor_number , `timestamp` , count(*) AS CC from measure_record mr
	where `timestamp` >= '2023-02-20'
	-- where blob_name = 'SIG_CourbeCharge_20221001_20221031_d101653d-13c3-40ca-97bb-7a74e313eb72.csv' 
	group by blob_name , sensor_number , feature_type , `timestamp` HAVING CC > 1
;