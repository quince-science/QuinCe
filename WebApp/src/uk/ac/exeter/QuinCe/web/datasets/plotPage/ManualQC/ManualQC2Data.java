package uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
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
   * Get the database ID of a column by its index.
   *
   * <p>
   * This method assumes that only sensor or diagnostic columns will be
   * requested, since they are the only ones that will be edited. Requesting a
   * column from a variable will result in an
   * {@link ArrayIndexOutOfBoundsException}.
   * </p>
   *
   * @param columnIndex
   *          The column index.
   * @return The column ID.
   */
  private long getColumnId(int columnIndex) {

    long result = -1L;

    Iterator<String> groups = columnHeadings.keySet().iterator();
    int currentIndex = 0;

    while (groups.hasNext()) {
      List<ColumnHeading> groupHeaders = columnHeadings.get(groups.next());
      if (currentIndex + groupHeaders.size() > columnIndex) {
        result = groupHeaders.get(columnIndex - currentIndex).getId();
        break;
      } else {
        currentIndex += groupHeaders.size();
      }
    }

    return result;
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
      long selectedColumnId = getColumnId(selectedColumn);

      List<SensorValue> updates = new ArrayList<SensorValue>(
        selectedRows.size());

      for (String rowId : selectedRows) {
        LocalDateTime rowTime = DateTimeUtils.longToDate(Long.parseLong(rowId));
        SensorValue sensorValue = sensorValues.getSensorValue(rowTime,
          selectedColumnId);

        sensorValue.setUserQC(sensorValue.getAutoQcFlag(),
          sensorValue.getAutoQcResult().getAllMessages());

        updates.add(sensorValue);
      }

      DataSetDataDB.storeSensorValues(conn, updates);
    } catch (Exception e) {
      error("Error while updating QC flags", e);
    }
  }

  protected void destroy() {
    try {
      conn.close();
    } catch (SQLException e) {
      // Not much we can do about this.
    }
  }
}
