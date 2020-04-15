package uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.CalculationParameter;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPage2Data;

public class ManualQC2Data implements PlotPage2Data {

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
  private List<Measurement> measurements;

  /**
   * The dataset's sensor values.
   */
  private DatasetSensorValues sensorValues;

  /**
   * The values calculated by data reduction.
   */
  private Map<Long, Map<InstrumentVariable, DataReductionRecord>> dataReduction;

  /**
   * The column headers for data reduction.
   */
  private Map<InstrumentVariable, List<CalculationParameter>> dataReductionHeaders;

  /**
   * Construct the data object and load all data for a dataset.
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
  protected ManualQC2Data(Instrument instrument, DataSet dataset,
    DataSource dataSource) throws Exception {
    this.instrument = instrument;
    this.dataset = dataset;
    load(dataSource);
  }

  /**
   * Load all the data for the dataset.
   *
   * @param dataSource
   *          A data source.
   * @throws Exception
   *           If the data cannot be loaded.
   */
  private void load(DataSource dataSource) throws Exception {

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
      throw e;
    }
  }
}
