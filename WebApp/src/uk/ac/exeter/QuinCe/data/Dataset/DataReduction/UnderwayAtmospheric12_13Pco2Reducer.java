package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class UnderwayAtmospheric12_13Pco2Reducer
  extends UnderwayAtmosphericPco2Reducer {

  public UnderwayAtmospheric12_13Pco2Reducer(Variable variable,
    Map<String, Properties> properties) {
    super(variable, properties);
  }

  @Override
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws DataReductionException {

    try {
      // We use equilibrator temperature as the presumed most realistic gas
      // temperature
      Double waterTemperature = measurement
        .getMeasurementValue("Water Temperature").getCalculatedValue();
      Double salinity = measurement.getMeasurementValue("Salinity")
        .getCalculatedValue();
      Double atmosphericPressure = measurement
        .getMeasurementValue("Atmospheric Pressure").getCalculatedValue();

      Double seaLevelPressure = Calculators.calcSeaLevelPressure(
        atmosphericPressure, waterTemperature,
        getFloatProperty("atm_pres_sensor_height"));

      record.put("Sea Level Pressure", seaLevelPressure);

      if (getStringProperty(UnderwayMarine12_13Pco2Reducer.CAL_GAS_TYPE_ATTR)
        .equals(UnderwayMarine12_13Pco2Reducer.SPLIT_CO2_GAS_CAL_TYPE)) {

        doSplitCalculation(record, measurement, waterTemperature, salinity,
          seaLevelPressure);
      } else {
        doTotalCalculation(record, measurement, waterTemperature, salinity,
          seaLevelPressure);
      }
    } catch (Exception e) {
      throw new DataReductionException(e);
    }
  }

  private void doSplitCalculation(DataReductionRecord record,
    Measurement measurement, Double waterTemperature, Double salinity,
    Double seaLevelPressure)
    throws SensorTypeNotFoundException, DataReductionException {

    // xCO2 values are dried as part of sorting out their Calculated Value
    Double x12CO2 = measurement.getMeasurementValue("x¹²CO₂ (with standards)")
      .getCalculatedValue();
    Double x13CO2 = measurement.getMeasurementValue("x¹³CO₂ (with standards)")
      .getCalculatedValue();

    Calculator x12CO2Calculator = new Calculator(waterTemperature, salinity,
      seaLevelPressure, x12CO2);
    Calculator x13CO2Calculator = new Calculator(waterTemperature, salinity,
      seaLevelPressure, x13CO2);

    // Will be the same for both 12C and 13C
    record.put("pH₂O", x12CO2Calculator.pH2O);
    record.put("xCO₂", x12CO2 + x13CO2);
    record.put("pCO₂", x12CO2Calculator.pCO2 + x13CO2Calculator.pCO2);
    record.put("fCO₂", x12CO2Calculator.fCO2 + x13CO2Calculator.fCO2);
  }

  private void doTotalCalculation(DataReductionRecord record,
    Measurement measurement, Double waterTemperature, Double salinity,
    Double seaLevelPressure)
    throws SensorTypeNotFoundException, DataReductionException {

    // xCO2 values are dried as part of sorting out their Calculated Value
    Double co2 = measurement
      .getMeasurementValue("x¹²CO₂ + x¹³CO₂ (with standards)")
      .getCalculatedValue();

    Calculator calculator = new Calculator(waterTemperature, salinity,
      seaLevelPressure, co2);

    record.put("pH₂O", calculator.pH2O);
    record.put("xCO₂", co2);
    record.put("pCO₂", calculator.pCO2);
    record.put("fCO₂", calculator.fCO2);
  }
}
