-- A parent that depends on another sensor (invalid)
INSERT INTO sensor_types (name, vargroup, depends_on) VALUES ('Dependent Parent',
  'Group', (SELECT id FROM sensor_types WHERE name = 'Salinity'));

-- The children
INSERT INTO sensor_types (name, vargroup, parent) VALUES ('Child 1',
  'Group', (SELECT id FROM sensor_types WHERE name = 'Dependent Parent'));

INSERT INTO sensor_types (name, vargroup, parent) VALUES ('Child 2',
  'Group', (SELECT id FROM sensor_types WHERE name = 'Dependent Parent'));
