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
    Double value = record.getCalculationValue(parameterName);
    return null == value ? null : String.valueOf(value);
  }

  @Override
  public boolean getUsed() {
    return false;
  }

  @Override
  public String getQcMessage() {
    return StringUtils.collectionToDelimited(record.getQCMessages(), ";");
  }

  @Override
  public boolean getFlagNeeded() {
    return record.getQCFlag().equals(Flag.NEEDED);
  }

  @Override
  public Flag getQcFlag() {
    return record.getQCFlag();
  }
}
