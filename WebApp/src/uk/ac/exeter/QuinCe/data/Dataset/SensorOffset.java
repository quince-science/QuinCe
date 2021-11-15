package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;

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
public class SensorOffset {

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

  public long getOffset() {
    return offset;
  }
}
