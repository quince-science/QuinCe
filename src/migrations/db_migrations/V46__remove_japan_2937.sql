-- Remove Japan Custom variable
DELETE FROM variable_sensors WHERE variable_id IN (SELECT id FROM variables WHERE name = 'Japan Custom (temp)');
DELETE FROM variables WHERE name = 'Japan Custom (temp)';
