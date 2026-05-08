-- Datasets for calibration edits. The calibration beans must determine
-- which of these datasets will be affected by different edit actions.

-- 2024-02-20 to 2024-03-10
INSERT INTO dataset
  (id, instrument_id, name, start, end, min_longitude, max_longitude, min_latitude, max_latitude, status, status_date, nrt, exported)
  VALUES
  (1, 1, 'D1', '1676851200000', '1678406400000', -10, 10, -10, 10, 50, 1718197125000, 0, 0);

-- 2024-05-10 to 2024-05-20
INSERT INTO dataset
  (id, instrument_id, name, start, end, min_longitude, max_longitude, min_latitude, max_latitude, status, status_date, nrt, exported)
  VALUES
  (2, 1, 'D2', '1683676800000', '1684540800000', -10, 10, -10, 10, 50, 1718197125000, 0, 0);

-- 2024-06-10 to 2024-06-20
INSERT INTO dataset
  (id, instrument_id, name, start, end, min_longitude, max_longitude, min_latitude, max_latitude, status, status_date, nrt, exported)
  VALUES
  (3, 1, 'D3', '1686355200000', '1687219200000', -10, 10, -10, 10, 50, 1718197125000, 0, 0);

-- 2024-07-10 to 2024-07-20
INSERT INTO dataset
  (id, instrument_id, name, start, end, min_longitude, max_longitude, min_latitude, max_latitude, status, status_date, nrt, exported)
  VALUES
  (4, 1, 'D4', '1688947200000', '1689811200000', -10, 10, -10, 10, 50, 1718197125000, 0, 0);

