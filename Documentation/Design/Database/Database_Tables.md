% QuinCe Database Tables

# Introduction
This document specifies the database tables that are used in the QuinCe project.

Field names followed by stars(\*) indicate primary keys. Multiple fields are sometimes combined to produce
a compound primary key.

The field type `F_KEY` indicates a lookup to a value in another table. The table in question will be
made clear in the field description.

## Timestamps
To remove complications related to time zones, all times related to data will be stored in the database
as milliseconds since the epoch in UTC. Such fields will be in this document with the type `Datetime`.
In MySQL/MariaDB these fields will have a type of `bigint(20)`. These are not to be confused with fields
given the type `Timestamp`, which will use MySQL/MariaDB's own date/time mechanism. These will be used
for recording times that are not critical to data integrity.

## Boolean fields
Fields marked as `Boolean` will be accessed using `setBoolean` and `getBoolean` methods in Java.
MySQL/MariaDB do not have a specific Boolean type, so in practice these fields will be a `tinyint` that
contain `1` or `0`.

## `created` and `modified`
Most tables will contain fields named `created` and `modified`, which store the creation and last
modification dates of each record. While these are not used at the time of writing, they are
included in case they become useful for future functionality.


# Users
This section defines tables related to users and the different parts
of the system they are linked to.

## `user`
Defines a user account

Field                | Type           | Description
:--------------------|:---------------|:---------------------------------------------------
`id`*                | `Integer`      | ID.
`email`              | `String`       | The user's email address - used as the username.†
`salt`               | `Binary`       | The salt used for the password.
`password`           | `Binary`       | Password field (salted and hashed).
`firstname`          | `String`       | The user's first/given name.
`surname`            | `String`       | The user's surname.
`email_code`         | `String`       | Code for verifying email address.
`email_code_time`    | `Timestamp`    | The time that the email verification code was generated.
`password_code`      | `String`       | Code for resetting password.
`password_code_time` | `Timestamp`    | The time that the password reset code was generated.
`permissions`        | `Integer`      | A field for defining user permissions. Defined as a bitmask.
`preferences`        | `String`       | The user's preferences.
`last_login`         | `Datetime`     | Records the most recent time that the user logged in.
`created`            | `Timestamp`    | The time the record was created.
`modified`           | `Timestamp`    | The time the record was last modified.

†Enforced uniqueness

#### Permissions
The `permissions` for a user are stored as a bit mask. The permissions bits that can be applied to a user are specified in the `User` class in the code.

# Variables
The database contains information on the supported variables that can be processed, and the sensor information
required for that processing.

## `variables`
This table contains the list of variables that can be processed in QuinCe. The `name` field is matched
in the code to determine which data reduction routines must be run for a given instrument (via the
`instrument_variables` table).

Field          | Type         | Description
:--------------|:-------------|:-------------------------------------------------------------   
`id`*          | `Integer`    | ID.
`name`         | `String`     | The name of the variable.
`visible`      | `Boolean`    | Can be used to hide a variable from the user interface.
`attributes`   | `String`     | Attributes that must be specified by the user when setting up an instrument that uses this variable. Stored as a JSON string.†
`properties`   | `String`     | Information required for configuration and/or processing of data for the variable. Stored as a JSON string.†
`created`      | `Timestamp`  | The time the record was created.
`modified`     | `Timestamp`  | The time the record was last modified.

#### †`attributes` and `properties`
These two fields can be confusing since they appear to refer to very similar concepts. However, their functions
distinct.

The `attributes` field contains details of parameters that the user must provide for this variable when
setting up the instrument (e.g. the atmospheric pressure sensor height for
atmospheric CO₂ measurements). The values set by the user are stored in the `instrument_variables` table.

The `properties` field contains details that the QuinCe code will use to set up the processing of the variable.
This may include such things as the run type values used to recognise measurements of different types, or the
names of calculation coefficients that must be entered for the processing.

## `sensor_types`
Each `variable` needs information from a number of sensors in order for the data reduction to be performed.
This table contains all the sensor types needed for the variables configured in the application.

The `variable_sensors` table (below) is used to specify which sensor types are required for which variables. This
allows a sensor type to be required for multiple variables.

