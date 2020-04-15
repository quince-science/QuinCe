package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.utils.NoEmptyStringList;
import uk.ac.exeter.QuinCe.utils.StringUtils;

public class ReadOnlyDataReductionRecord extends DataReductionRecord {

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

  @Override
  public void setQc(Flag flag, List<String> messages) {
    throw new NotImplementedException("This record is read only");
  }
}
