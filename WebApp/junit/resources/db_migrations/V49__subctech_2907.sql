-- Use new P01 code for combined xCO2 columns (#3108)
UPDATE sensor_types SET column_code = 'XCO2WTUM' WHERE id IN (9, 45);

-- New sensor types
INSERT INTO sensor_types (
    name, vargroup, display_order, column_code, column_heading, units, source_columns
  ) VALUES (
    'Internal Pressure (differential)', 'Pressure', 420, 'PRESEQREL',
    'Internal Equilibration Pressure', 'mbar', 'dpressint'
  );

INSERT INTO sensor_types (
    name, vargroup, display_order, column_code, column_heading, units, source_columns
  ) VALUES (
    'xH₂O (no standards)', 'Moisture', 605, 'WMXRZZ01',
    'H₂O Mole Fraction', 'μmol mol-1', 'h2o'
  );

-- Update existing sensor types
UPDATE sensor_types SET source_columns = 'cell gas pressure;CellPress' WHERE name = 'Cell Gas Pressure';

-- Self-calibrating version
INSERT INTO variables (name, visible)
  VALUES ('SubCTech CO₂ (self calibrating)', 1);
  
-- Variable Sensors
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES ((SELECT id FROM variables WHERE name = 'SubCTech CO₂ (self calibrating)'),
          (SELECT id FROM sensor_types WHERE name = 'Internal Pressure (differential)'),
          0, 3, 4);

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES ((SELECT id FROM variables WHERE name = 'SubCTech CO₂ (self calibrating)'),
          (SELECT id FROM sensor_types WHERE name = 'Cell Gas Pressure'),
          0, 3, 4);

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES ((SELECT id FROM variables WHERE name = 'SubCTech CO₂ (self calibrating)'),
          (SELECT id FROM sensor_types WHERE name = 'xCO₂ (wet, no standards)'),
          1, 3, 4);

          INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES ((SELECT id FROM variables WHERE name = 'SubCTech CO₂ (self calibrating)'),
          (SELECT id FROM sensor_types WHERE name = 'xH₂O (no standards)'),
          0, 3, 4);

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES ((SELECT id FROM variables WHERE name = 'SubCTech CO₂ (self calibrating)'),
          (SELECT id FROM sensor_types WHERE name = 'Water Temperature'),
          0, 3, 4);
          
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES ((SELECT id FROM variables WHERE name = 'SubCTech CO₂ (self calibrating)'),
          (SELECT id FROM sensor_types WHERE name = 'Salinity'),
          0, 3, 4);


-- Self-calibrating version
INSERT INTO variables (name, visible, properties)
  VALUES ('SubCTech CO₂ (post calibration)', 1, '{"dependsQuestionAnswers":{"9":true}}');
  
-- Variable Sensors
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES ((SELECT id FROM variables WHERE name = 'SubCTech CO₂ (post calibration)'),
          (SELECT id FROM sensor_types WHERE name = 'Internal Pressure (differential)'),
          0, 3, 4);

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES ((SELECT id FROM variables WHERE name = 'SubCTech CO₂ (post calibration)'),
          (SELECT id FROM sensor_types WHERE name = 'Cell Gas Pressure'),
          0, 3, 4);

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES ((SELECT id FROM variables WHERE name = 'SubCTech CO₂ (post calibration)'),
          (SELECT id FROM sensor_types WHERE name = 'xCO₂ (with standards)'),
          1, 3, 4);

          INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES ((SELECT id FROM variables WHERE name = 'SubCTech CO₂ (post calibration)'),
          (SELECT id FROM sensor_types WHERE name = 'xH₂O (with standards)'),
          0, 3, 4);

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES ((SELECT id FROM variables WHERE name = 'SubCTech CO₂ (post calibration)'),
          (SELECT id FROM sensor_types WHERE name = 'Water Temperature'),
          0, 3, 4);
          
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES ((SELECT id FROM variables WHERE name = 'SubCTech CO₂ (post calibration)'),
          (SELECT id FROM sensor_types WHERE name = 'Salinity'),
          0, 3, 4);
