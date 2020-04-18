package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;

/**
 * A basic object representing a column in the plot page table.
 *
 * @see PlotPageTableRecord
 * @author Steve Jones
 *
 */
class PlotPageTableColumn {

  /**
   * The displayed value.
   */
  private final String value;

  /**
   * Indicates whether or not the value is used in a calculation.
   */
  private final boolean used;

  /**
   * The value's QC flag.
   */
  private final Flag qcFlag;

  /**
   * The QC message.
   */
  private final String qcMessage;

  /**
   * Indicates whether or not user QC is required.
   */
  private final boolean flagNeeded;

  /**
   * Simple constructor with all values.
   *
   * @param value
   *          The value.
   * @param used
   *          Whether the value is used in a calculation.
   * @param qcFlag
   *          The QC flag.
   * @param qcMessage
   *          The QC message.
   * @param flagNeeded
   *          Whether or not user QC is required.
   */
  public PlotPageTableColumn(String value, boolean used, Flag qcFlag,
    String qcMessage, boolean flagNeeded) {
    this.value = value;
    this.used = used;
    this.qcFlag = qcFlag;
    this.qcMessage = qcMessage;
    this.flagNeeded = flagNeeded;
  }

  /**
   * Get the value string.
   *
   * @return The value.
   */
  public String getValue() {
    return value;
  }

  /**
   * Get the flag indicating whether the value is used in a calculation.
   *
   * @return The used flag.
   */
  public boolean getUsed() {
    return used;
  }

  /**
   * Get the QC flag.
   *
   * @return The QC flag.
   */
  public Flag getQcFlag() {
    return qcFlag;
  }

  /**
   * Get the QC message.
   *
   * @return The QC message.
   */
  public String getQcMessage() {
    return qcMessage;
  }

  /**
   * Get the flag indicating whether user QC is needed.
   *
   * @return The user QC flag.
   */
  public boolean getFlagNeeded() {
    return flagNeeded;
  }

}
