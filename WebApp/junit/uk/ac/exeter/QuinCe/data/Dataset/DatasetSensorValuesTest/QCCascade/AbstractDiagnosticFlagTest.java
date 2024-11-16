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
import uk.ac.exeter.QuinCe.utils.StringUtils;

public abstract class AbstractDiagnosticFlagTest extends TestSetTest {

  protected List<String> expectedCommentList(TestSetLine line, int col) {
    return StringUtils.delimitedToList(line.getStringField(col, false), ";");
  }

  /**
   * Check that the User QC for a {@link SensorValue} is as expected.
   *
   * <ul>
   * <li>The User QC flag should match the specified flag</li>
   * <li>The User QC comment should contain the specified comments. If the
   * specified comment is empty, the User QC comment should also be empty.</li>
   *
   * @param sensorValue
   * @param expectedFlag
   * @param expectedComment
   * @return
   */
  protected void checkQC(String valueName, SensorValue sensorValue,
    int expectedFlag, Collection<String> expectedComment,
    DatasetSensorValues allSensorValues) throws Exception {

    checkQC(valueName, sensorValue.getDisplayFlag(), expectedFlag,
      sensorValue.getDisplayQCMessage(allSensorValues), expectedComment);
  }

  protected void checkQC(DataReductionRecord record, int expectedFlag,
    Collection<String> expectedComment) {

    Flag flag = record.getQCFlag();
    String qcComment = StringUtils.listToDelimited(record.getQCMessages(), ";");

    checkQC("Data Reduction", flag, expectedFlag, qcComment, expectedComment);
  }

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
      conn);
  }

  protected void checkQC(String valueName, SensorValue sensorValue,
    int expectedFlag) throws Exception {
    checkQC(valueName, sensorValue, expectedFlag, null, null);
  }
}
