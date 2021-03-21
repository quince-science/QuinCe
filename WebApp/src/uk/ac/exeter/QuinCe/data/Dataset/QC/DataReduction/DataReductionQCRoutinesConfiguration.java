package uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducer;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * The configuration of QC routines to be run after data reduction.
 *
 * @author Steve Jones
 *
 */
public class DataReductionQCRoutinesConfiguration {

  /**
   * The name of the package in which all routine classes will be stored
   */
  private static final String ROUTINE_CLASS_ROOT = "uk.ac.exeter.QuinCe.data.Dataset.QC.";

  /**
   * The name of the package in which all data reduction routine classes will be
   * stored (child of {@link #ROUTINE_CLASS_ROOT})
   */
  private static final String ROUTINE_CLASS_PACKAGE = "DataReduction";

  /**
   * All routine class names must end with the same text
   */
  private static final String ROUTINE_CLASS_TAIL = "Routine";

  /**
   * The set of routines configured for each data reducer.
   */
  private Map<Class<? extends DataReducer>, List<DataReductionQCRoutine>> routines;

  /**
   * Initialise the configuration from the specific configuration file.
   *
   * @param sensorsConfig
   *          The application's sensors configuration.
   * @param configFile
   *          The path to the configuration file.
   * @throws MissingParamException
   *           If any required parameters are missing.
   * @throws DataReductionQCRoutinesConfigurationException
   *           If the configuration is invalid.
   */
  public DataReductionQCRoutinesConfiguration(
    SensorsConfiguration sensorsConfig, String configFile)
    throws MissingParamException,
    DataReductionQCRoutinesConfigurationException {

    MissingParam.checkMissing(configFile, "configFile");
    routines = new HashMap<Class<? extends DataReducer>, List<DataReductionQCRoutine>>();
    init(sensorsConfig, configFile);
  }

  /**
   * Read and initialise the configuration.
   *
   * @param sensorsConfig
   *          The application's sensors configuration.
   * @param configFile
   *          The path to the configuration file.
   */
  private void init(SensorsConfiguration sensorsConfig, String configFile)
    throws DataReductionQCRoutinesConfigurationException {

    Gson settingsGson = new GsonBuilder()
      .registerTypeAdapter(DataReductionQCRoutineSettings.class,
        new DataReductionQCRoutineSettingsDeserializer(sensorsConfig))
      .create();

    try {
      JsonParser parser = new JsonParser();
      JsonElement root = parser
        .parse(new JsonReader(new FileReader(configFile)));

      if (!root.isJsonArray()) {
        throw new DataReductionQCRoutinesConfigurationException(configFile,
          "Main element must be an array");
      }

      JsonArray array = root.getAsJsonArray();

      int entryCount = 0;
      for (JsonElement entry : array) {
        if (!entry.isJsonObject()) {
          throw new DataReductionQCRoutinesConfigurationException(configFile,
            "Entry " + entryCount + " must be a JsonObject");
        }

        JsonObject object = entry.getAsJsonObject();
        if (!object.has("variable")) {
          throw new DataReductionQCRoutinesConfigurationException(configFile,
            "Entry " + entryCount + " is missing the 'reducer' element");
        }

        String variable = object.get("variable").getAsString();
        Class<? extends DataReducer> reducerClass = DataReducerFactory
          .getReducerClass(variable);

        if (!object.has("routines")) {
          throw new DataReductionQCRoutinesConfigurationException(configFile,
            "No routines specified for " + variable);
        }

        JsonArray routines = object.get("routines").getAsJsonArray();
        for (JsonElement routineElement : routines) {
          JsonObject routine = routineElement.getAsJsonObject();

          if (!routine.has("name")) {
            throw new DataReductionQCRoutinesConfigurationException(configFile,
              "Missing routine name");
          }

          String routineClassName = getFullClassName(
            routine.get("name").getAsString());

          DataReductionQCRoutineSettings settings = settingsGson
            .fromJson(routine, DataReductionQCRoutineSettings.class);

          // Instantiate the routine class
          @SuppressWarnings("unchecked")
          Class<? extends DataReductionQCRoutine> routineClass = (Class<? extends DataReductionQCRoutine>) Class
            .forName(routineClassName);

          DataReductionQCRoutine instance = routineClass
            .getDeclaredConstructor().newInstance();
          instance.applySettings(settings);

          if (!this.routines.containsKey(reducerClass)) {
            this.routines.put(reducerClass,
              new ArrayList<DataReductionQCRoutine>());
          }
          this.routines.get(reducerClass).add(instance);
        }

        entryCount++;
      }

    } catch (DataReductionQCRoutinesConfigurationException e) {
      throw e;
    } catch (Exception e) {
      throw new DataReductionQCRoutinesConfigurationException(configFile, e);
    }
  }

  /**
   * Get the routines for the specified {@link DataReducer}.
   *
   * @param reducer
   *          The reducer.
   * @return The routines for the reducer.
   */
  public List<DataReductionQCRoutine> getRoutines(
    Class<? extends DataReducer> reducerClass) {
    return routines.get(reducerClass);
  }

  /**
   * Get the shortcut name of a concrete Routine instance, for storage in the
   * database.
   *
   * This is the class name without the package prefix, and with the word
   * 'Routine' stripped off the end
   *
   * @param routine
   *          The instance
   * @return The shortcut name.
   */
  public static String getRoutineName(DataReductionQCRoutine routine) {
    return ROUTINE_CLASS_PACKAGE + "."
      + routine.getClass().getSimpleName().replaceAll("Routine$", "");
  }

  public static DataReductionQCRoutine getRoutine(String routineName)
    throws RoutineException {

    try {
      return (DataReductionQCRoutine) Class
        .forName(getFullClassName(routineName)).getDeclaredConstructor()
        .newInstance();
    } catch (Exception e) {
      throw new RoutineException(
        "Cannot get routine instance for '" + routineName + "'", e);
    }
  }

  /**
   * Get the full class name from a routine name
   *
   * @param routineName
   *          The routine name
   * @return The full class name
   */
  private static String getFullClassName(String routineName) {
    String className = routineName;
    if (routineName.contains(".")) {
      String[] split = routineName.split("\\.");
      className = split[split.length - 1];
    }

    return ROUTINE_CLASS_ROOT + ROUTINE_CLASS_PACKAGE + "." + className
      + ROUTINE_CLASS_TAIL;
  }
}
