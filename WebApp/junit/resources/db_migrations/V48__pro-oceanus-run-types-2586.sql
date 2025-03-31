-- Add column guess for Pro Ocean Zero count
UPDATE sensor_types SET source_columns = 'zero a/d' WHERE name = 'ProOceanus Zero Count';