% QuinCe Overview

# Introduction
This document provides an overview of how QuinCe operates. Its purpose is to provide a guide for coders explaining how to navigate the code and database setup to find the relevant functionality. It describes the various steps of creating instruments, uploading data, creating and processing datasets, and exporting data. This is by no means an exhaustive document - it is intended to provide hints and initial guidance as to where things can be found in the database and the code (mostly through the names of conceptual items) when you are working on specific parts of the application.

# Basic Code Structure
QuinCe is a web application build on the Jakarta Server Faces (JSF) framework, typically running in Tomcat with a MySQL database. From the Git repository there are a number of relevant folders.

## Important Folders
The central folder is `WebApp`, which contains the main application. Within this folder are:

- **`src`** - The Java code
- **`WebContent`** - The front end HTML, CSS, Javascript etc.
- **`junit`** - The unit tests for the application

Other important folders include:

- **`DataPreparation`** - Various standalone scripts and programs for pre-processing data before it is ingested into QuinCe. Although we aim to handle any incoming data format, this is not always a practical goal.
- **`external_scripts`** - Scripts that handle data processing around the QuinCe software, e.g. collecting data for ingestion from external sources and exporting processed data for publication.
- **`src`** - Not to be confused with `WebApp/src`, this contains database migrations and external code libraries.
- **`configuration`** - All the configuration files for the application are stored here.

### WebApp

#### src
The application's source code is all developed in the Java package `uk.ac.exeter.QuinCe`. This is an artifact of the geographic location where QuinCe development first started. Beneath this, the package structure is divided into several main parts:

- **`data`** - This contains all classes representing the data that QuinCe handles. The sub-packages reperesent logical entities within the application, e.g. `Instrument`, `DataSet` etc. Each package will typically contain classes for those entities, and a static `DB` class (e.g. `InstrumentDB`) that handles database storage and retrieval for them. Many classes are direct analogues of tables in the underlying database.
- **`web`** - These classes contain the JSF Beans and other classes relating to the web user interface of the application. They are organised in accordance with the various areas of the user interface, which often (but not always) mirror the structure of the `data` classes.
- **`job`** - Much of the data processing in QuinCe is performed using independent threads running specific jobs. Each dataset is processed by a sequence of jobs (e.g. extraction, quality control, data reduction etc.). There are also some utility jobs for miscuellaneous housekeeping tasks.
- **`api`** QuinCe has a basic API that allows external scripts to perform some functions such as uploading new files and exporting processed data.

#### WebContent
This folder contains the `xhtml` pages for the application, organised into folders based on the different areas of the user interface. As with all JSF applications, the progression of pages is controlled by `WEB-INF/faces-config.xml`.

The `resources` folder contains `image` (for static visual assets), `script` (Javascript files) and `style` (CSS). `script` contains copies of all external Javascript libraries as well as code specific to QuinCe pages. This should be converted to some kind of package management at some point, but it's a low priority. The other folders in `resources` (`export_postprocess` and `python`) are artifacts of poor code organisation in previous versions of QuinCe and should be ignored.

#### junit
This folder contains the unit tests for the code in `src`, and is structured to mirror that folder exactly. Writing tests is a lower priority than it ideally should be.

# Database Migrations
QuinCe uses the gradle build system for package management and building the application. It will not be covered in detail here, except for the Flyway database migration system which is used to construct the database.

The QuinCe database is built using a sequence of migration files that create the required tables and populate them as needed. As the application evolves, new migrations will adjust the database as required. Running the complete set of migrations on an empty database will set up that database so it is ready to be used.

Migration files are either simple text files containing a sequence of SQL statements, or Java classes that perform more complex operations (e.g. data conversions). The main migration files are stored in `src/migrations/db_migrations`. The JUnit tests use an in-memory H2 database for speed, which unfortunately has a slightly different syntax to MySQL. Consequently, a mirrored set of the database migrations is maintained in `WebApp/junit/resources/db_migrations`.

# The Application
This section describes the various functions of the application, and where the relevant pieces of code and database tables entries can be found. This is not a manual for the application, nor is it an exhaustive description of how the code is written - it is simply a guide to help you find the most likely places to start looking for how any given piece of the application works.

