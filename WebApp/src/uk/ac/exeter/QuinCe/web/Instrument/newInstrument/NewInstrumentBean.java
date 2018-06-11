package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

import uk.ac.exeter.QuinCe.data.Dataset.DataSetRawData;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecificationException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.InvalidPositionFormatException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.LatitudeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.LongitudeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.PositionSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.NoSuchCategoryException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignmentException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.HighlightedString;
import uk.ac.exeter.QuinCe.utils.HighlightedStringException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.FileUploadBean;
import uk.ac.exeter.QuinCe.web.Instrument.InstrumentListBean;
import uk.ac.exeter.QuinCe.web.datasets.DataSetsBean;
import uk.ac.exeter.QuinCe.web.files.DataFilesBean;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Bean for collecting data about a new instrument
 * @author Steve Jones
 */
@ManagedBean
@SessionScoped
public class NewInstrumentBean extends FileUploadBean {

  /**
   * Navigation to start definition of a new instrument
   */
  private static final String NAV_NAME = "name";

  /**
   * Navigation when cancelling definition of a new instrument
   */
  private static final String NAV_INSTRUMENT_LIST = "instrument_list";

  /**
   * Navigation to the Upload File page
   */
  private static final String NAV_UPLOAD_FILE = "upload_file";

  /**
   * Navigation to the Assign Variables page
   */
  private static final String NAV_ASSIGN_VARIABLES = "assign_variables";

  /**
   * Navigation to the run types selection page
   */
  private static final String NAV_RUN_TYPES = "run_types";

  /**
   * Navigation to the other info page
   */
  private static final String NAV_OTHER_INFO = "other_info";

  /**
   * Date/Time formatter for previewing extracted dates
   */
  private static final DateTimeFormatter PREVIEW_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /**
   * The Instrument List Bean
   */
  @ManagedProperty("#{instrumentListBean}")
  private InstrumentListBean instrumentListBean;

  /**
   * The data sets bean
   */
  @ManagedProperty("#{dataSetsBean}")
  private DataSetsBean dataSetsBean;

  /**
   * The data sets bean
   */
  @ManagedProperty("#{dataFilesBean}")
  private DataFilesBean dataFilesBean;

  /**
   * The name of the new instrument
   */
  private String instrumentName;

  /**
   * The set of sample files for the instrument definition
   */
  private NewInstrumentFileSet instrumentFiles;

  /**
   * The assignments of sensors to data file columns
   */
  private SensorAssignments sensorAssignments;

  /**
   * The sample file that is currently being edited
   */
  private FileDefinitionBuilder currentInstrumentFile;

  /**
   * Sensor assignment - file index
   */
  private String sensorAssignmentFile = null;

  /**
   * Sensor assignment - column index
   */
  private int sensorAssignmentColumn = -1;

  /**
   * Sensor assignment - column index
   */
  private String sensorAssignmentName = null;

  /**
   * The name of the sensor being assigned
   */
  private String sensorAssignmentSensorType = null;

  /**
   * Sensor assignment - must post-calibration be applied?
   */
  private boolean sensorAssignmentPostCalibrated = false;

  /**
   * Sensor assignment - the answer to the Depends Question
   * @see SensorType#getDependsQuestion()
   */
  private boolean sensorAssignmentDependsQuestionAnswer = false;

  /**
   * Sensor assignment - is this a primary or fallback sensor?
   */
  private boolean sensorAssignmentPrimary = false;

  /**
   * Sensor assignment - the 'missing' value
   */
  private String sensorAssignmentMissingValue = null;

  /**
   * The file for which the longitude is being set
   */
  private String longitudeFile = null;

  /**
   * The column index of the longitude
   */
  private int longitudeColumn = -1;

  /**
   * The longitude format
   */
  private int longitudeFormat = 0;

  /**
   * The file for which the latitude is being set
   */
  private String latitudeFile = null;

  /**
   * The column index of the latitude
   */
  private int latitudeColumn = -1;

  /**
   * The latitude format
   */
  private int latitudeFormat = 0;

  /**
   * The file for which the hemisphere is being set
   */
  private String hemisphereFile = null;

  /**
   * The coordinate (longitude or latitude) for which the hemisphere
   * is being set
   */
  private int hemisphereCoordinate = -1;

  /**
   * The column index of the hemisphere
   */
  private int hemisphereColumn = -1;

  /**
   * The file for which the date/time is being set
   */
  private String dateTimeFile = null;

  /**
   * The column index for the date/time field
   */
  private int dateTimeColumn = -1;

  /**
   * The date/time variable being set
   */
  private String dateTimeVariable = null;

  /**
   * The format of the date/time string
   */
  private String dateTimeFormat = null;

  /**
   * The format of the date string
   */
  private String dateFormat = null;

  /**
   * The format of the time string
   */
  private String timeFormat = null;

  /**
   * The prefix for the start time in the file header
   */
  private String startTimePrefix = null;

  /**
   * The suffix for the start time in the file header
   */
  private String startTimeSuffix = null;

  /**
   * The format for the start time in the file header
   */
  private String startTimeFormat = "MMM dd YYYY HH:MM:SS";

  /**
   * The start time line extracted from the file header. This is a JSON string
   * that contains details of how to format the line.
   */
  private String startTimeLine = null;

  /**
   * The start time extracted from the file header
   */
  private String startTimeDate = null;

  /**
   * The index of the file to be unassigned
   */
  private int unassignFile = -1;

  /**
   * The index of the column to be unassigned
   */
  private int unassignColumn = -1;

  /**
   * The name of the file to be removed
   */
  private String removeFileName = null;

  /**
   * The file for which a run type is being assigned
   */
  private int runTypeAssignFile = -1;

  /**
   * The run type name to be assigned to a Run Type Category
   */
  private String runTypeAssignName = null;

