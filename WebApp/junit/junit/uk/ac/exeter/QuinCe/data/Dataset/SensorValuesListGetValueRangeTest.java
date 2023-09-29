package junit.uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import junit.uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValuesListException;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValuesListValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Test the
 * {@link SensorValuesList#getValue(java.time.LocalDateTime, java.time.LocalDateTime)}
 * method.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class SensorValuesListGetValueRangeTest
  extends SensorValuesListGetValueContinuousTest {

  private static final int START_TIME_COL = 2;

  private static final int END_TIME_COL = 3;

  @Override
  protected String getTestSetName() {
    return "SensorValuesListGetValueRange";
  }

  @Override
  protected void buildSensorValues(DatasetSensorValues allSensorValues,
    TestSetLine line) throws RecordNotFoundException, InvalidFlagException {

    makeSensorValues(allSensorValues, line, 0, 11);
    makeSensorValues(allSensorValues, line, 1, 21);
  }

  @Override
  protected SensorValuesListValue getValue(SensorValuesList list,
    TestSetLine line) throws SensorValuesListException {

    LocalDateTime startTime = makeTime(line.getIntField(START_TIME_COL));
    LocalDateTime endTime = makeTime(line.getIntField(END_TIME_COL));

    return list.getValue(startTime, endTime,
      DateTimeUtils.midPoint(startTime, endTime));
  }

  @Override
  protected int getExpectedStartTimeCol() {
    return 4;
  }

  @Override
  protected int getExpectedEndTimeCol() {
    return 5;
  }

  @Override
  protected int getExpectedNominalTimeCol() {
    return 6;
  }

  @Override
  protected int getExpectedValueCol() {
    return 7;
  }

  @Override
  protected int getExpectedFlagCol() {
    return 8;
  }

  @Override
  protected int getExpectedUsedValuesCol() {
    return 9;
  }

}
