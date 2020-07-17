package uk.ac.exeter.QuinCe.web.datasets.export;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

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
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageColumnHeading;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC.ManualQCData;

/**
 * A representation of a dataset's data for export.
 *
 * <p>
 * Based on the data for the {@link ManualQCData} for manual QC pages, since
 * that contains everything we need.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class ExportData extends ManualQCData {

  public static final String FIXED_GROUP = "FIXED";

  /**
   * The fixed Depth field & value
   */
  // TODO Replace this with something more generic. See issue #1284

  private static final long DEPTH_ID = -10000L;

  private PlotPageColumnHeading depthHeading;

  private int depth;

  public ExportData(DataSource dataSource, Instrument instrument,
    DataSet dataset) throws SQLException {
    super(dataSource, instrument, dataset);
    depthHeading = new PlotPageColumnHeading(DEPTH_ID, "Depth", "Depth",
      "ADEPZZ01", "m", true, false, false);
    depth = instrument.getDepth();
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

  @Override
  public LinkedHashMap<String, List<PlotPageColumnHeading>> getExtendedColumnHeadings()
    throws Exception {

    LinkedHashMap<String, List<PlotPageColumnHeading>> result = super.getExtendedColumnHeadings();

    ArrayList<PlotPageColumnHeading> fixedColumns = new ArrayList<PlotPageColumnHeading>();
    fixedColumns.add(depthHeading);
    result.put(FIXED_GROUP, fixedColumns);
    return result;
  }

  public String getFixedValue(PlotPageColumnHeading heading) {
    String result = null;

    switch (heading.getCodeName()) {
    case "ADEPZZ01": {
      result = String.valueOf(depth);
      break;
    }
    }

    return result;
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
