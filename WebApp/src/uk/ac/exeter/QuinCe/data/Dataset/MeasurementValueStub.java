package uk.ac.exeter.QuinCe.data.Dataset;

public class MeasurementValueStub {

  private final long measurementId;

  private final long columnId;

  public MeasurementValueStub(Measurement measurement, long columnId) {

    this.measurementId = measurement.getId();
    this.columnId = columnId;
  }

  public MeasurementValueStub(MeasurementValueStub stub) {
    this.measurementId = stub.measurementId;
    this.columnId = stub.columnId;
  }

  public long getMeasurementId() {
    return measurementId;
  }

  public long getColumnId() {
    return columnId;
  }
}
