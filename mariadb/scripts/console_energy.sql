	alter table history add   `location` 		VARCHAR(32) NOT NULL DEFAULT '' after agent_url;
alter table history add     `distance` 			TINYINT UNSIGNED NOT NULL DEFAULT 0.0 after localization;



alter table history drop constraint unicity_date
alter table history add constraint UNIQUE KEY `unicity_date_loc` (`date`, `localization`)
alter table history drop column agent_url

alter table  link_history_active_event add   `localization` 	VARCHAR(32) NOT NULL DEFAULT '' after agent


alter table transition_matrix CHANGE  `scope` `location` VARCHAR(32) NOT NULL DEFAULT '';

alter table prediction CHANGE  `scope` `location` VARCHAR(32) NOT NULL DEFAULT '';

alter table state_history CHANGE  `scope` `location` VARCHAR(32) NOT NULL DEFAULT '';

update transition_matrix set  location = '192.168.1.79:10009';
update prediction set  location = '192.168.1.79:10009';
update state_history set  location = '192.168.1.79:10009';




select * 