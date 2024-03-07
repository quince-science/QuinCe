package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.util.Collection;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;

/**
 * Stub {@link PlotPageTableValue} for a null value.
 */
public class NullPlotPageTableValue implements PlotPageTableValue {

  @Override
  public long getId() {
    return DatabaseUtils.NO_DATABASE_RECORD;
  }

  @Override
  public String getValue() {
    return null;
  }

  @Override
  public Object getRawValue() {
    return null;
  }

  @Override
  public Flag getQcFlag() {
    return Flag.NO_QC;
  }

  @Override
  public String getQcMessage(DatasetSensorValues allSensorValues,
    boolean replaceNewlines) {
    return null;
  }

  @Override
  public boolean getFlagNeeded() {
    return false;
  }

  @Override
  public boolean isNull() {
    return true;
  }

  @Override
  public char getType() {
    return PlotPageTableValue.MEASURED_TYPE;
  }

  @Override
  public Collection<Long> getSources() {
    return null;
  }
}
