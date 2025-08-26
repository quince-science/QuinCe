package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument;

import java.io.IOException;
import java.sql.Connection;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.TreeNode;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

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
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorConfigurationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroups;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupsException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
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
 */
@ManagedBean
@SessionScoped
public class NewInstrumentBean extends FileUploadBean {

  /**
   * Navigation to start definition of a new instrument
   */
  private static final String NAV_NAME = "name";

  /**
   * Navigation to choose variables
   */
  private static final String NAV_VARIABLES = "variables";

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
   * Navigation to the general info page
   */
  private static final String NAV_GENERAL_INFO = "general_info";

  /**
   * Navigation to the variable info page
   */
  private static final String NAV_VARIABLE_INFO = "variable_info";

  /**
   * Navigation to the Sensor Groups page
   */
  private static final String NAV_SENSOR_GROUPS = "sensor_groups";

  /**
   * Date/Time formatter for previewing extracted dates
   */
  private static final DateTimeFormatter PREVIEW_DATE_TIME_FORMATTER = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm:ss");

  private static Gson gson = null;

  private static List<PositionFormatEntry> lonFormats = null;

  private static List<PositionFormatEntry> latFormats = null;

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
   * The data for the assignments tree in {@code assign_variables.xhtml}.
   */
  private AssignmentsTree assignmentsTree;

  /**
   * The {@link SensorAssignment}s grouped by their physical locations.
   */
  private SensorGroups sensorGroups;

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
   * Sensor assignment - sensor name
   */
  private String sensorAssignmentName = null;

  /**
   * The name of the sensor being assigned
   */
  private Long sensorAssignmentSensorType = null;

  /**
   * Sensor assignment - the answer to the Depends Question
   *
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
   * The coordinate (longitude or latitude) for which the hemisphere is being
   * set
   */
  private String hemisphereCoordinate = null;

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
  private int dateTimeVariable = -1;

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
   * The code of the Run Type Category that the run type is being assigned to
   */
  private long runTypeAssignCode = RunTypeCategory.IGNORED_TYPE;

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
   * Indicates whether or not intake depth is specified for the instrument
   */
  private boolean hasDepth = true;

  /**
   * The instrument/intake depth
   */
  private int depth;

  /**
   * The platform name.
   */
  private String platformName = null;

  /**
   * The platform code.
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
   * The variables that the new instrument will measure
   */
  private List<Variable> instrumentVariables;

  /**
   * Indicates whether or not the instrument has a fixed position (i.e. does not
   * provide GPS data).
   */
  private boolean fixedPosition = false;

  /**
   * The fixed longitude.
   */
  private double longitude;

  /**
   * The fixed latitude.
   */
  private double latitude;

  /**
   * The file description of a file that is to be renamed.
   */
  private String renameOldFile;

  /**
   * The new file description for a file being renamed.
   */
  private String renameNewFile;

  /**
   * The ID of the sensor type for which an assignment is to be removed.
   */
  private long removeAssignmentSensorType;

  /**
   * The data file for which an assignment is being removed.
   */
  private String removeAssignmentDataFile;

  /**
   * The column for which an assignment is being removed.
   */
  private int removeAssignmentColumn;

  /**
   * The name of the sensor group being renamed.
   */
  private String groupRenameFrom;

  /**
   * The new name for the sensor group.
   */
  private String groupRenameTo;

  /**
   * The name of the group that an added sensor group will be after.
   */
  private String addGroupAfter;

  /**
   * The name for a new sensor group.
   */
  private String addGroupName;

  /**
   * The name of the sensor group to be deleted.
   */
  private String deleteGroupName;

  /**
   * The sensor being moved to another group.
   */
  private String moveSensorName;

  /**
   * The group to which a sensor is being moved.
   */
  private String moveSensorGroup;

  /**
   * The name of the group whose link is being set.
   */
  private String groupLinkGroup;

  /**
   * The sensor that is being set as a group link.
   */
  private String groupLinkSensor;

  /**
   * The link direction ({@link SensorGroups#PREVIOUS}
   * {@link SensorGroups#NEXT}).
   */
  private int groupLinkDirection;

  /**
   * Existing platform names/codes
   */
  private TreeMap<String, String> existingPlatforms;

  /**
   * Record the file which has just been updated.
   */
  private String updatedFile = null;

  /**
   * Indicates whether QuinCe has guessed any column assignments.
   */
  private Set<String> sensorAssignmentsGuessed = new HashSet<String>();

  /**
   * Indicates whether QuinCe has attempted to guess any run types.
   */
  private boolean runTypesGuessed = false;

  /**
   * Begin a new instrument definition
   *
   * @return The navigation to the start page
   */
  public String start() throws Exception {
    clearAllData();
    existingPlatforms = InstrumentDB.getPlatforms(getDataSource(), getUser());
    return NAV_NAME;
  }

  /**
   * Cancel the current instrument definition
   *
   * @return Navigation to the instrument list
   */
  public String cancel() throws Exception {
    clearAllData();
    return NAV_INSTRUMENT_LIST;
  }

  /**
   * Navigate to the Name page
   *
   * @return Navigation to the name page
   */
  public String goToName() {
    clearFile();
    return NAV_NAME;
  }

  public String goToVariables() {
    return NAV_VARIABLES;
  }

  /**
   * Navigate to the files step.
   * <p>
   * The page we navigate to depends on the current status of the instrument.
   * <p>
   * <p>
   * If no files have been added, we create a new empty file and go to the
   * upload page. Otherwise, we go to the variable assignment page.
   * </p>
   *
   * @return Navigation to the files
   * @throws InstrumentFileExistsException
   *           If the default instrument file has already been added.
   */
  public String goToFiles() throws InstrumentFileExistsException {
    String result;

    if (instrumentFiles.size() == 0) {
      currentInstrumentFile = new FileDefinitionBuilder(instrumentFiles);

      result = NAV_UPLOAD_FILE;
    } else {
      if (null == currentInstrumentFile) {
        currentInstrumentFile = FileDefinitionBuilder
          .copy(instrumentFiles.get(0));
      }
      result = NAV_ASSIGN_VARIABLES;
    }

    return result;
  }

  /**
   * Direct navigation to the Assign Variables page
   *
   * @return Navigation to the Assign Variables page
   */
  public String goToAssignVariables() {
    return NAV_ASSIGN_VARIABLES;
  }

  /**
   * Begin the process of adding a new file to the instrument.
   *
   * @return The navigation to the file upload.
   * @throws DateTimeSpecificationException.
   */
  public String addFile() throws DateTimeSpecificationException {
    currentInstrumentFile = new FileDefinitionBuilder(instrumentFiles);
    return NAV_UPLOAD_FILE;
  }

  @Override
  protected String getFormName() {
    return "newInstrumentForm";
  }

  /**
   * Store the uploaded data in the current instrument file. Detailed processing
   * will be triggered by the source page calling
   * {@link FileDefinitionBuilder#guessFileLayout}.
   */
  @Override
  public void processUploadedFile() {
    currentInstrumentFile.setFileContents(getFileLines());
  }

