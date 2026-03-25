package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.Coordinate;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.RunTypePeriods;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;
import uk.ac.exeter.QuinCe.jobs.files.ExtractDataSetJob;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Auto QC routine for position values.
 *
 * <p>
 * This is a special routine that must be run independently of the other
 * routines, since it operates very differently. The constructor takes the
 * complete set of {@link SensorValue}s from the {@link AutoQCJob} and works
 * with them directly instead of receiving just the position values as a normal
 * {@link AutoQCRoutine} would. This is because the position QC affects the QC
 * all of other measured values in the dataset.
 * </p>
 *
 * <p>
 * The routine checks for missing position values, and positions outside the
 * legal range (-180:180 for longitude, -90:90 for latitude). Note that the
 * {@link ExtractDataSetJob} will have converted positions of any format to
 * these ranges. Any position values that fail these checks will be marked BAD.
 * Additionally, all sensors will also have a BAD flag applied, since a sensor
 * value without a valid position cannot be used (see
 * {@link PositionQCCascadeRoutine}).
 * </p>
 *
 * <p>
 * <b>Note:</b> This routine should only set BAD QC flags. The behaviour of
 * subsequent QC if the position QC is QUESTIONABLE is undefined.
 * </p>
 */
public class PositionQCRoutine extends AutoQCRoutine {

  /**
   * The complete set of sensor values for the current dataset
   */
  protected DatasetSensorValues allSensorValues;

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
   * @throws MissingParamException
   */
  public PositionQCRoutine(DatasetSensorValues positionSensorValues)
    throws RoutineException, MissingParamException {

    super(positionSensorValues.getFlagScheme(), null, null);

    MissingParam.checkMissing(positionSensorValues, "allSensorValues");
    this.allSensorValues = positionSensorValues;
  }

  @Override
  public void qc(List<SensorValue> values, RunTypePeriods runTypePeriods)
    throws RoutineException {
    qcAction(null);
  }

  @Override
  protected void qcAction(List<SensorValue> values) throws RoutineException {

    try {
      // Step through each coordinate in the dataset
      for (Coordinate time : allSensorValues.getRawPositionCoordinates()) {

        // Get the position values for this time
        SensorValue longitude = allSensorValues
          .getRawSensorValue(SensorType.LONGITUDE_ID, time);
        SensorValue latitude = allSensorValues
          .getRawSensorValue(SensorType.LATITUDE_ID, time);

        if (null == longitude || longitude.isNaN()) {
          flag(longitude, latitude);
        } else if (longitude.getDoubleValue() < -180D
          || longitude.getDoubleValue() > 180D) {
          flag(longitude, latitude);
        }

        if (null == latitude || latitude.isNaN()) {
          flag(longitude, latitude);
        } else if (latitude.getDoubleValue() < -90D
          || latitude.getDoubleValue() > 90D) {
          flag(longitude, latitude);
        }
      }
    } catch (

    Exception e) {
      throw new RoutineException(e);
    }
  }

  @Override
  public String getShortMessage() {
    return "Invalid position";
  }

  @Override
  public String getLongMessage(RoutineFlag flag) {
    return "Invalid position";
  }

  @Override
  protected void checkSetup() {
    // No checks required
  }

  private void flag(SensorValue lon, SensorValue lat) throws RoutineException {
    if (null != lon) {
      addFlag(lon, allSensorValues.getFlagScheme().getBadFlag(), "", "");
    }

    if (null != lat) {
      addFlag(lat, allSensorValues.getFlagScheme().getBadFlag(), "", "");
    }
  }
}