  /**
   * The code of the Run Type Category that the
   * run type is being assigned to
   */
  private String runTypeAssignCode = null;

  /**
   * The run type that the assigned run type is aliased to
   */
  private String runTypeAssignAliasTo = null;

  /**
   * The pre-flushing time
   */
  private int preFlushingTime = 0;

  /**
   * The post-flushing time
   */
  private int postFlushingTime = 0;

  /**
   * The minimum water flow
   */
  private int minimumWaterFlow = -1;

  /**
   * The averaging mode
   */
  private int averagingMode = DataSetRawData.AVG_MODE_NONE;

  /**
   * The averaging mode
   */
  private String platformCode = null;

  /**
   * The name of the file for which a Run Type column is being defined
   */
  private String runTypeFile = null;

  /**
   * The column index being assigned for Run Type
   */
  private int runTypeColumn = -1;

  /**
   * Begin a new instrument definition
   * @return The navigation to the start page
   */
  public String start() {
    clearAllData();
    return NAV_NAME;
  }

  /**
   * Cancel the current instrument definition
   * @return Navigation to the instrument list
   */
  public String cancel() {
    clearAllData();
    return NAV_INSTRUMENT_LIST;
  }

  /**
   * Navigate to the Name page
   * @return Navigation to the name page
   */
  public String goToName() {
    clearFile();
    return NAV_NAME;
  }

  /**
   * Navigate to the files step.
   *
   * <p>
   *   The page we navigate to depends on the current status of the instrument.
   * <p>
   *
   * <p>
   *   If no files have been added, we create a new empty file and go to the upload page.
   *   Otherwise, we go to the variable assignment page.
   * </p>
   *
   * @return Navigation to the files
   * @throws InstrumentFileExistsException If the default instrument file has already been added.
   */
  public String goToFiles() throws InstrumentFileExistsException {
    String result;

    if (instrumentFiles.size() == 0) {
      currentInstrumentFile = new FileDefinitionBuilder(instrumentFiles);

      result = NAV_UPLOAD_FILE;
    } else {
      if (null == currentInstrumentFile) {
        currentInstrumentFile = FileDefinitionBuilder.copy((FileDefinitionBuilder) instrumentFiles.get(0));
      }
      result = NAV_ASSIGN_VARIABLES;
    }

    return result;
  }

  /**
   * Direct navigation to the Assign Variables page
   * @return Navigation to the Assign Variables page
   */
  public String goToAssignVariables() {
    return NAV_ASSIGN_VARIABLES;
  }

  /**
   * Add a new file to the instrument
   * @return The navigation to the file upload
   */
  public String addFile() {
    currentInstrumentFile = new FileDefinitionBuilder(instrumentFiles);
    return NAV_UPLOAD_FILE;
  }

  @Override
  protected String getFormName() {
    return "newInstrumentForm";
  }

  /**
   * Store the uploaded data in the current instrument file.
   * Detailed processing will be triggered by the source page calling {@link FileDefinitionBuilder#guessFileLayout}.
   */
  @Override
  public void processUploadedFile() {
    currentInstrumentFile.setFileContents(getFileLines());
  }


  /**
   * Clear all data from the bean ready for a new
   * instrument to be defined
   */
  private void clearAllData() {
    instrumentName = null;
    instrumentFiles = new NewInstrumentFileSet();
    sensorAssignments = ResourceManager.getInstance().getSensorsConfiguration().getNewSensorAssigments();
    resetSensorAssignmentValues();
    resetPositionAssignmentValues();
    resetDateTimeAssignmentValues();
    clearRunTypeAssignments();
    clearFile();
  }

  /**
   * Get the name of the new instrument
   * @return The instrument name
   */
  public String getInstrumentName() {
    return instrumentName;
  }

  /**
   * Set the name of the new instrument
   * @param instrumentName The instrument name
   */
  public void setInstrumentName(String instrumentName) {
    this.instrumentName = instrumentName;
  }

  /**
   * Get the instrument file that is currently being worked on
   * @return The current instrument file
   */
  public FileDefinitionBuilder getCurrentInstrumentFile() {
    return currentInstrumentFile;
  }

  /**
   * Retrieve the full set of instrument files
   * @return The instrument files
   */
  public NewInstrumentFileSet getInstrumentFiles() {
    return instrumentFiles;
  }

  /**
   * Determines whether or not the file set contains more than one file
   * @return {@code true} if more than one file is in the set; {@code false} if there are zero or one files
   */
  public boolean getHasMultipleFiles() {
    return (instrumentFiles.size() > 1);
  }

  /**
   * Add the current instrument file to the file set (or update it)
   * and clear its status as 'current'. Then navigate to the
   * variable assignment page.
   * @return The navigation to the variable assignment page
   */
  public String useFile() {
    instrumentFiles.add(currentInstrumentFile);
    clearFile();
    return NAV_ASSIGN_VARIABLES;
  }

  /**
   * Discard the current instrument file
   */
  public void discardUploadedFile() {
    clearFile();
  }

  @Override
  public void clearFile() {
    currentInstrumentFile = new FileDefinitionBuilder(instrumentFiles);
    super.clearFile();
  }

