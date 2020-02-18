-- Basic instrument setup for all Calibration Bean tests

-- User
INSERT INTO user (id, email, salt, password)
  VALUES (1000000, 'test@test.com', 'FF', 'FF');

-- Instrument
INSERT INTO instrument (id, owner, name)
  VALUES (1000000, 1000000, 'Test Instrument');

-- File definition
INSERT INTO file_definition (id, instrument_id, description, column_separator,
    header_type, column_header_rows, column_count,
    lon_format, lon_value_col, lat_format, lat_value_col, date_time_col, date_time_props)
  VALUES (1000000, 1000000, 'File', ',', 0, 0, 3,
    0, 2, 0, 3, 1, '#Fri Nov 15 10:18:57 CET 2019
formatString=yyyy-MM-dd HH\:mm\:ss');

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
