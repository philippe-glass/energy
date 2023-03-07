

CREATE TEMPORARY TABLE TmpCleanHisto 
AS SELECT h.id FROM history h WHERE h.date > '2022-10-18 17:30:52' AND h.id_session = '20221018_173052_3728' AND NOT EXISTS (SELECT 1 FROM Event e WHERE e.id_histo = h.id)
;
DELETE link_h_e FROM link_history_active_event link_h_e WHERE id_history IN (SELECT id FROM TmpCleanHisto)
;
DELETE h FROM history h WHERE id IN (SELECT id FROM TmpCleanHisto)


DELETE h FROM history h WHERE h.date > '2022-10-18 16:08:53' AND h.id_session = '20221018_160853_2355' AND NOT EXISTS (SELECT 1 FROM Event e WHERE e.id_histo = h.id)

select * from history h  order by date

select history.*  from history where id = 1594411

delete link_history_active_event from link_history_active_event where id_history = 1594411

select * from link_history_active_event where id_history = 1594411

select * from link_history_active_event where id_last = 63837333

update link_history_active_event set id_last = null where id_last = 63837333

delete h.*  from history h where id = 1594411

-- TODO : delete history where is refresh and date > date current history

select * from transition_matrix tm order by id desc

select * from time_window tw 

select * from transition_matrix tm where scenario = 'MeyrinSimulator' and variable_name = 'requested' and id_time_window  = 9

select * from context c 

delete transition_matrix_cell_iteration from transition_matrix_cell_iteration where id_transition_matrix IN
(select id from transition_matrix where location like 'localhost:10001')
;
delete transition_matrix_cell from transition_matrix_cell where id_transition_matrix IN
(select id from transition_matrix where location like 'localhost:10001')
;
update prediction set id_correction = null where id_correction in
	(select id from log_self_correction where id_transition_matrix IN
		(select id from transition_matrix where location like 'localhost:10001' ))
;
delete log_self_correction from log_self_correction where id_transition_matrix IN
		(select id from transition_matrix where location like 'localhost:10001')
;
delete transition_matrix_iteration from transition_matrix_iteration where id_transition_matrix IN
(select id from transition_matrix where location like 'localhost:10001')
;

delete transition_matrix from transition_matrix where  location like 'localhost:10001'
;



select * from transition_matrix tm where location = 'localhost:10001'

select * from transition_matrix tm where location = '192.168.1.79:10001'

update transition_matrix set location = 'localhost:10001' where location='192.168.1.79:10001'

select * from context c 



select * from event e where location='localhost:10001'
left join history h ON h.id = e.id_histo 
where e.agent like '%N3%'

select h.* 
,(select COunt(*) from event where event.id_histo = h.id) as nbEvebts
from history h
order by h.date

select * from history h where id = 1627684
select * from event where id_histo = 1627684

select * from event where date = '2022-08-30 07:09:12.000'

SELECT history.id, history.id_last, history.id_next, history.date, history.total_produced, history.total_requested, history.total_consumed 
,history.total_available, history.total_missing, history.total_provided, history.min_request_missing, history.total_margin, history.location, history.distance, history.max_warning_duration,history.max_warning_consumer
,IFNULL(hNext.date,NOW()) AS date_next
,IFNULL(TmpUnReqByHisto.nb_missing_request,0) AS nb_missing_request
,IFNULL(TmpUnReqByHisto.list_missing_requests,'') AS list_missing_requests
,IF(IFNULL(TmpUnReqByHisto.sum_warning_missing1,0) <= history.total_available
		,IFNULL(TmpUnReqByHisto.sum_warning_missing1,0)
		,COMPUTE_WARNING_SUM4(history.id, history.total_available, IFNULL(TmpUnReqByHisto.sum_warning_missing1,0))
	) AS sum_warning_missing
 FROM history
 LEFT JOIN history AS hNext ON hNext.id = history.id_next
 LEFT JOIN (SELECT
	     UnReq.id_histo
		,Count(*) 	AS nb_missing_request
		,SUM(UnReq.warning_missing) AS sum_warning_missing1
		,GROUP_CONCAT(UnReq.Label3  ORDER BY UnReq.warning_duration DESC, UnReq.power SEPARATOR ', ') AS list_missing_requests
	FROM (
		SELECT
			 histo_req.id_history AS id_histo
			,histo_req.agent AS consumer
			,histo_req.power
			,histo_req.missing
			,IF(warning_duration > 0 , histo_req.missing, 0) AS warning_missing
			,CONCAT(histo_req.agent, '(',  histo_req.power, ')'  ) AS Label
			,CONCAT(histo_req.agent, '#',  histo_req.missing, '#', IF(histo_req.has_warning_req,1,0) , '#' ,histo_req.warning_duration) AS Label3
			,histo_req.warning_duration
		FROM link_history_active_event AS histo_req
		WHERE histo_req.location =  'localhost:10001'
			AND is_request > 0
			AND missing > 0
		) AS UnReq
	GROUP BY UnReq.id_histo
) AS TmpUnReqByHisto ON TmpUnReqByHisto.id_histo = history.id
 WHERE history.id_session =  '20221128_212241_9697' AND history.location ='localhost:10001'
 ORDER BY history.date, history.ID
;

select * from single_offer so where is_complementary
;
select * from event where is_complementary
;
select * from context c
;
select * from transition_matrix tm order by id desc
;
SELECT * FROM transition_matrix_cell_iteration
	join transition_matrix_iteration tmi on tmi.id = transition_matrix_cell_iteration.id_transition_matrix_iteration 
	join transition_matrix tm  on tm.id = transition_matrix_cell_iteration.id_transition_matrix 
	where tm.scenario = 'HomeSimulator'


select distinct comment from event where type = 'CONTRACT_STOP'

select * from event e where id_histo is null

update transition_matrix_cell_iteration set creation_time = '1970-01-01'

 select * from state_history where id_session like '2022%' and scenario = 'HomeSimulator'
update state_history set id_session  = concat(id_session, '_HmeS') where id_session like '2022%' and scenario = 'HomeSimulator'


select * from event where object_type = 'CONTRACT' and location = 'localhost:10001' and distance > 0

select * from link_event_agent lea where id_event = 1436041

1436041

select * from 

select * from simulator_log sl 


select * from clemap_data_light.phase_measure_record pmr 

select * from event


DROP TEMPORARY TABLE IF EXISTS TmpCleanHisto
§CREATE TEMPORARY TABLE TmpCleanHisto AS SELECT h.id FROM history h WHERE h.date > '2023-02-17 15:51:32' AND h.id_session = '20230217_152354_6323' AND NOT EXISTS (SELECT 1 FROM Event e WHERE e.id_histo = h.id)
§
UPDATE history SET id_next = NULL WHERE id_next IN (SELECT id FROM TmpCleanHisto)
§
UPDATE link_history_active_event SET id_last = NULL WHERE id_last IN (SELECT ID FROM link_history_active_event link_h_e WHERE id_history IN (SELECT id FROM TmpCleanHisto))
§
DELETE link_h_e FROM link_history_active_event link_h_e WHERE id_history IN (SELECT id FROM TmpCleanHisto)
§DELETE h FROM history h WHERE id IN (SELECT id FROM TmpCleanHisto)


select * from link_event_agent lea where power_max <= power

select * from link_history_active_event lhae where is_contract  

select * from history order by id desc

select * from clemap_data.sensor 
select * from clemap_data_light.sensor 


select * from state_history where date = '2022-07-01 12:29:34' and variable_name = 'produced'

select location, scenario, count(*) from state_history where id_session = '' group by location, scenario



select * from state_history sh order by id desc 
select * from state_history sh where date >= '2022-07-04 08:00' and variable_name = 'requested' order by ID desc

select * from state_history sh where observation_update = '2022-07-04 10:24:04.000'

select * from time_window tw where id = 6

select * from transition_matrix tm where id = 178668

select * from transition_matrix_cell_iteration tmci order by creation_time  DESC

2022-07-21 08:32:13.000
select * from state_history h where variable_name = 'produced' order by id desc 

select hour(date)  as hour, state_name , count(*) as nb
	from state_history h where scenario = 'MeyrinSimulator' and location='192.168.1.79:10001' and variable_name = 'produced' 
	group by state_name ,hour
	order by hour, nb DESC 

SELECT * FROM device_statistic where device_category like 'EX%ENG%'

select * from event e where object_type = 'PRODUCTION' and agent= 'Prod_N1_2' -- SIG

select * from clemap_data_light.sensor where id = 11

select * from clemap_data_light.sensor_input
	join clemap_data_light.sensor s on s.id = sensor_input.id_sensor
where description = 'Production PV brute'


select * from state_history sh order by id desc 



1151653


SELECT 0.5* SUM(ABS(pi2.proba - IFNULL(TmpStateDistrib1.ratio,0)))
			    	from prediction p 
			    	join prediction_item pi2 on pi2.id_prediction = p.id
			    	LEFT JOIN TmpStateDistrib1 ON
			    				 TmpStateDistrib1.state_date =  Date(p.target_date)
					    	 and TmpStateDistrib1.hour = HOUR(p.target_date)
					    	 AND TmpStateDistrib1.variable_name = p.variable_name
					    	 AND TmpStateDistrib1.state_idx = pi2.state_idx
			    	 WHERE p.id = 1151653
			    	 
SELECT SUM(TmpStateDistrib1.ratio)
			    	from prediction p 
			    	LEFT JOIN TmpStateDistrib1 ON
			    				 TmpStateDistrib1.state_date =  Date(p.target_date)
					    	 and TmpStateDistrib1.hour = HOUR(p.target_date)
					    	 AND TmpStateDistrib1.variable_name = p.variable_name
			    	 WHERE p.id = 1151653
			    	 
			    	 
			    	 
			    	 
select COMPUTE_VECTOR_DIFFERENTIAL(1151653)






select * from TmpPredictionStatisticLine

	drop temporary table if EXISTS TmpPred


select * from prediction_item
	
select * FROM(

explain 		select TmpPredictionStatistic.id as id_statistic
			 ,TmpPredictionStatistic.Date, TmpPredictionStatistic.time_slot, TmpPredictionStatistic.variable_name
			 ,TmpPredictionStatistic.horizon, TmpPredictionStatistic.use_corrections
				, p.id as id_prediction
				,pi2.state_idx
				,pi2.proba
				FROM TmpPredictionStatistic
				 JOIN prediction p on p.target_date >= TmpPredictionStatistic.min_target_date --  Date(p.target_date) = TmpPredictionStatistic.date
					and  p.target_date < TmpPredictionStatistic.max_target_date
					AND p.variable_name  = TmpPredictionStatistic.variable_name
					AND p.horizon_minutes  = TmpPredictionStatistic.horizon
					AND p.use_corrections  = TmpPredictionStatistic.use_corrections
				 left JOIN prediction_item pi2 on pi2.id_prediction =  p.id
				 
				 group by (p.target_day)
				 
			) as FOO	 
				 
				 GROUP BY FOO.id_statistic 		
					
					
				) as TmpPred				
		JOIN prediction_item pi2 on pi2.id_prediction =  TmpPred.id_prediction
		
		alter table prediction add key _foo(DATE(target_date))
alter table prediction_item add key state_idx(state_idx)


select * from prediction p order by id desc

explain select target_day  as Date, target_hour as hour, p.variable_name , p.horizon_minutes as horizon , p.use_corrections
	, pi2.state_idx,avg(pi2.proba) as proba
	from prediction p 
	join prediction_item pi2 on pi2.id_prediction =p.id
	where target_day >= '2022-09-01'  -- and p.variable_name ='produced'
	group by p.target_day, p.target_hour, p.variable_name , p.horizon_minutes , p.use_corrections
	 , pi2.state_idx 
	 
	 
	 
	explain select target_day  as Date, target_hour as hour, p.variable_name , p.horizon_minutes as horizon , p.use_corrections
	, pi2.state_idx 
	,avg(pi2.proba) as proba
	from prediction p 
	 join prediction_item pi2 on pi2.id_prediction =p.id
	where target_day >= '2022-09-01'  -- and p.variable_name ='produced'
	group by p.target_day, p.target_hour, p.variable_name , p.horizon_minutes , p.use_corrections, pi2.state_idx

	 
	
	select * from prediction p order by id desc
	
	
	
	
	
	
	
	
	
	
	
	
	
select Date(target_date) as Date, HOUR(target_date) as hour, p.variable_name , p.horizon_minutes as horizon , p.use_corrections
	, pi2.state_idx,avg(pi2.proba) as proba
	from prediction p 
	join prediction_item pi2 on pi2.id_prediction =p.id
	where target_date >= '2022-09-01'  -- and p.variable_name ='produced'
	group by Date(target_date)  , HOUR(target_date)  , p.horizon_minutes , p.use_corrections
	 , pi2.state_idx 
	 
	 
	 
	 
	 
	 
