package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import com.javadocmd.simplelatlng.LatLng;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;

@SuppressWarnings("serial")
public class DataLatLng extends LatLng {

  private final PlotPageTableValue longitude;

  public DataLatLng(PlotPageTableValue latitude, PlotPageTableValue longitude) {
    super(Double.parseDouble(latitude.getValue()),
      Double.parseDouble(longitude.getValue()));
    this.longitude = longitude;
  }

  public Flag getFlag() {
    return longitude.getQcFlag();
  }

  public String getQcMessage(DatasetSensorValues allSensorValues)
    throws RoutineException {
    return longitude.getQcMessage(allSensorValues, true);
  }

  public boolean getFlagNeeded() {
    return longitude.getFlagNeeded();
  }

  public char getType() {
    return longitude.getType();
  }
}
