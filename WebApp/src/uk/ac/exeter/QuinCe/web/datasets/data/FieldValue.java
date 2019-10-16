package uk.ac.exeter.QuinCe.web.datasets.data;

import java.util.LinkedHashMap;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.AutoQCResult;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.RoutineException;

public class FieldValue {

  /**
   * The sensor value's database ID
   */
  private long valueId;

  /**
   * The measured value
   */
  protected Double value;

  /**
   * The QC flag - either the user QC flag, or if that is NEEDS FLAG, the auto
   * QC flag
   */
  protected Flag qcFlag;

  /**
   * Indicates whether the user must verify the automatic QC
   */
  protected boolean needsFlag;

  /**
   * The QC comment
   */
  private String qcComment;

  /**
   * Indicates whether or not this value is used in any calculations
   */
  private boolean used;

  /**
   * Indicates whether or not this is a ghost value.
   *
   * <p>
   * Some values are stored in the database, but never used in any calculations
   * or data. The most common purpose for these for data collected during the
   * flushing period for an instrument. The data in themselves are of no use,
   * but it can be useful to view them for QC purposes.
   * </p>
   *
   * <p>
   * Setting a value's {@code ghost} flag implies that {@link #used} is
   * {@code false}.
   * </p>
   */
  private boolean ghost = false;

  /**
   * Constructor to directly set all values, with the {@link #ghost} flag set to
   * the default {@code false}.
   *
   * @param valueId
   *          The value's database ID
   * @param value
   *          The measured value
   * @param qcFlag
   *          The QC Flag
   * @param needsFlag
   *          Indicates whether the user must verify automatic QC
   * @param qcComment
   *          The QC comment
   * @param used
   *          Indicates whether or not the value is used in any calculations
   */
  public FieldValue(long valueId, Double value, Flag qcFlag, boolean needsFlag,
    String qcComment, boolean used) {

    this.valueId = valueId;
    if (null == value) {
      this.value = Double.NaN;
    } else {
      this.value = value;
    }
    this.qcFlag = qcFlag;
    this.needsFlag = needsFlag;
    this.qcComment = qcComment;
    this.used = used;
    this.ghost = false;
  }

  /**
   * Constructor with an explicit {@link #ghost} flag.
   *
   * @param valueId
   *          The value's database ID
   * @param value
   *          The measured value
   * @param qcFlag
   *          The QC Flag
   * @param needsFlag
   *          Indicates whether the user must verify automatic QC
   * @param qcComment
   *          The QC comment
   * @param used
   *          Indicates whether or not the value is used in any calculations
   * @param ghost
   *          The ghost flag
   */
  public FieldValue(long valueId, Double value, Flag qcFlag, boolean needsFlag,
    String qcComment, boolean used, boolean ghost) {

    this.valueId = valueId;
    if (null == value) {
      this.value = Double.NaN;
    } else {
      this.value = value;
    }
    this.qcFlag = qcFlag;
    this.needsFlag = needsFlag;
    this.qcComment = qcComment;
    this.used = used;
    this.ghost = ghost;
  }

  /**
   * Constructor with logic for setting status from QC results.
   *
   * @param valueId
   *          The value's database ID
   * @param value
   *          The measured value
   * @param autoQC
   *          The automatic QC result
   * @param userQCFlag
   *          The user QC flag
   * @param qcComment
   *          The user QC comment
   * @param used
   *          Indicates whether or not the value is used in any calculations
   * @throws RoutineException
   *           If the automatic QC result cannot be processed
   */
  public FieldValue(long valueId, Double value, AutoQCResult autoQC,
    Flag userQCFlag, String qcComment, boolean used) throws RoutineException {

    this.valueId = valueId;
    if (null == value) {
      this.value = Double.NaN;
    } else {
      this.value = value;
    }

    if (userQCFlag.equals(Flag.NEEDED)) {
      this.qcFlag = autoQC.getOverallFlag();
      this.needsFlag = true;
      this.qcComment = autoQC.getAllMessages();
    } else {
      this.qcFlag = userQCFlag;
      this.needsFlag = false;
      this.qcComment = qcComment;
    }

    this.used = used;
    this.ghost = false;
  }

  /**
   * Constructor with logic for setting status from QC results, and an explicit
   * {@link #ghost} flag.
   *
   * @param valueId
   *          The value's database ID
   * @param value
   *          The measured value
   * @param autoQC
   *          The automatic QC result
   * @param userQCFlag
   *          The user QC flag
   * @param qcComment
   *          The user QC comment
   * @param used
   *          Indicates whether or not the value is used in any calculations
   * @param ghost
   *          The ghost flag
   * @throws RoutineException
   *           If the automatic QC result cannot be processed
   */
  public FieldValue(long valueId, Double value, AutoQCResult autoQC,
    Flag userQCFlag, String qcComment, boolean used, boolean ghost)
    throws RoutineException {

    this.valueId = valueId;
    if (null == value) {
      this.value = Double.NaN;
    } else {
      this.value = value;
    }

    if (userQCFlag.equals(Flag.NEEDED)) {
      this.qcFlag = autoQC.getOverallFlag();
      this.needsFlag = true;
      this.qcComment = autoQC.getAllMessages();
    } else {
      this.qcFlag = userQCFlag;
      this.needsFlag = false;
      this.qcComment = qcComment;
    }

    this.used = used;
    this.ghost = ghost;
    if (ghost) {
      this.used = false;
    }
  }

  /**
   * Copy constructor
   *
   * @param init
   *          The source FieldValue
   */
  public FieldValue(FieldValue init) {
    valueId = init.valueId;
    value = init.value;
    qcFlag = init.qcFlag;
    needsFlag = init.needsFlag;
    qcComment = init.qcComment;
    used = init.used;
  }

  /**
   * Initialise a map of id -> FieldValue for a list of IDs, maintaining the
   * same order of IDs. Entries in the map are null
   *
   * @param ids
   *          The IDs
   * @return The map
   */
  public static LinkedHashMap<Long, FieldValue> initMap(List<Long> ids) {
    LinkedHashMap<Long, FieldValue> map = new LinkedHashMap<Long, FieldValue>();
    for (Long id : ids) {
      map.put(id, null);
    }

    return map;
  }

  public long getValueId() {
    return valueId;
  }

  public Double getValue() {
    return value;
  }

  public Flag getQcFlag() {
    return qcFlag;
  }

  public void setQcFlag(Flag flag) {
    qcFlag = flag;
  }

  public boolean needsFlag() {
    return needsFlag;
  }

  public String getQcComment() {
    return qcComment;
  }

  public void setQcComment(String comment) {
    qcComment = comment;
  }

  public boolean isUsed() {
    return used;
  }

  public boolean isNaN() {
    return value.isNaN();
  }

  public void setNeedsFlag(boolean needsFlag) {
    this.needsFlag = needsFlag;
  }

  public boolean isGhost() {
    return ghost;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