Field                | Type        | Description
:--------------------|:------------|:--------------------------------------------------------------------------------------
`id`*                | `Integer`   | ID.
`name`               | `String`    | The name of the sensor type, as displayed to the user.
`vargroup`           | `String`    | Each sensor type is part of a `vargroup`, which groups sensors of similar types (e.g. temperatures, pressures etc.) together. At the time of writing these are not used for anything, but they could become useful for certain functions in the user interface.
`parent`             | `F_KEY`     | Some sensor types may be defined by one of a set of types, e.g. Equilibrator Pressure could be either absolute or differential. In this case, a parent 'Equilibrator Pressure' sensor type is defined, with two child types of 'Equilibrator Pressure (absolute)' and 'Equilibrator Pressure (differential)'. The ID of the parent type is set as the `parent` field of the two children.
`depends_on`         | `F_KEY`     | Indicates that this sensor requires another sensor type to be used.
`depends_question`   | `F_KEY`     | In some situations, the `depends_on` requirement is only triggered based on a question to be answered by the user (i.e. it cannot be determined in the code). The question to be answered is stored in this field, and displayed to the user at the appropriate time. It must be a Yes/No question, where a 'Yes' answer triggers the dependency. If this field is `NULL`, the `depends_on` requirement is always enforced.
`run_type_aware`     | `Boolean`   | Indicates that values from this sensor can be treated differently depending on the current Run Type of the instrument.
`diagnostic`         | `Boolean`   | Indicates that this sensor type is for diagnostics. These can be assigned to any instrument independently of its variables.
`display_order`      | `Integer`   | Any time that sensor types are displayed, they are ordered by the ascending value of this field.
`column_code`        | `String`    | The code that should be used in export files if the column header is a code. Ideally something from a controlled vocabulary (preferably [P01](https://vocab.seadatanet.org/p01-facet-search) or [P02](https://vocab.seadatanet.org/v_bodc_vocab_v2/search.asp?lib=P02)).
`column_heading`     | `String`    | The default column heading for this sensor type in export files. Ideally related to an entry from a controlled vocabuary, but does not need to be exact.
`units`              | `String`    | The units for values from this sensor.
`created`            | `Timestamp` | The time the record was created.
`modified`           | `Timestamp` | The time the record was last modified.

## `variable_sensors`
This table defines which `sensor_type`s are required for each `variable`.

Field                   | Type        | Description
:-----------------------|:------------|:--------------------------------------------------------------------------------------
`variable_id`*          | `F_KEY`     | The ID of the variable
`sensor_type`*          | `F_KEY`     | The ID of the sensor type
`core`                  | `Boolean`   | Indicates whether or not this is the core sensor type for the variable. QuinCe will create measurements based on values from the core sensor type.
`attribute_name`        | `String`    | For variables with attributes, this sensor type is only required if the attribute named here has the value specified in `attribute_value`.
`attribute_value`       | `String`    | For variables with attributes, this sensor type is only required if the attribute named in `attribute_name` has the value specified here.
`questionable_cascade`  | `Integer`   | Specifies the QC flag to be set on the data reduction result if a value from this sensor has a Questionable QC flag.
`bad_cascade`           | `Integer`   | Specifies the QC flag to be set on the data reduction result if a value from this sensor has a Bad QC flag.
`export_column_short`   | `String`    | If set, specifies the short column heading to be used for this sensor. Overrides `sensor_types.column_heading`.
`export_column_long`    | `String`    | If set, specifies the long column heading to be used for this sensor. Overrides `sensor_types.column_heading`.
`export_column_code`    | `String`    | If set, specifies the code to be used for this sensor. Overrides `sensor_types.column_code`.
`created`               | `Timestamp` | The time the record was created.
`modified`              | `Timestamp` | The time the record was last modified.


# Instruments
The definition of an instrument is quite complex and requires multiple tables

## `instrument`
This table defines the basic details of an instrument.

Field              | Type         | Description
:------------------|:-------------|:--------------------------------------------------------------------------------------
`id`*              | `Integer`    | ID.
`owner`            | `F_KEY`      | The ID of the user who owns this instrument.
`name`             | `String`     | The name of the instrument.
`platform_name`    | `String`     | The name of the platform on which this instrument is deployed.
`platform_code`    | `String`     | The identifying code of the platform (usually its ICES code).
`nrt`              | `Boolean`    | Indicates whether or not this instrument supplies near real time data.
`last_nrt_export`  | `Datetime`   | The last time that an NRT dataset was exported from this instrument.
`properties`       | `String`     | The configured properties for the instrument, stored as a JSON string.
`created`          | `Timestamp`  | The time the record was created.
`modified`         | `Timestamp`  | The time the record was last modified.

## `instrument_variables`
This table specifies which `variable`s an instrument provides data for, along with any specific configuration
required for the `variable`s' data reduction routines.

Field              | Type         | Description
:------------------|:-------------|:--------------------------------------------------------------------------------------
`instrument_id`*   | `F_KEY`      | The instrument ID.
`variable_id`*     | `F_KEY`      | The variable ID.
`properties`       | `String`     | Instrument-specific configuration for the variables. Stores the information entered by the user based on the `variables.attributes` field.†
`created`          | `Timestamp`  | The time the record was created.
`modified`         | `Timestamp`  | The time the record was last modified.

†The `properties` field should be renamed to `attributes` to match the `variables` table.
This is [Issue #3069](https://github.com/quince-science/QuinCe/issues/3069) in Github.

## `file_definition`
This table defines the layout of data files used with a given instrument. This definition will be set up by the user when they define their instrument. An instrument may have only one data file, or it may have multiple files if different data comes from different places.

Column indices and format information for the date/time and position are stored in this table, since they are universal for all instrument types. These are stored as JSON strings controlled by the code. Other column details are stored in the `file_columns` table.

All files must contain date/time information, while position information is optional.

Field                | Type        | Description
:--------------------|:------------|:--------------------------------------------------------------------------------------
`id`*                | `Integer`   | ID.
`instrument_id`      | `F_KEY`     | ID of the instrument that this data file belongs to.
`description`        | `String`    | File description.
`column_separator`   | `String`    | Column separator.
`header_type`        | `Integer`   | How the header is defined (number of lines or the last line string).
`header_lines`       | `Integer`   | Number of lines in the file header.
`header_end_string`  | `String`    | The line that indicates the end of the file header.
`column_header_rows` | `Integer`   | The number of column header rows in the file.
`column_count`       | `Integer`   | Number of colums in the file.
`lon_spec`           | `String`    | The longitude format specification (JSON string).
`lat_spec`           | `String`    | The latitude format specification (JSON string).
`datetime_spec`      | `String`    | The specification of the date/time (JSON string).
`created`            | `Timestamp` | The time the record was created.
`modified`           | `Timestamp` | The time the record was last modified.

## `file_column`
This table will contains details of the columns present in a given file definition.

Field                     |   Type       | Description
:-------------------------|:-------------|:-----------------------------------------------------------------
`file_definition_id`*     | `F_KEY`      | The ID of the file definition.
`file_column`*            | `Integer`    | Column where this sensor's value can be found in the data file.
`sensor_type`             | `String`     | Name of the sensor type, as identified in the `sensor_config.csv` file.
`sensor_name`             | `String`     | Name of this column as it will be displayed in QuinCe.
`primary_sensor`          | `Boolean`    | Indicates whether or not this is a primary sensor.
`value_column`            | `Integer`    | Position at which this value will be stored in the `voyage_data` table.
`depends_question_answer` | `Boolean`    | Answer to the Depends Question (see `sensor_types.depends_question`).
`missing_value`           | `String`     | Value that indicates a missing value (`NaN`, `-999` etc.).
`post_calibrated`         | `Boolean`    | Indicates whether values need to be calibrated by QuinCe.
`created`                 | `Timestamp`  | The time the record was created.
`modified`                | `Timestamp`  | The time the record was last modified.

## `run_type`
Each instrument will have a number of run types, for measurements, gas standards etc.

This table will hold details of which values in the Run Type column (as defined in the `file_definition` table) correspond to which run types.

Field                 | Type         | Description
:---------------------|:-------------|:-----------------------------------------------------------------
`file_definition_id`* | `F_KEY`      |  The ID of the `file_definition` where this run type value will be found.
`run_name`*           | `String`     |  The value from the Run Type column.
`category_code`       | `Integer`    |  The category of this run type.
`alias_to`            | `String`     |  Contains the run name for which this run type is an alias (if applicable).
`created`             | `Timestamp`  |  The time the record was created.
`modified`            | `Timestamp`  |  The time the record was last modified.

#### Category codes
Each run type will be of a specific category. This will be one of:

- The ID of a `variable`; this indicates that records with this run type should be treated as measurements for that `variable`.
- An internal calibration
- An alias to another run type
- Ignored


## `calibration`
Each calibration (external standard, sensor calibration or calculation coefficient) is stored in this table.

The `type` field indicates the type of calibration, and the `target` field specifies the individual item being calibrated (the specific gas standard or sensor).

A calibration may consist of multiple values (e.g. the equation coefficients for a sensor calibration). These values will be stored in a text field as a semi-colon separated list of values. The application code will perform the conversion during the read/write phases.

Field              | Type          | Description
:------------------|:------------- |:-------------------------------------------------------------------------
`id`*              |  `Integer`    | ID.
`instrument_id`    |  `F_KEY`      | This instrument that this calibration belongs to.
`type`             |  `String`     | The calibration type. One of `EXTERNAL_STANDARD`, `SENSOR_CALIBRATION` or `CALC_COEFFICIENT`.
`target`           |  `String`     | The target of this calibration.†
`deployment_date`  |  `DateTime`   | Time from which the calbration applies.
`coefficients`     |  `String`     | The values for the calibration. The content depends on the type of calibration and the instrument configuration. Stored as a JSON string.
`class`            |  `String`     | The Java class corresponding to this calibration.
`created`          |  `Timestamp`  | The time the record was created.
`modified`         |  `Timestamp`  | The time the record was last modified.

†The `target` of a calibration depends on the type of calibration.

Calibration Type     | Target
:--------------------|:-------------------------------------------------------------------------------
`EXTERNAL_STANDARD`  | The run type indicating the mode that the instrument is in for running that external standard.
`SENSOR_CALIBRATION` | The database ID of the `file_column` that defines the sensor.
`CALC_COEFFICIENT`   | The name of the variable coefficient, in the form `<variable_id>.<coefficient_name>`.

# Data Files
This section describes the tables used to hold data as it is processed to perform data reduction and quality control.

## `data_file`
This table holds details of the raw data files held in the system.
Sections of these files will be used to construct voyages/deployments

Field                 | Type         | Description
:---------------------|:-------------|:---------------------------------------------------
`id`*                 | `Integer`    | ID.
`file_definition_id`  | `F_KEY`      | File definition describing the format of the file.
`filename`            | `String`     | Original filename.
`start_date`          | `DateTime`   | Date/Time of the first record in the file.
`end_date`            | `DateTime`   | Date/Time of the last record in the file.
`record_count`        | `Integer`    | Number of records in the file.
`properties`          | `String`     | Specific processing requirements for the data file.
`created`             | `Timestamp`  | The time the record was created.
`modified`            | `Timestamp`  | The time the record was last modified.

Data Files will be stored on the file system of the server in a configured folder. The path to the file within that folder will be `<file_definition_id>/<data_file_id>`. It will therefore not have its original filename.

## `dataset`
This table will define a dataset. It will not contain any data, but its contents will be used to determine which data should be extracted from uploaded data files to be processed and quality controlled.

Field                    Type         | Description
:----------------------|:-------------|:-------------------------------------------------------------------------
`id`*                  | `Integer`    | ID.
`instrument_id`        | `F_KEY`      | The instrument that this dataset belongs to.
`name`                 | `String`     | The name of the dataset. Usually an EXPOCode.
`start`                | `Datetime`   | The start time of the dataset.†
`end`                  | `Datetime`   | The end time of the dataset.†
`min_longitude`        | `Double`     | The westernmost point of the dataset. Set automatically during processing.
`max_longitude`        | `Double`     | The easternmost point of the dataset. Set automatically during processing.
`min_latitude`         | `Double`     | The southernmost point of the dataset. Set automatically during processing.
`max_latitude`         | `Double`     | The northernmost point of the dataset. Set automatically during processing.
`status`               | `Integer`    | The dataset's current status.‡
`nrt`                  | `Boolean`    | Indicates whether or not this is a Near Real Time dataset.
`status_date`          | `Datetime`   | The time at which the dataset's status was last changed.
`properties`           | `String`     | Contains a copy of the instrument's configuration at the time the dataset was created. This ensures that if the instrument's configuration changes for future datasets, it will not affect older datasets.
`error_messages`       | `Text`       | Details of any unrecoverable error (usually a stack trace) that occurs while processing the dataset.
`processing_messages`  | `Text`       | Information of any issues found during dataset processing that do not prevent processing from completing. Usually invalid values that are ignored.
`user_messages`        | `Text`       | General notes provided by the user about the dataset. These will be included when the dataset is exported.
`last_touched`         | `Datetime`   | The last time that the dataset or its data were updated.
`exported`             | `Boolean`    | Indicates whether or not the dataset has ever been exported.
`created`              | `Timestamp`  | The time the record was created.
`modified`             | `Timestamp`  | The time the record was last modified.

†The start and end times for a dataset may be changed during processing: QuinCe will 'collapse' the dataset's timespan to match the range of available valid data.

‡The dataset will have several statuses during its lifecycle. These are defined in the `DataSet` class.

## `sensor_values`
This table contains every value extracted from data files for use in a dataset.

Field                    Type    | Description
:------------------|:------------|:-------------------------------------------------------------------------
`id`*              | `Integer`   | ID.
`dataset_id`       | `F_KEY`     | The ID of the dataset that this value belongs to.
`file_colum`       | `F_KEY`     | The column of the datafile that this value was loaded from.
`date`             | `Datetime`  | The time that the value was measured.
`value`            | `String`    | The value from the file, in String form. It will be converted as required during processing.
`auto_qc`          | `Text`      | The results of automatic QC performed on this value.
`user_qc_flag`     | `Integer`   | The QC flag applied to the value by the user.
`user_qc_message`  | `Text`      | The QC comment supplied by the user.

## `measurements`
Every `sensor_value` for a `sensor_type` that acts as a 'core' type for a `variable` (see `variable_sensors.core`) will trigger the creation of a `measurement`, which is the basis for the data reduction.

Field                    Type      | Description
:---------------------|:------------|:-------------------------------------------------------------------------
`id`*                 | `Integer`   | ID.
`dataset_id`          | `F_KEY`     | The ID of the dataset that this measurement belongs to.
`date`                | `Datetime`  | The time of the measurement.
`measurement_values`  | `Text`      | Details of the `sensor_values` used by this measurement in data reduction as a JSON string. The content of this is complex and beyond the scope of this document.

## `measurement_run_types`
A `measurement` will apply to one or more `variable`s, and each of those `variable`s may have a different associated run type. This table stores the details of the run types associated with each `measurement`.

Field              | Type      | Description
:------------------|:----------|:-------------------------------------------------------------------------
`measurement_id`*  | `F_KEY`   | The `measurement` ID.
`variable_id`*     | `F_KEY`   | The `variable` ID.
`run_type`         | `String`  | The run type.

## `data_reduction`
The results of the data reduction for each `measurement` and `variable` are stored in this table.

Field                 |  Type      | Description
:---------------------|:---------- |:-------------------------------------------------------------------------
`measurement_id`*     | `F_KEY`    | The `measurement` ID.
`variable_id`*        | `F_KEY`    | The `variable` ID.
`calculation_values`  | `Text`     | The results of the data reduction, including the intermediate calculations, stored as a JSON string. This content of this is complex and beyond the scope of this document.
`qc_flag`             | `Integer`  | The quality control flag for the calculations, based on the flags of the contributing `sensor_values`.
`qc_message`          | `Text`     | The quality control message(s) that apply to the calculated values, based on the quality control of the contributing `sensor_values`.


# Miscellaneous Tables
The database will contain other tables that are not directly related to the data processing.

## `job`
The majority of the data processing performed by QuinCe will be done by background tasks, which will be managed within the database. Each background task will have a row in this table.

Field          | Type         | Description
:--------------|:-------------|:---------------------------------------------------------------------------------
`job_id`*      | `Integer`    | ID.
`owner`        | `F_KEY`      | The ID of the user who created the job.
`class`        | `String`     | The fully qualified Java class name of the job.
`properties`   | `String`     | The parameters to be passed to the job when it is run, e.g. the `dataset` ID to be processed.
`status`       | `ENUM`       | The current status of the job. One of `WAITING`, `RUNNING`, `FINISHED`, `ERROR`, `KILLED`.
`started`      | `DateTime`   | The time when the job was started.
`ended`        | `DateTime`   | The time when the job completed.
`thread_name`  | `String`     | The name of the thread which is running the job.†
`progress`     | `Float`      | The progress of the job, expressed as a percentage.
`stack_trace`  | `String`     | Used to store a stack trace in the event of an error.
`created`      | `Timestamp`  | The time the record was created.
`modified`     | `Timestamp`  | The time the record was last modified.

†Jobs will be run as new threads within the system. Each thread will be named
according to the job it is running, and its name will be stored in the database
so the system can monitor whether the thread as died and left the job in a bad
state. In such situations the job will be returned to the queue to be re-run.

## `shared_instruments`
Users can share the `instrument`s they own with other `user`s, allowing the quality control duties to be distributed amongst multiple people. This table keeps records of the shared `instrument`s. Each instrument can be shared with multiple other users.

Field             |  Type     | Description
:-----------------|:----------|:------------------------
`instrument_id`*  | `F_KEY`   | The `instrument` being shared.
`shared_with`*    | `F_KEY`   | The `user` with whom the instrument is shared.
