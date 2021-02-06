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
 * Data Reduction class for underway marine pCO₂
 *
 * @author Steve Jones
 *
 */
public class UnderwayAtmosphericPco2Reducer extends DataReducer {

  private static List<CalculationParameter> calculationParameters = null;

  public UnderwayAtmosphericPco2Reducer(Variable variable,
    Map<String, Properties> properties) {

    super(variable, properties);
  }

  @Override
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws Exception {

    // We use equilibrator temperature as the presumed most realistic gas
    // temperature
    Double equilibratorTemperature = measurement
      .getMeasurementValue("Equilibrator Temperature").getCalculatedValue();
    Double salinity = measurement.getMeasurementValue("Salinity")
      .getCalculatedValue();
    Double seaLevelPressure = measurement
      .getMeasurementValue("Atmospheric Pressure").getCalculatedValue();
    Double co2InGas = measurement.getMeasurementValue("xCO₂ (with standards)")
      .getCalculatedValue();

    Double pH2O = Calculators.calcPH2O(salinity, equilibratorTemperature);

    Double pCO2 = Calculators.calcpCO2TEWet(co2InGas, seaLevelPressure, pH2O);
    Double fCO2 = Calculators.calcfCO2(pCO2, co2InGas, seaLevelPressure,
      equilibratorTemperature);

    record.put("pH₂O", pH2O);
    record.put("Calibrated CO₂", co2InGas);
    record.put("pCO₂", pCO2);
    record.put("fCO₂", fCO2);
  }

  @Override
  protected String[] getRequiredTypeStrings() {
    return new String[] { "Equilibrator Temperature", "Salinity",
      "Atmospheric Pressure", "xCO₂ (with standards)" };
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    if (null == calculationParameters) {
      calculationParameters = new ArrayList<CalculationParameter>(7);

      calculationParameters
        .add(new CalculationParameter(makeParameterId(3), "pH₂O",
          "Atmosphere Water Vapour Pressure", "CPVPZZ01", "hPa", false, false));

      calculationParameters.add(new CalculationParameter(makeParameterId(4),
        "Calibrated CO₂", "xCO₂ In Atmosphere - Calibrated In Dry Air",
        "XCO2DCMA", "μmol mol-1", false, false));

      calculationParameters.add(new CalculationParameter(makeParameterId(5),
        "pCO₂", "pCO₂ In Atmosphere", "ACO2XXXX", "μatm", true, false));

      calculationParameters.add(new CalculationParameter(makeParameterId(6),
        "fCO₂", "fCO₂ In Atmosphere", "FCO2WTAT", "μatm", true, false));
    }

    return calculationParameters;
  }
}
