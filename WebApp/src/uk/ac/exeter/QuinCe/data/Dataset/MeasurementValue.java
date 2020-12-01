package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

public class MeasurementValue {

  private final long measurementId;

  private final SensorType sensorType;

  private final long columnId;

  private Long prior;

  private Long post;

  private Flag worstValueFlag = Flag.ASSUMED_GOOD;

  private List<String> qcMessage = new ArrayList<String>();

  public MeasurementValue(Measurement measurement, SensorType sensorType,
    long columnId) {
    this.measurementId = measurement.getId();
    this.sensorType = sensorType;
    this.columnId = columnId;
  }

  public long getMeasurementId() {
    return measurementId;
  }

  public long getColumnId() {
    return columnId;
  }

  public Long getPrior() {
    return prior;
  }

  public Long getPost() {
    return post;
  }

  public boolean hasPost() {
    return post != null;
  }

  public void setValues(SensorValue prior, SensorValue post)
    throws RoutineException {

    if (prior.noValue()) {
      this.prior = null;
    } else {
      this.prior = prior.getId();
      addQC(prior);
    }

    if (null != post && !post.noValue()) {
      this.post = post.getId();
      addQC(post);
    }

    if (null == this.prior && null == this.post) {
      worstValueFlag = Flag.NO_QC;
      qcMessage = new ArrayList<String>(1);
      qcMessage.add("No value");
    }
  }

  private void addQC(SensorValue sensorValue) throws RoutineException {

    if (sensorValue.getUserQCFlag(true).moreSignificantThan(worstValueFlag)) {
      worstValueFlag = sensorValue.getUserQCFlag(true);
    }

    String valueQCMessage = sensorValue.getUserQCMessage(true);

    String messagePrefix = sensorType.getName();

    // The latitude is ignored so we don't need to worry about that.
    if (sensorType.equals(SensorType.LONGITUDE_SENSOR_TYPE)) {
      messagePrefix = "Position";
    }

    if (valueQCMessage.length() > 0) {
      qcMessage.add(messagePrefix + ": " + valueQCMessage);
    }
  }

  public Flag getQcFlag() {
    return worstValueFlag;
  }

  public List<String> getQcMessages() {
    return qcMessage;
  }

  @Override
  public String toString() {
    StringBuilder string = new StringBuilder('[');
    string.append(measurementId);
    string.append('/');
    string.append(columnId);
    string.append(": ");
    string.append(prior);
    string.append('/');
    string.append(post);
    string.append(']');
    return string.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(columnId, measurementId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof MeasurementValue))
      return false;
    MeasurementValue other = (MeasurementValue) obj;
    return columnId == other.columnId && measurementId == other.measurementId;
  }

  public SensorType getSensorType() {
    return sensorType;
  }

  public boolean hasValue() {
    return null != prior || null != post;
  }
}
