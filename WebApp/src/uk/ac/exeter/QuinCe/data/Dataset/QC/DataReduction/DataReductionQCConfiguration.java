package uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducer;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;

@SuppressWarnings("serial")
public class DataReductionQCConfiguration
  extends HashMap<FlagScheme, DataReductionQCRoutinesConfiguration> {

  public DataReductionQCConfiguration(SensorsConfiguration sensorsConfig,
    String configFile) throws Exception {

    super();
    parseConfig(sensorsConfig, configFile);
  }

  private void parseConfig(SensorsConfiguration sensorsConfig,
    String configFile) throws Exception {

    String fileContent = new String(Files.readAllBytes(Paths.get(configFile)),
      StandardCharsets.UTF_8);

    JsonObject jsonObj = JsonParser.parseString(fileContent).getAsJsonObject();
    Set<String> basisNames = jsonObj.keySet();

    for (String basisName : basisNames) {
      Integer basis = Instrument.basisFromString(basisName);
      FlagScheme flagScheme = Instrument.getFlagScheme(basis);

      Gson settingsGson = new GsonBuilder()
        .registerTypeAdapter(DataReductionQCRoutineSettings.class,
          new DataReductionQCRoutineSettingsDeserializer(sensorsConfig,
            flagScheme))
        .create();

      DataReductionQCRoutinesConfiguration basisRoutines = new DataReductionQCRoutinesConfiguration();

      JsonArray array = jsonObj.get(basisName).getAsJsonArray();

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

          String routineClassName = DataReductionQCRoutinesConfiguration
            .getFullClassName(routine.get("name").getAsString());

          DataReductionQCRoutineSettings settings = settingsGson
            .fromJson(routine, DataReductionQCRoutineSettings.class);

          // Instantiate the routine class
          @SuppressWarnings("unchecked")
          Class<? extends DataReductionQCRoutine> routineClass = (Class<? extends DataReductionQCRoutine>) Class
            .forName(routineClassName);

          DataReductionQCRoutine instance = routineClass
            .getDeclaredConstructor(FlagScheme.class).newInstance(flagScheme);
          instance.applySettings(settings);

          if (!basisRoutines.containsKey(reducerClass)) {
            basisRoutines.put(reducerClass,
              new ArrayList<DataReductionQCRoutine>());
          }
          basisRoutines.get(reducerClass).add(instance);
        }

        entryCount++;
      }

      put(flagScheme, basisRoutines);
    }
  }
}
