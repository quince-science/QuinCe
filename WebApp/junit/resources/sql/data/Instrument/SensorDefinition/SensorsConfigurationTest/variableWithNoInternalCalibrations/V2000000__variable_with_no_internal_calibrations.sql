-- A test variable
INSERT INTO variables (name) VALUES ('No Internal Calibration');

-- A valid core sensor
INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'No Internal Calibration'),
    (SELECT id FROM sensor_types WHERE name = 'Water Temperature'),
    1, 3, 4
  );
