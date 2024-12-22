select prod.* 
	,(select SUM(link_event_agent.power) 
			from link_event_agent where link_event_agent.agent_name = prod.agent 
	) as provided
	from event as prod
	-- left join link_event_agent on link_event_agent.agent_name = prod.agent 
	where	prod.type='PRODUCTION' and prod.distance = 0
	
	
	select * from history
	
	
	select * from link_event_agent
	
alter table history drop constraint unicity_date
alter table history add constraint UNIQUE KEY `unicity_date_loc` (`date`, `localization`)


update transition_matrix  set scenario  = REPLACE(scenario, 'HomeSimulator1', 'HomeSimulator')

 
select * from history h where not state_idx_available  is null

select * from prediction p order by id desc

select * from state_history sh order by id desc

SELECT MAX(date) FROM state_history WHERE location = '192.168.1.79:10002' AND scenario = 'HomeSimulator' AND date < '2021-09-22 05:07:17'

SELECT MAX(date) FROM state_history


select * from device d where category = 'ICT'
select * from time_window where start_hour = 17
select * from transition_matrix tm where id_time_window =13 and variable_name ='produced'
select * from transition_matrix_cell_iteration tmci where id_transition_matrix = 400 and row_idx = 2

call REFRESH_TRANSITION_MATRIX_CELL2('192.168.1.79:10002', 'HomeSimulator', NULL, 14)



SET @date_last = (SELECT MAX(date) FROM state_history WHERE location = '192.168.1.79:10002' AND scenario = 'HomeSimulator' AND date < '2021-09-22 05:07:17')
;

delete state_history FROM state_history  where date_next is null and date<  '2021-09-22 00:00'

where date_next is null and date<  '2021-09-22 00:00'

select date,date_next,date_next-date , TIMEDIFF(date_next,date) from state_history sh where TIMEDIFF(date_next,date) > '02:00'


delete state_history from state_history where TIMEDIFF(date_next,date) > '05:00'

select * from state_history sh  order by id desc 
select * from prediction p where initial_date >='2021-09-22 00:00'  AND p.horizon_minutes = 5

SET @date_last = (SELECT MAX(date) FROM state_history WHERE location = '192.168.1.79:10002' AND scenario = 'HomeSimulator' AND date < '2021-09-22 05:19:40')
;

select state_history.* ,
	 (SELECT next_sh.date FROM state_history as next_sh
     WHERE next_sh.date > state_history.date AND next_sh.variable_name  = state_history.variable_name
    	AND next_sh.location  = state_history.location AND next_sh.scenario  = state_history.scenario
 	ORDER BY next_sh.date LIMIT 0,1) as date_next
	from state_history  where state_history.date >= @date_last AND date < '2021-09-22 05:19:40'



SET @date_last = (SELECT MAX(date) FROM state_history WHERE location = '192.168.1.79:10002' AND scenario = 'HomeSimulator' AND date < '2021-09-22 05:19:40')
;
UPDATE state_history SET 
date_next = (SELECT next_sh.date FROM state_history as next_sh
     WHERE next_sh.date > state_history.date AND next_sh.variable_name  = state_history.variable_name
    	AND next_sh.location  = state_history.location AND next_sh.scenario  = state_history.scenario
 	ORDER BY next_sh.date LIMIT 0,1)
 WHERE (state_history.date  >=DATE_ADD(@date_last, INTERVAL -10 MINUTE) or date_next IS null)  AND date < '2021-09-22 05:19:40'
 
 select * from state_history sh where variable_name = 'produced' order by id desc 
alter table state_history  add  `value`	DECIMAL(10,5) NOT NULL DEFAULT 0.0  after date_next

  select * from state_history where variable_name = 'produced' order by id desc 

 
 
 
alter table prediction drop column horizonMinutes

INSERT INTO prediction SET variable_name='requested'
	, location ='192.168.1.79:10002', scenario ='HomeSimulator', initial_date='2021-09-22 05:52:05', target_date='2021-09-22 05:57:05'
	, horizon_minutes=5, learning_window=100, random_state_idx=3, random_state_name='S4'

select * from prediction p  where variable_name = 'produced' order by id desc

select * from state_history   where variable_name = 'produced' order by id DESC


select * from history h2  order by id desc



select * from state_history sh where variable_name = 'provided' order by id desc

select device_category ,  12*power from device_statistic where start_hour  = 5 AND device_category    like '%_ENG'

select 12*SUM(power) from device_statistic  where start_hour  = 5 AND device_category    like '%_ENG'
select start_hour, 12*SUM(power) from device_statistic  where   device_category    like '%_ENG' group by start_hour 

select initial_date , hour(initial_date) from prediction p order by id desc


select * from time_window tw 




select * from state_history  where 1 order by id desc

select * from state_history  where variable_name = 'provided' and creation_date >='2021-09-25' order by id desc
select * from state_history  where variable_name = 'missing' and creation_date >='2021-09-25' order by id desc
select * from state_history  where variable_name = 'available' and creation_date >='2021-09-25' order by id desc

select * from prediction p
	join prediction_item pi2  on pi2.id_prediction = p.id
	where hour(initial_date) > 12 and creation_date >='2021-09-25'


