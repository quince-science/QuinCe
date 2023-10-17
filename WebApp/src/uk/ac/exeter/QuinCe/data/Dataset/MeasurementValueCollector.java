package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.util.Collection;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * MeasurementValueCollectors are responsible for generating all the
 * {@link MeasurementValue} objects for a {@link Measurement} object.
 */
public interface MeasurementValueCollector {

  public abstract Collection<MeasurementValue> collectMeasurementValues(
    Instrument instrument, DataSet dataSet, Variable variable,
    DatasetMeasurements allMeasurements, DatasetSensorValues allSensorValues,
    Connection conn, SensorValuesListValue referenceValue)
    throws MeasurementValueCalculatorException;
}
