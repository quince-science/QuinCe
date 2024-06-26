-- Run Types
INSERT INTO run_type
  (file_definition_id, run_name, category_code)
  VALUES (1, 'std1', -3);

INSERT INTO run_type
  (file_definition_id, run_name, category_code)
  VALUES (1, 'std2', -3);

  
-- External Standards

-- 2024-02-01
INSERT INTO calibration
  (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES
  (1, 1, 'EXTERNAL_STANDARD', 'std1', 1706745600000, '{"xH₂O (with standards)":"0","xCO₂ (with standards)":"102"}', 'DefaultExternalStandard');

INSERT INTO calibration
  (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES
  (2, 1, 'EXTERNAL_STANDARD', 'std2', 1706745600000, '{"xH₂O (with standards)":"0","xCO₂ (with standards)":"202"}', 'DefaultExternalStandard');

-- 2024-05-01
INSERT INTO calibration
  (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES
  (3, 1, 'EXTERNAL_STANDARD', 'std2', 1714521600000, '{"xH₂O (with standards)":"0","xCO₂ (with standards)":"205"}', 'DefaultExternalStandard');

-- 2024-07-01
INSERT INTO calibration
  (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES
  (4, 1, 'EXTERNAL_STANDARD', 'std1', 1719792000000, '{"xH₂O (with standards)":"0","xCO₂ (with standards)":"107"}', 'DefaultExternalStandard');

INSERT INTO calibration
  (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES
  (5, 1, 'EXTERNAL_STANDARD', 'std2', 1719792000000, '{"xH₂O (with standards)":"0","xCO₂ (with standards)":"207"}', 'DefaultExternalStandard');
