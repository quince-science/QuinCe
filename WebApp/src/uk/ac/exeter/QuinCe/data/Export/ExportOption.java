package uk.ac.exeter.QuinCe.data.Export;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Class to hold details of a single export configuration
 * @author Steve Jones
 * @see ExportConfig
 */
public class ExportOption {

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
   * The variables to be exported as part of this export.
   * If any instrument does not contain a given variable it will be ignored
   */
  private List<Long> variables;

  /**
   * Indicates whether all sensors will be exported, or just those required
   * for the variables being exported
   */
  private boolean allSensors = true;

  /**
   * Indicates whether all intermediate calculation columns will be exported,
   * or just the final calculated value
   */
  private boolean includeCalculationColumns = false;

  /**
   * Build an ExportOption object from a JSON string
   * @param index The index of the configuration in the export configuration file
   * @param json The JSON string defining the export option
   * @throws ExportException If the export configuration is invalid
   */
  public ExportOption(ResourceManager resourceManager, int index, JSONObject json) throws ExportException {
    this.index = index;
    parseJson(resourceManager, json);
  }

  public int getIndex() {
    return index;
  }

  /**
   * Returns the name of the configuration
   * @return The configuration name
   */
  public String getName() {
    return name;
  }

  /**
   * The column separator to be used in the export file
   * @return The column separator
   */
  public String getSeparator() {
    return separator;
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

  /**
   * Export the option details from a JSON object
   * @param json The JSON
   * @throws ExportConfigurationException If the JSON does not contain valid values
   */
  private void parseJson(ResourceManager resourceManager, JSONObject json) throws ExportConfigurationException {

    // Export name
    if (!json.has("exportName")) {
      throw new ExportConfigurationException(json, "Missing exportName");
    } else {
      this.name = json.getString("exportName").trim();
      if (name.contains("/")) {
        throw new ExportConfigurationException(name, "Export option name cannot contain '/'");
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
      allVariables = InstrumentDB.getAllVariables(resourceManager.getDBDataSource());
    } catch (Exception e) {
      throw new ExportConfigurationException("N/A", "Unable to retrieve variable information");
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
            throw new ExportConfigurationException(name, "Invalid variable name '" + varName);
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
  }

  /**
   * Parse the {@code separator} entry in the export configuration JSON
   * @param separator the separator entry
   * @throws ExportConfigurationException If the separator is not recognised
   */
  private void parseSeparator(String separator) throws ExportConfigurationException {
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
      throw new ExportConfigurationException(name, "Separator must be 'tab' or 'comma'");
    }
    }
  }

  /**
   * Get the file extension for this export option,
   * based on the separator used
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
}
