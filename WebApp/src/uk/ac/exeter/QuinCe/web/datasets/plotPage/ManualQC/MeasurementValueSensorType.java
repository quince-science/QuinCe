package uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC;

import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeading;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Dummy {@link SensorType} instance for use with
 * {@link uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue}s.
 *
 * <p>
 * {@link uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue}s are repeat
 * instances of {@link uk.ac.exeter.QuinCe.data.Dataset.SensorValue}s but
 * containing the actual values used in calculations after interpolation etc. As
 * such they share {@link SensorType}s, which causes issues because the
 * {@link SensorType} IDs overlap.
 * </p>
 *
 * <p>
 * This is a proxy instance of a {@link SensorType} object which overrides the
 * standard database ID by adding an offset to it, thereby making it unique.
 * </p>
 *
 *
 */
public class MeasurementValueSensorType extends SensorType {

  private static final long OFFSET = 5000000;

  private SensorType source;

  public MeasurementValueSensorType(SensorType source) {
    super(source);
    this.source = source;
  }

  @Override
  public long getId() {
    return super.getId() + OFFSET;
  }

  public long getOriginalId() {
    return super.getId();
  }

  @Override
  public boolean isPosition() {
    return SensorType.isPosition(source.getId());
  }

  public static SensorType getSensorType(ColumnHeading heading)
    throws SensorTypeNotFoundException {
    return ResourceManager.getInstance().getSensorsConfiguration()
      .getSensorType(heading.getId() - OFFSET);
  }
}
