package uk.ac.exeter.QuinCe.web.datasets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.SensorCalibrationDB;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;
import uk.ac.exeter.QuinCe.jobs.files.DataReductionJob;
import uk.ac.exeter.QuinCe.jobs.files.ExtractDataSetJob;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Bean for handling the creation and management of data sets
 * @author Steve Jones
 */
@ManagedBean
@SessionScoped
public class DataSetsBean extends BaseManagedBean {

  /**
   * Navigation string for the New Dataset page
   */
  private static final String NAV_NEW_DATASET = "new_dataset";

  /**
   * Navigation string for the datasets list
   */
  private static final String NAV_DATASET_LIST = "dataset_list";

  /**
   * The data sets for the current instrument
   */
  private List<DataSet> dataSets;

  /**
   * The file definitions for the current instrument in JSON format for the timeline
   */
  private String fileDefinitionsJson;

  /**
   * The data sets and data files for the current instrument in JSON format
   */
  private String timelineEntriesJson;

  /**
   * The data set being created
   */
  private DataSet newDataSet;

  /**
   * The ID of the data set being processed
   */
  private long datasetId;

  /**
   * Plaform code of the instrument for this dataset
   */
  private String platformCode = null;

  /**
   * Says whether the dataset being defined has valid calibrations,
   * both for sensors and external standards. This
   * defaults to true, but is actually checked when the form is submitted.
   */
  private boolean validCalibration = true;

