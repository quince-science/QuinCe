-- Coordinates for Argo data

-- Drop the old cycle field
ALTER TABLE coordinates DROP COLUMN cycle;

-- Add the new fields
ALTER TABLE coordinates ADD cycle_number INT(3);
ALTER TABLE coordinates ADD nprof INT(2);
ALTER TABLE coordinates ADD direction CHAR(1);
ALTER TABLE coordinates ADD nlevel INT(3);
ALTER TABLE coordinates ADD pres DOUBLE;
ALTER TABLE coordinates ADD source_file VARCHAR(25);

-- Sensor types for coordinate components
INSERT INTO sensor_types (
    name, vargroup, display_order, column_code, source_columns
  ) VALUES (
    'Cycle Number', 'Coordinate', 50, 'CYCLE_NUMBER', 'cycle_number'
  );

INSERT INTO sensor_types (
    name, vargroup, display_order, column_code, source_columns
  ) VALUES (
    'Profile', 'Coordinate', 51, 'NPROF', 'nprof'
  );

INSERT INTO sensor_types (
    name, vargroup, display_order, column_code, source_columns
  ) VALUES (
    'Direction', 'Coordinate', 52, 'DIRECTION', 'direction'
  );

INSERT INTO sensor_types (
    name, vargroup, display_order, column_code, source_columns
  ) VALUES (
    'Level', 'Coordinate', 53, 'NLEVEL', 'nlevel'
  );

INSERT INTO sensor_types (
    name, vargroup, display_order, column_code, source_columns
  ) VALUES (
    'Pressure (Depth)', 'Coordinate', 54, 'PRES', 'pres'
  );

INSERT INTO sensor_types (
    name, vargroup, display_order, column_code, source_columns
  ) VALUES (
    'Source File', 'Coordinate', 55, 'SOURCE_FILE', 'source_file'
  );

-- Update salinity source columns
UPDATE sensor_types SET source_columns = 'salinity;ssps;tsg sal;sal;sss;psal' WHERE id = 2;
