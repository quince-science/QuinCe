-- Dataset: 2019-06-03T00:00:00 to 2019-06-05T00:00:00
INSERT INTO dataset (id, instrument_id, name, start, end, status, status_date)
  VALUES (1001, 1000000, 'A', 1559520000000, 1559692800000, 4, 0);

-- Calibration: 2019-06-02T00:00:00
INSERT INTO calibration (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (1001, 1000000, 'SENSOR_CALIBRATION', '1001', 1559433600000, '{"x⁵":"0.0","x⁴":"0.0","x³":"0.0","x²":"0.0","x":"1.0","Intercept":"0.0"}', 'PolynomialSensorCalibration');
