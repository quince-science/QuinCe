package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

/**
 * Default implementation of the {@link CalibrationTargetNameMapper} interface.
 * All targets are left unchanged.
 */
public class DefaultTargetNameMapper implements CalibrationTargetNameMapper {

  @Override
  public String map(String target) {
    return target;
  }

}
