INSERT INTO sensor_types (name, parent) VALUES ('Test Parent', null);
INSERT INTO sensor_types (name, parent)
  VALUES ('Test Child', (SELECT id FROM sensor_types WHERE name = 'Test Parent'));