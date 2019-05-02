package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;

/**
 * Class to hold a Caculation Value with its QC information.
 * These are built for a given sensor type from all available values
 * using fallbacks, averaging etc.
 * @author Steve Jones
 *
 */
public class CalculationValue {

  private List<Long> usedSensorValues;
  
  private Double value;
  
  private Flag qcFlag;
  
  private List<String> qcMessages;
  
  public CalculationValue(List<Long> usedSensorValues, Double value,
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
}