select p.*
	 ,(select pi_0.proba from prediction_item pi_0 where pi_0.id_prediction = p.id  and pi_0.state_idx=0) as proba_0
	from prediction p 
	where p.target_date >= '2022-09-01'  -- and p.variable_name ='produced'
	group by DATE(p.target_date), HOUR(target_date), p.variable_name , p.horizon_minutes , p.use_corrections
	
	
alter table	prediction_item add unique key (id_prediction, state_idx)
, pi2.state_idx 

	
alter table 
	
select * from 
	
	
	, HOUR(p.target_date), pi2.state_idx 
		
		
select * from prediction p where target_date >= '2022-09-01'  group by DATE(p.target_date)



ALTER TABLE TmpPredictionStatisticLine ADD KEY(`id_statistic`)					

SELECT
			TmpPred.id_statistic,
			TmpPred.Date, TmpPred.time_slot, TmpPred.variable_name,
			TmpPred.horizon, TmpPred.use_corrections,
			pi2.state_idx ,
			pi2.state_name ,
			Count(*) as Nb,
			avg(pi2.proba) as proba
		FROM TmpPred
		JOIN prediction_item pi2 on pi2.id_prediction =  TmpPred.id_prediction
		WHERE 1
		GROUP BY TmpPred.id_statistic, pi2.state_idx 
					


		
CALL SP_COMPUTE_STATE_DISTRIBUTION('192.168.1.79:10001','MeyrinSimulator','2022-09-01', NULL, NULL)


CALL SP_COMPUTE_PREDICTION_STATISTICS('192.168.1.79:10001','MeyrinSimulator','2022-09-01','2022-09-07',NULL,NULL,NULL,NULL) 					
;


UPDATE TmpPredictionStatistic
		JOIN TmpStateDistribution AS TmpSD ON TmpSD.date = TmpPredictionStatistic.date
				AND TmpSD.hour = TmpPredictionStatistic.time_slot
				AND TmpSD.variable_name = TmpPredictionStatistic.variable_name
		SET TmpPredictionStatistic.gini_index = TmpSD.gini_index
		,TmpPredictionStatistic.shannon_entropie = TmpSD.shannon_entropie
	;

UPDATE prediction p SET p.vector_differential = COMPUTE_VECTOR_DIFFERENTIAL(p.id) 
		WHERE p.target_day >= '2022-09-01'
			-- AND (p.target_day <= p_date_max OR p_date_max IS NULL)
		    -- AND (p.target_hour  >= p_min_hour OR p_min_hour IS NULL)
		   --  AND (p.target_hour <= 0+p_max_hour OR p_max_hour IS NULL)
		   --  AND (p.use_corrections = p_use_corrections OR p_use_corrections IS NULL)
		   --  AND (p.variable_name = p_variable_name OR p_variable_name IS NULL)
			AND p.target_date <= DATE_ADD(NOW(), interval -1 HOUR)
		    AND p.vector_differential IS NULL
		    AND NOT id_target_state_histo IS NULL
		    AND 1

		    
		    
select * FROM prediction p 
		WHERE p.target_date >= '2022-09-01'
			-- AND (p.target_day <= p_date_max OR p_date_max IS NULL)
		    -- AND (p.target_hour  >= p_min_hour OR p_min_hour IS NULL)
		   --  AND (p.target_hour <= 0+p_max_hour OR p_max_hour IS NULL)
		   --  AND (p.use_corrections = p_use_corrections OR p_use_corrections IS NULL)
		   --  AND (p.variable_name = p_variable_name OR p_variable_name IS NULL)
		   AND p.target_date <= DATE_ADD(NOW(), interval -1 HOUR)
		    AND p.vector_differential IS NULL
		    AND NOT id_target_state_histo IS NULL
		    AND 1

		    

SELECT target_day  as Date, target_hour as hour, p.variable_name , p.horizon_minutes as horizon , p.use_corrections
			, pi2.state_idx, pi2.state_name, avg(pi2.proba) as proba
			FROM prediction p 
			JOIN prediction_item pi2 on pi2.id_prediction =p.id
			WHERE p.target_day >='2022-09-01'
			    AND 1
			GROUP BY p.target_day, p.target_hour, p.variable_name , p.horizon_minutes , p.use_corrections, pi2.state_idx



			
			
			
			
			
			
			
SELECT location
		,scenario
		,variable_name
		-- ,Date(initial_date) as date
		-- ,hour(initial_date) as time_slot
		,target_day AS date
		,target_hour AS time_slot
		,DATE_ADD(target_day, interval target_hour hour ) AS min_target_date
		,DATE_ADD(target_day, interval 1+target_hour hour ) AS max_target_date
		-- ,MAX(target_date) AS max_target_date
		,horizon_minutes AS horizon
		,use_corrections
		,id_initial_time_window
		,COUNT(*) AS nb_total
		,SUM(is_ok1) AS nb_ok1
		,SUM(is_ok2) AS nb_ok2
		,SUM(is_ok1) / SUM(1) AS rate_ok1
		,SUM(is_ok2) / SUM(1) AS rate_ok2
		,SUM(has_correction) AS corrections_number
		,MIN(creation_date) AS creation_date
		,AVG(proba_random) AS proba1
		,AVG(proba_likely) AS proba2
		,0 AS gini_index
		,0 AS shannon_entropie
		,AVG(vector_differential) AS vector_differential
		,MAX(has_states_distrib) AS has_states_distrib
		,GROUP_CONCAT(distinct id_correction) AS list_id_correction
		FROM (
			 SELECT p.*
			    ,sh.`date`
			    ,sh.state_idx
			    ,sh.state_name
			    ,(p.random_state_idx = sh.state_idx) AS is_ok1
			    ,(p.likely_state_idx = sh.state_idx) AS is_ok2
			    ,p.random_state_proba AS proba_random
			    ,p.likely_state_proba AS proba_likely
			    ,ABS(CONVERT(p.random_state_idx , signed)   -  CONVERT(sh.state_idx, signed) ) as delta_abs
			    ,(CONVERT(p.random_state_idx , signed)   -  CONVERT(sh.state_idx, signed) ) as delta
			    ,IF(p.id_correction IS NULL, 0, 1) AS has_correction
			   --  ,COMPUTE_VECTOR_DIFFERENTIAL(p.id) AS vector_differential
			    -- ,TmpStateDistrib1.vector_differential
			    -- ,0 AS vector_differential
			    ,(SELECT IFNULL(SUM(TmpStateDistrib1.ratio),0)
					FROM TmpStateDistrib1 WHERE
						TmpStateDistrib1.date =  Date(p.target_date)
				    AND TmpStateDistrib1.hour = HOUR(p.target_date)
				    AND TmpStateDistrib1.variable_name = p.variable_name) as has_states_distrib
			 FROM prediction p
			 JOIN state_history AS sh ON sh.id = p.id_target_state_histo
			 WHERE p.target_day >='2022-09-01'
			  --	 AND (p.target_day <= p_date_max OR p_date_max IS NULL)
			  --  AND (p.target_hour >= p_min_hour OR p_min_hour IS NULL)
			 --   AND (p.target_hour <= 0+p_max_hour OR p_max_hour IS NULL)
			 --   AND (p.use_corrections = p_use_corrections OR p_use_corrections IS NULL)
			  --  AND (p.variable_name = p_variable_name OR p_variable_name IS NULL)
	    ) AS Result
	    GROUP by target_day, target_hour, variable_name , horizon, use_corrections
	    HAVING nb_total >= 30
	    ORDER BY rate_OK2 desc
			
			
			
			
			
			
					
CALL SP_COMPUTE_PREDICTION_STATISTICS('192.168.1.79:10001','MeyrinSimulator','2022-09-01 11:00:00',NULL,NULL,NULL)
;
select * from TmpPredictionStatisticLine

SELECT
			TmpPred.id_statistic,
			TmpPred.Date, TmpPred.time_slot, TmpPred.variable_name,
			TmpPred.horizon, TmpPred.use_corrections,
			pi2.state_idx ,
			pi2.state_name ,
			Count(*) as Nb,
			avg(pi2.proba) as proba
		FROM TmpPred
		JOIN prediction_item pi2 on pi2.id_prediction =  TmpPred.id_prediction
		WHERE 1
		GROUP BY TmpPred.id_statistic, pi2.state_idx 
		
		
		
		
alter table prediction drop key _variable_name;
alter table prediction add KEY _target_date_varname(target_date, variable_name);


