package uk.ac.exeter.QuinCe.web.datasets.export;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeading;
import uk.ac.exeter.QuinCe.data.Dataset.Coordinate;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.TimeCoordinate;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionException;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Export.ExportOption;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategoryException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
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
 * An {@code ExportData} object is constructed from a {@link ManualQCData}
 * object, and filters its contents based on the {@link ExportOption} being
 * used. The required contents from the source object are copied over, and can
 * then be filtered and processed as needed.
 * </p>
 *
 * <p>
 * <b>NOTE:</b> This class will not take a deep copy of the contents of the
 * source {@link ManualQCData} object, so the contents of that object may be
 * changed during actions taken by this class.
 * </p>
 */
public class ExportData {

  LinkedHashMap<String, List<PlotPageColumnHeading>> headingsWithProperties = null;

  // TODO Replace this with something more generic. See issue #1845

  private static final long FIXED_LON_ID = -10000L;

  private static final long FIXED_LAT_ID = -10001L;

  private static final long FIXED_DEPTH_ID = -10002L;

  private ExportOption exportOption = null;

  private FixedPlotPageTableValue lonValue = null;

  private FixedPlotPageTableValue latValue = null;

  private FixedPlotPageTableValue depthValue = null;

  /**
   * The Row IDs for the dataset. These are the database IDs of the Coordinate
   * objects.
   */
  private TreeMap<Long, Coordinate> coordinates = new TreeMap<Long, Coordinate>();

  /**
   * The filtered measurements from the source data.
   */
  protected TreeMap<Coordinate, Measurement> measurements = new TreeMap<Coordinate, Measurement>();

  /**
   * The filtered sensor values from the source data.
   */
  protected DatasetSensorValues sensorValues = null;

  /**
   * The source data for this export.
   */
  protected ManualQCData sourceData;

  /**
   * Takes in a {@link ManualQCData} object for a dataset and filters it
   * according to the supplied {@link ExportOption}.
   *
   * @param sourceData
   *          The source data.
   * @param exportOption
   *          The export option.
   * @throws RecordNotFoundException
   *           If the filtering actions fail.
   * @throws RunTypeCategoryException
   */
  public ExportData(ManualQCData sourceData, ExportOption exportOption)
    throws Exception {

    this.sourceData = sourceData;
    this.exportOption = exportOption;
    init();
  }

