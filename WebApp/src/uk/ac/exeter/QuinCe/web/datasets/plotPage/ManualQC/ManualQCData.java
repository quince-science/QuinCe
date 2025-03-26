package uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValuesListException;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValuesListOutput;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValuesListValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.CalculationParameter;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionException;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.ReadOnlyDataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.PositionException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategoryException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.utils.ValueCounter;
import uk.ac.exeter.QuinCe.web.Progress;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.DataLatLng;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.DataReductionRecordPlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.NullPlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageColumnHeading;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageData;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageDataException;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableRecord;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.SensorValuePlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.SimplePlotPageTableValue;

/**
 * A version of {@link PlotPageData} used for the main manual QC page.
 *
 * <p>
 * <b>A note on positions:</b>
 * </p>
 * <p>
 * Positions can be stored in two places: in the {@link SensorValue}s and in the
 * {@link MeasurementValue}s. The former contains the raw, measured position
 * from the sensors, and the latter contains the calculated position for a
 * measurement. For instruments with one input file, these will be the same. For
 * other files, though, they will not. The {@link MeasurementValue} may store an
 * interpolated position, and some data rows with no measurements may not
 * contain a position at all if that's measured in a different data file.
 * </p>
 * <p>
 * Because of this, positions are present in the ROOT column group, but are
 * populated as follows:
 * </p>
 * <ol>
 * <li>If there is a measurement at the time of the row, then use the
 * {@link MeasurementValue} position.</li>
 * <li>If there is no measurement but there is position data in the row's
 * {@link SensorValue}s, use those.</li>
 * <li>Finally, interpolate the position using the closest available position
 * {@link SensorValue}s.
 * </ol>
 */
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
  private TreeSet<MeasurementValueSensorType> measurementSensorTypes = null;

  /**
   * All row IDs for the dataset. Row IDs are the millisecond values of the
   * times.
   */
  protected List<Long> rowIDs = null;

  /**
   * The dataset's sensor values.
   */
  protected DatasetSensorValues sensorValues = null;

  /**
   * The values calculated by data reduction.
   */
  protected Map<Long, Map<Variable, ReadOnlyDataReductionRecord>> dataReduction = null;

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
  public void loadDataAction(Progress progress) throws Exception {

    try (Connection conn = dataSource.getConnection()) {

      // Fake value after "initialising" message
      progress.setValue(5F);

      progress.setName("Loading sensor data");
      sensorValues = DataSetDataDB.getSensorValues(conn, instrument,
        dataset.getId(), false, true);
      progress.setValue(33F);

      progress.setName("Loading measurements");
      List<Measurement> measurementsList = DataSetDataDB.getMeasurements(conn,
        dataset.getId());
      progress.setValue(66F);

      measurements = new TreeMap<LocalDateTime, Measurement>();

      measurementsList.forEach(m -> measurements.put(m.getTime(), m));

      progress.setName("Loading data reduction");
      dataReduction = DataSetDataDB.getDataReductionData(conn, instrument,
        dataset);
      progress.setValue(100F);

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
      FileDefinition.TIME_COLUMN_HEADING, false, false, false));

    if (!dataset.fixedPosition()) {
      rootColumns
        .add(new PlotPageColumnHeading(FileDefinition.LONGITUDE_COLUMN_ID,
          "Position", "Position", "POSITION", null, true, false, false, false));
    }

    columnHeadings.put(ROOT_FIELD_GROUP, rootColumns);

    // Extended Time and Position
    List<PlotPageColumnHeading> extendedRootColumns = new ArrayList<PlotPageColumnHeading>(
      3);
    extendedRootColumns.add(new PlotPageColumnHeading(
      FileDefinition.TIME_COLUMN_HEADING, false, false, false));

    if (!dataset.fixedPosition()) {
      extendedRootColumns.add(new PlotPageColumnHeading(
        FileDefinition.LONGITUDE_COLUMN_HEADING, false, false, false));
      extendedRootColumns
        .add(new PlotPageColumnHeading(FileDefinition.LATITUDE_COLUMN_HEADING,
          false, false, false, FileDefinition.LONGITUDE_COLUMN_ID));
    }

    extendedColumnHeadings.put(ROOT_FIELD_GROUP, extendedRootColumns);

    // Sensor Assignments are divided into sensors and diagnostics
    List<SensorAssignment> sensorColumns = new ArrayList<SensorAssignment>();
    List<SensorAssignment> diagnosticColumns = new ArrayList<SensorAssignment>();

    for (Map.Entry<SensorType, TreeSet<SensorAssignment>> entry : instrument
      .getSensorAssignments().entrySet()) {

      // Skip the position
      if (!entry.getKey().isPosition()) {
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
        .add(new PlotPageColumnHeading(column.getColumnHeading(), true, true,
          column.getSensorType().questionableFlagAllowed()));
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

        diagnosticColumnNames
          .add(new PlotPageColumnHeading(column.getColumnHeading(), true, true,
            column.getSensorType().questionableFlagAllowed()));
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
    measurementSensorTypes = new TreeSet<MeasurementValueSensorType>();

    // Each of the instrument variables
    for (Variable variable : instrument.getVariables()) {

      // Get the SensorTypes for this variable
      variable.getAllSensorTypes(true).stream()
        .filter(
          s -> instrument.getSensorAssignments().isAssigned(s, false, true))
        .forEach(s -> {
          measurementSensorTypes.add(new MeasurementValueSensorType(s));
        });

      // Now the calculation parameters
      try {
        List<CalculationParameter> variableHeadings = DataReducerFactory
          .getCalculationParameters(variable, true);

        if (variableHeadings.size() > 0) {
          List<PlotPageColumnHeading> variableQCHeadings = new ArrayList<PlotPageColumnHeading>(
            variableHeadings.size());
          variableHeadings.stream()
            .forEach(x -> variableQCHeadings.add(new PlotPageColumnHeading(x)));

          columnHeadings.put(variable.getName(), variableQCHeadings);
          extendedColumnHeadings.put(variable.getName(), variableQCHeadings);
        }
      } catch (DataReductionException e) {
        error("Error getting variable headers", e);
      }
    }

    // Now we can fill in the proper MeasurementValue column headings.
    // Skip the positions though - see main class description
    List<PlotPageColumnHeading> measurementValueColumnNames = measurementSensorTypes
      .stream().filter(s -> !s.isPosition())
      .map(s -> new PlotPageColumnHeading(s)).collect(Collectors.toList());

    columnHeadings.put(MEASUREMENTVALUES_FIELD_GROUP,
      measurementValueColumnNames);
    extendedColumnHeadings.put(MEASUREMENTVALUES_FIELD_GROUP,
      measurementValueColumnNames);
  }

  @Override
  public int size() {
    return (null != rowIDs ? rowIDs.size() : -1);
  }

  @Override
  public List<PlotPageTableRecord> generateTableDataRecords(int start,
    int length) {

    List<PlotPageTableRecord> records = new ArrayList<PlotPageTableRecord>(
      length);

    try {

      List<LocalDateTime> times = sensorValues.getTimes();

      // Make sure we don't fall off the end of the dataset
      int lastRecord = start + length;
      if (lastRecord > times.size()) {
        lastRecord = times.size();
      }

      for (int i = start; i < lastRecord; i++) {
        PlotPageTableRecord record = new PlotPageTableRecord(times.get(i));

        // Get the closest measurement
        Measurement concurrentMeasurement = getConcurrentMeasurement(
          times.get(i));

        // Timestamp
        record.addColumn(times.get(i));

        Map<Long, SensorValue> recordSensorValues = sensorValues
          .get(times.get(i));

        if (!dataset.fixedPosition()) {

          DataLatLng position = getMapPosition(times.get(i));

          if (null != position) {
            StringBuilder positionString = new StringBuilder();
            positionString
              .append(StringUtils.formatNumber(position.getLongitude()));
            positionString.append(" | ");
            positionString
              .append(StringUtils.formatNumber(position.getLatitude()));

            record.addColumn(positionString.toString(),
              position.getFlag(getAllSensorValues()),
              position.getQcMessage(sensorValues), position.getFlagNeeded(),
              position.getType(), position.getSourceIds());
          } else {
            // Empty position column
            record.addColumn("", Flag.GOOD, null, false,
              PlotPageTableValue.NAN_TYPE, null);
          }
        }

        for (long columnId : sensorColumnIds) {

          // If the sensor type has internal calibrations, AND we're in a run
          // type for the internal calibrations, don't include it.
          SensorType sensorType = instrument.getSensorAssignments()
            .getSensorTypeForDBColumn(columnId);

          boolean useValue = true;

          if (null == concurrentMeasurement) {
            if (isCoreSensorType(sensorType)) {
              useValue = false;
            }
          } else {
            if (!isMeasurementForAnyVariable(concurrentMeasurement)
              && (isCoreSensorType(sensorType)
                || sensorType.hasInternalCalibration())) {
              useValue = false;
            }
          }

          if (useValue) {
            record.addColumn(recordSensorValues.get(columnId));
          } else {
            record.addBlankColumn(PlotPageTableValue.MEASURED_TYPE);
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
          record.addBlankColumns(measurementSensorTypes.size(),
            PlotPageTableValue.MEASURED_TYPE);
        } else {
          // MeasurementValues
          measurementSensorTypes.forEach(s -> {

            if (!s.isPosition()) {
              record.addColumn(measurement.hasMeasurementValue(s)
                ? measurement.getMeasurementValue(s)
                : new NullPlotPageTableValue());
            }

          });
        }

        Map<Variable, ReadOnlyDataReductionRecord> dataReductionData = null;

        if (null != measurementId) {
          // Retrieve the data reduction data
          dataReductionData = dataReduction.get(measurementId);
        }

        // If there's no measurement, or no data reduction for that measurement
        // (which can happen if the instrument is in a flushing period or in
        // calibration mode), make a blank data reduction set.
        if (null == dataReductionData) {
          // Make a blank set
          dataReductionData = new HashMap<Variable, ReadOnlyDataReductionRecord>();
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
              .getCalculationParameters(variable, true);

            for (CalculationParameter param : params) {
              Double value = variableDataReduction
                .getCalculationValue(param.getShortName());
              String stringValue = null == value ? "" : String.valueOf(value);

              record.addColumn(stringValue, variableDataReduction.getQCFlag(),
                variableDataReduction.getQCMessages().toString(), false,
                PlotPageTableValue.DATA_REDUCTION_TYPE,
                Arrays.asList(variableDataReduction.getMeasurementId()));
            }
          } else {
            // Make blank columns because this measurement doesn't have data
            // reduction for the variable.
            if (columnHeadings.containsKey(variable.getName())) {
              record.addBlankColumns(
                columnHeadings.get(variable.getName()).size(),
                PlotPageTableValue.DATA_REDUCTION_TYPE);
            }
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

  private boolean isCoreSensorType(SensorType sensorType) {
    return instrument.getVariables().stream()
      .anyMatch(v -> v.getCoreSensorType().equals(sensorType));
  }

  private boolean isMeasurementForAnyVariable(Measurement measurement)
    throws RunTypeCategoryException {
    boolean result = false;

    for (Map.Entry<Long, String> runTypeEntry : measurement.getRunTypes()
      .entrySet()) {
      if (instrument.getRunTypeCategory(runTypeEntry).isMeasurementType()) {
        result = true;
        break;
      }
    }

    return result;
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
      List<SensorValue> selectedValues = getSelectedSensorValues();

      List<SensorValue> changedValues = new ArrayList<SensorValue>(
        selectedValues.size() * 2);

      for (SensorValue sensorValue : selectedValues) {
        // Only override the existing user QC if it has Needs Flag or Assumed
        // Good

        SensorValue otherPositionValue = null;

        if (SensorType.isPosition(sensorValue.getColumnId())) {
          if (sensorValue.getColumnId() == SensorType.LONGITUDE_ID) {
            otherPositionValue = sensorValues
              .getRawSensorValue(SensorType.LATITUDE_ID, sensorValue.getTime());
          } else {
            otherPositionValue = sensorValues.getRawSensorValue(
              SensorType.LONGITUDE_ID, sensorValue.getTime());
          }
        }

        Flag setFlag = null;
        String setMessage = null;

        setFlag = sensorValue.getAutoQcFlag();
        setMessage = sensorValue.getAutoQcResult().getAllMessages();

        sensorValue.setUserQC(setFlag, setMessage);
        changedValues.add(sensorValue);

        if (null != otherPositionValue && null != setFlag) {
          otherPositionValue.setUserQC(setFlag, setMessage);
          changedValues.add(otherPositionValue);
        }
      }

      try (Connection conn = dataSource.getConnection()) {
        DataSetDataDB.storeSensorValues(conn, changedValues);
      }
      clearSelection();
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
        values.add(sensorValues.getRawSensorValue(selectedColumn,
          DateTimeUtils.longToDate(rowId)));
      }
    }

    return values;
  }

  /**
   * Generate the QC comments list and find the worst QC flag from the currently
   * selected values.
   */
  public void generateUserComment() {

    ValueCounter comments = new ValueCounter();
    userFlag = Flag.GOOD;

    for (SensorValue sensorValue : getSelectedSensorValues()) {
      if (sensorValue.getDisplayFlag(getAllSensorValues())
        .moreSignificantThan(userFlag)) {
        userFlag = sensorValue.getDisplayFlag(getAllSensorValues());
      }

      if (!sensorValue.flagNeeded()
        && !sensorValue.getUserQCFlag().equals(Flag.LOOKUP)) {
        comments.add(sensorValue.getUserQCMessage());
      } else {
        try {
          comments.addAll(sensorValue.getAutoQcResult().getAllMessagesSet());
        } catch (RoutineException e) {
          error("Error getting QC comments", e);
        }
      }
    }

    userComment = comments.toString();
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
      Set<SensorValue> changedValues = new HashSet<SensorValue>();

      List<SensorValue> selectedValues = getSelectedSensorValues();

      for (SensorValue value : selectedValues) {

        SensorValue otherPositionValue = null;

        if (SensorType.isPosition(value.getColumnId())) {
          if (value.getColumnId() == SensorType.LONGITUDE_ID) {
            otherPositionValue = sensorValues
              .getRawSensorValue(SensorType.LATITUDE_ID, value.getTime());
          } else {
            otherPositionValue = sensorValues
              .getRawSensorValue(SensorType.LONGITUDE_ID, value.getTime());
          }
        }

        value.setUserQC(userFlag, userComment);
        changedValues.add(value);
        changedValues
          .addAll(sensorValues.applyQCCascade(value, runTypePeriods));

        if (null != otherPositionValue) {
          otherPositionValue.setUserQC(userFlag, userComment);
          changedValues.add(otherPositionValue);
          changedValues.addAll(
            sensorValues.applyQCCascade(otherPositionValue, runTypePeriods));
        }
      }

      // Store the updated sensor values
      try (Connection conn = dataSource.getConnection()) {
        DataSetDataDB.storeSensorValues(conn, changedValues);
      }

      clearSelection();
      initPlots();

    } catch (Exception e) {
      error("Error storing QC data", e);
    }
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
      if (null == sensorValue || sensorValue.isNaN() || isGhost(sensorValue)) {
        selectable = false;
      }
    }

    return selectable;
  }

  @Override
  protected List<LocalDateTime> getDataTimes() {
    return sensorValues.getTimes();
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

    List<Long> sensorColumnIds = instrument.getSensorAssignments()
      .getSensorColumnIds();

    List<Long> diagnosticColumnIds = instrument.getSensorAssignments()
      .getDiagnosticColumnIds();

    if (column.getId() == FileDefinition.TIME_COLUMN_ID) {
      for (LocalDateTime time : getDataTimes()) {
        result.put(time, new SimplePlotPageTableValue(time, null, true));
      }
    } else if (SensorType.isPosition(column.getId())) {
      List<SensorValuesListValue> values = sensorValues
        .getColumnValues(column.getId()).getValues();
      values.forEach(
        v -> result.put(v.getTime(), new MeasurementValue(v.getSensorType(),
          new SensorValuesListOutput(v, false))));
    } else if (sensorColumnIds.contains(column.getId())
      || diagnosticColumnIds.contains(column.getId())) {

      SensorType sensorType = instrument.getSensorAssignments()
        .getSensorTypeForDBColumn(column.getId());

      // For some reason doing this in a single if statement didn't work.
      // ¯\_(ツ)_/¯
      boolean useAllValues = true;
      if (isCoreSensorType(sensorType)) {
        useAllValues = false;
      }
      if (sensorType.hasInternalCalibration()) {
        useAllValues = false;
      }

      SensorValuesList svList = sensorValues.getColumnValues(column.getId());
      if (null != svList) {
        if (useAllValues) {
          for (SensorValue sensorValue : svList.getRawValues()) {

            result.put(sensorValue.getTime(),
              new SensorValuePlotPageTableValue(sensorValue));
          }
        } else {
          for (SensorValue sensorValue : svList.getRawValues()) {

            // Get the run type from the closest measurement
            Measurement concurrentMeasurement = getConcurrentMeasurement(
              sensorValue.getTime());

            // Only include the value if the run type is not an internal
            // calibration
            if (null != concurrentMeasurement
              && isMeasurementForAnyVariable(concurrentMeasurement)) {
              result.put(sensorValue.getTime(),
                new SensorValuePlotPageTableValue(sensorValue));
            }
          }
        }
      }
    } else {

      // See if we have a proxy SensorType used for Measurement Values
      SensorType sensorType = null;

      try {
        sensorType = MeasurementValueSensorType.getSensorType(column);
      } catch (SensorTypeNotFoundException e) {
        // This just means we're not using a SensorType
      }

      if (null != sensorType) {
        for (Map.Entry<LocalDateTime, Measurement> entry : measurements
          .entrySet()) {
          if (entry.getValue().hasMeasurementValue(sensorType)) {
            result.put(entry.getKey(),
              entry.getValue().getMeasurementValue(sensorType));
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
    }

    return result;
  }

  @Override
  protected PlotPageColumnHeading getDefaultYAxis1() throws Exception {
    // The first sensor
    return columnHeadings.get(SENSORS_FIELD_GROUP).get(0);
  }

  @Override
  protected PlotPageColumnHeading getDefaultYAxis2() throws Exception {
    Variable variable = instrument.getVariables().get(0);
    SensorType coreSensorType = variable.getCoreSensorType();
    long coreColumn = instrument.getSensorAssignments()
      .getColumnIds(coreSensorType).get(0);
    return getColumnHeading(coreColumn);
  }

  /**
   * Get the number of {@link SensorValue}s whose QC flag is
   * {@link Flag#NEEDED}, grouped by column ID.
   *
   * @return The number of NEEDED flags
   */
  public Map<Long, Integer> getNeedsFlagCounts() {
    return null == sensorValues ? null
      : sensorValues.getNonPositionNeedsFlagCounts();
  }

  /**
   * Get a value for a {@link SensorValue} or a {@link CalculationParameter}
   * from Data Reduction. Note that Measurement Values must be retrieved
   * directly from the Measurement object.
   *
   * @param rowId
   *          The row ID
   * @param columnId
   *          The column/paramtere ID
   * @return The value
   * @throws InstrumentException
   * @throws DataReductionException
   * @throws RecordNotFoundException
   */
  public PlotPageTableValue getColumnValue(long rowId, long columnId)
    throws PlotPageDataException {

    PlotPageTableValue result = null;

    try {
      // The rowId is the row time
      LocalDateTime rowTime = DateTimeUtils.longToDate(rowId);

      List<Long> sensorColumnIds = instrument.getSensorAssignments()
        .getSensorColumnIds();

      List<Long> diagnosticColumnIds = instrument.getSensorAssignments()
        .getDiagnosticColumnIds();

      // The time is just the time
      if (columnId == FileDefinition.TIME_COLUMN_ID) {
        result = new SimplePlotPageTableValue(rowTime, null, false);

      } else if (columnId == FileDefinition.LONGITUDE_COLUMN_ID) {
        result = getInterpolatedPositionValue(SensorType.LONGITUDE_SENSOR_TYPE,
          DateTimeUtils.longToDate(rowId));

      } else if (columnId == FileDefinition.LATITUDE_COLUMN_ID) {
        result = getInterpolatedPositionValue(SensorType.LATITUDE_SENSOR_TYPE,
          DateTimeUtils.longToDate(rowId));

        // Sensor Value
      } else if (sensorColumnIds.contains(columnId)
        || diagnosticColumnIds.contains(columnId)) {

        // Get the SensorValue
        SensorValue sensorValue = sensorValues.getRawSensorValue(columnId,
          rowTime);
        if (null != sensorValue) {

          SensorType sensorType = instrument.getSensorAssignments()
            .getSensorTypeForDBColumn(columnId);

          // If the sensor has internal calibrations, only add the value if it's
          // a measurement
          boolean useValue = true;

          if (sensorType.hasInternalCalibration()) {

            Measurement concurrentMeasurement = getConcurrentMeasurement(
              sensorValue.getTime());

            // Only include the value if the run type is not an internal
            // calibration
            if (null != concurrentMeasurement
              && !isMeasurementForAnyVariable(concurrentMeasurement)) {
              useValue = false;
            }
          }

          if (useValue) {
            result = new SensorValuePlotPageTableValue(sensorValue);
          }
        }

        // Data Reduction value
      } else {
        Variable variable = DataReducerFactory.getVariable(instrument,
          columnId);
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
    } catch (PlotPageDataException e) {
      throw e;
    } catch (Exception e) {
      throw new PlotPageDataException("Error getting column value", e);
    }

    return result;
  }

  protected DataReductionRecord getDataReductionRecord(long rowId,
    Variable variable) {

    DataReductionRecord result = null;

    LocalDateTime rowTime = DateTimeUtils.longToDate(rowId);
    Measurement measurement = measurements.get(rowTime);

    if (null != measurement) {
      Map<Variable, ReadOnlyDataReductionRecord> rowRecords = dataReduction
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

  public TreeSet<MeasurementValueSensorType> getMeasurementSensorTypes() {
    return measurementSensorTypes;
  }

  public LocalDateTime getRowTime(long rowId) {
    return DateTimeUtils.longToDate(rowId);
  }

  public Measurement getMeasurement(LocalDateTime time) {
    return measurements.get(time);
  }

  protected boolean headingGroupContains(String group, long columnId) {
    boolean result = false;

    for (PlotPageColumnHeading heading : extendedColumnHeadings.get(group)) {

      if (heading.getId() == columnId) {
        result = true;
        break;
      }
    }

    return result;
  }

  protected PlotPageTableValue getInterpolatedPositionValue(
    SensorType sensorType, LocalDateTime time)
    throws PlotPageDataException, PositionException, SensorValuesListException {

    PlotPageTableValue result = null;

    if (sensorType.isPosition()) {
      // If there is a measurement at this time, try using the value from that
      Measurement measurement = null == measurements ? null
        : measurements.get(time);

      if (null != measurement) {
        if (measurement.hasMeasurementValue(sensorType)) {
          result = measurement.getMeasurementValue(sensorType);
        }
      }

      // Try getting SensorValues from the current row
      if (null == result) {
        Map<Long, SensorValue> recordSensorValues = sensorValues.get(time);
        if (null != recordSensorValues) {
          SensorValue sensorValue = recordSensorValues.get(sensorType.getId());
          if (null != sensorValue && null != sensorValue.getValue()) {
            result = new SensorValuePlotPageTableValue(sensorValue);
          }
        }

        if (null == result) {
          // Now just try to get an interpolated value
          long columnId = instrument.getSensorAssignments()
            .getColumnIds(sensorType).get(0);

          result = sensorValues.getPositionTableValue(columnId, time);
        }
      }
    }

    return result;
  }

  @Override
  public DatasetSensorValues getAllSensorValues() {
    return sensorValues;
  }

  @Override
  protected DataLatLng getMapPosition(LocalDateTime time) throws Exception {
    PlotPageTableValue longitude = getInterpolatedPositionValue(
      SensorType.LONGITUDE_SENSOR_TYPE, time);
    PlotPageTableValue latitude = getInterpolatedPositionValue(
      SensorType.LATITUDE_SENSOR_TYPE, time);

    DataLatLng result = null;

    if (null != longitude && !longitude.isNull() && null != latitude
      && !latitude.isNull()) {
      result = new DataLatLng(latitude, longitude);
    }

    return result;
  }
}
