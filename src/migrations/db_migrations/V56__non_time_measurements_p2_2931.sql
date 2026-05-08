-- Part two of the migration for non-time based measurements

-- Add foreign key reference to coordinate id
SET SESSION foreign_key_checks = OFF;

ALTER TABLE sensor_values ADD CONSTRAINT sv_coord FOREIGN KEY (coordinate_id) REFERENCES coordinates(id), ALGORITHM = INPLACE, LOCK = NONE;
ALTER TABLE measurements ADD CONSTRAINT meas_coord FOREIGN KEY (coordinate_id) REFERENCES coordinates(id), ALGORITHM = INPLACE, LOCK = NONE;

-- Remove old sensor_values columns
ALTER TABLE sensor_values DROP FOREIGN KEY SENSORVALUE_DATASET, ALGORITHM = INPLACE, LOCK = NONE;
ALTER TABLE sensor_values DROP COLUMN dataset_id, ALGORITHM = INPLACE, LOCK = NONE;
ALTER TABLE sensor_values DROP COLUMN date, ALGORITHM = INPLACE, LOCK = NONE;

-- Remove old measurements columns
ALTER TABLE measurements DROP FOREIGN KEY measurement_dataset, ALGORITHM = INPLACE, LOCK = NONE;
ALTER TABLE measurements DROP COLUMN dataset_id, ALGORITHM = INPLACE, LOCK = NONE;
ALTER TABLE measurements DROP COLUMN date, ALGORITHM = INPLACE, LOCK = NONE;

-- Remove the original dataset start and end columns
-- and convert them to strings
ALTER TABLE dataset CHANGE start start VARCHAR(20);
ALTER TABLE dataset CHANGE end end VARCHAR(20);

SET SESSION foreign_key_checks = ON;

-- Add measurement basis columns
-- All existing instruments and variables are surface based
ALTER TABLE instrument ADD basis TINYINT NOT NULL AFTER platform_code;
UPDATE instrument SET basis = 1;
ALTER TABLE variables ADD allowed_basis INT AFTER visible;
UPDATE variables SET allowed_basis = 1;

-- Remove start and end date from files. We can't use these for anything
ALTER TABLE data_file CHANGE start_date start VARCHAR(20);
ALTER TABLE data_file CHANGE end_date end VARCHAR(20);

-- Store the DataFile class
ALTER TABLE file_definition ADD file_class VARCHAR(45) DEFAULT 'TimeDataFile' NOT NULL AFTER datetime_spec;
