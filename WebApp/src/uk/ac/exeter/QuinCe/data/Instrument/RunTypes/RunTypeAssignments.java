package uk.ac.exeter.QuinCe.data.Instrument.RunTypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Instrument.MissingRunTypeException;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Holder for a set of run type assignments for a file definition. This class
 * does not reference the file definition to which it belongs - it is stored as
 * a field of the FileDefinition class.
 */
@SuppressWarnings("serial")
public class RunTypeAssignments extends TreeMap<String, RunTypeAssignment> {

  public static final String RUN_TYPE_SEPARATOR = "|";

  /**
   * The Run Type column in the parent file definition
   */
  private TreeSet<Integer> columns;

  public RunTypeAssignments(int column) {
    super();
    columns = new TreeSet<Integer>();
    columns.add(column);
  }

  public void addColumn(int column) {
    columns.add(column);
  }

  public void removeColumn(int column) {
    columns.remove(column);
  }

  public int getColumnCount() {
    return columns.size();
  }

  /**
   * Automatically convert a key to match the internal Map If the key is a
   * String, convert it to lower case. Otherwise leave it as it is
   *
   * @param key
   *          The key
   * @return The converted key
   */
  private static String convertKey(Object key) {
    return key.toString().toLowerCase();
  }

  /**
   * Get the category to which the specified run type is assigned. If the run
   * type is an alias, category of the run type to which the specified type is
   * aliased will be returned.
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
   *
   * @return The run type column
   */
  public TreeSet<Integer> getColumns() {
    return columns;
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
   *
   * @param runName
   *          The run name
   * @param followAlias
   *          Indicates whether or not aliases should be followed
   * @return The Run Type
   */
  public RunTypeAssignment get(String runName, boolean followAlias)
    throws MissingRunTypeException {

    RunTypeAssignment result = null;
    String searchKey = convertKey(runName);

    if (!containsKey(searchKey)) {
      throw new MissingRunTypeException(
        "Unrecognised run type '" + runName + "'. Please register new run type "
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

  public Map<String, Long> getVariableRunTypes() {

    Map<String, Long> result = new HashMap<String, Long>();

    for (Map.Entry<String, RunTypeAssignment> entry : entrySet()) {
      RunTypeCategory category = entry.getValue().getCategory();
      if (category.isVariable()) {
        result.put(entry.getKey(), category.getType());
      }
    }

    return result;
  }

  public String getRunType(List<String> line) {
    return StringUtils.listToDelimited(line, columns, RUN_TYPE_SEPARATOR);
  }
}
