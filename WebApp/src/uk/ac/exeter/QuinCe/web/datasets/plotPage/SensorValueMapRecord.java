package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import com.javadocmd.simplelatlng.LatLng;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;

public class SensorValueMapRecord extends MapRecord {

  protected final SensorValue value;

  protected SensorValueMapRecord(LatLng position, long id, SensorValue value) {
    super(position, id);
    this.value = value;
  }

  @Override
  public boolean isGood(DatasetSensorValues allSensorValues) {
    return value.getDisplayFlag(allSensorValues).isGood();
  }

  @Override
  public boolean flagNeeded() {
    return value.flagNeeded();
  }

  @Override
  public Double getValue() {
    return value.getDoubleValue();
  }

  @Override
  public Flag getFlag(DatasetSensorValues allSensorValues,
    boolean ignoreNeeded) {

    Flag result;

    if (!ignoreNeeded && value.flagNeeded()) {
      result = Flag.NEEDED;
    } else {
      result = value.getDisplayFlag(allSensorValues);
    }

    return result;
  }
}
