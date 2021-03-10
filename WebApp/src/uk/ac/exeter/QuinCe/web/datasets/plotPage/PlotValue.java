package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.time.LocalDateTime;
import java.util.Objects;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;

class PlotValue implements Comparable<PlotValue> {

  /**
   * The value ID (typically the timestamp)
   */
  private final long id;

  /**
   * The x value as a timestamp
   */
  private final LocalDateTime xTime;

  /**
   * The x value
   */
  private final Double xDouble;

  /**
   * The y value
   */
  private final Double y;

  /**
   * Indicates whether or not this is a ghost value
   */
  private final boolean ghost;

  /**
   * The y value's QC flag
   */
  private Flag flag;

  /**
   * Constructor for all fields.
   *
   * @param id
   *          The value ID (typically the timestamp).
   * @param x
   *          The x value.
   * @param y
   *          The y value.
   * @param ghost
   *          Indicates whether or not this is a ghost value.
   * @param flag
   *          The y value's QC flag.
   */
  protected PlotValue(long id, Double x, Double y, boolean ghost, Flag flag) {
    this.id = id;
    this.xDouble = x;
    this.xTime = null;
    this.y = y;
    this.ghost = ghost;
    this.flag = flag;
  }

  /**
   * Constructor for all fields.
   *
   * @param id
   *          The value ID (typically the timestamp).
   * @param x
   *          The x value.
   * @param y
   *          The y value.
   * @param ghost
   *          Indicates whether or not this is a ghost value.
   * @param flag
   *          The y value's QC flag.
   */
  protected PlotValue(long id, LocalDateTime x, Double y, boolean ghost,
    Flag flag) {
    this.id = id;
    this.xDouble = 0D;
    this.xTime = x;
    this.y = y;
    this.ghost = ghost;
    this.flag = flag;
  }

  @Override
  public int compareTo(PlotValue o) {

    // Compare by x value first (for ordering in plot)
    // followed by id
    // Use time if both are times. Else double value.
    // Weird shit will happen if you compare a time to a non-time.
    int result;

    if (xIsTime() && o.xIsTime()) {
      result = xTime.compareTo(o.xTime);
    } else {
      if (null == xDouble && null == o.xDouble) {
        result = 0;
      } else if (null == xDouble) {
        result = -1;
      } else if (null == o.xDouble) {
        result = 1;
      } else {
        result = Double.compare(xDouble, o.xDouble);
      }
    }

    if (result == 0) {
      result = Long.compare(id, o.id);
    }

    return result;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof PlotValue))
      return false;
    PlotValue other = (PlotValue) obj;
    return Objects.equals(id, other.id);
  }

  public Double getXDouble() {
    return xDouble;
  }

  public LocalDateTime getXTime() {
    return xTime;
  }

  public Double getY() {
    return y;
  }

  public long getId() {
    return id;
  }

  public boolean isGhost() {
    return ghost;
  }

  public Flag getFlag() {
    return flag;
  }

  /**
   * Determines whether or not this value should be included in the Flags plot.
   *
   * <p>
   * Values are in the Flags plot if their flag is {@link Flag#BAD},
   * {@link Flag#QUESTIONABLE}, or {@link Flag#NEEDED}.
   * </p>
   *
   * @return {@code true} if the value should appear in the Flags plot;
   *         {@code false} otherwise.
   */
  public boolean inFlagPlot() {
    return (!xDouble.isNaN() && !y.isNaN() && (flag.equals(Flag.BAD)
      || flag.equals(Flag.QUESTIONABLE) || flag.equals(Flag.NEEDED)));
  }

  /**
   * Indicates whether or not the X Axis is time.
   *
   * <p>
   * This is used by the JSON serializers to determine how to build the JSON.
   *
   * @return {@code true} if the X axis is time. {@code false} otherwise.
   */
  public boolean xIsTime() {
    return null != xTime;
  }
}
