package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.util.Collection;
import java.util.Objects;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;

public class PresetRunType implements Comparable<PresetRunType> {

  private TreeSet<String> runTypes;
  private RunTypeCategory category;

  protected PresetRunType(Collection<String> runTypes,
    RunTypeCategory category) {
    this.runTypes = new TreeSet<String>();
    this.runTypes.addAll(runTypes);
    this.category = category;
  }

  public boolean containsRunType(String runType) {
    return runTypes.contains(runType.toLowerCase());
  }

  public RunTypeCategory getCategory() {
    return category;
  }

  public String getDefaultRunType() {
    return runTypes.first();
  }

  @Override
  public String toString() {
    return runTypes.toString() + " -> " + category.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(runTypes);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    PresetRunType other = (PresetRunType) obj;
    return runTypes.first().equals(other.runTypes.first());
  }

  @Override
  public int compareTo(PresetRunType o) {
    return runTypes.first().compareTo(o.runTypes.first());
  }
}
