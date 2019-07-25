-- THIS CAN BE FOLDED INTO THE sensor_types CREATION SCRIPT

ALTER TABLE sensor_types ADD units VARCHAR(20) NULL AFTER display_order;
ALTER TABLE sensor_types ADD column_code VARCHAR(50) NULL AFTER units;
ALTER TABLE sensor_types ADD column_heading VARCHAR(100) NULL AFTER column_code;


UPDATE sensor_types SET name = 'Wind Speed (absolute)' WHERE id = 19;
UPDATE sensor_types SET name = 'Speed' WHERE id = 17;
UPDATE sensor_types SET name = 'Heading' WHERE id = 18;
  
UPDATE sensor_types SET units = '°C', column_code = 'TEMPPR01', column_heading = 'Water Temperature' WHERE id = 1;
UPDATE sensor_types SET units = 'psu', column_code = 'PSALPR01', column_heading = 'Practical Salinity' WHERE id = 2;
UPDATE sensor_types SET units = '°C', column_code = 'TEMPEQMN', column_heading = 'Temperature of Equilibration' WHERE id = 3;
UPDATE sensor_types SET units = 'hPa', column_code = 'PRESEQ', column_heading = 'Pressure in Equilibrator' WHERE id = 4;
UPDATE sensor_types SET units = 'hPa', column_code = 'PRESEQ', column_heading = 'Absolute Pressure in Equilibrator' WHERE id = 5;
UPDATE sensor_types SET units = 'hPa', column_code = 'PRESEQREL', column_heading = 'Relative Pressure in Equilibrator' WHERE id = 6;
UPDATE sensor_types SET units = 'hPa', column_code = 'PRESAMB', column_heading = 'Instrument Ambient Pressure' WHERE id = 7;
UPDATE sensor_types SET units = 'μmol mol-1', column_code = 'WMXRZZ01', column_heading = 'H₂O Mole Fraction' WHERE id = 8;
UPDATE sensor_types SET units = 'μmol mol-1', column_code = 'XCO2EQWT', column_heading = 'CO₂ Mole Fraction' WHERE id = 9;
UPDATE sensor_types SET units = 'hPa', column_code = 'CAPHZZ01', column_heading = 'Atmospheric Pressure' WHERE id = 10;
UPDATE sensor_types SET units = 'm s-1', column_code = 'APSAZZ01', column_heading = 'Speed' WHERE id = 17;
UPDATE sensor_types SET units = 'deg', column_code = 'HDNGGP01', column_heading = 'Heading' WHERE id = 18;
UPDATE sensor_types SET units = 'm s-1', column_code = 'EWSBZZ01', column_heading = 'Wind Speed (absolute)' WHERE id = 19;
UPDATE sensor_types SET units = 'deg', column_code = 'EWDAZZ01', column_heading = 'Wind Direction (absolute)' WHERE id = 20;
UPDATE sensor_types SET units = 'deg', column_code = 'ERWDZZ01', column_heading = 'Wind Direction (relative)' WHERE id = 21;


-- ADD NEW RECORDS

-- Relative wind speed
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, units, column_code, column_heading)
  VALUES ('Wind Speed (relative)', 'Other', NULL, NULL, NULL, 0, 0, 100, 'deg', 'ERWDZZ01', 'Wind Speed (relative)');
