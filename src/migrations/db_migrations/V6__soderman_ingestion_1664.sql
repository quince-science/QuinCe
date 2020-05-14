-- Updating database to allow ingestion of data supplied by Harald Soderman
INSERT INTO variables(name) VALUES('Soderman');

-- New sensor types
INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (28, 'H₂O Water vapour mixing ratio','Moisture', NULL, NULL, NULL, 0, 0, 1205,'WMXRZZ01','H20 mole fraction','ppm by volume');
 
INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (29, 'δH₂¹⁸O','Concentration', NULL, NULL, NULL, 0, 0, 1210,'HSH2OD18','delta value of H218O water molecules','permil');
 
INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (30, 'δHD¹⁶O','Concentration', NULL, NULL, NULL, 0, 0, 1215, 'HSHDODDH','delta value of HDO water molecules','permil');
 
INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (31, 'CH₄ Mixing ratio','Concentration', NULL, NULL, NULL, 0, 0, 1220, 'CF12N284','Mass concentration of methane in air','not used');
 
INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (32, 'Ambient Pressure (Torr)','Pressure', NULL, NULL, NULL, 0, 0, 1225, 'PRESAMBT','Instrument ambient air pressure','Torr');

-- Preexisting sensor_type ambient_pressure creates ambiguity, renamed.
UPDATE sensor_types SET name = 'Pressure at instrument' WHERE id=7;


-- New corresponding variable_sensors connections. 
INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade) 
  VALUES (5, 28, 0, 3, 4);

 
INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade) 
  VALUES (5, 29, 0, 3, 4);

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade) 
  VALUES (5, 30, 0, 3, 4);

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade) 
  VALUES (5, 31, 0, 3, 4);

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (5, 32, 0, 3, 4);


