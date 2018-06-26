-- Renaming Athmospheric Pressure to Ambient Pressure globally

ALTER TABLE dataset_data CHANGE COLUMN atmospheric_pressure
ambient_pressure DOUBLE NULL;
ALTER TABLE calibration_data CHANGE COLUMN atmospheric_pressure
ambient_pressure DOUBLE NULL;
