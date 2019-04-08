package uk.ac.exeter.QuinCe.data.Instrument.RunTypes;

import java.util.List;

import uk.ac.exeter.QuinCe.web.system.ResourceManager;

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

  /**
   * The run type name
   */
  private String runName;

  /**
   * The category to which the run type is assigned.
   * If the run type is an alias, this will be {@code null}.
   */
  private RunTypeCategory category;

  /**
   * Indicates whether or not this run type
   * is an alias to another run type
   */
  private boolean alias = false;

  /**
   * The run type to which this run type is
   * aliased. {@code null} if it is not an alias
   */
  private String aliasTo = null;

  /**
   * Create an empty assignment for a run type
   * @param runType The run type
   */
  public RunTypeAssignment(String runName) {
    this.runName = runName.toLowerCase();
    this.category = null;
  }

  /**
   * Construct a standard run type assignment to a category
   * @param runType The run type
   * @param category The category
   */
  public RunTypeAssignment(String runName, RunTypeCategory category) {
    this.runName = runName.toLowerCase();
    this.category = category;
  }

  /**
   * Create an alias from one run type to another
   * @param runType The run type
   * @param aliasTo The run type to which is it aliased
   */
  public RunTypeAssignment(String runName, String aliasTo) {
    this.runName = runName.toLowerCase();
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
   * Get the run name that this assignment is for
   * @return The run name
   */
  public String getRunName() {
    return runName;
  }

  /**
   * Get the category to which this run type is assigned.
   * If the run type is an alias, returned category will be
   * {@code null}.
   *
   * If you want to get the aliased category of a run type,
   * use {@link RunTypeAssignments#getRunTypeCategory(String)}
   *
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
   * Set the flag stating whether or not this run type is an alias
   * @param alias The alias flag
   */
  public void setAlias(boolean alias) {
    this.alias = alias;
  }

  /**
   * Get the run type to which this run type is aliased.
   * Returns {@code null} if this is not an alias
   * @return The alias
   */
  public String getAliasTo() {
    return aliasTo;
  }

  /**
   * Set the run type to which this run type is aliased.
   * @param aliasTo The alias
   */
  public void setAliasTo(String aliasTo) {
    this.aliasTo = aliasTo;
  }

  @Override
  public int compareTo(RunTypeAssignment o) {
    return runName.compareTo(o.runName);
  }

  @Override
  public String toString() {
    String result;

    if (alias) {
      result = runName + " aliased to " + aliasTo;
    } else if (null == category) {
      result = runName + " not assigned";
    } else {
      result = runName + " assigned to " + category.getDescription();
    }

    return result;
  }

  @Override
  public boolean equals(Object o) {
    boolean result;

    if (!(o instanceof RunTypeAssignment)) {
      result = false;
    } else {
      result = runName.equals(((RunTypeAssignment) o).runName);
    }

    return result;
  }

  @Override
  public int hashCode() {
    return runName.hashCode();
  }

  /**
   * Get a human readable description of this assignment
   * @return The assignment description
   */
  public String getAssignmentText() {
    StringBuilder result = new StringBuilder();

    if (alias) {
      result.append("Alias to ");
      result.append(aliasTo);
    } else {
      result.append(category.getDescription());
    }

    return result.toString();
  }

  /**
   * Get the code for the assigned Run Type category.
   * If no category is assigned, returns {@code null}
   * @return The assigned category
   */
  public long getCategoryCode() {
    long result = RunTypeCategory.NOT_ASSIGNED;

    if (null != category) {
      if (isAlias()) {
        result = RunTypeCategory.ALIAS.getType();
      } else {
        result = category.getType();
      }
    }

    return result;
  }

  /**
   * Set a run type
   * @param code
   * @throws RunTypeCategoryException If the code is not recognised
   */
  public void setCategoryCode(long type) throws RunTypeCategoryException {
    boolean categoryAssigned = false;

    if (type == RunTypeCategory.ALIAS.getType()) {
      category = null;
      alias = true;
      categoryAssigned = true;
    } else {
      try {
        List<RunTypeCategory> categories = ResourceManager.getInstance().getRunTypeCategoryConfiguration().getCategories(true, true);
        for (RunTypeCategory checkCategory : categories) {
          if (type == checkCategory.getType()) {
            category = checkCategory;
            categoryAssigned = true;
            break;
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        throw e;
      }
    }

    if (!categoryAssigned) {
      RunTypeCategoryException e = new RunTypeCategoryException("Unrecognised run type '" + type + "'");
      e.printStackTrace();
      throw e;
    }
  }
}
