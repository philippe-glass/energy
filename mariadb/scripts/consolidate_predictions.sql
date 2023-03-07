use energy1
;
CALL SP_CONSOLIDATE_PREDICTIONS('192.168.1.79:10001', 'MeyrinSimulator','2022-09-01', date_add(NOW() , INTERVAL 1 DAY))
;


CALL SP_COMPUTE_PREDICTION_STATISTICS('192.168.1.79:10001', 'MeyrinSimulator', '2022-09-08 00:00',NULL,NULL,NULL,NULL,NULL)
;
SELECT * from TmpPredictionStatistic where has_states_distrib > 0
;



CALL SP_COMPUTE_STATE_DISTRIBUTION('192.168.1.79:10001', 'MeyrinSimulator', '2022-09-01 00:00',null,NULL)
;
UPDATE prediction p SET p.vector_differential = COMPUTE_VECTOR_DIFFERENTIAL(p.id) WHERE p.target_date >='2022-09-01 00:00'
		-- AND (HOUR(p.target_date) >= '2022-09-01 00:00' OR 0)
	   -- AND (HOUR(p.target_date) <= 0+p_max_hour OR p_max_hour IS NULL)
	    -- AND (p.variable_name = p_variable_name OR p_variable_name IS NULL)
	     AND p.target_date <= DATE_ADD(NOW(), interval -1 HOUR)
	    AND p.vector_differential IS NULL
	    AND NOT id_target_state_histo IS NULL
;


call SP_COMPUTE_STATE_DISTRIBUTION('192.168.1.79:10001', 'MeyrinSimulator', '2022-08-30', '2022-08-30', 'produced')
;


 CALL SP_COMPUTE_STATE_DISTRIBUTION('192.168.1.79:10001','MeyrinSimulator','2022-07-01 02:00:00',NULL,NULL)
 ;
SELECT * FROM TmpStateDistribution where date = '2022-07-26' and hour=7
;



select * from TmpPredictionStatisticLine
;
select * from TmpStateDistrib1 where variable_name = 'produced' and date='2022-08-30' and hour=17
;

select * from prediction p
	join prediction_item pi2 on pi2.id_prediction = p.id
	where p.variable_name = 'produced' and Date(target_date) = '2022-08-30' and hour(target_date) = 17
		and p.horizon_minutes = 5 and use_corrections = 0 -- and state_idx=4
;

select * from TmpStateDistrib1
;
select p.*
	,pi2.*
	,ifnull(TmpStateDistrib1.ratio,0) as ratio
	,(pi2.proba - ifnull(TmpStateDistrib1.ratio,0)) as delta
	,ABS(pi2.proba - ifnull(TmpStateDistrib1.ratio,0)) as delta2
	from prediction p
	join prediction_item pi2 on pi2.id_prediction = p.id
	left join TmpStateDistrib1 on TmpStateDistrib1.date = DATE(p.target_date)
		and TmpStateDistrib1.hour = hour(p.target_date)
		and TmpStateDistrib1.variable_name = p.variable_name
		and TmpStateDistrib1.state_idx = pi2.state_idx
	where Date(target_date) = '2022-08-30'
		-- p.variable_name = 'produced' and  and hour(target_date) = 17  and p.horizon_minutes = 5  and p.use_corrections = 0 -- and pi2.state_idx=4
;


SELECT * from TmpPredictionStatistic where not list_id_correction is null
;
-- select 0.52*0.48*2, -1*(0.52*log2(0.52) + 0.48*log2(0.48))

-- select * from TmpStateDistribution where date = '2022-08-03' and hour=13 and variable_name='produced'
SELECT * FROM TmpGroupedStates WHERE variable_name='produced'
;

 drop temporary table if exists TmpPredictionDistribution
 ;
 create temporary table TmpPredictionDistribution
	 select select3.hour
	 	, select3.target_date as date
	 	, select3.variable_name
		, group_concat(Label, '  '  order by nb DESC) as likely_state_distribution
		, SUM(nb) as total_nb
		, STDDEV(select3.likely_state_idx)* 100 / SUM(nb) as likely_state_std_deviation
		-- , SUM(ratio) as check_sum
		from
			(
				select select1.hour
					,select1.target_date
					,select1.variable_name
					,select1.likely_state_name
					,select1.likely_state_idx
					,select1.nb
					,select2.total_nb
					,select1.nb/select2.total_nb as ratio
					,CONCAT(likely_state_name, ':', ROUND(nb/total_nb,2)) as Label
					FROM
							(
								select hour(target_date)  as hour, date(target_date) as target_date, likely_state_name , likely_state_idx, variable_name, count(*) as nb
								from prediction p where scenario = 'MeyrinSimulator' and location='192.168.1.79:10001'  and p.id_target_state_histo  > 0
									and p.horizon_minutes = 10
								group by hour, date(target_date), variable_name, likely_state_name
								order by hour, nb DESC
					) as select1
					JOIN
							(
								select hour(target_date)  as hour, date(target_date) as target_date, variable_name, count(*) as total_nb
								from prediction p where scenario = 'MeyrinSimulator' and location='192.168.1.79:10001'  and p.id_target_state_histo  > 0
									and p.horizon_minutes = 10
								group by hour, date(target_date), variable_name
					) as select2
					ON select2.hour = select1.hour and select2.target_date = select1.target_date and select2.variable_name = select1.variable_name
			) AS select3
		group by select3.hour, select3.target_date, select3.variable_name
