package uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValuesTest.QCCascade;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;

import uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetMeasurements;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.DefaultMeasurementValueCollector;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementLocator;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValueCollector;
import uk.ac.exeter.QuinCe.data.Dataset.RunTypePeriods;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.SimpleMeasurementLocator;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducer;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.jobs.files.DataReductionJob;
import uk.ac.exeter.QuinCe.jobs.files.LocateMeasurementsJob;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Parent class for tests of QC flags set on Diagnostic values and their
 * propagation to other {@link SensorValue}s.
 */
public abstract class AbstractDiagnosticFlagTest extends TestSetTest {

  /**
   * Extract multiple QC comments (separated by {@code ;}) specified in a
   * {@link TestSetLine} into a {@link List} of comments.
   *
   * @param line
   *          The {@link TestSetLine}.
   * @param col
   *          The column number containing the comments.
   * @return The comments as a {@link List}.
   */
  protected List<String> expectedCommentList(TestSetLine line, int col) {
    return StringUtils.delimitedToList(line.getStringField(col, false), ";");
  }

  /**
   * Check that the QC for a {@link SensorValue} is as expected.
   *
   * <ul>
   * <li>The Display QC flag should match the specified flag</li>
   * <li>The Display QC comment should contain the specified comments. If the
   * specified comment is empty, the User QC comment should also be empty.</li>
   * </ul>
   *
   * <p>
   * This method does not return a result; it performs a JUnit assertion.
   * </p>
   *
   * @param valueName
   *          A recognisable name for the value being checked.
   * @param sensorValue
   *          The {@link SensorValue} to be checked.
   * @param expectedFlag
   *          The expected display QC flag.
   * @param expectedComment
   *          The expected display QC comment.
   * @param allSensorValues
   *          The complete set of Sensor Values for the DataSet being processed.
   * @throws Exception
   *           If any errors are thrown during checking.
   *
   * @see #checkQC(String, Flag, int, String, Collection)
   */
  protected void checkQC(String valueName, SensorValue sensorValue,
    int expectedFlag, Collection<String> expectedComment,
    DatasetSensorValues allSensorValues) throws Exception {

    checkQC(valueName, sensorValue.getDisplayFlag(allSensorValues),
      expectedFlag, sensorValue.getDisplayQCMessage(allSensorValues),
      expectedComment);
  }

  /**
   * Check that the QC for a {@link DataReductionRecord} is as expected.
   *
   * <ul>
   * <li>The QC flag should match the specified flag</li>
   * <li>The QC comment should contain the specified comments. If the specified
   * comment is empty, the User QC comment should also be empty.</li>
   * </ul>
   *
   * <p>
   * This method does not return a result; it performs a JUnit assertion.
   * </p>
   *
   * @param record
   *          The {@link DataReductionRecord} being checked.
   * @param expectedFlag
   *          The expected QC flag.
   * @param expectedComment
   *          The expected QC comments.
   *
   * @see #checkQC(String, Flag, int, String, Collection)
   */
  protected void checkQC(DataReductionRecord record, int expectedFlag,
    Collection<String> expectedComment) {

    Flag flag = record.getQCFlag();
    String qcComment = StringUtils.collectionToDelimited(record.getQCMessages(), ";");

    checkQC("Data Reduction", flag, expectedFlag, qcComment, expectedComment);
  }

  /**
   * Check that a set of QC information matches the expected information.
   *
   * <p>
   * This method does not return a result: it performs a JUnit assertion.
   * </p>
   *
   * @param name
   *          A recognisable name for the value being checked.
   * @param valueFlag
   *          The flag to be checked.
   * @param expectedFlag
   *          The expected flag.
   * @param valueComment
   *          The comment to be checked.
   * @param expectedComment
   *          The expected comment(s).
   */
  protected void checkQC(String name, Flag valueFlag, int expectedFlag,
    String valueComment, Collection<String> expectedComment) {

    boolean flagOK = true;

    if (expectedFlag == 2) {
      flagOK = valueFlag.isGood();
    } else {
      flagOK = valueFlag.getFlagValue() == expectedFlag;
    }

    assertTrue(flagOK, name + " flag incorrect (expected " + expectedFlag
      + ", was " + valueFlag.getFlagValue() + ")");

    boolean commentOK = true;
    String invalidCommentPart = null;

    if (null != expectedComment) {
      if (expectedComment.size() == 0) {
        commentOK = null == valueComment || valueComment.length() == 0;
      } else {
        for (String commentPart : expectedComment) {
          if (StringUtils.countMatches(valueComment, commentPart) != 1) {
            commentOK = false;
            invalidCommentPart = commentPart;
            break;
          }
        }
      }
    }

    assertTrue(commentOK, name + " comment incorrect: expected "
      + invalidCommentPart + ", was " + valueComment);
  }