  /**
   * The message to be displayed if any calibrations
   * are invalid
   */
  private String validCalibrationMessage = null;

/**
   * Initialise/Reset the bean
   */
  @PostConstruct
  public void initialise() {
    try {
      initialiseInstruments();
      loadDataSets();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Start the dataset definition procedure
   * @return The navigation to the dataset definition page
   */
  public String startNewDataset() {
    initialise();
    newDataSet = new DataSet(getCurrentInstrument().getDatabaseId());
    fileDefinitionsJson = null;
    timelineEntriesJson = null;
    validCalibration = true;
    validCalibrationMessage = null;

    return NAV_NEW_DATASET;
  }

  /**
   * Navigate to the datasets list
   * @return The navigation string
   */
  public String goToList() {
    return NAV_DATASET_LIST;
  }

  /**
   * Get the data sets for the current instrument
   * @return The data sets
   */
  public List<DataSet> getDataSets() {
    try {
      loadDataSets();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return dataSets;
  }

  /**
   * Load the list of data sets for the instrument from the database
   * @throws ResourceException If the app resources cannot be accessed
   * @throws InstrumentException If the instrument data is invalid
   * @throws RecordNotFoundException If the instrument cannot be found
   * @throws MissingParamException If any internal calls have missing parameters
   */
  private void loadDataSets() throws MissingParamException, DatabaseException, RecordNotFoundException, InstrumentException, ResourceException {
    if (null != getCurrentInstrument()) {
      dataSets = DataSetDB.getDataSets(getDataSource(), getCurrentInstrument().getDatabaseId());
    } else {
      dataSets = null;
    }
  }

  /**
   * Get the data files for the current instrument in JSON format
   * for the timeline
   * @return The data files JSON
   */
  public String getTimelineEntriesJson() {
    if (null == timelineEntriesJson) {
      buildTimelineJson();
    }

    return timelineEntriesJson;
  }

  /**
   * Get the file definitions for the current instrument in JSON format
   * for the timeline
   * @return The file definitions JSON
   */
  public String getFileDefinitionsJson() {
    if (null == timelineEntriesJson) {
      buildTimelineJson();
    }

    return fileDefinitionsJson;
  }

  /**
   * Build the timeline JSON string for the data files
   */
  private void buildTimelineJson() {
    try {
      // Make the list of file definitions
      Map<String, Integer> definitionIds = new HashMap<String, Integer>();

      StringBuilder fdJson = new StringBuilder();

      fdJson.append('[');

      // Add a fake definition for the data sets, so they can be seen on the timeline
      fdJson.append("{\"id\":-1000,\"content\":\"File Type:\",\"order\":-1000},");

      for (int i = 0; i < getCurrentInstrument().getFileDefinitions().size(); i++) {
        FileDefinition definition = getCurrentInstrument().getFileDefinitions().get(i);

        // Store the definition number for use when building the files JSON below
        definitionIds.put(definition.getFileDescription(), i);

        fdJson.append('{');
        fdJson.append("\"id\":");
        fdJson.append(i);
        fdJson.append(",\"content\":\"");
        fdJson.append(definition.getFileDescription());
        fdJson.append("\",\"order\":");
        fdJson.append(i);
        fdJson.append('}');

        if (i < getCurrentInstrument().getFileDefinitions().size() - 1) {
          fdJson.append(',');
        }
      }

      fdJson.append(']');

      fileDefinitionsJson = fdJson.toString();

      // Now the actual files
      List<DataFile> dataFiles = DataFileDB.getFiles(getDataSource(), getAppConfig(), getCurrentInstrument().getDatabaseId());

      StringBuilder entriesJson = new StringBuilder();
      entriesJson.append('[');

      for (int i = 0; i < dataFiles.size(); i++) {
        DataFile file = dataFiles.get(i);

        entriesJson.append('{');
        entriesJson.append("\"type\":\"range\", \"group\":");
        entriesJson.append(definitionIds.get(file.getFileDefinition().getFileDescription()));
        entriesJson.append(",\"start\":\"");
        entriesJson.append(DateTimeUtils.toIsoDate(file.getStartDate()));
        entriesJson.append("\",\"end\":\"");
        entriesJson.append(DateTimeUtils.toIsoDate(file.getEndDate()));
        entriesJson.append("\",\"content\":\"");
        entriesJson.append(file.getFilename());
        entriesJson.append("\",\"title\":\"");
        entriesJson.append(file.getFilename());
        entriesJson.append("\"}");

        if (i < dataFiles.size() - 1) {
          entriesJson.append(',');
        }
      }

      if (dataSets.size() > 0) {
        entriesJson.append(',');

        for (int i = 0; i < dataSets.size(); i++) {
          DataSet dataSet = dataSets.get(i);

          entriesJson.append('{');
          entriesJson.append("\"type\":\"background\",");
          entriesJson.append("\"start\":\"");
          entriesJson.append(DateTimeUtils.toIsoDate(dataSet.getStart()));
          entriesJson.append("\",\"end\":\"");
          entriesJson.append(DateTimeUtils.toIsoDate(dataSet.getEnd()));
          entriesJson.append("\",\"content\":\"");
          entriesJson.append(dataSet.getName());
          entriesJson.append("\",\"title\":\"");
          entriesJson.append(dataSet.getName());
          entriesJson.append("\",\"className\":\"");
          if (dataSet.isNrt()) {
            entriesJson.append("timelineNrtDataSet");
          } else {
            entriesJson.append("timelineDataSet");
          }
          entriesJson.append("\"}");

          if (i < dataSets.size() - 1) {
            entriesJson.append(',');
          }
        }
      }

      entriesJson.append(']');

      timelineEntriesJson = entriesJson.toString();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Get the new data set
   * @return The new data set
   */
  public DataSet getNewDataSet() {
    return newDataSet;
  }

  /**
   * Get the names of all data sets for the instrument as a JSON string
   * @return The data set names
   */
  public String getDataSetNamesJson() {
    StringBuilder json = new StringBuilder();

    json.append('[');

    for (int i = 0; i < dataSets.size(); i++) {
      json.append('"');
      json.append(dataSets.get(i).getName());
      json.append('"');

      if (i < dataSets.size() - 1) {
        json.append(',');
      }
    }

    json.append(']');

    return json.toString();
  }

  /**
   * Store the newly defined data set
   * @return Navigation to the data set list
   */
  public String addDataSet() {

    try {
      // Delete any existing NRT dataset. The odds are that the new
      // dataset will replace it
      DataSetDB.deleteNrtDataSet(getDataSource(), newDataSet.getInstrumentId());

      DataSetDB.addDataSet(getDataSource(), newDataSet);

      Map<String, String> params = new HashMap<String, String>();
      params.put(ExtractDataSetJob.ID_PARAM, String.valueOf(newDataSet.getId()));

      JobManager.addJob(getDataSource(), getUser(), ExtractDataSetJob.class.getCanonicalName(), params);

      loadDataSets();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return NAV_DATASET_LIST;
  }

  /**
   * @return the platformCode
   */
  public String getPlatformCode() {
    if (platformCode == null) {
      if (newDataSet == null) {
        return null;
      }
      ResourceManager rm = ResourceManager.getInstance();
      try {
        Instrument i = InstrumentDB.getInstrument(
          rm.getDBDataSource(),
          getNewDataSet().getInstrumentId(),
          rm.getSensorsConfiguration(),
          rm.getRunTypeCategoryConfiguration()
        );
        platformCode = i.getPlatformCode();
        return platformCode;
      } catch (MissingParamException | DatabaseException | RecordNotFoundException | InstrumentException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return platformCode;
  }

  /**
   * @param platformCode the platformCode to set
   */
  public void setPlatformCode(String platformCode) {
    this.platformCode = platformCode;
  }

  /**
   * Get the dataset ID
   * @return The dataset ID
   */
  public long getDatasetId() {
    return datasetId;
  }

  /**
   * Set the dataset ID
   * @param datasetId The dataset ID
   */
  public void setDatasetId(long datasetId) {
    this.datasetId = datasetId;
  }

  /**
   * Submit the data set for approval
   */
  public void submitForApproval() {
    try {
      DataSetDB.setDatasetStatus(getDataSource(), datasetId, DataSet.STATUS_WAITING_FOR_APPROVAL);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Approve the data set for export
   */
  public void approve() {
    try {
      DataSetDB.setDatasetStatus(getDataSource(), datasetId, DataSet.STATUS_READY_FOR_EXPORT);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Submit the data set for data reduction
   */
  public void submitDataReductionJob() {
    try {
      DataSetDB.setDatasetStatus(getDataSource(), datasetId, DataSet.STATUS_DATA_REDUCTION);
      Map<String, String> jobParams = new HashMap<String, String>();
      jobParams.put(DataReductionJob.ID_PARAM, String.valueOf(datasetId));
      JobManager.addJob(getDataSource(), getUser(), DataReductionJob.class.getCanonicalName(), jobParams);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Submit the data set for automatic QC
   */
  public void submitAutoQcJob() {
    try {
      Map<String, String> jobParams = new HashMap<String, String>();
      jobParams.put(AutoQCJob.ID_PARAM, String.valueOf(datasetId));
      jobParams.put(AutoQCJob.PARAM_ROUTINES_CONFIG, ResourceManager.QC_ROUTINES_CONFIG);
      JobManager.addJob(getDataSource(), getUser(), AutoQCJob.class.getCanonicalName(), jobParams);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Check if this instrument has a valid calibration for the start-time of the
   * data set the user wants to create. Checks both sensor calibrations
   * and external standards.
   */
  public void checkValidCalibration() {
    validCalibration = true;
    Map<String, String> params = FacesContext.getCurrentInstance()
        .getExternalContext().getRequestParameterMap();

    String startTime = params.get("uploadForm:startDate_input");
    // startTime not yet set
    if (startTime.length() > 0) {
      try {


        // Check sensor calibration equations
        CalibrationSet calibrations = new SensorCalibrationDB()
            .getMostRecentCalibrations(getDataSource(), getCurrentInstrumentId(),
                DateTimeUtils.parseDisplayDateTime(startTime));

        if (!calibrations.isValid()) {
          validCalibration = false;
          validCalibrationMessage = "One or more sensor calibration equations are missing";
        } else {
          // Check external standards
          CalibrationSet externalStandards = ExternalStandardDB.getInstance().getStandardsSet(getDataSource(), getCurrentInstrumentId(), DateTimeUtils.parseDisplayDateTime(startTime));
          if (!externalStandards.isComplete()) {
            validCalibration = false;
            validCalibrationMessage = "No complete set of external standards is available";
          } else if (!ExternalStandardDB.hasZeroStandard(externalStandards)) {
            validCalibration = false;
            validCalibrationMessage = "One external standard must have a zero concentration";
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        validCalibration = false;
        validCalibrationMessage = "Error while checking calibrations";
      }
    }
  }

  /**
   * @return true if the time and instrument for this dataset has a valid
   *         calibration, otherwise false
   */
  public boolean isValidCalibration() {
    return validCalibration;
  }

  /**
   * Get the error message for invalid calibrations
   * @return The error message
   */
  public String getValidCalibrationMessage() {
    return validCalibrationMessage;
  }

  /**
   * Update the list of data sets
   */
  public void updateDatasetList() {
    try {
      loadDataSets();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
