% Near Real Time Data Processing

# Introduction

The primary purpose of QuinCe is to generate datasets from ocean-borne measuring instruments that have been fully processed and had first level quality control applied by the user. These datasets will be ready for publishing in data centres (e.g. the ICOS Carbon Portal) and synthesised databases (e.g. SOCAT). The primary focus of QuinCe is therefore in delayed-mode data processing, where measurements are submitted after being retrieved from the measuring platform and organised into logical datasets (representing a crossing, deployment etc.).

Some stations have the ability to trasmit collected measurements on a frequent basis (e.g. hourly or daily), and there is a demand for this data to be processed and made available as quickly as possible: having been automatically processed, but without the detailed first level quality control. This data will serve two main purposes:

* Identification of possible issues with the instrument at the earliest opportunity

* To provide an early look at the data, to expedite access to knowledge and also to identify possible interesting events that warrant closer study.

A new feature will be added to QuinCe whereby data files sent directly from instruments can be uploaded automatically. These will be combined into a special 'Near Real Time' data set that will have data reduction and automatic quality control applied. The results of this processing will then be exported to interested parties in the appropriate format. Data will remain in the 'Near Real Time' data set until the user adds it to a conventional data set for complete processing and quality control, at which point it will be removed since the fully processed data becomes the definitive version.

# Overview

NRT data transmitted from stations will arrive either by email, uploaded to an FTP server, or placed in some other location that can be read by the QuinCe system. The QuinCe application itself will not read these files; a separate script will be written that locates new files, submits them to QuinCe via an API, and then archives the files according to whether or not they were uploaded successfully. The QuinCe application will be responsible for receiving the uploaded files and reporting whether or not they can be successfully processed. Failed files will be stored for later examination by the user (successfully uploaded files will be stored within QuinCe). Messages regarding the results of the upload can be stored in a log file or emailed to interested parties. Meanwhile QuinCe will process the data to create and publish the NRT dataset.

```{.mermaid width=1000}
sequenceDiagram
  participant ID as Incoming Data
  participant AS as Acquisition Script
  participant Q as QuinCe
  participant FS as File Store
  participant EX as External
  ID->>AS: Acquire files
  AS->>Q: Upload
  Q->Q: Check files
  Q->>AS: Send check result
  alt Check failed
    AS->>FS: Store in Failure archive
  end
  AS->>EX: Send file processing results
  Q->>EX: Generate and publish NRT dataset
```

## Data acquisition

The data acquisition script will be a standalone tool that acquires data files and submits them to QuinCe.

### Acquiring files

NRT data from stations can arrive in a variety of ways: sent by email, uploaded to an FTP server, etc. The data acquisition script will retrieve files from the required source on a regular schedule defined on a per-station basis. This will typically be daily, at a time shortly after the files are expected to be sent.

When a new file is detected, the script must ensure that it is completely uploaded before attempting to process it[^email_complete]. Therefore script will not download new files immediately; instead it will record the file's size and check again after a few minutes. If the file size has changed then the file is still being uploaded. The script will only download a file once its size has been stable for the required period.

[^email_complete]: This does not apply to emails because the email server will only make an email available once it is complete.

The script will need to check that retrieved files have not already been uploaded to QuinCe. To this end, it will keep a log of all files it has uploaded with their hashes, and compare new files against this list. Files that have new data appended to them will not be detected by this approach, which means that they will be uploaded for the added records to be processed.

### Uploading files

The script will upload acquired files to QuinCe for processing, using a new API (described later in this document). QuinCe will perform some quick checks to ensure that the file can be processed, and will return a JSON string detailing the result of the checks and any message that should be passed to the user.

It is possible that an uploaded file can be partially processed. In that case the status of the uploaded file will be `true` and the `message` field will describe the issues that prevented the full file from being processed.

If the file checks have failed, the script will store that file in a specified location for later review by the user.

Once a file has been processed (either successfully or not), the script will optionally remove it from the source location, depending on the needs of others who may want to access those same files.

## QuinCe API

The API for uploading NRT data files will consist of a single command and response. The upload command will consist of an HTTP POST request with the following parameters:

* The database ID of the instrument for which files are being uploaded
* A key associated with the instrument (for security)
* The data files with their filenames

QuinCe will perform some initial checks on the files (described below), and then respond with a JSON object with the following format:

```json
{
  "instrument_id" : 7,
  "files": [
  	{
  	  "filename": "file1.csv",
  	  "ok": true,
  	  "messages": []
  	}
  	{
  	  "filename": "file2.csv",
  	  "ok": false,
  	  "messages": [
        {
          "line": 22,
          "message": "Invalid date"
        },
        {
          "line": 43,
          "message": "Incorrect number of columns"
        }
  	  ]
  	}
  ]
}
```

### Replacing or updating existing files

* Uploading overlapping files
  * Already in dataset
  * Appended
  * Overlaps differ from existing (how to check?)

### File checks

When a file is manually uploaded through the web-based interface, QuinCe performs a number of checks to ensure that the file can be processed properly. If any of these checks fail, the user will be informed and asked to upload a corrected version of the file. However, the purpose of NRT data production is to process the data as quickly as possible with no human intervention, so this is not a practical approach. QuinCe will run the same set of checks as for manually uploaded data, but it will not reject a file unless it is completely impossible to process it. Any found issues will be returned in the messages returned from the API call.

-------------------------------------------------------------------------------
Check                                 Handling
----------------------------------  -------------------------------------------
File format cannot be recognised    Reject file

File empty (either zero bytes or    Ignore file (do not reject)
has header but no data)

Cannot parse date of first line     Ignore line

Cannot parse dates on other line    Ignore line

Cannot parse any dates              Reject file

Non-monotonic date                  Ignore line

Incorrect number of columns         Ignore line
-------------------------------------------------------------------------------
Table: Checks performed on uploaded data files and associated actions

All other problems in the file will be detected during data extraction or automatic quality control.

## Generating the NRT dataset

Once a set of files has been uploaded, QuinCe will automatically create a new NRT dataset. This will be the same as a standard dataset, but with a flag to indicate its special status. If an NRT dataset already exists, it will be deleted. The new NRT data set will be generated using data from the latest user-defined dataset covering all available data from that point onwards. Once created, the dataset will be processed as any other, going through data extraction, data reduction and automatic quality control. Any lines that fail the [file checks] will be ignored during data extraction.

## Exporting NRT datasets

Once the NRT dataset has been created, it will be automatically exported and published in an appropriate location (e.g. the ICOS Carbon Portal) along with all available metadata (and the original raw files if required).

In the first development iteration the export and publication will be performed by an external script (using APIs developed in QuinCe). This script is being designed at the time of writing, and support for NRT datasets will be incorporated.

## Viewing NRT datasets

QuinCe users will be able to view NRT datasets, together with the results of the automatic quality control, in two ways. The NRT dataset will be available for viewing in QuinCe in the same manner as any other data set. The dataset will not be editable (quality control flags cannot be set manually), but all plots, maps and values will be viewable. The user will also be able to export the NRT dataset if they wish to perform some quick analysis on the data before it is fully quality controlled and ready for publication.

An optional extra function will allow the results of the automatic quality control to be sent to the user as soon as it is complete. This will help to provide an 'early warning system' of potential issues with an instrument. The exact nature of what is sent, and how frequently, may need to be customised to prevent excessive or low-value messages being received.

## Building a 'real' dataset

* Re-upload files with issues