CREATE TEMPORARY TABLE TmpPredictionStatisticLine (
		id_statistic INT(11) UNSIGNED NOT NULL
		-- (CONSTRAINT `fk_id_statistic1` FOREIGN KEY (`id_statistic`) REFERENCES `TmpPredictionStatistic` (`id`)
		)
		AS
explain SELECT
			TmpPredictionStatistic.*
		FROM TmpPredictionStatistic
		join prediction p  on p.target_date >= TmpPredictionStatistic.min_target_date
			-- and  p.target_date < DATE_ADD(TmpPredictionStatistic.min_target_date, INTERVAL 1 HOUR)
			and p.target_date < TmpPredictionStatistic.max_target_date
			-- prediction p on Date(p.target_date) = TmpPredictionStatistic.date		
			-- AND Hour(p.target_date) = TmpPredictionStatistic.time_slot
			AND p.variable_name  = TmpPredictionStatistic.variable_name
			AND p.horizon_minutes  = TmpPredictionStatistic.horizon
			AND p.use_corrections  = TmpPredictionStatistic.use_corrections
		WHERE 1


		
		
		
		
		drop temporary table TmpPredictionStatisticLine
		;
			CREATE TEMPORARY TABLE TmpPredictionStatisticLine (
		id_statistic INT(11) UNSIGNED NOT NULL
		-- (CONSTRAINT `fk_id_statistic1` FOREIGN KEY (`id_statistic`) REFERENCES `TmpPredictionStatistic` (`id`)
		)
		AS
		
explain 

drop temporary table if EXISTS TmpPred
;
create temporary table TmpPred as
	select TmpPredictionStatistic.id as id_statistic, p.id as id_prediction
			FROM TmpPredictionStatistic
			JOIN prediction p on p.target_date >= TmpPredictionStatistic.min_target_date --  Date(p.target_date) = TmpPredictionStatistic.date
				and  p.target_date < TmpPredictionStatistic.max_target_date
				AND p.variable_name  = TmpPredictionStatistic.variable_name
				AND p.horizon_minutes  = TmpPredictionStatistic.horizon
				AND p.use_corrections  = TmpPredictionStatistic.use_corrections
;
select TmpPred.id_statistic , TmpPred.id_prediction , pi2.state_idx 
	,pi2.state_idx
	,pi2.state_name
	,Count(*) as Nb
	,avg(pi2.proba) as proba
	from TmpPred
		JOIN prediction_item pi2 on pi2.id_prediction =  TmpPred.id_prediction
		WHERE 1
	GROUP BY TmpPred.id_statistic, pi2.state_idx 

drop temporary table if EXISTS TmpFOO

create temporary table TmpFOO AS
	select TmpPred.id_statistic , TmpPred.id_prediction -- , pi2.state_idx 
	from 
		(select TmpPredictionStatistic.id as id_statistic, p.id as id_prediction
			FROM TmpPredictionStatistic
			JOIN prediction p on p.target_date >= TmpPredictionStatistic.min_target_date --  Date(p.target_date) = TmpPredictionStatistic.date
				and  p.target_date < TmpPredictionStatistic.max_target_date
				AND p.variable_name  = TmpPredictionStatistic.variable_name
				AND p.horizon_minutes  = TmpPredictionStatistic.horizon
				AND p.use_corrections  = TmpPredictionStatistic.use_corrections) as TmpPred
		JOIN prediction_item pi2 on pi2.id_prediction =  TmpPred.id_prediction
		WHERE 1
		
select id_statistic, state_idx from TmpFOO group by id_statistic,state_idx



		GROUP BY TmpPred.id_statistic, pi2.state_idx 
		
		
		
		
		
		
		
		
		
select target_date, DATE(target_date), HOUR(target_date)
	,DATE_ADD(DATE(target_date), interval HOUR(target_date) hour )as min_target_date
	,DATE_ADD(DATE(target_date), interval 1+HOUR(target_date) hour )as max_target_date
	from prediction
;
explain SELECT
			TmpPredictionStatistic.*
		FROM TmpPredictionStatistic
			-- join prediction p  on p.target_date >= TmpPredictionStatistic.min_target_date
			-- and p.target_date <= TmpPredictionStatistic.max_target_date
		join	prediction p on Date(p.target_date) = TmpPredictionStatistic.date		
		  AND Hour(p.target_date) = TmpPredictionStatistic.time_slot
			AND p.variable_name  = TmpPredictionStatistic.variable_name
			AND p.horizon_minutes  = TmpPredictionStatistic.horizon
			AND p.use_corrections  = TmpPredictionStatistic.use_corrections
			
			
			
		JOIN prediction_item pi2 on pi2.id_prediction =  p.id
		WHERE 1


;
select * from TmpPredictionStatistic where horizon = 10 and use_corrections = 1
;
alter table TmpPredictionStatisticLine ADD KEY(`id_statistic`)
;
alter table TmpPredictionStatisticLine ADD CONSTRAINT `fk_id_statistic1` FOREIGN KEY (`id_statistic`) REFERENCES `TmpPredictionStatistic` (`id`)
;
select * from TmpPredictionStatisticLine
;
select id_statistic, ROUND(SUM(proba),5) from TmpPredictionStatisticLine group by id_statistic
;
select  TmpPredictionStatistic.ID,
	pi2.state_idx ,
	pi2.state_name ,
	Count(*) as Nb,
	avg(pi2.proba) as proba
	from TmpPredictionStatistic
	join prediction p on Date(p.target_date) = TmpPredictionStatistic.date
	join prediction_item pi2 on pi2.id_prediction =  p.id
		and Hour(p.target_date) = TmpPredictionStatistic.time_slot
		and p.variable_name  = TmpPredictionStatistic.variable_name
		and p.horizon_minutes  = TmpPredictionStatistic.horizon
		and p.use_corrections  = TmpPredictionStatistic.use_corrections
	where TmpPredictionStatistic.horizon = 10 and TmpPredictionStatistic.use_corrections = 1 
	group by TmpPredictionStatistic.ID, pi2.state_idx 


	(HOUR(p.target_date) >= p_min_hour OR p_min_hour IS NULL)
			    AND (HOUR(p.target_date) <= 0+p_max_hour OR p_max_hour IS NULL)
			    AND (p.variable_name = p_variable_name OR p_variable_name IS NULL)


update prediction  set vector_differential = NULL

-- TODO : 
CALL SP_COMPUTE_PREDICTION_STATISTICS('192.168.1.79:10001','MeyrinSimulator','2022-09-06 11:00:00',11,NULL,'produced')
;

select * from TmpStateDistrib1 where hour=11

SELECT p.*
			    ,sh.`date`
			    ,sh.state_idx
			    ,sh.state_name
			    ,(p.random_state_idx = sh.state_idx) AS is_ok1
			    ,(p.likely_state_idx = sh.state_idx) AS is_ok2
			    ,p.random_state_proba AS proba_random
			    ,p.likely_state_proba AS proba_likely
			    ,ABS(CONVERT(p.random_state_idx , signed)   -  CONVERT(sh.state_idx, signed) ) as delta_abs
			    ,(CONVERT(p.random_state_idx , signed)   -  CONVERT(sh.state_idx, signed) ) as delta
			    ,IF(p.id_correction IS NULL, 0, 1) AS has_correction
			   --  ,COMPUTE_VECTOR_DIFFERENTIAL(p.id) AS vector_differential
			    -- ,TmpStateDistrib1.vector_differential
			    -- ,0 AS vector_differential
			    ,(SELECT IFNULL(SUM(TmpStateDistrib1.ratio),0)
			    			FROM TmpStateDistrib1 WHERE
			    				 TmpStateDistrib1.date =  Date(p.target_date)
					    	 AND TmpStateDistrib1.hour = HOUR(p.target_date)
					    	 AND TmpStateDistrib1.variable_name = p.variable_name) as has_states_distrib
			 	,(select pi2.proba  from prediction_item pi2 where pi2.id_prediction=p.id and pi2.state_idx=0) as p_S1
			 	,(select pi2.proba  from prediction_item pi2 where pi2.id_prediction=p.id and pi2.state_idx=1) as p_S2
			 	,(select pi2.proba  from prediction_item pi2 where pi2.id_prediction=p.id and pi2.state_idx=2) as p_S
			 	,(select pi2.proba  from prediction_item pi2 where pi2.id_prediction=p.id and pi2.state_idx=3) as p_S4
			 	,(select pi2.proba  from prediction_item pi2 where pi2.id_prediction=p.id and pi2.state_idx=4) as p_S5
			 	,(select pi2.proba  from prediction_item pi2 where pi2.id_prediction=p.id and pi2.state_idx=5) as p_S6
			 	,(select pi2.proba  from prediction_item pi2 where pi2.id_prediction=p.id and pi2.state_idx=6) as p_S7
				,COMPUTE_VECTOR_DIFFERENTIAL(p.id) as compute_diff
				,( DATE_ADD(p.target_date, interval 1 HOUR)  < NOW() ) as checkup1
				,(SELECT IFNULL(SUM(TmpStateDistrib1.ratio),0)
					FROM TmpStateDistrib1 WHERE
							TmpStateDistrib1.date = Date(p.target_date)
						AND TmpStateDistrib1.hour = HOUR(p.target_date)
						AND TmpStateDistrib1.variable_name = p.variable_name) as checkup2
				,(SELECT IFNULL(SUM(TmpStateDistrib1.ratio),0)
					FROM TmpStateDistrib1 WHERE
							TmpStateDistrib1.date = '2022-09-06'
						AND TmpStateDistrib1.hour =11
						AND TmpStateDistrib1.variable_name = p.variable_name) as test_foo
			 FROM prediction p
			 JOIN state_history AS sh ON sh.id = p.id_target_state_histo
			 WHERE Date(p.target_date) = '2022-09-06'
			    AND (HOUR(p.target_date) = 11)
			    AND (p.variable_name = 'produced')
				and p.horizon_minutes = 10 and p.use_corrections 


				
				
				
				
				
				
				
				
				
				

select current_date();
				
CALL SP_COMPUTE_STATE_DISTRIBUTION('192.168.1.79:10001','MeyrinSimulator','2022-09-01 02:00:00',NULL,'produced')
;
SELECT * FROM TmpStateDistribution
;
select
	DATE(p.target_date)  <= current_date() as t1
	,HOUR(p.target_date) < HOUR(NOW()) as t2
	,p.* from prediction p where NOT(
			DATE_ADD(p.target_date, interval 1 HOUR)  < NOW()
		)
;
select * from state_history sh where id_session = '20220831_162141_9269' and creation_date >='2022-09-01' and variable_name = 'produced' order by id desc
;
select * from prediction p
	join prediction_item pi2 on pi2.id_prediction = P.id
	where p.target_date  >='2022-08-09' 
;
delete p from prediction p where p.target_date  >='2022-08-09' 
;
delete pi2 from prediction p
	join prediction_item pi2 on pi2.id_prediction = P.id
	where p.target_date  >='2022-08-09' 
;
select count(*) from prediction p  where target_date  >='2022-08-09' 
;
select * from state_history where creation_date  >= '2022-07-23 07:28:21.000' and id_session<> '20220722_225153_4603' order by id
;
-- update state_history set id_last=null   where id_session = '20220808_222536_6798' and creation_date >='2022-08-09' 
 
-- delete state_history from state_history  where id_session = '20220808_222536_6798' and creation_date >='2022-08-09' 

select * from transition_matrix_cell_iteration tmci
	where tmci.creation_time >='2022-08-09'  --  and tmci.creation_time < '2022-07-23 08:56:50.000'

delete tmci from  transition_matrix_cell_iteration tmci where tmci.creation_time >='2022-08-09'
	
;
select * from state_history sh where variable_name = 'requested'
 and creation_date  >= '2022-07-23 07:28:21.000'
 and id_session = '20220722_225153_4603'
order by iD desc


;
select tr_mtx.* 
	,(select MAX(number) from transition_matrix_iteration  where id_transition_matrix = tr_mtx.id) as max_number
	from transition_matrix as tr_mtx
	WHERE tr_mtx.location = '192.168.1.79:10001'
		AND tr_mtx.scenario = 'MeyrinSimulator'
		-- and (select MAX(number) from transition_matrix_iteration  where id_transition_matrix = tr_mtx.id) <> iteration_number
;
SELECT
			  cell_it.id_transition_matrix
			 ,cell_it.row_idx
			 ,cell_it.column_idx
			 ,SUM(obs_number) AS new_obs_number
			 ,SUM(obs_number) AS new_obs_number1
		FROM transition_matrix AS tr_mtx
		JOIN transition_matrix_cell_iteration AS cell_it ON cell_it.id_transition_matrix = tr_mtx.id
		JOIN transition_matrix_iteration AS tmi ON tmi.id = cell_it.id_transition_matrix_iteration
		WHERE tr_mtx.location = '192.168.1.79:10001'
			AND tr_mtx.scenario = 'MeyrinSimulator'
			-- AND (tr_mtx.id_time_window = p_id_time_window OR p_id_time_window IS NULL)
		 AND tmi.number >= IF(tr_mtx.iteration_number > 100,tr_mtx.iteration_number - 100,0)
		 AND tmi.number <= tr_mtx.iteration_number
		 GROUP BY id_transition_matrix , row_idx , column_idx
;
SELECT
	  cell_it.id_transition_matrix
	 ,cell_it.row_idx
	 ,cell_it.column_idx
	 ,SUM(if(rank<=100,obs_number,0)) AS new_obs_number
	 ,SUM(if(rank<=10,obs_number,0)) AS new_obs_number_10
	 ,SUM(if(rank<=10,(1+10-rank)*obs_number,0)) AS new_obs_number_10b
	 ,SUM(EXP(-0.1*(rank-1)) *obs_number) AS new_obs_number_10c
		FROM
		(
			select tr_mtx_it1.*
			,EXP(-0.1*(rank2-1))  AS coeff_rank
				FROM
					(
						SELECT tmi.*
								,  1+tr_mtx.iteration_number - tmi.`number`  AS rank
								, 1+ (select Count(*) from transition_matrix_iteration AS tmi2 where tmi2.id_transition_matrix = tr_mtx.ID and tmi2.number > tmi.`number`) as rank2
								from transition_matrix AS tr_mtx
								JOIN transition_matrix_iteration AS tmi ON tmi.id_transition_matrix = tr_mtx.id
								WHERE tr_mtx.location = '192.168.1.79:10001'
									AND tr_mtx.scenario = 'MeyrinSimulator'
					) AS tr_mtx_it1	
		) AS tr_mtx_it
JOIN transition_matrix_cell_iteration AS cell_it ON cell_it.id_transition_matrix_iteration  = tr_mtx_it.id
GROUP BY id_transition_matrix , row_idx , column_idx
;
select * from transition_matrix_cell_iteration
;
select * from prediction p  where id_tgt_state_histo is null 
;
select p.* 
	, unix_timestamp(sh.date)  - unix_timestamp(target_date) as delta
	from prediction p 
	join state_history sh on sh.id = p.id_tgt_state_histo 
;
select * from transition_matrix_cell where rowsum  > 0
;
select * from transition_matrix_cell order by obs_number desc, id_transition_matrix DESC
;
UPDATE TmpPrediction SET delta = (SELECT sh.ut_date  from TmpSH AS sh WHERE sh.id = id_tgt_state_histo) - ut_target_date
		WHERE NOT id_tgt_state_histo IS NULL
;
select * from event e where object_type = 'PRODUCTION' and agent= 'Prod_N1_1' -- P PV
;

select select3.hour
	, group_concat(Label, '  '  order by nb DESC) as state_distribution
	, SUM(nb) as total_nb 
	from
		(
		select select1.hour
			,select1.state_name
			,select1.nb
			,select2.total_nb
			,select1.nb/select2.total_nb as ratio
			,CONCAT(state_name, ':', ROUND(nb/total_nb,2)) as Label
			FROM
					(select hour(date)  as hour, state_name , count(*) as nb
						from state_history h where scenario = 'MeyrinSimulator' and location='192.168.1.79:10001' and variable_name = 'produced' 
						group by state_name ,hour
						order by hour, nb DESC ) as select1
			JOIN
					(select hour(date)  as hour , count(*) as total_nb
						from state_history h where scenario = 'MeyrinSimulator' and location='192.168.1.79:10001' and variable_name = 'produced' 
						group by hour
					) as select2
					 on select2.hour = select1.hour
		) AS select3
	group by select3.hour
;

select * from prediction p2 order by id desc
;
update prediction set likely_state_idx = 0
;
update prediction set likely_state_name = ''
;

update prediction p set  p.random_state_proba =(select proba from prediction_item pi2 where pi2.id_prediction = p.id and pi2.state_idx = p.random_state_idx) where p.random_state_proba = 0
;
select p.* 
	,(select proba from prediction_item pi2 where pi2.id_prediction = p.id and pi2.state_idx = p.random_state_idx) as radom_state_proba2
	from prediction p
where p.random_state_proba = 0
;

select p.* 
	,(select proba from prediction_item pi2 where pi2.id_prediction = p.id order by proba desc limit 0,1) as likely_state_proba2
	from prediction p
where p.likely_state_proba = 0
;

select p.* from prediction p
	where exists (select 1 from prediction_item pi2 where pi2.id_prediction = p.id and pi2.proba > p.likely_state_proba )
;
update prediction p set p.likely_state_proba = (select proba from prediction_item pi2 where pi2.id_prediction = p.id order by proba desc limit 0,1) 
	where p.likely_state_proba = 0;
;
update prediction p set p.likely_state_idx = (select pi2.state_idx  from prediction_item pi2 where pi2.id_prediction = p.id and pi2.proba = p.likely_state_proba limit 0,1) 
	where p.likely_state_name=''
;
update prediction p set p.likely_state_name = (select pi2.state_name  from prediction_item pi2 where pi2.id_prediction = p.id and pi2.state_idx  = p.likely_state_idx) 
	where p.likely_state_name=''
;

select * from prediction_item where id_prediction =1 and state_idx =3

select * from prediction
--  join prediction_item pi2  on pi2.id_prediction  = prediction.id 
order by id desc


select count(*) from prediction p where id_tgt_state_histo in (select id from state_history  where observation_update is NULL )

update prediction set id_tgt_state_histo = null where id_tgt_state_histo in (select id from state_history  where observation_update is NULL )

select SUM(1) as total
, SUM(is_ok2) as isOK2
 , SUM(is_ok2) / SUM(1) AS rate_ok2
from
(
	select p.*
		  ,(p.likely_state_idx = sh.state_idx) as is_ok2
		from prediction p 
		join state_history sh on sh.id = p.id_tgt_state_histo 
		where p.scenario = 'MeyrinSimulator' and sh.observation_update is NULL
		-- order by date
	) as FOO



call SP_CONSOLIDATE_PREDICTIONS('2022-07-01', date_add(NOW() , interval 1 DAY))
;

select p.*, sh.date, sh.state_name
	, p.likely_state_idx = sh.state_idx as is_ok2 
	,sh.creation_date 
	 from prediction p
	 join state_history sh on sh.id = p.id_tgt_state_histo 
	 WHERE p.variable_name  in ('-available', 'produced')
	order by p.id desc
;
	
update prediction set link_done=1 where not id_tgt_state_histo is NULL
;
select * from prediction p where id_tgt_state_histo is null and link_done
;
select * from state_history sh where variable_name = 'requested' order by id desc
;

select * from prediction p 
	join state_history sh on sh.id = p.id_tgt_state_histo 
	where p.scenario = 'MeyrinSimulator' and  p.variable_name = 'produced' and HOUR(p.initial_date) = 8
	order by date




select * from time_window tw 
select * from state_history sh where creation_date >= '2022-07-04 10:50' and state_idx = state_idx_last and state_idx >= 3


delete tmc from transition_matrix_cell tmc where id_transition_matrix in (select id from transition_matrix tm where location='192.168.1.79:10001' and scenario = 'MeyrinSimulator')
select  tmc.* from transition_matrix_cell tmc where id_transition_matrix in (select id from transition_matrix tm where location='192.168.1.79:10001' and scenario = 'MeyrinSimulator')


update transition_matrix_cell_iteration set corrections_number = 0;

SELECT * from clemap_data_light.measure_record mr where feature_type LIKE '15_MN' order by `timestamp` desc


SELECT distinct measure_record.timestamp
,TIMESTAMPADD(MINUTE,
    TIMESTAMPDIFF(MINUTE,DATE(timestamp),timestamp),
    DATE(timestamp)) AS timestamp3
,sensor.serial_number AS sensor_number
,sensor.location
,sensor_input.device_category
,sensor_input.description  AS device_name
,phase_mr.phase
,ABS(phase_mr.p) AS p
,ABS(phase_mr.q) AS q
,ABS(phase_mr.s) AS s
FROM clemap_data_light.measure_record
JOIN clemap_data_light.sensor on sensor.serial_number = measure_record.sensor_number
JOIN clemap_data_light.phase_measure_record AS phase_mr ON phase_mr.id_measure_record = measure_record.id
JOIN clemap_data_light.sensor_input ON  sensor_input.id_sensor = sensor.id AND sensor_input.phase=phase_mr.phase
WHERE  measure_record.timeStamp >='2022-08-30 09:14:17'
	AND measure_record.timeStamp <'2022-08-30 11:14:17' 
	AND feature_type='MN' 
	AND NOT sensor_input.is_disabled
	ORDER BY measure_record.timestamp

INSERT INTO event SET begin_date = '2022-09-30 10:49:15', expiry_date = '2023-09-30 10:49:15', id_session = '20220930_104913_9119', type = 'REQUEST_START', object_type = 'REQUEST', main_category = 'START', warning_type = '', power = '556.53' ,power_min = '556.53' ,power_max = '556.53', power_update = '0.0' ,power_min_update = '0.0' ,power_max_update = '0.0' 
,time_shift_MS = '-2678400000', agent = 'Consumer_N1_2', location = '192.168.1.79:10001', distance = '0', device_name = 'Entré du tableau TSG.03', device_category = 'Electrical panel', environmental_impact = '3', is_cancel = 0, is_ending = 0, id_origin = NULL, is_complementary = 0, comment = ''






desc event





select * from history h order by date desc 
desc

select * from event e where begin_date >= '2022-09-01'

select * from event e where object_type = 'CONTRACT'

select * from single_offer so order by id desc


select * from history h where id = 1510688

select * from prediction p order by id desc


SELECT distinct measure_record.timestamp
,TIMESTAMPADD(MINUTE,
    TIMESTAMPDIFF(MINUTE,DATE(timestamp),timestamp),
    DATE(timestamp)) AS timestamp3
FROM clemap_data_light.measure_record
WHERE  measure_record.timeStamp >='2022-08-30 10:14:17'
	AND measure_record.timeStamp <'2022-08-31 23:14:17' 
	AND feature_type='MN' 
	ORDER BY measure_record.timestamp




select * from time_window tw where id=6

select * from log_self_correction lsc 
join transition_matrix tm on tm.id = lsc.id_transition_matrix
where variable_name = 'produced' and tm.id_time_window = 6
and excess= 0.10678
order by lsc.id desc

select * from event e order by id desc

delete from transition_matrix_cell_iteration where obs_number = 0 and corrections_number =0;


SET @min_date = (SELECT MIN(target_date) FROM prediction WHERE target_date>= '2022-07-01' AND NOT link_done AND location = '192.168.1.79:10001' AND scenario = 'MeyrinSimulator')
;
SET @max_date = DATE_ADD(NOW() , INTERVAL -2 MINUTE)
;
CALL SP_CONSOLIDATE_PREDICTIONS(@min_date, @max_date) 




select * from transition_matrix tm where id_context = 1

CALL REFRESH_TRANSITION_MATRIX_CELL2(1,NULL,100);


SELECT
			  cell_it.id_transition_matrix
			 ,cell_it.row_idx
			 ,cell_it.column_idx
			 ,SUM(obs_number) AS new_obs_number
			 ,SUM(corrections_number) AS new_corrections_number
		FROM transition_matrix AS tr_mtx
		JOIN transition_matrix_cell_iteration AS cell_it ON cell_it.id_transition_matrix = tr_mtx.id
		JOIN transition_matrix_iteration AS tmi ON tmi.id = cell_it.id_transition_matrix_iteration
		WHERE tr_mtx.id_context = 1
		 AND tmi.number >= IF(tr_mtx.iteration_number > 100,tr_mtx.iteration_number - 100,0)
		 AND tmi.number <= tr_mtx.iteration_number
		 GROUP BY id_transition_matrix , row_idx , column_idx

select * from TmpComputeObsNb

select count(*) from prediction where creation_date >= '2022-07-28'and creation_date < '2022-08-10 12:44:31.000'

select * from transition_matrix_cell_iteration tmci where corrections_number > 0

select * from prediction p where p.target_date >= DATE_ADD(NOW(), interval -1 hour)

select * from transition_matrix_cell_iteration tmci where id_transition_matrix = 171690 and creation_time >= '2022-08-12' and id_transition_matrix_iteration  = @it_id

select * from log_self_correction lsc where creation_date >='2022-08-12' and id_transition_matrix ='171690' order by id desc


select * from log_self_correction lsc
	join prediction p on p.id = lsc.id_prediction 
	where not lsc.id_prediction is NULL

update  log_self_correction lsc
	join prediction p on p.id = lsc.id_prediction 
	set p.id_correction = lsc.id
	where not lsc.id_prediction is NULL
	
select * from prediction p where not id_correction is NULL

select * from prediction p order by id desc
select * from state_history sh order by id desc

select * from transition_matrix tm 
select * from time_window tw 
select * from log_self_correction lsc where tag = 'generateMassivePredictions' order by id desc

-- 17h
select * from transition_matrix_cell_iteration tmci where id_transition_matrix = 171361 and ABS(corrections_number) > 0


2022-08-12 17:59:59.000	368468	171361	4	3	0	73
2022-08-12 17:59:59.000	368468	171361	4	4	43	-72
2022-08-29 17:59:55.000	370580	171361	4	3	1	-4
2022-08-29 17:59:55.000	370580	171361	4	4	17	4

CALL REFRESH_TRANSITION_MATRIX_CELL2('192.168.1.79:10001','MeyrinSimulator',NULL,100);

-- 13 h
select * from transition_matrix_cell_iteration tmci where id_transition_matrix = 171223 and ABS(corrections_number) > 0
2022-08-12 13:59:35.000	367310	171223	5	4	2	17
2022-08-12 13:59:35.000	367310	171223	5	5	27	-17
2022-08-30 13:59:05.000	371624	171223	4	4	13	-43
2022-08-30 13:59:05.000	371624	171223	4	5	2	57
2022-08-30 13:59:05.000	371624	171223	5	4	2	76
2022-08-30 13:59:05.000	371624	171223	5	5	26	-76

 


-- BEGIN


SET @it_number = (SELECT iteration_number FROM transition_matrix tm where id = 171361)
§
SET @it_id = (SELECT id FROM transition_matrix_iteration tmi WHERE tmi.id_transition_matrix = 171361 AND tmi.number = @it_number)
§
SET @row_sum = (SELECT IFNULL(SUM(obs_number + corrections_number),0) FROM transition_matrix_cell AS tmc  WHERE tmc.id_transition_matrix = 171361 AND row_idx = 4)
§
SET @cell_sum = (SELECT IFNULL(SUM(obs_number + corrections_number),0) FROM transition_matrix_cell AS tmc  WHERE tmc.id_transition_matrix = 171361 AND row_idx = 4 AND column_idx = 4)
§
SET @added_corrections_number = GREATEST(1, ROUND(0.1*(0.5*@row_sum - @cell_sum)))
§
INSERT INTO transition_matrix_cell_iteration SET
 id_transition_matrix_iteration = @it_id
,id_transition_matrix = 171361
,row_idx = 4
,column_idx = 3
,corrections_number = -1*@added_corrections_number
 ON DUPLICATE KEY UPDATE corrections_number = -1*@added_corrections_number + corrections_number
§
INSERT INTO transition_matrix_cell_iteration SET
 id_transition_matrix_iteration = @it_id
,id_transition_matrix = 171361
,row_idx = 4
,column_idx = 4
,corrections_number = 1*@added_corrections_number
 ON DUPLICATE KEY UPDATE corrections_number = 1*@added_corrections_number + corrections_number
§
UPDATE transition_matrix SET last_update=NOW() WHERE id = 171361
§
INSERT INTO log_self_correction SET
 id_session = '20220830_104907_2154'
,tag = 'generateMassivePredictions'
,id_transition_matrix = 171361
,id_transition_matrix_iteration = @it_id
,it_number = @it_number
,initial_state_idx = 4
,from_state_idx = 3
,dest_state_idx = 4
,cell_sum = @cell_sum
,row_sum = @row_sum
,corrections_number = @added_corrections_number
§
SET @id_correction = LAST_INSERT_ID()
§
SELECT * FROM log_self_correction WHERE id = @id_correction




--- FIN







SELECT p.id_initial_transition_matrix, p.id
,p.variable_name 
 ,p.target_date
,p.initial_state_idx AS initialStateIdx
,p.likely_state_idx AS predictedStateIdx
 ,p.likely_state_proba AS predictedStateProba
,sh.state_idx AS actualStateIdx 
FROM prediction p
JOIN state_history sh ON sh.id = p.id_target_state_histo
WHERE p.target_date >= DATE_ADD(NOW(), interval -1 hour) 
 AND p.use_corrections 
 AND NOT p.id_target_state_histo IS NULL 
 AND NOT p.likely_state_idx = sh.state_idx
 AND p.id_correction IS NULL
 AND p.id_initial_transition_matrix = p.id_target_transition_matrix
--
SET @it_number = (SELECT iteration_number FROM transition_matrix tm where id = 171690)
;
SET @it_id = (SELECT id FROM transition_matrix_iteration tmi WHERE tmi.id_transition_matrix = 171690 AND tmi.number = @it_number)
;
SET @row_sum = (SELECT IFNULL(SUM(obs_number + corrections_number),0) FROM transition_matrix_cell AS tmc  WHERE tmc.id_transition_matrix = 171690 AND row_idx = 0)
;
SET @cell_sum = (SELECT IFNULL(SUM(obs_number + corrections_number),0) FROM transition_matrix_cell AS tmc  WHERE tmc.id_transition_matrix = 171690 AND row_idx = 0 AND column_idx = 1)
;
SET @added_corrections_number = GREATEST(1, ROUND(0.1*(0.5*@row_sum - @cell_sum)))
;
INSERT INTO transition_matrix_cell_iteration SET
 id_transition_matrix_iteration = @it_id
,id_transition_matrix = 171690
,row_idx = 0
,column_idx = 1
,corrections_number = 1*@added_corrections_number
 ON DUPLICATE KEY UPDATE corrections_number = 1*@added_corrections_number + corrections_number
--
select * from transition_matrix tm where id = 171684

select * from 

-- update transition_matrix_cell_iteration set corrections_number = 0;

SELECT p.*
			    ,sh.`date`
			    ,sh.state_idx
			    ,sh.state_name
			     ,p.likely_state_name
			    -- ,(p.random_state_idx = sh.state_idx) as is_ok1
			    ,(p.likely_state_idx = sh.state_idx) as is_ok2
			   -- ,p.random_state_proba as proba_random
			    ,p.likely_state_proba as proba_likely
			    ,(SELECT IFNULL(SUM(lsc.corrections_number),0) FROM log_self_correction lsc WHERE lsc.id_prediction = p.id) AS corrections_number
			 FROM prediction p
			 JOIN state_history AS sh ON sh.id = p.id_target_state_histo
			 WHERE p.target_date >='2022-08-11'
			    AND (HOUR(p.target_date) >= 9)
			    AND (HOUR(p.target_date) < 10 )
			    AND (p.variable_name = 'produced')
			    and p.use_corrections 
			    and p.horizon_minutes = 5





update prediction set use_corrections = 1 where creation_date >= '2022-07-28'and creation_date < '2022-08-10 12:44:31.000'


select * from prediction p order by id desc

select * from log_self_correction lsc


select * from transition_matrix_cell_iteration tmci where id_transition_matrix = 172465 and creation_time >= '2022-08-09'

ALTER TABLE log_self_correction ADD CONSTRAINT `fk3_id_prediction` FOREIGN KEY (`id_prediction`) REFERENCES `prediction` (`id`);

update log_self_correction set corrections_number=1 where corrections_number=0

select * from log_self_correction lsc where id_prediction  in (select id from prediction p) and corrections_number > 1
update log_self_correction set id_prediction = NULL where id_prediction not in (select id from prediction p)


creation_time	id_transition_matrix_iteration	id_transition_matrix	row_idx	column_idx	obs_number	corrections_number
2022-08-09 15:59:47.000	353516	172465	3	3	27	42
2022-08-09 15:59:47.000	353516	172465	3	4	0	-122
2022-08-09 15:59:47.000	353516	172465	3	5	1	80
2022-08-09 15:59:47.000	353516	172465	4	3	1	0
2022-08-09 15:59:47.000	353516	172465	4	4	13	13
2022-08-09 15:59:47.000	353516	172465	4	5	0	-13
2022-08-09 15:59:47.000	353516	172465	5	4	1	-6
2022-08-09 15:59:47.000	353516	172465	5	5	14	6


select  p.id_initial_transition_matrix, p.id_target_transition_matrix, p.id, p.likely_state_idx AS fromStateIdx , sh.state_idx AS destStateIdx 
	,p.*
FROM TmpPrediction
JOIN prediction p  ON p.id = TmpPrediction.id
JOIN state_history sh ON sh.id = p.id_target_state_histo
WHERE not p.id_target_state_histo IS NULL AND NOT p.likely_state_idx = sh.state_idx
AND p.id_initial_transition_matrix = p.id_target_transition_matrix

select * from transition_matrix tm 
join time_window tw on tw.id = tm.id_time_window 
where tm.id in (171689,179243 )

SELECT p.id_initial_transition_matrix, p.id, p.likely_state_idx AS fromStateIdx , sh.state_idx AS destStateIdx 
FROM TmpPrediction
JOIN prediction p  ON p.id = TmpPrediction.id
JOIN state_history sh ON sh.id = p.id_target_state_histo
WHERE not p.id_target_state_histo IS NULL AND NOT p.likely_state_idx = sh.state_idx
AND p.id_initial_transition_matrix = p.id_target_transition_matrix




select * from prediction p
join state_history sh on sh.id = p.id_target_state_histo 
where not p.likely_state_idx = sh.state_idx 
order by p.id desc 





CALL REFRESH_TRANSITION_MATRIX_CELL2('192.168.1.79:10001','MeyrinSimulator',NULL,100);
CALL REFRESH_TRANSITION_MATRIX_CELL2('192.168.1.79:10001','MeyrinSimulator',8,100);

select * from TmpComputeObsNb where id_transition_matrix = 171965

INSERT INTO transition_matrix_cell(id_transition_matrix ,row_idx,column_idx,obs_number,corrections_number)
		SELECT id_transition_matrix, row_idx, column_idx, new_obs_number, new_corrections_number
		FROM TmpComputeObsNb WHERE (new_obs_number + new_corrections_number) > 0 and id_transition_matrix = 171965
		ON DUPLICATE KEY UPDATE obs_number = TmpComputeObsNb.new_obs_number
			, corrections_number = TmpComputeObsNb.new_corrections_number
	;



se

select * from transition_matrix tm where variable_name = 'produced' and id_time_window = 14

select * from log_self_correction lsc order by id desc

select * from log_self_correction lsc where id_transition_matrix in (171967  , 171223  ) and creation_date >= '2022-08-03'


select * from transition_matrix_cell tmc
	join transition_matrix tm on tm.id = tmc.id_transition_matrix 
where corrections_number < 0


select 0.1*(0.5*215-76)



select * from transition_matrix tm where id = 171963  


select * from transition_matrix tm  where id in (172463, 171233)

select * from transition_matrix_cell_iteration tmci where
	1 -- tmci.corrections_number > 0
and	tmci.id_transition_matrix in (171967  , 171223  )
and tmci.creation_time >= '2022-08-03 09:59:16.000'


select * from transition_matrix_cell tmc where
	1 -- tmci.corrections_number > 0
and	tmc.id_transition_matrix in (171967  , 171223  )
and row_idx = 4 and column_idx =3

-- correctionsNb = 28

select cell.* FROM TmpComputeObsNb 
		JOIN transition_matrix_cell AS cell ON cell.id_transition_matrix = TmpComputeObsNb.id_transition_matrix
			AND cell.row_idx = TmpComputeObsNb.row_idx
			AND cell.column_idx = TmpComputeObsNb.column_idx
		WHERE TmpComputeObsNb.new_obs_number = 0 AND TmpComputeObsNb.new_corrections_number = 0
;

select * from transition_matrix_cell tmc 
join transition_matrix_cell_iteration tmci on tmci.id_transition_matrix = tmc.id_transition_matrix and tmci.row_idx = tmc.row_idx and tmci.column_idx = tmc.column_idx and tmci.corrections_number > 100 
where tmc.corrections_number > tmc.obs_number order by (tmc.corrections_number- tmc.obs_number) DESC

select * from transition_matrix_cell_iteration tmci where id_transition_matrix = 171222 and row_idx = 0 and column_idx =1 and corrections_number > 0
;

select * from TmpComputeObsNb where id_transition_matrix in (172463, 171233) 

CALL REFRESH_TRANSITION_MATRIX_CELL2('192.168.1.79:10001','MeyrinSimulator',NULL,100);

select * from transition_matrix_cell_iteration tmci   where
	abs(tmci.corrections_number) > 0
and	tmci.id_transition_matrix in (171967 ,171223) 
	and creation_time >= '2022-08-03'


select * from transition_matrix_cell tmc where
	tmc.corrections_number > 0
and	tmc.id_transition_matrix in (171967 ,171223) 



select @row_sum, @cell_sum, @corrections_number

select * from log_self_correction lsc
join transition_matrix tm on tm.id = lsc.id_transition_matrix 
where tm.id in (171967 ,171223  ) and lsc.creation_date >= '2022-08-03' and tag like 'generateMassivePredictions%'
order by lsc.id desc













2022-08-03 01:46:19.000

select * from state_history sh where id_session = '20220802_224319_3814' and variable_name = 'produced' order by id desc

update state_history set id_last = NULL where id_session = '20220802_224319_3814' 

delete state_history from state_history  where id_session = '20220802_224319_3814' 

delete transition_matrix_cell_iteration from transition_matrix_cell_iteration where creation_time between '2022-08-02 22:44:21.000' and '2022-08-03 08:34:56.000'

delete prediction from prediction  where creation_date between '2022-08-02 22:44:21.000' and '2022-08-03 08:34:56.000'

delete ib from prediction
join prediction_item ib on ib.id_prediction = prediction.id
where creation_date between '2022-08-02 22:44:21.000' and '2022-08-03 08:34:56.000'

select * from transition_matrix tm
join time_window tw on tw.id = tm.id_time_window 
join transition_matrix_cell tmc on tmc.id_transition_matrix  = tm.id
where tm.id=171965 and tmc.corrections_number > 0


select * from transition_matrix tm
join transition_matrix_cell_iteration tmci  on tmci.id_transition_matrix  = tm.id
where tm.id=171965 and tmci.corrections_number > 0




select * from transition_matrix_cell_iteration tmci where id_transition_matrix  = 173299 order by creation_time desc

select  DATE_ADD(NOW() , INTERVAL -1 MINUTE)

update prediction set id_target_state_histo = null, delta_target_state_histo=null, link_done = 0 where target_date >='2022-07-27' and delta_target_state_histo < 0
;

select count(*) , DATE(p.target_date) as date1
	from prediction p
	where not id_target_state_histo is null and abs(delta_target_state_histo)  > 90
	group by date1
	order by date1 DESC
;

select
	 row_idx
	,numerator/denominator as state_avg
	FROM
		(select row_idx, 
			SUM(obs_number*column_idx)  as numerator,
			SUM(obs_number) as denominator
			from transition_matrix_cell tmc where  id_transition_matrix = 171965 
			group by row_idx
		) as select1
;
select COMPUTE_STATE_VARIANCE(171965)
;
select VARIANCE(numerator/denominator) as state_variance
	FROM
		(select row_idx, 
			SUM(obs_number*column_idx)  as numerator,
			SUM(obs_number) as denominator
			from transition_matrix_cell tmc where  id_transition_matrix = 171965 
			group by row_idx
		) as select1

select * from transition_matrix_cell_iteration tmci 
;




select * from transition_matrix_cell tmc
join transition_matrix tm on tm.id = tmc.id_transition_matrix 
where corrections_number > 0


select * from transition_matrix_cell_iteration tmci
join transition_matrix tm on tm.id = tmci.id_transition_matrix 
where corrections_number > 0



SET @it_number = (SELECT iteration_number FROM transition_matrix tm where id = 179245)
§
SET @it_id = (SELECT id FROM transition_matrix_iteration tmi WHERE tmi.id_transition_matrix = 179245 AND tmi.number = @it_number)
§
INSERT INTO transition_matrix_cell_iteration SET
 id_transition_matrix_iteration = @it_id
,id_transition_matrix = 179245
,row_idx = 2
,column_idx = 1
,corrections_number = 1
 ON DUPLICATE KEY UPDATE corrections_number = 1 + corrections_number
§
UPDATE transition_matrix SET last_update=NOW() WHERE id = 179245
§








SET @it_number = (SELECT iteration_number FROM transition_matrix tm where id = 173299)
§
SET @it_id = (SELECT id FROM transition_matrix_iteration tmi WHERE tmi.id_transition_matrix = 173299 AND tmi.number = @it_number)
§
INSERT INTO transition_matrix_cell_iteration SET
 id_transition_matrix_iteration = @it_id
,id_transition_matrix = 173299
,row_idx = 2
,column_idx = 1
,corrections_number = 1
 ON DUPLICATE KEY UPDATE corrections_number = 1 + corrections_number
§
UPDATE transition_matrix SET last_update=NOW() WHERE id = 173299
 
 select * from prediction p order by id desc 
  select *  from transition_matrix tm where id  = 173299
 
 select obs_number  from transition_matrix_cell tmc where id_transition_matrix  = 173299 and row_idx = 1 and column_idx = 2
select obs_number  from transition_matrix_cell tmc where id_transition_matrix  = 173299 and row_idx = 2 and column_idx = 3

 select * from transition_matrix tm where id = 171967
 select * from time_window tw  where id=8
 
  select * from transition_matrix_cell tmc  where corrections_number > 0

 select * from transition_matrix_cell_iteration tmci  where corrections_number > 0
 
 select * from transition_matrix_cell tmc where id_transition_matrix = 173299 and row_idx =2 and column_idx =1 
 

SET @it_number = (SELECT iteration_number FROM transition_matrix tm where id = 173299)
;
SET @it_id = (SELECT id FROM transition_matrix_iteration tmi WHERE tmi.id_transition_matrix = 173299 AND tmi.number = @it_number)
;
select * FROM transition_matrix_cell_iteration tmci 
	WHERE tmci.id_transition_matrix_iteration = @it_id AND tmci.row_idx = 1 AND tmci.column_idx = 2




SET @it_id = (SELECT id FROM transition_matrix_iteration tmi WHERE tmi.id_transition_matrix = 173299 AND tmi.number = @it_number)
;
UPDATE transition_matrix_cell_iteration tmci ON tmci.id_transition_matrix_iteration = tmi.id SET tmci.errors_number = tmci.errors_number + 1  WHERE tmi.id_transition_matrix_iteration = @it_id AND tmi.row_idx = 1 AND tmi.column_idx = 2



UPDATE transition_matrix_cell_iteration tmci
join transition_matrix tm  on tm.id = tmci.id_transition_matrix
join time_window tw on tw.id = tm.id_time_window
set tmci.obs_number = 0
where 1
	-- and tm.variable_name = 'consumed'
and tm.scenario = 'MeyrinSimulator'
and creation_time  > '2000-01-01'
 and row_idx  = column_idx
and row_idx in (0,1,2,3,4,5,6)
order by tmci.creation_time desc

SELECT state_history.*, 1+state_idx AS state_id FROM state_history WHERE  location = '192.168.1.79:10001' AND scenario = 'MeyrinSimulator' AND variable_name = 'available' AND id_session = '20220721_101842_4230' AND creation_date >= '2022-07-21 10:18:42' 

select * from state_history sh where variable_name ='available' order by id desc

select * from transition_matrix_cell_iteration tmci 
select iteration_number  from transition_matrix tm where id = 1 

select * from transition_matrix_cell_iteration tmci 
join transition_matrix tm  on tm.id = tmci.id_transition_matrix
join time_window tw on tw.id = tm.id_time_window 
where 1
	-- and tm.variable_name = 'consumed'
and tm.scenario = 'MeyrinSimulator'
and creation_time  > '2000-01-01'
 and row_idx  = column_idx
and row_idx in (0)
-- and tw.start_hour = 10
-- and variable_name='produced'
order by tmci.creation_time desc


-- Check cells
select * from transition_matrix_cell tmc 
join transition_matrix tm  on tm.id = tmc.id_transition_matrix
join time_window tw on tw.id = tm.id_time_window 
where 1
	-- and tm.variable_name = 'consumed'
and tm.scenario = 'MeyrinSimulator'
and tm.variable_name = 'available'
and tw.start_hour = 10
order by row_idx , column_idx 

178667


2022-07-05 10:59:13.000
2022-07-08 10:59:18.000
select * from state_history sh where observation_update = '2022-07-05 10:59:13.000'




update transition_matrix_cell_iteration tmci
join transition_matrix_iteration tmi ON  tmi.id  = tmci.id_transition_matrix_iteration 
join transition_matrix tm  on tm.id = tmci.id_transition_matrix
join time_window tw on tw.id = tm.id_time_window 
set tmci.obs_number  = 0
where tmi.creation_time < '2022-06-28'
and tm.scenario = 'MeyrinSimulator'
and tmci.creation_time  < '2000-01-01'
 and row_idx  = column_idx
 -- and tw.start_hour in (9,10,11,12,13)
-- and row_idx in (1,2,3,4,5,6)


select * from transition_matrix_cell_iteration tmci
join transition_matrix_iteration tmi ON  tmi.id  = tmci.id_transition_matrix_iteration 
join transition_matrix tm  on tm.id = tmci.id_transition_matrix
join time_window tw on tw.id = tm.id_time_window 
where tmi.creation_time < '2022-06-28'
and tm.scenario = 'MeyrinSimulator'
and tmci.creation_time  < '2000-01-01'
 and row_idx  = column_idx
 and tw.start_hour in (9,10,11,12,13)
-- and row_idx in (1,2,3,4,5,6)
order by tmci.creation_time desc



select p.*, p.variable_name , sh1.variable_name, p.target_date
		, sh1.date as date_sh1
		, sh2.date as date_sh2
		, TIMEDIFF(p.target_date,SH1.date) as delta1
		, UNIX_TIMESTAMP(p.target_date) - UNIX_TIMESTAMP(SH1.date) as delta1_sec
		, UNIX_TIMESTAMP(p.target_date) - UNIX_TIMESTAMP(SH2.date) as delta2_sec
	from prediction p
	join state_history as SH1 on sh1.ID = p.id_tgt_state_histo1
	join state_history as SH2 on sh1.ID = p.id_tgt_state_histo2

update prediction set id_tgt_state_histo1 = null 

alter table state_history drop key date
alter table state_history add KEY(date)



update prediction set id_tgt_state_histo1 = null ;
update prediction set id_tgt_state_histo2 = null ;



select * from history h  order by id desc



-- 2022-07-01 16:31:25.000
select * from state_history   where variable_name = 'consumed' order by id desc 

SELECT * FROM state_history WHERE  location = '192.168.1.79:10001' AND scenario = 'MeyrinSimulator'  and variable_name ='consumed' AND date >= '2022-07-01 13:00:00' order by date desc


select * from single_offer so where consumer_agent = 'Consumer_N1_1'

select * from device d where category like '%ENG%'

select * from history order by date

select * from link_history_active_event where id_history = 944228

select * from event e where  not localization = '192.168.1.79:10001'

update transition_matrix  set scenario  = REPLACE(scenario, 'HomeSimulator1', 'HomeSimulator')


select * from transition_matrix where scenario = 'HomeSimulator'

select  * from single_offer so where is_complementary 


select * from event e  where agent like '192.168.%'

select * from event e where begin_date >=  '2022-09-01'

select * from history h where date >= '2022-09-01'

select * from single_offer so where date >= '2022-09-01'

select * from prediction p order by id desc



select * from state_history sh order by id desc



select * from state_history sh 

select * from prediction p order by id desc
select * from prediction p where not link_done order by creation_date 

SET @min_date = (SELECT MIN(creation_date) FROM prediction WHERE target_date>= '2022-09-29' AND NOT link_done AND location = '192.168.1.79:10001' AND scenario = 'MeyrinSimulator')


(SELECT MIN(creation_date) FROM prediction where location = '192.168.1.79:10001' AND scenario = 'MeyrinSimulator' and NOT link_done  )

update prediction set link_done = 0  where creation_date>= '2022-10-07' AND id_target_state_histo is null 
;


SET @min_date = (SELECT MIN(creation_date) FROM prediction WHERE  NOT link_done AND id_context = 1)
;
SET @max_date = DATE_ADD(NOW() , INTERVAL -2 MINUTE)
;
CALL SP_CONSOLIDATE_PREDICTIONS(1,@min_date,@max_date) 
;
select * from TmpPrediction
;
UPDATE TmpPrediction p SET p.link_done = 1  WHERE NOT link_done and EXISTS (
	 		select 1 from TmpSH where TmpSH.variable_name = p.variable_name
					and TmpSH.ut_date > p.ut_target_date
					and TmpSH.ut_creation_date > p.ut_creation_date
			)
	;

SELECT variable_name, MAX(ut_creation_date) AS ut_creation_date , MAX(ut_date) AS ut_date			FROM TmpSH GROUP BY variable_name
;
select * from state_history sh order by id desc
;
select * from context c 
;
update prediction set id_context = (select context.id from context where context.location=prediction.location and context.scenario = prediction.scenario) where id_context is null;
;
update state_history  set id_context = (select context.id from context where context.location=state_history.location and context.scenario = state_history.scenario) where id_context is null 
:
update transition_matrix set id_context = (select context.id from context where context.location=transition_matrix.location and context.scenario = transition_matrix.scenario) where id_context is null;
;

select location, scenario, id_context, count(*) as nb from prediction p where 1 group by id_context, location, scenario order by nb desc
;
select location, scenario, id_context, count(*) as nb from state_history p where 1 group by id_context, location, scenario order by nb desc
;
select DATE(initial_date)  from prediction p where scenario = '' group by DATE(initial_date)  
;
delete prediction FROM  prediction  where prediction.scenario = ''
;
delete state_history FROM  state_history  where state_history.scenario = ''
;
delete pi FROM  prediction 
	join prediction_item pi on pi.id_prediction = prediction.id
	where prediction.scenario = ''
;
update state_history  set scenario = 'HomeSimulator' where scenario = 'HomeSimulator1'
;

INSERT INTO context SET
  location = '192.168.1.79:10001'
, scenario = 'MeyrinSimulator'
, last_id_session = '20221007_155203_7910'
, last_time_shift_ms ) -3283200000
 ON DUPLICATE KEY UPDATE 
 last_id_session = '20221007_155203_7910'
, last_time_shift_ms = -3283200000

select * from context

select  ABS(UNIX_TIMESTAMP(sh2.date) - UNIX_TIMESTAMP(sh1.date))
	, sh1.creation_date , sh2.creation_date
	, sh1.date, sh2.date
	from state_history sh1 
	join state_history sh2 on sh2.variable_name = sh1.variable_name 
	and sh1.creation_date >= '2022-10-01'
	and sh2.creation_date >= '2022-10-01'
	and ABS(UNIX_TIMESTAMP(sh2.date) - UNIX_TIMESTAMP(sh1.date)) < 30
	and DATE(sh1.creation_date) < DATE(sh2.creation_date)
	and not sh1.id_session = sh2.id_session 
;

delete sh1 
	from state_history sh1 
	join state_history sh2 on sh2.variable_name = sh1.variable_name 
	and sh1.creation_date >= '2022-10-01'
	and sh2.creation_date >= '2022-10-01'
	and ABS(UNIX_TIMESTAMP(sh2.date) - UNIX_TIMESTAMP(sh1.date)) < 30
	and DATE(sh1.creation_date) < DATE(sh2.creation_date)
	and not sh1.id_session = sh2.id_session 
;

CALL SP_COMPUTE_PREDICTION_STATISTICS(1,'2022-10-07','2022-10-07',NULL,NULL,NULL,NULL,NULL,'produced') 
;
CALL SP_COMPUTE_STATE_DISTRIBUTION(1,'2022-10-01','2022-10-07',NULL,NULL,'produced')
;
SELECT * FROM TmpStateDistrib1 WHERE variable_name='produced';
select * from TmpPredictionStatistic;
select * from TmpPredictionStatisticLine;


CALL SP_COMPUTE_STATE_DISTRIBUTION(1,'2022-10-07','2022-10-07', NULL, NULL,  'produced');


DROP TEMPORARY TABLE IF EXISTS TmpCleanPrediction
;
CREATE TEMPORARY TABLE TmpCleanPrediction AS
	SELECT id FROM prediction WHERE initial_date BETWEEN '2022-08-30 15:58:40' AND DATE_ADD('2022-08-30 15:58:40', INTERVAL 1 HOUR)
 	AND creation_date < DATE_ADD(NOW(), INTERVAL -1 HOUR)
;
DELETE pi FROM prediction_item pi WHERE pi.id_prediction IN (SELECT id FROM TmpCleanPrediction)
;
DELETE p FROM prediction p WHERE p.id IN (SELECT id FROM TmpCleanPrediction)
;
select * from state_history sh where date between '2022-08-30 17:00:00' and '2022-08-30 18:00:00' and variable_name = 'produced'
;


select * from state_history sh where date like '2022-08-31%' and creation_date > '2022-09-01'
;
select  DATE(date) as day
	from state_history sh group by day
	order by day desc
;
explain select  target_day ,  Count(*) as nb
	from prediction p	where target_day>'2022-09-01'	 group by target_day
	order by target_day desc
desc prediction 
;
select * from prediction p 
;
select  location ,  COunt(*) as nb
	from state_history sh group by location
;
select  DATE(date) as day, HOUR(date) as hour, COunt(*) as nb
	from state_history sh group by day, hour
	order by day desc
;
select * from TmpPrediction
;
select * from TmpPrediction
	left join prediction p  on p.id = TmpPrediction.id
	where p.id is NULL
;
select * from prediction p order by id desc
;
select p.* 
,(select MAX(creation_date) FROM state_history sh  WHERE variable_name = 'produced' and id_context=1) as max_creation_date
,(select MAX(date) FROM state_history sh  WHERE variable_name = 'produced' and id_context=1) as max_date
from prediction p
where creation_date > '2022-10-07 17:00' and variable_name = 'produced' and id_target_state_histo is null and link_done order by creation_date desc

select * from prediction p where creation_date >='2022-10-06 10:00:00.000' and not id_target_state_histo is null and abs(delta_target_state_histo)> 40 order by id desc
;

select  DATE_ADD( NOW(), INTERVAL 1 HOUR)
;

select * from state_history sh order by id desc

select * from state_history sh   where date between '2022-08-30 12:41:19.000' and DATE_ADD( '2022-08-30 12:41:19.000', INTERVAL 1 HOUR)



UPDATE state_history set id_last = null WHERE  date BETWEEN '2022-08-30 14:48:33' AND DATE_ADD('2022-08-30 14:48:33', INTERVAL 1 HOUR) AND NOT id_session = '20221006_105523_4970'



select  DATE(date) as date , HOUR(date) as hours, GROUP_CONCAT(distinct DATE(creation_date))
	, count(distinct DATE(creation_date)) as nb_creation_date
	, count(*)
	from state_history sh where variable_name = 'produced'
	group BY DATE(date) , HOUR(date)
	having nb_creation_date>1



select * from prediction p 

;
DROP TEMPORARY TABLE IF EXISTS TmpCleanSH


CREATE TEMPORARY TABLE TmpCleanSH AS
-- SELECT  sh1.id from state_history sh1 where sh1.`date` like '2022-08-31 %'

CREATE TEMPORARY TABLE TmpCleanSH AS
SELECT  sh1.id from state_history sh1 
	join state_history sh2 on sh2.variable_name = sh1.variable_name 
	and DATE(sh2.date) =  DATE(sh1.date)
	and sh1.creation_date >= '2022-08-01'
	and sh2.creation_date >= '2022-08-01'
	and ABS(UNIX_TIMESTAMP(sh2.date) - UNIX_TIMESTAMP(sh1.date)) < 60
	and DATE(sh1.creation_date) < DATE(sh2.creation_date)
	and not sh1.id_session = sh2.id_session 
	where  DATE(sh1.date) = '2022-08-30'
;


DROP TEMPORARY TABLE IF EXISTS TmpSH2
;
CREATE TEMPORARY TABLE TmpSH2 AS  SELECT sh.id FROM TmpCleanSH JOIN state_history sh  on sh.id_last  = TmpCleanSH.id
;
UPDATE TmpSH2
 JOIN state_history sh ON sh.id = TmpSH2.id
 SET sh.id_last = NULL
;
DROP TEMPORARY TABLE IF EXISTS TmpPrediction2
;
CREATE TEMPORARY TABLE TmpPrediction2 AS 
 SELECT p.id FROM TmpCleanSH
 JOIN prediction p ON p.id_target_state_histo  = TmpCleanSH.id
;
UPDATE TmpPrediction2
 JOIN prediction p ON p.id = TmpPrediction2.id
 SET p.id_target_state_histo = NULL
;
DELETE state_history FROM state_history WHERE id IN (SELECT id FROM TmpCleanSH)
;




drop TEMPORARY table if exists TmpCleanPrediction
;
CREATE TEMPORARY TABLE TmpCleanPrediction AS
	SELECT  p1.id from prediction p1 
		join prediction p2 on p2.variable_name = p1.variable_name 
		and p2.target_day = p1.target_day 
		and p2.horizon_minutes = p1.horizon_minutes 
		and p1.creation_date >= '2022-08-01'
		and p2.creation_date >= '2022-08-01'
		and ABS(UNIX_TIMESTAMP(p2.target_date) - UNIX_TIMESTAMP(p1.target_date)) < 60
		and DATE(p1.creation_date) < DATE(p2.creation_date)
		-- and not sh1.id_session = sh2.id_session 
		where  DATE(p1.target_date) = '2022-08-30'
;

DELETE prediction_item from prediction_item where id_prediction in (select id from TmpCleanPrediction)




SET @date_last = (SELECT MAX(date) FROM state_history WHERE location = '192.168.1.79:10001' AND scenario = 'MeyrinSimulator' AND date < '2022-08-30 15:42:10' AND id_session = '20221006_153323_3043')
;
INSERT INTO state_history(date,date_last,id_session,variable_name,location,scenario,state_idx,state_name,value,id_last) 
	values
	('2022-08-30 15:42:10',@date_last,'20221006_153323_3043','requested','192.168.1.79:10001','MeyrinSimulator',2,'S3','12136.53'
			,(SELECT lastSH.id FROM state_history AS lastSH WHERE lastSH.id_session = ''20221006_153323_3043'' AND lastSH.date = @date_last AND lastSH.variable_name = 'requested' LIMIT 0,1)
			)
	, ('2022-08-30 15:42:10',@date_last,'20221006_153323_3043','produced','192.168.1.79:10001','MeyrinSimulator',3,'S4','28260.0',(SELECT lastSH.id FROM state_history AS lastSH WHERE lastSH.id_session = ''20221006_153323_3043'' AND lastSH.date = @date_last AND lastSH.variable_name = 'produced' LIMIT 0,1)), ('2022-08-30 15:42:10',@date_last,'20221006_153323_3043','consumed','192.168.1.79:10001','MeyrinSimulator',2,'S3','12136.53',(SELECT lastSH.id FROM state_history AS lastSH WHERE lastSH.id_session = ''20221006_153323_3043'' AND lastSH.date = @date_last AND lastSH.variable_name = 'consumed' LIMIT 0,1)), ('2022-08-30 15:42:10',@date_last,'20221006_153323_3043','provided','192.168.1.79:10001','MeyrinSimulator',2,'S3','12136.53',(SELECT lastSH.id FROM state_history AS lastSH WHERE lastSH.id_session = ''20221006_153323_3043'' AND lastSH.date = @date_last AND lastSH.variable_name = 'provided' LIMIT 0,1)), ('2022-08-30 15:42:10',@date_last,'20221006_153323_3043','available','192.168.1.79:10001','MeyrinSimulator',2,'S3','15746.66',(SELECT lastSH.id FROM state_history AS lastSH WHERE lastSH.id_session = ''20221006_153323_3043'' AND lastSH.date = @date_last AND lastSH.variable_name = 'available' LIMIT 0,1)), ('2022-08-30 15:42:10',@date_last,'20221006_153323_3043','missing','192.168.1.79:10001','MeyrinSimulator',0,'S1','0.0',(SELECT lastSH.id FROM state_history AS lastSH WHERE lastSH.id_session = ''20221006_153323_3043'' AND lastSH.date = @date_last AND lastSH.variable_name = 'missing' LIMIT 0,1))
 ON DUPLICATE KEY UPDATE value = value
;
UPDATE state_history SET 
    state_idx_last =  (SELECT MAX(last.state_idx) FROM state_history AS last WHERE last.id = state_history.id_last)
   ,state_name_last =  (SELECT MAX(last.state_name) FROM state_history AS last WHERE last.id = state_history.id_last)
 WHERE id_session = '20221006_153323_3043' AND  date = '2022-08-30 15:42:10' AND NOT id_last IS NULL
;
UPDATE state_history SET 
date_next =  '2022-08-30 15:42:10'
 WHERE id_session = ''20221006_153323_3043'' AND  date = @date_last
;


SET @date_last = (SELECT MAX(date) FROM state_history WHERE location = '192.168.1.79:10001' AND scenario = 'MeyrinSimulator' AND date < '2022-08-30 15:08:03' AND id_session = '20221006_105523_4970')
;
INSERT INTO state_history(date,date_last,id_session,variable_name,location,scenario,state_idx,state_name,value,id_last) VALUES 
;
DELETE state_history FROM state_history WHERE  date BETWEEN '2022-08-30 14:48:33' AND DATE_ADD('2022-08-30 14:48:33', INTERVAL 1 HOUR) AND NOT id_session = '20221006_105523_4970'
;


select * from prediction p where initial_date  between  '2022-08-30 12:41:19.000' and DATE_ADD(  '2022-08-30 12:41:19.000', INTERVAL 1 HOUR)
;
select count(*) from state_history sh 
;
select DATE(creation_date ), count(*) from state_history sh where variable_name = 'produced' and hour(date) = 10 and date like '2022-08-30%'
	group by DATE(creation_date )

select * FROM prediction p
		-- WHERE p.target_date  >= p_min_date AND p.target_date < p_max_date AND id_target_state_histo IS NULL AND NOT link_done
		WHERE p.creation_date  >= @min_date 
			AND p.creation_date < @max_date 
		--	AND p.location = '192.168.1.79:10001'
		--	AND p.scenario = 'MeyrinSimulator'
			AND id_target_state_histo IS NULL AND NOT link_done
;









;

select * from TmpPrediction

update prediction set link_done = 0 where creation_day = '2022-10-07' and id_target_state_histo is null

select * from prediction p where variable_name ='produced' order by id desc


CALL SP_COMPUTE_STATE_DISTRIBUTION('192.168.1.79:10001','MeyrinSimulator','2022-10-07','2022-10-07',NULL,NULL,'produced')
;

SELECT * FROM TmpStateDistribution
;
CALL SP_COMPUTE_PREDICTION_STATISTICS(1,'2022-10-07','2022-10-07',NULL,NULL,NULL,NULL,NULL,'produced')
;
select * from TmpPredictionStatistic
;


select * from prediction p  where p.creation_date  >= '2022-10-07' and variable_name = 'produced'

select current_timestamp(), current_date();



select * from prediction p where creation_date >= '2022-10-04' and variable_name = 'produced' order by id desc
;
SELECT variable_name, compute_day, target_day
	,GROUP_CONCAT(DISTINCT time_slot ORDER BY time_slot) AS time_slot
	,GROUP_CONCAT(DISTINCT horizon ORDER BY horizon) AS horizon
	,GROUP_CONCAT(DISTINCT If(use_corrections, 'True', 'False') ORDER BY use_corrections) AS use_corrections
	,DATE_ADD(target_day, INTERVAL (MIN(time_slot)+0) HOUR) AS date_begin
	,DATE_ADD(target_day, INTERVAL (MAX(time_slot)+1) HOUR) AS date_end
	,AVG(rate_ok1)  AS rate_ok1
	,AVG(rate_ok2)  AS rate_ok2
	,AVG(vector_differential) AS vector_differential
	,SUM(nb_ok2)  AS nb_ok2
   ,SUM(nb_total) AS nb_total
   ,SUM(corrections_number) AS corrections_number
	,AVG(proba1)  AS proba_avg1
	,AVG(proba2)  AS proba_avg2
	,AVG(gini_index) AS gini_index
	,AVG(shannon_entropie) AS shannon_entropie
	,Count(*) as nb_results
	,(SELECT ID from transition_matrix tm WHERE tm.variable_name = TmpPredictionStatistic.variable_name AND id_time_window = TmpPredictionStatistic.id_initial_time_window 
				AND tm.scenario = TmpPredictionStatistic.scenario and tm.location = TmpPredictionStatistic.location) AS id_tm
	FROM TmpPredictionStatistic
	GROUP BY compute_day, target_day, variable_name,use_corrections,time_slot
	ORDER BY date_begin, variable_name, horizon
;
select * from TmpPredictionStatistic
;
SELECT compute_day, target_day, hour, variable_name, state_name
	,GROUP_CONCAT(DISTINCT horizon ORDER BY horizon) AS horizon
	,GROUP_CONCAT(DISTINCT If(use_corrections, 'True', 'False') ORDER BY use_corrections) AS use_corrections
	,ROUND(AVG(proba),5) AS proba
  FROM TmpPredictionStatisticLine
  GROUP BY compute_day, target_day, hour, variable_name, state_name,use_corrections


alter table history drop constraint unicity_date
alter table history add constraint UNIQUE KEY `unicity_date_loc` (`date`, `localization`)
alter table history drop column agent_url

alter table  link_history_active_event add   `locatin` 	VARCHAR(32) NOT NULL DEFAULT '' after agent


alter table history add `total_provided` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'total provided (KWH)' after total_produced


select * from link_event_agent lea where agent_name = 'Prod_N1_1'

select prod.* 
	,(select SUM(link_event_agent.power) 
			from link_event_agent where link_event_agent.agent_name = prod.agent 
	) as provided
	from event as prod
	-- left join link_event_agent on link_event_agent.agent_name = prod.agent 
	where	prod.type='PRODUCTION' and prod.distance = 0

select * from link_event_agent where id_event = 837417

select * from link_history_active_event lhae 

select * from event e 
select * from TmpEvent

DE
grant DROP  ON energy1.* TO 'learning_agent'@'%';

select * from link_history_active_event where is_request 


 
 select * from event e where agent = 'Consumer_N1_1'

select * from event where not cancel_date is null
 
	select * from event e where type='PRODUCTION_UPDATE'
	
	select * from history h where date like '% 12:33:01'
	896889
	CONS : 21486.170
	PROD : 32122.020


	

	

CREATE TEMPORARY TABLE TmpCorrectLast AS
	SELECT id, id_last, id_last_toset FROM
		( 
		SELECT id, id_last,
			 	(SELECT h2.ID FROM history h2 WHERE h2.date < h.date AND h2.location = h.location
					ORDER BY h2.date DESC LIMIT 0,1) AS id_last_toset
	 			FROM history h
				WHERE h.date > '2000-01-01' AND location = '192.168.1.79:10001' -- AND id_session = '20220622_164715_8038'
	 	) AS TmpRecentHistory
		WHERE NOT TmpRecentHistory.id_last = TmpRecentHistory.id_last_toset
	
	
	select * from event where agent = 'Prod_N1_1'
		
	select * from history h_current
		join link_history_active_event as lhae_current on lhae_current.id_history = h_current.id
		join link_history_active_event as lhae_last on lhae_last.id = lhae_current.id_last
		where not  lhae_last.id_history = h_current.id_last
	
	select * from link_history_active_event where id_event  = 842162
	
	
	UPDATE link_history_active_event AS current
						SET current.id_last = (SELECT (last.id) FROM link_history_active_event AS last WHERE last.id_history=@foo AND last.id_event = current.id_event_origin)
					  WHERE current.id_history = @new_id_histo AND current.id_last IS NULL
	
	select lhae.* 
		,lastEvent.*
		,history.id_last
		--	,(SELECT (last.id) FROM link_history_active_event AS last WHERE last.id_history=" + idLast2 +" AND last.id_event = TmpEvent.id) AS id_last2
		,(SELECT (last.id) FROM link_history_active_event AS last WHERE last.id_history=history.id_last AND last.id_event = lhae.id_event_origin) AS id_last2
		from link_history_active_event lhae
		join history on history.id = lhae.id_history 
		left join event as lastEvent on lastEvent.id  = lhae.id_event_origin
		where lhae.id_last  is null and lhae.type like '%UPDATE'
		and (SELECT (last.id) FROM link_history_active_event AS last WHERE last.id_history=history.id_last AND last.id_event = lhae.id_event_origin) is null
		and lhae.is_request 
		
		-- and not lhae.agent = lastEvent.agent
	
	select * from link_history_active_event lhae where  id_history = 896889 order by agent, type
	
		select  SUM(consumed), SUM(provided) from link_history_active_event lhae where  id_history = 896889 
select SUM(power) from link_history_active_event lhae where  id_history = 896889  and is_contract 
	
	837417
	
select * from transition_matrix
	
alter table transition_matrix CHANGE  `scope` `location` VARCHAR(32) NOT NULL DEFAULT '';

alter table prediction CHANGE  `scope` `location` VARCHAR(32) NOT NULL DEFAULT '';

alter table state_history CHANGE  `scope` `location` VARCHAR(32) NOT NULL DEFAULT '';

update transition_matrix set  location = '192.168.1.79:10001';
update prediction set  location = '192.168.1.79:10001';
update state_history set  location = '192.168.1.79:10001';

select * from prediction p2  where initial_date >='2021-09-15'







select * from transition_matrix_cell_iteration tmci
 join transition_matrix tm  on tm.id = tmci.id_transition_matrix 
 join transition_matrix_iteration tmi  on tmi.id = tmci.id_transition_matrix_iteration 
where creation_time >='2021-09-16' and tm.location = '192.168.1.79:10001'

select * from transition_matrix_cell tmc
 join transition_matrix tm  on tm.id = tmc.id_transition_matrix 
where  tm.location = '192.168.1.79:10001'


CALL REFRESH_TRANSITION_MATRIX_CELL('192.168.1.79:10001', 'test', 100)


DELIMITER §

DROP TEMPORARY TABLE IF EXISTS TmpTrMatrix
§
CREATE TEMPORARY TABLE TmpTrMatrix AS
SELECT transition_matrix.id
, transition_matrix.variable_name
, transition_matrix.location
, transition_matrix.scenario
, transition_matrix.id_time_window
, transition_matrix.iteration_number
, GET_ITERATION_ID(transition_matrix.id, '2021-09-16 10:54:06' )  AS id_transition_matrix_iteration
 FROM transition_matrix 
 WHERE transition_matrix.id_time_window IN (1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19)
 	AND variable_name IN ('requested','produced','consumed','available','missing')
 	AND location = '192.168.1.79:10001'
 	AND scenario = 'HomeSimulator1'
§
SELECT TmpTrMatrix.*
	,(TmpTrMatrix.id_transition_matrix_iteration IS NULL) AS IsNewIteration
	,cell.*
	,IFNULL(cellIt.obs_number,0) AS obs_number_iter
 FROM TmpTrMatrix 
 JOIN transition_matrix_cell AS cell ON cell.id_transition_matrix = TmpTrMatrix.id
 LEFT JOIN transition_matrix_cell_iteration  AS cellIt ON cellIt.id_transition_matrix_iteration = TmpTrMatrix.id_transition_matrix_iteration
 			AND cellIt.row_idx = cell.row_idx AND cellIt.column_idx = cell.column_idx
 WHERE 1
 ORDER BY TmpTrMatrix.ID
 
 
 
 
 
 
 
 
 select * from prediction where initial_date >= '2021-09-17 15:00' and horizon_minutes < 60
 
 
 SELECT horizon_minutes AS horizon
	,variable_name , count(*)
	,location
	,scenario
 	,SUM(is_ok) AS nb_ok
 	,SUM(is_ok) / SUM(1) AS rate_ok
 	from (
		 select p.* 
	 		,sh.`date`	 		
	 		,sh.state_idx 
	 		, sh.state_name 	 	
	 		, (p.random_state_idx = sh.state_idx) as is_ok
	 		, ABS(CONVERT(p.random_state_idx , signed)   -  CONVERT(sh.state_idx, signed) ) as delta_abs
	 		, (CONVERT(p.random_state_idx , signed)   -  CONVERT(sh.state_idx, signed) ) as delta
	 	FROM prediction p
	 	JOIN state_history AS sh ON sh.date<=p.target_date  AND sh.date_next > p.target_date
	 	 		AND sh.variable_name  = p.variable_name
	 	 		AND sh.location  = p.location AND sh.scenario  = p.scenario
	 	WHERE p.initial_date >='2021-09-16 00:00'  AND p.horizon_minutes = 5
 	) AS result
 	GROUP BY variable_name , horizon
 
 
 select  h.*, abs(total_consumed - total_provided) as delta from history h where abs(total_consumed - total_provided) >= 0.01 
 
 
select * from link_event_agent lea where id_event = '428277' -- //agent_name = 'Consumer_N1_27'
 
 SELECT ctr.*  ,(SELECT IFNULL(sum(link2.power),0) FROM link_event_agent AS link2 		
 	WHERE link2.id_event = ctr.id_contract_evt and link2.agent_type = 'Producer') AS provided FROM TmpEvent AS ctr  WHERE ctr.is_selected_local AND ctr.is_contract
 
 


select * from link_event_agent 
	where id_event=427788
	
	
	select id_event, agent_type , group_concat(agent_name), SUM(power) from link_event_agent group by id_event , agent_type 

	SELECT FOO.* from (
		select id_event, agent_type , agent_name, power as consumed
		, (select sum(link2.power) from link_event_agent as link2 where link2.id_event = link_event_agent.id_event and link2.agent_type = 'Producer') as provided
		from link_event_agent 
		where agent_type = 'Consumer'
		) as FOO 
		WHERE not consumed = provided 
		
		SELECT ctr.*  
			,(SELECT sum(link2.power) FROM link_event_agent AS link2 		
			 WHERE link2.id_event = ctr.id_event and link2.agent_type = 'Producer') AS provided 
		FROM TmpEvent AS ctr  WHERE ctr.is_selected_local AND ctr.is_contract
	
agent_name='Contract_N1_22'

SELECT IFNULL(SUM(lea.power),0) AS provided FROM link_event_agent AS lea WHERE lea.id_event = '428441' AND lea.agent_type = 'Producer' 


select * from event e 


SELECT '2021-09-20 09:29:38' AS date 
,IFNULL(SUM(TmpEvent.power),0) AS sum_all
,IFNULL(SUM(IF(TmpEvent.is_request, TmpEvent.power,0.0)),0) AS total_requested
,IFNULL(SUM(IF(TmpEvent.is_producer, TmpEvent.power,0.0)),0) AS total_produced
,IFNULL(SUM(IF(TmpEvent.is_producer, TmpEvent.provided,0.0)),0) AS total_provided
,IFNULL(SUM(IF(TmpEvent.is_contract, TmpEvent.power,0.0)),0) AS total_consumed
	 FROM TmpEvent WHERE is_selected_local
 
	 
	 select * from event e where agent = 'Contract_N1_93'

	  select * from event e where agent = 'Consumer_N1_93'
	 
	 
 select * from single_offer so  where not power = round(power,2) 
select * from event e  where not power = round(power,2) 
 
select h2.*,  abs(total_provided - total_consumed) as gap
  from history h2
	where abs( total_provided - total_consumed) > 0.99 

	
	select * from history h 
	
	-- 430231
	select * from event e  where agent = 'Consumer_N1_59'
	select * from event e  where agent = 'Contract_N1_59'

	
select * from event e  where distance >0 


select * from event e  where `type` = 'CONTRACT_STOP'


select *  from link_event_agent



	 
	 
	 
	 
select * from TmpEvent where provided2 like '%93%'
	
	
-- select * FROM link_event_agent
§
SELECT '2021-09-20 19:11:43' AS date 
,IFNULL(SUM(TmpEvent.power),0) AS sum_all
,IFNULL(SUM(IF(TmpEvent.is_request, TmpEvent.power,0.0)),0) AS total_requested
,IFNULL(SUM(IF(TmpEvent.is_producer, TmpEvent.power,0.0)),0) AS total_produced
,IFNULL(SUM(IF(TmpEvent.is_producer, TmpEvent.provided,0.0)),0) AS total_provided
,IFNULL(SUM(IF(TmpEvent.is_contract, TmpEvent.power,0.0)),0) AS total_consumed
	 FROM TmpEvent WHERE is_selected_location

	 
	 
	 
	 

§
UPDATE TmpEvent 
	JOIN TmpContractEvent ON TmpContractEvent.consumer = TmpEvent.agent
	SET TmpEvent.id_contract_evt = TmpContractEvent.id 
	WHERE TmpEvent.is_selected AND TmpEvent.is_request
§
UPDATE TmpEvent SET provided = (SELECT IFNULL(SUM(lea.power),0) 
   		FROM link_event_agent AS lea 
    	JOIN TmpContractEvent ON TmpContractEvent.id = lea.id_event
		WHERE lea.agent_name = TmpEvent.agent)
	WHERE TmpEvent.is_selected_location AND TmpEvent.is_producer
§
SELECT '2021-09-20 17:47:27' AS date 
,IFNULL(SUM(TmpEvent.power),0) AS sum_all
,IFNULL(SUM(IF(TmpEvent.is_request, TmpEvent.power,0.0)),0) AS total_requested
,IFNULL(SUM(IF(TmpEvent.is_producer, TmpEvent.power,0.0)),0) AS total_produced
,IFNULL(SUM(IF(TmpEvent.is_producer, TmpEvent.provided,0.0)),0) AS total_provided
,IFNULL(SUM(IF(TmpEvent.is_contract, TmpEvent.power,0.0)),0) AS total_consumed
	 FROM TmpEvent WHERE is_selected_location
;
select @ut_date : 1658595600

SET @ut_date = UNIX_TIMESTAMP('2022-07-23 19:00:00')
;
SELECT sh.date , UNIX_TIMESTAMP(sh.date), ABS(@ut_date - UNIX_TIMESTAMP(sh.date)) as delta
FROM state_history sh 
JOIN state_history AS last ON last.id = sh.id_last
WHERE sh.location = '192.168.1.79:10001' AND sh.scenario = 'MeyrinSimulator' AND sh.variable_name = 'requested'
	AND UNIX_TIMESTAMP(sh.date) >= @ut_date - 300 
	AND UNIX_TIMESTAMP(sh.date) <= @ut_date + 300
ORDER BY ABS(@ut_date - UNIX_TIMESTAMP(sh.date)), sh.date 
LIMIT 0,1


SELECT sh.variable_name, sh.value, last.value AS value_last 
FROM state_history sh 
JOIN state_history AS last ON last.id = sh.id_last
WHERE sh.location = '192.168.1.79:10001' AND sh.scenario = 'MeyrinSimulator'
 AND sh.date = '2022-07-23 19:00:19'
 AND sh.variable_name IN ('requested','produced','consumed','provided','available','missing')