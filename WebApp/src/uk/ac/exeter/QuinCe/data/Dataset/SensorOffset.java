package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;

import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * A single sensor offset.
 *
 * <p>
 * The offset is defined in milliseconds to be applied to the second of the
 * sensor group pairs, i.e. the sensor values in the second group should be
 * moved backwards in time by the specified offset.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class SensorOffset implements Comparable<SensorOffset> {

  /**
   * The time of the offset.
   */
  private final LocalDateTime time;

  /**
   * The number of milliseconds to offset by.
   */
  private final long offset;

  public SensorOffset(LocalDateTime time, long offset) {
    this.time = time;
    this.offset = offset;
  }

  public LocalDateTime getTime() {
    return time;
  }

  public long getTimeMilliseconds() {
    return DateTimeUtils.dateToLong(time);
  }

  public long getOffset() {
    return offset;
  }

  public String getOffsetText() {
    double seconds = offset / 1000D;
    return StringUtils.formatNumber(seconds) + " s";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((time == null) ? 0 : time.hashCode());
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
    SensorOffset other = (SensorOffset) obj;
    if (time == null) {
      if (other.time != null)
        return false;
    } else if (!time.equals(other.time))
      return false;
    return true;
  }

  @Override
  public int compareTo(SensorOffset o) {
    return this.time.compareTo(o.time);
  }
}
