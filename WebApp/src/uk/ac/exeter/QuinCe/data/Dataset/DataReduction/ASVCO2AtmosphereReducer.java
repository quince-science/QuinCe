package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Data reducer for atmospheric measurements from the ASVCO2 sensor.
 *
 * <p>
 * This is simply a version of the {@link UnderwayAtmosphericPco2Reducer}, but
 * does not use gas standards.
 * </p>
 *
 */
public class ASVCO2AtmosphereReducer extends UnderwayAtmosphericPco2Reducer {

  /**
   * Basic {@link DataReducer} constructor.
   *
   * @param variable
   *          The {@link Variable} being processed.
   * @param properties
   *          The variable properties.
   * @param calculationCoefficients
   *          The calculation coefficients specified for the instrument.
   */
  public ASVCO2AtmosphereReducer(Variable variable,
    Map<String, Properties> properties,
    CalibrationSet calculationCoefficients) {
    super(variable, properties, calculationCoefficients);
  }

  @Override
  protected String getXCO2Parameter() {
    return "xCO₂ (dry, no standards)";
  }
}
