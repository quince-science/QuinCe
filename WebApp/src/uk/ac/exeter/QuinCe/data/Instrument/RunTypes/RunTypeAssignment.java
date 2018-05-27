package uk.ac.exeter.QuinCe.data.Instrument.RunTypes;

/**
 * Stores details of the assignment of a given run type
 * to a run type category. Handles aliases to other run
 * types.
 *
 * The category can be empty to show that the run type
 * is not assigned to anything.
 *
 * The natural ordering of this class is the name of
 * the run type.
 *
 * @author zuj007
 *
 */
public class RunTypeAssignment implements Comparable<RunTypeAssignment> {

  private String runType;

  private RunTypeCategory category;

  private boolean alias = false;

  private String aliasTo = null;

  /**
   * Create an empty assignment for a run type
   * @param runType The run type
   */
  public RunTypeAssignment(String runType) {
    this.runType = runType;
    this.category = null;
  }

  /**
   * Construct a standard run type assignment to a category
   * @param runType The run type
   * @param category The category
   */
  public RunTypeAssignment(String runType, RunTypeCategory category) {
    this.runType = runType;
    this.category = category;
  }

  /**
   * Create an alias from one run type to another
   * @param runType The run type
   * @param aliasTo The run type to which is it aliased
   */
  public RunTypeAssignment(String runType, String aliasTo) {
    this.runType = runType;
    this.category = null;
    this.alias = true;
    this.aliasTo = aliasTo;
  }

  /**
   * Determine whether or not this run type is correctly assigned,
   * either to a category or as an alias
   * @return {@code true} if the run type is assigned; {@code false} if it is not
   */
  public boolean isAssigned() {
    return (!alias && null == category);
  }

  /**
   * Get the run type that this assignment is for
   * @return The run type
   */
  public String getRunType() {
    return runType;
  }

  /**
   * Get the category to which this run type is assigned.
   * If the run type is an alias, the category will be {@code null}.
   * @return The assigned category
   */
  public RunTypeCategory getCategory() {
    return category;
  }

  /**
   * Determine whether or not this run type is an alias
   * @return {@code true} if the run type is an alias; {@code false} if it is not
   */
  public boolean isAlias() {
    return alias;
  }

  /**
   * Get the run type to which this run type is aliased.
   * Returns {@code null} if this is not an alias
   * @return The alias
   */
  public String getAliasTo() {
    return aliasTo;
  }

  @Override
  public int compareTo(RunTypeAssignment o) {
    return runType.compareTo(o.runType);
  }

  @Override
  public String toString() {
    String result;

    if (alias) {
      result = runType + " aliased to " + aliasTo;
    } else if (null == category) {
      result = runType + " not assigned";
    } else {
      result = runType + " assigned to " + category.getName();
    }

    return result;
  }

  @Override
  public boolean equals(Object o) {
    boolean result;

    if (!(o instanceof RunTypeAssignment)) {
      result = false;
    } else {
      result = runType.equals(((RunTypeAssignment) o).runType);
    }

    return result;
  }

  @Override
  public int hashCode() {
    return runType.hashCode();
  }
}
