-- Create separate measurement run types table
CREATE TABLE measurement_run_types (
  measurement_id BIGINT(20) NOT NULL,
  variable_id BIGINT(20) NOT NULL,
  run_type VARCHAR(45) NOT NULL,
  PRIMARY KEY (measurement_id, variable_id),
  CONSTRAINT `mrt_m`
    FOREIGN KEY (measurement_id)
    REFERENCES measurements (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

INSERT INTO measurement_run_types (measurement_id, variable_id, run_type) (
  SELECT id, -1, run_type FROM measurements WHERE run_type IS NOT NULL
);

ALTER TABLE measurements DROP COLUMN run_type;

-- Field for arbitrary variable properties
ALTER TABLE variables ADD COLUMN properties MEDIUMTEXT NULL AFTER attributes;

-- CONTROS pCO₂ sensor types
INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units)
  VALUES ('Contros pCO₂ Raw Detector Signal', 'Sensor Internal', 1250, 'CONTROSRAW', 'CONTROS Raw Detector Signal', NULL);

INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units)
  VALUES ('Contros pCO₂ Reference Signal', 'Sensor Internal', 1251, 'CONTROSREF', 'CONTROS Reference Signal', NULL);

INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units)
  VALUES ('Contros pCO₂ Zero Mode', 'Sensor Internal', 1252, 'CONTROSZERO', 'CONTROS Zero Mode', NULL);

INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units)
  VALUES ('Contros pCO₂ Flush Mode', 'Sensor Internal', 1253, 'CONTROSFLUSH', 'CONTROS Flush Mode', NULL);

INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units)
  VALUES ('Contros pCO₂ Gas Stream Temperature', 'Temperature', 350, 'CONTROSGASTEMP', 'Temperature of gas stream', '°C');

INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units)
  VALUES ('Contros pCO₂ Gas Stream Pressure', 'Pressure', 450, 'CONTROSGASPRES', 'Pressure of gas stream', 'hPa');

INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units)
  VALUES ('Contros pCO₂ Membrane Pressure', 'Pressure', 451, 'CONTROSMEMBRANEPRES', 'Pressure at membrane', 'hPa');

INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units, diagnostic)
  VALUES ('Diagnostic Relative Humidity', 'Other', 1350, 'DIAGHUMID', 'Relative Humidity (Diagnostic)', '%', 1);

-- CONTROS pCO₂ Variable
INSERT INTO variables (name, attributes, properties)
  VALUES ('CONTROS pCO₂', NULL, '{"coefficients": ["F", "Tsensor", "f(Tsensor)", "k1", "k2", "k3"]}');

-- CONTROS pCO₂ Variable Sensors
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'CONTROS pCO₂'),
    (SELECT id FROM sensor_types WHERE column_code = 'CONTROSRAW'),
    1, 3, 4
  );

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'CONTROS pCO₂'),
    (SELECT id FROM sensor_types WHERE column_code = 'CONTROSREF'),
    1, 3, 4
  );

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'CONTROS pCO₂'),
    (SELECT id FROM sensor_types WHERE column_code = 'CONTROSZERO'),
    0, 3, 4
  );

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'CONTROS pCO₂'),
    (SELECT id FROM sensor_types WHERE column_code = 'CONTROSFLUSH'),
    0, 3, 4
  );

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'CONTROS pCO₂'),
    (SELECT id FROM sensor_types WHERE column_code = 'CONTROSGASPRES'),
    0, 3, 4
  );

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'CONTROS pCO₂'),
    (SELECT id FROM sensor_types WHERE column_code = 'CONTROSMEMBRANEPRES'),
    0, 3, 4
  );

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'CONTROS pCO₂'),
    (SELECT id FROM sensor_types WHERE name = 'Intake Temperature'),
    0, 3, 4
  );

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'CONTROS pCO₂'),
    (SELECT id FROM sensor_types WHERE name = 'Salinity'),
    0, 2, 3
  );
