package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.NotImplementedException;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.Message;

/**
 * {#link DataSet} implementation for time basis.
 *
 * <p>
 * The start and end of the dataset are the start time and end time. Therefore
 * this class does not use the {@code startTime} and {@code endTime} properties
 * as in the default implementation from {@link DataSet}, but stores them
 * directly in {@link DataSet#start} and {@link DataSet#end}.
 */
public class TimeDataSet extends DataSet {

  private LocalDateTime startTime;

  private LocalDateTime endTime;

  public TimeDataSet(Instrument instrument) {
    super(instrument);
  }

  public TimeDataSet(Instrument instrument, String name,
    LocalDateTime startTime, LocalDateTime endTime, boolean nrt) {
    super(instrument, name, null, null, nrt);
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public TimeDataSet(long id, Instrument instrument, String name, String start,
    String end, int status, LocalDateTime statusDate, boolean nrt,
    Map<String, Properties> properties, SensorOffsets sensorOffsets,
    LocalDateTime createdDate, LocalDateTime lastTouched,
    List<Message> errorMessages, DatasetProcessingMessages processingMessages,
    DatasetUserMessages userMessages, double minLon, double minLat,
    double maxLon, double maxLat, boolean exported) {

    super(id, instrument, name, null, null, status, statusDate, nrt, properties,
      sensorOffsets, createdDate, lastTouched, errorMessages,
      processingMessages, userMessages, minLon, minLat, maxLon, maxLat,
      exported);

    this.startTime = DateTimeUtils.longToDate(Long.parseLong(start));
    this.endTime = DateTimeUtils.longToDate(Long.parseLong(end));
  }

  @Override
  public String getStart() {
    return String.valueOf(DateTimeUtils.dateToLong(startTime));
  }

  @Override
  public void setStart(String start) {
    throw new NotImplementedException("setStartTime must be used");
  }

  @Override
  public String getDisplayStart() {
    return DateTimeUtils.formatDateTime(startTime);
  }

  @Override
  public String getEnd() {
    return String.valueOf(DateTimeUtils.dateToLong(endTime));
  }

  @Override
  public void setEnd(String end) {
    throw new NotImplementedException("setEndTime must be used");
  }

  @Override
  public String getDisplayEnd() {
    return DateTimeUtils.formatDateTime(endTime);
  }

  @Override
  protected int compareToWorker(DataSet o) {
    return this.startTime.compareTo(((TimeDataSet) o).startTime);
  }

  @Override
  public LocalDateTime getStartTime() {
    return startTime;
  }

  @Override
  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  @Override
  public LocalDateTime getEndTime() {
    return endTime;
  }

  @Override
  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public static boolean overlap(TimeDataSet ds1, TimeDataSet ds2) {
    return DateTimeUtils.overlap(ds1.getStartTime(), ds1.getEndTime(),
      ds2.getStartTime(), ds2.getEndTime());
  }
}
