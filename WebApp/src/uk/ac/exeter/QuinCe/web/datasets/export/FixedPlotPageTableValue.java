package uk.ac.exeter.QuinCe.web.datasets.export;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;

public class FixedPlotPageTableValue implements PlotPageTableValue {

  private String value;

  protected FixedPlotPageTableValue(String value) {
    this.value = value;
  }

  @Override
  public long getId() {
    return -1L;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public boolean getUsed() {
    return false;
  }

  @Override
  public Flag getQcFlag() {
    return Flag.GOOD;
  }

  @Override
  public String getQcMessage() {
    return "";
  }

  @Override
  public boolean getFlagNeeded() {
    return false;
  }
}
