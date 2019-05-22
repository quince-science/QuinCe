-- Add display order to sensor types
ALTER TABLE sensor_types
ADD COLUMN display_order INT NOT NULL AFTER diagnostic;

UPDATE sensor_types SET display_order = 1 WHERE id = 1;
UPDATE sensor_types SET display_order = 2 WHERE id = 2;
UPDATE sensor_types SET display_order = 3 WHERE id = 3;
UPDATE sensor_types SET display_order = 4 WHERE id = 4;
UPDATE sensor_types SET display_order = 4 WHERE id = 5;
UPDATE sensor_types SET display_order = 4 WHERE id = 6;
UPDATE sensor_types SET display_order = 5 WHERE id = 7;
UPDATE sensor_types SET display_order = 6 WHERE id = 8;
UPDATE sensor_types SET display_order = 7 WHERE id = 9;
UPDATE sensor_types SET display_order = 8 WHERE id = 10;
UPDATE sensor_types SET display_order = 9 WHERE id = 11;
UPDATE sensor_types SET display_order = 10 WHERE id = 12;
UPDATE sensor_types SET display_order = 11 WHERE id = 13;
UPDATE sensor_types SET display_order = 12 WHERE id = 14;
UPDATE sensor_types SET display_order = 13 WHERE id = 15;
UPDATE sensor_types SET display_order = 14 WHERE id = 16;
UPDATE sensor_types SET display_order = 15 WHERE id = 17;
UPDATE sensor_types SET display_order = 16 WHERE id = 18;
UPDATE sensor_types SET display_order = 17 WHERE id = 19;
UPDATE sensor_types SET display_order = 18 WHERE id = 20;
UPDATE sensor_types SET display_order = 18 WHERE id = 21;
