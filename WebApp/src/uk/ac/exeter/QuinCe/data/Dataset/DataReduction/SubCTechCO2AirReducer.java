package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Data Reducer for water measurements from a SubCTech CO₂ sensor.
 *
 * Algorithm provided by SubCTech via email.
 */
public class SubCTechCO2AirReducer extends UnderwayMarinePco2Reducer {

  /**
   * The reducer's calculation parameters.
   */
  private static List<CalculationParameter> calculationParameters = null;

  public SubCTechCO2AirReducer(Variable variable,
    Map<String, Properties> properties,
    CalibrationSet calculationCoefficients) {
    super(variable, properties, calculationCoefficients);
  }

  @Override
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws DataReductionException {

    try {
      Double airTemperature = measurement.getMeasurementValue("Air Temperature")
        .getCalculatedValue();
      Double cellGasPressure = measurement
        .getMeasurementValue("Cell Gas Pressure").getCalculatedValue();
      Double xCO2wet = measurement
        .getMeasurementValue("xCO₂ (wet, no standards)").getCalculatedValue();

      // TODO Unit conversion?
      Double xCO2dry = xCO2wet / (1 - (cellGasPressure));

      Double p = Calculators.hPaToAtmospheres(cellGasPressure);
      Double pCO2 = xCO2wet * p;
      Double fCO2 = Calculators.calcfCO2(pCO2, xCO2wet, p, airTemperature);

      record.put("xCO₂", xCO2dry);
      record.put("pCO₂", pCO2);
      record.put("fCO₂", fCO2);
    } catch (Exception e) {
      throw new DataReductionException(e);
    }
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    calculationParameters = new ArrayList<CalculationParameter>(3);

    calculationParameters.add(new CalculationParameter(makeParameterId(0),
      "xCO₂", "xCO₂ In Atmosphere", "XCO2DRAT", "μmol mol⁻¹", true));

    calculationParameters.add(new CalculationParameter(makeParameterId(1),
      "pCO₂", "pCO₂ In Atmosphere", "ACO2XXXX", "μatm", true));

    calculationParameters.add(new CalculationParameter(makeParameterId(2),
      "fCO₂", "fCO₂ In Atmosphere", "FCO2WTAT", "μatm", true));

    return calculationParameters;
  }

  @Override
  protected String getXCO2Parameter() {
    return "xCO₂ (with standards)";
  }

}
