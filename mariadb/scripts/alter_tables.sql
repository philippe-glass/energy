
alter table event add `interruption_date`	DATETIME DEFAULT null after cancel_date

  
  alter table history add    `max_warning_duration` INT(10) unsigned NOT NULL DEFAULT 0 COMMENT 'max warning duration of requests'

  

alter table single_offer ADD `contract_time`  		DATETIME null after acceptance_time
alter table single_offer add log_cancel TEXT NOT NULL DEFAULT '' after log2

alter table history add  `max_warning_duration` INT(10) unsigned NOT NULL DEFAULT 0 COMMENT 'max warning duration of requests'
alter table history add   `max_warning_consumer`	VARCHAR(100) NULL


alter table history drop constraint unicity_date
alter table history add constraint UNIQUE KEY `unicity_date_loc` (`date`, `localization`)
alter table history drop column agent_url

alter table  link_history_active_event add   `locatin` 	VARCHAR(32) NOT NULL DEFAULT '' after agent


alter table history add `total_provided` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT 'total provided (KWH)' after total_produced

-- JUNE 2022

 alter table event drop column environmental_impact
 alter table event add `environmental_impact`  TINYINT UNSIGNED NOT NULL DEFAULT 0 after device_category 
 
 alter table event add interruption_date DATETIME DEFAULT null after cancel_date
 
  alter table device drop column environmental_impact
 alter table device add `environmental_impact`  TINYINT UNSIGNED NOT NULL DEFAULT 0 after category
 

select * from single_offer so  where id=241833 order by id desc
 
 
 -- 09/06/2022
 use energy1

 
ALTER table  history drop column `state_idx_produced`;
ALTER table  history drop column `state_idx_requested`;
ALTER table  history drop column `state_idx_consumed`;
ALTER table  history drop column `state_idx_available`;
ALTER table  history drop column `state_idx_missing`;
 
 alter table event add   `power_min`			DECIMAL(15,3) NOT null after power;
 alter table event add   `power_max`			DECIMAL(15,3) NOT null after power_min;


 alter table single_offer add  `power_min`			DECIMAL(15,3) NOT null after power;
 alter table single_offer add   `power_max`			DECIMAL(15,3) NOT null after power_min;
 
 