  /**
   * Get the current set of sensor assignments as a JSON string.
   *
   * <p>
   *   The format of the JSON is as follows:
   * </p>
   * <pre>
   * [
   *   {
   *     "name": "&lt;sensor type name&gt;",
   *     "required": &lt;true/false&gt;,
   *     "assignments": [
   *       "file": "&lt;data file name&gt;",
   *       "column": &lt;column index&gt;
   *     ]
   *   }
   * ]
   * </pre>
   *
   * @return The sensor assignments
   * @throws SensorAssignmentException If the sensor assignments are internally invalid
   */
  public String getSensorAssignments() throws SensorAssignmentException {
    StringBuilder json = new StringBuilder();

    // Start the array of objects
    json.append('[');

    int count = 0;
    for (SensorType sensorType : sensorAssignments.keySet()) {
      json.append('{');

      // Sensor Type name
      json.append("\"name\": \"");
      json.append(sensorType.getName());
      json.append("\",");

      // Is an assignment required?
      json.append("\"required\":");
      json.append(sensorAssignments.isAssignmentRequired(sensorType));
      json.append(',');

      // Is an assignment required?
      json.append("\"named\":");
      json.append(sensorType.canBeNamed());
      json.append(',');

      // Is this a core sensor?
      json.append("\"core\":");
      json.append(sensorType.isCoreSensor());
      json.append(',');

      // Are many assignments allowed for this sensor?
      json.append("\"many\":");
      json.append(sensorType.canHaveMany());
      json.append(',');

      // Will multiple sensors be averaged?
      json.append("\"averaged\":");
      json.append(sensorType.isAveraged());
      json.append(',');

      // Can sensors of this type be post-calibrated?
      json.append("\"postCalibrated\":");
      json.append(sensorType.canBePostCalibrated());
      json.append(',');

      // The Depends Question
      json.append("\"dependsQuestion\":");
      if (null == sensorType.getDependsQuestion()) {
        json.append("null");
      } else {
        json.append('"');
        json.append(sensorType.getDependsQuestion());
        json.append('"');
      }
      json.append(',');

      // The columns assigned to the sensor type
      Set<SensorAssignment> assignments = sensorAssignments.get(sensorType);

      json.append("\"assignments\":[");

      if (null != assignments) {
        int assignmentCount = 0;
        for (SensorAssignment assignment : assignments) {
          json.append("{\"file\":\"");
          json.append(assignment.getDataFile());
          json.append("\",\"column\":");
          json.append(assignment.getColumn());
          json.append(",\"sensorName\":\"");
          json.append(assignment.getSensorName());
          json.append("\",\"postCalibrated\":");
          json.append(assignment.getPostCalibrated());
          json.append(",\"primary\":");
          json.append(assignment.isPrimary());
          json.append('}');

          if (assignmentCount < assignments.size() - 1) {
            json.append(',');
          }

          assignmentCount++;
        }
      }

      json.append(']');

      // End the array, and add a comma if this isn't the last object
      json.append('}');
      if (count < sensorAssignments.size() - 1) {
        json.append(',');
      }
      count++;
    }

    // Finish the array
    json.append(']');

    return json.toString();
  }

  /**
   * Dummy method for setting sensor assignments. It doesn't
   * actually do anything, but it's needed for the JSF communications
   * to work.
   * @param assignments The assignments. They are ignored.
   */
  public void setSensorAssignments(String assignments) {
    // Do nothing
  }

  /**
   * Dummy method for setting time and position assignments. It doesn't
   * actually do anything, but it's needed for the JSF communications
   * to work.
   * @param assignments The assignments. They are ignored.
   */
  public void setTimePositionAssignments(String assignments) {
    // Do nothing
  }

  /**
   * Get the sensor assignment file
   * @return The file
   */
  public String getSensorAssignmentFile() {
    return sensorAssignmentFile;
  }

  /**
   * Set the sensor assignment file
   * @param sensorAssignmentFile The file
   */
  public void setSensorAssignmentFile(String sensorAssignmentFile) {
    this.sensorAssignmentFile = sensorAssignmentFile;
  }

  /**
   * Get the sensor assignment column index
   * @return The column index
   */
  public int getSensorAssignmentColumn() {
    return sensorAssignmentColumn;
  }

  /**
   * Set the sensor assignment column index
   * @param sensorAssignmentColumn The column index
   */
  public void setSensorAssignmentColumn(int sensorAssignmentColumn) {
    this.sensorAssignmentColumn = sensorAssignmentColumn;
  }

  /**
   * Get the name of the assigned sensor
   * @return The sensor name
   */
  public String getSensorAssignmentName() {
    return sensorAssignmentName;
  }

  /**
   * Set the name of the assigned sensor
   * @param sensorAssignmentName The sensor name
   */
  public void setSensorAssignmentName(String sensorAssignmentName) {
    this.sensorAssignmentName = sensorAssignmentName;
  }

  /**
   * Get the name of the sensor type being assigned
   * @return The sensor type
   */
  public String getSensorAssignmentSensorType() {
    return sensorAssignmentSensorType;
  }

  /**
   * Set the name of the sensor type being assigned
   * @param sensorAssignmentSensorType The sensor type
   */
  public void setSensorAssignmentSensorType(String sensorAssignmentSensorType) {
    this.sensorAssignmentSensorType = sensorAssignmentSensorType;
  }

  /**
   * Get the sensor assignment post-calibration flag
   * @return The post-calibration flag
   */
  public boolean getSensorAssignmentPostCalibrated() {
    return sensorAssignmentPostCalibrated;
  }

  /**
   * Set the sensor assignment post-calibration flag
   * @param sensorAssignmentPostCalibrated The post-calibration flag
   */
  public void setSensorAssignmentPostCalibrated(boolean sensorAssignmentPostCalibrated) {
    this.sensorAssignmentPostCalibrated = sensorAssignmentPostCalibrated;
  }

  /**
   * Get the answer to the sensor assignment's Depends Question
   * @return The answer to the Depends Question
   * @see SensorType#getDependsQuestion()
   */
  public boolean getSensorAssignmentDependsQuestionAnswer() {
    return sensorAssignmentDependsQuestionAnswer;
  }

  /**
   * Set the answer to the sensor assignment's Depends Question
   * @param sensorAssignmentDependsQuestionAnswer The answer to the Depends Question
   * @see SensorType#getDependsQuestion()
   */
  public void setSensorAssignmentDependsQuestionAnswer(boolean sensorAssignmentDependsQuestionAnswer) {
    this.sensorAssignmentDependsQuestionAnswer = sensorAssignmentDependsQuestionAnswer;
  }

