package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

public class SensorTypeNodeData extends AssignmentsTreeNodeData {

  private final SensorType sensorType;

  protected SensorTypeNodeData(SensorType sensorType) {
    this.sensorType = sensorType;
  }

  protected SensorType getSensorType() {
    return sensorType;
  }

  @Override
  public String getLabel() {
    return sensorType.getShortName();
  }

}
