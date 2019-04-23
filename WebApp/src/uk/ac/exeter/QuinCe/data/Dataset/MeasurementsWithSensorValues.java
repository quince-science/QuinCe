package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * This class contains all the measurements for a dataset (of all Run Types)
 * and the Sensor Values associated with them as determined by the
 * ChooseSensorValues job.
 *
 * This is structured as a TreeMap. The keys are the measurements ordered
 * by time. The values are a HashMap. The key of the HashMap is a SensorType,
 * and the Values are a TreeSet of SensorValue objects of that Sensor Type
 * which have been associated with the parent Measurement object, ordered by
 * the time that the SensorValue was measured.
 *
 * The class provides a number of utility methods for accessing these
 * values in ways that are useful to data reduction routines.
 *
 * @author Steve Jones
 *
 */
public class MeasurementsWithSensorValues extends
  TreeMap<Measurement, HashMap<SensorType, TreeSet<SensorValue>>> {

  /**
   * The instrument that took these measurements. Used for lookups
   */
  private Instrument instrument;

  /**
   * Internal lookup map of measurement IDs to measurement objects
   */
  private HashMap<Long, Measurement> idLookup;


  /**
   * Build a new map for the supplied measurements.
   *
   * Note that an empty or null list will create the object, but
   * it will be unusable.
   *
   * @param instrument The instrument that took the measurements
   * @param measurements The Measurement objects for the dataset
   * @throws MissingParamException If any parameters are missing
   */
  public MeasurementsWithSensorValues(Instrument instrument,
    List<Measurement> measurements) throws MissingParamException {

    super();

    MissingParam.checkMissing(instrument, "instrument");
    MissingParam.checkMissing(measurements, "measurements");

    this.instrument = instrument;

    idLookup = new HashMap<Long, Measurement>();

    if (null != measurements) {
      for (Measurement measurement : measurements) {
        put(measurement, new HashMap<SensorType, TreeSet<SensorValue>>());
        idLookup.put(measurement.getId(), measurement);
      }
    }
  }

  /**
   * Add a SensorValue to the data structure
   * @param measurementID The database ID of the measurement
   * @param sensorValue The SensorValue to add
   * @throws RecordNotFoundException If measurement is not in the data structure
   */
  public void add(long measurementID, SensorValue sensorValue)
    throws RecordNotFoundException {

    SensorType sensorType = instrument.getSensorAssignments().
      getSensorTypeForDBColumn(sensorValue.getColumnId());

    Measurement measurement = idLookup.get(measurementID);
    if (null == measurement) {
      throw new RecordNotFoundException(
        "Measurement ID " + measurementID + " not found");
    }

    HashMap<SensorType, TreeSet<SensorValue>> measurementEntry =
      get(measurement);

    if (!measurementEntry.containsKey(sensorType)) {
      measurementEntry.put(sensorType, new TreeSet<SensorValue>());
    }

    measurementEntry.get(sensorType).add(sensorValue);
  }
}
