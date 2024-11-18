package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.util.ArrayList;
import java.util.Collection;

import com.javadocmd.simplelatlng.LatLng;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;

@SuppressWarnings("serial")
public class DataLatLng extends LatLng {

  private final PlotPageTableValue longitude;

  private final PlotPageTableValue latitude;

  public DataLatLng(PlotPageTableValue latitude, PlotPageTableValue longitude) {
    super(Double.parseDouble(latitude.getValue()),
      Double.parseDouble(longitude.getValue()));
    this.longitude = longitude;
    this.latitude = latitude;
  }

  public Flag getFlag(DatasetSensorValues allSensorValues) {
    return longitude.getQcFlag(allSensorValues);
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

  public Collection<Long> getSourceIds() {
    Collection<Long> result = new ArrayList<Long>();
    result.addAll(longitude.getSources());
    result.addAll(latitude.getSources());
    return result;
  }
}
