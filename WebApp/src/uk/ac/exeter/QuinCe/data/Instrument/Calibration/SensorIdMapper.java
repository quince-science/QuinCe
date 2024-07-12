package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;

public class SensorIdMapper implements CalibrationTargetNameMapper {

  private SensorAssignments sensorAssignments;

  public SensorIdMapper(SensorAssignments sensorAssignments) {
    this.sensorAssignments = sensorAssignments;
  }

  @Override
  public String map(String target) {
    return sensorAssignments.getById(Long.parseLong(target)).getSensorName();
  }
}
