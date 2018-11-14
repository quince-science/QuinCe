package uk.ac.exeter.QuinCe.data.Export;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.primefaces.json.JSONArray;

import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * <p>
 *   Holds details of the various file export options configured for the application.
 * </p>
 *
 * <p>
 *   The export options are provided in a JSON file. Each JSON entry represents one export option,
 *   and contains the following items:
 * </p>
 * <ul>
 *   <li>
 *     <b>exportName</b> - The display name of this export option.
 *   </li>
 *   <li>
 *     <b>separator</b> - The column separator in the exported file. This must be a string, either
 *      'comma' or 'tab'.
 *   </li>
 *   <li>
 *     <b>flags</b> - The flag values to be included in the export, as numeric values.
 *   </li>
 *   <li>
 *     <b>sensors</b> - The names of the sensor columns to be included in the output
 *   </li>
 *   <li>
 *     <b>sensors_titles</b> - The column headers to use for the sensors
 *   </li>
 *   <li>
 *     <b>[Calculation Name]</b> - For each available calculation path, Any calculated value
 *     can be included.
 *   </li>
 *   <li>
 *     <b>[Calculation Name]_titles</b> - The headers to use for the calculation columns
 *   </li>
 * </ul>
 *
 * <p>
 *   The export options are held in a list, kept in the same order as the options appear in the configuration file.
 * </p>
 *
 * <p>
 *   This class exists as a singleton that must be initialised before it is used by calling the {@link #init(String)} method.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class ExportConfig {

  /**
   * The concrete instance of the {@code ExportConfig} singleton
   */
  private static ExportConfig instance = null;

  /**
   * The set of export options
   */
  private List<ExportOption> options = null;

  /**
   * The full path of the export configuration file
   */
  private static String configFilename = null;

  /**
   * <p>
   *   Loads and parses the export configuration file.
   * </p>
   *
   * <p>
   *   This is an internal constructor that is called by the {@link #init(String)} method.
   * </p>
   *
   * @throws ExportException If the configuration file cannot be loaded or parsed
   */
  private ExportConfig(ResourceManager resourceManager) throws ExportException {
    if (configFilename == null) {
      throw new ExportException("ExportConfig filename has not been set - must run init first");
    }

    options = new ArrayList<ExportOption>();
    try {
      readFile(resourceManager);
    } catch (FileNotFoundException e) {
      throw new ExportException("Could not find configuration file '" + configFilename + "'");
    }
  }

  /**
   * Initialises the {@code ExportConfig} singleton. This must be called before the class can be used.
   * @param configFile The full path to the export configuration file
   * @throws ExportException If the configuration file cannot be loaded or parsed
   */
  public static void init(ResourceManager resourceManager, String configFile) throws ExportException {
    configFilename = configFile;
    instance = new ExportConfig(resourceManager);
  }

  /**
   * Returns the {@code ExportConfig} singleton instance
   * @return The {@code ExportConfig} singleton instance
   * @throws ExportException If the class has not been initialised
   */
  public static ExportConfig getInstance() throws ExportException {

    if (null == instance) {
      throw new ExportException("Export options have not been configured");
    }

    return instance;
  }

  /**
   * Read and parse the export options configuration file
   * @throws ExportException If the configuration file is invalid
   * @throws FileNotFoundException If the file specified in the {@link #init(String)} call does not exist
   */
  private void readFile(ResourceManager resourceManager) throws ExportException, FileNotFoundException {
    try {
      String fileContent = new String(Files.readAllBytes(Paths.get(configFilename)), StandardCharsets.UTF_8);
      JSONArray jsonEntries = new JSONArray(fileContent);
      for (int i = 0; i < jsonEntries.length(); i++) {
        options.add(new ExportOption(resourceManager, i, jsonEntries.getJSONObject(i)));
      }
    } catch (Exception e) {
      if (e instanceof ExportException) {
        throw (ExportException) e;
      } else {
        throw new ExportException("Error initialising export options", e);
      }
    }
  }

  /**
   * Returns the list of all configured export options
   * @return The list of export options
   */
  public List<ExportOption> getOptions() {
    return options;
  }

  /**
   * <p>
   *   Returns a specified export configuration referenced by its position in the configuration file (zero-based).
   * </p>
   *
   * <p>
   *   This method will throw an {@link ArrayIndexOutOfBoundsException} if the specified index is outside the range
   *   of the list of export options.
   * </p>
   *
   * @param index The index of the desired configuration.
   * @return The export configuration
   */
  public ExportOption getOption(int index) {
    return options.get(index);
  }
}
