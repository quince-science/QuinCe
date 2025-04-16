package uk.ac.exeter.QuinCe.data.Instrument.RunTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Dataset.ProOceanusCO2MeasurementLocator;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.MissingRunTypeException;
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

  /**
   * The preset run type assignments already known by QuinCe.
   */
  private static List<PresetRunType> PRESET_RUN_TYPES;

  public static final String RUN_TYPE_SEPARATOR = "|";

  /**
   * The Run Type column in the parent file definition
   */
  private TreeSet<Integer> columns;

  static {

    /*
     * Set up preset run types.
     *
     * Groups of run type names indicate aliases.
     */
    PRESET_RUN_TYPES = new ArrayList<PresetRunType>();

    // General Oceanics
    PRESET_RUN_TYPES
      .add(new PresetRunType(Arrays.asList(new String[] { "equ", "equ-drain" }),
        new RunTypeCategory(1L, "Underway Marine pCO₂")));

    PRESET_RUN_TYPES
      .add(new PresetRunType(Arrays.asList(new String[] { "atm", "atm-drain" }),
        new RunTypeCategory(2L, "Underway Atmospheric pCO₂")));

    PRESET_RUN_TYPES.add(new PresetRunType(
      Arrays
        .asList(new String[] { "std1", "std1-drain", "std1z", "std1z-drain" }),
      RunTypeCategory.INTERNAL_CALIBRATION));

    PRESET_RUN_TYPES.add(new PresetRunType(
      Arrays
        .asList(new String[] { "std2", "std2-drain", "std2s", "std2s-drain" }),
      RunTypeCategory.INTERNAL_CALIBRATION));

    PRESET_RUN_TYPES.add(new PresetRunType(
      Arrays
        .asList(new String[] { "std3", "std3-drain", "std3s", "std3s-drain" }),
      RunTypeCategory.INTERNAL_CALIBRATION));

    PRESET_RUN_TYPES.add(new PresetRunType(
      Arrays
        .asList(new String[] { "std4", "std4-drain", "std4s", "std4s-drain" }),
      RunTypeCategory.INTERNAL_CALIBRATION));

    PRESET_RUN_TYPES
      .add(new PresetRunType(
        Arrays.asList(new String[] { "std5", "std5-drain", "std5s",
          "std5s-drain", "std5z", "std5z-drain" }),
        RunTypeCategory.INTERNAL_CALIBRATION));

    PRESET_RUN_TYPES
      .add(new PresetRunType(Arrays.asList(new String[] { "emergency stop" }),
        RunTypeCategory.IGNORED));

    PRESET_RUN_TYPES.add(new PresetRunType(
      Arrays.asList(new String[] { "filter" }), RunTypeCategory.IGNORED));

    PRESET_RUN_TYPES.add(new PresetRunType(
      Arrays.asList(new String[] { "go to sleep" }), RunTypeCategory.IGNORED));

    PRESET_RUN_TYPES.add(new PresetRunType(
      Arrays.asList(new String[] { "ign" }), RunTypeCategory.IGNORED));

    PRESET_RUN_TYPES.add(new PresetRunType(
      Arrays.asList(new String[] { "shut down" }), RunTypeCategory.IGNORED));

    PRESET_RUN_TYPES.add(new PresetRunType(
      Arrays.asList(new String[] { "wake up" }), RunTypeCategory.IGNORED));

    // Pro Oceanus
    PRESET_RUN_TYPES.add(new PresetRunType(
      Arrays
        .asList(new String[] { ProOceanusCO2MeasurementLocator.WATER_MODE }),
      new RunTypeCategory(8L, "Pro Oceanus CO₂ Water")));

    PRESET_RUN_TYPES.add(new PresetRunType(
      Arrays.asList(new String[] { ProOceanusCO2MeasurementLocator.ATM_MODE }),
      new RunTypeCategory(9L, "Pro Oceanus CO₂ Atmosphere")));

    // SubCTech CO2
    PRESET_RUN_TYPES.add(new PresetRunType(Arrays.asList(new String[] { "1" }),
      RunTypeCategory.INTERNAL_CALIBRATION));

    PRESET_RUN_TYPES.add(new PresetRunType(Arrays.asList(new String[] { "2" }),
      RunTypeCategory.INTERNAL_CALIBRATION));

    PRESET_RUN_TYPES.add(new PresetRunType(Arrays.asList(new String[] { "4" }),
      RunTypeCategory.IGNORED));

    PRESET_RUN_TYPES.add(new PresetRunType(Arrays.asList(new String[] { "5" }),
      new RunTypeCategory(22L, "SubCTech CO₂")));

    PRESET_RUN_TYPES.add(new PresetRunType(Arrays.asList(new String[] { "7" }),
      RunTypeCategory.IGNORED));

    PRESET_RUN_TYPES.add(new PresetRunType(Arrays.asList(new String[] { "13" }),
      RunTypeCategory.IGNORED));

    PRESET_RUN_TYPES.add(new PresetRunType(Arrays.asList(new String[] { "15" }),
      RunTypeCategory.INTERNAL_CALIBRATION));

    PRESET_RUN_TYPES.add(new PresetRunType(Arrays.asList(new String[] { "18" }),
      RunTypeCategory.IGNORED));

    PRESET_RUN_TYPES.add(new PresetRunType(Arrays.asList(new String[] { "18" }),
      RunTypeCategory.IGNORED));

    PRESET_RUN_TYPES.add(new PresetRunType(Arrays.asList(new String[] { "20" }),
      RunTypeCategory.IGNORED));

    PRESET_RUN_TYPES.add(new PresetRunType(Arrays.asList(new String[] { "21" }),
      RunTypeCategory.IGNORED));

    PRESET_RUN_TYPES.add(new PresetRunType(Arrays.asList(new String[] { "22" }),
      RunTypeCategory.IGNORED));
  }

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
  public static RunTypeAssignments buildRunTypes(int column,
    Collection<String> runTypes) {

    RunTypeAssignments result = new RunTypeAssignments(column);

    Map<PresetRunType, TreeSet<String>> runTypeGroups = new TreeMap<PresetRunType, TreeSet<String>>();

    for (String runType : runTypes) {

      PresetRunType preset = getRunTypePreset(runType);

      // If no preset is found, add the run type as Ignored. Otherwise save it
      // in the relevant group for later.
      if (null == preset) {
        result.put(runType,
          new RunTypeAssignment(runType, RunTypeCategory.IGNORED));
      } else {
        if (!runTypeGroups.containsKey(preset)) {
          runTypeGroups.put(preset,
            new TreeSet<String>(new AscendingLengthComparator()));
        }

        runTypeGroups.get(preset).add(runType);
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

  private static PresetRunType getRunTypePreset(String runType) {
    return PRESET_RUN_TYPES.stream()
      .filter(p -> p.containsRunType(runType.toLowerCase())).findFirst()
      .orElse(null);
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

  public static RunTypeAssignment getPresetAssignment(String runType,
    RunTypeAssignments existingAssignments) {

    RunTypeAssignment result = null;

    PresetRunType preset = getRunTypePreset(runType);

    if (null != preset) {
      for (String baseRunType : existingAssignments.keySet()) {
        if (preset.containsRunType(baseRunType)
          && !existingAssignments.get(baseRunType).isAlias()) {
          result = new RunTypeAssignment(runType, baseRunType);
          break;
        }
      }

      /*
       * String baseRunType = existingAssignments.keySet().stream() .filter(rt
       * -> preset.containsRunType(runType) &&
       * !existingAssignments.get(rt).isAlias()) .findAny().orElse(null);
       */
      /*
       * if (null != baseRunType) { result = new RunTypeAssignment(runType,
       * baseRunType); }
       */
    }

    return result;
  }

  public boolean allIgnored() {
    return values().stream()
      .allMatch(v -> v.getCategory().equals(RunTypeCategory.IGNORED));
  }
}

/**
 * Holds a set of preset run types and the {@link RunTypeCategory} that should
 * be assigned to them.
 *
 * <p>
 * A single run type String must be unique across all instances of this class.
 * If this is not the case then the behaviour of code using this class is
 * undefined.
 * </p>
 *
 * <p>
 * <b>Developer's note:</b> The {@link #equals(Object)} and
 * {@link #compareTo(PresetRunType)} methods compare the first entry in
 * {@link #runTypes} only, relying on the assumption that a given run type will
 * appear only once across all instances of the class.
 * </p>
 */
class PresetRunType implements Comparable<PresetRunType> {
  private TreeSet<String> runTypes;
  private RunTypeCategory category;

  protected PresetRunType(Collection<String> runTypes,
    RunTypeCategory category) {
    this.runTypes = new TreeSet<String>();
    this.runTypes.addAll(runTypes);
    this.category = category;
  }

  protected boolean containsRunType(String runType) {
    return runTypes.contains(runType.toLowerCase());
  }

  protected RunTypeCategory getCategory() {
    return category;
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
