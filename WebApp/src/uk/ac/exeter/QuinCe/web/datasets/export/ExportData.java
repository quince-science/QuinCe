package uk.ac.exeter.QuinCe.web.datasets.export;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeading;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionException;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Export.ExportOption;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageColumnHeading;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageDataException;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.SimplePlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC.ManualQCData;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC.MeasurementValueSensorType;

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

  LinkedHashMap<String, List<PlotPageColumnHeading>> headingsWithProperties = null;

  // TODO Replace this with something more generic. See issue #1845

  private static final long FIXED_LON_ID = -10000L;

  private static final long FIXED_LAT_ID = -10001L;

  private static final long DEPTH_ID = -10002L;

  private ExportOption exportOption = null;

  private FixedPlotPageTableValue lonValue = null;

  private FixedPlotPageTableValue latValue = null;

  private FixedPlotPageTableValue depthValue = null;

  public ExportData(DataSource dataSource, Instrument instrument,
    DataSet dataset, ExportOption exportOption) throws SQLException {
    super(dataSource, instrument, dataset);
    this.exportOption = exportOption;
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
   * Different data can be loaded depending on the export options.
   */
  @Override
  public void loadDataAction() throws Exception {

    // Load all data
    super.loadDataAction();

    /*
     * Filter measurements to only contain those with Good QC flags if required
     */
    if (exportOption.goodOnly()) {
      TreeMap<LocalDateTime, Measurement> filteredMeasurements = new TreeMap<LocalDateTime, Measurement>();

      for (Map.Entry<LocalDateTime, Measurement> entry : measurements
        .entrySet()) {
        if (entry.getValue().getQCFlag().isGood()) {
          filteredMeasurements.put(entry.getKey(), entry.getValue());
        }
      }

      measurements = filteredMeasurements;
    }

    /*
     * Now filter the SensorValues if required.
     */
    if (exportOption.filterSensorValues()) {
      /*
       * If filtering is required, we always need SensorValues related to the
       * selected Measurements (which may have been filtered above).
       */

      // Get all SensorValues related to the measurements
      TreeSet<LocalDateTime> filteredTimes = new TreeSet<LocalDateTime>();
      TreeSet<Long> filteredIds = new TreeSet<Long>();

      // Get all sensor values related to measurements
      for (Measurement measurement : measurements.values()) {
        filteredTimes.add(measurement.getTime());

        for (MeasurementValue measurementValue : measurement
          .getMeasurementValues()) {

          filteredIds.addAll(measurementValue.getSensorValueIds());
          filteredIds.addAll(measurementValue.getSupportingSensorValueIds());
        }
      }

      /*
       * If we don't want values from only Measurements, but we do want only
       * Good values, filter them here.
       */
      if (!exportOption.measurementsOnly() && exportOption.goodOnly()) {
        List<Long> goodIds = sensorValues.getAll().stream()
          .filter(v -> v.getUserQCFlag().isGood()).map(v -> v.getId()).toList();

        filteredIds.addAll(goodIds);
      }

      // Now subset the SensorValues to only contain the selected ones.
      sensorValues = sensorValues.subset(filteredTimes, filteredIds);
    }

    // Diagnostic sensors do not always exist in all datasets. Make sure
    // there's a column entry for all diagnostic sensors, even if there's no
    // associated data.
    if (exportOption.includeRawSensors()) {
      for (long diagnosticId : instrument.getSensorAssignments()
        .getDiagnosticColumnIds()) {
        sensorValues.addOptionalColumn(diagnosticId);
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public LinkedHashMap<String, List<PlotPageColumnHeading>> getExtendedColumnHeadings()
    throws Exception {

    if (null == headingsWithProperties) {
      headingsWithProperties = (LinkedHashMap<String, List<PlotPageColumnHeading>>) super.getExtendedColumnHeadings()
        .clone();

      List<PlotPageColumnHeading> rootColumns = headingsWithProperties
        .get(ROOT_FIELD_GROUP);

      // Manually add the position headings if the dataset has fixed position
      // properties
      if (dataset.fixedPosition()) {
        PlotPageColumnHeading lonColumn = new PlotPageColumnHeading(
          FIXED_LON_ID, "Longitude", "Longitude", "ALONGP01", "degrees_east",
          true, true, false, true);
        PlotPageColumnHeading latColumn = new PlotPageColumnHeading(
          FIXED_LAT_ID, "Latitude", "Latitude", "ALATGP01", "degrees_north",
          true, true, false, true);
        rootColumns.add(lonColumn);
        rootColumns.add(latColumn);

        // Set up the fixed values ready for later
        lonValue = new FixedPlotPageTableValue(
          dataset.getProperty(DataSet.INSTRUMENT_PROPERTIES_KEY, "longitude"));
        latValue = new FixedPlotPageTableValue(
          dataset.getProperty(DataSet.INSTRUMENT_PROPERTIES_KEY, "latitude"));

      }
      // Add the depth heading if the dataset has the depth property
      if (null != dataset.getProperty(DataSet.INSTRUMENT_PROPERTIES_KEY,
        "depth")) {
        PlotPageColumnHeading depthHeading = new PlotPageColumnHeading(DEPTH_ID,
          "Depth", "Depth", "ADEPZZ01", "m", true, false, true);
        rootColumns.add(depthHeading);

        // Set up the fixed value ready for later
        depthValue = new FixedPlotPageTableValue(
          dataset.getProperty(DataSet.INSTRUMENT_PROPERTIES_KEY, "depth"));
      }

      // Sensor Types used for Measurement Values may need to be split according
      // to which Variable they're used for
      LinkedHashSet<PlotPageColumnHeading> measurementValuesHeadings = new LinkedHashSet<PlotPageColumnHeading>();

      for (PlotPageColumnHeading sourceHeading : headingsWithProperties
        .get(MEASUREMENTVALUES_FIELD_GROUP)) {

        SensorType sensorType = MeasurementValueSensorType
          .getSensorType(sourceHeading);

        for (Variable variable : instrument.getVariables()) {
          ColumnHeading variableHeading = variable.getColumnHeading(sensorType);
          if (null != variableHeading) {

            measurementValuesHeadings
              .add(new PlotPageColumnHeading(variableHeading, true, false));
          }
        }
      }

      headingsWithProperties.put(MEASUREMENTVALUES_FIELD_GROUP,
        new ArrayList<PlotPageColumnHeading>(measurementValuesHeadings));
    }

    return headingsWithProperties;
  }

  @Override
  public PlotPageTableValue getColumnValue(long rowId, long columnId)
    throws PlotPageDataException {

    // TODO Replace this with something more generic. See issue #1845
    PlotPageTableValue value = null;

    // The time is just the time
    if (columnId == FileDefinition.TIME_COLUMN_ID) {
      LocalDateTime rowTime = DateTimeUtils.longToDate(rowId);
      value = new SimplePlotPageTableValue(rowTime,
        exportOption.getTimestampFormatter(), false);
    } else if (columnId == FIXED_LON_ID) {
      value = lonValue;
    } else if (columnId == FIXED_LAT_ID) {
      value = latValue;
    } else if (columnId == DEPTH_ID) {
      value = depthValue;
    } else {
      value = super.getColumnValue(rowId, columnId);
    }

    return value;
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
   * @throws InvalidFlagException
   */
  protected void overrideQc(long rowId, long columnId, Flag qcFlag,
    String qcComment)
    throws InstrumentException, DataReductionException, InvalidFlagException {

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
      Variable variable = DataReducerFactory.getVariable(instrument, columnId);

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

  public boolean containsTime(LocalDateTime time, boolean includeSensorValues) {
    boolean containsMeasurement = !(null == getMeasurement(time));
    boolean result;
    if (includeSensorValues) {
      result = containsMeasurement || sensorValues.contains(time);
    } else {
      result = containsMeasurement;
    }

    return result;
  }
}
