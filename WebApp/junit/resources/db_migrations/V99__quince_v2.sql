-- Create the sensor_types table, which used to be in a config file
CREATE TABLE sensor_types (
  id INT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  vargroup VARCHAR(100) NOT NULL,
  parent INT NULL,
  depends_on INT NULL,
  depends_question TEXT NULL,
  internal_calibration TINYINT NOT NULL DEFAULT 0,
  diagnostic TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE SENSORTYPENAME (name))
  ENGINE = InnoDB;

-- Intake temp
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Intake Temperature', 'Temperature', NULL, NULL, NULL, 0);

-- Salinity
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Salinity', 'Salinity', NULL, NULL, NULL, 0);

-- Equilibrator Temperature
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Equilibrator Temperature', 'Temperature', NULL, NULL, NULL, 0);

-- Equilibrator Pressure - parent of the two pressure types
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Equilibrator Pressure', 'Pressure', NULL, NULL, NULL, 0);

-- Equilibrator Pressure (absolute)
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Equilibrator Pressure (absolute)', 'Pressure',
    (SELECT id FROM sensor_types WHERE name = 'Equilibrator Pressure'),
    NULL, NULL, 0);

-- Equilibrator Pressure (differential)
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Equilibrator Pressure (differential)', 'Pressure',
    (SELECT id FROM sensor_types WHERE name = 'Equilibrator Pressure'),
    NULL, NULL, 0);

-- Ambient Pressure
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Ambient Pressure', 'Pressure', NULL, NULL, NULL, 0);

-- xH2O in gas
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('xH₂O in gas', 'Moisture', NULL, NULL, NULL, 1);

-- CO2 (Licor etc)
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('CO₂ in gas', 'CO₂', NULL, NULL, NULL, 1);

-- Atmospheric Pressure
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Atmospheric Pressure', 'Pressure', NULL, NULL, NULL, 0);

-- Equilibrator Pressure (differential) depends on Ambient Pressure
UPDATE sensor_types
  SET depends_on = (SELECT id FROM sensor_types WHERE name = 'Ambient Pressure')
  WHERE name = 'Equilibrator Pressure (differential)';

-- CO₂ sometimes needs to be mathematically dried, which needs xH₂O
UPDATE sensor_types
  SET depends_on = (SELECT id FROM sensor_types WHERE name = 'xH₂O in gas')
  WHERE name = 'CO₂ in gas';

UPDATE sensor_types
  SET depends_question = 'Do values from CO₂ require moisture adjustment?'
  WHERE name = 'CO₂ in gas';

-- Diagnostic sensors
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Diagnostic Temperature', 'Temperature', NULL, NULL, NULL, 0, 1);
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Diagnostic Pressure', 'Pressure', NULL, NULL, NULL, 0, 1);
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Diagnostic Gas Flow', 'Other', NULL, NULL, NULL, 0, 1);
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Diagnostic Water Flow', 'Other', NULL, NULL, NULL, 0, 1);
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Diagnostic Voltage', 'Other', NULL, NULL, NULL, 0, 1);
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Diagnostic Misc', 'Other', NULL, NULL, NULL, 0, 1);

------------------------------
-- Variable definition
CREATE TABLE variables (
  id INT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE VARNAME_UNIQUE (name)
  ) ENGINE = InnoDB;

INSERT INTO variables (name) VALUES ('Underway Marine pCO₂');

CREATE TABLE variable_sensors (
  variable_id INT NOT NULL,
  sensor_type INT NOT NULL,
  core TINYINT(1) NOT NULL DEFAULT 0,
  questionable_cascade TINYINT(1) NOT NULL,
  bad_cascade TINYINT(1) NOT NULL,
  PRIMARY KEY (variable_id, sensor_type),
  CONSTRAINT VARSENSOR_VARIABLE
    FOREIGN KEY (variable_id)
    REFERENCES variables (id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT VARSENSOR_SENSOR
    FOREIGN KEY (sensor_type)
    REFERENCES sensor_types (id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT);

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables),
    (SELECT id FROM sensor_types WHERE name = 'Intake Temperature'),
    0, 3, 4);

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables),
    (SELECT id FROM sensor_types WHERE name = 'Salinity'),
    0, 2, 3);

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables),
    (SELECT id FROM sensor_types WHERE name = 'Equilibrator Temperature'),
    0, 3, 4);

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables),
    (SELECT id FROM sensor_types WHERE name = 'Equilibrator Pressure'),
    0, 3, 4);

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables),
    (SELECT id FROM sensor_types WHERE name = 'CO₂ in gas'),
    1, 3, 4);

