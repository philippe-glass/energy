alter table event add `interruption_date`	DATETIME DEFAULT null after cancel_date;

  
alter table history add    `max_warning_duration` INT(10) unsigned NOT NULL DEFAULT 0 COMMENT 'max warning duration of requests';

  

alter table single_offer ADD `contract_time`  		DATETIME null after acceptance_time;
alter table single_offer add log_cancel TEXT NOT NULL DEFAULT '' after log2;

alter table history add  `max_warning_duration` INT(10) unsigned NOT NULL DEFAULT 0 COMMENT 'max warning duration of requests';
alter table history add   `max_warning_consumer`	VARCHAR(100) null;


alter table history drop constraint unicity_date;
alter table history add constraint UNIQUE KEY `unicity_date_loc` (`date`, `localization`);
alter table history drop column agent_url;

alter table  link_history_active_event add   `locatin` 	VARCHAR(32) NOT NULL DEFAULT '' after agent;


alter table history add `total_provided` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'total provided (KWH)' after total_produced;alter table event add `interruption_date`	DATETIME DEFAULT null after cancel_date;

  
alter table history add    `max_warning_duration` INT(10) unsigned NOT NULL DEFAULT 0 COMMENT 'max warning duration of requests';

  

alter table single_offer ADD `contract_time`  		DATETIME null after acceptance_time;
alter table single_offer add log_cancel TEXT NOT NULL DEFAULT '' after log2;

alter table history add  `max_warning_duration` INT(10) unsigned NOT NULL DEFAULT 0 COMMENT 'max warning duration of requests';
alter table history add   `max_warning_consumer`	VARCHAR(100) null;


alter table history drop constraint unicity_date;
alter table history add constraint UNIQUE KEY `unicity_date_loc` (`date`, `localization`);
alter table history drop column agent_url;

alter table  link_history_active_event add   `locatin` 	VARCHAR(32) NOT NULL DEFAULT '' after agent;


alter table history add `total_provided` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'total provided (KWH)' after total_produced;

-- JUNE 2022

 alter table event drop column environmental_impact;
 alter table event add `environmental_impact`  TINYINT UNSIGNED NOT NULL DEFAULT 0 after device_category ;
 
 alter table event add interruption_date DATETIME DEFAULT null after cancel_date;
 
 alter table device drop column environmental_impact;
 alter table device add `environmental_impact`  TINYINT UNSIGNED NOT NULL DEFAULT 0 after category;
 

-- select * from single_offer so  where id=241833 order by id desc
 
 
 -- 09/06/2022
 -- use energy3

 
ALTER table  history drop column `state_idx_produced`;
ALTER table  history drop column `state_idx_requested`;
ALTER table  history drop column `state_idx_consumed`;
ALTER table  history drop column `state_idx_available`;
ALTER table  history drop column `state_idx_missing`;
 
 alter table event add   `power_min`			DECIMAL(15,3) NOT null after power;
 alter table event add   `power_max`			DECIMAL(15,3) NOT null after power_min;

 alter table single_offer add  `power_min`			DECIMAL(15,3) NOT null after power;
 alter table single_offer add   `power_max`			DECIMAL(15,3) NOT null after power_min;

-- 10/06/2022
alter table history add `total_margin`		DECIMAL(15,3) NOT NULL DEFAULT 0.00 COMMENT 'total used for contract margins (KWH)'  after total_consumed;

-- 17/06/2022
alter table  event add `is_complementary`	BIT(1) NOT NULL DEFAULT b'0' COMMENT 'to identify complementary contracts';

alter table single_offer add `is_complementary`	BIT(1) NOT NULL DEFAULT b'0' COMMENT 'to identify complementary contracts' after consumer_agent;

-- 20/06/2022
alter table event drop  KEY `unicity_1`;
alter table event add  UNIQUE KEY `unicity_1` (`begin_date`,`type`,`agent`, `is_complementary`);

-- 21/06/2022
alter table link_history_active_event add `consumed`		DECIMAL(15,3) NOT null AFTER power;
 alter table link_history_active_event add `missing`		DECIMAL(15,3) NOT null after consumed;
alter table link_history_active_event  add `is_complementary`	BIT(1) NOT NULL DEFAULT b'0' COMMENT 'to identify complementary contracts' after is_contract;
alter table link_history_active_event add  `provided`		DECIMAL(15,3) NOT null  AFTER power;

-- 22/06/2022
alter table event add  `object_type`			ENUM('PRODUCTION','REQUEST','CONTRACT') DEFAULT null after type;
alter table event add  `main_category`			ENUM('START', 'STOP',  'EXPIRY','UPDATE') DEFAULT null after object_type;

