-- Create the sensor_types table, which used to be in a config file
CREATE TABLE sensor_types (
  id INT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  parent INT NULL,
  depends_on INT NULL,
  depends_question TEXT NULL,
  internal_calibration TINYINT NOT NULL DEFAULT 0,
  diagnostic TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE SENSORTYPENAME (name))
  ENGINE = InnoDB;

-- Intake temp
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Intake Temperature', NULL, NULL, NULL, 0);

-- Salinity
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Salinity', NULL, NULL, NULL, 0);

-- Equilibrator Temperature
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Equilibrator Temperature', NULL, NULL, NULL, 0);

-- Equilibrator Pressure - parent of the two pressure types
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Equilibrator Pressure', NULL, NULL, NULL, 0);

-- Equilibrator Pressure (absolute)
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Equilibrator Pressure (absolute)',
    (SELECT id FROM sensor_types WHERE name = 'Equilibrator Pressure'),
    NULL, NULL, 0);

-- Equilibrator Pressure (differential)
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Equilibrator Pressure (differential)',
    (SELECT id FROM sensor_types WHERE name = 'Equilibrator Pressure'),
    NULL, NULL, 0);

-- Ambient Pressure
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Ambient Pressure', NULL, NULL, NULL, 0);

-- xH2O in gas
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration)
  VALUES ('xH₂O in gas', NULL, NULL, NULL, 1);

-- CO2 (Licor etc)
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration)
  VALUES ('CO₂ in gas', NULL, NULL, NULL, 1);

-- Atmospheric Pressure
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Atmospheric Pressure', NULL, NULL, NULL, 0);

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
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Diagnostic Temperature', NULL, NULL, NULL, 0, 1);
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Diagnostic Pressure', NULL, NULL, NULL, 0, 1);
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Diagnostic Air Flow', NULL, NULL, NULL, 0, 1);
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Diagnostic Water Flow', NULL, NULL, NULL, 0, 1);
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Diagnostic Voltage', NULL, NULL, NULL, 0, 1);
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Diagnostic Misc', NULL, NULL, NULL, 0, 1);

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
    0, 3, 4);

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
