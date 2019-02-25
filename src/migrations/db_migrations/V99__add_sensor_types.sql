DROP TABLE IF EXISTS instrument_variables;
DROP TABLE IF EXISTS variable_sensors;
DROP TABLE IF EXISTS variables;
DROP TABLE IF EXISTS sensor_types;


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
SELECT id INTO @sstId FROM sensor_types WHERE name = 'Intake Temperature';

-- Salinity
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Salinity', 'Salinity', NULL, NULL, NULL, 0);
SELECT id INTO @salinityId FROM sensor_types WHERE name = 'Salinity';

-- Equilibrator Temperature
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Equilibrator Temperature', 'Temperature', NULL, NULL, NULL, 0);
SELECT id INTO @eqtId FROM sensor_types WHERE name = 'Equilibrator Temperature';

-- Equilibrator Pressure - parent of the two pressure types
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Equilibrator Pressure', 'Pressure', NULL, NULL, NULL, 0);
SELECT id INTO @eqpId FROM sensor_types WHERE name = 'Equilibrator Pressure';

-- Equilibrator Pressure (absolute)
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Equilibrator Pressure (absolute)', 'Pressure', (SELECT @eqpId), NULL, NULL, 0);
SELECT id INTO @eqpAbsId FROM sensor_types WHERE name = 'Equilibrator Pressure (absolute)';

-- Equilibrator Pressure (differential)
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Equilibrator Pressure (differential)', 'Pressure', (SELECT @eqpId), NULL, NULL, 0);
SELECT id INTO @eqpDiffId FROM sensor_types WHERE name = 'Equilibrator Pressure (differential)';

-- Ambient Pressure
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Ambient Pressure', 'Pressure', NULL, NULL, NULL, 0);
SELECT id INTO @ambientId FROM sensor_types WHERE name = 'Ambient Pressure';

-- xH2O in gas
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('xH₂O in gas', 'Moisture', NULL, NULL, NULL, 1);
SELECT id INTO @xh2oId FROM sensor_types WHERE name = 'xH₂O in gas';

-- CO2 (Licor etc)
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('CO₂ in gas', 'CO₂', NULL, NULL, NULL, 1);
SELECT id INTO @co2Id FROM sensor_types WHERE name = 'CO₂ in gas';

-- Atmospheric Pressure
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Atmospheric Pressure', 'Pressure', NULL, NULL, NULL, 0);

-- Equilibrator Pressure (differential) depends on Ambient Pressure
UPDATE sensor_types
  SET depends_on = (SELECT @ambientId)
  WHERE name = 'Equilibrator Pressure (differential)';

-- CO₂ sometimes needs to be mathematically dried, which needs xH₂O
UPDATE sensor_types
  SET depends_on = (SELECT @xh2oId)
  WHERE name = 'CO₂ in gas';

UPDATE sensor_types
  SET depends_question = 'Do values from CO₂ require moisture adjustment?'
  WHERE name = 'CO₂ in gas';

-- Diagnostic sensors
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Diagnostic Temperature', 'Temperature', NULL, NULL, NULL, 0, 1);
SELECT id INTO @diagTempId FROM sensor_types WHERE name = 'Diagnostic Temperature';

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Diagnostic Pressure', 'Pressure', NULL, NULL, NULL, 0, 1);
SELECT id INTO @diagPresId FROM sensor_types WHERE name = 'Diagnostic Pressure';

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Diagnostic Gas Flow', 'Other', NULL, NULL, NULL, 0, 1);
SELECT id INTO @diagAirId FROM sensor_types WHERE name = 'Diagnostic Air Flow';

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Diagnostic Water Flow', 'Other', NULL, NULL, NULL, 0, 1);
SELECT id INTO @diagWaterId FROM sensor_types WHERE name = 'Diagnostic Water Flow';

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Diagnostic Voltage', 'Other', NULL, NULL, NULL, 0, 1);
SELECT id INTO @diagVoltId FROM sensor_types WHERE name = 'Diagnostic Voltage';

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Diagnostic Misc', 'Other', NULL, NULL, NULL, 0, 1);
SELECT id INTO @diagMiscId FROM sensor_types WHERE name = 'Diagnostic Misc';

