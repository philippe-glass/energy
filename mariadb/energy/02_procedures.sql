DELIMITER §

DROP FUNCTION IF EXISTS COMPUTE_WARNING_SUM3
§
DROP FUNCTION IF EXISTS GET_EFFECTIVE_END_DATE
§
DROP FUNCTION IF EXISTS COMPUTE_OBS_NB_FROM_ITERATIONS
§
DROP FUNCTION IF EXISTS COMPUTE_STATE_VARIANCE
§
DROP PROCEDURE IF EXISTS CORRECT_OBS_NUMBERS
§
DROP PROCEDURE IF EXISTS compute_missing_request
§
DROP PROCEDURE IF EXISTS compute_missing_request2
§
DROP PROCEDURE IF EXISTS UPDATE_TRANSITION_MATRIX_CELL
§
DROP PROCEDURE IF EXISTS REFRESH_TRANSITION_MATRIX_CELL
§
DROP FUNCTION IF EXISTS GET_FIRST_WARNING
§
DROP FUNCTION IF EXISTS GET_DATE_FIRST_WARNING
§
DROP FUNCTION IF EXISTS GET_ITERATION_ID
§







DROP FUNCTION IF EXISTS COMPUTE_WARNING_SUM4
§
CREATE FUNCTION COMPUTE_WARNING_SUM4(p_id_histo INT, p_available DECIMAL(15,3), p_warning1 DECIMAL(15,3)) RETURNS DECIMAL(15,3)
BEGIN
	DECLARE result		  		DECIMAL(15,3);
	DECLARE vWarningMissing  	DECIMAL(15,3);
	DECLARE done INT DEFAULT false;
	DECLARE cursorWarningReq CURSOR FOR
		SELECT UnReq.missing
		FROM link_history_active_event AS UnReq
		WHERE UnReq.id_history = p_id_histo AND UnReq.has_warning_req AND UnReq.warning_duration > 0
		ORDER BY 1*warning_duration DESC ;
   DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = true
   ;
   SET result = 0
   ;
   IF(p_available > 0 ) THEN
		IF(p_warning1  <= p_available) THEN
			SET result = p_warning1
			;
		ELSE
			OPEN cursorWarningReq
			;
			FETCH cursorWarningReq INTO vWarningMissing
			;
			WHILE(NOT done) DO
		        IF(result + vWarningMissing <= p_available) THEN
				    SET result = result + vWarningMissing
				    ;
				END IF
				;
		        FETCH cursorWarningReq INTO vWarningMissing
		        ;
		    END WHILE
		    ;
			CLOSE cursorWarningReq
			;
		END IF
		;
	END IF
	;
	RETURN result
	;
END
§



§



