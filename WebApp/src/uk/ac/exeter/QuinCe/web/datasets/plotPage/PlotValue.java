package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.util.Objects;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;

class PlotValue implements Comparable<PlotValue> {

  /**
   * The value ID (typically the timestamp)
   */
  private final String id;

  /**
   * The x value
   */
  private final double x;

  /**
   * The y value
   */
  private final double y;

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
  protected PlotValue(String id, double x, double y, boolean ghost, Flag flag) {
    this.id = id;
    this.x = x;
    this.y = y;
    this.ghost = ghost;
    this.flag = flag;
  }

  /**
   * Constructor for a value that is not a ghost.
   *
   * @param id
   *          The value ID (typically the timestamp).
   * @param x
   *          The x value.
   * @param y
   *          The y value.
   * @param flag
   *          The y value's QC flag.
   */
  protected PlotValue(String id, double x, double y, Flag flag) {
    this.id = id;
    this.x = x;
    this.y = y;
    this.ghost = false;
    this.flag = flag;
  }

  @Override
  public int compareTo(PlotValue o) {

    // Compare by x value first (for ordering in plot)
    // followed by id
    int result = Double.compare(x, o.x);
    if (result == 0) {
      result = id.compareTo(o.id);
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

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public String getId() {
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
    return (flag.equals(Flag.BAD) || flag.equals(Flag.QUESTIONABLE)
      || flag.equals(Flag.NEEDED));
  }
}
