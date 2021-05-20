package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Data reducer for SAMI pCO₂ sensor.
 *
 * <p>
 * Note that it is not possible to calculate fCO₂ from the sensor values because
 * we are missing xCO₂ and the pressure in the sensor.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class SamiPco2DataReducer extends DataReducer {

  private static List<CalculationParameter> calculationParameters = null;

  public SamiPco2DataReducer(Variable variable,
    Map<String, Properties> properties) {
    super(variable, properties);
  }

  @Override
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws Exception {

    Double intakeTemperature = measurement
      .getMeasurementValue("Intake Temperature").getCalculatedValue();
    Double equilibrationTemperature = measurement
      .getMeasurementValue("Equilibrator Temperature").getCalculatedValue();
    Double pCO2TEWet = measurement
      .getMeasurementValue("pCO₂ (wet at equilibration)").getCalculatedValue();

    Double pCO2SST = Calculators.calcCO2AtSST(pCO2TEWet,
      equilibrationTemperature, intakeTemperature);

    record.put("ΔT", Math.abs(intakeTemperature - equilibrationTemperature));
    record.put("pCO₂ SST", pCO2SST);
  }

  @Override
  protected String[] getRequiredTypeStrings() {
    return new String[] { "Intake Temperature", "Equilibrator Temperature",
      "pCO₂ (wet at equilibration)" };
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    if (null == calculationParameters) {
      calculationParameters = new ArrayList<CalculationParameter>(2);

      calculationParameters
        .add(new CalculationParameter(makeParameterId(0), "ΔT",
          "Water-Equilibrator Temperature Difference", "DELTAT", "°C", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(4),
        "pCO₂ SST", "pCO₂ In Water", "PCO2TK02", "μatm", true));
    }

    return calculationParameters;
  }
}