  /**
   * Get the flag indicating whether the assigned sensor is primary or fallback
   * @return The primary sensor flag
   */
  public boolean getSensorAssignmentPrimary() {
    return sensorAssignmentPrimary;
  }

  /**
   * Set the flag indicating whether the assigned sensor is primary or fallback
   * @param sensorAssignmentPrimary The primary sensor flag
   */
  public void setSensorAssignmentPrimary(boolean sensorAssignmentPrimary) {
    this.sensorAssignmentPrimary = sensorAssignmentPrimary;
  }

  /**
   * Get the 'missing value' value for the current sensor assignment
   * @return The missing value
   */
  public String getSensorAssignmentMissingValue() {
    return sensorAssignmentMissingValue;
  }

  /**
   * Set the 'missing value' value for the current sensor assignment
   * @param sensorAssignmentMissinngValue The missing value
   */
  public void setSensorAssignmentMissingValue(String sensorAssignmentMissinngValue) {
    this.sensorAssignmentMissingValue = sensorAssignmentMissinngValue;
  }

  /**
   * Add a new assignment to the sensor assignments
   * @throws Exception If any errors occur
   */
  public void storeSensorAssignment() throws Exception {
    SensorAssignment assignment = new SensorAssignment(sensorAssignmentFile, sensorAssignmentColumn, sensorAssignmentName, sensorAssignmentPostCalibrated, sensorAssignmentPrimary, sensorAssignmentDependsQuestionAnswer, sensorAssignmentMissingValue);
    sensorAssignments.addAssignment(sensorAssignmentSensorType, assignment);

    // Reset the assign dialog values, because it's so damn hard to do in Javascript
    resetSensorAssignmentValues();
  }

  /**
   * Set the assignment dialog values to their defaults
   */
  public void resetSensorAssignmentValues() {
    sensorAssignmentFile = null;
    sensorAssignmentColumn = -1;
    sensorAssignmentName = null;
    sensorAssignmentSensorType = null;
    sensorAssignmentPrimary = true;
    sensorAssignmentPostCalibrated = false;
    sensorAssignmentDependsQuestionAnswer = false;
    sensorAssignmentMissingValue = null;
  }

  /**
   * Get the file for which the longitude is being set
   * @return The longitude file
   */
  public String getLongitudeFile() {
    return longitudeFile;
  }

  /**
   * Set the file for which the longitude is being set
   * @param longitudeFile The longitude file
   */
  public void setLongitudeFile(String longitudeFile) {
    this.longitudeFile = longitudeFile;
  }

  /**
   * Get the longitude column index
   * @return The longitude column index
   */
  public int getLongitudeColumn() {
    return longitudeColumn;
  }

  /**
   * Set the longitude column index
   * @param longitudeColumn The longitude column index
   */
  public void setLongitudeColumn(int longitudeColumn) {
    this.longitudeColumn = longitudeColumn;
  }

  /**
   * Get the longitude format
   * @return The longitude format
   */
  public int getLongitudeFormat() {
    return longitudeFormat;
  }

  /**
   * Set the longitude format
   * @param longitudeFormat The longitude format
   */
  public void setLongitudeFormat(int longitudeFormat) {
    this.longitudeFormat = longitudeFormat;
  }

  /**
   * Set the longitude column and format for a file
   * @throws InvalidPositionFormatException If the format is invalid
   */
  public void assignLongitude() throws InvalidPositionFormatException {
    FileDefinitionBuilder file = (FileDefinitionBuilder) instrumentFiles.get(longitudeFile);
    file.getLongitudeSpecification().setValueColumn(longitudeColumn);
    file.getLongitudeSpecification().setFormat(longitudeFormat);
    if (longitudeFormat != LongitudeSpecification.FORMAT_0_180) {
      file.getLongitudeSpecification().setHemisphereColumn(-1);
    }

    resetPositionAssignmentValues();
  }

  /**
   * Get the file for which the latitude is being set
   * @return The latitude file
   */
  public String getLatitudeFile() {
    return latitudeFile;
  }

  /**
   * Set the file for which the latitude is being set
   * @param latitudeFile The latitude file
   */
  public void setLatitudeFile(String latitudeFile) {
    this.latitudeFile = latitudeFile;
  }

  /**
   * Get the latitude column index
   * @return The latitude column index
   */
  public int getLatitudeColumn() {
    return latitudeColumn;
  }

  /**
   * Set the latitude column index
   * @param latitudeColumn The latitude column index
   */
  public void setLatitudeColumn(int latitudeColumn) {
    this.latitudeColumn = latitudeColumn;
  }

  /**
   * Get the latitude format
   * @return The latitude format
   */
  public int getLatitudeFormat() {
    return latitudeFormat;
  }

  /**
   * Set the latitude format
   * @param latitudeFormat The latitude format
   */
  public void setLatitudeFormat(int latitudeFormat) {
    this.latitudeFormat = latitudeFormat;
  }

  /**
   * Set the latitude column and format for a file
   * @throws InvalidPositionFormatException If the format is invalid
   */
  public void assignLatitude() throws InvalidPositionFormatException {
    FileDefinitionBuilder file = (FileDefinitionBuilder) instrumentFiles.get(latitudeFile);
    file.getLatitudeSpecification().setValueColumn(latitudeColumn);
    file.getLatitudeSpecification().setFormat(latitudeFormat);
    if (latitudeFormat != LatitudeSpecification.FORMAT_0_90) {
      file.getLatitudeSpecification().setHemisphereColumn(-1);
    }

    resetPositionAssignmentValues();
  }

  /**
   * Get the file for which the hemisphere is being set
   * @return The hemisphere file
   */
  public String getHemisphereFile() {
    return hemisphereFile;
  }

