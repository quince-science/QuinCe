-- Remove guesses from CO2
UPDATE sensor_types SET source_columns = NULL WHERE id = 9;
