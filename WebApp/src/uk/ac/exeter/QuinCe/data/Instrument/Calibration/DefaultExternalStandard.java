package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.utils.ParameterException;

public class DefaultExternalStandard extends ExternalStandard {

  public DefaultExternalStandard(Instrument instrument, long id,
    LocalDateTime date) {
    super(instrument, id, date);
  }

  public DefaultExternalStandard(long id, Instrument instrument, String target,
    LocalDateTime deploymentDate, Map<String, String> coefficients)
    throws ParameterException, CalibrationException {
    super(id, instrument, target, deploymentDate, coefficients);
  }

  public DefaultExternalStandard(long id, Instrument instrument, String target,
    LocalDateTime deploymentDate, List<CalibrationCoefficient> coefficients)
    throws ParameterException, CalibrationException {
    super(id, instrument, target, deploymentDate, coefficients);
  }

  /**
   * Copy constructor.
   *
   * @param source
   *          The copy source.
   * @throws CalibrationException
   */
  public DefaultExternalStandard(DefaultExternalStandard source)
    throws CalibrationException {
    super(source.getId(), source.getInstrument(), source.getTarget(),
      source.getDeploymentDate(), duplicateCoefficients(source));
  }

  @Override
  protected List<String> getHiddenSensorTypes() {
    return Arrays.asList("xH₂O (with standards)");
  }

  @Override
  public Calibration makeCopy() {
    try {
      return new DefaultExternalStandard(this);
    } catch (CalibrationException e) {
      // This shouldn't happen, because it implies that we successfully created
      // in invalid object
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getCoefficientsLabel() {
    return "Concentration";
  }

}