## Bootstrapping

### Database
Three tables in the database are populated independently of the running application, containing details of the types of sensors and measured variables that QuinCe can work with.

- **`sensor_types`** contains the different types of sensor that can be defined as part of an instrument. These will be Sea Surface Temperature, Salinity, Atmospheric Pressure, etc.
- **`variables`** contains the variables that QuinCe knows how to calculate - Underway Marine pCO~2~, Contros pH, etc. Each of these variables will have a corresponding `DataReducer` in the code to perform the required calculations (described later).
- **`variable_sensors`** lists which sensors are *required* for a given variable to be calculated.

These tables are populated through the database migration system, and cannot be edited via the application itself.

Note the difference between these bootstrapped database tables managed through database migrations, and the configuration files in the `configuration` folder. The database tables are intended to be relatively static across multiple deployments of QuinCe and rarely change, while entries in configuration files may change more frequently, either between different deployments or to make fine-grained adjustments to system parameters.

### Java
When the application starts, a special `ResourceManager` object is created. This is a singleton class accessed through `ResourceManager.getInstance()` and provides access to all aspects of the application configuration that are not linked to any specific entities (instruments, users etc.) in the system. It also provides access to a `DataSource` through which database connections can be obtained.

## Instruments
The instrument is the central entity that users will work with in the application, and the first thing a new user must define. The configuration of an instrument is stored across a number of database tables reflecting its various component parts (from an application standpoint). However, when you retrieve an `Instrument` object from the database, it will contain all these parts so multiple requests for them are not required.

The components of an Instrument in QuinCe are as follows:

- **Instrument** (`instrument`): The basic details of the instrument that are not specifically related to the measurements taken. Includes the name and platform code, but also information such as the instrument's flushing times when switching modes, or the position for a fixed instrument.
- **Variables** (`instrument_variables`): The variables that the instrument measures. This will be used to determine which sensors have to be identified as part of the instrument, and which calculations are performed when data is processed.
- **File Definition** (`file_definition`, `file_column` and `run_type`): When an instrument is configured, QuinCe must be told about the format of the incoming data files. The general details, such as separators, date/time and position formats etc., are stored in the `file_definition` table. The mapping of individual columns to specific sensor types is stored in `file_column`. In the code, this mapping is encompassed by the `SensorAssignments` object. Where an instrument has different Run Types (for different operation modes of the insturment), these are stored in the `run_type` table.

*Note:* An instrument may produce data in more than one file (e.g. one file from the CO~2~ measuring system, and one from a separate thermosalinograph). Thus an instrument may have multiple file definitions.

### Ownership
Each instrument is owned by one user. At the time of writing, only an instrument's owner can see it (apart from administrators who can see everything).

## Calibrations
There are two types of calibration possible for an instrument in QuinCe:

- **Sensor Calibrations** - used to adjust recorded sensor values. These are applied as values are read from the data files.
- **Internal Calibrations** (*aka* External Standards or Gas Standards)[^externalstandards] - measurements taken from sources of known values (usually gas bottles) which are then used to calibrate recorded values. These are applied during data reduction.

Each of these calibrations are implemented for a specific date - either the date that the sensor was calibrated and the adjustment coefficients calculated, or the date that the external standard was installed. Both types of calibration are stored in the `calibration` table.

[^externalstandards]: External Standards or Gas Standards are the common names for these calibration types. In QuinCe they are termed Internal Calibrations because they are calculated and applied inside the application.

## Files
Once an instrument has been created, the user must upload files containing the data to be processed. The details of each file are stored in the `data_file` table, which caches basic metdata about the file, but not its contents.

The file itself is stored in a directory known as the File Store. This has the following structure:

    FILE_STORE
     |
     |- <file_definition id>
     |    |
     |    |- <data_file id>
     |    |- <data_file id>
     |
     |- <file_definition id>
          |
          |- <data_file id>
          |- <data_file id>

Within the `FILE_STORE`, each sub-directory is the database ID of a record in the `file_definition` table. **Note that, since an instrument can have more than one file definition, the `file_definition` ID *is not necessarily* the same as the `instrument` ID.**

