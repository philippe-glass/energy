


select * from state_history sh where variable_name = 'requested'
 and creation_date  >= '2022-07-23 07:28:21.000'
 and id_session = '20220722_225153_4603'
order by iD desc

;

SELECT * FROM clemap_data_light.meteo_data md order by id desc
;
alter table clemap_data_light.meteo_data add key if not exists _ut_timestamp(ut_timestamp)
;
drop table if exists tmp_prod_states
;
CREATE TEMPORARY table tmp_prod_states as 
	select 'FOO'
			-- Date(mr.timestamp) as Date
			,mr.`timestamp`
			,MONTH(mr.timestamp) as month
			,DATE(mr.timestamp) as date
			,HOUR(mr.timestamp) as hour1
			,dayofweek(mr.timestamp) as day_of_week
			,(select id from time_window tw where tw.start_hour <= HOUR(mr.timestamp) and tw.end_hour > HOUR(mr.timestamp)) as id_time_window
			,(select id from clemap_data_light.meteo_data 
					where meteo_data.ut_timestamp =  UNIX_TIMESTAMP(mr.timestamp) - UNIX_TIMESTAMP(mr.timestamp) % 3600) AS id_meteo_data
			,pmr.p as pv_production
			,10*78*12+pmr.p as total_production
			,((10*78*12+pmr.p) / 60000) as test1
			,((10*78*12+pmr.p) / 12000) as test2
			,1+FLOOR((10*78*12+pmr.p) / 12000) as state_id
			-- ,ROUND(AVG(pmr.p),0) as avg_p
			-- ,ROUND(STDDEV(pmr.p),0) as std_dev_p
			-- ,STDDEV(pmr.p)/AVG(pmr.p) as rel_std_dev_p
			FROM clemap_data_light.measure_record mr
			JOIN clemap_data_light.phase_measure_record pmr on pmr.id_measure_record =mr.id
			WHERE feature_type = '15_MN' and sensor_number = 'CH1022501234500000000000000326365'
				AND mr.timestamp >= '2022-06-01' -- AND timestamp < '2022-06-23'
;


alter table tmp_prod_states add start_hour INT(11) null after hour1;
alter table tmp_prod_states add gh DECIMAL(10,5) null;
alter table tmp_prod_states add ta DECIMAL(10,5) null;


alter table tmp_prod_states add gh_class tinyint null;
alter table tmp_prod_states add ta_class tinyint null;

update tmp_prod_states st
	join time_window tw on tw.id = st.id_time_window
	set st.start_hour = tw.start_hour
;


update tmp_prod_states st
	join  clemap_data_light.meteo_data on meteo_data.id = st.id_meteo_data
	set 
		 st.gh 			= meteo_data.gh
		,st.ta 			= meteo_data.ta
;

update tmp_prod_states set
		gh_class 	= FLOOR(gh/300)
		,ta_class	= if(ta>-99,FLOOR(ta/10),null)
;

select distinct ta, ta_class from tmp_prod_states order by ta
;

set @total_nb=(select Count(*) FROM tmp_prod_states)
;
drop temporary table if exists Tmp_entropie_hour
;
create temporary table Tmp_entropie_hour AS
	select start_hour, total_nb
		, SUM(ratio)
		, ROUND(SUM(-1*ratio*log2(ratio)),3) as entropie
		, total_nb/@total_nb as weight
		, total_nb/@total_nb * ROUND(SUM(-1*ratio*log2(ratio)),3) as weighted_entropie
		, GROUP_CONCAT(label order by ratio DESC) as Label
		FROM
		(
			select sub_total.*, total.total_nb 
				, nb/total_nb as ratio
				,CONCAT('S', state_id, ' : ', ROUND(nb/total_nb,2)) as label
			FROM
				(
					select start_hour, state_id, count(*) as nb
					FROM tmp_prod_states
					group by start_hour, state_id
				) as sub_total
				join (
					select start_hour, count(*) as total_nb, GROUP_CONCAT(distinct(state_id))
					FROM tmp_prod_states
					group by start_hour
				) as total
				on total.start_hour = sub_total.start_hour
		) as sub_total2
		group by start_hour
;

set @total_nb=(select Count(*) FROM tmp_prod_states)
;




