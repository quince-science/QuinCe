package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class UnderwayAtmospheric12_13Pco2Reducer
  extends UnderwayAtmosphericPco2Reducer {

  public UnderwayAtmospheric12_13Pco2Reducer(Variable variable,
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

    // xCO2 values are dried as part of sorting out their Calculated Value
    Double x12CO2 = measurement.getMeasurementValue("x¹²CO₂ (with standards)")
      .getCalculatedValue();
    Double x13CO2 = measurement.getMeasurementValue("x¹³CO₂ (with standards)")
      .getCalculatedValue();

    Double seaLevelPressure = Calculators.calcSeaLevelPressure(
      atmosphericPressure, intakeTemperature,
      getFloatProperty("atm_pres_sensor_height"));

    Calculator x12CO2Calculator = new Calculator(intakeTemperature, salinity,
      seaLevelPressure, x12CO2);
    Calculator x13CO2Calculator = new Calculator(intakeTemperature, salinity,
      seaLevelPressure, x13CO2);

    record.put("Sea Level Pressure", seaLevelPressure);
    record.put("pH₂O", x12CO2Calculator.pH2O + x13CO2Calculator.pH2O);
    record.put("xCO₂", x12CO2 + x13CO2);
    record.put("pCO₂", x12CO2Calculator.pCO2 + x13CO2Calculator.pCO2);
    record.put("fCO₂", x12CO2Calculator.fCO2 + x13CO2Calculator.fCO2);
  }

  @Override
  protected String[] getRequiredTypeStrings() {
    return new String[] { "Intake Temperature", "Salinity",
      "Atmospheric Pressure", "xH₂O (with standards)",
      "x¹²CO₂ (with standards)", "x¹³CO₂ (with standards)" };
  }
}
