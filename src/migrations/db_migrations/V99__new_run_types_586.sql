-- The category_code field is now an INT that either references
-- a varialbe in the variables table, or a special value

ALTER TABLE run_type ADD COLUMN new_category_code INT NOT NULL AFTER `category_code`;
UPDATE run_type SET new_category_code = -1 WHERE category_code = 'IGN';
UPDATE run_type SET new_category_code = -2 WHERE category_code = 'ALIAS';
UPDATE run_type SET new_category_code = -3 WHERE category_code = 'EXT';
UPDATE run_type SET new_category_code =
  (SELECT id FROM variables WHERE name='Underway Marine pCO₂') WHERE category_code = 'MCO2';

ALTER TABLE run_type DROP COLUMN category_code;
ALTER TABLE run_type CHANGE COLUMN `new_category_code` `category_code` INT NOT NULL;
