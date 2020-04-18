package uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionException;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPage2Data;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableRecord;

public class ManualQC2Data extends PlotPage2Data {

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
  private LinkedHashMap<String, List<String>> columnHeadings = null;

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
   * @param progress
   *          A Progress object that can be updated as data is loaded.
   * @throws Exception
   *           If the data cannot be loaded.
   */
  protected ManualQC2Data(Instrument instrument, DataSet dataset) {
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

    try (Connection conn = dataSource.getConnection()) {

      sensorValues = DataSetDataDB.getSensorValues(conn, instrument,
        dataset.getId(), false);

      measurements = DataSetDataDB.getMeasurementTimes(conn, dataset.getId(),
        instrument.getMeasurementRunTypes());

      dataReduction = DataSetDataDB.getDataReductionData(conn, instrument,
        dataset);

      buildColumnHeaders();
    }
  }

  private void buildColumnHeaders() {

    columnHeadings = new LinkedHashMap<String, List<String>>();

    // Time and Position
    List<String> rootColumns = new ArrayList<String>(3);
    rootColumns.add(FileDefinition.TIME_COLUMN_NAME);
    rootColumns.add(FileDefinition.LONGITUDE_COLUMN_NAME);
    rootColumns.add(FileDefinition.LATITUDE_COLUMN_NAME);

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

    List<String> sensorColumnNames = new ArrayList<String>(
      sensorColumns.size());
    sensorColumnIds = new long[sensorColumns.size()];

    for (int i = 0; i < sensorColumns.size(); i++) {
      sensorColumnNames.add(sensorColumns.get(i).getSensorName());
      sensorColumnIds[i] = sensorColumns.get(i).getDatabaseId();
    }

    columnHeadings.put("Sensors", sensorColumnNames);

    diagnosticColumnIds = new long[diagnosticColumns.size()];
    if (diagnosticColumns.size() > 0) {
      List<String> diagnosticColumnNames = new ArrayList<String>(
        diagnosticColumns.size());

      for (int i = 0; i < diagnosticColumns.size(); i++) {
        diagnosticColumnNames.add(diagnosticColumns.get(i).getSensorName());
        diagnosticColumnIds[i] = diagnosticColumns.get(i).getDatabaseId();
      }

      columnHeadings.put("Diagnostics", diagnosticColumnNames);
    }

    // Each of the instrument variables
    for (InstrumentVariable variable : instrument.getVariables()) {
      try {
        columnHeadings.put(variable.getName(), new ArrayList<String>(
          DataReducerFactory.getCalculationParameters(variable).keySet()));
      } catch (DataReductionException e) {
        error("Error getting variable headers", e);
      }
    }
  }

  @Override
  protected LinkedHashMap<String, List<String>> getColumnHeadings() {
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
        record.addColumn(
          recordSensorValues.get(FileDefinition.LONGITUDE_COLUMN_ID), true);

        record.addColumn(
          recordSensorValues.get(FileDefinition.LATITUDE_COLUMN_ID), true);

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
}