;


select * from TmpPredictionDistribution
;

 select * from TmpPredictionStatistic
 	left join TmpStateDistribution on TmpStateDistribution.variable_name = TmpPredictionStatistic.variable_name
 		and TmpStateDistribution.hour = TmpPredictionStatistic.time_slot
 		and TmpStateDistribution.date = TmpPredictionStatistic.date
--  left join TmpPredictionDistribution on TmpPredictionDistribution.variable_name = TmpPredictionStatistic.variable_name
-- 	and TmpPredictionDistribution.hour = TmpPredictionStatistic.time_slot
-- 	and TmpPredictionDistribution.date = TmpPredictionStatistic.date
 where TmpPredictionStatistic.date>='2022-08-02' and TmpPredictionStatistic.variable_name in ('produced', 'available')
 order by TmpPredictionStatistic.rate_OK2 DESC
 ;




select * from state_history sh
	join prediction p on p.id_target_state_histo = sh.id and p.horizon_minutes = 5
	where sh.variable_name = 'produced' and not sh.observation_update is null and DATE(sh.date) = '2022-07-23' and HOUR(sh.date) = 19
;

select * from TmpPredictionStatistic where variable_name = 'produced' and date = '2022-07-23' and time_slot = 19 and horizon = 10
  ;
SELECT p.*
		    ,sh.`date`
		    ,sh.state_idx
		    ,sh.state_name
		    ,(p.random_state_idx = sh.state_idx) as is_ok1
		    ,(p.likely_state_idx = sh.state_idx) as is_ok2
		    ,p.random_state_proba as proba_random
		    ,p.likely_state_proba as proba_likely
		    ,p.likely_state_name
		    ,sh.state_name
		    ,ABS(CONVERT(p.random_state_idx , signed)   -  CONVERT(sh.state_idx, signed) ) as delta_abs
		    ,(CONVERT(p.random_state_idx , signed)   -  CONVERT(sh.state_idx, signed) ) as delta
		 FROM prediction p
		 JOIN state_history AS sh ON sh.id = p.id_target_state_histo
		 WHERE p.initial_date >='2022-07-01 00:00' and p.variable_name = 'produced' and Date(p.target_date)='2022-07-23' and hour(p.target_date) = 19 and p.horizon_minutes  = 10
		 	and 0
;

 -- select * from TmpPredictionStatistic

/*
 -- The Best
 select location, scenario, variable_name, horizon
 	 ,(select rate_ok1 from TmpPredictionStatistic order by rate_ok1 DESC, nb_total DESC  limit 0,1) as rate_ok1
	 ,(select nb_total from TmpPredictionStatistic order by rate_ok1 desc, nb_total DESC  limit 0,1) as nb_total
	 ,(select date from TmpPredictionStatistic order by rate_ok1 desc, nb_total DESC  limit 0,1) as date
	 ,(select time_slot from TmpPredictionStatistic order by rate_ok1 desc, nb_total DESC  limit 0,1) as time_slot2
	 ,(select proba1 from TmpPredictionStatistic order by rate_ok1 desc, nb_total DESC  limit 0,1) as proba1
	from TmpPredictionStatistic group by location, scenario,  horizon, variable_name;

  -- The Worst
  select location, scenario, variable_name, horizon
 	 ,(select rate_ok1 from TmpPredictionStatistic order by rate_ok1 , nb_total DESC  limit 0,1) as rate_ok1
  	 ,(select rate_ok2 from TmpPredictionStatistic order by rate_ok1 , nb_total DESC  limit 0,1) as rate_ok2
	 ,(select nb_total from TmpPredictionStatistic order by rate_ok1 , nb_total DESC  limit 0,1) as nb_total
	  ,(select date from TmpPredictionStatistic order by rate_ok1 desc, nb_total DESC  limit 0,1) as date
	 ,(select time_slot from TmpPredictionStatistic order by rate_ok1 desc, nb_total DESC  limit 0,1) as time_slot
	  ,(select proba1 from TmpPredictionStatistic order by rate_ok1 desc, nb_total DESC  limit 0,1) as proba1
	from TmpPredictionStatistic group by location, scenario,  horizon, variable_name;
;
*/

  select location, scenario, variable_name, horizon, time_slot
 		,avg(rate_ok1)  as rate_ok1
 		,avg(rate_ok2)  as rate_ok2
 		,AVG(shannon_entropie)  as shannon_entropie
 		,avg(proba1)  as proba1
		 ,SUM(nb_total) as nb_total
		from TmpPredictionStatistic
	group by location, scenario,  horizon, variable_name, time_slot
	order by rate_ok2 desc
