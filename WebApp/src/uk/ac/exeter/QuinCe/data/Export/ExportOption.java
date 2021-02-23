package uk.ac.exeter.QuinCe.data.Export;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.math.NumberUtils;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.datasets.export.ExportData;

/**
 * Class to hold details of a single export configuration
 *
 * @author Steve Jones
 * @see ExportConfig
 */
public class ExportOption {

  public static final int HEADER_MODE_SHORT = 0;

  public static final int HEADER_MODE_LONG = 1;

  public static final int HEADER_MODE_CODE = 2;

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
   * The string to use for QC flags of missing values
   */
  private String missingQcFlag = "";

  /**
   * The variables to be exported as part of this export. If any instrument does
   * not contain a given variable it will be ignored
   */
  private List<Variable> variables;

  /**
   * The export data class to use for this export. Used for custom
   * post-processors
   */
  private Class<? extends ExportData> dataClass;

  /**
   * Indicates whether raw sensors will be exported
   */
  private boolean includeRawSensors = false;

  /**
   * Indicates whether all intermediate calculation columns will be exported, or
   * just the final calculated value
   */
  private boolean includeCalculationColumns = false;

  /**
   * Indicates whether to use column codes instead of names
   */
  private int headerMode = HEADER_MODE_LONG;

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
   * Indicates whether export option should be visible in webapp
   */
  private boolean visible = true;

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
  public ExportOption(Connection conn, SensorsConfiguration sensorConfig,
    int index, JSONObject json) throws ExportException {
    this.index = index;
    parseJson(conn, sensorConfig, json);
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

  public String getMissingQcFlag() {
    return missingQcFlag;
  }

  public List<Variable> getVariables() {
    return variables;
  }

  public boolean includeRawSensors() {
    return includeRawSensors;
  }

  public boolean includeCalculationColumns() {
    return includeCalculationColumns;
  }

  public int getHeaderMode() {
    return headerMode;
  }

  public boolean includeUnits() {
    return includeUnits;
  }

  public boolean getVisible() {
    return visible;
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
  private void parseJson(Connection conn, SensorsConfiguration sensorConfig,
    JSONObject json) throws ExportConfigurationException {

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
    List<Variable> allVariables;
    try {
      allVariables = InstrumentDB.getAllVariables(conn, sensorConfig);
    } catch (Exception e) {
      throw new ExportConfigurationException("N/A",
        "Unable to retrieve variable information");
    }

    // If the element isn't provided, assume all variables
    if (!json.has("variables")) {
      variables = allVariables;
    } else {
      JSONArray varArray = json.getJSONArray("variables");

      // If we have an empty array, use all variables
      if (varArray.length() == 0) {
        variables = allVariables;
      } else {
        variables = new ArrayList<Variable>(varArray.length());

        Iterator<Object> iter = json.getJSONArray("variables").iterator();
        while (iter.hasNext()) {
          String varName = (String) iter.next();

          boolean variableFound = false;
          for (Variable variable : allVariables) {
            if (varName.equals(variable.getName())) {
              variables.add(variable);
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

    if (json.has("visible")) {
      visible = json.getBoolean("visible");
    } else {
      visible = true;
    }

    if (json.has("missingValue")) {
      missingValue = json.getString("missingValue");
    }

    if (json.has("missingQCFlag")) {
      missingQcFlag = json.getString("missingQCFlag");
    }

    if (json.has("includeRawSensors")) {
      includeRawSensors = json.getBoolean("includeRawSensors");
    }

    if (json.has("includeCalculationColumns")) {
      includeCalculationColumns = json.getBoolean("includeCalculationColumns");
    }

    if (json.has("headerMode")) {
      headerMode = parseHeaderMode(json, json.getString("headerMode"));
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

    // Replacement column headers.
    if (json.has("replaceColumnHeaders")) {
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
            DataSet.class);
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
    String result = null;
    if (null != replacementColumnHeaders
      && null != replacementColumnHeaders.get(code)) {
      result = replacementColumnHeaders.get(code);
    }
    return result;
  }

  /**
   * Get the class to be used for building the export data. This is typically
   * used for customizing data post-processors before the final export.
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
        Instrument.class, DataSet.class);

      return (ExportData) constructor.newInstance(dataSource, instrument,
        dataset);
    } catch (Exception e) {
      throw new ExportConfigurationException(name,
        "Error creating ExportData object", e);
    }
  }

  private int parseHeaderMode(JSONObject json, String mode)
    throws ExportConfigurationException {

    int result;

    switch (mode.toLowerCase()) {
    case "short": {
      result = HEADER_MODE_SHORT;
      break;
    }
    case "long": {
      result = HEADER_MODE_LONG;
      break;
    }
    case "code": {
      result = HEADER_MODE_CODE;
      break;
    }
    default: {
      throw new ExportConfigurationException(json,
        "Unrecognised header mode '" + mode + "'");
    }
    }

    return result;
  }

  /**
   * Format a field value to fit the export format.
   *
   * <p>
   * Newlines are replaced with semicolons. Instances of the separator are
   * replaced with spaces.
   * </p>
   *
   * @param fieldValue
   * @return
   */
  public String format(String fieldValue) {
    String result = null;

    if (null == fieldValue) {
      result = fieldValue;
    } else if (NumberUtils.isCreatable(fieldValue)) {
      result = StringUtils.formatNumber(fieldValue);
    } else {
      String newlinesRemoved = fieldValue.replaceAll("[\\r\\n]", " ");
      String separatorsRemoved = newlinesRemoved.replaceAll(separator, " ");
      return separatorsRemoved;
    }

    return result;
  }
}