  /**
   * Set the file for which the hemisphere is being set
   * @param hemisphereFile The hemisphere file
   */
  public void setHemisphereFile(String hemisphereFile) {
    this.hemisphereFile = hemisphereFile;
  }

  /**
   * Get the hemisphere column index
   * @return The hemisphere column index
   */
  public int getHemisphereColumn() {
    return hemisphereColumn;
  }

  /**
   * Set the hemisphere column index
   * @param hemisphereColumn The hemisphere column index
   */
  public void setHemisphereColumn(int hemisphereColumn) {
    this.hemisphereColumn = hemisphereColumn;
  }

  /**
   * Get the coordinate for which the hemisphere is being set
   * @return The hemipshere coordinate
   */
  public int getHemisphereCoordinate() {
    return hemisphereCoordinate;
  }

  /**
   * Set the coordinate for which the hemisphere is being set
   * @param hemisphereCoordinate The hemipshere coordinate
   */
  public void setHemisphereCoordinate(int hemisphereCoordinate) {
    this.hemisphereCoordinate = hemisphereCoordinate;
  }

  /**
   * Assign the hemisphere column for a coordinate
   */
  public void assignHemisphere() {
    FileDefinitionBuilder file = (FileDefinitionBuilder) instrumentFiles.get(hemisphereFile);

    PositionSpecification posSpec = null;

    if (hemisphereCoordinate == PositionSpecification.COORD_LONGITUDE) {
      posSpec = file.getLongitudeSpecification();
    } else {
      posSpec = file.getLatitudeSpecification();
    }

    posSpec.setHemisphereColumn(hemisphereColumn);
    resetPositionAssignmentValues();
  }

  /**
   * Clear all position assignment data
   */
  private void resetPositionAssignmentValues() {
    longitudeFile = null;
    longitudeColumn = -1;
    longitudeFormat = -1;
    latitudeFile = null;
    latitudeColumn = -1;
    latitudeFormat = -1;
    hemisphereFile = null;
    hemisphereCoordinate = -1;
    hemisphereColumn = -1;
  }

  /**
   * Clear all date/time assignment data
   */
  private void resetDateTimeAssignmentValues() {
    dateTimeFile = null;
    dateTimeColumn = -1;
    dateTimeVariable = null;
    dateFormat = null;
    startTimePrefix = null;
    startTimeSuffix = null;
    startTimeFormat = "MMM dd YYYY HH:MM:SS";
  }

  /**
   * Get the file for which a date/time variable is being assigned
   * @return The file
   */
  public String getDateTimeFile() {
    return dateTimeFile;
  }

  /**
   * Set the file for which a date/time variable is being assigned
   * @param dateTimeFile The file
   */
  public void setDateTimeFile(String dateTimeFile) {
    this.dateTimeFile = dateTimeFile;
  }

  /**
   * Get the column index that is being assigned to a date/time variable
   * @return The column index
   */
  public int getDateTimeColumn() {
    return dateTimeColumn;
  }

  /**
   * Set the column index that is being assigned to a date/time variable
   * @param dateTimeColumn The column index
   */
  public void setDateTimeColumn(int dateTimeColumn) {
    this.dateTimeColumn = dateTimeColumn;
  }

  /**
   * Get the name of the date/time variable being assigned
   * @return The variable name
   */
  public String getDateTimeVariable() {
    return dateTimeVariable;
  }

  /**
   * Set the name of the date/time variable being assigned
   * @param dateTimeVariable The variable name
   */
  public void setDateTimeVariable(String dateTimeVariable) {
    this.dateTimeVariable = dateTimeVariable;
  }

  /**
   * Get the format of the date/time string
   * @return The format
   */
  public String getDateTimeFormat() {
    return dateTimeFormat;
  }

  /**
   * Set the format of the date/time string
   * @param dateTimeFormat The format
   */
  public void setDateTimeFormat(String dateTimeFormat) {
    this.dateTimeFormat = dateTimeFormat;
  }

  /**
   * Get the format of the date string
   * @return The format
   */
  public String getDateFormat() {
    return dateFormat;
  }

  /**
   * Set the format of the date string
   * @param dateFormat The format
   */
  public void setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  /**
   * Get the format of the time string
   * @return The format
   */
  public String getTimeFormat() {
    return timeFormat;
  }

  /**
   * Set the format of the time string
   * @param timeFormat The format
   */
  public void setTimeFormat(String timeFormat) {
    this.timeFormat = timeFormat;
  }

  /**
   * Assign a date/time variable
   * @throws DateTimeSpecificationException If the assignment cannot be made
   */
  public void assignDateTime() throws DateTimeSpecificationException {
    DateTimeSpecification dateTimeSpec = instrumentFiles.get(dateTimeFile).getDateTimeSpecification();

    int assignmentIndex = DateTimeSpecification.getAssignmentIndex(dateTimeVariable);

    switch (assignmentIndex) {
    case DateTimeSpecification.DATE_TIME: {
      dateTimeSpec.assign(dateTimeVariable, dateTimeColumn, dateTimeFormat);
      break;
    }
    case DateTimeSpecification.DATE: {
      dateTimeSpec.assign(dateTimeVariable, dateTimeColumn, dateFormat);
      break;
    }
    case DateTimeSpecification.TIME: {
      dateTimeSpec.assign(dateTimeVariable, dateTimeColumn, timeFormat);
      break;
    }
    case DateTimeSpecification.HOURS_FROM_START: {
      dateTimeSpec.assignHoursFromStart(dateTimeColumn, startTimePrefix, startTimeSuffix, startTimeFormat);
      break;
    }
    default: {
      dateTimeSpec.assign(dateTimeVariable, dateTimeColumn, null);
      break;
    }
    }

    resetDateTimeAssignmentValues();
  }

  /**
   * Get the start time prefix
   * @return The start time prefix
   */
  public String getStartTimePrefix() {
    return startTimePrefix;
  }