;
-- Reuslt by time slot
select time_slot, variable_name
 		,avg(rate_ok1)  as rate_ok1
 		,avg(rate_ok2)  as rate_ok2
		,SUM(nb_total) as nb_total
		,AVG(shannon_entropie)  as shannon_entropie
		from TmpPredictionStatistic
		where variable_name = 'available'
	group by  time_slot, variable_name
	order by rate_ok1 desc
;
-- Reuslt by time horizon
select horizon
 		,AVG(rate_ok1)  as rate_ok1
 		,AVG(rate_ok2)  as rate_ok2
 		,AVG(shannon_entropie)  as shannon_entropie
		,SUM(nb_total) as nb_total
		from TmpPredictionStatistic
	group by  horizon
	order by rate_ok1 desc
;
-- Reuslt by  variable_name
select variable_name
 		,AVG(rate_ok1)  as rate_ok1
 		,AVG(rate_ok2)  as rate_ok2
		,SUM(nb_total) as nb_total
		,AVG(shannon_entropie)  as shannon_entropie
		,AVG(proba1)  as proba1
		,AVG(proba2)  as proba2
		from TmpPredictionStatistic
	group by  variable_name	
	order by rate_ok1 desc
;

-- Reuslt by date
select date
 		,AVG(rate_ok1)  as rate_ok1
 		,AVG(rate_ok2)  as rate_ok2
		,SUM(nb_total) as nb_total
		,AVG(shannon_entropie) as shannon_entropie
		,AVG(proba1)  as proba_avg1
		,AVG(proba2)  as proba_avg2
		,GROUP_CONCAT(distinct time_slot) as timeSlots
		from TmpPredictionStatistic
		where 1
		-- where time_slot in (6,7,8,9,10,11,12,13,14,15,16,17)
	group by  date	
	order by rate_ok2 desc
;


select variable_name
 		,AVG(rate_ok1)  as rate_ok1
 		,AVG(rate_ok2)  as rate_ok2
		,SUM(nb_total) as nb_total
		,AVG(proba1)  as proba_avg1
		from TmpPredictionStatistic
		where time_slot=10
	group by  variable_name	
	order by rate_ok1 desc
;

-- TODO Reuslt by time variable_name AND time slot AND Date
select variable_name, time_slot, horizon
		,Date(TmpPredictionStatistic.date) as date1
 		,AVG(rate_ok1)  as rate_ok1
  		,AVG(rate_ok2)  as rate_ok2
	    ,SUM(nb_total) as nb_total
		,AVG(proba1)  as proba_avg1
		,AVG(proba2)  as proba_avg2
		,AVG(gini_index) as gini_index
		,AVG(shannon_entropie) as shannon_entropie
		,Count(*) as nb_results
		 ,(select ID from transition_matrix tm WHERE tm.variable_name = TmpPredictionStatistic.variable_name and id_time_window = TmpPredictionStatistic.id_initial_time_window 
				AND tm.scenario = TmpPredictionStatistic.scenario and tm.location = TmpPredictionStatistic.location) as id_tm
		 --  ,(select COMPUTE_STATE_VARIANCE(tm.id) from transition_matrix tm WHERE tm.variable_name = TmpPredictionStatistic.variable_name and id_time_window = TmpPredictionStatistic.id_initial_time_window 
		 --		AND tm.scenario = TmpPredictionStatistic.scenario and tm.location = TmpPredictionStatistic.location) as state_variance_tm
		from TmpPredictionStatistic
		where date>='2022-08-08'
	group by  variable_name, time_slot, date1, horizon
	--  having id_tm in (178667,171965,179243,221849,221851)
	having rate_ok2 < 0.5
	order by rate_ok2, shannon_entropie DESC
;

select date_add(date, interval time_slot HOUR)
	,date_add(date, interval (time_slot+1) HOUR)
	, date, time_slot from TmpPredictionStatistic where date>='2022-08-08'  -- GROUP by horizon
;



