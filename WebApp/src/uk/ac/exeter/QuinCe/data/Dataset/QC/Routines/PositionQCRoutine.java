package uk.ac.exeter.QuinCe.data.Dataset.QC.Routines;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;
import uk.ac.exeter.QuinCe.jobs.files.ExtractDataSetJob;

/**
 * Auto QC routine for position values.
 *
 * <p>
 * This is a special routine that must be run independently of the other
 * routines, since it operates very differently. The constructor takes the
 * complete set of {@link SensorValue}s from the {@link AutoQCJob} and works
 * with them directly instead of receiving just the position values as a normal
 * {@link Routine} would. This is because the position QC affects the QC all of
 * other measured values in the dataset.
 * </p>
 *
 * <p>
 * The routine checks for missing position values, and positions outside the
 * legal range (-180:180 for longitude, -90:90 for latitude). Note that the
 * {@link ExtractDataSetJob} will have converted positions of any format to
 * these ranges. Any position values that fail these checks will be marked BAD.
 * Additionally, all sensors will also have a BAD flag applied, since a sensor
 * value without a valid position cannot be used.
 * </p>
 *
 * <p>
 * <b>Note:</b> This routine should only set BAD QC flags. The behaviour of
 * subsequent QC if the position QC is QUESTIONABLE is undefined.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class PositionQCRoutine extends Routine {

  /**
   * Prefix applied to QC comments on sensors inherited from positional QC
   */
  public static final String POSITION_QC_PREFIX = "Position QC:";

  /**
   * The longitude values
   */
  private List<SensorValue> lonValues;

  /**
   * The latitude values
   */
  private List<SensorValue> latValues;

  /**
   * The set of {@link SensorValue}s for the dataset from the {@link AutoQCJob},
   * excluding diagnostics and system values.
   */
  private Set<SearchableSensorValuesList> sensorValues;

  /**
   * Initialise the routine with the position and sensor values.
   *
   * <p>
   * {@code sensorValues} should contain only values from data sensors;
   * diagnostic and other system sensors should be excluded.
   * </p>
   *
   * @param sensorAssignments
   *          The sensor assignments for the instrument whose dataset is being
   *          QCed
   * @param sensorValues
   *          The sensor values
   * @throws RoutineException
   *           If the routine cannot be constructed
   */
  public PositionQCRoutine(List<SensorValue> lonValues,
    List<SensorValue> latValues, Set<SearchableSensorValuesList> sensorValues)
    throws RoutineException {

    super(null);

    if (lonValues.size() != latValues.size()) {
      throw new RoutineException("Longitude and latitude counts are different");
    }

    this.lonValues = lonValues;
    this.latValues = latValues;
    this.sensorValues = sensorValues;
  }

  /**
   * This routine does not take any parameters, so the validation method does
   * nothing.
   */
  @Override
  protected void validateParameters() throws RoutineException {
    // NOOP
  }

  @Override
  public void qcValues(List<SensorValue> values) throws RoutineException {

    // Initialise date range searches for all SensorValue lists
    sensorValues.forEach(SearchableSensorValuesList::initRangeSearch);

    int posIndex = 0;

    while (posIndex < lonValues.size()) {

      SensorValue lon = lonValues.get(posIndex);
      SensorValue lat = latValues.get(posIndex);

      boolean qcFailed = false;

      // The method to use to set QC flags on sensors
      Consumer<SensorValue> flagSetter = null;

      // Missing value check
      if (isMissing(lon) || isMissing(lat)) {
        setPositionMissing(lon, lat);
        qcFailed = true;
        flagSetter = this::setPositionMissing;

      } else {

        // Range check
        double lonValue = lon.getDoubleValue();
        double latValue = lat.getDoubleValue();

        if (lonValue < -180.0 || lonValue > 180.0) {
          qcFailed = true;
        } else if (latValue < -90.0 || latValue > 90.0) {
          qcFailed = true;
        }

        if (qcFailed) {
          setPositionOutOfRange(lon, lat);
          flagSetter = this::setPositionOutOfRange;
        }
      }

      // If the position QC failed, apply the same QC flag to each sensor value
      // between this and the next position (exclusive).
      // This handles the case where sensor values are not aligned with position
      // values in time.
      if (qcFailed) {
        LocalDateTime currentPosTime = lon.getTime();
        LocalDateTime nextPosTime = LocalDateTime.MAX;
        SensorValue nextPos = lonValues.get(posIndex + 1);
        if (null != nextPos) {
          nextPosTime = nextPos.getTime();
        }

        for (SearchableSensorValuesList valuesList : sensorValues) {
          List<SensorValue> updateValues = valuesList
            .rangeSearch(currentPosTime, nextPosTime);

          updateValues.forEach(flagSetter);
        }
      }

      posIndex++;
    }
  }

  /**
   * Determine whether or not a {@link SensorValue} contains a missing value.
   *
   * @param value
   *          The value to check.
   * @return {@code true} if the value is missing; {@code false} otherwise.
   */
  private boolean isMissing(SensorValue value) {
    return null == value.getValue();
  }

  /**
   * Set the QC for an Out Of Range position value on the specified
   * {@link SensorValue}. More than one value can be supplied.
   *
   * @param value
   *          The value whose QC is to be set.
   * @see #setPositionQC(String, SensorValue...)
   */
  private void setPositionOutOfRange(SensorValue... value) {
    setPositionQC("Out of range", value);
  }

  /**
   * Set the QC for an Missing position value on the specified
   * {@link SensorValue}. More than one value can be supplied.
   *
   * @param value
   *          The value whose QC is to be set.
   * @see #setPositionQC(String, SensorValue...)
   */
  private void setPositionMissing(SensorValue... value) {
    setPositionQC("Missing", value);
  }

  /**
   * Set the position QC result as the QC for a {@link SensorValue}. Multiple
   * values can be supplied.
   *
   * <p>
   * The QC message will be prepended with {@link #POSITION_QC_PREFIX}.
   * </p>
   *
   * @param message
   *          The QC message.
   * @param value
   *          The value whose QC is to be set.
   */
  private void setPositionQC(String message, SensorValue... value) {
    for (SensorValue v : value) {
      v.setUserQC(Flag.BAD, POSITION_QC_PREFIX + message);
    }
  }
}
