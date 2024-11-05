package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.time.LocalDateTime;

import com.javadocmd.simplelatlng.LatLng;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

public class PlotPageValueMapRecord extends MapRecord {

  private PlotPageTableValue value;

  protected PlotPageValueMapRecord(LatLng position, LocalDateTime time,
    PlotPageTableValue value) {
    super(position, DateTimeUtils.dateToLong(time));
    this.value = value;
  }

  @Override
  public boolean isGood(DatasetSensorValues allSensorValues) {
    return value.getQcFlag(allSensorValues).isGood();
  }

  @Override
  public boolean flagNeeded() {
    return value.getFlagNeeded();
  }

  @Override
  public Double getValue() {
    Double result = Double.NaN;

    if (null != value && null != value.getValue()) {
      result = Double.parseDouble(value.getValue());
    }

    return result;
  }

  @Override
  public Flag getFlag(DatasetSensorValues allSensorValues,
    boolean ignoreNeeded) {
    Flag result;

    if (!ignoreNeeded && flagNeeded()) {
      result = Flag.NEEDED;
    } else {
      result = value.getQcFlag(allSensorValues);
    }

    return result;
  }

}
