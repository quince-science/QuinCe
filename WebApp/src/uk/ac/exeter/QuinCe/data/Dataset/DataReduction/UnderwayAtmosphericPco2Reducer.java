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
    Double intakeTemperature = measurement
      .getMeasurementValue("Intake Temperature").getCalculatedValue();
    Double salinity = measurement.getMeasurementValue("Salinity")
      .getCalculatedValue();
    Double atmosphericPressure = measurement
      .getMeasurementValue("Atmospheric Pressure").getCalculatedValue();
    Double co2InGas = measurement.getMeasurementValue(getXCO2Parameter())
      .getCalculatedValue();

    Double seaLevelPressure = Calculators.calcSeaLevelPressure(
      atmosphericPressure, intakeTemperature,
      getFloatProperty("atm_pres_sensor_height"));

    Double pH2O = Calculators.calcPH2O(salinity, intakeTemperature);

    Double pCO2 = Calculators.calcpCO2TEWet(co2InGas, seaLevelPressure, pH2O);
    Double fCO2 = Calculators.calcfCO2(pCO2, co2InGas, seaLevelPressure,
      intakeTemperature);

    record.put("Sea Level Pressure", seaLevelPressure);
    record.put("pH₂O", pH2O);
    record.put("xCO₂", co2InGas);
    record.put("pCO₂", pCO2);
    record.put("fCO₂", fCO2);
  }

  @Override
  protected String[] getRequiredTypeStrings() {
    return new String[] { "Intake Temperature", "Salinity",
      "Atmospheric Pressure", getXCO2Parameter() };
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    if (null == calculationParameters) {
      calculationParameters = new ArrayList<CalculationParameter>(5);

      calculationParameters.add(new CalculationParameter(makeParameterId(0),
        "Sea Level Pressure", "Sea Level Pressure", "CAPASS01", "hPa", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(1),
        "pH₂O", "Atmosphere Water Vapour Pressure", "CPVPZZ01", "hPa", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(2),
        "xCO₂", "xCO₂ In Atmosphere", "XCO2DRAT", "μmol mol⁻¹", true));

      calculationParameters.add(new CalculationParameter(makeParameterId(3),
        "pCO₂", "pCO₂ In Atmosphere", "ACO2XXXX", "μatm", true));

      calculationParameters.add(new CalculationParameter(makeParameterId(4),
        "fCO₂", "fCO₂ In Atmosphere", "FCO2WTAT", "μatm", true));
    }

    return calculationParameters;
  }

  protected String getXCO2Parameter() {
    return "xCO₂ (with standards)";
  }
}
