-- Create a dataset for the period 2021-01-01T00:00:00Z to 2021-01-02T00:00:00Z
-- Properties are taken from the instrument
-- Status = 0 (Waiting), Status date = 2021-01-04T12:00:00Z
-- Last touched is 2021-01-05T0:00:00Z

-- Assumes instrument from testbase.instrument
INSERT INTO dataset (
    id, instrument_id, name, start, end, min_longitude,
    max_longitude, min_latitude, max_latitude, status, nrt, status_date,
    properties, messages_json, last_touched
  ) VALUES (
    1, 1, 'BSBS20210101', 1609459200, 1609545600, 0, 0, 0, 0, 0, 0, 1609718400,
    '{"_INSTRUMENT":{"depth":"5","postFlushingTime":"0","preFlushingTime":"0"},"Underway Marine pCO₂":{},"Underway Atmospheric pCO₂":{"atm_pres_sensor_height":"10.0"}}',
    NULL, 1609804800
  );
