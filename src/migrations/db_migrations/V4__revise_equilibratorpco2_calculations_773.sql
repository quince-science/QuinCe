ALTER TABLE equilibrator_pco2 ADD COLUMN pco2_sst DOUBLE NULL DEFAULT NULL AFTER pco2_te_wet;
ALTER TABLE equilibrator_pco2 DROP COLUMN pco2_te_dry;
ALTER TABLE equilibrator_pco2 DROP COLUMN fco2_te;
