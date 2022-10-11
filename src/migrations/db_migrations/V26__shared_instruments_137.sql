CREATE TABLE shared_instruments (
  instrument_id INT NOT NULL,
  shared_with INT NOT NULL,
  PRIMARY KEY (instrument_id, shared_with),
  INDEX sharedinstruments_sharedwith_idx (shared_with ASC),
  CONSTRAINT sharedinstruments_instrument
    FOREIGN KEY (instrument_id)
    REFERENCES instrument (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT sharedinstruments_sharedwith
    FOREIGN KEY (shared_with)
    REFERENCES user (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);