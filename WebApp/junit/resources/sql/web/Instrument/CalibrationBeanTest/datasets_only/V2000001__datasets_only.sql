-- A pair of datasets

-- 2019-06-03T00:00:00 to 2019-06-05T00:00:00
INSERT INTO dataset (id, instrument_id, name, start, end, status, status_date)
  VALUES (1001, 1000000, 'A', 1559520000000, 1559692800000, 4, 0);

-- 2019-06-10T00:00:00 to 2019-06-015T00:00:00
INSERT INTO dataset (id, instrument_id, name, start, end, status, status_date)
  VALUES (1002, 1000000, 'B', 1560124800000, 1560556800000, 4, 0);
