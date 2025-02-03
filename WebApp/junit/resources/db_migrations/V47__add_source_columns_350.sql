-- Add Source Columns field
ALTER TABLE sensor_types ADD source_columns TEXT NULL AFTER `units`;

-- Water Temperature
UPDATE sensor_types SET source_columns = 'intake temp;sst;ssjt;intaketemp;intake_temp;temp (intake);' WHERE id = 1;

-- Salinity
UPDATE sensor_types SET source_columns = 'salinity;ssps;tsg sal;sal;sss' WHERE id = 2;

-- Equilibrator Temperature
UPDATE sensor_types SET source_columns = 'equ temp;equ_t;tempequ;equtemp' WHERE id = 3;

-- xH2O
UPDATE sensor_types SET source_columns = 'h2o mm/m;xh2o;h2omm_m' WHERE id = 8;

-- xCO2
UPDATE sensor_types SET source_columns = 'co2 um/m;xco2;co2um_m' WHERE id = 9;

-- Atmospheric Pressure
UPDATE sensor_types SET source_columns = 'atm press;atm pres;atm pressure' WHERE id = 10;

-- Diagnostic gas flow
UPDATE sensor_types SET source_columns = 'licor flow;vent flow;licorflow;ventflow;bypass flow' WHERE id = 13;

-- Diagnostic water flow
UPDATE sensor_types SET source_columns = 'h2o flow;waterflow' WHERE id = 14;

-- Air Temperature
UPDATE sensor_types SET source_columns = 'air temperature;Air temperature [degrees C];Air Temp [degC]' WHERE id = 23;

-- Raw Detector Signal (CONTROS)
UPDATE sensor_types SET source_columns = 'signal_raw' WHERE id = 33;

-- Reference Signal (CONTROS)
UPDATE sensor_types SET source_columns = 'signal_ref' WHERE id = 34;

-- Zero mode (CONTROS)
UPDATE sensor_types SET source_columns = 'zero' WHERE id = 35;

-- Flush mode (CONTROS)
UPDATE sensor_types SET source_columns = 'flush' WHERE id = 36;

-- Runtime (CONTROS)
UPDATE sensor_types SET source_columns = 'runtime' WHERE id = 37;

-- Gas Stream Temperature (Pro Oceanus)
UPDATE sensor_types SET source_columns = 't_gas' WHERE id = 38;

-- Gas Stream Pressure (Pro Oceanus)
UPDATE sensor_types SET source_columns = 'p_ndir' WHERE id = 40;

-- Membrane Pressure (Pro Oceanus)
UPDATE sensor_types SET source_columns = 'p_in' WHERE id = 41;

-- xCO2 wet no standards (Pro Oceanus etc)
UPDATE sensor_types SET source_columns = 'co2 concentration [ppmv]' WHERE id = 45;

-- Pro Oceanus Current Count
UPDATE sensor_types SET source_columns = 'current a/d;current count;sample a/d' WHERE id = 54;

-- Pro Oceanus Cell Gas Pressure
UPDATE sensor_types SET source_columns = 'cell gas pressure' WHERE id = 55;

-- Depth
UPDATE sensor_types SET source_columns = 'depth' WHERE id = 61;
