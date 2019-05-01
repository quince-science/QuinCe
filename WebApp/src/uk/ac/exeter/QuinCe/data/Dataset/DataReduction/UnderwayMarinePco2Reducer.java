package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementsWithSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

/**
 * Data Reduction class for underway marine pCO₂
 * @author Steve Jones
 *
 */
public class UnderwayMarinePco2Reducer extends DataReducer {

  /**
   * The conversion factor from Pascals to Atmospheres
   */
  private static final double PASCALS_TO_ATMOSPHERES = 0.00000986923266716013;

  private static List<String> calculationParameters;
  
  static {
    calculationParameters = new ArrayList<String>(8);
    calculationParameters.add("ΔT");
    calculationParameters.add("True Moisture");
    calculationParameters.add("pH₂O");
    calculationParameters.add("Dried CO₂");
    calculationParameters.add("Calibrated CO₂");
    calculationParameters.add("pCO₂ TE Wet");
    calculationParameters.add("pCO₂ SST");
    calculationParameters.add("fCO₂");
  }
  
  @Override
  protected void doCalculation(Instrument instrument, Measurement measurement,
      HashMap<SensorType, TreeSet<SensorValue>> sensorValues,
      MeasurementsWithSensorValues allMeasurements, DataReductionRecord record) throws Exception {

    CalculationInputValues inputValues = getCalculationInputValues(
      instrument, sensorValues,
      "Intake Temperature", "Salinity", "Equilibrator Temperature",
      "Equilibrator Pressure");
    
    
  }

  @Override
  protected List<String> getCalculationParameters() {
    return calculationParameters;
  }
}
