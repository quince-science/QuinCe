package junit.uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues.QCCascade;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import junit.uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.RunTypePeriods;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Tests for setting Diagnostic sensor flags manually.
 *
 * <p>
 * The database setup for this will be as follows:
 * </p>
 * <table>
 * <caption>Setup of diagnostic sensors and the effects they will
 * have.</caption>
 * <tr>
 * <th>Diagnostic Sensor</th>
 * <th>Affects SST with Run Types</th>
 * <th>Affects CO₂ with Run Types</th>
 * </tr>
 * <tr>
 * <td>Water Flow</td>
 * <td>var_1, var_2</td>
 * <td>var_1</td>
 * </tr>
 * <tr>
 * <td>Air Flow</td>
 * <td>None</td>
 * <td>var_2</td>
 * </tr>
 * </table>
 *
 * <p>
 * The tests will create a Dataset with no QC flags set. Then the following
 * actions are taken:
 * </p>
 * <ol>
 * <li>Set sensor value flags</li>
 * <li>Set diagnostic flags</li>
 * <li>Check sensor value flags</li>
 * <li>Run Data Reduction</li>
 * <li>Check flags on data reduction results</li>
 * <li>(Optional) Change diagnostic flags</li>
 * <li>(Optional) Check sensor value flags</li>
 * <li>(Optional) Run Data Reduction</li>
 * <li>(Optional) Check flags on data reduction results</li>
 * </ol>
 *
 * <p>
 * Auto-QC flags are always
 */
@TestInstance(Lifecycle.PER_CLASS)
public class ManualDiagnosticFlagTest extends AbstractDiagnosticFlagTest {

  private static final long RUNTYPE_VAL_ID = 1L;

  private static final long SST_VAL_ID = 2L;

  private static final long SALINITY_VAL_ID = 3L;

  private static final long CO2_VAL_ID = 4L;

  private static final long DIAGNOSTIC_WATER_VAL_ID = 5L;

  private static final long DIAGNOSTIC_GAS_VAL_ID = 6L;

  private static final int SST_MANUAL_QC_COL = 0;

  private static final int SALINITY_MANUAL_QC_COL = 1;

  private static final int CO2_MANUAL_QC_COL = 2;

  private static final int RUN_TYPE_COL = 3;

  private static final int DIAGNOSTIC_WATER_FLAG_COL = 4;

  private static final int DIAGNOSTIC_GAS_FLAG_COL = 5;

  private static final int EXPECTED_SST_FLAG_1_COL = 6;

  private static final int EXPECTED_SST_COMMENT_1_COL = 7;

  private static final int EXPECTED_SALINITY_FLAG_1_COL = 8;

  private static final int EXPECTED_SALINITY_COMMENT_1_COL = 9;

  private static final int EXPECTED_CO2_FLAG_1_COL = 10;

  private static final int EXPECTED_CO2_COMMENT_1_COL = 11;

  private static final int EXPECTED_DATA_REDUCTION_FLAG_1_COL = 12;

  private static final int EXPECTED_DATA_REDUCTION_COMMENT_1_COL = 13;

  private static final int UPDATED_DIAG_WATER_FLAG_COL = 14;

  private static final int UPDATED_DIAG_GAS_FLAG_COL = 15;

  private static final int EXPECTED_SST_FLAG_2_COL = 16;

  private static final int EXPECTED_SST_COMMENT_2_COL = 17;

  private static final int EXPECTED_SALINITY_FLAG_2_COL = 18;

  private static final int EXPECTED_SALINITY_COMMENT_2_COL = 19;

  private static final int EXPECTED_CO2_FLAG_2_COL = 20;

  private static final int EXPECTED_CO2_COMMENT_2_COL = 21;

  private static final int EXPECTED_DATA_REDUCTION_FLAG_2_COL = 22;

  private static final int EXPECTED_DATA_REDUCTION_COMMENT_2_COL = 23;

  @Override
  protected String getTestSetName() {
    return "ManualDiagnosticFlagTest";
  }