alter table event add `power_update` 			DECIMAL(15,3) NOT NULL DEFAULT 0.00 COMMENT 'changes since the last event' after power_max;
alter table event add `power_min_update` 		DECIMAL(15,3) NOT NULL DEFAULT 0.00 COMMENT 'changes since the last event' after power_update;
alter table event add `power_max_update` 		DECIMAL(15,3) NOT NULL DEFAULT 0.00 COMMENT 'changes since the last event' after power_min_update;

alter table link_history_active_event add `id_event_origin` INT(11) DEFAULT NULL after id_event;

-- 24/06/2022
alter table event modify  `type` 				ENUM('','PRODUCTION','REQUEST','CONTRACT'
  						,'PRODUCTION_START'	,'REQUEST_START'	,'CONTRACT_START'
					  	,'PRODUCTION_STOP'	,'REQUEST_STOP'		,'CONTRACT_STOP'
					  	,'PRODUCTION_EXPIRY','REQUEST_EXPIRY'	,'CONTRACT_EXPIRY'
					  	,'PRODUCTION_UPDATE', 'REQUEST_UPDATE'	,'CONTRACT_UPDATE') DEFAULT NULL
;

alter table event add `comment`			TEXT NOT NULL DEFAULT '';

-- 29/06/2022
alter table history ADD  `min_request_missing` DECIMAL(15,3) NOT NULL DEFAULT 0.00 COMMENT 'min of non satisfied request in KWH' after total_missing;

-- 30/06/2022
alter table transition_matrix_cell_iteration add `creation_time` DATETIME DEFAULT current_timestamp();

alter table  state_history add `id_session` 			VARCHAR(32) NOT NULL after creation_date;

alter table state_history  add `date_last`				DATETIME  NULL after date;

alter table state_history add `observation_update`	DATETIME  NULL;

-- 01/07/2022

alter table state_history modify `value` DECIMAL(15,3) NOT NULL DEFAULT 0.0;


-- 04/07/2022
alter table state_history add UNIQUE KEY   `unicity_session_date_variable_name` ( `id_session` , `date`,`variable_name`);

alter table state_history add `id_last` INT(11) UNSIGNED null after state_name;
alter table state_history add 	`state_idx_last`		TINYINT UNSIGNED NULL after id_last;
alter table state_history add 	`state_name_last`		VARCHAR(32) NULL after state_idx_last;

alter table state_history add CONSTRAINT `fk_id_last` FOREIGN KEY (`id_last`) REFERENCES `state_history` (`id`);


-- 05/07/2022

-- alter table prediction add `id_tgt_state_histo`	INT(11) UNSIGNED null;
alter table prediction add `id_target_state_histo`	INT(11) UNSIGNED null;
alter table prediction add `delta_target_state_histo`	INT(11) SIGNED null;



-- 19/07/2022
ALTER TABLE prediction add `random_state_proba`	DECIMAL(10,5) NOT NULL DEFAULT 0.0 after random_state_name;
ALTER TABLE prediction_item RENAME COLUMN value TO proba;

-- 20/07/2022
alter table prediction ADD `initial_state_idx`	TINYINT UNSIGNED NOT NULL after target_date;
alter table prediction ADD `initial_state_name`	VARCHAR(32) DEFAULT '' AFTER initial_state_idx;
alter table prediction add `likely_state_idx`	TINYINT UNSIGNED NOT null after random_state_proba;
alter table prediction add `likely_state_name`	VARCHAR(32) DEFAULT '' after likely_state_idx;
alter table prediction add `likely_state_proba`	DECIMAL(10,5) DEFAULT 0.0 after likely_state_name;

alter table prediction add `id_initial_time_window`	INT(11) UNSIGNED NOT null after target_date;

-- 21/07-/2022
alter table prediction add link_done BIT default 0;

-- 26/07/2022
alter table transition_matrix_cell_iteration add `corrections_number` INT(11) NOT NULL DEFAULT 0.0 COMMENT 'Used for self-adaptive correction';
-- 27/07/2022
alter table transition_matrix_cell add `corrections_number` INT(11) NOT NULL DEFAULT 0.0  COMMENT 'Used for self-adaptive correction' after obs_number ;
;
alter table prediction add key _target_date(target_date);
;
alter table prediction add `id_initial_transition_matrix`	INT(11) UNSIGNED  null after initial_state_name;
;
alter table prediction add `id_target_transition_matrix`	INT(11) UNSIGNED  NULL after id_initial_transition_matrix
;

-- 10/08/2022
alter table prediction add `use_corrections`			BIT default 0 after target_date
;
-- 12/08/2022
-- TODO : create table log_self_correction
alter table prediction add `id_correction`				INT(11) UNSIGNED null
;
alter table prediction add CONSTRAINT `fk_id_correction` FOREIGN KEY (`id_correction`) REFERENCES `log_self_correction` (`id`);
;