The uploaded files are stored within each file definition folder. They are not given their original filenames. Instead, they are named with the database ID of the corresponding record in the `data_file` table in the database.

Note that when a file is uploaded, QuinCe does not read data from the file into the database. This is only done when datasets are being processed.

## Datasets
When the user has uploaded some data files, they can define a new dataset. Each dataset is defined as a time range, and thus is independent of the uploaded data files; QuinCe will choose which data to load from those files to make up the dataset. Basic details of the dataset are stored in the `dataset` table. The actual data will be stored in other tables that link to the `dataset` record.

As soon as a dataset is created, a sequence of background jobs is started. Each job in the sequence is scheduled on its own, and when it is finished it automatically schedules the next job in the chain. The jobs are as follows:

- `ExtractDataSetJob` - loads data from the data files into the database
- `AutoQCJob` - performs automatic quality control on the loaded data
- `LocateMeasurementsJob` - locates the variable measurements in the data
- `DataReductionJob` - performs data reduction for all variables registered to the instrument

### `ExtractDataSetJob`
This job retrieves all the configured data values from the data files that fall within the time period encompassed by the dataset. Each value is stored individually as a record in the `sensor_values` table, since it can be given a quality control flag that is independent of all other values. The `sensor_values` table will therefore contain several million records, although each is very small. Where required, sensor calibrations are applied to these values as they read into the database.

### `AutoQCJob`
This job performs automatic quality control routines on all the extracted sensor values. The quality control routines are configured in `configuration/qc_routines_config.csv`. All the sensor values from a given file column are collected into a time series and passed to each QC routine in turn. This is because some routines, such as spike detectors or stuck value checks, require the full time series to work.

The results of the automatic QC checks are stored in the `sensor_values.auto_qc` field as a JSON string, containing the results of all Bad or Questionable flags applied from all the QC routines and which routine(s) caused the flag to be set.

### `LocateMeasurementsJob`
Sensor values from incoming files can be recorded at any time, and not all sensors are necessarily measured at the same time. QuinCe must therefore decide which times correspond to measurements that require data reduction calculations. Remember that the `variable_sensors` specifies which sensor types are required for a given variable. For each variable, one of those types is designated the core sensor. QuinCe identifies measurements by finding the times at which values from those core sensors are present in the dataset.

Found measurements are stored in the `measurements` table, along with the current Run Type of the instrument which will inform the data reduction routines how those measurements should be treated.

### `DataReductionJob`
This job performs the data reduction for each of the instrument's variables. It selects the relevant measurements from the `measurements` table, and determines which values from the `sensor_values` table should be used as the inputs for the calculation. This decision is based on a number of factors, including interpolating data where no value is available at the exact time of the measurement, perhaps excluding values flagged Bad during quality control, or applying offsets to account for time delays in the physical measuring system.

Once all the sensor values have been chosen, they are linked to the `measurement` record using the `measurement_values` table. This allows the same sensor value to be used in multiple measurements if required. Each entry in the  `measurement_values` table contains `prior` and `post` fields to indicate when two sensor values should be interpolated.

The results of a data reduction calculation are stored in the `data_reduction` table. The calculated values are stored as a JSON string, allowing all intermediate calculations to be stored for complete data provenance. The quality control flags for the data reduction record are calculated from the quality control flags on the sensor values. The `questionable_cascade` and `bad_cascade` fields in the `variable_sensors` table define how a flag on a sensor value influences the flag placed on the data reduction record. The data reduction record is given the worst flag from the input sensor values.

The code for data reduction is stored in child classes of the `DataReducer` class. There is one `DataReducer` for each variable, which performs the calculations and also provides details of all the calculated values to the rest of the application (this is important displaying on QC pages and in export files).

# Properties
Instruments, variables and datasets all have properties. The properties for instruments and variables are set when the instrument is set up. Each time a dataset is created, those properties are copied into the dataset's properties so that future changes at the instrument level do not affect existing datasets (e.g. changing the fixed position of an instrument will not affect the position recorded for older datasets).