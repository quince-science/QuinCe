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
   * Get the flag indicating whether the value is used in a calculation.
   *
   * @return The used flag.
   */
  public boolean getUsed();

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
}
