package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.ArrayList;
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
  }

  @Override
  protected void doCalculation(Instrument instrument, Measurement measurement,
    Map<SensorType, CalculationValue> sensorValues, DataReductionRecord record)
    throws Exception {
  }

  @Override
  protected String[] getRequiredTypeStrings() {
    return new String[] {};
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    return new ArrayList<CalculationParameter>(0);
  }

}
