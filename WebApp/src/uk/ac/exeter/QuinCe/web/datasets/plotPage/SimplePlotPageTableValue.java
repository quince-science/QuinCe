package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.util.Collection;
import java.util.Collections;

import uk.ac.exeter.QuinCe.data.Dataset.Coordinate;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.utils.StringUtils;

public class SimplePlotPageTableValue implements PlotPageTableValue {

  /**
   * The displayed value.
   */
  private final String value;

  /**
   * The raw value.
   */
  private final Object rawValue;

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

  private final char type;

  private Collection<Long> sources;

  /**
   * Constructor for a straightforward nominal value.
   * 
   * @param value
   *          The value.
   * @param flagScheme
   *          The current flag scheme.
   */
  public SimplePlotPageTableValue(String value, FlagScheme flagScheme) {
    this.value = value;
    this.rawValue = value;
    this.qcFlag = flagScheme.getGoodFlag();
    this.qcMessage = null;
    this.flagNeeded = false;
    this.type = NOMINAL_TYPE;
    this.sources = null;
  }

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
  public SimplePlotPageTableValue(String value, Flag qcFlag, String qcMessage,
    boolean flagNeeded, char type, Collection<Long> sources) {
    this.value = StringUtils.formatNumber(value);
    this.rawValue = value;
    this.qcFlag = qcFlag;
    this.qcMessage = qcMessage;
    this.flagNeeded = flagNeeded;
    this.type = type;
    this.sources = sources;
  }

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
  public SimplePlotPageTableValue(String value, Flag qcFlag, String qcMessage,
    boolean flagNeeded, char type, Long source) {
    this.value = StringUtils.formatNumber(value);
    this.rawValue = value;
    this.qcFlag = qcFlag;
    this.qcMessage = qcMessage;
    this.flagNeeded = flagNeeded;
    this.type = type;
    this.sources = Collections.singleton(source);
  }

  /**
   * Constructor for a {@link Coordinate}.
   *
   * @param coordinate
   *          The coordinate.
   */
  public SimplePlotPageTableValue(Coordinate coordinate,
    FlagScheme flagScheme) {
    this.rawValue = coordinate;
    this.value = coordinate.toString();
    this.qcFlag = flagScheme.getGoodFlag();
    this.qcMessage = "";
    this.flagNeeded = false;
    this.type = PlotPageTableValue.MEASURED_TYPE;
    this.sources = null;
  }

  /**
   * Get the value string.
   *
   * @return The value.
   */
  @Override
  public String getValue() {
    return value;
  }

  @Override
  public Object getRawValue() {
    return rawValue;
  }

  /**
   * Get the QC flag.
   *
   * @return The QC flag.
   */
  @Override
  public Flag getQcFlag(DatasetSensorValues allSensorValues) {
    return qcFlag;
  }

  /**
   * Get the QC message.
   *
   * @return The QC message.
   */
  @Override
  public String getQcMessage(DatasetSensorValues allSensorValues,
    boolean replaceNewlines) {
    return replaceNewlines ? StringUtils.replaceNewlines(qcMessage) : qcMessage;
  }

  @Override
  public boolean getFlagNeeded() {
    return flagNeeded;
  }

  @Override
  public long getId() {
    return -1L;
  }

  @Override
  public boolean isNull() {
    return null == value;
  }

  @Override
  public char getType() {
    return type;
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public Collection<Long> getSources() {
    return sources;
  }
}
