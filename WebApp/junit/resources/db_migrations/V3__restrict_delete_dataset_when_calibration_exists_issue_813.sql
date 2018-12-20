ALTER TABLE calibration_data ADD CONSTRAINT fk_dataset_id FOREIGN KEY (dataset_id)
REFERENCES dataset(id) ON UPDATE RESTRICT ON DELETE RESTRICT;

-- Rollback: ALTER TABLE calibration_data DROP FOREIGN KEY fk_dataset_id;
