-- Users with various combinations of password and email codes

-- No codes
INSERT INTO user (email, salt, password, firstname, surname, email_code, email_code_time, password_code, password_code_time)
  VALUES('nocodes@test.com', 'FF', 'FF', 'Test', 'Test', NULL, NULL, NULL, NULL)
