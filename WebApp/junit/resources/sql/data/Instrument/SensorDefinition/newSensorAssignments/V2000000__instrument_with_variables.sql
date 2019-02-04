-- The instrument
INSERT INTO instrument (id, owner, name)
  VALUES (1, 1, 'Test Instrument');
  
-- Instrument uses the basic marine pCO2 variable
INSERT INTO instrument_variables (instrument_id, variable_id)
  VALUES (1, (SELECT id FROM variables WHERE name = 'Underway Marine pCO₂'));