package uk.ac.exeter.QuinCe.data.Export;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.web.datasets.export.ExportData;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Class to hold details of a single export configuration
 *
 * @author Steve Jones
 * @see ExportConfig
 */
public class ExportOption {

  private static final String EXPORT_DATA_PACKAGE = "uk.ac.exeter.QuinCe.web.datasets.export.";

  private static final String DEFAULT_EXPORT_DATA_CLASS = EXPORT_DATA_PACKAGE
    + "ExportData";

  private int index;

  /**
   * The name of this export configuration
   */
  private String name;

  /**
   * The separator to be used between columns in the output file
   */
  private String separator;

  /**
   * The string to put in files where a value is missing
   */
  private String missingValue = "NaN";

  /**
   * The variables to be exported as part of this export. If any instrument does
   * not contain a given variable it will be ignored
   */
  private List<Long> variables;

  /**
   * The export data class to use for this export. Used for custom
   * post-processors
   */
  private Class<? extends ExportData> dataClass;

  /**
   * Indicates whether all sensors will be exported, or just those required for
   * the variables being exported
   */
  private boolean allSensors = true;

  /**
   * Indicates whether all intermediate calculation columns will be exported, or
   * just the final calculated value
   */
  private boolean includeCalculationColumns = false;

  /**
   * Indicates whether to use column codes instead of names
   */
  private boolean useColumnCodes = false;

  /**
   * Indicates whether units should be included in column headings
   */
  private boolean includeUnits = true;

  /**
   * Indicates whether QC comments should be included in the output
   */
  private boolean includeQCComments = true;

  /**
   * The header for the timestamp column
   */
  private String timestampHeader = "Date/Time";

  /**
   * QC Flag header suffix
   */
  private String qcFlagSuffix = " QC Flag";

  /**
   * QC Comment header suffix
   */
  private String qcCommentSuffix = " QC Comment";

  /**
   * Custom column header replacements
   */
  private Map<String, String> replacementColumnHeaders = null;

  /**
   * Build an ExportOption object from a JSON string
   *
   * @param index
   *          The index of the configuration in the export configuration file
   * @param json
   *          The JSON string defining the export option
   * @throws ExportException
   *           If the export configuration is invalid
   */
  public ExportOption(ResourceManager resourceManager, int index,
    JSONObject json) throws ExportException {
    this.index = index;
    parseJson(resourceManager, json);
  }

  public int getIndex() {
    return index;
  }

  /**
   * Returns the name of the configuration
   *
   * @return The configuration name
   */
  public String getName() {
    return name;
  }

  /**
   * The column separator to be used in the export file
   *
   * @return The column separator
   */
  public String getSeparator() {
    return separator;
  }

  public String getMissingValue() {
    return missingValue;
  }

  public List<Long> getVariables() {
    return variables;
  }

  public boolean includeAllSensors() {
    return allSensors;
  }

  public boolean includeCalculationColumns() {
    return includeCalculationColumns;
  }

  public boolean useColumnCodes() {
    return useColumnCodes;
  }

  public boolean includeUnits() {
    return includeUnits;
  }

  /**
   * Export the option details from a JSON object
   *
   * @param json
   *          The JSON
   * @throws ExportConfigurationException
   *           If the JSON does not contain valid values
   */
  @SuppressWarnings("unchecked")
  private void parseJson(ResourceManager resourceManager, JSONObject json)
    throws ExportConfigurationException {

    // Export name
    if (!json.has("exportName")) {
      throw new ExportConfigurationException(json, "Missing exportName");
    } else {
      this.name = json.getString("exportName").trim();
      if (name.contains("/")) {
        throw new ExportConfigurationException(name,
          "Export option name cannot contain '/'");
      }
    }

    // Separator
    if (!json.has("separator")) {
      throw new ExportConfigurationException(name, "Missing separator");
    } else {
      parseSeparator(json.getString("separator"));
    }

    // Variables
    Map<Long, String> allVariables;
    try {
      allVariables = InstrumentDB
        .getAllVariables(resourceManager.getDBDataSource());
    } catch (Exception e) {
      throw new ExportConfigurationException("N/A",
        "Unable to retrieve variable information");
    }

    // If the element isn't provided, assume all variables
    if (!json.has("variables")) {
      variables = new ArrayList<Long>(allVariables.keySet());
    } else {
      JSONArray varArray = json.getJSONArray("variables");

      // If we have an empty array, use all variables
      if (varArray.length() == 0) {
        variables = new ArrayList<Long>(allVariables.keySet());
      } else {
        variables = new ArrayList<Long>(varArray.length());

        Iterator<Object> iter = json.getJSONArray("variables").iterator();
        while (iter.hasNext()) {
          String varName = (String) iter.next();

          boolean variableFound = false;
          for (Map.Entry<Long, String> entry : allVariables.entrySet()) {
            if (varName.equals(entry.getValue())) {
              variables.add(entry.getKey());
              variableFound = true;
              break;
            }
          }

          if (!variableFound) {
            throw new ExportConfigurationException(name,
              "Invalid variable name '" + varName);
          }
        }
      }
    }

    if (json.has("missingValue")) {
      missingValue = json.getString("missingValue");
    }

    if (json.has("allSensors")) {
      allSensors = json.getBoolean("allSensors");
    }

    if (json.has("includeCalculationColumns")) {
      includeCalculationColumns = json.getBoolean("includeCalculationColumns");
    }

    if (json.has("useColumnCodes")) {
      useColumnCodes = json.getBoolean("useColumnCodes");
    }

    if (json.has("includeUnits")) {
      includeUnits = json.getBoolean("includeUnits");
    }

    if (json.has("includeQCComments")) {
      includeQCComments = json.getBoolean("includeQCComments");
    }

    if (json.has("timestampHeader")) {
      timestampHeader = json.getString("timestampHeader");
    }

    if (json.has("qcFlagSuffix")) {
      qcFlagSuffix = json.getString("qcFlagSuffix");
    }

    if (json.has("qcCommentSuffix")) {
      qcCommentSuffix = json.getString("qcCommentSuffix");
    }

    // Replacement column headers. Forces useColumnCodes and does
    // not include units
    if (json.has("replaceColumnHeaders")) {
      useColumnCodes = true;
      includeUnits = false;
      replacementColumnHeaders = new HashMap<String, String>();

      JSONObject replacementHeaderJson = json
        .getJSONObject("replaceColumnHeaders");
      replacementHeaderJson.keySet().stream().forEach(k -> {
        replacementColumnHeaders.put(k, replacementHeaderJson.getString(k));

      });
    }

    // ExportData class. All classes are in the
    // uk.ac.exeter.QuinCe.web.datasets.export package.
    String className = EXPORT_DATA_PACKAGE;

    if (json.has("exportDataClass")) {
      className = className + json.getString("exportDataClass");
      String checkMessage = checkExportDataClass(className);
      if (null != checkMessage) {
        throw new ExportConfigurationException(name, checkMessage);
      }
    } else {
      className = DEFAULT_EXPORT_DATA_CLASS;
    }

    try {
      dataClass = (Class<? extends ExportData>) Class.forName(className);
    } catch (ClassNotFoundException e) {
      // Since we've already checked the class, this shouldn't really happen
      throw new ExportConfigurationException(name, e);
    }
  }

