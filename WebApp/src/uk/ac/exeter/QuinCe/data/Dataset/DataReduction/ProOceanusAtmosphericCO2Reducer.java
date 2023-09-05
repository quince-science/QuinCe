package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class ProOceanusAtmosphericCO2Reducer extends DataReducer {

  private static List<CalculationParameter> calculationParameters = null;

  public ProOceanusAtmosphericCO2Reducer(Variable variable,
    Map<String, Properties> properties) {
    super(variable, properties);
  }

  @Override
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws DataReductionException {

    try {
      Double airTemperature = measurement.getMeasurementValue("Air Temperature")
        .getCalculatedValue();
      Double cellGasPressure = measurement
        .getMeasurementValue("Cell Gas Pressure").getCalculatedValue();
      Double humidityPressure = measurement
        .getMeasurementValue("Humidity Pressure").getCalculatedValue();
      Double xCO2wet = measurement
        .getMeasurementValue("xCO₂ (wet, no standards)").getCalculatedValue();

      // TODO Unit conversion?
      Double xCO2dry = xCO2wet / (1 - (humidityPressure / cellGasPressure));

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
}
