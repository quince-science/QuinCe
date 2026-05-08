-- Part two of the migration for non-time based measurements

-- Add foreign key reference to coordinate id
ALTER TABLE sensor_values ADD CONSTRAINT sv_coord FOREIGN KEY (coordinate_id) REFERENCES coordinates(id);
ALTER TABLE measurements ADD CONSTRAINT meas_coord FOREIGN KEY (coordinate_id) REFERENCES coordinates(id);

-- Remove old sensor_values columns
DROP INDEX SV_DATASETID_DATE ON sensor_values;
ALTER TABLE sensor_values DROP FOREIGN KEY SENSORVALUE_DATASET;
ALTER TABLE sensor_values DROP COLUMN dataset_id;
ALTER TABLE sensor_values DROP COLUMN date;

-- Remove old measurements columns
DROP INDEX MEAS_DATASETID_DATE ON measurements;
ALTER TABLE measurements DROP FOREIGN KEY measurement_dataset;
ALTER TABLE measurements DROP COLUMN dataset_id;
ALTER TABLE measurements DROP COLUMN date;

-- Remove the original dataset start and end columns
ALTER TABLE dataset CHANGE start start VARCHAR(20);
ALTER TABLE dataset CHANGE end end VARCHAR(20);

-- Add measurement basis columns
-- All existing instruments and variables are surface based
ALTER TABLE instrument ADD basis TINYINT NOT NULL AFTER platform_code;
UPDATE instrument SET basis = 1;
ALTER TABLE variables ADD allowed_basis INT AFTER visible;
UPDATE variables SET allowed_basis = 1;

-- Remove start and end date from files. We can't use these for anything
ALTER TABLE data_file DROP COLUMN start_date;
ALTER TABLE data_file DROP COLUMN end_date;

-- Store the DataFile class
ALTER TABLE file_definition ADD file_class VARCHAR(45) DEFAULT 'TimeDataFile' NOT NULL AFTER datetime_spec;
