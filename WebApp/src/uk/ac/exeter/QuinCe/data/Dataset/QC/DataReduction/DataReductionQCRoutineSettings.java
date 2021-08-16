package uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  private List<SensorType> flaggedSensors;

  /**
   * The routine options
   */
  private Map<String, String> options;

  /**
   * Initialise a new, empty settings object.
   */
  protected DataReductionQCRoutineSettings() {
    this.flaggedSensors = new ArrayList<SensorType>();
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

  protected String getOption(String key) {
    return options.get(key);
  }

  protected Double getDoubleOption(String key) {
    return Double.valueOf(options.get(key));
  }

  protected List<SensorType> getFlaggedSensors() {
    return flaggedSensors;
  }
}
