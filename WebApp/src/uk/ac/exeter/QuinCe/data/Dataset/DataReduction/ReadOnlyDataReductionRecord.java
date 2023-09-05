package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;

import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.utils.NoEmptyStringSet;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * A read-only version of a {@link DataReductionRecord}.
 *
 * <p>
 * The value of this record cannot be changed. The QC flag can be overridden if
 * desired for customisation (mostly during export).
 * </p>
 *
 * <p>
 * Changes to QC flags can be stored using
 * {@link DataSetDataDB#storeDataReductionQC}. Attempting to store changes to
 * the actual data values will result in an Exception.
 * </p>
 */
public class ReadOnlyDataReductionRecord extends DataReductionRecord {

  /**
   * A QC flag that can be used to override the flag from the original record.
   *
   * <p>
   * Calling {@link #getQCFlag()} will return this {@link Flag} if it is set,
   * completely overriding the original QC flag on the record. The normal
   * precedence of QC flags (where {@link Flag#BAD} supersedes
   * {@link Flag#GOOD}) does not apply.
   * </p>
   */
  private Flag overrideQCFlag = null;

  /**
   * A set of QC messages that will override those stored in the original
   * record.
   *
   * <p>
   * If this list is populated, any QC messages on the original record will be
   * replaced by these when {@link #getQCMessages()} is called.
   * </p>
   */
  private NoEmptyStringSet overrideQcMessages = null;

  /**
   * Create a record object with the specified values.
   *
   * @param measurementId
   *          The measurement's database ID.
   * @param variableId
   *          The ID of the variable that that the measurement refers to.
   * @param calculationValues
   *          The values calculated during data reduction.
   * @param qcFlag
   *          The record's QC flag.
   * @param qcMessage
   *          The record's QC message. If the message is a combination of
   *          multiple messages (separated by {@code ;}) they will be split into
   *          separate messages.
   * @return A record object.
   */
  public static ReadOnlyDataReductionRecord makeRecord(long measurementId,
    long variableId, Map<String, Double> calculationValues, Flag qcFlag,
    String qcMessage) {

    List<String> parameterNames = new ArrayList<String>();
    calculationValues.keySet().forEach(parameterNames::add);

    ReadOnlyDataReductionRecord record = new ReadOnlyDataReductionRecord(
      measurementId, variableId, parameterNames, calculationValues, qcFlag,
      new NoEmptyStringSet(StringUtils.delimitedToList(qcMessage, ";")));

    return record;
  }

  /**
   * Internal constructor for {@link ReadOnlyDataReductionRecord} objects. Use
   * {@link #makeRecord(long, long, Map, Flag, String)} to create instances of
   * this class.
   *
   * @param measurementId
   *          The measurement's database ID.
   * @param variableId
   *          The ID of the variable that that the measurement refers to.
   * @param calculationValues
   *          The values calculated during data reduction.
   * @param qcFlag
   *          The record's QC flag.
   * @param qcMessage
   *          The record's QC message. If the message is a combination of
   *          multiple messages (separated by {@code ;}) they will be split into
   *          separate messages.
   */
  private ReadOnlyDataReductionRecord(long measurementId, long variableId,
    List<String> parameterNames, Map<String, Double> calculationValues,
    Flag qcFlag, NoEmptyStringSet qcMessages) {
    super(measurementId, variableId, parameterNames, calculationValues, qcFlag,
      qcMessages);
  }

  @Override
  public void put(String parameter, Double value)
    throws DataReductionException {
    throw new NotImplementedException("This record is read only");
  }

  @Override
  public void setQc(Flag flag, String message) throws DataReductionException {
    setQc(flag, Arrays.asList(new String[] { message }));
  }

  /**
   * Sets the override QC flag and message(s).
   *
   * @see #overrideQCFlag
   * @see #overrideQcMessages
   */
  @Override
  public void setQc(Flag flag, List<String> messages)
    throws DataReductionException {

    NoEmptyStringSet filteredMessages = new NoEmptyStringSet(messages);
    if (!flag.isGood() && filteredMessages.size() == 0) {
      throw new DataReductionException("Empty QC message not allowed");
    }

    if (null == overrideQCFlag) {
      if (!flag.lessSignificantThan(super.getQCFlag())) {
        overrideQCFlag = flag;
        overrideQcMessages = filteredMessages;

      }
    } else {
      if (flag.equalSignificance(overrideQCFlag)) {
        overrideQcMessages.addAll(filteredMessages);
      } else if (flag.moreSignificantThan(overrideQCFlag)) {
        overrideQCFlag = flag;
        overrideQcMessages = filteredMessages;
      }
    }
  }

  /**
   * Get the QC flag for the record, or the override flag if that is set and is
   * more significant than the original flag.
   */
  @Override
  public Flag getQCFlag() {
    Flag result;

    if (null != overrideQCFlag
      && overrideQCFlag.moreSignificantThan(super.getQCFlag())) {
      result = overrideQCFlag;
    } else {
      result = super.getQCFlag();
    }

    return result;
  }

  /**
   * The the QC messages for the record, or the override messages if they are
   * set.
   */
  @Override
  public Set<String> getQCMessages() {

    Set<String> result = super.getQCMessages();

    if (null != overrideQCFlag) {
      if (overrideQCFlag.moreSignificantThan(super.getQCFlag())) {
        result = overrideQcMessages;
      } else if (overrideQCFlag.equalSignificance(super.getQCFlag())) {
        result.addAll(overrideQcMessages);
      }
    }

    return result;
  }

  /**
   * Determines whether or not the QC flag has been overridden, and therefore
   * the record needs to be saved.
   *
   * @return {@code true} if the record needs saving; {@code false) if not.
   */
  public boolean isDirty() {
    return null != overrideQCFlag;
  }
}
