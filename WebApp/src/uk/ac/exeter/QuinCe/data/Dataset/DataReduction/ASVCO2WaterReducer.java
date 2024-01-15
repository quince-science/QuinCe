package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Data reducer for water measurements from the ASVCO2 sensor.
 *
 * <p>
 * This is simply a version of the {@link UnderwayMarinePco2Reducer}, but does
 * not use gas standards.
 * </p>
 *
 */
public class ASVCO2WaterReducer extends UnderwayMarinePco2Reducer {

  /**
   * Basic {@link DataReducer} constructor.
   *
   * @param variable
   *          The {@link Variable} being processed.
   * @param properties
   *          The variable properties.
   */
  public ASVCO2WaterReducer(Variable variable,
    Map<String, Properties> properties) {
    super(variable, properties);
  }

  @Override
  protected String getXCO2Parameter() {
    return "xCO₂ (dry, no standards)";
  }
}
