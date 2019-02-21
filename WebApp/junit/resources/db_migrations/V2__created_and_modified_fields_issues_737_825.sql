
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
UPDATE calibration set modified=now();

ALTER TABLE calibration_data ADD COLUMN modified DATETIME;
UPDATE calibration_data set modified=now();

ALTER TABLE data_file ADD COLUMN modified DATETIME;
UPDATE data_file set modified=now();

ALTER TABLE dataset ADD COLUMN modified DATETIME;
UPDATE dataset set modified=now();

ALTER TABLE dataset_data ADD COLUMN modified DATETIME;
UPDATE dataset_data set modified=now();

ALTER TABLE equilibrator_pco2 ADD COLUMN modified DATETIME;
UPDATE equilibrator_pco2 set modified=now();

ALTER TABLE file_column ADD COLUMN modified DATETIME;
UPDATE file_column set modified=now();

ALTER TABLE file_definition ADD COLUMN modified DATETIME;
UPDATE file_definition set modified=now();

ALTER TABLE instrument ADD COLUMN modified DATETIME;
UPDATE instrument set modified=now();

ALTER TABLE job ADD COLUMN modified DATETIME;
UPDATE job set modified=now();

ALTER TABLE run_type ADD COLUMN modified DATETIME;
UPDATE run_type set modified=now();

ALTER TABLE user ADD COLUMN modified DATETIME;
UPDATE user set modified=now();


-- Rollback:
/***************************************************************************************************
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