  /**
   * Set the start time prefix
   * @param startTimePrefix The start time prefix
   */
  public void setStartTimePrefix(String startTimePrefix) {
    this.startTimePrefix = startTimePrefix;
  }

  /**
   * Get the start time suffix
   * @return The start time suffix
   */
  public String getStartTimeSuffix() {
    return startTimeSuffix;
  }

  /**
   * Set the start time suffix
   * @param startTimeSuffix The start time suffix
   */
  public void setStartTimeSuffix(String startTimeSuffix) {
    this.startTimeSuffix = startTimeSuffix;
  }

  /**
   * Get the start time format
   * @return The start time format
   */
  public String getStartTimeFormat() {
    return startTimeFormat;
  }

  /**
   * Set the start time format
   * @param startTimeFormat The start time format
   */
  public void setStartTimeFormat(String startTimeFormat) {
    this.startTimeFormat = startTimeFormat;
  }

  /**
   * Get the start time line extracted from the header
   * @return The start time line
   */
  public String getStartTimeLine() {
    return startTimeLine;
  }

  /**
   * Dummy method for setting start time line - does nothing
   * @param startTimeLine The start time line (ignored)
   */
  public void setStartTimeLine(String startTimeLine) {
    // Do nothing
  }

  /**
   * Get the start time extracted from the header
   * @return The start time
   */
  public String getStartTimeDate() {
    return startTimeDate;
  }

  /**
   * Dummy method for setting start time date - does nothing
   * @param startTimeDate The start time date (ignored)
   */
  public void setStartTimeDate(String startTimeDate) {
    // Do nothing
  }

  /**
   * Extract the start time from a file header
   * @throws HighlightedStringException If the highlighted string cannot be created
   */
  public void extractStartTime() throws HighlightedStringException {
    FileDefinitionBuilder fileDefinition = (FileDefinitionBuilder) instrumentFiles.get(dateTimeFile);

    HighlightedString headerLine = fileDefinition.getHeaderLine(startTimePrefix, startTimeSuffix);
    if (null == headerLine) {
      startTimeLine = null;
      startTimeDate = null;
    } else {
      startTimeLine = headerLine.getJson();

      try {
        String headerDateString = headerLine.getHighlightedPortion();
        LocalDateTime headerDate = LocalDateTime.parse(headerDateString, DateTimeFormatter.ofPattern(startTimeFormat));
        startTimeDate = PREVIEW_DATE_TIME_FORMATTER.format(headerDate);
      } catch (DateTimeException e) {
        startTimeDate = null;
      }
    }
  }

  /**
   * Get the list of registered file descriptions and their columns as a JSON string
   * @return The file names
   * @see NewInstrumentFileSet#getFilesAndColumns()
   */
  public String getFilesAndColumns() {
    return instrumentFiles.getFilesAndColumns();
  }

  /**
   * Get the time and position column assignments for all
   * files related to this instrument.
   * Required because {@link NewInstrumentFileSet} is a list, so
   * JSF can't access named properties.
   * @return The time and position assignments
   * @throws DateTimeSpecificationException If an error occurs while generating the date/time string
   * @see NewInstrumentFileSet#getFileSpecificAssignments(SensorAssignments)
   */
  public String getFileSpecificAssignments() throws DateTimeSpecificationException {
    return instrumentFiles.getFileSpecificAssignments(sensorAssignments);
  }

  /**
   * Dummy method for setting fileSpecificAssignments. Does nothing.
   * @see #getFileSpecificAssignments()
   * @param dummy Ignored value
   */
  public void setFileSpecificAssignments(String dummy) {
    // Do nothing
  }

  /**
   * Get the index of the file to unassign
   * @return The file index
   */
  public int getUnassignFile() {
    return unassignFile;
  }

  /**
   * Set the index of the file to unassign
   * @param unassignFile The file index
   */
  public void setUnassignFile(int unassignFile) {
    this.unassignFile = unassignFile;
  }

  /**
   * Get the index of the column to unassign
   * @return The column index
   */
  public int getUnassignColumn() {
    return unassignColumn;
  }

  /**
   * Set the index of the column to unassign
   * @param unassignColumn The column index
   */
  public void setUnassignColumn(int unassignColumn) {
    this.unassignColumn = unassignColumn;
  }

  /**
   * Remove a variable assignment
   */
  public void unassignVariable() {

    boolean unassigned = false;

    FileDefinitionBuilder fileDefinition = (FileDefinitionBuilder) instrumentFiles.get(unassignFile);

    if (null != fileDefinition) {

      if (fileDefinition.getRunTypeColumn() == unassignColumn) {
        fileDefinition.setRunTypeColumn(-1);
      } else {
        unassigned = sensorAssignments.removeAssignment(fileDefinition.getFileDescription(), unassignColumn);

        if (!unassigned) {
          unassigned = fileDefinition.removeAssignment(unassignColumn);
        }

        // If the run type column is no longer required, unassign it
        if (!fileDefinition.requiresRunTypeColumn(sensorAssignments)) {
          fileDefinition.setRunTypeColumn(-1);
        }
      }
    }
  }

  /**
   * Get the name of the file to be removed
   * @return The file name
   */
  public String getRemoveFileName() {
    return removeFileName;
  }

  /**
   * Set the name of the file to be removed
   * @param removeFileName The file name
   */
  public void setRemoveFileName(String removeFileName) {
    this.removeFileName = removeFileName;
  }

  /**
   * Remove a file from the instrument
   * @return Navigation to either the upload page (if all files have been removed), or the assignment page
   */
  @SuppressWarnings("unlikely-arg-type")
  public String removeFile() {
    String result;

    if (null != removeFileName) {
      instrumentFiles.remove(removeFileName);
      sensorAssignments.removeFileAssignments(removeFileName);
    }

    if (instrumentFiles.size() == 0) {
      result = NAV_UPLOAD_FILE;
    } else {
      result = NAV_ASSIGN_VARIABLES;
    }

    return result;
  }