DROP PROCEDURE IF EXISTS SP_COMPUTE_STATE_DISTRIBUTION
§
CREATE PROCEDURE SP_COMPUTE_STATE_DISTRIBUTION(IN p_id_context INT, p_creation_date_min DATETIME, p_creation_date_max DATETIME, p_date_min DATETIME, p_date_max DATETIME, p_variable VARCHAR(32) )
BEGIN
	 DROP TEMPORARY TABLE IF EXISTS TmpGroupedStates
	 ;
	 CREATE TEMPORARY TABLE TmpGroupedStates AS
		SELECT date(creation_date) AS compute_day, hour(date)  AS hour, date(date) AS date, state_name , state_idx, variable_name, count(*) AS nb
		FROM state_history h
		-- selection of state history : use the date min/max filter
		WHERE h.id_context  = p_id_context
			AND (p_creation_date_min IS NULL OR h.creation_date  >= DATE(p_creation_date_min))
			AND (p_creation_date_max IS NULL OR DATE(h.creation_date) <= DATE(p_creation_date_max))
			AND (p_date_min IS NULL OR h.date >= DATE(p_date_min))
			AND (p_date_max IS NULL OR DATE(h.date) <= DATE(p_date_max))
			AND (p_variable IS NULL OR  p_variable = variable_name)
			AND NOT observation_update IS null -- obsevation must be set on the corresponding transition matrix
		GROUP BY date(creation_date), date, hour, variable_name, state_idx
	 ;
	 ALTER TABLE TmpGroupedStates ADD KEY _key1(compute_day, date, hour, variable_name)
	 ;
	 DROP TEMPORARY TABLE IF EXISTS TmpStateDistrib1
	;
	CREATE TEMPORARY TABLE TmpStateDistrib1 AS
		SELECT select1.date
			,select1.compute_day
			,select1.hour
			,select1.variable_name
			,select1.state_name
			,select1.state_idx
			,select1.nb
			,select2.total_nb
			,select1.nb/select2.total_nb as ratio
			,CONCAT(state_name, ':', ROUND(nb/total_nb,2)) as Label
			,CONCAT(state_name, ':', nb) as Label2
			FROM
					(
						SELECT compute_day, date, hour, state_name, state_idx, variable_name, SUM(nb) AS nb
						FROM TmpGroupedStates
						GROUP BY compute_day, date, hour, variable_name, state_idx
						ORDER BY hour, nb DESC
					) AS select1
			JOIN
					(
						SELECT compute_day, date, hour, variable_name, SUM(nb) AS total_nb
						FROM TmpGroupedStates
						GROUP BY compute_day, date, hour, variable_name
					) AS select2
			ON select2.compute_day = select1.compute_day
				AND select2.date = select1.date
				AND select2.hour = select1.hour
				AND select2.variable_name = select1.variable_name
	 ;
	 ALTER TABLE TmpStateDistrib1 add key _key1(compute_day, date, hour, variable_name)
	 ;
	 DROP TEMPORARY TABLE IF EXISTS TmpStateDistribution
	 ;
	 CREATE TEMPORARY TABLE TmpStateDistribution
		 SELECT date AS date
			, compute_day
			, hour
			, variable_name
			, GROUP_CONCAT(Label, '  '  ORDER BY nb DESC) AS state_distribution
			, SUM(nb) AS total_nb
			, STDDEV(state_idx)* 100 / SUM(nb) AS state_std_deviation
			, SUM(ratio*(1 - ratio))  AS gini_index
			, -1*SUM(ratio * LOG2(ratio)) AS shannon_entropie
			, SUM(ratio)  AS sum_ratio
			-- , SUM(ratio) AS check_sum
			FROM TmpStateDistrib1
			GROUP BY date, hour, variable_name
	;
	ALTER TABLE TmpStateDistribution ADD KEY _key1(compute_day, date, hour, variable_name)
	;
END


§
DROP FUNCTION IF EXISTS COMPUTE_VECTOR_DIFFERENTIAL
§
CREATE FUNCTION COMPUTE_VECTOR_DIFFERENTIAL(p_id_prediction INT) RETURNS DECIMAL(10,5)
BEGIN
	DECLARE vCheckup1 BIT;
	DECLARE vCheckup2 DECIMAL(10,5);
	DECLARE vResult DECIMAL(10,5);

	SET vCheckup1 = (SELECT DATE_ADD(p.target_date, INTERVAL 1 HOUR)  < NOW() from prediction p where id=p_id_prediction)
	;
	IF(vCheckup1) THEN
		SET vCheckup2 = (SELECT IFNULL(SUM(TmpStateDistrib1.ratio),0)
				FROM prediction p
				JOIN TmpStateDistrib1 ON
						TmpStateDistrib1.date = Date(p.target_date)
					AND TmpStateDistrib1.hour = HOUR(p.target_date)
					AND TmpStateDistrib1.variable_name = p.variable_name
				WHERE p.id = p_id_prediction)
		;
		IF(ABS(vCheckup2 - 1) < 0.01) THEN
			SET vResult = (SELECT 0.5* SUM(ABS(pi2.proba - IFNULL(TmpStateDistrib1.ratio,0)))
				FROM prediction p
				JOIN prediction_item pi2 on pi2.id_prediction = p.id
				LEFT JOIN TmpStateDistrib1 ON
						TmpStateDistrib1.date =  Date(p.target_date)
					AND TmpStateDistrib1.hour = HOUR(p.target_date)
					AND TmpStateDistrib1.variable_name = p.variable_name
					AND TmpStateDistrib1.state_idx = pi2.state_idx
				WHERE p.id = p_id_prediction)
			;
		END IF
		;
	end if;
	RETURN vResult
	;
END