  /**
   * Set the QC flag on a {@link SensorValue} using a numeric value.
   *
   * @param sensorValue
   *          The {@link SensorValue} whose QC flag is to be set.
   * @param flagValue
   *          The numeric value of the flag to be set.
   * @throws InvalidFlagException
   *           If the numeric flag value does not correspond to a valid QC flag.
   */
  protected void setUserQC(SensorValue sensorValue, int flagValue)
    throws InvalidFlagException {

    Flag flag = new Flag(flagValue);
    String comment = flag.isGood() ? "" : "User QC";
    sensorValue.setUserQC(flag, comment);
  }

  /**
   * Generate a {@link RunTypePeriods} object based on the specified
   * {@link SensorValue}.
   *
   * @param source
   *          The source SensorValue.
   * @return The RunTypePeriods object.
   * @throws Exception
   *           If the object cannot be created.
   */
  protected RunTypePeriods makeRunTypePeriods(SensorValue source)
    throws Exception {
    RunTypePeriods result = new RunTypePeriods();
    result.add(source.getValue(), source.getTime());
    return result;
  }

  /**
   * Run the data reduction for a {@link DataSet}.
   *
   * <p>
   * This performs all the standard tasks of the {@link LocateMeasurementsJob}
   * and {@link DataReductionJob}, storing the results in the test database.
   * </p>
   *
   * <p>
   * Although the data reduction is run for the complete {@link DataSet}, only
   * the first {@link DataReductionRecord} is returned. We assume that the
   * calling test is set up such that the values of interest will be in the
   * first {@link Measurement}.
   * </p>
   *
   * @param conn
   *          A database connection.
   * @param instrument
   *          The {@link Instrument} that the {@link DataSet} belongs to.
   * @param variable
   *          The {@link Variable} whose reduction is to be performed.
   * @param dataset
   *          The {@link DataSet}.
   * @param allSensorValues
   *          The complete set of {@link SensorValue}s belonging to the
   *          {@link DataSet}.
   * @return The {@link DataReductionRecord} for the first identified
   *         {@link Measurement} in the {@link DataSet}.
   * @throws Exception
   *           If any errors are thrown.
   */
  protected DataReductionRecord runDataReduction(Connection conn,
    Instrument instrument, Variable variable, DataSet dataset,
    DatasetSensorValues allSensorValues) throws Exception {
    // Create the measurements for the dataset by running the
    // MeasurementLocator job
    MeasurementLocator measurementLocator = new SimpleMeasurementLocator(
      variable);
    List<Measurement> locatedMeasurements = measurementLocator
      .locateMeasurements(conn, instrument, dataset, allSensorValues);

    DataSetDataDB.storeMeasurements(conn, locatedMeasurements);

    // Get all the measurements grouped by run type
    DatasetMeasurements allMeasurements = DataSetDataDB
      .getMeasurementsByRunType(conn, instrument, dataset.getId());

    MeasurementValueCollector measurementValueCollector = new DefaultMeasurementValueCollector();

    Measurement measurement = locatedMeasurements.get(0);

    Collection<MeasurementValue> measurementValues = measurementValueCollector
      .collectMeasurementValues(instrument, dataset, variable, allMeasurements,
        allSensorValues, conn, measurement);

    // Store the measurement values for processing.
    measurementValues.forEach(mv -> {
      if (null != mv) {
        if (!measurement.hasMeasurementValue(mv.getSensorType())) {
          measurement.setMeasurementValue(mv);
        }
      }
    });

    DataSetDataDB.storeMeasurementValues(conn, measurement);

    DataReducer reducer = new QCCascadeReducer(variable, null);

    return reducer.performDataReduction(instrument, locatedMeasurements.get(0),
      allSensorValues, conn);
  }

  /**
   * Check that a {@link SensorValue} has the expected numeric Display QC Flag
   * value.
   *
   * <p>
   * This method does not return a result; it performs a JUnit assertion.
   * </p>
   *
   * @param valueName
   *          A recognisable name for the value being checked.
   * @param sensorValue
   *          The {@link SensorValue} being checked.
   * @param expectedFlag
   *          The expected numeric value of the QC flag.
   * @throws Exception
   *           If an error occurs during checking.
   */
  protected void checkQC(String valueName, SensorValue sensorValue,
    int expectedFlag) throws Exception {
    checkQC(valueName, sensorValue, expectedFlag, null, null);
  }
}
