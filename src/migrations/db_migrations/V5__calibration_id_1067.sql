-- Add a normal ID primary key to the calibrations table
-- We have to remove the Instrument foreign key and then re-add it

ALTER TABLE calibration DROP FOREIGN KEY CALIBRATION_INSTRUMENT;

ALTER TABLE calibration
  ADD COLUMN id INT(11) NOT NULL AUTO_INCREMENT FIRST,
  DROP PRIMARY KEY, ADD PRIMARY KEY (id);
  
ALTER TABLE calibration ADD CONSTRAINT CALIBRATION_INSTRUMENT
  FOREIGN KEY (instrument_id) REFERENCES instrument (id)
  ON DELETE NO ACTION ON UPDATE NO ACTION;

-- Reinstate the original primary key as a separate index
-- to enforce uniqueness
ALTER TABLE calibration
  ADD UNIQUE INDEX CALIBRATION_UNIQUE
  (instrument_id ASC, type ASC, target ASC, deployment_date ASC);