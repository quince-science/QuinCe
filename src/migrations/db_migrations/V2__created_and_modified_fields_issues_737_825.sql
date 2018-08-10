
-- created - column
ALTER TABLE calibration ADD COLUMN created TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE calibration_data ADD COLUMN created TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE data_file ADD COLUMN created TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE dataset ADD COLUMN created TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE dataset_data ADD COLUMN created TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE equilibrator_pco2 ADD COLUMN created TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE file_column ADD COLUMN created TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE file_definition ADD COLUMN created TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE instrument ADD COLUMN created TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE run_type ADD COLUMN created TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE user ADD COLUMN created TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- job table already has a default timestamp column. Rename this for consistency.
ALTER TABLE job CHANGE submitted created TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- last modified column. This needs special handling for mysql version < 5.6
ALTER TABLE calibration ADD COLUMN modified DATETIME;
CREATE TRIGGER `calibration_insert_trigger` BEFORE INSERT ON  `calibration` FOR EACH ROW SET NEW.modified=NOW();
CREATE TRIGGER `calibration_update_trigger` BEFORE UPDATE ON  `calibration` FOR EACH ROW SET NEW.modified=NOW();
UPDATE calibration set modified=now();

ALTER TABLE calibration_data ADD COLUMN modified DATETIME;
CREATE TRIGGER `calibration_data_insert_trigger` BEFORE INSERT ON  `calibration_data` FOR EACH ROW SET NEW.modified=NOW();
CREATE TRIGGER `calibration_data_update_trigger` BEFORE UPDATE ON  `calibration_data` FOR EACH ROW SET NEW.modified=NOW();
UPDATE calibration_data set modified=now();

ALTER TABLE data_file ADD COLUMN modified DATETIME;
CREATE TRIGGER `data_file_insert_trigger` BEFORE INSERT ON  `data_file` FOR EACH ROW SET NEW.modified=NOW();
CREATE TRIGGER `data_file_update_trigger` BEFORE UPDATE ON  `data_file` FOR EACH ROW SET NEW.modified=NOW();
UPDATE data_file set modified=now();

ALTER TABLE dataset ADD COLUMN modified DATETIME;
CREATE TRIGGER `dataset_insert_trigger` BEFORE INSERT ON  `dataset` FOR EACH ROW SET NEW.modified=NOW();
CREATE TRIGGER `dataset_update_trigger` BEFORE UPDATE ON  `dataset` FOR EACH ROW SET NEW.modified=NOW();
UPDATE dataset set modified=now();

ALTER TABLE dataset_data ADD COLUMN modified DATETIME;
CREATE TRIGGER `dataset_data_insert_trigger` BEFORE INSERT ON  `dataset_data` FOR EACH ROW SET NEW.modified=NOW();
CREATE TRIGGER `dataset_data_update_trigger` BEFORE UPDATE ON  `dataset_data` FOR EACH ROW SET NEW.modified=NOW();
UPDATE dataset_data set modified=now();

ALTER TABLE equilibrator_pco2 ADD COLUMN modified DATETIME;
CREATE TRIGGER `equilibrator_pco2_insert_trigger` BEFORE INSERT ON  `equilibrator_pco2` FOR EACH ROW SET NEW.modified=NOW();
CREATE TRIGGER `equilibrator_pco2_update_trigger` BEFORE UPDATE ON  `equilibrator_pco2` FOR EACH ROW SET NEW.modified=NOW();
UPDATE equilibrator_pco2 set modified=now();

ALTER TABLE file_column ADD COLUMN modified DATETIME;
CREATE TRIGGER `file_column_insert_trigger` BEFORE INSERT ON  `file_column` FOR EACH ROW SET NEW.modified=NOW();
CREATE TRIGGER `file_column_update_trigger` BEFORE UPDATE ON  `file_column` FOR EACH ROW SET NEW.modified=NOW();
UPDATE file_column set modified=now();

ALTER TABLE file_definition ADD COLUMN modified DATETIME;
CREATE TRIGGER `file_definition_insert_trigger` BEFORE INSERT ON  `file_definition` FOR EACH ROW SET NEW.modified=NOW();
CREATE TRIGGER `file_definition_update_trigger` BEFORE UPDATE ON  `file_definition` FOR EACH ROW SET NEW.modified=NOW();
UPDATE file_definition set modified=now();

