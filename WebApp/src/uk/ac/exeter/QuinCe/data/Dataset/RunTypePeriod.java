package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;

public class RunTypePeriod {

  private String runType;

  private LocalDateTime start;

  private LocalDateTime end;

  protected RunTypePeriod(String runType, LocalDateTime start) {
    this.runType = runType;
    this.start = start;
    this.end = start;
  }

  public String getRunType() {
    return runType;
  }

  public LocalDateTime getStart() {
    return start;
  }

  public LocalDateTime getEnd() {
    return end;
  }

  protected void setEnd(LocalDateTime end) {
    this.end = end;
  }

  public boolean encompasses(LocalDateTime time) {
    boolean result = false;
    if (start.equals(end)) {
      result = time.equals(start);
    } else {
      boolean afterStart = false;
      boolean beforeEnd = false;

      if (time.equals(start) || time.isAfter(start)) {
        afterStart = true;
      }

      if (time.equals(end) || time.isBefore(end)) {
        beforeEnd = true;
      }

      result = afterStart && beforeEnd;
    }

    return result;
  }

}
