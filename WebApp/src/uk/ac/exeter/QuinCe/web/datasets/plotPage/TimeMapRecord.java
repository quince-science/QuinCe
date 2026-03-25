package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.time.LocalDateTime;

import com.javadocmd.simplelatlng.LatLng;

import uk.ac.exeter.QuinCe.data.Dataset.Coordinate;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

public class TimeMapRecord extends MapRecord {

  private LocalDateTime time;

  public TimeMapRecord(LatLng position, Coordinate coordinate) {
    super(position, coordinate.getId());
    this.time = coordinate.getTime();
  }

  @Override
  public boolean isGood(DatasetSensorValues allSensorValues) {
    return true;
  }

  @Override
  public boolean flagNeeded() {
    return false;
  }

  @Override
  public Double getValue() {
    return (double) DateTimeUtils.dateToLong(time);
  }

  @Override
  public Flag getFlag(DatasetSensorValues allSensorValues,
    boolean ignoreNeeded) {
    return allSensorValues.getFlagScheme().getGoodFlag();
  }
}
