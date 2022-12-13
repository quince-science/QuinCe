-- Add platform_name field. Has to be populated manually for existing instruments.
ALTER TABLE instrument ADD platform_name VARCHAR(100) NOT NULL AFTER name;
