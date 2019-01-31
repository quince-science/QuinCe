-- Create the sensor_types table, which used to be in a config file
CREATE TABLE sensor_types (
	id INT NOT NULL AUTO_INCREMENT,
	name VARCHAR(100) NOT NULL,
	required_group VARCHAR(100) NULL,
	depends_on INT NULL,
	depends_question TEXT NULL,
	internal_calibration TINYINT NOT NULL DEFAULT 0,
	PRIMARY KEY (id),
	UNIQUE SENSORTYPENAME (name))
  ENGINE = InnoDB;

-- Intake temp
INSERT INTO sensor_types (name, required_group, depends_on, depends_question, internal_calibration)
	VALUES ('Intake Temperature', NULL, NULL, NULL, 0);

-- Salinity
INSERT INTO sensor_types (name, required_group, depends_on, depends_question, internal_calibration)
	VALUES ('Salinity', NULL, NULL, NULL, 0);

-- Equilibrator Temperature
INSERT INTO sensor_types (name, required_group, depends_on, depends_question, internal_calibration)
	VALUES ('Equilibrator Temperature', NULL, NULL, NULL, 0);

-- Equilibrator Pressure (absolute)
INSERT INTO sensor_types (name, required_group, depends_on, depends_question, internal_calibration)
	VALUES ('Equilibrator Pressure (absolute)', 'Equilibrator Pressure', NULL, NULL, 0);

-- Equilibrator Pressure (differential)
INSERT INTO sensor_types (name, required_group, depends_on, depends_question, internal_calibration)
	VALUES ('Equilibrator Pressure (differential)', 'Equilibrator Pressure', NULL, NULL, 0);

-- Ambient Pressure
INSERT INTO sensor_types (name, required_group, depends_on, depends_question, internal_calibration)
	VALUES ('Ambient Pressure', NULL, NULL, NULL, 0);

-- xH2O in gas
INSERT INTO sensor_types (name, required_group, depends_on, depends_question, internal_calibration)
	VALUES ('xH₂O in gas', NULL, NULL, NULL, 1);

-- CO2 (Licor etc)
INSERT INTO sensor_types (name, required_group, depends_on, depends_question, internal_calibration)
	VALUES ('CO₂ in gas', NULL, NULL, NULL, 1);

-- Atmospheric Pressure
INSERT INTO sensor_types (name, required_group, depends_on, depends_question, internal_calibration)
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
