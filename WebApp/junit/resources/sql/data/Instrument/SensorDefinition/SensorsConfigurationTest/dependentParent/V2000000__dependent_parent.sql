-- A parent that depends on another sensor (invalid)
INSERT INTO sensor_types (name, depends_on) VALUES ('Dependent Parent',
  (SELECT id FROM sensor_types WHERE name = 'Salinity'));

-- The children
INSERT INTO sensor_types (name, parent) VALUES ('Child 1',
  (SELECT id FROM sensor_types WHERE name = 'Dependent Parent'));

INSERT INTO sensor_types (name, parent) VALUES ('Child 2',
  (SELECT id FROM sensor_types WHERE name = 'Dependent Parent'));
