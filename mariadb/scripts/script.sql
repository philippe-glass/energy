
delete state_history from state_history where TIMEDIFF(date_next,date) > '05:00'

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
	 	WHERE p.initial_date >='2021-09-22 00:00'  AND p.horizon_minutes = 5
 	) AS result
 	GROUP BY variable_name , horizon


CALL CORRECT_OBS_NUMBERS(@start_hour, 'available', 3 , 2  , 1);
CALL CORRECT_OBS_NUMBERS(@start_hour, 'available', 3 , 3  , 2);
CALL CORRECT_OBS_NUMBERS(@start_hour, 'available', 3 ,4  , 1);
-- CALL CORRECT_OBS_NUMBERS(@start_hour, 'available', 5 , 6  , 1);
CALL REFRESH_TRANSITION_MATRIX_CELL2('HOME', 'HomeSimulator1', NULL, 100)

set @start_hour=21;
CALL CORRECT_OBS_NUMBERS(@start_hour, 'produced', 6 , 5  , 10);
CALL CORRECT_OBS_NUMBERS(@start_hour, 'requested', 6 , 5  , 10);
CALL CORRECT_OBS_NUMBERS(@start_hour, 'available', 6 , 5  , 10);
CALL CORRECT_OBS_NUMBERS(@start_hour, 'consumed', 6 , 5  , 10);
CALL CORRECT_OBS_NUMBERS(@start_hour, 'missing', 6 , 5  , 10);
CALL REFRESH_TRANSITION_MATRIX_CELL2('HOME', 'HomeSimulator1', NULL, 100)
