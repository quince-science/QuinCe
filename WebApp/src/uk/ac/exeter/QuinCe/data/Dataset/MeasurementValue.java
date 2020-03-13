package uk.ac.exeter.QuinCe.data.Dataset;

public class MeasurementValue extends MeasurementValueStub {

  private long prior;

  private Long post;

  public MeasurementValue(MeasurementValueStub stub) {
    super(stub);
  }

  public long getPrior() {
    return prior;
  }

  public Long getPost() {
    return post;
  }

  public void setValues(SensorValue prior, SensorValue post) {
    this.prior = prior.getId();
    if (null != post) {
      this.post = post.getId();
    }
  }
}
