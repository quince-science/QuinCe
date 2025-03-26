package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * The default implementation of {@link MeasurementValueCalculator}.
 *
 * <p>
 * Applies temporal offsets between sensor groups and calibrations as needed.
 * </p>
 */
public class DefaultMeasurementValueCalculator
  extends MeasurementValueCalculator {

  private static final int PRIOR = -1;

  private static final int POST = 1;

  public static final String STANDARDS_COUNT_PROPERTY = "stdcount";

  @Override
  public MeasurementValue calculate(Instrument instrument, DataSet dataSet,
    SensorValuesListValue timeReference, Variable variable,
    SensorType requiredSensorType, DatasetMeasurements allMeasurements,
    DatasetSensorValues allSensorValues, Connection conn)
    throws MeasurementValueCalculatorException {

    try {
      // TODO #1128 This currently assumes only one sensor for each SensorType.
      // This will have to change eventually.
      if (requiredSensorType.equals(SensorType.LATITUDE_SENSOR_TYPE)
        || requiredSensorType.equals(SensorType.LONGITUDE_SENSOR_TYPE)) {
        return getPositionValue(instrument, dataSet, timeReference, variable,
          requiredSensorType, allMeasurements, allSensorValues, conn);
      } else {
        return getSensorValue(instrument, dataSet, timeReference, variable,
          requiredSensorType, allMeasurements, allSensorValues, conn);
      }
    } catch (Exception e) {
      throw new MeasurementValueCalculatorException(e);
    }
  }

  protected MeasurementValue getSensorValue(Instrument instrument,
    DataSet dataSet, SensorValuesListValue timeReference, Variable variable,
    SensorType requiredSensorType, DatasetMeasurements allMeasurements,
    DatasetSensorValues allSensorValues, Connection conn)
    throws MeasurementValueCalculatorException, SensorValuesListException {

    return getSensorValue(instrument, dataSet, timeReference, variable,
      requiredSensorType, allMeasurements, allSensorValues, true, conn);
  }

  protected MeasurementValue getSensorValue(Instrument instrument,
    DataSet dataSet, SensorValuesListValue timeReference, Variable variable,
    SensorType requiredSensorType, DatasetMeasurements allMeasurements,
    DatasetSensorValues allSensorValues, boolean allowCalibration,
    Connection conn)
    throws MeasurementValueCalculatorException, SensorValuesListException {

    SensorAssignment requiredAssignment = instrument.getSensorAssignments()
      .get(requiredSensorType).first();

    SensorValuesList sensorValues = allSensorValues
      .getColumnValues(requiredAssignment.getDatabaseId());

    MeasurementValue result = null;

    // This can be happen if a sensor has no values at all.
    if (null == sensorValues) {
      result = new MeasurementValue(requiredSensorType);
    } else {
      try {
        // TODO #1128 This currently assumes only one sensor for each
        // SensorType.
        // This will have to change eventually.
        SensorAssignment coreAssignment = instrument.getSensorAssignments()
          .get(variable.getCoreSensorType()).first();

        LocalDateTime valueTime = dataSet.getSensorOffsets().getOffsetTime(
          timeReference.getNominalTime(), coreAssignment, requiredAssignment);

        /*
         * Values from the core SensorType do not get interpolated, because they
         * are the basis for measurements. Other SensorTypes can be
         * interpolated.
         */
        result = new MeasurementValue(requiredSensorType, sensorValues.getValue(
          valueTime, !requiredSensorType.equals(variable.getCoreSensorType())));
      } catch (SensorGroupsException e) {
        throw new MeasurementValueCalculatorException(
          "Cannot calculate time offset", e);
      }

      // Calibrate the value if (a) the SensorType can have calibrations, and
      // (b) the instrument has calibration Run Types defined.
      if (allowCalibration && requiredSensorType.hasInternalCalibration()
        && instrument.hasInternalCalibrations()) {
        calibrate(instrument, dataSet, timeReference, requiredSensorType,
          result, allMeasurements, sensorValues, conn);
      }
    }

    return result;
  }

  /**
   * Get the {@link MeasurementValue} for a position (longitude or latitude).
   * Automatically applies a time offset to the first sensor group defined for
   * the instrument.
   *
   * @param instrument
   *          The {@link Instrument} for which measurements are being
   *          calculated.
   * @param dataSet
   *          The {@link DataSet} for which measurements are being calculated.
   * @param timeReference
   *          A {link SensorValuesListValue} containing details of the time
   *          period to be considered when building the {@link MeasuremntValue}.
   * @param variable
   *          The {@link Variable} for the current {@link Measurement}.
   * @param requiredSensorType
   *          The {@link SensorType} for which a value is required.
   * @param allMeasurements
   *          The complete set of {@link Measurements} for the current
   *          {@link DataSet}.
   * @param allSensorValues
   *          The complete set of {@link SensorValue}s for the current
   *          {@link DataSet}.
   * @param conn
   *          A database connection.
   * @return The calculated {@link MeasurementValue}.
   * @throws SensorValuesListException
   *           If {@link SensorValue}s cannot be retrieved for the position.
   * @throws MeasurementValueCalculatorException
   *           If the value cannot be constructed.
   */
  private MeasurementValue getPositionValue(Instrument instrument,
    DataSet dataSet, SensorValuesListValue timeReference, Variable variable,
    SensorType requiredSensorType, DatasetMeasurements allMeasurements,
    DatasetSensorValues allSensorValues, Connection conn)
    throws SensorValuesListException, MeasurementValueCalculatorException {

    MeasurementValue result;

    SensorAssignment coreAssignment = instrument.getSensorAssignments()
      .get(variable.getCoreSensorType()).first();

    try {
      // If Lon/Lat, get offset to first group. Get lat/lon at that time.
      LocalDateTime positionTime = dataSet.getSensorOffsets()
        .offsetToFirstGroup(timeReference.getNominalTime(), coreAssignment);

      long columnId = requiredSensorType
        .equals(SensorType.LONGITUDE_SENSOR_TYPE) ? SensorType.LONGITUDE_ID
          : SensorType.LATITUDE_ID;

      SensorValuesList sensorValues = allSensorValues.getColumnValues(columnId);

      SensorValuesListOutput sensorValue = null == sensorValues ? null
        : sensorValues.getValue(positionTime, true);

      result = null == sensorValue ? null
        : new MeasurementValue(requiredSensorType, sensorValue);
    } catch (SensorGroupsException e) {
      throw new MeasurementValueCalculatorException(
        "Unable to apply sensor offsets", e);
    }

    return result;
  }

  /**
   * Apply calibration to a {@link MeasurementValue}.
   *
   * @param instrument
   *          The {@link Instrument} for which measurements are being
   *          calculated.
   * @param measurement
   *          The {@link Measurement} being calculated.
   * @param sensorType
   *          The {@link SensorType} of the value being calibrated.
   * @param value
   *          The value to be calculated.
   * @param allMeasurements
   *          The complete set of {@link Measurements} for the current
   *          {@link DataSet}.
   * @param allSensorValues
   *          The complete set of {@link SensorValue}s for the current
   *          {@link DataSet}.
   * @param conn
   *          A database connection.
   * @throws MeasurementValueCalculatorException
   *           If the calibration cannot be performed.
   */
  protected void calibrate(Instrument instrument, DataSet dataset,
    SensorValuesListValue timeReference, SensorType sensorType,
    MeasurementValue value, DatasetMeasurements allMeasurements,
    SensorValuesList sensorValues, Connection conn)
    throws MeasurementValueCalculatorException {

    if (!value.getCalculatedValue().isNaN()) {

      try {
        // TODO This reads the calibrations from the database every time. Load
        // it up front.
        CalibrationSet externalStandards = ExternalStandardDB.getInstance()
          .getCalibrationSet(conn, dataset);

        // Get the calibration offset for the standard run prior to the
        // measurement
        CalibrationOffset prior = getCalibrationOffset(PRIOR, externalStandards,
          allMeasurements, sensorValues, sensorType,
          timeReference.getNominalTime(), value);

        // Get the calibration offset for the standard run after the measurement
        CalibrationOffset post = getCalibrationOffset(POST, externalStandards,
          allMeasurements, sensorValues, sensorType,
          timeReference.getNominalTime(), value);

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
            timeReference.getNominalTime());

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
    CalibrationSet externalStandards, DatasetMeasurements allMeasurements,
    SensorValuesList sensorValues, SensorType sensorType,
    LocalDateTime measurementTime, MeasurementValue value)
    throws RecordNotFoundException {

    CalibrationOffset result = new CalibrationOffset();

    SimpleRegression regression = new SimpleRegression();

    // Loop through each calibration target
    for (String runType : externalStandards.getTargets()) {

      double standardConcentration = externalStandards
        .getCalibrations(measurementTime).get(runType)
        .getDoubleCoefficient(sensorType.getShortName());

      if (sensorType.includeZeroInCalibration()
        || standardConcentration > 0.0D) {

        // Get the measurements for the closest run
        TreeSet<Measurement> runTypeMeasurements;

        if (direction == PRIOR) {
          runTypeMeasurements = allMeasurements.getRunBefore(
            Measurement.RUN_TYPE_DEFINES_VARIABLE, runType, measurementTime);
        } else {
          runTypeMeasurements = allMeasurements.getRunAfter(
            Measurement.RUN_TYPE_DEFINES_VARIABLE, runType, measurementTime);
        }

        /*
         * Get the mean sensor value for these calibration measurements,
         * filtering out any bad ones.
         */
        List<SensorValue> runSensorValues = runTypeMeasurements.stream()
          .map(m -> sensorValues.getRawSensorValue(m.getTime()))
          .filter(v -> null != v).filter(v -> !v.getDoubleValue().isNaN())
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
