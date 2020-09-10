-- Delete all existing jobs.
DELETE FROM job;

-- Rename the parameters field
ALTER TABLE job CHANGE parameters properties MEDIUMTEXT;
