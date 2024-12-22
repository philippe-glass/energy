DELIMITER §


DROP FUNCTION IF EXISTS GET_EFFECTIVE_END_DATE
§
CREATE FUNCTION GET_EFFECTIVE_END_DATE(p_id_event INT) RETURNS datetime
BEGIN
	DECLARE vResult datetime;
	DECLARE vInterruption datetime;
	SET vResult = (SELECT
		IF(event.cancel_date IS NULL OR event.cancel_date > event.expiry_date  , event.expiry_date, event.cancel_date)
		FROM event WHERE event.ID = p_id_event)
	;
	SET vInterruption = (SELECT interruption.begin_date
		FROM event AS interruption WHERE interruption.id_origin = p_id_event and interruption.is_cancel
		ORDER BY interruption.begin_date  LIMIT 0,1)
	;
	IF NOT(vInterruption IS NULL) THEN
		SET vResult = LEAST(vResult , vInterruption);
	END IF
	;
	RETURN vResult
	;
END
§
SELECT event.*
  ,GET_EFFECTIVE_END_DATE(event.id) AS effective_end_date
  FROM event
  WHERE NOT event.is_cancel

§
DROP FUNCTION IF EXISTS COMPUTE_OBS_NB_FROM_ITERATIONS
§
CREATE FUNCTION COMPUTE_OBS_NB_FROM_ITERATIONS(p_id_transition_matrix INT, p_itnumber_min INT, p_itnumber_max INT, p_row_idx TINYINT, p_column_idx TINYINT) RETURNS INT
BEGIN
	DECLARE vResult INT(11);
	SET vResult = (SELECT IFNULL(SUM(obs_number),0)
			FROM transition_matrix_iteration AS it
			JOIN transition_matrix_cell_iteration AS it_cell ON it_cell.id_transition_matrix_iteration = it.id
			WHERE it.id_transition_matrix = p_id_transition_matrix
				AND it.number >=p_itnumber_min
				AND it.number <=p_itnumber_max
				AND it_cell.row_idx  = p_row_idx
				AND it_cell.column_idx  = p_column_idx
				AND it.begin_date >= '2021-04-20'
			);
	RETURN vResult
	;
END
§

DROP FUNCTION IF EXISTS COMPUTE_STATE_VARIANCE
§
CREATE FUNCTION COMPUTE_STATE_VARIANCE(p_id_transition_matrix INT) RETURNS DECIMAL(10,5)
BEGIN
	DECLARE vResult DECIMAL(10,5);
	SET vResult = (SELECT VARIANCE(numerator/denominator) FROM
		(SELECT row_idx, 
			SUM(obs_number*column_idx) AS numerator,
			SUM(obs_number) AS denominator
			FROM transition_matrix_cell tmc
			WHERE id_transition_matrix = p_id_transition_matrix 
			GROUP BY row_idx
		) AS select1
	);
	RETURN vResult
	;
END
§


DROP FUNCTION IF EXISTS CORRECT_OBS_NUMBERS
§
CREATE PROCEDURE CORRECT_OBS_NUMBERS(IN p_id_context INT, p_start_hour INT, p_variable_name VARCHAR(32), p_row_idx TINYINT , p_column_idx TINYINT , p_obs_number INT)
BEGIN
	DECLARE v_id_time_window INT(11);
	DECLARE v_id_transition_matrix INT(11);
	DECLARE v_id_iteration INT(11);
	SET v_id_time_window=(SELECT id from time_window where start_hour=p_start_hour)
	;
	SET v_id_transition_matrix = (SELECT id FROM transition_matrix WHERE id_context  = p_id_context AND variable_name = p_variable_name AND id_time_window = v_id_time_window LIMIT 0,1)
	;
	SET v_id_iteration = (SELECT id FROM transition_matrix_iteration where id_transition_matrix = v_id_transition_matrix ORDER BY `number`  DESC limit 0,1)
	;
	INSERT INTO transition_matrix_cell_iteration(id_transition_matrix_iteration, id_transition_matrix, row_idx ,column_idx, obs_number) values
		 (v_id_iteration,v_id_transition_matrix,p_row_idx,p_column_idx,p_obs_number)
		ON DUPLICATE KEY UPDATE obs_number = p_obs_number
	;
  -- CALL REFRESH_TRANSITION_MATRIX_CELL(3)
END
§


