-- Allow file columns to be assigned to more than one SensorType
ALTER TABLE file_column 
  DROP FOREIGN KEY FILECOLUMN_FILEDEFINITION;

ALTER TABLE file_column DROP INDEX FILEDEFINITIONID_FILECOLUMN;
ALTER TABLE file_column
  ADD UNIQUE INDEX FILEDEF_COL_SENSOR (file_definition_id ASC, file_column ASC, sensor_type ASC);
  
ALTER TABLE file_column 
  ADD CONSTRAINT FILECOL_FILEDEF
    FOREIGN KEY (file_definition_id)
    REFERENCES file_definition (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;
    

-- SAMI Sensor
    
-- pCO2 sensor type
INSERT INTO sensor_types (
    name, vargroup, parent, depends_on, depends_question, internal_calibration,
    diagnostic, display_order, column_code, column_heading, units
  ) VALUES (
    'pCO₂ (wet at equilibration)', 'CO₂', NULL, NULL, NULL, 0, 0, 1110,
    'PCO2IG02', 'pCO₂ (wet at equilibration)', 'μatm'
  );
  
-- Variable
INSERT INTO variables (name, attributes, properties)
  VALUES ('SAMI CO₂', NULL, NULL);

-- Variable sensors

-- SST (may be the same as EqT)
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'SAMI CO₂'),
    (SELECT id FROM sensor_types WHERE column_code = 'TEMPPR01'),
    0, 3, 4, NULL, NULL, NULL
  );
  
-- EqT
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'SAMI CO₂'),
    (SELECT id FROM sensor_types WHERE column_code = 'TEMPEQMN'),
    0, 3, 4, NULL, NULL, NULL
  );

-- pCO₂
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'SAMI CO₂'),
    (SELECT id FROM sensor_types WHERE column_code = 'PCO2IG02'),
    1, 3, 4, NULL, NULL, NULL
  );
