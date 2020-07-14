package uk.ac.exeter.QuinCe.web.datasets.export;

import java.sql.SQLException;
import java.time.LocalDateTime;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionException;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC.ManualQC2Data;

/**
 * A representation of a dataset's data for export.
 *
 * <p>
 * Based on the data for the {@link ManualQC2Data} for manual QC pages, since
 * that contains everything we need.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class Export2Data extends ManualQC2Data {

  public Export2Data(DataSource dataSource, Instrument instrument,
    DataSet dataset) throws SQLException {
    super(dataSource, instrument, dataset);
  }

  @Override
  public void loadData() {
    try {
      loadDataAction();
      loaded = true;
    } catch (Exception e) {
      error("Error while loading dataset data", e);
    }
  }

  /**
   * Post-process the data before it's finally exported. This instance does no
   * post-processing, but extending classes can override it as needed.
   */
  public void postProcess() throws Exception {
    // NOOP
  }

  /**
   * Override the QC values for a given row and column
   *
   * <p>
   * This has to figure out whether it's for a sensor or a calculation parameter
   * and act accordingly.
   * </p>
   *
   * <p>
   * Attempting to set the QC on the time column will have no effect.
   * </p>
   *
   * @param rowId
   *          The row ID
   * @param columnId
   *          The column ID
   * @param qcFlag
   *          The new QC flag
   * @param qcComment
   *          The new QC comment
   * @throws InstrumentException
   * @throws DataReductionException
   */
  protected void overrideQc(long rowId, long columnId, Flag qcFlag,
    String qcComment) throws InstrumentException, DataReductionException {

    // The rowId is the row time
    LocalDateTime rowTime = DateTimeUtils.longToDate(rowId);

    if (sensorValues.containsColumn(columnId)) {

      // Get the SensorValue
      SensorValue sensorValue = sensorValues.getSensorValue(rowTime, columnId);
      if (null != sensorValue) {
        sensorValue.setUserQC(qcFlag, qcComment);
      }

      // Data Reduction value
    } else {
      InstrumentVariable variable = DataReducerFactory.getVariable(instrument,
        columnId);

      Measurement measurement = measurements.get(rowTime);
      if (null != measurement) {
        if (dataReduction.containsKey(measurement.getId())) {
          DataReductionRecord record = dataReduction.get(measurement.getId())
            .get(variable);

          if (null != record) {
            record.setQc(qcFlag, qcComment);
          }
        }
      }
    }
  }
}
