package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.Calculators;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupsException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;

/**
 * The default implementation of {@link MeasurementValueCalculator}.
 * <p>
 * Applies interpolation based on quality flags, and standard calibration as
 * needed.
 * </p>
 *
 * @author stevej
 */
public class DefaultMeasurementValueCalculator
  extends MeasurementValueCalculator {

  public static final String STANDARDS_COUNT_PROPERTY = "stdcount";

  private static final int PRIOR = -1;

  private static final int POST = 1;

  // TODO Need limits on how far interpolation goes before giving up.

  @Override
  public MeasurementValue calculate(Instrument instrument, DataSet dataSet,
    Measurement measurement, SensorType coreSensorType,
    SensorType requiredSensorType, DatasetMeasurements allMeasurements,
    DatasetSensorValues allSensorValues, Connection conn)
    throws MeasurementValueCalculatorException {

    // TODO #1128 This currently assumes only one sensor for each SensorType.
    // This will have to change eventually.
    if (requiredSensorType.equals(SensorType.LATITUDE_SENSOR_TYPE)
      || requiredSensorType.equals(SensorType.LONGITUDE_SENSOR_TYPE)) {
      return getPositionValue(instrument, dataSet, measurement, coreSensorType,
        requiredSensorType, allMeasurements, allSensorValues, conn);
    } else {
      return getSensorValue(instrument, dataSet, measurement, coreSensorType,
        requiredSensorType, allMeasurements, allSensorValues, conn);
    }
  }

  private MeasurementValue getSensorValue(Instrument instrument,
    DataSet dataSet, Measurement measurement, SensorType coreSensorType,
    SensorType requiredSensorType, DatasetMeasurements allMeasurements,
    DatasetSensorValues allSensorValues, Connection conn)
    throws MeasurementValueCalculatorException {

    SensorAssignment requiredAssignment = instrument.getSensorAssignments()
      .get(requiredSensorType).first();

    SearchableSensorValuesList sensorValues = allSensorValues
      .getColumnValues(requiredAssignment.getDatabaseId());

    MeasurementValue result = new MeasurementValue(requiredSensorType);

    try {
      // TODO #1128 This currently assumes only one sensor for each
      // SensorType.
      // This will have to change eventually.
      SensorAssignment coreAssignment = instrument.getSensorAssignments()
        .get(coreSensorType).first();

      LocalDateTime valueTime = dataSet.getSensorOffsets().getOffsetTime(
        measurement.getTime(), coreAssignment, requiredAssignment);

      List<SensorValue> valuesToUse;

      if (requiredSensorType.equals(coreSensorType)) {
        valuesToUse = Arrays.asList(sensorValues.get(valueTime));
      } else {
        valuesToUse = sensorValues.getWithInterpolation(valueTime, true, true);
      }

      char valueType = calcValueType(valueTime, valuesToUse);

      populateMeasurementValue(measurement.getTime(), result, valuesToUse,
        valueType);
    } catch (SensorGroupsException e) {
      throw new MeasurementValueCalculatorException(
        "Cannot calculate time offset", e);
    }

    // Calibrate the value if (a) the SensorType can have calibrations, and
    // (b) the instrument has calibration Run Types defined.
    if (requiredSensorType.hasInternalCalibration()
      && instrument.hasInternalCalibrations()) {
      calibrate(instrument, measurement, requiredSensorType, result,
        allMeasurements, sensorValues, conn);
    }

    return result;
  }

  private MeasurementValue getPositionValue(Instrument instrument,
    DataSet dataSet, Measurement measurement, SensorType coreSensorType,
    SensorType requiredSensorType, DatasetMeasurements allMeasurements,
    DatasetSensorValues allSensorValues, Connection conn)
    throws MeasurementValueCalculatorException {

    MeasurementValue result = new MeasurementValue(requiredSensorType);

    SensorAssignment coreAssignment = instrument.getSensorAssignments()
      .get(coreSensorType).first();

    try {
      // If Lon/Lat, get offset to first group. Get lat/lon at that time.
      LocalDateTime positionTime = dataSet.getSensorOffsets()
        .offsetToFirstGroup(measurement.getTime(), coreAssignment);

      long columnId = requiredSensorType
        .equals(SensorType.LONGITUDE_SENSOR_TYPE) ? SensorType.LONGITUDE_ID
          : SensorType.LATITUDE_ID;

      SearchableSensorValuesList sensorValues = allSensorValues
        .getColumnValues(columnId);

      List<SensorValue> valuesToUse = sensorValues
        .getWithInterpolation(positionTime, true, true);

      char valueType = calcValueType(positionTime, valuesToUse);

      populateMeasurementValue(measurement.getTime(), result, valuesToUse,
        valueType);
    } catch (SensorGroupsException e) {
      throw new MeasurementValueCalculatorException(
        "Unable to apply sensor offsets", e);
    }

    return result;
  }

  private char calcValueType(LocalDateTime usedTime,
    List<SensorValue> usedValues) {
    char result;

    if (usedValues.size() == 1
      && usedValues.get(0).getTime().equals(usedTime)) {
      result = PlotPageTableValue.MEASURED_TYPE;
    } else {
      result = PlotPageTableValue.INTERPOLATED_TYPE;
    }

    return result;
  }

  /**
   *
   * @param measurementTime
   * @param measurementValue
   * @param valuesToUse
   * @param preferredType
   *          The type that the calling function would like to set for the
   *          MeasurementValue. May be overridden by this method for
   *          interpolated values.
   * @throws MeasurementValueCalculatorException
   */
  private void populateMeasurementValue(LocalDateTime measurementTime,
    MeasurementValue measurementValue, List<SensorValue> valuesToUse,
    char preferredType) throws MeasurementValueCalculatorException {
    switch (valuesToUse.size()) {
    case 0: {
      // We should not use a value here
      measurementValue.setCalculatedValue(Double.NaN);
      break;
    }
    case 1: {
      /*
       * For a single value we trust what the provided type tells us (whether
       * it's measured or interpolated).
       */
      if (preferredType == PlotPageTableValue.MEASURED_TYPE) {
        measurementValue.addSensorValue(valuesToUse.get(0));
      } else {
        measurementValue.addInterpolatedSensorValue(valuesToUse.get(0), true);
      }

      measurementValue.setCalculatedValue(valuesToUse.get(0).getDoubleValue());
      break;
    }
    case 2: {
      /*
       * Everything is always interpolated
       */
      measurementValue.addSensorValues(valuesToUse);
      measurementValue.setCalculatedValue(SensorValue
        .interpolate(valuesToUse.get(0), valuesToUse.get(1), measurementTime));
      break;
    }
    default: {
      throw new MeasurementValueCalculatorException(
        "Invalid number of values in search result");
    }
    }
  }

  private void calibrate(Instrument instrument, Measurement measurement,
    SensorType sensorType, MeasurementValue value,
    DatasetMeasurements allMeasurements,
    SearchableSensorValuesList sensorValues, Connection conn)
    throws MeasurementValueCalculatorException {

    if (!value.getCalculatedValue().isNaN()) {

      try {
        CalibrationSet calibrationSet = ExternalStandardDB.getInstance()
          .getMostRecentCalibrations(conn, instrument, measurement.getTime());

        // Get the calibration offset for the standard run prior to the
        // measurement
        CalibrationOffset prior = getCalibrationOffset(PRIOR, calibrationSet,
          allMeasurements, sensorValues, sensorType, measurement.getTime(),
          value);

        // Get the calibration offset for the standard run after the measurement
        CalibrationOffset post = getCalibrationOffset(POST, calibrationSet,
          allMeasurements, sensorValues, sensorType, measurement.getTime(),
          value);

        /*
         * Note that even though we know QC messages are required here, we can't
         * store them because we aren't an Auto QC routine. There is a Data
         * Reduction QC routine that will generate the QC messages.
         */
        if (prior.isValid() && !post.isValid()) {

          // We only found a calibration run after the measurement, so use that
          // without any interpolation.
          applyOffset(value, post.getOffset());
          value.addSupportingSensorValues(post.getUsedValues());
        } else if (!prior.isValid() && post.isValid()) {

          // We only found a calibration run before the measurement, so use that
          // without any interpolation.
          applyOffset(value, prior.getOffset());
          value.addSupportingSensorValues(prior.getUsedValues());
        } else if (prior.isValid() && post.isValid()) {
          // We have valid calibrations either side. Get their offsets, and
          // interpolated to the time of the measured value.
          double offset = Calculators.interpolate(prior.getTime(),
            prior.getOffset(), post.getTime(), post.getOffset(),
            measurement.getTime());

          applyOffset(value, offset);

          value.addSupportingSensorValues(prior.getUsedValues());
          value.addSupportingSensorValues(post.getUsedValues());
        }
      } catch (Exception e) {
        throw new MeasurementValueCalculatorException(
          "Error while calculating calibrated value", e);
      }
    }
  }

  private CalibrationOffset getCalibrationOffset(int direction,
    CalibrationSet calibrationSet, DatasetMeasurements allMeasurements,
    SearchableSensorValuesList sensorValues, SensorType sensorType,
    LocalDateTime measurementTime, MeasurementValue value)
    throws RecordNotFoundException {

    CalibrationOffset result = new CalibrationOffset();

    SimpleRegression regression = new SimpleRegression();

    // Loop through each calibration target
    for (String runType : calibrationSet.getTargets().keySet()) {

      double standardConcentration = calibrationSet.getCalibrationValue(runType,
        sensorType.getShortName());

      if (sensorType.includeZeroInCalibration()
        || standardConcentration > 0.0D) {

        // Get the measurements for the closest run
        TreeSet<Measurement> runTypeMeasurements;

        if (direction == PRIOR) {
          runTypeMeasurements = allMeasurements.getRunBefore(
            Measurement.GENERIC_RUN_TYPE_VARIABLE, runType, measurementTime);
        } else {
          runTypeMeasurements = allMeasurements.getRunAfter(
            Measurement.GENERIC_RUN_TYPE_VARIABLE, runType, measurementTime);
        }

        /*
         * Get the mean sensor value for these calibration measurements,
         * filtering out any bad ones.
         */
        List<SensorValue> runSensorValues = runTypeMeasurements.stream()
          .map(m -> sensorValues.get(m.getTime())).filter(v -> null != v)
          .filter(v -> !v.getDoubleValue().isNaN())
          .filter(v -> v.getUserQCFlag().isGood()).collect(Collectors.toList());

        Mean mean = new Mean();

        runSensorValues.stream().map(v -> v.getDoubleValue()).forEach(d -> {
          mean.increment(d);
        });

        // If there are values from the run...
        if (!Double.isNaN(mean.getResult())) {

          result.addUsedSensorValues(runSensorValues);

          double offset = mean.getResult() - standardConcentration;

          regression.addData(standardConcentration, offset);
        }
      }
    }

    if (regression.getN() < 2) {
      result.addComment("Not enough gas standards available");
    } else {
      result.setOffset(regression.predict(value.getCalculatedValue()));
    }

    return result;
  }

  private void applyOffset(MeasurementValue value, Double offset) {
    if (!offset.isNaN()) {
      value.setCalculatedValue(value.getCalculatedValue() - offset);
    }
  }

  /**
   * Inner class to hold the results of calculating the offset for a given value
   * from a set of gas standard runs.
   *
   * @author Steve Jones
   *
   */
  class CalibrationOffset {

    /**
     * The {@link SensorValue}s used in the calculation of this calibration.
     */
    private HashSet<SensorValue> usedValues;

    /**
     * The calculated offset from the gas standards.
     */
    private Double offset;

    /**
     * Comment for this calibration. Setting a comment implies that the
     * calibration is bad.
     */
    private List<String> comments;

    /**
     * Initialise with no used values, invalid offset and no comments.
     */
    protected CalibrationOffset() {
      this.usedValues = new HashSet<SensorValue>();
      this.offset = Double.NaN;
      this.comments = new ArrayList<String>();
    }

    /**
     * Get the calculated offset,
     *
     * @return The offset of the measured value at the time specified by
     *         {@link #time}.
     */
    protected Double getOffset() {
      return offset;
    }

    /**
     * Indicates whether or not this calibration can be used.
     *
     * @return {@code true} if the calibration can be used to calibrate a
     *         measurement; {@code false} if not.
     */
    protected boolean isValid() {
      return !offset.isNaN();
    }

    /**
     * Get the QC flag set for this calibration.
     *
     * @return The QC flag.
     */
    protected boolean isBad() {
      return !comments.isEmpty();
    }

    /**
     * Get the QC comment for this calibration.
     *
     * @return The QC comment.
     */
    protected List<String> getComments() {
      return comments;
    }

    /**
     * Get the {@link SensorValue} objects used to calculate this calibration.
     *
     * @return The used {@link SensorValue}s.
     */
    protected HashSet<SensorValue> getUsedValues() {
      return usedValues;
    }

    /**
     * Get the time for this calibration.
     *
     * <p>
     * The time is calculated as the mean time of all the {@link SensorValue}s
     * used in the calibration.
     * </p>
     *
     * @return The calculated calibration time.
     */
    protected LocalDateTime getTime() {
      return DateTimeUtils.meanTime(usedValues.stream().map(v -> v.getTime()));
    }

    /**
     * Add a collection of {@link SensorValue}s.
     *
     * @param values
     *          The {@link SensorValue}s.
     */
    protected void addUsedSensorValues(Collection<SensorValue> values) {
      usedValues.addAll(values);
    }

    /**
     * Add a comment to the offset QC information.
     *
     * @param comment
     *          The comment.
     */
    protected void addComment(String comment) {
      comments.add(comment);
    }

    protected void setOffset(double offset) {
      this.offset = offset;
    }
  }
}
