-- Pre-calibrations for Contros pCO2 reducer test

-- 1624838400000 = 2021-06-21 00:00:00

INSERT INTO calibration (instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (116, 'CALC_COEFFICIENT', '6.F', 1624838400000, '{"Value":"61279"}', 'CalculationCoefficient');

INSERT INTO calibration (instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (116, 'CALC_COEFFICIENT', '6.Runtime', 1624838400000, '{"Value":"97335"}', 'CalculationCoefficient');

INSERT INTO calibration (instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (116, 'CALC_COEFFICIENT', '6.k1', 1624838400000, '{"Value":"6.145946e-02"}', 'CalculationCoefficient');

INSERT INTO calibration (instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (116, 'CALC_COEFFICIENT', '6.k2', 1624838400000, '{"Value":"3.103593e-06"}', 'CalculationCoefficient');

INSERT INTO calibration (instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (116, 'CALC_COEFFICIENT', '6.k3', 1624838400000, '{"Value":"3.546593e-10"}', 'CalculationCoefficient');
