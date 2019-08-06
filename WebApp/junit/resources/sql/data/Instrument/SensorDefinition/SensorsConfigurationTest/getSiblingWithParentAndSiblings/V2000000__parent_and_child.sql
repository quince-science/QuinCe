INSERT INTO sensor_types (name, vargroup, parent, display_order)
  VALUES ('Test Parent', 'Group', null, 1000);

INSERT INTO sensor_types (name, vargroup, parent, display_order)
  VALUES ('Test Child', 'Group', (SELECT id FROM sensor_types WHERE name = 'Test Parent'), 1001);

INSERT INTO sensor_types (name, vargroup, parent, display_order)
  VALUES ('Sibling 1', 'Group', (SELECT id FROM sensor_types WHERE name = 'Test Parent'), 1002);
  
INSERT INTO sensor_types (name, vargroup, parent, display_order)
  VALUES ('Sibling 2', 'Group', (SELECT id FROM sensor_types WHERE name = 'Test Parent'), 1003);