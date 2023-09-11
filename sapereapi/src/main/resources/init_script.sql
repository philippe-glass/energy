-- Test V1

DROP TABLE IF EXISTS `node_config`
;
CREATE TABLE `node_config` (
	`id` 					INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
	`creation_date` 		DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
	`name` 					VARCHAR(16) NOT NULL DEFAULT '',
	`host`					VARCHAR(32) NOT NULL DEFAULT '',
	`main_port`				INTEGER  NULL,
	`rest_port`				INTEGER  NULL,
-- 	PRIMARY KEY (`id`),
	UNIQUE (name, host, main_port)
)
;

-- energy.history definition
DROP TABLE IF EXISTS `history`
;
CREATE TABLE `history` (
  `id` 					INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
  `creation_time` 		DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
  `date` 				DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
  `time_shift_ms`		BIGINT(15)  NOT NULL DEFAULT 0,
  `id_session` 			VARCHAR(32) NOT NULL,
  `id_node_config`		INTEGER  , -- Node config
  `learning_agent` 		VARCHAR(100) NOT NULL,
  `location` 			VARCHAR(32) NOT NULL DEFAULT '',
  `distance` 			TINYINT  NOT NULL DEFAULT 0.0,
  `id_last` 			INTEGER  DEFAULT NULL,
  `id_next` 			INTEGER  DEFAULT NULL,
  `total_produced` 		DECIMAL(15,3) NOT NULL DEFAULT 0.00 , -- total produced (KWH)
  `total_provided` 		DECIMAL(15,3) NOT NULL DEFAULT 0.00 , -- total provided (KWH)
  `total_requested` 	DECIMAL(15,3) NOT NULL DEFAULT 0.00 , -- total needed for consumption (KWH)
  `total_consumed` 		DECIMAL(15,3) NOT NULL DEFAULT 0.00 , -- total consumed (KWH)
  `total_margin`		DECIMAL(15,3) NOT NULL DEFAULT 0.00 , -- total used for contract margins (KWH)
  `total_available` 	DECIMAL(15,3) NOT NULL DEFAULT 0.00 , -- total produced - total consumed (KWH)
  `total_missing` 		DECIMAL(15,3) NOT NULL DEFAULT 0.00 , -- total requested and not provided (KWH)
  `min_request_missing` DECIMAL(15,3) NOT NULL DEFAULT 0.00 , -- min of non satisfied request in KWH
  `max_warning_duration` INTEGER  NOT NULL DEFAULT 0 , -- max warning duration of requests
  `max_warning_consumer`	VARCHAR(100) NULL,
--   PRIMARY KEY (`id`),
	UNIQUE (date, location),
	FOREIGN KEY(id_last) REFERENCES history(id),
	FOREIGN KEY(id_next) REFERENCES history(id),
	FOREIGN KEY(id_node_config) REFERENCES node_config(id)
)
;

-- energy.event definition
CREATE INDEX _id_session ON `history` (id_session);
CREATE INDEX idx_date ON `history` (date);
DROP TABLE IF EXISTS `event`
;
CREATE TABLE `event` (
  `id` 					INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `creation_time` 		DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
  `time_shift_ms`		BIGINT(15)  NOT NULL DEFAULT 0,
  `id_session` 			VARCHAR(32) NOT NULL,
  `id_node_config`		INTEGER  , -- Node config
  `id_histo` 			INTEGER  NULL,
  `type` VARCHAR(32)  CHECK(`type` IN ('','PRODUCTION','REQUEST','CONTRACT','PRODUCTION_START','REQUEST_START','CONTRACT_START','PRODUCTION_STOP','REQUEST_STOP','CONTRACT_STOP','PRODUCTION_EXPIRY','REQUEST_EXPIRY','CONTRACT_EXPIRY','PRODUCTION_UPDATE','REQUEST_UPDATE','CONTRACT_UPDATE')) DEFAULT NULL,
  `object_type` VARCHAR(32)  CHECK(`object_type` IN ('PRODUCTION','REQUEST','CONTRACT')) DEFAULT NULL,
  `main_category` VARCHAR(32)  CHECK(`main_category` IN ('START','STOP','EXPIRY','UPDATE')) DEFAULT NULL,
  `warning_type` 		VARCHAR(32) NOT NULL DEFAULT '',
  `agent` 				VARCHAR(100) NOT NULL,
  `is_complementary`	BIT(1) NOT NULL DEFAULT 0 , -- to identify complementary contracts
  `location` 			VARCHAR(32) NOT NULL DEFAULT '',
  `distance` 			TINYINT  NOT NULL DEFAULT 0.0,
  `device_name` 		VARCHAR(100) NOT NULL DEFAULT '',
  `device_category` 	VARCHAR(100) NOT NULL DEFAULT '',
  `environmental_impact`  TINYINT  NOT NULL DEFAULT 0,
  `begin_date` 			DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
  `expiry_date` 		DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00',
  `cancel_date` 		DATETIME DEFAULT NULL,
  `interruption_date`	DATETIME DEFAULT NULL,
  `duration` 			DECIMAL(15,3) NOT NULL DEFAULT 0.00,
  `power` 				DECIMAL(15,3) NOT NULL,
  `power_min`			DECIMAL(15,3) NOT NULL,
  `power_max`			DECIMAL(15,3) NOT NULL,
  `power_update` 		DECIMAL(15,3) NOT NULL DEFAULT 0.00 , -- changes since the last event
  `power_min_update` 	DECIMAL(15,3) NOT NULL DEFAULT 0.00 , -- changes since the last event
  `power_max_update` DECIMAL(15,3) NOT NULL DEFAULT 0.00 , -- changes since the last event
  `id_origin` 			INTEGER DEFAULT NULL,
  `is_cancel` 			BIT(1) NOT NULL DEFAULT 0,
  `is_ending` 			BIT(1) NOT NULL DEFAULT 0,
   `comment`			TEXT NOT NULL DEFAULT '',
--   PRIMARY KEY (`id`),
	FOREIGN KEY(id_histo) REFERENCES history(id),
	FOREIGN KEY(id_node_config) REFERENCES node_config(id),
	UNIQUE (begin_date,type,agent, is_complementary)
)
;





