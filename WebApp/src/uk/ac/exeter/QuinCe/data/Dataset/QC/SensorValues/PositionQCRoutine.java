package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.RunTypePeriods;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;
import uk.ac.exeter.QuinCe.jobs.files.ExtractDataSetJob;
import uk.ac.exeter.QuinCe.utils.CollectionUtils;
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
public class PositionQCRoutine extends AutoQCRoutine {

  /**
   * The instrument.
   */
  private Instrument instrument;

  /**
   * The complete set of sensor values for the current dataset
   */
  private final DatasetSensorValues allSensorValues;

  /**
   * The run types in the dataset.
   */
  private SearchableSensorValuesList runTypes;

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
  public PositionQCRoutine(Instrument instrument,
    DatasetSensorValues allSensorValues, SearchableSensorValuesList runTypes)
    throws RoutineException, MissingParamException {

    super();
    super.parameters = new ArrayList<String>(); // No parameters needed

    MissingParam.checkMissing(instrument, "instrument");
    MissingParam.checkMissing(allSensorValues, "allSensorValues");
    MissingParam.checkMissing(runTypes, "runTypes", true);

    this.instrument = instrument;
    this.allSensorValues = allSensorValues;
    this.runTypes = runTypes;
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
  public void qc(List<SensorValue> values, RunTypePeriods runTypePeriods)
    throws RoutineException {
    qcAction(values);
  }

  @Override
  protected void qcAction(List<SensorValue> values) throws RoutineException {

    try {
      SearchableSensorValuesList longitudes = allSensorValues
        .getColumnValues(SensorType.LONGITUDE_ID);
      SearchableSensorValuesList latitudes = allSensorValues
        .getColumnValues(SensorType.LATITUDE_ID);

      // Step through each time in the dataset
      for (LocalDateTime time : allSensorValues.getTimes()) {

        // Get the position values for this time
        List<SensorValue> longitude = longitudes.getWithInterpolation(time,
          false, true);
        List<SensorValue> latitude = latitudes.getWithInterpolation(time, false,
          true);

        // Figure out what the position flag and QC message are
        Flag positionFlag = Flag.GOOD;
        String positionMessage = null;

        if (CollectionUtils.getNonNullCount(longitude) == 0
          || CollectionUtils.getNonNullCount(latitude) == 0) {
          positionFlag = Flag.BAD;
          positionMessage = "Missing";
        } else {
          SensorValue worstLongitude = SensorValue
            .getValueWithWorstFlag(longitude);
          SensorValue worstLatitude = SensorValue
            .getValueWithWorstFlag(longitude);

          if (worstLongitude.getDisplayFlag()
            .moreSignificantThan(worstLatitude.getDisplayFlag())) {
            positionFlag = worstLongitude.getDisplayFlag();
            positionMessage = worstLongitude.getDisplayQCMessage();
          } else {
            positionFlag = worstLatitude.getDisplayFlag();
            positionMessage = worstLatitude.getDisplayQCMessage();
          }
        }

        if (!positionFlag.equals(Flag.GOOD)) {
          // Now loop through all SensorValues for this time and apply the
          // position QC
          for (Map.Entry<Long, SensorValue> entry : allSensorValues.get(time)
            .entrySet()) {

            SensorType sensorType = instrument.getSensorAssignments()
              .getSensorTypeForDBColumn(entry.getKey());

            // We don't re-set position value flags
            if (!sensorType.isPosition()) {

              // Sensor values with internal calibrations are OK if they aren't
              // part of a measurement; they can still be used for calibrations,
              // so we don't flag them.
              boolean flagValue = true;

              if (sensorType.hasInternalCalibration()) {
                String runType = runTypes.timeSearch(entry.getValue().getTime())
                  .getValue();
                if (!instrument.getMeasurementRunTypes().contains(runType)) {
                  flagValue = false;
                }
              }

              if (flagValue) {
                entry.getValue().setPositionQC(positionFlag, positionMessage);
              }
            }
          }
        }
      }
    } catch (Exception e) {
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
}