DROP FUNCTION IF EXISTS GET_ITERATION_ID
§
CREATE FUNCTION GET_ITERATION_ID(p_id_transition_matrix INT, p_date VARCHAR(100) ) RETURNS INT UNSIGNED
BEGIN
	DECLARE vResult INT(11) UNSIGNED;
	SET vResult = (SELECT id FROM transition_matrix_iteration WHERE
		 id_transition_matrix=p_id_transition_matrix
		  AND begin_date<=p_date AND end_date > p_date
		LIMIT 0,1)
	;
	RETURN vResult
	;
END

§
DROP PROCEDURE IF EXISTS REFRESH_TRANSITION_MATRIX_CELL2
§
CREATE PROCEDURE REFRESH_TRANSITION_MATRIX_CELL2(in p_id_context INT, p_id_time_window INT, p_max_iteration_nb INT)
BEGIN
	DROP TEMPORARY TABLE IF EXISTS TmpComputeObsNb
	;
	CREATE TEMPORARY TABLE TmpComputeObsNb AS
		SELECT
			  cell_it.id_transition_matrix
			 ,cell_it.row_idx
			 ,cell_it.column_idx
			 ,SUM(obs_number) AS new_obs_number
			 ,SUM(corrections_number) AS new_corrections_number
		FROM transition_matrix AS tr_mtx
		JOIN transition_matrix_cell_iteration AS cell_it ON cell_it.id_transition_matrix = tr_mtx.id
		JOIN transition_matrix_iteration AS tmi ON tmi.id = cell_it.id_transition_matrix_iteration
		WHERE tr_mtx.id_context = p_id_context
			AND (tr_mtx.id_time_window = p_id_time_window OR p_id_time_window IS NULL)
		 AND tmi.number >= IF(tr_mtx.iteration_number > p_max_iteration_nb,tr_mtx.iteration_number - p_max_iteration_nb,0)
		 AND tmi.number <= tr_mtx.iteration_number
		 GROUP BY id_transition_matrix , row_idx , column_idx
	;
	-- The sum (obs number + corrections number) must be positive (e.g. >=1) if a negative correction number si set.
	UPDATE TmpComputeObsNb SET new_corrections_number = 1-new_obs_number WHERE  new_corrections_number < 0 AND new_corrections_number < 1-new_obs_number
	;
	INSERT INTO transition_matrix_cell(id_transition_matrix ,row_idx,column_idx,obs_number,corrections_number)
		SELECT id_transition_matrix, row_idx, column_idx, new_obs_number, new_corrections_number
		FROM TmpComputeObsNb WHERE (new_obs_number + new_corrections_number) > 0
		ON DUPLICATE KEY UPDATE obs_number = TmpComputeObsNb.new_obs_number
			, corrections_number = TmpComputeObsNb.new_corrections_number
	;
	-- DELETE cell with a new obs number and correction number equal to zero
	DELETE cell.* FROM TmpComputeObsNb
		JOIN transition_matrix_cell AS cell ON cell.id_transition_matrix = TmpComputeObsNb.id_transition_matrix
			AND cell.row_idx = TmpComputeObsNb.row_idx
			AND cell.column_idx = TmpComputeObsNb.column_idx
		WHERE TmpComputeObsNb.new_obs_number = 0 AND TmpComputeObsNb.new_corrections_number = 0
	;
	-- DELETE cells from matrices that have no link with TmpComputeObsNb
	DELETE cell.* FROM transition_matrix AS tr_mtx
		JOIN transition_matrix_cell AS cell ON cell.id_transition_matrix = tr_mtx.id
		LEFT JOIN TmpComputeObsNb ON TmpComputeObsNb.id_transition_matrix = cell.id_transition_matrix
			AND TmpComputeObsNb.row_idx = cell.row_idx
			AND TmpComputeObsNb.column_idx = cell.column_idx
		WHERE tr_mtx.id_context = p_id_context
				AND (tr_mtx.id_time_window = p_id_time_window OR p_id_time_window IS NULL)
				AND TmpComputeObsNb.id_transition_matrix IS NULL
	;
 END

§

GRANT EXECUTE ON FUNCTION GET_EFFECTIVE_END_DATE TO 'learning_agent'@'%'
§
GRANT EXECUTE ON PROCEDURE CORRECT_OBS_NUMBERS TO 'learning_agent'@'%'
§