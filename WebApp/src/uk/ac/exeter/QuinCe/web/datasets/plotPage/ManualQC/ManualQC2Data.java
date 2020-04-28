package uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionException;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.utils.ValueCounter;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ColumnHeading;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPage2Data;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableRecord;

public class ManualQC2Data extends PlotPage2Data {

  /**
   * The database connection to use for all actions. Set up by
   * {@link #loadData(DataSource)}.
   */
  private Connection conn = null;

  /**
   * The dataset whose data is represented.
   */
  private final DataSet dataset;

  /**
   * The instrument that the dataset belongs to.
   */
  private final Instrument instrument;

  /**
   * The Measurement objects for the dataset
   */
  private Map<LocalDateTime, Long> measurements = null;

  /**
   * All row IDs for the dataset. Row IDs are the millisecond values of the
   * times.
   */
  private List<String> rowIDs = null;

  /**
   * The dataset's sensor values.
   */
  private DatasetSensorValues sensorValues = null;

  /**
   * The values calculated by data reduction.
   */
  private Map<Long, Map<InstrumentVariable, DataReductionRecord>> dataReduction = null;

  /**
   * The column headers for the data
   */
  private LinkedHashMap<String, List<ColumnHeading>> columnHeadings = null;

  /**
   * The list of sensor column IDs in the same order as they are represented in
   * {@link #columnHeaders}.
   */
  private long[] sensorColumnIds = null;

  /**
   * The list of diagnostic column IDs in the same order as they are represented
   * in {@link #columnHeaders}.
   */
  private long[] diagnosticColumnIds = null;

  /**
   * The initial user QC comments generated from the selected values.
   */
  private String userCommentsList = null;

  /**
   * The worst QC flag set on any of the selected values.
   */
  private Flag worstSelectedFlag = Flag.GOOD;

  /**
   * The flag set during user QC
   */
  private Flag userFlag = Flag.GOOD;

  /**
   * The user QC comment
   */
  private String userComment = null;

  /**
   * Construct the data object.
   *
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
  protected ManualQC2Data(DataSource dataSource, Instrument instrument,
    DataSet dataset) throws SQLException {
    this.instrument = instrument;
    this.dataset = dataset;
  }

  /**
   * Load all the data for the dataset.
   *
   * @param dataSource
   *          A data source.
   * @throws Exception
   *           If the data cannot be loaded.
   */
  @Override
  public void loadDataAction(DataSource dataSource) throws Exception {

    // Store the connection for later use.
    conn = dataSource.getConnection();

    sensorValues = DataSetDataDB.getSensorValues(conn, instrument,
      dataset.getId(), false);

    measurements = DataSetDataDB.getMeasurementTimes(conn, dataset.getId(),
      instrument.getMeasurementRunTypes());

    dataReduction = DataSetDataDB.getDataReductionData(conn, instrument,
      dataset);

    // Build the row IDs
    rowIDs = sensorValues.getTimes().stream()
      .map(t -> String.valueOf(DateTimeUtils.dateToLong(t)))
      .collect(Collectors.toList());

    buildColumnHeaders();
  }

  private void buildColumnHeaders() {

    columnHeadings = new LinkedHashMap<String, List<ColumnHeading>>();

    // Time and Position
    List<ColumnHeading> rootColumns = new ArrayList<ColumnHeading>(3);
    rootColumns.add(new ColumnHeading(FileDefinition.TIME_COLUMN_ID,
      FileDefinition.TIME_COLUMN_NAME, false, false));
    rootColumns.add(new ColumnHeading(FileDefinition.LONGITUDE_COLUMN_ID,
      "Position", false, true));

    columnHeadings.put(ROOT_FIELD_GROUP, rootColumns);

    // Sensor Assignments are divided into sensors and diagnostics
    List<SensorAssignment> sensorColumns = new ArrayList<SensorAssignment>();
    List<SensorAssignment> diagnosticColumns = new ArrayList<SensorAssignment>();

    for (Map.Entry<SensorType, List<SensorAssignment>> entry : instrument
      .getSensorAssignments().entrySet()) {

      // Skip the position
      if (!entry.getKey().equals(SensorType.LONGITUDE_SENSOR_TYPE)
        && !entry.getKey().equals(SensorType.LATITUDE_SENSOR_TYPE)) {

        for (SensorAssignment assignment : entry.getValue()) {

          if (entry.getKey().isSensor()) {
            sensorColumns.add(assignment);
          } else if (entry.getKey().isDiagnostic()) {
            diagnosticColumns.add(assignment);
          }
        }
      }
    }

    List<ColumnHeading> sensorColumnHeadings = new ArrayList<ColumnHeading>(
      sensorColumns.size());
    sensorColumnIds = new long[sensorColumns.size()];

    for (int i = 0; i < sensorColumns.size(); i++) {

      SensorAssignment column = sensorColumns.get(i);
      sensorColumnHeadings.add(new ColumnHeading(column.getDatabaseId(),
        column.getSensorName(), true, true));
      sensorColumnIds[i] = column.getDatabaseId();
    }

    columnHeadings.put("Sensors", sensorColumnHeadings);

    diagnosticColumnIds = new long[diagnosticColumns.size()];
    if (diagnosticColumns.size() > 0) {
      List<ColumnHeading> diagnosticColumnNames = new ArrayList<ColumnHeading>(
        diagnosticColumns.size());

      for (int i = 0; i < diagnosticColumns.size(); i++) {

        SensorAssignment column = diagnosticColumns.get(i);

        diagnosticColumnNames.add(new ColumnHeading(column.getDatabaseId(),
          column.getSensorName(), true, true));
        diagnosticColumnIds[i] = column.getDatabaseId();
      }

      columnHeadings.put("Diagnostics", diagnosticColumnNames);
    }

    // Each of the instrument variables
    for (InstrumentVariable variable : instrument.getVariables()) {
      try {
        columnHeadings.put(variable.getName(), ColumnHeading.headingList(
          DataReducerFactory.getCalculationParameters(variable), true, false));
      } catch (DataReductionException e) {
        error("Error getting variable headers", e);
      }
    }
  }

