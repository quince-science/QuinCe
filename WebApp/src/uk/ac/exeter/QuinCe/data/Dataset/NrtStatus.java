package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.Duration;
import java.time.LocalDateTime;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

public class NrtStatus {

  private final Instrument instrument;

  private final LocalDateTime createdDate;

  private final LocalDateTime lastRecord;

  private final int status;

  private final LocalDateTime statusDate;

  public NrtStatus(Instrument instrument, LocalDateTime createdDate,
    LocalDateTime lastRecord, int status, LocalDateTime statusDate) {
    this.instrument = instrument;
    this.createdDate = createdDate;
    this.lastRecord = lastRecord;
    this.status = status;
    this.statusDate = statusDate;
  }

  public String getInstrument() {
    return instrument.getName();
  }

  public String getPlatformCode() {
    return instrument.getPlatformCode();
  }

  public String getCreatedDate() {
    return DateTimeUtils.formatDateTime(createdDate);
  }

  public String getLastRecordDate() {
    return DateTimeUtils.formatDateTime(lastRecord);
  }

  public String getStatus() {
    return DataSet.getStatusName(status);
  }

  public String getStatusDate() {
    return DateTimeUtils.formatDateTime(statusDate);
  }

  public long getCreatedDelay() {
    return Duration.between(lastRecord, createdDate).toMinutes();
  }

  public long getStatusDelay() {
    return Duration.between(lastRecord, statusDate).toMinutes();
  }
}