-- energy.link_event_agent definition

CREATE INDEX idx_begin_date ON `event` (begin_date);
CREATE INDEX idx_expiry_date ON `event` (expiry_date);
CREATE INDEX idx_cancel_date ON `event` (cancel_date);
CREATE INDEX idx_agent ON `event` (agent);
CREATE INDEX id_origin ON `event` (id_origin);
DROP TABLE IF EXISTS `link_event_agent`
;
CREATE TABLE `link_event_agent` (
  `id` 						INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `id_event` 				INTEGER DEFAULT NULL,
  `agent_type` 				VARCHAR(100) NOT NULL,
  `agent_name` 				VARCHAR(100) NOT NULL,
  `agent_location` 			VARCHAR(32) NOT NULL DEFAULT '',
  `agent_id_node_config`	INTEGER  , -- Node config
  `power` 					DECIMAL(15,3) NOT NULL DEFAULT 0.00,
  `power_min` 				DECIMAL(15,3) NOT NULL DEFAULT 0.00,
  `power_max` 				DECIMAL(15,3) NOT NULL DEFAULT 0.00,
--   PRIMARY KEY (`id`),
	FOREIGN KEY(id_event) REFERENCES event(id),
	FOREIGN KEY(agent_id_node_config) REFERENCES node_config(id)
)


;
CREATE INDEX event_id ON `link_event_agent` (id_event);
DROP TABLE IF EXISTS `link_history_active_event`
;
CREATE TABLE `link_history_active_event` (
  `id` 				INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
  `id_history`		INTEGER   NULL,
  `id_event` 		INTEGER DEFAULT NULL,
  `id_event_origin` INTEGER DEFAULT NULL,
  `date` 			DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
  `type` 			VARCHAR(100) NOT NULL,
  `agent` 			VARCHAR(100) NOT NULL,
  `location` 		VARCHAR(32) NOT NULL DEFAULT '',
  `power` 			DECIMAL(15,3) NOT NULL,
  `provided`		DECIMAL(15,3) NOT NULL , -- (For producers)
  `consumed`		DECIMAL(15,3) NOT NULL , -- (For consumers)
  `missing`			DECIMAL(15,3) NOT NULL , -- (For consumers)
  `is_request` 		BIT(1) NOT NULL DEFAULT 0,
  `is_producer` 	BIT(1) NOT NULL DEFAULT 0,
  `is_contract` 	BIT(1) NOT NULL DEFAULT 0,
  `is_complementary` BIT(1) NOT NULL DEFAULT 0 , -- to identify complementary contracts
  `id_contract_evt` INTEGER NULL , -- Not to be used because a consumer can have several contracts
  `id_last` 		INTEGER  NULL DEFAULT NULL,
  `warning_duration` INTEGER  NOT NULL DEFAULT 0,
  `has_warning_req` BIT(1) NOT NULL DEFAULT 0,
--    PRIMARY KEY (`id`),
	FOREIGN KEY(id_event) REFERENCES event(id),
	FOREIGN KEY(id_history) REFERENCES history(id),
	FOREIGN KEY(id_contract_evt) REFERENCES event(id),
	FOREIGN KEY(id_last) REFERENCES link_history_active_event(id)
)



