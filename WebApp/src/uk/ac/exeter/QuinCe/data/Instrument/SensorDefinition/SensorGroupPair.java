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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((first == null) ? 0 : first.hashCode());
    result = prime * result + ((second == null) ? 0 : second.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SensorGroupPair other = (SensorGroupPair) obj;
    if (first == null) {
      if (other.first != null)
        return false;
    } else if (!first.equals(other.first))
      return false;
    if (second == null) {
      if (other.second != null)
        return false;
    } else if (!second.equals(other.second))
      return false;
    return true;
  }
}
