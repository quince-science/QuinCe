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
-- None

-- Measured value
INSERT INTO sensor_values(id, dataset_id, file_column, date, value, user_qc_flag)
  VALUES (7, 1, 5, 1609502400000, 350, 2);
INSERT INTO sensor_values(id, dataset_id, file_column, date, value, user_qc_flag)
  VALUES (8, 1, 6, 1609502400000, 'equ', 2);
  
-- Subsequent internal calibration value
--

-- Measurements
INSERT INTO measurements (id, dataset_id, date, measurement_values)
  VALUES (4, 1, 1609502400000, '{"9":{"svids":[7],"suppids":[],"memberCount":1,"interpolatesOverFlag":false,"value":402.41366693446776,"flag":-2,"qcComments":[],"type":"M","props":{}}}');

-- Measurement run types
INSERT INTO measurement_run_types (measurement_id, variable_id, run_type)
  VALUES (4, 1, 'equ');

-- Data Reduction record
INSERT INTO data_reduction (measurement_id, variable_id, calculation_values, qc_flag)
  VALUES (4, 1, '{}', 2);