-- ----------------------------
-- Variable definition
CREATE TABLE variables (
  id INT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE VARNAME_UNIQUE (name)
  ) ENGINE = InnoDB;

INSERT INTO variables (name) VALUES ('Underway Marine pCO₂');
SELECT id INTO @varId FROM variables;

CREATE TABLE variable_sensors (
  variable_id INT NOT NULL,
  sensor_type INT NOT NULL,
  core TINYINT(1) NOT NULL DEFAULT 0,
  questionable_cascade TINYINT(1) NOT NULL,
  bad_cascade TINYINT(1) NOT NULL,
  PRIMARY KEY (variable_id, sensor_type),
  INDEX VARSENSOR_SENSOR_idx (sensor_type ASC),
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
  VALUES ((SELECT @varId), (SELECT @sstId), 0, 3, 4);

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES ((SELECT @varId), (SELECT @salinityId), 0, 2, 3);

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES ((SELECT @varId), (SELECT @eqtId), 0, 3, 4);

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES ((SELECT @varId), (SELECT @eqpId), 0, 3, 4);

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES ((SELECT @varId), (SELECT @co2Id), 1, 3, 4);

-- --------------------------------
-- Link instruments to variables

CREATE TABLE instrument_variables (
  instrument_id INT NOT NULL,
  variable_id INT(11) NOT NULL,
  INDEX INSTRVAR_INSTRUMENT_idx (instrument_id ASC),
  INDEX INSTRVAR_VARIABLE_idx (variable_id ASC),
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
UPDATE file_column SET sensor_type = (SELECT @sstId) WHERE sensor_type_old = 'Intake Temperature';
UPDATE file_column SET sensor_type = (SELECT @salinityId) WHERE sensor_type_old = 'Salinity';
UPDATE file_column SET sensor_type = (SELECT @eqtId) WHERE sensor_type_old = 'Equilibrator Temperature';
UPDATE file_column SET sensor_type = (SELECT @eqpAbsId) WHERE sensor_type_old = 'Equilibrator Pressure (absolute)';
UPDATE file_column SET sensor_type = (SELECT @eqpDiffId) WHERE sensor_type_old = 'Equilibrator Pressure (differential)';
UPDATE file_column SET sensor_type = (SELECT @ambientId) WHERE sensor_type_old = 'Ambient Pressure';
UPDATE file_column SET sensor_type = (SELECT @xh2oId) WHERE sensor_type_old = 'xH2O';
UPDATE file_column SET sensor_type = (SELECT @co2Id) WHERE sensor_type_old = 'CO2';
UPDATE file_column SET sensor_type = (SELECT @diagTempId) WHERE sensor_type_old = 'Diagnostic: Temperature';
UPDATE file_column SET sensor_type = (SELECT @diagPresId) WHERE sensor_type_old = 'Diagnostic: Pressure';
UPDATE file_column SET sensor_type = (SELECT @diagAirId) WHERE sensor_type_old = 'Diagnostic: Air Flow';
UPDATE file_column SET sensor_type = (SELECT @diagWaterId) WHERE sensor_type_old = 'Diagnostic: Water Flow';
UPDATE file_column SET sensor_type = (SELECT @diagVoltId) WHERE sensor_type_old = 'Diagnostic: Voltage';

-- Run Type sensor has a special fixed ID
UPDATE file_column SET sensor_type = -1 WHERE sensor_type_old = 'Run Type';

-- Remove the old column
ALTER TABLE file_column DROP COLUMN sensor_type_old;

-- Remove the post_calibrated field - no longer used
ALTER TABLE file_column DROP COLUMN post_calibrated;