;
CREATE INDEX _location ON `link_history_active_event` (location);
DROP TABLE IF EXISTS `single_offer`
;
CREATE TABLE `single_offer` (
  `id` 					INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `id_session` 			VARCHAR(32) NOT NULL,
  `creation_time` 		DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
  `date` 				DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
  `time_shift_ms` 		BIGINT(15)  NOT NULL DEFAULT 0 ,
  `id_history`			INTEGER   NULL,
  `deadline` 			DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00',
  `producer_agent` 		VARCHAR(100) NOT NULL,
  `consumer_agent` 		VARCHAR(100) NOT NULL,
  `is_complementary`	BIT(1) NOT NULL DEFAULT 0 , -- to identify complementary contracts
  `production_event_id` INTEGER  NULL,
  `request_event_id` 	INTEGER NULL,
  `contract_event_id` 	INTEGER NULL,
  `power` 				DECIMAL(15,3) NOT NULL,
  `power_min`			DECIMAL(15,3) NOT NULL,
  `power_max`			DECIMAL(15,3) NOT NULL,
  `acquitted`  			BIT(1) NOT NULL DEFAULT 0,
  `used`  				BIT(1) NOT NULL DEFAULT 0,
  `used_time` 			DATETIME NULL,
  `accepted`  			BIT(1) NOT NULL DEFAULT 0,
  `acceptance_time` 	DATETIME NULL,
  `contract_time`  		DATETIME NULL,
   `log` 				TEXT NOT NULL DEFAULT '',
   `log2` 				TEXT NOT NULL DEFAULT '',
   `log_cancel`			TEXT NOT NULL DEFAULT '',
--   PRIMARY KEY (`id`),
	FOREIGN KEY(contract_event_id) REFERENCES event(id),
	FOREIGN KEY(production_event_id) REFERENCES event(id),
	FOREIGN KEY(request_event_id) REFERENCES event(id),
	FOREIGN KEY(id_history) REFERENCES history(id)
)




;
DROP TABLE IF EXISTS `context`
;
CREATE TABLE `context` (
	`id` 					INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
	`creation_date` 		DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
	`location` 				VARCHAR(32) NOT NULL DEFAULT '',
	`id_node_config`		INTEGER  , -- Node config
	`scenario`				VARCHAR(32) NOT NULL DEFAULT '',
	`last_id_session`		VARCHAR(32) NOT NULL,
	`last_time_shift_ms`	BIGINT(15)  NOT NULL DEFAULT 0,
-- 	PRIMARY KEY (`id`),
	UNIQUE (location, scenario),
	FOREIGN KEY(id_node_config) REFERENCES node_config(id)
)


;
DROP TABLE IF EXISTS `context_neighbour`
;
CREATE TABLE `context_neighbour` (
	`creation_date` 		DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
	`id_context`			INTEGER  NOT NULL , -- Context
	`id_node_config`		INTEGER  NOT NULL , -- Neighbour node config
-- 	PRIMARY KEY (`id_context`, `id_node_config`),
	FOREIGN KEY(id_context) REFERENCES context(id),
	FOREIGN KEY(id_node_config) REFERENCES node_config(id)
)


;
DROP TABLE IF EXISTS `time_window`
;
CREATE TABLE `time_window` (
	`id`				INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
	`days_of_week`  	VARCHAR(32) DEFAULT '',
	`start_hour`		TINYINT  NOT NULL DEFAULT 0.0,
	`start_minute`		TINYINT  NOT NULL DEFAULT 0.0,
	`end_hour`			TINYINT  NOT NULL DEFAULT 0.0,
	`end_minute`		TINYINT  NOT NULL DEFAULT 0.0
-- 	PRIMARY KEY (`id`)
)


;



INSERT INTO `time_window` VALUES
 (1,'1,2,3,4,5,6,7',0,0,6,0),(2,'1,2,3,4,5,6,7',6,0,7,0),(3,'1,2,3,4,5,6,7',7,0,8,0),(4,'1,2,3,4,5,6,7',8,0,9,0)
,(5,'1,2,3,4,5,6,7',9,0,10,0),(6,'1,2,3,4,5,6,7',10,0,11,0),(7,'1,2,3,4,5,6,7',11,0,12,0),(8,'1,2,3,4,5,6,7',12,0,13,0)
,(9,'1,2,3,4,5,6,7',13,0,14,0),(10,'1,2,3,4,5,6,7',14,0,15,0),(11,'1,2,3,4,5,6,7',15,0,16,0),(12,'1,2,3,4,5,6,7',16,0,17,0)
,(13,'1,2,3,4,5,6,7',17,0,18,0),(14,'1,2,3,4,5,6,7',18,0,19,0),(15,'1,2,3,4,5,6,7',19,0,20,0),(16,'1,2,3,4,5,6,7',20,0,21,0)
,(17,'1,2,3,4,5,6,7',21,0,22,0),(18,'1,2,3,4,5,6,7',22,0,23,0),(19,'1,2,3,4,5,6,7',23,0,24,0)
;


DROP TABLE IF EXISTS `t_value`
;
CREATE TABLE t_value (
  `id` 					INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
  `date`		 		DATETIME NOT NULL ,
  `value` 				DECIMAL(15,3) NOT NULL DEFAULT 0.00,  -- power,
  UNIQUE (date)
 )