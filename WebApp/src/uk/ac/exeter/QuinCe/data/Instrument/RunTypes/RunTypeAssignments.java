package uk.ac.exeter.QuinCe.data.Instrument.RunTypes;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.MissingRunTypeException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.PresetRunType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.AscendingLengthComparator;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Holder for a set of run type assignments for a file definition. This class
 * does not reference the file definition to which it belongs - it is stored as
 * a field of the FileDefinition class.
 *
 * <p>
 * This class is a map of {@code Run Type Name -> RunTypeAssignment}, so looking
 * up a run type name from an input file will give its assignment details.
 * </p>
 *
 * <p>
 * All run type names are converted to lower case.
 * </p>
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

  /**
   * Generate a new {@code RunTypeAssignments} object based using the specified
   * run types.
   *
   * <p>
   * Run types are converted to {@link RunTypeAssignments} based on the
   * {@link #PRESET_RUN_TYPES}. Any run types not found in the presets will be
   * set to {@link RunTypeCategory#IGNORED}.
   * </p>
   */
  public static RunTypeAssignments buildRunTypes(Collection<Variable> variables,
    int column, Collection<String> runTypes) {

    RunTypeAssignments result = new RunTypeAssignments(column);

    /*
     * Find each run type in the PresetRunTypes from each Variable.
     *
     * Since a PresetRunType contains multiple aliases, map each runType string
     * against its matching PresetRunType. Once we have all of those, we can set
     * up the RunTypeAssignments for all the runType strings.
     */

    Map<PresetRunType, TreeSet<String>> runTypeGroups = new TreeMap<PresetRunType, TreeSet<String>>();

    for (String runType : runTypes) {

      // Search the preset run types for each variable
      for (Variable variable : variables) {
        for (PresetRunType presetRunType : variable.getPresetRunTypes()) {
          if (presetRunType.containsRunType(runType)) {

            if (!runTypeGroups.containsKey(presetRunType)) {
              runTypeGroups.put(presetRunType,
                new TreeSet<String>(new AscendingLengthComparator()));
            }

            runTypeGroups.get(presetRunType).add(runType);
            break;
          }
        }
      }
    }

    /*
     * Now we have the groups for aliases, we set them up.
     *
     * We know that the first entry in each group is the 'base' run type
     * (because we sort by shortest first), and the others should alias to it.
     */
    for (Map.Entry<PresetRunType, TreeSet<String>> entry : runTypeGroups
      .entrySet()) {

      TreeSet<String> groupRunTypes = entry.getValue();

      String mainRunType = groupRunTypes.first();

      result.put(mainRunType,
        new RunTypeAssignment(mainRunType, entry.getKey().getCategory()));

      for (String otherRunType : groupRunTypes.tailSet(mainRunType, false)) {
        result.put(otherRunType,
          new RunTypeAssignment(otherRunType, mainRunType));
      }
    }

    return result;
  }

  public static RunTypeAssignment getPreviousRunTypeAssignment(String runType,
    List<Instrument> previousInstruments) {

    RunTypeAssignment result = null;

    for (Instrument previousInstrument : previousInstruments) {

      Map<RunTypeCategory, TreeSet<RunTypeAssignment>> previousAssignments = previousInstrument
        .getAllRunTypes();

      for (Map.Entry<RunTypeCategory, TreeSet<RunTypeAssignment>> entry : previousAssignments
        .entrySet()) {

        RunTypeAssignment foundAssignment = entry.getValue().stream()
          .filter(a -> a.getRunName().equals(runType.toLowerCase())).findAny()
          .orElse(null);

        if (null != foundAssignment) {
          if (foundAssignment.isAlias()) {
            result = new RunTypeAssignment(foundAssignment.getRunName(),
              foundAssignment.getAliasTo());
          } else {
            result = new RunTypeAssignment(foundAssignment.getRunName(),
              foundAssignment.getCategory());
          }
          break;
        }
      }
    }

    return result;
  }

  public static RunTypeAssignment getPresetAssignment(
    Collection<Variable> variables, String runType,
    RunTypeAssignments existingAssignments) {

    RunTypeAssignment result = null;

    PresetRunType preset = null;

    for (Variable variable : variables) {
      for (PresetRunType presetRunType : variable.getPresetRunTypes()) {
        if (presetRunType.containsRunType(runType)) {
          preset = presetRunType;
          break;
        }
      }
    }

    if (null != preset) {
      for (String baseRunType : existingAssignments.keySet()) {
        if (preset.containsRunType(baseRunType)
          && !existingAssignments.get(baseRunType).isAlias()) {
          result = new RunTypeAssignment(runType, baseRunType);
          break;
        }
      }
    }

    return result;
  }

  public boolean allIgnored() {
    return values().stream()
      .allMatch(v -> v.getCategory().equals(RunTypeCategory.IGNORED));
  }
}
