-- A complete set of datasets and calibrations for edit testing

-- Datasets

-- 2019-06-01T00:20:00Z to 2019-06-01T00:40:00Z
INSERT INTO dataset (id, instrument_id, name, start, end, status, status_date)
  VALUES (1001, 1000000, 'a', 1559348400000, 1559349600000, 4, 0);

-- 2019-06-01T01:00:00Z to 2019-06-01T02:40:00Z
INSERT INTO dataset (id, instrument_id, name, start, end, status, status_date)
  VALUES (1002, 1000000, 'b', 1559352000000, 1559356800000, 4, 0);

-- 2019-06-01T03:20:00Z to 2019-06-01T04:40:00Z
INSERT INTO dataset (id, instrument_id, name, start, end, status, status_date)
  VALUES (1003, 1000000, 'c', 1559359200000, 1559364000000, 4, 0);

-- 2019-06-01T05:20:00Z to 2019-06-01T05:40:00Z
INSERT INTO dataset (id, instrument_id, name, start, end, status, status_date)
  VALUES (1004, 1000000, 'd', 1559366400000, 1559367600000, 4, 0);


-- Sensor calibrations (Prior calibration not required for processing)

-- 2019-06-01T00:10:00Z
INSERT INTO calibration (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (1001, 1000000, 'SENSOR_CALIBRATION', '1001', 1559347800000, '0.0;0.0;0.0;0.0;1.0;0.0', 'PolynomialSensorCalibration');

-- 2019-06-01T01:10:00Z
INSERT INTO calibration (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (1002, 1000000, 'SENSOR_CALIBRATION', '1001', 1559351400000, '0.0;0.0;0.0;0.0;1.0;0.0', 'PolynomialSensorCalibration');

-- 2019-06-01T03:10:00Z
INSERT INTO calibration (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (1003, 1000000, 'SENSOR_CALIBRATION', '1001', 1559358600000, '0.0;0.0;0.0;0.0;1.0;0.0', 'PolynomialSensorCalibration');

-- 2019-06-01T05:10:00Z
INSERT INTO calibration (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (1004, 1000000, 'SENSOR_CALIBRATION', '1001', 1559365800000, '0.0;0.0;0.0;0.0;1.0;0.0', 'PolynomialSensorCalibration');

-- 2019-06-01T06:10:00Z
INSERT INTO calibration (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (1005, 1000000, 'SENSOR_CALIBRATION', '1001', 1559369400000, '0.0;0.0;0.0;0.0;1.0;0.0', 'PolynomialSensorCalibration');

-- Gas standards (Prior calibration required for processing)
-- 2019-06-01T00:10:00Z
INSERT INTO calibration (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (2001, 1000000, 'EXTERNAL_STANDARD', 'TARGET1', 1559347800000, '200.0;0.0', 'ExternalStandard');

-- 2019-06-01T01:10:00Z
INSERT INTO calibration (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (2002, 1000000, 'EXTERNAL_STANDARD', 'TARGET1', 1559351400000, '200.0;0.0', 'ExternalStandard');

-- 2019-06-01T03:10:00Z
INSERT INTO calibration (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (2003, 1000000, 'EXTERNAL_STANDARD', 'TARGET1', 1559358600000, '200.0;0.0', 'ExternalStandard');

-- 2019-06-01T05:10:00Z
INSERT INTO calibration (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (2004, 1000000, 'EXTERNAL_STANDARD', 'TARGET1', 1559365800000, '200.0;0.0', 'ExternalStandard');

-- 2019-06-01T06:10:00Z
INSERT INTO calibration (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (2005, 1000000, 'EXTERNAL_STANDARD', 'TARGET1', 1559369400000, '200.0;0.0', 'ExternalStandard');
