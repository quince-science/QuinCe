-- pH variable
INSERT INTO variables (name) VALUES ('testVar');

-- pH sensor type
INSERT INTO sensor_types (name, vargroup) VALUES ('testSensor', 'testGroup');

-- An unused sensor type
INSERT INTO sensor_types (name, vargroup) VALUES ('Unused sensor', 'Unusded group');

-- Variables for pH - SST, Salinity and the pH
INSERT INTO variable_sensors VALUES (
  (SELECT id FROM variables WHERE name = 'testVar'),
  (SELECT id FROM sensor_types WHERE name = 'Intake Temperature'),
  0, 3, 4
);

INSERT INTO variable_sensors VALUES (
  (SELECT id FROM variables WHERE name = 'testVar'),
  (SELECT id FROM sensor_types WHERE name = 'Salinity'),
  0, 3, 4
);

INSERT INTO variable_sensors VALUES (
  (SELECT id FROM variables WHERE name = 'testVar'),
  (SELECT id FROM sensor_types WHERE name = 'testSensor'),
  1, 3, 4
);
