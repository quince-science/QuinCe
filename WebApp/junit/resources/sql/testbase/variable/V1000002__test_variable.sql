-- Test variable
INSERT INTO variables (name) VALUES ('testVar');

-- Test sensor type
INSERT INTO sensor_types (name, vargroup, display_order) VALUES ('testSensor', 'testGroup', 1000);

-- An unused sensor type
INSERT INTO sensor_types (name, vargroup, display_order) VALUES ('Unused sensor', 'Unusded group', 1001);

-- Variables for Test - SST, Salinity and the test sensor
INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'testVar'),
    (SELECT id FROM sensor_types WHERE name = 'Intake Temperature'),
    0, 3, 4
  );

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'testVar'),
    (SELECT id FROM sensor_types WHERE name = 'Salinity'),
    0, 3, 4
  );

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'testVar'),
    (SELECT id FROM sensor_types WHERE name = 'testSensor'),
    1, 3, 4
  );
