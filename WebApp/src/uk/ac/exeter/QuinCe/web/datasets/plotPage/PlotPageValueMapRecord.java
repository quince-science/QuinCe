package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.time.LocalDateTime;

import com.javadocmd.simplelatlng.LatLng;

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
  public boolean isGood() {
    return value.getQcFlag().isGood();
  }

  @Override
  public boolean flagNeeded() {
    return value.getFlagNeeded();
  }

  @Override
  public Double getValue() {
    return Double.parseDouble(value.getValue());
  }

  @Override
  public Flag getFlag(boolean ignoreNeeded) {
    Flag result;

    if (!ignoreNeeded && flagNeeded()) {
      result = Flag.NEEDED;
    } else {
      result = value.getQcFlag();
    }

    return result;
  }

}
