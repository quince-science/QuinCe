package uk.ac.exeter.QuinCe.data.Instrument.RunTypes;

import java.util.TreeMap;

/**
 * Holder for a set of run type assignments for a file
 * @author zuj007
 *
 */
public class RunTypeAssignments extends TreeMap<String, RunTypeAssignment> {

  public RunTypeAssignments() {
    super();
  }

  /**
   * Automatically convert a key to match the internal Map
   * If the key is a String, convert it to lower case.
   * Otherwise leave it as it is
   * @param key The key
   * @return The converted key
   */
  private Object convertKey(Object key) {
    Object result = key;

    if (key instanceof String) {
      result = ((String) key).toLowerCase();
    }

    return result;
  }

  /**
   * Get the category to which the specified run type is assigned.
   * If the run type is an alias, category of the run type
   * to which the specified type is aliased will be returned.
   *
   * If the specified run type is not found, {@code null} is returned.
   *
   * @return The assigned category
   */
  public RunTypeCategory getRunTypeCategory(String runType) {
    RunTypeCategory result = null;

    RunTypeAssignment assignment = get(runType);
    if (null != assignment) {
      result = assignment.getCategory();
      if (assignment.isAlias()) {
        result = getRunTypeCategory(assignment.getAliasTo());
      }
    }

    return result;
  }

  @Override
  public RunTypeAssignment put(String key, RunTypeAssignment assignment) {
    return super.put(key.toLowerCase(), assignment);
  }

  @Override
  public RunTypeAssignment get(Object key) {
    return super.get(convertKey(key));
  }

  @Override
  public boolean containsKey(Object key) {
    return super.containsKey(convertKey(key));
  }
}
