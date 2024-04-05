-- Test variable
INSERT INTO variables (id, name) VALUES (1000000, 'testVar');

-- Test sensor type
INSERT INTO sensor_types (id, name, vargroup, display_order) VALUES (1000000, 'testSensor', 'testGroup', 1000);

-- An unused sensor type
INSERT INTO sensor_types (id, name, vargroup, display_order) VALUES (1000001, 'Unused sensor', 'Unusded group', 1001);

-- Variables for Test - SST, Salinity and the test sensor
INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'testVar'),
    (SELECT id FROM sensor_types WHERE name = 'Water Temperature'),
    0, 3, 4
  );

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'testVar'),
    (SELECT id FROM sensor_types WHERE name = 'Salinity'),
    0, 4, 3
  );

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'testVar'),
    (SELECT id FROM sensor_types WHERE name = 'testSensor'),
    1, 3, 4
  );
