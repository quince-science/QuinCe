-- Basic instrument setup for all Calibration Bean tests

-- User
INSERT INTO user (id, email, salt, password, firstname, surname, permissions)
  VALUES (1000000, 'test@test.com', 'FF', 'FF', 'FF', 'FF', 0);

-- Instrument
INSERT INTO instrument (id, owner, name)
  VALUES (1000000, 1000000, 'Test Instrument');

-- File definition
INSERT INTO file_definition (id, instrument_id, description, column_separator,
    header_type, column_header_rows, column_count, lon_spec, lat_spec, datetime_spec)
  VALUES (1000000, 1000000, 'File', ',', 0, 0, 3,
    '{"valueColumn":2,"hemisphereColumn":-1,"format":0}',
    '{"valueColumn":3,"hemisphereColumn":-1,"format":0}',
    '{"assignments":{"0":{"assignmentIndex":0,"column":1,"properties":{"formatString":"yyyy-MM-dd HH:mm:ss"}},"1":{"assignmentIndex":1,"column":-1,"properties":{}},"2":{"assignmentIndex":2,"column":-1,"properties":{}},"3":{"assignmentIndex":3,"column":-1,"properties":{}},"4":{"assignmentIndex":4,"column":-1,"properties":{}},"5":{"assignmentIndex":5,"column":-1,"properties":{}},"6":{"assignmentIndex":6,"column":-1,"properties":{}},"7":{"assignmentIndex":7,"column":-1,"properties":{}},"8":{"assignmentIndex":8,"column":-1,"properties":{}},"9":{"assignmentIndex":9,"column":-1,"properties":{}},"10":{"assignmentIndex":10,"column":-1,"properties":{}},"11":{"assignmentIndex":11,"column":-1,"properties":{}},"12":{"assignmentIndex":12,"column":-1,"properties":{}}},"fileHasHeader":false}'
  );

-- Some file columns with calibratable sensors
INSERT INTO file_column (id, file_definition_id, file_column, primary_sensor,
    sensor_type, sensor_name)
  VALUES (1001, 1000000, 4, 1, 1, 'SENSOR1');

INSERT INTO file_column (id, file_definition_id, file_column, primary_sensor,
    sensor_type, sensor_name)
  VALUES (1002, 1000000, 5, 1, 2, 'SENSOR2');

INSERT INTO file_column (id, file_definition_id, file_column, primary_sensor,
    sensor_type, sensor_name)
  VALUES (1003, 1000000, 6, 1, 3, 'SENSOR3');


-- External standard run types
INSERT INTO run_type (file_definition_id, run_name, category_code)
  VALUES (1000000, 'TARGET1', -3);
INSERT INTO run_type (file_definition_id, run_name, category_code)
  VALUES (1000000, 'TARGET2', -3);
INSERT INTO run_type (file_definition_id, run_name, category_code)
  VALUES (1000000, 'TARGET3', -3);
