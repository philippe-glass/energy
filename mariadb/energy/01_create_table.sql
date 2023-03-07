DELIMITER §

-- use energy4
-- §
-- energy.history definition
CREATE TABLE `history` (
  `id` 					INT(10) unsigned NOT NULL AUTO_INCREMENT,
  `creation_time` 		DATETIME DEFAULT current_timestamp(),
  `date` 				DATETIME NOT NULL DEFAULT current_timestamp(),
  `time_shift_ms`		BIGINT(15) SIGNED NOT NULL DEFAULT 0,
  `id_session` 			VARCHAR(32) NOT NULL,
  `learning_agent` 		VARCHAR(100) NOT NULL,
  `location` 			VARCHAR(32) NOT NULL DEFAULT '',
  `distance` 			TINYINT UNSIGNED NOT NULL DEFAULT 0.0,
  `id_last` 			INT(10) unsigned DEFAULT NULL,
  `id_next` 			INT(10) unsigned DEFAULT NULL,
  `total_produced` 		DECIMAL(15,3) NOT NULL DEFAULT 0.00 COMMENT 'total produced (KWH)',
  `total_provided` 		DECIMAL(15,3) NOT NULL DEFAULT 0.00 COMMENT 'total provided (KWH)',
  `total_requested` 	DECIMAL(15,3) NOT NULL DEFAULT 0.00 COMMENT 'total needed for consumption (KWH)',
  `total_consumed` 		DECIMAL(15,3) NOT NULL DEFAULT 0.00 COMMENT 'total consumed (KWH)',
  `total_margin`		DECIMAL(15,3) NOT NULL DEFAULT 0.00 COMMENT 'total used for contract margins (KWH)',
  `total_available` 	DECIMAL(15,3) NOT NULL DEFAULT 0.00 COMMENT 'total produced - total consumed (KWH)',
  `total_missing` 		DECIMAL(15,3) NOT NULL DEFAULT 0.00 COMMENT 'total requested and not provided (KWH)',
  `min_request_missing` DECIMAL(15,3) NOT NULL DEFAULT 0.00 COMMENT 'min of non satisfied request in KWH',
  `max_warning_duration` INT(10) unsigned NOT NULL DEFAULT 0 COMMENT 'max warning duration of requests',
  `max_warning_consumer`	VARCHAR(100) NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unicity_date_loc` (`date`, `location`),
  CONSTRAINT `link_id_last` FOREIGN KEY (`id_last`) REFERENCES `history` (`id`),
  CONSTRAINT `link_id_next` FOREIGN KEY (`id_next`) REFERENCES `history` (`id`),
  KEY _id_session(`id_session`),
  KEY idx_date (date)
) ENGINE=InnoDB AUTO_INCREMENT=9298 DEFAULT CHARSET=utf8 COMMENT='History of energy production/consumption in a house'
§

-- energy.event definition
CREATE TABLE `event` (
  `id` 					INT(11) NOT NULL AUTO_INCREMENT,
  `creation_time` 		DATETIME DEFAULT current_timestamp(),
  `time_shift_ms`		BIGINT(15) SIGNED NOT NULL DEFAULT 0,
  `id_session` 			VARCHAR(32) NOT NULL,
  `id_histo` 			INT(10) unsigned NULL,
  `type` 				ENUM('','PRODUCTION','REQUEST','CONTRACT'
					  	,'PRODUCTION_START'	,'REQUEST_START'	,'CONTRACT_START'
					  	,'PRODUCTION_STOP'	,'REQUEST_STOP'		,'CONTRACT_STOP'
					  	,'PRODUCTION_EXPIRY','REQUEST_EXPIRY'	,'CONTRACT_EXPIRY'
					  	,'PRODUCTION_UPDATE', 'REQUEST_UPDATE'	,'CONTRACT_UPDATE') DEFAULT NULL,
  `object_type`			ENUM('PRODUCTION','REQUEST','CONTRACT') DEFAULT NULL,
  `main_category`		ENUM('START', 'STOP',  'EXPIRY','UPDATE') DEFAULT NULL,
  `warning_type` 		VARCHAR(32) NOT NULL DEFAULT '',
  `agent` 				VARCHAR(100) NOT NULL,
  `is_complementary`	BIT(1) NOT NULL DEFAULT b'0' COMMENT 'to identify complementary contracts',
  `location` 			VARCHAR(32) NOT NULL DEFAULT '',
  `distance` 			TINYINT UNSIGNED NOT NULL DEFAULT 0.0,
  `device_name` 		VARCHAR(100) NOT NULL DEFAULT '',
  `device_category` 	VARCHAR(100) NOT NULL DEFAULT '',
  `environmental_impact`  TINYINT UNSIGNED NOT NULL DEFAULT 0,
  `begin_date` 			DATETIME NOT NULL DEFAULT current_timestamp(),
  `expiry_date` 		DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00',
  `cancel_date` 		DATETIME DEFAULT NULL,
  `interruption_date`	DATETIME DEFAULT NULL,
  `duration` 			DECIMAL(15,3) NOT NULL DEFAULT 0.00,
  `power` 				DECIMAL(15,3) NOT NULL,
  `power_min`			DECIMAL(15,3) NOT NULL,
  `power_max`			DECIMAL(15,3) NOT NULL,
  `power_update` 		DECIMAL(15,3) NOT NULL DEFAULT 0.00 COMMENT 'changes since the last event' ,
  `power_min_update` 	DECIMAL(15,3) NOT NULL DEFAULT 0.00 COMMENT 'changes since the last event' ,
  `power_max_update` DECIMAL(15,3) NOT NULL DEFAULT 0.00 COMMENT 'changes since the last event' ,
  `id_origin` 			INT(11) DEFAULT NULL,
  `is_cancel` 			BIT(1) NOT NULL DEFAULT b'0',
  `is_ending` 			BIT(1) NOT NULL DEFAULT b'0',
   `comment`			TEXT NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  CONSTRAINT `link_id_histo` FOREIGN KEY (`id_histo`) REFERENCES `history` (`id`),
  UNIQUE KEY `unicity_1` (`begin_date`,`type`,`agent`, `is_complementary`),
  KEY `idx_begin_date` (begin_date),
  KEY `idx_expiry_date` (expiry_date),
  KEY `idx_cancel_date` (cancel_date),
  KEY `idx_agent` (agent),
  KEY `id_origin` (id_origin)
) ENGINE=InnoDB AUTO_INCREMENT=9637 DEFAULT CHARSET=utf8
§





-- energy.link_event_agent definition

CREATE TABLE `link_event_agent` (
  `id` 				INT(11) NOT NULL AUTO_INCREMENT,
  `id_event` 		INT(11) DEFAULT NULL,
  `agent_type` 		VARCHAR(100) NOT NULL,
  `agent_name` 		VARCHAR(100) NOT NULL,
  `agent_location` 	VARCHAR(32) NOT NULL DEFAULT '',
  `power` 			DECIMAL(15,3) NOT NULL DEFAULT 0.00,
  `power_min` 		DECIMAL(15,3) NOT NULL DEFAULT 0.00,
  `power_max` 		DECIMAL(15,3) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (`id`),
  KEY `event_id` (`id_event`),
  CONSTRAINT `link_event_agent_ibfk_1` FOREIGN KEY (`id_event`) REFERENCES `event` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=746 DEFAULT CHARSET=utf8


§
CREATE TABLE `link_history_active_event` (
  `id` 				INT(11) unsigned NOT NULL AUTO_INCREMENT,
  `id_history`		INT(10) unsigned  NULL,
  `id_event` 		INT(11) DEFAULT NULL,
  `id_event_origin` INT(11) DEFAULT NULL,
  `date` 			DATETIME NOT NULL DEFAULT current_timestamp(),
  `type` 			VARCHAR(100) NOT NULL,
  `agent` 			VARCHAR(100) NOT NULL,
  `location` 		VARCHAR(32) NOT NULL DEFAULT '',
  `power` 			DECIMAL(15,3) NOT NULL,
  `provided`		DECIMAL(15,3) NOT NULL COMMENT '(For producers)',
  `consumed`		DECIMAL(15,3) NOT NULL COMMENT '(For consumers)',
  `missing`			DECIMAL(15,3) NOT NULL COMMENT '(For consumers)',
  `is_request` 		BIT(1) NOT NULL DEFAULT b'0',
  `is_producer` 	BIT(1) NOT NULL DEFAULT b'0',
  `is_contract` 	BIT(1) NOT NULL DEFAULT b'0',
  `is_complementary` BIT(1) NOT NULL DEFAULT b'0' COMMENT 'to identify complementary contracts',
  `id_contract_evt` INT(11) NULL COMMENT 'Not to be used because a consumer can have several contracts',
  `id_last` 		INT(10) UNSIGNED NULL DEFAULT NULL,
  `warning_duration` INT(10) UNSIGNED NOT NULL DEFAULT 0,
  `has_warning_req` BIT(1) NOT NULL DEFAULT b'0',
   PRIMARY KEY (`id`)
  ,CONSTRAINT `history_active_evt_1` FOREIGN KEY (`id_event`) REFERENCES `event` (`id`)
  ,CONSTRAINT `history_active_evt_2` FOREIGN KEY (`id_history`) REFERENCES `history` (`id`)
  ,CONSTRAINT `history_active_evt_3` FOREIGN KEY (`id_contract_evt`) REFERENCES `event` (`id`)
  ,CONSTRAINT `_link_id_last` FOREIGN KEY (`id_last`) REFERENCES `link_history_active_event` (`id`)
  ,KEY `_location` (`location`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8



§
CREATE TABLE `single_offer` (
  `id` 					INT(11) NOT NULL AUTO_INCREMENT,
  `id_session` 			VARCHAR(32) NOT NULL,
  `creation_time` 		DATETIME DEFAULT current_timestamp(),
  `date` 				DATETIME NOT NULL DEFAULT current_timestamp(),
  `time_shift_ms` 		BIGINT(15) SIGNED NOT NULL DEFAULT 0 ,
  `id_history`			INT(10) UNSIGNED  NULL,
  `deadline` 			DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00',
  `producer_agent` 		VARCHAR(100) NOT NULL,
  `consumer_agent` 		VARCHAR(100) NOT NULL,
  `is_complementary`	BIT(1) NOT NULL DEFAULT b'0' COMMENT 'to identify complementary contracts',
  `production_event_id` INT(11)  NULL,
  `request_event_id` 	INT(11) NULL,
  `contract_event_id` 	INT(11) NULL,
  `power` 				DECIMAL(15,3) NOT NULL,
  `power_min`			DECIMAL(15,3) NOT NULL,
  `power_max`			DECIMAL(15,3) NOT NULL,
  `acquitted`  			BIT(1) NOT NULL DEFAULT b'0',
  `used`  				BIT(1) NOT NULL DEFAULT b'0',
  `used_time` 			DATETIME NULL,
  `accepted`  			BIT(1) NOT NULL DEFAULT b'0',
  `acceptance_time` 	DATETIME NULL,
  `contract_time`  		DATETIME NULL,
   `log` 				TEXT NOT NULL DEFAULT '',
   `log2` 				TEXT NOT NULL DEFAULT '',
   `log_cancel`			TEXT NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  CONSTRAINT `link_contract_event_id` FOREIGN KEY (`contract_event_id`) REFERENCES `event` (`id`),
  CONSTRAINT `link_production_event_id` FOREIGN KEY (`production_event_id`) REFERENCES `event` (`id`),
  CONSTRAINT `link_request_event_id` FOREIGN KEY (`request_event_id`) REFERENCES `event` (`id`),
  CONSTRAINT `link_sg_history` FOREIGN KEY (`id_history`) REFERENCES `history` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8


§
CREATE TABLE `context` (
	`id` 					INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	`creation_date` 		DATETIME NOT NULL DEFAULT current_timestamp(),
	`location` 				VARCHAR(32) NOT NULL DEFAULT '',
	`scenario`				VARCHAR(32) NOT NULL DEFAULT '',
	`last_id_session`		VARCHAR(32) NOT NULL,
	`last_time_shift_ms`	BIGINT(15) SIGNED NOT NULL DEFAULT 0,
	PRIMARY KEY (`id`),
	UNIQUE KEY `unicity_context` (`location`, `scenario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8


§
CREATE TABLE `time_window` (
	`id`				INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	`days_of_week`  	VARCHAR(32) DEFAULT '',
	`start_hour`		TINYINT UNSIGNED NOT NULL DEFAULT 0.0,
	`start_minute`		TINYINT UNSIGNED NOT NULL DEFAULT 0.0,
	`end_hour`			TINYINT UNSIGNED NOT NULL DEFAULT 0.0,
	`end_minute`		TINYINT UNSIGNED NOT NULL DEFAULT 0.0,
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8
§
CREATE TABLE `transition_matrix` (
	`id` 					INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	`id_context`			INT(11) UNSIGNED NULL,
	`id_time_window`		INT(11) UNSIGNED NOT NULL,
	`variable_name`			VARCHAR(100) NOT NULL,
	`location` 				VARCHAR(32) NOT NULL DEFAULT '',
	`scenario`				VARCHAR(32) NOT NULL DEFAULT '',
	`iteration_number`		INT(11) UNSIGNED NOT NULL DEFAULT 0,
	`learning_agent`		VARCHAR(100) NOT NULL,
	`last_update`			DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00',
	PRIMARY KEY (`id`),
	UNIQUE KEY `unicity_trmatrix` (`id_time_window`, `variable_name`, `location`, `scenario`),
	CONSTRAINT `time_window_fk` FOREIGN KEY (`id_time_window`) REFERENCES `time_window` (`id`),
	CONSTRAINT `fk_id_context2` FOREIGN KEY (`id_context`) REFERENCES `context` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8


§
CREATE TABLE `transition_matrix_cell` (
	`id_transition_matrix` 			INT(11) UNSIGNED NOT NULL,
	`row_idx`						TINYINT UNSIGNED NOT NULL,
	`column_idx`					TINYINT UNSIGNED NOT NULL,
	`obs_number`					INT(11) UNSIGNED NOT NULL DEFAULT 0.0,
	`corrections_number`			INT(11) SIGNED NOT NULL DEFAULT 0.0 COMMENT 'Used for self-adaptive correction',
	`value`							DECIMAL(10,5) NOT NULL DEFAULT 0.0,
	`rowsum`						INT(11) NOT NULL DEFAULT 0.0,
	CONSTRAINT `link_transition_matrix_fk` FOREIGN KEY (`id_transition_matrix`) REFERENCES `transition_matrix` (`id`),
	UNIQUE KEY `unicity_1` (`id_transition_matrix`, `row_idx`,  `column_idx`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8


§
CREATE TABLE `transition_matrix_iteration` (
	`id` 						INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	`creation_time` 			DATETIME DEFAULT current_timestamp(),
	`id_time_window`			INT(11) UNSIGNED NOT NULL,
	`id_transition_matrix` 		INT(11) UNSIGNED NOT NULL,
	`number` 					INT(11) UNSIGNED NOT NULL DEFAULT 0,
	`begin_date`				DATETIME,
	`end_date`					DATETIME,
	`last_update`				DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00',
	PRIMARY KEY (`id`),
	UNIQUE KEY `unicity_1` (`id_transition_matrix`, `end_date`),
	CONSTRAINT `link_transition_matrix_fk2` FOREIGN KEY (`id_transition_matrix`) REFERENCES `transition_matrix` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8


§
CREATE TABLE `transition_matrix_cell_iteration` (
	`creation_time` 					DATETIME DEFAULT current_timestamp(),
	`id_transition_matrix_iteration` 	INT(11) UNSIGNED NOT NULL,
    `id_transition_matrix`	 			INT(11) UNSIGNED NOT NULL,
	`row_idx`							TINYINT UNSIGNED NOT NULL,
	`column_idx`						TINYINT UNSIGNED NOT NULL,
	`obs_number`						INT(11) UNSIGNED NOT NULL DEFAULT 0.0,
	`corrections_number`				INT(11) SIGNED NOT NULL DEFAULT 0.0 COMMENT 'Used for self-adaptive correction',
	CONSTRAINT `fk_transition_matrix_iteration` FOREIGN KEY (`id_transition_matrix_iteration`) REFERENCES `transition_matrix_iteration` (`id`),
	CONSTRAINT `fk3_transition_matrix` FOREIGN KEY (`id_transition_matrix`) REFERENCES `transition_matrix` (`id`),
	UNIQUE KEY `unicity_1_tmcellit` (`id_transition_matrix_iteration`, `row_idx`,  `column_idx`)
)

§
CREATE TABLE `prediction` (
	`id` 						INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	`creation_date` 			DATETIME NOT NULL DEFAULT current_timestamp(),
	`creation_day` 				DATE NOT NULL DEFAULT current_date(),
	`id_context`				INT(11) UNSIGNED NULL,
	-- `id_time_window`			INT(11) UNSIGNED NOT NULL,
	`variable_name`				VARCHAR(100) NOT NULL,
	`location` 					VARCHAR(32) NOT NULL DEFAULT '',
	`scenario`					VARCHAR(32) NOT NULL DEFAULT '',
	`initial_date`				DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00',
	`target_date`				DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00',
	`target_day` 				DATE null,
	`target_hour` 				TINYINT null,
	`use_corrections`			BIT default 0,
	`id_initial_time_window`	INT(11) UNSIGNED NOT NULL,
	`initial_state_idx`			TINYINT UNSIGNED NOT NULL,
	`initial_state_name`		VARCHAR(32) DEFAULT '',
	`id_initial_transition_matrix`	INT(11) UNSIGNED  NULL,
	`id_target_transition_matrix`	INT(11) UNSIGNED  NULL,
	`random_state_idx`			TINYINT UNSIGNED NOT NULL,
	`random_state_name`			VARCHAR(32) DEFAULT '',
	`random_state_proba`		DECIMAL(10,5) NOT NULL DEFAULT 0.0,
	`likely_state_idx`			TINYINT UNSIGNED NOT NULL,
	`likely_state_name`			VARCHAR(32) DEFAULT '',
	`likely_state_proba`		DECIMAL(10,5) DEFAULT 0.0,
	`learning_window`			INT(11) UNSIGNED NOT NULL,
	`horizon_minutes`			INT(11) UNSIGNED NOT NULL DEFAULT 0,
	`id_target_state_histo`		INT(11) UNSIGNED NULL,
	`delta_target_state_histo`	INT(11) SIGNED NULL,
	`link_done`					BIT default 0,
	`id_correction`				INT(11) UNSIGNED NULL,
	`vector_differential`		DECIMAL(10,5) NULL COMMENT 'Comparison between the prediction vector and the actual state distributionn',
	PRIMARY KEY (`id`),
	KEY `_id_target_state_histo`(id_target_state_histo),
	KEY `_creation_date`(creation_date),
	KEY `_creation_day`(creation_day),
	KEY `_initial_date`(initial_date),
	-- KEY _creation_date_varname(creation_date, variable_name),
	KEY `_target_date`(target_date),
	KEY `_target_date_varname`(target_date, variable_name),
	KEY `_target_day_hour_var2`(target_day, target_hour, variable_name, horizon_minutes , use_corrections)
	-- KEY _variable_name(variable_name)
	-- CONSTRAINT `fk_id_correction` FOREIGN KEY (`id_correction`) REFERENCES `log_self_correction` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8


§
CREATE TABLE `prediction_item` (
	`id_prediction`				INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	`proba`						DECIMAL(10,5) NOT NULL DEFAULT 0.0,
	`state_idx`					TINYINT UNSIGNED NOT NULL,
	`state_name`				VARCHAR(32) DEFAULT '',
	CONSTRAINT `fk3_prediction` FOREIGN KEY (`id_prediction`) REFERENCES `prediction` (`id`),
	CONSTRAINT `unicity_prediction_state` UNIQUE KEY(id_prediction, state_idx)
) ENGINE=InnoDB DEFAULT CHARSET=utf8

§
CREATE TABLE `state_history` (
	`id` 					INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	`creation_date` 		DATETIME NOT NULL DEFAULT current_timestamp(),
	`id_context`			INT(11) UNSIGNED NULL,
	`id_session` 			VARCHAR(32) NOT NULL,
	`variable_name`			VARCHAR(100) NOT NULL,
	`location` 				VARCHAR(32) NOT NULL DEFAULT '',
	`scenario`				VARCHAR(32) NOT NULL DEFAULT '',
	`date`					DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00',
	`date_last`				DATETIME  NULL,
	`date_next`				DATETIME  NULL,
	`value`					DECIMAL(15,3) NOT NULL DEFAULT 0.0,
	`state_idx`				TINYINT UNSIGNED NOT NULL,
	`state_name`			VARCHAR(32) DEFAULT '',
	`id_last`				INT(11) UNSIGNED null,
	`state_idx_last`		TINYINT UNSIGNED NULL,
	`state_name_last`		VARCHAR(32) NULL,
	`observation_update`	DATETIME  NULL,
	 PRIMARY KEY (`id`),
	 UNIQUE KEY `unicity_date_variable_name` (`id_context`, `date`, `variable_name`),
	 CONSTRAINT `fk_id_last` FOREIGN KEY (`id_last`) REFERENCES `state_history` (`id`),
	 KEY(date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8



§
CREATE TABLE `log_self_correction` (
	`id` 								INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	`creation_date` 					DATETIME NOT NULL DEFAULT current_timestamp(),
	`id_session` 						VARCHAR(32) NOT NULL,
	`tag` 								TEXT NOT NULL,
	`id_transition_matrix`				INT(11) UNSIGNED NOT NULL,
	`id_transition_matrix_iteration`	INT(11) UNSIGNED NOT NULL,
	`it_number`							INT(11) UNSIGNED NOT NULL,
	`initial_state_idx`					TINYINT UNSIGNED NOT NULL,
	`from_state_idx`					TINYINT UNSIGNED NOT NULL,
	`dest_state_idx`					TINYINT UNSIGNED NOT NULL,
	`row_sum`							INT(11) UNSIGNED NOT NULL,
	`cell_sum`							INT(11) UNSIGNED NOT NULL,
	`cardinality`						INT(11) UNSIGNED NOT NULL,
	`excess`							DECIMAL(10,5) NOT NULL,
	`corrections_number` 				INT(11) UNSIGNED NOT NULL,
	`id_prediction`						INT(11) UNSIGNED NULL,
	PRIMARY KEY (`id`)
	,CONSTRAINT `fk1_id_transition_matrix` FOREIGN KEY (`id_transition_matrix`) REFERENCES `transition_matrix` (`id`)
	,CONSTRAINT `fk2_transition_matrix_iteration` FOREIGN KEY (`id_transition_matrix_iteration`) REFERENCES `transition_matrix_iteration` (`id`)
	,CONSTRAINT `fk3_id_prediction` FOREIGN KEY (`id_prediction`) REFERENCES `Prediction` (`id`)
)
§
ALTER TABLE Prediction add CONSTRAINT `fk_id_correction` FOREIGN KEY (`id_correction`) REFERENCES `log_self_correction` (`id`)

§
CREATE TABLE `device` (
	`id`					INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	`name`				VARCHAR(64) DEFAULT '',
	`category` 			ENUM ('UNKNOWN','WATER_HEATING', 'HEATING', 'COOKING', 'SHOWERS', 'WASHING_DRYING', 'LIGHTING'
						, 'AUDIOVISUAL', 'COLD_APPLIANCES', 'ICT', 'OTHER', 'ELECTRICAL_PANEL'
						, 'WIND_ENG', 'SOLOR_ENG', 'EXTERNAL_ENG', 'BIOMASS_ENG', 'HYDRO_ENG')
						 DEFAULT 'UNKNOWN',
	`environmental_impact`  TINYINT UNSIGNED NOT NULL DEFAULT 0,
	`power_min`			DECIMAL(10,5) NOT NULL DEFAULT 0.0,
	`power_max`			DECIMAL(10,5) NOT NULL DEFAULT 0.0,
	`avg_duration`		DECIMAL(10,5) NOT NULL DEFAULT 0.0,
	`is_producer`			BIT(1) NOT NULL DEFAULT b'0',
	`priority_level`		TINYINT UNSIGNED NOT NULL DEFAULT 0.0,
	PRIMARY KEY (`id`),
	UNIQUE KEY `unicity_device_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8


§
CREATE TABLE `device_statistic`(
	`device_category`	ENUM('UNKNOWN','WATER_HEATING', 'HEATING', 'COOKING', 'SHOWERS', 'WASHING_DRYING', 'LIGHTING'
						, 'AUDIOVISUAL', 'COLD_APPLIANCES', 'ICT', 'OTHER', 'ELECTRICAL_PANEL'
						, 'WIND_ENG', 'SOLOR_ENG', 'EXTERNAL_ENG', 'BIOMASS_ENG', 'HYDRO_ENG')
			 			DEFAULT 'UNKNOWN',
	`start_hour`		TINYINT UNSIGNED NOT NULL DEFAULT 0,
	`end_hour`			TINYINT UNSIGNED NOT NULL DEFAULT 0,
	`power`				DECIMAL(10,5) NOT NULL DEFAULT 0.0,
	PRIMARY KEY (`device_category`, `start_hour`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8



§
CREATE TABLE `simulator_log`(
	`id` 					INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	`id_session` 			VARCHAR(32) NOT NULL,
	`creation_time` 		DATETIME DEFAULT current_timestamp(),
	`device_category`		ENUM ('UNKNOWN','WATER_HEATING', 'HEATING', 'COOKING', 'SHOWERS', 'WASHING_DRYING', 'LIGHTING'
							,'AUDIOVISUAL', 'COLD_APPLIANCES', 'ICT', 'OTHER', 'ELECTRICAL_PANEL'
							,'WIND_ENG', 'SOLOR_ENG', 'EXTERNAL_ENG', 'BIOMASS_ENG', 'HYDRO_ENG')
			 				DEFAULT 'UNKNOWN',
	`loop_Number` 			INT(11) UNSIGNED NOT NULL DEFAULT 0,
	`power_target`			INT(11) UNSIGNED NOT NULL DEFAULT 0,
	`power_target_min`	 	DECIMAL(10,5) NOT NULL DEFAULT 0.0,
	`power_target_max` 		DECIMAL(10,5) NOT NULL DEFAULT 0.0,
	`power`					DECIMAL(10,5) NOT NULL DEFAULT 0.0,
	`is_reached`			BIT(1) NOT NULL DEFAULT b'0',
	`nb_started`			INT(11) UNSIGNED NOT NULL DEFAULT 0,
	`nb_modified`			INT(11) UNSIGNED NOT NULL DEFAULT 0,
	`nb_stopped`			INT(11) UNSIGNED NOT NULL DEFAULT 0,
	`nb_devices`			INT(11) UNSIGNED NOT NULL DEFAULT 0,
	`target_device_combination_found`	BIT(1) NOT NULL DEFAULT b'0',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8


