package uk.ac.exeter.QuinCe.data.Export;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationDB;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationDBFactory;
import uk.ac.exeter.QuinCe.data.Calculation.CalculatorException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorConfigurationException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Class to hold details of a single export configuration
 * @author Steve Jones
 * @see ExportConfig
 */
public class ExportOption {

  private static List<String> FIXED_KEYS;

  /**
   * The index of this configuration in the export configuration file
   */
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
   * The list of flags that records must match to be included in the export
   */
  private List<Flag> flags;

  /**
   * The set of columns to be exported. See {@link ExportConfig} for a list
   * of supported columns
   */
  private List<String> sensorColumns;

  /**
   * The calculation columns to be exported for the different calculation paths
   */
  private TreeMap<String, List<String>> calculationColumns;

  static {
    FIXED_KEYS = new ArrayList<String>(4);
    FIXED_KEYS.add("exportName");
    FIXED_KEYS.add("separator");
    FIXED_KEYS.add("flags");
    FIXED_KEYS.add("sensors");
  }

  /**
   * Build an ExportOption object from a JSON string
   * @param index The index of the configuration in the export configuration file
   * @param json The JSON string defining the export option
   * @throws ExportException If the export configuration is invalid
   */
  public ExportOption(ResourceManager resourceManager, int index, JSONObject json) throws ExportException {
    this.index = index;
    this.calculationColumns = new TreeMap<String, List<String>>();
    parseJson(resourceManager, json);
  }

  /**
   * Returns the index of this configuration in the export configuration file
   * @return The configuration index
   */
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

  /**
   * Returns the list of record flags to be included in the export file.
   * Records without one of these flags will not be exported.
   * @return The list of flags
   */
  public List<Flag> getFlags() {
    return flags;
  }

  /**
   * Export the option details from a JSON object
   * @param json The JSON
   * @throws ExportConfigurationException If the JSON does not contain valid values
   */
  private void parseJson(ResourceManager resourceManager, JSONObject json) throws ExportConfigurationException {

    // Export name
    try {
      this.name = json.getString("exportName").trim();
      if (name.length() == 0) {
        throw new ExportConfigurationException(index, "exportName cannot be empty");
      }
    } catch (JSONException e) {
      throw new ExportConfigurationException(index, "Missing exportName");
    }

    // Separator
    try {
      String separator = json.getString("separator");
      parseSeparator(separator);
    } catch (JSONException e) {
      throw new ExportConfigurationException(index, "Missing separator");
    }

    // Flags
    try {

      JSONArray flagsEntry = json.getJSONArray("flags");
      if (flagsEntry.length() == 0) {
        throw new ExportConfigurationException(index, "At least one flag must be specified");
      }

      this.flags = new ArrayList<Flag>(flagsEntry.length());
      for (int i = 0; i < flagsEntry.length(); i++) {
        int flagEntry = flagsEntry.getInt(i);

        try {
          this.flags.add(new Flag(flagsEntry.getInt(i)));
        } catch (InvalidFlagException e) {
          throw new ExportConfigurationException(index, "Invalid flag value '" + flagEntry + "'");
        }
      }

    } catch (JSONException e) {
      throw new ExportConfigurationException(index, "Missing or invalid flags entry");
    }

    // Sensors
    try {
      JSONArray sensorColumnsEntry = json.getJSONArray("sensors");
      sensorColumns = new ArrayList<String>();
      for (int i = 0; i < sensorColumnsEntry.length(); i++) {
        sensorColumns.add(sensorColumnsEntry.getString(i).trim());
      }

      try {
        resourceManager.getSensorsConfiguration().validateSensorNames(sensorColumns);
      } catch (SensorConfigurationException e) {
        throw new ExportConfigurationException(index, "Error parsing sensors entry: " + e.getMessage());
      }
    } catch (JSONException e) {
      throw new ExportConfigurationException(index, "Missing or invalid sensor columns entry");
    }

    // Calculation columns.
    // These are any other entries that haven't been processed above.
    for (String key : json.keySet()) {
      if (!FIXED_KEYS.contains(key)) {

        CalculationDB calculationDb = null;

        try {
          calculationDb = CalculationDBFactory.getCalculationDB(key);
        } catch (CalculatorException e) {
          throw new ExportConfigurationException(index, "Unrecognised calculation idendtifier '" + key + "'");
        }

        try {
          JSONArray calculationColumnsEntry = json.getJSONArray(key);
          List<String> calculationColumnNames = new ArrayList<String>();
          for (int i = 0; i < calculationColumnsEntry.length(); i++) {
            calculationColumnNames.add(calculationColumnsEntry.getString(i));
          }

          calculationDb.validateColumnHeadings(calculationColumnNames);
          this.calculationColumns.put(key, calculationColumnNames);
        } catch (JSONException e) {
          throw new ExportConfigurationException(index, "Invalid calculation columns entry '" + key + "'");
        } catch (CalculatorException e) {
          throw new ExportConfigurationException(index, e);
        }
      }
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
      throw new ExportConfigurationException(index, "Separator must be 'tab' or 'comma'");
    }
    }
  }

  /**
   * Get the list of sensor columns to be included in the export file
   * @return The sensor columns
   */
  public List<String> getSensorColumns() {
    return sensorColumns;
  }

  /**
   * Get the list of columns required for the given calculation
   * @param calculation The calculation name
   * @return The list of columns
   */
  public List<String> getCalculationColumns(String calculation) {
    return calculationColumns.get(calculation);
  }

  /**
   * See if a given flag value can be included in this export,
   * i.e. it is contained in the {@code flags} list
   * @param flag The flag to be checked
   * @return {@code true} if the flag can be included; {@code false} if it cannot
   */
  public boolean flagAllowed(Flag flag) {
    boolean allowed = false;

    for (Flag checkFlag : flags) {
      if (Flag.getWoceValue(checkFlag.getFlagValue()) == Flag.getWoceValue(flag.getFlagValue())) {
        allowed = true;
        break;
      }
    }

    return allowed;
  }
}
