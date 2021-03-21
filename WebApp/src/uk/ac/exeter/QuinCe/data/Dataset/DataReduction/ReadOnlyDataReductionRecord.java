package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.utils.NoEmptyStringList;
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
 * Any attempt to save this to the database using
 * {@link DataSetDataDB#storeDataReduction(java.sql.Connection, List)} will
 * result in an Exception.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class ReadOnlyDataReductionRecord extends DataReductionRecord {

  /**
   * A local copy of the QC flag, so it can be overridden
   */
  private Flag overrideQcFlag = null;

  /**
   * A local copy of the QC messages, so they can be overridden
   */
  private NoEmptyStringList overrideQcMessages = null;

  public static ReadOnlyDataReductionRecord makeRecord(long measurementId,
    long variableId, Map<String, Double> calculationValues, Flag qcFlag,
    String qcMessage) {

    List<String> parameterNames = new ArrayList<String>();

    ReadOnlyDataReductionRecord record = new ReadOnlyDataReductionRecord(
      measurementId, variableId, parameterNames, calculationValues, qcFlag,
      new NoEmptyStringList(StringUtils.delimitedToList(qcMessage, ";")));

    return record;
  }

  private ReadOnlyDataReductionRecord(long measurementId, long variableId,
    List<String> parameterNames, Map<String, Double> calculationValues,
    Flag qcFlag, NoEmptyStringList qcMessages) {
    super(measurementId, variableId, parameterNames, calculationValues, qcFlag,
      qcMessages);
  }

  @Override
  protected void put(String parameter, Double value)
    throws DataReductionException {
    throw new NotImplementedException("This record is read only");
  }

  /**
   * This sets the override QC flag. It does not take into account previous QC
   * flag values.
   */
  @Override
  public void setQc(Flag flag, List<String> messages) {
    if (null == overrideQcFlag || flag.moreSignificantThan(overrideQcFlag)) {
      overrideQcFlag = flag;
      overrideQcMessages = new NoEmptyStringList(messages);
    } else if (null != overrideQcFlag && flag.equals(overrideQcFlag)) {
      overrideQcMessages.addAll(messages);
    }
  }

  /**
   * If the override QC flag is set, then that is returned. Otherwise we return
   * the original record's flag
   */
  @Override
  public Flag getQCFlag() {
    return null != overrideQcFlag ? overrideQcFlag : super.getQCFlag();
  }

  /**
   * If the override QC flag is set, then we return the override messages.
   * Otherwise we return the original record's messages.
   */
  @Override
  public List<String> getQCMessages() {
    return null != overrideQcFlag ? overrideQcMessages : super.getQCMessages();
  }

  /**
   * Determines whether or not the QC flag has been overridden, and therefore
   * the record needs to be saved.
   *
   * @return {@code true} if the record needs saving; {@code false) if not.
   */
  public boolean isDirty() {
    return null != overrideQcFlag;
  }
}
