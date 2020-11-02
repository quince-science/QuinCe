package junit.uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducer;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.MeasurementValues;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.ValueCalculators;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;

/**
 * A dummy {@link MeasurementValues} class that returns fixed values
 *
 */
@SuppressWarnings("serial")
public class FixedMeasurementValues extends MeasurementValues {

  private Map<String, Double> sensorValues;

  /**
   * Initialise the object for a given measurement and the specified sensor
   * values.
   *
   * @param measurement
   *          The measurement.
   * @param values
   *          The sensor values.
   */
  protected FixedMeasurementValues(Instrument instrument,
    Measurement measurement, String searchId,
    Map<String, Double> sensorValues) {

    super(instrument, measurement, searchId);
    this.sensorValues = sensorValues;
  }

  @Override
  public Double getValue(String sensorType,
    Map<String, ArrayList<Measurement>> allMeasurements,
    DatasetSensorValues allSensorValues, DataReducer reducer,
    ValueCalculators valueCalculators, Connection conn) throws Exception {

    return sensorValues.get(sensorType);
  }

  @Override
  public Double getValue(SensorType sensorType,
    Map<String, ArrayList<Measurement>> allMeasurements,
    DatasetSensorValues allSensorValues, DataReducer reducer,
    ValueCalculators valueCalculators, Connection conn) throws Exception {

    return getValue(sensorType.getName(), allMeasurements, allSensorValues,
      reducer, valueCalculators, conn);
  }

  @Override
  public void loadSensorValues(DatasetSensorValues allSensorValues,
    SensorType sensorType)
    throws RoutineException, SensorTypeNotFoundException {

    // Do nothing
  }

  @Override
  public List<MeasurementValue> getAllValues() {
    return null;
  }

  @Override
  public void put(SensorType sensorType, MeasurementValue value) {
    // Ignore
  }
}