-- --------------------------------
-- Link instruments to variables

CREATE TABLE instrument_variables (
  instrument_id INT NOT NULL,
  variable_id INT(11) NOT NULL,
  CONSTRAINT INSTRVAR_INSTRUMENT
    FOREIGN KEY (instrument_id)
    REFERENCES instrument (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT INSTRVAR_VARIABLE
    FOREIGN KEY (variable_id)
    REFERENCES variables (id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT);

INSERT INTO instrument_variables (instrument_id, variable_id)
  SELECT id, 1 FROM instrument;

-- --------------------------------
-- Rejig file_column table

-- Rename existing sensor type column
ALTER TABLE file_column CHANGE COLUMN sensor_type sensor_type_old varchar(100);

-- Create new sensor_type foreign key column (no constraints yet)
ALTER TABLE file_column ADD COLUMN sensor_type INT NULL AFTER `primary_sensor`;

-- Migrate all values
UPDATE file_column SET sensor_type = (SELECT id FROM sensor_types WHERE name = 'Intake Temperature') WHERE sensor_type_old = 'Intake Temperature';
UPDATE file_column SET sensor_type = (SELECT id FROM sensor_types WHERE name = 'Salinity') WHERE sensor_type_old = 'Salinity';
UPDATE file_column SET sensor_type = (SELECT id FROM sensor_types WHERE name = 'Equilibrator Temperature') WHERE sensor_type_old = 'Equilibrator Temperature';
UPDATE file_column SET sensor_type = (SELECT id FROM sensor_types WHERE name = 'Equilibrator Pressure (absolute)') WHERE sensor_type_old = 'Equilibrator Pressure (absolute)';
UPDATE file_column SET sensor_type = (SELECT id FROM sensor_types WHERE name = 'Equilibrator Pressure (differential)') WHERE sensor_type_old = 'Equilibrator Pressure (differential)';
UPDATE file_column SET sensor_type = (SELECT id FROM sensor_types WHERE name = 'Ambient Pressure') WHERE sensor_type_old = 'Ambient Pressure';
UPDATE file_column SET sensor_type = (SELECT id FROM sensor_types WHERE name = 'xH₂O in gas') WHERE sensor_type_old = 'xH2O';
UPDATE file_column SET sensor_type = (SELECT id FROM sensor_types WHERE name = 'CO₂ in gas') WHERE sensor_type_old = 'CO2';
UPDATE file_column SET sensor_type = (SELECT id FROM sensor_types WHERE name = 'Diagnostic Temperature') WHERE sensor_type_old = 'Diagnostic: Temperature';
UPDATE file_column SET sensor_type = (SELECT id FROM sensor_types WHERE name = 'Diagnostic Pressure') WHERE sensor_type_old = 'Diagnostic: Pressure';
UPDATE file_column SET sensor_type = (SELECT id FROM sensor_types WHERE name = 'Diagnostic Air Flow') WHERE sensor_type_old = 'Diagnostic: Air Flow';
UPDATE file_column SET sensor_type = (SELECT id FROM sensor_types WHERE name = 'Diagnostic Water Flow') WHERE sensor_type_old = 'Diagnostic: Water Flow';
UPDATE file_column SET sensor_type = (SELECT id FROM sensor_types WHERE name = 'Diagnostic Voltage') WHERE sensor_type_old = 'Diagnostic: Voltage';


-- Run Type sensor has a special fixed ID
UPDATE file_column SET sensor_type = -1 WHERE sensor_type_old = 'Run Type';

-- Apply constraints to new sensor type column
ALTER TABLE file_column ALTER COLUMN sensor_type SET NOT NULL;

-- Remove the old column
ALTER TABLE file_column DROP COLUMN sensor_type_old;

-- Remove the post_calibrated field - no longer used
ALTER TABLE file_column DROP COLUMN post_calibrated;

-- Add ship speed and direction sensors
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Ship Speed', 'Other', NULL, NULL, NULL, 0);

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Ship Course', 'Other', NULL, NULL, NULL, 0);

-- Add wind speed and direction sensors. Speeds can be
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Wind Speed', 'Other', NULL, NULL, NULL, 0);

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Wind Direction (absolute)', 'Other', NULL, NULL, NULL, 0);

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Wind Direction (relative)', 'Other', NULL, NULL, NULL, 0);

CREATE TABLE sensor_values (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  dataset_id INT NOT NULL,
  file_column INT NOT NULL,
  date BIGINT(20) NOT NULL,
  value VARCHAR(100) NULL,
  CONSTRAINT SENSORVALUE_DATASET
    FOREIGN KEY (dataset_id)
    REFERENCES dataset (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

-- Remove value_column from file_column table - no longer used
ALTER TABLE file_column DROP COLUMN value_column;


-- The category_code field is now an INT that either references
-- a varialbe in the variables table, or a special value

ALTER TABLE run_type ADD COLUMN new_category_code INT NOT NULL AFTER `category_code`;
UPDATE run_type SET new_category_code = -1 WHERE category_code = 'IGN';
UPDATE run_type SET new_category_code = -2 WHERE category_code = 'ALIAS';
UPDATE run_type SET new_category_code = -3 WHERE category_code = 'EXT';
UPDATE run_type SET new_category_code =
  (SELECT id FROM variables WHERE name='Underway Marine pCO₂') WHERE category_code = 'MCO2';

ALTER TABLE run_type DROP COLUMN category_code;
ALTER TABLE run_type CHANGE COLUMN `new_category_code` `category_code` INT NOT NULL;


-- Changes for the new data reduction scheme
CREATE TABLE measurements (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  dataset_id INT NOT NULL,
  variable_id INT NOT NULL,
  date BIGINT(20) NOT NULL,
  longitude DOUBLE NOT NULL,
  latitude DOUBLE NOT NULL,
  run_type VARCHAR(45) NULL,
  PRIMARY KEY (id),
  CONSTRAINT measurement_dataset
    FOREIGN KEY (dataset_id)
    REFERENCES dataset (id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT);

CREATE INDEX measurement_dataset_idx ON measurements(dataset_id ASC);

CREATE TABLE measurement_values (
  measurement_id BIGINT(20) NOT NULL,
  variable_id INT(11) NOT NULL,
  sensor_value_id BIGINT(20) NOT NULL,
  PRIMARY KEY (measurement_id, variable_id, sensor_value_id),
  CONSTRAINT MEASVAL_MEASUREMENT
    FOREIGN KEY (measurement_id)
    REFERENCES measurements (id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT MEASVAL_VARIABLE
    FOREIGN KEY (variable_id)
    REFERENCES variables (id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT MEASVAL_SENSORVALUE
    FOREIGN KEY (sensor_value_id)
    REFERENCES sensor_values (id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT);

CREATE INDEX measval_measurements_idx ON measurement_values(measurement_id ASC);

CREATE TABLE data_reduction (
  measurement_id BIGINT(20) NOT NULL,
  variable_id INT(11) NOT NULL,
  calculation_values MEDIUMTEXT NOT NULL,
  qc_flag SMALLINT(2) NOT NULL,
  qc_message TEXT NULL,
  PRIMARY KEY (measurement_id, variable_id),
  CONSTRAINT DATAREDUCTION_MEASUREMENT
    FOREIGN KEY (measurement_id)
    REFERENCES measurements (id)
    ON DELETE RESTRICT
    ON UPDATE NO ACTION,
  CONSTRAINT DATAREDUCTION_VARIABLE
    FOREIGN KEY (variable_id)
    REFERENCES variables (id)
    ON DELETE RESTRICT
    ON UPDATE NO ACTION);

CREATE INDEX datareduction_variable_idx ON data_reduction(variable_id ASC);