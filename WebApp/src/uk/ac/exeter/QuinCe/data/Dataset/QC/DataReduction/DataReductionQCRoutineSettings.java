package uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

/**
 * Holds the settings for a {@link DataReductionQCRoutine}.
 */
public class DataReductionQCRoutineSettings {

  /**
   * The sensors that should be flagged by the routine.
   */
  private List<SensorType> flaggedSensors;

  /**
   * The routine options (single values).
   */
  private Map<String, String> singleOptions;

  /**
   * The routine options (list values).
   */
  private Map<String, List<String>> listOptions;

  /**
   * Initialise a new, empty settings object.
   */
  protected DataReductionQCRoutineSettings() {
    this.flaggedSensors = new ArrayList<SensorType>();
    this.singleOptions = new HashMap<String, String>();
    this.listOptions = new HashMap<String, List<String>>();
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

  protected void addSingleOption(String key, String value) {
    singleOptions.put(key, value);
  }

  protected void addListOption(String key, List<String> value) {
    listOptions.put(key, value);
  }

  protected String getOption(String key) {
    return singleOptions.get(key);
  }

  protected Double getDoubleOption(String key) {
    return Double.valueOf(singleOptions.get(key));
  }

  protected List<String> getListOption(String key) {
    return listOptions.get(key);
  }

  protected List<SensorType> getFlaggedSensors() {
    return flaggedSensors;
  }
}
