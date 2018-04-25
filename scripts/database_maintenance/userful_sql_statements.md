# Useful SQL statements and queries

## Remove all NEEDS_FLAG
This sets the user flag to Questionable for all records that need a flag in a given dataset.
```sql
update equilibrator_pco2 set user_flag = 3 where measurement_id in (select id from dataset_data where dataset_id = %%DATASET_ID%%) and user_flag = -10;
```
