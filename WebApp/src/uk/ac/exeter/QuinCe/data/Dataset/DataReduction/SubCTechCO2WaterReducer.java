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
public class SubCTechCO2WaterReducer extends UnderwayMarinePco2Reducer {

  /**
   * The reducer's calculation parameters.
   */
  private static List<CalculationParameter> calculationParameters = null;

  public SubCTechCO2WaterReducer(Variable variable,
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
      Double cellGasPressure = measurement
        .getMeasurementValue("Cell Gas Pressure").getCalculatedValue();
      Double internalPressure = measurement
        .getMeasurementValue("Internal Pressure (differential)")
        .getCalculatedValue();

      Double equilibrationPressure = cellGasPressure - internalPressure;

      Double xCO2 = measurement.getMeasurementValue(getXCO2Parameter())
        .getCalculatedValue();

      /**
       * The calculator will calculate pCO₂ and fCO₂ at equilibrator temperature
       * and SST. Here we set them both to the same temperature (since there's
       * no EqT) so we just ignore the equilibrator values.
       */
      Calculator calculator = new Calculator(waterTemperature, salinity,
        waterTemperature, equilibrationPressure, xCO2);

      // Store the calculated values
      record.put("Equilibration Pressure", equilibrationPressure);
      record.put("pH₂O", calculator.pH2O);
      record.put("pCO₂ SST", calculator.pCO2SST);
      record.put("fCO₂", calculator.fCO2);
    } catch (Exception e) {
      throw new DataReductionException(e);
    }
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    if (null == calculationParameters) {
      calculationParameters = new ArrayList<CalculationParameter>(4);

      calculationParameters.add(
        new CalculationParameter(makeParameterId(0), "Equilibration Pressure",
          "Equilibration Pressure", "PRESSEQ", "hPa", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(1),
        "pH₂O", "Marine Water Vapour Pressure", "RH2OX0EQ", "hPa", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(2),
        "pCO₂ SST", "pCO₂ In Water", "PCO2TK02", "μatm", true));

      calculationParameters.add(new CalculationParameter(makeParameterId(3),
        "fCO₂", "fCO₂ In Water", "FCO2XXXX", "μatm", true));
    }

    return calculationParameters;
  }

  @Override
  protected String getXCO2Parameter() {
    return "SubCTech xCO₂";
  }

}
