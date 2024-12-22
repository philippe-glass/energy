
PRAGMA foreign_keys = NO
;

DROP TABLE IF EXISTS `model_context`
;
CREATE TABLE `model_context` (
	`id` 						INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
	`id_node_context`			INTEGER  NULL,
	`scope`						VARCHAR(16) NOT NULL CHECK( scope IN ('NODE','CLUSTER') ),
	`model_type`				VARCHAR(16) NOT NULL,
	`learning_window`			INTEGER  NOT NULL,
	`aggregation_operator`		VARCHAR(32) NULL,
	`creation_time` 			DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
	FOREIGN KEY(id_node_context) REFERENCES node_context(id)
)
;


DROP TABLE IF EXISTS `mc_model_iteration`
;
CREATE TABLE `mc_model_iteration` (
	`id_model_context`			INTEGER  NOT NULL,
	`iteration_number`			INTEGER  NOT NULL,
	`creation_time` 			DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
	`iteration_date`			DATE NOT NULL,
	`last_update`				DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00',
	PRIMARY KEY (`id_model_context`, `iteration_number`),
	UNIQUE (id_model_context, iteration_date),
	FOREIGN KEY(id_model_context) REFERENCES model_context(id)
)
;

DROP TABLE IF EXISTS `mc_transition_matrix`
;
CREATE TABLE `mc_transition_matrix` (
	`id` 						INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
	`id_node_context`			INTEGER  NULL,
	`id_model_context`			INTEGER  NOT NULL,
	`id_time_window`			INTEGER  NOT NULL,
	`variable_name`				VARCHAR(100) NOT NULL,
	`current_iteration_number`	INTEGER  NOT NULL DEFAULT 0,
	`last_update`				DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00',
-- 	PRIMARY KEY (`id`),
	UNIQUE (id_model_context, id_time_window, variable_name),
	FOREIGN KEY(id_time_window) REFERENCES time_window(id),
	FOREIGN KEY(id_model_context) REFERENCES model_context(id),
	FOREIGN KEY(id_node_context) REFERENCES node_context(id)
)

;
DROP TABLE IF EXISTS `mc_transition_matrix_cell_iteration`
;
CREATE TABLE `mc_transition_matrix_cell_iteration` (
    `id_transition_matrix`	 			INTEGER  NOT NULL,
    `id_model_context`					INTEGER  NOT NULL,
    `iteration_number` 					INTEGER  NOT NULL DEFAULT 0,
	`row_idx`							TINYINT  NOT NULL,
	`column_idx`						TINYINT  NOT NULL,
	`creation_time` 					DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),	
	`obs_number`						DECIMAL(10,5) NOT NULL DEFAULT 0.0,
	`corrections_number`				INTEGER  NOT NULL DEFAULT 0.0 , -- Used for self-adaptive correction
	FOREIGN KEY(id_transition_matrix) REFERENCES mc_transition_matrix(id),
	FOREIGN KEY(id_model_context, iteration_number) REFERENCES mc_model_iteration(id_model_context, iteration_number),
	PRIMARY KEY(id_transition_matrix, iteration_number, row_idx,  column_idx)
)

;
DROP TABLE IF EXISTS `prediction`
;
CREATE TABLE `prediction` (
	`id` 						INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
	`creation_date` 			DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
	`creation_day` 				DATE NOT NULL DEFAULT (date(CURRENT_TIMESTAMP, 'localtime')),
	`id_model_context`			INTEGER  NOT NULL,
	`id_node_context`			INTEGER  NULL,
	`variable_name`				VARCHAR(100) NOT NULL,
	`initial_date`				DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00',
	`target_date`				DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00',
	`target_day` 				DATE null,
	`target_hour` 				TINYINT null,
	`ut_target_begin_slot`		INTEGER  NULL,
	`use_corrections`			BIT default 0,
	`id_initial_time_window`	INTEGER  NOT NULL,
	`initial_state_idx`			TINYINT  NOT NULL,
	`initial_state_name`		VARCHAR(32) DEFAULT '',
	`id_initial_transition_matrix`	INTEGER   NULL,
	`id_target_transition_matrix`	INTEGER   NULL,
	`random_state_idx`			TINYINT  NOT NULL,
	`random_state_name`			VARCHAR(32) DEFAULT '',
	`random_state_proba`		DECIMAL(10,5) NOT NULL DEFAULT 0.0,
	`likely_state_idx`			TINYINT  NOT NULL,
	`likely_state_name`			VARCHAR(32) DEFAULT '',
	`likely_state_proba`		DECIMAL(10,5) DEFAULT 0.0,
	`likely_value`				DECIMAL(15,3) NULL,
	`horizon_minutes`			INTEGER  NOT NULL DEFAULT 0,
	`id_target_state_histo`		INTEGER  NULL,
	`delta_target_state_histo`	INTEGER  NULL,
	`link_done`					BIT default 0,
	`id_correction`				INTEGER  NULL,
	`sum_state_distrib`			DECIMAL(10,5) NULL ,
	`vector_differential`		DECIMAL(10,5) NULL , -- Comparison between the prediction vector and the actual state distributionn
	`cross_entropy_loss`		DECIMAL(10,5) NULL , -- Cross entroy between the prediction vector and the actual state distributionn
	-- KEY _creation_date_varname(creation_date, variable_name),
	-- KEY _variable_name(variable_name)
	FOREIGN KEY(id_node_context) REFERENCES node_context(id),
	FOREIGN KEY(id_model_context) REFERENCES model_context(id),
	FOREIGN KEY(id_correction) REFERENCES log_mc_self_correction(id),
	FOREIGN KEY(id_target_state_histo) REFERENCES state_history(id)
)


