package uk.ac.exeter.QuinCe.data.Export;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.web.datasets.export.ExportData;

public class ExportOptionDeserializer
  implements JsonDeserializer<ExportOption> {

  private Connection conn;

  private SensorsConfiguration sensorConfig;

  private int index = -1;

  private List<Variable> allVariables = null;

  protected ExportOptionDeserializer(Connection conn,
    SensorsConfiguration sensorConfig) {
    this.conn = conn;
    this.sensorConfig = sensorConfig;
  }

  @Override
  public ExportOption deserialize(JsonElement json, Type typeOfT,
    JsonDeserializationContext context) throws JsonParseException {

    try {
      init();
    } catch (Exception e) {
      throw new JsonParseException(e);
    }

    index++;
    ExportOption option;

    try {
      String optionName;
      String separator;
      List<Variable> variables;

      JsonObject jsonObj = (JsonObject) json;

      // Export name
      if (!jsonObj.has("exportName")) {
        throw new ExportConfigurationException(json, "Missing exportName");
      } else {
        optionName = jsonObj.get("exportName").getAsString().trim();
        if (optionName.contains("/") || optionName.contains("\\")) {
          throw new ExportConfigurationException(optionName,
            "Export option name cannot contain '/' or '\\'");
        }
      }

      // Separator
      if (!jsonObj.has("separator")) {
        throw new ExportConfigurationException(optionName, "Missing separator");
      } else {
        separator = parseSeparator(jsonObj.get("separator").getAsString(),
          optionName);
      }

      // Variables
      variables = parseVariables(jsonObj, optionName);

      // Create the basic option with default settings
      option = new ExportOption(index, optionName, separator, variables);

      // Override defaults as required
      if (jsonObj.has("visible")) {
        option.setVisible(jsonObj.get("visible").getAsBoolean());
      }

      if (jsonObj.has("missingValue")) {
        option
          .setMissingValue(jsonObj.get("missingValue").getAsString().trim());
      }

      if (jsonObj.has("missingQCFlag")) {
        option
          .setMissingQCFlag(jsonObj.get("missingQCFlag").getAsString().trim());
      }

      if (jsonObj.has("includeRawSensors")) {
        option.setIncludeRawSensors(
          jsonObj.get("includeRawSensors").getAsBoolean());
      }

      if (jsonObj.has("includeCalculationColumns")) {
        option.setIncludeCalculationColumns(
          jsonObj.get("includeCalculationColumns").getAsBoolean());
      }

      if (jsonObj.has("headerMode")) {
        option.setHeaderMode(
          parseHeaderMode(jsonObj.get("headerMode").getAsString(), optionName));
      }

      if (jsonObj.has("includeUnits")) {
        option.setIncludeUnits(jsonObj.get("includeUnits").getAsBoolean());
      }

      if (jsonObj.has("includeQCComments")) {
        option.setIncludeQCComments(
          jsonObj.get("includeQCComments").getAsBoolean());
      }

      if (jsonObj.has("timestampHeader")) {
        option.setTimestampHeader(
          jsonObj.get("timestampHeader").getAsString().trim());
      }

      if (jsonObj.has("timestampFormat")) {
        option.setTimestampFormat(DateTimeUtils
          .makeDateTimeFormatter(jsonObj.get("timestampFormat").getAsString()));
      }

      if (jsonObj.has("qcFlagSuffix")) {
        option
          .setQcFlagSuffix(jsonObj.get("qcFlagSuffix").getAsString().trim());
      }

      if (jsonObj.has("qcCommentSuffix")) {
        option.setQcCommentSuffix(
          jsonObj.get("qcCommentSuffix").getAsString().trim());
      }

      if (jsonObj.has("measurementsOnly")) {
        option
          .setMeasurementsOnly(jsonObj.get("measurementsOnly").getAsBoolean());
      }

      if (jsonObj.has("skipBad")) {
        option.setSkipBad(jsonObj.get("skipBad").getAsBoolean());
      }

      if (jsonObj.has("exportDataClass")) {
        option.setDataClass(parseDataClass(
          jsonObj.get("exportDataClass").getAsString(), optionName));
      }

      if (jsonObj.has("replaceColumnHeaders")) {

        JsonObject replacementHeadersJson = jsonObj
          .getAsJsonObject("replaceColumnHeaders");

        Map<String, String> replacementColumnHeaders = new HashMap<String, String>();

        replacementHeadersJson.keySet().stream().forEach(k -> {
          replacementColumnHeaders.put(k,
            replacementHeadersJson.get(k).getAsString());
        });

        option.setReplacementColumnHeaders(replacementColumnHeaders);
      }

      if (jsonObj.has("excludeColumns")) {
        List<String> excludedColumns = new ArrayList<String>();

        jsonObj.getAsJsonArray("excludeColumns")
          .forEach(e -> excludedColumns.add(e.getAsString()));

        if (excludedColumns.size() > 0) {
          option.setExcludedColumns(excludedColumns);
        }
      }

    } catch (Exception e) {
      throw new JsonParseException(e);
    }

    return option;
  }

  private void init() throws ExportConfigurationException {
    if (null == allVariables) {
      try {
        allVariables = InstrumentDB.getAllVariables(conn, sensorConfig);
      } catch (Exception e) {
        throw new ExportConfigurationException("N/A",
          "Unable to retrieve variable information");
      }
    }
  }

  @SuppressWarnings("unchecked")
  private Class<? extends ExportData> parseDataClass(String className,
    String optionName) throws ExportConfigurationException {

    Class<? extends ExportData> clazz;

    // ExportData class. All classes are in the
    // uk.ac.exeter.QuinCe.web.datasets.export package.
    String fullClassName = ExportOption.EXPORT_DATA_PACKAGE + className;

    try {
      Class<? extends Object> testClass = Class.forName(fullClassName);
      Class<ExportData> rootClass = (Class<ExportData>) Class
        .forName(ExportOption.DEFAULT_EXPORT_DATA_CLASS);

      // Check that the class inherits from the correct parent class.
      if (!rootClass.isAssignableFrom(testClass)) {
        throw new ExportConfigurationException(optionName,
          "Specified export class does not extend "
            + ExportOption.DEFAULT_EXPORT_DATA_CLASS);
      }

      // Check that the correct constructor is available
      try {
        testClass.getConstructor(DataSource.class, Instrument.class,
          DataSet.class, ExportOption.class);
      } catch (NoSuchMethodException e) {
        throw new ExportConfigurationException(optionName,
          "No valid constructor found in " + fullClassName);
      }

    } catch (ClassNotFoundException e) {
      throw new ExportConfigurationException(optionName,
        "Cannot find class " + fullClassName);
    } catch (Exception e) {
      if (e instanceof ExportConfigurationException) {
        throw e;
      } else {
        throw new ExportConfigurationException(optionName, e);
      }
    }

    try {
      clazz = (Class<? extends ExportData>) Class.forName(fullClassName);
    } catch (ClassNotFoundException e) {
      // Since we've already checked the class, this shouldn't really happen
      throw new ExportConfigurationException(optionName, e);
    }

    return clazz;
  }

  /**
   * Parse the {@code separator} entry in the export configuration JSON
   *
   * @param separator
   *          the separator entry
   * @throws ExportConfigurationException
   *           If the separator is not recognised
   */
  private String parseSeparator(String separator, String optionName)
    throws ExportConfigurationException {

    String result;

    switch (separator.toLowerCase()) {
    case "comma": {
      result = ",";
      break;
    }
    case "tab": {
      result = "\t";
      break;
    }
    default: {
      throw new ExportConfigurationException(optionName,
        "Separator must be 'tab' or 'comma'");
    }
    }

    return result;
  }

  private List<Variable> parseVariables(JsonObject jsonObj, String optionName)
    throws ExportConfigurationException {

    List<Variable> variables;

    // Variables
    // If the element isn't provided, assume all variables
    if (!jsonObj.has("variables")) {
      variables = allVariables;
    } else {
      JsonArray varArray = jsonObj.getAsJsonArray("variables");

      // If we have an empty array, use all variables
      if (varArray.size() == 0) {
        variables = allVariables;
      } else {
        variables = new ArrayList<Variable>(varArray.size());

        Iterator<JsonElement> iter = varArray.iterator();
        while (iter.hasNext()) {
          String varName = iter.next().getAsString();

          boolean variableFound = false;
          for (Variable variable : allVariables) {
            if (varName.equals(variable.getName())) {
              variables.add(variable);
              variableFound = true;
              break;
            }
          }

          if (!variableFound) {
            throw new ExportConfigurationException(optionName,
              "Invalid variable name '" + varName);
          }
        }
      }
    }

    return variables;
  }

  private int parseHeaderMode(String mode, String optionName)
    throws ExportConfigurationException {

    int result;

    switch (mode.toLowerCase()) {
    case "short": {
      result = ExportOption.HEADER_MODE_SHORT;
      break;
    }
    case "long": {
      result = ExportOption.HEADER_MODE_LONG;
      break;
    }
    case "code": {
      result = ExportOption.HEADER_MODE_CODE;
      break;
    }
    default: {
      throw new ExportConfigurationException(optionName,
        "Unrecognised header mode '" + mode + "'");
    }
    }

    return result;
  }
}
