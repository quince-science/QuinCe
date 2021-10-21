package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.ParameterException;

/**
 * Represents a external standard calibration
 *
 * @author Steve Jones
 *
 */
public class ExternalStandard extends Calibration {

  private static final List<String> HIDDEN_SENSOR_TYPES;

  static {
    HIDDEN_SENSOR_TYPES = Arrays.asList("xH₂O (with standards)");
  }

  /**
   * Create an empty external standard placeholder that isn't bound to a
   * particular standard
   *
   * @param instrumentId
   *          The instrument ID
   */
  public ExternalStandard(Instrument instrument) {
    super(instrument, ExternalStandardDB.EXTERNAL_STANDARD_CALIBRATION_TYPE);
  }

  /**
   * Creates an empty external standard for a specified standard
   *
   * @param instrumentid
   *          The instrument ID
   * @param standard
   *          The standard
   */
  protected ExternalStandard(Instrument instrument, String standard) {
    super(instrument, ExternalStandardDB.EXTERNAL_STANDARD_CALIBRATION_TYPE,
      standard);
  }

  /**
   * Construct a complete external standard object with all data
   *
   * @param instrumentId
   *          The instrument ID
   * @param target
   *          The target external standard
   * @param deploymentDate
   *          The deployment date
   * @param coefficients
   *          The standard concentration
   * @throws ParameterException
   *           If the calibration details are invalid
   */
  protected ExternalStandard(long id, Instrument instrument, String target,
    LocalDateTime deploymentDate, Map<String, String> coefficients)
    throws ParameterException {
    super(id, instrument, ExternalStandardDB.EXTERNAL_STANDARD_CALIBRATION_TYPE,
      target);

    if (null != target) {
      setDeploymentDate(deploymentDate);
      setCoefficients(coefficients);
      if (!validate()) {
        throw new ParameterException("Deployment date/coefficients",
          "Calibration deployment is invalid");
      }
    }
  }

  /**
   * The coefficient names are the names of the {@link SensorType}s that have
   * internal calibrations.
   */
  @Override
  public List<String> getCoefficientNames() {
    return instrument.getSensorAssignments().getAssignedSensorTypes().stream()
      .filter(s -> s.hasInternalCalibration()).map(s -> s.getShortName())
      .collect(Collectors.toList());
  }

  /**
   * Get the concentration of the external standard
   *
   * @return The concentration
   */
  public Double getConcentration(SensorType sensorType) {
    return getDoubleCoefficient(sensorType.getShortName());
  }

  /**
   * Set the concentration of the external standard for the specified
   * {@link SensorType}.
   *
   * @param concentration
   *          The concentration
   */
  public void setConcentration(SensorType sensorType, String concentration) {
    setCoefficient(sensorType.getShortName(), concentration);
  }

  @Override
  public boolean coefficientsValid() {
    return true;
  }

  @Override
  public Double calibrateValue(Double rawValue) {
    return rawValue;
  }

  @Override
  public List<CalibrationCoefficient> getEditableCoefficients() {
    return getCoefficients().stream()
      .filter(c -> !HIDDEN_SENSOR_TYPES.contains(c.getName()))
      .collect(Collectors.toList());
  }
}
