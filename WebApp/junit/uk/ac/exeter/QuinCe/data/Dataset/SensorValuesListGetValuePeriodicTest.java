package uk.ac.exeter.QuinCe.data.Dataset;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Test the {@link SensorValuesList#getValue(java.time.LocalDateTime, boolean)}
 * method for periodic measurements.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class SensorValuesListGetValuePeriodicTest
  extends SensorValuesListGetValueContinuousTest {

  @Override
  protected String getTestSetName() {
    return "SensorValuesListGetValuePeriodic";
  }

  @Override
  protected void buildSensorValues(DatasetSensorValues allSensorValues,
    TestSetLine line) throws RecordNotFoundException, InvalidFlagException {

    makeSensorValues(allSensorValues, line, 0, 11);
    makeSensorValues(allSensorValues, line, 1, 21);
    makeSensorValues(allSensorValues, line, 2, 35);
  }

  @Override
  protected boolean getInterpolationAllowed(TestSetLine line) {
    return true;
  }

  @Override
  protected int getExpectedUsedValuesCol() {
    return 9;
  }

  @Override
  protected int getExpectedFlagCol() {
    return 8;
  }

  @Override
  protected int getExpectedNominalTimeCol() {
    return 6;
  }

  @Override
  protected int getExpectedEndTimeCol() {
    return 5;
  }

  @Override
  protected int getExpectedStartTimeCol() {
    return 4;
  }

  @Override
  protected int getExpectedValueCol() {
    return 7;
  }

  @Override
  protected int getRequestedMinuteCol() {
    return 3;
  }

  @Override
  protected int getInterpolatesAroundFlagCol() {
    return 10;
  }
}