  @SuppressWarnings("unchecked")
  private String checkExportDataClass(String className) {

    String errorMessage = null;

    try {
      Class<? extends Object> testClass = Class.forName(className);
      Class<ExportData> rootClass = (Class<ExportData>) Class
        .forName(DEFAULT_EXPORT_DATA_CLASS);

      if (!rootClass.isAssignableFrom(testClass)) {
        errorMessage = "Specified export class does not extend "
          + DEFAULT_EXPORT_DATA_CLASS;
      } else {

        try {
          // Check that the correct constructor is available
          testClass.getConstructor(DataSource.class, Instrument.class,
            DataSet.class, ExportOption.class);
        } catch (NoSuchMethodException e) {
          errorMessage = "No valid constructor found in " + className;
        }

      }

    } catch (ClassNotFoundException e) {
      errorMessage = "Cannot find class " + className;
    }

    return errorMessage;
  }

  /**
   * Parse the {@code separator} entry in the export configuration JSON
   *
   * @param separator
   *          the separator entry
   * @throws ExportConfigurationException
   *           If the separator is not recognised
   */
  private void parseSeparator(String separator)
    throws ExportConfigurationException {
    switch (separator.toLowerCase()) {
    case "comma": {
      this.separator = ",";
      break;
    }
    case "tab": {
      this.separator = "\t";
      break;
    }
    default: {
      throw new ExportConfigurationException(name,
        "Separator must be 'tab' or 'comma'");
    }
    }
  }

  /**
   * Get the file extension for this export option, based on the separator used
   *
   * @return The file extension
   */
  public String getFileExtension() {
    String result;

    switch (separator) {
    case ",": {
      result = ".csv";
      break;
    }
    case "\t": {
      result = ".tsv";
      break;
    }
    default: {
      result = ".txt";
    }
    }

    return result;
  }

  public boolean includeQCComments() {
    return includeQCComments;
  }

  public String getQcFlagSuffix() {
    return qcFlagSuffix;
  }

  public String getQcCommentSuffix() {
    return qcCommentSuffix;
  }

  public String getTimestampHeader() {
    return timestampHeader;
  }

  public String getReplacementHeader(String code) {
    String result = code;

    if (null != replacementColumnHeaders
      && replacementColumnHeaders.containsKey(code)) {

      result = replacementColumnHeaders.get(code);

    }

    return result;
  }

  /**
   * Get the class to be used for building the export data. This is typically
   * used for customising data prost-processors before the final export.
   *
   * @return The export data class
   */
  public Class<? extends ExportData> getExportDataClass() {
    return dataClass;
  }

  /**
   * Create the ExportData object to be used for this Export Option
   *
   * @param dataSource
   *          A data source
   * @param instrument
   *          The instrument that the exported data set belongs to
   * @param dataset
   *          The dataset that will be exported
   * @return The ExportData object
   */
  public ExportData makeExportData(DataSource dataSource, Instrument instrument,
    DataSet dataset) throws ExportConfigurationException {

    try {
      Constructor<?> constructor = dataClass.getConstructor(DataSource.class,
        Instrument.class, DataSet.class, ExportOption.class);

      return (ExportData) constructor.newInstance(dataSource, instrument,
        dataset, this);
    } catch (Exception e) {
      throw new ExportConfigurationException(name,
        "Error creating ExportData object", e);
    }

  }
}