  @Override
  protected LinkedHashMap<String, List<ColumnHeading>> getColumnHeadings() {
    return columnHeadings;
  }

  @Override
  public int size() {
    // Each record is identified by its time stamp, so each time = 1 record
    return (loaded ? sensorValues.getTimes().size() : -1);
  }

  @Override
  public List<PlotPageTableRecord> generateTableDataRecords(int start,
    int length) {

    List<PlotPageTableRecord> records = new ArrayList<PlotPageTableRecord>(
      length);

    try {

      List<LocalDateTime> times = sensorValues.getTimes();

      for (int i = start; i < start + length; i++) {
        PlotPageTableRecord record = new PlotPageTableRecord(times.get(i));

        // Timestamp
        record.addColumn(times.get(i), true, Flag.GOOD, null, false);

        Map<Long, SensorValue> recordSensorValues = sensorValues
          .get(times.get(i));

        // Lon and Lat

        // We assume there's only one position - the UI won't allow users to
        // enter more than one.
        //
        // The position is combined into a single column.

        SensorValue longitude = recordSensorValues
          .get(FileDefinition.LONGITUDE_COLUMN_ID);
        SensorValue latitude = recordSensorValues
          .get(FileDefinition.LATITUDE_COLUMN_ID);

        StringBuilder positionString = new StringBuilder();
        positionString.append(StringUtils.formatNumber(longitude.getValue()));
        positionString.append(" | ");
        positionString.append(StringUtils.formatNumber(latitude.getValue()));

        record.addColumn(positionString.toString(), true,
          longitude.getDisplayFlag(), longitude.getDisplayQCMessage(),
          longitude.flagNeeded());

        for (long columnId : sensorColumnIds) {
          record.addColumn(recordSensorValues.get(columnId), false);
        }

        if (null != diagnosticColumnIds) {
          for (long columnId : diagnosticColumnIds) {
            record.addColumn(recordSensorValues.get(columnId), false);
          }
        }

        Long measurementId = measurements.get(times.get(i));
        Map<InstrumentVariable, DataReductionRecord> dataReductionData;

        if (null != measurementId) {
          // Retrieve the data reduction data
          dataReductionData = dataReduction.get(measurementId);
        } else {
          // Make a blank set
          dataReductionData = new HashMap<InstrumentVariable, DataReductionRecord>();
          instrument.getVariables()
            .forEach(x -> dataReductionData.put(x, null));
        }

        // Variables
        for (InstrumentVariable variable : instrument.getVariables()) {
          DataReductionRecord variableDataReduction = dataReductionData
            .get(variable);

          if (null != variableDataReduction) {
            Set<String> params = DataReducerFactory
              .getCalculationParameters(variable).keySet();

            for (String param : params) {
              record.addColumn(
                String
                  .valueOf(variableDataReduction.getCalculationValue(param)),
                false, variableDataReduction.getQCFlag(),
                variableDataReduction.getQCMessages().toString(), false);
            }
          } else {
            // Make blank columns
            record
              .addBlankColumns(columnHeadings.get(variable.getName()).size());
          }
        }

        records.add(record);
      }
    } catch (

    Exception e) {
      error("Error loading table data", e);
    }

    return records;
  }

  @Override
  protected List<String> getRowIDs() {
    return rowIDs;
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
        sensorValue.setUserQC(sensorValue.getAutoQcFlag(),
          sensorValue.getAutoQcResult().getAllMessages());
      }

      // If we QCed, the position, update all related sensor values
      if (FileDefinition.LONGITUDE_COLUMN_ID == selectedColumn) {
        sensorValues.addAll(propagatePositionQC(sensorValues));
      }

