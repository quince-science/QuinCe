package junit.uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import junit.uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import junit.uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.DefaultMeasurementValueCalculator;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorOffsets;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AutoQCResult;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Tests the interpolation capabilities of
 * {@link DefaultMeasurementValueCalculator#calculate}.
 * <p>
 * The test creates a sequence of {@link SensorValue}s and then requests a
 * {@link MeasurementValue} for a sequence of times. The returned value should
 * be interpolated correctly and have the correct flag. The flags are set
 * according to the first column in the test set CSV file.
 * </p>
 * <p>
 * This test uses a {@link SensorType} that does not have calibrations.
 * </p>
 *
 * @author Steve Jones
 */
@TestInstance(Lifecycle.PER_CLASS)
public class DefaultMeasurementValueCalculatorInterpolationTest
  extends TestSetTest {

  private static final long COLUMN_ID = 1L;

  private static final long DATASET_ID = 1L;

  /**
   * A dummy Auto QC result
   */
  private static final AutoQCResult MOCK_QC_RESULT = Mockito
    .mock(AutoQCResult.class);

  private static final int FLAGS_COL = 0;

  private static final int TIMESTAMP_COL = 1;

  private static final int EXPECTED_VALUE_COL = 2;

  private static final int EXPECTED_TYPE_COL = 3;

  private static final int EXPECTED_FLAG_COL = 4;

  private static final int EXPECTED_MESSAGE_COL = 5;

  private List<LocalDateTime> timestamps;

  private List<String> values;

  private Instrument instrument;

  private SensorType sensorType;

  private Variable variable;

  /**
   * Build everything needed for the tests
   *
   * @throws Exception
   */
  @BeforeEach
  public void setup() throws Exception {

    initResourceManager();

    // Test data
    // 2021-07-22T00:05:00Z,2
    // 2021-07-22T00:15:00Z,3
    // 2021-07-22T00:25:00Z,3.5
    // 2021-07-22T00:35:00Z,4.5
    // 2021-07-22T00:45:00Z,5
    // 2021-07-22T00:55:00Z,6
    // 2021-07-22T01:05:00Z,7
    // 2021-07-22T01:15:00Z,8.5
    // 2021-07-22T01:25:00Z,9
    // 2021-07-22T01:35:00Z,10

    timestamps = new ArrayList<LocalDateTime>(10);
    timestamps.add(LocalDateTime.parse("2021-07-22T00:24:00Z",
      DateTimeFormatter.ISO_DATE_TIME));
    timestamps.add(LocalDateTime.parse("2021-07-22T00:26:00Z",
      DateTimeFormatter.ISO_DATE_TIME));
    timestamps.add(LocalDateTime.parse("2021-07-22T00:28:00Z",
      DateTimeFormatter.ISO_DATE_TIME));
    timestamps.add(LocalDateTime.parse("2021-07-22T00:30:00Z",
      DateTimeFormatter.ISO_DATE_TIME));
    timestamps.add(LocalDateTime.parse("2021-07-22T00:32:00Z",
      DateTimeFormatter.ISO_DATE_TIME));
    timestamps.add(LocalDateTime.parse("2021-07-22T00:34:00Z",
      DateTimeFormatter.ISO_DATE_TIME));
    timestamps.add(LocalDateTime.parse("2021-07-22T00:36:00Z",
      DateTimeFormatter.ISO_DATE_TIME));
    timestamps.add(LocalDateTime.parse("2021-07-22T00:38:00Z",
      DateTimeFormatter.ISO_DATE_TIME));
    timestamps.add(LocalDateTime.parse("2021-07-22T00:40:00Z",
      DateTimeFormatter.ISO_DATE_TIME));
    timestamps.add(LocalDateTime.parse("2021-07-22T00:42:00Z",
      DateTimeFormatter.ISO_DATE_TIME));

    values = new ArrayList<String>(10);
    values.add("2");
    values.add("3");
    values.add("3.5");
    values.add("4.5");
    values.add("5");
    values.add("6");
    values.add("7");
    values.add("8.5");
    values.add("9");
    values.add("10");

    sensorType = ResourceManager.getInstance().getSensorsConfiguration()
      .getSensorType("Intake Temperature");

    variable = Mockito.mock(Variable.class);
    Mockito.when(variable.getCoreSensorType())
      .thenReturn(ResourceManager.getInstance().getSensorsConfiguration()
        .getSensorType("xCO₂ (with standards)"));

    List<Long> columnList = new ArrayList<Long>(1);
    columnList.add(COLUMN_ID);

    SensorAssignments mockAssignments = Mockito.mock(SensorAssignments.class);
    Mockito.when(mockAssignments.getColumnIds(sensorType))
      .thenReturn(columnList);

    SensorAssignment mockAssignment = Mockito.mock(SensorAssignment.class);
    Mockito.when(mockAssignment.getDatabaseId()).thenReturn(COLUMN_ID);

    TreeSet<SensorAssignment> mockAssignmentSet = new TreeSet<SensorAssignment>();
    mockAssignmentSet.add(mockAssignment);

    Mockito.when(mockAssignments.get(Mockito.any()))
      .thenReturn(mockAssignmentSet);

    instrument = Mockito.mock(Instrument.class);
    Mockito.when(instrument.getSensorAssignments()).thenReturn(mockAssignments);
    Mockito.when(instrument.hasInternalCalibrations()).thenReturn(false);
  }

  /**
   * Create a mock {@link DatasetSensorValues} object containing the test
   * {@link SensorValue}s.
   * <p>
   * Ten {@link SensorValue} objects are created, with flags set according to
   * the passed in {@code flagString} which should be ten characters of either
   * 2, 3, or 4 corresponding to {@link Flag#GOOD}, {@link Flag#QUESTIONABLE} or
   * {@link Flag#BAD} respectively.
   * </p>
   * <p>
   * The QC message applied to {@link Flag#QUESTIONABLE} or {@link Flag#BAD}
   * values will be {@code Qx} or {@code Bx} respectively, where {@code x} is
   * the zero-based index of the {@link SensorValue}.
   * </p>
   *
   * @param flagsString
   *          The QC flags to apply to the {@link SensorValue}s.
   * @return The {@link DatasetSensorValues} object.
   * @throws InvalidFlagException
   */
  private DatasetSensorValues makeSensorValues(String flagsString)
    throws InvalidFlagException {

    List<SensorValue> sensorValues = new ArrayList<SensorValue>(10);
    for (int i = 0; i < timestamps.size(); i++) {
      Flag qcFlag = new Flag(flagsString.charAt(i));
      String qcMessage = "";
      if (qcFlag.equals(Flag.QUESTIONABLE)) {
        qcMessage = "Q" + i;
      } else if (qcFlag.equals(Flag.BAD)) {
        qcMessage = "B" + i;
      }

      SensorValue sensorValue = new SensorValue(i, DATASET_ID, COLUMN_ID,
        timestamps.get(i), values.get(i), MOCK_QC_RESULT, qcFlag, qcMessage);

      sensorValues.add(sensorValue);
    }

    SensorValuesList ssvl = SensorValuesList
      .newFromSensorValueCollection(sensorValues);

    DatasetSensorValues mock = Mockito.mock(DatasetSensorValues.class);
    Mockito.when(mock.getColumnValues(Mockito.anyLong())).thenReturn(ssvl);

    return mock;
  }

  @FlywayTest
  @ParameterizedTest
  @MethodSource("getLines")
  public void interpolationTest(TestSetLine line) throws Exception {

    // All SensorOffset details are mocked such that no offsets are calculated.
    // This is covered in other tests.
    SensorOffsets sensorOffsets = Mockito.mock(SensorOffsets.class);
    Mockito.when(
      sensorOffsets.getOffsetTime(Mockito.any(), Mockito.any(), Mockito.any()))
      .thenAnswer(invocation -> invocation.getArgument(0));

    DataSet dataSet = Mockito.mock(DataSet.class);
    Mockito.when(dataSet.getSensorOffsets()).thenReturn(sensorOffsets);

    DatasetSensorValues sensorValues = makeSensorValues(
      line.getStringField(FLAGS_COL, true));
    Measurement measurement = Measurement
      .dummyTimeMeasurement(line.getTimeField(TIMESTAMP_COL));

    DefaultMeasurementValueCalculator calculator = new DefaultMeasurementValueCalculator();

    MeasurementValue value = calculator.calculate(instrument, dataSet,
      measurement, variable, sensorType, null, sensorValues,
      getDataSource().getConnection());

    Double expectedValue = line.getDoubleField(EXPECTED_VALUE_COL);
    char expectedType = line.getCharField(EXPECTED_TYPE_COL);
    Flag expectedFlag = line.getFlagField(EXPECTED_FLAG_COL);
    String expectedMessage = line.getStringField(EXPECTED_MESSAGE_COL, false);

    assertAll(() -> {
      assertEquals(expectedValue, value.getCalculatedValue(), 0.0004,
        "Value mismatch");
      assertEquals(expectedType, value.getType(), "Value type mismatch");
      assertEquals(expectedFlag, value.getQcFlag(), "Flag mismatch");
      assertEquals(expectedMessage, value.getQcMessage(sensorValues, false),
        "Message mismatch");
    });
  }

  @Override
  protected String getTestSetName() {
    return "DefaultMeasurementValueCalculatorInterpolationTest";
  }
}
