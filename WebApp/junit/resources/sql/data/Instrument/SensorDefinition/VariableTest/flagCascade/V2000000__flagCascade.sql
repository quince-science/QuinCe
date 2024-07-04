-- Test variable
INSERT INTO variables (id, name) VALUES (1000000, 'testVar');

-- Variable sensor types
INSERT INTO sensor_types (id, name, vargroup, display_order) VALUES (1000000, 'coreSensor', 'testGroup', 1000);
INSERT INTO sensor_types (id, name, vargroup, display_order) VALUES (1000001, 'requiredSensor1', 'testGroup', 1001);
INSERT INTO sensor_types (id, name, vargroup, display_order) VALUES (1000002, 'requiredSensor2', 'testGroup', 1002);
INSERT INTO sensor_types (id, name, vargroup, display_order) VALUES (1000003, 'requiredSensor3', 'testGroup', 1003);
INSERT INTO sensor_types (id, name, vargroup, display_order) VALUES (1000004, 'requiredSensor4', 'testGroup', 1004);

INSERT INTO sensor_types (id, name, vargroup, display_order) VALUES (1000006, 'parentSensor', 'testGroup', 1006);
INSERT INTO sensor_types (id, name, vargroup, parent, display_order) VALUES (1000007, 'childSensor1', 'testGroup', 1000006, 1007);
INSERT INTO sensor_types (id, name, vargroup, parent, display_order) VALUES (1000008, 'childSensor2', 'testGroup', 1000006, 1008);

-- An unused sensor type
INSERT INTO sensor_types (id, name, vargroup, display_order) VALUES (1000010, 'Optional', 'testGroup', 1010);


-- Core sensor type
INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'testVar'),
    (SELECT id FROM sensor_types WHERE name = 'coreSensor'),
    1, 3, 4
  );

-- Required senor types with cascade combo
INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'testVar'),
    (SELECT id FROM sensor_types WHERE name = 'requiredSensor1'),
    0, 3, 3
  );

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'testVar'),
    (SELECT id FROM sensor_types WHERE name = 'requiredSensor2'),
    0, 3, 4
  );

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'testVar'),
    (SELECT id FROM sensor_types WHERE name = 'requiredSensor3'),
    0, 4, 3
  );

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'testVar'),
    (SELECT id FROM sensor_types WHERE name = 'requiredSensor4'),
    0, 4, 4
  );

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'testVar'),
    (SELECT id FROM sensor_types WHERE name = 'parentSensor'),
    0, 3, 4
  );

  
-- Now an instrument using the variable
INSERT INTO instrument VALUES (1,1,'Test Instrument','Test Instrument','TEST',0,NULL,'','2019-01-28 13:31:21','2019-01-28 14:31:21');

INSERT INTO instrument_variables (instrument_id, variable_id)
  VALUES (1, (SELECT id FROM variables WHERE name = 'testVar'));

-- File definition
INSERT INTO file_definition VALUES
  (1,1,'Data File',' ',0,0,NULL,2,65,
   '{"valueColumn":7,"hemisphereColumn":8,"format":2}',
   '{"valueColumn":5,"hemisphereColumn":6,"format":1}',
   '{"assignments":{"0":{"assignmentIndex":0,"column":-1,"properties":{}},"1":{"assignmentIndex":1,"column":-1,"properties":{}},"2":{"assignmentIndex":2,"column":3,"properties":{"formatString":"dd/MM/yy"}},"3":{"assignmentIndex":3,"column":-1,"properties":{}},"4":{"assignmentIndex":4,"column":-1,"properties":{}},"5":{"assignmentIndex":5,"column":-1,"properties":{}},"6":{"assignmentIndex":6,"column":-1,"properties":{}},"7":{"assignmentIndex":7,"column":-1,"properties":{}},"8":{"assignmentIndex":8,"column":4,"properties":{"formatString":"HH:mm:ss"}},"9":{"assignmentIndex":9,"column":-1,"properties":{}},"10":{"assignmentIndex":10,"column":-1,"properties":{}},"11":{"assignmentIndex":11,"column":-1,"properties":{}},"12":{"assignmentIndex":12,"column":-1,"properties":{}}},"fileHasHeader":false}',
   '2019-01-28 13:31:21','2019-01-28 14:31:21');

INSERT INTO file_column VALUES (1,1,1,1,1000000,'Core',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (2,1,2,1,1000001,'Required 1',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (3,1,3,1,1000002,'Required 2',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (4,1,4,1,1000003,'Required 3',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (5,1,5,1,1000004,'Required 4',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (6,1,6,1,1000007,'Child',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (7,1,7,1,1000010,'Optional',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
