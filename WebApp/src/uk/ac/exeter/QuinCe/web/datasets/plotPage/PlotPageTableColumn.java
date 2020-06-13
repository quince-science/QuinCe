package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.time.LocalDateTime;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.RoutineException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * A basic object representing a column in the plot page table.
 *
 * @see PlotPageTableRecord
 * @author Steve Jones
 *
 */
public class PlotPageTableColumn {

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
    this.value = StringUtils.formatNumber(value);
    this.used = used;
    this.qcFlag = qcFlag;
    this.qcMessage = qcMessage;
    this.flagNeeded = flagNeeded;
  }

  /**
   * Constructor for a timestamp.
   *
   * @param time
   *          The timestamp.
   */
  public PlotPageTableColumn(LocalDateTime time, boolean milliseconds) {

    if (milliseconds) {
      this.value = String.valueOf(DateTimeUtils.dateToLong(time));
    } else {
      this.value = DateTimeUtils.formatDateTime(time);
    }

    this.used = true;
    this.qcFlag = Flag.GOOD;
    this.qcMessage = "";
    this.flagNeeded = false;
  }

  /**
   * Builds a {@link PlotPageTableColumn} from a {@link SensorValue}.
   *
   * @param sensorValue
   *          The {@link SensorValue}.
   * @param used
   *          Whether the value is used in a calculation.
   * @throws RoutineException
   *           If the QC message cannot be extracted.
   */
  public PlotPageTableColumn(SensorValue sensorValue, boolean used)
    throws RoutineException {

    this.value = sensorValue.getValue();
    this.used = used;
    this.qcFlag = sensorValue.getDisplayFlag();
    this.qcMessage = sensorValue.getDisplayQCMessage();
    this.flagNeeded = sensorValue.flagNeeded();
  }

  /**
   * Create a column value for a parameter from a {@link DataReductionRecord}.
   *
   * @param record
   *          The data reduction record.
   * @param parameterName
   *          The parameter.
   */
  public PlotPageTableColumn(DataReductionRecord record, String parameterName) {
    this.value = String.valueOf(record.getCalculationValue(parameterName));
    this.used = false;
    this.qcFlag = record.getQCFlag();
    this.qcMessage = StringUtils.collectionToDelimited(record.getQCMessages(),
      ";");
    this.flagNeeded = qcFlag.equals(Flag.NEEDED);
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