  /**
   * Initialise the export view of the data from the source object.
   *
   * @param sourceData
   *          The source data object.
   * @throws RecordNotFoundException
   * @throws RunTypeCategoryException
   * @throws SensorTypeNotFoundException
   */
  @SuppressWarnings("unchecked")
  private void init() throws Exception {

    // Default to storing all values
    this.measurements = sourceData.getAllMeasurements();
    this.sensorValues = sourceData.getAllSensorValues();

    /*
     * Filter measurements to only contain those with Good QC flags if required
     */
    if (exportOption.skipBad()) {
      TreeMap<Coordinate, Measurement> filteredMeasurements = new TreeMap<Coordinate, Measurement>();
      for (Map.Entry<Coordinate, Measurement> entry : measurements.entrySet()) {
        if (!sensorValues.getFlagScheme()
          .isBad(entry.getValue().getQCFlag(sensorValues))) {

          filteredMeasurements.put(entry.getKey(), entry.getValue());
        }
      }

      this.measurements = filteredMeasurements;
    }

    /*
     * See if we need to filter the SensorValues. The logic for this is:
     *
     * If !measurementsOnly AND !skipBad, we include all SensorValues
     *
     * If measurementsOnly, we only take SensorValues from the Measurements. The
     * Measurements are already filtered by skipBad so we don't need to do
     * anything else.
     *
     * If !measurementsOnly AND skipBad, we take the SensorValues from
     * Measurements and add in any other non-Bad SensorValues.
     */

    if (exportOption.measurementsOnly() || exportOption.skipBad()) {

      TreeSet<Long> filteredIds = new TreeSet<Long>();

      // We always take the SensorValues from the already-filtered Measurements
      for (Measurement measurement : measurements.values()) {

        // Skip non-Variable measurements
        if (sourceData.isMeasurementForAnyVariable(measurement)) {
          for (MeasurementValue measurementValue : measurement
            .getMeasurementValues()) {

            filteredIds.addAll(measurementValue.getSensorValueIds());
            filteredIds.addAll(measurementValue.getSupportingSensorValueIds());
          }
        }
      }

      /*
       * Now if we're skipping bad values, add in all non-measurement
       * SensorValues that aren't Bad.
       *
       * We don't need to explicitly check if we're re-adding a SensorValue
       * that's already in the list from a Measurement - because we're using
       * TreeSets the duplicates are handled.
       */
      for (SensorValue sensorValue : sensorValues.getAll()) {
        if (!sensorValues.getFlagScheme().isBad(sensorValue.getUserQCFlag())) {
          filteredIds.add(sensorValue.getId());
        }
      }

      // Now subset the SensorValues to only contain the selected ones.
      sensorValues = sensorValues.subset(filteredIds);
    }

    /*
     * Now generate the Row IDs for the exported data.
     *
     * Row IDs are the database IDs of the Coordinate objects in the dataset.
     *
     * If !measurementsOnly, this will be all the Coordinates of all used
     * SensorValues.
     *
     * If measurementsOnly, we only want the Coordinates of actual Measurements.
     *
     * Finally, this approach can pull in rows that we don't want to export,
     * e.g. the supporting sensor values for a Measurement will include
     * information from the gas standard runs, and we don't necessarily want
     * them. To remove these, we discard any acquired Coordinates that were not
     * in the original sourceData.
     */

    Collection<Coordinate> selectedCoordinates;

    if (exportOption.measurementsOnly()) {
      selectedCoordinates = measurements.keySet();
    } else {
      selectedCoordinates = sensorValues.getCoordinates();
    }

    HashSet<Long> sourceRowIDs = new HashSet<Long>(sourceData.getRowIDs());
    selectedCoordinates.forEach(c -> {
      if (sourceRowIDs.contains(c.getId())) {
        coordinates.put(c.getId(), c);
      }
    });

    /*
     * Diagnostic sensors do not always exist in all datasets. Make sure there's
     * a column entry for all diagnostic sensors, even if there's no associated
     * data.
     */
    if (exportOption.includeRawSensors())

    {
      sourceData.getInstrument().getSensorAssignments().getDiagnosticColumnIds()
        .forEach(id -> sensorValues.addOptionalColumn(id));
    }

    // Build the column headings
    headingsWithProperties = (LinkedHashMap<String, List<PlotPageColumnHeading>>) sourceData
      .getExtendedColumnHeadings().clone();

    List<PlotPageColumnHeading> rootColumns = headingsWithProperties
      .get(ManualQCData.ROOT_FIELD_GROUP);

    /*
     * Manually add the position headings if the dataset has fixed position
     * properties.
     */
    if (sourceData.getDataset().fixedPosition()) {
      PlotPageColumnHeading lonColumn = new PlotPageColumnHeading(FIXED_LON_ID,
        "Longitude", "Longitude", "ALONGP01", "degrees_east", true, true, false,
        false);
      PlotPageColumnHeading latColumn = new PlotPageColumnHeading(FIXED_LAT_ID,
        "Latitude", "Latitude", "ALATGP01", "degrees_north", true, true, false,
        false);
      rootColumns.add(lonColumn);
      rootColumns.add(latColumn);

      // Set up the fixed values ready for later
      lonValue = new FixedPlotPageTableValue(sourceData.getDataset()
        .getProperty(DataSet.INSTRUMENT_PROPERTIES_KEY, "longitude"));
      latValue = new FixedPlotPageTableValue(sourceData.getDataset()
        .getProperty(DataSet.INSTRUMENT_PROPERTIES_KEY, "latitude"));

    }

    /*
     * Add the depth if the dataset has fixed depth
     */
    if (sourceData.getDataset().fixedDepth()) {
      PlotPageColumnHeading depthHeading = new PlotPageColumnHeading(
        FIXED_DEPTH_ID, "Depth", "Depth", "ADEPZZ01", "m", true, false, true,
        true);
      rootColumns.add(depthHeading);

      // Set up the fixed value ready for later
      depthValue = new FixedPlotPageTableValue(sourceData.getDataset()
        .getProperty(DataSet.INSTRUMENT_PROPERTIES_KEY, "depth"));
    }

    // Sensor Types used for Measurement Values may need to be split according
    // to which Variable they're used for
    LinkedHashSet<PlotPageColumnHeading> measurementValuesHeadings = new LinkedHashSet<PlotPageColumnHeading>();

    for (PlotPageColumnHeading sourceHeading : headingsWithProperties
      .get(ManualQCData.MEASUREMENTVALUES_FIELD_GROUP)) {

      SensorType sensorType = MeasurementValueSensorType
        .getSensorType(sourceHeading);

      for (Variable variable : sourceData.getInstrument().getVariables()) {
        ColumnHeading variableHeading = variable.getColumnHeading(sensorType);
        if (null != variableHeading) {
          measurementValuesHeadings.add(new PlotPageColumnHeading(
            variableHeading, true, false, sensorType.badFlagOnly()));
        }
      }
    }

    headingsWithProperties.put(ManualQCData.MEASUREMENTVALUES_FIELD_GROUP,
      new ArrayList<PlotPageColumnHeading>(measurementValuesHeadings));

  }

