
DELIMITER §
DROP TEMPORARY TABLE IF EXISTS TmpEvent
§
CREATE TEMPORARY TABLE TmpEvent(
	 effective_end_date 	DATETIME
	,interruption_date 		DATETIME
	,is_selected 			BIT(1) NOT NULL DEFAULT b'0'
	,is_selected_location	BIT(1) NOT NULL DEFAULT b'0'
	,id_contract_evt 		INT(11) NULL
	,power					DECIMAL(15,3) NOT NULL DEFAULT 0.0
	,provided 				DECIMAL(15,3) NOT NULL DEFAULT 0.0
	,provided2				TEXT NULL
	) AS
	SELECT ID, begin_date
		,expiry_date 		AS effective_end_date
		,type,agent,location,power,distance
		,type IN ('REQUEST', 'REQUEST_UPDATE') 			AS is_request
		,type IN ('PRODUCTION', 'PRODUCTION_UPDATE') 	AS is_producer
		,type IN ('CONTRACT', 'CONTRACT_UPDATE') 		AS is_contract
		,0 					AS is_selected
		,0 					AS is_selected_location
		,NULL 				AS id_contract_evt
		,0.0 				AS provided
 		,location='192.168.1.79:10001' 	AS is_location_ok
	FROM event
	WHERE NOT event.is_ending AND IFNULL(event.cancel_date,'3000-01-01') > '2021-09-20 19:11:43'
		-- and (event.`type`IN ('PRODUCTION', 'PRODUCTION_UPDATE') or event.agent like '%93')
	    --  and (NOT event.`type`IN ('PRODUCTION', 'PRODUCTION_UPDATE') or event.agent IN ('Prod_N1_3', 'Prod_N1_5'))
§
UPDATE TmpEvent SET interruption_date = (SELECT interruption.begin_date
		FROM event AS interruption WHERE interruption.id_origin = TmpEvent.id AND interruption.is_cancel
		ORDER BY interruption.begin_date  LIMIT 0,1)
§
	UPDATE TmpEvent SET effective_end_date = LEAST(effective_end_date , interruption_date) WHERE NOT interruption_date IS NULL
§
	UPDATE TmpEvent SET is_selected = 1 WHERE begin_date<='2021-09-20 19:11:43' AND effective_end_date > '2021-09-20 19:11:43'
§
	UPDATE TmpEvent SET is_selected_location = is_selected AND is_location_ok
§
DROP TEMPORARY TABLE IF EXISTS TmpRequestEvent
§
CREATE TEMPORARY TABLE TmpRequestEvent AS
  SELECT TmpEvent.id, TmpEvent.agent AS consumer, power, is_location_ok
	  FROM TmpEvent 
  WHERE is_selected AND is_request
§
ALTER TABLE TmpRequestEvent ADD KEY (consumer)
§
DROP TEMPORARY TABLE IF EXISTS TmpContractEvent
§
CREATE TEMPORARY TABLE TmpContractEvent AS
 	SELECT TmpEvent.id, consumer.agent_name AS consumer, TmpEvent.is_location_ok
 	FROM TmpEvent 
 	JOIN link_event_agent AS consumer ON consumer.id_event = TmpEvent.id AND consumer.prosumer_role='CONSUMER
 	JOIN TmpRequestEvent ON TmpRequestEvent.consumer = consumer.agent_name
  	WHERE is_selected AND is_contract
§
update tmpevent 
	left join TmpContractEvent on TmpContractEvent.id = tmpevent.ID
	set tmpevent.is_selected_location = 0
	where tmpevent.is_selected AND is_contract  and TmpContractEvent.id is NULL
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
    	JOIN TmpContractEvent ON TmpContractEvent.id = lea.id_event
		WHERE lea.agent_name = TmpEvent.agent and  TmpContractEvent.consumer like '%')
	WHERE TmpEvent.is_selected_location AND TmpEvent.is_producer
§
UPDATE TmpEvent SET provided2 = (SELECT IFNULL(group_concat(TmpContractEvent.consumer),'') 
   		FROM link_event_agent AS lea 
    	JOIN TmpContractEvent ON TmpContractEvent.id = lea.id_event
		WHERE lea.agent_name = TmpEvent.agent)
	WHERE TmpEvent.is_selected_location AND TmpEvent.is_producer
§


SELECT '2021-09-20 19:11:43' AS date 
,IFNULL(SUM(TmpEvent.power),0) AS sum_all
,IFNULL(SUM(IF(TmpEvent.is_request, TmpEvent.power,0.0)),0) AS total_requested
,IFNULL(SUM(IF(TmpEvent.is_producer, TmpEvent.power,0.0)),0) AS total_produced
,IFNULL(SUM(IF(TmpEvent.is_producer, TmpEvent.provided,0.0)),0) AS total_provided
,IFNULL(SUM(IF(TmpEvent.is_contract and  TmpEvent.agent like '%', TmpEvent.power,0.0)),0) AS total_consumed
	 FROM TmpEvent WHERE is_selected_location

	 
	 §
	 
	 
	 select * from event where agent  like  'Consumer_N1_33'
§
select * from TmpEvent where agent like '%33'

§