select * from TmpPredictionStatistic where shannon_entropie > 0 order by  rate_ok2 DESC
;

select * from prediction p 
	join state_history sh on sh.id = p.id_target_state_histo 
	left join log_self_correction lsc on lsc.id_prediction = p.id
		where p.target_date >= '2022-08-02 09:00' and p.target_date < '2022-08-02 10:00'  and p.variable_name = 'available'
		and p.horizon_minutes = 30
	order by p.target_date 
;
-- id_transition_matrix of result < 50% : 178667,171965,179243,221849,221851


-- select * from prediction p

-- 7-9-10-12

select * from transition_matrix tm
	join time_window tw on tw.id = tm.id_time_window 
	where tm.id in (178667,171965,179243,221849,221851)
;


SELECT horizon_minutes AS horizon
	,location
	,scenario
	,variable_name
	,Date(initial_date) as date
	,hour(initial_date) as time_slot
	,id_initial_time_window
	,count(*) as nb_total
	,SUM(is_ok1) AS nb_ok1
	,SUM(is_ok2) AS nb_ok2
	,SUM(is_ok1) / SUM(1) AS rate_ok1
	,SUM(is_ok2) / SUM(1) AS rate_ok2
	,min(creation_date) as creation_date
	,AVG(proba_random) as proba1
	,AVG(proba_likely) as proba2
	FROM (
		select  p.*
				    ,sh.`date`
				    ,sh.state_idx
				    ,sh.state_name
				    ,(p.random_state_idx = sh.state_idx) as is_ok1
				    ,(p.likely_state_idx = sh.state_idx) as is_ok2
				    ,p.random_state_proba as proba_random
				    ,p.likely_state_proba as proba_likely
				    ,ABS(CONVERT(p.random_state_idx , signed)   -  CONVERT(sh.state_idx, signed) ) as delta_abs
				    ,(CONVERT(p.random_state_idx , signed)   -  CONVERT(sh.state_idx, signed) ) as delta
			from transition_matrix tm
			-- join time_window tw on tw.id = tm.id_time_window 
			join prediction p on tm.variable_name = p.variable_name
						and tm.id_time_window = p.id_initial_time_window 
						AND tm.scenario = p.scenario 
						and tm.location = p.location
			JOIN state_history AS sh ON sh.id = p.id_target_state_histo
			WHERE  0 -- tm.id in (178667,171965,179243,221849,221851)
				-- and tm.variable_name in ( 'produced', 'available') and tm.id_time_window = 8
				and tm.variable_name in (  'produced') -- and tm.id_time_window = 8
				and p.initial_date >='2022-07-20 00:00'
				-- and hour(initial_date)=7
    ) AS result
    GROUP BY variable_name 
    -- , horizon
    , Date(initial_date), hour(initial_date) -- ,  hour(creation_date)
    HAVING nb_total >= 50
    ORDER BY rate_OK2 desc
 ;
 select * from state_history sh order by id desc
 ;
 select * from prediction p order by id desc
 ;

-- 7,9,10,12
select Count(*), tm.id, tw.start_hour 
	from transition_matrix tm
	join time_window tw on tw.id = tm.id_time_window 
	join prediction p on tm.variable_name = p.variable_name
				and tm.id_time_window = p.id_initial_time_window 
				AND tm.scenario = p.scenario 
				and tm.location = p.location
	-- join prediction_item pi on pi.id_prediction = p.ID
	where tm.id in (178667,171965,179243,221849,221851)
	group by tm.id
;


-- select * from transition_matrix_cell_iteration tmci where id_transition_matrix in (178667,171965,179243,221849,221851)
-- delete  transition_matrix_cell_iteration from transition_matrix_cell_iteration where id_transition_matrix in (178667,171965,179243,221849,221851)


/*
-- delete p from transition_matrix tm
	-- join time_window tw on tw.id = tm.id_time_window 
	join prediction p on tm.variable_name = p.variable_name
				and tm.id_time_window = p.id_initial_time_window 
				AND tm.scenario = p.scenario 
				and tm.location = p.location
	-- join prediction_item pi on pi.id_prediction = p.ID
	where tm.id in (178667,171965,179243,221849,221851)
;
*/

select * from time_window;
select * from transition_matrix tm WHERE variable_name = 'available' and id_time_window = 8 AND scenario = 'MeyrinSimulator' and location = '192.168.1.79:10001';
select * from transition_matrix_cell tmc where id_transition_matrix = 171965;
(select column_idx from transition_matrix_cell tmc where  id_transition_matrix = 171965 and row_idx=0 order by obs_number desc 	);



-- select  h.*, abs(total_consumed - total_provided) as delta from history h where abs(total_consumed - total_provided) >= 0.01 ;

