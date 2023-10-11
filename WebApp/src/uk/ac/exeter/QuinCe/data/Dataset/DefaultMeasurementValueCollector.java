package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * The default implementation of the {@link MeasurementValuesCollector}.
 *
 * This collects the {@link MeasurementValue}s for all the {@link Variable}'s
 * required {@link SensorType}s according to the time specified in the supplied
 * reference {@link SensorValuesListValue}.
 */
public class DefaultMeasurementValueCollector
  extends MeasurementValueCollector {

  public Collection<MeasurementValue> collectMeasurementValues(
    Instrument instrument, DataSet dataSet, Variable variable,
    DatasetMeasurements allMeasurements, DatasetSensorValues allSensorValues,
    Connection conn, SensorValuesListValue referenceValue)
    throws MeasurementValueCalculatorException {

    List<MeasurementValue> result = new ArrayList<MeasurementValue>();

    for (SensorType sensorType : variable
      .getAllSensorTypes(!dataSet.fixedPosition())) {

      result.add(MeasurementValueCalculatorFactory.calculateMeasurementValue(
        instrument, dataSet, referenceValue, variable, sensorType,
        allMeasurements, allSensorValues, conn));
    }

    return result;
  }
}
