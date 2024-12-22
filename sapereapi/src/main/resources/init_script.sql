-- Test V1

DROP TABLE IF EXISTS `session`
;
CREATE TABLE `session` (
  `id` 					INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `creation_time` 		DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
  `number` 				VARCHAR(32) NOT NULL,
  UNIQUE(number)
)
;

DROP TABLE IF EXISTS `node_location`
;
CREATE TABLE `node_location` (
	`name` 					VARCHAR(16) NOT NULL DEFAULT '' PRIMARY KEY,
	`creation_date` 		DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
	`host`					VARCHAR(32) NOT NULL DEFAULT '',
	`main_port`				INTEGER NULL,
	`rest_port`				INTEGER NULL,
	UNIQUE (host, main_port),
	UNIQUE (host, rest_port)
)
;

-- energy.node_history definition
DROP TABLE IF EXISTS `node_history`
;
CREATE TABLE `node_history` (
  `id` 					INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
  `creation_time` 		DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
  `date` 				DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
  `time_shift_ms`		BIGINT(15)  NOT NULL DEFAULT 0,
  `id_session` 			INTEGER NULL, -- Session identifier
  `node`				VARCHAR(16)  , -- Node config
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
  `is_additional_refresh`	BIT(1) NOT NULL DEFAULT 0,
	UNIQUE (date, node),
	FOREIGN KEY(id_session) REFERENCES session(id),
	FOREIGN KEY(id_last) REFERENCES node_history(id),
	FOREIGN KEY(id_next) REFERENCES node_history(id),
	FOREIGN KEY(node) REFERENCES node_location(name)
)
;

CREATE INDEX idx_node_history_date ON `node_history` (date)
;

-- energy.event definition

DROP TABLE IF EXISTS `agent`
;
CREATE TABLE `agent` (
  `id_session` 				INTEGER NOT NULL,
  `name` 					VARCHAR(32) NOT NULL,
  `node`					VARCHAR(16) NULL, -- Node config
  `distance` 				TINYINT  NOT NULL DEFAULT 0.0,
  `device_name` 			VARCHAR(100) NOT NULL DEFAULT '',
  `device_category` 		VARCHAR(100) NOT NULL DEFAULT '',
  `environmental_impact`  	TINYINT  NOT NULL DEFAULT 0,
	PRIMARY KEY(id_session, name),
	FOREIGN KEY(id_session) REFERENCES session(id),
	FOREIGN KEY(node) REFERENCES node_location(name)
	)
;

DROP TABLE IF EXISTS `event`
;
CREATE TABLE `event` (
  `id` 					INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `creation_time` 		DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
  `time_shift_ms`		BIGINT(15)  NOT NULL DEFAULT 0,
  `id_session` 			INTEGER NULL,
  `id_history` 			INTEGER  NULL,
  `object_type` 		VARCHAR(16) CHECK(`object_type` IN ('PRODUCTION','REQUEST','CONTRACT')) NOT NULL,
  `main_category` 		VARCHAR(16) CHECK(`main_category` IN ('START','STOP','EXPIRY','UPDATE', 'SWITCH')) NOT NULL,
  `warning_type` 		VARCHAR(32) NOT NULL DEFAULT '',
  `agent_name`			VARCHAR(32) NOT NULL,
  `is_complementary`	BIT(1) NOT NULL DEFAULT 0 , -- to identify complementary contracts
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
  `power_max_update` 	DECIMAL(15,3) NOT NULL DEFAULT 0.00 , -- changes since the last event
  `additional_power` 	DECIMAL(15,3) NOT NULL DEFAULT 0.00 , -- power used from battery
  `first_rate`			DECIMAL(10,3) NOT NULL DEFAULT 0.00 , -- energy rate for contract event
  `id_origin` 			INTEGER DEFAULT NULL,
  `is_cancel` 			BIT(1) NOT NULL DEFAULT 0,
  `is_ending` 			BIT(1) NOT NULL DEFAULT 0,
  `comment`			TEXT NOT NULL DEFAULT '',
    FOREIGN KEY(id_history) REFERENCES node_history(id),
    FOREIGN KEY(id_session) REFERENCES session(id),
	FOREIGN KEY(id_session, agent_name) REFERENCES agent(id_session, name),
	UNIQUE (begin_date,object_type,main_category,agent_name,is_complementary)
)
;





-- energy.link_event_agent definition

CREATE INDEX idx_begin_date ON `event` (begin_date);
CREATE INDEX idx_expiry_date ON `event` (expiry_date);
CREATE INDEX idx_cancel_date ON `event` (cancel_date);
CREATE INDEX idx_agent ON `event` (agent_name);
CREATE INDEX id_origin ON `event` (id_origin);
DROP TABLE IF EXISTS `link_event_agent`
;
CREATE TABLE `link_event_agent` (
  `id_event` 				INTEGER NOT NULL,
  `agent_role` 				VARCHAR(100) NOT NULL CHECK( agent_role IN ('PRODUCER','CONSUMER') ),
  `agent_name` 				VARCHAR(32) NOT NULL,
  `id_session` 				INTEGER NULL,
  `power` 					DECIMAL(15,3) NOT NULL DEFAULT 0.00,
  `power_min` 				DECIMAL(15,3) NOT NULL DEFAULT 0.00,
  `power_max` 				DECIMAL(15,3) NOT NULL DEFAULT 0.00,
	PRIMARY KEY(id_event, agent_name),
	FOREIGN KEY(id_event) REFERENCES event(id),
	FOREIGN KEY(id_session) REFERENCES session(id),
	FOREIGN KEY(id_session, agent_name) REFERENCES agent(id_session, name)
)


