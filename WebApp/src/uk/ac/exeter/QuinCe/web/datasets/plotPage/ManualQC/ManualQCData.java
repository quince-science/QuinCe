package uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.CalculationParameter;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionException;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.utils.ValueCounter;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.DataReductionRecordPlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.NullPlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageColumnHeading;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageData;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableRecord;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.SensorValuePlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.SimplePlotPageTableValue;

public class ManualQCData extends PlotPageData {

  /**
   * The Measurement objects for the dataset
   */
  protected TreeMap<LocalDateTime, Measurement> measurements = null;

  /**
   * The set of {@link SensorType}s used by the measurements in this dataset.
   * 
   * <p>
   * Used to display interpolated values from
   * </p>
   * 
   * @see Measurement#getMeasurementValue(SensorType)
   */
  private TreeSet<SensorType> measurementSensorTypes = null;

  /**
   * All row IDs for the dataset. Row IDs are the millisecond values of the
   * times.
   */
  private List<Long> rowIDs = null;

  /**
   * The dataset's sensor values.
   */
  protected DatasetSensorValues sensorValues = null;

  /**
   * The values calculated by data reduction.
   */
  protected Map<Long, Map<Variable, DataReductionRecord>> dataReduction = null;

  /**
   * The list of sensor column IDs in the same order as they are represented in
   * {@link #columnHeaders}.
   */
  private List<Long> sensorColumnIds = null;

