package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

public class SensorGroupPair {

  private final SensorGroup first;

  private final SensorGroup second;

  protected SensorGroupPair(SensorGroup first, SensorGroup second) {
    this.first = first;
    this.second = second;
  }

  public SensorGroup first() {
    return first;
  }

  public SensorGroup second() {
    return second;
  }

  @Override
  public String toString() {
    return first.getName() + "/" + second.getName();
  }

  /**
   * Get a unique identifier for this group pair
   * 
   * @return
   */
  public int getId() {
    return first.getName().hashCode();
  }
}
