package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.RoutineException;

public class MeasurementValue {

  private final long measurementId;

  private final long columnId;

  private long prior;

  private Long post;

  private Flag worstValueFlag = Flag.ASSUMED_GOOD;

  private Flag overrideFlag = null;

  private List<String> qcMessage = new ArrayList<String>();

  public MeasurementValue(Measurement measurement, long columnId) {
    this.measurementId = measurement.getId();
    this.columnId = columnId;
  }

  public long getMeasurementId() {
    return measurementId;
  }

  public long getColumnId() {
    return columnId;
  }

  public long getPrior() {
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
    this.prior = prior.getId();
    addQC(prior);
    if (null != post) {
      this.post = post.getId();
      addQC(post);
    }
  }

  private void addQC(SensorValue sensorValue) throws RoutineException {
    if (null != sensorValue.getUserQCFlag()) {
      if (sensorValue.getUserQCFlag().moreSignificantThan(worstValueFlag)) {
        worstValueFlag = sensorValue.getUserQCFlag();
      }

      qcMessage.add(sensorValue.getUserQCMessage());
    } else {
      if (sensorValue.getAutoQcFlag().moreSignificantThan(worstValueFlag)) {
        worstValueFlag = sensorValue.getAutoQcFlag();
      }

      qcMessage.addAll(sensorValue.getAutoQcResult().getAllMessagesList());
    }
  }
}
