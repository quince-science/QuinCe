---
title: Data Export for Submission to External Repositories
tblPrefix: "Table"
---

# Introduction

QuinCe exists to allow Principal Investigators (PIs) to upload, process and quality control datasets from their stations to make them ready for publication. In the workflow of the ICOS Ocean Thematic Centre (OTC), these datasets are checked by an expert in OTC who discusses any issues with the PI. Once the OTC expert is satisfied, the dataset is exported to the Carbon Portal and SOCAT for publication ([@Fig:dataset_flow]). (When data is received from non-ICOS stations the checking step will be skipped and the export destinations may be different, but the general idea will remain the same.)

![Simplified view of the flow of a a dataset from submission by the PI to final export](dataset_flow.png){#fig:dataset_flow}

The process for submitting data to the Carbon Portal is relatively complex, requiring pre-registration of a submitted data set with metadata and the acquisition of a persistent identifier (PID) before the data itself is uploaded. This system is not designed to be a manual process. Additionally, QuinCe will not handle the complete metadata for the datasets it processes, since this functionality is being developed by collaborators in other projects and institutions. Similar automated submission protocols are being developed for SOCAT to reduce the manual effort involved, and these should be used by ICOS wherever possible.

Export of datasets from QuinCe and their publication to external data repositories will therefore be performed by an external script that communicates with QuinCe, and the publication systems at the Carbon Portal, SOCAT etc. This script will interrogate QuinCe to determine which data sets are ready for export, retrieve the necessary files, construct the required metdata documents and submit them to the appropriate locations ([@Fig:simple_script_flow]). The script will communicate the results of the export back to QuinCe so that the PI can track the progress and see when exporting is complete. At this point the dataset may be left alone, or archived in future versions on QuinCe[^archive].

[^archive]:Archiving a dataset will hide it from the default views in QuinCe to prevent cluttering when many datasets have been submitted. The exact mechanism for this is not yet determined, but all information regarding a dataset will always be fully retrievable.

![Basic overview of export script operations](simple_script_flow.png){#fig:simple_script_flow}

# QuinCe

This section describes the features that will be added to QuinCe to support the export functionality.

## Dataset status

While a dataset is being processed, it is given various statuses to indicate progress through the automatic and manual processes. These help the user to understand what is happening to the dataset. Alongside the status is a section showing the actions that can be performed on that dataset ([@Fig:status_and_actions]). These change as the dataset is processed according to its current state.

![Dataset Status and Actions](StatusActions.png "The dataset status and available actions displayed in QuinCe"){#fig:status_and_actions}

The possible satuses for a dataset, and the actions that will be available on a dataset in each of those statuses, are listed in [@Tbl:statuses].

-------------------------------------------------------------------------------
Status            Meaning                              Available actions
-------           ------------------------------------ ------------------------
Waiting           Waiting to be processed by           None
                  background jobs
  
Data extraction   Data being extracted from raw files  None
  
Data reduction    Data reduction under way             None
  
Automatic QC      Automatic QC under way               None
  
Ready for QC      Automatic processing complete;       Manual QC
                  Manual QC required
  
QC Complete       All QC activities complete           Manual QC,
                                                       Manual export,
                                                       Submit for checking
  
Expert            Waiting for expert check             Manual QC,
check                                                  Manual export,
                                                       Expert check
  
Ready for         Dataset ready for automated export   Manual QC,
export                                                 Manual export

Exporting         Automatic export and publishing      None
                  under way
  
Exported          Dataset has been exported            Manual QC,
                                                       Manual export,
                                                       Archive
  
ERROR             Error encountered during             None[^errorstatus]
                  any automatic processing
-------------------------------------------------------------------------------
Table: Possible status values for datasets {#tbl:statuses}

[^errorstatus]:If any automatic processing jobs fail, the dataset's status will be set to 'ERROR'. The cause and remedial action will need to be determined by the QuinCe support team, who will be notified of the problem automatically.

The station PI and OTC expert will be able to perform various actions on a dataset at different times, depending on its current status. [@Tbl:actions] describes the possible actions.

-------------------------------------------------------------------------------
Action            Description
---------         -------------------------------------------------------------
Manual QC         The user can examine the dataset and manually set QC flags
  
Manual export     The user can export the dataset for examination offline and
                  to allow them to submit datasets to repositories that are not
                  supported by the automated export script. The PI will be
                  responsible for ensuring that the dataset is in a suitable
                  state for publication.
  
Submit for        (ICOS stations) Once manual QC is complete and all required
checking          flags have been assigned, the PI must explicitly trigger an
                  action to submit the dataset to the OTC expert for checking.
  
Automatic export  (Non-ICOS stations) Since the expert check is not required,
                  the user will be given a separate action to signal that the
                  dataset is ready to be published to data repositories through
                  the automatic channels.

Expert check      This action will only be available to OTC experts. When a
                  dataset is submitted to the expert, they will be able to
                  approve or reject the dataset. If the dataset is approved it
                  will be changed to the 'Ready for export' state; otherwise
                  it will be returned to the 'Ready for QC' state and returned
                  to the PI.

Archive           Hides the dataset from the main QuinCe interface.
-------------------------------------------------------------------------------
Table: Actions that can be performed on datasets {#tbl:actions}

## Export API

The QuinCe API will provide three functions related to exporting of data, matching the three communications between the export script and QuinCe shown in [@Fig:simple_script_flow]. Each of these calls must have the necessary authentication tokens to identify the script to QuinCe to ensure that the application is secure, and that the identity being used by the script has the necessary permissions to export datasets; these will not be discussed in this document. Similarly, the various error situations that can occur (e.g. requesting export of a dataset that has not passed quality control) will not be discussed here.

### `exportList` {#export_list}

__URL:__ `QuinCe/api/export/exportList`

__HTTP Method:__ GET

__Return:__ JSON

__Parameters:__ None

This call will retrieve a list of all the datasets that are ready to be exported. The datasets will be identified by their database ID, but extra information will be returned to allow the export script to decide how to process each dataset and to produce meaningful log messages as it runs. The dataset details will be returned as a JSON array of objects in the following form:

```json
[
  {
    "id": 34,
    "name": "GVNA20180703",
    "nrt": false,
    "instrument": {
      "name": "Black Pig",
      "owner": "Horatio Pugwash"
    }
  },
  {
    "id": 42,
    "name": "NRTPESC1531481725",
    "nrt": true,
    "instrument": {
      "name": "Not The Pescod",
      "owner": "Captain Birdseye"
    }
  }
]
```

The `id` is the unique identifier for the dataset, which can be used by subsequent API calls to perform operations on individual datasets. `nrt` indicates whether or not a dataset contains near-real-time data, which may need to be processed in a different manner to those that have been fully quality controlled.

The call will only provide details of datasets whose status is 'Ready to export'. Complete, quality controlled datasets will be set to this status once manual quality control is complete and (optionally) the dataset has passed inspection by an OTC expert. Near-real-time datasets will automatically have their status set to 'Ready to export' as soon as they are created.


### `exportDataset`

__URL:__ `QuinCe/api/export/exportDataset`

__HTTP Method:__ GET

__Return:__ ZIP archive

__Parameters:__

-------------------------------------------------------------------------------
Parameter      Description
-------------- ----------------------------------------------------------------
`id`           The dataset ID

`includeRaw`   Indicates whether the raw data files are required
-------------------------------------------------------------------------------

This call will get everything required to export a dataset and publish it to an external repository, including all files and metadata information. The result will be a ZIP archive containing all requested data files plus a JSON file containing metadata information. The dataset is selected using its ID, as returned by the [`exportList`](#export_list) call. QuinCe will be able to export datasets in a variety of formats, since different repositories may require different formats. Setting `includeRaw` to `true` will include the raw data files used to create the dataset in the output.

The ZIP archive will be named as the dataset name with a `.zip` extension, and will be structured as follows:

```
GVNA20180703.zip
+-- manifest.json
+-- dataset
|   +-- CarbonPortal
|   |   +-- GVNA20180703.csv
|   +-- SOCAT
|       +-- GVNA20180703.tsv
+-- raw
    +-- 20150117.cnv
    +-- 20150118.cnv
    +-- GO175_2015-103-0000dat.txt
```

The `dataset` folder will contain one or more copies of the publication-ready dataset. The definition of each instrument in QuinCe will contain a list of the publication destinations for its datasets (Carbon Portal, SOCAT etc.). There will be one sub-folder for each of those destinations, containing the dataset file in the format required by that destination. The filename will be the dataset name and an extension suitable for the format (`.tsv`, `.csv` etc.).

If `includeRaw` is `true` , the `raw` folder will contain all the raw files used to construct the dataset. These will have their original filenames as they were uploaded to QuinCe. If `includeRaw` is `false`, this folder will not be included in the archive.

`manifest.json` will contain a JSON object containing details of the files included in the archive together. It will also contain dataset-specific metadata that the export script can use to build the metadata documents required by the different publication destinations. The manifest will be formatted as follows:

```json
{
  "manifest": {
    "raw": ["20150117.cnv", "20150118.cnv", "GO175_2015-103-0000dat.txt"],
    "dataset": [
      {
        "destination": "CarbonPortal",
        "filename": "GVNA20180703.csv"
      },
      {
        "destination": "SOCAT",
        "filename": "GVNA20180703.tsv"
      }
    ]
  },
  "metadata": {
    "name": "GVNA20180703",
    "nrt": false,
    "startdate": "2018-07-03T12:03:34Z",
    "enddate": "2018-07-10T03:12:56Z",
    "records": 722,
    "bounds": {
      "north": 68.764,
      "south": 52.455,
      "east": 5.432,
      "west": -70.345
    },
    "quince_information": "Processed by QuinCe version 1.0.0"
  }
}
```

When this call is made and the data files are returned, the status of the dataset in QuinCe will be set to 'Exporting', so multiple concurrent export requests are not made. The intention is that [`completeExport`](#complete_export) is called by the export script once it has finished processing the dataset. However, if that call is not made within a given time period (e.g. 30 minutes), QuinCe will assume that the export process failed and set the dataset's status back to 'Ready to export' so it is freed to be processed again.

### `completeExport` {#complete_export}

__URL:__ `QuinCe/api/export/completeExport`

__HTTP Method:__ GET

__Return:__ JSON

__Parameters:__

-------------------------------------------------------------------------------
Parameter      Description
-------------- ----------------------------------------------------------------
`id`           The dataset ID
-------------------------------------------------------------------------------

This call will simply set the status of the specified dataset to 'Exported', to indicate to QuinCe and its users that it has been published. It will return a simple JSON object that indicates whether or not the change of status was successful:

```json
{
  "succeeded": "true"
}
```

### `abandonExport`

__URL:__ `QuinCe/api/export/abandonExport`

__HTTP Method:__ GET

__Return:__ JSON

__Parameters:__

-------------------------------------------------------------------------------
Parameter      Description
-------------- ----------------------------------------------------------------
`id`           The dataset ID
-------------------------------------------------------------------------------

This call can be used if the export script retrieves a dataset for publication, but is unable to complete the publication process. It will reset the dataset's status to 'Ready to export', so it can be processed in a subsequent call. The call will return a simple JSON object that indicates whether or not the change of status was successful:

```json
{
  "succeeded": "true"
}
```
