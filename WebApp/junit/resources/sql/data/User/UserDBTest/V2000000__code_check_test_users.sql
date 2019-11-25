-- Users with various combinations of password and email codes

-- No codes
INSERT INTO user (email, salt, password, firstname, surname, email_code, email_code_time, password_code, password_code_time)
  VALUES('nocodes@test.com', 'FF', 'FF', 'Test', 'Test', NULL, NULL, NULL, NULL);

-- Email code only
INSERT INTO user (email, salt, password, firstname, surname, email_code, email_code_time, password_code, password_code_time)
  VALUES('emailcode@test.com', 'FF', 'FF', 'Test', 'Test', 'IAMACODE', CURRENT_TIMESTAMP(), NULL, NULL);

-- Password code only
INSERT INTO user (email, salt, password, firstname, surname, email_code, email_code_time, password_code, password_code_time)
  VALUES('passwordcode@test.com', 'FF', 'FF', 'Test', 'Test', NULL, NULL, 'IAMACODE', CURRENT_TIMESTAMP());

-- Email and password codes
INSERT INTO user (email, salt, password, firstname, surname, email_code, email_code_time, password_code, password_code_time)
  VALUES('bothcodes@test.com', 'FF', 'FF', 'Test', 'Test', 'IAMACODE', CURRENT_TIMESTAMP(), 'IAMACODE', CURRENT_TIMESTAMP());