SELECT horizon_minutes AS horizon
	,location
	,scenario
	,variable_name 
	, Date(initial_date) as date
	, hour(initial_date) as time_slot
	, count(*) as nb_total
 	,SUM(is_ok) AS nb_ok
 	,SUM(is_ok) / SUM(1) AS rate_ok
 	,min(creation_date)
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
	 			and TIMEDIFF(date_next,date) <= '01:30'
	 	 		AND sh.variable_name  = p.variable_name
	 	 		AND sh.location  = p.location AND sh.scenario  = p.scenario
	 	 		and sh.creation_date >= '2021-10-01 00:00'
	 	 		-- and hour(sh.creation_date) = hour(p.creation_date)
	 	 		-- and hour(initial_date) < 13
	 	WHERE p.initial_date >='2021-10-01 00:00'
	 		 AND p.horizon_minutes = 5
	 		and p.creation_date >= '2021-10-01 00:00'
	 		--  and p.variable_name = 'produced'
 	) AS result
 	GROUP BY variable_name , horizon, Date(initial_date), hour(initial_date) -- ,  hour(creation_date)
 	having nb_total >= 20
 	order by rate_OK DESC


SELECT * FROM transition_matrix_cell_iteration
	join transition_matrix_iteration tmi on tmi.id = transition_matrix_cell_iteration.id_transition_matrix_iteration 
	join transition_matrix tm  on tm.id = transition_matrix_cell_iteration.id_transition_matrix 
	where tm.scenario = 'HomeSimulator'


select *  FROM transition_matrix_cell_iteration
	join transition_matrix_iteration tmi on tmi.id = transition_matrix_cell_iteration.id_transition_matrix_iteration 
	join transition_matrix tm  on tm.id = transition_matrix_cell_iteration.id_transition_matrix 
	where tm.scenario = 'HomeSimulator'

select * from transition_matrix tm where scenario = 'HomeSimulator'

 select * from prediction where initial_date >= '2021-09-22 00:00' -- and horizon_minutes < 60
 
 select * from device d where is_producer 
 
 
 

 	
 	
 	
 	
 	select * from history h2 where distance=0 order by id desc

 	
 	
 	
 	
 	
DELIMITER §
DROP TEMPORARY TABLE IF EXISTS TmpEvent
§
CREATE TEMPORARY TABLE TmpEvent(effective_end_date datetime
	, interruption_date datetime
	, is_selected	bit(1) NOT NULL DEFAULT b'0'
	, id_contract_evt INT(11) NULL
	, provided DECIMAL(15,2) NOT NULL DEFAULT 0.0
	) AS
	SELECT ID ,begin_date
		,expiry_date  as effective_end_date
		,type,agent,location,power
		,type IN ('REQUEST', 'REQUEST_UPDATE') AS is_request
		,type IN ('PRODUCTION', 'PRODUCTION_UPDATE') AS is_producer
		,type IN ('CONTRACT', 'CONTRACT_UPDATE') AS is_contract
		,0 AS is_selected
		,NULL AS id_contract_evt
		,0.0 AS provided
	FROM event
	WHERE NOT event.is_ending AND IFNULL(event.cancel_date,'3000-01-01') > '2021-09-17 16:01:49'
    AND event.distance=0
§
UPDATE TmpEvent SET interruption_date = (SELECT interruption.begin_date
		FROM event AS interruption WHERE interruption.id_origin = TmpEvent.id AND interruption.is_cancel
		ORDER BY interruption.begin_date  LIMIT 0,1)
§
	UPDATE TmpEvent SET effective_end_date = LEAST(effective_end_date , interruption_date) WHERE not interruption_date IS NULL
§
	UPDATE TmpEvent SET is_selected = 1 WHERE begin_date<='2021-09-17 16:01:49' AND effective_end_date > '2021-09-17 16:01:49'
§
DROP TEMPORARY TABLE IF EXISTS TmpContractEvent
§
CREATE TEMPORARY TABLE TmpContractEvent AS
 SELECT TmpEvent.id, consumer.agent_name AS consumer
 FROM TmpEvent 
 JOIN link_event_agent AS consumer ON consumer.id_event = TmpEvent.id AND consumer.prosumer_role='CONSUMER'
  WHERE is_selected AND is_contract
§
ALTER TABLE TmpContractEvent ADD KEY (consumer)
§
UPDATE TmpEvent 
 JOIN TmpContractEvent ON TmpContractEvent.consumer = TmpEvent.agent
 SET TmpEvent.id_contract_evt = TmpContractEvent.id 
 WHERE TmpEvent.is_selected AND TmpEvent.is_request
§
UPDATE TmpEvent SET provided = (SELECT IFNULL(SUM(lea.power),0) 
   	FROM link_event_agent AS lea
   	join TmpContractEvent on TmpContractEvent.id = lea.id_event 
   	WHERE lea.agent_name = TmpEvent.agent)
	WHERE TmpEvent.is_producer
§
select * from TmpEvent where is_producer and is_selected

select * from link_event_agent

select * from history h order by id desc

SELECT '2021-09-17 16:01:49' AS date 
,IFNULL(SUM(TmpEvent.power),0) 										AS sum_all
,IFNULL(SUM(IF(TmpEvent.is_request, TmpEvent.power,0.0)),0) AS total_requested
,IFNULL(SUM(IF(TmpEvent.is_producer, TmpEvent.power,0.0)),0) AS total_produced
,IFNULL(SUM(IF(TmpEvent.is_producer, TmpEvent.provided,0.0)),0) AS total_provided
,IFNULL(SUM(IF(TmpEvent.is_contract, TmpEvent.power,0.0)),0) AS total_consumed
	 FROM TmpEvent WHERE is_selected