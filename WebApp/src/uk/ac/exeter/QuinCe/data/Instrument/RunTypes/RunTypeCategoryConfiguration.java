package uk.ac.exeter.QuinCe.data.Instrument.RunTypes;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;

/**
 * Configuration for the run type categories used in the application
 * 
 * @author Steve Jones
 *
 */
public class RunTypeCategoryConfiguration {

  /**
   * The configured run type categories
   */
  TreeSet<RunTypeCategory> categories = null;

  /**
   * Loads the configuration from a config file
   * 
   * @param configFile
   *          The config file
   * @throws RunTypeCategoryException
   *           If the configuration cannot be accessed or is invalid
   */
  public RunTypeCategoryConfiguration(Connection conn)
    throws RunTypeCategoryException {
    init(conn);
  }

  private void init(Connection conn) throws RunTypeCategoryException {

    try {
      Map<Long, String> variables = InstrumentDB.getAllVariables(conn);
      categories = new TreeSet<RunTypeCategory>();

      for (Map.Entry<Long, String> entry : variables.entrySet()) {
        categories.add(new RunTypeCategory(entry.getKey(), entry.getValue()));
      }
    } catch (Exception e) {
      throw new RunTypeCategoryException(
        "Error initialising run type categories", e);
    }
  }

  /**
   * Return the configured run type categories.
   * <p>
   * The returned list is in the order of the configuration file. The special
   * IGNORED category (@link {@link RunTypeCategory#IGNORED}) is included at the
   * end of the list if requested
   * </p>
   * 
   * @param includeIgnored
   *          Specifies whether the IGNORED category should be included
   * @return The run type categories
   */
  public List<RunTypeCategory> getCategories(boolean includeIgnored,
    boolean includeAlias) {
    List<RunTypeCategory> result = new ArrayList<RunTypeCategory>(categories);
    result.add(RunTypeCategory.INTERNAL_CALIBRATION);

    if (includeIgnored) {
      result.add(RunTypeCategory.IGNORED);
    }

    if (includeAlias) {
      result.add(RunTypeCategory.ALIAS);
    }

    return result;
  }

  /**
   * Get a category using its type
   * 
   * @param type
   *          The type
   * @return The category
   * @throws NoSuchCategoryException
   *           If a category with the specified type cannot be found
   */
  public RunTypeCategory getCategory(long type) throws NoSuchCategoryException {

    RunTypeCategory result = null;

    if (type == RunTypeCategory.IGNORED.getType()) {
      result = RunTypeCategory.IGNORED;
    } else if (type == RunTypeCategory.ALIAS.getType()) {
      result = RunTypeCategory.ALIAS;
    } else if (type == RunTypeCategory.INTERNAL_CALIBRATION.getType()) {
      result = RunTypeCategory.INTERNAL_CALIBRATION;
    } else {
      for (RunTypeCategory category : categories) {
        if (type == category.getType()) {
          result = category;
          break;
        }
      }
    }
    return result;
  }
}
