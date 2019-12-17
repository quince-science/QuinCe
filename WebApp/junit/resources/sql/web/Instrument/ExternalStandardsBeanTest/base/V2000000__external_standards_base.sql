-- Basic instrument setup

-- User
INSERT INTO user (id, email, salt, password)
  VALUES (1000000, 'test@test.com', 'FF', 'FF');

-- Instrument
INSERT INTO instrument (id, owner, name)
  VALUES (1000000, 1000000, 'Test Instrument');

-- File definition
INSERT INTO file_definition (id, instrument_id, description, column_separator,
  header_type, column_header_rows, column_count)
  
  VALUES (1000000, 1000000, 'File', ',', 0, 0, 12);
  
-- External standard run types
INSERT INTO run_type (file_definition_id, run_name, category_code)
  VALUES (1000000, 'std2', -3);
INSERT INTO run_type (file_definition_id, run_name, category_code)
  VALUES (1000000, 'std3', -3);
INSERT INTO run_type (file_definition_id, run_name, category_code)
  VALUES (1000000, 'std4', -3);
