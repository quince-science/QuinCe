package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class MapCO2AtmosphereReducer extends UnderwayAtmosphericPco2Reducer {

  public MapCO2AtmosphereReducer(Variable variable,
    Map<String, Properties> properties) {
    super(variable, properties);
  }

  @Override
  protected String getXCO2Parameter() {
    return "xCO₂ (dry, no standards)";
  }
}
