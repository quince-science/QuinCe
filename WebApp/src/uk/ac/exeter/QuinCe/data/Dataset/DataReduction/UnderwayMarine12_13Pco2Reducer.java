package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class UnderwayMarine12_13Pco2Reducer extends UnderwayMarinePco2Reducer {

  public static final String CAL_GAS_TYPE_ATTR = "cal_gas_type";

  public static final String SPLIT_CO2_GAS_CAL_TYPE = "¹²CO₂/¹³CO₂";

  public static final String TOTAL_CO2_GAS_CAL_TYPE = "Total CO₂";

  public UnderwayMarine12_13Pco2Reducer(Variable variable,
    Map<String, Properties> properties,
    CalibrationSet calculationCoefficients) {
    super(variable, properties, calculationCoefficients);
  }

  @Override
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws DataReductionException {

    try {
      Double waterTemperature = measurement
        .getMeasurementValue("Water Temperature").getCalculatedValue();
      Double salinity = measurement.getMeasurementValue("Salinity")
        .getCalculatedValue();
      Double equilibratorTemperature = measurement
        .getMeasurementValue("Equilibrator Temperature").getCalculatedValue();
      Double equilibratorPressure = measurement
        .getMeasurementValue("Equilibrator Pressure").getCalculatedValue();

      // Store the calculated values
      record.put("ΔT", equilibratorTemperature - waterTemperature);

      if (getStringProperty(CAL_GAS_TYPE_ATTR).equals(SPLIT_CO2_GAS_CAL_TYPE)) {

        doSplitCalculation(record, measurement, waterTemperature, salinity,
          equilibratorTemperature, equilibratorPressure);
      } else {
        doTotalCalculation(record, measurement, waterTemperature, salinity,
          equilibratorTemperature, equilibratorPressure);
      }
    } catch (Exception e) {
      throw new DataReductionException(e);
    }
  }

  private void doSplitCalculation(DataReductionRecord record,
    Measurement measurement, Double waterTemperature, Double salinity,
    Double equilibratorTemperature, Double equilibratorPressure)
    throws SensorTypeNotFoundException, DataReductionException {

    // xCO2 values are dried as part of sorting out their Calculated Value
    Double x12CO2 = measurement.getMeasurementValue("x¹²CO₂ (with standards)")
      .getCalculatedValue();
    Double x13CO2 = measurement.getMeasurementValue("x¹³CO₂ (with standards)")
      .getCalculatedValue();

    Calculator x12CO2Calculator = new Calculator(waterTemperature, salinity,
      equilibratorTemperature, equilibratorPressure, x12CO2);
    Calculator x13CO2Calculator = new Calculator(waterTemperature, salinity,
      equilibratorTemperature, equilibratorPressure, x13CO2);

    // Will be the same for both 12C and 13C
    record.put("pH₂O", x12CO2Calculator.pH2O);

    record.put("pCO₂ TE Wet",
      x12CO2Calculator.pCo2TEWet + x13CO2Calculator.pCo2TEWet);
    record.put("fCO₂ TE Wet",
      x12CO2Calculator.fCo2TEWet + x13CO2Calculator.fCo2TEWet);
    record.put("pCO₂ SST", x12CO2Calculator.pCO2SST + x13CO2Calculator.pCO2SST);
    record.put("fCO₂", x12CO2Calculator.fCO2 + x13CO2Calculator.fCO2);
  }

  private void doTotalCalculation(DataReductionRecord record,
    Measurement measurement, Double waterTemperature, Double salinity,
    Double equilibratorTemperature, Double equilibratorPressure)
    throws SensorTypeNotFoundException, DataReductionException {

    // xCO2 values are dried as part of sorting out their Calculated Value
    Double co2 = measurement
      .getMeasurementValue("x¹²CO₂ + x¹³CO₂ (with standards)")
      .getCalculatedValue();

    Calculator calculator = new Calculator(waterTemperature, salinity,
      equilibratorTemperature, equilibratorPressure, co2);

    record.put("pH₂O", calculator.pH2O);
    record.put("pCO₂ TE Wet", calculator.pCo2TEWet);
    record.put("fCO₂ TE Wet", calculator.fCo2TEWet);
    record.put("pCO₂ SST", calculator.pCO2SST);
    record.put("fCO₂", calculator.fCO2);
  }
}