drop temporary table if exists Tmp_entropie_hour_gh
;
create temporary table Tmp_entropie_hour_gh as (
		select start_hour,  gh_class, total_nb, SUM(ratio) as ratio
			, ROUND(SUM(-1*ratio*log2(ratio)),3) as entropie
			, total_nb/@total_nb as weight
			, total_nb/@total_nb * ROUND(SUM(-1*ratio*log2(ratio)),3) as weighted_entropie
			, GROUP_CONCAT(label order by ratio DESC) as Label
			FROM
			(
				select sub_total.*, total.total_nb
					, nb/total_nb as ratio
					,CONCAT('S', state_id, ' : ', ROUND(nb/total_nb,2)) as label
				FROM
					(
						select start_hour, gh_class, state_id, count(*) as nb
						FROM tmp_prod_states
						where not gh_class is NULL
						group by start_hour, gh_class, state_id
					) as sub_total
					join (
						select start_hour,  gh_class, count(*) as total_nb, GROUP_CONCAT(distinct(state_id))
						FROM tmp_prod_states
						group by start_hour,  gh_class
					) as total
					on total.start_hour = sub_total.start_hour and total.gh_class = sub_total.gh_class
			) as sub_total2
			group by start_hour, gh_class
		)
;





drop temporary table if exists Tmp_entropie_hour_month_gh
;
create temporary table Tmp_entropie_hour_month_gh as (
		select start_hour, month, gh_class, total_nb, SUM(ratio) as ratio
			, ROUND(SUM(-1*ratio*log2(ratio)),3) as entropie
			, total_nb/@total_nb as weight
			, total_nb/@total_nb * ROUND(SUM(-1*ratio*log2(ratio)),3) as weighted_entropie
			, GROUP_CONCAT(label order by ratio DESC) as Label
			FROM
			(
				select sub_total.*, total.total_nb
					, nb/total_nb as ratio
					,CONCAT('S', state_id, ' : ', ROUND(nb/total_nb,2)) as label
				FROM
					(
						select start_hour, month, gh_class, state_id, count(*) as nb
						FROM tmp_prod_states
						where not gh_class is NULL
						group by start_hour, month, gh_class, state_id
					) as sub_total
					join (
						select start_hour, month, gh_class, count(*) as total_nb, GROUP_CONCAT(distinct(state_id))
						FROM tmp_prod_states
						group by start_hour, month, gh_class
					) as total
					on total.start_hour = sub_total.start_hour and total.month = sub_total.month and total.gh_class = sub_total.gh_class
			) as sub_total2
			group by start_hour, gh_class
		)
;







drop temporary table if exists Tmp_entropie_hour_dow_gh
;
create temporary table Tmp_entropie_hour_dow_gh as (
		select start_hour, day_of_week , gh_class, total_nb, SUM(ratio) as ratio
			, ROUND(SUM(-1*ratio*log2(ratio)),3) as entropie
			, total_nb/@total_nb as weight
			, total_nb/@total_nb * ROUND(SUM(-1*ratio*log2(ratio)),3) as weighted_entropie
			, GROUP_CONCAT(label order by ratio DESC) as Label
			FROM
			(
				select sub_total.*, total.total_nb 
					, nb/total_nb as ratio
					,CONCAT('S', state_id, ' : ', ROUND(nb/total_nb,2)) as label
				FROM
					(
						select start_hour, day_of_week, gh_class, state_id, count(*) as nb
						FROM tmp_prod_states
						where not gh_class is NULL
						group by start_hour, day_of_week, gh_class, state_id
					) as sub_total
					join (
						select start_hour, day_of_week, gh_class, count(*) as total_nb, GROUP_CONCAT(distinct(state_id))
						FROM tmp_prod_states
						group by start_hour, day_of_week, gh_class
					) as total
					on total.start_hour = sub_total.start_hour and total.day_of_week = sub_total.day_of_week and total.gh_class = sub_total.gh_class
			) as sub_total2
			group by start_hour, day_of_week, gh_class
		)
;




