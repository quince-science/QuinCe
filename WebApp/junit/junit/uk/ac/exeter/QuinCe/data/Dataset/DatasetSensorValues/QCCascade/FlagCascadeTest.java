package junit.uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues.QCCascade;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import junit.uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import junit.uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetMeasurements;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.DefaultMeasurementValueCollector;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementLocator;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValueCollector;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.SimpleMeasurementLocator;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducer;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

@TestInstance(Lifecycle.PER_CLASS)
public class FlagCascadeTest extends TestSetTest {

  private static final long VAR_ID = 1000000L;

  private static final int SST_Q_CASCADE_COL = 0;

  private static final int SST_B_CASCADE_COL = 1;

  private static final int SAL_Q_CASCADE_COL = 2;

  private static final int SAL_B_CASCADE_COL = 3;

  private static final int SST_FLAG_COL = 4;

  private static final int SAL_FLAG_COL = 5;

  private static final int DATA_REDUCTION_FLAG_COL = 6;

  private int setCascade(Connection conn, String sensorType,
    int questionableCascade, int badCascade) throws SQLException {

    PreparedStatement stmt = conn.prepareStatement("UPDATE "
      + "variable_sensors SET questionable_cascade = ?, bad_cascade = ? "
      + "WHERE variable_id = " + VAR_ID + " AND sensor_type = "
      + "(SELECT id FROM sensor_types WHERE name = ?)");

    stmt.setInt(1, questionableCascade);
    stmt.setInt(2, badCascade);
    stmt.setString(3, sensorType);

    int updateCount = stmt.executeUpdate();
    stmt.close();
    conn.commit();

    return updateCount;
  }

  /**
   * Test that flag cascades are applied when measurements have sensor values at
   * the same timestamps.
   *
   * @param line
   * @throws Exception
   */
  @FlywayTest(locationsForMigrate = {
    "resources/sql/testbase/DataReduction/base",
    "resources/sql/testbase/DataReduction/singleMeasurement" })
  @ParameterizedTest
  @MethodSource("getLines")
  public void flagCascadeTest(TestSetLine line) throws Exception {

    initResourceManager();

    try (Connection conn = getConnection()) {
      // First we adjust the flag cascades
      int sstQuestionableCascade = line.getIntField(SST_Q_CASCADE_COL);
      int sstBadCascade = line.getIntField(SST_B_CASCADE_COL);

      assertEquals(1, setCascade(conn, "Water Temperature",
        sstQuestionableCascade, sstBadCascade));

      int salQuestionableCascade = line.getIntField(SAL_Q_CASCADE_COL);
      int salBadCascade = line.getIntField(SAL_B_CASCADE_COL);

      assertEquals(1,
        setCascade(conn, "Salinity", salQuestionableCascade, salBadCascade));
    }

    // Reinitialise the Resource Manager so the new cascades are loaded
    ResourceManager.destroy();
    initResourceManager();

    try (Connection conn = getConnection()) {
      Instrument instrument = InstrumentDB.getInstrument(conn, 1L);
      Variable variable = instrument.getVariables().get(0);
      DataSet dataset = DataSetDB.getDataSet(conn, 1L);

      // Set the QC flags on sensor values
      DatasetSensorValues allSensorValues = DataSetDataDB.getSensorValues(conn,
        instrument, dataset.getId(), false, false);

      SensorValue sstVal = allSensorValues.getById(2L);
      SensorValue salVal = allSensorValues.getById(3L);

      sstVal.setUserQC(new Flag(line.getIntField(SST_FLAG_COL)), "Comment");
      salVal.setUserQC(new Flag(line.getIntField(SAL_FLAG_COL)), "Comment");

      DataSetDataDB.storeSensorValues(conn, Arrays.asList(sstVal, salVal));

      // Create the measurements for the dataset by running the
      // MeasurementLocator job
      MeasurementLocator measurementLocator = new SimpleMeasurementLocator(
        variable);
      List<Measurement> locatedMeasurements = measurementLocator
        .locateMeasurements(conn, instrument, dataset);

      DataSetDataDB.storeMeasurements(conn, locatedMeasurements);

      // Get all the measurements grouped by run type
      DatasetMeasurements allMeasurements = DataSetDataDB
        .getMeasurementsByRunType(conn, instrument, dataset.getId());

      MeasurementValueCollector measurementValueCollector = new DefaultMeasurementValueCollector();

      Measurement measurement = locatedMeasurements.get(0);

      Collection<MeasurementValue> measurementValues = measurementValueCollector
        .collectMeasurementValues(instrument, dataset, variable,
          allMeasurements, allSensorValues, conn, measurement);

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

      DataReductionRecord record = reducer.performDataReduction(instrument,
        locatedMeasurements.get(0), conn);

      assertEquals(line.getIntField(DATA_REDUCTION_FLAG_COL),
        record.getQCFlag().getWoceValue());
    }
  }

  @Override
  protected String getTestSetName() {
    return "FlagCascadeTests";
  }
}
