package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Range;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;

public class QCConfigurationDeserializer
  implements JsonDeserializer<QCConfiguration> {

  private final SensorsConfiguration sensorsConfig;

  private final Class<? extends AbstractQCRoutinesConfiguration> routinesConfigClass;

  public QCConfigurationDeserializer(SensorsConfiguration sensorsConfig,
    Class<? extends AbstractQCRoutinesConfiguration> routinesConfigClass) {

    this.sensorsConfig = sensorsConfig;
    this.routinesConfigClass = routinesConfigClass;
  }

  @Override
  public QCConfiguration deserialize(JsonElement json, Type typeOfT,
    JsonDeserializationContext context) throws JsonParseException {

    QCConfiguration result = new QCConfiguration();

    try {
      JsonObject jsonObj = (JsonObject) json;
      Set<String> basisNames = jsonObj.keySet();

      for (String basisName : basisNames) {
        Integer basis = Instrument.basisFromString(basisName);
        FlagScheme flagScheme = Instrument.getFlagScheme(basis);

        Constructor<? extends AbstractQCRoutinesConfiguration> configConstructor = routinesConfigClass
          .getDeclaredConstructor();

        AbstractQCRoutinesConfiguration basisRoutinesConfig = (AbstractQCRoutinesConfiguration) configConstructor
          .newInstance();

        result.put(basis, basisRoutinesConfig);

        JsonArray sensorTypesArray = jsonObj.get(basisName).getAsJsonArray();

        for (JsonElement sensorTypeElement : sensorTypesArray) {
          JsonObject sensorTypeObj = sensorTypeElement.getAsJsonObject();

          String sensorTypeName = sensorTypeObj.get("sensorType").getAsString();
          SensorType sensorType = sensorsConfig.getSensorType(sensorTypeName);

          JsonArray jsonRoutines = sensorTypeObj.get("routines")
            .getAsJsonArray();

          for (JsonElement routineElem : jsonRoutines) {
            JsonObject jsonRoutine = (JsonObject) routineElem;

            String className = jsonRoutine.get("class").getAsString();

            Map<Flag, Range<Double>> limits = new HashMap<Flag, Range<Double>>();

            if (jsonRoutine.has("limits")) {
              JsonArray limitsArray = jsonRoutine.get("limits")
                .getAsJsonArray();

              for (JsonElement limitElement : limitsArray) {
                JsonObject limitObject = limitElement.getAsJsonObject();

                int flagValue = limitObject.get("flag").getAsInt();
                Flag flag = flagScheme.getFlag(flagValue);

                JsonArray rangeArray = limitObject.get("range")
                  .getAsJsonArray();
                if (rangeArray.size() != 2) {
                  throw new QCRoutinesConfigurationDeserializerException(
                    sensorTypeName, className, "Limits must have 2 entries");
                }

                Double lower = rangeArray.get(0).getAsDouble();
                Double upper = rangeArray.get(1).getAsDouble();
                Range<Double> range = Range.between(lower, upper);

                limits.put(flag, range);
              }
            }

            basisRoutinesConfig.addRoutine(flagScheme, sensorType, className,
              limits);
          }
        }

      }
    } catch (Exception e) {
      throw new QCRoutinesConfigurationDeserializerException(e);
    }

    return result;
  }
}

@SuppressWarnings("serial")
class QCRoutinesConfigurationDeserializerException extends JsonParseException {

  protected QCRoutinesConfigurationDeserializerException(String message) {
    super(message);
  }

  protected QCRoutinesConfigurationDeserializerException(Throwable cause) {
    super("Error parsing QC configuration", cause);
  }

  protected QCRoutinesConfigurationDeserializerException(String sensorType,
    String className, String message) {

    super(sensorType + "/" + className + ": " + message);
  }
}