ALTER TABLE instrument ADD COLUMN modified DATETIME;
CREATE TRIGGER `instrument_insert_trigger` BEFORE INSERT ON  `instrument` FOR EACH ROW SET NEW.modified=NOW();
CREATE TRIGGER `instrument_update_trigger` BEFORE UPDATE ON  `instrument` FOR EACH ROW SET NEW.modified=NOW();
UPDATE instrument set modified=now();

ALTER TABLE job ADD COLUMN modified DATETIME;
CREATE TRIGGER `job_insert_trigger` BEFORE INSERT ON  `job` FOR EACH ROW SET NEW.modified=NOW();
CREATE TRIGGER `job_update_trigger` BEFORE UPDATE ON  `job` FOR EACH ROW SET NEW.modified=NOW();
UPDATE job set modified=now();

ALTER TABLE run_type ADD COLUMN modified DATETIME;
CREATE TRIGGER `run_type_insert_trigger` BEFORE INSERT ON  `run_type` FOR EACH ROW SET NEW.modified=NOW();
CREATE TRIGGER `run_type_update_trigger` BEFORE UPDATE ON  `run_type` FOR EACH ROW SET NEW.modified=NOW();
UPDATE run_type set modified=now();

ALTER TABLE user ADD COLUMN modified DATETIME;
CREATE TRIGGER `user_insert_trigger` BEFORE INSERT ON  `user` FOR EACH ROW SET NEW.modified=NOW();
CREATE TRIGGER `user_update_trigger` BEFORE UPDATE ON  `user` FOR EACH ROW SET NEW.modified=NOW();
UPDATE user set modified=now();


-- Rollback:
/***************************************************************************************************
DROP TRIGGER calibration_insert_trigger;
DROP TRIGGER calibration_update_trigger;
DROP TRIGGER calibration_data_insert_trigger;
DROP TRIGGER calibration_data_update_trigger;
DROP TRIGGER data_file_insert_trigger;
DROP TRIGGER data_file_update_trigger;
DROP TRIGGER dataset_insert_trigger;
DROP TRIGGER dataset_update_trigger;
DROP TRIGGER dataset_data_insert_trigger;
DROP TRIGGER dataset_data_update_trigger;
DROP TRIGGER equilibrator_pco2_insert_trigger;
DROP TRIGGER equilibrator_pco2_update_trigger;
DROP TRIGGER file_column_insert_trigger;
DROP TRIGGER file_column_update_trigger;
DROP TRIGGER file_definition_insert_trigger;
DROP TRIGGER file_definition_update_trigger;
DROP TRIGGER instrument_insert_trigger;
DROP TRIGGER instrument_update_trigger;
DROP TRIGGER job_insert_trigger;
DROP TRIGGER job_update_trigger;
DROP TRIGGER run_type_insert_trigger;
DROP TRIGGER run_type_update_trigger;
DROP TRIGGER user_insert_trigger;
DROP TRIGGER user_update_trigger;

ALTER TABLE calibration DROP COLUMN created;
ALTER TABLE calibration_data DROP COLUMN created;
ALTER TABLE data_file DROP COLUMN created;
ALTER TABLE dataset DROP COLUMN created;
ALTER TABLE dataset_data DROP COLUMN created;
ALTER TABLE equilibrator_pco2 DROP COLUMN created;
ALTER TABLE file_column DROP COLUMN created;
ALTER TABLE file_definition DROP COLUMN created;
ALTER TABLE instrument DROP COLUMN created;
ALTER TABLE run_type DROP COLUMN created;
ALTER TABLE user DROP COLUMN created;

ALTER TABLE job CHANGE created submitted TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE calibration DROP COLUMN modified;
ALTER TABLE calibration_data DROP COLUMN modified;
ALTER TABLE data_file DROP COLUMN modified;
ALTER TABLE dataset DROP COLUMN modified;
ALTER TABLE dataset_data DROP COLUMN modified;
ALTER TABLE equilibrator_pco2 DROP COLUMN modified;
ALTER TABLE file_column DROP COLUMN modified;
ALTER TABLE file_definition DROP COLUMN modified;
ALTER TABLE instrument DROP COLUMN modified;
ALTER TABLE job DROP COLUMN modified;
ALTER TABLE run_type DROP COLUMN modified;
ALTER TABLE user DROP COLUMN modified;
***************************************************************************************************/