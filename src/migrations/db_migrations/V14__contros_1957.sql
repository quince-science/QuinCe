-- Field for arbitrary variable properties
ALTER TABLE variables ADD COLUMN properties MEDIUMTEXT NULL AFTER attributes;

-- CONTROS pCO2 sensor types
INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units)
  VALUES ('Raw Detector Signal', 'Sensor Internal', 1250, 'CONTROSRAW', 'CONTROS Raw Detector Signal', NULL);

INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units)
  VALUES ('Reference Signal', 'Sensor Internal', 1251, 'CONTROSREF', 'CONTROS Reference Signal', NULL);

INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units)
  VALUES ('Zero Mode', 'Sensor Internal', 1252, 'CONTROSZERO', 'CONTROS Zero Mode', NULL);

INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units)
  VALUES ('Flush Mode', 'Sensor Internal', 1253, 'CONTROSFLUSH', 'CONTROS Flush Mode', NULL);

INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units)
  VALUES ('Gas Stream Temperature', 'Temperature', 350, 'CONTROSGASTEMP', 'Temperature of gas stream', '°C');

INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units)
  VALUES ('Gas Stream Pressure', 'Pressure', 450, 'CONTROSGASPRES', 'Pressure of gas stream', 'hPa');

INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units)
  VALUES ('Membrane Pressure', 'Pressure', 451, 'CONTROSMEMBRANEPRES', 'Pressure at membrane', 'hPa');

INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units, diagnostic)
  VALUES ('Diagnostic Relative Humidity', 'Other', 1350, 'DIAGHUMID', 'Relative Humidity (Diagnostic)', '%', 1);

-- CONTROS pCO2 Variable
INSERT INTO variables (name, attributes, properties)
  VALUES ('CONTROS pCO₂', NULL, '{"coefficients": ["F", "Tsensor", "f(Tsensor)", "k1", "k2", "k3"]}');

-- CONTROS pCO2 Variable Sensors
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
    0, 3, 4
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
