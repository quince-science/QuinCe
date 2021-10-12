-- Replace the existing single variable for Harald Sodemann's data
-- with four independent variables

-- Water Vapour Mixing Ratio
INSERT INTO variables (name) VALUES('Water Vapour Mixing Ratio');

INSERT INTO variable_sensors (
		variable_id, sensor_type, core, questionable_cascade,
		bad_cascade, export_column_short, export_column_long, export_column_code)
	VALUES (
		(SELECT id FROM variables WHERE name = 'Water Vapour Mixing Ratio'),
		(SELECT id FROM sensor_types WHERE name = 'H₂O Water vapour mixing ratio'),
		1, 3, 4,
		'Water Vapour Mixing Ratio', 'Water Vapour Mixing Ratio', 'WVMR');

-- d-excess
INSERT INTO variables (name) VALUES('D-Excess');

INSERT INTO variable_sensors (
		variable_id, sensor_type, core, questionable_cascade,
		bad_cascade, export_column_short, export_column_long, export_column_code)
	VALUES (
		(SELECT id FROM variables WHERE name = 'D-Excess'),
		(SELECT id FROM sensor_types WHERE name = 'δH₂¹⁸O'),
		1, 3, 4,
		'δH₂¹⁸O', 'δH₂¹⁸O', 'δH₂¹⁸O');

INSERT INTO variable_sensors (
		variable_id, sensor_type, core, questionable_cascade,
		bad_cascade, export_column_short, export_column_long, export_column_code)
	VALUES (
		(SELECT id FROM variables WHERE name = 'D-Excess'),
		(SELECT id FROM sensor_types WHERE name = 'δHD¹⁶O'),
		1, 3, 4,
		'δHD¹⁶O', 'δHD¹⁶O', 'δHD¹⁶O');

UPDATE sensor_types SET internal_calibration = 1 WHERE name = 'δH₂¹⁸O';
UPDATE sensor_types SET internal_calibration = 1 WHERE name = 'δHD¹⁶O';

-- CH₄ Mixing ratio
INSERT INTO variables (name) VALUES('CH₄ Mixing ratio');

INSERT INTO variable_sensors (
		variable_id, sensor_type, core, questionable_cascade,
		bad_cascade, export_column_short, export_column_long, export_column_code)
	VALUES (
		(SELECT id FROM variables WHERE name = 'CH₄ Mixing ratio'),
		(SELECT id FROM sensor_types WHERE name = 'CH₄ Mixing ratio'),
		1, 3, 4,
		'CH₄ Mixing ratio', 'CH₄ Mixing ratio', 'CH4MR');

-- Ambient Pressure (Torr)
INSERT INTO variables (name) VALUES('Ambient Pressure (Torr)');

INSERT INTO variable_sensors (
		variable_id, sensor_type, core, questionable_cascade,
		bad_cascade, export_column_short, export_column_long, export_column_code)
	VALUES (
		(SELECT id FROM variables WHERE name = 'Ambient Pressure (Torr)'),
		(SELECT id FROM sensor_types WHERE name = 'Ambient Pressure (Torr)'),
		1, 3, 4,
		'Ambient Pressure', 'Ambient Pressure', 'AMBPRESS');

    
-- Make the old Harald variable invisible
ALTER TABLE variables ADD COLUMN visible TINYINT NOT NULL DEFAULT 1 AFTER name;
UPDATE variables SET visible = 0 WHERE name = 'Soderman';