  /**
   * Handle the Back button pressed on the File Upload page.
   * Navigates to the variable assignments page if files have
   * been uploaded, or to the instrument name.
   * @return The navigation
   */
  public String backFromFileUpload() {
    String result;

    if (instrumentFiles.size() == 0) {
      result = NAV_NAME;
    } else {
      result = NAV_ASSIGN_VARIABLES;
    }

    return result;
  }

  /**
   * Initialise the run types selection data and
   * navigate to the page.
   * @return The navigation to the run types selection page
   */
  public String goToRunTypes() {
    return NAV_RUN_TYPES;
  }

  /**
   * Go to the Other Info page
   * @return The navigation to the Other Info page
   */
  public String goToOtherInfo() {
    return NAV_OTHER_INFO;
  }

  /**
   * Get the file to which a run type is being assigned
   * @return The file
   */
  public int getRunTypeAssignFile() {
    return runTypeAssignFile;
  }

  /**
   * Set the file to which a run type is being assigned
   * @param runTypeAssignFile The file
   */
  public void setRunTypeAssignFile(int runTypeAssignFile) {
    this.runTypeAssignFile = runTypeAssignFile;
  }

  /**
   * Get the run type to be assigned to a Run Type Category
   * @return The run type
   */
  public String getRunTypeAssignName() {
    return runTypeAssignName;
  }

  /**
   * Set the run type to be assigned to a Run Type Category
   * @param runTypeAssignName The run type
   */
  public void setRunTypeAssignName(String runTypeAssignName) {
    this.runTypeAssignName = runTypeAssignName;
  }

  /**
   * Get the code of the Run Type Category that a run type is being assigned to
   * @return The Run Type Category code
   */
  public String getRunTypeAssignCode() {
    return runTypeAssignCode;
  }

  /**
   * Set the code of the Run Type Category that a run type is being assigned to
   * @param runTypeAssignCode The Run Type Category code
   */
  public void setRunTypeAssignCode(String runTypeAssignCode) {
    this.runTypeAssignCode = runTypeAssignCode;
  }

  /**
   * Get the name of the run type that the assigned run type is aliased to
   * @return The aliased run type
   */
  public String getRunTypeAssignAliasTo() {
    return runTypeAssignAliasTo;
  }

  /**
   * Get the name of the run type that the assigned run type is aliased to
   * @param runTypeAssignAliasTo The aliased run type
   */
  public void setRunTypeAssignAliasTo(String runTypeAssignAliasTo) {
    this.runTypeAssignAliasTo = runTypeAssignAliasTo;
  }

  /**
   * Assign a run type to a Run Type Category
   * @throws NoSuchCategoryException If the chosen category does not exist
   */
  public void assignRunTypeCategory() throws NoSuchCategoryException {
    FileDefinition file = instrumentFiles.get(runTypeAssignFile);

    RunTypeCategory category = null;
    if (null != runTypeAssignCode && runTypeAssignCode.length() > 0) {
      category = ResourceManager.getInstance().getRunTypeCategoryConfiguration().getCategory(runTypeAssignCode);

      if (category.equals(RunTypeCategory.ALIAS_CATEGORY)) {
        file.setRunTypeCategory(runTypeAssignName, runTypeAssignAliasTo);
      } else {
        file.setRunTypeCategory(runTypeAssignName, category);
      }
    }
  }

  /**
   * Get the details of the Run Type Category assignments as a JSON string
   * @return The Run Type Category assignments
   */
  public String getCategoryAssignments() {

    // Set up the category list
    List<RunTypeCategory> categories = ResourceManager.getInstance().getRunTypeCategoryConfiguration().getCategories(false, false);
    TreeMap<RunTypeCategory, Integer> assignedCategories = new TreeMap<RunTypeCategory, Integer>();

    for (RunTypeCategory category : categories) {
      assignedCategories.put(category, 0);
    }

    // Find all the assigned categories from each file
    for (FileDefinition file : instrumentFiles) {
      RunTypeAssignments fileRunTypes = file.getRunTypes();
      if (null != fileRunTypes) {
        for (RunTypeAssignment assignment : fileRunTypes.values()) {
          if (!assignment.isAlias()) {
            RunTypeCategory category = assignment.getCategory();
            if (!category.equals(RunTypeCategory.IGNORED_CATEGORY)) {
              assignedCategories.put(category, assignedCategories.get(category) + 1);
            }
          }
        }
      }
    }

    // Make the JSON output
    JSONArray json = new JSONArray();

    for (Map.Entry<RunTypeCategory, Integer> entry : assignedCategories.entrySet()) {
      JSONArray jsonEntry = new JSONArray();
      jsonEntry.put(entry.getKey().getName());
      jsonEntry.put(entry.getValue());
      jsonEntry.put(entry.getKey().getMinCount());

      json.put(jsonEntry);
    }

    return json.toString();
  }

  /**
   * Dummy set method to go with {@link #getCategoryAssignments()}. Does nothing.
   * @param dummy Dummy string
   */
  public void setCategoryAssignments(String dummy) {
    // Do nothing
  }

  /**
   * Get the run type assignments as a JSON string
   * @return The run type assignments
   */
  public String getRunTypeAssignments() {
    JSONArray json = new JSONArray();

    for (int i = 0; i < instrumentFiles.size(); i++) {
      FileDefinition file = instrumentFiles.get(0);
      if (file.getRunTypeColumn() != -1) {
        RunTypeAssignments assignments = file.getRunTypes();

        JSONObject fileAssignments = new JSONObject();

        fileAssignments.put("index", i);

        JSONArray jsonAssignments = new JSONArray();

        for (RunTypeAssignment assignment : assignments.values()) {
          JSONObject jsonAssignment = new JSONObject();
          jsonAssignment.put("runType", assignment.getRunType());

          RunTypeCategory category = assignment.getCategory();
          if (null == category) {
            jsonAssignment.put("category", JSONObject.NULL);
            jsonAssignment.put("aliasTo", assignment.getAliasTo());
          } else {
            jsonAssignment.put("category", assignment.getCategory().getCode());
            jsonAssignment.put("aliasTo", JSONObject.NULL);
          }

          jsonAssignments.put(jsonAssignment);
        }

        fileAssignments.put("assignments", jsonAssignments);
        json.put(fileAssignments);
      }
    }

    return json.toString();
  }

