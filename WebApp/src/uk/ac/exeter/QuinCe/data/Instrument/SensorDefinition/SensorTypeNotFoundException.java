package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeading;
import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeadingNotFoundException;

/**
 * Exception for sensor types that can't be found
 */
@SuppressWarnings("serial")
public class SensorTypeNotFoundException
  extends ColumnHeadingNotFoundException {

  public SensorTypeNotFoundException(String sensorName) {
    super(sensorName);
  }

  public SensorTypeNotFoundException(long sensorId) {
    super(sensorId);
  }

  public SensorTypeNotFoundException(ColumnHeading columnHeading) {
    super(columnHeading);
  }

  protected String getItemName() {
    return "Sensor Type";
  }
}