;
-- CREATE INDEX event_id ON `link_event_agent` (id_event);
DROP TABLE IF EXISTS `link_history_active_event`
;
CREATE TABLE `link_history_active_event` (
  `id` 				INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
  `id_history`		INTEGER   NULL,
  `id_event` 		INTEGER DEFAULT NULL,
  `date` 			DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
  `provided`		DECIMAL(15,3) NOT NULL , -- (For producers)
  `provided_margin`	DECIMAL(15,3) NOT NULL , -- (For producers)
  `consumed`		DECIMAL(15,3) NOT NULL , -- (For consumers)
  `missing`			DECIMAL(15,3) NOT NULL , -- (For consumers)
  `is_request` 		BIT(1) NOT NULL DEFAULT 0,
  `is_producer` 	BIT(1) NOT NULL DEFAULT 0,
  `is_contract` 	BIT(1) NOT NULL DEFAULT 0,
  `id_last` 		INTEGER  NULL DEFAULT NULL,
  `warning_duration` INTEGER  NOT NULL DEFAULT 0,
  `has_warning_req` BIT(1) NOT NULL DEFAULT 0,
	FOREIGN KEY(id_event) REFERENCES event(id),
	FOREIGN KEY(id_history) REFERENCES node_history(id),
	FOREIGN KEY(id_last) REFERENCES link_history_active_event(id),
	UNIQUE(id_history, id_event)
)



;
-- CREATE INDEX lhae_node ON `link_history_active_event` (node)
-- ;
DROP TABLE IF EXISTS `single_offer`
;
CREATE TABLE `single_offer` (
  `id` 					INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `id_session` 			INTEGER NULL,
  `creation_time` 		DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
  `date` 				DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
  `time_shift_ms` 		BIGINT(15)  NOT NULL DEFAULT 0 ,
  `id_history`			INTEGER   NULL,
  `deadline` 			DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00',
  `begin_date` 			DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00',
  `end_date` 			DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00',
  `producer_agent` 		VARCHAR(32) NOT NULL,
  `consumer_agent` 		VARCHAR(32) NOT NULL,
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
  `credit_granted_wh`	DECIMAL(15,5) NOT NULL DEFAULT 0.00,
  `credit_duration_sec`	DECIMAL(15,3) NOT NULL DEFAULT 0.00,
  `discount_rate`		DECIMAL(15,5) NOT NULL DEFAULT 0.00,
  `log` 				TEXT NOT NULL DEFAULT '',
  `log2` 				TEXT NOT NULL DEFAULT '',
  `log_cancel`			TEXT NOT NULL DEFAULT '',
--   PRIMARY KEY (`id`),
	FOREIGN KEY(id_session) REFERENCES session(id),
	FOREIGN KEY(contract_event_id) REFERENCES event(id),
	FOREIGN KEY(production_event_id) REFERENCES event(id),
	FOREIGN KEY(request_event_id) REFERENCES event(id),
	FOREIGN KEY(id_history) REFERENCES node_history(id)
)




;
DROP TABLE IF EXISTS `context`
;
DROP TABLE IF EXISTS `node_context`
;
CREATE TABLE `node_context` (
	`id` 					INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
	`creation_date` 		DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
	`node`					VARCHAR(16) NULL, -- Node config
	`scenario`				VARCHAR(32) NOT NULL DEFAULT '',
	`learning_agent`		VARCHAR(32) NOT NULL,
	`regulator_agent`		VARCHAR(32) NOT NULL,
	`last_id_session`		INTEGER NULL,
	`last_time_shift_ms`	BIGINT(15)  NOT NULL DEFAULT 0,
-- 	PRIMARY KEY (`id`),
	UNIQUE (node, scenario),
	FOREIGN KEY(last_id_session) REFERENCES session(id),
	FOREIGN KEY(node) REFERENCES node_location(name)
)


;
DROP TABLE IF EXISTS `context_neighbour`
;
CREATE TABLE `context_neighbour` (
	`creation_date` 		DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
	`id_node_context`		INTEGER  NOT NULL , -- Context
	`node`					VARCHAR(16) NULL, -- Node config
-- 	PRIMARY KEY (`id_node_context`, `node`),
	FOREIGN KEY(id_node_context) REFERENCES node_context(id),
	FOREIGN KEY(node) REFERENCES node_location(name)
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
;

DROP TABLE IF EXISTS `log_credit_usage`
;
CREATE TABLE `log_credit_usage` (
	 `id` 						INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
	 `creation_date` 			DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
	 `id_contract_event`		INTEGER NOT NULL,
	 `id_request_event`			INTEGER NULL,
	 `credit_granted_wh`		DECIMAL(15,5) NOT NULL DEFAULT 0.00,
	 `credit_used_wh`			DECIMAL(15,5) NOT NULL DEFAULT 0.00,
	 `first_rate_discount`		DECIMAL(15,5) NOT NULL DEFAULT 0.00,
	 `time_spent_sec`			DECIMAL(15,3) NOT NULL DEFAULT 0.00,
	 `time_spent_expected_sec`	DECIMAL(15,3) NOT NULL DEFAULT 0.00,
	 `begin_date`				DATETIME NOT NULL,
	 `end_date`					DATETIME NOT NULL,
	 `expected_end_date`		DATETIME NOT NULL,
	 `log` 						TEXT NOT NULL DEFAULT '',
	 `last_update` 				DATETIME NULL,
	 `update_nb`				INTEGER NOT NULL DEFAULT 0,
	 UNIQUE (id_contract_event, begin_date)
 )
;
PRAGMA foreign_keys = YES
;