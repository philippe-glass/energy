DELIMITER §



CREATE TABLE sensor(
	`id` 					INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	`serial_number`			VARCHAR(64) NOT NULL,
	`name`					VARCHAR(64) NOT NULL,
	`firmeware_version`		VARCHAR(64) NOT NULL,
	`location`				TEXT NOT NULL,
	`comment`				TEXT NOT NULL DEFAULT '',
	`electrical_panel`		VARCHAR(32) NOT NULL,
	PRIMARY KEY (`id`),
	UNIQUE KEY (`serial_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8


§

CREATE TABLE sensor_input (
	`id` 					INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	`id_sensor`				INT(11) UNSIGNED NOT NULL,
	`phase`					ENUM('l1','l2','l3') COMMENT 'phase : 1 or 2 or 3',
	`panel_input`			VARCHAR(16) NOT NULL,
	`device_category` 		ENUM ('UNKNOWN','WATER_HEATING', 'HEATING', 'COOKING', 'SHOWERS', 'WASHING_DRYING', 'LIGHTING'
							, 'AUDIOVISUAL', 'COLD_APPLIANCES', 'ICT', 'OTHER', 'ELECTRICAL_PANEL'
							, 'WIND_ENG', 'SOLOR_ENG', 'EXTERNAL_ENG', 'BIOMASS_ENG', 'HYDRO_ENG')
							 DEFAULT 'UNKNOWN',
	`description`			TEXT NOT NULL,
	`is_disabled` 			BIT(1) NOT NULL DEFAULT b'0',
	PRIMARY KEY (`id`),
	CONSTRAINT `fk_id_sensor` FOREIGN KEY (`id_sensor`) REFERENCES `sensor` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8

§
CREATE TABLE measure_record(
	`id` 					INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	 `timestamp`			DATETIME,
	 `blob_name`			VARCHAR(128) NOT NULL,
	 `sensor_number`		VARCHAR(64) NOT NULL,
	 `sensor_number2`		VARCHAR(64) NOT NULL,
	`feature_type`			ENUM('MN', '15_MN', 'TEN_SEC'),
	PRIMARY KEY (`id`),
	KEY _blob_name (blob_name),
	KEY _timestamp(timestamp),
	KEY _blob_name_sensor_number(blob_name, sensor_number),
	UNIQUE KEY mr_unicity (blob_name, sensor_number, feature_type, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8


§
 CREATE TABLE phase_measure_record(
	`id` 					INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	`id_measure_record`		INT(11) UNSIGNED NOT NULL,
	`phase`					ENUM('l1','l2','l3') COMMENT 'phase : 1 or 2 or 3',
	`voltage`				DECIMAL(10,5) NULL COMMENT 'voltage',
	`intensity`				DECIMAL(10,5) NULL COMMENT 'current intensity ',
	`power_s`				DECIMAL(10,5) NULL COMMENT 'apparent power : s^2 = p^2 + q^2',
	`power_p`				DECIMAL(10,5) NULL COMMENT 'real(or active) power',
	`power_q`				DECIMAL(10,5) NULL COMMENT 'reactive power',
	`power_factor`			DECIMAL(10,5) NULL COMMENT 'power factor',
	`power_phi`				DECIMAL(10,5) NULL COMMENT 'power phase angle',
	`avg_energy`			DECIMAL(10,5) NULL COMMENT '',
	PRIMARY KEY (`id`),
	CONSTRAINT `fk_pmr_measure_record` FOREIGN KEY (`id_measure_record`) REFERENCES `measure_record` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8

§

CREATE TABLE meteo_data(
	`id` 					INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	`site`					VARCHAR(32) not NULL DEFAULT '',
	`YEAR`					INT(11) UNSIGNED NOT null default 0,
	`doy`					INT(11) UNSIGNED NOT NULL COMMENT 'day of year',
	`HOUR`					TINYINT UNSIGNED NOT NULL,
	`timestamp`				DATETIME,
	`ut_timestamp`			INT(11) UNSIGNED NULL COMMENT 'time in UNIX format',
	 `se`					DECIMAL(10,5) NULL COMMENT 'solar elevation at corresponding legal time ',
	 `az`					DECIMAL(10,5) NULL COMMENT 'azimuth at corresponding legal time', 
	 `gh`					DECIMAL(10,5) NULL COMMENT 'global on a horizontal plane irradiances [Wh/m2h] ', 
	 `dh`					DECIMAL(10,5) NULL COMMENT 'diffuse on a horizontal plane irradiances [Wh/m2h]', 
	 `bn`					DECIMAL(10,5) NULL COMMENT 'normal beam irradiances [Wh/m2h] ',
	 `ta`					DECIMAL(10,5) NULL COMMENT 'dry bulb temperature [deg celcius]',
	 `hr`					DECIMAL(10,5) NULL COMMENT 'relative humidity [%] ', 
	 `w`					DECIMAL(10,5) NULL COMMENT 'precipitable water content of the atmosphere [cm] ',
	 `vv`					DECIMAL(10,5) NULL COMMENT 'wind speed [m/s] ',
	 `dv`					DECIMAL(10,5) NULL COMMENT 'wind direction - from N=0Â° E=90Â° S=180 ° W=270 ° ',
	 `pr`					DECIMAL(10,5) NULL COMMENT 'atmospheric pressure at sea level [hp] ',
	 `gn`					DECIMAL(10,5) NULL COMMENT 'global irradiance on a vertical plane - north [Wh/m2h]',
	 `ge`					DECIMAL(10,5) NULL COMMENT 'global irradiance on a vertical plane - east  [Wh/m2h]',
	 `gs`					DECIMAL(10,5) NULL COMMENT 'global irradiance on a vertical plane - south [Wh/m2h]',
	 `gw`					DECIMAL(10,5) NULL COMMENT 'global irradiance on a vertical plane - west [Wh/m2h]',
	 `g35e`					DECIMAL(10,5) NULL COMMENT 'global irradiance on a 35 ° tilted plane - east [Wh/m2h]',
	 `g35s`					DECIMAL(10,5) NULL COMMENT 'global irradiance on a 35 ° tilted plane - south [Wh/m2h]',
	 `g35w`					DECIMAL(10,5) NULL COMMENT 'global irradiance on a 35 ° tilted plane - west [Wh/m2h]',
	 `g45s`					DECIMAL(10,5) NULL COMMENT 'global irradiance on a 45Â° tilted plane - south orientation [Wh/m2h] ',
	 `gtrack`				DECIMAL(10,5) NULL COMMENT 'global irradiance on a plane tracking the sun - perpendicular to the sun rays [Wh/m2h] ',
	 `g35_45`				DECIMAL(10,5) NULL COMMENT 'global irradiance on a 35Â° tilted and 45Â° oriented plane (south=0Â°) [Wh/m2h] ',
	 `min_sun`				DECIMAL(10,5) NULL COMMENT 'sunshine duration in minutes (threshold on beam irradiance: 120 [W/m2] ',
	 `ir`					DECIMAL(10,5) NULL COMMENT 'infrared radiation balance  [Wh/m2h]', 
	 `ird`					DECIMAL(10,5) NULL COMMENT 'incoming infrared radiation [Wh/m2h]', 
	 `irup`					DECIMAL(10,5) NULL COMMENT 'outgoing infrared radiation [Wh/m2h] ',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8



§

CREATE TABLE log_import_clemap(
	 `id` 					INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	 `creation_date` 		DATETIME NOT NULL DEFAULT current_timestamp(),
	 `blob_name`			VARCHAR(128) NOT NULL,
	 `sensor_number`		VARCHAR(64) NOT NULL,
	 `nb_records`			INT(11) UNSIGNED NOT NULL,
	 `nb_records_required`	INT(11) UNSIGNED NOT NULL,
	 `nb_imports`			INT(11) UNSIGNED NOT NULL,
	 `last_import_date`		DATETIME NOT NULL,
	PRIMARY KEY (`id`),
	KEY _blob_name (blob_name),
	KEY _sensor_number(sensor_number),
	-- KEY _blob_name_sensor_number(blob_name, sensor_number)
	UNIQUE KEY log_unicity (blob_name, sensor_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8


§
CREATE TABLE tmp_import_measure(
	`id` 					INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	 `timestamp`			DATETIME,
	 `blob_name`			VARCHAR(128) NOT NULL,
	 `sensor_number`		VARCHAR(64) NOT NULL,
	 `sensor_number2`		VARCHAR(64) NOT NULL,
	 `feature_type`			ENUM('MN', '15_MN', 'TEN_SEC'),
	 `id_measure_record`	INT(11) UNSIGNED NULL,
	 -- phase L1
	`l1_voltage`			DECIMAL(10,5) NULL COMMENT 'voltage',
	`l1_intensity`			DECIMAL(10,5) NULL COMMENT 'current intensity ',
	`l1_power_s`			DECIMAL(10,5) NULL COMMENT 'apparent power : s^2 = p^2 + q^2',
	`l1_power_p`			DECIMAL(10,5) NULL COMMENT 'real(or active) power',
	`l1_power_q`			DECIMAL(10,5) NULL COMMENT 'reactive power',
	`l1_power_factor`		DECIMAL(10,5) NULL COMMENT 'power factor',
	`l1_power_phi`			DECIMAL(10,5) NULL COMMENT 'power phase angle',
	`l1_avg_energy`			DECIMAL(10,5) NULL COMMENT '',
	 -- phase L2
	`l2_voltage`			DECIMAL(10,5) NULL COMMENT 'voltage',
	`l2_intensity`			DECIMAL(10,5) NULL COMMENT 'current intensity ',
	`l2_power_s`			DECIMAL(10,5) NULL COMMENT 'apparent power : s^2 = p^2 + q^2',
	`l2_power_p`			DECIMAL(10,5) NULL COMMENT 'real(or active) power',
	`l2_power_q`			DECIMAL(10,5) NULL COMMENT 'reactive power',
	`l2_power_factor`		DECIMAL(10,5) NULL COMMENT 'power factor',
	`l2_power_phi`			DECIMAL(10,5) NULL COMMENT 'power phase angle',
	`l2_avg_energy`			DECIMAL(10,5) NULL COMMENT '',
	-- phase L3
	`l3_voltage`			DECIMAL(10,5) NULL COMMENT 'voltage',
	`l3_intensity`			DECIMAL(10,5) NULL COMMENT 'current intensity ',
	`l3_power_s`			DECIMAL(10,5) NULL COMMENT 'apparent power : s^2 = p^2 + q^2',
	`l3_power_p`			DECIMAL(10,5) NULL COMMENT 'real(or active) power',
	`l3_power_q`			DECIMAL(10,5) NULL COMMENT 'reactive power',
	`l3_power_factor`		DECIMAL(10,5) NULL COMMENT 'power factor',
	`l3_power_phi`			DECIMAL(10,5) NULL COMMENT 'power phase angle',
	`l3_avg_energy`			DECIMAL(10,5) NULL COMMENT '',
	PRIMARY KEY (`id`),
	KEY _blob_name (blob_name),
	KEY _timestamp(timestamp),
	KEY _blob_name_sensor_number(blob_name, sensor_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8
