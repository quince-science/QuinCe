package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class SubCTechMeasurementValueCalculator
  extends DefaultMeasurementValueCalculator {

  @Override
  public MeasurementValue calculate(Instrument instrument, DataSet dataSet,
    SensorValuesListValue timeReference, Variable variable,
    SensorType requiredSensorType, DatasetMeasurements allMeasurements,
    DatasetSensorValues allSensorValues, Connection conn)
    throws MeasurementValueCalculatorException {

    try {

      if (!requiredSensorType.getShortName().equals("SubCTech xCO₂")
        && !requiredSensorType.getShortName().equals("SubCTech xH₂O")) {
        throw new MeasurementValueCalculatorException(
          "Invalid sensorType requested");

      }

      return getSensorValue(instrument, dataSet, timeReference, variable,
        requiredSensorType, allMeasurements, allSensorValues, conn);
    } catch (Exception e) {
      throw new MeasurementValueCalculatorException(e);
    }
  }

  @Override
  protected void calibrate(Instrument instrument, TimeDataSet dataset,
    SensorValuesListValue timeReference, SensorType sensorType,
    MeasurementValue value, DatasetMeasurements allMeasurements,
    SensorValuesList sensorValues, long columnId, Connection conn)
    throws MeasurementValueCalculatorException {

    // SubCTech has a custom calibration algorithm

  }
}
