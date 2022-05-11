package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.utils.StringUtils;

public class DataReductionRecordPlotPageTableValue
  implements PlotPageTableValue {

  private final DataReductionRecord record;

  private final String parameterName;

  /**
   * Create a column value for a parameter from a {@link DataReductionRecord}.
   *
   * @param record
   *          The data reduction record.
   * @param parameterName
   *          The parameter.
   */
  public DataReductionRecordPlotPageTableValue(DataReductionRecord record,
    String parameterName) {

    this.record = record;
    this.parameterName = parameterName;
  }

  @Override
  public long getId() {
    return record.getMeasurementId() + parameterName.hashCode();
  }

  @Override
  public String getValue() {
    Double result = record.getCalculationValue(parameterName);
    return null == result ? null : String.valueOf(result);
  }

  @Override
  public Object getRawValue() {
    return record.getCalculationValue(parameterName);
  }

  @Override
  public String getQcMessage(boolean replaceNewlines) {

    String result = StringUtils.collectionToDelimited(record.getQCMessages(),
      ";");

    if (replaceNewlines) {
      result = StringUtils.replaceNewlines(result);
    }

    return result;
  }

  @Override
  public boolean getFlagNeeded() {
    return record.getQCFlag().equals(Flag.NEEDED);
  }

  @Override
  public Flag getQcFlag() {
    return record.getQCFlag();
  }

  @Override
  public boolean isNull() {
    return null == record;
  }

  @Override
  public char getType() {
    return PlotPageTableValue.DATA_REDUCTION_TYPE;
  }
}
