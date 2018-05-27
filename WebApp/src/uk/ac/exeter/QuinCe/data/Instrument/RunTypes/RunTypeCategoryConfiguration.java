package uk.ac.exeter.QuinCe.data.Instrument.RunTypes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.ac.exeter.QuinCe.utils.FileUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Configuration for the run type categories used in the application
 * @author Steve Jones
 *
 */
public class RunTypeCategoryConfiguration {

  /**
   * The number of columns required in the configuration file
   */
  private static final int COL_COUNT = 4;

  /**
   * The column index for the category code
   */
  private static final int COL_CODE = 0;

  /**
   * The column index for the category name
   */
  private static final int COL_NAME = 1;

  /**
   * The column index for the category type
   */
  private static final int COL_TYPE = 2;

  /**
   * The column index for the minimum count
   */
  private static final int COL_MIN_COUNT = 3;

  /**
   *  The configured run type categories
   */
  List<RunTypeCategory> categories = null;

  /**
   * Loads the configuration from a config file
   * @param configFile The config file
   * @throws RunTypeCategoryException If the configuration cannot be accessed or is invalid
   */
  public RunTypeCategoryConfiguration(File configFile) throws RunTypeCategoryException {
    if (!FileUtils.canAccessFile(configFile)) {
      throw new RunTypeCategoryException("Cannot access config file '" + configFile.getAbsolutePath() + "'");
    }

    loadConfig(configFile);
  }

  /**
   * Load the configuration
   * @param configFile The config file
   * @throws RunTypeCategoryException If the configuration is invalid or cannot be read
   */
  private void loadConfig(File configFile) throws RunTypeCategoryException {
    categories = new ArrayList<RunTypeCategory>();
    BufferedReader reader = null;

    try {
      reader = new BufferedReader(new FileReader(configFile));
      String line = reader.readLine();
      int lineCount = 1;

      while (null != line) {
        if (!StringUtils.isComment(line)) {
          List<String> fields = StringUtils.trimList(Arrays.asList(line.split(",", -1)));

          if (fields.size() != COL_COUNT) {
            throw new RunTypeCategoryException(lineCount, "Incorrect number of columns");
          } else {
            if (!StringUtils.isInteger(fields.get(COL_TYPE))) {
              throw new RunTypeCategoryException(lineCount, "Value for Type column must be numeric");
            }

            if (!StringUtils.isInteger(fields.get(COL_MIN_COUNT))) {
              throw new RunTypeCategoryException(lineCount, "Value for MinCount column must be numeric");
            }

            String code = fields.get(COL_CODE);
            String name = fields.get(COL_NAME);
            int type = Integer.parseInt(fields.get(COL_TYPE));
            int minCount = Integer.parseInt(fields.get(COL_MIN_COUNT));

            categories.add(new RunTypeCategory(code, name, type, minCount));

          }
        }
        line = reader.readLine();
      }
    } catch (IOException e) {
      throw new RunTypeCategoryException("Error while reading config file", e);
    } finally {
      if (null != reader) {
        try {
          reader.close();
        } catch (IOException e) {
          // We tried.
        }
      }
    }
  }

  /**
   * Return the configured run type categories.
   * <p>
   *   The returned list is in the order of the configuration file.
   *   The special IGNORED category (@link {@link RunTypeCategory#IGNORED_CATEGORY})
   *   is included at the end of the list if requested
   * </p>
   * @param includeIgnored Specifies whether the IGNORED category should be included
   * @return The run type categories
   */
  public List<RunTypeCategory> getCategories(boolean includeIgnored, boolean includeAlias) {
    List<RunTypeCategory> result = new ArrayList<RunTypeCategory>(categories);
    result.add(RunTypeCategory.EXTERNAL_STANDARD_CATEGORY);

    if (includeIgnored) {
      result.add(RunTypeCategory.IGNORED_CATEGORY);
    }

    if (includeAlias) {
      result.add(RunTypeCategory.ALIAS_CATEGORY);
    }

    return result;
  }

  /**
   * Get a category using its code
   * @param categoryCode The code
   * @return The category
   * @throws NoSuchCategoryException If a category with the specified code cannot be found
   */
  public RunTypeCategory getCategory(String categoryCode) throws NoSuchCategoryException {

    RunTypeCategory result = null;

    if (categoryCode.equalsIgnoreCase(RunTypeCategory.IGNORED_CATEGORY.getCode())) {
      result = RunTypeCategory.IGNORED_CATEGORY;
    } else if (categoryCode.equalsIgnoreCase(RunTypeCategory.ALIAS_CATEGORY.getCode())) {
        result = RunTypeCategory.ALIAS_CATEGORY;
    } else if (categoryCode.equalsIgnoreCase(RunTypeCategory.EXTERNAL_STANDARD_CATEGORY.getCode())) {
      result = RunTypeCategory.EXTERNAL_STANDARD_CATEGORY;
    } else {
      for (RunTypeCategory category : categories) {
        if (category.getCode().equalsIgnoreCase(categoryCode)) {
          result = category;
          break;
        }
      }
    }
    return result;
  }
}
