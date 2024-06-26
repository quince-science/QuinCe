package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;

public class DefaultExternalStandard extends ExternalStandard {

  public DefaultExternalStandard(Instrument instrument, long id,
    LocalDateTime date) {
    super(instrument, id, date);
  }

  public DefaultExternalStandard(long id, Instrument instrument, String target,
    LocalDateTime deploymentDate, Map<String, String> coefficients) {
    super(id, instrument, target, deploymentDate, coefficients);
  }

  /**
   * Copy constructor.
   *
   * @param source
   *          The copy source.
   */
  public DefaultExternalStandard(DefaultExternalStandard source) {
    super(source.getId(), source.getInstrument(), source.getTarget(),
      source.getDeploymentDate(), duplicateCoefficients(source));
  }

  @Override
  protected List<String> getHiddenSensorTypes() {
    return Arrays.asList("xH₂O (with standards)");
  }

  @Override
  public Calibration makeCopy() {
    return new DefaultExternalStandard(this);
  }

}