§
DROP PROCEDURE IF EXISTS SP_CONSOLIDATE_PREDICTIONS
§
CREATE PROCEDURE SP_CONSOLIDATE_PREDICTIONS(IN p_id_context INT, p_min_creation_date DATETIME, p_max_creation_date DATETIME)
BEGIN
	DECLARE v_min_target_date DATETIME;
	DECLARE v_min_creation_date DATETIME;
	DECLARE v_min_target_date2 DATETIME;

	DROP  TABLE IF EXISTS TmpPrediction
	;
	DROP TEMPORARY TABLE IF EXISTS TmpPrediction
	;
	DROP TEMPORARY table IF EXISTS TmpPrediction
	;
	CREATE TEMPORARY TABLE TmpPrediction (id_target_state_histo INT UNSIGNED NULL
			, is_slot_ok BIT default 0
			, link_done BIT default 0
			) AS
		SELECT id, horizon_minutes
			, p.creation_date
			, p.target_date
			, variable_name
			, id_target_state_histo
			, 0 AS is_slot_ok
			, 0 AS link_done
			, DATE_ADD(p.target_date, INTERVAL  -120 SECOND) AS date_min
			, DATE_ADD(p.target_date, INTERVAL  +120 SECOND) AS date_max
			, UNIX_TIMESTAMP(creation_date) AS ut_creation_date
			, UNIX_TIMESTAMP(target_date) AS ut_target_date
			, UNIX_TIMESTAMP(target_date) - 120 AS ut_target_date_min
			, UNIX_TIMESTAMP(target_date) + 120 AS ut_target_date_max
			, UNIX_TIMESTAMP(target_date) - UNIX_TIMESTAMP(target_date) % 3600 AS slot_target_date
		FROM prediction p
		-- WHERE p.target_date  >= p_min_date AND p.target_date < p_max_date AND id_target_state_histo IS NULL AND NOT link_done
		WHERE p.creation_date  >= p_min_creation_date
			AND p.creation_date < p_max_creation_date
			AND p.id_context  = p_id_context
			AND id_target_state_histo IS NULL AND NOT link_done
	;
	UPDATE TmpPrediction SET is_slot_ok = ut_target_date_min >= slot_target_date AND ut_target_date_max < slot_target_date + 3600
	;
	SET v_min_target_date = (SELECT MIN(target_date) FROM TmpPrediction)
	;
	SET v_min_creation_date = (SELECT MIN(creation_date) FROM TmpPrediction)
	;
	SET v_min_target_date2 = DATE_ADD(v_min_target_date, INTERVAL -1 HOUR)
	;
	DROP TABLE IF EXISTS TmpSH
	;
	DROP TEMPORARY TABLE IF EXISTS TmpSH
	;
	CREATE TEMPORARY TABLE TmpSH (ID INT UNSIGNED NOT NULL,
		PRIMARY KEY (`id`)
		) AS
			SELECT ID, date, UNIX_TIMESTAMP(date) AS ut_date
			, creation_date
			, UNIX_TIMESTAMP(creation_date) AS ut_creation_date
			, UNIX_TIMESTAMP(date) - UNIX_TIMESTAMP(date) % 3600 AS slot_date
			,variable_name,id_context,location,scenario
			FROM state_history AS sh
			WHERE sh.date >= v_min_target_date2
				-- and sh.date < @date2
				AND sh.creation_date  >= v_min_creation_date
				AND sh.id_context = p_id_context
				AND NOT sh.observation_update IS NULL
	;
	ALTER TABLE TmpSH ADD KEY(date)
	;
	ALTER TABLE TmpSH ADD KEY(ut_date, variable_name)
	;
	ALTER TABLE TmpSH ADD KEY(slot_date, ut_date, variable_name)
	;
	UPDATE TmpPrediction p SET id_target_state_histo = (SELECT sh.ID
			FROM TmpSH AS sh
			WHERE
					p.slot_target_date = sh.slot_date
				AND ABS(p.ut_target_date - sh.ut_date) < 120
				AND sh.variable_name  = p.variable_name
				AND ABS(p.ut_creation_date - sh.ut_creation_date) < 3600*24
	        ORDER BY ABS(p.ut_target_date - sh.ut_date), sh.ut_date
	        LIMIT 0,1)
	        WHERE p.id_target_state_histo IS NULL AND is_slot_ok
	;
	UPDATE TmpPrediction p SET id_target_state_histo = (SELECT sh.ID
			FROM TmpSH AS sh
			WHERE
					ABS(p.ut_target_date - sh.ut_date) < 120
				AND sh.variable_name  = p.variable_name
				AND ABS(p.ut_creation_date - sh.ut_creation_date) < 3600*24
	        ORDER BY ABS(p.ut_target_date - sh.ut_date), sh.ut_date
	        LIMIT 0,1)
	        WHERE p.id_target_state_histo IS NULL AND NOT is_slot_ok
	;
	UPDATE TmpPrediction p SET link_done = 1 WHERE NOT id_target_state_histo IS NULL
	;
	/*
	 *
	 SELECT * FROM prediction WHERE scenario = 'HomeSimulator' AND id_target_state_histo is null AND target_date >= '2022-07-01'
	UPDATE  prediction SET link_done = 1 WHERE scenario = 'HomeSimulator' AND id_target_state_histo is null AND target_date >= '2022-07-01'
	 *
	 * */
	 UPDATE TmpPrediction p SET p.link_done = 1  WHERE NOT link_done AND EXISTS (
			SELECT 1 FROM TmpSH WHERE TmpSH.variable_name = p.variable_name
					AND TmpSH.ut_date > p.ut_target_date
					AND TmpSH.ut_creation_date > p.ut_creation_date
			)
	;
	/*
	UPDATE TmpPrediction p
		JOIN (
			SELECT variable_name, MAX(ut_creation_date) AS ut_creation_date , MAX(ut_date) AS ut_date
			FROM TmpSH GROUP BY variable_name
		) AS TmpMaxSHdate ON TmpMaxSHdate.variable_name  = p.variable_name
		SET p.link_done = 1
		WHERE NOT link_done AND TmpMaxSHdate.ut_date > p.ut_target_date AND TmpMaxSHdate.ut_creation_date > p.ut_creation_date
	;*/
	UPDATE TmpPrediction
		JOIN prediction on prediction.ID = TmpPrediction.id
		SET prediction.id_target_state_histo = TmpPrediction.id_target_state_histo
		 ,prediction.link_done = TmpPrediction.link_done
	;
	UPDATE TmpPrediction
		JOIN prediction AS p ON p.ID = TmpPrediction.id
		JOIN state_history sh ON sh.id = p.id_target_state_histo
		SET p.delta_target_state_histo = UNIX_TIMESTAMP(sh.date)  - UNIX_TIMESTAMP(p.target_date)
	;
	CALL SP_COMPUTE_STATE_DISTRIBUTION(p_id_context, p_min_creation_date, p_max_creation_date, NULL, NULL, NULL)
	;
	UPDATE TmpPrediction
		JOIN prediction AS p ON p.ID = TmpPrediction.id
		SET p.vector_differential = COMPUTE_VECTOR_DIFFERENTIAL(p.id)
		WHERE  p.target_date <= DATE_ADD(NOW(), INTERVAL -1 HOUR)
			AND p.vector_differential IS NULL
		    AND NOT p.id_target_state_histo IS NULL
		    AND 1
	;