  /**
   * Run the test as described in the introduction.
   *
   * @param line
   *          The test line.
   * @throws Exception
   *           If the test run fails.
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/testbase/DataReduction/base",
    "resources/sql/testbase/DataReduction/singleMeasurement" })
  @ParameterizedTest
  @MethodSource("getLines")
  public void manualDiagnosticFlagTest(TestSetLine line) throws Exception {

    initResourceManager();

    try (Connection conn = getConnection()) {

      // Load Dataset data
      Instrument instrument = InstrumentDB.getInstrument(conn, 1L);
      Variable variable = instrument.getVariables().get(0);
      DataSet dataset = DataSetDB.getDataSet(conn, 1L);
      DatasetSensorValues allSensorValues = DataSetDataDB.getSensorValues(conn,
        instrument, dataset.getId(), false, false);

      // Set the auto QC and user QC values for the data SensorValues
      SensorValue sst = allSensorValues.getById(SST_VAL_ID);
      setUserQC(sst, line.getIntField(SST_MANUAL_QC_COL));

      SensorValue salinity = allSensorValues.getById(SALINITY_VAL_ID);
      setUserQC(salinity, line.getIntField(SALINITY_MANUAL_QC_COL));

      SensorValue co2 = allSensorValues.getById(CO2_VAL_ID);
      setUserQC(co2, line.getIntField(CO2_MANUAL_QC_COL));

      // Set the run type
      SensorValue runType = allSensorValues.getById(RUNTYPE_VAL_ID);
      runType.setValue(line.getStringField(RUN_TYPE_COL, false));

      // Write updated values to DB
      DataSetDataDB.storeSensorValues(conn,
        Arrays.asList(sst, salinity, co2, runType));

      // Set the Diagnostic QC flags and apply the cascade
      RunTypePeriods runTypePeriods = makeRunTypePeriods(runType);

      SensorValue waterFlow = allSensorValues.getById(DIAGNOSTIC_WATER_VAL_ID);
      waterFlow.setUserQC(new Flag(line.getIntField(DIAGNOSTIC_WATER_FLAG_COL)),
        "Liquid");

      allSensorValues.applyQCCascade(waterFlow, runTypePeriods);

      SensorValue gasFlow = allSensorValues.getById(DIAGNOSTIC_GAS_VAL_ID);
      gasFlow.setUserQC(new Flag(line.getIntField(DIAGNOSTIC_GAS_FLAG_COL)),
        "Air");

      allSensorValues.applyQCCascade(gasFlow, runTypePeriods);

      DataSetDataDB.storeSensorValues(conn, allSensorValues.getAll());

      // Run the Data Reduction
      DataReductionRecord dataReductionRecord = runDataReduction(conn,
        instrument, variable, dataset, allSensorValues);

      // Check the SST QC flag
      int expectedSSTFlag1 = line.getIntField(EXPECTED_SST_FLAG_1_COL);
      List<String> expectedSSTComment1 = expectedCommentList(line,
        EXPECTED_SST_COMMENT_1_COL);

      checkQC("SST", allSensorValues.getById(SST_VAL_ID), expectedSSTFlag1,
        expectedSSTComment1, allSensorValues);

      // Check the Salinity QC flag
      int expectedSalinityFlag1 = line
        .getIntField(EXPECTED_SALINITY_FLAG_1_COL);
      List<String> expectedSalinityComment1 = expectedCommentList(line,
        EXPECTED_SALINITY_COMMENT_1_COL);

      checkQC("Salinity", allSensorValues.getById(SALINITY_VAL_ID),
        expectedSalinityFlag1, expectedSalinityComment1, allSensorValues);

      // Check the CO2 QC flag
      int expectedCO2Flag1 = line.getIntField(EXPECTED_CO2_FLAG_1_COL);
      List<String> expectedCO2Comment1 = expectedCommentList(line,
        EXPECTED_CO2_COMMENT_1_COL);

      checkQC("CO2", allSensorValues.getById(CO2_VAL_ID), expectedCO2Flag1,
        expectedCO2Comment1, allSensorValues);

      // Check the Data Reduction QC flag
      int expectedDataReductionFlag1 = line
        .getIntField(EXPECTED_DATA_REDUCTION_FLAG_1_COL);
      List<String> expectedDataReductionComment1 = expectedCommentList(line,
        EXPECTED_DATA_REDUCTION_COMMENT_1_COL);

      checkQC(dataReductionRecord, expectedDataReductionFlag1,
        expectedDataReductionComment1);

      // Update the diagnostic flags and re-test if required
      if (!line.isFieldEmpty(UPDATED_DIAG_WATER_FLAG_COL)) {

        // Update diagnostic flags
        waterFlow.setUserQC(
          new Flag(line.getIntField(UPDATED_DIAG_WATER_FLAG_COL)), "Liquid");

        allSensorValues.applyQCCascade(waterFlow, runTypePeriods);

        gasFlow.setUserQC(new Flag(line.getIntField(UPDATED_DIAG_GAS_FLAG_COL)),
          "Air");

        allSensorValues.applyQCCascade(gasFlow, runTypePeriods);

        DataSetDataDB.storeSensorValues(conn, allSensorValues.getAll());

        // Run the Data Reduction
        DataReductionRecord newDataReductionRecord = runDataReduction(conn,
          instrument, variable, dataset, allSensorValues);

        // Check the SST QC flag
        int expectedSSTFlag2 = line.getIntField(EXPECTED_SST_FLAG_2_COL);
        List<String> expectedSSTComment2 = expectedCommentList(line,
          EXPECTED_SST_COMMENT_2_COL);

        checkQC("SST", allSensorValues.getById(SST_VAL_ID), expectedSSTFlag2,
          expectedSSTComment2, allSensorValues);

        // Check the Salinity QC flag
        int expectedSalinityFlag2 = line
          .getIntField(EXPECTED_SALINITY_FLAG_2_COL);
        List<String> expectedSalinityComment2 = expectedCommentList(line,
          EXPECTED_SALINITY_COMMENT_2_COL);

        checkQC("Salinity", allSensorValues.getById(SALINITY_VAL_ID),
          expectedSalinityFlag2, expectedSalinityComment2, allSensorValues);

        // Check the CO2 QC flag
        int expectedCO2Flag2 = line.getIntField(EXPECTED_CO2_FLAG_2_COL);
        List<String> expectedCO2Comment2 = expectedCommentList(line,
          EXPECTED_CO2_COMMENT_2_COL);

        checkQC("CO2", allSensorValues.getById(CO2_VAL_ID), expectedCO2Flag2,
          expectedCO2Comment2, allSensorValues);

        // Check the Data Reduction QC flag
        int expectedDataReductionFlag2 = line
          .getIntField(EXPECTED_DATA_REDUCTION_FLAG_2_COL);
        List<String> expectedDataReductionComment2 = expectedCommentList(line,
          EXPECTED_DATA_REDUCTION_COMMENT_2_COL);

        checkQC(newDataReductionRecord, expectedDataReductionFlag2,
          expectedDataReductionComment2);
      }
    }
  }
}
