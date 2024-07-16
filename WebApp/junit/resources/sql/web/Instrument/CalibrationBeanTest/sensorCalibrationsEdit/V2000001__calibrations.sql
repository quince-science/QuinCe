-- Sensor Calibrations

-- 2023-02-01
INSERT INTO calibration
  (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES
  (1, 1, 'SENSOR_CALIBRATION', '1', 1675209600000, '{"x⁵":"0","x⁴":"0","x³":"0","x²":"0","x":"1.01","Intercept":"0"}', 'PolynomialSensorCalibration');

INSERT INTO calibration
  (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES
  (2, 1, 'SENSOR_CALIBRATION', '3', 1675209600000, '{"x⁵":"0","x⁴":"0","x³":"0","x²":"0","x":"1.02","Intercept":"0"}', 'PolynomialSensorCalibration');

-- 2023-05-01
INSERT INTO calibration
  (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES
  (3, 1, 'SENSOR_CALIBRATION', '3', 1682899200000, '{"x⁵":"0","x⁴":"0","x³":"0","x²":"0","x":"1.03","Intercept":"0"}', 'PolynomialSensorCalibration');

-- 2023-07-01
INSERT INTO calibration
  (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES
  (4, 1, 'SENSOR_CALIBRATION', '1', 1688169600000, '{"x⁵":"0","x⁴":"0","x³":"0","x²":"0","x":"1.04","Intercept":"0"}', 'PolynomialSensorCalibration');

INSERT INTO calibration
  (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES
  (5, 1, 'SENSOR_CALIBRATION', '3', 1688169600000, '{"x⁵":"0","x⁴":"0","x³":"0","x²":"0","x":"1.05","Intercept":"0"}', 'PolynomialSensorCalibration');
