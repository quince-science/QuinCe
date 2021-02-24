package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;

/**
 * Represents a column in the plot page table.
 *
 * @see PlotPageTableRecord
 * @author Steve Jones
 *
 */
public interface PlotPageTableValue {

  public static final char MEASURED_TYPE = 'M';

  public static final char INTERPOLATED_TYPE = 'I';

  public static final char DATA_REDUCTION_TYPE = 'R';

  // For fixed values, e.g. position/depth that isn't measured
  public static final char NOMINAL_TYPE = 'N';

  /**
   * Get the unique ID for this column value.
   *
   * @return The column value's ID.
   */
  public long getId();

  /**
   * Get the value string.
   *
   * @return The value.
   */
  public String getValue();

  /**
   * Get the QC flag.
   *
   * @return The QC flag.
   */
  public Flag getQcFlag();

  /**
   * Get the QC message.
   *
   * @return The QC message.
   */
  public String getQcMessage();

  /**
   * Get the flag indicating whether user QC is needed.
   *
   * @return The user QC flag.
   */
  public boolean getFlagNeeded();

  /**
   * See if this value is null.
   */
  public boolean isNull();

  /**
   * Get the type of value (measured, interpolated etc)
   *
   * @return
   */
  public char getType();
}