;
-- CREATE INDEX _id_target_state_histo ON `prediction` (id_target_state_histo);
CREATE INDEX _creation_date ON `prediction` (id_model_context, creation_date);
CREATE INDEX _creation_day ON `prediction` (id_model_context, creation_day);
CREATE INDEX _initial_date ON `prediction` (id_model_context, initial_date);
CREATE INDEX _target_date ON `prediction` (id_model_context, target_date);
CREATE INDEX _target_date_varname ON `prediction` (id_model_context, target_date, variable_name);
CREATE INDEX _target_day_hour_var2 ON `prediction` (id_model_context, target_day, target_hour, variable_name, horizon_minutes , use_corrections);
DROP TABLE IF EXISTS `prediction_item`
;
CREATE TABLE `prediction_item` (
	`id_prediction`				INTEGER  NOT NULL, -- PRIMARY KEY AUTOINCREMENT,
	`proba`						DECIMAL(10,5) NOT NULL DEFAULT 0.0,
	`state_idx`					TINYINT  NOT NULL,
	`state_name`				VARCHAR(32) DEFAULT '',
	FOREIGN KEY(id_prediction) REFERENCES prediction(id),
	CONSTRAINT `unicity_prediction_state` UNIQUE(id_prediction, state_idx)
)

;
DROP TABLE IF EXISTS `state_history`
;
CREATE TABLE `state_history` (
	`id` 					INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
	`creation_date` 		DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
	`id_model_context`		INTEGER  NOT NULL,
	`id_node_context`		INTEGER  NULL,
	`id_session` 			INTEGER NULL,
	`variable_name`			VARCHAR(100) NOT NULL,
	`date`					DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00',
	`date_last`				DATETIME  NULL,
	`date_next`				DATETIME  NULL,
	`value`					DECIMAL(15,3) NOT NULL DEFAULT 0.0,
	`state_idx`				TINYINT  NOT NULL,
	`state_name`			VARCHAR(32) DEFAULT '',
	`id_last`				INTEGER  null,
	`state_idx_last`		TINYINT  NULL,
	`state_name_last`		VARCHAR(32) NULL,
	`observation_update`	DATETIME  NULL,
-- 	 PRIMARY KEY (`id`),
	UNIQUE (id_model_context, date, variable_name),
	FOREIGN KEY(id_session) REFERENCES session(id),
	FOREIGN KEY(id_last) REFERENCES state_history(id)
	FOREIGN KEY(id_model_context) REFERENCES model_context(id)
	FOREIGN KEY(id_node_context) REFERENCES node_context(id)
)



;
CREATE INDEX _date ON `state_history` (id_model_context, date)
;
CREATE INDEX _last_state_history ON `state_history` (id_last)
;
DROP TABLE IF EXISTS `log_mc_self_correction`