END





§
DROP PROCEDURE IF EXISTS SP_COMPUTE_PREDICTION_STATISTICS
§
CREATE PROCEDURE SP_COMPUTE_PREDICTION_STATISTICS(IN p_id_context INT
	, p_creation_date_min DATE
	, p_creation_date_max DATE
	, p_target_date_min DATE
	, p_target_date_max DATE
	, p_min_target_hour TINYINT
	, p_max_target_hour TINYINT
	, p_use_corrections BIT
	, p_variable_name VARCHAR(32))
BEGIN
	CALL SP_COMPUTE_STATE_DISTRIBUTION(p_id_context, p_creation_date_min, NULL, NULL, NULL, p_variable_name)
	;
	UPDATE prediction p SET p.vector_differential = COMPUTE_VECTOR_DIFFERENTIAL(p.id)
		WHERE (DATE(p.creation_date) >= DATE(p_creation_date_min))
			AND (DATE(p.creation_date) <= p_creation_date_max OR p_creation_date_max IS NULL)
			AND (DATE(p.target_date) >= DATE(p_target_date_min) OR p_target_date_min IS NULL)
			AND (DATE(p.target_date) <= p_target_date_max OR p_target_date_max IS NULL)
		    AND (p.target_hour  >= p_min_target_hour OR p_min_target_hour IS NULL)
		    AND (p.target_hour <= 0+p_max_target_hour OR p_max_target_hour IS NULL)
		    AND (p.use_corrections = p_use_corrections OR p_use_corrections IS NULL)
		    AND (p.variable_name = p_variable_name OR p_variable_name IS NULL)
		    AND p.target_date <= DATE_ADD(NOW(), INTERVAL -1 HOUR)
		    AND p.vector_differential IS NULL
		    AND NOT id_target_state_histo IS NULL
		    AND 1
	;/**/
	DROP TEMPORARY TABLE IF EXISTS TmpPredictionStatistic
	;
	DROP  TABLE IF EXISTS TmpPredictionStatistic
	;
	CREATE TEMPORARY TABLE TmpPredictionStatistic(
		`id` 					INT(11) UNSIGNED NOT NULL AUTO_INCREMENT
		,proba1					DECIMAL(10,5) NOT NULL
		,proba2					DECIMAL(10,5) NOT NULL
		,gini_index				DECIMAL(10,5) NOT NULL
		,shannon_entropie		DECIMAL(10,5) NOT NULL
		,vector_differential	DECIMAL(10,5) NULL
		,PRIMARY KEY (`id`)
	) AS
		SELECT location
		,scenario
		,id_context
		,variable_name
		-- ,Date(initial_date) as date
		-- ,hour(initial_date) as time_slot
		,creation_day AS compute_day
		,target_day AS target_day
		,target_hour AS time_slot
		,DATE_ADD(target_day, INTERVAL target_hour HOUR ) AS min_target_date
		,DATE_ADD(target_day, INTERVAL 1+target_hour HOUR ) AS max_target_date
		-- ,MAX(target_date) AS max_target_date
		,horizon_minutes AS horizon
		,use_corrections
		,id_initial_time_window
		,COUNT(*) AS nb_total
		,SUM(is_ok1) AS nb_ok1
		,SUM(is_ok2) AS nb_ok2
		,SUM(is_ok1) / SUM(1) AS rate_ok1
		,SUM(is_ok2) / SUM(1) AS rate_ok2
		,SUM(has_correction) AS corrections_number
		,MIN(creation_date) AS creation_datetime
		,AVG(proba_random) AS proba1
		,AVG(proba_likely) AS proba2
		,0 AS gini_index
		,0 AS shannon_entropie
		,AVG(vector_differential) AS vector_differential
		,MAX(has_states_distrib) AS has_states_distrib
		,GROUP_CONCAT(distinct id_correction) AS list_id_correction
		FROM (
			 SELECT p.*
			    ,sh.`date`
			    ,sh.state_idx
			    ,sh.state_name
			    ,(p.random_state_idx = sh.state_idx) AS is_ok1
			    ,(p.likely_state_idx = sh.state_idx) AS is_ok2
			    ,p.random_state_proba AS proba_random
			    ,p.likely_state_proba AS proba_likely
			    ,ABS(CONVERT(p.random_state_idx , signed)   -  CONVERT(sh.state_idx, signed) ) as delta_abs
			    ,(CONVERT(p.random_state_idx , signed)   -  CONVERT(sh.state_idx, signed) ) as delta
			    ,IF(p.id_correction IS NULL, 0, 1) AS has_correction
			   --  ,COMPUTE_VECTOR_DIFFERENTIAL(p.id) AS vector_differential
			    -- ,TmpStateDistrib1.vector_differential
			    -- ,0 AS vector_differential
			    ,(SELECT IFNULL(SUM(TmpStateDistrib1.ratio),0)
					FROM TmpStateDistrib1 WHERE
						TmpStateDistrib1.date =  Date(p.target_date)
				    AND TmpStateDistrib1.hour = HOUR(p.target_date)
				    AND TmpStateDistrib1.variable_name = p.variable_name) as has_states_distrib
			 FROM prediction p
			 JOIN state_history AS sh ON sh.id = p.id_target_state_histo
			 WHERE p.creation_day >=DATE(p_creation_date_min)
			    AND (p.creation_day <= p_creation_date_max OR p_creation_date_max IS NULL)
			    AND (p.target_hour >= p_min_target_hour OR p_min_target_hour IS NULL)
			    AND (p.target_hour <= 0+p_max_target_hour OR p_max_target_hour IS NULL)
			    AND (p.use_corrections = p_use_corrections OR p_use_corrections IS NULL)
			    AND (p.variable_name = p_variable_name OR p_variable_name IS NULL)
			    AND 1
	    ) AS Result
	    GROUP BY creation_day, target_day, target_hour, variable_name , horizon, use_corrections
	    HAVING nb_total >= 30
	    -- ORDER BY rate_OK2 desc
	;
	UPDATE TmpPredictionStatistic
		JOIN TmpStateDistribution AS TmpSD ON TmpSD.date = TmpPredictionStatistic.target_day
				AND TmpSD.hour = TmpPredictionStatistic.time_slot
				AND TmpSD.variable_name = TmpPredictionStatistic.variable_name
		SET TmpPredictionStatistic.gini_index = TmpSD.gini_index
		,TmpPredictionStatistic.shannon_entropie = TmpSD.shannon_entropie
	;
	/*
	drop temporary table if EXISTS TmpPred
	;
	create temporary table TmpPred as
		select TmpPredictionStatistic.id as id_statistic
			 ,TmpPredictionStatistic.Date, TmpPredictionStatistic.time_slot, TmpPredictionStatistic.variable_name
			 ,TmpPredictionStatistic.horizon, TmpPredictionStatistic.use_corrections
				, p.id as id_prediction
				FROM TmpPredictionStatistic
				JOIN prediction p on p.target_date >= TmpPredictionStatistic.min_target_date --  Date(p.target_date) = TmpPredictionStatistic.date
					and  p.target_date < TmpPredictionStatistic.max_target_date
					AND p.variable_name  = TmpPredictionStatistic.variable_name
					AND p.horizon_minutes  = TmpPredictionStatistic.horizon
					AND p.use_corrections  = TmpPredictionStatistic.use_corrections
	;*/
	-- Retrieve average probabilité of each state
	DROP TEMPORARY TABLE IF EXISTS TmpPredictionStatisticLine
	;
	CREATE TEMPORARY TABLE TmpPredictionStatisticLine
		-- id_statistic INT(11) UNSIGNED NOT NULL
		-- (CONSTRAINT `fk_id_statistic1` FOREIGN KEY (`id_statistic`) REFERENCES `TmpPredictionStatistic` (`id`)
		AS
			SELECT DATE(p.creation_date) AS compute_day
			, target_day  AS target_day
			, target_hour AS hour, p.variable_name
			, p.horizon_minutes AS horizon , p.use_corrections
			, pi2.state_idx, pi2.state_name
			, avg(pi2.proba) AS proba
			FROM prediction p
			JOIN prediction_item pi2 on pi2.id_prediction =p.id
			WHERE p.creation_date >= DATE(p_creation_date_min)
			    AND (p.target_hour >= p_min_target_hour OR p_min_target_hour IS NULL)
			    AND (p.target_hour <= 0+p_max_target_hour OR p_max_target_hour IS NULL)
			    AND (p.variable_name = p_variable_name OR p_variable_name IS NULL)
			    AND 1
			GROUP BY DATE(p.creation_date), p.target_day, p.target_hour, p.variable_name , p.horizon_minutes , p.use_corrections, pi2.state_idx
		;
		/*
		SELECT
			TmpPred.id_statistic,
			TmpPred.Date, TmpPred.time_slot, TmpPred.variable_name,
			TmpPred.horizon, TmpPred.use_corrections,
			pi2.state_idx ,
			pi2.state_name ,
			Count(*) as Nb,
			avg(pi2.proba) as proba
		FROM TmpPredictionStatistic
		JOIN prediction p on p.target_date >= TmpPredictionStatistic.min_target_date --  Date(p.target_date) = TmpPredictionStatistic.date
			and  p.target_date < TmpPredictionStatistic.max_target_date
			AND p.variable_name  = TmpPredictionStatistic.variable_name
			AND p.horizon_minutes  = TmpPredictionStatistic.horizon
			AND p.use_corrections  = TmpPredictionStatistic.use_corrections
		JOIN prediction_item pi2 on pi2.id_prediction =  p.id
		WHERE 1
		GROUP BY TmpPredictionStatistic.ID, pi2.state_idx*/
	-- ALTER TABLE TmpPredictionStatisticLine ADD KEY(`id_statistic`)
	-- ;
END




§


GRANT EXECUTE ON FUNCTION COMPUTE_WARNING_SUM4 TO 'learning_agent'@'%'


DELIMITER §
