ALTER TABLE dataset ADD COLUMN min_longitude DOUBLE NULL AFTER `end`;
ALTER TABLE dataset ADD COLUMN max_longitude DOUBLE NULL AFTER min_longitude;
ALTER TABLE dataset ADD COLUMN min_latitude DOUBLE NULL AFTER max_longitude;
ALTER TABLE dataset ADD COLUMN max_latitude DOUBLE NULL AFTER min_latitude;