  /**
   * The list of diagnostic column IDs in the same order as they are represented
   * in {@link #columnHeaders}.
   */
  private List<Long> diagnosticColumnIds = null;

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
  protected ManualQCData(DataSource dataSource, Instrument instrument,
    DataSet dataset) throws SQLException {
    super(dataSource, instrument, dataset);
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
  public void loadDataAction() throws Exception {

    try (Connection conn = dataSource.getConnection()) {
      sensorValues = DataSetDataDB.getSensorValues(conn, instrument,
        dataset.getId(), false);

      List<Measurement> measurementsList = DataSetDataDB.getMeasurements(conn,
        dataset.getId());

      measurements = new TreeMap<LocalDateTime, Measurement>();

      measurementsList.forEach(m -> measurements.put(m.getTime(), m));

      dataReduction = DataSetDataDB.getDataReductionData(conn, instrument,
        dataset);

      // Build the row IDs
      rowIDs = sensorValues.getTimes().stream()
        .map(t -> DateTimeUtils.dateToLong(t)).collect(Collectors.toList());
    }
  }

  @Override
  protected void buildColumnHeadings() {

    columnHeadings = new LinkedHashMap<String, List<PlotPageColumnHeading>>();
    extendedColumnHeadings = new LinkedHashMap<String, List<PlotPageColumnHeading>>();

    // Time and Position
    List<PlotPageColumnHeading> rootColumns = new ArrayList<PlotPageColumnHeading>(
      3);
    rootColumns.add(new PlotPageColumnHeading(
      FileDefinition.TIME_COLUMN_HEADING, false, false));

    if (!dataset.fixedPosition()) {
      rootColumns
        .add(new PlotPageColumnHeading(FileDefinition.LONGITUDE_COLUMN_ID,
          "Position", "Position", "POSITION", null, false, true));
    }

    columnHeadings.put(ROOT_FIELD_GROUP, rootColumns);

    // Extended Time and Position
    List<PlotPageColumnHeading> extendedRootColumns = new ArrayList<PlotPageColumnHeading>(
      3);
    extendedRootColumns.add(new PlotPageColumnHeading(
      FileDefinition.TIME_COLUMN_HEADING, false, false));

    if (!dataset.fixedPosition()) {
      extendedRootColumns.add(new PlotPageColumnHeading(
        FileDefinition.LONGITUDE_COLUMN_HEADING, false, true));
      extendedRootColumns
        .add(new PlotPageColumnHeading(FileDefinition.LATITUDE_COLUMN_HEADING,
          false, true, FileDefinition.LONGITUDE_COLUMN_ID));
    }

    extendedColumnHeadings.put(ROOT_FIELD_GROUP, extendedRootColumns);

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

    List<PlotPageColumnHeading> sensorColumnHeadings = new ArrayList<PlotPageColumnHeading>(
      sensorColumns.size());
    sensorColumnIds = new ArrayList<Long>(sensorColumns.size());

    for (int i = 0; i < sensorColumns.size(); i++) {

      SensorAssignment column = sensorColumns.get(i);
      sensorColumnHeadings
        .add(new PlotPageColumnHeading(column.getColumnHeading(), true, true));
      sensorColumnIds.add(column.getDatabaseId());
    }

    columnHeadings.put(SENSORS_FIELD_GROUP, sensorColumnHeadings);
    extendedColumnHeadings.put(SENSORS_FIELD_GROUP, sensorColumnHeadings);

    diagnosticColumnIds = new ArrayList<Long>(diagnosticColumns.size());
    if (diagnosticColumns.size() > 0) {
      List<PlotPageColumnHeading> diagnosticColumnNames = new ArrayList<PlotPageColumnHeading>(
        diagnosticColumns.size());

      for (int i = 0; i < diagnosticColumns.size(); i++) {

        SensorAssignment column = diagnosticColumns.get(i);

        diagnosticColumnNames.add(
          new PlotPageColumnHeading(column.getColumnHeading(), true, true));
        diagnosticColumnIds.add(column.getDatabaseId());
      }

      columnHeadings.put(DIAGNOSTICS_FIELD_GROUP, diagnosticColumnNames);
      extendedColumnHeadings.put(DIAGNOSTICS_FIELD_GROUP,
        diagnosticColumnNames);
    }

    // Drop the Measurement Values in the correct place in the column heading
    // maps.
    // We will fill it in properly later
    columnHeadings.put(MEASUREMENTVALUES_FIELD_GROUP, null);
    extendedColumnHeadings.put(MEASUREMENTVALUES_FIELD_GROUP, null);

    // We use a TreeSet to maintain order and uniqueness
    measurementSensorTypes = new TreeSet<SensorType>();

    // Each of the instrument variables
    for (Variable variable : instrument.getVariables()) {

      // Get the SensorTypes for this variable
      variable.getAllSensorTypes(true).forEach(measurementSensorTypes::add);

      // Now the calculation parameters
      try {
        List<CalculationParameter> variableHeadings = DataReducerFactory
          .getCalculationParameters(variable, true, true);

        List<PlotPageColumnHeading> variableQCHeadings = new ArrayList<PlotPageColumnHeading>(
          variableHeadings.size());
        variableHeadings.stream()
          .forEach(x -> variableQCHeadings.add(new PlotPageColumnHeading(x)));

        columnHeadings.put(variable.getName(), variableQCHeadings);
        extendedColumnHeadings.put(variable.getName(), variableQCHeadings);
      } catch (DataReductionException e) {
        error("Error getting variable headers", e);
      }
    }

    // Now we can fill in the proper MeasurementValue column headings
    List<PlotPageColumnHeading> measurementValueColumnNames = measurementSensorTypes
      .stream().map(x -> new PlotPageColumnHeading(x))
      .collect(Collectors.toList());

    columnHeadings.put(MEASUREMENTVALUES_FIELD_GROUP,
      measurementValueColumnNames);
    extendedColumnHeadings.put(MEASUREMENTVALUES_FIELD_GROUP,
      measurementValueColumnNames);
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

        // Get the run type from the closest measurement
        Measurement concurrentMeasurement = getConcurrentMeasurement(
          times.get(i));
        String runType = null == concurrentMeasurement ? null
          : concurrentMeasurement.getRunType();

        // Timestamp
        record.addColumn(times.get(i));

        Map<Long, SensorValue> recordSensorValues = sensorValues
          .get(times.get(i));

        if (!dataset.fixedPosition()) {

          // We assume there's only one position - the UI won't allow users to
          // enter more than one.
          //
          // The position is combined into a single column.
          SensorValue longitude = recordSensorValues
            .get(FileDefinition.LONGITUDE_COLUMN_ID);
          SensorValue latitude = recordSensorValues
            .get(FileDefinition.LATITUDE_COLUMN_ID);

          // The lon/lat can be null if the instrument has a fixed position
          // or the file containing the current values doesn't contain a
          // position.
          if (null != longitude && null != latitude
            && null != longitude.getValue() && null != latitude.getValue()) {

            StringBuilder positionString = new StringBuilder();
            if (null != longitude.getValue() && null != latitude.getValue()) {
              positionString
                .append(StringUtils.formatNumber(longitude.getValue()));
              positionString.append(" | ");
              positionString
                .append(StringUtils.formatNumber(latitude.getValue()));
            }

            record.addColumn(positionString.toString(),
              longitude.getDisplayFlag(), longitude.getDisplayQCMessage(),
              longitude.flagNeeded());
          } else {
            // Empty position column
            record.addColumn("", Flag.GOOD, null, false);
          }
        }

        for (long columnId : sensorColumnIds) {

          // If the sensor type has internal calibrations, AND we're in a run
          // type for the internal calibrations, don't include it.
          SensorType sensorType = instrument.getSensorAssignments()
            .getSensorTypeForDBColumn(columnId);

          if (sensorType.hasInternalCalibration()
            && (null == runType || instrument.getRunTypeCategory(runType)
              .equals(RunTypeCategory.INTERNAL_CALIBRATION))) {
            record.addBlankColumn();
          } else {
            record.addColumn(recordSensorValues.get(columnId));
          }
        }

        // Diagnostic values
        if (null != diagnosticColumnIds) {
          for (long columnId : diagnosticColumnIds) {
            record.addColumn(recordSensorValues.get(columnId));
          }
        }

        Long measurementId = null;
        Measurement measurement = measurements.get(times.get(i));
        if (null != measurement) {
          measurementId = measurement.getId();
        }

        if (null == measurement) {
          record.addBlankColumns(measurementSensorTypes.size());
        } else {
          // MeasurementValues
          measurementSensorTypes.forEach(s -> {

            record.addColumn(measurement.containsMeasurementValue(s)
              ? measurement.getMeasurementValue(s)
              : new NullPlotPageTableValue());
          });
        }

        Map<Variable, DataReductionRecord> dataReductionData = null;

        if (null != measurementId) {
          // Retrieve the data reduction data
          dataReductionData = dataReduction.get(measurementId);
        }

        // If there's no measurement, or no data reduction for that measurement
        // (which can happen if the instrument is in a flushing period or in
        // calibration mode), make a blank data reduction set.
        if (null == dataReductionData) {
          // Make a blank set
          dataReductionData = new HashMap<Variable, DataReductionRecord>();
          for (Variable variable : instrument.getVariables()) {
            dataReductionData.put(variable, null);
          }
        }

        // Variables
        for (Variable variable : instrument.getVariables()) {
          DataReductionRecord variableDataReduction = dataReductionData
            .get(variable);

          if (null != variableDataReduction) {
            List<CalculationParameter> params = DataReducerFactory
              .getCalculationParameters(variable, true, true);

            for (CalculationParameter param : params) {
              Double value = variableDataReduction
                .getCalculationValue(param.getShortName());
              String stringValue = null == value ? "" : String.valueOf(value);

              record.addColumn(stringValue, variableDataReduction.getQCFlag(),
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
  public List<Long> getRowIDs() {
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

      // If we QCed the position update all related sensor values
      if (FileDefinition.LONGITUDE_COLUMN_ID == selectedColumn) {
        sensorValues.addAll(propagatePositionQC(sensorValues));
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
   * Get the {@link SensorValue}s for the current selection.
   *
   * @return The {@link SensorValue}s.
   */
  private List<SensorValue> getSelectedSensorValues() {

    List<SensorValue> values = new ArrayList<SensorValue>(selectedRows.size());

    if (null != selectedRows) {
      for (Long rowId : selectedRows) {
        values.add(sensorValues.getSensorValue(DateTimeUtils.longToDate(rowId),
          selectedColumn));
      }
    }

    return values;
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
      try (Connection conn = dataSource.getConnection()) {
        DataSetDataDB.storeSensorValues(conn, selectedValues);
      }

      initPlots();

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

  /**
   * Determine whether or not a cell can be selected by the user.
   *
   * <p>
   * A cell is selectable if:
   * </p>
   * <ul>
   * <li>The column is editable</li>
   * <li>The cell is not empty</li>
   * <li>The cell is not a ghost ({@link #isGhost(SensorValue)})</li>
   * </ul>
   *
   * @param row
   *          The row ID.
   * @param column
   *          The column.
   * @return {@code true} if the cell can be selected; {@code false} if not.
   * @throws Exception
   * @see #isGhost(SensorValue)
   */
  @Override
  protected boolean canSelectCell(long row, long column) throws Exception {

    boolean selectable = isColumnEditable(column);

    if (selectable) {
      SensorValue sensorValue = sensorValues.get(DateTimeUtils.longToDate(row))
        .get(column);
      if (sensorValue.isNaN() || isGhost(sensorValue)) {
        selectable = false;
      }
    }

    return selectable;
  }

  /**
   * Determine whether or not a given {@link SensorValue} should be regarded as
   * a ghost value (i.e. visible but not editable).
   *
   * <p>
   * For Manual QC, a value is a ghost if it has its QC flag set to
   * {@link Flag#FLUSHING}.
   * </p>
   *
   * @param sensorValue
   *          The sensor value.
   * @return {@code true} if the value is a ghost; {@code false} otherwise.
   */
  private boolean isGhost(SensorValue sensorValue) {
    return sensorValue.getUserQCFlag().equals(Flag.FLUSHING);
  }

  @Override
  protected TreeMap<LocalDateTime, PlotPageTableValue> getColumnValues(
    PlotPageColumnHeading column) throws Exception {

    TreeMap<LocalDateTime, PlotPageTableValue> result = new TreeMap<LocalDateTime, PlotPageTableValue>();

    SensorType sensorType = instrument.getSensorAssignments()
      .getSensorTypeForDBColumn(column.getId());

    if (column.getId() == FileDefinition.TIME_COLUMN_ID) {
      for (LocalDateTime time : sensorValues.getTimes()) {
        result.put(time, new SimplePlotPageTableValue(time, true));
      }
    } else if (sensorValues.containsColumn(column.getId())) {

      // If the sensor type doesn't have internal calibrations, add all values
      if (!sensorType.hasInternalCalibration()) {
        for (SensorValue sensorValue : sensorValues
          .getColumnValues(column.getId())) {

          result.put(sensorValue.getTime(),
            new SensorValuePlotPageTableValue(sensorValue));
        }
      } else {

        for (SensorValue sensorValue : sensorValues
          .getColumnValues(column.getId())) {

          // Get the run type from the closest measurement
          Measurement concurrentMeasurement = getConcurrentMeasurement(
            sensorValue.getTime());
          String runType = null == concurrentMeasurement ? null
            : concurrentMeasurement.getRunType();

          // Only include the value if the run type is not an internal
          // calibration
          if (null != runType && !instrument.getRunTypeCategory(runType)
            .equals(RunTypeCategory.INTERNAL_CALIBRATION)) {

            result.put(sensorValue.getTime(),
              new SensorValuePlotPageTableValue(sensorValue));
          }
        }
      }
    } else {

      Variable variable = DataReducerFactory.getVariable(instrument,
        column.getId());
      CalculationParameter parameter = DataReducerFactory
        .getVariableParameter(variable, column.getId());

      for (Map.Entry<LocalDateTime, Measurement> measurement : measurements
        .entrySet()) {

        if (dataReduction.containsKey(measurement.getValue().getId())) {
          DataReductionRecord record = dataReduction
            .get(measurement.getValue().getId()).get(variable);
          if (null != record) {
            result.put(measurement.getKey(),
              new DataReductionRecordPlotPageTableValue(record,
                parameter.getShortName()));
          }
        }
      }
    }

    return result;
  }

  @Override
  protected PlotPageColumnHeading getDefaultYAxis1() {
    // The first sensor
    return columnHeadings.get(SENSORS_FIELD_GROUP).get(0);
  }

  @Override
  protected PlotPageColumnHeading getDefaultYAxis2() throws Exception {

    PlotPageColumnHeading result = null;

    // The core sensor value for the first variable, or if there isn't one,
    // The second sensor
    Variable variable = instrument.getVariables().get(0);
    SensorType sensorType = variable.getCoreSensorType();

    if (null != sensorType) {
      long coreColumn = instrument.getSensorAssignments()
        .getColumnIds(sensorType).get(0);
      result = getColumnHeading(coreColumn);
    } else {
      return columnHeadings.get(SENSORS_FIELD_GROUP).get(1);
    }

    return result;
  }

  /**
   * Get the number of {@link SensorValue}s whose QC flag is
   * {@link Flag#NEEDED}, grouped by column ID.
   *
   * @return The number of NEEDED flags
   */
  public Map<Long, Integer> getNeedsFlagCounts() {
    return null == sensorValues ? null : sensorValues.getNeedsFlagCounts();
  }

  public PlotPageTableValue getColumnValue(long rowId, long columnId)
    throws InstrumentException, DataReductionException,
    RecordNotFoundException {

    PlotPageTableValue result = null;

    // The rowId is the row time
    LocalDateTime rowTime = DateTimeUtils.longToDate(rowId);

    // The time is just the time
    if (columnId == FileDefinition.TIME_COLUMN_ID) {
      result = new SimplePlotPageTableValue(rowTime, false);

      // Sensor Value
    } else if (sensorValues.containsColumn(columnId)) {

      // Get the SensorValue
      SensorValue sensorValue = sensorValues.getSensorValue(rowTime, columnId);
      if (null != sensorValue) {

        SensorType sensorType = instrument.getSensorAssignments()
          .getSensorTypeForDBColumn(columnId);

        // If the sensor has internal calibrations, only add the value if it's
        // a measurement
        boolean useValue = true;

        if (sensorType.hasInternalCalibration()) {

          Measurement concurrentMeasurement = getConcurrentMeasurement(
            sensorValue.getTime());
          String runType = null == concurrentMeasurement ? null
            : concurrentMeasurement.getRunType();

          // Only include the value if the run type is not an internal
          // calibration
          if (null == runType || instrument.getRunTypeCategory(runType)
            .equals(RunTypeCategory.INTERNAL_CALIBRATION)) {
            useValue = false;
          }
        }

        if (useValue) {
          result = new SensorValuePlotPageTableValue(sensorValue);
        }
      }

      // Data Reduction value
    } else {
      Variable variable = DataReducerFactory.getVariable(instrument, columnId);
      CalculationParameter parameter = DataReducerFactory
        .getVariableParameter(variable, columnId);

      Measurement measurement = measurements.get(rowTime);
      if (null != measurement) {
        if (dataReduction.containsKey(measurement.getId())) {
          DataReductionRecord record = dataReduction.get(measurement.getId())
            .get(variable);
          if (null != record) {
            result = new DataReductionRecordPlotPageTableValue(record,
              parameter.getShortName());
          }
        }
      }
    }

    return result;
  }

  protected DataReductionRecord getDataReductionRecord(long rowId,
    Variable variable) {

    DataReductionRecord result = null;

    LocalDateTime rowTime = DateTimeUtils.longToDate(rowId);
    Measurement measurement = measurements.get(rowTime);

    if (null != measurement) {
      Map<Variable, DataReductionRecord> rowRecords = dataReduction
        .get(measurement.getId());
      if (null != rowRecords) {
        result = rowRecords.get(variable);
      }
    }

    return result;
  }

  private Measurement getConcurrentMeasurement(LocalDateTime time) {
    Measurement concurrentMeasurement = null;
    LocalDateTime measurementTime = measurements.floorKey(time);
    if (null != measurementTime) {
      concurrentMeasurement = measurements.get(measurementTime);
    }
    return concurrentMeasurement;
  }
}
