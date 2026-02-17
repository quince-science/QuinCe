package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction.DataReductionQCRoutine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction.DataReductionQCRoutinesConfiguration;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.FlaggedItems;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Base class for tests that result in a {@link FlaggedItems} object that needs
 * to be checked.
 */
public class DataReductionQCRoutineTest extends BaseTest {

  /**
   * Initialise the Resource Manager.
   */
  @BeforeEach
  public void setup() throws Exception {
    initResourceManager();
  }

  /**
   * Destroy the Resource Manager.
   */
  @AfterEach
  public void tearDown() {
    ResourceManager.destroy();
  }

  /**
   * Check a {@link FlaggedItems} object to see if it is empty (i.e. there are
   * no flagged items).
   *
   * @param flaggedItems
   *          The {@link FlaggedItems} object.
   * @return {@code true} if there are no flagged items; {@code false} if there
   *         are.
   */
  protected boolean flaggedItemsEmpty(FlaggedItems flaggedItems) {
    return flaggedItems.getSensorValues().size() == 0
      && flaggedItems.getMeasurements().size() == 0
      && flaggedItems.getDataReductionRecords().size() == 0;
  }

  /**
   * Check a {@link FlaggedItems} object to see if it contains the specified
   * items.
   *
   * <p>
   * Items are identified by their database IDs. For data reduction records, the
   * corresponding measurement IDs are used.
   * </p>
   *
   * @param flaggedItems
   *          The {@link FlaggedItems} object.
   * @param sensorValueIds
   *          The IDs of the expected {@link SensorValue}s.
   * @param measurementIDs
   *          The IDs of the expected {@link Measurement}s.
   * @param dataReductionMeasurementIds
   *          The measurement IDs of the expected {@link DataReductionRecord}s.
   * @return {@code true} if the {@link FlaggedItems} object contains exactly
   *         the specified items; {@code false} if not.
   */
  protected boolean flaggedItemsCheck(FlaggedItems flaggedItems,
    List<Long> sensorValueIds, List<Long> measurementIDs,
    List<Long> dataReductionMeasurementIds) {

    boolean ok = true;

    List<Long> flaggedSensorValues = flaggedItems.getSensorValues().stream()
      .map(v -> v.getId()).toList();
    ok = listsEqualUnsorted(flaggedSensorValues, sensorValueIds);

    if (ok) {
      List<Long> flaggedMeasurements = flaggedItems.getMeasurements().stream()
        .map(m -> m.getId()).toList();
      ok = listsEqualUnsorted(flaggedMeasurements, measurementIDs);
    }

    if (ok) {
      List<Long> flaggedDataReductionRecords = flaggedItems
        .getDataReductionRecords().stream().map(d -> d.getMeasurementId())
        .toList();
      ok = listsEqualUnsorted(flaggedDataReductionRecords,
        dataReductionMeasurementIds);
    }

    return ok;
  }

  /**
   * Get the Data Reduction QC Routines configuration.
   *
   * @return The configuration.
   */
  protected DataReductionQCRoutinesConfiguration getDataReductionQCRoutinesConfiguration() {
    return ResourceManager.getInstance()
      .getDataReductionQCRoutinesConfiguration();
  }

  /**
   * Get a specified {@link DataReductionQCRoutine} for a specified
   * {@link DataReducer} from the system configuration.
   *
   * @param reducerClass
   *          The {@link DataReducer}.
   * @param routineClass
   *          The {@link DataReductionQCRoutine}.
   * @return The routine.
   */
  protected DataReductionQCRoutine getRoutine(
    Class<? extends DataReducer> reducerClass,
    Class<? extends DataReductionQCRoutine> routineClass) {

    return getDataReductionQCRoutinesConfiguration().getRoutines(reducerClass)
      .stream().filter(routine -> routine.getClass().equals(routineClass))
      .findAny().get();
  }
}
