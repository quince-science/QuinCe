package uk.ac.exeter.QuinCe.data.Dataset.QC.DataReductionRoutines;

import java.io.FileReader;
import java.lang.reflect.Constructor;
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
  private static final String ROUTINE_CLASS_ROOT = "uk.ac.exeter.QuinCe.data.Dataset.QC.DataReductionRoutines.";

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

          String routineClassName = ROUTINE_CLASS_ROOT
            + routine.get("name").getAsString() + ROUTINE_CLASS_TAIL;

          DataReductionQCRoutineSettings settings = settingsGson
            .fromJson(routine, DataReductionQCRoutineSettings.class);

          // Instantiate the routine class
          @SuppressWarnings("unchecked")
          Class<? extends DataReductionQCRoutine> routineClass = (Class<? extends DataReductionQCRoutine>) Class
            .forName(routineClassName);

          @SuppressWarnings("unchecked")
          Constructor<DataReductionQCRoutine> constructor = (Constructor<DataReductionQCRoutine>) routineClass
            .getConstructor(DataReductionQCRoutineSettings.class);

          DataReductionQCRoutine instance = constructor.newInstance(settings);

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
  public List<DataReductionQCRoutine> getRoutines(DataReducer reducer) {
    return routines.get(reducer.getClass());
  }
}
