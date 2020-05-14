package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.DateColumnGroupedSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

/**
 * No data reduction performed.
 *
 */
public class NoReductionReducer extends DataReducer {

  public NoReductionReducer(InstrumentVariable variable, boolean nrt,
    Map<String, Float> variableAttributes, List<Measurement> allMeasurements,
    DateColumnGroupedSensorValues groupedSensorValues,
    CalibrationSet calibrationSet) {
    super(variable, nrt, variableAttributes, allMeasurements,
      groupedSensorValues, calibrationSet);
    // TODO Auto-generated constructor stub
  }

  @Override
  protected void doCalculation(Instrument instrument, Measurement measurement,
    Map<SensorType, CalculationValue> sensorValues, DataReductionRecord record)
    throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  protected String[] getRequiredTypeStrings() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    // TODO Auto-generated method stub
    return null;
  }

}
