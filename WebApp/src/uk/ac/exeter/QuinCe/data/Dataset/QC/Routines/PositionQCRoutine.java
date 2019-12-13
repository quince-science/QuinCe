package uk.ac.exeter.QuinCe.data.Dataset.QC.Routines;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import uk.ac.exeter.QuinCe.data.Dataset.NavigableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;

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
  private Set<NavigableSensorValuesList> sensorValues;

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
    List<SensorValue> latValues, Set<NavigableSensorValuesList> sensorValues)
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
    sensorValues.forEach(NavigableSensorValuesList::initRangeSearch);

    int posIndex = 0;

    while (posIndex < lonValues.size()) {

      SensorValue lon = lonValues.get(posIndex);
      SensorValue lat = latValues.get(posIndex);

      if (isMissing(lon) || isMissing(lat)) {

        LocalDateTime currentPosTime = lon.getTime();
        LocalDateTime nextPosTime = LocalDateTime.MAX;
        SensorValue nextPos = lonValues.get(posIndex + 1);
        if (null != nextPos) {
          nextPosTime = nextPos.getTime();
        }

        for (NavigableSensorValuesList valuesList : sensorValues) {
          List<SensorValue> updateValues = valuesList
            .rangeSearch(currentPosTime, nextPosTime);

          updateValues.forEach(this::setPositionMissing);
        }
      }

      posIndex++;
    }
  }

  private boolean isMissing(SensorValue value) {
    return null == value.getValue();
  }

  private void setPositionMissing(SensorValue value) {
    value.setUserQC(Flag.BAD, POSITION_QC_PREFIX + "Missing");
  }
}
