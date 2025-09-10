-- WARNING: All existing SubCTech Instruments must be deleted when running this migration

-- Remove 'self-calibrating' question. It was a red herring.
UPDATE variables SET attributes = NULL WHERE id=22;

-- Add the Fixed Run Types flag
UPDATE variables SET properties = '{"dependsQuestionAnswers":{"9":true}, "fixedRunTypes": true}' WHERE id=22;

-- Add a special xCO2 sensor type for SubCTech
-- It has its own calibration routine
INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units, source_columns, internal_calibration, run_type_aware)
  VALUES ('SubCTech xCO₂', 'CO₂', 1150, 'XCO2WTUM', 'CO₂ Mole Fraction', 'μmol mol-1', 'CO2', 1, 1);
INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units, source_columns, internal_calibration, run_type_aware)
  VALUES ('SubCTech xH₂O', 'Moisture', 610, 'WMXRZZ01', 'H₂O Mole Fraction', 'μmol mol-1', 'H2O', 1, 1);

-- Rename the variable for water
UPDATE variables SET name = 'SubCTech CO₂ Water' WHERE id=22;

-- Adjust the variable sensors for water
UPDATE variable_sensors SET sensor_type = (SELECT id FROM sensor_types WHERE name = 'SubCTech xCO₂') WHERE variable_id = 22 AND sensor_type = 9;
UPDATE variable_sensors SET sensor_type = (SELECT id FROM sensor_types WHERE name = 'SubCTech xH₂O') WHERE variable_id = 22 AND sensor_type = 8;

-- Add the SubCTech Air variable
INSERT INTO variables (name, visible) VALUES ('SubCTech CO₂ Air', 1);

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade) VALUES (23, 1, 0, 3, 4);
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade) VALUES (23, 2, 0, 3, 4);
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade) VALUES (23, (SELECT id FROM sensor_types WHERE name = 'SubCTech xH₂O'), 0, 3, 4);
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade) VALUES (23, (SELECT id FROM sensor_types WHERE name = 'SubCTech xCO₂'), 1, 3, 4);
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade) VALUES (23, 55, 0, 3, 4);
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade) VALUES (23, 62, 0, 3, 4);

-- Put preset run types in the database.
-- They used to be in RunTypeAssignments.
-- This is for all variables.

-- Underway pCO2 Water
UPDATE variables SET properties = '{"presetRunTypes": [{"runType": ["std1", "std1-drain", "std1z", "std1z-drain"], "category": -3}, {"runType": ["std2", "std2-drain", "std2s", "std2s-drain"], "category": -3}, {"runType": ["std3", "std3-drain", "std3s", "std3s-drain"], "category": -3}, {"runType": ["std4", "std4-drain", "std4s", "std4s-drain"], "category": -3}, {"runType": ["std5", "std5-drain", "std5s", "std5s-drain", "std5z", "std5z-drain"], "category": -3}, {"runType": ["emergency stop"], "category": -1}, {"runType": ["go to sleep"], "category": -1}, {"runType": ["ign"], "category": -1}, {"runType": ["shut down"], "category": -1}, {"runType": ["wake up"], "category": -1}, {"runType": ["equ", "equ-drain"], "category": 1}]}' WHERE id = 1;
UPDATE variables SET properties = '{"presetRunTypes": [{"runType": ["std1", "std1-drain", "std1z", "std1z-drain"], "category": -3}, {"runType": ["std2", "std2-drain", "std2s", "std2s-drain"], "category": -3}, {"runType": ["std3", "std3-drain", "std3s", "std3s-drain"], "category": -3}, {"runType": ["std4", "std4-drain", "std4s", "std4s-drain"], "category": -3}, {"runType": ["std5", "std5-drain", "std5s", "std5s-drain", "std5z", "std5z-drain"], "category": -3}, {"runType": ["emergency stop"], "category": -1}, {"runType": ["go to sleep"], "category": -1}, {"runType": ["ign"], "category": -1}, {"runType": ["shut down"], "category": -1}, {"runType": ["wake up"], "category": -1}, {"runType": ["atm", "atm-drain"], "category": 2}]}' WHERE id = 2;
UPDATE variables SET properties = '{"presetRunTypes": [{"runType": ["w m"], "category": 8 }]}' WHERE id = 8;
UPDATE variables SET properties = '{"presetRunTypes": [{"runType": ["a m"], "category": 9 }]}' WHERE id = 9;
UPDATE variables SET properties = '{"presetRunTypes": [{"runType": ["1"], "category": -3}, {"runType": ["2"], "category": -3}, {"runType": ["4"], "category": -1}, {"runType": ["5"], "category": 22}, {"runType": ["7"], "category": -1}, {"runType": ["13"], "category": -1}, {"runType": ["15"], "category": -3}, {"runType": ["18"], "category": -3}, {"runType": ["19"], "category": -1}, {"runType": ["20"], "category": -1}, {"runType": ["21"], "category": -1}]}' WHERE id = 22;
UPDATE variables SET properties = '{"presetRunTypes": [{"runType": ["1"], "category": -3}, {"runType": ["2"], "category": -3}, {"runType": ["4"], "category": -1}, {"runType": ["22"], "category": 23}, {"runType": ["7"], "category": -1}, {"runType": ["13"], "category": -1}, {"runType": ["15"], "category": -3}, {"runType": ["18"], "category": -3}, {"runType": ["19"], "category": -1}, {"runType": ["20"], "category": -1}, {"runType": ["21"], "category": -1}]}' WHERE id = 23;

-- Add waterTemp as a recognised column header
UPDATE sensor_types SET source_columns='waterTemp' WHERE id=1;
