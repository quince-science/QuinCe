-- Datasets for calibration edits. The calibration beans must determine
-- which of these datasets will be affected by different edit actions.

-- 2024-02-20 to 2024-03-10
INSERT INTO dataset
  (id, instrument_id, name, start, end, min_longitude, max_longitude, min_latitude, max_latitude, status, status_date, nrt, exported)
  VALUES
  (1, 1, 'D1', 1708387200000, 1710028800000, -10, 10, -10, 10, 50, 1718197125000, 0, 0);

-- 2024-05-10 to 2024-05-20
INSERT INTO dataset
  (id, instrument_id, name, start, end, min_longitude, max_longitude, min_latitude, max_latitude, status, status_date, nrt, exported)
  VALUES
  (2, 1, 'D2', 1715299200000, 1716163200000, -10, 10, -10, 10, 50, 1718197125000, 0, 0);

-- 2024-06-10 to 2024-06-20
INSERT INTO dataset
  (id, instrument_id, name, start, end, min_longitude, max_longitude, min_latitude, max_latitude, status, status_date, nrt, exported)
  VALUES
  (3, 1, 'D3', 1717977600000, 1718841600000, -10, 10, -10, 10, 50, 1718197125000, 0, 0);

-- 2024-07-10 to 2024-07-20
INSERT INTO dataset
  (id, instrument_id, name, start, end, min_longitude, max_longitude, min_latitude, max_latitude, status, status_date, nrt, exported)
  VALUES
  (4, 1, 'D4', 1720569600000, 1721433600000, -10, 10, -10, 10, 50, 1718197125000, 0, 0);

