ALTER TABLE `quince_v2`.`dataset` 
ADD COLUMN `min_longitude` DOUBLE NULL AFTER `end`,
ADD COLUMN `max_longitude` DOUBLE NULL AFTER `min_longitude`,
ADD COLUMN `min_latitude` DOUBLE NULL AFTER `max_longitude`,
ADD COLUMN `max_latitude` DOUBLE NULL AFTER `min_latitude`;
