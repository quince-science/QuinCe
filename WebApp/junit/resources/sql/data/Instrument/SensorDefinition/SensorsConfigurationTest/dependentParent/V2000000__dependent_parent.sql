-- A parent that depends on another sensor (invalid)
INSERT INTO sensor_types (name, vargroup, depends_on, display_order)
  VALUES ('Dependent Parent', 'Group', (SELECT id FROM sensor_types WHERE name = 'Salinity'), 1000);

-- The children
INSERT INTO sensor_types (name, vargroup, parent, display_order)
  VALUES ('Child 1', 'Group', (SELECT id FROM sensor_types WHERE name = 'Dependent Parent'), 1001);

INSERT INTO sensor_types (name, vargroup, parent, display_order)
  VALUES ('Child 2', 'Group', (SELECT id FROM sensor_types WHERE name = 'Dependent Parent'), 1002);
