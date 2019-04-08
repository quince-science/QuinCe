package uk.ac.exeter.QuinCe.data.Instrument.RunTypes;

import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Instrument.MissingRunTypeException;

/**
 * Holder for a set of run type assignments for a file definition.
 * This class does not reference the file definition to which it belongs -
 * it is stored as a field of the FileDefinition class.
 *
 * @author zuj007
 *
 */
public class RunTypeAssignments extends TreeMap<String, RunTypeAssignment> {

  /**
   * Serial version UID
   */
  private static final long serialVersionUID = -4123234930155566035L;

  /**
   * The Run Type column in the parent file definition
   */
  private int column;

  public RunTypeAssignments(int column) {
    super();
    this.column = column;
  }

  /**
   * Automatically convert a key to match the internal Map
   * If the key is a String, convert it to lower case.
   * Otherwise leave it as it is
   * @param key The key
   * @return The converted key
   */
  private static String convertKey(Object key) {
    return key.toString().toLowerCase();
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

  /**
   * Get the Run Type column for the parent file definition
   * @return The run type column
   */
  public int getColumn() {
    return column;
  }

  @Override
  public RunTypeAssignment put(String key, RunTypeAssignment assignment) {
    return super.put(key.toLowerCase(), assignment);
  }

  @Override
  public RunTypeAssignment get(Object key) {
    RunTypeAssignment result = null;

    try {
      result = get(convertKey(key), true);
    } catch (MissingRunTypeException e) {
      // Do nothing - we'll return null
    }

    return result;
  }

  @Override
  public boolean containsKey(Object key) {
    return super.containsKey(convertKey(key));
  }

  /**
   * Get the run type for a given run name, following aliases if required
   * @param runName The run name
   * @param followAlias Indicates whether or not aliases should be followed
   * @return The Run Type
   */
  public RunTypeAssignment get(String runName, boolean followAlias)
    throws MissingRunTypeException {

    RunTypeAssignment result = null;
    String searchKey = convertKey(runName);

    if (!containsKey(searchKey)) {
      throw new MissingRunTypeException("Unrecognised run type '"
        + runName + "'. Please register new run type "
        + runName + " on instrument to load this file.",
        runName);
    } else {
      if (!followAlias) {
        result = super.get(searchKey);
      } else {
        RunTypeAssignment assignment = super.get(searchKey);
        while (assignment.isAlias()) {
          assignment = get(assignment.getAliasTo());
        }

        result = assignment;
      }
    }

    return result;
  }
}
