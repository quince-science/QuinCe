package uk.ac.exeter.QuinCe.data.Dataset.QC.DataReductionRoutines;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

/**
 * Holds the settings for a {@link DataReductionQCRoutine}.
 *
 * @author Steve Jones
 *
 */
public class DataReductionQCRoutineSettings {

  /**
   * The sensors that should be flagged by the routine
   */
  private TreeSet<SensorType> flaggedSensors;

  /**
   * The routine options
   */
  private Map<String, String> options;

  /**
   * Initialise a new, empty settings object.
   */
  protected DataReductionQCRoutineSettings() {
    this.flaggedSensors = new TreeSet<SensorType>();
    this.options = new HashMap<String, String>();
  }

  /**
   * Add a flagged {@link SensorType}.
   *
   * @param sensorType
   *          The SensorType
   */
  protected void addFlaggedSensor(SensorType sensorType) {
    flaggedSensors.add(sensorType);
  }

  protected void addOption(String key, String value) {
    options.put(key, value);
  }
}