;
DROP TABLE IF EXISTS `log_mc_self_correction`
;
CREATE TABLE `log_mc_self_correction` (
	`id` 								INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
	`creation_date` 					DATETIME NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP, 'localtime')),
	`id_session` 						INTEGER NULL,
	`tag` 								TEXT NOT NULL,
	`id_transition_matrix`				INTEGER  NOT NULL,
	`iteration_number`					INTEGER  NOT NULL,
	`it_number`							INTEGER  NOT NULL,
	`initial_state_idx`					TINYINT  NOT NULL,
	`from_state_idx`					TINYINT  NOT NULL,
	`dest_state_idx`					TINYINT  NOT NULL,
	`row_sum`							INTEGER  NOT NULL,
	`cell_sum`							INTEGER  NOT NULL,
	`cardinality`						INTEGER  NOT NULL,
	`excess`							DECIMAL(10,5) NOT NULL,
	`corrections_number` 				INTEGER  NOT NULL,
	`id_prediction`						INTEGER  NULL,
-- 	PRIMARY KEY (`id`)
	FOREIGN KEY(id_session) REFERENCES session(id),
	FOREIGN KEY(id_transition_matrix) REFERENCES mc_transition_matrix(id),
	FOREIGN KEY(iteration_number)  REFERENCES mc_model_iteration(iteration_number),
	FOREIGN KEY(id_prediction) REFERENCES prediction(id)
)
;
--	ALTER TABLE prediction ADD CONSTRAINT fk_id_correction FOREIGN KEY(id_correction) REFERENCES log_mc_self_correction(id)






-- TODO : define max_interation_number in mc_transition_matrix table (for the moment, defined in hard : 100
--
DROP VIEW IF EXISTS v_transition_matrix_cell
;
DROP VIEW IF EXISTS v_mc_transition_matrix_cell
;
CREATE VIEW v_mc_transition_matrix_cell
	AS
	SELECT    tr_mtx.id_node_context
			 ,tr_mtx.id_model_context
			 ,tr_mtx.id_time_window
			 ,tr_mtx.id AS id_transition_matrix
			 ,cell_it.row_idx
			 ,cell_it.column_idx
			 ,SUM(obs_number) AS obs_number
			 ,SUM(corrections_number) AS corrections_number
			 ,model_context.learning_window
		FROM model_context
		JOIN mc_transition_matrix AS tr_mtx ON tr_mtx.id_model_context = model_context.id
		JOIN mc_transition_matrix_cell_iteration AS cell_it ON cell_it.id_transition_matrix = tr_mtx.id
		WHERE 1 -- tr_mtx.id_node_context = p_id_node_context
			-- AND (tr_mtx.id_time_window = p_id_time_window OR p_id_time_window IS NULL)
		 -- AND cell_it.iteration_number >= tr_mtx.iteration_number - p_max_iteration_nb
		 AND cell_it.iteration_number >= tr_mtx.current_iteration_number - model_context.learning_window
		 AND cell_it.iteration_number <= tr_mtx.current_iteration_number
		 GROUP BY cell_it.id_transition_matrix , row_idx , column_idx
		 HAVING (obs_number + corrections_number) >= 0
;


SELECT * FROM v_mc_transition_matrix_cell
;



DROP  VIEW IF EXISTS v_grouped_state
;
CREATE VIEW v_grouped_state AS
	SELECT id_model_context
	, UNIXEPOCH(date) - UNIXEPOCH(date) % 3600 AS ut_begin_slot
	, strftime('%H', date)  AS hour
	, date(date) AS hs_day, variable_name, state_name , state_idx, count(*) AS nb
	FROM state_history h
	WHERE NOT observation_update IS NULL -- obsevation
	GROUP BY id_model_context, ut_begin_slot, variable_name, state_idx
;
DROP  VIEW IF EXISTS v_state_distribution
;
CREATE VIEW v_state_distribution AS
SELECT
	 v_grouped_state.*
	,totalBySlot.total_nb
	,CAST(v_grouped_state.nb AS REAL) / CAST(totalBySlot.total_nb AS REAL) AS ratio
	,state_name || ':' ||  ROUND(nb/totalBySlot.total_nb,2)  AS Label
	,state_name || ':' ||  nb  AS Label
FROM v_grouped_state
JOIN  (
			SELECT id_model_context, ut_begin_slot, variable_name, SUM(nb) AS total_nb
			FROM v_grouped_state
			GROUP BY id_model_context, ut_begin_slot, variable_name
	  ) AS totalBySlot ON
				totalBySlot.id_model_context = v_grouped_state.id_model_context
			AND totalBySlot.ut_begin_slot = v_grouped_state.ut_begin_slot
			AND totalBySlot.variable_name = v_grouped_state.variable_name
;


SELECT * FROM v_state_distribution
;
PRAGMA foreign_keys = YES
;