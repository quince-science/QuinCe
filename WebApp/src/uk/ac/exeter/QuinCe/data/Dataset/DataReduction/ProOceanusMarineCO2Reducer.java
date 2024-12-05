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

public class ProOceanusMarineCO2Reducer extends DataReducer {

  private static List<CalculationParameter> calculationParameters = null;

  public ProOceanusMarineCO2Reducer(Variable variable,
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
      Double cellGasPressure = measurement
        .getMeasurementValue("Cell Gas Pressure").getCalculatedValue();
      Double xCO2 = measurement.getMeasurementValue("xCO₂ (wet, no standards)")
        .getCalculatedValue();

      Double p = Calculators.hPaToAtmospheres(cellGasPressure);
      Double pCO2WetSST = xCO2 * p;
      Double fCO2 = Calculators.calcfCO2(pCO2WetSST, xCO2, p, waterTemperature);

      record.put("pCO₂ SST", pCO2WetSST);
      record.put("fCO₂", fCO2);
    } catch (Exception e) {
      throw new DataReductionException(e);
    }
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    calculationParameters = new ArrayList<CalculationParameter>(2);

    calculationParameters.add(new CalculationParameter(makeParameterId(0),
      "pCO₂ SST", "pCO₂ In Water", "PCO2TK02", "μatm", true));

    calculationParameters.add(new CalculationParameter(makeParameterId(1),
      "fCO₂", "fCO₂ In Water", "FCO2XXXX", "μatm", true));

    return calculationParameters;
  }
}
