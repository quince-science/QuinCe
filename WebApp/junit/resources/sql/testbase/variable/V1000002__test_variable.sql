-- Test variable
INSERT INTO variables (id, name, allowed_basis) VALUES (1000000, 'testVar', 1);

-- Test sensor type
INSERT INTO sensor_types (id, name, vargroup, display_order) VALUES (1000000, 'testSensor', 'testGroup', 1000);

-- An unused sensor type
INSERT INTO sensor_types (id, name, vargroup, display_order) VALUES (1000001, 'Unused sensor', 'Unusded group', 1001);

-- Variables for Test - SST, Salinity and the test sensor
INSERT INTO variable_sensors
  (variable_id, sensor_type, core, cascades)
  VALUES (
    (SELECT id FROM variables WHERE name = 'testVar'),
    (SELECT id FROM sensor_types WHERE name = 'Water Temperature'),
    0, '{"Time":[[3,3],[4,4]]}'
  );

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, cascades)
  VALUES (
    (SELECT id FROM variables WHERE name = 'testVar'),
    (SELECT id FROM sensor_types WHERE name = 'Salinity'),
    0, '{"Time":[[3,4],[4,3]]}'
  );

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, cascades)
  VALUES (
    (SELECT id FROM variables WHERE name = 'testVar'),
    (SELECT id FROM sensor_types WHERE name = 'testSensor'),
    1, '{"Time":[[3,3],[4,4]]}'
  );
