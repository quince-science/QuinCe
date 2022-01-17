package junit.uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class MeasurementTest extends BaseTest {

  private static final String POSITION_MESSAGE = "POSMESSAGE";

  private static final String SENSOR_MESSAGE = "SENSORMESSAGE";

  private static final String ADDED_POSITION_MESSAGE = Measurement.POSITION_QC_PREFIX
    + POSITION_MESSAGE;

  @BeforeEach
  public void init() {
    initResourceManager();
  }

  /**
   * Get a {@link SensorType} object for Intake Temperature. Useful as a general
   * SensorType.
   *
   * @return The SensorType
   * @throws SensorTypeNotFoundException
   */
  private SensorType intakeTemperature() throws SensorTypeNotFoundException {
    return ResourceManager.getInstance().getSensorsConfiguration()
      .getSensorType("Intake Temperature");
  }

  /**
   * Create a measurement containing {@link MeasurementValue}s for position and
   * another sensor, each with the specified QC flags.
   *
   * @param positionFlag
   *          The flag for the position value
   * @param sensorFlag
   *          The flag for the other sensor value
   * @return The measurement
   * @throws SensorTypeNotFoundException
   */
  private Measurement makePositionFlagMeasurement(Flag positionFlag,
    Flag sensorFlag) throws SensorTypeNotFoundException {

    Measurement result = new Measurement(1L, 1L, LocalDateTime.now(),
      "RunType");

    if (null != positionFlag) {
      List<String> posMessage = new ArrayList<String>(1);
      posMessage.add(POSITION_MESSAGE);

      MeasurementValue longitude = new MeasurementValue(SensorType.LONGITUDE_ID,
        Arrays.asList(new Long[] { 1L }), null, 1, 0D, positionFlag, posMessage,
        null);

      MeasurementValue latitude = new MeasurementValue(SensorType.LATITUDE_ID,
        Arrays.asList(new Long[] { 2L }), null, 1, 0D, positionFlag, posMessage,
        null);

      result.setMeasurementValue(longitude);
      result.setMeasurementValue(latitude);
    }

    ArrayList<String> sensorMessage = new ArrayList<String>(1);
    sensorMessage.add(SENSOR_MESSAGE);

    MeasurementValue other = new MeasurementValue(intakeTemperature().getId(),
      Arrays.asList(new Long[] { 3L }), null, 1, 0D, sensorFlag, sensorMessage,
      null);

    result.setMeasurementValue(other);

    return result;
  }

  @FlywayTest
  @Test
  public void postprocessMeasurementValuesNoPositionTest()
    throws SensorTypeNotFoundException {
    Measurement measurement = makePositionFlagMeasurement(null, Flag.GOOD);
    measurement.postProcessMeasurementValues();

    assertEquals(Flag.GOOD,
      measurement.getMeasurementValue(intakeTemperature()).getQcFlag());
  }

  @FlywayTest
  @Test
  public void postprocessMeasurementValuesBadPosGoodSensorTest()
    throws SensorTypeNotFoundException {
    Measurement measurement = makePositionFlagMeasurement(Flag.BAD, Flag.GOOD);
    measurement.postProcessMeasurementValues();

    assertEquals(Flag.BAD,
      measurement.getMeasurementValue(intakeTemperature()).getQcFlag());

    String qcMessage = measurement.getMeasurementValue(intakeTemperature())
      .getQcMessage(false);

    assertTrue(qcMessage.contains(ADDED_POSITION_MESSAGE));
  }

  @FlywayTest
  @Test
  public void postprocessMeasurementValuesBadPosQuestionableSensorTest()
    throws SensorTypeNotFoundException {
    Measurement measurement = makePositionFlagMeasurement(Flag.BAD,
      Flag.QUESTIONABLE);

    // Before post-processing, the questionable sensor message should be present
    String preQcMessage = measurement.getMeasurementValue(intakeTemperature())
      .getQcMessage(false);
    assertTrue(preQcMessage.contains(SENSOR_MESSAGE));

    measurement.postProcessMeasurementValues();

    assertEquals(Flag.BAD,
      measurement.getMeasurementValue(intakeTemperature()).getQcFlag());

    String qcMessage = measurement.getMeasurementValue(intakeTemperature())
      .getQcMessage(false);

    assertTrue(qcMessage.contains(ADDED_POSITION_MESSAGE));

    // Now the sensor message should have gone - replaced by the position
    // message
    assertFalse(qcMessage.contains(SENSOR_MESSAGE));
  }

  @FlywayTest
  @Test
  public void postprocessMeasurementValuesBadPosBadSensorTest()
    throws SensorTypeNotFoundException {
    Measurement measurement = makePositionFlagMeasurement(Flag.BAD, Flag.BAD);
    measurement.postProcessMeasurementValues();

    assertEquals(Flag.BAD,
      measurement.getMeasurementValue(intakeTemperature()).getQcFlag());

    String qcMessage = measurement.getMeasurementValue(intakeTemperature())
      .getQcMessage(false);

    // Both sensor and position messages should be present
    assertTrue(qcMessage.contains(ADDED_POSITION_MESSAGE));
    assertTrue(qcMessage.contains(SENSOR_MESSAGE));
  }
}
