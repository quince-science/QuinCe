package uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Range;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

/**
 * Holds the settings for a {@link DataReductionQCRoutine}.
 */
public class DataReductionQCRoutineSettings {

  private FlagScheme flagScheme;

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
   * Range limits parsed from the options
   */
  private Map<Flag, Range<Double>> limits;

  /**
   * Initialise a new, empty settings object.
   */
  protected DataReductionQCRoutineSettings(FlagScheme flagScheme) {
    this.flaggedSensors = new ArrayList<SensorType>();
    this.singleOptions = new HashMap<String, String>();
    this.listOptions = new HashMap<String, List<String>>();
    this.limits = new HashMap<Flag, Range<Double>>();
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
    if (!key.startsWith("limit.")) {
      singleOptions.put(key, value);
    } else {
      char flagChar = key.charAt(6);
      Flag flag = flagScheme.getFlag(flagChar);
      Range<Double> range = Range.between(0D, Double.parseDouble(value));
      limits.put(flag, range);
    }
  }

  protected void addListOption(String key, List<String> value) {
    listOptions.put(key, value);
  }

  public String getOption(String key) {
    return singleOptions.get(key);
  }

  public Double getDoubleOption(String key) {
    return Double.valueOf(singleOptions.get(key));
  }

  public List<String> getListOption(String key) {
    return listOptions.get(key);
  }

  public List<SensorType> getFlaggedSensors() {
    return flaggedSensors;
  }

  protected Map<Flag, Range<Double>> getLimits() {
    return limits;
  }
}
