# Useful SQL statements and queries

## Remove all NEEDS_FLAG
This sets the user flag to Questionable for all records that need a flag in a given dataset.
```sql
update sensor_values set user_qc_flag = 3, user_qc_message = 'FAKE: SET BY SCRIPT' where dataset_id = %%DATASET_ID%% and user_qc_flag = -10;
```
## Set a dataset's status
Manually set a dataset's status

```sql
update dataset set status = %%SATUS%% where id = %%DATASET_ID%%
```

Possible values:

* `-2` = Marked for deletion
* `-1` = Error
* ` 0` = Waiting
* ` 1` = Data extraction
* ` 2` = Marked for deletion
* ` 3` = Automatic QC
* ` 4` = Ready for QC
* ` 5` = Ready for submission
* ` 6` = Waiting for approval
* ` 7` = Waiting for automatic export
* ` 8` = Automatic export in progress
* ` 9` = Automatic export complete

Note that setting most of these statuses on their own doesn't make much sense,
because they're related to background processing that won't happen.

## Reset QC flags to Auto QC result
This statement will reset the User QC flag to `-10` (`NEEDS_FLAG`) for any record where the auto_qc was not GOOD
(i.e. where auto QC found an issue). It will also set the QC comment to `Auto QC`, which isn't correct but
we can't pull out the reason from the database. So **Don't use this in production without consulting the
relevant PI so they know what's happening**.

```sql
update sensor_values set user_qc_flag = -10, user_qc_message = 'Auto QC' where auto_qc is not null and dataset_id = %%DATASET_ID%%
```

This will not touch any QC flags that the user set on records that the Auto QC thought was good. To reset those
(to `ASSUMED_GOOD`):

```sql
update sensor_values set user_qc_flag = -2, user_qc_message = NULL where auto_qc is null and dataset_id = %%DATASET_ID%%
```