  /**
   * Dummy set method to go with {@link #getRunTypeAssignments()}. Does nothing.
   * @param dummy Dummy string
   */
  public void setRunTypeAssignments(String dummy) {
    // Do nothing
  }

  /**
   * Reset all data regarding run type assignments
   */
  private void clearRunTypeAssignments() {
    runTypeAssignName = null;
    runTypeAssignCode = null;
  }

  /**
   * Get the pre-flushing time
   * @return The pre-flushing time
   */
  public int getPreFlushingTime() {
    return preFlushingTime;
  }

  /**
   * Get the pre-flushing time
   * @param preFlushingTime The pre-flushing time
   */
  public void setPreFlushingTime(int preFlushingTime) {
    this.preFlushingTime = preFlushingTime;
  }

  /**
   * Get the post-flushing time
   * @return The post-flushing time
   */
  public int getPostFlushingTime() {
    return postFlushingTime;
  }

  /**
   * Get the post-flushing time
   * @param postFlushingTime The post-flushing time
   */
  public void setPostFlushingTime(int postFlushingTime) {
    this.postFlushingTime = postFlushingTime;
  }

  /**
   * Get the minimum water flow
   * @return The minimum water flow
   */
  public int getMinimumWaterFlow() {
    return minimumWaterFlow;
  }

  /**
   * Set the minimum water flow
   * @param minimumWaterFlow The minimum water flow
   */
  public void setMinimumWaterFlow(int minimumWaterFlow) {
    this.minimumWaterFlow = minimumWaterFlow;
  }

  /**
   * Get the averaging mode
   * @return The averaging mode
   */
  public int getAveragingMode() {
    return averagingMode;
  }

  /**
   * Set the averaging mode
   * @param averagingMode The averaging mode
   */
  public void setAveragingMode(int averagingMode) {
    this.averagingMode = averagingMode;
  }

  /**
   * @return the platformCode
   */
  public String getPlatformCode() {
    return platformCode;
  }

  /**
   * @param platformCode the platformCode to set
   */
  public void setPlatformCode(String platformCode) {
    this.platformCode = platformCode;
  }

  /**
   * Store the instrument
   * @return Navigation to the instrument list
   * @throws InstrumentException If the instrument object is invalid
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws IOException If certain data cannot be converted for storage in the database
   */
  public String saveInstrument() throws MissingParamException, InstrumentException, DatabaseException, IOException {

    try {

      /*
       * Adding a Run Type sensor assignment
       */
      SensorAssignment sensorAssignment = new SensorAssignment(getRunTypeFile(),
          getRunTypeColumn(), "Run Type", false, true, false, null);
      HashSet<SensorAssignment> hashSet = new HashSet<SensorAssignment>();
      hashSet.add(sensorAssignment);
      sensorAssignments.put(SensorType.getRunTypeSensorType(), hashSet);
      Instrument instrument = new Instrument(getUser(), instrumentName, instrumentFiles,
        sensorAssignments, preFlushingTime, postFlushingTime, minimumWaterFlow, averagingMode,
        platformCode);
      InstrumentDB.storeInstrument(getDataSource(), instrument);
      setCurrentInstrumentId(instrument.getDatabaseId());

      // Reinitialise beans to update their instrument lists
      instrumentListBean.init();
      dataFilesBean.initialise();
      dataSetsBean.initialise();
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }

    return NAV_INSTRUMENT_LIST;
  }

  /**
   * Set up the reference to the Instrument List Bean
   * @param instrumentListBean The instrument list bean
   */
    public void setInstrumentListBean(InstrumentListBean instrumentListBean) {
        this.instrumentListBean = instrumentListBean;
    }

  /**
   * Set up the reference to the Data Files Bean
   * @param dataFilesBean The data files bean
   */
    public void setDataFilesBean(DataFilesBean dataFilesBean) {
        this.dataFilesBean = dataFilesBean;
    }

  /**
   * Set up the reference to the Data Sets Bean
   * @param dataSetsBean The data sets bean
   */
    public void setDataSetsBean(DataSetsBean dataSetsBean) {
        this.dataSetsBean = dataSetsBean;
    }

    /**
     * Get the file for which a Run Type column is being assigned
     * @return The Run Type file
     */
    public String getRunTypeFile() {
      return runTypeFile;
    }

    /**
     * Set the file for which a Run Type column is being assigned
     * @param runTypeFile The Run Type file
     */
    public void setRunTypeFile(String runTypeFile) {
      this.runTypeFile = runTypeFile;
    }

    /**
     * Get the index of the Run Type column being assigned
     * @return The Run Type column index
     */
    public int getRunTypeColumn() {
      return runTypeColumn;
    }

    /**
     * Set the index of the Run Type column being assigned
     * @param runTypeColumn The Run Type column index
     */
    public void setRunTypeColumn(int runTypeColumn) {
      this.runTypeColumn = runTypeColumn;
    }

    /**
     * Set the Run Type column for a file
     */
    public void assignRunType() {
      FileDefinition file = instrumentFiles.get(runTypeFile);
      file.setRunTypeColumn(runTypeColumn);
    }

    /**
     * Get the available averaging modes
     * @return The averaging modes
     */
    public Map<String, Integer> getAveragingModes() {
      return DataSetRawData.averagingModes();
    }
}
