package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.VariableNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class ExternalStandardFactory {

  public static ExternalStandard getExternalStandard(Instrument instrument,
    long id, LocalDateTime date) throws Exception {

    Class<? extends ExternalStandard> clazz = getExternalStandardClass(
      instrument);

    Constructor<? extends ExternalStandard> con = clazz
      .getConstructor(Instrument.class, Long.TYPE, LocalDateTime.class);

    return con.newInstance(instrument, id, date);
  }

  public static ExternalStandard getExternalStandard(long id,
    Instrument instrument, String target, LocalDateTime deploymentDate,
    Map<String, String> coefficients) throws Exception {

    Class<? extends ExternalStandard> clazz = getExternalStandardClass(
      instrument);

    Constructor<? extends ExternalStandard> constructor = clazz.getConstructor(
      long.class, Instrument.class, String.class, LocalDateTime.class,
      Map.class);

    return constructor.newInstance(id, instrument, target, deploymentDate,
      coefficients);

  }

  private static Class<? extends ExternalStandard> getExternalStandardClass(
    Instrument instrument) throws VariableNotFoundException {

    Class<? extends ExternalStandard> result;

    Variable d12D13MarineVar = ResourceManager.getInstance()
      .getSensorsConfiguration()
      .getInstrumentVariable("Underway Marine pCO₂ from ¹²CO₂/¹³CO₂");

    Variable d12D13AtmosVar = ResourceManager.getInstance()
      .getSensorsConfiguration()
      .getInstrumentVariable("Underway Atmospheric pCO₂ from ¹²CO₂/¹³CO₂");

    Variable subCTechWaterVar = ResourceManager.getInstance()
      .getSensorsConfiguration().getInstrumentVariable("SubCTech CO₂ Water");

    Variable subCTechAirVar = ResourceManager.getInstance()
      .getSensorsConfiguration().getInstrumentVariable("SubCTech CO₂ Air");

    if (instrument.hasVariable(d12D13MarineVar)
      || instrument.hasVariable(d12D13AtmosVar)) {
      result = D12D13SensorExternalStandard.class;
    } else if (instrument.hasVariable(subCTechWaterVar)
      || instrument.hasVariable(subCTechAirVar)) {
      result = SubCTechExternalStandard.class;
    } else {
      result = DefaultExternalStandard.class;
    }

    return result;
  }
}
