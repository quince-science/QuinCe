package uk.ac.exeter.QuinCe.web.datasets.export;

import java.util.Collection;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
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
  public Object getRawValue() {
    return getValue();
  }

  @Override
  public Flag getQcFlag(DatasetSensorValues allSensorValues) {
    return Flag.GOOD;
  }

  @Override
  public String getQcMessage(DatasetSensorValues allSensorValues,
    boolean replaceNewlines) {
    return "";
  }

  @Override
  public boolean getFlagNeeded() {
    return false;
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  public char getType() {
    return PlotPageTableValue.NOMINAL_TYPE;
  }

  @Override
  public Collection<Long> getSources() {
    return null;
  }
}