  public LinkedHashMap<String, List<PlotPageColumnHeading>> getColumnHeadings() {
    return headingsWithProperties;
  }

  public PlotPageTableValue getColumnValue(long rowId, long columnId)
    throws PlotPageDataException {

    // TODO Replace this with something more generic. See issue #1845
    PlotPageTableValue value = null;

    // The time is just the time
    if (columnId == FileDefinition.TIME_COLUMN_ID) {
      TimeCoordinate coordinate = (TimeCoordinate) coordinates.get(rowId);
      coordinate.setFormatter(exportOption.getTimestampFormatter());
      value = new SimplePlotPageTableValue(coordinate,
        sensorValues.getFlagScheme());
    } else if (columnId == FIXED_LON_ID) {
      value = lonValue;
    } else if (columnId == FIXED_LAT_ID) {
      value = latValue;
    } else if (columnId == FIXED_DEPTH_ID) {
      value = depthValue;
    } else {
      value = sourceData.getColumnValue(rowId, columnId);
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

    if (sensorValues.containsColumn(columnId)) {

      // Get the SensorValue
      SensorValue sensorValue = sensorValues.getRawSensorValue(columnId,
        sourceData.getCoordinate(rowId));
      if (null != sensorValue) {
        sensorValue.setUserQC(qcFlag, qcComment);
      }

      // Data Reduction value
    } else {
      Variable variable = DataReducerFactory
        .getVariable(sourceData.getInstrument(), columnId);

      Measurement measurement = measurements
        .get(sourceData.getCoordinate(rowId));
      if (null != measurement) {
        if (sourceData.hasMeasurementDataReductionRecord(measurement.getId())) {
          DataReductionRecord record = sourceData
            .getMeasurementDataReductionRecord(measurement.getId(), variable);

          if (null != record) {
            record.setQc(qcFlag, qcComment);
          }
        }
      }
    }
  }

  public boolean contains(long rowId, boolean includeSensorValues) {

    boolean containsMeasurement = !(null == measurements
      .get(coordinates.get(rowId)));
    boolean result;
    if (includeSensorValues) {
      result = containsMeasurement
        || sensorValues.contains(coordinates.get(rowId));
    } else {
      result = containsMeasurement;
    }

    return result;
  }

  public Set<Long> getRowIDs() {
    return coordinates.keySet();
  }

  public DatasetSensorValues getSensorValues() {
    return sensorValues;
  }

  public void destroy() {
    this.measurements = null;
    this.sensorValues = null;
    sourceData.destroy();
  }

  public Measurement getMeasurement(long rowId) {
    return measurements.get(coordinates.get(rowId));
  }
}
