package uk.ac.exeter.QuinCe.data.Export;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.primefaces.json.JSONArray;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.web.datasets.export.ExportData;

/**
 * <p>
 * Holds details of the various file export options configured for the
 * application.
 * </p>
 *
 * <p>
 * The export options are provided in a JSON file. Each JSON entry represents
 * one export option, and contains the following items:
 * </p>
 * <table>
 * <caption>ExportOption JSON items</caption>
 * <tr>
 * <th>Item</th>
 * <th>Purpose</th>
 * </tr>
 * <tr>
 * <td style="vertical-align: top">{@code exportName}</td>
 * <td>The name of the export option. Required.</td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top">{@code separator}</td>
 * <td>The column separator to use. Required.</td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top">{@code variables}</td>
 * <td>The variables to be included in the export. If not specified all
 * variables are included.</td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top">{@code visible}</td>
 * <td>Indicates whether or not this export option will be visible in the web
 * application. Default is {@code true}.</td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top">{@code missingValue}</td>
 * <td>The value to use for missing values. Default is {@code NaN}.</td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top">{@code allSensors}</td>
 * <td>If {@code true}, all sensor values are included in the export file. If
 * {@code false}, only those sensor values used in the exported variables are
 * included. Default is {@code true}.</td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top">{@code includeCalculationColumns}</td>
 * <td>If {@code true}, intermediate values from the data reduction are included
 * in the output file. If {@code false}, only the final result is included.
 * Default is {@code false}.</td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top">{@code headerMode}</td>
 * <td>Specifies the type of column header in the file. One of
 * {@link ExportOption#HEADER_MODE_LONG},
 * {@link ExportOption#HEADER_MODE_SHORT}, or
 * {@link ExportOption#HEADER_MODE_CODE}. Default is
 * {@link ExportOption#HEADER_MODE_LONG}.</td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top">{@code includeUnits}</td>
 * <td>Indicates whether or not units will be included in the column headers.
 * Default is {@code true}.</td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top">{@code includeQCComments}</td>
 * <td>Indicates whether or QC comments will be included in the export file.
 * Otherwise only the QC Flag is included. Default is {@code true}.</td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top">{@code timestampHeader}</td>
 * <td>The column header to use for the timestamp field. Default is
 * {@code Date/Time}.</td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top">{@code qcFlagSuffix}</td>
 * <td>The text added to the column header for QC flags. Default is
 * {@code " QC Flag"}.</td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top">{@code qcCommentSuffix}</td>
 * <td>The text added to the column header for QC comments. Default is
 * {@code " QC Comment"}.</td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top">{@code replaceColumnHeaders}</td>
 * <td>Allows column header names to be overridden for specific purposes.
 * Written as a dict of {@code "columnName": "replacement"} pairs.
 * {@code columnName} must be the {@link ExportOption#HEADER_MODE_CODE} version
 * of the column. {@code includeUnits} is not honoured when a column is
 * replaced: if units are required, they must be included here.
 * {@code qcFlagSuffix} and {@code qcCommentSuffix} are still applied to related
 * QC Flag/Comment columns.</td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top">{@code exportDataClass}</td>
 * <td>Optionally specifies a special {@link ExportData} class used during
 * export. The specified class must extend {@link ExportData} and be in the
 * {@link uk.ac.exeter.QuinCe.web.datasets.export} package. Only include the
 * {@link Class#getSimpleName()} of the class (i.e. without the package
 * prefix).</td>
 * </tr>
 * </table>
 *
 * 
 * <p>
 * The export options are held in a list, kept in the same order as the options
 * appear in the configuration file.
 * 
 * </p>
 *
 * 
 * <p>
 * This class exists as a singleton that must be initialised before it is used
 * by calling the {@link #init(Connection, SensorsConfiguration, String)}
 * method.
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
   * Loads and parses the export configuration file.
   * </p>
   *
   * <p>
   * This is an internal constructor that is called by the {@link #init(String)}
   * method.
   * </p>
   *
   * @throws ExportException
   *           If the configuration file cannot be loaded or parsed
   */
  private ExportConfig(Connection conn, SensorsConfiguration sensorConfig)
    throws ExportException {
    if (configFilename == null) {
      throw new ExportException(
        "ExportConfig filename has not been set - must run init first");
    }

    options = new ArrayList<ExportOption>();
    try {
      readFile(conn, sensorConfig);
    } catch (FileNotFoundException e) {
      throw new ExportException(
        "Could not find configuration file '" + configFilename + "'");
    }
  }

  /**
   * Initialises the {@code ExportConfig} singleton. This must be called before
   * the class can be used.
   *
   * @param configFile
   *          The full path to the export configuration file
   * @throws ExportException
   *           If the configuration file cannot be loaded or parsed
   */
  public static void init(Connection conn, SensorsConfiguration sensorConfig,
    String configFile) throws ExportException {
    configFilename = configFile;
    instance = new ExportConfig(conn, sensorConfig);
  }

  /**
   * Returns the {@code ExportConfig} singleton instance
   *
   * @return The {@code ExportConfig} singleton instance
   * @throws ExportException
   *           If the class has not been initialised
   */
  public static ExportConfig getInstance() throws ExportException {

    if (null == instance) {
      throw new ExportException("Export options have not been configured");
    }

    return instance;
  }

  /**
   * Read and parse the export options configuration file
   *
   * @throws ExportException
   *           If the configuration file is invalid
   * @throws FileNotFoundException
   *           If the file specified in the {@link #init(String)} call does not
   *           exist
   */
  private void readFile(Connection conn, SensorsConfiguration sensorConfig)
    throws ExportException, FileNotFoundException {
    try {
      String fileContent = new String(
        Files.readAllBytes(Paths.get(configFilename)), StandardCharsets.UTF_8);
      JSONArray jsonEntries = new JSONArray(fileContent);
      for (int i = 0; i < jsonEntries.length(); i++) {
        options.add(new ExportOption(conn, sensorConfig, i,
          jsonEntries.getJSONObject(i)));
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
   *
   * @return The list of export options
   */
  public List<ExportOption> getOptions() {
    return options;
  }

  /**
   * <p>
   * Returns a specified export configuration referenced by its position in the
   * configuration file (zero-based).
   * </p>
   *
   * <p>
   * This method will throw an {@link ArrayIndexOutOfBoundsException} if the
   * specified index is outside the range of the list of export options.
   * </p>
   *
   * @param index
   *          The index of the desired configuration.
   * @return The export configuration
   */
  public ExportOption getOption(int index) {
    return options.get(index);
  }
}
