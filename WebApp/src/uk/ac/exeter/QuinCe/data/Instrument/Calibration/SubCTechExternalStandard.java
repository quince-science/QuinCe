package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.utils.ParameterException;

public class SubCTechExternalStandard extends DefaultExternalStandard {

  /**
   * Copy constructor.
   *
   * @param source
   *          The copy source.
   * @throws CalibrationException
   */
  public SubCTechExternalStandard(SubCTechExternalStandard source)
    throws CalibrationException {
    super(source.getId(), source.getInstrument(), source.getTarget(),
      source.getDeploymentDate(), duplicateCoefficients(source));
  }

  public SubCTechExternalStandard(Instrument instrument, long id,
    LocalDateTime date) throws CalibrationException {
    super(instrument, id, date);
  }

  public SubCTechExternalStandard(long id, Instrument instrument, String target,
    LocalDateTime deploymentDate, Map<String, String> coefficients)
    throws ParameterException, CalibrationException {
    super(id, instrument, target, deploymentDate, coefficients);
  }

  @Override
  protected List<String> getHiddenSensorTypes() {
    return Arrays.asList("SubCTech xH₂O");
  }

  @Override
  public Calibration makeCopy() {
    try {
      return new SubCTechExternalStandard(this);
    } catch (CalibrationException e) {
      // This shouldn't happen, because it implies that we successfully created
      // in invalid object
      throw new RuntimeException(e);
    }
  }
}
