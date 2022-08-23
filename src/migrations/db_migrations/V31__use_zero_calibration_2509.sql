ALTER TABLE sensor_types ADD COLUMN use_zero_in_calibration TINYINT NOT NULL DEFAULT 1 AFTER internal_calibration;
UPDATE sensor_types SET use_zero_in_calibration = 0 WHERE name = 'xCO₂ (with standards)';
