package uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.CalculationParameter;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionException;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPage2Data;

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
  private List<Measurement> measurements = null;

  /**
   * The dataset's sensor values.
   */
  private DatasetSensorValues sensorValues = null;

  /**
   * The values calculated by data reduction.
   */
  private Map<Long, Map<InstrumentVariable, DataReductionRecord>> dataReduction = null;

  /**
   * The column headers for data reduction.
   */
  private Map<InstrumentVariable, List<CalculationParameter>> dataReductionHeaders = null;

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
  public void loadData(DataSource dataSource) {

    try (Connection conn = dataSource.getConnection()) {

      measurements = DataSetDataDB.getMeasurements(conn, instrument,
        dataset.getId());

      sensorValues = DataSetDataDB.getSensorValues(conn, instrument,
        dataset.getId(), false);

      dataReduction = DataSetDataDB.getDataReductionData(conn, instrument,
        dataset);

      dataReductionHeaders = DataReducerFactory
        .getCalculationParameters(instrument.getVariables());

    } catch (Exception e) {
      error("Error while loading dataset data", e);
    }
  }

  @Override
  protected LinkedHashMap<String, List<String>> getColumnHeadings() {

    LinkedHashMap<String, List<String>> headings = new LinkedHashMap<String, List<String>>();

    // Time and Position
    List<String> rootColumns = new ArrayList<String>(3);
    rootColumns.add(FileDefinition.TIME_COLUMN_NAME);
    rootColumns.add(FileDefinition.LONGITUDE_COLUMN_NAME);
    rootColumns.add(FileDefinition.LATITUDE_COLUMN_NAME);

    headings.put(ROOT_FIELD_GROUP, rootColumns);

    // Sensor Assignments are divided into sensors and diagnostics
    List<String> sensorColumns = new ArrayList<String>();
    List<String> diagnosticColumns = new ArrayList<String>();

    for (Map.Entry<SensorType, List<SensorAssignment>> entry : instrument
      .getSensorAssignments().entrySet()) {

      for (SensorAssignment assignment : entry.getValue()) {

        if (entry.getKey().isSensor()) {
          sensorColumns.add(assignment.getSensorName());
        } else if (entry.getKey().isDiagnostic()) {
          diagnosticColumns.add(assignment.getSensorName());
        }
      }
    }

    headings.put("Sensors", sensorColumns);

    if (diagnosticColumns.size() > 0) {
      headings.put("Diagnostics", diagnosticColumns);
    }

    // Each of the instrument variables
    for (InstrumentVariable variable : instrument.getVariables()) {
      try {
        headings.put(variable.getName(), new ArrayList<String>(
          DataReducerFactory.getCalculationParameters(variable).keySet()));
      } catch (DataReductionException e) {
        error("Error getting variable headers", e);
      }
    }

    return headings;

  }
}