  /**
   * Clear all data from the bean ready for a new instrument to be defined
   */
  private void clearAllData() throws Exception {

    Connection conn = null;

    try {
      conn = getDataSource().getConnection();

      instrumentName = null;
      instrumentFiles = new NewInstrumentFileSet();
      sensorAssignments = null;
      assignmentsTree = null;
      sensorGroups = null;
      instrumentVariables = null;
      preFlushingTime = 0;
      postFlushingTime = 0;
      depth = 0;
      platformName = "";
      platformCode = "";
      fixedPosition = false;
      longitude = 0;
      latitude = 0;

      resetSensorAssignmentValues();
      resetPositionAssignmentValues();
      resetDateTimeAssignmentValues();
      clearRunTypeAssignments();
      clearFile();
    } catch (Exception e) {
      throw e;
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Get the name of the new instrument
   *
   * @return The instrument name
   */
  public String getInstrumentName() {
    return instrumentName;
  }

  public String getInstrumentDisplayName() {
    return platformName + ";" + instrumentName;
  }

  /**
   * Set the name of the new instrument
   *
   * @param instrumentName
   *          The instrument name
   */
  public void setInstrumentName(String instrumentName) {
    this.instrumentName = instrumentName;
  }

  public List<Variable> getInstrumentVariables() {
    return instrumentVariables;
  }

  public void setInstrumentVariables(List<Long> instrumentVariables) {

    try {
      SensorsConfiguration sensorConfig = ResourceManager.getInstance()
        .getSensorsConfiguration();

      this.instrumentVariables = sensorConfig
        .getInstrumentVariables(instrumentVariables);

      sensorAssignments = new SensorAssignments(getDataSource(),
        instrumentVariables);
      assignmentsTree = new AssignmentsTree(this.instrumentVariables,
        sensorAssignments, !fixedPosition);
      sensorGroups = new SensorGroups();
      this.instrumentVariables.forEach(v -> v.getAttributes().reset());
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
    }
  }

  public Map<Long, String> getAllVariables() {
    Map<Long, String> variables = new HashMap<Long, String>();

    try {
      for (Variable variable : InstrumentDB.getAllVariables(getDataSource())) {
        variables.put(variable.getId(), variable.getName());
      }
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
    }

    return variables;
  }

  /**
   * Get the instrument file that is currently being worked on
   *
   * @return The current instrument file
   */
  public FileDefinitionBuilder getCurrentInstrumentFile() {
    return currentInstrumentFile;
  }

  /**
   * Retrieve the full set of instrument files
   *
   * @return The instrument files
   */
  public NewInstrumentFileSet getInstrumentFiles() {
    return instrumentFiles;
  }

  /**
   * Determines whether or not the file set contains more than one file
   *
   * @return {@code true} if more than one file is in the set; {@code false} if
   *         there are zero or one files
   */
  public boolean getHasMultipleFiles() {
    return (instrumentFiles.size() > 1);
  }

  /**
   * Add the current instrument file to the file set (or update it) and clear
   * its status as 'current'. Then navigate to the variable assignment page.
   *
   * @return The navigation to the variable assignment page
   * @throws DateTimeSpecificationException
   * @throws SensorGroupsException
   * @throws SensorConfigurationException
   * @throws SensorTypeNotFoundException
   * @throws SensorAssignmentException
   */
  public String useFile() throws DateTimeSpecificationException,
    SensorAssignmentException, SensorTypeNotFoundException,
    SensorConfigurationException, SensorGroupsException {
    instrumentFiles.add(currentInstrumentFile);
    assignmentsTree.addFile(currentInstrumentFile);
    updatedFile = currentInstrumentFile.getFileDescription();

    autoAssignColumns(currentInstrumentFile);

    clearFile();
    return NAV_ASSIGN_VARIABLES;
  }

  /**
   * Try to automatically add {@link SensorAssignments} from the specified file.
   *
   * <p>
   * The column headings for the file (if it has any) are examined in turn, and
   * one of the following actions taken, starting at the first and continuing
   * until a {@link SensorAssignment} is created.
   * </p>
   * <ol>
   * <li>If there is an existing {@link Instrument} owned by the current
   * {@link User} that has the same platform name and code, check its
   * {@link SensorAssignments} to see if the column name is used. If so, create
   * the same {@link SensorAssignment}.</li>
   * <li>Check {@link SensorType#getSourceColumns()} for each {@link SensorType}
   * in the {@link #assignmentsTree} and see if the heading matches any. If so,
   * create a {@link SensorAssignment}.</li>
   * </ol>
   * <p>
   * If the supplied file does not have column headings, no action is taken.
   * </p>
   *
   * @param file
   *          The file whose columns are to be examined.
   * @throws SensorConfigurationException
   * @throws SensorTypeNotFoundException
   * @throws SensorAssignmentException
   * @throws SensorGroupsException
   */
  private void autoAssignColumns(FileDefinitionBuilder file)
    throws SensorAssignmentException, SensorTypeNotFoundException,
    SensorConfigurationException, SensorGroupsException {

    List<Instrument> previousInstruments = getPreviousInstruments();
    for (FileColumn column : file.getFileColumns()) {

      boolean assignmentMade = false;

      for (Instrument instrument : previousInstruments) {
        SensorAssignment existingAssignment = instrument.getSensorAssignments()
          .getSingleAssignment(column.getName());

        if (null != existingAssignment) {
          SensorType existingAssignmentType = existingAssignment
            .getSensorType();
          if (null != instrument.getSensorAssignments()
            .get(existingAssignmentType)) {

            try {
              autoAssignColumn(file, column,
                existingAssignment.getSensorType());
              assignmentMade = true;
            } catch (SensorAssignmentException e) {
              // If an assignment fails, we just carry on.
              // The user will have to specify it.
            }
          }
          break;
        }
      }

      if (!assignmentMade) {
        for (SensorType sensorType : assignmentsTree.getSensorTypes()) {
          if (sensorType.getSourceColumns()
            .contains(column.getName().toLowerCase())) {
            autoAssignColumn(file, column, sensorType);
            assignmentMade = true;
            break;
          }
        }
      }
    }
  }

  /**
   * Get the previously defined instruments with the same platform name and code
   * as the instrument being created, ordered by the most recently created
   * first.
   *
   * @return The previous instruments from the same platform.
   */
  private List<Instrument> getPreviousInstruments() {
    return Instrument.filterByPlatform(getInstruments(), platformName,
      platformCode, -1L);
  }

  /**
   * Action method for {@link #autoAssignColumns(FileDefinitionBuilder)}.
   *
   * @param file
   * @param column
   * @param sensorType
   * @throws SensorAssignmentException
   * @throws SensorTypeNotFoundException
   * @throws SensorConfigurationException
   * @throws SensorGroupsException
   */
  private void autoAssignColumn(FileDefinitionBuilder file, FileColumn column,
    SensorType sensorType)
    throws SensorAssignmentException, SensorTypeNotFoundException,
    SensorConfigurationException, SensorGroupsException {

    if (!sensorAssignments.isAssigned(sensorType)) {
      if (sensorType.equals(SensorType.RUN_TYPE_SENSOR_TYPE)) {
        runTypeFile = file.getFileDescription();
        runTypeColumn = column.getIndex();
        assignRunType();
      } else {

        sensorAssignmentFile = file.getFileDescription();
        sensorAssignmentColumn = column.getIndex();
        sensorAssignmentSensorType = sensorType.getId();
        sensorAssignmentName = column.getName();
        sensorAssignmentPrimary = true;
        sensorAssignmentDependsQuestionAnswer = false;
        sensorAssignmentMissingValue = null;
        storeSensorAssignment();
      }

      sensorAssignmentsGuessed.add(file.getFileDescription());
    }
  }

  public boolean getSensorAssignmentGuessed() {
    return sensorAssignmentsGuessed.size() > 0;
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
   * Get the sensor assignment file
   *
   * @return The file
   */
  public String getSensorAssignmentFile() {
    return sensorAssignmentFile;
  }

  /**
   * Set the sensor assignment file
   *
   * @param sensorAssignmentFile
   *          The file
   */
  public void setSensorAssignmentFile(String sensorAssignmentFile) {
    this.sensorAssignmentFile = sensorAssignmentFile;
  }

  /**
   * Get the sensor assignment column index
   *
   * @return The column index
   */
  public int getSensorAssignmentColumn() {
    return sensorAssignmentColumn;
  }

  /**
   * Set the sensor assignment column index
   *
   * @param sensorAssignmentColumn
   *          The column index
   */
  public void setSensorAssignmentColumn(int sensorAssignmentColumn) {
    this.sensorAssignmentColumn = sensorAssignmentColumn;
  }

  /**
   * Get the name of the assigned sensor
   *
   * @return The sensor name
   */
  public String getSensorAssignmentName() {
    return sensorAssignmentName;
  }

  /**
   * Set the name of the assigned sensor
   *
   * @param sensorAssignmentName
   *          The sensor name
   */
  public void setSensorAssignmentName(String sensorAssignmentName) {
    this.sensorAssignmentName = sensorAssignmentName;
  }

  /**
   * Get the name of the sensor type being assigned
   *
   * @return The sensor type
   */
  public Long getSensorAssignmentSensorType() {
    return sensorAssignmentSensorType;
  }

  /**
   * Set the name of the sensor type being assigned
   *
   * @param sensorAssignmentSensorType
   *          The sensor type
   */
  public void setSensorAssignmentSensorType(Long sensorAssignmentSensorType) {
    this.sensorAssignmentSensorType = sensorAssignmentSensorType;
  }

  /**
   * Get the answer to the sensor assignment's Depends Question
   *
   * @return The answer to the Depends Question
   * @see SensorType#getDependsQuestion()
   */
  public boolean getSensorAssignmentDependsQuestionAnswer() {
    return sensorAssignmentDependsQuestionAnswer;
  }

  /**
   * Set the answer to the sensor assignment's Depends Question
   *
   * @param sensorAssignmentDependsQuestionAnswer
   *          The answer to the Depends Question
   * @see SensorType#getDependsQuestion()
   */
  public void setSensorAssignmentDependsQuestionAnswer(
    boolean sensorAssignmentDependsQuestionAnswer) {
    this.sensorAssignmentDependsQuestionAnswer = sensorAssignmentDependsQuestionAnswer;
  }

  /**
   * Get the flag indicating whether the assigned sensor is primary or fallback
   *
   * @return The primary sensor flag
   */
  public boolean getSensorAssignmentPrimary() {
    return sensorAssignmentPrimary;
  }

  /**
   * Set the flag indicating whether the assigned sensor is primary or fallback
   *
   * @param sensorAssignmentPrimary
   *          The primary sensor flag
   */
  public void setSensorAssignmentPrimary(boolean sensorAssignmentPrimary) {
    this.sensorAssignmentPrimary = sensorAssignmentPrimary;
  }

  /**
   * Get the 'missing value' value for the current sensor assignment
   *
   * @return The missing value
   */
  public String getSensorAssignmentMissingValue() {
    return sensorAssignmentMissingValue;
  }

  /**
   * Set the 'missing value' value for the current sensor assignment
   *
   * @param sensorAssignmentMissinngValue
   *          The missing value
   */
  public void setSensorAssignmentMissingValue(
    String sensorAssignmentMissinngValue) {
    this.sensorAssignmentMissingValue = sensorAssignmentMissinngValue;
  }

  /**
   * Add a new assignment to the sensor assignments
   *
   * @throws SensorTypeNotFoundException
   * @throws SensorAssignmentException
   * @throws SensorGroupsException
   * @throws SensorConfigurationException
   *
   * @throws Exception
   *           If any errors occur
   */
  public void storeSensorAssignment()
    throws SensorTypeNotFoundException, SensorAssignmentException,
    SensorGroupsException, SensorConfigurationException {

    SensorType sensorType = ResourceManager.getInstance()
      .getSensorsConfiguration().getSensorType(sensorAssignmentSensorType);

    boolean dependsQuestionAnswer = this.sensorAssignmentDependsQuestionAnswer;

    Boolean fixedAnswer = getFixedDependsQuestionAnswer(sensorType.getId());
    if (null != fixedAnswer) {
      dependsQuestionAnswer = fixedAnswer.booleanValue();
    }

    SensorAssignment assignment = new SensorAssignment(sensorAssignmentFile,
      sensorAssignmentColumn, sensorType, sensorAssignmentName,
      sensorAssignmentPrimary, dependsQuestionAnswer,
      sensorAssignmentMissingValue);

    sensorAssignments.addAssignment(assignment);
    sensorGroups.addAssignment(assignment);
    assignmentsTree.addAssignment(assignment);

    // Reset the assign dialog values, because it's so damn hard to do in
    // Javascript
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
    sensorAssignmentDependsQuestionAnswer = false;
    sensorAssignmentMissingValue = null;
  }

  /**
   * Get the file for which the longitude is being set
   *
   * @return The longitude file
   */
  public String getLongitudeFile() {
    return longitudeFile;
  }

  /**
   * Set the file for which the longitude is being set
   *
   * @param longitudeFile
   *          The longitude file
   */
  public void setLongitudeFile(String longitudeFile) {
    this.longitudeFile = longitudeFile;
  }

  /**
   * Get the longitude column index
   *
   * @return The longitude column index
   */
  public int getLongitudeColumn() {
    return longitudeColumn;
  }

  /**
   * Set the longitude column index
   *
   * @param longitudeColumn
   *          The longitude column index
   */
  public void setLongitudeColumn(int longitudeColumn) {
    this.longitudeColumn = longitudeColumn;
  }

  /**
   * Get the longitude format
   *
   * @return The longitude format
   */
  public int getLongitudeFormat() {
    return longitudeFormat;
  }

  /**
   * Set the longitude format
   *
   * @param longitudeFormat
   *          The longitude format
   */
  public void setLongitudeFormat(int longitudeFormat) {
    this.longitudeFormat = longitudeFormat;
  }

  /**
   * Set the longitude column and format for a file
   *
   * @throws InvalidPositionFormatException
   *           If the format is invalid
   */
  public void assignLongitude() throws InvalidPositionFormatException {
    FileDefinitionBuilder file = (FileDefinitionBuilder) instrumentFiles
      .get(longitudeFile);
    file.getLongitudeSpecification().setValueColumn(longitudeColumn);
    file.getLongitudeSpecification().setFormat(longitudeFormat);
    if (longitudeFormat != LongitudeSpecification.FORMAT_0_180) {
      file.getLongitudeSpecification().setHemisphereColumn(-1);
    }

    assignmentsTree.updatePositionNodes("Longitude");
    resetPositionAssignmentValues();
  }

  /**
   * Get the file for which the latitude is being set
   *
   * @return The latitude file
   */
  public String getLatitudeFile() {
    return latitudeFile;
  }

  /**
   * Set the file for which the latitude is being set
   *
   * @param latitudeFile
   *          The latitude file
   */
  public void setLatitudeFile(String latitudeFile) {
    this.latitudeFile = latitudeFile;
  }

  /**
   * Get the latitude column index
   *
   * @return The latitude column index
   */
  public int getLatitudeColumn() {
    return latitudeColumn;
  }

  /**
   * Set the latitude column index
   *
   * @param latitudeColumn
   *          The latitude column index
   */
  public void setLatitudeColumn(int latitudeColumn) {
    this.latitudeColumn = latitudeColumn;
  }

  /**
   * Get the latitude format
   *
   * @return The latitude format
   */
  public int getLatitudeFormat() {
    return latitudeFormat;
  }

  /**
   * Set the latitude format
   *
   * @param latitudeFormat
   *          The latitude format
   */
  public void setLatitudeFormat(int latitudeFormat) {
    this.latitudeFormat = latitudeFormat;
  }

  /**
   * Set the latitude column and format for a file
   *
   * @throws InvalidPositionFormatException
   *           If the format is invalid
   */
  public void assignLatitude() throws InvalidPositionFormatException {
    FileDefinitionBuilder file = (FileDefinitionBuilder) instrumentFiles
      .get(latitudeFile);
    file.getLatitudeSpecification().setValueColumn(latitudeColumn);
    file.getLatitudeSpecification().setFormat(latitudeFormat);
    if (latitudeFormat != LatitudeSpecification.FORMAT_0_90) {
      file.getLatitudeSpecification().setHemisphereColumn(-1);
    }

    assignmentsTree.updatePositionNodes("Latitude");
    resetPositionAssignmentValues();
  }

  /**
   * Get the file for which the hemisphere is being set
   *
   * @return The hemisphere file
   */
  public String getHemisphereFile() {
    return hemisphereFile;
  }

  /**
   * Set the file for which the hemisphere is being set
   *
   * @param hemisphereFile
   *          The hemisphere file
   */
  public void setHemisphereFile(String hemisphereFile) {
    this.hemisphereFile = hemisphereFile;
  }

  /**
   * Get the hemisphere column index
   *
   * @return The hemisphere column index
   */
  public int getHemisphereColumn() {
    return hemisphereColumn;
  }

  /**
   * Set the hemisphere column index
   *
   * @param hemisphereColumn
   *          The hemisphere column index
   */
  public void setHemisphereColumn(int hemisphereColumn) {
    this.hemisphereColumn = hemisphereColumn;
  }

  /**
   * Get the coordinate for which the hemisphere is being set
   *
   * @return The hemipshere coordinate
   */
  public String getHemisphereCoordinate() {
    return hemisphereCoordinate;
  }

  /**
   * Set the coordinate for which the hemisphere is being set
   *
   * @param hemisphereCoordinate
   *          The hemisphere coordinate
   */
  public void setHemisphereCoordinate(String hemisphereCoordinate) {
    this.hemisphereCoordinate = hemisphereCoordinate;
  }

  /**
   * Assign the hemisphere column for a coordinate
   */
  public void assignHemisphere() {
    FileDefinitionBuilder file = (FileDefinitionBuilder) instrumentFiles
      .get(hemisphereFile);

    PositionSpecification posSpec;
    String expandType;

    if (hemisphereCoordinate.equals("Longitude")) {
      posSpec = file.getLongitudeSpecification();
      expandType = "Longitude";
    } else {
      posSpec = file.getLatitudeSpecification();
      expandType = "Latitude";
    }

    posSpec.setHemisphereColumn(hemisphereColumn);

    assignmentsTree.updatePositionNodes(expandType);
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
    hemisphereCoordinate = null;
    hemisphereColumn = -1;
  }

  /**
   * Clear all date/time assignment data
   */
  private void resetDateTimeAssignmentValues() {
    dateTimeFile = null;
    dateTimeColumn = -1;
    dateTimeVariable = -1;
    dateFormat = null;
    startTimePrefix = null;
    startTimeSuffix = null;
    startTimeFormat = "MMM dd YYYY HH:MM:SS";
  }

  /**
   * Get the file for which a date/time variable is being assigned
   *
   * @return The file
   */
  public String getDateTimeFile() {
    return dateTimeFile;
  }

  /**
   * Set the file for which a date/time variable is being assigned
   *
   * @param dateTimeFile
   *          The file
   */
  public void setDateTimeFile(String dateTimeFile) {
    this.dateTimeFile = dateTimeFile;
  }

  /**
   * Get the column index that is being assigned to a date/time variable
   *
   * @return The column index
   */
  public int getDateTimeColumn() {
    return dateTimeColumn;
  }

  /**
   * Set the column index that is being assigned to a date/time variable
   *
   * @param dateTimeColumn
   *          The column index
   */
  public void setDateTimeColumn(int dateTimeColumn) {
    this.dateTimeColumn = dateTimeColumn;
  }

  /**
   * Get the name of the date/time variable being assigned
   *
   * @return The variable name
   */
  public int getDateTimeVariable() {
    return dateTimeVariable;
  }

  /**
   * Set the name of the date/time variable being assigned
   *
   * @param dateTimeVariable
   *          The variable name
   */
  public void setDateTimeVariable(int dateTimeVariable) {
    this.dateTimeVariable = dateTimeVariable;
  }

  /**
   * Get the format of the date/time string
   *
   * @return The format
   */
  public String getDateTimeFormat() {
    return dateTimeFormat;
  }

  /**
   * Set the format of the date/time string
   *
   * @param dateTimeFormat
   *          The format
   */
  public void setDateTimeFormat(String dateTimeFormat) {
    this.dateTimeFormat = dateTimeFormat;
  }

  /**
   * Get the format of the date string
   *
   * @return The format
   */
  public String getDateFormat() {
    return dateFormat;
  }

  /**
   * Set the format of the date string
   *
   * @param dateFormat
   *          The format
   */
  public void setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  /**
   * Get the format of the time string
   *
   * @return The format
   */
  public String getTimeFormat() {
    return timeFormat;
  }

  /**
   * Set the format of the time string
   *
   * @param timeFormat
   *          The format
   */
  public void setTimeFormat(String timeFormat) {
    this.timeFormat = timeFormat;
  }

  /**
   * Assign a date/time variable
   *
   * @throws DateTimeSpecificationException
   *           If the assignment cannot be made
   */
  public void assignDateTime() throws DateTimeSpecificationException {
    DateTimeSpecification dateTimeSpec = instrumentFiles.get(dateTimeFile)
      .getDateTimeSpecification();

    switch (dateTimeVariable) {
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
      dateTimeSpec.assignHoursFromStart(dateTimeColumn, startTimePrefix,
        startTimeSuffix, startTimeFormat);
      break;
    }
    case DateTimeSpecification.SECONDS_FROM_START: {
      dateTimeSpec.assignSecondsFromStart(dateTimeColumn, startTimePrefix,
        startTimeSuffix, startTimeFormat);
      break;
    }
    default: {
      dateTimeSpec.assign(dateTimeVariable, dateTimeColumn, null);
      break;
    }
    }

    assignmentsTree
      .setDateTimeAssignment(instrumentFiles.getByDescription(dateTimeFile));

    resetDateTimeAssignmentValues();
  }

  /**
   * Get the start time prefix
   *
   * @return The start time prefix
   */
  public String getStartTimePrefix() {
    return startTimePrefix;
  }

  /**
   * Set the start time prefix
   *
   * @param startTimePrefix
   *          The start time prefix
   */
  public void setStartTimePrefix(String startTimePrefix) {
    this.startTimePrefix = startTimePrefix;
  }

  /**
   * Get the start time suffix
   *
   * @return The start time suffix
   */
  public String getStartTimeSuffix() {
    return startTimeSuffix;
  }

  /**
   * Set the start time suffix
   *
   * @param startTimeSuffix
   *          The start time suffix
   */
  public void setStartTimeSuffix(String startTimeSuffix) {
    this.startTimeSuffix = startTimeSuffix;
  }

  /**
   * Get the start time format
   *
   * @return The start time format
   */
  public String getStartTimeFormat() {
    return startTimeFormat;
  }

  /**
   * Set the start time format
   *
   * @param startTimeFormat
   *          The start time format
   */
  public void setStartTimeFormat(String startTimeFormat) {
    this.startTimeFormat = startTimeFormat;
  }

  /**
   * Get the start time line extracted from the header
   *
   * @return The start time line
   */
  public String getStartTimeLine() {
    return startTimeLine;
  }

  /**
   * Dummy method for setting start time line - does nothing
   *
   * @param startTimeLine
   *          The start time line (ignored)
   */
  public void setStartTimeLine(String startTimeLine) {
    // Do nothing
  }

  /**
   * Get the start time extracted from the header
   *
   * @return The start time
   */
  public String getStartTimeDate() {
    return startTimeDate;
  }

  /**
   * Dummy method for setting start time date - does nothing
   *
   * @param startTimeDate
   *          The start time date (ignored)
   */
  public void setStartTimeDate(String startTimeDate) {
    // Do nothing
  }

  /**
   * Extract the start time from a file header
   *
   * @throws HighlightedStringException
   *           If the highlighted string cannot be created
   */
  public void extractStartTime() throws HighlightedStringException {
    FileDefinitionBuilder fileDefinition = (FileDefinitionBuilder) instrumentFiles
      .get(dateTimeFile);

    HighlightedString headerLine = fileDefinition.getHeaderLine(startTimePrefix,
      startTimeSuffix);
    if (null == headerLine) {
      startTimeLine = null;
      startTimeDate = null;
    } else {
      startTimeLine = headerLine.getJson();

      try {
        String headerDateString = headerLine.getHighlightedPortion();
        LocalDateTime headerDate = LocalDateTime.parse(headerDateString,
          DateTimeFormatter.ofPattern(startTimeFormat));
        startTimeDate = PREVIEW_DATE_TIME_FORMATTER.format(headerDate);
      } catch (DateTimeException e) {
        startTimeDate = null;
      }
    }
  }

  /**
   * Get the name of the file to be removed
   *
   * @return The file name
   */
  public String getRemoveFileName() {
    return removeFileName;
  }

  /**
   * Set the name of the file to be removed
   *
   * @param removeFileName
   *          The file name
   */
  public void setRemoveFileName(String removeFileName) {
    this.removeFileName = removeFileName;
  }

  /**
   * Remove a file from the instrument
   *
   * @return Navigation to either the upload page (if all files have been
   *         removed), or the assignment page
   * @throws Exception
   */
  public String removeFile() throws Exception {

    try {
      String result;

      if (null != removeFileName) {
        instrumentFiles.remove(removeFileName);
        sensorGroups
          .remove(sensorAssignments.getFileAssignments(removeFileName));
        sensorAssignments.removeFileAssignments(removeFileName);
        assignmentsTree.removeFile(removeFileName);
        sensorAssignmentsGuessed.remove(removeFileName);
      }

      if (instrumentFiles.size() == 0) {
        result = NAV_UPLOAD_FILE;
      } else {
        result = NAV_ASSIGN_VARIABLES;
      }

      updatedFile = null;
      return result;
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
      throw e;
    }
  }

  /**
   * Handle the Back button pressed on the File Upload page. Navigates to the
   * variable assignments page if files have been uploaded, or to the instrument
   * name.
   *
   * @return The navigation
   */
  public String backFromFileUpload() {
    String result;

    if (instrumentFiles.size() == 0) {
      result = NAV_VARIABLES;
    } else {
      result = NAV_ASSIGN_VARIABLES;
    }

    return result;
  }

  /**
   * Initialise the run types selection data and navigate to the page.
   *
   * @return The navigation to the run types selection page
   * @throws IOException
   * @throws DatabaseException
   * @throws InstrumentException
   * @throws MissingParamException
   */
  public String goToRunTypes() throws MissingParamException,
    InstrumentException, DatabaseException, IOException {

    String result;

    if (sensorAssignments.hasRunType()) {

      for (FileDefinition file : instrumentFiles) {
        // TODO Get run types from previous instrument if possible
        file.setRunTypes(autoAssignRunTypes((FileDefinitionBuilder) file));
      }

      result = NAV_RUN_TYPES;
    } else {
      result = NAV_SENSOR_GROUPS;
    }

    return result;
  }

  private RunTypeAssignments autoAssignRunTypes(FileDefinitionBuilder file) {

    RunTypeAssignments result;

    if (!file.hasRunTypes()) {
      result = null;
    } else {
      file.buildRunTypeCategories();

      List<Instrument> previousInstruments = getPreviousInstruments();

      if (previousInstruments.size() == 0) {
        result = RunTypeAssignments.buildRunTypes(instrumentVariables,
          file.getRunTypeColumns().first(), file.getRunTypeValues());
      } else {
        result = new RunTypeAssignments(file.getRunTypeColumns().first());

        for (String runType : file.getRunTypeValues().stream()
          .map(rt -> rt.toLowerCase()).toList()) {

          RunTypeAssignment previousAssignment = RunTypeAssignments
            .getPreviousRunTypeAssignment(runType, previousInstruments);

          if (null != previousAssignment) {
            result.put(runType, previousAssignment);
          }
        }

        // If we haven't found any previous assignments, revert to the base
        // guess.
        if (result.size() == 0) {
          result = RunTypeAssignments.buildRunTypes(instrumentVariables,
            file.getRunTypeColumns().first(), file.getRunTypeValues());
        }
      }

      if (!result.allIgnored()) {
        runTypesGuessed = true;
      }
    }

    return result;
  }

  public String goToSensorGroups() {
    return NAV_SENSOR_GROUPS;
  }

  /**
   * Go to the General Info page
   *
   * @return The navigation to the General Info page
   */
  public String goToGeneralInfo() {
    return (nameValid() ? NAV_GENERAL_INFO : null);
  }

  private boolean nameValid() {
    boolean result = true;

    if (StringUtils.isBlank(platformName)) {
      result = false;
      setMessage(null, "Platform Name required");
    }

    if (StringUtils.isBlank(platformCode)) {
      result = false;
      setMessage(null, "Platform Code required");
    }

    if (StringUtils.isBlank(instrumentName)) {
      result = false;
      setMessage(null, "Instrument Name required");
    }

    try {
      if (InstrumentDB.instrumentExists(getDataSource(), getUser(),
        platformName, instrumentName)) {
        result = false;
        setMessage(null, "Platform/Instrument name combination already used");
      }
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
      result = false;
      setMessage(null, "Error checking instrument name");
    }

    return result;
  }

  /**
   * Go to the Variable Info page, or skip straight to files if there are no
   * attributes to be assigned
   *
   * @return The navigation target
   */
  public String goToVariableInfo() {
    return instrumentVariables.stream().filter(v -> v.hasAttributes()).findAny()
      .isPresent() ? NAV_VARIABLE_INFO : NAV_UPLOAD_FILE;
  }

  /**
   * Get the file to which a run type is being assigned
   *
   * @return The file
   */
  public int getRunTypeAssignFile() {
    return runTypeAssignFile;
  }

  /**
   * Set the file to which a run type is being assigned
   *
   * @param runTypeAssignFile
   *          The file
   */
  public void setRunTypeAssignFile(int runTypeAssignFile) {
    this.runTypeAssignFile = runTypeAssignFile;
  }

  /**
   * Get the run type to be assigned to a Run Type Category
   *
   * @return The run type
   */
  public String getRunTypeAssignName() {
    return runTypeAssignName;
  }

  /**
   * Set the run type to be assigned to a Run Type Category
   *
   * @param runTypeAssignName
   *          The run type
   */
  public void setRunTypeAssignName(String runTypeAssignName) {
    this.runTypeAssignName = runTypeAssignName;
  }

  /**
   * Get the code of the Run Type Category that a run type is being assigned to
   *
   * @return The Run Type Category code
   */
  public long getRunTypeAssignCode() {
    return runTypeAssignCode;
  }

  /**
   * Set the code of the Run Type Category that a run type is being assigned to
   *
   * @param runTypeAssignCode
   *          The Run Type Category code
   */
  public void setRunTypeAssignCode(long runTypeAssignCode) {
    this.runTypeAssignCode = runTypeAssignCode;
  }

  /**
   * Get the name of the run type that the assigned run type is aliased to
   *
   * @return The aliased run type
   */
  public String getRunTypeAssignAliasTo() {
    return runTypeAssignAliasTo;
  }

  /**
   * Get the name of the run type that the assigned run type is aliased to
   *
   * @param runTypeAssignAliasTo
   *          The aliased run type
   */
  public void setRunTypeAssignAliasTo(String runTypeAssignAliasTo) {
    this.runTypeAssignAliasTo = runTypeAssignAliasTo;
  }

  /**
   * Assign a run type to a Run Type Category
   *
   * @throws NoSuchCategoryException
   *           If the chosen category does not exist
   */
  public void assignRunTypeCategory() throws NoSuchCategoryException {
    FileDefinition file = instrumentFiles.get(runTypeAssignFile);

    RunTypeCategory category = null;
    category = ResourceManager.getInstance().getRunTypeCategoryConfiguration()
      .getCategory(runTypeAssignCode);

    if (category.equals(RunTypeCategory.ALIAS)) {
      file.setRunTypeCategory(runTypeAssignName, runTypeAssignAliasTo);
    } else {
      file.setRunTypeCategory(runTypeAssignName, category);
    }
  }

  /**
   * Get the run type assignments as a JSON string
   *
   * @return The run type assignments
   */
  public String getRunTypeAssignments() {
    JsonArray json = new JsonArray();

    for (int i = 0; i < instrumentFiles.size(); i++) {
      FileDefinition file = instrumentFiles.get(i);
      if (file.hasRunTypes()) {
        RunTypeAssignments assignments = file.getRunTypes();

        JsonObject fileAssignments = new JsonObject();

        fileAssignments.addProperty("index", i);

        JsonArray jsonAssignments = new JsonArray();

        for (RunTypeAssignment assignment : assignments.values()) {
          JsonObject jsonAssignment = new JsonObject();
          jsonAssignment.addProperty("runType", assignment.getRunName());

          RunTypeCategory category = assignment.getCategory();
          if (category.equals(RunTypeCategory.ALIAS)) {
            jsonAssignment.addProperty("category", RunTypeCategory.ALIAS_TYPE);
            jsonAssignment.addProperty("aliasTo", assignment.getAliasTo());
          } else {
            jsonAssignment.addProperty("category",
              assignment.getCategory().getType());
            jsonAssignment.add("aliasTo", JsonNull.INSTANCE);
          }

          jsonAssignments.add(jsonAssignment);
        }

        fileAssignments.add("assignments", jsonAssignments);
        json.add(fileAssignments);
      }
    }

    return json.toString();
  }

  /**
   * Dummy set method to go with {@link #getRunTypeAssignments()}. Does nothing.
   *
   * @param dummy
   *          Dummy string
   */
  public void setRunTypeAssignments(String dummy) {
    // Do nothing
  }

  /**
   * Reset all data regarding run type assignments
   */
  private void clearRunTypeAssignments() {
    runTypeFile = null;
    runTypeAssignName = null;
    runTypeAssignCode = RunTypeCategory.IGNORED_TYPE;
  }

  /**
   * Get the pre-flushing time
   *
   * @return The pre-flushing time
   */
  public int getPreFlushingTime() {
    return preFlushingTime;
  }

  /**
   * Get the pre-flushing time
   *
   * @param preFlushingTime
   *          The pre-flushing time
   */
  public void setPreFlushingTime(int preFlushingTime) {
    this.preFlushingTime = preFlushingTime;
  }

  /**
   * Get the post-flushing time
   *
   * @return The post-flushing time
   */
  public int getPostFlushingTime() {
    return postFlushingTime;
  }

  /**
   * Get the post-flushing time
   *
   * @param postFlushingTime
   *          The post-flushing time
   */
  public void setPostFlushingTime(int postFlushingTime) {
    this.postFlushingTime = postFlushingTime;
  }

  /**
   * @return the platformName
   */
  public String getPlatformName() {
    return platformName;
  }

  /**
   * @param platformName
   *          the platform name to set
   */
  public void setPlatformName(String platformName) {
    this.platformName = platformName;
  }

  /**
   * @return the platformCode
   */
  public String getPlatformCode() {
    return platformCode;
  }

  /**
   * @param platformCode
   *          the platformCode to set
   */
  public void setPlatformCode(String platformCode) {
    this.platformCode = platformCode;
  }

  /**
   * Store the instrument
   *
   * @return Navigation to the instrument list
   * @throws InstrumentException
   *           If the instrument object is invalid
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   * @throws IOException
   *           If certain data cannot be converted for storage in the database
   */
  public String saveInstrument() throws MissingParamException,
    InstrumentException, DatabaseException, IOException {

    try {
      Map<Variable, Properties> storedVariableProperties = getVariableProperties();

      // Create the new Instrument object
      // TODO groups in here.
      Instrument instrument = new Instrument(getUser(), instrumentName, null,
        instrumentFiles, instrumentVariables, storedVariableProperties,
        sensorAssignments, sensorGroups, platformName, platformCode, false,
        null, LocalDateTime.now());

      instrument.setProperty(Instrument.PROP_PRE_FLUSHING_TIME,
        preFlushingTime);
      instrument.setProperty(Instrument.PROP_POST_FLUSHING_TIME,
        postFlushingTime);

      if (hasDepth) {
        instrument.setProperty(Instrument.PROP_DEPTH, depth);
      }

      if (fixedPosition) {
        instrument.setProperty(Instrument.PROP_LONGITUDE, longitude);
        instrument.setProperty(Instrument.PROP_LATITUDE, latitude);
      }

      InstrumentDB.storeInstrument(getDataSource(), instrument);
      setCurrentInstrumentId(instrument.getId());

      // Reinitialise beans to update their instrument lists
      instrumentListBean.initialiseInstruments();
      dataFilesBean.initialiseInstruments();
      dataSetsBean.initialiseInstruments();
    } catch (Exception e) {
      throw e;
    }

    return NAV_INSTRUMENT_LIST;
  }

  /**
   * Convert user-entered attributes to the format in which they'll be stored
   *
   * @return The variable attributes
   */
  protected Map<Variable, Properties> getVariableProperties() {
    Map<Variable, Properties> storedVariableProperties = new HashMap<Variable, Properties>();

    for (Variable var : instrumentVariables) {
      if (var.hasAttributes()) {
        Properties props = new Properties();
        var.getAttributes().forEach(attr -> {
          props.put(attr.getId(), attr.getValue());
        });

        storedVariableProperties.put(var, props);
      }
    }
    return storedVariableProperties;
  }

  /**
   * Set up the reference to the Instrument List Bean
   *
   * @param instrumentListBean
   *          The instrument list bean
   */
  public void setInstrumentListBean(InstrumentListBean instrumentListBean) {
    this.instrumentListBean = instrumentListBean;
  }

  /**
   * Set up the reference to the Data Files Bean
   *
   * @param dataFilesBean
   *          The data files bean
   */
  public void setDataFilesBean(DataFilesBean dataFilesBean) {
    this.dataFilesBean = dataFilesBean;
  }

  /**
   * Set up the reference to the Data Sets Bean
   *
   * @param dataSetsBean
   *          The data sets bean
   */
  public void setDataSetsBean(DataSetsBean dataSetsBean) {
    this.dataSetsBean = dataSetsBean;
  }

  /**
   * Get the file for which a Run Type column is being assigned
   *
   * @return The Run Type file
   */
  public String getRunTypeFile() {
    return runTypeFile;
  }

  /**
   * Set the file for which a Run Type column is being assigned
   *
   * @param runTypeFile
   *          The Run Type file
   */
  public void setRunTypeFile(String runTypeFile) {
    this.runTypeFile = runTypeFile;
  }

  /**
   * Get the index of the Run Type column being assigned
   *
   * @return The Run Type column index
   */
  public int getRunTypeColumn() {
    return runTypeColumn;
  }

  /**
   * Set the index of the Run Type column being assigned
   *
   * @param runTypeColumn
   *          The Run Type column index
   */
  public void setRunTypeColumn(int runTypeColumn) {
    this.runTypeColumn = runTypeColumn;
  }

  /**
   * Set the Run Type column for a file
   *
   * @throws SensorAssignmentException
   * @throws SensorTypeNotFoundException
   * @throws SensorConfigurationException
   */
  public void assignRunType() throws SensorAssignmentException,
    SensorTypeNotFoundException, SensorConfigurationException {

    FileDefinitionBuilder file = instrumentFiles.getByDescription(runTypeFile);
    file.addRunTypeColumn(runTypeColumn);

    SensorAssignment sensorAssignment = new SensorAssignment(runTypeFile,
      runTypeColumn, SensorType.RUN_TYPE_SENSOR_TYPE,
      file.getFileColumns().get(runTypeColumn).getName(), true, false, null);

    sensorAssignments.addAssignment(sensorAssignment);
    assignmentsTree.addAssignment(sensorAssignment);
  }

  public int getDepth() {
    return depth;
  }

  public void setDepth(int depth) {
    this.depth = depth;
  }

  @Override
  protected List<Long> getInstrumentVariableIDs() {
    return instrumentVariables.stream().map(x -> x.getId())
      .collect(Collectors.toList());
  }

  /**
   * Get the flag indicating whether or not this instrument has a fixed
   * position.
   *
   * @return {@code true} if the position is fixed; {@code false} otherwise.
   */
  public boolean getFixedPosition() {
    return fixedPosition;
  }

  /**
   * Set the fixed position flag.
   *
   * @param fixedPosition
   *          The flag value.
   */
  public void setFixedPosition(boolean fixedPosition) {
    this.fixedPosition = fixedPosition;
  }

  /**
   * Get the fixed longitude
   *
   * @return The fixed longitude
   */
  public double getLongitude() {
    return longitude;
  }

  /**
   * Set the fixed longitude.
   *
   * @param longitude
   */
  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  /**
   * Get the fixed latitude.
   *
   * @return The fixed latitude.
   */
  public double getLatitude() {
    return latitude;
  }

  /**
   * Set the fixed latitude.
   * <p>
   * Values greater than 180 degrees are switched to negative values.
   *
   * @param latitude
   *          The latitude.
   */
  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public boolean getHasDepth() {
    return hasDepth;
  }

  public void setHasDepth(boolean hasDepth) {
    this.hasDepth = hasDepth;
  }

  public String getRenameOldFile() {
    return renameOldFile;
  }

  public void setRenameOldFile(String renameOldFile) {
    this.renameOldFile = renameOldFile;
  }

  public String getRenameNewFile() {
    return renameNewFile;
  }

  public void setRenameNewFile(String renameNewFile) {
    this.renameNewFile = renameNewFile;
  }

  public String renameFile() throws DateTimeSpecificationException {
    FileDefinitionBuilder fileDefinition = instrumentFiles
      .getByDescription(renameOldFile);

    if (null != fileDefinition) {
      fileDefinition.setFileDescription(renameNewFile);
      sensorAssignments.renameFile(renameOldFile, renameNewFile);
      assignmentsTree.renameFile(renameOldFile, fileDefinition);
    }

    updatedFile = fileDefinition.getFileDescription();

    return NAV_ASSIGN_VARIABLES;
  }

  public TreeNode<AssignmentsTreeNodeData> getAssignmentsTree()
    throws Exception {
    return assignmentsTree.getRoot();
  }

  public String getSensorTypesJson() throws SensorConfigurationException {

    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();

    HashSet<SensorType> sensorTypes = new HashSet<SensorType>();

    for (Variable var : instrumentVariables) {
      sensorTypes
        .addAll(sensorConfig.getSensorTypes(var.getId(), true, true, true));
    }

    sensorTypes.addAll(sensorConfig.getDiagnosticSensorTypes());

    return new Gson().toJson(sensorTypes);
  }

  public String getSensorTypesWithFixedDependsQuestionAnswer() {
    JsonArray json = new JsonArray();

    for (Variable var : instrumentVariables) {
      var.getDependsQuestionAnswers().entrySet()
        .forEach(e -> json.add(e.getKey()));
    }

    return new Gson().toJson(json);
  }

  private Boolean getFixedDependsQuestionAnswer(long sensorTypeId) {

    Boolean result = null;

    for (Variable var : instrumentVariables) {
      result = var.getDependsQuestionAnswers().get(sensorTypeId);
      if (null != result) {
        break;
      }
    }

    return result;
  }

  public long getRemoveAssignmentSensorType() {
    return removeAssignmentSensorType;
  }

  public void setRemoveAssignmentSensorType(long removeAssignmentSensorType) {
    this.removeAssignmentSensorType = removeAssignmentSensorType;
  }

  public String getRemoveAssignmentDataFile() {
    return removeAssignmentDataFile;
  }

  public void setRemoveAssignmentDataFile(String removeAssignmentDataFile) {
    this.removeAssignmentDataFile = removeAssignmentDataFile;
  }

  public int getRemoveAssignmentColumn() {
    return removeAssignmentColumn;
  }

  public void setRemoveAssignmentColumn(int removeAssignmentColumn) {
    this.removeAssignmentColumn = removeAssignmentColumn;
  }

  public void removeSensorAssignment() {

    try {
      SensorType sensorType = ResourceManager.getInstance()
        .getSensorsConfiguration().getSensorType(removeAssignmentSensorType);

      SensorAssignment removed = sensorAssignments.removeAssignment(sensorType,
        removeAssignmentDataFile, removeAssignmentColumn);
      sensorGroups.remove(removed);
      assignmentsTree.removeAssignment(removed);

      if (sensorType.equals(SensorType.RUN_TYPE_SENSOR_TYPE)) {
        instrumentFiles.get(removeAssignmentDataFile)
          .removeRunTypeColumn(removeAssignmentColumn);
      }
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
    }
  }

  public void removeDateTimeAssignment() throws DateTimeSpecificationException {
    DateTimeSpecification dateTimeSpec = instrumentFiles.get(dateTimeFile)
      .getDateTimeSpecification();

    dateTimeSpec.removeAssignment(dateTimeColumn);

    assignmentsTree
      .setDateTimeAssignment(instrumentFiles.getByDescription(dateTimeFile));

    resetDateTimeAssignmentValues();
  }

  /**
   * Remove a position specification.
   * <p>
   * If the main column is removed and the format was hemisphere, the hemisphere
   * assignment is removed as well.
   * </p>
   * <p>
   * This cheats a bit and reuses the Remove Sensor Assignment inputs.
   * <p>
   *
   * @throws InvalidPositionFormatException
   */
  public void removePositionAssignment() throws InvalidPositionFormatException {

    String expandNode = null;

    FileDefinitionBuilder file = (FileDefinitionBuilder) instrumentFiles
      .get(removeAssignmentDataFile);

    if (removeColumnFromPositionSpec(file.getLongitudeSpecification(),
      removeAssignmentColumn)) {

      expandNode = "Longitude";
    } else if (removeColumnFromPositionSpec(file.getLatitudeSpecification(),
      removeAssignmentColumn)) {

      expandNode = "Latitude";
    }

    assignmentsTree.updatePositionNodes(expandNode);
    resetSensorAssignmentValues();
  }

  private boolean removeColumnFromPositionSpec(PositionSpecification spec,
    int column) throws InvalidPositionFormatException {

    boolean removed = false;

    if (spec.getValueColumn() == column) {
      removed = true;
      spec.clearValueColumn();
    } else if (spec.getHemisphereColumn() == column) {
      removed = true;
      spec.clearHemisphereColumn();
    }

    return removed;
  }

  public FileDefinitionBuilder getRunTypeFileDefinition() {
    FileDefinitionBuilder result = null;

    for (FileDefinition file : instrumentFiles) {
      if (file.hasRunTypes()) {
        result = (FileDefinitionBuilder) file;
        break;
      }
    }

    return result;
  }

  public boolean getRunTypesGuessed() {
    return runTypesGuessed;
  }

  public String getAssignedSensorNames() {
    return getGson().toJson(sensorAssignments.getAllSensorNames());
  }

  public void setAssignedSensorNames(String dummy) {
    // Do nothing
  }

  private Gson getGson() {
    if (null == gson) {
      gson = new Gson();
    }

    return gson;
  }

  public SensorGroups getSensorGroups() {
    return sensorGroups;
  }

  public String getSensorGroupNamesJson() {
    return new Gson().toJson(sensorGroups.getGroupNames());
  }

  public void setSensorGroupNamesJson(String dummy) {
    //
  }

  public String getGroupRenameFrom() {
    return ""; // The server can't tell the front end
  }

  public void setGroupRenameFrom(String groupRenameFrom) {
    this.groupRenameFrom = groupRenameFrom;
  }

  public String getGroupRenameTo() {
    return ""; // The server can't tell the front end
  }

  public void setGroupRenameTo(String groupRenameTo) {
    this.groupRenameTo = groupRenameTo;
  }

  public void renameSensorGroup() throws SensorGroupsException {
    sensorGroups.renameGroup(groupRenameFrom, groupRenameTo);
  }

  public String getAddGroupAfter() {
    return addGroupAfter;
  }

  public void setAddGroupAfter(String addGroupAfter) {
    this.addGroupAfter = addGroupAfter.trim().equals("") ? null : addGroupAfter;
  }

  public String getAddGroupName() {
    return addGroupName;
  }

  public void setAddGroupName(String addGroupName) {
    this.addGroupName = addGroupName;
  }

  public String addSensorGroup() throws SensorGroupsException {
    sensorGroups.addGroup(addGroupName, addGroupAfter);
    return NAV_SENSOR_GROUPS;
  }

  public String getDeleteGroupName() {
    return deleteGroupName;
  }

  public void setDeleteGroupName(String deleteGroupName) {
    this.deleteGroupName = deleteGroupName;
  }

  public void deleteSensorGroup() throws SensorGroupsException {
    sensorGroups.deleteGroup(deleteGroupName);
  }

  public String getMoveSensorName() {
    return moveSensorName;
  }

  public void setMoveSensorName(String moveSensorName) {
    this.moveSensorName = moveSensorName;
  }

  public String getMoveSensorGroup() {
    return moveSensorGroup;
  }

  public void setMoveSensorGroup(String moveSensorGroup) {
    this.moveSensorGroup = moveSensorGroup;
  }

  public String moveSensor() throws SensorGroupsException {
    sensorGroups.moveSensor(moveSensorName, moveSensorGroup);
    return NAV_SENSOR_GROUPS;
  }

  public boolean getGroupsComplete() {
    return sensorGroups.isComplete();
  }

  public void setGroupsComplete(boolean groupsComplete) {
    // Dummy
  }

  public String getGroupLinksJson() {
    return sensorGroups.getLinksJson();
  }

  public void setGroupLinksJson(String dummy) {
    // Dummy
  }

  public String getGroupLinkGroup() {
    return groupLinkGroup;
  }

  public void setGroupLinkGroup(String groupLinkGroup) {
    this.groupLinkGroup = groupLinkGroup;
  }

  public String getGroupLinkSensor() {
    return groupLinkSensor;
  }

  public void setGroupLinkSensor(String groupLinkSensor) {
    this.groupLinkSensor = groupLinkSensor;
  }

  public int getGroupLinkDirection() {
    return groupLinkDirection;
  }

  public void setGroupLinkDirection(int groupLinkDirection) {
    this.groupLinkDirection = groupLinkDirection;
  }

  public void setGroupLink() throws SensorGroupsException {
    sensorGroups.getGroup(groupLinkGroup).setLink(groupLinkSensor,
      groupLinkDirection);
  }

  public List<PositionFormatEntry> getLongitudeFormats() {

    if (null == lonFormats) {
      LinkedHashMap<Integer, String> lonFormatsMap = LongitudeSpecification
        .getFormats();

      lonFormats = new ArrayList<PositionFormatEntry>(lonFormatsMap.size());

      lonFormatsMap.entrySet().forEach(
        e -> lonFormats.add(new PositionFormatEntry(e.getKey(), e.getValue())));
    }

    return lonFormats;

  }

  public List<PositionFormatEntry> getLatitudeFormats() {
    if (null == latFormats) {
      TreeMap<Integer, String> latFormatsMap = LatitudeSpecification
        .getFormats();

      latFormats = new ArrayList<PositionFormatEntry>(latFormatsMap.size());

      latFormatsMap.entrySet().forEach(
        e -> latFormats.add(new PositionFormatEntry(e.getKey(), e.getValue())));
    }

    return latFormats;
  }

  public List<String> getExistingPlatformNames() {
    return new ArrayList<String>(existingPlatforms.keySet());
  }

  public void platformNameChanged() {
    if (existingPlatforms.containsKey(platformName)) {
      platformCode = existingPlatforms.get(platformName);
    }
  }

  public List<Variable> getVariablesWithAttributes() {
    return instrumentVariables.stream().filter(v -> v.hasAttributes()).toList();
  }

  /**
   * Get a JSON object specifying which data files have had columns assigned.
   *
   * <p>
   * Assignment of date/time is not considered.
   * </p>
   *
   * @return The assignment status of the data files.
   */
  public String getAssignedFiles() {
    Map<String, Boolean> map = new HashMap<String, Boolean>();
    instrumentFiles.stream().forEach(f -> {
      map.put(f.getFileDescription(), f.hasPosition()
        || sensorAssignments.isFileAssigned(f.getFileDescription()));
    });

    return getGson().toJson(map);
  }

  /**
   * Get the most recently updated file.
   *
   * <p>
   * This is used to set the selected file tab when the page loads.
   * </p>
   *
   * @return The updated file.
   */
  public String getUpdatedFile() {
    return updatedFile;
  }
}
