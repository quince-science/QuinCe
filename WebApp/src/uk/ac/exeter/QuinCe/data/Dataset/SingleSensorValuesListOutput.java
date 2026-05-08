package uk.ac.exeter.QuinCe.data.Dataset;

public class SingleSensorValuesListOutput extends SingleSensorValuesListValue
  implements SensorValuesListOutput {

  protected SingleSensorValuesListOutput(SingleSensorValuesListValue source) {
    super(source);
  }

  @Override
  public boolean interpolatesAroundFlags() {
    return false;
  }

  @Override
  public void setInterpolatesAroundFlags() {
    // Do nothing
  }
}
