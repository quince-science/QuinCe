package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;

/**
 * Stub {@link PlotPageTableValue} for a null value.
 *
 * @author Steve Jones
 *
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
  public Flag getQcFlag() {
    return Flag.NO_QC;
  }

  @Override
  public String getQcMessage(boolean replaceNewlines) {
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

}
