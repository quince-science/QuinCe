package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

/**
 * Class to hold a Caculation Value with its QC information. These are built for
 * a given sensor type from all available values using fallbacks, averaging etc.
 * 
 * @author Steve Jones
 *
 */
public class CalculationValue {

  private final long measurementId;

  private final long variableId;

  private TreeSet<Long> usedSensorValues;

  private Double value;

  private Flag qcFlag;

  private List<String> qcMessages;

  private boolean flagNeeded;

  public CalculationValue(long measurementId, long variableId,
    TreeSet<Long> usedSensorValues, Double value, Flag qcFlag,
    List<String> qcMessages, boolean flagNeeded) {

    this.measurementId = measurementId;
    this.variableId = variableId;
    this.usedSensorValues = usedSensorValues;
    this.value = value;
    this.qcFlag = qcFlag;
    this.qcMessages = qcMessages;
    this.flagNeeded = flagNeeded;
  }

  /**
   * Determine whether or not this value is NaN
   * 
   * @return {@code true} if the value is NaN; {@code false} otherwise
   */
  public boolean isNaN() {
    return value.isNaN();
  }

  /**
   * Get the value
   * 
   * @return The value
   */
  public Double getValue() {
    return value;
  }

  /**
   * Get the QC flag
   * 
   * @return The QC flag
   */
  public Flag getQCFlag() {
    return qcFlag;
  }

  public boolean flagNeeded() {
    return flagNeeded;
  }

  /**
   * Get the QC messages
   * 
   * @return The QC messages
   */
  public List<String> getQCMessages() {
    return qcMessages;
  }

  /**
   * Get the database IDs of the sensor values used to generate this
   * CalculationValue
   * 
   * @return The sensor value IDs
   */
  public TreeSet<Long> getUsedSensorValueIds() {
    return usedSensorValues;
  }

  public long getMeasurementId() {
    return measurementId;
  }

  public long getVariableId() {
    return variableId;
  }

  /**
   * Get the value to be used in data reduction calculations from a given set of
   * sensor values
   * 
   * @param list
   *          The sensor values
   * @return The calculation value
   * @throws RoutineException
   */
  public static CalculationValue get(Measurement measurement,
    SensorType sensorType, List<SensorValue> values) throws RoutineException {

    // TODO Make this more intelligent - handle fallbacks, averages,
    // interpolation etc.
    // For now we're just averaging all the values we get.

    TreeSet<Long> usedSensorValues = new TreeSet<Long>();
    Double finalValue = Double.NaN;
    Flag qcFlag = Flag.ASSUMED_GOOD;
    boolean flagNeeded = false;
    List<String> qcMessages = new ArrayList<String>();

    if (null == values) {
      qcFlag = Flag.BAD;
      qcMessages.add("Missing " + sensorType.getName());
    } else {

      Double valueTotal = 0.0;
      int count = 0;

      for (SensorValue value : values) {
        if (!value.isNaN()) {
          valueTotal += value.getDoubleValue();
          count++;

          usedSensorValues.add(value.getId());

          // Update the QC flag to be applied to the overall value
          Flag flagToCheck = value.getUserQCFlag();
          String qcMessage = value.getUserQCMessage();
          if (value.getUserQCFlag().equals(Flag.NEEDED)) {
            flagNeeded = true;
            flagToCheck = value.getAutoQcFlag();
            qcMessage = value.getAutoQcResult().getAllMessages();
          }

          if (flagToCheck.moreSignificantThan(qcFlag)) {
            qcFlag = flagToCheck;
            qcMessages = new ArrayList<String>();
            qcMessages.add(qcMessage);
          } else if (flagToCheck.equals(qcFlag)) {
            qcMessages.add(qcMessage);
          }
        }
      }

      if (count == 0) {
        qcFlag = Flag.BAD;
        qcMessages = new ArrayList<String>(1);
        qcMessages.add("Missing " + sensorType.getName());
      } else {
        finalValue = valueTotal / count;
      }
    }

    return new CalculationValue(measurement.getId(),
      measurement.getVariable().getId(), usedSensorValues, finalValue, qcFlag,
      qcMessages, flagNeeded);
  }

  /**
   * Sum the value of a number of CalculationValue objects, producing a new
   * object. The {@code usedSensorValues} of the result will be the combined
   * list from both input values. The QC flag will be the most significant flag,
   * and the messages will also be combined.
   *
   * {@code null} values are ignored
   *
   * @param value1
   *          The first value
   * @param value2
   *          The second value
   * @return The summed value
   */
  public static CalculationValue sum(CalculationValue... values) {
    Double sum = 0.0;

    long measurementId = -1L;
    long variableId = -1L;

    for (CalculationValue cv : values) {
      if (null != cv) {
        if (measurementId < 0) {
          measurementId = cv.measurementId;
          variableId = cv.variableId;
        }

        sum += cv.value;
      }
    }

    return makeCombinedCalculationValue(measurementId, variableId, sum, values);
  }

  /**
   * Calculate the mean value of a number of CalculationValue objects, producing
   * a new object. The {@code usedSensorValues} of the result will be the
   * combined list from both input values. The QC flag will be the most
   * significant flag, and the messages will also be combined.
   *
   * {@code null} values are ignored
   *
   * @param value1
   *          The first value
   * @param value2
   *          The second value
   * @return The summed value
   */
  public static CalculationValue mean(CalculationValue... values) {
    Double sum = 0.0;
    int count = 0;

    long measurementId = -1L;
    long variableId = -1L;

    for (CalculationValue cv : values) {
      if (null != cv) {
        if (measurementId < 0) {
          measurementId = cv.measurementId;
          variableId = cv.variableId;
        }

        sum += cv.value;
        count++;
      }
    }

    return makeCombinedCalculationValue(measurementId, variableId, sum / count,
      values);
  }

  /**
   * Create a new CalculationValue by combining the sensor values, QC flag and
   * QC messages from both values and setting the value to that specified
   *
   * @param value1
   *          The first value object
   * @param value2
   *          The second value object
   * @param newValue
   *          The value for the new object
   * @return The combined object
   */
  private static CalculationValue makeCombinedCalculationValue(
    long measurementId, long variableId, Double newValue,
    CalculationValue... valueObjects) {

    TreeSet<Long> newSensorValues = new TreeSet<Long>();
    Flag newQcFlag = Flag.GOOD;
    boolean newFlagNeeded = false;
    List<String> newQcMessages = new ArrayList<String>();

    for (CalculationValue cv : valueObjects) {
      if (null != cv) {
        newSensorValues.addAll(cv.usedSensorValues);
        if (cv.qcFlag.moreSignificantThan(newQcFlag)) {
          newQcFlag = cv.qcFlag;
        }
        newQcMessages.addAll(cv.qcMessages);
        if (cv.flagNeeded) {
          newFlagNeeded = true;
        }
      }
    }

    return new CalculationValue(measurementId, variableId, newSensorValues,
      newValue, newQcFlag, newQcMessages, newFlagNeeded);
  }
}
