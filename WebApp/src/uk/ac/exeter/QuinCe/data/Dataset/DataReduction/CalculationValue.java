package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

/**
 * Class to hold a Caculation Value with its QC information.
 * These are built for a given sensor type from all available values
 * using fallbacks, averaging etc.
 * @author Steve Jones
 *
 */
public class CalculationValue {

  private TreeSet<Long> usedSensorValues;
  
  private Double value;
  
  private Flag qcFlag;
  
  private List<String> qcMessages;
  
  public CalculationValue(TreeSet<Long> usedSensorValues, Double value,
    Flag qcFlag, List<String> qcMessages) {
    
    this.usedSensorValues = usedSensorValues;
    this.value = value;
    this.qcFlag = qcFlag;
    this.qcMessages = qcMessages;
  }
  
  /**
   * Determine whether or not this value is NaN
   * @return {@code true} if the value is NaN; {@code false} otherwise
   */
  public boolean isNaN() {
    return value.isNaN();
  }
  
  /**
   * Get the value
   * @return The value
   */
  public Double getValue() {
    return value;
  }

  /**
   * Get the QC flag
   * @return The QC flag
   */
  public Flag getQCFlag() {
    return qcFlag;
  }

  /**
   * Get the QC messages
   * @return The QC messages
   */
  public List<String> getQCMessages() {
    return qcMessages;
  }
  
  /**
   * Get the value to be used in data reduction calculations from a given
   * set of sensor values
   * @param list The sensor values
   * @return The calculation value
   */
  public static  CalculationValue get(SensorType sensorType,
    List<SensorValue> values) {
    
    // TODO Make this more intelligent - handle fallbacks, averages, interpolation etc.
    // For now we're just averaging all the values we get.
    
    TreeSet<Long> usedSensorValues = new TreeSet<Long>();
    Double finalValue = Double.NaN;
    Flag qcFlag = Flag.ASSUMED_GOOD;
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
          if (value.getUserQCFlag().equals(Flag.NEEDED)) {
            
            if (!qcFlag.equals(Flag.NEEDED)) {
              qcFlag = Flag.NEEDED;
              qcMessages = new ArrayList<String>();
              qcMessages.add("AUTO QC: " + sensorType.getName() + " " + value.getUserQCMessage());
            } else if (value.getUserQCFlag().moreSignificantThan(qcFlag)) {
              qcFlag = value.getUserQCFlag();
              qcMessages = new ArrayList<String>();
              qcMessages.add(value.getUserQCMessage());
            } else if (value.getUserQCFlag().equals(qcFlag)) {
              qcMessages.add(value.getUserQCMessage());
            }
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
    
    return new CalculationValue(usedSensorValues, finalValue, qcFlag, qcMessages);
  }
  
  /**
   * Sum the value of a number of CalculationValue objects, producing a new object.
   * The {@code usedSensorValues} of the result will be the combined list from
   * both input values. The QC flag will be the most significant flag, and the
   * messages will also be combined.
   * 
   * {@code null} values are ignored
   * 
   * @param value1 The first value
   * @param value2 The second value
   * @return The summed value
   */
  public static CalculationValue sum(CalculationValue... values) {
    Double sum = 0.0;
    
    for (CalculationValue cv : values) {
      if (null != cv) {
        sum += cv.value;
      }
    }
    
    return makeCombinedCalculationValue(sum, values);
  }
  
  /**
   * Calculate the mean value of a number of CalculationValue objects, producing a new object.
   * The {@code usedSensorValues} of the result will be the combined list from
   * both input values. The QC flag will be the most significant flag, and the
   * messages will also be combined.
   *
   * {@code null} values are ignored
   * 
   * @param value1 The first value
   * @param value2 The second value
   * @return The summed value
   */
  public static CalculationValue mean(CalculationValue... values) {
    Double sum = 0.0;
    int count = 0;
    
    for (CalculationValue cv : values) {
      if (null != cv) {
        sum += cv.value;
        count++;
      }
    }
    
    return makeCombinedCalculationValue(sum / count, values);
  }
  
  /**
   * Create a new CalculationValue by combining the sensor values, QC flag
   * and QC messages from both values and setting the value to that specified
   * 
   * @param value1 The first value object
   * @param value2 The second value object
   * @param newValue The value for the new object
   * @return The combined object
   */
  private static CalculationValue makeCombinedCalculationValue(
    Double newValue, CalculationValue... valueObjects) {

    TreeSet<Long> newSensorValues = new TreeSet<Long>();
    Flag newQcFlag = Flag.GOOD;
    List<String> newQcMessages = new ArrayList<String>();
    
    for (CalculationValue cv : valueObjects) {
      if (null != cv) {
        newSensorValues.addAll(cv.usedSensorValues);
        if (cv.qcFlag.moreSignificantThan(newQcFlag)) {
          newQcFlag = cv.qcFlag;
        }
        newQcMessages.addAll(cv.qcMessages);
      }
    }
    
    return new CalculationValue(newSensorValues, newValue, newQcFlag, newQcMessages);
  }
}
