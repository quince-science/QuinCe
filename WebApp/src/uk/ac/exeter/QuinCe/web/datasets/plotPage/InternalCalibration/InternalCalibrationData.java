package uk.ac.exeter.QuinCe.web.datasets.plotPage.InternalCalibration;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.RunTypeSensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.ValueCounter;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageColumnHeading;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageData;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableRecord;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.SimplePlotPageDataStructure;

public class InternalCalibrationData extends PlotPageData {

  private PlotPageColumnHeading defaultYAxis1 = null;

  private PlotPageColumnHeading defaultYAxis2 = null;

  private SimplePlotPageDataStructure dataStructure = null;

  /**
   * The dataset's sensor values.
   */
  private DatasetSensorValues datasetSensorValues = null;

  /**
   * The initial user QC comments generated from the selected values.
   */
  private String userCommentsList = null;

  /**
   * The worst QC flag set on any of the selected values.
   */
  private Flag worstSelectedFlag = Flag.GOOD;

  /**
   * Construct the data object.
   * <p>
   * Initially the object is empty. The data will be loaded by the
   * {@link #load(DataSource)} method.
   * </p>
   *
   * @param instrument
   *          The instrument that the dataset belongs to.
   * @param dataset
   *          The dataset.
   * @param dataSource
   *          A data source.
   * @throws SQLException
   * @throws Exception
   *           If the data cannot be loaded.
   */
  protected InternalCalibrationData(DataSource dataSource,
    Instrument instrument, DataSet dataset) throws SQLException {
    super(dataSource, instrument, dataset);
  }

  @Override
  protected void loadDataAction() throws Exception {

    try (Connection conn = dataSource.getConnection()) {

      datasetSensorValues = DataSetDataDB.getSensorValues(conn, instrument,
        dataset.getId(), false, true);

      List<RunTypeSensorValue> sensorValues = DataSetDataDB
        .getInternalCalibrationSensorValues(conn, instrument, dataset.getId());

      dataStructure = new SimplePlotPageDataStructure(getColumnHeadingsList());

      for (RunTypeSensorValue value : sensorValues) {
        long columnId = makeColumnId(value.getRunType(), value.getColumnId());
        dataStructure.add(value.getTime(), getColumnHeading(columnId), value);
      }
    }
  }

  @Override
  protected DatasetSensorValues getAllSensorValues() {
    return datasetSensorValues;
  }