      DataSetDataDB.storeSensorValues(conn, sensorValues);
    } catch (Exception e) {
      error("Error while updating QC flags", e);
    }
  }

  /**
   * Get the {@link SensorValue}s for the current selection.
   *
   * @return The {@link SensorValue}s.
   */
  private List<SensorValue> getSelectedSensorValues() {

    List<SensorValue> values = new ArrayList<SensorValue>(selectedRows.size());

    if (null != selectedRows) {
      for (String rowId : selectedRows) {
        values.add(sensorValues.getSensorValue(
          DateTimeUtils.longToDate(Long.parseLong(rowId)), selectedColumn));
      }
    }

    return values;
  }

  protected void destroy() {
    try {
      conn.close();
    } catch (SQLException e) {
      // Not much we can do about this.
    }
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
          comments.addAll(sensorValue.getAutoQcResult().getAllMessagesList());
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

  public int getUserFlag() {
    return userFlag.getFlagValue();
  }

  public void setUserFlag(int userFlag) {
    try {
      this.userFlag = new Flag(userFlag);
    } catch (InvalidFlagException e) {
      error("Error setting QC flag", e);
    }
  }

  public String getUserComment() {
    return userComment;
  }

  public void setUserComment(String userComment) {
    this.userComment = userComment;
  }

  public void applyManualFlag() {
    try {
      List<SensorValue> selectedValues = getSelectedSensorValues();

      // If we're doing position QC, apply it and update all related sensor
      // values.
      if (FileDefinition.LONGITUDE_COLUMN_ID == selectedColumn) {
        selectedValues.forEach(v -> v.setUserQC(userFlag, userComment));
        selectedValues.addAll(propagatePositionQC(selectedValues));
      } else {

        for (SensorValue value : selectedValues) {
          // If we are directly overwriting an existing position QC, go ahead
          // and set it - the SensorValue will decide whether to update or not
          if (value.hasPositionQC()) {
            value.setUserQC(userFlag, userComment);
          } else {

            // Find the position QC for this sensor value, and see if we need to
            // apply it.
            SensorValue positionSensorValue = sensorValues
              .getSensorValueOnOrBefore(FileDefinition.LONGITUDE_COLUMN_ID,
                value.getTime());

            if (null != positionSensorValue && positionSensorValue
              .getDisplayFlag().moreSignificantThan(userFlag)) {

              value.setUserQC(positionSensorValue.getDisplayFlag(),
                positionSensorValue.getDisplayQCMessage());
            } else {
              value.setUserQC(userFlag, userComment);
            }
          }
        }
      }

      // Store the updated sensor values
      DataSetDataDB.storeSensorValues(conn, selectedValues);

    } catch (Exception e) {
      error("Error storing QC data", e);
    }
  }

  /**
   * Applies position QC flags to values from sensors.
   *
   * <p>
   * If the position for a given sensor is bad, then that value must also be bad
   * - a sensor value with an untrustworthy position is of no use. This is
   * applied to any sensor values whose time is >= to the position's time and <
   * the time of the next position value.
   * </p>
   *
   * <p>
   * This method takes in a list of position values, and finds all sensor values
   * related to that position and propagates the QC flag to them. If the
   * position flag is worse than the sensor's own flag, then it will be
   * overridden.
   * </p>
   *
   * @param positionValues
   *          The position values
   * @return The list of sensor values whose QC flags have been updated
   * @throws RecordNotFoundException
   * @throws RoutineException
   */
  private List<SensorValue> propagatePositionQC(
    List<SensorValue> positionValues)
    throws RecordNotFoundException, RoutineException {

    List<SensorValue> updatedValues = new ArrayList<SensorValue>();

    // Get all the position values as a sorted list
    List<SensorValue> allPositionValues = new ArrayList<SensorValue>(
      sensorValues.getBySensorType(SensorType.LONGITUDE_SENSOR_TYPE));

    for (SensorValue positionValue : positionValues) {

      int positionValueLocation = allPositionValues.indexOf(positionValue);
      LocalDateTime nextPositionTime = allPositionValues
        .get(positionValueLocation + 1).getTime();

      List<SensorValue> relatedSensorValues = sensorValues
        .getByTimeRange(positionValue.getTime(), nextPositionTime);

      for (SensorValue value : relatedSensorValues) {

        // For latitude, just copy the longitude QC
        if (FileDefinition.LATITUDE_COLUMN_ID == value.getColumnId()) {
          value.setUserQC(positionValue.getDisplayFlag(),
            positionValue.getDisplayQCMessage());

          // Skip the longitude - that's what we're basing this whole thing on
          // in the first place.
        } else if (FileDefinition.LONGITUDE_COLUMN_ID != value.getColumnId()) {
          // Don't process diagnostics
          if (!instrument.getSensorAssignments()
            .getSensorTypeForDBColumn(value.getColumnId()).isDiagnostic()) {

            value.setPositionQC(positionValue.getDisplayFlag(),
              positionValue.getDisplayQCMessage());

            updatedValues.add(value);
          }
        }
      }
    }

    return updatedValues;
  }
}
