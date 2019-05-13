package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Class to hold the values to be used in a calculation. Generated
 * by a Data Reducer to create a single value for each required sensor type,
 * having performed averaging/interpolation as required.
 *
 * The class also holds a QC flag and message, indicating the most significant
 * flag gathered from all the source sensor values.
 *
 * @author Steve Jones
 *
 */
public class CalculationInputValues {

  /**
   * The sensor type values to be used for the calculation
   */
  private HashMap<String, Double> values;

  /**
   * The most significant QC flag for each sensor type
   */
  private HashMap<String, Flag> qcFlags;

  /**
   * The QC messages for each sensor type
   */
  private HashMap<String, List<String>> qcMessages;

  /**
   * Simple constructor
   */
  protected CalculationInputValues() {
    this.values = new HashMap<String, Double>();
    this.qcFlags = new HashMap<String, Flag>();
    this.qcMessages = new HashMap<String, List<String>>();
  }

  /**
   * Set the value and QC details for a sensor type
   * @param sensorType The sensor type name
   * @param value The value
   * @param qcFlag The QC flag
   * @param qcMessage The QC messages
   */
  public void put(String sensorType, Double value, Flag qcFlag, List<String> qcMessages) {

    values.put(sensorType, value);
    Flag existingQCFlag = qcFlags.get(sensorType);
    if (qcFlag.moreSignificantThan(existingQCFlag)) {
      qcFlags.put(sensorType, qcFlag);
      this.qcMessages.put(sensorType, qcMessages);
    }
  }

  /**
   * Set the value and QC details for a sensor type with a single QC message
   * @param sensorType The sensor type name
   * @param value The value
   * @param qcFlag The QC flag
   * @param qcMessage The QC messages
   */
  public void put(String sensorType, Double value, Flag qcFlag, String qcMessage) {
    List<String> messageList = new ArrayList<String>(1);
    messageList.add(qcMessage);
    put(sensorType, value, qcFlag, messageList);
  }

  /**
   * Get a list of the sensor types that have a NaN value. Returns
   * an empty list if there are no NaN values
   * @return The sensor types with NaN value
   */
  public List<String> getNaNs() {
    List<String> nanSensorTypes = new ArrayList<String>();

    for (Map.Entry<String, Double> entry : values.entrySet()) {
      if (entry.getValue().isNaN()) {
        nanSensorTypes.add(entry.getKey());
      }
    }

    return nanSensorTypes;
  }

  @Override
  public String toString() {
    StringBuilder string = new StringBuilder();

    for (String sensorType : values.keySet()) {
      string.append(sensorType);
      string.append(": ");
      string.append(values.get(sensorType));
      string.append(", Flag ");
      string.append(qcFlags.get(sensorType));
      string.append(": ");
      string.append(StringUtils.collectionToDelimited(qcMessages.get(sensorType)));
      string.append('\n');
    }

    return string.toString();
  }
}
