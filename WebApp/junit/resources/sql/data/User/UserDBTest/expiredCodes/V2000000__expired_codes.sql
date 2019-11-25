-- User with expired email and password codes
INSERT INTO user (email, salt, password, firstname, surname, email_code, email_code_time, password_code, password_code_time)
  VALUES('expiredcodes@test.com', 'FF', 'FF', 'Test', 'Test',
    'IAMACODE', TIMESTAMP WITH TIME ZONE '1900-01-01 00:00:00Z',
    'IAMACODE', TIMESTAMP WITH TIME ZONE '1900-01-01 00:00:00Z');
