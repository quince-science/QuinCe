package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.util.Comparator;

public class CalibrationTimeComparator implements Comparator<Calibration> {

  @Override
  public int compare(Calibration o1, Calibration o2) {
    return o1.getDeploymentDate().compareTo(o2.getDeploymentDate());
  }

}
