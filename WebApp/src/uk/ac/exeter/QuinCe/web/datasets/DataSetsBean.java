package uk.ac.exeter.QuinCe.web.datasets;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficientDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.InvalidCalibrationDateException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;
import uk.ac.exeter.QuinCe.jobs.files.DataSetJob;
import uk.ac.exeter.QuinCe.jobs.files.ExtractDataSetJob;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ResourceException;

/**
 * Bean for handling the creation and management of data sets
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
  public static final String NAV_DATASET_LIST = "dataset_list";

  public static final String NAV_APPROVAL_LIST = "approval_list";

  public static final String CURRENT_VIEW_ATTR = "datasetListView";

  public static final int DELETE_NOT_ALLOWED = -1;

  public static final int DELETE_ALLOWED = 0;

  public static final int DELETE_EXPORTED_ALLOWED = 1;

  /**
   * The data sets for the current instrument
   */
  private LinkedHashMap<Long, DataSet> dataSets;

  private LinkedHashMap<Long, DataSet> approvalDatasets;

  /**
   * The file definitions for the current instrument in JSON format for the
   * timeline
   */
  private String fileDefinitionsJson;

  /**
   * The data sets and data files for the current instrument in JSON format
   */
  private String timelineEntriesJson;

  /**
   * Details of the configurations on the instrument.
   */
  private String calibrationsJson;

  /**
   * The data set being created
   */
  private DataSet newDataSet;

  /**
   * The ID of the data set being processed
   */
  private long datasetId;

  /**
   * Says whether the dataset being defined has valid calibrations, (for
   * sensors, external standards, and calculation coefficients). This defaults
   * to true, but is actually checked when the form is submitted.
   */
  private boolean validCalibration = true;

  /**
   * Indicates whether or not the selected instrument has files
   */
  private boolean hasFiles = false;

  /**
   * The message to be displayed if any calibrations are invalid
   */
  private String validCalibrationMessage = null;

  /**
   * The ID of the datasets whose processing messages are being displayed.
   */
  private long processingMessagesId = -1L;

  /**
   * Start the dataset definition procedure
   *
   * @return The navigation to the dataset definition page
   */
  public String startNewDataset() {
    newDataSet = new DataSet(getCurrentInstrument());
    fileDefinitionsJson = null;
    timelineEntriesJson = null;
    calibrationsJson = null;
    validCalibration = true;
    validCalibrationMessage = null;

    return NAV_NEW_DATASET;
  }

  /**
   * Navigate to the datasets list
   *
   * @return The navigation string
   */
  public String goToList() {
    updateDatasetList();
    return getDatasetListNavigation();
  }

  /**
   * Get the {@link DataSet}s for the current instrument.
   *
   * <p>
   * The list is presented in reverse order, i.e. with the newest
   * {@link DataSet} first.
   * </p>
   *
   * @return The {@link DataSet}s
   */
  public List<DataSet> getDataSets() {

    List<DataSet> result = null;

    if (null != dataSets) {
      result = new ArrayList<DataSet>(dataSets.values().stream().toList());
      Collections.reverse(result);
    }

    return result;
  }

  public Collection<DataSet> getDatasetsForApproval() {
    return null == approvalDatasets ? null : approvalDatasets.values();
  }

  /**
   * Load the list of data sets for the instrument from the database
   *
   * @throws ResourceException
   *           If the app resources cannot be accessed
   * @throws InstrumentException
   *           If the instrument data is invalid
   * @throws RecordNotFoundException
   *           If the instrument cannot be found
   * @throws MissingParamException
   *           If any internal calls have missing parameters
   */
  private void loadDataSets() throws MissingParamException, DatabaseException,
    RecordNotFoundException, InstrumentException, ResourceException {
    if (null != getCurrentInstrument()) {
      dataSets = DataSetDB.getDataSets(getDataSource(),
        getCurrentInstrument().getId(), true);
      hasFiles = DataFileDB.getFileCount(getDataSource(),
        getCurrentInstrument().getId()) > 0;

      // Approval datasets are sorted by name, which (mostly) also sorts them by
      // instrument
      LinkedHashMap<Long, DataSet> approvalDatasetsIn = DataSetDB
        .getDatasetsWithStatus(getDataSource(),
          DataSet.STATUS_WAITING_FOR_APPROVAL);

      approvalDatasets = new LinkedHashMap<Long, DataSet>();
      approvalDatasetsIn.values().stream().sorted(new DatasetNameComparator())
        .forEach(d -> approvalDatasets.put(d.getId(), d));

    } else {
      dataSets = null;
    }
  }

  /**
   * Get the data files for the current instrument in JSON format for the
   * timeline
   *
   * @return The data files JSON
   */
  public String getTimelineEntriesJson() {
    if (null == timelineEntriesJson) {
      buildTimelineJson();
    }

    return timelineEntriesJson;
  }

  /**
   * Get the file definitions for the current instrument in JSON format for the
   * timeline
   *
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

      JsonArray fdJson = new JsonArray();

      // Add a fake definition for the data sets, so they can be seen on the
      // timeline
      JsonObject datasetDefinition = new JsonObject();

      datasetDefinition.addProperty("id", -1000);
      datasetDefinition.addProperty("content", "File Type:");
      datasetDefinition.addProperty("order", -1000);

      fdJson.add(datasetDefinition);

      for (int i = 0; i < getCurrentInstrument().getFileDefinitions()
        .size(); i++) {
        FileDefinition definition = getCurrentInstrument().getFileDefinitions()
          .get(i);

        // Store the definition number for use when building the files JSON
        // below
        definitionIds.put(definition.getFileDescription(), i);

        JsonObject fdJsonObject = new JsonObject();
        fdJsonObject.addProperty("id", i);
        fdJsonObject.addProperty("content", definition.getFileDescription());
        fdJsonObject.addProperty("order", i);

        fdJson.add(fdJsonObject);
      }

      fileDefinitionsJson = fdJson.toString();

      // Now the actual files
      List<DataFile> dataFiles = DataFileDB.getFiles(getDataSource(),
        getAppConfig(), getCurrentInstrument());

      JsonArray entriesJson = new JsonArray();

      for (int i = 0; i < dataFiles.size(); i++) {
        DataFile file = dataFiles.get(i);

        JsonObject entry = new JsonObject();

        entry.addProperty("type", "range");
        entry.addProperty("group",
          definitionIds.get(file.getFileDefinition().getFileDescription()));
        entry.addProperty("start",
          DateTimeUtils.toIsoDate(file.getOffsetStartTime()));
        entry.addProperty("end",
          DateTimeUtils.toIsoDate(file.getOffsetEndTime()));
        entry.addProperty("content", file.getFilename());
        entry.addProperty("title", file.getFilename());

        entriesJson.add(entry);
      }

      for (DataSet dataSet : dataSets.values()) {
        JsonObject entry = new JsonObject();

        entry.addProperty("type", "background");
        entry.addProperty("start", DateTimeUtils.toIsoDate(dataSet.getStart()));
        entry.addProperty("end", DateTimeUtils.toIsoDate(dataSet.getEnd()));
        entry.addProperty("content", dataSet.getName());
        entry.addProperty("title", dataSet.getName());
        entry.addProperty("className",
          dataSet.isNrt() ? "timelineNrtDataSet" : "timelineDataSet");

        entriesJson.add(entry);
      }

      timelineEntriesJson = entriesJson.toString();
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
    }
  }

  /**
   * Get the calibrations information for the timeline as a JSON string.
   *
   * @return The calibration info.
   */
  public String getCalibrationsJson() {
    if (null == calibrationsJson) {
      buildCalibrationsJson();
    }

    return calibrationsJson;
  }

  private void buildCalibrationsJson() {
    try {
      TreeMap<LocalDateTime, List<String>> calibrations = CalibrationDB
        .getCalibrationTimes(getDataSource(), getCurrentInstrument());

      JsonObject json = new JsonObject();

      for (Map.Entry<LocalDateTime, List<String>> entry : calibrations
        .entrySet()) {

        JsonArray types = new JsonArray();
        entry.getValue().forEach(t -> types.add(t));
        json.add(DateTimeUtils.toIsoDate(entry.getKey()), types);
      }

      calibrationsJson = json.toString();
    } catch (DatabaseException e) {
      ExceptionUtils.printStackTrace(e);
    }
  }

  /**
   * Get the new data set
   *
   * @return The new data set
   */
  public DataSet getNewDataSet() {
    return newDataSet;
  }

  /**
   * Get the names of all data sets for the instrument as a JSON string
   *
   * @return The data set names
   */
  public String getDataSetNamesJson() {
    StringBuilder json = new StringBuilder();

    json.append('[');

    int count = -1;
    for (DataSet dataSet : dataSets.values()) {
      count++;
      json.append('"');
      json.append(dataSet.getName());
      json.append('"');

      if (count < dataSets.size() - 1) {
        json.append(',');
      }
    }

    json.append(']');

    return json.toString();

  }

  /**
   * Store the newly defined data set
   *
   * @return Navigation to the data set list
   */
  public String addDataSet() {

    try {
      // Mark any existing NRT dataset for deletion
      DataSetDB.addDataSet(getDataSource(), newDataSet);

      Properties jobProperties = new Properties();
      jobProperties.setProperty(DataSetJob.ID_PARAM,
        String.valueOf(newDataSet.getId()));

      JobManager.addJob(getDataSource(), getUser(),
        ExtractDataSetJob.class.getCanonicalName(), jobProperties);

      loadDataSets();
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
    }

    return NAV_DATASET_LIST;
  }

  /**
   * Get the dataset ID
   *
   * @return The dataset ID
   */
  public long getDatasetId() {
    return datasetId;
  }

  /**
   * Set the dataset ID
   *
   * @param datasetId
   *          The dataset ID
   */
  public void setDatasetId(long datasetId) {
    this.datasetId = datasetId;
  }

  /**
   * Submit the data set for approval
   */
  public void submitForApproval() {
    try {
      DataSetDB.setDatasetStatus(getDataSource(), datasetId,
        DataSet.STATUS_WAITING_FOR_APPROVAL);
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
    }
  }

  /**
   * Approve the data set for export
   */
  public void approve() {
    try {
      DataSetDB.setDatasetStatus(getDataSource(), datasetId,
        DataSet.STATUS_READY_FOR_EXPORT);
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
    }
  }

  /**
   * Recalculate the data set, starting with automatic QC
   */
  public void recalculate() {
    try {
      DataSetDB.setDatasetStatus(getDataSource(), datasetId,
        DataSet.STATUS_SENSOR_QC);
      Properties properties = new Properties();
      properties.setProperty(DataSetJob.ID_PARAM, String.valueOf(datasetId));
      JobManager.addJob(getDataSource(), getUser(),
        AutoQCJob.class.getCanonicalName(), properties);
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
    }
  }

  /**
   * Check if this instrument has a valid calibration for the start-time of the
   * data set the user wants to create. Checks both sensor calibrations and
   * external standards.
   */
  public void checkValidCalibration() {
    validCalibration = true;
    Map<String, String> params = FacesContext.getCurrentInstance()
      .getExternalContext().getRequestParameterMap();

    String startTime = params.get("uploadForm:startDate_input");
    String endTime = params.get("uploadForm:endDate_input");

    if (startTime.length() > 0) {
      try {
        // Check for external standards if required.
        if (getCurrentInstrument().hasInternalCalibrations()) {

          // Check internal calibration standards
          CalibrationSet standards = ExternalStandardDB.getInstance()
            .getCalibrationSet(getDataSource(), getCurrentInstrument(),
              DateTimeUtils.parseDisplayDateTime(startTime),
              DateTimeUtils.parseDisplayDateTime(endTime));

          if (!standards.hasCompletePrior()) {
            validCalibration = false;
            validCalibrationMessage = "No complete set of external standards is available";
          }

          // Disable zero standard check. #2037
          // May be reinstated for different types of pCO2 system in the
          // future.
          /*
           * else if (!ExternalStandardDB.hasZeroStandard(standards)) {
           * validCalibration = false; validCalibrationMessage =
           * "One external standard must have a zero concentration"; }
           */
        }

        if (validCalibration) {
          // Check calculation coefficients, if there are any
          if (getCurrentInstrument().hasCalculationCoefficients()) {
            try {

              CalibrationSet coefficients = CalculationCoefficientDB
                .getInstance().getCalibrationSet(getDataSource(),
                  getCurrentInstrument(),
                  DateTimeUtils.parseDisplayDateTime(startTime),
                  DateTimeUtils.parseDisplayDateTime(endTime));

              if (!coefficients.hasCompletePrior()) {
                validCalibration = false;
                validCalibrationMessage = "One or more calculation coefficents are missing";
              }
            } catch (InvalidCalibrationDateException e) {
              validCalibration = false;
              validCalibrationMessage = "Cannot change calculation coefficients within a dataset";
            }
          }
        }
      } catch (Exception e) {
        ExceptionUtils.printStackTrace(e);
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
   *
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
      ExceptionUtils.printStackTrace(e);
    }
  }

  public boolean getHasFiles() {
    return hasFiles;
  }

  public void setHasFiles(boolean dummy) {
    // Ignore any attempt to set this value
  }

  public long getProcessingMessagesId() {
    return processingMessagesId;
  }

  public void setProcessingMessagesId(long processingMessagesId) {
    this.processingMessagesId = processingMessagesId;
  }

  public String getProcessingMessages() {
    String result = null;

    if (null != dataSets) {
      for (DataSet dataSet : dataSets.values()) {
        if (dataSet.getId() == processingMessagesId) {
          result = dataSet.getProcessingMessages().getDisplayString();
          break;
        }
      }
    }

    if (null == result && null != approvalDatasets) {
      for (DataSet dataSet : approvalDatasets.values()) {
        if (dataSet.getId() == processingMessagesId) {
          result = dataSet.getProcessingMessages().getDisplayString();
          break;
        }
      }
    }

    return result;
  }

  public String getInstrumentName(DataSet dataSet) {
    return getInstrument(dataSet.getInstrumentId()).getDisplayName();
  }

  public String getOwnerName(DataSet dataSet) {
    User owner = getInstrument(dataSet.getInstrumentId()).getOwner();
    return owner.getSurname() + ", " + owner.getGivenName();
  }

  public String getOwnerEmail(DataSet dataSet) {
    return getInstrument(dataSet.getInstrumentId()).getOwner()
      .getEmailAddress();
  }

  private void setCurrentView(String view) {
    getSession().setAttribute(CURRENT_VIEW_ATTR, view);
  }

  public void setApprovalView() {
    setCurrentView(NAV_APPROVAL_LIST);
  }

  public void setListView() {
    setCurrentView(NAV_DATASET_LIST);
  }

  public int canExport(long datasetId) {
    int result = DELETE_NOT_ALLOWED;

    DataSet dataset = dataSets.get(datasetId);

    if (!dataset.isNrt()) {
      if (!dataset.hasBeenExported()) {
        result = DELETE_ALLOWED;
      } else if (getUser().isAdminUser()) {
        result = DELETE_EXPORTED_ALLOWED;
      }
    }

    return result;
  }

  public void delete() {
    try {
      if (canExport(datasetId) != DELETE_NOT_ALLOWED) {
        dataSets.get(datasetId).setStatus(DataSet.STATUS_DELETE);
        DataSetDB.updateDataSet(getDataSource(), dataSets.get(datasetId));
      }
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
    }
  }

  class DatasetNameComparator implements Comparator<DataSet> {
    @Override
    public int compare(DataSet o1, DataSet o2) {
      return o1.getName().compareTo(o2.getName());
    }

  }
}
