-- Change the column codes for equilibrator pressures
UPDATE sensor_types SET column_code = 'LPRES0EQ_equil' WHERE id = 26;
UPDATE sensor_types SET column_code = 'LPRES0EQ_atm' WHERE id = 27;
