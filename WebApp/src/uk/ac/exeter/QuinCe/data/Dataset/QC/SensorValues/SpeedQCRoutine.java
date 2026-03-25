package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.util.List;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;

import uk.ac.exeter.QuinCe.data.Dataset.Coordinate;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * QC routine to check ship speed.
 *
 * <p>
 * Because the GPS resolution is recorded to 3 decimal places, it is possible
 * for a ship to travel from 0N/0E to 0.001N/0.001E between 1-second
 * measurements. This equates to an apparent speed of 157.3 ms<sup>-1</sup>.
 * Therefore we have a rounded up allowable max speed of 160 ms<sup>-1</sup>.
 * </p>
 */
public class SpeedQCRoutine extends PositionQCRoutine {

  private static final double MAX_SPEED = 160;

  public SpeedQCRoutine(DatasetSensorValues positionSensorValues)
    throws RoutineException, MissingParamException {
    super(positionSensorValues);
  }

  @Override
  protected void qcAction(List<SensorValue> values) throws RoutineException {

    try {

      Coordinate lastCoordinate = null;
      SensorValue lastLon = null;
      SensorValue lastLat = null;
      LatLng lastPos = null;

      // Step through each time in the dataset
      for (Coordinate coordinate : allSensorValues
        .getRawPositionCoordinates()) {

        // Get the position values for this time
        SensorValue longitude = allSensorValues
          .getRawSensorValue(SensorType.LONGITUDE_ID, coordinate);
        SensorValue latitude = allSensorValues
          .getRawSensorValue(SensorType.LATITUDE_ID, coordinate);

        if (null != longitude && null != latitude && !longitude.isNaN()
          && !latitude.isNaN()
          && flagScheme.isGood(longitude.getDisplayFlag(allSensorValues), true)
          && flagScheme.isGood(latitude.getDisplayFlag(allSensorValues),
            true)) {

          LatLng pos = new LatLng(latitude.getDoubleValue(),
            longitude.getDoubleValue());

          if (null != lastCoordinate) {
            double distance = LatLngTool.distance(lastPos, pos,
              LengthUnit.METER);
            long seconds = Math.abs(DateTimeUtils
              .secondsBetween(coordinate.getTime(), lastCoordinate.getTime()));

            double speed = distance / (double) seconds;

            if (speed > MAX_SPEED) {
              flag(speed, lastLon, lastLat, longitude, latitude);
            }
          }

          lastCoordinate = coordinate;
          lastLon = longitude;
          lastLat = latitude;
          lastPos = pos;
        }
      }
    } catch (Exception e) {
      throw new RoutineException(e);
    }
  }

  @Override
  public String getShortMessage() {
    return "Speed too high";
  }

  @Override
  public String getLongMessage(RoutineFlag flag) {
    return "Speed too high";
  }

  private void flag(double speed, SensorValue... values)
    throws RoutineException {

    for (SensorValue value : values) {
      addFlag(value, flagScheme.getBadFlag(), MAX_SPEED, speed);
    }
  }
}
