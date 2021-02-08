package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

/**
 * Holds the calculated value of a given {@link SensorType} for a measurement.
 * 
 * <p>
 * The calculated value is derived from one or more {@link SensorValue}s,
 * depending on the configuration of the instrument, the relative time of the
 * measurement and available {@link SensorValues}, and whether bad/questionable
 * values are being ignored.
 * </p>
 * 
 * <p>
 * This class implements the most common calculation of a measurement value.
 * Some {@link SensorTypes} require more complex calculations (e.g. xCO2 with
 * standards, which also requires xH2O). These can be implemented by overriding
 * classes.
 * </p>
 * 
 * @author Steve Jones
 *
 */
public class MeasurementValue {

  /**
   * The {@link SensorType} that this measurement value is for.
   */
  private final SensorType sensorType;

  /**
   * The IDs of the {@link SensorValue}s used to calculate the value.
   * 
   * <p>
   * Note that the sensor values may not all belong to the specified
   * {@link #sensorType}: some {@link SensorType}s require calculation based on
   * other sensors (e.g. CO2 requires xH2O).
   * </p>
   */
  private List<Long> sensorValueIds = new ArrayList<Long>();

  /**
   * The IDs of {@link SensorValue}s used to support the value calculation.
   * 
   * <p>
   * This can be used for values that aren't directly used in the calculation,
   * such as those used in calibrations.
   * </p>
   */
  private List<Long> supportingSensorValueIds = new ArrayList<Long>();

  /**
   * The number of calculations used to build this value.
   * 
   * <p>
   * This is useful for combining multiple values, where a weighted mean is
   * often required.
   * </p>
   */
  private int memberCount = 0;

  /**
   * The calculated value for the sensor type.
   */
  private Double calculatedValue = Double.NaN;

  /**
   * The QC flag for this value, derived from the contributing
   * {@link SensorValues}.
   */
  private Flag flag = Flag.ASSUMED_GOOD;

  /**
   * The QC message for this value, derived from the contributing
   * {@link SensorValues}.
   */
  private List<String> qcMessage = new ArrayList<String>();

  /**
   * Creates a stub value with no assigned {@link SensorValue}s.
   * 
   * @param sensorType
   *          The sensor type that the value is calculated for.
   */
  public MeasurementValue(SensorType sensorType) {
    this.sensorType = sensorType;
  }

  /**
   * Create a {@code MeasurementValue} based on the contents of existing
   * {@code MeasurementValue} objects.
   * 
   * @param sensorType
   * @param sourceValues
   */

  /**
   * Create a fully populated {@code MeasurementValue}.
   * 
   * @param sensorType
   *          The sensor type.
   * @param sourceValues
   *          The contributing {@link SensorValue}s.
   * @param calculatedValue
   *          The calculated value.
   * @param memberCount
   *          The member count.
   */
  public MeasurementValue(SensorType sensorType,
    Collection<SensorValue> sensorValues, Double calculatedValue,
    int memberCount) {

    this.sensorType = sensorType;
    this.calculatedValue = calculatedValue;
    this.memberCount = memberCount;
    addSensorValues(sensorValues);
  }

  public void addSensorValues(Collection<SensorValue> values) {
    values.forEach(this::addSensorValue);
  }

  /**
   * Add the sensor values used in the specified {@code MeasurementValue}s.
   * 
   * @param sourceValues
   *          The source {@code MeasurementValue}s.
   */
  public void addSensorValues(Collection<MeasurementValue> sourceValues,
    DatasetSensorValues allSensorValues) {

    sourceValues.forEach(x -> addSensorValues(x, allSensorValues));
  }

  /**
   * Add the sensor values used in the specified {@code MeasurementValue}.
   * 
   * @param sourceValues
   *          The source {@code MeasurementValue}.
   */
  public void addSensorValues(MeasurementValue sourceValue,
    DatasetSensorValues allSensorValues) {
    for (Long sensorValueId : sourceValue.getSensorValueIds()) {
      addSensorValue(allSensorValues.getById(sensorValueId));
    }
  }

  /**
   * Add a {@link SensorValue} to the value.
   * 
   * <p>
   * Adds the {@link SensorValue}'s ID and updates the QC information.
   * </p>
   * 
   * @param value
   *          The value to add.
   */
  public void addSensorValue(SensorValue value) {
    if (!sensorValueIds.contains(value.getId())) {
      sensorValueIds.add(value.getId());

      Flag valueFlag = value.getDisplayFlag();
      if (valueFlag.equals(flag)) {
        if (value.getUserQCMessage().trim().length() > 0) {
          qcMessage.add(value.getUserQCMessage());
        }
      } else if (valueFlag.moreSignificantThan(flag)) {
        flag = valueFlag;
        qcMessage.clear();

        if (value.getUserQCMessage().trim().length() > 0) {
          qcMessage.add(value.getUserQCMessage());
        }
      }
    }
  }

  public void addSupportingSensorValue(SensorValue value) {
    supportingSensorValueIds.add(value.getId());
  }

  public List<Long> getSupportingSensorValueIds() {
    return supportingSensorValueIds;
  }

  public void setCalculatedValue(Double caluclatedValue) {
    this.calculatedValue = caluclatedValue;
  }

  public Double getCalculatedValue() {
    return calculatedValue;
  }

  public Flag getQcFlag() {
    return flag;
  }

  public List<String> getQcMessages() {
    return qcMessage;
  }

  public SensorType getSensorType() {
    return sensorType;
  }

  public boolean hasValue() {
    return sensorValueIds.size() > 0;
  }

  public void incrMemberCount(int count) {
    memberCount += count;
  }

  public void incrMemberCount() {
    memberCount++;
  }

  public void setMemberCount(int memberCount) {
    this.memberCount = memberCount;
  }

  public int getMemberCount() {
    return memberCount;
  }

  public List<Long> getSensorValueIds() {
    return sensorValueIds;
  }

  public void addQC(Flag flag, String comment) {
    if (flag.moreSignificantThan(this.flag)) {
      qcMessage.clear();
      qcMessage.add(comment);
    } else if (flag.equals(this.flag)) {
      qcMessage.add(comment);
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
      + ((calculatedValue == null) ? 0 : calculatedValue.hashCode());
    result = prime * result
      + ((sensorType == null) ? 0 : sensorType.hashCode());
    result = prime * result
      + ((sensorValueIds == null) ? 0 : sensorValueIds.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MeasurementValue other = (MeasurementValue) obj;
    if (calculatedValue == null) {
      if (other.calculatedValue != null)
        return false;
    } else if (!calculatedValue.equals(other.calculatedValue))
      return false;
    if (sensorType == null) {
      if (other.sensorType != null)
        return false;
    } else if (!sensorType.equals(other.sensorType))
      return false;
    if (sensorValueIds == null) {
      if (other.sensorValueIds != null)
        return false;
    } else if (!sensorValueIds.equals(other.sensorValueIds))
      return false;
    return true;
  }
}