drop temporary table if exists Tmp_entropie_hour_ta
;
create temporary table Tmp_entropie_hour_ta as (
		select start_hour, ta_class, total_nb, SUM(ratio) as ratio
			, ROUND(SUM(-1*ratio*log2(ratio)),3) as entropie
			, total_nb/@total_nb as weight
			, total_nb/@total_nb * ROUND(SUM(-1*ratio*log2(ratio)),3) as weighted_entropie
			, GROUP_CONCAT(label order by ratio DESC) as Label
			FROM
			(
				select sub_total.*, total.total_nb 
					, nb/total_nb as ratio
					,CONCAT('S', state_id, ' : ', ROUND(nb/total_nb,2)) as label
				FROM
					(
						select start_hour, ta_class, state_id, count(*) as nb
						FROM tmp_prod_states
						where not ta_class is NULL
						group by start_hour, ta_class, state_id
					) as sub_total
					join (
						select start_hour, ta_class, count(*) as total_nb, GROUP_CONCAT(distinct(state_id))
						FROM tmp_prod_states
						group by start_hour, ta_class
					) as total
					on total.start_hour = sub_total.start_hour and total.ta_class = sub_total.ta_class
			) as sub_total2
			group by start_hour, ta_class
		)
;

-- select * from Tmp_entropie_hour_dow_gh

select 
	 (select SUM(weighted_entropie) from Tmp_entropie_hour) as entropie_hour
	,(select SUM(weighted_entropie) from Tmp_entropie_hour_gh) as entropie_hour_gh
	,(select SUM(weighted_entropie) from Tmp_entropie_hour_month_gh) as entropie_hour_month_gh
	,(select SUM(weighted_entropie) from Tmp_entropie_hour_dow_gh) as entropie_hour_dow_gh
	,(select SUM(weighted_entropie) from Tmp_entropie_hour_ta) as entropie_hour_ta
;
-- select * from state_history sh where variable_name = 'produced' order by id desc;


DELIMITER §

DROP TABLE IF EXISTS tmp_prod_states
§
CREATE TEMPORARY TABLE tmp_prod_states (
	 start_hour 	tinyint NULL
	,gh 			DECIMAL(10,5) NULL
	,ta 			DECIMAL(10,5) NULL
	,gh_class 		tinyint NULL
	,ta_class 		tinyint NULL
) AS
	SELECT 'FOO'
			,mr.`timestamp`
			,MONTH(mr.timestamp) 		AS month
			,DATE(mr.timestamp) 		AS date
			,HOUR(mr.timestamp) 		AS hour1
			,0					 		AS start_hour
			,dayofweek(mr.timestamp) 	AS day_of_week
			,(SELECT id from time_window tw where tw.start_hour <= HOUR(mr.timestamp) and tw.end_hour > HOUR(mr.timestamp)) AS id_time_window
			,(SELECT id from clemap_data_light.meteo_data 
					WHERE meteo_data.ut_timestamp =  UNIX_TIMESTAMP(mr.timestamp) - UNIX_TIMESTAMP(mr.timestamp) % 3600) AS id_meteo_data
			,pmr.p as pv_production
			,10*78*12 + pmr.p AS total_production
			,((10*78*12 + pmr.p) / 60000) AS test1
			,((10*78*12 + pmr.p) / 12000) AS test2
			,1 + FLOOR((10*78*12 + pmr.p) / 12000) AS state_id
			,NULL AS gh, NULL AS ta, NULL AS gh_class, NULL AS ta_class
			FROM clemap_data_light.measure_record mr
			JOIN clemap_data_light.phase_measure_record pmr on pmr.id_measure_record =mr.id
			WHERE feature_type = '15_MN' AND sensor_number = 'CH1022501234500000000000000326365'
				AND mr.timestamp >= '2022-06-01' -- AND timestamp < '2022-06-23'
§
UPDATE tmp_prod_states st
	JOIN time_window tw on tw.id = st.id_time_window
	SET st.start_hour = tw.start_hour
§
UPDATE tmp_prod_states st
	JOIN  clemap_data_light.meteo_data on meteo_data.id = st.id_meteo_data
	SET 
		 st.gh 			= meteo_data.gh
		,st.ta 			= meteo_data.ta
§
UPDATE tmp_prod_states SET
		gh_class 	= FLOOR(gh/300)
		,ta_class	= if(ta>-99,FLOOR(ta/10),null) 