-- Add field for run type awareness
ALTER TABLE sensor_types ADD COLUMN run_type_aware TINYINT(1) DEFAULT 0 NOT NULL AFTER use_zero_in_calibration;

UPDATE sensor_types SET run_type_aware = 1 WHERE name = 'Gas Stream Pressure';
UPDATE sensor_types SET run_type_aware = 1 WHERE name = 'xCO₂ (with standards)';
UPDATE sensor_types SET run_type_aware = 1 WHERE name = 'xH₂O (with standards)';
UPDATE sensor_types SET run_type_aware = 1 WHERE name = 'xCO₂ (wet, no standards)';
UPDATE sensor_types SET run_type_aware = 1 WHERE name = 'xCO₂ (dry, no standards)';
