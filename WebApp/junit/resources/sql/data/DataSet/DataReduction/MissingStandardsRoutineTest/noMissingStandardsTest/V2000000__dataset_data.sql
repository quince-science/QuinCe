-- Run Types

-- Run Type values
INSERT INTO run_type (file_definition_id, run_name, category_code)
  VALUES (1, 'std1', -3);
INSERT INTO run_type (file_definition_id, run_name, category_code)
  VALUES (1, 'std2', -3);
INSERT INTO run_type (file_definition_id, run_name, category_code)
  VALUES (1, 'std3', -3);
INSERT INTO run_type (file_definition_id, run_name, category_code)
  VALUES (1, 'equ', 1);

-- Sensor Values

-- Preceding internal calibration value
INSERT INTO sensor_values(id, dataset_id, file_column, date, value, user_qc_flag)
  VALUES (1, 1, 5, 1609498800000, 200, 2);
INSERT INTO sensor_values(id, dataset_id, file_column, date, value, user_qc_flag)
  VALUES (2, 1, 6, 1609498800000, 'std1', 2);
INSERT INTO sensor_values(id, dataset_id, file_column, date, value, user_qc_flag)
  VALUES (3, 1, 5, 1609498801000, 400, 2);
INSERT INTO sensor_values(id, dataset_id, file_column, date, value, user_qc_flag)
  VALUES (4, 1, 6, 1609498801000, 'std2', 2);
INSERT INTO sensor_values(id, dataset_id, file_column, date, value, user_qc_flag)
  VALUES (5, 1, 5, 1609498802000, 800, 2);
INSERT INTO sensor_values(id, dataset_id, file_column, date, value, user_qc_flag)
  VALUES (6, 1, 6, 1609498802000, 'std3', 2);
  
-- Measured value
INSERT INTO sensor_values(id, dataset_id, file_column, date, value, user_qc_flag)
  VALUES (7, 1, 5, 1609502400000, 350, 2);
INSERT INTO sensor_values(id, dataset_id, file_column, date, value, user_qc_flag)
  VALUES (8, 1, 6, 1609502400000, 'equ', 2);
  
-- Subsequent internal calibration value
INSERT INTO sensor_values(id, dataset_id, file_column, date, value, user_qc_flag)
  VALUES (9, 1, 5, 1609506000000, 200, 2);
INSERT INTO sensor_values(id, dataset_id, file_column, date, value, user_qc_flag)
  VALUES (10, 1, 6, 1609506000000, 'std1', 2);
INSERT INTO sensor_values(id, dataset_id, file_column, date, value, user_qc_flag)
  VALUES (11, 1, 5, 1609506001000, 400, 2);
INSERT INTO sensor_values(id, dataset_id, file_column, date, value, user_qc_flag)
  VALUES (12, 1, 6, 1609506001000, 'std2', 2);
INSERT INTO sensor_values(id, dataset_id, file_column, date, value, user_qc_flag)
  VALUES (13, 1, 5, 1609506002000, 800, 2);
INSERT INTO sensor_values(id, dataset_id, file_column, date, value, user_qc_flag)
  VALUES (14, 1, 6, 1609506002000, 'std3', 2);

-- Measurements
INSERT INTO measurements (id, dataset_id, date, measurement_values)
  VALUES (1, 1, 1609498800000, NULL);
INSERT INTO measurements (id, dataset_id, date, measurement_values)
  VALUES (2, 1, 1609498800000, NULL);
INSERT INTO measurements (id, dataset_id, date, measurement_values)
  VALUES (3, 1, 1609498800000, NULL);
INSERT INTO measurements (id, dataset_id, date, measurement_values)
  VALUES (4, 1, 1609502400000, '{"9":{"svids":[7],"suppids":[1, 3, 5, 9, 11, 13],"memberCount":1,"interpolatesOverFlag":false,"value":402.41366693446776,"flag":-2,"qcComments":[],"type":"M","props":{}}}');
INSERT INTO measurements (id, dataset_id, date, measurement_values)
  VALUES (5, 1, 1609506000000, NULL);
INSERT INTO measurements (id, dataset_id, date, measurement_values)
  VALUES (6, 1, 1609506001000, NULL);
INSERT INTO measurements (id, dataset_id, date, measurement_values)
  VALUES (7, 1, 1609506002000, NULL);

-- Measurement run types
INSERT INTO measurement_run_types (measurement_id, variable_id, run_type)
  VALUES (1, 1, 'std1');
INSERT INTO measurement_run_types (measurement_id, variable_id, run_type)
  VALUES (2, 1, 'std2');
INSERT INTO measurement_run_types (measurement_id, variable_id, run_type)
  VALUES (3, 1, 'std3');
INSERT INTO measurement_run_types (measurement_id, variable_id, run_type)
  VALUES (4, 1, 'equ');
INSERT INTO measurement_run_types (measurement_id, variable_id, run_type)
  VALUES (5, 1, 'std1');
INSERT INTO measurement_run_types (measurement_id, variable_id, run_type)
  VALUES (6, 1, 'std2');
INSERT INTO measurement_run_types (measurement_id, variable_id, run_type)
  VALUES (7, 1, 'std3');

-- Data Reduction record
INSERT INTO data_reduction (measurement_id, variable_id, calculation_values, qc_flag)
  VALUES (4, 1, '{}', 2);
