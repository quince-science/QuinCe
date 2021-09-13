package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
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
   * The longitude values
   */
  private List<SensorValue> lonValues;

  /**
   * The latitude values
   */
  private List<SensorValue> latValues;

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
  public PositionQCRoutine(List<SensorValue> lonValues,
    List<SensorValue> latValues, Instrument instrument,
    DatasetSensorValues allSensorValues, SearchableSensorValuesList runTypes)
    throws RoutineException, MissingParamException {

    super();
    super.parameters = new ArrayList<String>(); // No parameters needed

    MissingParam.checkMissing(lonValues, "lonValues", true);
    MissingParam.checkMissing(latValues, "latValues", true);
    MissingParam.checkMissing(instrument, "instrument");
    MissingParam.checkMissing(allSensorValues, "allSensorValues");
    MissingParam.checkMissing(runTypes, "runTypes", true);

    if (lonValues.size() != latValues.size()) {
      throw new RoutineException("Longitude and latitude counts are different");
    }

    this.lonValues = lonValues;
    this.latValues = latValues;
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
  protected void qcAction(List<SensorValue> values) throws RoutineException {

    try {
      int posIndex = 0;

      while (posIndex < lonValues.size()) {

        SensorValue lon = lonValues.get(posIndex);
        SensorValue lat = latValues.get(posIndex);

        boolean qcFailed = false;

        // The method to use to set QC flags on sensors
        String qcMessage = null;

        // Missing value check
        if (lon.isNaN() || lat.isNaN()) {
          setPositionQC("Missing", lon, lat);
          qcFailed = true;
          qcMessage = "Missing";

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
            setPositionQC("Out of range", lon, lat);
            qcMessage = "Out of range";
          }
        }

        // If the position QC failed, apply the same QC flag to each sensor
        // value between this and the next position (exclusive).
        // This handles the case where sensor values are not aligned with
        // position values in time.
        if (qcFailed) {
          LocalDateTime currentPosTime = lon.getTime();
          LocalDateTime nextPosTime = LocalDateTime.MAX;

          SensorValue nextPos = null;
          if (posIndex + 1 < lonValues.size()) {
            nextPos = lonValues.get(posIndex + 1);
          }
          if (null != nextPos) {
            nextPosTime = nextPos.getTime();
          }

          for (long columnId : instrument.getSensorAssignments()
            .getSensorColumnIds()) {
            SearchableSensorValuesList columnValues = allSensorValues
              .getColumnValues(columnId);

            SensorType sensorType = instrument.getSensorAssignments()
              .getSensorTypeForDBColumn(columnId);

            for (SensorValue value : columnValues.rangeSearch(currentPosTime,
              nextPosTime)) {

              // Sensor values with internal calibrations are OK if they aren't
              // part of a measurement; they can still be used for calibrations,
              // so we don't flag them.
              boolean flagValue = true;

              if (sensorType.hasInternalCalibration()) {
                String runType = runTypes.timeSearch(value.getTime())
                  .getValue();
                if (!instrument.getMeasurementRunTypes().contains(runType)) {
                  flagValue = false;
                }
              }

              if (flagValue) {
                value.setPositionQC(Flag.BAD, qcMessage);
              }
            }
          }
        }

        posIndex++;
      }
    } catch (Exception e) {
      throw new RoutineException(e);
    }
  }

  /**
   * Set the position QC result as the QC for a {@link SensorValue}. Multiple
   * values can be supplied.
   *
   * <p>
   * The QC message will be prepended with
   * {@link SensorValue#POSITION_QC_PREFIX}.
   * </p>
   *
   * @param message
   *          The QC message.
   * @param value
   *          The value whose QC is to be set.
   * @throws RoutineException
   */
  private void setPositionQC(String message, SensorValue... value)
    throws RoutineException {
    for (SensorValue v : value) {
      v.setPositionQC(Flag.BAD, message);
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
}