  @Override
  protected void buildColumnHeadings()
    throws MissingParamException, DatabaseException, RecordNotFoundException {

    columnHeadings = new LinkedHashMap<String, List<PlotPageColumnHeading>>();

    try (Connection conn = dataSource.getConnection()) {
      CalibrationSet calibrations = ExternalStandardDB.getInstance()
        .getStandardsSet(conn, instrument, dataset.getStart());

      // Time
      List<PlotPageColumnHeading> rootColumns = new ArrayList<PlotPageColumnHeading>(
        1);
      rootColumns.add(new PlotPageColumnHeading(
        FileDefinition.TIME_COLUMN_HEADING, false, false, false));

      columnHeadings.put(ROOT_FIELD_GROUP, rootColumns);

      List<String> runTypes = instrument
        .getRunTypes(RunTypeCategory.INTERNAL_CALIBRATION_TYPE);

      int columnCount = 0;

      // Each SensorType with internal calibrations goes in its own group
      for (SensorType sensorType : instrument.getSensorAssignments().keySet()) {
        if (sensorType.hasInternalCalibration()) {

          List<PlotPageColumnHeading> sensorTypeColumns = new ArrayList<PlotPageColumnHeading>();

          TreeSet<SensorAssignment> assignments = instrument
            .getSensorAssignments().get(sensorType);

          // Each sensor assigned to that SensorType becomes a set of columns,
          // one for each calibration run type
          for (SensorAssignment assignment : assignments) {
            for (String runType : runTypes) {

              Double calibrationValue = calibrations
                .getCalibrationValue(runType, sensorType.getShortName());

              long columnId = makeColumnId(runType, assignment);
              String columnName = runType + ":" + assignment.getSensorName();

              PlotPageColumnHeading heading = new PlotPageColumnHeading(
                columnId, columnName, sensorType.getLongName(),
                sensorType.getCodeName(), sensorType.getUnits(), false, true,
                true, calibrationValue, sensorType.questionableFlagAllowed());
              sensorTypeColumns.add(heading);
              columnCount++;
              if (columnCount == 1) {
                defaultYAxis1 = heading;
              } else if (columnCount == 2) {
                defaultYAxis2 = heading;
              }
            }
          }

          columnHeadings.put(sensorType.getShortName(), sensorTypeColumns);
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error getting column headings", e);
    }
  }

  /**
   * Generate a unique column ID for a run type/sensor assignment combination.
   * <p>
   * Calculated as the hash code of the run type plus the assignment's database
   * ID.
   * </p>
   *
   * @param runType
   *          The run type
   * @param assignment
   *          The sensor assignment
   * @return The column ID
   */
  private long makeColumnId(String runType, SensorAssignment assignment) {
    return makeColumnId(runType, assignment.getDatabaseId());
  }

  /**
   * Generate a unique column ID for a run type and column ID.
   * <p>
   * Calculated as the hash code of the run type plus the column ID.
   * </p>
   *
   * @param runType
   *          The run type
   * @param assignment
   *          The sensor assignment
   * @return The column ID
   */
  private long makeColumnId(String runType, long columnId) {
    return (runType + columnId).hashCode();
  }

  @Override
  public LinkedHashMap<String, List<PlotPageColumnHeading>> getExtendedColumnHeadings()
    throws Exception {
    return getColumnHeadings();
  }

  @Override
  public int size() {
    return null == dataStructure ? 0 : dataStructure.size();
  }

  @Override
  public List<Long> getRowIDs() {
    List<Long> result = null;

    if (null != dataStructure) {
      result = dataStructure.getRowIds();
    }

    return result;
  }

  @Override
  protected List<PlotPageTableRecord> generateTableDataRecords(int start,
    int length) {

    return dataStructure.generateTableDataRecords(start, length);
  }

  @Override
  protected TreeMap<LocalDateTime, PlotPageTableValue> getColumnValues(
    PlotPageColumnHeading column) throws Exception {

    return dataStructure.getColumnValues(column);
  }

  @Override
  protected PlotPageColumnHeading getDefaultYAxis1() {
    return defaultYAxis1;
  }

  @Override
  protected PlotPageColumnHeading getDefaultYAxis2() {
    return defaultYAxis2;
  }

  private List<SensorValue> getSelectedSensorValues() {
    return dataStructure.getSensorValues(selectedColumn,
      DateTimeUtils.longsToDates(selectedRows));
  }

  protected void applyFlag(Flag flag, String message)
    throws MissingParamException, DatabaseException, InvalidFlagException {

    List<SensorValue> sensorValues = getSelectedSensorValues();

    for (SensorValue sensorValue : sensorValues) {
      sensorValue.setUserQC(flag, message);
    }

    // Store the updated sensor values
    try (Connection conn = dataSource.getConnection()) {
      DataSetDataDB.storeSensorValues(conn, sensorValues);
    } catch (Exception e) {
      throw new DatabaseException("Error while applying QC flag", e);
    }

    initPlots();
  }

  /**
   * Accept automatic QC flags for the selected values.
   *
   * @throws RoutineException
   *           If the automatic QC details cannot be retrieved.
   * @throws SQLException
   * @throws DatabaseException
   * @throws MissingParamException
   */
  public void acceptAutoQC() {
    try {
      List<SensorValue> sensorValues = getSelectedSensorValues();

      for (SensorValue sensorValue : sensorValues) {

        // Only override the existing user QC if it has Needs Flag or Assumed
        // Good
        if (sensorValue.getUserQCFlag().equals(Flag.NEEDED)
          || sensorValue.getUserQCFlag().equals(Flag.ASSUMED_GOOD)) {
          sensorValue.setUserQC(sensorValue.getAutoQcFlag(),
            sensorValue.getAutoQcResult().getAllMessages());
        }
      }

      try (Connection conn = dataSource.getConnection()) {
        DataSetDataDB.storeSensorValues(conn, sensorValues);
      }
      initPlots();
    } catch (Exception e) {
      error("Error while updating QC flags", e);
    }
  }

  /**
   * Get the number of {@link SensorValue}s whose QC flag is
   * {@link Flag#NEEDED}, grouped by column ID.
   *
   * @return The number of NEEDED flags
   */
  public int getNeedsFlagCount() {
    return null == dataStructure ? 0 : dataStructure.getNeedsFlagCount();
  }

  /**
   * Generate the QC comments list and find the worst QC flag from the currently
   * selected values.
   */
  public void generateUserCommentsList() {

    ValueCounter comments = new ValueCounter();
    worstSelectedFlag = Flag.GOOD;

    for (SensorValue sensorValue : getSelectedSensorValues()) {
      if (sensorValue.getDisplayFlag().moreSignificantThan(worstSelectedFlag)) {
        worstSelectedFlag = sensorValue.getDisplayFlag();
      }

      if (!sensorValue.flagNeeded()) {
        comments.add(sensorValue.getUserQCMessage());
      } else {
        try {
          comments.addAll(sensorValue.getAutoQcResult().getAllMessagesSet());
        } catch (RoutineException e) {
          error("Error getting QC comments", e);
        }
      }
    }

    userCommentsList = comments.toString();
  }

  /**
   * Get the QC comments generated from the current selection.
   *
   * @return The QC comments
   */
  public String getUserCommentsList() {
    return userCommentsList;
  }

  /**
   * Get the worst QC flag from the current selection.
   *
   * @return The QC flag.
   */
  public Flag getWorstSelectedFlag() {
    return worstSelectedFlag;
  }

  @Override
  protected List<LocalDateTime> getDataTimes() {
    return dataStructure.getTimes();
  }
}
