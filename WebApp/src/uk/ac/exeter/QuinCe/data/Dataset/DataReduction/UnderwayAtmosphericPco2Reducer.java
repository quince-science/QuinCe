package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
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
  protected void doCalculation(Instrument instrument,
    MeasurementValues sensorValues, DataReductionRecord record,
    Map<String, ArrayList<Measurement>> allMeasurements,
    DatasetSensorValues allSensorValues, Connection conn) throws Exception {

    // We use equilibrator temperature as the presumed most realistic gas
    // temperature
    Double equilibratorTemperature = sensorValues.getValue(
      "Equilibrator Temperature", allMeasurements, allSensorValues, this,
      valueCalculators, conn);
    Double salinity = sensorValues.getValue("Salinity", allMeasurements,
      allSensorValues, this, valueCalculators, conn);
    Double seaLevelPressure = sensorValues.getValue(
      "Atmospheric Pressure at Sea Level", allMeasurements, allSensorValues,
      this, valueCalculators, conn);
    Double co2InGas = sensorValues.getValue("xCO₂ (with standards)",
      allMeasurements, allSensorValues, this, valueCalculators, conn);

    Double pH2O = Calculators.calcPH2O(salinity, equilibratorTemperature);

    Double pCO2 = Calculators.calcpCO2TEWet(co2InGas, seaLevelPressure, pH2O);
    Double fCO2 = Calculators.calcfCO2(pCO2, co2InGas, seaLevelPressure,
      equilibratorTemperature);

    record.put("Sea Level Pressure", seaLevelPressure);
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
      calculationParameters = new ArrayList<CalculationParameter>(5);
      calculationParameters
        .add(new CalculationParameter(makeParameterId(0), "Sea Level Pressure",
          "Atmospheric Pressure At Sea Level", "CAPAZZ01", "hPa", false));
      calculationParameters.add(new CalculationParameter(makeParameterId(1),
        "pH₂O", "Atmosphere Water Vapour Pressure", "RH2OX0EQ", "hPa", false));
      calculationParameters.add(new CalculationParameter(makeParameterId(2),
        "Calibrated CO₂", "xCO₂ In Atmosphere - Calibrated In Dry Air",
        "XCO2DCMA", "μmol mol-1", false));
      calculationParameters.add(new CalculationParameter(makeParameterId(3),
        "pCO₂", "pCO₂ In Atmosphere", "ACO2XXXX", "μatm", true));
      calculationParameters.add(new CalculationParameter(makeParameterId(4),
        "fCO₂", "fCO₂ In Atmosphere", "FCO2WTAT", "μatm", true));
    }

    return calculationParameters;
  }
}