-- 01/09/2022
alter table prediction add 	`vector_differential`	DECIMAL(10,5) NULL COMMENT 'Comparison between the prediction vector and the actual state distribution'
;
-- 07/09-2022
alter table prediction drop key _variable_name;
alter table prediction add KEY _target_date_varname(target_date, variable_name);


alter table prediction add target_day DATE null;
alter table prediction add target_hour tinyint null;


update prediction set target_day = DATE(target_date);
update prediction set target_hour = HOUR(target_date);

-- alter table prediction add key _target_day(target_day) 
-- alter table prediction add key _target_day_hour(target_day, target_hour) 
-- alter table prediction add key _target_day_hour_var(target_day, target_hour, variable_name) 

alter table prediction add key _target_day_hour_var2(target_day, target_hour, variable_name, horizon_minutes , use_corrections);
-- alter table prediction  drop key _target_day_hour_var

alter table	prediction_item add unique key (id_prediction, state_idx);


-- 30/09/2022
alter table history add  `creation_time` 		DATETIME DEFAULT current_timestamp() after id;

alter table history add  `time_shift_ms`		BIGINT(15) SIGNED NOT NULL DEFAULT 0 after date;
-- alter table history modify time_shift_ms BIGINT(15) SIGNED NOT NULL DEFAULT 0

alter table event add `time_shift_ms` 			BIGINT(15) SIGNED NOT NULL DEFAULT 0 after creation_time;
-- alter table event modify time_shift_ms BIGINT(15) SIGNED NOT NULL DEFAULT 0

alter table single_offer add `time_shift_ms` 	BIGINT(15) SIGNED NOT NULL DEFAULT 0 after creation_time;
-- alter table single_offer modify time_shift_ms BIGINT(15) SIGNED NOT NULL DEFAULT 0


alter table single_offer add `date` 				DATETIME NOT NULL DEFAULT current_timestamp() after creation_time;

-- 05/10/2022
alter table prediction add creation_day DATE NOT NULL DEFAULT current_date() after creation_date;
-- update prediction  set creation_day = DATE(creation_date);
alter table prediction add KEY _creation_day(creation_day);


-- 06/10/2022
alter table prediction add key _initial_date(initial_date) ;

alter table prediction add key _id_target_state_histo(id_target_state_histo);

alter table state_history add key _id_last(id_last);

alter table prediction add KEY _target_day(target_day);

-- 07/10/2022
alter table prediction add  `id_context` INT(11) UNSIGNED null after creation_day;
alter table transition_matrix add id_context 	INT(11) UNSIGNED null after id;
alter table state_history  add id_context 	INT(11) UNSIGNED null after creation_date;

alter table prediction add CONSTRAINT `fk_id_context` FOREIGN KEY (`id_context`) REFERENCES `context` (`id`);
alter table transition_matrix add CONSTRAINT `fk_id_context2` FOREIGN KEY (`id_context`) REFERENCES `context` (`id`);
alter table state_history add CONSTRAINT `fk_id_context3` FOREIGN KEY (`id_context`) REFERENCES `context` (`id`);

alter table state_history drop constraint unicity_session_date_variable_name;
alter table state_history drop constraint unicity_date_variable_name;
alter table state_history ADD constraint UNIQUE KEY `unicity_date_variable_name` (`id_context`, `date`, `variable_name`);



-- 16/02/2023

ALTER TABLE link_event_agent add `power_min` 		DECIMAL(15,3) NOT NULL DEFAULT 0.00;
ALTER TABLE link_event_agent add `power_max` 		DECIMAL(15,3) NOT NULL DEFAULT 0.00;

-- 19/04/2023
-- create table node_config

ALTER TABLE history ADD id_node_config INT(11) unsigned COMMENT 'Node config' after id_session;
ALTER TABLE event ADD id_node_config INT(11) unsigned COMMENT 'Node config' after id_session;
ALTER TABLE link_event_agent add agent_id_node_config INT(11) unsigned COMMENT 'Node config' after agent_location;

-- 24/04/2023 :
ALTER TABLE history add CONSTRAINT `_histo_node_config` FOREIGN KEY (`id_node_config`) REFERENCES `node_config` (`id`);
ALTER TABLE event add CONSTRAINT `_event_node_config` FOREIGN KEY (`id_node_config`) REFERENCES `node_config` (`id`);
ALTER TABLE link_event_agent add CONSTRAINT `_link_event_agent_node_config` FOREIGN KEY (`agent_id_node_config`) REFERENCES `node_config` (`id`);

-- TODO : link_history_active_event, scenario, transition_matrix, prediction, state_history

-- 01/05/2023
-- create table context_neighbour

-- use energy4;
ALTER TABLE context ADD id_node_config  INT(11) UNSIGNED COMMENT 'Node config' after location;
ALTER TABLE context ADD CONSTRAINT `_context_node_config` FOREIGN KEY (`id_node_config`) REFERENCES `node_config` (`id`);
